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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.util.aggregator.Grouping;

/**
 * @author Sebastian Baechle
 * 
 */
public class GroupBy extends Check implements Operator {
	final Operator in;
	final int[] groupSpecs; // positions of grouping variables
	final boolean onlyLast;

	public GroupBy(Operator in, int groupSpecCount, boolean onlyLast) {
		this.in = in;
		this.groupSpecs = new int[groupSpecCount];
		this.onlyLast = onlyLast;
	}

	private class GroupByCursor implements Cursor {
		final Cursor c;
		final Grouping grp;
		Tuple next;

		public GroupByCursor(Cursor c, int tupleSize) {
			this.c = c;
			this.grp = new Grouping(groupSpecs, onlyLast, tupleSize);
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

			// pass through
			if ((check) && (dead(t))) {
				return t;
			}

			grp.add(t);
			while ((next = c.next(ctx)) != null) {
				if ((check) && (separate(t, next))) {
					break;
				}
				if (!grp.add(next)) {
					break;
				}
			}

			Tuple emit = grp.emit();
			grp.clear();
			return emit;
		}
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new GroupByCursor(in.create(ctx, tuple), in.tupleWidth(tuple
				.getSize()));
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple[] buf, int len)
			throws QueryException {
		return new GroupByCursor(in.create(ctx, buf, len), in.tupleWidth(buf[0]
				.getSize()));
	}

	@Override
	public int tupleWidth(int initSize) {
		return in.tupleWidth(initSize);
	}

	public Reference group(final int groupSpecNo) {
		return new Reference() {
			public void setPos(int pos) {
				groupSpecs[groupSpecNo] = pos;
			}
		};
	}
}
