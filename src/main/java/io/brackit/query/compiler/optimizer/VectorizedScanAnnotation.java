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
  /** Top-N pattern (order-by + slice). */
  public static final String VECTORIZED_TOPN = "VECTORIZED_TOPN";

  // ---- Group-by ----
  /** Field name to group by (String). */
  public static final String GROUPBY_FIELD = "VECTORIZED_GROUPBY_FIELD";
  /** Additional group-by fields for multi-key grouping (String[]). */
  public static final String GROUPBY_FIELDS_EXTRA = "VECTORIZED_GROUPBY_FIELDS_EXTRA";

  // ---- Filter ----
  /** Filter field name (String). */
  public static final String FILTER_FIELD = "VECTORIZED_FILTER_FIELD";
  /** Filter operator: "gt", "lt", "ge", "le", "eq". */
  public static final String FILTER_OP = "VECTORIZED_FILTER_OP";
  /** Filter value for numeric comparisons (Long). */
  public static final String FILTER_VALUE = "VECTORIZED_FILTER_VALUE";
  /** Filter value for string equality comparisons (String). */
  public static final String FILTER_STRING_VALUE = "VECTORIZED_FILTER_STRING_VALUE";
  /** Second filter (for AND compound predicates). */
  public static final String FILTER2_FIELD = "VECTORIZED_FILTER2_FIELD";
  public static final String FILTER2_OP = "VECTORIZED_FILTER2_OP";
  public static final String FILTER2_VALUE = "VECTORIZED_FILTER2_VALUE";
  public static final String FILTER2_STRING_VALUE = "VECTORIZED_FILTER2_STRING_VALUE";

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

  // ---- Top-N ----
  /** Number of results to return (Long). */
  public static final String TOPN_LIMIT = "VECTORIZED_TOPN_LIMIT";

  private VectorizedScanAnnotation() {
  }
}
