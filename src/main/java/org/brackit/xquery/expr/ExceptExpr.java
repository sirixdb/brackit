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

import java.util.Comparator;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.sequence.SortedNodeSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ExceptExpr implements Expr {
	private final Expr firstExpr;

	private final Expr secondExpr;

	public ExceptExpr(Expr firstExpr, Expr secondExpr) {
		this.firstExpr = firstExpr;
		this.secondExpr = secondExpr;
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, Tuple tuple)
			throws QueryException {
		Sequence sequenceA = firstExpr.evaluate(ctx, tuple);
		Sequence sequenceB = secondExpr.evaluate(ctx, tuple);

		if ((sequenceA == null) || (sequenceB == null)) {
			return null;
		}

		final Comparator<Tuple> comparator = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				return ((Node<?>) o1).cmp((Node<?>) o2);
			}
		};

		final Sequence sortedA = new SortedNodeSequence(comparator, sequenceA,
				true);
		final Sequence sortedB = new SortedNodeSequence(comparator, sequenceB,
				true);

		return new LazySequence() {
			@Override
			public Iter iterate() {
				return new BaseIter() {
					Iter aIt;
					Iter bIt;

					Node<?> a;
					Node<?> b;

					@Override
					public Item next() throws QueryException {
						if (aIt == null) {
							aIt = sortedA.iterate();
							bIt = sortedB.iterate();
							a = (Node<?>) aIt.next();
							b = (Node<?>) bIt.next();
						}

						while ((a != null) && (b != null)) {
							int res = a.cmp(b);

							if (res == 0) {
								a = (Node<?>) aIt.next();
								b = (Node<?>) bIt.next();
							} else if (res < 0) {
								Node<?> deliver = a;
								a = (Node<?>) aIt.next();
								return deliver;
							} else {
								b = (Node<?>) bIt.next();
							}
						}

						if (a != null) {
							Node<?> deliver = a;
							a = (Node<?>) aIt.next();
							return deliver;
						}
						return null;
					}

					@Override
					public void close() {
						if (aIt != null) {
							aIt.close();
						}
						if (bIt != null) {
							bIt.close();
						}
					}
				};
			}
		};
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return ExprUtil.asItem(evaluate(ctx, tuple));
	}

	@Override
	public boolean isUpdating() {
		return ((firstExpr.isUpdating()) || (secondExpr.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}
}
