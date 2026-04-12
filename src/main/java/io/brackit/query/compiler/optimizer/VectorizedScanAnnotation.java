/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

/**
 * AST annotation keys for vectorized scan optimization.
 * <p>
 * When the optimizer detects a pattern eligible for vectorized execution
 * (e.g., collection scan + group-by), it sets these properties on the
 * PipeExpr AST node. The translator checks for these annotations and
 * delegates to the configured {@link VectorizedExecutor} instead of
 * building a Volcano operator tree.
 * <p>
 * This enables both bjq (JSON file scan) and SirixDB (page scan) to
 * share the same pattern detection logic but use different physical
 * operators.
 */
public final class VectorizedScanAnnotation {

  /** Set to {@code true} on PipeExpr when vectorized group-by is applicable. */
  public static final String VECTORIZED_GROUPBY = "VECTORIZED_GROUPBY";

  /** The field name to group by (String). */
  public static final String GROUPBY_FIELD = "VECTORIZED_GROUPBY_FIELD";

  /** The filter field name (String), if a WHERE clause is present. */
  public static final String FILTER_FIELD = "VECTORIZED_FILTER_FIELD";

  /** The filter operator ("gt", "lt", "ge", "le", "eq"). */
  public static final String FILTER_OP = "VECTORIZED_FILTER_OP";

  /** The filter threshold value (Long). */
  public static final String FILTER_VALUE = "VECTORIZED_FILTER_VALUE";

  /** Set to {@code true} when the query is a simple count (no group-by). */
  public static final String VECTORIZED_COUNT = "VECTORIZED_COUNT";

  private VectorizedScanAnnotation() {
  }
}
