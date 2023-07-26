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

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class RangeExpr implements Expr {
  protected final Expr leftExpr;
  protected final Expr rightExpr;

  public RangeExpr(Expr leftExpr, Expr rightExpr) {
    this.leftExpr = leftExpr;
    this.rightExpr = rightExpr;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Item lItem = leftExpr.evaluateToItem(ctx, tuple);
    Item rItem = rightExpr.evaluateToItem(ctx, tuple);

    if ((lItem == null) || (rItem == null)) {
      return null;
    }

    Atomic left = lItem.atomize();
    Atomic right = rItem.atomize();

    if (!(left instanceof IntNumeric)) {
      left = convert(left);
    }

    if (!(right instanceof IntNumeric)) {
      right = convert(right);
    }

    int comparison = left.cmp(right);
    if (comparison > 0) {
      return null;
    } else if (comparison == 0) {
      return left;
    } else {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    }
  }

  protected Atomic convert(Atomic val) {
    if (val.type() == Type.UNA) {
      val = Cast.cast(null, val, Type.INR, false);
    } else {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               val.type(),
                               Type.INR);
    }
    return val;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    Item lItem = leftExpr.evaluateToItem(ctx, tuple);
    Item rItem = rightExpr.evaluateToItem(ctx, tuple);

    if ((lItem == null) || (rItem == null)) {
      return null;
    }

    Atomic left = lItem.atomize();
    Atomic right = rItem.atomize();

    if (!(left instanceof IntNumeric)) {
      left = convert(left);
    }

    if (!(right instanceof IntNumeric)) {
      right = convert(right);
    }

    int comparison = left.cmp(right);
    if (comparison > 0) {
      return null;
    } else if (comparison == 0) {
      return left;
    } else {
      final IntNumeric s = (IntNumeric) left;
      final IntNumeric e = (IntNumeric) right;

      return new AbstractSequence() {
        private final IntNumeric start = s;
        private final IntNumeric end = e;

        @Override
        public boolean booleanValue() {
          if (!size().eq(Int32.ONE)) {
            throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                                     "Effective boolean value is undefined " + "for sequences with two or more items "
                                         + "not starting with a node");
          }
          return start.booleanValue();
        }

        @Override
        public IntNumeric size() {
          return (IntNumeric) end.subtract(start).add(Int32.ONE);
        }

        @Override
        public Iter iterate() {
          return new BaseIter() {
            IntNumeric current = start;

            @Override
            public void close() {
            }

            @Override
            public Item next() {
              if (current.cmp(e) > 0)
                return null;

              IntNumeric res = current;
              current = current.inc();
              return res;
            }

            @Override
            public void skip(IntNumeric i) {
              if (i.cmp(Int32.ZERO) <= 0) {
                return;
              }
              current = (IntNumeric) current.add(i);
            }
          };
        }

        @Override
        public Item get(IntNumeric pos) {
          if (Int32.ZERO.cmp(pos) >= 0) {
            return null;
          }
          if (size().cmp(pos) < 0) {
            return null;
          }
          return start.add(pos).subtract(Int32.ONE);
        }
      };
    }
  }

  @Override
  public boolean isUpdating() {
    return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    return "(" + leftExpr + " to " + rightExpr + ")";
  }
}
