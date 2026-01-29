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

import io.brackit.query.compiler.XQ;

/**
 * Simple cost model based on I/O and CPU costs.
 *
 * <p>Cost = I/O_cost + CPU_cost</p>
 *
 * <p>Calibrated for typical SSD-based systems with in-memory processing.</p>
 *
 * <p>Cost factors are tunable via system properties:</p>
 * <ul>
 * <li>{@code brackit.cost.scan} - Cost per tuple for sequential scan</li>
 * <li>{@code brackit.cost.index.overhead} - Fixed overhead for index access</li>
 * <li>{@code brackit.cost.index.tuple} - Cost per tuple for index scan</li>
 * <li>{@code brackit.cost.hash.build} - Factor for hash table build</li>
 * <li>{@code brackit.cost.hash.probe} - Factor for hash table probe</li>
 * <li>{@code brackit.cost.sort} - Factor for sort operations</li>
 * <li>{@code brackit.cost.selection} - Cost per tuple for predicate evaluation</li>
 * </ul>
 */
public final class SimpleCostModel implements CostModel {

  // Cost constants (tunable via system properties)
  private final double scanCostPerTuple;
  private final double indexOverhead;
  private final double indexCostPerTuple;
  private final double hashBuildFactor;
  private final double hashProbeFactor;
  private final double sortFactor;
  private final double selectionCost;

  // JSON/JSONiq specific cost factors
  private final double derefCostPerLevel;
  private final double descendantDerefFactor;
  private final double arrayAccessCost;
  private final double arrayUnboxingCostPerElement;

  /**
   * Creates a SimpleCostModel with default cost factors.
   */
  public SimpleCostModel() {
    this.scanCostPerTuple = getDoubleProperty("brackit.cost.scan", 1.0);
    this.indexOverhead = getDoubleProperty("brackit.cost.index.overhead", 10.0);
    this.indexCostPerTuple = getDoubleProperty("brackit.cost.index.tuple", 0.5);
    this.hashBuildFactor = getDoubleProperty("brackit.cost.hash.build", 2.0);
    this.hashProbeFactor = getDoubleProperty("brackit.cost.hash.probe", 1.2);
    this.sortFactor = getDoubleProperty("brackit.cost.sort", 0.01);
    this.selectionCost = getDoubleProperty("brackit.cost.selection", 0.1);
    // JSON/JSONiq specific costs
    this.derefCostPerLevel = getDoubleProperty("brackit.cost.deref.level", 0.2);
    this.descendantDerefFactor = getDoubleProperty("brackit.cost.deref.descendant", 5.0);
    this.arrayAccessCost = getDoubleProperty("brackit.cost.array.access", 0.1);
    this.arrayUnboxingCostPerElement = getDoubleProperty("brackit.cost.array.unbox", 0.05);
  }

  /**
   * Creates a SimpleCostModel with custom cost factors for basic operations.
   *
   * <p>JSON/JSONiq cost factors use defaults.</p>
   *
   * @param scanCostPerTuple  Cost per tuple for sequential scan
   * @param indexOverhead     Fixed overhead for index access
   * @param indexCostPerTuple Cost per tuple for index scan
   * @param hashBuildFactor   Factor for hash table build
   * @param hashProbeFactor   Factor for hash table probe
   * @param sortFactor        Factor for sort operations
   * @param selectionCost     Cost per tuple for predicate evaluation
   */
  public SimpleCostModel(double scanCostPerTuple, double indexOverhead, double indexCostPerTuple,
      double hashBuildFactor, double hashProbeFactor, double sortFactor, double selectionCost) {
    this(scanCostPerTuple,
         indexOverhead,
         indexCostPerTuple,
         hashBuildFactor,
         hashProbeFactor,
         sortFactor,
         selectionCost,
         0.2,
         5.0,
         0.1,
         0.05);
  }

  /**
   * Creates a SimpleCostModel with all custom cost factors.
   *
   * @param scanCostPerTuple            Cost per tuple for sequential scan
   * @param indexOverhead               Fixed overhead for index access
   * @param indexCostPerTuple           Cost per tuple for index scan
   * @param hashBuildFactor             Factor for hash table build
   * @param hashProbeFactor             Factor for hash table probe
   * @param sortFactor                  Factor for sort operations
   * @param selectionCost               Cost per tuple for predicate evaluation
   * @param derefCostPerLevel           Cost per navigation level for deref
   * @param descendantDerefFactor       Multiplier for descendant deref cost
   * @param arrayAccessCost             Base cost for array access
   * @param arrayUnboxingCostPerElement Cost per element for array unboxing
   */
  public SimpleCostModel(double scanCostPerTuple, double indexOverhead, double indexCostPerTuple,
      double hashBuildFactor, double hashProbeFactor, double sortFactor, double selectionCost, double derefCostPerLevel,
      double descendantDerefFactor, double arrayAccessCost, double arrayUnboxingCostPerElement) {
    this.scanCostPerTuple = scanCostPerTuple;
    this.indexOverhead = indexOverhead;
    this.indexCostPerTuple = indexCostPerTuple;
    this.hashBuildFactor = hashBuildFactor;
    this.hashProbeFactor = hashProbeFactor;
    this.sortFactor = sortFactor;
    this.selectionCost = selectionCost;
    this.derefCostPerLevel = derefCostPerLevel;
    this.descendantDerefFactor = descendantDerefFactor;
    this.arrayAccessCost = arrayAccessCost;
    this.arrayUnboxingCostPerElement = arrayUnboxingCostPerElement;
  }

  private static double getDoubleProperty(String key, double defaultValue) {
    final String value = System.getProperty(key);
    if (value != null) {
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException e) {
        // Fall through to default
      }
    }
    return defaultValue;
  }

  @Override
  public double estimateCost(int operatorType, OperatorContext ctx) {
    final double cost = switch (operatorType) {
      case XQ.ForBind -> estimateScanCost(ctx);
      case XQ.Selection -> estimateSelectionCost(ctx);
      case XQ.Join -> estimateJoinCost(ctx);
      case XQ.GroupBy -> estimateGroupByCost(ctx);
      case XQ.OrderBy -> estimateSortCost(ctx);
      case XQ.LetBind -> 0.0; // Negligible
      case XQ.Start, XQ.End -> 0.0; // Pipeline markers, no cost
      // JSON/JSONiq specific operators
      case XQ.DerefExpr -> estimateDerefCost(ctx);
      case XQ.DerefDescendantExpr -> estimateDescendantDerefCost(ctx);
      case XQ.ArrayAccess -> estimateArrayAccessCost(ctx);
      case XQ.FlattenedField -> estimateFlattenedFieldCost(ctx);
      case XQ.ArrayIndexSlice -> estimateArraySliceCost(ctx);
      default -> ctx.inputCardinality * scanCostPerTuple;
    };
    return clampCost(cost);
  }

  /**
   * Estimate cost for a scan operation.
   *
   * <p>For index scans: fixed overhead + (result tuples * index cost)</p>
   * <p>For full scans: input tuples * scan cost</p>
   *
   * @param ctx Operator context with cardinality info
   * @return Estimated scan cost
   */
  private double estimateScanCost(OperatorContext ctx) {
    if (ctx.isIndexScan) {
      return indexOverhead + ctx.outputCardinality * indexCostPerTuple;
    }
    return ctx.inputCardinality * scanCostPerTuple;
  }

  /**
   * Estimate cost for a selection (filter) operation.
   *
   * <p>Cost = input tuples * selection cost per tuple</p>
   *
   * @param ctx Operator context with cardinality info
   * @return Estimated selection cost
   */
  private double estimateSelectionCost(OperatorContext ctx) {
    return ctx.inputCardinality * selectionCost;
  }

  /**
   * Estimate cost for a join operation using hash join model.
   *
   * <p>Hash join cost model:</p>
   * <ul>
   * <li>Build phase: smaller input * build factor</li>
   * <li>Probe phase: larger input * probe factor</li>
   * </ul>
   *
   * @param ctx Operator context with left/right cardinalities
   * @return Estimated join cost
   */
  private double estimateJoinCost(OperatorContext ctx) {
    final long buildSide = Math.min(ctx.leftCardinality, ctx.rightCardinality);
    final long probeSide = Math.max(ctx.leftCardinality, ctx.rightCardinality);

    final double buildCost = buildSide * hashBuildFactor;
    final double probeCost = probeSide * hashProbeFactor;

    return buildCost + probeCost;
  }

  /**
   * Estimate cost for a group-by operation.
   *
   * <p>Hash-based grouping: input tuples * build factor</p>
   *
   * @param ctx Operator context with cardinality info
   * @return Estimated group-by cost
   */
  private double estimateGroupByCost(OperatorContext ctx) {
    return ctx.inputCardinality * hashBuildFactor;
  }

  /**
   * Estimate cost for a sort operation.
   *
   * <p>O(n log n) sorting: n * log2(n) * sort factor</p>
   *
   * @param ctx Operator context with cardinality info
   * @return Estimated sort cost
   */
  private double estimateSortCost(OperatorContext ctx) {
    if (ctx.inputCardinality <= 1) {
      return 0.0;
    }
    // n log n sorting
    final double logN = Math.log(ctx.inputCardinality) / Math.log(2);
    return ctx.inputCardinality * logN * sortFactor;
  }

  /**
   * Estimate cost for a deref (field navigation) operation.
   *
   * <p>Cost model: input * (baseCost + depth * costPerLevel)</p>
   *
   * <p>The cost increases with navigation depth because:</p>
   * <ul>
   * <li>Each level requires a field lookup</li>
   * <li>Deeper paths may have worse cache locality</li>
   * <li>Type checking overhead at each level</li>
   * </ul>
   *
   * @param ctx Operator context with deref depth info
   * @return Estimated deref cost
   */
  private double estimateDerefCost(OperatorContext ctx) {
    final int depth = Math.max(1, ctx.derefDepth);
    // Base cost + incremental cost per level
    final double perTupleCost = selectionCost + (depth * derefCostPerLevel);
    return ctx.inputCardinality * perTupleCost;
  }

  /**
   * Estimate cost for a descendant deref operation ($obj..field).
   *
   * <p>Descendant navigation is much more expensive than direct deref because:</p>
   * <ul>
   * <li>Must traverse entire subtree looking for matches</li>
   * <li>May return multiple results per input</li>
   * <li>Cannot use path index efficiently without statistics</li>
   * </ul>
   *
   * @param ctx Operator context with deref info
   * @return Estimated descendant deref cost
   */
  private double estimateDescendantDerefCost(OperatorContext ctx) {
    final int depth = Math.max(1, ctx.derefDepth);
    // Descendant is much more expensive - must traverse subtree
    final double baseCost = estimateDerefCost(ctx);
    return baseCost * descendantDerefFactor;
  }

  /**
   * Estimate cost for array access operations.
   *
   * <p>Cost model depends on access pattern:</p>
   * <ul>
   * <li>Single index ($array[1]): O(1) access</li>
   * <li>Full unboxing ($array[]): O(n) where n = array size</li>
   * <li>Filtered ($array[predicate]): O(n) with predicate cost</li>
   * </ul>
   *
   * @param ctx Operator context with array access info
   * @return Estimated array access cost
   */
  private double estimateArrayAccessCost(OperatorContext ctx) {
    if (!ctx.isArrayAccess) {
      // Single element access (like $array[1])
      return ctx.inputCardinality * arrayAccessCost;
    }

    // Full array unboxing or slice
    final long avgSize = Math.max(1L, ctx.avgArraySize);
    final double unboxingCost = ctx.inputCardinality * (arrayAccessCost + avgSize * arrayUnboxingCostPerElement);
    return unboxingCost;
  }

  /**
   * Estimate cost for flattened field operations ($array[].field).
   *
   * <p>Combines array unboxing cost with field navigation cost.</p>
   *
   * @param ctx Operator context with array and deref info
   * @return Estimated flattened field cost
   */
  private double estimateFlattenedFieldCost(OperatorContext ctx) {
    // Cost = array unboxing + deref on each element
    final long avgSize = Math.max(1L, ctx.avgArraySize);
    final int depth = Math.max(1, ctx.derefDepth);

    // Unboxing cost
    final double unboxCost = ctx.inputCardinality * (arrayAccessCost + avgSize * arrayUnboxingCostPerElement);

    // Deref cost on unboxed elements
    final long unboxedCount = ctx.inputCardinality * avgSize;
    final double derefCost = unboxedCount * (selectionCost + depth * derefCostPerLevel);

    return unboxCost + derefCost;
  }

  /**
   * Estimate cost for array slice operations ($array[start to end]).
   *
   * <p>Slice is cheaper than full unboxing because we know the bounds.</p>
   *
   * @param ctx Operator context with array info
   * @return Estimated slice cost
   */
  private double estimateArraySliceCost(OperatorContext ctx) {
    // Slice cost is proportional to slice size, not full array
    // Output cardinality gives us the slice size
    final long sliceSize = Math.max(1L, ctx.outputCardinality / Math.max(1L, ctx.inputCardinality));
    return ctx.inputCardinality * (arrayAccessCost + sliceSize * arrayUnboxingCostPerElement);
  }

  /**
   * Get the scan cost per tuple.
   *
   * @return Scan cost per tuple
   */
  public double getScanCostPerTuple() {
    return scanCostPerTuple;
  }

  /**
   * Get the index overhead cost.
   *
   * @return Index overhead cost
   */
  public double getIndexOverhead() {
    return indexOverhead;
  }

  /**
   * Get the index cost per tuple.
   *
   * @return Index cost per tuple
   */
  public double getIndexCostPerTuple() {
    return indexCostPerTuple;
  }
}
