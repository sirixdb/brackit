/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

/**
 * AST annotation keys for vectorized scan optimization.
 * Set by the optimizer, checked by the translator.
 */
public final class VectorizedScanAnnotation {

  // ---- Pattern flags ----
  /** Group-by detected. */
  public static final String VECTORIZED_GROUPBY = "VECTORIZED_GROUPBY";
  /** Filtered count detected (no group-by). */
  public static final String VECTORIZED_COUNT = "VECTORIZED_COUNT";
  /** Order-by detected. */
  public static final String VECTORIZED_ORDERBY = "VECTORIZED_ORDERBY";
  /** Pure aggregation (sum/avg/min/max) without group-by. */
  public static final String VECTORIZED_AGGREGATE = "VECTORIZED_AGGREGATE";
  /** Count-distinct over a single field (count(for ... group by $d return $d)). */
  public static final String VECTORIZED_COUNT_DISTINCT = "VECTORIZED_COUNT_DISTINCT";

  // ---- Group-by ----
  /** Field name to group by (String) — canonical single-key claim. */
  public static final String GROUPBY_FIELD = "VECTORIZED_GROUPBY_FIELD";

  /**
   * Generalized (multi-key and/or renamed-output) group-by detected (Boolean).
   * Set whenever the return clause is the generalized canonical shape
   * {@code {name1: $k1, ..., nameM: $kM, countName: count($loop)}} for let-bound
   * direct-deref keys matching the GroupBySpec exactly. Dispatch is gated on
   * {@link VectorizedExecutor#supportsMultiKeyGroupBy()}.
   */
  public static final String VECTORIZED_GROUPBY_MULTI = "VECTORIZED_GROUPBY_MULTI";
  /** Group-by source field names in RETURN-clause order (String[]). */
  public static final String GROUPBY_FIELDS = "VECTORIZED_GROUPBY_FIELDS";
  /** Output object field names for the group keys, aligned with GROUPBY_FIELDS (String[]). */
  public static final String GROUPBY_OUT_NAMES = "VECTORIZED_GROUPBY_OUT_NAMES";
  /** Output object field name for the per-group count (String). */
  public static final String GROUPBY_COUNT_NAME = "VECTORIZED_GROUPBY_COUNT_NAME";

  // ---- Filter ----
  /**
   * Generic predicate-tree representation of the WHERE clause. Value is a
   * {@link PredicateNode}. The dispatcher routes to
   * {@link VectorizedExecutor#executePredicateCount} /
   * {@link VectorizedExecutor#executePredicateGroupByCount} /
   * {@link VectorizedExecutor#executePredicateAggregate}, which evaluates the
   * arbitrary tree against record batches.
   *
   * <p>This mirrors the Umbra / DuckDB / ClickHouse / Velox model: a single
   * physical Filter operator takes an arbitrary predicate expression rather
   * than having a combinatorial explosion of filter-shape-specific operators.
   */
  public static final String PREDICATE_TREE = "VECTORIZED_PREDICATE_TREE";

  /**
   * Path prefix for the loop variable's source expression. Value is a
   * {@code String[]} whose elements are step names, with {@code "[]"} marking
   * array descent. Examples:
   * <ul>
   * <li>{@code for $u in $doc[] where ...} → {@code ["[]"]}
   * <li>{@code for $u in $doc.items[] where ...} → {@code ["items", "[]"]}
   * <li>{@code for $u in $doc[].items[] where ...} → {@code ["[]", "items", "[]"]}
   * <li>{@code for $u in $doc.items where ...} → {@code ["items"]}
   * </ul>
   *
   * <p>Executors combine this prefix with a per-predicate field name to obtain
   * the full query path, which they then resolve against the document's path
   * summary to a concrete path identifier (e.g. Sirix's pathNodeKey). That
   * enables path-scoped aggregate / group-by / filter correctness without
   * double-counting fields that share a local name at different tree depths.
   *
   * <p>Absent (property unset) for un-representable source expressions — the
   * executor falls back to a tree-walk that doesn't require the prefix.
   */
  public static final String SOURCE_PATH_PREFIX = "VECTORIZED_SOURCE_PATH_PREFIX";

  /**
   * Identity of the document the scan reads from. Value is a {@link SourceRef}. Where
   * {@link #SOURCE_PATH_PREFIX} captures <em>which path</em> a scan walks, this captures <em>which
   * document</em> it dereferences — the concrete {@code jn:doc}/{@code jn:open} resource/revision, the
   * query's context item, or {@link SourceRef.Kind#UNKNOWN} when identity can't be proven.
   *
   * <p>Set on every vectorizable {@code PipeExpr} the walker annotates. The translator hands it to
   * {@link VectorizedExecutor#acceptsSource(SourceRef)} so a resource-bound executor can decline a scan
   * over a document it is not bound to — falling back to the generic pipeline rather than answering with
   * the wrong resource's data. Executors that are not resource-bound (the default) accept every source.
   */
  public static final String SOURCE_REF = "VECTORIZED_SOURCE_REF";

  // ---- Order-by ----
  /** Order field name (String). */
  public static final String ORDER_FIELD = "VECTORIZED_ORDER_FIELD";
  /** Order direction: "ascending" or "descending". */
  public static final String ORDER_DIRECTION = "VECTORIZED_ORDER_DIRECTION";

  // ---- Aggregation ----
  /** Aggregate function name: "count", "sum", "avg", "min", "max". */
  public static final String AGGREGATE_FUNC = "VECTORIZED_AGGREGATE_FUNC";
  /** Aggregate field name (String). */
  public static final String AGGREGATE_FIELD = "VECTORIZED_AGGREGATE_FIELD";

  /**
   * The two operand fields and the operator of an aggregate over an ARITHMETIC return expression —
   * {@code sum(for $m in ... return $m.a * $m.b)} — as {@code {left, op, right}}, where {@code op}
   * is one of {@code "*"}, {@code "+"}, {@code "-"}.
   *
   * <p>Set INSTEAD of {@link #AGGREGATE_FIELD}, never beside it: the two say different things about
   * what the executor must compute, and a backend reading the wrong one aggregates a single column
   * where the query asked for a product. A backend that does not implement the shape declines via
   * {@code VectorizedExecutor#supportsBinaryAggregate} and the generic pipeline answers.
   */
  public static final String AGGREGATE_BINARY = "VECTORIZED_AGGREGATE_BINARY";

  /** Count-distinct target field (local-name String). */
  public static final String COUNT_DISTINCT_FIELD = "VECTORIZED_COUNT_DISTINCT_FIELD";

  private VectorizedScanAnnotation() {
  }
}
