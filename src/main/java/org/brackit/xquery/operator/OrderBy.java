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
import org.brackit.xquery.util.TupleComparator;
import org.brackit.xquery.util.TupleSort;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class OrderBy implements Operator {
	private static class OrderByCursor implements Cursor {
		private final Cursor c;

		private final Expr[] orderByExprs;

		private final int[] orderBySpec;

		private TupleSort sort;

		private int tupleSize = -1;

		private Stream<? extends Tuple> sorted;

		public OrderByCursor(QueryContext ctx, Cursor c, Expr[] orderByExprs,
				int[] orderBySpec) {
			this.c = c;
			this.orderByExprs = orderByExprs;
			this.orderBySpec = orderBySpec;
		}

		@Override
		public void close(QueryContext ctx) {
			if (sorted != null) {
				sorted.close();
			}
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t = sorted.next();
			return (t != null) ? new TupleImpl(Arrays.copyOfRange(t.array(), 0,
					tupleSize)) : null;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
			if (sort == null) {
				sort = new TupleSort(new TupleComparator(ctx, orderBySpec), -1);

				Tuple current;
				while ((current = c.next(ctx)) != null) {
					if (tupleSize == -1) {
						tupleSize = current.getSize();
					}
					Sequence[] concat = new Sequence[orderByExprs.length];
					for (int i = 0; i < orderByExprs.length; i++) {
						concat[i] = orderByExprs[i]
								.evaluateToItem(ctx, current);
					}
					sort.add(new TupleImpl(current, concat));
				}
				sort.sort();
			}
			sorted = sort.stream();
		}
	}

	private final Operator in;

	private final Expr[] orderByExprs;

	private final int[] orderBySpec;

	public OrderBy(Operator in, Expr[] orderByExprs, int[] orderBySpec) {
		this.in = in;
		this.orderByExprs = orderByExprs;
		this.orderBySpec = orderBySpec;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new OrderByCursor(ctx, in.create(ctx, tuple), orderByExprs,
				orderBySpec);
	}
}
