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
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Select implements Operator {
	private final Operator in;
	final Expr predicate;
	int check = -1;

	public class SelectCursor implements Cursor {
		private final Cursor c;
		private Tuple prev;
		private Tuple next;

		public SelectCursor(Cursor c) {
			this.c = c;
		}

		@Override
		public void close(QueryContext ctx) {
			c.close(ctx);
		}

		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t;
			while (((t = next) != null) || (t = c.next(ctx)) != null) {
				next = null;
				if ((check >= 0) && (t.get(check) == null)) {
					break;
				}
				Sequence p = predicate.evaluate(ctx, t);
				if ((p != null) && (p.booleanValue())) {
					break;
				}
				if (check < 0) {
					continue;
				}
				// predicate is not fulfilled but we must keep
				// lifted iteration group alive for "left-join" semantics.
				Atomic gk = (Atomic) t.get(check);
				// skip if previously returned tuple was in same iteration group
				Atomic pgk = (prev != null) ? (Atomic) prev.get(check) : null;
				if ((pgk != null) && (gk.cmp(pgk) == 0)) {
					continue;
				}
				next = c.next(ctx);
				// skip if next tuple is in same iteration group
				Atomic ngk = (next != null) ? (Atomic) next.get(check) : null;
				if ((ngk != null) && (gk.cmp(ngk) == 0)) {
					continue;
				}
				// emit "dead" tuple where "check" field is switched-off
				// for pass-through in upstream operators
				t = t.replace(check, null); // switch-off check var
				break;
			}
			prev = t;
			return t;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			c.open(ctx);
		}
	}

	public Select(Operator in, Expr predicate) {
		this.in = in;
		this.predicate = predicate;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new SelectCursor(in.create(ctx, tuple));
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
