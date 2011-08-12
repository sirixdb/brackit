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
import org.brackit.xquery.util.sort.TupleSort;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class PathStepExpr implements Expr {
	// private static final Logger log = Logger.getLogger(PathStepExpr.class);

	final Expr nextStep;

	final Expr step;

	final boolean sortResult;

	final boolean bindItem;

	final boolean bindPos;

	final boolean bindSize;

	final int bindCount;

	public PathStepExpr(Expr step, Expr nextStep, boolean sortResult,
			boolean bindItem, boolean bindPos, boolean bindSize) {
		this.step = step;
		this.nextStep = nextStep;
		this.sortResult = sortResult;
		this.bindItem = bindItem;
		this.bindPos = bindPos;
		this.bindSize = bindSize;
		bindCount = (bindItem ? 1 : 0) + (bindPos ? 1 : 0) + (bindSize ? 1 : 0);
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, final Tuple tuple)
			throws QueryException {
		// System.out.println("Performing step " + step + " with tuple " +
		// tuple);
		Sequence nodes = step.evaluate(ctx, tuple);

		if (nodes == null) {
			return null;
		}

		if (nodes instanceof Atomic) {
			throw new QueryException(
					ErrorCode.ERR_PATH_STEP_RETURNED_ATOMIC_VALUE,
					"Input for axis step is not a node: %s", ((Atomic) nodes)
							.type());
		}

		Sequence result = new PathStepSequence(ctx, tuple, nodes,
				(bindSize) ? nodes.size() : null);

		// if (log.isTraceEnabled())
		// {
		// log.trace(String.format("Result of initial step %s is:\n%s", step,
		// print(ctx, nodes)));
		// }

		if ((sortResult) && ((!(nodes instanceof Node<?>)))) {
			result = new StepOutSequence(result, true);
			// if (log.isTraceEnabled())
			// {
			// log.trace(String.format("Sorted result of initial step %s is:\n%s",
			// step, print(ctx, result)));
			// }
		}

		return result;
	}

	protected String print(QueryContext ctx, Sequence sequence)
			throws QueryException {
		StringBuilder out = new StringBuilder();
		out.append("-----------");
		out.append('\n');
		if (sequence == null) {
			out.append("-----------");
			out.append('\n');
			return out.toString();
		}
		Iter it = sequence.iterate();
		Item item;
		try {
			while ((item = it.next()) != null) {
				out.append(item);
				out.append('\n');
			}
		} finally {
			it.close();
			out.append("-----------");
			out.append('\n');
		}
		return out.toString();
	}

	private class PathStepSequence extends LazySequence {
		final QueryContext ctx;
		final Sequence n; // result of unfiltered step expression
		final Tuple tuple;
		final IntNumeric s;

		PathStepSequence(QueryContext ctx, Tuple tuple, Sequence nodes,
				IntNumeric s) {
			this.ctx = ctx;
			this.tuple = tuple;
			this.n = nodes;
			this.s = s;
		}

		@Override
		public Iter iterate() {
			return (n instanceof Node<?>) ? new SinglePathStepSequenceIter(ctx,
					tuple, (Node<?>) n) : new PathStepSequenceIter(ctx, tuple,
					s, n.iterate());
		}

		public String toString() {
			return step + "/" + nextStep;
		}
	}

	private class SinglePathStepSequenceIter extends BaseIter {
		final QueryContext ctx;
		final Tuple tuple;
		Node<?> currentNode;
		Iter nextS;

		SinglePathStepSequenceIter(QueryContext ctx, Tuple tuple, Node<?> node) {
			this.ctx = ctx;
			this.tuple = tuple;
			this.currentNode = node;
		}

		@Override
		public Item next() throws QueryException {
			if (nextS == null) {
				Tuple current = tuple;
				if (bindCount > 0) {
					Sequence[] tmp = new Sequence[bindCount];
					int p = 0;
					if (bindItem) {
						tmp[p++] = currentNode;
					}
					if (bindPos) {
						tmp[p++] = Int32.ONE;
					}
					if (bindSize) {
						tmp[p++] = Int32.ONE;
					}
					current = current.concat(tmp);
				}

				Sequence sequence = nextStep.evaluate(ctx, current);
				nextS = (sequence != null) ? sequence.iterate() : null;
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

	private class PathStepSequenceIter extends BaseIter {
		final QueryContext ctx;
		final Tuple tuple;
		final IntNumeric inSeqSize;
		final Iter in;

		IntNumeric pos = Int32.ZERO;
		Node<?> currentNode;
		Iter nextS;

		PathStepSequenceIter(QueryContext ctx, Tuple tuple,
				IntNumeric inSeqSize, Iter in) {
			this.ctx = ctx;
			this.tuple = tuple;
			this.inSeqSize = inSeqSize;
			this.in = in;
		}

		@Override
		public Item next() throws QueryException {
			while (true) {
				if (nextS != null) {
					Item next = nextS.next();
					if (next != null) {
						return next;
					}
					nextS.close();
					nextS = null;
				}

				Item runVar = null;
				Tuple current = tuple;

				runVar = in.next();

				if (runVar == null) {
					return null;
				}
				if (!(runVar instanceof Node<?>)) {
					throw new QueryException(
							ErrorCode.ERR_PATH_STEP_RETURNED_ATOMIC_VALUE,
							"Input for axis step is not a node: %s", runVar
									.type());
				}
				currentNode = (Node<?>) runVar;

				if (bindCount > 0) {
					Sequence[] tmp = new Sequence[bindCount];
					int p = 0;
					if (bindItem) {
						tmp[p++] = runVar;
					}
					if (bindPos) {
						tmp[p++] = (pos = pos.inc());
					}
					if (bindSize) {
						tmp[p++] = inSeqSize;
					}
					current = current.concat(tmp);
				}

				// System.out.println("Performing nextStep " + nextStep +
				// " with tuple " + tuple + " bindItem=" + bindItem +
				// " bindPos="+ bindPos + " bindSize=" + bindSize);
				Sequence sequence = nextStep.evaluate(ctx, current);
				nextS = (sequence != null) ? sequence.iterate() : null;
			}
		}

		@Override
		public void close() {
			in.close();
			if (nextS != null) {
				nextS.close();
			}
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

	private static class StepOutSequence extends LazySequence {
		static final Comparator<Tuple> cmp = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				int res = ((Node<?>) o1).cmp((Node<?>) o2);
				return res;
			}
		};

		final Sequence s;
		final boolean lastStep;
		// volatile fields because they are
		// computed on demand
		volatile TupleSort tupleSort;
		volatile boolean atomicOnly;

		public StepOutSequence(Sequence s, boolean lastStep) {
			this.s = s;
			this.lastStep = lastStep;
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
							if (!lastStep) {
								it.close();
								it = null;
								throw new QueryException(
										ErrorCode.ERR_INTERMEDIARY_STEP_RETURNED_ATOMIC,
										"Intermediary step in path expression returned atomic values");
							}

							atomicOnly = true;
							return next;
						}

						try {
							// TODO -1 means no external sort
							sort = new TupleSort(cmp, -1);
							do {
								if (!(next instanceof Node<?>)) {
									throw new QueryException(
											ErrorCode.ERR_LAST_STEP_RETURNED_MIXED_NODE_AND_ATOMIC,
											"Last step in path expression returned both "
													+ "nodes and atomic values: %s (first) %s (current)",
											first, next);
								}
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
		return ((step.isUpdating()) || (nextStep.isUpdating()));
	}

	@Override
	public boolean isVacuous() {
		return false;
	}

	public String toString() {
		return step + "/" + nextStep;
	}
}