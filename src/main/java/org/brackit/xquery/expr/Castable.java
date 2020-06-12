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
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * @author Sebastian Baechle
 */
public class Castable implements Expr {
  private final StaticContext sctx;
  private final Expr expr;
  private final Type target;
  private final boolean allowEmptySequence;

  public Castable(StaticContext sctx, Expr expr, Type targetType, boolean allowEmptySequence) {
    this.sctx = sctx;
    this.expr = expr;
    this.target = targetType;
    this.allowEmptySequence = allowEmptySequence;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Sequence sequence = expr.evaluate(ctx, tuple);

    if (sequence == null) {
      return (allowEmptySequence) ? Bool.TRUE : Bool.FALSE;
    }

    Item item = null;

    if (sequence instanceof Item) {
      item = (Item) sequence;
    } else {
      Iter it = sequence.iterate();
      try {
        item = it.next();

        if (item == null) {
          return (allowEmptySequence) ? Bool.TRUE : Bool.FALSE;
        }

        if (it.next() != null) {
          return Bool.FALSE;
        }
      } finally {
        it.close();
      }
    }

    try {
      Cast.cast(sctx, item, target, allowEmptySequence);
      return Bool.TRUE;
    } catch (QueryException e) {
      QNm code = e.getCode();

      if ((code.eq(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE)) || (code.eq(ErrorCode.ERR_UNKNOWN_ATOMIC_SCHEMA_TYPE))
          || (code.eq(ErrorCode.ERR_INVALID_LEXICAL_VALUE)) || (code.eq(ErrorCode.ERR_INVALID_VALUE_FOR_CAST))
          || (code.eq(ErrorCode.ERR_ILLEGAL_CAST_TARGET_TYPE))) {
        return Bool.FALSE;
      }

      throw e;
    }
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
