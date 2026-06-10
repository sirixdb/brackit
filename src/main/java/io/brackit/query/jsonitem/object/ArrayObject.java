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
  // Two parallel arrays for ordered key/value access. The HashMap is built
  // lazily on first by-name lookup — for output-only usage (return clause
  // serializing through positional iteration), the map is never built,
  // saving a HashMap + N puts per object. Critical for high-fan-out joins
  // that materialize millions of result records.
  private final List<QNm> fields;
  private final List<Sequence> vals;
  private Map<QNm, Sequence> fieldsToVals;  // lazy

  public ArrayObject(QNm[] fields, Sequence[] values) {
    this.fields = new GapList<>(Arrays.asList(fields));
    this.vals = new GapList<>(Arrays.asList(values));
    this.fieldsToVals = null;  // lazy
  }

  /**
   * Efficient constructor that takes pre-built lists directly, avoiding array-to-list copies.
   * The caller transfers ownership — the lists must not be modified after this call.
   *
   * <p>{@code fieldsToVals} may be {@code null}; in that case it will be built lazily on
   * the first by-name lookup.
   */
  public ArrayObject(List<QNm> fields, List<Sequence> vals, Map<QNm, Sequence> fieldsToVals) {
    this.fields = fields;
    this.vals = vals;
    this.fieldsToVals = fieldsToVals;
  }

  /** Build the field → value lookup map on demand. Idempotent (caller should null-check first). */
  private Map<QNm, Sequence> ensureMap() {
    Map<QNm, Sequence> m = fieldsToVals;
    if (m == null) {
      m = HashMap.newHashMap(fields.size());
      for (int i = 0, n = fields.size(); i < n; i++) {
        m.put(fields.get(i), vals.get(i));
      }
      fieldsToVals = m;
    }
    return m;
  }

  @Override
  public Object replace(QNm field, Sequence value) {
    requireNonNull(field);
    for (int i = 0, size = fields.size(); i < size; i++) {
      final QNm currentField = fields.get(i);
      if (currentField.equals(field)) {
        vals.set(i, value);
        ensureMap().put(field, value);
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

        Map<QNm, Sequence> m = ensureMap();
        final Sequence value = m.remove(field);
        m.put(newFieldName, value);
        break;
      }
    }

    return this;
  }

  @Override
  public Object insert(QNm field, Sequence value) {
    Map<QNm, Sequence> m = ensureMap();
    if (m.containsKey(field)) {
      throw new QueryException(new QNm("Field already defined."));
    }
    fields.add(field);
    vals.add(value);
    m.put(field, value);
    return this;
  }

  @Override
  public Object remove(QNm field) {
    int index = -1;
    for (int i = 0, size = fields.size(); i < size; i++) {
      final QNm currentField = fields.get(i);
      if (field.equals(currentField)) {
        index = i;
        break;
      }
    }
    // No-op when the field is absent — index was initialized to 0, so a missing field deleted
    // the FIRST entry (and threw IndexOutOfBounds on an empty object).
    if (index < 0) {
      return this;
    }
    fields.remove(index);
    vals.remove(index);
    if (fieldsToVals != null)
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
    if (fieldsToVals != null)
      fieldsToVals.remove(field);
    return this;
  }

  @Override
  public Sequence get(QNm field) {
    return ensureMap().get(field);
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
