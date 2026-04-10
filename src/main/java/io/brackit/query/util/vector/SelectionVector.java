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
 * DuckDB-style selection vector for branchless filtering.
 *
 * A selection vector is an array of indices indicating which rows in a vector
 * are "active". Instead of branching per row, filters populate selection vectors
 * using a branchless pattern: {@code sel[count] = i; count += (condition ? 1 : 0);}
 *
 * This avoids branch mispredictions since the CPU never needs to predict
 * which rows pass the filter. Downstream operators iterate only over the
 * selected indices.
 *
 * NOTE: Each filter method inlines the comparison directly in the loop body
 * rather than delegating through a lambda/predicate. This is intentional --
 * the JIT compiler can only auto-vectorize and eliminate branches in tight
 * loops over primitives when the comparison is inlined. A lambda indirection
 * would defeat the branchless optimization that is the entire point of this class.
 *
 * @author Brackit Project Team
 */
@SuppressWarnings("DuplicatedCode") // Intentional: inlined comparisons for JIT optimization
public final class SelectionVector {

  public static final int DEFAULT_CAPACITY = 2048;

  private final int[] selected;
  private int size;

  public SelectionVector() {
    this(DEFAULT_CAPACITY);
  }

  public SelectionVector(int capacity) {
    this.selected = new int[capacity];
    this.size = 0;
  }

  public int[] getSelected() {
    return selected;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  /**
   * Initialize to identity mapping [0, 1, 2, ..., n-1].
   * Used when all rows are active (no filtering yet).
   */
  public void initIdentity(int n) {
    for (int i = 0; i < n; i++) {
      selected[i] = i;
    }
    this.size = n;
  }

  // ==================== Branchless Long Filters ====================

  public int filterEqLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] == target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterNeLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] != target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterLtLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] < target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterLeLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] <= target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterGtLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] > target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterGeLong(long[] values, int length, long target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] >= target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  // ==================== Branchless Double Filters ====================

  public int filterEqDouble(double[] values, int length, double target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] == target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterLtDouble(double[] values, int length, double target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] < target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterLeDouble(double[] values, int length, double target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] <= target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterGtDouble(double[] values, int length, double target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] > target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int filterGeDouble(double[] values, int length, double target) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      selected[count] = i;
      count += (values[i] >= target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  // ==================== Cascading Selection ====================

  public int tightenEqLong(long[] values, long target) {
    int count = 0;
    for (int i = 0; i < size; i++) {
      int idx = selected[i];
      selected[count] = idx;
      count += (values[idx] == target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int tightenGtLong(long[] values, long target) {
    int count = 0;
    for (int i = 0; i < size; i++) {
      int idx = selected[i];
      selected[count] = idx;
      count += (values[idx] > target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  public int tightenLtLong(long[] values, long target) {
    int count = 0;
    for (int i = 0; i < size; i++) {
      int idx = selected[i];
      selected[count] = idx;
      count += (values[idx] < target) ? 1 : 0;
    }
    this.size = count;
    return count;
  }

  // ==================== Validity Mask Integration ====================

  /**
   * Apply validity mask: remove indices where validity bit is not set.
   * Uses branchless pattern for NULL filtering.
   */
  public int applyValidityMask(long[] validityMask) {
    int count = 0;
    for (int i = 0; i < size; i++) {
      int idx = selected[i];
      int wordIdx = idx >>> 6;
      int bitIdx = idx & 63;
      long valid = (validityMask[wordIdx] >>> bitIdx) & 1L;
      selected[count] = idx;
      count += (int) valid;
    }
    this.size = count;
    return count;
  }
}
