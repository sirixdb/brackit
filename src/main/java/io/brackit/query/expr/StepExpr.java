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
package io.brackit.query.expr;

import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.util.ExprUtil;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.type.NodeType;

/**
 * @author Sebastian Baechle
 */
public class StepExpr extends PredicateExpr {
  final Accessor accessor;
  final Expr input;
  final NodeType test;

  public StepExpr(Accessor accessor, NodeType test, Expr input, Expr[] filter, boolean[] bindItem, boolean[] bindPos,
      boolean[] bindSize) {
    super(filter, bindItem, bindPos, bindSize);
    this.accessor = accessor;
    this.test = test;
    this.input = input;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) {
    Sequence node = input.evaluate(ctx, tuple);

    if (node == null) {
      return null;
    }

    if (!(node instanceof Item)) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "Context item in axis step is not an item: %s",
                               node);
    }
    if (!(node instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_PATH_STEP_CONTEXT_ITEM_IS_NOT_A_NODE,
                               "Context item in axis step is not a node: %s",
                               ((Item) node).itemType());
    }
    Sequence s = new AxisStepSequence((Node<?>) node);
    boolean backwardAxis = !accessor.getAxis().isForward();
    boolean reversed = false;

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
          try (Iter it = fs.iterate()) {
            Item first = it.next();
            if ((first != null) && (it.next() == null) && (first instanceof Numeric)) {
              IntNumeric pos = ((Numeric) first).asIntNumeric();
              return (pos != null) ? s.get(pos) : null;
            }
          }
          if (!fs.booleanValue()) {
            return null;
          }
        }
      } else {
        // the filter predicate is dependent on the context item
        if ((backwardAxis) && (!reversed) && (bindPos[i])) {
          s = reverse(s);
          reversed = true;
        }
        s = new DependentFilterSeq(ctx, tuple, s, i);
      }
    }

    if (reversed) {
      assert s != null;
      s = reverse(s);
    }

    return s;
  }

  private Sequence reverse(Sequence s) {
    Item[] items = new Item[s.size().intValue()];
    Item item;
    Iter iter = s.iterate();

    int i = items.length - 1;
    while ((item = iter.next()) != null) {
      items[i--] = item;
    }

    return new ItemSequence(items);
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
    public Item next() {
      if (nextS == null) {
        nextS = accessor.performStep(node, test);
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

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    if (input.isUpdating()) {
      return true;
    }
    return super.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    StringBuilder s = new StringBuilder(accessor.toString());
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