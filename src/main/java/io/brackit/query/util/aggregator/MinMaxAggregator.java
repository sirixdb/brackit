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
package io.brackit.query.util.aggregator;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.AbstractNumeric;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.expr.Cast;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;
import io.brackit.query.util.simd.VectorOps;

/**
 * Aggregator for operations with fn:min() and fn:max() semantics.
 * Enhanced with SIMD-accelerated batch operations for homogeneous numeric sequences.
 *
 * @author Sebastian Baechle
 */
public class MinMaxAggregator implements Aggregator {
  private enum AggType {
    NUMERIC, STRING, GENERIC
  }

  /**
   * Buffer size for batch operations.
   */
  private static final int BATCH_SIZE = 1024;

  final boolean min;
  AggType aggType;
  Atomic minmax = null;
  Type minmaxType = null;

  // SIMD batch buffers (lazily allocated)
  private long[] longBuffer;
  private double[] doubleBuffer;

  public MinMaxAggregator(boolean min) {
    this.min = min;
  }

  @Override
  public Sequence getAggregate() {
    return minmax;
  }

  @Override
  public void clear() {
    aggType = null;
    minmax = null;
    minmaxType = null;
  }

  @Override
  public void add(Sequence seq) throws QueryException {
    if (seq == null) {
      return;
    }
    if (seq instanceof Item) {
      addItem((Item) seq, (minmax == null));
    } else {
      addSequence(seq, (minmax == null));
    }
  }

  private void addSequence(Sequence seq, boolean first) throws QueryException {
    Item item;
    Iter in = seq.iterate();
    try {
      if (first) {
        if ((item = in.next()) != null) {
          addItem(item, first);
        } else {
          return;
        }
      }
      if (aggType == AggType.NUMERIC) {
        minmax = numericMinmax(in, minmax);
      } else if (aggType == AggType.STRING) {
        minmax = stringMinmax(in, minmax);
      } else if (aggType == AggType.GENERIC) {
        minmax = genericMinmax(in, minmax);
      }
    } finally {
      in.close();
    }
  }

  private void addItem(Item item, boolean first) throws QueryException {
    if (!first) {
      if (aggType == AggType.NUMERIC) {
        minmax = numericMinmax(minmax, item, minmax.type().getPrimitiveBase());
      } else if (aggType == AggType.STRING) {
        minmax = stringMinmax(minmax, item, minmax.type().getPrimitiveBase());
      } else if (aggType == AggType.GENERIC) {
        minmax = genericMinmax(minmax, item, minmax.type().getPrimitiveBase());
      }
    } else {
      minmax = item.atomize();
      minmaxType = minmax.type();

      if (minmaxType == Type.UNA) {
        minmax = Cast.cast(null, minmax, Type.DBL, false);
        minmaxType = Type.DBL;
      }

      if (minmaxType.isNumeric()) {
        aggType = AggType.NUMERIC;
      } else if (minmaxType.instanceOf(Type.STR)) {
        aggType = AggType.STRING;
      } else if (minmaxType.instanceOf(Type.YMD) || minmaxType.instanceOf(Type.DTD) || minmaxType.instanceOf(Type.DATE)
          || minmaxType.instanceOf(Type.AURI) || minmaxType.instanceOf(Type.BOOL) || minmaxType.instanceOf(Type.DATE)
          || minmaxType.instanceOf(Type.TIME)) {
        aggType = AggType.GENERIC;
      } else {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                                 "Cannot compute min/max for items of type: %s",
                                 minmaxType);
      }
    }
  }

  private Atomic genericMinmax(Iter in, Atomic minmax) throws QueryException {
    Item item;
    final Type minmaxType = minmax.type().getPrimitiveBase();

    while ((item = in.next()) != null) {
      minmax = genericMinmax(minmax, item, minmaxType);
    }

    return minmax;
  }

  private Atomic genericMinmax(Atomic minmax, Item item, final Type minmaxType) throws QueryException {
    Atomic s = item.atomize();
    Type type = s.type();

    if (!type.instanceOf(minmaxType)) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                               "Incomparable types in aggregate function: %s and %s.",
                               minmaxType,
                               type);
    }

    int res = minmax.cmp(s);

    if ((min) ? (res > 0) : (res < 0)) {
      minmax = s;
    }
    return minmax;
  }

  private Atomic stringMinmax(Iter in, Atomic minmax) throws QueryException {
    Item item;
    final Type minmaxType = minmax.type().getPrimitiveBase();

    while ((item = in.next()) != null) {
      minmax = stringMinmax(minmax, item, minmaxType);
    }

    return minmax;
  }

  private Atomic stringMinmax(Atomic minmax, Item item, final Type minmaxType) throws QueryException {
    Atomic s = item.atomize();
    Type type = s.type();

    if (type == Type.AURI) {
      s = Cast.cast(null, s, Type.STR, false);
      type = Type.STR;
    } else if (!type.instanceOf(Type.STR)) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                               "Incomparable types in aggregate function: %s and %s.",
                               minmaxType,
                               type);
    }

    int res = minmax.cmp(s);

    if ((min) ? (res > 0) : (res < 0)) {
      minmax = s;
    }
    return minmax;
  }

  private Atomic numericMinmax(Iter in, Atomic minmax) throws QueryException {
    Item item = in.next();
    if (item == null) {
      return minmax;
    }

    // Detect type from first item for potential SIMD optimization
    Atomic first = item.atomize();
    Type firstType = first.type();
    if (firstType == Type.UNA) {
      first = Cast.cast(null, first, Type.DBL, false);
      firstType = Type.DBL;
    }

    // Try optimized Int64 batch path
    if (first instanceof Int64 i64 && minmax instanceof Int64 minmaxI64) {
      return numericMinmaxInt64Batch(in, minmaxI64.longValue(), i64.longValue());
    }

    // Try optimized Dbl batch path
    if (first instanceof Dbl dbl && minmax instanceof Dbl minmaxDbl) {
      return numericMinmaxDoubleBatch(in, minmaxDbl.doubleValue(), dbl.doubleValue());
    }

    // Fallback to original loop for other types
    final Type minmaxTypeLocal = minmax.type();
    minmax = numericMinmax(minmax, first, minmaxTypeLocal);
    while ((item = in.next()) != null) {
      minmax = numericMinmax(minmax, item, minmaxTypeLocal);
    }
    return minmax;
  }

  /**
   * SIMD-optimized batch min/max for Int64 sequences.
   */
  private Atomic numericMinmaxInt64Batch(Iter in, long currentMinmax, long firstValue) throws QueryException {
    if (longBuffer == null) {
      longBuffer = new long[BATCH_SIZE];
    }

    // Compare first value with current
    if (min) {
      currentMinmax = Math.min(currentMinmax, firstValue);
    } else {
      currentMinmax = Math.max(currentMinmax, firstValue);
    }

    int bufferLen = 0;
    Item item;
    while ((item = in.next()) != null) {
      Atomic a = item.atomize();

      // Check if we're still getting Int64 values
      if (!(a instanceof Int64 i64)) {
        // Type changed - flush buffer and fall back to object path
        if (bufferLen > 0) {
          long batchResult = min
              ? VectorOps.minLong(longBuffer, 0, bufferLen)
              : VectorOps.maxLong(longBuffer, 0, bufferLen);
          currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
        }
        // Continue with mixed-type processing
        Atomic minmaxAtomic = new Int64(currentMinmax);
        Type minmaxTypeLocal = minmaxAtomic.type();
        minmaxAtomic = numericMinmax(minmaxAtomic, item, minmaxTypeLocal);
        while ((item = in.next()) != null) {
          minmaxAtomic = numericMinmax(minmaxAtomic, item, minmaxTypeLocal);
        }
        return minmaxAtomic;
      }

      longBuffer[bufferLen++] = i64.longValue();

      if (bufferLen == BATCH_SIZE) {
        long batchResult = min
            ? VectorOps.minLong(longBuffer, 0, bufferLen)
            : VectorOps.maxLong(longBuffer, 0, bufferLen);
        currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
        bufferLen = 0;
      }
    }

    // Flush remaining
    if (bufferLen > 0) {
      long batchResult = min
          ? VectorOps.minLong(longBuffer, 0, bufferLen)
          : VectorOps.maxLong(longBuffer, 0, bufferLen);
      currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
    }

    return new Int64(currentMinmax);
  }

  /**
   * SIMD-optimized batch min/max for Dbl sequences.
   */
  private Atomic numericMinmaxDoubleBatch(Iter in, double currentMinmax, double firstValue) throws QueryException {
    if (doubleBuffer == null) {
      doubleBuffer = new double[BATCH_SIZE];
    }

    currentMinmax = min ? Math.min(currentMinmax, firstValue) : Math.max(currentMinmax, firstValue);

    int bufferLen = 0;
    Item item;
    while ((item = in.next()) != null) {
      Atomic a = item.atomize();

      if (!(a instanceof Dbl dbl)) {
        if (bufferLen > 0) {
          double batchResult = min
              ? VectorOps.minDouble(doubleBuffer, 0, bufferLen)
              : VectorOps.maxDouble(doubleBuffer, 0, bufferLen);
          currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
        }
        Atomic minmaxAtomic = new Dbl(currentMinmax);
        Type minmaxTypeLocal = minmaxAtomic.type();
        minmaxAtomic = numericMinmax(minmaxAtomic, item, minmaxTypeLocal);
        while ((item = in.next()) != null) {
          minmaxAtomic = numericMinmax(minmaxAtomic, item, minmaxTypeLocal);
        }
        return minmaxAtomic;
      }

      doubleBuffer[bufferLen++] = dbl.doubleValue();

      if (bufferLen == BATCH_SIZE) {
        double batchResult = min
            ? VectorOps.minDouble(doubleBuffer, 0, bufferLen)
            : VectorOps.maxDouble(doubleBuffer, 0, bufferLen);
        currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
        bufferLen = 0;
      }
    }

    if (bufferLen > 0) {
      double batchResult = min
          ? VectorOps.minDouble(doubleBuffer, 0, bufferLen)
          : VectorOps.maxDouble(doubleBuffer, 0, bufferLen);
      currentMinmax = min ? Math.min(currentMinmax, batchResult) : Math.max(currentMinmax, batchResult);
    }

    return new Dbl(currentMinmax);
  }

  private Atomic numericMinmax(Atomic minmax, Item item, final Type minmaxType) throws QueryException {
    Atomic s = item.atomize();
    Type type = s.type();

    if (type == Type.UNA) {
      s = Cast.cast(null, s, Type.DBL, false);
      type = Type.DBL;
    }

    if (!(s instanceof Numeric)) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                               "Incomparable types in aggregate function: %s and %s.",
                               minmaxType,
                               type);
    }

    // F&O §14.4: if the converted sequence contains NaN, fn:min/fn:max return NaN. The cmp-based
    // update below uses the total order (NaN above everything), which never SELECTS NaN — so
    // min((1, NaN)) silently returned 1. NaN is sticky once seen.
    if (AbstractNumeric.isNaN(s)) {
      return s;
    }
    if (AbstractNumeric.isNaN(minmax)) {
      return minmax;
    }

    int res = minmax.cmp(s);

    if ((min) ? (res > 0) : (res < 0)) {
      minmax = s;
    }
    return minmax;
  }
}