package io.brackit.query.expr;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * Runs a vectorized expression and falls back to the generic one when it answers {@code null}.
 *
 * <p>Most vectorized substitutions decide at COMPILE time whether a backend can serve them, which
 * is why the dispatcher normally treats a {@code null} at evaluate time as an error: there is no
 * generic pipeline left by then. One shape cannot be decided that early. Whether a columnar store
 * covers BOTH operand columns of {@code sum($m.a * $m.b)} depends on the document the query is
 * finally run against — an externally bound variable resolves per evaluation, and a commit can
 * invalidate the store between two runs of one compiled query.
 *
 * <p>So that shape keeps its generic compilation and chooses per evaluation. {@code null} means
 * "not served" and nothing else: an empty aggregate is an empty SEQUENCE, never null.
 */
public final class FallbackOnNullExpr implements Expr {

  private final Expr vectorized;
  private final Expr generic;

  public FallbackOnNullExpr(final Expr vectorized, final Expr generic) {
    if (vectorized == null || generic == null) {
      throw new IllegalArgumentException("both expressions are required");
    }
    this.vectorized = vectorized;
    this.generic = generic;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) throws QueryException {
    final Sequence served = vectorized.evaluate(ctx, tuple);
    return served != null ? served : generic.evaluate(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(final QueryContext ctx, final Tuple tuple) throws QueryException {
    final Item served = vectorized.evaluateToItem(ctx, tuple);
    return served != null ? served : generic.evaluateToItem(ctx, tuple);
  }

  @Override
  public boolean isUpdating() {
    return vectorized.isUpdating() || generic.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return vectorized.isVacuous() && generic.isVacuous();
  }
}
