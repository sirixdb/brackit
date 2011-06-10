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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class LetBind implements Operator {
	private final Operator in;
	final Expr source;
	int check = -1;
	private boolean bind = true;

	private class LetBindCursor implements Cursor {
		private final Cursor c;

		public LetBindCursor(Cursor c) {
			this.c = c;
		}

		@Override
		public void close(QueryContext ctx) {
			c.close(ctx);
		}

		@Override
		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t = c.next(ctx);

			if (t == null) {
				return null;
			}
			if ((check >= 0) && (t.get(check) == null)) {
				return t.concat((Sequence) null);
			}

			Sequence sequence = source.evaluate(ctx, t);
			return t.concat(sequence);
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}
	}

	public LetBind(Operator in, Expr source) {
		this.in = in;
		this.source = source;
	}

	public void bind(boolean bind) {
		this.bind = bind;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return (bind) ? new LetBindCursor(in.create(ctx, tuple)) : in.create(
				ctx, tuple);
	}
	
	@Override
	public int tupleWidth(int initSize) {
		return in.tupleWidth(initSize) + (bind ? 1 : 0);
	}
	
	public Reference check() {
		return new Reference() {
			public void setPos(int pos) {
				check = pos;
			}
		};
	}
}
