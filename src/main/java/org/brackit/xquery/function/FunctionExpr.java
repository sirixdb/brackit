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
package org.brackit.xquery.function;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.sequence.TypedSequence;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Function;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class FunctionExpr implements Expr {
	private final Function function;

	private final Expr[] exprs;

	public FunctionExpr(Function function, Expr... exprs) throws QueryException {
		this.function = function;
		this.exprs = exprs;
	}

	public Signature getSignature() {
		return function.getSignature();
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence res;
		Sequence[] args;

		if (function.getSignature().defaultIsContextItem()) {
			args = new Sequence[] { exprs[0].evaluateToItem(ctx, tuple) };
		} else {
			SequenceType[] params = function.getSignature().getParams();
			args = new Sequence[exprs.length];

			for (int i = 0; i < exprs.length; i++) {
				SequenceType sequenceType = (i < params.length) ? params[i]
						: params[params.length - 1];
				boolean many = sequenceType.getCardinality().many();
				Sequence s = many ? exprs[i].evaluate(ctx, tuple) : exprs[i]
						.evaluateToItem(ctx, tuple);
				args[i] = ((many) && (sequenceType.getItemType().isAnyItem())) ? s
						: convToTypedSequence(ctx, sequenceType, s);
			}
		}

		try {
			res = function.execute(ctx, args);
		} catch (StackOverflowError e) {
			throw new QueryException(
					e,
					ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
					"Execution of function '%s' was aborted because of too deep recursion.",
					function.getName());
		}
		return (function.isBuiltIn()) ? res : convToTypedSequence(ctx, function
				.getSignature().getResultType(), res);
	}

	/**
	 * See XQuery 3.1.5 Function Calls (function conversion rules) and compare
	 * with TypedSequence.
	 */
	private Sequence convToTypedSequence(QueryContext ctx,
			SequenceType sequenceType, Sequence s) throws QueryException {
		if (s == null) {
			if (sequenceType.getCardinality().moreThanZero()) {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Invalid empty-sequence()");
			}
			return null;
		} else if (s instanceof Item) {
			// short-circuit wrapping of single item parameter
			ItemType itemType = sequenceType.getItemType();

			if (itemType instanceof AtomicType) {
				Atomic atomic = ((Item) s).atomize();
				Type expected = ((AtomicType) itemType).type;
				Type type = atomic.type();

				if ((type == Type.UNA) && (expected != Type.UNA)) {
					if ((function.isBuiltIn()) && (expected.isNumeric())) {
						atomic = Cast.cast(atomic, expected, false);
					} else {
						atomic = Cast.cast(atomic, expected, false);
					}
				} else if (!itemType.matches(atomic)) {
					if ((expected.isNumeric()) && (type.isNumeric())) {
						atomic = Cast.cast(atomic, expected, false);
					} else if ((expected.instanceOf(Type.STR))
							&& (type.instanceOf(Type.AURI))) {
						atomic = Cast.cast(atomic, expected, false);
					} else {
						throw new QueryException(
								ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
								"Item of invalid atomic type in typed sequence (expected %s): %s",
								itemType, atomic);
					}
				}

				return atomic;
			} else if (!itemType.matches((Item) s)) {
				throw new QueryException(
						ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
						"Item of invalid type in typed sequence (expected %s): %s",
						itemType, s);
			}

			return s;
		} else {
			boolean applyFunctionConversion = ((sequenceType.getItemType() instanceof AtomicType) && (((AtomicType) sequenceType
					.getItemType()).type.isNumeric()));
			boolean enforceDouble = function.isBuiltIn();
			TypedSequence typedSequence = new TypedSequence(sequenceType, s,
					applyFunctionConversion, enforceDouble);

			if (sequenceType.getCardinality().atMostOne()) {
				Iter it = typedSequence.iterate();
				try {
					return it.next();
				} finally {
					it.close();
				}
			}

			return typedSequence;
		}
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence res = evaluate(ctx, tuple);
		if ((res == null) || (res instanceof Item)) {
			return (Item) res;
		}
		Iter s = res.iterate();
		try {
			Item item = s.next();
			if (item == null) {
				return null;
			}

			if (s.next() != null) {
				throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
			}
			return item;
		} finally {
			s.close();
		}
	}

	@Override
	public boolean isUpdating() {
		return function.isUpdating();
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return function.toString();
	}
}