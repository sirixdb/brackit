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
import org.brackit.xquery.compiler.translator.Reference;
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
	final Expr bind;
	final boolean allowingEmpty;
	boolean bindVar = true;
	boolean bindPos = false;
	int check = -1;

	private class ForBindCursor implements Cursor {
		private final Cursor c;
		private IntegerNumeric pos;
		private Tuple t;
		private Iter it;

		public ForBindCursor(Cursor c) {
			this.c = c;
		}

		@Override
		public void close(QueryContext ctx) {
			if (it != null) {
				it.close();
			}
			it = null;
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			while (true) {
				if (it != null) {
					Item i = it.next();
					if (i != null) {
						return emit(t, i);
					}
					it.close();
					it = null;
				}
				if ((t = c.next(ctx)) == null) {
					return null;
				}
				if ((check >= 0) && (t.get(check) == null)) {
					Tuple tmp = passthrough(t);
					t = null;
					return tmp;
				}
				Sequence s = bind.evaluate(ctx, t);
				pos = Int32.ZERO;
				if (s == null) {
					Tuple tmp = (allowingEmpty) ? emit(t, null)
							: (check >= 0) ? passthroughUncheck(t, check) : null;
					t = null;
					return tmp;
				} else if (s instanceof Item) {
					return emit(t, s);
				} else {
					it = s.iterate();
					Item i = it.next();
					if (i != null) {
						return emit(t, i);
					}
					it.close();
					it = null;
					Tuple tmp = (allowingEmpty) ? emit(t, null)
							: (check >= 0) ? passthroughUncheck(t, check) : null;
					t = null;
					return tmp;
				}
			}
		}

		private Tuple emit(Tuple t, Sequence item) throws QueryException {
			if (bindVar) {
				if (bindPos) {
					return t.concat(new Sequence[] { item,
							(item != null) ? (pos = pos.inc()) : pos });
				} else {
					return t.concat(item);
				}
			} else if (bindPos) {
				return t.concat((item != null) ? (pos = pos.inc()) : pos);
			} else {
				return t;
			}
		}

		private Tuple passthrough(Tuple t) throws QueryException {
			if (bindVar) {
				if (bindPos) {
					return t.concat(new Sequence[2]);
				} else {
					return t.concat((Sequence) null);
				}
			} else if (bindPos) {
				return t.concat((Sequence) null);
			} else {
				return t;
			}
		}
		
		private Tuple passthroughUncheck(Tuple t, int check) throws QueryException {
			if (bindVar) {
				if (bindPos) {
					return t.conreplace(new Sequence[2], check, null);
				} else {
					return t.conreplace((Sequence) null, check, null);
				}
			} else if (bindPos) {
				return t.conreplace((Sequence) null, check, null);
			} else {
				return t;
			}
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			if (it != null) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
						"ForBind already opened");
			}
			c.open(ctx);
		}
	}

	public ForBind(Operator in, Expr bind, boolean allowingEmpty) {
		this.in = in;
		this.bind = bind;
		this.allowingEmpty = allowingEmpty;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new ForBindCursor(in.create(ctx, tuple));
	}

	@Override
	public int tupleWidth(int initSize) {
		return in.tupleWidth(initSize) + (bindVar ? 1 : 0) + (bindPos ? 1 : 0);
	}

	public void bindVariable(boolean bindVariable) {
		this.bindVar = bindVariable;
	}

	public void bindPosition(boolean bindPos) {
		this.bindPos = bindPos;
	}

	public Reference check() {
		return new Reference() {
			public void setPos(int pos) {
				check = pos;
			}
		};
	}
}
