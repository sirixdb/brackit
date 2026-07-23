/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.expr;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.compiler.optimizer.SourceRef;
import io.brackit.query.compiler.optimizer.VectorizedExecutor;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.Tuple;
import io.brackit.query.util.ExprUtil;

/**
 * Per-evaluation switch between a vectorized expression and its generic (always-correct)
 * equivalent, for scans whose source is a {@link SourceRef.Kind#VARIABLE} — a variable with no
 * binding inside the query tree, typically {@code declare variable $doc external} bound through
 * the {@link QueryContext} at execution time.
 *
 * <p>Such a source is unverifiable when the translator must choose an expression (compile time),
 * but fully verifiable once a context is in hand: the executor resolves the variable's actual
 * binding via {@link VectorizedExecutor#acceptsSource(SourceRef, QueryContext)} and serves only
 * a scan over its own bound resource/revision. A foreign or unresolvable binding evaluates the
 * generic expression instead — same result, generic cost — so the vectorized fast path can never
 * change an answer, only its speed (the same fail-closed contract as the compile-time gate).
 */
public final class RuntimeSourceGatedExpr implements Expr {

  private final VectorizedExecutor executor;
  private final SourceRef sourceRef;
  private final Expr vectorized;
  private final Expr generic;

  public RuntimeSourceGatedExpr(final VectorizedExecutor executor, final SourceRef sourceRef, final Expr vectorized,
      final Expr generic) {
    if (executor == null || sourceRef == null || vectorized == null || generic == null) {
      throw new IllegalArgumentException("executor, sourceRef, vectorized and generic must not be null");
    }
    this.executor = executor;
    this.sourceRef = sourceRef;
    this.vectorized = vectorized;
    this.generic = generic;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) throws QueryException {
    return executor.acceptsSource(sourceRef, ctx) ? vectorized.evaluate(ctx, tuple) : generic.evaluate(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(final QueryContext ctx, final Tuple tuple) throws QueryException {
    return executor.acceptsSource(sourceRef, ctx)
        ? ExprUtil.asItem(vectorized.evaluate(ctx, tuple))
        : generic.evaluateToItem(ctx, tuple);
  }

  @Override
  public boolean isUpdating() {
    // Both branches compile the same query; the generic expression is authoritative.
    return generic.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return generic.isVacuous();
  }

  @Override
  public String toString() {
    return "RuntimeSourceGatedExpr[" + sourceRef + "]";
  }
}
