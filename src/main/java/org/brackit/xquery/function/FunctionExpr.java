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
package org.brackit.xquery.function;

import java.util.ArrayList;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.sequence.FunctionConversionSequence;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Function;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class FunctionExpr implements Expr {
	private final StaticContext sctx;
	private final Function function;
	private final Expr[] exprs;
	private final boolean builtin;

	public FunctionExpr(StaticContext sctx, Function function, Expr... exprs)
			throws QueryException {
		this.sctx = sctx;
		this.function = function;
		this.exprs = exprs;
		this.builtin = function.isBuiltIn();
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
				SequenceType sType = (i < params.length) ? params[i]
						: params[params.length - 1];
				if (sType.getCardinality().many()) {
					args[i] = exprs[i].evaluate(ctx, tuple);
					if (!(sType.getItemType().isAnyItem())) {
						args[i] = FunctionConversionSequence.asTypedSequence(
								sType, args[i], builtin);
					}
				} else {
					args[i] = exprs[i].evaluateToItem(ctx, tuple);
					args[i] = FunctionConversionSequence.asTypedSequence(sType,
							args[i], builtin);
				}
			}
		}

		try {
			res = function.execute(sctx, ctx, args);
		} catch (StackOverflowError e) {
			throw new QueryException(
					e,
					ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
					"Execution of function '%s' was aborted because of too deep recursion.",
					function.getName());
		}
		if (function.isBuiltIn()) {
			return res;
		}
		res = FunctionConversionSequence.asTypedSequence(function
				.getSignature().getResultType(), res, builtin);

		return ExprUtil.materialize(res);
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