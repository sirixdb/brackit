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
package io.brackit.query.function;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

/**
 * Expression that creates a partially applied function from a dynamically resolved function.
 * Used for expressions like $fun(2, ?, 7) where $fun is a function-valued variable.
 *
 * @author Johannes Lichtenberger
 */
public class DynamicPartialFunctionCallExpr implements Expr {
  private final Expr functionExpr;
  private final Expr[] argExprs;
  private final int[] placeholderPositions;

  /**
   * Creates a dynamic partial function call expression.
   *
   * @param functionExpr         expression that evaluates to a function
   * @param argExprs             array of argument expressions (null for placeholders)
   * @param placeholderPositions positions of placeholders
   */
  public DynamicPartialFunctionCallExpr(Expr functionExpr, Expr[] argExprs, int[] placeholderPositions) {
    this.functionExpr = functionExpr;
    this.argExprs = argExprs;
    this.placeholderPositions = placeholderPositions;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    // Evaluate the function expression to get the actual function
    Item functionItem = functionExpr.evaluateToItem(ctx, tuple);
    if (!(functionItem instanceof Function function)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Expected function but got: %s", functionItem);
    }

    // Evaluate bound argument expressions
    Sequence[] boundArgs = new Sequence[argExprs.length];
    for (int i = 0; i < argExprs.length; i++) {
      if (argExprs[i] != null) {
        boundArgs[i] = argExprs[i].evaluate(ctx, tuple);
      }
    }

    // Create and return the partially applied function
    return new PartiallyAppliedFunction(function, boundArgs, placeholderPositions);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    return (Item) evaluate(ctx, tuple);
  }

  @Override
  public boolean isUpdating() {
    return functionExpr.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
