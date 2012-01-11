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
package org.brackit.xquery.expr;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Date;
import org.brackit.xquery.atomic.DateTime;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.Time;
import org.brackit.xquery.atomic.YMD;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ArithmeticExpr implements Expr {
	public enum ArithmeticOp {
		PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), MOD("mod"), IDIV("idiv");

		final String s;

		private ArithmeticOp(String s) {
			this.s = s;
		}

		public String toString() {
			return s;
		}
	}

	protected final Expr leftExpr;

	protected final Expr rightExpr;

	private final ArithmeticOp op;

	public ArithmeticExpr(ArithmeticOp op, Expr leftExpr, Expr rightExpr) {
		this.op = op;
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
	}

	@Override
	public final Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		// See XQuery 3.4 Arithmetic Expressions
		// Begin evaluate operands
		Item left = leftExpr.evaluateToItem(ctx, tuple);
		Item right = rightExpr.evaluateToItem(ctx, tuple);

		if ((left == null) || (right == null)) {
			return null;
		}

		left = left.atomize();
		right = right.atomize();

		Type leftType = ((Atomic) left).type();
		if (leftType.instanceOf(Type.UNA)) {
			left = Cast.cast(null, left, Type.DBL, false);
			leftType = Type.DBL;
		}

		Type rightType = ((Atomic) right).type();
		if (rightType.instanceOf(Type.UNA)) {
			right = Cast.cast(null, right, Type.DBL, false);
			rightType = Type.DBL;
		}
		// End evaluate operands

		if (leftType.isNumeric()) {
			if (rightType.isNumeric()) {
				switch (op) {
				case PLUS:
					return ((Numeric) left).add((Numeric) right);
				case MINUS:
					return ((Numeric) left).subtract((Numeric) right);
				case MULT:
					return ((Numeric) left).multiply((Numeric) right);
				case DIV:
					return ((Numeric) left).div((Numeric) right);
				case IDIV:
					return ((Numeric) left).idiv((Numeric) right);
				case MOD:
					return ((Numeric) left).mod((Numeric) right);
				}
			}
		} else if (leftType.instanceOf(Type.DTD)) {
			if (rightType.instanceOf(Type.DTD)) {
				switch (op) {
				case PLUS:
					return ((DTD) left).add((DTD) right);
				case MINUS:
					return ((DTD) left).subtract((DTD) right);
				case DIV:
					return ((DTD) left).divide((DTD) right);
				}
			} else if (rightType.isNumeric()) {
				right = Cast.cast(null, right, Type.DBL, false);

				switch (op) {
				case MULT:
					return ((DTD) left).multiply((Dbl) right);
				case DIV:
					return ((DTD) left).divide((Dbl) right);
				}
			}
		} else if (leftType.instanceOf(Type.YMD)) {
			if (rightType.instanceOf(Type.YMD)) {
				switch (op) {
				case PLUS:
					return ((YMD) left).add((YMD) right);
				case MINUS:
					return ((YMD) left).subtract((YMD) right);
				case DIV:
					return ((YMD) left).divide((YMD) right);
				}
			} else if (rightType.isNumeric()) {
				right = Cast.cast(null, right, Type.DBL, false);

				switch (op) {
				case MULT:
					return ((YMD) left).multiply((Dbl) right);
				case DIV:
					return ((YMD) left).divide((Dbl) right);
				}
			}
		} else if (leftType.instanceOf(Type.DATI)) {
			if (rightType.instanceOf(Type.YMD)) {
				switch (op) {
				case PLUS:
					return ((DateTime) left).add((YMD) right);
				case MINUS:
					return ((DateTime) left).subtract((YMD) right);
				}
			} else if (rightType.instanceOf(Type.DTD)) {
				switch (op) {
				case PLUS:
					return ((DateTime) left).add((DTD) right);
				case MINUS:
					return ((DateTime) left).subtract((DTD) right);
				}
			} else if (rightType.instanceOf(Type.DATI)) {
				return ((DateTime) left).subtract((DateTime) right);
			}
		} else if (leftType.instanceOf(Type.DATE)) {
			if (rightType.instanceOf(Type.YMD)) {
				switch (op) {
				case PLUS:
					return ((Date) left).add((YMD) right);
				case MINUS:
					return ((Date) left).subtract((YMD) right);
				}
			} else if (rightType.instanceOf(Type.DTD)) {
				switch (op) {
				case PLUS:
					return ((Date) left).add((DTD) right);
				case MINUS:
					return ((Date) left).subtract((DTD) right);
				}
			} else if (rightType.instanceOf(Type.DATE)) {
				return ((Date) left).subtract((Date) right);
			}
		} else if (leftType.instanceOf(Type.TIME)) {
			if (rightType.instanceOf(Type.DTD)) {
				switch (op) {
				case PLUS:
					return ((Time) left).add((DTD) right);
				case MINUS:
					return ((Time) left).subtract((DTD) right);
				}
			} else if (rightType.instanceOf(Type.TIME)) {
				return ((Time) left).subtract((Time) right);
			}
		}

		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compute %s %s %s.", leftType, op, rightType);
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
		return leftExpr + " " + op + " " + rightExpr;
	}
}
