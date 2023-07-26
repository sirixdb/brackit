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
package io.brackit.query.jsonitem.object;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jsonitem.array.DArray;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import org.magicwerk.brownies.collections.GapList;

import java.util.*;

import static java.util.Objects.*;

/**
 * @author Sebastian Baechle
 * @author Johannes Lichtenberger
 */
public final class ArrayObject extends AbstractObject {
  // two arrays for key/value mapping
  // if lookup costs dominate because the compiler cannot
  // exploit positional access as alternative, we should
  // switch to a more efficient (hash) map.
  private final List<QNm> fields;
  private final List<Sequence> vals;
  private final Map<QNm, Sequence> fieldsToVals;

  public ArrayObject(QNm[] fields, Sequence[] values) {
    this.fields = new GapList<>(Arrays.asList(fields));
    this.vals = new GapList<>(Arrays.asList(values));
    this.fieldsToVals = new HashMap<>();

    for (int i = 0; i < fields.length; i++) {
      final QNm field = fields[i];
      final Sequence value = values[i];
      fieldsToVals.put(field, value);
    }
  }

  @Override
  public Object replace(QNm field, Sequence value) {
    requireNonNull(field);
    for (int i = 0, size = fields.size(); i < size; i++) {
      final QNm currentField = fields.get(i);
      if (currentField.equals(field)) {
        vals.set(i, value);
        fieldsToVals.put(field, value);
        break;
      }
    }
    return this;
  }

  @Override
  public Object rename(QNm field, QNm newFieldName) {
    requireNonNull(field);
    requireNonNull(newFieldName);
    for (int i = 0, size = fields.size(); i < size; i++) {
      final QNm currentField = fields.get(i);
      if (currentField.equals(field)) {
        fields.set(i, newFieldName);

        final Sequence value = fieldsToVals.remove(field);
        fieldsToVals.put(newFieldName, value);
        break;
      }
    }

    return this;
  }

  @Override
  public Object insert(QNm field, Sequence value) {
    if (fieldsToVals.containsKey(field)) {
      throw new QueryException(new QNm("Field already defined."));
    }
    fields.add(field);
    vals.add(value);
    fieldsToVals.put(field, value);
    return this;
  }

  @Override
  public Object remove(QNm field) {
    int index = 0;
    for (int i = 0, size = fields.size(); i < size; i++) {
      final QNm currentField = fields.get(i);
      if (field.equals(currentField)) {
        index = i;
        break;
      }
    }
    fields.remove(index);
    vals.remove(index);
    fieldsToVals.remove(field);
    return this;
  }

  @Override
  public Object remove(IntNumeric index) {
    return remove(index.intValue());
  }

  @Override
  public Object remove(int index) {
    if (index < 0 || index > vals.size() - 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", index);
    }
    final QNm field = fields.remove(index);
    vals.remove(index);
    fieldsToVals.remove(field);
    return this;
  }

  @Override
  public Sequence get(QNm field) {
    return fieldsToVals.get(field);
  }

  @Override
  public Sequence value(IntNumeric i) {
    try {
      return vals.get(i.intValue());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid field index: %s", i);
    }
  }

  @Override
  public Sequence value(int i) {
    try {
      return vals.get(i);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid field index: %s", i);
    }
  }

  @Override
  public Array names() {
    return new DArray(fields);
  }

  @Override
  public Array values() {
    return new DArray(vals);
  }

  @Override
  public QNm name(IntNumeric i) {
    try {
      return fields.get(i.intValue());
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid field index: %s", i);
    }
  }

  @Override
  public QNm name(int i) {
    try {
      return fields.get(i);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid field index: %s", i);
    }
  }

  @Override
  public IntNumeric length() {
    int length = vals.size();
    return length <= 20 ? Int32.ZERO_TO_TWENTY[length] : new Int32(length);
  }

  @Override
  public int len() {
    return vals.size();
  }
}
