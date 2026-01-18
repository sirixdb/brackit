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

/**
 * Interface for predicates that support vectorized evaluation.
 * Implementations process arrays of primitive values in SIMD batches.
 *
 * @author Brackit Project Team
 */
public interface VectorizedPredicate {

  /**
   * Evaluate predicate on batch of long values.
   *
   * @param values  Input values
   * @param offset  Start offset
   * @param length  Number of elements
   * @param results Output: true if predicate matches
   * @return Number of matching elements
   */
  int evaluateBatch(long[] values, int offset, int length, boolean[] results);

  /**
   * Get matching indices directly (avoids boolean array allocation).
   *
   * @param values  Input values
   * @param offset  Start offset
   * @param length  Number of elements
   * @param indices Output: indices of matching elements
   * @return Number of matching indices written
   */
  int filterIndices(long[] values, int offset, int length, int[] indices);

  /**
   * Comparison operators supported by vectorized predicates.
   */
  enum ComparisonOp {
    EQ,  // equals
    NE,  // not equals
    LT,  // less than
    LE,  // less than or equal
    GT,  // greater than
    GE   // greater than or equal
  }

  /**
   * Create a vectorized predicate for comparing long values against a threshold.
   *
   * @param op        Comparison operator
   * @param threshold The threshold value to compare against
   * @return A vectorized predicate implementation
   */
  static VectorizedPredicate compareLong(ComparisonOp op, long threshold) {
    return new LongComparisonPredicate(op, threshold);
  }

  /**
   * Implementation of vectorized predicate for long comparisons.
   */
  final class LongComparisonPredicate implements VectorizedPredicate {
    private final ComparisonOp op;
    private final long threshold;

    LongComparisonPredicate(ComparisonOp op, long threshold) {
      this.op = op;
      this.threshold = threshold;
    }

    @Override
    public int evaluateBatch(long[] values, int offset, int length, boolean[] results) {
      int count = 0;
      for (int i = 0; i < length; i++) {
        boolean match = evaluate(values[offset + i]);
        results[i] = match;
        if (match)
          count++;
      }
      return count;
    }

    @Override
    public int filterIndices(long[] values, int offset, int length, int[] indices) {
      return switch (op) {
        case GT -> VectorOps.filterIndicesGreaterThan(values, offset, length, threshold, indices);
        default -> filterIndicesGeneric(values, offset, length, indices);
      };
    }

    private int filterIndicesGeneric(long[] values, int offset, int length, int[] indices) {
      int count = 0;
      for (int i = 0; i < length; i++) {
        if (evaluate(values[offset + i])) {
          indices[count++] = offset + i;
        }
      }
      return count;
    }

    private boolean evaluate(long value) {
      return switch (op) {
        case EQ -> value == threshold;
        case NE -> value != threshold;
        case LT -> value < threshold;
        case LE -> value <= threshold;
        case GT -> value > threshold;
        case GE -> value >= threshold;
      };
    }
  }
}
