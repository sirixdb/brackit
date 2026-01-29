/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.cost;

import io.brackit.query.compiler.XQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SimpleCostModel}.
 *
 * <p>Note: OperatorContext uses ThreadLocal pooling, so multiple acquire() calls
 * in the same thread return the same instance. Tests must compute costs immediately
 * after configuring each context.</p>
 */
public class SimpleCostModelTest {

  private SimpleCostModel costModel;

  @BeforeEach
  public void setUp() {
    costModel = new SimpleCostModel();
  }

  // Basic Operations

  @Test
  public void testScanCostScales() {
    // Compute costs immediately - acquire() returns same pooled instance
    double costLow = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(10));
    double costHigh = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(1000));

    assertTrue(costHigh > costLow, "Higher cardinality should have higher cost");
  }

  @Test
  public void testIndexScanCheaper() {
    double fullCost = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(10000));
    double indexCost = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forIndexScan(10000, 100, 1));

    assertTrue(indexCost < fullCost, "Index scan should be cheaper than full scan for selective queries");
  }

  @Test
  public void testSelectionCost() {
    double cost = costModel.estimateCost(XQ.Selection, OperatorContext.acquire().forSelection(1000, 0.1));

    assertTrue(cost > 0.0, "Selection cost should be positive");
  }

  @Test
  public void testJoinCost() {
    double cost = costModel.estimateCost(XQ.Join, OperatorContext.acquire().forJoin(100, 1000, 0.01));

    assertTrue(cost > 0.0, "Join cost should be positive");
  }

  @Test
  public void testLetBindZeroCost() {
    double cost = costModel.estimateCost(XQ.LetBind, OperatorContext.acquire().forScan(100));

    assertEquals(0.0, cost, 0.0, "LetBind should have zero cost");
  }

  @Test
  public void testSortCost() {
    double cost10 = costModel.estimateCost(XQ.OrderBy, OperatorContext.acquire().forSort(10));
    double cost1000 = costModel.estimateCost(XQ.OrderBy, OperatorContext.acquire().forSort(1000));

    // n log n growth: 1000 * log(1000) / (10 * log(10)) ≈ 300
    double ratio = cost1000 / cost10;
    assertTrue(ratio > 100 && ratio < 500, "Sort cost should grow faster than linear, ratio=" + ratio);
  }

  // Deref Operations

  @Test
  public void testDerefCostScalesWithDepth() {
    double costDepth1 = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 1, false));
    double costDepth3 = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 3, false));

    assertTrue(costDepth3 > costDepth1, "Deeper deref should cost more");
  }

  @Test
  public void testDescendantDerefMoreExpensive() {
    double costDirect = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 1, false));
    double costDescendant = costModel.estimateCost(XQ.DerefDescendantExpr,
                                                   OperatorContext.acquire().forDeref(100, 1, true));

    assertTrue(costDescendant > costDirect, "Descendant deref should be more expensive");
  }

  @Test
  public void testDerefCostScalesWithCardinality() {
    double costLow = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(10, 1, false));
    double costHigh = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(1000, 1, false));

    assertTrue(costHigh > costLow, "Higher cardinality should have higher cost");
  }

  // Array Operations

  @Test
  public void testSingleAccessCheaper() {
    double costSingle = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 10, true));
    double costFull = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 10, false));

    assertTrue(costSingle < costFull, "Single element access should be cheaper");
  }

  @Test
  public void testUnboxingCostScalesWithArraySize() {
    double costSmall = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 5, false));
    double costLarge = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 50, false));

    assertTrue(costLarge > costSmall, "Larger arrays should cost more to unbox");
  }

  @Test
  public void testFlattenedFieldCost() {
    double cost = costModel.estimateCost(XQ.FlattenedField, OperatorContext.acquire().forFlattenedField(100, 10, 1));

    assertTrue(cost > 0.0, "Flattened field should have positive cost");
  }

  @Test
  public void testSliceCost() {
    // Small slice
    OperatorContext ctxSmall = OperatorContext.acquire();
    ctxSmall.inputCardinality = 100;
    ctxSmall.outputCardinality = 300; // 3 elements per input
    ctxSmall.isArrayAccess = false;
    double costSmall = costModel.estimateCost(XQ.ArrayIndexSlice, ctxSmall);

    // Large slice
    OperatorContext ctxLarge = OperatorContext.acquire();
    ctxLarge.inputCardinality = 100;
    ctxLarge.outputCardinality = 1000; // 10 elements per input
    ctxLarge.isArrayAccess = false;
    double costLarge = costModel.estimateCost(XQ.ArrayIndexSlice, ctxLarge);

    assertTrue(costLarge > costSmall, "Larger slice should cost more");
  }

  // Cost Clamping

  @Test
  public void testNegativeCostClamped() {
    double clamped = costModel.clampCost(-10.0);
    assertEquals(0.0, clamped, 0.0, "Negative cost should be clamped to 0");
  }

  @Test
  public void testNaNCostClamped() {
    double clamped = costModel.clampCost(Double.NaN);
    assertEquals(0.0, clamped, 0.0, "NaN cost should be clamped to 0");
  }

  @Test
  public void testInfiniteCostClamped() {
    double clamped = costModel.clampCost(Double.POSITIVE_INFINITY);
    assertTrue(Double.isFinite(clamped), "Infinite cost should be clamped to finite value");
  }

  @Test
  public void testNormalCostUnchanged() {
    double cost = 100.0;
    double clamped = costModel.clampCost(cost);
    assertEquals(cost, clamped, 0.0, "Normal cost should be unchanged");
  }

  @Test
  public void testCustomCostFactors() {
    SimpleCostModel customModel = new SimpleCostModel(2.0,  // scanCostPerTuple
                                                      20.0, // indexOverhead
                                                      1.0,  // indexCostPerTuple
                                                      3.0,  // hashBuildFactor
                                                      2.0,  // hashProbeFactor
                                                      0.02, // sortFactor
                                                      0.2   // selectionCost
    );

    double defaultCost = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(100));
    double customCost = customModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(100));

    // Custom scan cost is 2x default
    assertTrue(Math.abs(customCost - 2 * defaultCost) < 0.001, "Custom cost should be 2x default");
  }
}
