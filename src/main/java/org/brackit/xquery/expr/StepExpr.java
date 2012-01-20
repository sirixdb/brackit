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
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.type.NodeType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class StepExpr implements Expr {
	final Accessor axis;
	final Expr input;
	final NodeType test;
	final Expr[] filter;
	final boolean[] bindItem;
	final boolean[] bindPos;
	final boolean[] bindSize;
	final int[] bindCount;

	public StepExpr(Accessor axis, NodeType test, Expr input, Expr[] filter,
			boolean[] bindItem, boolean[] bindPos, boolean[] bindSize) {
		this.axis = axis;
		this.test = test;
		this.input = input;
		this.filter = filter;
		this.bindItem = bindItem;
		this.bindPos = bindPos;
		this.bindSize = bindSize;
		this.bindCount = new int[filter.length];
		for (int i = 0; i < filter.length; i++) {
			bindCount[i] = (bindItem[i] ? 1 : 0) + (bindPos[i] ? 1 : 0)
					+ (bindSize[i] ? 1 : 0);
		}
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, final Tuple tuple)
			throws QueryException {
		Sequence node = input.evaluate(ctx, tuple);

		if (node == null) {
			return null;
		}

		if (!(node instanceof Item)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Context item in axis step is not an item: %s", node);
		}
		if (!(node instanceof Node<?>)) {
			throw new QueryException(
					ErrorCode.ERR_PATH_STEP_CONTEXT_ITEM_IS_NOT_A_NODE,
					"Context item in axis step is not a node: %s",
					((Item) node).itemType());
		}
		Sequence s = new AxisStepSequence((Node<?>) node);

		for (int i = 0; i < filter.length; i++) {
			// nothing to filter
			if (s == null) {
				return null;
			}

			// check if the filter predicate is independent
			// of the context item
			if (bindCount[i] == 0) {
				Sequence fs = filter[i].evaluate(ctx, tuple);
				if (fs == null) {
					return null;
				} else if (fs instanceof Numeric) {
					IntNumeric pos = ((Numeric) fs).asIntNumeric();
					s = (pos != null) ? s.get(pos) : null;
				} else {
					Iter it = fs.iterate();
					try {
						Item first = it.next();
						if ((first != null) && (it.next() == null)
								&& (first instanceof Numeric)) {
							IntNumeric pos = ((Numeric) first).asIntNumeric();
							return (pos != null) ? s.get(pos) : null;
						}
					} finally {
						it.close();
					}
					if (!fs.booleanValue()) {
						return null;
					}
				}
			} else {
				// the filter predicate is dependent on the context item
				s = new DependentFilterSeq(ctx, tuple, s, i);
			}
		}
		return s;
	}

	private class AxisStepSequence extends LazySequence {
		final Node<?> n;

		AxisStepSequence(Node<?> n) {
			this.n = n;
		}

		@Override
		public Iter iterate() {
			return new AxisStepSequenceIter(n);
		}
	}

	private class AxisStepSequenceIter extends BaseIter {
		final Node<?> node;
		Stream<? extends Node<?>> nextS;

		AxisStepSequenceIter(Node<?> node) {
			this.node = node;
		}

		@Override
		public Item next() throws QueryException {
			if (nextS == null) {
				nextS = axis.performStep(node, test);
			}
			return nextS.next();
		}

		@Override
		public void close() {
			if (nextS != null) {
				nextS.close();
			}
		}
	}

	private class DependentFilterSeq extends LazySequence {
		private final QueryContext ctx;
		private final Tuple tuple;
		private final Sequence s;
		private final int i;
		private final IntNumeric inSeqSize;

		public DependentFilterSeq(QueryContext ctx, Tuple tuple, Sequence s,
				int i) throws QueryException {
			this.ctx = ctx;
			this.tuple = tuple;
			this.s = s;
			this.i = i;
			this.inSeqSize = (bindSize[i] ? (s != null) ? s.size() : Int32.ZERO
					: Int32.ONE);
		}

		@Override
		public Iter iterate() {
			return new BaseIter() {
				IntNumeric pos;
				Iter it;

				@Override
				public Item next() throws QueryException {
					if (pos == null) {
						if (s instanceof Item) {
							pos = Int32.ONE;
							if (predicate((Item) s)) {
								// include single item in result
								return (Item) s;
							}
							return null;
						} else if (s != null) {
							pos = Int32.ZERO;
							it = s.iterate();
						}
					}

					if (it == null) {
						return null;
					}

					Item n;
					while ((n = it.next()) != null) {
						pos = pos.inc();

						if (predicate((Item) n)) {
							// include single item in result
							return (Item) n;
						}
					}
					it.close();
					return null;
				}

				private boolean predicate(Item item) throws QueryException {
					Tuple current = tuple;

					if (bindCount[i] > 0) {
						Sequence[] tmp = new Sequence[bindCount[i]];
						int p = 0;
						if (bindItem[i]) {
							tmp[p++] = item;
						}
						if (bindPos[i]) {
							tmp[p++] = pos;
						}
						if (bindSize[i]) {
							tmp[p++] = inSeqSize;
						}
						current = current.concat(tmp);
					}

					Sequence res = filter[i].evaluate(ctx, current);

					if (res == null) {
						return false;
					}

					if (res instanceof Numeric) {
						if (((Numeric) res).cmp(pos) != 0) {
							return false;
						}
					} else {
						Iter it = res.iterate();
						try {
							Item first = it.next();
							if ((first != null) && (it.next() == null)
									&& (first instanceof Numeric)
									&& (((Numeric) first).cmp(pos) != 0)) {
								return false;
							}
						} finally {
							it.close();
						}

						if (!res.booleanValue()) {
							return false;
						}
					}
					return true;
				}

				@Override
				public void close() {
					if (it != null) {
						it.close();
					}
				}
			};
		}
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return ExprUtil.asItem(evaluate(ctx, tuple));
	}

	@Override
	public boolean isUpdating() {
		if (input.isUpdating()) {
			return true;
		}
		for (Expr e : filter) {
			if (e.isUpdating()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		StringBuilder s = new StringBuilder(axis.toString());
		s.append("::");
		s.append(test.toString());
		for (int i = 0; i < filter.length; i++) {
			s.append('[');
			s.append(filter[i]);
			s.append(']');
		}
		return s.toString();
	}
}