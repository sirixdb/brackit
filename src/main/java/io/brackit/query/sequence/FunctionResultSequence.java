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
package io.brackit.query.sequence;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Counter;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.expr.SequenceExpr;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.util.ExprUtil;

/** Return conversion for UDF results whose producer explicitly supports independent readers. */
public final class FunctionResultSequence extends LazySequence {
  private final Sequence source;
  private final SequenceType type;
  private final SequenceType itemType;
  private final QNm functionName;
  private IntNumeric validatedSize;

  private FunctionResultSequence(Sequence source, SequenceType type, QNm functionName) {
    this.source = source;
    this.type = type;
    this.itemType = new SequenceType(type.getItemType(), Cardinality.One);
    this.functionName = functionName;
  }

  public static Sequence prepare(Function function, Sequence source) {
    if (source instanceof SequenceExpr.EvalSequence identity) {
      source = identity.repeatableResult();
    }
    SequenceType type = function.getSignature().getResultType();
    if (source == null || source instanceof Item || !source.isRepeatable()) {
      // Preserve master's conversion and materialization, including its error timing,
      // for every producer that has not explicitly opted into the lazy result path.
      return ExprUtil.materialize(FunctionConversionSequence.asTypedSequence(type, source, false));
    }
    FunctionResultSequence result = new FunctionResultSequence(source, type, function.getName());
    Cardinality card = type.getCardinality();
    if (card == Cardinality.Zero || card.atMostOne()) {
      // Upper bounds require a bounded lookahead before returning a scalar. This also
      // keeps scalar consumers (notably numeric predicates) on their existing Item path.
      try (Iter it = result.iterate()) {
        Item first = it.next();
        it.next();
        return first;
      }
    }
    return result;
  }

  public static Sequence numericPredicate(Sequence value) {
    Sequence source = value;
    while (source instanceof TypedSequence || source instanceof FunctionConversionSequence) {
      source = source instanceof TypedSequence typed ? typed.arg : ((FunctionConversionSequence) source).arg;
    }
    if (source instanceof FunctionResultSequence) {
      try (Iter it = value.iterate()) {
        Item first = it.next();
        if (first instanceof Numeric && it.next() == null) {
          return first;
        }
      }
    }
    return value;
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  private QueryException overflow(StackOverflowError error) {
    return new QueryException(error,
                              ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
                              "Execution of function '%s' was aborted because of too deep recursion.",
                              functionName);
  }

  private void requireNonempty(boolean empty) {
    if (empty && type.getCardinality().moreThanZero()) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Invalid empty typed sequence (expected %s)",
                               type.getCardinality());
    }
  }

  @Override
  public synchronized IntNumeric size() {
    try {
      if (type.getItemType().isAnyItem()) {
        IntNumeric size = source.size();
        requireNonempty(size.cmp(Int32.ZERO) == 0);
        return size;
      }
      if (validatedSize != null) {
        return validatedSize;
      }
      // A count must still check/convert every item of a constrained item type.
      Counter count = new Counter();
      try (Iter it = iterate()) {
        while (it.next() != null) {
          count.inc();
        }
      }
      return validatedSize = count.asIntNumeric();
    } catch (StackOverflowError error) {
      throw overflow(error);
    }
  }

  @Override
  public boolean booleanValue() {
    try (Iter it = iterate()) {
      Item first = it.next();
      if (first == null) {
        return false;
      }
      if (first instanceof Node<?>) {
        return true;
      }
      if (it.next() != null) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                                 "Effective boolean value is undefined for sequences with two or more items not starting with a node");
      }
      return first.booleanValue();
    } catch (StackOverflowError error) {
      throw overflow(error);
    }
  }

  @Override
  public Item get(IntNumeric position) {
    if (position.cmp(Int32.ZERO) <= 0) {
      return null;
    }
    try {
      if (type.getItemType().isAnyItem()) {
        Item item = source.get(position);
        if (item == null && type.getCardinality().moreThanZero()) {
          requireNonempty(position.cmp(Int32.ONE) == 0 || source.get(Int32.ONE) == null);
        }
        return item;
      }
      Counter count = new Counter();
      try (Iter it = iterate()) {
        Item item;
        while ((item = it.next()) != null) {
          if (count.inc().cmp(position) == 0) {
            return item;
          }
        }
      }
      return null;
    } catch (StackOverflowError error) {
      throw overflow(error);
    }
  }

  @Override
  public Iter iterate() {
    final Iter input;
    try {
      input = source.iterate();
    } catch (StackOverflowError error) {
      throw overflow(error);
    }
    return new BaseIter() {
      private boolean seen;
      private boolean finished;
      private boolean closed;

      @Override
      public Item next() {
        if (finished || closed) {
          return null;
        }
        try {
          Item item = input.next();
          if (item == null) {
            finished = true;
            requireNonempty(!seen);
            return null;
          }
          Cardinality card = type.getCardinality();
          if (card == Cardinality.Zero || seen && card.atMostOne()) {
            throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                     "Invalid cardinality of typed sequence (expected %s): >= %s",
                                     card,
                                     seen ? 2 : 1);
          }
          seen = true;
          return type.getItemType().isAnyItem()
              ? item
              : (Item) FunctionConversionSequence.asTypedSequence(itemType, item, false);
        } catch (StackOverflowError error) {
          throw overflow(error);
        }
      }

      @Override
      public void skip(IntNumeric count) {
        if (finished || closed || count.cmp(Int32.ZERO) <= 0) {
          return;
        }
        try {
          if (!type.getItemType().isAnyItem()) {
            // Skipped items still participate in return-type validation.
            super.skip(count);
            return;
          }
          if (!seen && type.getCardinality().moreThanZero()) {
            next();
            count = (IntNumeric) count.subtract(Int32.ONE);
          }
          if (count.cmp(Int32.ZERO) > 0) {
            input.skip(count);
          }
        } catch (StackOverflowError error) {
          throw overflow(error);
        }
      }

      @Override
      public void close() {
        if (!closed) {
          closed = true;
          try {
            input.close();
          } catch (StackOverflowError error) {
            throw overflow(error);
          }
        }
      }
    };
  }
}
