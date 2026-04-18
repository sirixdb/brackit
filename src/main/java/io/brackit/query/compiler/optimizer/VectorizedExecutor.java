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

  /**
   * Generic predicate-tree count: evaluate the {@link PredicateNode} against
   * the document and return the number of tuples where it holds. Replaces the
   * combinatorial explosion of {@code executeFilterCount*} variants with a
   * single entry point — the operator walks (or JIT-compiles) the tree.
   *
   * <p>This is the Umbra / DuckDB / ClickHouse / Velox model: one physical
   * Filter operator, arbitrary predicates. Implementations that don't yet
   * support the generic path should return {@code null}; the Brackit
   * dispatcher will then fall back to the legacy shape-specific methods
   * ({@link #executeFilterCount}, {@link #executeFilterCount2}, etc.) so
   * adoption can be gradual.
   *
   * @return scalar {@code Int64(count)} Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateCount(QueryContext ctx, PredicateNode predicate) throws QueryException {
    return null;
  }

  /**
   * Generic predicate-tree group-by-count: evaluate {@code predicate} against
   * the document and, for each distinct value of {@code groupField} among the
   * matches, emit the group key + count. Replaces {@link #executeFilteredGroupByCount}
   * (any predicate shape) and {@link #executeGroupByCount} (the predicate-free
   * case, via {@link PredicateNode.AlwaysTrue}).
   *
   * @return grouped-count Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateGroupByCount(QueryContext ctx, PredicateNode predicate, String groupField)
      throws QueryException {
    return null;
  }

  /**
   * Generic predicate-tree aggregate: evaluate {@code predicate}, then apply
   * {@code func} (sum/avg/min/max/count) to {@code field} over matching rows.
   * Replaces the filter-then-aggregate composition with a single scan.
   *
   * @return scalar-aggregate Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateAggregate(QueryContext ctx, PredicateNode predicate, String func, String field)
      throws QueryException {
    return null;
  }

  /** Check if this executor can handle the current query context. */
  boolean canExecute(QueryContext ctx);
}
