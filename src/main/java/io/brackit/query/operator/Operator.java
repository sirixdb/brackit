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
package io.brackit.query.operator;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * Set-oriented operators.
 *
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public interface Operator {
  /**
   * Create a new cursor.
   *
   * @param ctx   the query context
   * @param tuple the current tuple
   * @return the new cursor instance
   * @throws QueryException if any exception occurs during the creation of the cursor
   */
  Cursor create(QueryContext ctx, Tuple tuple);

  /**
   * Create a new cursor.
   *
   * @param ctx    the query context
   * @param tuples the current tuples
   * @return the new cursor instance
   * @throws QueryException if any exception occurs during the creation of the cursor
   */
  Cursor create(QueryContext ctx, Tuple[] tuples, int len);

  /**
   * The tuple width.
   *
   * @param initSize the initial size
   * @return the size
   */
  int tupleWidth(int initSize);
}
