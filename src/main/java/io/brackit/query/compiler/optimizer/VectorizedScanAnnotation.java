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
  /** Top-N pattern (order-by + slice). */
  public static final String VECTORIZED_TOPN = "VECTORIZED_TOPN";

  // ---- Group-by ----
  /** Field name to group by (String). */
  public static final String GROUPBY_FIELD = "VECTORIZED_GROUPBY_FIELD";
  /** Additional group-by fields for multi-key grouping (String[]). */
  public static final String GROUPBY_FIELDS_EXTRA = "VECTORIZED_GROUPBY_FIELDS_EXTRA";

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

  /** Count-distinct target field (local-name String). */
  public static final String COUNT_DISTINCT_FIELD = "VECTORIZED_COUNT_DISTINCT_FIELD";

  // ---- Top-N ----
  /** Number of results to return (Long). */
  public static final String TOPN_LIMIT = "VECTORIZED_TOPN_LIMIT";

  private VectorizedScanAnnotation() {
  }
}
