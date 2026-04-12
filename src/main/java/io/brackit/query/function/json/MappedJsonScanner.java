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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Memory-mapped JSON scanner using the Foreign Memory API.
 * Maps the entire file, then processes it through a local byte[] window
 * for cache-friendly, JIT-optimizable scanning.
 * <p>
 * The key optimization: instead of per-byte {@code MemorySegment.get()} calls
 * (which have bounds-check overhead), bulk-copy 8MB chunks into a local
 * {@code byte[]} and scan with direct array access. The JIT can vectorize
 * tight loops on {@code byte[]}, but not on MemorySegment random access.
 */
public final class MappedJsonScanner implements AutoCloseable {

  private static final int WINDOW_SIZE = 8 * 1024 * 1024; // 8 MB

  private final Arena arena;
  private final MemorySegment segment;
  private final long fileSize;

  // Local window for fast scanning
  private final byte[] window;
  private long windowStart; // file offset where current window begins
  private int windowLen;    // valid bytes in window

  private long pos; // global file position

  // Element boundaries
  private long elemStart;
  private long elemEnd;

  // Pre-allocated for field extraction
  private int localStart; // element start within current window
  private int localEnd;   // element end within current window
  private boolean elementInWindow; // true if entire element fits in current window

  public MappedJsonScanner(Path path) throws IOException {
    this.arena = Arena.ofShared();
    try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      this.fileSize = channel.size();
      this.segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
    }
    this.window = new byte[WINDOW_SIZE];
    this.pos = 0;
    this.windowStart = 0;
    this.windowLen = 0;
    refillWindow(0);
  }

  public void skipToArrayStart() {
    skipWhitespace();
    if (pos < fileSize && windowByte() == '[') {
      pos++;
    }
  }

  public boolean nextElement() {
    skipWhitespace();
    if (pos >= fileSize)
      return false;

    byte b = windowByte();
    if (b == ']')
      return false;
    if (b == ',') {
      pos++;
      skipWhitespace();
      if (pos >= fileSize)
        return false;
      if (windowByte() == ']')
        return false;
    }

    elemStart = pos;
    elemEnd = findValueEnd();
    if (elemEnd <= elemStart)
      return false;
    pos = elemEnd;

    // Check if element fits entirely within the current window
    if (elemStart >= windowStart && elemEnd <= windowStart + windowLen) {
      localStart = (int) (elemStart - windowStart);
      localEnd = (int) (elemEnd - windowStart);
      elementInWindow = true;
    } else {
      elementInWindow = false;
    }

    return true;
  }

  /**
   * Extract a string field from the current element using fast local array scanning.
   */
  public String extractStringField(byte[] pattern, byte[] patternSpaced) {
    if (elementInWindow) {
      return extractStringFromArray(window, localStart, localEnd, pattern, patternSpaced);
    }
    // Element spans windows — load into temp buffer
    int len = (int) (elemEnd - elemStart);
    byte[] tmp = new byte[len];
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, elemStart, tmp, 0, len);
    return extractStringFromArray(tmp, 0, len, pattern, patternSpaced);
  }

  /**
   * Extract a long field from the current element using fast local array scanning.
   */
  public long extractLongField(byte[] pattern, byte[] patternSpaced) {
    if (elementInWindow) {
      return extractLongFromArray(window, localStart, localEnd, pattern, patternSpaced);
    }
    int len = (int) (elemEnd - elemStart);
    byte[] tmp = new byte[len];
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, elemStart, tmp, 0, len);
    return extractLongFromArray(tmp, 0, len, pattern, patternSpaced);
  }

  // ==================== Fast array-based extraction ====================

  private static String extractStringFromArray(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(buf, pattern, start, end);
    if (pos < 0)
      pos = indexOf(buf, patternSpaced, start, end);
    if (pos < 0)
      return null;

    pos += pattern.length;
    if (pos < end && buf[pos] == ' ')
      pos++;
    if (pos >= end || buf[pos] != '"')
      return null;
    pos++;

    int valStart = pos;
    while (pos < end) {
      if (buf[pos] == '\\') {
        pos += 2;
        continue;
      }
      if (buf[pos] == '"') {
        return new String(buf, valStart, pos - valStart, StandardCharsets.UTF_8);
      }
      pos++;
    }
    return null;
  }

  private static long extractLongFromArray(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(buf, pattern, start, end);
    if (pos < 0)
      pos = indexOf(buf, patternSpaced, start, end);
    if (pos < 0)
      return 0;

    pos += pattern.length;
    while (pos < end && (buf[pos] == ' ' || buf[pos] == '\t'))
      pos++;

    boolean neg = false;
    if (pos < end && buf[pos] == '-') {
      neg = true;
      pos++;
    }
    long v = 0;
    while (pos < end && buf[pos] >= '0' && buf[pos] <= '9') {
      v = v * 10 + (buf[pos] - '0');
      pos++;
    }
    return neg ? -v : v;
  }

  private static int indexOf(byte[] buf, byte[] needle, int from, int limit) {
    int plen = needle.length;
    outer:
    for (int i = from; i <= limit - plen; i++) {
      for (int j = 0; j < plen; j++) {
        if (buf[i + j] != needle[j])
          continue outer;
      }
      return i;
    }
    return -1;
  }

  // ==================== Window management ====================

  private void refillWindow(long fileOffset) {
    windowStart = fileOffset;
    int toRead = (int) Math.min(WINDOW_SIZE, fileSize - fileOffset);
    if (toRead > 0) {
      MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, fileOffset, window, 0, toRead);
    }
    windowLen = toRead;
  }

  /**
   * Ensure pos is within the current window. If not, refill.
   */
  private void ensureInWindow() {
    if (pos < windowStart || pos >= windowStart + windowLen) {
      refillWindow(pos);
    }
  }

  /**
   * Get byte at current pos using the local window (fast path) or segment (fallback).
   */
  private byte windowByte() {
    ensureInWindow();
    return window[(int) (pos - windowStart)];
  }

  private void skipWhitespace() {
    while (pos < fileSize) {
      ensureInWindow();
      int localPos = (int) (pos - windowStart);
      while (localPos < windowLen) {
        byte b = window[localPos];
        if (b != ' ' && b != '\t' && b != '\n' && b != '\r') {
          pos = windowStart + localPos;
          return;
        }
        localPos++;
      }
      pos = windowStart + localPos; // advance past window
    }
  }

  private long findValueEnd() {
    int depth = 0;
    boolean inString = false;
    long startPos = pos;

    while (pos < fileSize) {
      ensureInWindow();
      int localPos = (int) (pos - windowStart);
      int localLimit = windowLen;

      // Scan within the current window — pure array access, JIT-optimizable
      while (localPos < localLimit) {
        byte b = window[localPos];

        if (inString) {
          if (b == '\\') {
            localPos += 2;
            continue;
          }
          if (b == '"') {
            inString = false;
            if (depth == 0) {
              pos = windowStart + localPos + 1;
              return pos;
            }
          }
          localPos++;
          continue;
        }

        switch (b) {
          case '"' -> {
            inString = true;
            localPos++;
          }
          case '{', '[' -> {
            depth++;
            localPos++;
          }
          case '}', ']' -> {
            if (depth == 0) {
              pos = windowStart + localPos;
              return pos;
            }
            depth--;
            localPos++;
            if (depth == 0) {
              pos = windowStart + localPos;
              return pos;
            }
          }
          case ',' -> {
            if (depth == 0) {
              pos = windowStart + localPos;
              return pos;
            }
            localPos++;
          }
          case ' ', '\t', '\n', '\r' -> {
            if (depth == 0 && (windowStart + localPos) > startPos) {
              pos = windowStart + localPos;
              return pos;
            }
            localPos++;
          }
          default -> localPos++;
        }
      }
      pos = windowStart + localPos; // end of window — continue with next
    }
    return fileSize;
  }

  @Override
  public void close() {
    arena.close();
  }
}
