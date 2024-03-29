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
package io.brackit.query.expr;

import io.brackit.query.atomic.Bool;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class NodeCmpExpr implements Expr {
  public enum NodeCmp {
    Is {
      @Override
      public Bool compare(QueryContext ctx, Node<?> left, Node<?> right) {
        return left.equals(right) ? Bool.TRUE : Bool.FALSE;
      }
    },
    Preceding {
      @Override
      public Bool compare(QueryContext ctx, Node<?> left, Node<?> right) {
        return left.isPrecedingOf(right) ? Bool.TRUE : Bool.FALSE;
      }
    },
    Following {
      @Override
      public Bool compare(QueryContext ctx, Node<?> left, Node<?> right) {
        return left.isFollowingOf(right) ? Bool.TRUE : Bool.FALSE;
      }
    };

    public abstract Bool compare(QueryContext ctx, Node<?> left, Node<?> right) throws QueryException;
  }

  protected final NodeCmp nodeCmp;
  protected final Expr leftExpr;
  protected final Expr rightExpr;

  public NodeCmpExpr(NodeCmp nodeCmp, Expr leftExpr, Expr rightExpr) {
    this.nodeCmp = nodeCmp;
    this.leftExpr = leftExpr;
    this.rightExpr = rightExpr;
  }

  @Override
  public final Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    Item left = leftExpr.evaluateToItem(ctx, tuple);
    Item right = rightExpr.evaluateToItem(ctx, tuple);

    if ((left == null) || (right == null)) {
      return null;
    }

    if (!(left instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Left argument is not a node: %s", left);
    }

    if (!(right instanceof Node<?>)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "right argument is not a node: %s", right);
    }

    return nodeCmp.compare(ctx, (Node<?>) left, (Node<?>) right);
  }

  @Override
  public boolean isUpdating() {
    return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
