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

import java.util.Arrays;

import jdk.incubator.vector.*;

/**
 * SIMD-accelerated operations using Java's Vector API.
 * Provides vectorized implementations for common query operations.
 * Falls back to scalar implementations when Vector API is unavailable
 * (e.g., in GraalVM native image).
 *
 * @author Brackit Project Team
 */
public final class VectorOps {

  private static final boolean VECTOR_AVAILABLE;

  // Preferred vector species for different element types (may be null in native image)
  private static final VectorSpecies<Byte> BYTE_SPECIES;
  private static final VectorSpecies<Integer> INT_SPECIES;
  private static final VectorSpecies<Long> LONG_SPECIES;
  private static final VectorSpecies<Double> DOUBLE_SPECIES;

  static {
    boolean available;
    VectorSpecies<Byte> byteSpecies = null;
    VectorSpecies<Integer> intSpecies = null;
    VectorSpecies<Long> longSpecies = null;
    VectorSpecies<Double> doubleSpecies = null;
    try {
      byteSpecies = ByteVector.SPECIES_PREFERRED;
      intSpecies = IntVector.SPECIES_PREFERRED;
      longSpecies = LongVector.SPECIES_PREFERRED;
      doubleSpecies = DoubleVector.SPECIES_PREFERRED;
      // Force initialization to detect native-image issues early
      byteSpecies.length();
      available = true;
    } catch (Throwable t) {
      available = false;
    }
    VECTOR_AVAILABLE = available;
    BYTE_SPECIES = byteSpecies;
    INT_SPECIES = intSpecies;
    LONG_SPECIES = longSpecies;
    DOUBLE_SPECIES = doubleSpecies;
  }

  private VectorOps() {
    // Utility class
  }

  // ==================== String Operations ====================

  /**
   * Vectorized string equality check.
   * Compares two byte arrays (UTF-8 encoded strings) using SIMD.
   */
  public static boolean stringEquals(byte[] a, byte[] b) {
    if (a == b)
      return true;
    if (a == null || b == null)
      return false;
    if (!VECTOR_AVAILABLE) {
      return Arrays.equals(a, b);
    }
    if (a.length != b.length)
      return false;

    int i = 0;
    int length = a.length;

    // Process in vector-sized chunks
    int vectorLength = BYTE_SPECIES.length();
    int limit = length - (length % vectorLength);

    for (; i < limit; i += vectorLength) {
      ByteVector va = ByteVector.fromArray(BYTE_SPECIES, a, i);
      ByteVector vb = ByteVector.fromArray(BYTE_SPECIES, b, i);
      if (!va.eq(vb).allTrue()) {
        return false;
      }
    }

    // Handle remaining bytes
    for (; i < length; i++) {
      if (a[i] != b[i])
        return false;
    }

    return true;
  }

  /**
   * Vectorized string comparison (lexicographic).
   * Returns negative if a < b, 0 if equal, positive if a > b.
   */
  public static int stringCompare(byte[] a, byte[] b) {
    if (a == b)
      return 0;
    if (a == null)
      return -1;
    if (b == null)
      return 1;
    if (!VECTOR_AVAILABLE) {
      return Arrays.compareUnsigned(a, b);
    }

    int minLength = Math.min(a.length, b.length);
    int i = 0;

    // Process in vector-sized chunks to find first difference
    int vectorLength = BYTE_SPECIES.length();
    int limit = minLength - (minLength % vectorLength);

    for (; i < limit; i += vectorLength) {
      ByteVector va = ByteVector.fromArray(BYTE_SPECIES, a, i);
      ByteVector vb = ByteVector.fromArray(BYTE_SPECIES, b, i);
      VectorMask<Byte> neq = va.compare(VectorOperators.NE, vb);
      if (neq.anyTrue()) {
        // Find first differing byte
        int firstDiff = neq.firstTrue();
        return Byte.compareUnsigned(a[i + firstDiff], b[i + firstDiff]);
      }
    }

    // Handle remaining bytes
    for (; i < minLength; i++) {
      int cmp = Byte.compareUnsigned(a[i], b[i]);
      if (cmp != 0)
        return cmp;
    }

    return a.length - b.length;
  }

  /**
   * Vectorized hash code computation for byte arrays.
   * Uses a polynomial rolling hash with SIMD acceleration.
   */
  public static int stringHash(byte[] bytes) {
    if (bytes == null)
      return 0;
    if (bytes.length == 0)
      return 1;

    int hash = 0;
    int i = 0;
    int length = bytes.length;

    // For small arrays or when Vector API is unavailable, use scalar code
    if (!VECTOR_AVAILABLE || length < INT_SPECIES.length() * 4) {
      for (byte b : bytes) {
        hash = 31 * hash + (b & 0xff);
      }
      return hash;
    }

    // Process in chunks using vectorization
    int vectorLength = INT_SPECIES.length();
    int limit = length - (length % vectorLength);

    // Initialize multiplier vector [31^(n-1), 31^(n-2), ..., 31, 1]
    int[] multipliers = new int[vectorLength];
    int mult = 1;
    for (int j = vectorLength - 1; j >= 0; j--) {
      multipliers[j] = mult;
      mult *= 31;
    }
    IntVector multVector = IntVector.fromArray(INT_SPECIES, multipliers, 0);
    int chunkMultiplier = mult; // 31^vectorLength

    for (; i < limit; i += vectorLength) {
      // Load bytes and convert to ints
      int[] ints = new int[vectorLength];
      for (int j = 0; j < vectorLength; j++) {
        ints[j] = bytes[i + j] & 0xff;
      }
      IntVector values = IntVector.fromArray(INT_SPECIES, ints, 0);

      // Multiply by position weights and sum
      IntVector weighted = values.mul(multVector);
      int chunkHash = weighted.reduceLanes(VectorOperators.ADD);

      hash = hash * chunkMultiplier + chunkHash;
    }

    // Handle remaining bytes
    for (; i < length; i++) {
      hash = 31 * hash + (bytes[i] & 0xff);
    }

    return hash;
  }

  // ==================== Numeric Aggregations ====================

  /**
   * Vectorized sum of long array.
   * Uses 4x loop unrolling for better instruction-level parallelism.
   */
  public static long sumLong(long[] values, int offset, int length) {
    long sum = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        sum += values[offset + i];
      return sum;
    }

    int vectorLength = LONG_SPECIES.length();

    // Unrolled loop with 4 independent accumulators
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      LongVector sum0 = LongVector.zero(LONG_SPECIES);
      LongVector sum1 = LongVector.zero(LONG_SPECIES);
      LongVector sum2 = LongVector.zero(LONG_SPECIES);
      LongVector sum3 = LongVector.zero(LONG_SPECIES);

      for (; i < limit4; i += step) {
        sum0 = sum0.add(LongVector.fromArray(LONG_SPECIES, values, offset + i));
        sum1 = sum1.add(LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength));
        sum2 = sum2.add(LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 2));
        sum3 = sum3.add(LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 3));
      }
      sum = sum0.add(sum1).add(sum2).add(sum3).reduceLanes(VectorOperators.ADD);
    }

    // Handle remaining full vectors
    int limit = length - (length % vectorLength);
    for (; i < limit; i += vectorLength) {
      LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
      sum += v.reduceLanes(VectorOperators.ADD);
    }

    // Handle remaining elements
    for (; i < length; i++) {
      sum += values[offset + i];
    }

    return sum;
  }

  /**
   * Vectorized sum of double array.
   * Uses 4x loop unrolling for better instruction-level parallelism.
   */
  public static double sumDouble(double[] values, int offset, int length) {
    double sum = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        sum += values[offset + i];
      return sum;
    }

    int vectorLength = DOUBLE_SPECIES.length();

    // Unrolled loop with 4 independent accumulators
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      DoubleVector sum0 = DoubleVector.zero(DOUBLE_SPECIES);
      DoubleVector sum1 = DoubleVector.zero(DOUBLE_SPECIES);
      DoubleVector sum2 = DoubleVector.zero(DOUBLE_SPECIES);
      DoubleVector sum3 = DoubleVector.zero(DOUBLE_SPECIES);

      for (; i < limit4; i += step) {
        sum0 = sum0.add(DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i));
        sum1 = sum1.add(DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength));
        sum2 = sum2.add(DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 2));
        sum3 = sum3.add(DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 3));
      }
      sum = sum0.add(sum1).add(sum2).add(sum3).reduceLanes(VectorOperators.ADD);
    }

    // Handle remaining full vectors
    int limit = length - (length % vectorLength);
    for (; i < limit; i += vectorLength) {
      DoubleVector v = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i);
      sum += v.reduceLanes(VectorOperators.ADD);
    }

    // Handle remaining elements
    for (; i < length; i++) {
      sum += values[offset + i];
    }

    return sum;
  }

  /**
   * Vectorized sum of int array.
   */
  public static long sumInt(int[] values, int offset, int length) {
    long sum = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        sum += values[offset + i];
      return sum;
    }

    int vectorLength = INT_SPECIES.length();
    int limit = length - (length % vectorLength);

    // Use long accumulator to avoid overflow
    LongVector sumVector = LongVector.zero(LONG_SPECIES);
    int longVectorLength = LONG_SPECIES.length();

    for (; i < limit; i += vectorLength) {
      IntVector v = IntVector.fromArray(INT_SPECIES, values, offset + i);
      // Convert to longs in chunks
      for (int j = 0; j < vectorLength; j += longVectorLength) {
        long[] longs = new long[longVectorLength];
        for (int k = 0; k < longVectorLength && j + k < vectorLength; k++) {
          longs[k] = values[offset + i + j + k];
        }
        sumVector = sumVector.add(LongVector.fromArray(LONG_SPECIES, longs, 0));
      }
    }
    sum = sumVector.reduceLanes(VectorOperators.ADD);

    // Handle remaining elements
    for (; i < length; i++) {
      sum += values[offset + i];
    }

    return sum;
  }

  /**
   * Vectorized minimum of long array.
   * Uses compare+blend approach with 4x loop unrolling for better performance.
   * Note: .min() method is significantly slower on most JVM implementations.
   */
  public static long minLong(long[] values, int offset, int length) {
    if (length == 0)
      return Long.MAX_VALUE;

    long min = values[offset];
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (i = 1; i < length; i++)
        min = Math.min(min, values[offset + i]);
      return min;
    }

    int vectorLength = LONG_SPECIES.length();

    // Unrolled loop with 4 independent accumulators
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      LongVector min0 = LongVector.broadcast(LONG_SPECIES, Long.MAX_VALUE);
      LongVector min1 = LongVector.broadcast(LONG_SPECIES, Long.MAX_VALUE);
      LongVector min2 = LongVector.broadcast(LONG_SPECIES, Long.MAX_VALUE);
      LongVector min3 = LongVector.broadcast(LONG_SPECIES, Long.MAX_VALUE);

      for (; i < limit4; i += step) {
        LongVector v0 = LongVector.fromArray(LONG_SPECIES, values, offset + i);
        LongVector v1 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength);
        LongVector v2 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 2);
        LongVector v3 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 3);

        // Use compare+blend instead of .min() - significantly faster
        min0 = min0.blend(v0, v0.lt(min0));
        min1 = min1.blend(v1, v1.lt(min1));
        min2 = min2.blend(v2, v2.lt(min2));
        min3 = min3.blend(v3, v3.lt(min3));
      }

      // Combine accumulators using compare+blend
      LongVector result = min0.blend(min1, min1.lt(min0));
      result = result.blend(min2, min2.lt(result));
      result = result.blend(min3, min3.lt(result));
      min = result.reduceLanes(VectorOperators.MIN);
    }

    // Handle remaining full vectors
    int limit = length - (length % vectorLength);
    if (i < limit) {
      LongVector minVector = LongVector.broadcast(LONG_SPECIES, min);
      for (; i < limit; i += vectorLength) {
        LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
        minVector = minVector.blend(v, v.lt(minVector));
      }
      min = minVector.reduceLanes(VectorOperators.MIN);
    }

    // Handle remaining elements
    for (; i < length; i++) {
      if (values[offset + i] < min) {
        min = values[offset + i];
      }
    }

    return min;
  }

  /**
   * Vectorized maximum of long array.
   * Uses compare+blend approach with 4x loop unrolling for better performance.
   * Note: .max() method is significantly slower on most JVM implementations.
   */
  public static long maxLong(long[] values, int offset, int length) {
    if (length == 0)
      return Long.MIN_VALUE;

    long max = values[offset];
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (i = 1; i < length; i++)
        max = Math.max(max, values[offset + i]);
      return max;
    }

    int vectorLength = LONG_SPECIES.length();

    // Unrolled loop with 4 independent accumulators
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      LongVector max0 = LongVector.broadcast(LONG_SPECIES, Long.MIN_VALUE);
      LongVector max1 = LongVector.broadcast(LONG_SPECIES, Long.MIN_VALUE);
      LongVector max2 = LongVector.broadcast(LONG_SPECIES, Long.MIN_VALUE);
      LongVector max3 = LongVector.broadcast(LONG_SPECIES, Long.MIN_VALUE);

      for (; i < limit4; i += step) {
        LongVector v0 = LongVector.fromArray(LONG_SPECIES, values, offset + i);
        LongVector v1 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength);
        LongVector v2 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 2);
        LongVector v3 = LongVector.fromArray(LONG_SPECIES, values, offset + i + vectorLength * 3);

        // Use compare+blend instead of .max() - significantly faster
        max0 = max0.blend(v0, v0.compare(VectorOperators.GT, max0));
        max1 = max1.blend(v1, v1.compare(VectorOperators.GT, max1));
        max2 = max2.blend(v2, v2.compare(VectorOperators.GT, max2));
        max3 = max3.blend(v3, v3.compare(VectorOperators.GT, max3));
      }

      // Combine accumulators using compare+blend
      LongVector result = max0.blend(max1, max1.compare(VectorOperators.GT, max0));
      result = result.blend(max2, max2.compare(VectorOperators.GT, result));
      result = result.blend(max3, max3.compare(VectorOperators.GT, result));
      max = result.reduceLanes(VectorOperators.MAX);
    }

    // Handle remaining full vectors
    int limit = length - (length % vectorLength);
    if (i < limit) {
      LongVector maxVector = LongVector.broadcast(LONG_SPECIES, max);
      for (; i < limit; i += vectorLength) {
        LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
        maxVector = maxVector.blend(v, v.compare(VectorOperators.GT, maxVector));
      }
      max = maxVector.reduceLanes(VectorOperators.MAX);
    }

    // Handle remaining elements
    for (; i < length; i++) {
      if (values[offset + i] > max) {
        max = values[offset + i];
      }
    }

    return max;
  }

  // ==================== Filtering Operations ====================

  /**
   * Vectorized filtering: count elements matching predicate (greater than threshold).
   */
  public static int countGreaterThan(long[] values, int offset, int length, long threshold) {
    int count = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        if (values[offset + i] > threshold)
          count++;
      return count;
    }

    int vectorLength = LONG_SPECIES.length();
    int limit = length - (length % vectorLength);

    LongVector thresholdVector = LongVector.broadcast(LONG_SPECIES, threshold);

    for (; i < limit; i += vectorLength) {
      LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
      VectorMask<Long> mask = v.compare(VectorOperators.GT, thresholdVector);
      count += mask.trueCount();
    }

    // Handle remaining elements
    for (; i < length; i++) {
      if (values[offset + i] > threshold) {
        count++;
      }
    }

    return count;
  }

  /**
   * Vectorized filtering: extract indices of elements matching predicate.
   * Returns the number of matching elements written to indices array.
   */
  public static int filterIndicesGreaterThan(long[] values, int offset, int length, long threshold, int[] indices) {
    int count = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        if (values[offset + i] > threshold)
          indices[count++] = offset + i;
      return count;
    }

    int vectorLength = LONG_SPECIES.length();
    int limit = length - (length % vectorLength);

    LongVector thresholdVector = LongVector.broadcast(LONG_SPECIES, threshold);

    for (; i < limit; i += vectorLength) {
      LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
      VectorMask<Long> mask = v.compare(VectorOperators.GT, thresholdVector);

      // Extract matching indices
      if (mask.anyTrue()) {
        for (int j = 0; j < vectorLength; j++) {
          if (mask.laneIsSet(j)) {
            indices[count++] = offset + i + j;
          }
        }
      }
    }

    // Handle remaining elements
    for (; i < length; i++) {
      if (values[offset + i] > threshold) {
        indices[count++] = offset + i;
      }
    }

    return count;
  }

  /**
   * Vectorized equality check: count elements equal to target.
   */
  public static int countEquals(long[] values, int offset, int length, long target) {
    int count = 0;
    int i = 0;

    if (!VECTOR_AVAILABLE) {
      for (; i < length; i++)
        if (values[offset + i] == target)
          count++;
      return count;
    }

    int vectorLength = LONG_SPECIES.length();
    int limit = length - (length % vectorLength);

    LongVector targetVector = LongVector.broadcast(LONG_SPECIES, target);

    for (; i < limit; i += vectorLength) {
      LongVector v = LongVector.fromArray(LONG_SPECIES, values, offset + i);
      VectorMask<Long> mask = v.eq(targetVector);
      count += mask.trueCount();
    }

    // Handle remaining elements
    for (; i < length; i++) {
      if (values[offset + i] == target) {
        count++;
      }
    }

    return count;
  }

  // ==================== Join Operations ====================

  /**
   * Vectorized hash probe: find index of key in hash table.
   * Returns -1 if not found.
   */
  public static int hashProbe(long[] keys, int tableSize, long searchKey) {
    int hash = Long.hashCode(searchKey) & (tableSize - 1);

    if (!VECTOR_AVAILABLE) {
      for (int probes = 0; probes < tableSize; probes++) {
        int idx = (hash + probes) & (tableSize - 1);
        if (keys[idx] == searchKey)
          return idx;
        if (keys[idx] == Long.MIN_VALUE)
          return -1;
      }
      return -1;
    }

    // Linear probing with vectorized comparison
    int vectorLength = LONG_SPECIES.length();
    LongVector searchVector = LongVector.broadcast(LONG_SPECIES, searchKey);
    LongVector emptyVector = LongVector.broadcast(LONG_SPECIES, Long.MIN_VALUE);

    int probeStart = hash;
    for (int probes = 0; probes < tableSize; probes += vectorLength) {
      int idx = (probeStart + probes) & (tableSize - 1);

      // Handle wrap-around
      if (idx + vectorLength <= tableSize) {
        LongVector v = LongVector.fromArray(LONG_SPECIES, keys, idx);

        // Check for match
        VectorMask<Long> matchMask = v.eq(searchVector);
        if (matchMask.anyTrue()) {
          return idx + matchMask.firstTrue();
        }

        // Check for empty slot (search failed)
        VectorMask<Long> emptyMask = v.eq(emptyVector);
        if (emptyMask.anyTrue()) {
          return -1;
        }
      } else {
        // Scalar fallback near end of array
        for (int j = 0; j < vectorLength && probes + j < tableSize; j++) {
          int k = (probeStart + probes + j) & (tableSize - 1);
          if (keys[k] == searchKey)
            return k;
          if (keys[k] == Long.MIN_VALUE)
            return -1;
        }
      }
    }

    return -1;
  }

  /**
   * Vectorized batch hash computation for join keys.
   */
  public static void computeHashes(long[] keys, int offset, int length, int[] hashes, int tableSizeMask) {
    // Use scalar implementation for simplicity and correctness
    // The hash computation is simple enough that SIMD overhead isn't worth it
    for (int i = 0; i < length; i++) {
      long key = keys[offset + i];
      hashes[i] = (int) (key ^ (key >>> 32)) & tableSizeMask;
    }
  }

  // ==================== Branchless Filtering ====================
  // NOTE: Each method inlines the comparison directly rather than delegating
  // through a lambda. This is intentional -- the JIT can only auto-vectorize
  // tight primitive loops when the comparison is inlined. A lambda indirection
  // prevents the branchless optimization that is the purpose of these methods.

  /**
   * Branchless filter: select indices where values[i] == target.
   * Uses the pattern: sel[count] = i; count += (cond ? 1 : 0)
   * This eliminates branch mispredictions since the write always happens
   * and only the counter conditionally increments.
   */
  @SuppressWarnings("DuplicatedCode")
  public static int filterEqLong(long[] values, int offset, int length, long target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] == target) ? 1 : 0;
    }
    return count;
  }

  @SuppressWarnings("DuplicatedCode")
  public static int filterLtLong(long[] values, int offset, int length, long target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] < target) ? 1 : 0;
    }
    return count;
  }

  @SuppressWarnings("DuplicatedCode")
  public static int filterLeLong(long[] values, int offset, int length, long target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] <= target) ? 1 : 0;
    }
    return count;
  }

  @SuppressWarnings("DuplicatedCode")
  public static int filterGeLong(long[] values, int offset, int length, long target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] >= target) ? 1 : 0;
    }
    return count;
  }

  @SuppressWarnings("DuplicatedCode")
  public static int filterGtDouble(double[] values, int offset, int length, double target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] > target) ? 1 : 0;
    }
    return count;
  }

  @SuppressWarnings("DuplicatedCode")
  public static int filterLtDouble(double[] values, int offset, int length, double target, int[] selected) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[offset + i] < target) ? 1 : 0;
    }
    return count;
  }

  // ==================== Selection-Aware Aggregation ====================

  /**
   * Sum only the selected indices from a long array.
   * Operates on a pre-computed selection vector for post-filter aggregation.
   */
  public static long sumLongSelected(long[] values, int[] selected, int count) {
    long sum = 0;
    for (int i = 0; i < count; i++) {
      sum += values[selected[i]];
    }
    return sum;
  }

  /**
   * Sum only the selected indices from a double array.
   */
  public static double sumDoubleSelected(double[] values, int[] selected, int count) {
    double sum = 0.0;
    for (int i = 0; i < count; i++) {
      sum += values[selected[i]];
    }
    return sum;
  }

  /**
   * Min over selected indices from a long array.
   */
  public static long minLongSelected(long[] values, int[] selected, int count) {
    if (count == 0) {
      return Long.MAX_VALUE;
    }
    long min = values[selected[0]];
    for (int i = 1; i < count; i++) {
      long v = values[selected[i]];
      min = Math.min(min, v);
    }
    return min;
  }

  /**
   * Max over selected indices from a long array.
   */
  public static long maxLongSelected(long[] values, int[] selected, int count) {
    if (count == 0) {
      return Long.MIN_VALUE;
    }
    long max = values[selected[0]];
    for (int i = 1; i < count; i++) {
      long v = values[selected[i]];
      max = Math.max(max, v);
    }
    return max;
  }

  // ==================== Branchless Min/Max for Doubles ====================
  // NOTE: minDouble and maxDouble are kept as separate inlined implementations
  // rather than sharing a reduceDouble(boolean findMin) helper. The boolean flag
  // would add a branch inside every SIMD iteration and scalar tail, preventing
  // the JIT from producing optimal compare+blend sequences.

  /**
   * Vectorized minimum of double array.
   * Uses compare+blend with 4x loop unrolling.
   */
  @SuppressWarnings("DuplicatedCode")
  public static double minDouble(double[] values, int offset, int length) {
    if (length == 0) {
      return Double.POSITIVE_INFINITY;
    }

    if (!VECTOR_AVAILABLE) {
      double min = values[offset];
      for (int j = 1; j < length; j++)
        min = Math.min(min, values[offset + j]);
      return min;
    }

    int i = 0;
    double result;
    int vectorLength = DOUBLE_SPECIES.length();
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      DoubleVector acc0 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.POSITIVE_INFINITY);
      DoubleVector acc1 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.POSITIVE_INFINITY);
      DoubleVector acc2 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.POSITIVE_INFINITY);
      DoubleVector acc3 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.POSITIVE_INFINITY);

      for (; i < limit4; i += step) {
        DoubleVector v0 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i);
        DoubleVector v1 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength);
        DoubleVector v2 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 2);
        DoubleVector v3 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 3);

        acc0 = acc0.blend(v0, v0.lt(acc0));
        acc1 = acc1.blend(v1, v1.lt(acc1));
        acc2 = acc2.blend(v2, v2.lt(acc2));
        acc3 = acc3.blend(v3, v3.lt(acc3));
      }

      DoubleVector combined = acc0.blend(acc1, acc1.lt(acc0));
      combined = combined.blend(acc2, acc2.lt(combined));
      combined = combined.blend(acc3, acc3.lt(combined));
      result = combined.reduceLanes(VectorOperators.MIN);
    } else {
      result = values[offset];
    }

    int limit = length - (length % vectorLength);
    for (; i < limit; i += vectorLength) {
      DoubleVector v = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i);
      DoubleVector accVec = DoubleVector.broadcast(DOUBLE_SPECIES, result);
      accVec = accVec.blend(v, v.lt(accVec));
      result = accVec.reduceLanes(VectorOperators.MIN);
    }

    for (; i < length; i++) {
      if (values[offset + i] < result) {
        result = values[offset + i];
      }
    }

    return result;
  }

  /**
   * Vectorized maximum of double array.
   * Uses compare+blend with 4x loop unrolling.
   */
  @SuppressWarnings("DuplicatedCode")
  public static double maxDouble(double[] values, int offset, int length) {
    if (length == 0) {
      return Double.NEGATIVE_INFINITY;
    }

    if (!VECTOR_AVAILABLE) {
      double max = values[offset];
      for (int j = 1; j < length; j++)
        max = Math.max(max, values[offset + j]);
      return max;
    }

    int i = 0;
    double result;
    int vectorLength = DOUBLE_SPECIES.length();
    int step = vectorLength * 4;
    int limit4 = length - (length % step);

    if (limit4 > 0) {
      DoubleVector acc0 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.NEGATIVE_INFINITY);
      DoubleVector acc1 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.NEGATIVE_INFINITY);
      DoubleVector acc2 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.NEGATIVE_INFINITY);
      DoubleVector acc3 = DoubleVector.broadcast(DOUBLE_SPECIES, Double.NEGATIVE_INFINITY);

      for (; i < limit4; i += step) {
        DoubleVector v0 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i);
        DoubleVector v1 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength);
        DoubleVector v2 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 2);
        DoubleVector v3 = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i + vectorLength * 3);

        acc0 = acc0.blend(v0, v0.compare(VectorOperators.GT, acc0));
        acc1 = acc1.blend(v1, v1.compare(VectorOperators.GT, acc1));
        acc2 = acc2.blend(v2, v2.compare(VectorOperators.GT, acc2));
        acc3 = acc3.blend(v3, v3.compare(VectorOperators.GT, acc3));
      }

      DoubleVector combined = acc0.blend(acc1, acc1.compare(VectorOperators.GT, acc0));
      combined = combined.blend(acc2, acc2.compare(VectorOperators.GT, combined));
      combined = combined.blend(acc3, acc3.compare(VectorOperators.GT, combined));
      result = combined.reduceLanes(VectorOperators.MAX);
    } else {
      result = values[offset];
    }

    int limit = length - (length % vectorLength);
    for (; i < limit; i += vectorLength) {
      DoubleVector v = DoubleVector.fromArray(DOUBLE_SPECIES, values, offset + i);
      DoubleVector accVec = DoubleVector.broadcast(DOUBLE_SPECIES, result);
      accVec = accVec.blend(v, v.compare(VectorOperators.GT, accVec));
      result = accVec.reduceLanes(VectorOperators.MAX);
    }

    for (; i < length; i++) {
      if (values[offset + i] > result) {
        result = values[offset + i];
      }
    }

    return result;
  }

  // ==================== Utility Methods ====================

  /**
   * Get the preferred vector length for byte operations.
   */
  public static int getByteVectorLength() {
    return BYTE_SPECIES.length();
  }

  /**
   * Get the preferred vector length for int operations.
   */
  public static int getIntVectorLength() {
    return INT_SPECIES.length();
  }

  /**
   * Get the preferred vector length for long operations.
   */
  public static int getLongVectorLength() {
    return LONG_SPECIES.length();
  }

  /**
   * Get the preferred vector length for double operations.
   */
  public static int getDoubleVectorLength() {
    return DOUBLE_SPECIES.length();
  }
}
