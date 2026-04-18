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
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * Expression that delegates to a {@link VectorizedExecutor} for
 * parallel scan execution. Replaces the Volcano/Block pipeline
 * when the optimizer detects an eligible pattern.
 * <p>
 * Supports: group-by, filtered count, filtered group-by, sorted scan.
 */
public final class VectorizedGroupByExpr implements Expr {

  public enum Mode {
    GROUP_BY, FILTER_COUNT, FILTER_COUNT2, FILTERED_GROUP_BY, SORTED_SCAN, AGGREGATE, COUNT_DISTINCT,
    /** Numeric filter AND boolean-field "is true" conjunct. */
    FILTER_COUNT_AND_BOOL
  }

  private final VectorizedExecutor executor;
  private final Mode mode;
  private final String groupField;
  private final String filterField;
  private final String filterOp;
  private final long filterValue;
  private String filter2Field;
  private String filter2Op;
  private long filter2Value;
  private String boolFilterField;
  private final String orderField;
  private final String orderDirection;
  private final String aggregateFunc;
  private final String aggregateField;

  /** Group-by only. */
  public VectorizedGroupByExpr(VectorizedExecutor executor, String groupField) {
    this(executor, Mode.GROUP_BY, groupField, null, null, 0, null, null, null, null);
  }

  /** Filtered count. */
  public VectorizedGroupByExpr(VectorizedExecutor executor, String filterField, String filterOp, long filterValue) {
    this(executor, Mode.FILTER_COUNT, null, filterField, filterOp, filterValue, null, null, null, null);
  }

  /** Filtered group-by. */
  public VectorizedGroupByExpr(VectorizedExecutor executor, String groupField, String filterField, String filterOp,
      long filterValue) {
    this(executor, Mode.FILTERED_GROUP_BY, groupField, filterField, filterOp, filterValue, null, null, null, null);
  }

  /** Sorted scan. */
  public static VectorizedGroupByExpr sorted(VectorizedExecutor executor, String orderField, String direction) {
    return new VectorizedGroupByExpr(executor,
                                     Mode.SORTED_SCAN,
                                     null,
                                     null,
                                     null,
                                     0,
                                     orderField,
                                     direction,
                                     null,
                                     null);
  }

  /** Pure aggregate (no group-by): sum/avg/min/max/count. */
  public static VectorizedGroupByExpr aggregate(VectorizedExecutor executor, String func, String field) {
    return new VectorizedGroupByExpr(executor, Mode.AGGREGATE, null, null, null, 0, null, null, func, field);
  }

  /**
   * Count distinct values of a single field — matches {@code count(for ... group by
   * $d return $d)}. The {@code field} is the group key's source field name.
   */
  public static VectorizedGroupByExpr countDistinct(VectorizedExecutor executor, String field) {
    return new VectorizedGroupByExpr(executor, Mode.COUNT_DISTINCT, field, null, null, 0, null, null, null, null);
  }

  /**
   * Filter-count with two AND-conjoined numeric predicates. Matches
   * {@code count(for $u in SRC where $u.F1 OP1 V1 and $u.F2 OP2 V2 return $u)}
   * and delegates to {@link VectorizedExecutor#executeFilterCount2}.
   */
  public static VectorizedGroupByExpr filterCount2(VectorizedExecutor executor, String field1, String op1, long value1,
      String field2, String op2, long value2) {
    VectorizedGroupByExpr e = new VectorizedGroupByExpr(executor,
                                                        Mode.FILTER_COUNT2,
                                                        null,
                                                        field1,
                                                        op1,
                                                        value1,
                                                        null,
                                                        null,
                                                        null,
                                                        null);
    e.filter2Field = field2;
    e.filter2Op = op2;
    e.filter2Value = value2;
    return e;
  }

  /** Numeric filter AND boolean-field "is true" conjunct. */
  public static VectorizedGroupByExpr filterCountAndBool(VectorizedExecutor executor, String filterField,
      String filterOp, long filterValue, String boolField) {
    VectorizedGroupByExpr e = new VectorizedGroupByExpr(executor,
                                                        Mode.FILTER_COUNT_AND_BOOL,
                                                        null,
                                                        filterField,
                                                        filterOp,
                                                        filterValue,
                                                        null,
                                                        null,
                                                        null,
                                                        null);
    e.boolFilterField = boolField;
    return e;
  }

  private VectorizedGroupByExpr(VectorizedExecutor executor, Mode mode, String groupField, String filterField,
      String filterOp, long filterValue, String orderField, String orderDirection, String aggregateFunc,
      String aggregateField) {
    this.executor = executor;
    this.mode = mode;
    this.groupField = groupField;
    this.filterField = filterField;
    this.filterOp = filterOp;
    this.filterValue = filterValue;
    this.orderField = orderField;
    this.orderDirection = orderDirection;
    this.aggregateFunc = aggregateFunc;
    this.aggregateField = aggregateField;
    this.filter2Field = null;
    this.filter2Op = null;
    this.filter2Value = 0;
  }

  /** Which vectorized path this expression dispatches to — exposed for dispatch-correctness tests. */
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
      case FILTER_COUNT -> executor.executeFilterCount(ctx, filterField, filterOp, filterValue);
      case FILTER_COUNT2 -> {
        Sequence result = executor.executeFilterCount2(ctx,
                                                       filterField,
                                                       filterOp,
                                                       filterValue,
                                                       filter2Field,
                                                       filter2Op,
                                                       filter2Value);
        if (result == null) {
          // Executor doesn't support fused 2-predicate count — fall back to single-pred
          // (caller must apply the 2nd predicate through the generic pipeline).
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
                                   "Vectorized 2-predicate filter count not supported by this executor");
        }
        yield result;
      }
      case FILTERED_GROUP_BY -> {
        Sequence result = executor.executeFilteredGroupByCount(ctx, groupField, filterField, filterOp, filterValue);
        if (result == null) {
          // Matches the contract of FILTER_COUNT2 / SORTED_SCAN / AGGREGATE /
          // COUNT_DISTINCT: null signals "not supported" and fails loud
          // rather than silently returning a wrong (unfiltered) result.
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
                                   "Vectorized filtered group-by-count not supported by this executor");
        }
        yield result;
      }
      case FILTER_COUNT_AND_BOOL -> {
        Sequence result = executor.executeFilterCountAndBool(ctx, filterField, filterOp, filterValue, boolFilterField);
        if (result == null) {
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
                                   "Vectorized filter-count with boolean conjunct not supported by this executor");
        }
        yield result;
      }
      case SORTED_SCAN -> {
        Sequence result = executor.executeSortedScan(ctx, orderField, orderDirection);
        if (result == null) {
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
                                   "Vectorized sorted scan not supported by this executor");
        }
        yield result;
      }
      case AGGREGATE -> {
        Sequence result = executor.executeAggregate(ctx, aggregateFunc, aggregateField);
        if (result == null) {
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR, "Vectorized aggregate not supported by this executor");
        }
        yield result;
      }
      case COUNT_DISTINCT -> {
        Sequence result = executor.executeCountDistinct(ctx, groupField);
        if (result == null) {
          throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
                                   "Vectorized count-distinct not supported by this executor");
        }
        yield result;
      }
    };
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
