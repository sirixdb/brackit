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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.atomic.Numeric;
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
public class FilterExpr implements Expr {
	private final Expr[] filter;

	private final Expr expr;

	private final boolean bindItem;

	private final boolean bindPos;

	private final boolean bindSize;

	private final int bindCount;

	public FilterExpr(Expr expr, Expr filter, boolean bindItem,
			boolean bindPos, boolean bindSize) {
		this(expr, new Expr[] { filter }, bindItem, bindPos, bindSize);
	}

	public FilterExpr(Expr expr, Expr[] filter, boolean bindItem,
			boolean bindPos, boolean bindSize) {
		super();
		this.filter = filter;
		this.expr = expr;
		this.bindItem = bindItem;
		this.bindPos = bindPos;
		this.bindSize = bindSize;
		bindCount = (bindItem ? 1 : 0) + (bindPos ? 1 : 0) + (bindSize ? 1 : 0);
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, final Tuple tuple)
			throws QueryException {
		final Sequence inSeq = expr.evaluate(ctx, tuple);
		final IntegerNumeric inSeqSize = (bindSize ? (inSeq != null) ? inSeq
				.size(ctx) : Int32.ZERO : Int32.ONE);

		// return an anonymous sequence flattening the result
		// of each expression for the current tuple
		return new LazySequence() {
			@Override
			public Iter iterate() {
				return new Iter() {
					IntegerNumeric pos;
					Iter s;

					@Override
					public Item next() throws QueryException {
						if (pos == null) {
							if (inSeq instanceof Item) {
								pos = Int32.ONE;
								if (predicate((Item) inSeq)) {
									// include single item in result
									return (Item) inSeq;
								}
								return null;
							} else if (inSeq != null) {
								pos = Int32.ZERO;
								s = inSeq.iterate();
							}
						}

						if (s == null) {
							return null;
						}

						Item n;
						while ((n = s.next()) != null) {
							pos = pos.inc();

							if (predicate((Item) n)) {
								// include single item in result
								return (Item) n;
							}
						}
						s.close();
						return null;
					}

					private boolean predicate(Item item) throws QueryException {
						Tuple current = tuple;

						if (bindCount > 0) {
							Sequence[] tmp = new Sequence[bindCount];
							int p = 0;
							if (bindItem) {
								tmp[p++] = item;
							}
							if (bindPos) {
								tmp[p++] = pos;
							}
							if (bindSize) {
								tmp[p++] = inSeqSize;
							}
							current = current.concat(tmp);
						}

						for (Expr f : filter) {
							Sequence res = f.evaluate(ctx, current);

							if (res == null) {
								return false;
							}

							if (res instanceof Numeric) {
								if (((Numeric) res).cmp(pos) != 0) {
									return false;
								}
							} else {
								Iter it = res.iterate();
								try {
									Item first = it.next();
									if ((first != null)
											&& (it.next() == null)
											&& (first instanceof Numeric)
											&& (((Numeric) first).cmp(pos) != 0)) {
										return false;
									}
								} finally {
									it.close();
								}

								if (!res.booleanValue(ctx)) {
									return false;
								}
							}
						}
						return true;
					}

					@Override
					public void close() {
						if (s != null) {
							s.close();
						}
					}
				};
			}
		};
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence res = expr.evaluate(ctx, tuple);

		if (res == null) {
			return null;
		} else if (res instanceof Item) {
			Tuple current = tuple;

			if (bindCount > 0) {
				Sequence[] tmp = new Sequence[bindCount];
				int p = 0;
				if (bindItem) {
					tmp[p++] = res;
				}
				if (bindPos) {
					tmp[p++] = Int32.ONE;
				}
				if (bindSize) {
					tmp[p++] = Int32.ONE;
				}
				current = current.concat(tmp);
			}

			for (Expr f : filter) {
				Sequence fRes = f.evaluate(ctx, current);

				if (fRes == null) {
					return null;
				}

				if ((fRes instanceof Numeric)
						&& (((Numeric) fRes).intValue() != 1)) {
					return null;
				}

				if (!fRes.booleanValue(ctx)) {
					return null;
				}
			}

			return (Item) res;
		} else {
			return ExprUtil.asItem(evaluate(ctx, tuple));
		}
	}

	@Override
	public boolean isUpdating() {
		if (expr.isUpdating()) {
			return true;
		}
		for (Expr f : filter) {
			if (f.isUpdating()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		StringBuilder s = new StringBuilder(expr.toString());
		for (int i = 0; i < filter.length; i++) {
			s.append('[');
			s.append(filter[i]);
			s.append(']');
		}
		return s.toString();
	}
}
