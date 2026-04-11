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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.*;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jsonitem.array.DArray;
import io.brackit.query.jsonitem.object.CompactObject;

/**
 * High-performance byte-level JSON parser optimized for low allocation and cache efficiency.
 * <p>
 * Key optimizations over {@link JSONParser}:
 * <ul>
 * <li>Operates on byte[] directly — no char[] conversion or Tokenizer overhead</li>
 * <li>Field name interning — reuses QNm instances for repeated field names</li>
 * <li>Direct number parsing — parses int/long from bytes without String intermediaries</li>
 * <li>Singleton Null — reuses {@link Null#INSTANCE}</li>
 * <li>Pre-sized collections — avoids ArrayList resizing for typical JSON structures</li>
 * <li>Zero-copy transfer to ArrayObject — avoids GapList/HashMap re-creation</li>
 * </ul>
 */
public final class FastJSONParser {

  private byte[] input;
  private int pos;
  private int end;

  // Field name interning: reuse QNm instances for repeated keys (e.g., objects in arrays)
  private final HashMap<String, QNm> fieldNameCache = new HashMap<>(32);

  // String value interning: reuse Str instances for repeated values (e.g., city names)
  private final HashMap<String, Str> stringValueCache = new HashMap<>(64);

  // Reusable buffer for string parsing (avoids StringBuilder allocation per string)
  private final StringBuilder sb = new StringBuilder(128);

  public FastJSONParser(byte[] input) {
    this.input = input;
    this.pos = 0;
    this.end = input.length;
  }

  /**
   * Parse a slice of a byte array without copying. The caller guarantees
   * that {@code input[offset..offset+length)} contains a complete JSON value.
   */
  public FastJSONParser(byte[] input, int offset, int length) {
    this.input = input;
    this.pos = offset;
    this.end = offset + length;
  }

  public FastJSONParser(String input) {
    this(input.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Reset this parser to parse a new slice of a byte array.
   * Reuses the field name cache and StringBuilder, avoiding allocation.
   */
  public void reset(byte[] input, int offset, int length) {
    this.input = input;
    this.pos = offset;
    this.end = offset + length;
  }

  public Item parse() throws QueryException {
    skipWhitespace();
    if (pos >= end) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Empty JSON input");
    }
    Item result = parseValue();
    skipWhitespace();
    if (pos < end) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Trailing content at position %d", pos);
    }
    return result;
  }

  // ==================== Core dispatch ====================

  private Item parseValue() throws QueryException {
    skipWhitespace();
    if (pos >= end) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unexpected end of input");
    }
    return switch (input[pos]) {
      case '{' -> parseObject();
      case '[' -> parseArray();
      case '"' -> parseString();
      case 't' -> parseTrue();
      case 'f' -> parseFalse();
      case 'n' -> parseNull();
      default -> parseNumber();
    };
  }

  // ==================== Object parsing ====================

  private static final QNm[] EMPTY_QNM = new QNm[0];
  private static final Sequence[] EMPTY_SEQ = new Sequence[0];

  private Item parseObject() throws QueryException {
    pos++; // consume '{'
    skipWhitespace();

    if (pos < end && input[pos] == '}') {
      pos++;
      return new CompactObject(EMPTY_QNM, EMPTY_SEQ);
    }

    // Use local arrays to support nested objects (scratch arrays would be overwritten)
    QNm[] localFields = new QNm[8];
    Sequence[] localValues = new Sequence[8];
    int len = 0;

    do {
      skipWhitespace();
      if (pos >= end || input[pos] != '"') {
        throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Expected '\"' at position %d", pos);
      }
      String fieldName = parseRawString();
      QNm qnm = fieldNameCache.computeIfAbsent(fieldName, QNm::new);

      skipWhitespace();
      expect(':');

      Item value = parseValue();

      if (len == localFields.length) {
        int newCap = localFields.length * 2;
        QNm[] nf = new QNm[newCap];
        Sequence[] nv = new Sequence[newCap];
        System.arraycopy(localFields, 0, nf, 0, len);
        System.arraycopy(localValues, 0, nv, 0, len);
        localFields = nf;
        localValues = nv;
      }
      localFields[len] = qnm;
      localValues[len] = value;
      len++;

      skipWhitespace();
    } while (consumeIf(','));

    expect('}');

    // Trim to exact size if needed
    if (len < localFields.length) {
      QNm[] fields = new QNm[len];
      Sequence[] values = new Sequence[len];
      System.arraycopy(localFields, 0, fields, 0, len);
      System.arraycopy(localValues, 0, values, 0, len);
      return new CompactObject(fields, values);
    }
    return new CompactObject(localFields, localValues);
  }

  // ==================== Array parsing ====================

  private Item parseArray() throws QueryException {
    pos++; // consume '['
    skipWhitespace();

    if (pos < end && input[pos] == ']') {
      pos++;
      return new DArray(List.of());
    }

    List<Item> items = new ArrayList<>(16);
    do {
      items.add(parseValue());
      skipWhitespace();
    } while (consumeIf(','));

    expect(']');
    return new DArray(items);
  }

  // ==================== String parsing ====================

  private static final int STRING_CACHE_MAX = 1024;

  private Str parseString() throws QueryException {
    String raw = parseRawString();
    // Intern short strings that likely repeat (city names, status values, etc.)
    // Stop interning when cache is full to avoid unbounded growth from unique values
    if (raw.length() <= 16 && stringValueCache.size() < STRING_CACHE_MAX) {
      return stringValueCache.computeIfAbsent(raw, Str::new);
    }
    // Check existing cache entries even when full (for repeated values)
    if (raw.length() <= 16) {
      Str cached = stringValueCache.get(raw);
      if (cached != null)
        return cached;
    }
    return new Str(raw);
  }

  /**
   * Parse a JSON string and return its value. Handles escape sequences.
   * Uses a shared StringBuilder to avoid per-string allocation.
   */
  private String parseRawString() throws QueryException {
    pos++; // consume opening '"'
    int start = pos;

    // Fast path: scan for closing quote without escapes
    while (pos < end) {
      byte b = input[pos];
      if (b == '"') {
        // No escapes found — construct string directly from byte range
        String result = new String(input, start, pos - start, StandardCharsets.UTF_8);
        pos++; // consume closing '"'
        return result;
      }
      if (b == '\\') {
        // Has escapes — fall back to slow path
        return parseStringWithEscapes(start);
      }
      pos++;
    }
    throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unterminated string");
  }

  private String parseStringWithEscapes(int start) throws QueryException {
    sb.setLength(0);
    // Copy everything before the backslash
    if (pos > start) {
      sb.append(new String(input, start, pos - start, StandardCharsets.UTF_8));
    }

    while (pos < end) {
      byte b = input[pos];
      if (b == '"') {
        pos++;
        return sb.toString();
      }
      if (b == '\\') {
        pos++;
        if (pos >= end) {
          throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unterminated escape sequence");
        }
        sb.append(switch (input[pos]) {
          case '"' -> '"';
          case '\\' -> '\\';
          case '/' -> '/';
          case 'b' -> '\b';
          case 'f' -> '\f';
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          case 'u' -> {
            pos++;
            if (pos + 4 > end) {
              throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Incomplete unicode escape");
            }
            char c = (char) Integer.parseInt(new String(input, pos, 4, StandardCharsets.US_ASCII), 16);
            pos += 3; // +1 will happen below
            yield c;
          }
          default -> throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Invalid escape: \\%c", (char) input[pos]);
        });
      } else {
        sb.append((char) (b & 0xFF));
      }
      pos++;
    }
    throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unterminated string");
  }

  // ==================== Number parsing ====================

  private Numeric parseNumber() throws QueryException {
    int start = pos;
    boolean negative = false;
    boolean isFloat = false;

    if (pos < end && input[pos] == '-') {
      negative = true;
      pos++;
    }

    // Parse integer part
    long intPart = 0;
    int digits = 0;
    while (pos < end && input[pos] >= '0' && input[pos] <= '9') {
      intPart = intPart * 10 + (input[pos] - '0');
      digits++;
      pos++;
    }

    if (digits == 0) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Expected number at position %d", start);
    }

    // Check for decimal point
    if (pos < end && input[pos] == '.') {
      isFloat = true;
      pos++;
      while (pos < end && input[pos] >= '0' && input[pos] <= '9') {
        pos++;
      }
    }

    // Check for exponent
    if (pos < end && (input[pos] == 'e' || input[pos] == 'E')) {
      isFloat = true;
      pos++;
      if (pos < end && (input[pos] == '+' || input[pos] == '-')) {
        pos++;
      }
      while (pos < end && input[pos] >= '0' && input[pos] <= '9') {
        pos++;
      }
    }

    if (isFloat) {
      // Fall back to Double parsing for floating point
      String numStr = new String(input, start, pos - start, StandardCharsets.US_ASCII);
      return new Dbl(Double.parseDouble(numStr));
    }

    // Integer fast path: direct construction without String intermediary
    long value = negative ? -intPart : intPart;
    if (digits < 10 && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
      return Int32.cached((int) value);
    }
    if (digits < 19) {
      return new Int64(value);
    }
    // Very large numbers: fall back to string parsing
    String numStr = new String(input, start, pos - start, StandardCharsets.US_ASCII);
    return Int32.parse(numStr);
  }

  // ==================== Literal parsing ====================

  private Item parseTrue() throws QueryException {
    expectLiteral("true");
    return Bool.TRUE;
  }

  private Item parseFalse() throws QueryException {
    expectLiteral("false");
    return Bool.FALSE;
  }

  private Item parseNull() throws QueryException {
    expectLiteral("null");
    return Null.INSTANCE;
  }

  // ==================== Utility methods ====================

  private void skipWhitespace() {
    while (pos < end) {
      byte b = input[pos];
      if (b != ' ' && b != '\t' && b != '\n' && b != '\r') {
        return;
      }
      pos++;
    }
  }

  private void expect(char c) throws QueryException {
    if (pos >= end || input[pos] != c) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Expected '%c' at position %d", c, pos);
    }
    pos++;
  }

  private boolean consumeIf(char c) {
    if (pos < end && input[pos] == c) {
      pos++;
      return true;
    }
    return false;
  }

  private void expectLiteral(String literal) throws QueryException {
    int len = literal.length();
    if (pos + len > end) {
      throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Unexpected end of input, expected '%s'", literal);
    }
    for (int i = 0; i < len; i++) {
      if (input[pos + i] != literal.charAt(i)) {
        throw new QueryException(JSONFun.ERR_PARSING_ERROR, "Expected '%s' at position %d", literal, pos);
      }
    }
    pos += len;
  }
}
