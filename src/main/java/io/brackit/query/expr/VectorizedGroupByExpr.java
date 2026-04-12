/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.expr;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * Expression that delegates to a {@link VectorizedExecutor} for
 * parallel scan group-by execution. Replaces the Volcano pipeline
 * entirely when the optimizer detects an eligible pattern.
 */
public final class VectorizedGroupByExpr implements Expr {

  private final VectorizedExecutor executor;
  private final String groupField;

  public VectorizedGroupByExpr(VectorizedExecutor executor, String groupField) {
    this.executor = executor;
    this.groupField = groupField;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    if (executor.canExecute(ctx)) {
      return executor.executeGroupByCount(ctx, groupField);
    }
    // Executor can't handle this context — shouldn't happen if optimizer annotated correctly
    throw new QueryException(io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR,
                             "Vectorized executor cannot handle this query context");
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    Sequence result = evaluate(ctx, tuple);
    return (result instanceof Item item) ? item : null;
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
