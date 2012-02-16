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
package org.brackit.xquery.operator;

import java.util.Comparator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.util.sort.TupleSort;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class OrderBy implements Operator {

	public static class OrderModifier {
		public OrderModifier(boolean asc, boolean emptyLeast, String collation) {
			this.ASC = asc;
			this.EMPTY_LEAST = emptyLeast;
			this.collation = collation;
		}

		public final boolean ASC;
		public final boolean EMPTY_LEAST;
		public final String collation;
	}

	private static class OrderBySpec implements Comparator<Tuple> {
		private final int offset;
		private final OrderModifier[] modifier;

		public OrderBySpec(int offset, OrderModifier[] modifier) {
			this.offset = offset;
			this.modifier = modifier;
		}

		@Override
		public int compare(Tuple o1, Tuple o2) {
			try {
				for (int i = 0; i < modifier.length; i++) {
					int pos = offset + i;
					Atomic lAtomic = (Atomic) o1.get(pos);
					Atomic rAtomic = (Atomic) o2.get(pos);

					if (lAtomic == null) {
						if (rAtomic != null) {
							return (modifier[i].EMPTY_LEAST) ? -1 : 1;
						}
					}
					if (rAtomic == null) {
						return (modifier[i].EMPTY_LEAST) ? 1 : -1;
					}

					int res = lAtomic.cmp(rAtomic);
					if (res != 0) {
						return (modifier[i].ASC) ? res : -res;
					}
				}
				return 0;
			} catch (QueryException e) {
				if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
					throw new ClassCastException(e.getMessage());
				} else {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private final Operator in;
	final Expr[] orderByExprs;
	final OrderModifier[] modifier;
	int check = -1;

	private class OrderByCursor implements Cursor {
		private final Cursor c;
		private TupleSort sort;
		private int tupleSize = -1;
		private Stream<? extends Tuple> sorted;
		private Tuple next;

		public OrderByCursor(Cursor c) {
			this.c = c;
		}

		@Override
		public void close(QueryContext ctx) {
			if (sorted != null) {
				sorted.close();
				sort.clear();
			}
			c.close(ctx);
		}

		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t;
			if (sorted != null) {
				t = sorted.next();
				if (t != null) {
					return t.project(0, tupleSize);
				}
				sorted.close();
				sort.clear();
			}
			if (((t = next) == null) && ((t = c.next(ctx)) == null)) {
				return null;
			}
			next = null;
			tupleSize = t.getSize();

			// pass through
			Atomic gk = null;
			if ((check >= 0) && ((gk = (Atomic) t.get(check)) == null)) {
				return t;
			}

			// sort current tuple and all following in same group
			sort = new TupleSort(new OrderBySpec(tupleSize, modifier), -1);
			sort.add(addSortFields(ctx, t));
			while ((next = c.next(ctx)) != null) {
				if (check >= 0) {
					// check if next tuple belongs to different iteration
					Atomic ngk = (Atomic) next.get(check);
					if ((ngk == null) || (gk.atomicCmp(ngk) != 0)) {
						break;
					}
				}
				sort.add(addSortFields(ctx, next));
			}
			sort.sort();
			sorted = sort.stream();
			t = sorted.next();
			return t.project(0, tupleSize);
		}

		private Tuple addSortFields(QueryContext ctx, Tuple t)
				throws QueryException {
			Sequence[] concat = new Sequence[orderByExprs.length];
			for (int i = 0; i < orderByExprs.length; i++) {
				Item item = orderByExprs[i].evaluateToItem(ctx, t);
				Atomic atomic = (item != null) ? item.atomize() : null;
				if ((atomic != null) && (atomic.type().instanceOf(Type.UNA))) {
					atomic = Cast.cast(null, atomic, Type.STR);
				}
				concat[i] = atomic;
			}
			Tuple toSort = t.concat(concat);
			return toSort;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}
	}

	public OrderBy(Operator in, Expr[] orderByExprs, OrderModifier[] orderBySpec) {
		this.in = in;
		this.orderByExprs = orderByExprs;
		this.modifier = orderBySpec;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new OrderByCursor(in.create(ctx, tuple));
	}

	@Override
	public int tupleWidth(int initSize) {
		return in.tupleWidth(initSize);
	}
	
	public Reference check() {
		return new Reference() {
			public void setPos(int pos) {
				check = pos;
			}
		};
	}
}
