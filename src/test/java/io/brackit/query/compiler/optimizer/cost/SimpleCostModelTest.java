/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.cost;

import io.brackit.query.compiler.XQ;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SimpleCostModel}.
 *
 * <p>Note: OperatorContext uses ThreadLocal pooling, so multiple acquire() calls
 * in the same thread return the same instance. Tests must compute costs immediately
 * after configuring each context.</p>
 */
public class SimpleCostModelTest {

  private SimpleCostModel costModel;

  @Before
  public void setUp() {
    costModel = new SimpleCostModel();
  }

  // Basic Operations

  @Test
  public void testScanCostScales() {
    // Compute costs immediately - acquire() returns same pooled instance
    double costLow = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(10));
    double costHigh = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(1000));

    assertTrue("Higher cardinality should have higher cost", costHigh > costLow);
  }

  @Test
  public void testIndexScanCheaper() {
    double fullCost = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forScan(10000));
    double indexCost = costModel.estimateCost(XQ.ForBind, OperatorContext.acquire().forIndexScan(10000, 100, 1));

    assertTrue("Index scan should be cheaper than full scan for selective queries", indexCost < fullCost);
  }

  @Test
  public void testSelectionCost() {
    double cost = costModel.estimateCost(XQ.Selection, OperatorContext.acquire().forSelection(1000, 0.1));

    assertTrue("Selection cost should be positive", cost > 0.0);
  }

  @Test
  public void testJoinCost() {
    double cost = costModel.estimateCost(XQ.Join, OperatorContext.acquire().forJoin(100, 1000, 0.01));

    assertTrue("Join cost should be positive", cost > 0.0);
  }

  @Test
  public void testLetBindZeroCost() {
    double cost = costModel.estimateCost(XQ.LetBind, OperatorContext.acquire().forScan(100));

    assertEquals("LetBind should have zero cost", 0.0, cost, 0.0);
  }

  @Test
  public void testSortCost() {
    double cost10 = costModel.estimateCost(XQ.OrderBy, OperatorContext.acquire().forSort(10));
    double cost1000 = costModel.estimateCost(XQ.OrderBy, OperatorContext.acquire().forSort(1000));

    // n log n growth: 1000 * log(1000) / (10 * log(10)) ≈ 300
    double ratio = cost1000 / cost10;
    assertTrue("Sort cost should grow faster than linear, ratio=" + ratio, ratio > 100 && ratio < 500);
  }

  // Deref Operations

  @Test
  public void testDerefCostScalesWithDepth() {
    double costDepth1 = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 1, false));
    double costDepth3 = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 3, false));

    assertTrue("Deeper deref should cost more", costDepth3 > costDepth1);
  }

  @Test
  public void testDescendantDerefMoreExpensive() {
    double costDirect = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(100, 1, false));
    double costDescendant = costModel.estimateCost(XQ.DerefDescendantExpr,
                                                   OperatorContext.acquire().forDeref(100, 1, true));

    assertTrue("Descendant deref should be more expensive", costDescendant > costDirect);
  }

  @Test
  public void testDerefCostScalesWithCardinality() {
    double costLow = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(10, 1, false));
    double costHigh = costModel.estimateCost(XQ.DerefExpr, OperatorContext.acquire().forDeref(1000, 1, false));

    assertTrue("Higher cardinality should have higher cost", costHigh > costLow);
  }

  // Array Operations

  @Test
  public void testSingleAccessCheaper() {
    double costSingle = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 10, true));
    double costFull = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 10, false));

    assertTrue("Single element access should be cheaper", costSingle < costFull);
  }

  @Test
  public void testUnboxingCostScalesWithArraySize() {
    double costSmall = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 5, false));
    double costLarge = costModel.estimateCost(XQ.ArrayAccess, OperatorContext.acquire().forArrayAccess(100, 50, false));

    assertTrue("Larger arrays should cost more to unbox", costLarge > costSmall);
  }

  @Test
  public void testFlattenedFieldCost() {
    double cost = costModel.estimateCost(XQ.FlattenedField, OperatorContext.acquire().forFlattenedField(100, 10, 1));

    assertTrue("Flattened field should have positive cost", cost > 0.0);
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

    assertTrue("Larger slice should cost more", costLarge > costSmall);
  }

  // Cost Clamping

  @Test
  public void testNegativeCostClamped() {
    double clamped = costModel.clampCost(-10.0);
    assertEquals("Negative cost should be clamped to 0", 0.0, clamped, 0.0);
  }

  @Test
  public void testNaNCostClamped() {
    double clamped = costModel.clampCost(Double.NaN);
    assertEquals("NaN cost should be clamped to 0", 0.0, clamped, 0.0);
  }

  @Test
  public void testInfiniteCostClamped() {
    double clamped = costModel.clampCost(Double.POSITIVE_INFINITY);
    assertTrue("Infinite cost should be clamped to finite value", Double.isFinite(clamped));
  }

  @Test
  public void testNormalCostUnchanged() {
    double cost = 100.0;
    double clamped = costModel.clampCost(cost);
    assertEquals("Normal cost should be unchanged", cost, clamped, 0.0);
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
    assertTrue("Custom cost should be 2x default", Math.abs(customCost - 2 * defaultCost) < 0.001);
  }
}
