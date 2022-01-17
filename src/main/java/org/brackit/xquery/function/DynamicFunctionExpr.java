/*
 * [New BSD License]
 * Copyright (c) 2011-2022, Brackit Project Team <info@brackit.org>
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
package org.brackit.xquery.function;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.Object;
import org.magicwerk.brownies.collections.GapList;

import javax.management.Query;

/**
 * @author Johannes Lichtenberger
 */
public class DynamicFunctionExpr implements Expr {
  private final StaticContext sctx;
  private final Expr function;
  private final Expr[] arguments;

  public DynamicFunctionExpr(StaticContext sctx, Expr function, Expr... exprs) {
    this.sctx = sctx;
    this.function = function;
    this.arguments = exprs;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    final var functionItem = function.evaluateToItem(ctx, tuple);

    if (functionItem instanceof Array array) {
      if (arguments.length == 0) {
        final var it = array.iterate();

        final var buffer = new GapList<Item>(array.len());
        Item item;
        while ((item = it.next()) != null) {
          buffer.add(item);
        }
        return new ItemSequence(buffer.toArray(new Item[0]));
      }

      if (arguments.length == 1) {
        final var indexItem = arguments[0].evaluateToItem(ctx, tuple);

        if (!(indexItem instanceof IntNumeric)) {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Illegal operand type '%s' where '%s' is expected",
                                   indexItem.itemType(),
                                   Type.INR);
        }

        final int index = ((IntNumeric) indexItem).intValue();

        return array.at(index);
      }

      // TODO / FIXME
      throw new QueryException(new QNm(""));
    }

    if (functionItem instanceof Object object) {
      if (arguments.length == 0) {
        final var names = object.names();
        final var buffer = new GapList<Item>(names.len());

        for (int i = 0; i < names.len(); i++) {
          buffer.add(names.at(i).evaluateToItem(ctx, tuple));
        }

        return new ItemSequence(buffer.toArray(new Item[0]));
      }

      if (arguments.length == 1) {
        final var fieldItem = arguments[0].evaluateToItem(ctx, tuple);

        return getSequenceByObjectField(object, fieldItem);
      }

      // TODO / FIXME
      throw new QueryException(new QNm(""));
    }

    return null;
  }

  private Sequence getSequenceByObjectField(Object object, Item itemField) {
    if (itemField instanceof QNm qNmField) {
      return object.get(qNmField);
    } else if (itemField instanceof IntNumeric intNumericField) {
      return object.value(intNumericField);
    } else if (itemField instanceof Atomic atomicField) {
      return object.get(new QNm(atomicField.stringValue()));
    } else {
      throw new QueryException(Bits.BIT_ILLEGAL_OBJECT_FIELD, "Illegal object itemField reference: %s", itemField);
    }
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    return function.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    return function.toString();
  }
}