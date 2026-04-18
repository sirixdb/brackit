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
 * <p>Dispatches one of six semantically distinct modes:
 * <ul>
 * <li>{@link Mode#GROUP_BY} — unfiltered group-by-count</li>
 * <li>{@link Mode#SORTED_SCAN} — order-by + return
 * <li>{@link Mode#AGGREGATE} — pure sum/avg/min/max/count over a field
 * <li>{@link Mode#COUNT_DISTINCT} — HLL-backed distinct-value cardinality
 * <li>{@link Mode#GENERIC_PREDICATE_COUNT} — arbitrary predicate tree count
 * <li>{@link Mode#GENERIC_PREDICATE_GROUPBY} — filter + group-by
 * <li>{@link Mode#GENERIC_PREDICATE_AGGREGATE} — filter + aggregate
 * </ul>
 *
 * <p>The three GENERIC_PREDICATE_* modes replace the historical combinatorial
 * explosion of filter-shape operators (FILTER_COUNT, FILTER_COUNT2,
 * FILTER_COUNT_AND_BOOL, FILTER_COUNT2_AND_BOOL, FILTERED_GROUP_BY). The
 * executor receives an arbitrary {@link PredicateNode} tree and walks it —
 * same design as Umbra / DuckDB / ClickHouse / Velox.
 */
public final class VectorizedGroupByExpr implements Expr {

  public enum Mode {
    GROUP_BY, SORTED_SCAN, AGGREGATE, COUNT_DISTINCT, GENERIC_PREDICATE_COUNT, GENERIC_PREDICATE_GROUPBY, GENERIC_PREDICATE_AGGREGATE
  }

  private final VectorizedExecutor executor;
  private final Mode mode;
  private final String groupField;
  private final PredicateNode predicate;
  private final String orderField;
  private final String orderDirection;
  private final String aggregateFunc;
  private final String aggregateField;

  private VectorizedGroupByExpr(VectorizedExecutor executor, Mode mode, String groupField, PredicateNode predicate,
      String orderField, String orderDirection, String aggregateFunc, String aggregateField) {
    this.executor = executor;
    this.mode = mode;
    this.groupField = groupField;
    this.predicate = predicate;
    this.orderField = orderField;
    this.orderDirection = orderDirection;
    this.aggregateFunc = aggregateFunc;
    this.aggregateField = aggregateField;
  }

  /** Group-by count (no filter). */
  public static VectorizedGroupByExpr groupBy(VectorizedExecutor executor, String groupField) {
    return new VectorizedGroupByExpr(executor, Mode.GROUP_BY, groupField, null, null, null, null, null);
  }

  /** Back-compat constructor: group-by count. */
  public VectorizedGroupByExpr(VectorizedExecutor executor, String groupField) {
    this(executor, Mode.GROUP_BY, groupField, null, null, null, null, null);
  }

  /** Sorted scan. */
  public static VectorizedGroupByExpr sorted(VectorizedExecutor executor, String orderField, String direction) {
    return new VectorizedGroupByExpr(executor, Mode.SORTED_SCAN, null, null, orderField, direction, null, null);
  }

  /** Pure aggregate (no group-by): sum/avg/min/max/count. */
  public static VectorizedGroupByExpr aggregate(VectorizedExecutor executor, String func, String field) {
    return new VectorizedGroupByExpr(executor, Mode.AGGREGATE, null, null, null, null, func, field);
  }

  /**
   * Count distinct values of a single field — matches
   * {@code count(for ... group by $d return $d)}. The {@code field} is the
   * group key's source field name.
   */
  public static VectorizedGroupByExpr countDistinct(VectorizedExecutor executor, String field) {
    return new VectorizedGroupByExpr(executor, Mode.COUNT_DISTINCT, field, null, null, null, null, null);
  }

  /**
   * Generic predicate-tree count. Primary entry point — takes an arbitrary
   * {@link PredicateNode} tree instead of a shape-specific filter tuple.
   */
  public static VectorizedGroupByExpr predicateCount(VectorizedExecutor executor, PredicateNode predicate) {
    return new VectorizedGroupByExpr(executor, Mode.GENERIC_PREDICATE_COUNT, null, predicate, null, null, null, null);
  }

  /** Generic predicate-tree group-by-count. */
  public static VectorizedGroupByExpr predicateGroupByCount(VectorizedExecutor executor, PredicateNode predicate,
      String groupField) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.GENERIC_PREDICATE_GROUPBY,
                                     groupField,
                                     predicate,
                                     null,
                                     null,
                                     null,
                                     null);
  }

  /** Generic predicate-tree filtered aggregate. */
  public static VectorizedGroupByExpr predicateAggregate(VectorizedExecutor executor, PredicateNode predicate,
      String func, String field) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.GENERIC_PREDICATE_AGGREGATE,
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

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    if (!executor.canExecute(ctx)) {
      throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR, "Vectorized executor cannot handle this query context");
    }
    return switch (mode) {
      case GROUP_BY -> executor.executeGroupByCount(ctx, groupField);
      case SORTED_SCAN -> requireSupported(executor.executeSortedScan(ctx, orderField, orderDirection), "sorted scan");
      case AGGREGATE -> requireSupported(executor.executeAggregate(ctx, aggregateFunc, aggregateField), "aggregate");
      case COUNT_DISTINCT -> requireSupported(executor.executeCountDistinct(ctx, groupField), "count-distinct");
      case GENERIC_PREDICATE_COUNT -> requireSupported(executor.executePredicateCount(ctx, predicate),
                                                       "generic-predicate count");
      case GENERIC_PREDICATE_GROUPBY -> requireSupported(executor.executePredicateGroupByCount(ctx,
                                                                                               predicate,
                                                                                               groupField),
                                                         "generic-predicate group-by-count");
      case GENERIC_PREDICATE_AGGREGATE -> requireSupported(executor.executePredicateAggregate(ctx,
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
