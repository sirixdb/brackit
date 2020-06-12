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
package org.brackit.xquery.update;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.update.op.DeleteOp;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class Delete implements Expr {
  private final Expr expr;

  public Delete(Expr expr) {
    this.expr = expr;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Sequence result = expr.evaluate(ctx, tuple);

    if (result == null) {
      return null;
    }

    if (result instanceof Item) {
      handleItem(ctx, (Item) result);
      return null;
    }

    try (Iter it = result.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        handleItem(ctx, item);
      }
    }

    return null;
  }

  private void handleItem(QueryContext ctx, Item item) {
    if (!(item instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_DELETE_TARGET_NOT_A_NODE_SEQUENCE,
                               "Target item for delete is not a node: %s",
                               item);
    }
    ctx.addPendingUpdate(new DeleteOp((Node<?>) item));
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
