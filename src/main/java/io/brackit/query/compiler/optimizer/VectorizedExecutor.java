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
 *
 * <p>Every entry point that touches record fields takes a
 * {@code sourcePath} prefix that the optimizer extracted from the loop
 * variable's source expression (see {@link VectorizedScanAnnotation#SOURCE_PATH_PREFIX}).
 * Executors combine the prefix with per-predicate / per-aggregate field
 * names to obtain a fully-qualified query path and — when the target
 * document carries a path summary — resolve that to a concrete
 * implementation-specific path identifier (e.g. Sirix's {@code pathNodeKey}).
 * The prefix is {@code null} when the source expression is not a simple
 * path; implementations must then either fall back to a path-agnostic
 * tree-walk or return {@code null} to signal the caller should use the
 * generic pipeline.
 *
 * <p>Implementations:
 * <ul>
 * <li>bjq: {@code ParallelGroupByExec} — mmap + parallel chunk scan on JSON files</li>
 * <li>SirixDB: {@code SirixVectorizedExecutor} — parallel page scan with path-scoped evaluation</li>
 * </ul>
 */
public interface VectorizedExecutor {

  /** Execute a vectorized group-by-count query. */
  Sequence executeGroupByCount(QueryContext ctx, String[] sourcePath, String groupField) throws QueryException;

  /**
   * Execute a vectorized sorted scan.
   * Default: not supported (returns null → falls back to Volcano).
   */
  default Sequence executeSortedScan(QueryContext ctx, String[] sourcePath, String orderField, String direction)
      throws QueryException {
    return null;
  }

  /**
   * Execute a vectorized pure aggregate (no group-by): sum, avg, min, max, count.
   * Default: not supported (returns null → falls back to Volcano).
   *
   * @param func  one of {@code "sum"}, {@code "avg"}, {@code "min"}, {@code "max"}, {@code "count"}
   * @param field the numeric field to aggregate (ignored for {@code "count"})
   */
  default Sequence executeAggregate(QueryContext ctx, String[] sourcePath, String func, String field)
      throws QueryException {
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
  default Sequence executeCountDistinct(QueryContext ctx, String[] sourcePath, String field) throws QueryException {
    return null;
  }

  /**
   * Generic predicate-tree count. Evaluates the {@link PredicateNode} tree
   * against every record reached by {@code sourcePath} and returns the number
   * of tuples where it holds.
   *
   * <p>Umbra / DuckDB / ClickHouse / Velox model: one physical Filter
   * operator, arbitrary predicates. Implementations that don't yet support
   * the generic path should return {@code null} so the caller falls back to
   * the generic Volcano pipeline.
   *
   * @return scalar {@code Int64(count)} Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateCount(QueryContext ctx, String[] sourcePath, PredicateNode predicate)
      throws QueryException {
    return null;
  }

  /**
   * Generic predicate-tree group-by-count: evaluate {@code predicate} against
   * records reached via {@code sourcePath}; for each distinct value of
   * {@code groupField} among the matches, emit the group key + count.
   *
   * @return grouped-count Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateGroupByCount(QueryContext ctx, String[] sourcePath, PredicateNode predicate,
      String groupField) throws QueryException {
    return null;
  }

  /**
   * Generic predicate-tree aggregate: evaluate {@code predicate} over records
   * reached via {@code sourcePath}, then apply {@code func} (sum/avg/min/max/count)
   * to {@code field} over matching rows. Replaces the filter-then-aggregate
   * composition with a single scan.
   *
   * @return scalar-aggregate Sequence, or {@code null} if unsupported
   */
  default Sequence executePredicateAggregate(QueryContext ctx, String[] sourcePath, PredicateNode predicate,
      String func, String field) throws QueryException {
    return null;
  }

  /** Check if this executor can handle the current query context. */
  boolean canExecute(QueryContext ctx);
}
