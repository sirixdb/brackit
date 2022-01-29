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
package org.brackit.xquery.jsonitem.array;

import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.Array;

import static java.util.Objects.checkFromToIndex;

/**
 * @author Sebastian Baechle
 */
public final class DRArray extends AbstractArray {
  private final List<Sequence> vals;
  private final int start;
  private final int end;

  public DRArray(List<Sequence> vals, int start, int end) {
    if (start < 0 || start > end || start >= vals.size()) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array start index: %s", start);
    }
    if (end > vals.size()) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array end index: %s", end);
    }
    this.vals = vals;
    this.start = start;
    this.end = end;
  }

  @Override
  public Array replaceAt(IntNumeric index, Sequence value) {
    replace(index.intValue(), value);
    return this;
  }

  @Override
  public Array replaceAt(int index, Sequence value) {
    if (start + index < 0 || start + index > vals.size()) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }

    vals.set(start + index, value);

    return this;
  }

  @Override
  public Array insert(int index, Sequence value) {
    if (start + index < 0 || start + index > vals.size() - 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }

    vals.add(start + index, value);

    return this;
  }

  @Override
  public Array append(Sequence value) {
    vals.add(value);

    return this;
  }

  @Override
  public Array remove(int index) {
    if (start + index < 0 || start + index > vals.size() - 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }

    vals.remove(start + index);

    return this;
  }

  @Override
  public Array remove(IntNumeric index) {
    return remove(index.intValue());
  }

  @Override
  public Array insert(IntNumeric index, Sequence value) {
    return insert(index.intValue(), value);
  }

  @Override
  public List<Sequence> values() {
    final var values = new ArrayList<Sequence>();

    for (int i = 0, length = len(); i < length; i++) {
      values.add(at(i));
    }

    return values;
  }

  @Override
  public Sequence at(IntNumeric index) throws QueryException {
    try {
      int ii = start + index.intValue();
      if (ii >= end) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
      }
      return vals.get(ii);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }
  }

  @Override
  public Sequence at(int index) throws QueryException {
    try {
      int ii = start + index;
      if (ii >= end) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
      }
      return vals.get(ii);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }
  }

  @Override
  public IntNumeric length() throws QueryException {
    int length = end - start;
    return (length <= 20) ? Int32.ZERO_TWO_TWENTY[length] : new Int32(length);
  }

  @Override
  public int len() throws QueryException {
    return end - start;
  }

  @Override
  public Array range(IntNumeric from, IntNumeric to) throws QueryException {
    try {
      checkFromToIndex(from.intValue(), to.intValue(), vals.size());
    } catch (final IndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array indexes: %s", e.getMessage());
    }

    return new DRArray(vals, start + from.intValue(), start + to.intValue());
  }
}
