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
import org.brackit.xquery.array.DArray;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.*;
import org.brackit.xquery.xdm.json.Array;
import org.magicwerk.brownies.collections.GapList;

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
    final Item array = expr.evaluateToItem(ctx, tuple);
    if (array == null) {
      return null;
    }
    if (!(array instanceof Array)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               array.itemType(),
                               Type.INR);
    }
    final Item i = index.evaluateToItem(ctx, tuple);
    if (i == null) {
      final var it = array.iterate();

      final var buffer = new GapList<Item>(((Array) array).len());
      Item item;
      while ((item = it.next()) != null) {
        buffer.add(item);
      }
      return new ItemSequence(buffer.toArray(new Item[0]));
    }
    if (!(i instanceof IntNumeric)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Illegal operand type '%s' where '%s' is expected",
                               i.itemType(),
                               Type.INR);
    }
    return ((Array) array).at((IntNumeric) i);
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
    return expr.isUpdating() || index.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
