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
import org.brackit.xquery.jsonitem.array.DArray;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.jdm.*;
import org.brackit.xquery.jdm.json.Array;
import org.magicwerk.brownies.collections.GapList;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class ArrayIndexSliceExpr implements Expr {
  private final Expr expr;
  private final Expr firstIndex;
  private final Expr secondIndex;
  private final Expr step;

  public ArrayIndexSliceExpr(Expr expr, Expr firstIndex, Expr secondIndex, Expr increment) {
    this.expr = expr;
    this.firstIndex = firstIndex;
    this.secondIndex = secondIndex;
    this.step = increment;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    final Item arrayItem = expr.evaluateToItem(ctx, tuple);
    if (arrayItem == null) {
      return null;
    }
    if (!(arrayItem instanceof Array array)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               arrayItem.itemType(),
                               Type.INR);
    }
    final Item lowerBoundItem = firstIndex.evaluateToItem(ctx, tuple);
    final Item upperBoundItem = secondIndex.evaluateToItem(ctx, tuple);
    final Item stepItem = step.evaluateToItem(ctx, tuple);

    if (stepItem != null && !(stepItem instanceof IntNumeric)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               stepItem.itemType(),
                               Type.INR);
    }

    final int step;
    if (stepItem == null) {
      step = 1;
    } else {
      step = ((IntNumeric) stepItem).intValue();
    }

    if (lowerBoundItem == null) {
      if (upperBoundItem == null) {
        if (step > 0) {
          return getAllItemsFromArray(array, step);
        } else {
          return getAllItemsFromArrayReversed(array, step);
        }
      } else {
        if (!(upperBoundItem instanceof IntNumeric)) {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Illegal operand type '%s' where '%s' is expected",
                                   upperBoundItem.itemType(),
                                   Type.INR);
        }

        final int upperBoundIndexInt = ((IntNumeric) upperBoundItem).intValue();
        final int upperBoundIndex = upperBoundIndexInt >= 0 ? upperBoundIndexInt : array.len() + upperBoundIndexInt;

        if (step > 0) {
          final var it = array.iterate();

          final var buffer = new GapList<Item>(array.len());
          Item item;
          int i = 0;
          boolean first = true;
          while ((item = it.next()) != null && i < upperBoundIndex) {
            if (first) {
              first = false;
              buffer.add(item);
            } else if (i % step == 0) {
              buffer.add(item);
            }
            i++;
          }
          return new DArray(buffer);
        } else {
          final var buffer = new GapList<Item>(array.len());
          for (int i = array.len() - 1; i > upperBoundIndex && i != -1; i = i + step) {
            final var item = ExprUtil.asItem(array.at(i));
            buffer.add(item);
          }
          return new DArray(buffer);
        }
      }
    }
    if (!(lowerBoundItem instanceof IntNumeric)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               lowerBoundItem.itemType(),
                               Type.INR);
    }

    final int lowerBoundIndexInt = ((IntNumeric) lowerBoundItem).intValue();
    final int lowerBoundIndex = lowerBoundIndexInt >= 0 ? lowerBoundIndexInt : array.len() + lowerBoundIndexInt;

    if (upperBoundItem == null) {
      final int upperBoundIndex = step >= 0 ? array.len() : -1;
      return getArrayItemSlice(array, step, lowerBoundIndex, upperBoundIndex);
    }

    final int upperBoundIndexInt = ((IntNumeric) upperBoundItem).intValue();
    final int upperBoundIndex = upperBoundIndexInt >= 0 ? upperBoundIndexInt : array.len() + upperBoundIndexInt;
    return getArrayItemSlice(array, step, lowerBoundIndex, upperBoundIndex);
  }

  private Sequence getArrayItemSlice(Array array, int step, int lowerBoundIndex, int upperBoundIndex) {
    if (step > 0) {
      return getArrayItemSliceSequence(array, lowerBoundIndex, upperBoundIndex, step);
    } else {
      return getArrayItemSliceSequenceReversed(array, lowerBoundIndex, upperBoundIndex, step);
    }
  }

  private Array getArrayItemSliceSequence(Array array, int lowerBoundIndex, int upperBoundIndex, int step) {
    final var it = array.iterate();

    int i = 0;
    while (i < lowerBoundIndex && it.next() != null) {
      i++;
    }

    Item item;
    final var buffer = new GapList<Item>(array.len());
    boolean first = true;
    while ((item = it.next()) != null && i < upperBoundIndex) {
      if (first) {
        first = false;
        buffer.add(item);
      } else if (i % step == 0) {
        buffer.add(item);
      }
      i++;
    }
    return new DArray(buffer);
  }

  private Array getArrayItemSliceSequenceReversed(Array array, int lowerBoundIndex, int upperBoundIndex, int step) {
    final var buffer = new GapList<Item>(array.len());

    for (int i = lowerBoundIndex; i > upperBoundIndex && i != -1; i = i + step) {
      final var item = ExprUtil.asItem(array.at(i));
      buffer.add(item);
    }
    return new DArray(buffer);
  }

  private Array getAllItemsFromArray(Array array, int step) {
    final var it = array.iterate();

    final var buffer = new GapList<Item>(array.len());
    Item item;
    int i = 0;
    boolean first = true;
    while ((item = it.next()) != null) {
      if (first) {
        first = false;
        buffer.add(item);
      } else if (i % step == 0) {
        buffer.add(item);
      }
      i++;
    }
    return new DArray(buffer);
  }

  private Array getAllItemsFromArrayReversed(Array array, int step) {
    final var buffer = new GapList<Item>(array.len());
    for (int i = array.len() - 1; i != -1; i = i + step) {
      final var item = ExprUtil.asItem(array.at(i));
      buffer.add(item);
    }
    return new DArray(buffer);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    final var res = evaluate(ctx, tuple);
    if (res == null || res instanceof Item) {
      return (Item) res;
    }
    final var values = new GapList<Sequence>();
    try (Iter it = res.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        values.add(item);
      }
    }
    return new DArray(values);
  }

  @Override
  public boolean isUpdating() {
    return expr.isUpdating() || firstIndex.isUpdating() || secondIndex.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
