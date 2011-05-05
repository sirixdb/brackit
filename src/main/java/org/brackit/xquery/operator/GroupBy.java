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
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 * 
 */
public class GroupBy implements Operator {
	final Operator in;
	final Expr[] groupVarExprs;
	final Expr[] groupExprs;

	public GroupBy(Operator in, Expr[] groupVarExprs, Expr[] groupExprs) {
		this.in = in;
		this.groupVarExprs = groupVarExprs;
		this.groupExprs = groupExprs;
	}

	private class GroupByCursor implements Cursor {
		final Cursor c;
		Atomic[] groupingKeys;
		Tuple t;
		Item[][] buffer = new Item[groupExprs.length][10];

		public GroupByCursor(Cursor c) {
			this.c = c;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}

		@Override
		public void close(QueryContext ctx) {
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			if (groupingKeys == null) // first iteration
			{
				t = c.next(ctx);
				groupingKeys = (t != null) ? extractGroupingKeys(ctx, t) : null;
			}

			if (t == null) {
				return null;
			}

			int[] size = new int[groupExprs.length];
			Tuple n = t;

			do {
				Atomic[] gk = extractGroupingKeys(ctx, t);
				if (!cmp(groupingKeys, gk)) {
					groupingKeys = gk;
					break;
				}
				for (int i = 0; i < groupExprs.length; i++) {
					Expr groupExpr = groupExprs[i];
					Sequence s = groupExpr.evaluate(ctx, t);

					if (s != null) {
						if (s instanceof Item) {
							if (size[i] == buffer[i].length) {
								buffer[i] = Arrays.copyOf(buffer[i],
										(buffer[i].length * 3) / 2 + 1);
							}
							buffer[i][size[i]++] = (Item) s;
						} else {
							Iter it = s.iterate();
							try {
								for (Item item = it.next(); item != null; item = it
										.next()) {
									if (size[i] == buffer[i].length) {
										buffer[i] = Arrays.copyOf(buffer[i],
												(buffer[i].length * 3) / 2 + 1);
									}
									buffer[i][size[i]++] = item;
								}
							} finally {
								it.close();
							}
						}
					}
				}
			} while ((t = c.next(ctx)) != null);

			Sequence[] groupedSequences = new Sequence[groupExprs.length];
			for (int i = 0; i < groupExprs.length; i++) {
				if (size[i] != 0) {
					groupedSequences[i] = (size[i] == 1) ? buffer[i][0]
							: new ItemSequence(Arrays.copyOfRange(buffer[i], 0,
									size[i]));
				}
			}

			return new TupleImpl(n, groupedSequences);
		}

		private boolean cmp(Atomic[] gk1, Atomic[] gk2) {
			for (int i = 0; i < groupVarExprs.length; i++) {
				if (gk1[i] == null) {
					if (gk2[i] != null) {
						return false;
					}
				} else if ((gk2[i] == null) || (gk1[i].atomicCmp(gk2[i]) != 0)) {
					return false;
				}
			}
			return true;
		}

		private Atomic[] extractGroupingKeys(QueryContext ctx, Tuple t)
				throws QueryException {
			Atomic[] gk = new Atomic[groupVarExprs.length];
			for (int i = 0; i < groupVarExprs.length; i++) {
				Item item = groupVarExprs[i].evaluateToItem(ctx, t);
				gk[i] = (item != null) ? item.atomize() : null;
			}
			return gk;
		}
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new GroupByCursor(in.create(ctx, tuple));
	}
}
