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
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.expr.VCmpExpr.Cmp;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.join.FastList;
import org.brackit.xquery.util.join.MultiTypeJoinTable;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class TableJoin implements Operator {
	private class TableJoinCursor implements Cursor {
		final Cursor lc;
		MultiTypeJoinTable table;
		Atomic tgk; // grouping key of current table
		Tuple tuple;
		FastList<Sequence[]> it;
		int itPos = 0;
		int itSize = 0;
		Sequence[] leftJoinPadding;

		public TableJoinCursor(Cursor lc) {
			this.lc = lc;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			lc.open(ctx);
			it = null;
		}

		@Override
		public void close(QueryContext ctx) {
			lc.close(ctx);
			it = null;
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			if ((it != null) && (itPos < itSize)) {
				Tuple result = tuple.concat(it.get(itPos++));
				return result;
			}

			while ((tuple = lc.next(ctx)) != null) {
				// pass through -> padding?
				// if ((check >= 0) && (t.get(check) == null)) {
				// return t;
				// }
				if (groupVar >= 0) {
					Atomic gk = (Atomic) tuple.get(groupVar);
					if ((tgk != null) && (tgk.atomicCmp(gk) != 0)) {
						table = null;
					}
				}
				if (table == null) {
					buildTable(ctx, tuple);
				}
				final Sequence keys = (isGCmp) ? lExpr.evaluate(ctx, tuple)
						: lExpr.evaluateToItem(ctx, tuple);
				final FastList<Sequence[]> matches = table.probe(keys);

				if (!emitGroup) {
					it = matches;
					itPos = 0;
					itSize = matches.getSize();

					if (itPos < itSize) {
						Tuple result = tuple.concat(matches.get(itPos++));
						return result;
					} else if (leftJoin) {
						Tuple result = tuple.concat(leftJoinPadding);
						return result;
					}
				} else {
					Item[] items = new Item[matches.getSize()];
					for (int i = 0; i < matches.getSize(); i++) {
						Sequence[] match = matches.get(i);
						items[i] = (Item) match[0];
					}
					Sequence[] padded = new Sequence[leftJoinPadding.length + 1];
					padded[leftJoinPadding.length] = new ItemSequence(items);
					Tuple result = tuple.concat(padded);
					return result;
				}
			}

			return null;
		}

		protected void buildTable(QueryContext ctx, Tuple tuple)
				throws QueryException {
			int inputSize = tuple.getSize();
			table = new MultiTypeJoinTable(ctx, cmp, isGCmp, skipSort);
			if (groupVar >= 0) {
				tgk = (Atomic) tuple.get(groupVar);
			}
			int pos = 1;
			Tuple t;
			Cursor rc = r.create(ctx, tuple);
			try {
				rc.open(ctx);
				while ((t = rc.next(ctx)) != null) {
					Sequence keys = (isGCmp) ? rExpr.evaluate(ctx, t) : rExpr
							.evaluateToItem(ctx, t);
					Sequence[] tmp = t.array();
					Sequence[] bindings = Arrays.copyOfRange(tmp, inputSize,
							tmp.length);
					table.add(keys, bindings, pos++);

					if ((leftJoin) && (leftJoinPadding == null)) {
						leftJoinPadding = new Sequence[bindings.length];
					}
				}
			} finally {
				rc.close(ctx);
			}
		}
	}

	final Operator l;
	final Operator r;
	final Expr rExpr; // should be variable access only
	final Expr lExpr; // should be variable access only
	final boolean leftJoin;
	final Cmp cmp;
	final boolean isGCmp;
	final boolean skipSort;
	final boolean emitGroup;
	int groupVar = -1;

	public TableJoin(Cmp cmp, boolean isGCmsp, boolean leftJoin,
			boolean skipSort, boolean emitGroup, Operator l, Expr lExpr,
			Operator r, Expr rExpr) {
		this.cmp = cmp;
		this.isGCmp = isGCmsp;
		this.leftJoin = leftJoin;
		this.skipSort = skipSort;
		this.emitGroup = emitGroup;
		this.l = l;
		this.r = r;
		this.rExpr = rExpr;
		this.lExpr = lExpr;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new TableJoinCursor(l.create(ctx, tuple));
	}

	public Reference group() {
		return new Reference() {
			public void setPos(int pos) {
				groupVar = pos;
			}
		};
	}
}
