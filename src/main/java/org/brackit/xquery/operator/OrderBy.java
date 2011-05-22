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

import java.util.Arrays;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.util.TupleComparator;
import org.brackit.xquery.util.TupleSort;
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

	private static class OrderByCursor implements Cursor {
		private final Cursor c;

		private final Expr[] orderByExprs;

		private final OrderModifier[] orderBySpec;

		private final Expr groupVar;

		private final Expr check;

		private TupleSort sort;

		private int tupleSize = -1;

		private Stream<? extends Tuple> sorted;

		private Tuple next;

		private Atomic gk1;

		public OrderByCursor(QueryContext ctx, Cursor c, Expr[] orderByExprs,
				OrderModifier[] orderBySpec, Expr groupVar, Expr check) {
			this.c = c;
			this.orderByExprs = orderByExprs;
			this.orderBySpec = orderBySpec;
			this.groupVar = groupVar;
			this.check = check;

		}

		@Override
		public void close(QueryContext ctx) {
			if (sorted != null) {
				sorted.close();
				sort.clear();
			}
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t;
			if (sorted != null) {
				// consume remaining tuples last sort
				t = sorted.next();
				if (t != null) {
//					System.out.println("Emit "
//							+ new TupleImpl(Arrays.copyOfRange(t.array(), 0,
//									tupleSize)));
					return new TupleImpl(Arrays.copyOfRange(t.array(), 0,
							tupleSize));
				}
				sorted.close();
				sort.clear();
			}
			if (next != null) {
				// continue with last tuple read from input
				t = next;
				next = null;
			} else {
				// read (first) tuple from input
				t = c.next(ctx);
				if (t == null) {
					return null;
				}
				// determine input tuple size and start group key
				tupleSize = t.getSize();
			}

			// pass through
			if ((check != null) && (check.evaluate(ctx, t) == null)) {
				return t;
			}

			// sort current tuple and all following in same group
			sort = new TupleSort(new TupleComparator(ctx, tupleSize,
					orderBySpec), -1);
			System.out.println("Adding " + t);
			sort.add(addSortFields(ctx, t));
			while ((next = c.next(ctx)) != null) {
				if ((check != null) && (check.evaluate(ctx, next) == null)) {
					gk1 = null; // reset current grouping for pass through
					break;
				}
				if (groupVar != null) {
					Atomic gk2 = groupVar.evaluateToItem(ctx, next)
							.atomize();
					if (gk1 == null) {
						gk1 = gk2;
					} else if (gk1.atomicCmp(gk2) != 0) {

						gk1 = gk2;
						break;
					}
					System.out.print("Group " + gk2 + " ");
				}
//				System.out.println("Adding " + next);
				sort.add(addSortFields(ctx, next));
			}
			;
			sort.sort();
			sorted = sort.stream();
			t = sorted.next();
//			System.out
//					.println("Emit "
//							+ new TupleImpl(Arrays.copyOfRange(t.array(), 0,
//									tupleSize)));
			return new TupleImpl(Arrays.copyOfRange(t.array(), 0, tupleSize));
		}

		private Tuple addSortFields(QueryContext ctx, Tuple t)
				throws QueryException {
			Sequence[] concat = new Sequence[orderByExprs.length];
			for (int i = 0; i < orderByExprs.length; i++) {
				Item item = orderByExprs[i].evaluateToItem(ctx, t);
				Atomic atomic = (item != null) ? item.atomize() : null;
				if ((atomic != null) && (item.type().instanceOf(Type.UNA))) {
					atomic = Cast.cast(atomic, Type.STR);
				}
				concat[i] = atomic;
			}
			Tuple toSort = new TupleImpl(t, concat);
			return toSort;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}
	}

	private final Operator in;

	private final Expr[] orderByExprs;

	private final OrderModifier[] orderBySpec;

	private final Expr groupVar;

	private final Expr check;

	public OrderBy(Operator in, Expr[] orderByExprs,
			OrderModifier[] orderBySpec, Expr groupVar, Expr check) {
		this.in = in;
		this.orderByExprs = orderByExprs;
		this.orderBySpec = orderBySpec;
		this.groupVar = groupVar;
		this.check = check;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new OrderByCursor(ctx, in.create(ctx, tuple), orderByExprs,
				orderBySpec, groupVar, check);
	}
}
