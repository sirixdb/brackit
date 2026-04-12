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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Memory-mapped JSON scanner using the Foreign Memory API.
 * Maps the entire file into virtual address space — no buffer management,
 * no refills, no read() syscalls. The OS handles paging transparently.
 * <p>
 * Supports files of any size (no 2GB Java array limit) by operating
 * directly on {@link MemorySegment} offsets.
 * <p>
 * This scanner provides element-by-element access to a top-level JSON array,
 * returning byte offsets for zero-copy field extraction.
 */
public final class MappedJsonScanner implements AutoCloseable {

  private final Arena arena;
  private final MemorySegment segment;
  private final long fileSize;
  private long pos;

  public MappedJsonScanner(Path path) throws IOException {
    this.arena = Arena.ofShared();
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      this.fileSize = channel.size();
      this.segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
    }
    this.pos = 0;
  }

  /**
   * Skip to the start of the array content (past the opening '[').
   */
  public void skipToArrayStart() {
    skipWhitespace();
    if (pos < fileSize && getByte(pos) == '[') {
      pos++;
    }
  }

  /**
   * Advance to the next array element. Returns true if found.
   * Call {@link #getElementStart()} and {@link #getElementEnd()} for boundaries.
   */
  private long elemStart;
  private long elemEnd;

  public boolean nextElement() {
    skipWhitespace();
    if (pos >= fileSize)
      return false;

    byte b = getByte(pos);
    if (b == ']')
      return false;
    if (b == ',') {
      pos++;
      skipWhitespace();
      if (pos >= fileSize || getByte(pos) == ']')
        return false;
    }

    elemStart = pos;
    elemEnd = findValueEnd();
    if (elemEnd <= elemStart)
      return false;
    pos = elemEnd;
    return true;
  }

  public long getElementStart() {
    return elemStart;
  }

  public long getElementEnd() {
    return elemEnd;
  }

  /**
   * Extract a string field value from the element at [elemStart, elemEnd).
   * Scans for the field pattern directly in mapped memory.
   */
  public String extractStringField(String fieldName) {
    byte[] pattern = ("\"" + fieldName + "\":").getBytes();
    byte[] patternSpaced = ("\"" + fieldName + "\" :").getBytes();

    long fpos = indexOf(pattern, elemStart, elemEnd);
    if (fpos < 0)
      fpos = indexOf(patternSpaced, elemStart, elemEnd);
    if (fpos < 0)
      return null;

    fpos += pattern.length;
    while (fpos < elemEnd && getByte(fpos) == ' ')
      fpos++;
    if (fpos >= elemEnd || getByte(fpos) != '"')
      return null;
    fpos++;

    long valStart = fpos;
    while (fpos < elemEnd) {
      byte c = getByte(fpos);
      if (c == '\\') {
        fpos += 2;
        continue;
      }
      if (c == '"') {
        int len = (int) (fpos - valStart);
        byte[] bytes = new byte[len];
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, valStart, bytes, 0, len);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
      }
      fpos++;
    }
    return null;
  }

  /**
   * Extract a long field value from the element at [elemStart, elemEnd).
   */
  public long extractLongField(String fieldName) {
    byte[] pattern = ("\"" + fieldName + "\":").getBytes();
    byte[] patternSpaced = ("\"" + fieldName + "\" :").getBytes();

    long fpos = indexOf(pattern, elemStart, elemEnd);
    if (fpos < 0)
      fpos = indexOf(patternSpaced, elemStart, elemEnd);
    if (fpos < 0)
      return 0;

    fpos += pattern.length;
    while (fpos < elemEnd && (getByte(fpos) == ' ' || getByte(fpos) == '\t'))
      fpos++;

    boolean negative = false;
    if (fpos < elemEnd && getByte(fpos) == '-') {
      negative = true;
      fpos++;
    }
    long value = 0;
    while (fpos < elemEnd) {
      byte c = getByte(fpos);
      if (c < '0' || c > '9')
        break;
      value = value * 10 + (c - '0');
      fpos++;
    }
    return negative ? -value : value;
  }

  // ==================== Internal ====================

  private byte getByte(long offset) {
    return segment.get(ValueLayout.JAVA_BYTE, offset);
  }

  private void skipWhitespace() {
    while (pos < fileSize) {
      byte b = getByte(pos);
      if (b != ' ' && b != '\t' && b != '\n' && b != '\r')
        return;
      pos++;
    }
  }

  private long findValueEnd() {
    int depth = 0;
    boolean inString = false;
    long i = pos;

    while (i < fileSize) {
      byte b = getByte(i);
      if (inString) {
        if (b == '\\') {
          i += 2;
          continue;
        }
        if (b == '"') {
          inString = false;
          if (depth == 0)
            return i + 1;
        }
        i++;
        continue;
      }
      switch (b) {
        case '"' -> {
          inString = true;
          i++;
        }
        case '{', '[' -> {
          depth++;
          i++;
        }
        case '}', ']' -> {
          if (depth == 0)
            return i;
          depth--;
          i++;
          if (depth == 0)
            return i;
        }
        case ',' -> {
          if (depth == 0)
            return i;
          i++;
        }
        case ' ', '\t', '\n', '\r' -> {
          if (depth == 0 && i > pos)
            return i;
          i++;
        }
        default -> i++;
      }
    }
    return fileSize;
  }

  private long indexOf(byte[] pattern, long from, long limit) {
    int plen = pattern.length;
    outer:
    for (long i = from; i <= limit - plen; i++) {
      for (int j = 0; j < plen; j++) {
        if (getByte(i + j) != pattern[j])
          continue outer;
      }
      return i;
    }
    return -1;
  }

  @Override
  public void close() {
    arena.close();
  }
}
