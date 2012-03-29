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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.compiler.Unit;
import org.brackit.xquery.sequence.TypedSequence;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Reference to the default context item,
 * 
 * @author Sebastian Baechle
 * 
 */
public class DefaultCtxItem extends Variable implements Unit {

	private Expr expr;
	private ItemType type = AnyItemType.ANY;
	private boolean external = true;
	private Item item;

	public DefaultCtxItem() {
		super(Bits.FS_DOT);
	}

	@Override
	public void setExpr(Expr expr) {
		this.expr = expr;
	}

	public void setType(ItemType type) {
		this.type = type;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		if (item != null) {
			return item;
		}
		Item i = null;
		if (external) {
			i = ctx.getContextItem();
		}
		if ((i == null) && (expr != null)) {
			i = expr.evaluateToItem(ctx, tuple);
		}
		if (i == null) {
			throw new QueryException(
					ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
					"Dynamic context variable %s is not assigned a value", name);
		}
		item = TypedSequence.toTypedItem(ctx, new SequenceType(type,
				Cardinality.One), i);
		return i;
	}
}