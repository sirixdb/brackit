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
package io.brackit.query.update;

import java.util.EnumSet;

import io.brackit.query.update.op.RenameOp;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.QNm;
import io.brackit.query.expr.ConstructedNodeBuilder;
import io.brackit.query.module.StaticContext;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class Rename extends ConstructedNodeBuilder implements Expr {
  private static final EnumSet<Kind> renameNodeKind = EnumSet.of(Kind.ELEMENT,
                                                                 Kind.ATTRIBUTE,
                                                                 Kind.PROCESSING_INSTRUCTION);

  private final StaticContext sctx;
  private final Expr sourceExpr;
  private final Expr targetExpr;

  public Rename(StaticContext sctx, Expr sourceExpr, Expr targetExpr) {
    this.sctx = sctx;
    this.sourceExpr = sourceExpr;
    this.targetExpr = targetExpr;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Sequence target = targetExpr.evaluate(ctx, tuple);
    Item targetItem;
    Node<?> node;

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
          throw new QueryException(ErrorCode.ERR_UPDATE_RENAME_TARGET_NOT_A_EAP_NODE);
        }
      }
    }
    if (!(targetItem instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_RENAME_TARGET_NOT_A_EAP_NODE,
                               "Target item is atomic value %s",
                               targetItem);
    }

    node = (Node<?>) targetItem;

    if (!renameNodeKind.contains(node.getKind())) {
      throw new QueryException(ErrorCode.ERR_UPDATE_RENAME_TARGET_NOT_A_EAP_NODE,
                               "Target node kind is not allowed for rename node: %s",
                               node.getKind());
    }

    QNm name;
    Item nameItem = sourceExpr.evaluateToItem(ctx, tuple);
    if (node.getKind() == Kind.ELEMENT) {
      name = buildElementName(sctx, nameItem);
    } else if (node.getKind() == Kind.ATTRIBUTE) {
      name = buildAttributeName(sctx, nameItem);
    } else {
      name = buildPITarget(nameItem);
    }

    ctx.addPendingUpdate(new RenameOp(node, name));
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
