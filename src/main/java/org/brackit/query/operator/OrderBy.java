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
package org.brackit.query.operator;

import org.brackit.query.QueryContext;
import org.brackit.query.Tuple;
import org.brackit.query.util.sort.Ordering;
import org.brackit.query.util.sort.Ordering.OrderModifier;
import org.brackit.query.jdm.Expr;
import org.brackit.query.jdm.Stream;

/**
 * @author Sebastian Baechle
 */
public class OrderBy extends Check implements Operator {

  private class OrderByCursor implements Cursor {
    private final Cursor c;
    private Stream<? extends Tuple> sorted;
    private Tuple next;

    public OrderByCursor(Cursor c) {
      this.c = c;
    }

    @Override
    public void close(QueryContext ctx) {
      if (sorted != null) {
        sorted.close();
      }
      c.close(ctx);
    }

    public Tuple next(QueryContext ctx) {
      Tuple t;
      if (sorted != null) {
        t = sorted.next();
        if (t != null) {
          return t;
        }
        sorted.close();
      }
      if ((t = next) == null && (t = c.next(ctx)) == null) {
        return null;
      }
      next = null;

      // pass through
      if (check && dead(t)) {
        return t;
      }

      // sort current tuple and all following in same group
      Ordering sort = new Ordering(orderByExprs, modifier);
      sort.add(ctx, t);
      while ((next = c.next(ctx)) != null) {
        if (check && separate(t, next)) {
          break;
        }
        sort.add(ctx, next);
      }
      sorted = sort.sorted();
      t = sorted.next();
      return t;
    }

    @Override
    public void open(QueryContext ctx) {
      c.open(ctx);
    }
  }

  final Operator in;
  final Expr[] orderByExprs;
  final OrderModifier[] modifier;

  public OrderBy(Operator in, Expr[] orderByExprs, OrderModifier[] orderBySpec) {
    this.in = in;
    this.orderByExprs = orderByExprs;
    this.modifier = orderBySpec;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    return new OrderByCursor(in.create(ctx, tuple));
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    return new OrderByCursor(in.create(ctx, buf, len));
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize);
  }
}
