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
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class VCmpExpr implements Expr {
	public enum Cmp {
		eq, ne, lt, le, gt, ge;

		/**
		 * Compares two atomic values. This method performs type promotion if
		 * necessary. We assume, however, that none of the types is of type
		 * xs:untypedAtomic.
		 */
		public Bool compare(QueryContext ctx, Atomic left, Atomic right)
				throws QueryException {
			if (this == Cmp.eq) {
				return left.eq(right) ? Bool.TRUE : Bool.FALSE;
			} else if (this == Cmp.ne) {
				return left.eq(right) ? Bool.FALSE : Bool.TRUE;
			}

			int compare = left.cmp(right);
			boolean res;

			if (compare == 0) {
				res = ((this == Cmp.ge) || (this == Cmp.le));
			} else if (compare < 0) {
				res = ((this == Cmp.le) || (this == Cmp.lt));
			} else {
				res = ((this == Cmp.ge) || (this == Cmp.gt));
			}

			return (res ? Bool.TRUE : Bool.FALSE);
		}
	}

	protected final Cmp cmp;
	protected final Expr leftExpr;
	protected final Expr rightExpr;

	public VCmpExpr(Cmp cmp, Expr leftExpr, Expr rightExpr) {
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
		this.cmp = cmp;
	}

	@Override
	public final Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		// See XQuery 3.5 Comparison Expressions
		// Begin evaluate operands 3.5.1
		// TODO Is it legal to evaluate to item first, which may cause an
		// err:XPTY0004 before atomization which may cause an err:FOTY0012?
		Item left = leftExpr.evaluateToItem(ctx, tuple);
		Item right = rightExpr.evaluateToItem(ctx, tuple);

		if ((left == null) || (right == null)) {
			// TODO Standard says here return empty sequence
			return Bool.FALSE;
		}

		left = left.atomize();
		right = right.atomize();

		if (left instanceof Una) {
			left = new Str(((Una) left).str);
		}

		if (right instanceof Una) {
			right = new Str(((Una) right).str);
		}

		return cmp.compare(ctx, (Atomic) left, (Atomic) right);
	}

	@Override
	public boolean isUpdating() {
		return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return leftExpr + " " + cmp + " " + rightExpr;
	}

}
