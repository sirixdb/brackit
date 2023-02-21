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
package org.brackit.xquery.jdm;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.IntNumeric;

/**
 * <p>
 * A sequence as defined in {@linkplain http://www.w3.org/TR/xpath-datamodel}.
 * </p>
 *
 * <p>
 * The empty sequence is represented either as <code>null</code> when returned
 * as result from an {@link Expr Expression} or as any {@link Sequence} without
 * {@link Item Items} when it is required as input for expressions or as a query
 * result.
 * </p>
 *
 * <p>
 * For performance reasons sequences may only be iterated and do not allow
 * direct access to specific elements.
 * </p>
 *
 * <p>
 * Sequences must be <b>logically immutable</b>, i.e., it must be ensured that
 * concurrent access does not cause race conditions.
 * </p>
 *
 * @author Sebastian Baechle
 */
public interface Sequence extends Tuple, Expr {
  /**
   * Checks the effective boolean value of this sequence as defined in
   * {@linkplain http://www.w3.org/TR/xquery/#id-ebv}.
   *
   * <p>
   * <b>Caution:</b><br/>
   * Note that this method may require to iterate over a potentially large
   * input to determine the actual boolean value.
   * </p>
   */
  boolean booleanValue() throws QueryException;

  /**
   * Checks the size of this sequence.
   *
   * <p>
   * <b>Caution:</b><br/>
   * Note that this method may require to iterate over a potentially large
   * input to determine the actual size.
   * </p>
   */
  IntNumeric size() throws QueryException;

  /**
   * Returns the item at the given position or <code>null</code> iff
   * <code>pos</code> is out of range.
   *
   * <p>
   * <b>Caution:</b><br/>
   * Note that this method may require to iterate over a potentially large
   * input to determine the actual size.
   * </p>
   */
  Item get(IntNumeric pos) throws QueryException;

  /**
   * Create a stream to iterate over all items of this sequence.
   */
  Iter iterate();
}