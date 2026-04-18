/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Sequence;

/**
 * Pluggable interface for vectorized execution of annotated queries.
 * <p>
 * Implementations:
 * <ul>
 * <li>bjq: {@code ParallelGroupByExec} — mmap + parallel chunk scan on JSON files</li>
 * <li>SirixDB: {@code DirectPageScanner} — parallel page scan with bitmap-filtered moveTo</li>
 * </ul>
 */
public interface VectorizedExecutor {

  /** Execute a vectorized group-by-count query. */
  Sequence executeGroupByCount(QueryContext ctx, String groupField) throws QueryException;

  /** Execute a vectorized filtered count query. */
  Sequence executeFilterCount(QueryContext ctx, String filterField, String filterOp, long filterValue)
      throws QueryException;

  /**
   * Execute a vectorized filtered count with two AND-conjoined numeric predicates.
   * Default: runs the first filter via {@link #executeFilterCount} and ignores the
   * second (caller's responsibility to fall back to generic evaluation). Implementations
   * should fuse both predicates into a single scan when possible — for a same-field
   * range ({@code age > 30 AND age < 50}) a single SIMD pass with a range mask
   * eliminates the Brackit post-filter over the first predicate's match set.
   *
   * @return {@code null} to signal the caller should use the generic pipeline
   */
  default Sequence executeFilterCount2(QueryContext ctx, String field1, String op1, long value1, String field2,
      String op2, long value2) throws QueryException {
    return null;
  }

  /**
   * Numeric-predicate filter AND boolean-field "is true" conjunct, e.g.
   * {@code count(for $u in SRC where $u.F OP V and $u.B return $u)}. The walker
   * surfaces the boolean branch via
   * {@link VectorizedScanAnnotation#FILTER_BOOL_FIELD}; without this entry
   * point the boolean conjunct is silently dropped (pre-existing correctness
   * bug for every executor using the old defaults). Return {@code null} to
   * signal unsupported.
   */
  default Sequence executeFilterCountAndBool(QueryContext ctx, String filterField, String filterOp, long filterValue,
      String boolField) throws QueryException {
    return null;
  }

  /**
   * Execute a vectorized filtered group-by query.
   *
   * <p>Returns {@code null} to signal unsupported — the caller raises a
   * "not supported by this executor" {@link QueryException}, matching the
   * contract of the other optional methods here. The prior default silently
   * dropped the filter and delegated to {@link #executeGroupByCount}, which
   * produced <i>incorrect results</i> (unfiltered counts) rather than a
   * failure — a correctness bug for any executor relying on the default.
   */
  default Sequence executeFilteredGroupByCount(QueryContext ctx, String groupField, String filterField, String filterOp,
      long filterValue) throws QueryException {
    return null;
  }

  /**
   * Execute a vectorized sorted scan.
   * Default: not supported (returns null → falls back to Volcano).
   */
  default Sequence executeSortedScan(QueryContext ctx, String orderField, String direction) throws QueryException {
    return null;
  }

  /**
   * Execute a vectorized pure aggregate (no group-by): sum, avg, min, max, count.
   * Default: not supported (returns null → falls back to Volcano).
   *
   * @param func  one of {@code "sum"}, {@code "avg"}, {@code "min"}, {@code "max"}, {@code "count"}
   * @param field the numeric field to aggregate (ignored for {@code "count"})
   */
  default Sequence executeAggregate(QueryContext ctx, String func, String field) throws QueryException {
    return null;
  }

  /**
   * Execute a vectorized count-distinct over a single field, i.e. the query shape
   * {@code count(for $u in SRC let $d := $u.F group by $d return $d)}.
   * Implementations that maintain cardinality sketches (HLL, etc.) can answer this
   * in microseconds without a full scan.
   *
   * <p>Default: not supported (returns {@code null} → walker/compiler falls back to
   * the regular group-by-count expression, whose {@link Sequence} length gives the
   * correct answer).
   *
   * @param field the field's local name to count distinct values of
   */
  default Sequence executeCountDistinct(QueryContext ctx, String field) throws QueryException {
    return null;
  }

  /** Check if this executor can handle the current query context. */
  boolean canExecute(QueryContext ctx);
}
