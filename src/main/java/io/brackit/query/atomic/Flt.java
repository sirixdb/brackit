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
package io.brackit.query.atomic;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.util.Whitespace;
import io.brackit.query.jdm.Type;

public class Flt extends AbstractNumeric implements FltNumeric {
  public static final Flt NaN = new Flt(Float.NaN);

  public static final Flt NINF = new Flt(Float.NEGATIVE_INFINITY);

  public static final Flt PINF = new Flt(Float.POSITIVE_INFINITY);

  public final float v;

  private class DFlt extends Flt {
    private final Type type;

    public DFlt(float v, Type type) {
      super(v);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public Flt(String str) throws QueryException {
    float parsed;
    try {
      str = Whitespace.collapseTrimOnly(str);
      parsed = Float.parseFloat(str);
    } catch (NumberFormatException e) {
      if (str.equals("INF")) {
        parsed = Float.POSITIVE_INFINITY;
      } else if (str.equals("-INF")) {
        parsed = Float.NEGATIVE_INFINITY;
      } else if (str.equals("NaN")) {
        parsed = Float.NaN;
      }
      throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:float", str);
    }
    this.v = parsed;
  }

  public static Flt parse(String str) throws QueryException {
    try {
      str = Whitespace.collapseTrimOnly(str);
      float flt = Float.parseFloat(str);
      return new Flt(flt);
    } catch (NumberFormatException e) {
      if (str.equals("INF")) {
        return Flt.PINF;
      }
      if (str.equals("-INF")) {
        return Flt.NINF;
      }
      if (str.equals("NaN")) {
        return Flt.NaN;
      }
      throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:float", str);
    }
  }

  public Flt(Float v) {
    this.v = v;
  }

  public Flt(float v) {
    this.v = v;
  }

  @Override
  public Type type() {
    return Type.FLO;
  }

  @Override
  public IntNumeric asIntNumeric() {
    if (Float.isNaN(v) || Float.isInfinite(v)) {
      return null;
    }
    int i = (int) v;
    double f = v - i;
    return f == 0.0 ? new Int32(i) : null;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return validate(Type.FLO, new DFlt(v, type));
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return v != 0 && !Float.isNaN(v) && Float.isInfinite(v);
  }

  @Override
  public int cmp(Atomic other) throws QueryException {
    if (other instanceof Numeric) {
      if (!(other instanceof DblNumeric)) {
        return Float.compare(v, ((Numeric) other).floatValue());
      }
      return Double.compare(v, ((Numeric) other).doubleValue());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s' with '%s'",
                             type(),
                             other.type());
  }

  @Override
  public int atomicCmpInternal(Atomic other) {
    if (!(other instanceof DblNumeric)) {
      return Float.compare(v, ((Numeric) other).floatValue());
    }
    return Double.compare(v, ((Numeric) other).doubleValue());
  }

  @Override
  public String stringValue() {
    if (Float.isNaN(v))
      return "NaN";
    if (Float.isInfinite(v))
      return v > 0 ? "INF" : "-INF";
    if (v == 0)
      return 1 / v == 1 / 0.0f ? "0" : "-0";
    return killTrailingZeros(v > 0 && v >= 1e-6 && v < 1e6 || -v >= 1e-6 && -v < 1e6 ? DF.format(v) : SF.format(v));
  }

  @Override
  public BigDecimal decimalValue() {
    return new BigDecimal(v);
  }

  @Override
  public BigDecimal integerValue() {
    return new BigDecimal(Math.floor(v));
  }

  @Override
  public double doubleValue() {
    return v;
  }

  @Override
  public float floatValue() {
    return v;
  }

  @Override
  public long longValue() {
    return (long) v;
  }

  @Override
  public int intValue() {
    return (int) v;
  }

  @Override
  public Numeric add(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return addFloat(v, other.floatValue());
    } else {
      return addDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric subtract(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return subtractFloat(v, other.floatValue());
    } else {
      return subtractDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric multiply(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return multiplyFloat(v, other.floatValue());
    } else {
      return multiplyDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric div(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return divideFloat(v, other.floatValue());
    } else {
      return divideDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric idiv(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return idivideFloat(v, other.floatValue());
    } else {
      return idivideDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric mod(Numeric other) throws QueryException {
    if (!(other instanceof DblNumeric)) {
      return modFloat(v, other.floatValue());
    } else {
      return modDouble(v, other.doubleValue());
    }
  }

  @Override
  public Numeric negate() throws QueryException {
    return new Flt(-v);
  }

  @Override
  public Numeric round() throws QueryException {
    return Float.isInfinite(v) || v == 0 || Float.isNaN(v) ? this : new Flt(Math.round(v));
  }

  @Override
  public Numeric abs() throws QueryException {
    return v == Float.POSITIVE_INFINITY || v >= 0 || Float.isNaN(v) ? this : new Flt(Math.abs(v));
  }

  @Override
  public Numeric ceiling() throws QueryException {
    return Float.isInfinite(v) || v == 0 || Float.isNaN(v) ? this : new Flt((float) Math.ceil(v));
  }

  @Override
  public Numeric floor() throws QueryException {
    return Float.isInfinite(v) || v == 0 || Float.isNaN(v) ? this : new Flt((float) Math.floor(v));
  }

  @Override
  public Numeric roundHalfToEven(int precision) throws QueryException {
    if (Float.isInfinite(v) || v == 0 || Float.isNaN(v)) {
      return this;
    }

    double factor = Math.pow(10, precision);
    double scaled = v * factor;

    if (Float.isInfinite((float) scaled)) {
      BigDecimal bd = new BigDecimal(v);
      bd = bd.scaleByPowerOfTen(precision);
      // Previously we used BigDecimal setScale(int, int) version. However, since that
      // is deprecated
      // the recommended approach is to use BigDecimal.setScale(int, RoundingMode)
      bd = bd.setScale(0, RoundingMode.HALF_EVEN);
      bd = bd.scaleByPowerOfTen(-precision);
      return new Flt(bd.floatValue());
    }

    scaled = Math.rint(scaled);
    return new Flt((float) (scaled / factor));
  }
}
