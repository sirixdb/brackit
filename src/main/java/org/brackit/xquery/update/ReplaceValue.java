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
package org.brackit.xquery.update;

import java.util.EnumSet;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.expr.ConstructedNodeBuilder;
import org.brackit.xquery.update.op.ReplaceElementContentOp;
import org.brackit.xquery.update.op.ReplaceNodeOp;
import org.brackit.xquery.update.op.ReplaceValueOp;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ReplaceValue extends ConstructedNodeBuilder implements Expr {
	private static final EnumSet<Kind> replaceValueKind = EnumSet.of(
			Kind.ELEMENT, Kind.ATTRIBUTE, Kind.TEXT, Kind.COMMENT,
			Kind.PROCESSING_INSTRUCTION);

	private final Expr sourceExpr;

	private final Expr targetExpr;

	public ReplaceValue(Expr sourceExpr, Expr targetExpr) {
		this.sourceExpr = sourceExpr;
		this.targetExpr = targetExpr;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return evaluateToItem(ctx, tuple);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence target = targetExpr.evaluate(ctx, tuple);
		Item targetItem;
		Node<?> node;

		if (target == null) {
			throw new QueryException(
					ErrorCode.ERR_UPDATE_INSERT_TARGET_IS_EMPTY_SEQUENCE);
		} else if (target instanceof Item) {
			targetItem = (Item) target;
		} else {
			Iter it = target.iterate();
			try {
				targetItem = it.next();

				if (targetItem == null) {
					throw new QueryException(
							ErrorCode.ERR_UPDATE_INSERT_TARGET_IS_EMPTY_SEQUENCE);
				}
				if (it.next() != null) {
					throw new QueryException(
							ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE);
				}
			} finally {
				it.close();
			}
		}
		if (!(targetItem instanceof Node<?>)) {
			throw new QueryException(
					ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE,
					"Target item is atomic value %s", targetItem);
		}

		node = (Node<?>) targetItem;

		if (!replaceValueKind.contains(node.getKind())) {
			throw new QueryException(
					ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE,
					"Target node kind %s is not allowed for replace node: %",
					node.getKind());
		}

		Sequence source = sourceExpr.evaluate(ctx, tuple);
		String text = buildTextContent(ctx, source);
		ReplaceNodeOp op = null;

		if (node.getKind() == Kind.ELEMENT) {
			ctx.addPendingUpdate(new ReplaceElementContentOp(node, new Una(text)));
		} else {
			if ((text != null) && (!text.isEmpty())) {
				if (node.getKind() == Kind.COMMENT) {
					if ((text.contains("''")) || (text.endsWith("'"))) {
						throw new QueryException(
								ErrorCode.ERR_COMMENT_WOULD_CONTAIN_ILLEGAL_HYPHENS);
					}
				} else if (node.getKind() == Kind.PROCESSING_INSTRUCTION) {
					if (text.contains("?>")) {
						throw new QueryException(
								ErrorCode.ERR_PI_WOULD_CONTAIN_ILLEGAL_STRING);
					}
				}
			}
			ctx.addPendingUpdate(new ReplaceValueOp(node, new Una(text)));
		}

		return null;
	}

	@Override
	public boolean isUpdating() {
		return true;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}
}