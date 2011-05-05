/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.expr;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.sequence.FlatteningIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

public class SequenceExpr implements Expr {
	final Expr[] expr;

	public SequenceExpr(Expr... expr) {
		this.expr = expr;
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, final Tuple tuple)
			throws QueryException {
		// return an anonymous sequence flattening the result
		// of each expression for the current tuple
		return new LazySequence() {
			@Override
			public Iter iterate() {
				return new FlatteningIter() {
					int pos = 0;

					@Override
					protected Sequence nextSequence() throws QueryException {
						Sequence s = null;
						while ((pos < expr.length) && (s == null)) {
							s = expr[pos++].evaluate(ctx, tuple);
						}
						return s;
					}
				};
			}
		};
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		if (expr.length == 0) {
			return null;
		} else if (expr.length == 1) {
			return expr[0].evaluateToItem(ctx, tuple);
		} else {
			int i = 0;
			Item res = null;
			while ((i < expr.length)
					&& ((res = expr[i++].evaluateToItem(ctx, tuple)) == null))
				;

			if (i == expr.length) {
				return res;
			}

			while (i < expr.length) {
				if (expr[i++].evaluateToItem(ctx, tuple) != null) {
					throw new QueryException(
							ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
				}
			}

			return res;
		}
	}

	@Override
	public boolean isUpdating() {
		for (Expr e : this.expr) {
			if (e.isUpdating()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isVacuous() {
		for (Expr e : this.expr) {
			if (!e.isVacuous()) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("(");
		boolean first = true;
		for (Expr e : expr) {
			if (!first) {
				out.append(", ");
			}
			first = false;
			out.append(e.toString());
		}
		out.append(")");
		return out.toString();
	}
}
