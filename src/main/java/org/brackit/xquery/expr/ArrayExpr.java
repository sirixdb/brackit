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

import org.brackit.xquery.BrackitQueryContext;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.jsonitem.array.DArray;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.magicwerk.brownies.collections.GapList;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class ArrayExpr implements Expr {

  private final Expr[] expr;

  private final boolean[] flatten;

  public ArrayExpr(Expr[] expr, boolean[] flatten) {
    this.expr = expr;
    this.flatten = flatten;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple t) {
    return evaluateToItem(ctx, t);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple t) {
    final var vals = new GapList<Sequence>();
    for (int i = 0; i < expr.length; i++) {
      final Sequence res = expr[i].evaluate(ctx, t);
      if (res == null) {
        continue;
      }

      if (!(res instanceof SequenceExpr.EvalSequence) && !flatten[i] || res instanceof Item) {
        vals.add(res);
      } else {
        try (final Iter it = res.iterate()) {
          Item item;
          while ((item = it.next()) != null) {
            vals.add(item);
          }
        }
      }
    }
    return new DArray(vals);
  }

  @Override
  public boolean isUpdating() {
    for (final Expr e : this.expr) {
      if (e.isUpdating()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isVacuous() {
    for (final Expr e : this.expr) {
      if (!e.isVacuous()) {
        return false;
      }
    }
    return true;
  }

  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append("[");
    boolean first = true;
    for (int i = 0; i < expr.length; i++) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      if (flatten[i]) {
        out.append("=");
      }
      out.append(expr[i].toString());
    }
    out.append("]");
    return out.toString();
  }

  public static void main(String[] args) {
    new XQuery("[ 1, '2', 3, (1 > 0) cast as xs:boolean, 1.2343 + 5, =(1,2,3)  ][[4]]").serialize(new BrackitQueryContext(),
                                                                                                  System.out);
  }
}
