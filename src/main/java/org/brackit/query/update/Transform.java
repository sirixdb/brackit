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
package org.brackit.query.update;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;
import org.brackit.query.expr.ConstructedNodeBuilder;
import org.brackit.query.update.op.UpdateOp;
import org.brackit.query.jdm.Expr;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.node.Node;

import java.util.ArrayList;

public class Transform extends ConstructedNodeBuilder implements Expr {
  private final Expr[] copyBindings;
  private final Expr modifyExpr;
  private final Expr returnExpr;
  private final boolean[] referenced;

  public Transform(Expr[] copyBindings, boolean[] referenced, Expr modifyExpr, Expr returnExpr) {
    this.copyBindings = copyBindings;
    this.referenced = referenced;
    this.modifyExpr = modifyExpr;
    this.returnExpr = returnExpr;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return returnExpr.evaluate(ctx, copyAndModify(ctx, tuple));
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return returnExpr.evaluateToItem(ctx, copyAndModify(ctx, tuple));
  }

  private Tuple copyAndModify(QueryContext ctx, Tuple tuple) {

    // backup current pending update list
    final UpdateList saved = ctx.getUpdateList();
    final UpdateList mods = new UpdateList();
    ctx.setUpdateList(mods);
    // create copy bindings
    var nodeCoies = new ArrayList<Node<?>>(copyBindings.length);
    for (int i = 0; i < copyBindings.length; i++) {
      if (referenced[i]) {
        Item item = copyBindings[i].evaluateToItem(ctx, tuple);
        if (!(item instanceof Node<?>)) {
          throw (new QueryException(ErrorCode.ERR_TRANSFORM_SOURCE_EXPRESSION_NOT_SINGLE_NODE,
                                    "Source expression must evaluate to single node."));
        }
        Node<?> copy = copy(ctx, (Node<?>) item);
        nodeCoies.add(copy);
        tuple = tuple.concat(copy);
      }
    }
    // evaluate transform expression
    modifyExpr.evaluateToItem(ctx, tuple);
    // ensure that only copies are affected by the transformation
    for (final UpdateOp op : mods.list()) {
      if (op.getTarget() instanceof Node<?>) {
        boolean ok = false;
        for (Node<?> copy : nodeCoies) {
          if (copy.isAncestorOrSelfOf((Node<?>) op.getTarget())) {
            ok = true;
            break;
          }
        }

        if (!ok) {
          throw new QueryException(ErrorCode.ERR_TRANSFORM_MODIFIES_EXISTING_NODE,
                                   "Modify clause update expressions may not affect existing nodes.");
        }
      }
    }
    // apply transformation
    ctx.applyUpdates();
    // reinstall backup of pending updates
    ctx.setUpdateList(saved);

    return tuple;
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