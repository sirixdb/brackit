/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.expr;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.compiler.optimizer.PredicateNode;
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * Expression that delegates to a {@link VectorizedExecutor} for parallel scan
 * execution. Replaces the Volcano/Block pipeline when the optimizer detects an
 * eligible pattern.
 *
 * <p>Every vectorized mode carries a {@code sourcePath} prefix lifted from the
 * loop variable's source expression. Executors combine the prefix with
 * per-predicate field names to fully-qualify query paths, enabling
 * path-scoped correctness (no double-counting same-local-name fields at
 * different tree depths). {@code sourcePath} may be {@code null} if the
 * walker couldn't represent the source — executors that require it should
 * return {@code null} from {@code evaluate} to signal fallback.
 *
 * <p>Dispatches one of seven semantically distinct modes:
 * <ul>
 * <li>{@link Mode#GROUP_BY} — unfiltered group-by-count</li>
 * <li>{@link Mode#SORTED_SCAN} — order-by + return</li>
 * <li>{@link Mode#AGGREGATE} — pure sum/avg/min/max/count over a field</li>
 * <li>{@link Mode#COUNT_DISTINCT} — HLL-backed distinct-value cardinality</li>
 * <li>{@link Mode#GENERIC_PREDICATE_COUNT} — arbitrary predicate tree count</li>
 * <li>{@link Mode#GENERIC_PREDICATE_GROUPBY} — filter + group-by</li>
 * <li>{@link Mode#GENERIC_PREDICATE_AGGREGATE} — filter + aggregate</li>
 * </ul>
 */
public final class VectorizedGroupByExpr implements Expr {

  public enum Mode {
    GROUP_BY, SORTED_SCAN, AGGREGATE, COUNT_DISTINCT, GENERIC_PREDICATE_COUNT, GENERIC_PREDICATE_GROUPBY, GENERIC_PREDICATE_AGGREGATE
  }

  private final VectorizedExecutor executor;
  private final Mode mode;
  private final String[] sourcePath;
  private final String groupField;
  private final PredicateNode predicate;
  private final String orderField;
  private final String orderDirection;
  private final String aggregateFunc;
  private final String aggregateField;

  private VectorizedGroupByExpr(VectorizedExecutor executor, Mode mode, String[] sourcePath, String groupField,
      PredicateNode predicate, String orderField, String orderDirection, String aggregateFunc, String aggregateField) {
    this.executor = executor;
    this.mode = mode;
    this.sourcePath = sourcePath;
    this.groupField = groupField;
    this.predicate = predicate;
    this.orderField = orderField;
    this.orderDirection = orderDirection;
    this.aggregateFunc = aggregateFunc;
    this.aggregateField = aggregateField;
  }

  /** Group-by count (no filter). */
  public static VectorizedGroupByExpr groupBy(VectorizedExecutor executor, String[] sourcePath, String groupField) {
    return new VectorizedGroupByExpr(executor, Mode.GROUP_BY, sourcePath, groupField, null, null, null, null, null);
  }

  /** Sorted scan. */
  public static VectorizedGroupByExpr sorted(VectorizedExecutor executor, String[] sourcePath, String orderField,
      String direction) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.SORTED_SCAN,
                                     sourcePath,
                                     null,
                                     null,
                                     orderField,
                                     direction,
                                     null,
                                     null);
  }

  /** Pure aggregate (no group-by): sum/avg/min/max/count. */
  public static VectorizedGroupByExpr aggregate(VectorizedExecutor executor, String[] sourcePath, String func,
      String field) {
    return new VectorizedGroupByExpr(executor, Mode.AGGREGATE, sourcePath, null, null, null, null, func, field);
  }

  /**
   * Count distinct values of a single field — matches
   * {@code count(for ... group by $d return $d)}. {@code field} is the group
   * key's source field name.
   */
  public static VectorizedGroupByExpr countDistinct(VectorizedExecutor executor, String[] sourcePath, String field) {
    return new VectorizedGroupByExpr(executor, Mode.COUNT_DISTINCT, sourcePath, field, null, null, null, null, null);
  }

  /**
   * Generic predicate-tree count. Primary entry point — takes an arbitrary
   * {@link PredicateNode} tree instead of a shape-specific filter tuple.
   */
  public static VectorizedGroupByExpr predicateCount(VectorizedExecutor executor, String[] sourcePath,
      PredicateNode predicate) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.GENERIC_PREDICATE_COUNT,
                                     sourcePath,
                                     null,
                                     predicate,
                                     null,
                                     null,
                                     null,
                                     null);
  }

  /** Generic predicate-tree group-by-count. */
  public static VectorizedGroupByExpr predicateGroupByCount(VectorizedExecutor executor, String[] sourcePath,
      PredicateNode predicate, String groupField) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.GENERIC_PREDICATE_GROUPBY,
                                     sourcePath,
                                     groupField,
                                     predicate,
                                     null,
                                     null,
                                     null,
                                     null);
  }

  /** Generic predicate-tree filtered aggregate. */
  public static VectorizedGroupByExpr predicateAggregate(VectorizedExecutor executor, String[] sourcePath,
      PredicateNode predicate, String func, String field) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.GENERIC_PREDICATE_AGGREGATE,
                                     sourcePath,
                                     null,
                                     predicate,
                                     null,
                                     null,
                                     func,
                                     field);
  }

  /** Which vectorized path this expression dispatches to. Exposed for dispatch-correctness tests. */
  public Mode getMode() {
    return mode;
  }

  /** The loop-variable source-path prefix (may be {@code null}). Exposed for tests. */
  public String[] getSourcePath() {
    return sourcePath;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    if (!executor.canExecute(ctx)) {
      throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR, "Vectorized executor cannot handle this query context");
    }
    return switch (mode) {
      case GROUP_BY -> executor.executeGroupByCount(ctx, sourcePath, groupField);
      case SORTED_SCAN -> requireSupported(executor.executeSortedScan(ctx, sourcePath, orderField, orderDirection),
                                           "sorted scan");
      case AGGREGATE -> requireSupported(executor.executeAggregate(ctx, sourcePath, aggregateFunc, aggregateField),
                                         "aggregate");
      case COUNT_DISTINCT -> requireSupported(executor.executeCountDistinct(ctx, sourcePath, groupField),
                                              "count-distinct");
      case GENERIC_PREDICATE_COUNT -> requireSupported(executor.executePredicateCount(ctx, sourcePath, predicate),
                                                       "generic-predicate count");
      case GENERIC_PREDICATE_GROUPBY -> requireSupported(executor.executePredicateGroupByCount(ctx,
                                                                                               sourcePath,
                                                                                               predicate,
                                                                                               groupField),
                                                         "generic-predicate group-by-count");
      case GENERIC_PREDICATE_AGGREGATE -> requireSupported(executor.executePredicateAggregate(ctx,
                                                                                              sourcePath,
                                                                                              predicate,
                                                                                              aggregateFunc,
                                                                                              aggregateField),
                                                           "generic-predicate aggregate");
    };
  }

  private static Sequence requireSupported(Sequence result, String what) throws QueryException {
    if (result == null) {
      throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR, "Vectorized %s not supported by this executor", what);
    }
    return result;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    Sequence result = evaluate(ctx, tuple);
    return (result instanceof Item item) ? item : null;
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
