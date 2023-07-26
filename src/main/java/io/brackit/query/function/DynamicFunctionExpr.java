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
package io.brackit.query.function;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.*;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.ItemType;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.sequence.FunctionConversionSequence;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.util.ExprUtil;
import io.brackit.query.QueryException;
import io.brackit.query.compiler.Bits;
import org.magicwerk.brownies.collections.GapList;

/**
 * @author Johannes Lichtenberger
 */
public class DynamicFunctionExpr implements Expr {
  private final StaticContext sctx;
  private final Expr functionExpr;
  private final Expr[] arguments;

  public DynamicFunctionExpr(StaticContext sctx, Expr function, Expr... exprs) {
    this.sctx = sctx;
    this.functionExpr = function;
    this.arguments = exprs;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    final var functionItem = functionExpr.evaluateToItem(ctx, tuple);

    final var argumentsSize = arguments.length;
    if (functionItem instanceof Array array) {
      if (argumentsSize == 0) {
        final var it = array.iterate();

        final var buffer = new GapList<Item>(array.len());
        Item item;
        while ((item = it.next()) != null) {
          buffer.add(item);
        }
        return new ItemSequence(buffer.toArray(new Item[0]));
      }

      if (argumentsSize == 1) {
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
      if (argumentsSize == 0) {
        final var names = object.names();
        final var buffer = new GapList<Item>(names.len());

        for (int i = 0; i < names.len(); i++) {
          buffer.add(names.at(i).evaluateToItem(ctx, tuple));
        }

        return new ItemSequence(buffer.toArray(new Item[0]));
      }

      if (argumentsSize == 1) {
        final var fieldItem = arguments[0].evaluateToItem(ctx, tuple);

        return getSequenceByObjectField(object, fieldItem);
      }

      // TODO / FIXME
      throw new QueryException(new QNm(""));
    }

    if (functionItem instanceof Function function) {
      int pos = 0;
      for (Sequence sequence : tuple.array()) {
        if (sequence == functionItem) {
          break;
        }
        pos++;
      }

      final ItemType dftCtxItemType = function.getSignature().defaultCtxItemType();
      final SequenceType dftCtxType;
      if (dftCtxItemType != null) {
        dftCtxType = new SequenceType(dftCtxItemType, Cardinality.One);
      } else {
        dftCtxType = null;
      }

      Sequence res;
      Sequence[] args;

      if (dftCtxType != null) {
        Item ctxItem = arguments[0].evaluateToItem(ctx, tuple);
        FunctionConversionSequence.asTypedSequence(dftCtxType, ctxItem, false);
        args = new Sequence[] { ctxItem };
      } else {
        SequenceType[] params = function.getSignature().getParams();
        args = new Sequence[arguments.length + pos];

        for (int i = 0; i < pos; i++) {
          args[i] = tuple.get(i);
        }

        for (int i = 0; i < arguments.length; i++) {
          SequenceType sType = i < params.length ? params[i] : params[params.length - 1];
          if (sType.getCardinality().many()) {
            args[pos + i] = arguments[i].evaluate(ctx, tuple);
            if (!sType.getItemType().isAnyItem()) {
              args[pos + i] = FunctionConversionSequence.asTypedSequence(sType, args[i], false);
            }
          } else {
            args[pos + i] = arguments[i].evaluateToItem(ctx, tuple);
            args[pos + i] = FunctionConversionSequence.asTypedSequence(sType, args[i], false);
          }
        }
      }

      try {
        res = function.execute(sctx, ctx, args);
      } catch (StackOverflowError e) {
        throw new QueryException(e,
                                 ErrorCode.BIT_DYN_RT_STACK_OVERFLOW,
                                 "Execution of function '%s' was aborted because of too deep recursion.",
                                 function.getName());
      }
      if (function.isBuiltIn()) {
        return res;
      }
      res = FunctionConversionSequence.asTypedSequence(function.getSignature().getResultType(), res, false);

      return ExprUtil.materialize(res);
    }

    // TODO / FIXME
    throw new QueryException(new QNm(""));
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
    return functionExpr.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    return functionExpr.toString();
  }
}