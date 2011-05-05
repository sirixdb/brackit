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
package org.brackit.xquery.operator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ForBind implements Operator {
	private final Operator in;

	private final Expr source;

	private boolean bindVar = true;

	private boolean bindPos = false;

	static class ForBindCursor implements Cursor {
		private final Cursor in;

		private final Expr expr;

		private final boolean bindVar;

		private final boolean bindPos;

		private IntegerNumeric pos;

		private Sequence sequence;

		private Tuple current;

		private Iter it;

		public ForBindCursor(Cursor cursor, Expr expr, boolean bindVar,
				boolean bindPos) {
			this.in = cursor;
			this.expr = expr;
			this.bindVar = bindVar;
			this.bindPos = bindPos;
		}

		@Override
		public void close(QueryContext ctx) {
			if (it != null) {
				it.close();
			}
			it = null;
			in.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			while (true) {
				if (current == null) {
					current = in.next(ctx);

					if (current == null) {
						return null;
					}
				}
				if (sequence == null) {
					sequence = expr.evaluate(ctx, current);
					pos = Int32.ZERO;
				}
				if (sequence instanceof Item) {
					Tuple t = current;
					if (bindVar) {
						if (bindPos) {
							t = new TupleImpl((Tuple) current, sequence,
									(pos = pos.inc()), null);
						} else {
							t = new TupleImpl((Tuple) current, sequence);
						}
					} else if (bindPos) {
						t = new TupleImpl((Tuple) current,
								(Sequence) (pos = pos.inc()));
					}
					sequence = null;
					current = null;
					return t;
				}
				if (it == null) {
					it = sequence.iterate();
				}
				Item i = it.next();
				if (i != null) {
					Tuple t = current;
					if (bindVar) {
						if (bindPos) {
							t = new TupleImpl((Tuple) current, (Sequence) i,
									(pos = pos.inc()), null);
						} else {
							t = new TupleImpl((Tuple) current, (Sequence) i);
						}
					} else if (bindPos) {
						t = new TupleImpl((Tuple) current,
								(Sequence) (pos = pos.inc()));
					}
					return t;
				}
				it.close();
				it = null;
				sequence = null;
				current = null;
			}
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			if (it != null) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
						"ForBind %s already opened", sequence);
			}
			in.open(ctx);
		}
	}

	public ForBind(Operator in, Expr source) {
		this.in = in;
		this.source = source;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new ForBindCursor(in.create(ctx, tuple), source, bindVar,
				bindPos);
	}

	public void bindVariable(boolean bindVariable) {
		this.bindVar = bindVariable;
	}

	public void bindPosition(boolean bindPos) {
		this.bindPos = bindPos;
	}
}
