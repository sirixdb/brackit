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
package io.brackit.query.jsonitem.array;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import org.magicwerk.brownies.collections.GapList;

import java.util.List;

import static java.util.Objects.checkFromToIndex;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class DArray extends AbstractArray {

  private final List<Sequence> vals;

  public DArray(List<? extends Sequence> vals) {
    this.vals = new GapList<>(vals);
  }

  @Override
  public List<Sequence> values() {
    return vals;
  }

  @Override
  public Array insert(int index, Sequence value) {
    if (index < 0 || index > vals.size()) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }

    vals.add(index, value);

    return this;
  }

  @Override
  public Array append(Sequence value) {
    vals.add(value);

    return this;
  }

  @Override
  public Array replaceAt(int index, Sequence value) {
    if (index < 0 || index > vals.size() - 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }

    vals.set(index, value);

    return this;
  }

  @Override
  public Array replaceAt(IntNumeric index, Sequence value) {
    replace(index.intValue(), value);
    return this;
  }

  @Override
  public Array insert(IntNumeric index, Sequence value) {
    return insert(index.intValue(), value);
  }

  @Override
  public Array remove(int index) {
    if (index < 0 || index > vals.size() - 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }
    vals.remove(index);
    return this;
  }

  @Override
  public Array remove(IntNumeric index) {
    return remove(index.intValue());
  }

  @Override
  public Sequence at(IntNumeric index) {
    return at(index.intValue());
  }

  @Override
  public Sequence at(int i) {
    try {
      return vals.get(i);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", i);
    }
  }

  @Override
  public IntNumeric length() {
    final int length = vals.size();
    return length <= 20 ? Int32.ZERO_TO_TWENTY[length] : new Int32(length);
  }

  @Override
  public int len() {
    return vals.size();
  }

  @Override
  public Array range(IntNumeric from, IntNumeric to) {
    try {
      checkFromToIndex(from.intValue(), to.intValue(), vals.size());
    } catch (final IndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array indexes: %s", e.getMessage());
    }

    return new DRArray(vals, from.intValue(), to.intValue());
  }
}
