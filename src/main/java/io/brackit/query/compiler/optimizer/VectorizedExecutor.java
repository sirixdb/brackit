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
   * Execute a vectorized filtered group-by query.
   * Default: falls back to non-filtered group-by (ignores filter).
   */
  default Sequence executeFilteredGroupByCount(QueryContext ctx, String groupField, String filterField, String filterOp,
      long filterValue) throws QueryException {
    return executeGroupByCount(ctx, groupField);
  }

  /**
   * Execute a vectorized sorted scan.
   * Default: not supported (returns null → falls back to Volcano).
   */
  default Sequence executeSortedScan(QueryContext ctx, String orderField, String direction) throws QueryException {
    return null;
  }

  /** Check if this executor can handle the current query context. */
  boolean canExecute(QueryContext ctx);
}
