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
package io.brackit.query.util.vector;

/**
 * DuckDB-style validity bitmask for NULL tracking.
 *
 * Uses a long[] bitmask where each bit represents one row.
 * A bit set to 1 means the value is valid (non-NULL).
 * A bit set to 0 means NULL.
 *
 * Optimization: if the mask is null, ALL values are valid (the common case).
 * This avoids allocating 256 bytes for the typical case of no NULLs in a vector.
 *
 * For a 2048-element vector, only 32 longs (256 bytes) are needed.
 * Combining masks is a single bitwise AND per 64 rows.
 *
 * @author Brackit Project Team
 */
public final class ValidityMask {

  private long[] mask;
  private final int capacity;

  public ValidityMask(int capacity) {
    this.capacity = capacity;
    // null mask means all valid (optimization for the common case)
    this.mask = null;
  }

  /**
   * Returns the raw mask array, or null if all values are valid.
   */
  public long[] getMask() {
    return mask;
  }

  /**
   * Returns true if all values are valid (no NULLs).
   */
  public boolean allValid() {
    return mask == null;
  }

  /**
   * Check if a specific position is valid.
   */
  public boolean isValid(int pos) {
    if (mask == null) {
      return true;
    }
    return (mask[pos >>> 6] & (1L << (pos & 63))) != 0;
  }

  /**
   * Mark a position as NULL (invalid). Lazily allocates the mask.
   */
  public void setNull(int pos) {
    ensureAllocated();
    mask[pos >>> 6] &= ~(1L << (pos & 63));
  }

  /**
   * Mark a position as valid.
   */
  public void setValid(int pos) {
    if (mask == null) {
      return; // already all valid
    }
    mask[pos >>> 6] |= (1L << (pos & 63));
  }

  /**
   * Count the number of valid (non-NULL) entries.
   */
  public int countValid() {
    if (mask == null) {
      return capacity;
    }
    int count = 0;
    int words = (capacity + 63) >>> 6;
    for (int i = 0; i < words; i++) {
      count += Long.bitCount(mask[i]);
    }
    return count;
  }

  /**
   * Count the number of NULL entries.
   */
  public int countNull() {
    return capacity - countValid();
  }

  /**
   * Combine two masks with AND. The result has a NULL wherever either input has a NULL.
   * This handles the common case where both masks may be null (all valid).
   */
  public static long[] and(long[] a, long[] b, int words) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    long[] result = new long[words];
    for (int i = 0; i < words; i++) {
      result[i] = a[i] & b[i];
    }
    return result;
  }

  /**
   * Combine two masks with OR. The result is valid wherever either input is valid.
   */
  public static long[] or(long[] a, long[] b, int words) {
    if (a == null || b == null) {
      return null; // all valid
    }
    long[] result = new long[words];
    for (int i = 0; i < words; i++) {
      result[i] = a[i] | b[i];
    }
    return result;
  }

  /**
   * Reset to all-valid state.
   */
  public void reset() {
    mask = null;
  }

  /**
   * Reset with a specific count of valid entries.
   * Sets all bits up to 'count' as valid.
   */
  public void resetAllValid(int count) {
    if (mask == null) {
      return;
    }
    int fullWords = count >>> 6;
    int remaining = count & 63;
    for (int i = 0; i < fullWords; i++) {
      mask[i] = -1L; // all bits set
    }
    if (remaining > 0 && fullWords < mask.length) {
      mask[fullWords] = (1L << remaining) - 1;
    }
    for (int i = fullWords + (remaining > 0 ? 1 : 0); i < mask.length; i++) {
      mask[i] = 0L;
    }
  }

  private void ensureAllocated() {
    if (mask == null) {
      int words = (capacity + 63) >>> 6;
      mask = new long[words];
      // Set all bits to 1 (all valid) since we were in the "all valid" state
      for (int i = 0; i < words; i++) {
        mask[i] = -1L;
      }
    }
  }
}
