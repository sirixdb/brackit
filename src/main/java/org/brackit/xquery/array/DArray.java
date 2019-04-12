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
package org.brackit.xquery.array;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.Array;

/**
 * @author Sebastian Baechle
 *
 */
public class DArray extends AbstractArray {

  private final Sequence[] vals;

  public DArray(Sequence... vals) {
    this.vals = vals;
  }

  @Override
  public Sequence[] array() throws QueryException {
    // TODO Auto-generated method stub
    return super.array();
  }

  @Override
  public Sequence at(IntNumeric i) throws QueryException {
    try {
      // TODO ensure that index is not out of int range
      return (vals[i.intValue()]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", i);
    }
  }

  @Override
  public Sequence at(int i) throws QueryException {
    try {
      return (vals[i]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", i);
    }
  }

  @Override
  public IntNumeric length() throws QueryException {
    int l = vals.length;
    return (l <= 20)
        ? Int32.ZERO_TWO_TWENTY[l]
        : new Int32(l);
  }

  @Override
  public int len() throws QueryException {
    return vals.length;
  }

  @Override
  public Array range(IntNumeric from, IntNumeric to) throws QueryException {
    // TODO ensure that indexes are not out of int range
    return new DRArray(vals, from.intValue(), to.intValue());
  }
}
