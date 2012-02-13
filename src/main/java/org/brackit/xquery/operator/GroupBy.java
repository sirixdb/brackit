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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.sequence.NestedSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * @author Sebastian Baechle
 * 
 */
public class GroupBy implements Operator {
	final Operator in;
	final int[] groupSpecs; // positions of grouping variables
	final boolean onlyLast;
	int check = -1;

	public GroupBy(Operator in, int groupSpecCount, boolean onlyLast) {
		this.in = in;
		this.groupSpecs = new int[groupSpecCount];
		this.onlyLast = onlyLast;
	}

	private class GroupByCursor implements Cursor {
		final Cursor c;
		Tuple next;
		Sequence[][] buffer;
		boolean[] skipgroup;

		public GroupByCursor(Cursor c, int tupleSize) {
			this.c = c;
			this.buffer = new Sequence[tupleSize][10];
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
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t;
			if (((t = next) == null) && ((t = c.next(ctx)) == null)) {
				return null;
			}
			next = null;

			if ((check >= 0) && (t.get(check) == null)) {
				return t;
			}

			Atomic[] gks = extractGroupingKeys(ctx, t);
			int[] size = new int[buffer.length];
			addGroupFields(ctx, t, size, true);
			while ((next = c.next(ctx)) != null) {
				if ((check >= 0) && (t.get(check) == null)) {
					break;
				}
				Atomic[] ngks = extractGroupingKeys(ctx, next);
				if (!cmp(gks, ngks)) {
					break;
				}
				addGroupFields(ctx, next, size, false);
			}

			Sequence[] groupings = new Sequence[buffer.length];
			for (int i = 0; i < buffer.length; i++) {
				if (size[i] == 1) {
					groupings[i] = buffer[i][0];
				} else if (size[i] > 1) {
					Sequence[] tmp = Arrays.copyOfRange(buffer[i], 0, size[i]);
					groupings[i] = new NestedSequence(tmp);
				}
			}
			return new TupleImpl(groupings);
		}

		private void addGroupFields(QueryContext ctx, Tuple t, int[] size,
				boolean includeSkipGroup) throws QueryException {
			for (int i = 0; i < buffer.length; i++) {
				if ((skipgroup[i]) && (!includeSkipGroup)) {
					continue;
				}
				Sequence s = t.get(i);
				if (s == null) {
					continue;
				}
				if (size[i] == buffer[i].length) {
					buffer[i] = Arrays.copyOf(buffer[i],
							(buffer[i].length * 3) / 2 + 1);
				}
				buffer[i][size[i]++] = s;
			}
		}

		private boolean cmp(Atomic[] gk1, Atomic[] gk2) {
			for (int i = 0; i < groupSpecs.length; i++) {
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
			Atomic[] gk = new Atomic[groupSpecs.length];
			for (int i = 0; i < groupSpecs.length; i++) {
				Sequence seq = t.get(groupSpecs[i]);
				if (seq != null) {
					Item item = ExprUtil.asItem(seq);
					gk[i] = item.atomize();
					if (gk[i].type().instanceOf(Type.UNA)) {
						gk[i] = Cast.cast(null, gk[i], Type.STR);
					}
				}
			}
			return gk;
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
}
