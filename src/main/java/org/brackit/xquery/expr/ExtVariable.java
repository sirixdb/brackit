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
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.sequence.TypedSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Resolves references to declared external variables.
 * 
 * @author Sebastian Baechle
 * 
 */
public class ExtVariable extends Variable {
	private final Expr defaultExpr;

	public ExtVariable(QNm name, SequenceType type, Expr defaultExpr) {
		super(name, type);
		this.defaultExpr = defaultExpr;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence res = ctx.resolve(name);
		if (res == null) {
			if ((ctx.isBound(name)) && (defaultExpr == null)) {
				throw new QueryException(
						ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
						"External variable %s has not been bound", name);
			}
			res = defaultExpr.evaluate(ctx, TupleImpl.EMPTY_TUPLE);
		}
		if (type != null) {
			res = TypedSequence.toTypedSequence(ctx, type, res);
		}
		return res;
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence res = ctx.resolve(name);
		if (res == null) {
			if (defaultExpr == null) {
				throw new QueryException(
						ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
						"External variable %s has not been bound", name);
			}
			res = defaultExpr.evaluate(ctx, TupleImpl.EMPTY_TUPLE);
		}
		if (type != null) {
			return TypedSequence.toTypedItem(ctx, type, res);
		}
		return ExprUtil.asItem(res);
	}

	public boolean required() {
		return (defaultExpr == null);
	}

	@Override
	public boolean isUpdating() {
		return ((defaultExpr != null) && (defaultExpr.isUpdating()));
	}
}
