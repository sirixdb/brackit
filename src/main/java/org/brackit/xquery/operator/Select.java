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
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.xdm.Expr;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Select implements Operator {
	private final Operator in;

	private final Expr predicate;

	private final Expr groupVar;
	
	private final Expr check;

	public static class SelectCursor implements Cursor {
		private final Cursor in;

		private final Expr predicate;
		
		private final Expr groupVar;

		private final Expr check;

		private Tuple next;

		public SelectCursor(Cursor in, Expr predicate, Expr groupVar, Expr check) {
			this.in = in;
			this.predicate = predicate;
			this.groupVar = groupVar;
			this.check = check;
		}

		@Override
		public void close(QueryContext ctx) {
			in.close(ctx);
		}

		public Tuple next(QueryContext ctx) throws QueryException {
			Tuple t;
			while (((t = next) != null) || (t = in.next(ctx)) != null) {
				next = null;
				if ((check != null) && (check.evaluate(ctx, t) == null)) {
					break;
				}
				if (predicate.evaluate(ctx, t).booleanValue(ctx)) {
					break;
				}
				if (groupVar == null) {
					continue;
				}
				Tuple n = in.next(ctx);
				if (n == null) {
					break;
				}
				Atomic gk1 = (Atomic) groupVar.evaluate(ctx, t);
				Atomic gk2 = (Atomic) groupVar.evaluate(ctx, n);
				if (gk1.cmp(gk2) != 0) {
					next = n;
					break;
				}
			}
			return t;
		}

		@Override
		public void open(QueryContext ctx) throws QueryException {
			in.open(ctx);
		}
	}

	public Select(Operator in, Expr predicate, Expr groupVar, Expr check) {
		this.in = in;
		this.predicate = predicate;
		this.groupVar = groupVar;
		this.check = check;
	}

	@Override
	public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
		return new SelectCursor(in.create(ctx, tuple), predicate, groupVar, check);
	}
}
