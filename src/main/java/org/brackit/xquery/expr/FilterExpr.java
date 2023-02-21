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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.jdm.Expr;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class FilterExpr extends PredicateExpr {

  final Expr expr;

  public FilterExpr(Expr expr, Expr[] filter, boolean[] bindItem, boolean[] bindPos, boolean[] bindSize) {
    super(filter, bindItem, bindPos, bindSize);
    this.expr = expr;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) {
    Sequence s = expr.evaluate(ctx, tuple);

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
          s = pos != null ? s.get(pos) : null;
        } else {
          try (Iter it = fs.iterate()) {
            Item first = it.next();
            if (first != null && it.next() == null && first instanceof Numeric) {
              IntNumeric pos = ((Numeric) first).asIntNumeric();
              return pos != null ? s.get(pos) : null;
            }
          }
          if (!fs.booleanValue()) {
            return null;
          }
        }
      } else {
        // the filter predicate is dependent on the context item
        s = new DependentFilterSeq(ctx, tuple, s, i);
      }
    }
    return s;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Sequence s = expr.evaluate(ctx, tuple);

    for (int i = 0; i < filter.length; i++) {
      if (s == null) {
        return null;
      } else if (s instanceof Item) {
        Tuple current = tuple;

        if (bindCount[i] > 0) {
          Sequence[] tmp = new Sequence[bindCount[i]];
          int p = 0;
          if (bindItem[i]) {
            tmp[p++] = s;
          }
          if (bindPos[i]) {
            tmp[p++] = Int32.ONE;
          }
          if (bindSize[i]) {
            tmp[p] = Int32.ONE;
          }
          current = current.concat(tmp);
        }

        Sequence fRes = filter[i].evaluate(ctx, current);

        if (fRes == null) {
          return null;
        }

        if (fRes instanceof Numeric && ((Numeric) fRes).intValue() != 1) {
          return null;
        }

        if (!fRes.booleanValue()) {
          return null;
        }
      } else {
        s = ExprUtil.asItem(evaluate(ctx, tuple));
      }
    }
    return (Item) s;
  }

  @Override
  public boolean isUpdating() {
    if (expr.isUpdating()) {
      return true;
    }
    return super.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    return expr.toString() + '[' + filter + ']';
  }
}
