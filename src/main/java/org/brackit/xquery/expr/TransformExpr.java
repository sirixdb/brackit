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

import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

public class TransformExpr extends ConstructedNodeBuilder implements Expr {
	private Expr[] copyBindings;
	private Expr modifyExpr;
	private Expr returnExpr;
	private boolean[] referenced;

	public TransformExpr(Expr[] copyBindings, boolean[] referenced,
			Expr modifyExpr, Expr returnExpr) {
		this.copyBindings = copyBindings;
		this.referenced = referenced;
		this.modifyExpr = modifyExpr;
		this.returnExpr = returnExpr;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return returnExpr.evaluate(ctx, copyAndModify(ctx, tuple));
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return returnExpr.evaluateToItem(ctx, copyAndModify(ctx, tuple));
	}

	private Tuple copyAndModify(QueryContext ctx, Tuple tuple)
			throws QueryException {
		final List<Node<?>> copies = new ArrayList<Node<?>>();
		final QueryContext context = ctx;

		for (int i = 0; i < copyBindings.length; i++) {
			if (referenced[i]) {
				final Item source = copyBindings[i].evaluateToItem(ctx, tuple);

				if (source == null || !(source instanceof Node<?>)) {
					throw (new QueryException(
							ErrorCode.ERR_TRANSFORM_SOURCE_EXPRESSION_NOT_SINGLE_NODE,
							"Source expression must evaluate to single node."));
				}

				Sequence seq = new LazySequence() {
					private Node<?> orig = (Node<?>) source;
					private Node<?> copy;

					@Override
					public Iter iterate() {
						return new Iter() {
							private boolean returned = false;

							@Override
							public Item next() throws QueryException {
								if (!returned) {
									if (copy == null) {
										copy = copy(context, orig);
										copies.add(copy);
									}
									returned = true;
									return copy;
								}
								return null;
							}

							@Override
							public void close() {
							}
						};
					}
				};

				tuple = tuple.concat(seq);
			}
		}

		modifyExpr.evaluate(ctx, tuple);

		List<UpdateOp> updates = ctx.getPendingUpdates();
		if (updates != null) {
			for (UpdateOp op : updates) {
				boolean ok = false;
				for (Node<?> copy : copies) {
					if (op.getTarget().isDescendantOrSelfOf(copy)) {
						ok = true;
						break;
					}
				}

				if (!ok) {
					throw new QueryException(
							ErrorCode.ERR_TRANSFORM_MODIFIES_EXISTING_NODE,
							"Modify clause update expressions may not affect existing nodes.");
				}
			}
		}

		ctx.applyUpdates();
		return tuple;
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
