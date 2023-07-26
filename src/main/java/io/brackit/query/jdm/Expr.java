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
package io.brackit.query.jdm;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * Interface of all types of XQuery expressions.
 * <p>
 * An expression may be anything form a complex FLOWR, to a calculation, a
 * function call or a primitive {@link Item}.
 * <p>
 * Expressions are evaluated for a defined context which consists of both, the
 * current {@link BrackitQueryContext}, which provides access to e.g., to externally
 * bound variables, and a {@link Tuple}, which contains the dynamically bound
 * variables of surrounding FLOWR expressions.
 *
 * @author Sebastian Baechle
 */
public interface Expr {
  /**
   * Evaluates the expression for the current context. If the result is the
   * empty sequence, an expression may either return <code>null</code> or any
   * other {@link Sequence} without any items.
   *
   * <p>
   * <b>Caution:</b> One must ensure that the returned sequence is static,
   * i.e., all external references have to be resolved only once for the
   * current context.
   * </p>
   */
  Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException;

  /**
   * Evaluates the expression as defined in {
   * {@link Expr#evaluate(QueryContext, Tuple)}, but the result is ensured to be a
   * single {@link Item} or <code>null</code> if this expression evaluates to
   * the empty sequence.
   * <p>
   * If this expression does not return the empty sequence or a single item,
   * an {@link QueryException} with
   * {@link ErrorCode#ERR_TYPE_INAPPROPRIATE_TYPE} must be thrown.
   */
  Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException;

  /**
   * Checks if this expression or any subexpression is an updating expression
   * according to XQuery Update Facility 1.0: 2 Extensions to XQuery 1.0
   */
  boolean isUpdating();

  /**
   * Checks if this expression or any subexpression is an vacuous expression
   * according to XQuery Update Facility 1.0: 2 Extensions to XQuery 1.0
   */
  boolean isVacuous();
}