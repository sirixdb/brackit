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
package org.brackit.xquery.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.sequence.TypedSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Resolves references to bound variables of, e.g., for, let and quantified
 * expressions.
 * 
 * @author Sebastian Baechle
 * 
 */
public class BoundVariable extends Variable implements Reference {
	private int pos = -1;

	public BoundVariable(QNm name, SequenceType type) {
		super(name, type);
	}

	public BoundVariable(QNm name, int pos) {
		super(name);
		this.pos = pos;
	}

	@Override
	public void setPos(int pos) {
		this.pos = pos;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence s;
		try {
			s = tuple.get(pos);
		} catch (QueryException e) {
			throw new QueryException(
					e,
					e.getCode(),
					"Could not resolve variable %s in tuple[%s] at position %s: %s",
					name, tuple.getSize(), pos, tuple);
		}
		if (type != null) {
			s = TypedSequence.toTypedSequence(ctx, type, s);
		}
		return s;
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence s;
		try {
			s = tuple.get(pos);
		} catch (QueryException e) {
			throw new QueryException(
					e,
					e.getCode(),
					"Could not resolve variable %s in tuple at position %s: %s",
					name, tuple.getSize(), pos, tuple);
		}
		if (type != null) {
			return TypedSequence.toTypedItem(ctx, type, s);
		} else {
			return ExprUtil.asItem(s);
		}
	}

	@Override
	public boolean isUpdating() {
		return false;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}
}
