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

import io.brackit.query.util.Cmp;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class VCmpExpr implements Expr {
  protected final Cmp cmp;
  protected final Expr leftExpr;
  protected final Expr rightExpr;

  public VCmpExpr(Cmp cmp, Expr leftExpr, Expr rightExpr) {
    this.leftExpr = leftExpr;
    this.rightExpr = rightExpr;
    this.cmp = cmp;
  }

  @Override
  public final Sequence evaluate(QueryContext ctx, Tuple tuple) {
    return evaluateToItem(ctx, tuple);
  }

  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    // See XQuery 3.5 Comparison Expressions
    // Begin evaluate operands 3.5.1
    // TODO Is it legal to evaluate to item first, which may cause an
    // err:XPTY0004 before atomization which may cause an err:FOTY0012?
    Item left = leftExpr.evaluateToItem(ctx, tuple);
    Item right = rightExpr.evaluateToItem(ctx, tuple);
    return cmp.vCmpAsBool(ctx, left, right);
  }

  @Override
  public boolean isUpdating() {
    return ((leftExpr.isUpdating()) || (rightExpr.isUpdating()));
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    return leftExpr + " " + cmp + " " + rightExpr;
  }

}
