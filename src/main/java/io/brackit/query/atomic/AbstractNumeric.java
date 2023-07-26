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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Facets;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractNumeric extends AbstractAtomic implements Numeric {
  /**
   *
   */
  private static final int INTEGER_DIV_SCALE = 18;

  private static final DecimalFormatSymbols DF_SYMBOL = new DecimalFormatSymbols(Locale.US);

  protected static final DecimalFormat SD = new DecimalFormat("0.0################E0##", DF_SYMBOL);

  protected static final DecimalFormat DD = new DecimalFormat("#####0.0################", DF_SYMBOL);

  protected static final DecimalFormat SF = new DecimalFormat("0.0######E0##", DF_SYMBOL);

  protected static final DecimalFormat DF = new DecimalFormat("#####0.0######", DF_SYMBOL);

  @Override
  public final int atomicCode() {
    return Type.NUMERIC_CODE;
  }

  protected Atomic validate(Type baseType, Numeric value) throws QueryException {
    Type type = value.type();
    if (type == baseType) {
      return value;
    }
    if (type.getPrimitiveBase() != baseType && type.instanceOf(baseType)) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "Type '%s' is not a subtype of '%s",
                               type,
                               baseType);
    }
    Facets facets = type.getFacets();
    if (facets.maxExclusive != null) {
      if (value.cmp(facets.maxExclusive) >= 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Value %s is too large for type '%s'",
                                 value,
                                 type);
      }
    }
    if (facets.maxInclusive != null) {
      if (value.cmp(facets.maxInclusive) > 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Value %s is too large for type '%s'",
                                 value,
                                 type);
      }
    }
    if (facets.minExclusive != null) {
      if (value.cmp(facets.minExclusive) <= 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Value %s is too small for type '%s'",
                                 value,
                                 type);
      }
    }
    if (facets.minInclusive != null) {
      if (value.cmp(facets.minInclusive) < 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Value %s is too small for type '%s'",
                                 value,
                                 type);
      }
    }
    if (facets.enumeration != null) {
      boolean valid = false;
      for (Atomic a : facets.enumeration) {
        if (a.cmp(value) == 0) {
          valid = true;
          break;
        }
      }
      if (!valid) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Value %s is not allowed in type '%s'",
                                 value,
                                 type);
      }
    }

    return value;
  }

  protected final Numeric addDouble(double a, double b) {
    return new Dbl(a + b);
  }

  protected final Numeric addFloat(float a, float b) {
    return new Flt(a + b);
  }

  protected final Numeric addInt(int a, int b) {
    int r = a + b;
    if (b >= 0 ? r < a : r > a) {
      // overflow escalate to long
      return new Int64((long) a + (long) b);
    }
    return 0 <= r && r <= 20 ? Int32.ZERO_TO_TWENTY[r] : new Int32(r);
  }

  protected final Numeric addLong(long a, long b) {
    long r = a + b;
    if (b >= 0 ? r < a : r > a) {
      // overflow escalate to BigDecimal
      new Int(new BigDecimal(a).add(new BigDecimal(b)));
    }
    return new Int64(r);
  }

  protected final Numeric addBigDecimal(BigDecimal a, BigDecimal b, boolean isDecimal) {
    return isDecimal ? new Dec(a.add(b)) : new Int(a.add(b));
  }

  protected final Numeric subtractDouble(double a, double b) {
    return new Dbl(a - b);
  }

  protected final Numeric subtractFloat(float a, float b) {
    return new Flt(a - b);
  }

  protected final Numeric subtractInt(int a, int b) {
    int r = a - b;
    if (b >= 0 ? r >= a : r <= a) {
      // overflow escalate to long
      return new Int64((long) a - (long) b);
    }
    return new Int32(r);
  }

  protected final Numeric subtractLong(long a, long b) {
    long r = a - b;
    if (b >= 0 ? r >= a : r <= a) {
      // overflow escalate to BigDecimal
      new Int(new BigDecimal(a).subtract(new BigDecimal(b)));
    }
    return new Int64(r);
  }

  protected final Numeric subtractBigDecimal(BigDecimal a, BigDecimal b, boolean isDecimal) {
    return isDecimal ? new Dec(a.subtract(b)) : new Int(a.subtract(b));
  }

  protected final Numeric multiplyDouble(double a, double b) {
    return new Dbl(a * b);
  }

  protected final Numeric multiplyFloat(float a, float b) {
    return new Flt(a * b);
  }

  protected final Numeric multiplyInt(int a, int b) {
    int r = a * b;
    if (b != 0 && r / b != a) {
      // overflow escalate to long
      return new Int64((long) a * (long) b);
    }
    return new Int32(r);
  }

  protected final Numeric multiplyLong(long a, long b) {
    long r = a * b;
    if (b != 0 && r / b != a) {
      // overflow escalate to BigDecimal
      new Int(new BigDecimal(a).multiply(new BigDecimal(b)));
    }
    return new Int64(r);
  }

  protected final Numeric multiplyBigDecimal(BigDecimal a, BigDecimal b, boolean isDecimal) {
    return isDecimal ? new Dec(a.multiply(b)) : new Int(a.multiply(b));
  }

  protected final Numeric divideDouble(double a, double b) {
    if (b == 0) {
      return a < 0 ? Dbl.NINF : a == 0 ? Dbl.NaN : Dbl.PINF;
    }

    if (Double.isInfinite(a) && Double.isInfinite(b)) {
      return Dbl.NaN;
    }

    return new Dbl(a / b);
  }

  protected final Numeric divideFloat(float a, float b) {
    if (b == 0) {
      return a < 0 ? Flt.NINF : a == 0 ? Flt.NaN : Flt.PINF;
    }

    if (Float.isInfinite(a) && Float.isInfinite(b)) {
      return Flt.NaN;
    }

    return new Flt(a / b);
  }

  protected final Numeric divideInt(int a, int b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    if (a % b == 0) {
      return new Int32(a / b);
    }
    return new Dec(new BigDecimal(a).divide(new BigDecimal(b), INTEGER_DIV_SCALE, RoundingMode.HALF_EVEN));
  }

  protected final Numeric divideLong(long a, long b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    if (a % b == 0) {
      return new Int64(a / b);
    }
    return new Dec(new BigDecimal(a).divide(new BigDecimal(b), INTEGER_DIV_SCALE, RoundingMode.HALF_EVEN));
  }

  protected final Numeric divideBigDecimal(BigDecimal a, BigDecimal b, boolean isDecimal) throws QueryException {
    if (b.compareTo(BigDecimal.ZERO) == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }

    int scale = isDecimal ? a.scale() - b.scale() : INTEGER_DIV_SCALE;
    return new Dec(a.divide(b, scale, RoundingMode.HALF_EVEN));
  }

  protected final Numeric idivideDouble(double a, double b) {
    if (b == 0) {
      return a < 0 ? Dbl.NINF : a == 0 ? Dbl.NaN : Dbl.PINF;
    }

    if (Double.isInfinite(a) && Double.isInfinite(b)) {
      return Dbl.NaN;
    }

    double r = Math.floor(a / b);
    return r < Integer.MAX_VALUE ? new Int32((int) r) : r < Long.MAX_VALUE ? new Int64((long) r) : new Int(r);
  }

  protected final Numeric idivideFloat(float a, float b) {
    if (b == 0) {
      return a < 0 ? Flt.NINF : a == 0 ? Flt.NaN : Flt.PINF;
    }

    if (Float.isInfinite(a) && Float.isInfinite(b)) {
      return Flt.NaN;
    }

    float r = (float) Math.floor(a / b);
    return r < Integer.MAX_VALUE ? new Int32((int) r) : r < Long.MAX_VALUE ? new Int64((long) r) : new Int(r);
  }

  protected final Numeric idivideInt(int a, int b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    return new Int32(a / b);
  }

  protected final Numeric idivideLong(long a, long b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    return new Int64(a / b);
  }

  protected final Numeric idivideBigDecimal(BigDecimal a, BigDecimal b) throws QueryException {
    if (b.compareTo(BigDecimal.ZERO) == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }

    return new Int(a.divideToIntegralValue(b));
  }

  protected final Numeric modDouble(double a, double b) {
    return new Dbl(a % b);
  }

  protected final Numeric modFloat(float a, float b) {
    return new Flt(a % b);
  }

  protected final Numeric modInt(int a, int b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    return new Int32(a % b);
  }

  protected final Numeric modLong(long a, long b) throws QueryException {
    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }
    return new Int64(a % b);
  }

  protected final Numeric modBigDecimal(BigDecimal a, BigDecimal b) throws QueryException {
    if (b.compareTo(BigDecimal.ZERO) == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }

    return new Int(a.remainder(b));
  }

  protected final String killTrailingZeros(String s) {
    int len = s.length() - 1;
    int pos = len;
    if (len <= 1) {
      return s;
    }
    while (pos >= 0 && s.charAt(pos) == '0') {
      pos--;
    }
    if (pos > 0 && s.charAt(pos) == '.') {
      pos--;
    }
    return pos == len ? s : s.substring(0, pos + 1);
  }

  @Override
  public final int hashCode() {
    // Use same hash code as in OpenJDK's java.lang.Double
    long bits = Double.doubleToLongBits(doubleValue());
    return (int) (bits ^ bits >>> 32);
  }
}
