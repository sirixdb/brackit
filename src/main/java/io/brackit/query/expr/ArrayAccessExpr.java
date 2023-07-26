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
import io.brackit.query.jdm.*;
import io.brackit.query.util.ExprUtil;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.type.ArrayType;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class ArrayAccessExpr implements Expr {
  private final Expr expr;
  private final Expr index;

  public ArrayAccessExpr(Expr expr, Expr index) {
    this.expr = expr;
    this.index = index;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    final Sequence sequence = expr.evaluate(ctx, tuple);
    if (sequence == null) {
      return null;
    }

    if (sequence instanceof ItemSequence itemSequence) {
      return getLazySequence(ctx, tuple, itemSequence);
    }

    if (sequence instanceof LazySequence lazySequence) {
      return getLazySequence(ctx, tuple, lazySequence);
    }

    final var currItem = ExprUtil.asItem(sequence);

    if (!(currItem instanceof Array array)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               currItem.itemType(),
                               ArrayType.ARRAY);
    }

    final Item itemIndex = index.evaluateToItem(ctx, tuple);

    if (itemIndex == null) {
      return getLazySequence(ctx, tuple, array);
    }

    if (!(itemIndex instanceof IntNumeric numericIndex)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               itemIndex.itemType(),
                               Type.INR);
    }

    if (numericIndex.intValue() < 0) {
      final int index = array.len() + numericIndex.intValue();

      if (index < 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Illegal negative index: " + index);
      }

      return array.at(array.len() + numericIndex.intValue());
    }

    return array.at(numericIndex);
  }

  private LazySequence getLazySequence(final QueryContext ctx, final Tuple tuple, final Sequence sequence) {
    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          final Iter iter = sequence.iterate();
          Iter nestedIter;

          @Override
          public Item next() {
            Item item;

            if (nestedIter != null) {
              if ((item = nestedIter.next()) != null) {
                return item;
              }
            }

            while ((item = iter.next()) != null) {
              if (!(item instanceof Array array)) {
                continue;
              }
              final Item i = index.evaluateToItem(ctx, tuple);
              if (i == null) {
                nestedIter = getLazySequence(ctx, tuple, array).iterate();

                return nestedIter.next();
              } else {
                if (!(i instanceof IntNumeric intNumeric)) {
                  throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                           "Illegal operand type '%s' where '%s' is expected",
                                           i.itemType(),
                                           Type.INR);
                }

                final var index = intNumeric.intValue() >= 0
                    ? intNumeric.intValue()
                    : array.len() + intNumeric.intValue();

                return array.at(index).evaluateToItem(ctx, tuple);
              }
            }
            return null;
          }

          @Override
          public void close() {
          }
        };
      }
    };
  }

  private LazySequence getLazySequence(final QueryContext ctx, final Tuple tuple, final Array array) {
    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          int i = 0;

          @Override
          public Item next() {
            return i < array.len() ? array.at(i++).evaluateToItem(ctx, tuple) : null;
          }

          @Override
          public void close() {
          }

          @Override
          public Split split(int min, int max) throws QueryException {
            // TODO Auto-generated method stub
            return null;
          }
        };
      }
    };
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    return expr.isUpdating() || index.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
