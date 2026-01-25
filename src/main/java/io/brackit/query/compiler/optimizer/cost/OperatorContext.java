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
package io.brackit.query.compiler.optimizer.cost;

/**
 * Reusable context for cost estimation. Avoids allocation in hot paths.
 *
 * <p>Design: HFT-style with primitive fields, ThreadLocal pooling.
 * All fields are primitive types to avoid boxing overhead.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>{@code
 * OperatorContext ctx = OperatorContext.acquire();
 * ctx.forSelection(inputCard, selectivity);
 * double cost = costModel.estimateCost(XQ.Selection, ctx);
 * }</pre>
 *
 * <p>Thread-safety: Each thread gets its own instance via ThreadLocal.</p>
 */
public final class OperatorContext {

  /**
   * Input cardinality (number of tuples entering the operator).
   */
  public long inputCardinality;

  /**
   * Output cardinality (number of tuples produced by the operator).
   */
  public long outputCardinality;

  /**
   * Selectivity factor for filtering operators (0.0 to 1.0).
   */
  public double selectivity;

  /**
   * Index identifier (-1 if no index is used).
   */
  public int indexId;

  /**
   * Whether this operator uses an index scan.
   */
  public boolean isIndexScan;

  /**
   * Left input cardinality for binary operators (joins).
   */
  public long leftCardinality;

  /**
   * Right input cardinality for binary operators (joins).
   */
  public long rightCardinality;

  /**
   * Number of distinct values (for selectivity estimation).
   */
  public long distinctValues;

  /**
   * Navigation depth for deref expressions (e.g., $obj.a.b.c has depth 3).
   */
  public int derefDepth;

  /**
   * Whether this is a descendant deref ($obj..field) vs direct deref ($obj.field).
   */
  public boolean isDescendantDeref;

  /**
   * Whether this operation involves array access/unboxing.
   */
  public boolean isArrayAccess;

  /**
   * Average array size when array access is involved.
   */
  public long avgArraySize;

  /**
   * Thread-local pool to avoid allocation.
   */
  private static final ThreadLocal<OperatorContext> POOL = ThreadLocal.withInitial(OperatorContext::new);

  /**
   * Private constructor - use {@link #acquire()} to get an instance.
   */
  private OperatorContext() {
  }

  /**
   * Acquire a context from the thread-local pool.
   * The returned context is reset to default values.
   *
   * @return A reset OperatorContext instance
   */
  public static OperatorContext acquire() {
    return POOL.get().reset();
  }

  /**
   * Reset all fields to defaults.
   *
   * @return this context for method chaining
   */
  public OperatorContext reset() {
    inputCardinality = 0L;
    outputCardinality = 0L;
    selectivity = 1.0;
    indexId = -1;
    isIndexScan = false;
    leftCardinality = 0L;
    rightCardinality = 0L;
    distinctValues = 0L;
    derefDepth = 0;
    isDescendantDeref = false;
    isArrayAccess = false;
    avgArraySize = 0L;
    return this;
  }

  /**
   * Configure for a scan operation.
   *
   * @param cardinality Number of tuples to scan
   * @return this context for method chaining
   */
  public OperatorContext forScan(long cardinality) {
    this.inputCardinality = cardinality;
    this.outputCardinality = cardinality;
    this.selectivity = 1.0;
    return this;
  }

  /**
   * Configure for an index scan operation.
   *
   * @param totalCardinality  Total number of tuples in the relation
   * @param resultCardinality Expected number of tuples from index lookup
   * @param indexId           The index identifier
   * @return this context for method chaining
   */
  public OperatorContext forIndexScan(long totalCardinality, long resultCardinality, int indexId) {
    this.inputCardinality = totalCardinality;
    this.outputCardinality = Math.max(1L, resultCardinality);
    this.isIndexScan = true;
    this.indexId = indexId;
    this.selectivity = totalCardinality > 0 ? (double) resultCardinality / totalCardinality : 1.0;
    return this;
  }

  /**
   * Configure for a selection (filter) operation.
   *
   * @param input Input cardinality
   * @param sel   Selectivity factor (0.0 to 1.0)
   * @return this context for method chaining
   */
  public OperatorContext forSelection(long input, double sel) {
    this.inputCardinality = input;
    this.selectivity = Math.max(0.0, Math.min(1.0, sel));
    this.outputCardinality = Math.max(1L, (long) (input * this.selectivity));
    return this;
  }

  /**
   * Configure for a join operation.
   *
   * @param leftCard  Left input cardinality
   * @param rightCard Right input cardinality
   * @param joinSel   Join selectivity factor
   * @return this context for method chaining
   */
  public OperatorContext forJoin(long leftCard, long rightCard, double joinSel) {
    this.leftCardinality = leftCard;
    this.rightCardinality = rightCard;
    this.selectivity = Math.max(0.0, Math.min(1.0, joinSel));
    this.inputCardinality = leftCard + rightCard;
    // Ensure output cardinality doesn't overflow
    final double product = (double) leftCard * rightCard * this.selectivity;
    this.outputCardinality = Math.max(1L, (long) Math.min(product, Long.MAX_VALUE - 1));
    return this;
  }

  /**
   * Configure for a group-by operation.
   *
   * @param input           Input cardinality
   * @param estimatedGroups Estimated number of groups
   * @return this context for method chaining
   */
  public OperatorContext forGroupBy(long input, long estimatedGroups) {
    this.inputCardinality = input;
    this.outputCardinality = Math.max(1L, estimatedGroups);
    return this;
  }

  /**
   * Configure for a sort (order-by) operation.
   *
   * @param input Input cardinality (preserved in output)
   * @return this context for method chaining
   */
  public OperatorContext forSort(long input) {
    this.inputCardinality = input;
    this.outputCardinality = input;
    return this;
  }

  /**
   * Configure for a deref (field navigation) operation.
   *
   * <p>Deref expressions like {@code $obj.field} or {@code $obj.a.b.c}
   * have different costs based on navigation depth.</p>
   *
   * @param input        Input cardinality
   * @param depth        Navigation depth (number of field accesses)
   * @param isDescendant Whether this is descendant navigation ($obj..field)
   * @return this context for method chaining
   */
  public OperatorContext forDeref(long input, int depth, boolean isDescendant) {
    this.inputCardinality = input;
    this.derefDepth = Math.max(1, depth);
    this.isDescendantDeref = isDescendant;
    this.isArrayAccess = false;
    // Descendant deref may return multiple results per input
    if (isDescendant) {
      this.outputCardinality = input * 5L; // Heuristic multiplier
    } else {
      this.outputCardinality = input; // 1:1 for direct deref
    }
    return this;
  }

  /**
   * Configure for an array access operation.
   *
   * <p>Array operations like {@code $array[]} or {@code $array[1]}
   * have different costs based on access pattern.</p>
   *
   * @param input          Input cardinality
   * @param avgArraySize   Average array size (for full unboxing)
   * @param isSingleAccess Whether this is single element access ($array[1])
   * @return this context for method chaining
   */
  public OperatorContext forArrayAccess(long input, long avgArraySize, boolean isSingleAccess) {
    this.inputCardinality = input;
    this.avgArraySize = avgArraySize;
    this.isArrayAccess = !isSingleAccess; // false = single access, true = full unboxing
    this.isDescendantDeref = false;
    if (isSingleAccess) {
      this.outputCardinality = input; // Single access returns 1 per input
    } else {
      // Full unboxing multiplies by array size
      final double product = (double) input * avgArraySize;
      this.outputCardinality = Math.max(1L, (long) Math.min(product, Long.MAX_VALUE - 1));
    }
    return this;
  }

  /**
   * Configure for a flattened field operation.
   *
   * <p>Flattened field like {@code $array[].field} combines
   * array unboxing with field navigation.</p>
   *
   * @param input        Input cardinality
   * @param avgArraySize Average array size
   * @param depth        Field navigation depth
   * @return this context for method chaining
   */
  public OperatorContext forFlattenedField(long input, long avgArraySize, int depth) {
    this.inputCardinality = input;
    this.avgArraySize = avgArraySize;
    this.derefDepth = Math.max(1, depth);
    this.isArrayAccess = true;
    this.isDescendantDeref = false;
    // Output = input * array_size (unboxing)
    final double product = (double) input * avgArraySize;
    this.outputCardinality = Math.max(1L, (long) Math.min(product, Long.MAX_VALUE - 1));
    return this;
  }

  @Override
  public String toString() {
    return String.format("OperatorContext[in=%d, out=%d, sel=%.4f, left=%d, right=%d, idx=%d, isIdx=%b]",
                         inputCardinality,
                         outputCardinality,
                         selectivity,
                         leftCardinality,
                         rightCardinality,
                         indexId,
                         isIndexScan);
  }
}
