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

  /**
   * Execute a vectorized group-by-count query.
   *
   * <p>RESULT ENVELOPE CONTRACT: the returned {@link Sequence} must be a flat
   * sequence of per-group record items — exactly what the replaced FLWOR's
   * {@code return {"<groupField>": $key, "count": n}} would emit. It must NOT
   * be a single array item wrapping the records: brackit arrays iterate as
   * their members, so counts still come out right, but the serialized result
   * (one {@code [...]} item vs N object items) and positional semantics would
   * silently differ from the generic pipeline.
   */
  Sequence executeGroupByCount(QueryContext ctx, String[] sourcePath, String groupField) throws QueryException;

  /**
   * Whether {@link #executeSortedScan} is actually implemented. The dispatcher
   * substitutes the sorted-scan expression at TRANSLATE time, after which there
   * is no generic pipeline to fall back to — an executor that answers
   * {@code null} at evaluate time turns into a runtime error. Capability is
   * therefore declared up front; the default matches the default
   * {@code executeSortedScan} (unsupported).
   */
  default boolean supportsSortedScan() {
    return false;
  }

  /**
   * Whether {@link #executeGroupByCountMulti} is actually implemented. Same
   * translate-time gating rationale as {@link #supportsSortedScan()}.
   */
  default boolean supportsMultiKeyGroupBy() {
    return false;
  }

  /**
   * Generalized group-by-count: group the records reached via {@code sourcePath}
   * by one or more fields and emit ONE record per distinct key combination,
   * shaped {@code {outNames[0]: key0, ..., outNames[M-1]: keyM-1, countName: n}}.
   * {@code groupFields} and {@code outNames} are aligned and in RETURN-clause
   * order; {@code predicate} may be {@code null} (unfiltered) — when non-null it
   * is evaluated per record before grouping.
   *
   * <p>Key values must keep their original JSON types (a numeric field groups
   * and serializes as numbers, booleans as booleans) — stringifying keys is a
   * wrong result, not a degradation.
   *
   * <p>Same RESULT ENVELOPE CONTRACT as {@link #executeGroupByCount}: a flat
   * sequence of per-group record items, never a single wrapping array; an empty
   * grouping is an EMPTY sequence, {@code null} strictly means "unsupported".
   */
  default Sequence executeGroupByCountMulti(QueryContext ctx, String[] sourcePath, String[] groupFields,
      String[] outNames, String countName, PredicateNode predicate) throws QueryException {
    return null;
  }

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
   * <p>Same RESULT ENVELOPE CONTRACT as {@link #executeGroupByCount}: a flat
   * sequence of per-group record items, never a single wrapping array. A
   * legitimately empty result (predicate matches nothing) is an EMPTY sequence
   * — {@code null} strictly means "unsupported, fall back".
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

  /**
   * Whether this executor may serve a scan over the given source document.
   *
   * <p>{@code sourcePath} tells an executor which path inside a document a scan walks, but never which
   * document. An executor bound to a single physical resource/revision (e.g. SirixDB's
   * {@code SirixVectorizedExecutor}) would otherwise answer a same-shaped query over a <em>different</em>
   * document from its own columns — wrong results. The optimizer therefore lifts the scan's source
   * identity into a {@link SourceRef} (see {@link VectorizedScanAnnotation#SOURCE_REF}) and asks here,
   * at TRANSLATE time, before substituting the vectorized expression.
   *
   * <p>Returning {@code false} is not an error: the translator simply builds the generic (always-correct)
   * pipeline instead, so declining only ever costs the fast path. A resource-bound executor should fail
   * closed — accept {@link SourceRef.Kind#DOCUMENT} refs that match its binding (and the query's
   * {@link SourceRef.Kind#CONTEXT_ITEM}, the caller's own transaction), and decline everything else,
   * including {@link SourceRef.Kind#UNKNOWN}.
   *
   * <p>The default accepts every source — correct for executors that are not bound to one resource
   * (e.g. bjq's file-backed {@code ParallelGroupByExec}), so the added contract is opt-in and does not
   * change their behaviour.
   *
   * @param source the scan's source identity; never {@code null} when the optimizer annotated the scan
   * @return {@code true} to allow vectorized serving of this source, {@code false} to fall back
   */
  default boolean acceptsSource(SourceRef source) {
    return true;
  }

  /**
   * Runtime-capable variant of {@link #acceptsSource(SourceRef)} for gates that run at
   * EVALUATION time, when the {@link QueryContext} — and with it the actual binding of a
   * {@link SourceRef.Kind#VARIABLE} source (an external variable such as
   * {@code declare variable $doc external}) — is available. An executor can resolve
   * {@code source.variableName()} through {@code ctx}, inspect the concrete item actually bound,
   * and accept iff it denotes the executor's own resource/revision. Compile-time consumers keep
   * calling the single-argument overload, which must stay fail-closed for VARIABLE refs.
   *
   * <p>The default delegates to {@link #acceptsSource(SourceRef)}, so existing executors keep
   * their exact behaviour unless they opt in.
   *
   * @param source the scan's source identity; never {@code null} when the optimizer annotated the scan
   * @param ctx    the evaluating query context (carries external-variable bindings)
   * @return {@code true} to allow vectorized serving of this source, {@code false} to fall back
   */
  default boolean acceptsSource(SourceRef source, QueryContext ctx) {
    return acceptsSource(source);
  }
}
