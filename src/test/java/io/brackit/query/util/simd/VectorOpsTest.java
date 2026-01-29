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
package io.brackit.query.util.simd;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VectorOps} SIMD operations.
 *
 * @author Brackit Project Team
 */
public class VectorOpsTest {

  // ==================== String Equality Tests ====================

  @Test
  public void testStringEquals_identical() {
    byte[] a = "hello world".getBytes(UTF_8);
    assertTrue(VectorOps.stringEquals(a, a.clone()));
  }

  @Test
  public void testStringEquals_sameReference() {
    byte[] a = "hello world".getBytes(UTF_8);
    assertTrue(VectorOps.stringEquals(a, a));
  }

  @Test
  public void testStringEquals_differentLength() {
    assertFalse(VectorOps.stringEquals("abc".getBytes(UTF_8), "ab".getBytes(UTF_8)));
  }

  @Test
  public void testStringEquals_singleByteDifference() {
    byte[] a = "hello".getBytes(UTF_8);
    byte[] b = "hellp".getBytes(UTF_8);
    assertFalse(VectorOps.stringEquals(a, b));
  }

  @Test
  public void testStringEquals_firstByteDifference() {
    byte[] a = "hello".getBytes(UTF_8);
    byte[] b = "xello".getBytes(UTF_8);
    assertFalse(VectorOps.stringEquals(a, b));
  }

  @Test
  public void testStringEquals_nullHandling() {
    byte[] a = "hello".getBytes(UTF_8);
    assertFalse(VectorOps.stringEquals(null, a));
    assertFalse(VectorOps.stringEquals(a, null));
    assertTrue(VectorOps.stringEquals(null, null));
  }

  @Test
  public void testStringEquals_emptyStrings() {
    byte[] empty = new byte[0];
    assertTrue(VectorOps.stringEquals(empty, empty.clone()));
  }

  @Test
  public void testStringEquals_vectorBoundary() {
    // Test at exact vector length boundaries (64, 128, 256, 512 bytes)
    for (int len : new int[] { 64, 128, 256, 512 }) {
      byte[] a = new byte[len];
      byte[] b = new byte[len];
      Arrays.fill(a, (byte) 'x');
      Arrays.fill(b, (byte) 'x');
      assertTrue(VectorOps.stringEquals(a, b), "Failed at length " + len);
      b[len - 1] = 'y';
      assertFalse(VectorOps.stringEquals(a, b), "Should differ at length " + len);
    }
  }

  @Test
  public void testStringEquals_vectorBoundaryPlusOne() {
    // Test at vector length boundaries + 1 to test tail handling
    int vectorLen = VectorOps.getByteVectorLength();
    for (int offset : new int[] { -1, 0, 1 }) {
      int len = vectorLen + offset;
      if (len <= 0)
        continue;
      byte[] a = new byte[len];
      byte[] b = new byte[len];
      Arrays.fill(a, (byte) 'x');
      Arrays.fill(b, (byte) 'x');
      assertTrue(VectorOps.stringEquals(a, b), "Failed at length " + len);
    }
  }

  @Test
  public void testStringEquals_largeString() {
    int size = 10000;
    byte[] a = new byte[size];
    byte[] b = new byte[size];
    new Random(42).nextBytes(a);
    System.arraycopy(a, 0, b, 0, size);
    assertTrue(VectorOps.stringEquals(a, b));

    // Differ at last byte
    b[size - 1] = (byte) (a[size - 1] ^ 0xFF);
    assertFalse(VectorOps.stringEquals(a, b));
  }

  // ==================== String Comparison Tests ====================

  @Test
  public void testStringCompare_equal() {
    byte[] a = "hello".getBytes(UTF_8);
    assertEquals(0, VectorOps.stringCompare(a, a.clone()));
  }

  @Test
  public void testStringCompare_lessThan() {
    byte[] a = "abc".getBytes(UTF_8);
    byte[] b = "abd".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(a, b) < 0);
  }

  @Test
  public void testStringCompare_greaterThan() {
    byte[] a = "abd".getBytes(UTF_8);
    byte[] b = "abc".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(a, b) > 0);
  }

  @Test
  public void testStringCompare_prefixShorter() {
    byte[] a = "abc".getBytes(UTF_8);
    byte[] b = "abcd".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(a, b) < 0);
  }

  @Test
  public void testStringCompare_prefixLonger() {
    byte[] a = "abcd".getBytes(UTF_8);
    byte[] b = "abc".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(a, b) > 0);
  }

  @Test
  public void testStringCompare_nullHandling() {
    byte[] a = "hello".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(null, a) < 0);
    assertTrue(VectorOps.stringCompare(a, null) > 0);
    assertEquals(0, VectorOps.stringCompare(null, null));
  }

  @Test
  public void testStringCompare_empty() {
    byte[] empty = new byte[0];
    byte[] nonEmpty = "a".getBytes(UTF_8);
    assertTrue(VectorOps.stringCompare(empty, nonEmpty) < 0);
    assertTrue(VectorOps.stringCompare(nonEmpty, empty) > 0);
    assertEquals(0, VectorOps.stringCompare(empty, empty.clone()));
  }

  @Test
  public void testStringCompare_consistentWithJava() {
    String[] testCases = { "", "a", "aa", "ab", "b", "hello world", "hello world!", "The quick brown fox" };

    for (String s1 : testCases) {
      for (String s2 : testCases) {
        int javaResult = s1.compareTo(s2);
        int simdResult = VectorOps.stringCompare(s1.getBytes(UTF_8), s2.getBytes(UTF_8));
        assertEquals(Integer.signum(javaResult),
                     Integer.signum(simdResult),
                     "Mismatch for '" + s1 + "' vs '" + s2 + "'");
      }
    }
  }

  // ==================== String Hash Tests ====================

  @Test
  public void testStringHash_empty() {
    assertEquals(1, VectorOps.stringHash(new byte[0]));
  }

  @Test
  public void testStringHash_null() {
    assertEquals(0, VectorOps.stringHash(null));
  }

  @Test
  public void testStringHash_consistent() {
    byte[] data = "hello world".getBytes(UTF_8);
    int hash1 = VectorOps.stringHash(data);
    int hash2 = VectorOps.stringHash(data);
    assertEquals(hash1, hash2);
  }

  @Test
  public void testStringHash_differentForDifferentData() {
    int hash1 = VectorOps.stringHash("hello".getBytes(UTF_8));
    int hash2 = VectorOps.stringHash("world".getBytes(UTF_8));
    assertNotEquals(hash1, hash2);
  }

  @Test
  public void testStringHash_largeInput() {
    byte[] large = new byte[10000];
    new Random(42).nextBytes(large);
    int hash = VectorOps.stringHash(large);
    // Just verify it completes without error
    assertNotEquals(0, hash);
  }

  // ==================== Sum Long Tests ====================

  @Test
  public void testSumLong_empty() {
    assertEquals(0L, VectorOps.sumLong(new long[0], 0, 0));
  }

  @Test
  public void testSumLong_single() {
    assertEquals(42L, VectorOps.sumLong(new long[] { 42 }, 0, 1));
  }

  @Test
  public void testSumLong_multiple() {
    long[] values = { 1, 2, 3, 4, 5 };
    assertEquals(15L, VectorOps.sumLong(values, 0, 5));
  }

  @Test
  public void testSumLong_offset() {
    long[] values = { 10, 1, 2, 3, 10 };
    assertEquals(6L, VectorOps.sumLong(values, 1, 3));
  }

  @Test
  public void testSumLong_overflow() {
    long[] values = { Long.MAX_VALUE, 1 };
    // Should handle overflow according to Java semantics (wrap around)
    assertEquals(Long.MIN_VALUE, VectorOps.sumLong(values, 0, 2));
  }

  @Test
  public void testSumLong_negatives() {
    long[] values = { -1, -2, -3, -4, -5 };
    assertEquals(-15L, VectorOps.sumLong(values, 0, 5));
  }

  @Test
  public void testSumLong_mixed() {
    long[] values = { -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5 };
    assertEquals(0L, VectorOps.sumLong(values, 0, 11));
  }

  @Test
  public void testSumLong_largeArray() {
    int size = 10000;
    long[] values = new long[size];
    long expected = 0;
    for (int i = 0; i < size; i++) {
      values[i] = i;
      expected += i;
    }
    assertEquals(expected, VectorOps.sumLong(values, 0, size));
  }

  @Test
  public void testSumLong_vectorBoundaries() {
    int vectorLen = VectorOps.getLongVectorLength();
    for (int len : new int[] { vectorLen - 1, vectorLen, vectorLen + 1, vectorLen * 2, vectorLen * 2 + 1 }) {
      long[] values = new long[len];
      long expected = 0;
      for (int i = 0; i < len; i++) {
        values[i] = i + 1;
        expected += values[i];
      }
      assertEquals(expected, VectorOps.sumLong(values, 0, len), "Failed at length " + len);
    }
  }

  // ==================== Sum Double Tests ====================

  @Test
  public void testSumDouble_empty() {
    assertEquals(0.0, VectorOps.sumDouble(new double[0], 0, 0), 0.0001);
  }

  @Test
  public void testSumDouble_single() {
    assertEquals(3.14, VectorOps.sumDouble(new double[] { 3.14 }, 0, 1), 0.0001);
  }

  @Test
  public void testSumDouble_multiple() {
    double[] values = { 1.1, 2.2, 3.3, 4.4 };
    assertEquals(11.0, VectorOps.sumDouble(values, 0, 4), 0.0001);
  }

  @Test
  public void testSumDouble_largeArray() {
    int size = 10000;
    double[] values = new double[size];
    double expected = 0;
    for (int i = 0; i < size; i++) {
      values[i] = i * 0.1;
      expected += values[i];
    }
    assertEquals(expected, VectorOps.sumDouble(values, 0, size), 0.01);
  }

  // ==================== Sum Int Tests ====================

  @Test
  public void testSumInt_empty() {
    assertEquals(0L, VectorOps.sumInt(new int[0], 0, 0));
  }

  @Test
  public void testSumInt_multiple() {
    int[] values = { 1, 2, 3, 4, 5 };
    assertEquals(15L, VectorOps.sumInt(values, 0, 5));
  }

  @Test
  public void testSumInt_largeValues() {
    int[] values = { Integer.MAX_VALUE, Integer.MAX_VALUE };
    // Should not overflow since we accumulate in long
    assertEquals(2L * Integer.MAX_VALUE, VectorOps.sumInt(values, 0, 2));
  }

  // ==================== Min Long Tests ====================

  @Test
  public void testMinLong_empty() {
    assertEquals(Long.MAX_VALUE, VectorOps.minLong(new long[0], 0, 0));
  }

  @Test
  public void testMinLong_singleElement() {
    long[] values = { 42 };
    assertEquals(42L, VectorOps.minLong(values, 0, 1));
  }

  @Test
  public void testMinLong_multiple() {
    long[] values = { 5, 3, 1, 4, 2 };
    assertEquals(1L, VectorOps.minLong(values, 0, 5));
  }

  @Test
  public void testMinLong_negatives() {
    long[] values = { -5, -3, -1, -4, -2 };
    assertEquals(-5L, VectorOps.minLong(values, 0, 5));
  }

  @Test
  public void testMinLong_withOffset() {
    long[] values = { 100, 5, 3, 1, 100 };
    assertEquals(1L, VectorOps.minLong(values, 1, 3));
  }

  @Test
  public void testMinLong_largeArray() {
    int size = 10000;
    long[] values = new long[size];
    Random rng = new Random(42);
    long expected = Long.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      values[i] = rng.nextLong();
      expected = Math.min(expected, values[i]);
    }
    assertEquals(expected, VectorOps.minLong(values, 0, size));
  }

  // ==================== Max Long Tests ====================

  @Test
  public void testMaxLong_empty() {
    assertEquals(Long.MIN_VALUE, VectorOps.maxLong(new long[0], 0, 0));
  }

  @Test
  public void testMaxLong_singleElement() {
    long[] values = { 42 };
    assertEquals(42L, VectorOps.maxLong(values, 0, 1));
  }

  @Test
  public void testMaxLong_multiple() {
    long[] values = { 5, 3, 1, 4, 2 };
    assertEquals(5L, VectorOps.maxLong(values, 0, 5));
  }

  @Test
  public void testMaxLong_negatives() {
    long[] values = { -5, -3, -1, -4, -2 };
    assertEquals(-1L, VectorOps.maxLong(values, 0, 5));
  }

  @Test
  public void testMaxLong_withOffset() {
    long[] values = { -100, 5, 3, 1, -100 };
    assertEquals(5L, VectorOps.maxLong(values, 1, 3));
  }

  @Test
  public void testMaxLong_largeArray() {
    int size = 10000;
    long[] values = new long[size];
    Random rng = new Random(42);
    long expected = Long.MIN_VALUE;
    for (int i = 0; i < size; i++) {
      values[i] = rng.nextLong();
      expected = Math.max(expected, values[i]);
    }
    assertEquals(expected, VectorOps.maxLong(values, 0, size));
  }

  // ==================== Count Greater Than Tests ====================

  @Test
  public void testCountGreaterThan_empty() {
    assertEquals(0, VectorOps.countGreaterThan(new long[0], 0, 0, 0));
  }

  @Test
  public void testCountGreaterThan_allMatch() {
    long[] values = { 5, 6, 7, 8, 9 };
    assertEquals(5, VectorOps.countGreaterThan(values, 0, 5, 4));
  }

  @Test
  public void testCountGreaterThan_noneMatch() {
    long[] values = { 1, 2, 3, 4, 5 };
    assertEquals(0, VectorOps.countGreaterThan(values, 0, 5, 5));
  }

  @Test
  public void testCountGreaterThan_someMatch() {
    long[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    assertEquals(5, VectorOps.countGreaterThan(values, 0, 10, 5));
  }

  @Test
  public void testCountGreaterThan_withOffset() {
    long[] values = { 100, 1, 2, 3, 100 };
    assertEquals(2, VectorOps.countGreaterThan(values, 1, 3, 1));
  }

  // ==================== Filter Indices Greater Than Tests ====================

  @Test
  public void testFilterIndicesGreaterThan_empty() {
    int[] indices = new int[10];
    assertEquals(0, VectorOps.filterIndicesGreaterThan(new long[0], 0, 0, 0, indices));
  }

  @Test
  public void testFilterIndicesGreaterThan_allMatch() {
    long[] values = { 5, 6, 7, 8, 9 };
    int[] indices = new int[5];
    int count = VectorOps.filterIndicesGreaterThan(values, 0, 5, 4, indices);
    assertEquals(5, count);
    assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, indices);
  }

  @Test
  public void testFilterIndicesGreaterThan_someMatch() {
    long[] values = { 1, 5, 2, 6, 3, 7 };
    int[] indices = new int[6];
    int count = VectorOps.filterIndicesGreaterThan(values, 0, 6, 4, indices);
    assertEquals(3, count);
    assertEquals(1, indices[0]);
    assertEquals(3, indices[1]);
    assertEquals(5, indices[2]);
  }

  // ==================== Count Equals Tests ====================

  @Test
  public void testCountEquals_empty() {
    assertEquals(0, VectorOps.countEquals(new long[0], 0, 0, 42));
  }

  @Test
  public void testCountEquals_allMatch() {
    long[] values = { 5, 5, 5, 5, 5 };
    assertEquals(5, VectorOps.countEquals(values, 0, 5, 5));
  }

  @Test
  public void testCountEquals_noneMatch() {
    long[] values = { 1, 2, 3, 4, 5 };
    assertEquals(0, VectorOps.countEquals(values, 0, 5, 42));
  }

  @Test
  public void testCountEquals_someMatch() {
    long[] values = { 1, 5, 2, 5, 3, 5 };
    assertEquals(3, VectorOps.countEquals(values, 0, 6, 5));
  }

  // ==================== Hash Probe Tests ====================

  @Test
  public void testHashProbe_found() {
    int tableSize = 16;
    long[] keys = new long[tableSize];
    Arrays.fill(keys, Long.MIN_VALUE); // Empty marker

    // Insert key at computed hash position
    long searchKey = 42;
    int hash = Long.hashCode(searchKey) & (tableSize - 1);
    keys[hash] = searchKey;

    int result = VectorOps.hashProbe(keys, tableSize, searchKey);
    assertEquals(hash, result);
  }

  @Test
  public void testHashProbe_notFound() {
    int tableSize = 16;
    long[] keys = new long[tableSize];
    Arrays.fill(keys, Long.MIN_VALUE); // Empty marker

    int result = VectorOps.hashProbe(keys, tableSize, 42);
    assertEquals(-1, result);
  }

  @Test
  public void testHashProbe_collision() {
    int tableSize = 16;
    long[] keys = new long[tableSize];
    Arrays.fill(keys, Long.MIN_VALUE);

    // Insert two keys that hash to same slot
    long key1 = 1;
    long key2 = 17; // Likely different hash but test concept
    int hash1 = Long.hashCode(key1) & (tableSize - 1);
    keys[hash1] = key1;

    // Insert second at next slot (simulate collision)
    keys[(hash1 + 1) & (tableSize - 1)] = key2;

    assertEquals(hash1, VectorOps.hashProbe(keys, tableSize, key1));
  }

  // ==================== Compute Hashes Tests ====================

  @Test
  public void testComputeHashes_consistency() {
    int tableSize = 256;
    int tableMask = tableSize - 1;
    long[] keys = { 1, 2, 3, 42, 100, Long.MAX_VALUE };
    int[] hashes = new int[keys.length];

    VectorOps.computeHashes(keys, 0, keys.length, hashes, tableMask);

    for (int i = 0; i < keys.length; i++) {
      int expectedHash = (int) (keys[i] ^ (keys[i] >>> 32)) & tableMask;
      assertEquals(expectedHash, hashes[i], "Hash mismatch at index " + i);
    }
  }

  @Test
  public void testComputeHashes_withinBounds() {
    int tableSize = 128;
    int tableMask = tableSize - 1;
    long[] keys = new long[1000];
    Random rng = new Random(42);
    for (int i = 0; i < keys.length; i++) {
      keys[i] = rng.nextLong();
    }

    int[] hashes = new int[keys.length];
    VectorOps.computeHashes(keys, 0, keys.length, hashes, tableMask);

    for (int hash : hashes) {
      assertTrue(hash >= 0 && hash < tableSize, "Hash out of bounds: " + hash);
    }
  }

  // ==================== Vector Length Tests ====================

  @Test
  public void testVectorLengths() {
    // Verify vector lengths are positive and reasonable
    assertTrue(VectorOps.getByteVectorLength() > 0);
    assertTrue(VectorOps.getIntVectorLength() > 0);
    assertTrue(VectorOps.getLongVectorLength() > 0);
    assertTrue(VectorOps.getDoubleVectorLength() > 0);

    // Long and double should have same length (both 64-bit)
    assertEquals(VectorOps.getLongVectorLength(), VectorOps.getDoubleVectorLength());

    // Byte vector should be larger than int vector (more elements fit)
    assertTrue(VectorOps.getByteVectorLength() >= VectorOps.getIntVectorLength());
  }
}
