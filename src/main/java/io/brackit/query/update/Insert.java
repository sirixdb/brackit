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

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.expr.ConstructedNodeBuilder;
import io.brackit.query.update.op.AbstractInsertOp;
import io.brackit.query.update.op.InsertAfterOp;
import io.brackit.query.update.op.InsertAttributesOp;
import io.brackit.query.update.op.InsertBeforeOp;
import io.brackit.query.update.op.InsertIntoAsFirstOp;
import io.brackit.query.update.op.InsertIntoAsLastOp;
import io.brackit.query.update.op.InsertIntoOp;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public final class Insert extends ConstructedNodeBuilder implements Expr {
  public enum InsertType {
    FIRST, LAST, INTO, BEFORE, AFTER
  }

  private static final EnumSet<Kind> abNodeKind = EnumSet.of(Kind.ELEMENT,
                                                             Kind.TEXT,
                                                             Kind.COMMENT,
                                                             Kind.PROCESSING_INSTRUCTION);

  private static final EnumSet<Kind> intoNodeKind = EnumSet.of(Kind.DOCUMENT, Kind.ELEMENT);

  private final Expr sourceExpr;

  private final Expr targetExpr;

  private final InsertType insertType;

  public Insert(Expr sourceExpr, Expr targetExpr, InsertType insertType) {
    this.sourceExpr = sourceExpr;
    this.targetExpr = targetExpr;
    this.insertType = insertType;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    if ((insertType == InsertType.AFTER) || (insertType == InsertType.BEFORE)) {
      insertAB(ctx, tuple);
    } else {
      insertInto(ctx, tuple);
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

  private void insertInto(QueryContext ctx, Tuple tuple) {
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
          throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE);
        }
      }
    }
    if (!(targetItem instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE,
                               "Target item is atomic value %s",
                               targetItem);
    }

    node = (Node<?>) targetItem;

    if (!intoNodeKind.contains(node.getKind())) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE,
                               "Target node kind %s is not allowed for insert type: %s",
                               node.getKind(),
                               insertType);
    }

    AbstractInsertOp opOp = null;
    InsertAttributesOp insertAttsOp = null;
    Sequence source = sourceExpr.evaluate(ctx, tuple);
    ContentList cList = new ContentList();
    buildContentSequence(ctx, cList, source);

    for (Node<?> insertNode : cList) {
      if (insertNode.getKind() == Kind.ATTRIBUTE) {
        if (insertAttsOp == null) {
          if (node.getKind() != Kind.ELEMENT) {
            throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_INTO_TARGET_IS_DOCUMENT_NODE);
          }

          insertAttsOp = new InsertAttributesOp(node);
        }
        insertAttsOp.addContent(insertNode);
      } else {
        if (opOp == null) {
          opOp = createOpOp(node);
        }
        opOp.addContent(insertNode);
      }
    }

    if (insertAttsOp != null) {
      ctx.addPendingUpdate(insertAttsOp);
    }
    if (opOp != null) {
      ctx.addPendingUpdate(opOp);
    }
  }

  private void insertAB(QueryContext ctx, Tuple tuple) {
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
          throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ETCP_NODE);
        }
      }
    }
    if (!(targetItem instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ETCP_NODE,
                               "Target item is atomic value %s",
                               targetItem);
    }

    node = (Node<?>) targetItem;

    if (!abNodeKind.contains(node.getKind())) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ETCP_NODE,
                               "Target node kind %s is not allowed for insert type: %",
                               node.getKind(),
                               insertType);
    }

    parent = node.getParent();

    if (parent == null) {
      throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_TARGET_NODE_HAS_NO_PARENT);
    }

    AbstractInsertOp opOp = null;
    InsertAttributesOp insertAttsOp = null;
    Sequence source = sourceExpr.evaluate(ctx, tuple);
    ContentList cList = new ContentList();
    buildContentSequence(ctx, cList, source);

    for (Node<?> insertNode : cList) {
      if (insertNode.getKind() == Kind.ATTRIBUTE) {
        if (insertAttsOp == null) {
          if (parent.getKind() != Kind.ELEMENT) {
            throw new QueryException(ErrorCode.ERR_UPDATE_INSERT_BEFORE_AFTER_TARGET_PARENT_IS_DOCUMENT_NODE);
          }

          insertAttsOp = new InsertAttributesOp(parent);
        }
        insertAttsOp.addContent(insertNode);
      } else {
        if (opOp == null) {
          opOp = createOpOp(node);
        }
        opOp.addContent(insertNode);
      }
    }

    if (insertAttsOp != null) {
      ctx.addPendingUpdate(insertAttsOp);
    }
    if (opOp != null) {
      ctx.addPendingUpdate(opOp);
    }
  }

  private AbstractInsertOp createOpOp(Node<?> node) {
    return switch (insertType) {
      case AFTER -> new InsertAfterOp(node);
      case BEFORE -> new InsertBeforeOp(node);
      case FIRST -> new InsertIntoAsFirstOp(node);
      case LAST -> new InsertIntoAsLastOp(node);
      case INTO -> new InsertIntoOp(node);
      default -> throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR);
    };
  }
}
