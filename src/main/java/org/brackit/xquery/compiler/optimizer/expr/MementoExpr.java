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
package org.brackit.xquery.compiler.optimizer.expr;

import java.util.ArrayList;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class MementoExpr implements Expr {
	final Expr expr;

	public MementoExpr(Expr expr) {
		this.expr = expr;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		final Sequence s = expr.evaluate(ctx, tuple);

		return new Sequence() {
			IntegerNumeric size;
			Boolean booleanValue;
			ArrayList<Item> buffer;

			@Override
			public IntegerNumeric size(QueryContext ctx) throws QueryException {
				if (size == null) {
					size = s.size(ctx);
				}
				return size;
			}

			@Override
			public Iter iterate() {
				if (buffer == null) {
					buffer = new ArrayList<Item>();
					return new Iter() {
						Iter it = s.iterate();

						@Override
						public void close() {
							it.close();
						}

						@Override
						public Item next() throws QueryException {
							Item next = it.next();
							if (next != null) {
								buffer.add(next);
							}
							return next;
						}
					};
				} else {
					return new Iter() {
						int pos = 0;
						int size = buffer.size();

						@Override
						public Item next() throws QueryException {
							if (pos < 0) {
								throw new QueryException(
										ErrorCode.BIT_DYN_INT_ERROR,
										"Iterator already closed");
							}
							return (pos < size) ? buffer.get(pos++) : null;
						}

						@Override
						public void close() {
							pos = -1;
						}
					};
				}
			}

			@Override
			public boolean booleanValue(QueryContext ctx) throws QueryException {
				if (booleanValue == null) {
					booleanValue = s.booleanValue(ctx);
				}
				return booleanValue;
			}
		};
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return expr.evaluateToItem(ctx, tuple);
	}

	@Override
	public boolean isUpdating() {
		return expr.isUpdating();
	}

	@Override
	public boolean isVacuous() {
		return expr.isVacuous();
	}
}
