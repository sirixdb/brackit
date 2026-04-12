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
package io.brackit.query.operator.vectorized;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jsonitem.object.CompactObject;

/**
 * 1BRC-inspired parallel group-by execution.
 * <p>
 * Key techniques from the 1 Billion Row Challenge winners:
 * <ul>
 * <li>Memory-mapped I/O via Foreign Memory API (no read syscalls)</li>
 * <li>Parallel chunk processing — file split into N chunks, one per core</li>
 * <li>Per-thread hash maps merged at the end</li>
 * <li>Byte-level field extraction — no JSON object creation</li>
 * <li>Local byte[] windows for cache-friendly scanning</li>
 * </ul>
 */
public final class ParallelGroupByExec {

  private static final int WINDOW_SIZE = 8 * 1024 * 1024;
  private static final VectorSpecies<Byte> BYTE_SPECIES;
  static {
    VectorSpecies<Byte> species;
    try {
      species = ByteVector.SPECIES_PREFERRED;
      species.length(); // force init
    } catch (Throwable t) {
      species = null;
    }
    BYTE_SPECIES = species;
  }

  /**
   * Execute a parallel group-by-count on a memory-mapped JSON file.
   */
  public static List<Item> executeGroupByCount(Path path, String groupField) throws Exception {
    int nThreads = Runtime.getRuntime().availableProcessors();
    byte[] pattern = ("\"" + groupField + "\":").getBytes(StandardCharsets.UTF_8);
    byte[] patternSpaced = ("\"" + groupField + "\" :").getBytes(StandardCharsets.UTF_8);

    try (Arena arena = Arena.ofShared(); FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      long fileSize = channel.size();
      MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);

      long arrayStart = findArrayStart(segment, fileSize);
      long[] chunkStarts = new long[nThreads + 1];
      chunkStarts[0] = arrayStart;
      chunkStarts[nThreads] = fileSize;
      long chunkSize = (fileSize - arrayStart) / nThreads;
      for (int i = 1; i < nThreads; i++) {
        chunkStarts[i] = alignToRecordBoundary(segment, arrayStart + i * chunkSize, fileSize);
      }

      ExecutorService executor = Executors.newFixedThreadPool(nThreads);
      List<Future<HashMap<String, long[]>>> futures = new ArrayList<>(nThreads);
      for (int i = 0; i < nThreads; i++) {
        long start = chunkStarts[i];
        long end = chunkStarts[i + 1];
        futures.add(executor.submit(() -> processChunk(segment, start, end, pattern, patternSpaced)));
      }

      // Merge results
      HashMap<String, long[]> merged = new HashMap<>();
      for (Future<HashMap<String, long[]>> future : futures) {
        HashMap<String, long[]> partial = future.get();
        for (Map.Entry<String, long[]> entry : partial.entrySet()) {
          merged.merge(entry.getKey(), entry.getValue(), (a, b) -> {
            a[0] += b[0];
            return a;
          });
        }
      }

      executor.shutdown();
      return buildResult(merged, groupField);
    }
  }

  /**
   * Execute a parallel filtered count on a memory-mapped JSON file.
   */
  public static long executeFilterCount(Path path, String filterField, String filterOp, long filterValue)
      throws Exception {
    int nThreads = Runtime.getRuntime().availableProcessors();
    byte[] pattern = ("\"" + filterField + "\":").getBytes(StandardCharsets.UTF_8);
    byte[] patternSpaced = ("\"" + filterField + "\" :").getBytes(StandardCharsets.UTF_8);

    try (Arena arena = Arena.ofShared(); FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
      long fileSize = channel.size();
      MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);

      long arrayStart = findArrayStart(segment, fileSize);

      long[] chunkStarts = new long[nThreads + 1];
      chunkStarts[0] = arrayStart;
      chunkStarts[nThreads] = fileSize;

      long chunkSize = (fileSize - arrayStart) / nThreads;
      for (int i = 1; i < nThreads; i++) {
        long approx = arrayStart + i * chunkSize;
        chunkStarts[i] = alignToRecordBoundary(segment, approx, fileSize);
      }

      ExecutorService executor = Executors.newFixedThreadPool(nThreads);
      List<Future<Long>> futures = new ArrayList<>(nThreads);

      for (int i = 0; i < nThreads; i++) {
        long start = chunkStarts[i];
        long end = chunkStarts[i + 1];
        futures.add(executor.submit(() -> processChunkFilter(segment,
                                                             start,
                                                             end,
                                                             pattern,
                                                             patternSpaced,
                                                             filterOp,
                                                             filterValue)));
      }

      long total = 0;
      for (Future<Long> future : futures) {
        total += future.get();
      }

      executor.shutdown();
      return total;
    }
  }

  // ==================== Chunk processing ====================

  /**
   * Open-addressing byte-key hash map — avoids String allocation entirely.
   * 1BRC technique: hash and compare on raw bytes. Only create String for output.
   */
  private static final int INTERN_CAPACITY = 1024;
  private static final int INTERN_MASK = INTERN_CAPACITY - 1;

  private static HashMap<String, long[]> processChunk(MemorySegment segment, long start, long end, byte[] pattern,
      byte[] patternSpaced) {
    // Per-thread intern table: raw byte keys → count
    byte[][] internKeys = new byte[INTERN_CAPACITY][];
    long[] internCounts = new long[INTERN_CAPACITY];

    byte[] window = new byte[WINDOW_SIZE];
    long pos = start;
    int patLen = pattern.length;

    while (pos < end) {
      int winLen = (int) Math.min(WINDOW_SIZE, end - pos);
      MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, pos, window, 0, winLen);

      int localPos = 0;
      boolean rewound = false;

      while (localPos < winLen) {
        // Find '{' — scalar scan (records are ~50 bytes apart, SIMD overhead not worth it)
        while (localPos < winLen && window[localPos] != '{')
          localPos++;
        if (localPos >= winLen)
          break;

        int objStart = localPos;

        // Find matching '}' — for flat JSON objects (depth 1), we can skip
        // to the next '}' that's not inside a string. This is the common case
        // for our data: {"name":"...","age":N,"city":"..."}
        int depth = 0;
        boolean inStr = false;
        while (localPos < winLen) {
          byte b = window[localPos];
          if (inStr) {
            if (b == '\\') {
              localPos += 2;
              continue;
            }
            if (b == '"')
              inStr = false;
          } else {
            if (b == '"')
              inStr = true;
            else if (b == '{')
              depth++;
            else if (b == '}') {
              depth--;
              if (depth == 0) {
                localPos++;
                break;
              }
            }
          }
          localPos++;
        }

        if (depth != 0) {
          pos += objStart;
          rewound = true;
          break;
        }

        int objEnd = localPos;

        // SIMD-accelerated field search — find first byte of pattern, then verify rest
        int fp = simdFind(window, objStart, objEnd, pattern[0]);
        int valueStart = -1;
        int valueEnd = -1;

        int stop = objEnd - patLen;
        while (fp <= stop) {
          // Verify full pattern match
          boolean match = true;
          for (int j = 1; j < patLen; j++) {
            if (window[fp + j] != pattern[j]) {
              match = false;
              break;
            }
          }
          if (!match) {
            fp = simdFind(window, fp + 1, objEnd, pattern[0]);
            continue;
          }
          {
            int vp = fp + patLen;
            while (vp < objEnd && window[vp] == ' ')
              vp++;
            if (vp < objEnd && window[vp] == '"') {
              vp++;
              valueStart = vp;
              while (vp < objEnd) {
                if (window[vp] == '\\') {
                  vp += 2;
                  continue;
                }
                if (window[vp] == '"') {
                  valueEnd = vp;
                  break;
                }
                vp++;
              }
            }
            break;
          }
        }

        if (valueStart >= 0 && valueEnd > valueStart) {
          // Aggregate using byte-key intern table — no String allocation
          int keyLen = valueEnd - valueStart;
          int hash = longHash(window, valueStart, keyLen);
          int idx = hash & INTERN_MASK;

          while (true) {
            byte[] existing = internKeys[idx];
            if (existing == null) {
              byte[] keyCopy = new byte[keyLen];
              System.arraycopy(window, valueStart, keyCopy, 0, keyLen);
              internKeys[idx] = keyCopy;
              internCounts[idx] = 1;
              break;
            }
            if (existing.length == keyLen && bytesEqual(existing, 0, window, valueStart, keyLen)) {
              internCounts[idx]++;
              break;
            }
            idx = (idx + 31) & INTERN_MASK; // stride-31 probing (1BRC technique)
          }
        }
      }

      if (!rewound) {
        pos += winLen;
      }
    }

    // Convert byte-key intern table to HashMap<String> for merging
    HashMap<String, long[]> result = new HashMap<>();
    for (int i = 0; i < INTERN_CAPACITY; i++) {
      if (internKeys[i] != null) {
        result.put(new String(internKeys[i], StandardCharsets.UTF_8), new long[] { internCounts[i] });
      }
    }
    return result;
  }

  /**
   * 1BRC-style hash: XOR 8-byte longs from the key bytes.
   * Much faster than per-byte FNV for short keys (city names are 2-9 bytes).
   */
  private static int longHash(byte[] buf, int off, int len) {
    long h;
    if (len >= 8) {
      h = readLong(buf, off);
      if (len >= 16) {
        h ^= readLong(buf, off + 8);
      }
    } else {
      h = 0;
      for (int i = 0; i < len; i++) {
        h = (h << 8) | (buf[off + i] & 0xFF);
      }
    }
    h ^= (h >>> 33) ^ (h >>> 15);
    return (int) h;
  }

  private static long readLong(byte[] buf, int off) {
    return ((long) buf[off] << 56) | ((long) (buf[off + 1] & 0xFF) << 48) | ((long) (buf[off + 2] & 0xFF) << 40)
        | ((long) (buf[off + 3] & 0xFF) << 32) | ((long) (buf[off + 4] & 0xFF) << 24) | ((long) (buf[off + 5] & 0xFF)
            << 16) | ((long) (buf[off + 6] & 0xFF) << 8) | (buf[off + 7] & 0xFF);
  }

  /**
   * 1BRC-style key comparison: 8 bytes at a time via long reads.
   */
  private static boolean bytesEqual(byte[] a, int aOff, byte[] b, int bOff, int len) {
    int i = 0;
    for (; i + 8 <= len; i += 8) {
      if (readLong(a, aOff + i) != readLong(b, bOff + i))
        return false;
    }
    for (; i < len; i++) {
      if (a[aOff + i] != b[bOff + i])
        return false;
    }
    return true;
  }

  private static long processChunkFilter(MemorySegment segment, long start, long end, byte[] pattern,
      byte[] patternSpaced, String op, long threshold) {
    long count = 0;
    byte[] window = new byte[WINDOW_SIZE];
    long pos = start;

    while (pos < end) {
      int winLen = (int) Math.min(WINDOW_SIZE, end - pos);
      MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, pos, window, 0, winLen);

      int localPos = 0;
      boolean rewound = false;

      while (localPos < winLen) {
        while (localPos < winLen && window[localPos] != '{')
          localPos++;
        if (localPos >= winLen)
          break;

        int objStart = localPos;
        int depth = 0;
        boolean inStr = false;
        while (localPos < winLen) {
          byte b = window[localPos];
          if (inStr) {
            if (b == '\\') {
              localPos += 2;
              continue;
            }
            if (b == '"')
              inStr = false;
          } else {
            if (b == '"')
              inStr = true;
            else if (b == '{')
              depth++;
            else if (b == '}') {
              depth--;
              if (depth == 0) {
                localPos++;
                break;
              }
            }
          }
          localPos++;
        }

        if (depth != 0) {
          pos += objStart;
          rewound = true;
          break;
        }

        int objEnd = localPos;
        long value = extractLong(window, objStart, objEnd, pattern, patternSpaced);
        boolean pass = switch (op) {
          case "gt" -> value > threshold;
          case "lt" -> value < threshold;
          case "ge" -> value >= threshold;
          case "le" -> value <= threshold;
          case "eq" -> value == threshold;
          default -> true;
        };
        if (pass)
          count++;
      }

      if (!rewound) {
        pos += winLen;
      }
    }

    return count;
  }

  // ==================== Field extraction ====================

  private static String extractString(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(buf, pattern, start, end);
    if (pos < 0)
      pos = indexOf(buf, patternSpaced, start, end);
    if (pos < 0)
      return null;

    pos += pattern.length;
    while (pos < end && buf[pos] == ' ')
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

  private static long extractLong(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
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

  // ==================== File navigation ====================

  private static long findArrayStart(MemorySegment segment, long fileSize) {
    // Bulk copy to avoid per-byte MemorySegment.get() (required for native-image)
    int len = (int) Math.min(fileSize, 4096);
    byte[] buf = new byte[len];
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, 0, buf, 0, len);
    for (int i = 0; i < len; i++) {
      if (buf[i] == '[')
        return i + 1;
    }
    return 0;
  }

  private static long alignToRecordBoundary(MemorySegment segment, long approx, long fileSize) {
    // Bulk copy to find the next record boundary
    int len = (int) Math.min(8192, fileSize - approx);
    if (len <= 0)
      return fileSize;
    byte[] buf = new byte[len];
    MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, approx, buf, 0, len);
    for (int i = 0; i < len; i++) {
      if (buf[i] == '{')
        return approx + i;
    }
    return fileSize;
  }

  // ==================== Result building ====================

  private static List<Item> buildResult(HashMap<String, long[]> groups, String groupField) {
    List<Item> results = new ArrayList<>(groups.size());
    QNm fieldQnm = new QNm(groupField);
    QNm countQnm = new QNm("count");

    for (Map.Entry<String, long[]> entry : groups.entrySet()) {
      QNm[] fields = { fieldQnm, countQnm };
      Sequence[] values = { new Str(entry.getKey()), new Int64(entry.getValue()[0]) };
      results.add(new CompactObject(fields, values));
    }
    return results;
  }

  // ==================== SIMD-accelerated byte search ====================

  /**
   * Find the next occurrence of {@code target} in buf[from..limit) using SIMD.
   * Processes BYTE_SPECIES.length() bytes per cycle (32 or 64 on modern CPUs).
   * Falls back to scalar scan when Vector API is unavailable.
   */
  private static int simdFind(byte[] buf, int from, int limit, byte target) {
    if (BYTE_SPECIES == null) {
      // Scalar fallback
      for (int i = from; i < limit; i++) {
        if (buf[i] == target)
          return i;
      }
      return limit;
    }

    int i = from;
    int vectorLen = BYTE_SPECIES.length();
    int vectorLimit = limit - vectorLen;
    ByteVector targetVec = ByteVector.broadcast(BYTE_SPECIES, target);

    // SIMD scan: compare SPECIES_LENGTH bytes at a time
    while (i <= vectorLimit) {
      ByteVector v = ByteVector.fromArray(BYTE_SPECIES, buf, i);
      VectorMask<Byte> mask = v.eq(targetVec);
      if (mask.anyTrue()) {
        return i + mask.firstTrue();
      }
      i += vectorLen;
    }

    // Scalar tail
    for (; i < limit; i++) {
      if (buf[i] == target)
        return i;
    }
    return limit;
  }
}
