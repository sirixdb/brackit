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
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
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

	final Expr[] predicates;

	final boolean bindItem;

	final boolean bindPos;

	final boolean bindSize;

	final int bindCount;

	public StepExpr(Accessor axis, NodeType test, Expr input, Expr[] predicates,
			boolean bindItem, boolean bindPos, boolean bindSize) {
		this.axis = axis;
		this.test = test;
		this.input = input;
		this.predicates = predicates;
		this.bindItem = bindItem;
		this.bindPos = bindPos;
		this.bindSize = bindSize;
		bindCount = (bindItem ? 1 : 0) + (bindPos ? 1 : 0) + (bindSize ? 1 : 0);
	}

	@Override
	public Sequence evaluate(final QueryContext ctx, final Tuple tuple)
			throws QueryException {
		final Sequence nodes = input.evaluate(ctx, tuple);

		if (nodes == null) {
			return null;
		}

		if (nodes instanceof Atomic) {
			throw new QueryException(
					ErrorCode.ERR_PATH_STEP_RETURNED_ATOMIC_VALUE,
					"Input for axis step '%s' is not a node: %s", axis,
					((Atomic) nodes).type());
		}

		if (!(nodes instanceof Node<?>)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Input %s is not a single node", nodes);
		}

		return new AxisStepSequence(ctx, tuple, nodes);
	}

	private class AxisStepSequence extends LazySequence {
		final Sequence n;
		final QueryContext ctx;
		final Tuple tuple;

		AxisStepSequence(QueryContext ctx, Tuple tuple, Sequence n) {
			this.n = n;
			this.ctx = ctx;
			this.tuple = tuple;
		}

		@Override
		public Iter iterate() {
			return new AxisStepSequenceIter(ctx, tuple, (Node<?>) n);
		}
	}

	private class AxisStepSequenceIter extends BaseIter {
		final QueryContext ctx;
		final Tuple tuple;
		final Node<?> currentNode;

		IntNumeric inSeqSize = Int32.ZERO;
		IntNumeric pos = Int32.ZERO;

		Stream<? extends Node<?>> nextS;

		AxisStepSequenceIter(QueryContext ctx, Tuple tuple, Node<?> n) {
			this.ctx = ctx;
			this.tuple = tuple;
			this.currentNode = n;
		}

		@Override
		public Item next() throws QueryException {
			if (nextS == null) {
				nextS = axis.performStep(currentNode, test);
				pos = Int32.ZERO;
				inSeqSize = Int32.ZERO;

				if (bindSize) {
					try {
						while (nextS.next() != null) {
							inSeqSize = inSeqSize.inc();
						}
					} finally {
						nextS.close();
					}
					nextS = axis.performStep(currentNode, test);
				}
			}
			Node<?> res;
			while ((res = nextS.next()) != null) {
				if ((predicates.length == 0) || (predicate(res))) {
					return res;
				}
			}
			return null;
		}

		private boolean predicate(Item item) throws QueryException {
			Tuple current = tuple;

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

			for (int i = 0; i < predicates.length; i++) {
				pos = (bindPos) ? pos : pos.inc();
				Sequence res = predicates[i].evaluate(ctx, current);
				if (res == null) {
					return false;
				}
				if (res instanceof Numeric) {
					return (((Numeric) res).cmp(pos) == 0);
				}
				Iter it = res.iterate();
				try {
					Item first = it.next();
					if ((first != null) && (it.next() == null)
							&& (first instanceof Numeric)
							&& (((Numeric) first).cmp(pos) == 0)) {
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

	@Override
	public boolean isUpdating() {
		if (input.isUpdating()) {
			return true;
		}
		for (Expr e : predicates) {
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
		for (int i = 0; i < predicates.length; i++) {
			s.append('[');
			s.append(predicates[i]);
			s.append(']');
		}
		return s.toString();
	}
}