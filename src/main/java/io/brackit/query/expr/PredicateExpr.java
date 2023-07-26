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

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public abstract class PredicateExpr implements Expr {

  protected final Expr[] filter;
  protected final boolean[] bindItem;
  protected final boolean[] bindPos;
  protected final boolean[] bindSize;
  protected final int[] bindCount;

  public PredicateExpr(Expr[] filter, boolean[] bindItem, boolean[] bindPos, boolean[] bindSize) {
    this.filter = filter;
    this.bindItem = bindItem;
    this.bindPos = bindPos;
    this.bindSize = bindSize;
    this.bindCount = new int[filter.length];
    for (int i = 0; i < filter.length; i++) {
      bindCount[i] = (bindItem[i] ? 1 : 0) + (bindPos[i] ? 1 : 0) + (bindSize[i] ? 1 : 0);
    }
  }

  protected class DependentFilterSeq extends LazySequence {
    private final QueryContext ctx;
    private final Tuple tuple;
    private final Sequence s;
    private final int i;
    private final IntNumeric inSeqSize;

    public DependentFilterSeq(QueryContext ctx, Tuple tuple, Sequence s, int i) {
      this.ctx = ctx;
      this.tuple = tuple;
      this.s = s;
      this.i = i;
      this.inSeqSize = (bindSize[i] ? (s != null) ? s.size() : Int32.ZERO : Int32.ONE);
    }

    @Override
    public Iter iterate() {
      return new BaseIter() {
        IntNumeric pos;
        Iter it;

        @Override
        public Item next() {
          if (pos == null) {
            if (s instanceof Item) {
              pos = Int32.ONE;
              if (predicate((Item) s)) {
                // include single item in result
                return (Item) s;
              }
              return null;
            } else if (s != null) {
              pos = Int32.ZERO;
              it = s.iterate();
            }
          }

          if (it == null) {
            return null;
          }

          Item n;
          while ((n = it.next()) != null) {
            pos = pos.inc();

            if (predicate(n)) {
              // include single item in result
              return n;
            }
          }
          it.close();
          return null;
        }

        private boolean predicate(Item item) {
          Tuple current = tuple;

          if (bindCount[i] > 0) {
            Sequence[] tmp = new Sequence[bindCount[i]];
            int p = 0;
            if (bindItem[i]) {
              tmp[p++] = item;
            }
            if (bindPos[i]) {
              tmp[p++] = pos;
            }
            if (bindSize[i]) {
              tmp[p] = inSeqSize;
            }
            current = current.concat(tmp);
          }

          Sequence res = filter[i].evaluate(ctx, current);

          if (res == null) {
            return false;
          }

          if (res instanceof Numeric) {
            return ((Numeric) res).cmp(pos) == 0;
          } else {
            try (Iter it = res.iterate()) {
              Item first = it.next();
              if ((first != null) && (it.next() == null) && (first instanceof Numeric) && (((Numeric) first).cmp(pos)
                  != 0)) {
                return false;
              }
            }

            return res.booleanValue();
          }
        }

        @Override
        public void close() {
          if (it != null) {
            it.close();
          }
        }
      };
    }
  }

  @Override
  public boolean isUpdating() {
    for (Expr e : filter) {
      if (e.isUpdating()) {
        return true;
      }
    }
    return false;
  }
}