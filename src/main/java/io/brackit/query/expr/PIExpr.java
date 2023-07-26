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

import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class PIExpr extends ConstructedNodeBuilder implements Expr {
  protected final QNm name;
  protected final Expr nameExpr;
  protected final Expr contentExpr;
  protected final boolean appendOnly;

  public PIExpr(Expr nameExpr, Expr contentExpr, boolean appendOnly) {
    this.nameExpr = nameExpr;
    this.contentExpr = contentExpr;
    this.appendOnly = appendOnly;
    this.name = (QNm) ((nameExpr instanceof QNm) ? nameExpr : null);
  }

  @Override
  public final Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    // See XQuery 3.7.3.5 Computed PI Constructors
    QNm target = (this.name != null) ? this.name : buildPITarget(nameExpr.evaluateToItem(ctx, tuple));

    Sequence sequence = contentExpr.evaluate(ctx, tuple);
    String content = buildPIContent(sequence);

    if (appendOnly) {
      ((Node<?>) tuple.get(tuple.getSize() - 1)).append(Kind.PROCESSING_INSTRUCTION, target, new Str(content));
      return null;
    }

    Node<?> attribute = ctx.getNodeFactory().pi(target, new Str(content));
    return attribute;
  }

  @Override
  public boolean isUpdating() {
    return ((nameExpr.isUpdating()) || (contentExpr.isUpdating()));
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
