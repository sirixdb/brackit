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

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.Bits;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.util.ExprUtil;

/**
 * Descendant deref expression (=>>).
 * Performs preorder traversal through JSON structures (arrays and objects),
 * yielding values of all objects that have the specified field.
 *
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public class DerefDescendantExpr implements Expr {

  private static final int INITIAL_STACK_CAPACITY = 32;

  private final Expr object;
  private final Expr field;

  public DerefDescendantExpr(Expr object, Expr field) {
    this.object = object;
    this.field = field;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    final Sequence sequence = object.evaluate(ctx, tuple);
    if (sequence == null) {
      return null;
    }

    final Item fieldItem = field.evaluateToItem(ctx, tuple);
    if (fieldItem == null) {
      return null;
    }

    // Pre-compute the field name for fast lookup
    final QNm fieldName = toQNm(fieldItem);

    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new DescendantIter(ctx, tuple, sequence, fieldName);
      }
    };
  }

  /**
   * High-performance iterator using array-based stack for preorder traversal.
   * Avoids allocations in the hot path by using index-based access.
   */
  private static final class DescendantIter extends BaseIter {
    private final QueryContext ctx;
    private final Tuple tuple;
    private final QNm fieldName;

    // Array-based stack for better cache locality and no boxing
    private Sequence[] stack;
    private int stackSize;

    // For handling generic sequences
    private Iter currentIter;

    DescendantIter(QueryContext ctx, Tuple tuple, Sequence initial, QNm fieldName) {
      this.ctx = ctx;
      this.tuple = tuple;
      this.fieldName = fieldName;
      this.stack = new Sequence[INITIAL_STACK_CAPACITY];
      this.stackSize = 0;
      push(initial);
    }

    private void push(Sequence seq) {
      if (stackSize == stack.length) {
        // Grow stack (double capacity)
        final Sequence[] newStack = new Sequence[stack.length << 1];
        System.arraycopy(stack, 0, newStack, 0, stackSize);
        stack = newStack;
      }
      stack[stackSize++] = seq;
    }

    private Sequence pop() {
      return stack[--stackSize];
    }

    @Override
    public Item next() {
      while (true) {
        // First, drain any current iterator
        if (currentIter != null) {
          final Item item = currentIter.next();
          if (item != null) {
            push(item);
          } else {
            currentIter.close();
            currentIter = null;
          }
        }

        if (stackSize == 0) {
          return null;
        }

        final Sequence seq = pop();

        if (seq instanceof Object obj) {
          final Item result = processObject(obj);
          if (result != null) {
            return result;
          }
          // Continue loop if no match
        } else if (seq instanceof Array arr) {
          processArray(arr);
        } else if (seq instanceof Item) {
          // Atomic or other non-JSON item - skip
        } else if (seq != null) {
          // Generic sequence - iterate
          currentIter = seq.iterate();
        }
      }
    }

    /**
     * Process an object: check for field match (preorder), then push children.
     * Returns the field value if found, null otherwise.
     */
    private Item processObject(Object obj) {
      // First, get the field value (preorder - visit before children)
      final Sequence fieldValue = obj.get(fieldName);

      // Push all object values for recursive traversal (reverse order for preorder)
      final int len = obj.len();
      for (int i = len - 1; i >= 0; i--) {
        final Sequence value = obj.value(i);
        if (value != null) {
          push(value);
        }
      }

      // Return field value if found
      if (fieldValue != null) {
        return fieldValue.evaluateToItem(ctx, tuple);
      }
      return null;
    }

    /**
     * Process an array: push all elements for traversal (reverse order for preorder).
     */
    private void processArray(Array arr) {
      final int len = arr.len();
      for (int i = len - 1; i >= 0; i--) {
        final Sequence elem = arr.at(i);
        if (elem != null) {
          push(elem);
        }
      }
    }

    @Override
    public void close() {
      if (currentIter != null) {
        currentIter.close();
        currentIter = null;
      }
      // Help GC
      for (int i = 0; i < stackSize; i++) {
        stack[i] = null;
      }
      stackSize = 0;
    }
  }

  /**
   * Convert field item to QNm for efficient repeated lookups.
   */
  private static QNm toQNm(Item fieldItem) {
    if (fieldItem instanceof QNm qnm) {
      return qnm;
    } else if (fieldItem instanceof Atomic atomic) {
      return new QNm(atomic.stringValue());
    } else {
      throw new QueryException(Bits.BIT_ILLEGAL_OBJECT_FIELD, "Illegal object field reference: %s", fieldItem);
    }
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    return object.isUpdating() || field.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  @Override
  public String toString() {
    return "=>>" + field;
  }
}
