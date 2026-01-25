/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.cost;

import io.brackit.query.compiler.XQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SimpleCostModel}.
 */
class SimpleCostModelTest {

    private SimpleCostModel costModel;

    @BeforeEach
    void setUp() {
        costModel = new SimpleCostModel();
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("Scan cost scales with cardinality")
        void testScanCostScales() {
            OperatorContext ctxLow = OperatorContext.acquire().forScan(10);
            OperatorContext ctxHigh = OperatorContext.acquire().forScan(1000);

            double costLow = costModel.estimateCost(XQ.ForBind, ctxLow);
            double costHigh = costModel.estimateCost(XQ.ForBind, ctxHigh);

            assertTrue(costHigh > costLow, "Higher cardinality should have higher cost");
        }

        @Test
        @DisplayName("Index scan cheaper than full scan for selective queries")
        void testIndexScanCheaper() {
            OperatorContext fullScan = OperatorContext.acquire().forScan(10000);
            OperatorContext indexScan = OperatorContext.acquire().forIndexScan(10000, 100, 1);

            double fullCost = costModel.estimateCost(XQ.ForBind, fullScan);
            double indexCost = costModel.estimateCost(XQ.ForBind, indexScan);

            assertTrue(indexCost < fullCost, "Index scan should be cheaper than full scan for selective queries");
        }

        @Test
        @DisplayName("Selection cost scales with input")
        void testSelectionCost() {
            OperatorContext ctx = OperatorContext.acquire().forSelection(1000, 0.1);

            double cost = costModel.estimateCost(XQ.Selection, ctx);

            assertTrue(cost > 0.0, "Selection cost should be positive");
        }

        @Test
        @DisplayName("Join cost includes build and probe phases")
        void testJoinCost() {
            OperatorContext ctx = OperatorContext.acquire().forJoin(100, 1000, 0.01);

            double cost = costModel.estimateCost(XQ.Join, ctx);

            assertTrue(cost > 0.0, "Join cost should be positive");
        }

        @Test
        @DisplayName("LetBind has zero cost")
        void testLetBindZeroCost() {
            OperatorContext ctx = OperatorContext.acquire().forScan(100);

            double cost = costModel.estimateCost(XQ.LetBind, ctx);

            assertEquals(0.0, cost, "LetBind should have zero cost");
        }

        @Test
        @DisplayName("Sort cost is O(n log n)")
        void testSortCost() {
            OperatorContext ctx10 = OperatorContext.acquire().forSort(10);
            OperatorContext ctx1000 = OperatorContext.acquire().forSort(1000);

            double cost10 = costModel.estimateCost(XQ.OrderBy, ctx10);
            double cost1000 = costModel.estimateCost(XQ.OrderBy, ctx1000);

            // n log n growth: 1000 * log(1000) / (10 * log(10)) ≈ 300
            double ratio = cost1000 / cost10;
            assertTrue(ratio > 100 && ratio < 500, "Sort cost should grow faster than linear");
        }
    }

    @Nested
    @DisplayName("Deref Operations")
    class DerefOperations {

        @Test
        @DisplayName("Deref cost scales with depth")
        void testDerefCostScalesWithDepth() {
            OperatorContext ctxDepth1 = OperatorContext.acquire().forDeref(100, 1, false);
            OperatorContext ctxDepth3 = OperatorContext.acquire().forDeref(100, 3, false);

            double costDepth1 = costModel.estimateCost(XQ.DerefExpr, ctxDepth1);
            double costDepth3 = costModel.estimateCost(XQ.DerefExpr, ctxDepth3);

            assertTrue(costDepth3 > costDepth1, "Deeper deref should cost more");
        }

        @Test
        @DisplayName("Descendant deref more expensive than direct deref")
        void testDescendantDerefMoreExpensive() {
            OperatorContext ctxDirect = OperatorContext.acquire().forDeref(100, 1, false);
            OperatorContext ctxDescendant = OperatorContext.acquire().forDeref(100, 1, true);

            double costDirect = costModel.estimateCost(XQ.DerefExpr, ctxDirect);
            double costDescendant = costModel.estimateCost(XQ.DerefDescendantExpr, ctxDescendant);

            assertTrue(costDescendant > costDirect, "Descendant deref should be more expensive");
        }

        @Test
        @DisplayName("Deref cost scales with cardinality")
        void testDerefCostScalesWithCardinality() {
            OperatorContext ctxLow = OperatorContext.acquire().forDeref(10, 1, false);
            OperatorContext ctxHigh = OperatorContext.acquire().forDeref(1000, 1, false);

            double costLow = costModel.estimateCost(XQ.DerefExpr, ctxLow);
            double costHigh = costModel.estimateCost(XQ.DerefExpr, ctxHigh);

            assertTrue(costHigh > costLow, "Higher cardinality should have higher cost");
        }
    }

    @Nested
    @DisplayName("Array Operations")
    class ArrayOperations {

        @Test
        @DisplayName("Single element access cheaper than full unboxing")
        void testSingleAccessCheaper() {
            OperatorContext ctxSingle = OperatorContext.acquire().forArrayAccess(100, 10, true);
            OperatorContext ctxFull = OperatorContext.acquire().forArrayAccess(100, 10, false);

            double costSingle = costModel.estimateCost(XQ.ArrayAccess, ctxSingle);
            double costFull = costModel.estimateCost(XQ.ArrayAccess, ctxFull);

            assertTrue(costSingle < costFull, "Single element access should be cheaper");
        }

        @Test
        @DisplayName("Array unboxing cost scales with array size")
        void testUnboxingCostScalesWithArraySize() {
            OperatorContext ctxSmall = OperatorContext.acquire().forArrayAccess(100, 5, false);
            OperatorContext ctxLarge = OperatorContext.acquire().forArrayAccess(100, 50, false);

            double costSmall = costModel.estimateCost(XQ.ArrayAccess, ctxSmall);
            double costLarge = costModel.estimateCost(XQ.ArrayAccess, ctxLarge);

            assertTrue(costLarge > costSmall, "Larger arrays should cost more to unbox");
        }

        @Test
        @DisplayName("Flattened field combines array and deref costs")
        void testFlattenedFieldCost() {
            OperatorContext ctx = OperatorContext.acquire().forFlattenedField(100, 10, 1);

            double cost = costModel.estimateCost(XQ.FlattenedField, ctx);

            assertTrue(cost > 0.0, "Flattened field should have positive cost");
        }

        @Test
        @DisplayName("Array slice cost proportional to slice size")
        void testSliceCost() {
            // Small slice
            OperatorContext ctxSmall = OperatorContext.acquire();
            ctxSmall.inputCardinality = 100;
            ctxSmall.outputCardinality = 300; // 3 elements per input
            ctxSmall.isArrayAccess = false;

            // Large slice
            OperatorContext ctxLarge = OperatorContext.acquire();
            ctxLarge.inputCardinality = 100;
            ctxLarge.outputCardinality = 1000; // 10 elements per input
            ctxLarge.isArrayAccess = false;

            double costSmall = costModel.estimateCost(XQ.ArrayIndexSlice, ctxSmall);
            double costLarge = costModel.estimateCost(XQ.ArrayIndexSlice, ctxLarge);

            assertTrue(costLarge > costSmall, "Larger slice should cost more");
        }
    }

    @Nested
    @DisplayName("Cost Clamping")
    class CostClamping {

        @Test
        @DisplayName("Negative costs are clamped to zero")
        void testNegativeCostClamped() {
            double clamped = costModel.clampCost(-10.0);
            assertEquals(0.0, clamped, "Negative cost should be clamped to 0");
        }

        @Test
        @DisplayName("NaN costs are clamped to zero")
        void testNaNCostClamped() {
            double clamped = costModel.clampCost(Double.NaN);
            assertEquals(0.0, clamped, "NaN cost should be clamped to 0");
        }

        @Test
        @DisplayName("Infinite costs are clamped")
        void testInfiniteCostClamped() {
            double clamped = costModel.clampCost(Double.POSITIVE_INFINITY);
            assertTrue(Double.isFinite(clamped), "Infinite cost should be clamped to finite value");
        }

        @Test
        @DisplayName("Normal costs are unchanged")
        void testNormalCostUnchanged() {
            double cost = 100.0;
            double clamped = costModel.clampCost(cost);
            assertEquals(cost, clamped, "Normal cost should be unchanged");
        }
    }

    @Test
    @DisplayName("Custom cost factors are respected")
    void testCustomCostFactors() {
        SimpleCostModel customModel = new SimpleCostModel(
                2.0,  // scanCostPerTuple
                20.0, // indexOverhead
                1.0,  // indexCostPerTuple
                3.0,  // hashBuildFactor
                2.0,  // hashProbeFactor
                0.02, // sortFactor
                0.2   // selectionCost
        );

        OperatorContext ctx = OperatorContext.acquire().forScan(100);

        double defaultCost = costModel.estimateCost(XQ.ForBind, ctx);
        double customCost = customModel.estimateCost(XQ.ForBind, ctx);

        // Custom scan cost is 2x default
        assertTrue(Math.abs(customCost - 2 * defaultCost) < 0.001,
                "Custom cost should be 2x default");
    }
}
