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
import org.brackit.xquery.update.json.op.InsertIntoArrayOp;
import org.brackit.xquery.update.json.op.InsertIntoRecordOp;
import org.brackit.xquery.jdm.Expr;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.json.Array;
import org.brackit.xquery.jdm.json.Object;

/**
 * @author Johannes Lichtenberger
 */
public final class InsertJson implements Expr {
  private final Expr sourceExpr;

  private final Expr targetExpr;

  private final int position;

  public InsertJson(Expr sourceExpr, Expr targetExpr, int position) {
    this.sourceExpr = sourceExpr;
    this.targetExpr = targetExpr;
    this.position = position;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    insertInto(ctx, tuple);
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

  private void insertInto(QueryContext ctx, Tuple tuple) {
    final Sequence target = targetExpr.evaluate(ctx, tuple);
    Item targetItem;

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
          throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE);
        }
      }
    }

    final Sequence source = sourceExpr.evaluateToItem(ctx, tuple);

    if (target instanceof Object) {
      ctx.addPendingUpdate(new InsertIntoRecordOp((Object) targetItem, (Object) source));
    } else {
      // array
      if (!(targetItem instanceof Array)) {
        throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE,
                                 "Target item is atomic value %s",
                                 targetItem);
      }

      ctx.addPendingUpdate(new InsertIntoArrayOp((Array) targetItem, source, position));
    }
  }
}
