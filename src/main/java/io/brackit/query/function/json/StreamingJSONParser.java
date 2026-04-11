/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.function.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jsonitem.array.StreamingArray;
import io.brackit.query.jsonitem.object.ArrayObject;

/**
 * Streaming JSON parser that reads from an {@link InputStream} in fixed-size buffer chunks.
 * <p>
 * For top-level arrays (or objects wrapping a single array), returns a {@link StreamingArray}
 * whose elements are parsed on-demand — the file is never fully loaded into memory.
 * For small or non-array inputs, falls back to buffering and using {@link FastJSONParser}.
 * <p>
 * This enables processing of multi-GB JSON files that exceed Java's array size limit.
 */
public final class StreamingJSONParser {

  private static final int DEFAULT_BUFFER_SIZE = 8 * 1024 * 1024; // 8 MB

  private final InputStream in;
  private byte[] buf;
  private int pos;  // current read position in buf
  private int limit; // number of valid bytes in buf
  private boolean eof;

  public StreamingJSONParser(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE);
  }

  public StreamingJSONParser(InputStream in, int bufferSize) {
    this.in = in;
    this.buf = new byte[bufferSize];
    this.pos = 0;
    this.limit = 0;
    this.eof = false;
  }

  /**
   * Parse the top-level JSON value. If it's an array or an object containing a single
   * array value, returns a {@link StreamingArray} for lazy element-by-element parsing.
   * Otherwise buffers the entire input and parses with {@link FastJSONParser}.
   */
  public Item parse() throws QueryException {
    try {
      ensureAvailable();
      skipWhitespace();

      if (pos >= limit && eof) {
        throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Empty JSON input");
      }

      byte first = buf[pos];

      if (first == '[') {
        // Top-level array — stream it lazily
        pos++; // consume '['
        return new StreamingArray(this);
      }

      if (first == '{') {
        // Parse object — may contain streaming inner arrays
        return parseObjectOrStream();
      }

      // Scalar or other — buffer and use FastJSONParser
      return parseBuffered();
    } catch (IOException e) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "I/O error: %s", e.getMessage());
    }
  }

  /**
   * Called by StreamingArray to get the next array element's bytes.
   * Returns null when the array is exhausted (']' found).
   * <p>
   * Fast path: if the element fits entirely within the current buffer,
   * parse directly from the buffer slice (zero-copy). Falls back to
   * extractValue() only for elements that span buffer boundaries.
   */
  public Item nextArrayElement() throws QueryException {
    try {
      ensureAvailable();
      skipWhitespace();

      if (pos >= limit && eof) {
        return null; // Stream ended
      }

      byte b = buf[pos];

      // End of array
      if (b == ']') {
        pos++;
        return null;
      }

      // Skip comma between elements
      if (b == ',') {
        pos++;
        ensureAvailable();
        skipWhitespace();
        if (pos >= limit && eof) {
          return null;
        }
        b = buf[pos];
        if (b == ']') {
          pos++;
          return null;
        }
      }

      // Fast path: try to find the element boundary within the current buffer
      int elementEnd = findValueEnd(pos);
      if (elementEnd > 0) {
        // Element fits in buffer — parse directly from slice (zero-copy)
        Item result = new FastJSONParser(buf, pos, elementEnd - pos).parse();
        pos = elementEnd;
        return result;
      }

      // Slow path: element spans buffer boundary, must copy
      byte[] elementBytes = extractValue();
      return new FastJSONParser(elementBytes).parse();
    } catch (IOException e) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "I/O error: %s", e.getMessage());
    }
  }

  /**
   * Try to find the end position of a JSON value starting at {@code start}
   * within the current buffer. Returns the position after the value, or -1
   * if the value extends beyond the buffer (needs slow path).
   */
  private int findValueEnd(int start) {
    int depth = 0;
    boolean inString = false;
    int i = start;

    while (i < limit) {
      byte b = buf[i];

      if (inString) {
        if (b == '\\') {
          i += 2; // skip escape + next char
          continue;
        }
        if (b == '"') {
          inString = false;
          if (depth == 0) {
            return i + 1; // end of top-level string value
          }
        }
        i++;
        continue;
      }

      switch (b) {
        case '"':
          inString = true;
          i++;
          break;
        case '{':
        case '[':
          depth++;
          i++;
          break;
        case '}':
        case ']':
          if (depth == 0) {
            return (i > start) ? i : -1; // end of scalar before structural char
          }
          depth--;
          i++;
          if (depth == 0) {
            return i; // end of object/array
          }
          break;
        case ',':
          if (depth == 0) {
            return i; // end of scalar value
          }
          i++;
          break;
        case ' ':
        case '\t':
        case '\n':
        case '\r':
          if (depth == 0 && i > start) {
            return i; // whitespace after scalar
          }
          i++;
          break;
        default:
          i++;
          break;
      }
    }

    return -1; // extends beyond buffer
  }

  /**
   * Parse a top-level object. Parses field by field; when a field value is an array,
   * it's represented as a StreamingArray for lazy parsing. This allows queries like
   * {@code $$.users[]} to stream through a large inner array without materialization.
   */
  private Item parseObjectOrStream() throws QueryException, IOException {
    // Save position to rewind on parse issues
    int savedPos = pos;

    pos++; // consume '{'
    skipWhitespace();
    ensureAvailable();

    if (pos < limit && buf[pos] == '}') {
      pos++;
      return new ArrayObject(new QNm[0], new Item[0]);
    }

    // Parse fields one by one
    List<QNm> fields = new ArrayList<>(8);
    List<Sequence> values = new ArrayList<>(8);
    Map<QNm, Sequence> map = new HashMap<>(8);
    HashMap<String, QNm> fieldNameCache = new HashMap<>(8);

    while (true) {
      skipWhitespace();
      ensureAvailable();

      // Parse field name (must be a string)
      if (pos >= limit || buf[pos] != '"') {
        pos = savedPos;
        return parseBuffered();
      }
      String fieldName = extractJsonString();
      QNm qnm = fieldNameCache.computeIfAbsent(fieldName, QNm::new);

      skipWhitespace();
      ensureAvailable();

      // Expect ':'
      if (pos >= limit || buf[pos] != ':') {
        pos = savedPos;
        return parseBuffered();
      }
      pos++; // consume ':'

      skipWhitespace();
      ensureAvailable();

      Sequence value;

      if (pos < limit && buf[pos] == '[') {
        // Array value — use StreamingArray with forced materialization
        pos++; // consume '['
        StreamingArray streamingArr = new StreamingArray(this, false);
        streamingArr.values(); // force materialization so cursor advances past ']'
        value = streamingArr;
      } else {
        // Non-array value — extract and parse
        byte[] valueBytes = extractValue();
        value = new FastJSONParser(valueBytes).parse();
      }

      fields.add(qnm);
      values.add(value);
      map.put(qnm, value);

      skipWhitespace();
      ensureAvailable();

      if (pos < limit && buf[pos] == ',') {
        pos++; // consume ',' and continue to next field
      } else {
        break; // no more fields
      }
    }

    // Expect '}'
    if (pos < limit && buf[pos] == '}') {
      pos++;
    }

    return new ArrayObject(fields, values, map);
  }

  /**
   * Extract a JSON string value (starting from opening '"') and return its content.
   */
  private String extractJsonString() throws IOException, QueryException {
    if (pos >= limit || buf[pos] != '"') {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Expected '\"' at position %d", pos);
    }
    pos++; // consume opening '"'
    var sb = new StringBuilder(64);
    while (true) {
      ensureAvailable();
      if (pos >= limit) {
        throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unterminated string");
      }
      byte b = buf[pos];
      pos++;
      if (b == '"') {
        return sb.toString();
      }
      if (b == '\\') {
        ensureAvailable();
        if (pos < limit) {
          byte escaped = buf[pos];
          pos++;
          sb.append(switch (escaped) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> (char) escaped;
          });
        }
      } else {
        sb.append((char) (b & 0xFF));
      }
    }
  }

  /**
   * Buffer the entire remaining input and parse with FastJSONParser.
   * Used for small inputs or non-streamable top-level structures.
   */
  private Item parseBuffered() throws QueryException, IOException {
    // Collect all remaining bytes
    var bos = new ByteArrayOutputStream(Math.max(limit - pos, 1024));
    bos.write(buf, pos, limit - pos);
    if (!eof) {
      byte[] tmp = new byte[8192];
      int n;
      while ((n = in.read(tmp)) != -1) {
        bos.write(tmp, 0, n);
      }
    }
    pos = limit; // consumed everything
    eof = true;
    return new FastJSONParser(bos.toByteArray()).parse();
  }

  // ==================== Buffer management ====================

  private void ensureAvailable() throws IOException {
    if (pos >= limit && !eof) {
      refill();
    }
  }

  private void refill() throws IOException {
    // Move unconsumed data to the start of the buffer
    int remaining = limit - pos;
    if (remaining > 0) {
      System.arraycopy(buf, pos, buf, 0, remaining);
    }
    pos = 0;
    limit = remaining;

    // Fill the rest of the buffer
    while (limit < buf.length && !eof) {
      int n = in.read(buf, limit, buf.length - limit);
      if (n == -1) {
        eof = true;
      } else {
        limit += n;
      }
    }
  }

  // ==================== Byte-level scanning ====================

  /**
   * Extract a complete JSON value starting at the current position.
   * Tracks brace/bracket depth and string state to find the boundary.
   * Returns the raw bytes of the value.
   */
  byte[] extractValue() throws IOException, QueryException {
    var bos = new java.io.ByteArrayOutputStream(4096);
    int depth = 0;
    boolean inString = false;
    boolean started = false;

    while (true) {
      ensureAvailable();
      if (pos >= limit && eof) {
        if (!started) {
          throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unexpected end of input");
        }
        break;
      }

      byte b = buf[pos];

      if (!started) {
        // Skip leading whitespace
        if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
          pos++;
          continue;
        }
        started = true;
      }

      if (inString) {
        bos.write(b);
        pos++;
        if (b == '\\') {
          // Escape: consume the next byte too
          ensureAvailable();
          if (pos < limit) {
            bos.write(buf[pos]);
            pos++;
          }
        } else if (b == '"') {
          inString = false;
          if (depth == 0) {
            break; // Complete string value at top level
          }
        }
        continue;
      }

      switch (b) {
        case '"':
          inString = true;
          bos.write(b);
          pos++;
          break;
        case '{':
        case '[':
          depth++;
          bos.write(b);
          pos++;
          break;
        case '}':
        case ']':
          if (depth == 0) {
            // This closing bracket belongs to an outer structure — don't consume it
            return bos.toByteArray();
          }
          depth--;
          bos.write(b);
          pos++;
          if (depth == 0) {
            return bos.toByteArray(); // Complete object/array
          }
          break;
        case ',':
          if (depth == 0) {
            // End of a top-level scalar (number, true, false, null)
            return bos.toByteArray();
          }
          bos.write(b);
          pos++;
          break;
        default:
          // Part of a number, true, false, or null
          if (depth == 0 && (b == ' ' || b == '\t' || b == '\n' || b == '\r')) {
            // Whitespace after a top-level scalar
            return bos.toByteArray();
          }
          bos.write(b);
          pos++;
          break;
      }
    }

    return bos.toByteArray();
  }

  /**
   * Skip over a JSON string (starting at the opening '"').
   */
  private void skipJsonString() throws IOException {
    pos++; // consume opening '"'
    while (true) {
      ensureAvailable();
      if (pos >= limit) {
        return;
      }
      byte b = buf[pos];
      pos++;
      if (b == '\\') {
        ensureAvailable();
        if (pos < limit) {
          pos++; // skip escaped char
        }
      } else if (b == '"') {
        return;
      }
    }
  }

  private void skipWhitespace() throws IOException {
    while (true) {
      if (pos >= limit) {
        if (eof)
          return;
        refill();
        if (pos >= limit)
          return;
      }
      byte b = buf[pos];
      if (b != ' ' && b != '\t' && b != '\n' && b != '\r') {
        return;
      }
      pos++;
    }
  }

  /**
   * Skip trailing '}' after a wrapped array completes.
   */
  public void skipTrailingObjectClose() throws QueryException {
    try {
      skipWhitespace();
      ensureAvailable();
      if (pos < limit && buf[pos] == '}') {
        pos++;
      }
    } catch (IOException e) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "I/O error: %s", e.getMessage());
    }
  }
}
