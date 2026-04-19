/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import io.brackit.query.QueryContext;
import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.CompileChain;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.translator.SequentialPipelineStrategy;
import io.brackit.query.expr.VectorizedGroupByExpr;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Sequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test: compile the real bench countDistinct query through
 * {@link CompileChain} and verify that the optimized AST carries the
 * {@code VECTORIZED_COUNT_DISTINCT} annotation on the inner PipeExpr that
 * is the argument of the outer {@code count(...)} call.
 *
 * <p>The regression we are hunting: the walker's synthetic-AST tests pass,
 * but in the 100M-row bench the countDistinct query still runs at full-scan
 * speed (~33s vs expected sub-ms). Either the real parser produces an AST
 * shape the walker doesn't match, or the optimizer rewrites it out of shape
 * before the walker runs. This test pinpoints which.
 */
class CountDistinctDispatchTest {

  private static final String COUNT_DISTINCT_QUERY =
      "declare variable $doc external; count(for $u in $doc[] let $d := $u.dept group by $d return $d)";

  @Test
  void optimizedAstCarriesCountDistinctAnnotationOnCountArg() throws Exception {
    final CompileChain chain = new CompileChain();
    chain.compile(COUNT_DISTINCT_QUERY);
    final AST optimized = chain.getOptimizedAST();
    assertNotNull(optimized, "optimized AST must be captured");

    final AST countCall = findCountFunctionCall(optimized);
    assertNotNull(countCall, "expected to find a count(...) FunctionCall in optimized AST");

    assertEquals(1, countCall.getChildCount(), "count must have a single argument");
    final AST arg = countCall.getChild(0);

    System.out.println("count() arg node type = " + XQ.NAMES[arg.getType()]);
    System.out.println("count() arg children = " + arg.getChildCount());
    for (int i = 0; i < arg.getChildCount(); i++) {
      System.out.println("  child[" + i + "] = " + XQ.NAMES[arg.getChild(i).getType()]);
    }

    // If the argument is a PipeExpr, we expect the walker to have annotated it.
    if (arg.getType() == XQ.PipeExpr) {
      System.out.println("VECTORIZED_COUNT_DISTINCT = " + arg.getProperty(
                                                                          VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT));
      System.out.println("COUNT_DISTINCT_FIELD = " + arg.getProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD));
      System.out.println("VECTORIZED_GROUPBY = " + arg.getProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY));
      System.out.println("GROUPBY_FIELD = " + arg.getProperty(VectorizedScanAnnotation.GROUPBY_FIELD));
      assertEquals(Boolean.TRUE,
                   arg.getProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT),
                   "inner PipeExpr should carry VECTORIZED_COUNT_DISTINCT");
      assertEquals("dept", arg.getProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD));
    } else {
      // Dump what we actually got for triage.
      throw new AssertionError("count()'s argument is NOT a PipeExpr (got " + XQ.NAMES[arg.getType()] + "); "
          + "the Compiler's count-interception cannot fire. "
          + "Optimizer must lower this FLWOR to a PipeExpr before VectorizedGroupByDetection runs.");
    }
  }

  /**
   * Guard against the dispatch ordering regression: the walker annotates both
   * VECTORIZED_GROUPBY and VECTORIZED_COUNT_DISTINCT on the same pipe. When the
   * outer expression is {@code count(...)}, the count-distinct branch must win;
   * otherwise the group-by branch short-circuits before count-distinct is
   * checked and we fall back to a full materializing scan.
   */
  @Test
  void countDistinctTakesPrecedenceOverGroupByWhenCountWrapped() {
    // Install a stub executor so tryVectorizedExpr doesn't early-return null.
    SequentialPipelineStrategy.setVectorizedExecutor(new StubExecutor());
    try {
      // Build a PipeExpr with BOTH annotations the walker sets for
      //   count(for $u let $d := $u.dept group by $d return $d)
      final AST pipe = new AST(XQ.PipeExpr);
      pipe.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
      pipe.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, "dept");
      pipe.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT, Boolean.TRUE);
      pipe.setProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD, "dept");

      // countWrapped = true → must select COUNT_DISTINCT.
      final Expr wrapped = SequentialPipelineStrategy.tryVectorizedExpr(pipe, true);
      assertNotNull(wrapped, "count-wrapped dispatch must not return null");
      assertTrue(wrapped instanceof VectorizedGroupByExpr,
                 "expected VectorizedGroupByExpr, got " + wrapped.getClass().getSimpleName());
      assertEquals(VectorizedGroupByExpr.Mode.COUNT_DISTINCT,
                   ((VectorizedGroupByExpr) wrapped).getMode(),
                   "count-wrapped pipe with both VECTORIZED_GROUPBY and VECTORIZED_COUNT_DISTINCT must dispatch to COUNT_DISTINCT");

      // countWrapped = false → HLL sketch can't produce a group sequence, so
      // we must fall through to the materializing group-by path.
      final Expr unwrapped = SequentialPipelineStrategy.tryVectorizedExpr(pipe, false);
      assertNotNull(unwrapped);
      assertTrue(unwrapped instanceof VectorizedGroupByExpr);
      assertEquals(VectorizedGroupByExpr.Mode.GROUP_BY,
                   ((VectorizedGroupByExpr) unwrapped).getMode(),
                   "un-counted dispatch must stay on GROUP_BY — HLL can't materialize groups");
    } finally {
      SequentialPipelineStrategy.setVectorizedExecutor(null);
    }
  }

  @AfterEach
  void clearExecutor() {
    SequentialPipelineStrategy.setVectorizedExecutor(null);
  }

  /** Minimal VectorizedExecutor that reports it can handle any context. */
  private static final class StubExecutor implements VectorizedExecutor {
    @Override
    public boolean canExecute(QueryContext ctx) {
      return true;
    }

    @Override
    public Sequence executeGroupByCount(QueryContext ctx, String[] sourcePath, String groupField) {
      return null;
    }

    @Override
    public Sequence executeSortedScan(QueryContext ctx, String[] sourcePath, String field, String direction) {
      return null;
    }

    @Override
    public Sequence executeAggregate(QueryContext ctx, String[] sourcePath, String func, String field) {
      return null;
    }

    @Override
    public Sequence executeCountDistinct(QueryContext ctx, String[] sourcePath, String field) {
      return null;
    }
  }

  private static AST findCountFunctionCall(final AST node) {
    if (node == null) {
      return null;
    }
    if (node.getType() == XQ.FunctionCall) {
      final Object value = node.getValue();
      if (value instanceof QNm qnm && "count".equals(qnm.getLocalName())) {
        return node;
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      final AST found = findCountFunctionCall(node.getChild(i));
      if (found != null) {
        return found;
      }
    }
    return null;
  }
}
