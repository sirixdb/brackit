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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.util.sort.TupleSort;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.node.Node;

/**
 * <p>
 * This class implements a path step of the form E1/E2 with the enforcement of
 * de-duplication and sorting according to the XQuery/XPath semantics:
 * </p>
 * 
 * <pre>
 * E1/E2 
 * => 
 * fs:ddo(
 *     for $fs:dot in E1
 *     return E2	
 * )
 * </pre>
 * 
 * </p>
 * <p>
 * Longer paths of the form E1/E2/../EN are realized through multiple path step
 * expressions with a left-precedence:
 * </p>
 * <p>
 * 
 * <pre>
 * E1/E2/../EN = ((E1/E2)/..)/EN
 * </pre>
 * 
 * </p>
 * <p>
 * As optimization, path steps can sometimes omit duplicate removal and/or
 * sorting, but the inference of this property is outside the scope of this
 * class.
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
public class PathStepExpr implements Expr {
	final Expr e2;
	final Expr e1;
	final boolean bindItem;
	final boolean bindPos;
	final boolean bindSize;
	final int bindCount;
	final boolean lastStep;
	final boolean skipDDO;
	final boolean checkInput;

	public PathStepExpr(Expr e1, Expr e2, boolean bindItem, boolean bindPos,
			boolean bindSize, boolean lastStep, boolean skipDDO, boolean checkInput) {
		this.e1 = e1;
		this.e2 = e2;
		this.bindItem = bindItem;
		this.bindPos = bindPos;
		this.bindSize = bindSize;
		this.lastStep = lastStep;
		this.skipDDO = skipDDO;
		this.checkInput = checkInput;
		bindCount = (bindItem ? 1 : 0) + (bindPos ? 1 : 0) + (bindSize ? 1 : 0);
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple t) throws QueryException {
		Sequence in = e1.evaluate(ctx, t);
		if ((!skipDDO) && (checkInput)) {
			in = ExprUtil.materialize(in);
		}
		if (in == null) {
			return null;
		}
		IntNumeric size = (bindSize) ? in.size() : null;
		Sequence out = new PathStepSequence(ctx, t, in, size);
		if ((!skipDDO) && ((!checkInput) || (!(in instanceof Node<?>)))) {
			out = new DdoOrAtomicSequence(out);
		}
		return out;
	}

	private class PathStepSequence extends LazySequence {
		final QueryContext ctx;
		final Sequence in;
		final Tuple t;
		final IntNumeric s;

		PathStepSequence(QueryContext ctx, Tuple t, Sequence in, IntNumeric s) {
			this.ctx = ctx;
			this.t = t;
			this.in = in;
			this.s = s;
		}

		@Override
		public Iter iterate() {
			if (in instanceof Item) {
				return new ItemContextPathStepIter(ctx, t, (Item) in);
			} else {
				return new SequenceContextPathStepIter(ctx, t, s, in.iterate());
			}
		}

		public String toString() {
			return e1 + "/" + e2;
		}
	}

	private class ItemContextPathStepIter extends BaseIter {
		final QueryContext ctx;
		final Tuple t;
		final Item item;
		Boolean nodeOnly;
		Iter out;

		ItemContextPathStepIter(QueryContext ctx, Tuple t, Item item) {
			this.ctx = ctx;
			this.t = t;
			this.item = item;
		}

		@Override
		public Item next() throws QueryException {
			if (out == null) {
				if (nodeOnly != null) {
					// step was already performed
					return null;
				}
				Sequence s = performStep();
				if (s == null) {
					// just set it to some value
					// to avoid multiple evaluation of
					// next step
					nodeOnly = Boolean.TRUE;
					return null;
				}
				out = s.iterate();
			}

			Item next = out.next();
			if (next != null) {
				// check if step returns mixed output
				if (nodeOnly == null) {
					nodeOnly = (next instanceof Node);
				} else if ((lastStep) && ((nodeOnly) ^ (next instanceof Node))) {
					throw new QueryException(
							ErrorCode.ERR_PATH_STEP_RETURNED_NODE_AND_NON_NODE_VALUES,
							"Path step returned both nodes and non-node values");
				}
			}
			return next;
		}

		private Sequence performStep() throws QueryException {
			if (!(item instanceof Node<?>)) {
				throw new QueryException(
						ErrorCode.ERR_PATH_STEP_RETURNED_NON_NODE_VALUE,
						"Intermediate step in path expression returned a non-node: %s",
						item.itemType());
			}
			Tuple current = t;
			if (bindCount > 0) {
				Sequence[] tmp = new Sequence[bindCount];
				int p = 0;
				if (bindItem) {
					tmp[p++] = item;
				}
				if (bindPos) {
					tmp[p++] = Int32.ONE;
				}
				if (bindSize) {
					tmp[p++] = Int32.ONE;
				}
				current = current.concat(tmp);
			}

			Sequence s = e2.evaluate(ctx, current);
			return s;
		}

		@Override
		public void close() {
			if (out != null) {
				out.close();
			}
		}
	}

	private class SequenceContextPathStepIter extends BaseIter {
		final QueryContext ctx;
		final Tuple tuple;
		final IntNumeric inSeqSize;
		final Iter in;
		Boolean nodeOnly;
		IntNumeric pos = Int32.ZERO;
		Iter out;

		SequenceContextPathStepIter(QueryContext ctx, Tuple tuple,
				IntNumeric inSeqSize, Iter in) {
			this.ctx = ctx;
			this.tuple = tuple;
			this.inSeqSize = inSeqSize;
			this.in = in;
		}

		@Override
		public Item next() throws QueryException {
			while (true) {
				if (out != null) {
					Item next = out.next();
					if (next != null) {
						// check if step returns mixed output
						if (nodeOnly == null) {
							nodeOnly = (next instanceof Node);
						} else if ((lastStep)
								&& ((nodeOnly) ^ (next instanceof Node))) {
							throw new QueryException(
									ErrorCode.ERR_PATH_STEP_RETURNED_NODE_AND_NON_NODE_VALUES,
									"Path step returned both nodes and non-node values");
						}
						return next;
					}
					out.close();
					out = null;
				}

				Tuple current = tuple;
				Item item = in.next();

				if (item == null) {
					return null;
				}
				if (!(item instanceof Node<?>)) {
					throw new QueryException(
							ErrorCode.ERR_PATH_STEP_RETURNED_NON_NODE_VALUE,
							"Intermediate step in path expression returned a non-node: %s",
							item.itemType());
				}

				if (bindCount > 0) {
					Sequence[] tmp = new Sequence[bindCount];
					int p = 0;
					if (bindItem) {
						tmp[p++] = item;
					}
					if (bindPos) {
						tmp[p++] = (pos = pos.inc());
					}
					if (bindSize) {
						tmp[p++] = inSeqSize;
					}
					current = current.concat(tmp);
				}

				Sequence s = e2.evaluate(ctx, current);
				out = (s != null) ? s.iterate() : null;
			}
		}

		@Override
		public void close() {
			in.close();
			if (out != null) {
				out.close();
			}
		}
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return ExprUtil.asItem(evaluate(ctx, tuple));
	}

	private static class DdoOrAtomicSequence extends LazySequence {
		static final Comparator<Tuple> cmp = (o1, o2) -> ((Node<?>) o1).cmp((Node<?>) o2);

		final Sequence s;
		// volatile fields because they are
		// computed on demand
		volatile TupleSort tupleSort;
		volatile boolean atomicOnly;

		public DdoOrAtomicSequence(Sequence s) {
			this.s = s;
		}

		@Override
		public Iter iterate() {
			return new BaseIter() {
				Iter it;
				Stream<? extends Tuple> sorted;
				Node<?> prev = null;

				@Override
				public Item next() throws QueryException {
					if (atomicOnly) { // volatile read
						if (it == null) {
							it = s.iterate();
						}
						Item next = it.next();
						return next;
					}
					TupleSort sort = tupleSort; // volatile read
					if (sort == null) {
						it = s.iterate();
						final Item first = it.next();
						Item next = first;

						if (next == null) {
							// pretend it was atomic
							atomicOnly = true;
							return next;
						}

						if (next instanceof Atomic) {
							atomicOnly = true;
							return next;
						}

						try {
							// TODO -1 means no external sort
							sort = new TupleSort(cmp, -1);
							do {
								sort.add(next);
							} while ((next = it.next()) != null);
							sort.sort();
							tupleSort = sort;
						} finally {
							it.close();
							it = null;
						}
					}
					if (sorted == null) {
						sorted = sort.stream();
					}
					Node<?> next;
					while ((next = (Node<?>) sorted.next()) != null) {
						if ((prev == null) || (prev.cmp(next) != 0)) {
							prev = next;
							return next;
						}
					}
					return null;
				}

				@Override
				public void close() {
					if (sorted != null) {
						sorted.close();
					}
					if (it != null) {
						it.close();
					}
				}
			};
		}
	}

	@Override
	public boolean isUpdating() {
		return ((e1.isUpdating()) || (e2.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return e1 + "/" + e2;
	}
}