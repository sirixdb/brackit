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
 * When the optimizer annotates a PipeExpr with {@link VectorizedScanAnnotation},
 * the translator delegates execution to this interface instead of building
 * a Volcano operator tree.
 * <p>
 * Implementations:
 * <ul>
 * <li>bjq: {@code ParallelGroupByExec} — mmap + parallel chunk scan on JSON files</li>
 * <li>SirixDB: {@code DirectPageScanner} — parallel page scan with bitmap-filtered moveTo</li>
 * </ul>
 */
public interface VectorizedExecutor {

  /**
   * Execute a vectorized group-by-count query.
   *
   * @param ctx        the query context (provides the context item / collection)
   * @param groupField the field name to group by
   * @return the result as a Sequence (typically a DArray of CompactObjects)
   */
  Sequence executeGroupByCount(QueryContext ctx, String groupField) throws QueryException;

  /**
   * Execute a vectorized filtered count query.
   *
   * @param ctx         the query context
   * @param filterField the field name to filter on
   * @param filterOp    comparison operator ("gt", "lt", "ge", "le", "eq")
   * @param filterValue the threshold value
   * @return the count as an Int64
   */
  Sequence executeFilterCount(QueryContext ctx, String filterField, String filterOp, long filterValue)
      throws QueryException;

  /**
   * Check if this executor can handle the current query context.
   * For bjq: checks if the context item is a StreamingArray from a large file.
   * For SirixDB: checks if the context item is a JsonDBCollection.
   *
   * @param ctx the query context
   * @return true if vectorized execution is applicable
   */
  boolean canExecute(QueryContext ctx);
}
