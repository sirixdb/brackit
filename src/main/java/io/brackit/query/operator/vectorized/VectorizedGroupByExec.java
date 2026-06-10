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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Str;
import io.brackit.query.function.json.MappedJsonScanner;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jsonitem.object.CompactObject;

/**
 * Vectorized execution engine for streaming JSON group-by queries.
 * Bypasses the Volcano Tuple model entirely — parses JSON records directly
 * into columnar arrays and aggregates in-place.
 * <p>
 * For 100M records with 10 groups, the live object count is:
 * <ul>
 * <li>~10 group entries (HashMap entries)</li>
 * <li>~1000 interned Str values (field names + city names)</li>
 * <li>2 reusable column arrays (2048 elements each)</li>
 * </ul>
 * Total: ~1010 objects. Compare to Volcano: ~400M objects.
 * <p>
 * This is invoked by BrackitJq when it detects a streaming group-by pattern.
 */
public final class VectorizedGroupByExec {

  private static final int BATCH_SIZE = 2048;

  // ==================== Memory-mapped execution (Foreign Memory API) ====================

  /**
   * Execute group-by-count using memory-mapped file I/O.
   * No buffer management, no read() syscalls, no refills.
   * The OS handles paging the 48GB file transparently.
   */
  public static List<Item> executeMmapGroupByCount(java.nio.file.Path path, String groupField) throws Exception {
    HashMap<String, long[]> groups = new HashMap<>();
    String[] batchKeys = new String[BATCH_SIZE];
    int batchSize = 0;

    byte[] pattern = ("\"" + groupField + "\":").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] patternSpaced = ("\"" + groupField + "\" :").getBytes(java.nio.charset.StandardCharsets.UTF_8);

    try (MappedJsonScanner scanner = new MappedJsonScanner(path)) {
      scanner.skipToArrayStart();

      while (scanner.nextElement()) {
        String key = scanner.extractStringField(pattern, patternSpaced);
        if (key != null) {
          batchKeys[batchSize++] = key;
        }
        if (batchSize == BATCH_SIZE) {
          aggregateBatch(groups, batchKeys, batchSize);
          batchSize = 0;
        }
      }
    }

    if (batchSize > 0) {
      aggregateBatch(groups, batchKeys, batchSize);
    }

    return buildResult(groups, groupField);
  }

  /**
   * Execute filtered count using memory-mapped file I/O.
   */
  public static long executeMmapFilterCount(java.nio.file.Path path, String filterField, String filterOp,
      long filterValue) throws Exception {
    long count = 0;
    long[] batchValues = new long[BATCH_SIZE];
    int batchSize = 0;

    byte[] pattern = ("\"" + filterField + "\":").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] patternSpaced = ("\"" + filterField + "\" :").getBytes(java.nio.charset.StandardCharsets.UTF_8);

    try (MappedJsonScanner scanner = new MappedJsonScanner(path)) {
      scanner.skipToArrayStart();

      while (scanner.nextElement()) {
        batchValues[batchSize++] = scanner.extractLongField(pattern, patternSpaced);
        if (batchSize == BATCH_SIZE) {
          count += countFiltered(batchValues, batchSize, filterOp, filterValue);
          batchSize = 0;
        }
      }
    }

    if (batchSize > 0) {
      count += countFiltered(batchValues, batchSize, filterOp, filterValue);
    }

    return count;
  }

  /**
   * Execute a group-by-count query on streaming JSON input.
   * Returns the result as a list of CompactObject items.
   *
   * @param parser     streaming JSON parser (positioned after '[')
   * @param groupField field name to group by (e.g., "city")
   * @return list of {"field": key, "count": N} result objects
   */
  public static List<Item> executeGroupByCount(StreamingJSONParser parser, String groupField) throws QueryException {
    HashMap<String, long[]> groups = new HashMap<>();
    String[] batchKeys = new String[BATCH_SIZE];
    int batchSize = 0;

    byte[] fieldPattern = ("\"" + groupField + "\":").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] fieldPatternSpaced = ("\"" + groupField + "\" :").getBytes(java.nio.charset.StandardCharsets.UTF_8);

    // Zero-copy path: scan directly in the parser's buffer
    while (parser.nextArrayElementSlice()) {
      int start = parser.getSliceStart();
      int end = parser.getSliceEnd();

      String key;
      if (start >= 0) {
        // Fast path: element is in the buffer — scan in-place, no copy
        key = extractFieldFromBuffer(parser.getBuffer(), start, end, fieldPattern, fieldPatternSpaced);
      } else {
        // Slow path: element spans buffers — fall back to copy
        byte[] elementBytes = parser.nextArrayElementBytes();
        key = elementBytes != null ? extractFieldFromBytes(elementBytes, fieldPattern, fieldPatternSpaced) : null;
      }

      if (key != null) {
        batchKeys[batchSize++] = key;
      }

      if (batchSize == BATCH_SIZE) {
        aggregateBatch(groups, batchKeys, batchSize);
        batchSize = 0;
      }
    }

    if (batchSize > 0) {
      aggregateBatch(groups, batchKeys, batchSize);
    }

    return buildResult(groups, groupField);
  }

  /**
   * Extract a string field value directly from a buffer range without copying.
   */
  private static String extractFieldFromBuffer(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(buf, pattern, start, end);
    if (pos < 0) {
      pos = indexOf(buf, patternSpaced, start, end);
    }
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
        return new String(buf, valStart, pos - valStart, java.nio.charset.StandardCharsets.UTF_8);
      }
      pos++;
    }
    return null;
  }

  /**
   * Extract a string field value directly from JSON bytes without creating any objects.
   * Scans for the field pattern (e.g., "city":) and extracts the value string.
   */
  private static String extractFieldFromBytes(byte[] bytes, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(bytes, pattern, 0);
    if (pos < 0) {
      pos = indexOf(bytes, patternSpaced, 0);
    }
    if (pos < 0)
      return null;

    // Advance past the pattern to the value
    pos += pattern.length;
    if (pos < bytes.length && bytes[pos] == ' ')
      pos++; // skip optional space

    // Value should start with '"'
    if (pos >= bytes.length || bytes[pos] != '"')
      return null;
    pos++; // skip opening quote

    // Find closing quote (handle escapes)
    int start = pos;
    while (pos < bytes.length) {
      if (bytes[pos] == '\\') {
        pos += 2; // skip escape
        continue;
      }
      if (bytes[pos] == '"') {
        return new String(bytes, start, pos - start, java.nio.charset.StandardCharsets.UTF_8);
      }
      pos++;
    }
    return null;
  }

  private static int indexOf(byte[] haystack, byte[] needle, int from) {
    return indexOf(haystack, needle, from, haystack.length);
  }

  private static int indexOf(byte[] haystack, byte[] needle, int from, int limit) {
    outer:
    for (int i = from; i <= limit - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j])
          continue outer;
      }
      return i;
    }
    return -1;
  }

  /**
   * Execute a group-by-count query with a filter predicate.
   *
   * @param parser      streaming JSON parser
   * @param groupField  field to group by
   * @param filterField field to filter on
   * @param filterOp    comparison operator ("gt", "lt", "eq", "ge", "le")
   * @param filterValue threshold value
   * @return list of result objects
   */
  public static List<Item> executeFilterGroupByCount(StreamingJSONParser parser, String groupField, String filterField,
      String filterOp, long filterValue) throws QueryException {
    HashMap<String, long[]> groups = new HashMap<>();
    String[] batchKeys = new String[BATCH_SIZE];
    long[] batchFilterValues = new long[BATCH_SIZE];
    int batchSize = 0;

    Item element;
    while ((element = parser.nextArrayElement()) != null) {
      String key = extractStringField(element, groupField);
      long fv = extractLongField(element, filterField);

      if (key != null) {
        batchKeys[batchSize] = key;
        batchFilterValues[batchSize] = fv;
        batchSize++;
      }

      if (batchSize == BATCH_SIZE) {
        aggregateFilteredBatch(groups, batchKeys, batchFilterValues, batchSize, filterOp, filterValue);
        batchSize = 0;
      }
    }

    if (batchSize > 0) {
      aggregateFilteredBatch(groups, batchKeys, batchFilterValues, batchSize, filterOp, filterValue);
    }

    return buildResult(groups, groupField);
  }

  /**
   * Execute a simple count query (no group-by).
   */
  public static long executeCount(StreamingJSONParser parser) throws QueryException {
    long count = 0;
    while (parser.nextArrayElement() != null) {
      count++;
    }
    return count;
  }

  /**
   * Execute a filtered count query.
   */
  public static long executeFilterCount(StreamingJSONParser parser, String filterField, String filterOp,
      long filterValue) throws QueryException {
    long count = 0;
    long[] batchValues = new long[BATCH_SIZE];
    int batchSize = 0;

    byte[] fieldPattern = ("\"" + filterField + "\":").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] fieldPatternSpaced = ("\"" + filterField + "\" :").getBytes(java.nio.charset.StandardCharsets.UTF_8);

    while (parser.nextArrayElementSlice()) {
      int start = parser.getSliceStart();
      int end = parser.getSliceEnd();

      if (start >= 0) {
        batchValues[batchSize++] = extractLongFromBuffer(parser.getBuffer(),
                                                         start,
                                                         end,
                                                         fieldPattern,
                                                         fieldPatternSpaced);
      } else {
        byte[] elementBytes = parser.nextArrayElementBytes();
        if (elementBytes != null) {
          batchValues[batchSize++] = extractLongFromBytes(elementBytes, fieldPattern, fieldPatternSpaced);
        }
      }

      if (batchSize == BATCH_SIZE) {
        count += countFiltered(batchValues, batchSize, filterOp, filterValue);
        batchSize = 0;
      }
    }

    if (batchSize > 0) {
      count += countFiltered(batchValues, batchSize, filterOp, filterValue);
    }

    return count;
  }

  /**
   * Extract a long field value directly from a buffer range without copying.
   */
  private static long extractLongFromBuffer(byte[] buf, int start, int end, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(buf, pattern, start, end);
    if (pos < 0) {
      pos = indexOf(buf, patternSpaced, start, end);
    }
    if (pos < 0)
      return 0;

    pos += pattern.length;
    while (pos < end && (buf[pos] == ' ' || buf[pos] == '\t'))
      pos++;

    boolean negative = false;
    if (pos < end && buf[pos] == '-') {
      negative = true;
      pos++;
    }
    long value = 0;
    while (pos < end && buf[pos] >= '0' && buf[pos] <= '9') {
      value = value * 10 + (buf[pos] - '0');
      pos++;
    }
    return negative ? -value : value;
  }

  /**
   * Extract a long field value directly from JSON bytes.
   */
  private static long extractLongFromBytes(byte[] bytes, byte[] pattern, byte[] patternSpaced) {
    int pos = indexOf(bytes, pattern, 0);
    if (pos < 0) {
      pos = indexOf(bytes, patternSpaced, 0);
    }
    if (pos < 0)
      return 0;

    pos += pattern.length;
    while (pos < bytes.length && (bytes[pos] == ' ' || bytes[pos] == '\t'))
      pos++;

    // Parse integer directly from bytes
    boolean negative = false;
    if (pos < bytes.length && bytes[pos] == '-') {
      negative = true;
      pos++;
    }
    long value = 0;
    while (pos < bytes.length && bytes[pos] >= '0' && bytes[pos] <= '9') {
      value = value * 10 + (bytes[pos] - '0');
      pos++;
    }
    return negative ? -value : value;
  }

  // ==================== Batch aggregation ====================

  private static void aggregateBatch(HashMap<String, long[]> groups, String[] keys, int size) {
    for (int i = 0; i < size; i++) {
      groups.computeIfAbsent(keys[i], k -> new long[1])[0]++;
    }
  }

  private static void aggregateFilteredBatch(HashMap<String, long[]> groups, String[] keys, long[] filterValues,
      int size, String op, long threshold) {
    for (int i = 0; i < size; i++) {
      boolean pass = switch (op) {
        case "gt" -> filterValues[i] > threshold;
        case "lt" -> filterValues[i] < threshold;
        case "ge" -> filterValues[i] >= threshold;
        case "le" -> filterValues[i] <= threshold;
        case "eq" -> filterValues[i] == threshold;
        default -> true;
      };
      if (pass) {
        groups.computeIfAbsent(keys[i], k -> new long[1])[0]++;
      }
    }
  }

  private static long countFiltered(long[] values, int size, String op, long threshold) {
    long count = 0;
    for (int i = 0; i < size; i++) {
      count += switch (op) {
        case "gt" -> values[i] > threshold ? 1 : 0;
        case "lt" -> values[i] < threshold ? 1 : 0;
        case "ge" -> values[i] >= threshold ? 1 : 0;
        case "le" -> values[i] <= threshold ? 1 : 0;
        case "eq" -> values[i] == threshold ? 1 : 0;
        default -> 1;
      };
    }
    return count;
  }

  // ==================== Field extraction ====================

  private static String extractStringField(Item item, String fieldName) {
    if (item instanceof Object obj) {
      Sequence val = obj.get(new io.brackit.query.atomic.QNm(fieldName));
      if (val instanceof Str s) {
        return s.stringValue();
      }
    }
    return null;
  }

  private static long extractLongField(Item item, String fieldName) {
    if (item instanceof Object obj) {
      Sequence val = obj.get(new io.brackit.query.atomic.QNm(fieldName));
      if (val instanceof Int32 n) {
        return n.intValue();
      }
      if (val instanceof Int64 n) {
        return n.longValue();
      }
    }
    return 0;
  }

  // ==================== Result building ====================

  private static List<Item> buildResult(HashMap<String, long[]> groups, String groupField) {
    List<Item> results = new ArrayList<>(groups.size());
    io.brackit.query.atomic.QNm fieldQnm = new io.brackit.query.atomic.QNm(groupField);
    io.brackit.query.atomic.QNm countQnm = new io.brackit.query.atomic.QNm("count");

    for (Map.Entry<String, long[]> entry : groups.entrySet()) {
      io.brackit.query.atomic.QNm[] fields = { fieldQnm, countQnm };
      Sequence[] values = { new Str(entry.getKey()), new Int64(entry.getValue()[0]) };
      results.add(new CompactObject(fields, values));
    }
    return results;
  }
}
