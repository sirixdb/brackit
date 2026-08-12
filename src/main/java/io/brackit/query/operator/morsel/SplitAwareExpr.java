/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.operator.morsel;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.ExprUtil;

/**
 * The leaf scan's bind expression, made split-aware.
 *
 * <p>Wraps the expression a pipeline's bottom-most {@code ForBind} binds — {@code $doc[]} and the
 * like — and answers with the calling thread's assigned split instead, when it has one. Every
 * worker therefore drives the same compiled operator graph over a different part of the source,
 * which is what makes the scan parallel rather than the tuples merely being handed around after a
 * serial scan produced them.
 *
 * <p>The binding is <b>per instance</b>, not a single shared slot, and that is a correctness
 * requirement rather than tidiness. A predicate may itself contain a pipeline, whose leaf is its own
 * {@code SplitAwareExpr}; with one global slot that nested leaf would read the enclosing worker's
 * split and silently scan the wrong thing. Each instance owning its own thread-local makes a
 * binding visible only to the leaf it was meant for.
 *
 * <p>Outside a morsel run — on the consumer thread, and on the fallback path when the source turns
 * out not to be splittable — no binding is present and this is exactly the wrapped expression.
 *
 * @author The SirixDB authors
 */
public final class SplitAwareExpr implements Expr {

  private final Expr delegate;

  /** This leaf's split for the calling thread, or {@code null} when it is not running one. */
  private final ThreadLocal<Sequence> assigned = new ThreadLocal<>();

  public SplitAwareExpr(final Expr delegate) {
    this.delegate = delegate;
  }

  /** The wrapped expression, which {@code MorselPipeExpr} evaluates once to obtain the source. */
  public Expr delegate() {
    return delegate;
  }

  /**
   * Assign this thread's split, returning whatever was assigned before.
   *
   * <p>Callers must restore that previous value rather than merely clearing: the same compiled
   * expression can be re-entered on one thread — a worker whose return expression evaluates a query
   * over the same pipeline — and clearing would strand the outer scan with no binding, quietly
   * turning its split back into the whole source.
   */
  public Sequence bind(final Sequence split) {
    final Sequence previous = assigned.get();
    if (split == null) {
      assigned.remove();
    } else {
      assigned.set(split);
    }
    return previous;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) throws QueryException {
    final Sequence split = assigned.get();
    return split != null ? split : delegate.evaluate(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(final QueryContext ctx, final Tuple tuple) throws QueryException {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    return delegate.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return delegate.isVacuous();
  }

  @Override
  public String toString() {
    return "SplitAware(" + delegate + ")";
  }
}
