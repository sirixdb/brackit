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
import io.brackit.query.atomic.Int32;
import io.brackit.query.jsonitem.array.StreamingArray;
import io.brackit.query.jdm.*;
import io.brackit.query.jdm.SplittableSequence;
import io.brackit.query.util.ExprUtil;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.SplittableMembers;

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

    if (!(sequence instanceof Item currItem)) {
      return getLazySequence(ctx, tuple, sequence);
    }

    if (!(currItem instanceof Array array)) {
      // JSONiq Extension to XQuery, chapter 5 and sections 5.3/5.4: array lookup and
      // unboxing skip non-arrays, just as object lookup does in section 5.1.
      // https://www.jsoniq.org/docs/JSONiqExtensionToXQuery/html/section-json-navigation.html
      return null;
    }

    final Item itemIndex = index.evaluateToItem(ctx, tuple);

    if (itemIndex == null) {
      // The unboxed array — `$a[]`. Its items ARE the array's members, so when the backend can hand
      // those out in disjoint pieces this view is exactly what a morsel run needs to split. Present
      // it as such; everything else about the iteration is unchanged.
      final LazySequence unboxed = getLazySequence(ctx, tuple, array);
      if (array instanceof SplittableMembers splittable) {
        return new SplittableUnboxedArray(unboxed, splittable);
      }
      return unboxed;
    }

    return lookup(array, itemIndex);
  }

  private Sequence lookup(Array array, Item itemIndex) {
    if (!(itemIndex instanceof IntNumeric numericIndex)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               itemIndex.itemType(),
                               Type.INR);
    }

    IntNumeric idx = numericIndex;

    if (idx.cmp(Int32.ZERO) < 0) {
      idx = idx.add(array.length()).asIntNumeric();
      if (idx.cmp(Int32.ZERO) < 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Illegal negative index: " + idx);
      }
    }

    if (idx.cmp(new Int32(Integer.MAX_VALUE)) >= 0) {
      return null;
    }

    int position = idx.intValue();
    if (array instanceof StreamingArray streaming) {
      return streaming.atOrEmpty(position);
    }
    return position < array.len() ? array.at(position) : null;
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

            while (true) {
              if (nestedIter != null) {
                if ((item = nestedIter.next()) != null) {
                  return item;
                }
                nestedIter.close();
                nestedIter = null;
              }

              item = iter.next();
              if (item == null) {
                return null;
              }
              if (!(item instanceof Array array)) {
                continue;
              }
              final Item i = index.evaluateToItem(ctx, tuple);
              if (i == null) {
                nestedIter = getLazySequence(ctx, tuple, array).iterate();
              } else {
                Sequence value = lookup(array, i);
                if (value != null && (item = value.evaluateToItem(ctx, tuple)) != null) {
                  return item;
                }
              }
            }
          }

          @Override
          public void close() {
            if (nestedIter != null) {
              nestedIter.close();
            }
            iter.close();
          }
        };
      }
    };
  }

  private LazySequence getLazySequence(final QueryContext ctx, final Tuple tuple, final Array array) {
    // For StreamingArray, use iterate() to avoid materialization via len().
    // For regular arrays (DArray), use len()+at() which is the proven path.
    if (array instanceof io.brackit.query.jsonitem.array.StreamingArray) {
      return new LazySequence() {
        @Override
        public Iter iterate() {
          final Iter arrayIter = array.iterate();
          return new BaseIter() {
            @Override
            public Item next() {
              Item item = arrayIter.next();
              return item != null ? item.evaluateToItem(ctx, tuple) : null;
            }

            @Override
            public void close() {
              arrayIter.close();
            }
          };
        }
      };
    }

    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          int i = 0;

          @Override
          public Item next() {
            while (i < array.len()) {
              Sequence value = array.at(i++);
              if (value != null) {
                Item item = value.evaluateToItem(ctx, tuple);
                if (item != null) {
                  return item;
                }
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

  /**
   * An unboxed array that advertises its backend's member splitting.
   *
   * <p>Iteration is delegated untouched to the ordinary unboxed view, so this changes what a
   * consumer can <em>ask</em> for and nothing about what it gets.
   */
  private static final class SplittableUnboxedArray extends LazySequence implements SplittableSequence {
    private final LazySequence unboxed;
    private final SplittableMembers array;

    SplittableUnboxedArray(final LazySequence unboxed, final SplittableMembers array) {
      this.unboxed = unboxed;
      this.array = array;
    }

    @Override
    public Iter iterate() {
      return unboxed.iterate();
    }

    @Override
    public int splitCount(final int preferred) {
      return array.memberSplitCount(preferred);
    }

    @Override
    public Sequence split(final int index, final int total) {
      return array.memberSplit(index, total);
    }
  }
}
