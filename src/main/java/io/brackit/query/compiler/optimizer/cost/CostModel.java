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
 * Cost model interface for query optimization.
 *
 * <p>Implementations estimate execution costs for different operators
 * to enable cost-based query optimization decisions.</p>
 *
 * <p>Cost units are abstract and implementation-defined, but should be
 * consistent within a cost model so that costs can be compared.</p>
 */
public interface CostModel {

  /**
   * Epsilon for floating-point cost comparisons.
   */
  double COST_EPSILON = 1e-9;

  /**
   * Estimate cost for an operator.
   *
   * @param operatorType XQ constant (ForBind, Selection, Join, etc.)
   * @param ctx          Reusable context with cardinality information
   * @return Estimated cost (non-negative)
   */
  double estimateCost(int operatorType, OperatorContext ctx);

  /**
   * Compare two costs with tolerance for floating-point comparison.
   *
   * @param cost1 First cost to compare
   * @param cost2 Second cost to compare
   * @return negative if cost1 &lt; cost2, positive if cost1 &gt; cost2, 0 if equal within epsilon
   */
  default int compareCosts(double cost1, double cost2) {
    final double diff = cost1 - cost2;
    if (Math.abs(diff) < COST_EPSILON) {
      return 0;
    }
    return diff < 0 ? -1 : 1;
  }

  /**
   * Clamp a cost value to a valid range.
   *
   * @param cost The cost to clamp
   * @return The clamped cost, guaranteed to be non-negative and finite
   */
  default double clampCost(double cost) {
    if (Double.isNaN(cost) || cost < 0.0) {
      return 0.0;
    }
    if (Double.isInfinite(cost)) {
      return Double.MAX_VALUE / 2.0;
    }
    return cost;
  }
}
