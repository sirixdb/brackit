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
package org.brackit.xquery.expr;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class PIExpr extends ConstructedNodeBuilder implements Expr {
	protected final Expr leftExpr;

	protected final Expr rightExpr;

	protected final boolean appendOnly;

	public PIExpr(Expr leftExpr, Expr rightExpr, boolean appendOnly) {
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
		this.appendOnly = appendOnly;
	}

	@Override
	public final Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		// See XQuery 3.7.3.5 Computed PI Constructors
		Item item = leftExpr.evaluateToItem(ctx, tuple);

		if (item == null) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Empty name expression in processing instruction");
		}

		Atomic atomic = item.atomize();
		Type type = atomic.type();

		if (type != Type.NCN) {
			if ((type == Type.STR) || (type == Type.UNA)) {
				try {
					atomic = Cast.cast(atomic, Type.NCN, false);
				} catch (Exception e) {
					throw new QueryException(e,
							ErrorCode.ERR_PI_TARGET_CAST_TO_NCNAME,
							"Cast of name expression in processing instruction to xs:NCName failed");
				}
			} else {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Invalid target type in processing instruction");
			}
		}

		String target = atomic.stringValue();

		if (target.toLowerCase().equals("xml")) {
			throw new QueryException(
					ErrorCode.ERR_PI_TARGET_IS_XML,
					"NCName in processing instruction is not allowed to be 'XML': %s",
					target);
		}

		Sequence sequence = rightExpr.evaluate(ctx, tuple);
		StringBuilder buf = new StringBuilder("");

		if (sequence != null) {
			if (sequence instanceof Item) {
				atomic = ((Item) sequence).atomize();

				if (!atomic.type().instanceOf(Type.STR)) {
					atomic = Cast.cast(atomic, Type.STR, true);
				}

				buf.append(atomic.stringValue());
			} else {
				boolean first = true;
				Iter it = sequence.iterate();
				try {
					while ((item = it.next()) != null) {
						atomic = item.atomize();

						if (!atomic.type().instanceOf(Type.STR)) {
							atomic = Cast.cast(atomic, Type.STR, true);
						}

						String s = atomic.stringValue();
						if (!s.isEmpty()) {
							if (!first) {
								buf.append(' ');
							}
							first = false;
							buf.append(s);
						}
					}
				} finally {
					it.close();
				}
			}
		}

		String content = buf.toString();

		if (content.contains("?>")) {
			throw new QueryException(
					ErrorCode.ERR_PI_WOULD_CONTAIN_ILLEGAL_STRING,
					"Content expression of processing instruction illegal string '?>'",
					content);
		}

		if (appendOnly) {
			((Node<?>) tuple.get(tuple.getSize() - 1)).append(
					Kind.PROCESSING_INSTRUCTION, new QNm(target), new Str(
							content));
			return null;
		}

		Node<?> attribute = ctx.getNodeFactory().pi(new QNm(target),
				new Str(content));
		return attribute;
	}

	@Override
	public boolean isUpdating() {
		return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}
}
