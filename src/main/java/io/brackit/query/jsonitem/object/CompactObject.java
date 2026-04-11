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

import java.util.List;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jsonitem.array.DArray;

/**
 * Allocation-minimal JSON object for parsed data. Uses bare arrays instead of
 * GapList + HashMap. Field lookup is O(n) linear scan, which is fast for
 * typical JSON objects with fewer than ~20 fields (cache-line friendly).
 * <p>
 * Compared to {@link ArrayObject}:
 * <ul>
 * <li>No GapList allocation (2 saved)</li>
 * <li>No HashMap allocation (1 saved + N Entry objects)</li>
 * <li>No Arrays.asList wrapper (2 saved)</li>
 * </ul>
 * <p>
 * For a 3-field object, this saves ~8 object allocations per parse.
 * At 100M records, that's 800M fewer objects for GC.
 */
public final class CompactObject extends AbstractObject {

  private final QNm[] fields;
  private final Sequence[] values;

  public CompactObject(QNm[] fields, Sequence[] values) {
    this.fields = fields;
    this.values = values;
  }

  @Override
  public Sequence get(QNm field) {
    // Linear scan — fast for small objects (1-20 fields)
    for (int i = 0; i < fields.length; i++) {
      if (fields[i].equals(field)) {
        return values[i];
      }
    }
    return null;
  }

  @Override
  public Sequence value(IntNumeric i) {
    return values[i.intValue()];
  }

  @Override
  public Sequence value(int i) {
    return values[i];
  }

  @Override
  public Array names() {
    return new DArray(List.of(fields));
  }

  @Override
  public Array values() {
    return new DArray(List.of(values));
  }

  @Override
  public QNm name(IntNumeric i) {
    return fields[i.intValue()];
  }

  @Override
  public QNm name(int i) {
    return fields[i];
  }

  @Override
  public IntNumeric length() {
    return Int32.cached(fields.length);
  }

  @Override
  public int len() {
    return fields.length;
  }

  // Mutation methods — delegate to ArrayObject by converting

  @Override
  public Object replace(QNm field, Sequence value) {
    for (int i = 0; i < fields.length; i++) {
      if (fields[i].equals(field)) {
        values[i] = value;
        return this;
      }
    }
    return this;
  }

  @Override
  public Object rename(QNm field, QNm newFieldName) {
    for (int i = 0; i < fields.length; i++) {
      if (fields[i].equals(field)) {
        fields[i] = newFieldName;
        return this;
      }
    }
    return this;
  }

  @Override
  public Object insert(QNm field, Sequence value) {
    // Grow arrays — rare for parsed data, acceptable
    QNm[] newFields = new QNm[fields.length + 1];
    Sequence[] newValues = new Sequence[values.length + 1];
    System.arraycopy(fields, 0, newFields, 0, fields.length);
    System.arraycopy(values, 0, newValues, 0, values.length);
    newFields[fields.length] = field;
    newValues[values.length] = value;
    return new CompactObject(newFields, newValues);
  }

  @Override
  public Object remove(QNm field) {
    for (int i = 0; i < fields.length; i++) {
      if (fields[i].equals(field)) {
        return remove(i);
      }
    }
    return this;
  }

  @Override
  public Object remove(IntNumeric index) {
    return remove(index.intValue());
  }

  @Override
  public Object remove(int index) {
    if (index < 0 || index >= fields.length) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid index: %s", index);
    }
    QNm[] newFields = new QNm[fields.length - 1];
    Sequence[] newValues = new Sequence[values.length - 1];
    System.arraycopy(fields, 0, newFields, 0, index);
    System.arraycopy(fields, index + 1, newFields, index, fields.length - index - 1);
    System.arraycopy(values, 0, newValues, 0, index);
    System.arraycopy(values, index + 1, newValues, index, values.length - index - 1);
    return new CompactObject(newFields, newValues);
  }
}
