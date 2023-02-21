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

import java.util.EnumSet;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.expr.ConstructedNodeBuilder;
import org.brackit.xquery.update.op.ReplaceNodeOp;
import org.brackit.xquery.jdm.Expr;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Kind;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class ReplaceNode extends ConstructedNodeBuilder implements Expr {
  private static final EnumSet<Kind> replaceNodeKind =
      EnumSet.of(Kind.ELEMENT, Kind.ATTRIBUTE, Kind.TEXT, Kind.COMMENT, Kind.PROCESSING_INSTRUCTION);

  private static final EnumSet<Kind> allowedForReplaceNonAtt =
      EnumSet.of(Kind.ELEMENT, Kind.TEXT, Kind.COMMENT, Kind.PROCESSING_INSTRUCTION);

  private final Expr sourceExpr;

  private final Expr targetExpr;

  public ReplaceNode(Expr sourceExpr, Expr targetExpr) {
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
    Node<?> parent;

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
    if (!(targetItem instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE,
                               "Target item is atomic value %s",
                               targetItem);
    }

    node = (Node<?>) targetItem;

    if (!allowedForReplaceNonAtt.contains(node.getKind())) {
      throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE,
                               "Target node kind %s is not allowed for replace node: %s",
                               node.getKind(),
                               node);
    }

    parent = node.getParent();

    if (parent == null) {
      throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_TARGET_NODE_HAS_NO_PARENT);
    }

    Sequence source = sourceExpr.evaluate(ctx, tuple);
    ContentList rList = new ContentList();
    buildContentSequence(ctx, rList, source);
    ReplaceNodeOp op = null;

    if (node.getKind() != Kind.ATTRIBUTE) {
      for (Node<?> replacement : rList) {
        if (!allowedForReplaceNonAtt.contains(replacement.getKind())) {
          throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_NODE_REPLACEMENT_NOT_A_ETCP_NODE,
                                   "Cannot replace node of type %s with node of type %s",
                                   node.getKind(),
                                   replacement.getKind());
        }
        if (op == null) {
          op = new ReplaceNodeOp(node);
        }
        op.addContent(replacement);
      }
    } else {
      for (Node<?> replacement : rList) {
        if (replacement.getKind() != Kind.ATTRIBUTE) {
          throw new QueryException(ErrorCode.ERR_UPDATE_REPLACE_NODE_REPLACEMENT_NOT_AN_A_NODE,
                                   "Cannot replace attribute with node of type %s",
                                   replacement.getKind());
        }
        if (op == null) {
          op = new ReplaceNodeOp(node);
        }
        op.addContent(replacement);
      }
    }

    if (op != null) {
      ctx.addPendingUpdate(op);
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
