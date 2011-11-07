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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class GCmpExpr extends VCmpExpr {
	public GCmpExpr(Cmp cmp, Expr leftExpr, Expr rightExpr) {
		super(cmp, leftExpr, rightExpr);
	}

	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		// See XQuery 3.5 Comparison Expressions
		// Begin evaluate operands 3.5.2
		Sequence left = leftExpr.evaluate(ctx, tuple);
		Sequence right = rightExpr.evaluate(ctx, tuple);

		if ((left == null) || (right == null)) {
			return null;
		}

		// assume simple case and perform cheaper direct evaluation
		if ((left instanceof Item) && (right instanceof Item)) {
			return compareLeftAndRightAtomic(ctx, ((Item) left).atomize(),
					((Item) right).atomize());
		}

		Iter ls = left.iterate();
		Iter rs = null;
		Item lItem;
		Item rItem;
		Atomic lAtomic;
		Atomic rAtomic;

		try {
			while ((lItem = ls.next()) != null) {
				lAtomic = lItem.atomize();

				rs = right.iterate();
				while ((rItem = rs.next()) != null) {
					rAtomic = rItem.atomize();

					Bool res = compareLeftAndRightAtomic(ctx, lAtomic, rAtomic);

					if (res == Bool.TRUE) {
						return res;
					}
				}
				rs.close();
				rs = null;
			}
		} finally {
			ls.close();
			if (rs != null) {
				rs.close();
			}
		}

		return Bool.FALSE;
	}

	private Bool compareLeftAndRightAtomic(QueryContext ctx, Atomic lAtomic,
			Atomic rAtomic) throws QueryException {
		Type lType = lAtomic.type();
		Type rType = rAtomic.type();

		if (lType.instanceOf(Type.UNA)) {
			if (rType.isNumeric()) {
				lAtomic = Dbl.parse(((Una) lAtomic).str);
			} else if (rType.instanceOf(Type.UNA)
					|| (rType.instanceOf(Type.STR))) {
				// Optimized: Avoid explicit cast
				/*
				 * rAtomic = Cast.cast(ctx, rAtomic, Type.STR, false); lAtomic =
				 * Cast.cast(ctx, lAtomic, Type.STR, false);
				 */
			} else {
				lAtomic = Cast.cast(null, lAtomic, rAtomic.type(), false);
			}
		} else if (rType.instanceOf(Type.UNA)) {
			if (lType.isNumeric()) {
				rAtomic = Dbl.parse(((Una) rAtomic).str);
			} else if (lType.instanceOf(Type.STR)) {
				// Optimized: Avoid explicit cast
				/*
				 * lAtomic = Cast.cast(ctx, lAtomic, Type.STR, false); rAtomic =
				 * Cast.cast(ctx, rAtomic, Type.STR, false);
				 */
			} else {
				rAtomic = Cast.cast(null, rAtomic, lAtomic.type(), false);
			}
		}

		return cmp.compare(ctx, lAtomic, rAtomic);
	}

	public String toString() {
		return leftExpr + " " + toGcmpString(cmp) + " " + rightExpr;
	}

	private String toGcmpString(Cmp cmp) {
		switch (cmp) {
		case eq:
			return "=";
		case ne:
			return "!=";
		case ge:
			return ">=";
		case gt:
			return ">";
		case le:
			return "<=";
		case lt:
			return "<";
		default:
			return ">=";
		}
	}
}
