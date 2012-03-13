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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.expr.RangeExpr;
import org.brackit.xquery.expr.SequenceExpr;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.util.aggregator.Aggregator;
import org.brackit.xquery.util.aggregator.SequenceAggregator;
import org.brackit.xquery.util.aggregator.SumAvgAggregator;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * @author Sebastian Baechle
 * 
 */
public class HashGroupBy implements Operator {
	final Operator in;
	final int[] groupSpecs; // positions of grouping variables
	final boolean onlyLast;
	int check = -1;

	public HashGroupBy(Operator in, int groupSpecCount, boolean onlyLast) {
		this.in = in;
		this.groupSpecs = new int[groupSpecCount];
		this.onlyLast = onlyLast;
	}

	private static class Key {
		final int hash;
		final Atomic[] val;

		Key(Atomic[] val) {
			this.val = val;
			this.hash = Arrays.hashCode(val);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return Arrays.toString(val);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof Key) {
				Key k = (Key) obj;
				for (int i = 0; i < val.length; i++) {
					Atomic a1 = val[i];
					Atomic a2 = k.val[i];
					if (((a1 == null) && (a2 != null)) || (a2 == null)
							|| (a1.atomicCmp(a2) != 0)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}

	private class GroupByCursor implements Cursor {
		final Cursor c;
		final int tupleSize;
		Tuple next;
		boolean[] skipgroup;
		Map<Key, Aggregator[]> map;
		Iterator<Key> it;

		public GroupByCursor(Cursor c, int tupleSize) {
			this.c = c;
			this.tupleSize = tupleSize;
			this.skipgroup = new boolean[tupleSize];
			for (int pos : groupSpecs) {
				skipgroup[pos] = true;
			}
			if (onlyLast) {
				for (int pos = 0; pos < tupleSize - 1; pos++) {
					skipgroup[pos] = true;
				}
			}
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}

		@Override
		public void close(QueryContext ctx) {
			map = null;
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			while (true) {
				if (it != null) {
					if (it.hasNext()) {
						Key key = it.next();
						return emit(map.get(key));
					} else {
						it = null;
						map = null;
					}
				}

				Tuple t;
				while (((t = next) != null) || ((t = c.next(ctx)) != null)) {
					if ((check >= 0) && (t.get(check) == null)) {
						if (map != null) {
							break;
						} else {
							next = null;
							return t;
						}
					}
					next = null;
					if (map == null) {
						map = new LinkedHashMap<Key, Aggregator[]>();
					}
					Key gks = extractGroupingKeys(ctx, t);
					Aggregator[] grp = map.get(gks);
					if (grp == null) {
						grp = new Aggregator[tupleSize];
						for (int i = 0; i < tupleSize; i++) {
							grp[i] = new SequenceAggregator();
						}
						map.put(gks, grp);
						addGroupFields(ctx, t, grp, true);
					} else {
						addGroupFields(ctx, t, grp, false);
					}
				}

				if (map != null) {
					it = map.keySet().iterator();
				} else {
					return null;
				}
			}
		}

		private Tuple emit(Aggregator[] aggs) throws QueryException {
			Sequence[] groupings = new Sequence[tupleSize];
			for (int i = 0; i < tupleSize; i++) {
				groupings[i] = aggs[i].getAggregate();
			}
			return new TupleImpl(groupings);
		}

		private void addGroupFields(QueryContext ctx, Tuple t,
				Aggregator[] aggs, boolean includeSkipGroup)
				throws QueryException {
			for (int i = 0; i < tupleSize; i++) {
				if ((skipgroup[i]) && (!includeSkipGroup)) {
					continue;
				}
				Sequence s = t.get(i);
				if (s == null) {
					continue;
				}
				aggs[i].add(s);
			}
		}

		private Key extractGroupingKeys(QueryContext ctx, Tuple t)
				throws QueryException {
			Atomic[] gk = new Atomic[groupSpecs.length];
			for (int i = 0; i < groupSpecs.length; i++) {
				Sequence seq = t.get(groupSpecs[i]);
				if (seq != null) {
					Item item = ExprUtil.asItem(seq);
					if (item != null) {
						gk[i] = item.atomize();
						if (gk[i].type().instanceOf(Type.UNA)) {
							gk[i] = Cast.cast(null, gk[i], Type.STR);
						}
					}
				}
			}
			return new Key(gk);
		}
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new GroupByCursor(in.create(ctx, tuple), in.tupleWidth(tuple
				.getSize()));
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

	public Reference group(final int groupSpecNo) {
		return new Reference() {
			public void setPos(int pos) {
				groupSpecs[groupSpecNo] = pos;
			}
		};
	}

	public static void main(String[] args) throws Exception {
		Start s = new Start();
		ForBind forBind = new ForBind(s, new RangeExpr(new Int32(1), new Int32(
				10)), false);
		ForBind forBind2 = new ForBind(forBind, new SequenceExpr(new Str("a"),
				new Str("b"), new Str("c")), false);
		forBind.bindVariable(true);
		HashGroupBy groupBy = new HashGroupBy(forBind2, 1, false);
		groupBy.group(0).setPos(1);
		Print p = new Print(groupBy, System.out);
		QueryContext ctx = new QueryContext();
		Cursor c = p.create(ctx, TupleImpl.EMPTY_TUPLE);
		c.open(ctx);
		try {
			while (c.next(ctx) != null)
				;
		} finally {
			c.close(ctx);
		}
	}
}
