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
package org.brackit.xquery.atomic;

import java.math.BigDecimal;
import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Type;

/**
 *
 * @author Sebastian Baechle
 *
 */
public class Int64 extends AbstractNumeric implements LonNumeric {
  public static final Int64 MIN_VALUE = new Int64(Long.MIN_VALUE);

  public static final Int64 MAX_VALUE = new Int64(Long.MAX_VALUE);

  public static final BigDecimal MIN_VALUE_AS_DECIMAL = new BigDecimal(Long.MIN_VALUE);

  public static final BigDecimal MAX_VALUE_AS_DECIMAL = new BigDecimal(Long.MAX_VALUE);

  private final long v;

  private static class DInt64 extends Int64 {
    private final Type type;

    public DInt64(long v, Type type) {
      super(v);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public Int64(Long v) {
    this.v = v;
  }

  public Int64(long v) {
    this.v = v;
  }

  @Override
  public Type type() {
    return Type.INR;
  }

  @Override
  public IntNumeric asIntNumeric() {
    return this;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return validate(Type.INR, new DInt64(v, type));
  }

  @Override
  public IntNumeric inc() {
    return (v != Long.MAX_VALUE)
        ? new Int64(v + 1)
        : new Int(new BigDecimal(v).add(BigDecimal.ONE));
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return (v != 0);
  }

  @Override
  public int cmp(Atomic other) throws QueryException {
    if ((other instanceof IntNumeric)) {
      if (other instanceof LonNumeric) {
        long v2 = ((LonNumeric) other).longValue();
        return Long.compare(v, v2);
      }
      return -other.cmp(this);
    } else if (other instanceof DecNumeric) {
      return new BigDecimal(v).compareTo(((Numeric) other).decimalValue());
    } else if (other instanceof Dbl) {
      return Double.compare(v, ((Numeric) other).doubleValue());
    } else if (other instanceof Flt) {
      return Float.compare(v, ((Numeric) other).floatValue());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Cannot compare '%s' with '%s'", type(),
        other.type());
  }

  @Override
  public int atomicCmpInternal(Atomic other) {
    if ((other instanceof IntNumeric)) {
      if (other instanceof LonNumeric) {
        long v2 = ((LonNumeric) other).longValue();
        return Long.compare(v, v2);
      }
      return -((AbstractNumeric) other).atomicCmpInternal(this);
    } else if (other instanceof DecNumeric) {
      return new BigDecimal(v).compareTo(((Numeric) other).decimalValue());
    } else if (other instanceof Dbl) {
      return Double.compare(v, ((Numeric) other).doubleValue());
    } else {
      return Float.compare(v, ((Numeric) other).floatValue());
    }
  }

  @Override
  public String stringValue() {
    return Long.toString(v);
  }

  @Override
  public BigDecimal decimalValue() {
    return new BigDecimal(v);
  }

  @Override
  public BigDecimal integerValue() {
    return new BigDecimal(v);
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
    return v;
  }

  @Override
  public int intValue() {
    return (int) v;
  }

  @Override
  public Numeric add(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return addLong(v, other.longValue());
      }
      return other.add(this);
    } else if (other instanceof DecNumeric) {
      return addBigDecimal(new BigDecimal(v), other.decimalValue(), false);
    } else if (other instanceof Dbl) {
      return addDouble(v, other.doubleValue());
    } else {
      return addFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric subtract(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return subtractLong(v, other.longValue());
      }
      return other.negate().add(this);
    } else if (other instanceof DecNumeric) {
      return subtractBigDecimal(new BigDecimal(v), other.decimalValue(), false);
    } else if (other instanceof Dbl) {
      return subtractDouble(v, other.doubleValue());
    } else {
      return subtractFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric multiply(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return multiplyLong(v, other.longValue());
      }
      return other.multiply(this);
    } else if (other instanceof DecNumeric) {
      return multiplyBigDecimal(new BigDecimal(v), other.decimalValue(), true);
    } else if (other instanceof Dbl) {
      return multiplyDouble(v, other.doubleValue());
    } else {
      return multiplyFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric div(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return divideLong(v, other.longValue());
      }
      return other.add(this);
    } else if (other instanceof DecNumeric) {
      return divideBigDecimal(new BigDecimal(v), other.decimalValue(), false);
    } else if (other instanceof Dbl) {
      return divideDouble(v, other.doubleValue());
    } else {
      return divideFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric idiv(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return idivideLong(v, other.longValue());
      }
      return other.add(this);
    } else if (other instanceof DecNumeric) {
      return idivideBigDecimal(new BigDecimal(v), other.decimalValue());
    } else if (other instanceof Dbl) {
      return idivideDouble(v, other.doubleValue());
    } else {
      return idivideFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric mod(Numeric other) throws QueryException {
    if (other instanceof IntNumeric) {
      if (other instanceof LonNumeric) {
        return modLong(v, other.longValue());
      }
      return other.add(this);
    } else if (other instanceof DecNumeric) {
      return modBigDecimal(new BigDecimal(v), other.decimalValue());
    } else if (other instanceof Dbl) {
      return modDouble(v, other.doubleValue());
    } else {
      return divideFloat(v, other.floatValue());
    }
  }

  @Override
  public Numeric negate() throws QueryException {
    return (v != Long.MIN_VALUE)
        ? new Int64(-v)
        : new Int(new BigDecimal(v).negate());
  }

  @Override
  public Numeric round() throws QueryException {
    return this;
  }

  @Override
  public Numeric abs() throws QueryException {
    return (v >= 0)
        ? this
        : (v != Long.MIN_VALUE)
            ? new Int64(-v)
            : new Int(new BigDecimal(v).negate());
  }

  @Override
  public Numeric ceiling() throws QueryException {
    return this;
  }

  @Override
  public Numeric floor() throws QueryException {
    return this;
  }

  @Override
  public Numeric roundHalfToEven(int precision) throws QueryException {
    return this;
  }
}
