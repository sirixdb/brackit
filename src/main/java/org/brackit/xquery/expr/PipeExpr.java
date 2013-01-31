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
package org.brackit.xquery.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.operator.Cursor;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class PipeExpr implements Expr {
	private final Operator op;
	private final Expr expr;

	public PipeExpr(Operator op, Expr expr) {
		this.op = op;
		this.expr = expr;
	}

	public static class PipeSequence extends LazySequence {
		final QueryContext ctx;
		final Operator op;
		final Expr expr;
		final Tuple tuple;

		public PipeSequence(QueryContext ctx, Operator op, Expr expr,
				Tuple tuple) {
			this.ctx = ctx;
			this.op = op;
			this.expr = expr;
			this.tuple = tuple;
		}

		@Override
		public Iter iterate() {
			return new BaseIter() {
				Cursor cursor;
				Iter it;

				@Override
				public Item next() throws QueryException {
					while (true) {
						if (it != null) {
							Item i = it.next();
							if (i != null) {
								return i;
							}
							it.close();
							it = null;
						} else if (cursor == null) {
							cursor = op.create(ctx, tuple);
							cursor.open(ctx);
						}

						Tuple t = cursor.next(ctx);

						if (t == null) {
							return null;
						}

						Sequence s = expr.evaluate(ctx, t);

						if (s == null) {
							continue;
						}
						if (s instanceof Item) {
							return (Item) s;
						}
						it = s.iterate();
					}
				}

				@Override
				public void close() {
					if (it != null) {
						it.close();
					}
					if (cursor != null) {
						cursor.close(ctx);
					}
				}
			};
		}
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return new PipeSequence(ctx, op, expr, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return ExprUtil.asItem(evaluate(ctx, tuple));
	}

	@Override
	public boolean isUpdating() {
		// TODO
		return expr.isUpdating();
//		return false;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return PipeExpr.class.getSimpleName();
	}
}