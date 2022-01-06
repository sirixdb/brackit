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
package org.brackit.xquery.update.json;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.expr.ConstructedNodeBuilder;
import org.brackit.xquery.update.json.op.ReplaceArrayValueOp;
import org.brackit.xquery.update.json.op.ReplaceRecordValueOp;
import org.brackit.xquery.xdm.*;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.JsonItem;
import org.brackit.xquery.xdm.json.Object;

/**
 * @author Johannes Lichtenberger
 */
public final class ReplaceJsonValue extends ConstructedNodeBuilder implements Expr {
  private final Expr sourceExpr;

  private final Expr targetExpr;

  private final Expr recordFieldOrArrayIndex;

  public ReplaceJsonValue(Expr sourceExpr, Expr targetExpr, Expr recordFieldOrArrayIndex) {
    this.sourceExpr = sourceExpr;
    this.targetExpr = targetExpr;
    this.recordFieldOrArrayIndex = recordFieldOrArrayIndex;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    final Sequence target = targetExpr.evaluate(ctx, tuple);
    final Item targetItem;

    if (target == null) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_IS_EMPTY_SEQUENCE);
    } else if (target instanceof Item) {
      targetItem = (Item) target;
    } else {
      try (Iter it = target.iterate()) {
        targetItem = it.next();

        if (targetItem == null) {
          throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_IS_EMPTY_SEQUENCE);
        }
        if (it.next() != null) {
          throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE);
        }
      }
    }
    if (!(targetItem instanceof JsonItem)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE,
                               "Target item is atomic value %s",
                               targetItem);
    }

    final Sequence recordFieldOrArrayIndexSeq = recordFieldOrArrayIndex.evaluateToItem(ctx, tuple);
    final Sequence source = sourceExpr.evaluateToItem(ctx, tuple);

    if (target instanceof Array) {
      final Int32 index = (Int32) recordFieldOrArrayIndexSeq;
      ctx.addPendingUpdate(new ReplaceArrayValueOp((Array) target, index.intValue(), source));
    } else {
      final QNm field = (QNm) recordFieldOrArrayIndexSeq;
      ctx.addPendingUpdate(new ReplaceRecordValueOp((Object) target, field, source));
    }

    return null;
  }

  @Override
  public boolean isUpdating() {
    return true;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}