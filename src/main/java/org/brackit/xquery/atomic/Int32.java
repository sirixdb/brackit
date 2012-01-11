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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
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
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Int32 extends AbstractNumeric implements LonNumeric {
	public static final Int32 MIN_VALUE = new Int32(Integer.MIN_VALUE);

	public static final Int32 MAX_VALUE = new Int32(Integer.MAX_VALUE);

	public static final BigDecimal MIN_VALUE_AS_DECIMAL = new BigDecimal(
			Integer.MIN_VALUE);

	public static final BigDecimal MAX_VALUE_AS_DECIMAL = new BigDecimal(
			Integer.MAX_VALUE);

	public static final Int32 N_ONE = new Int32(-1);

	public static final Int32 ZERO = new Int32(0);

	public static final Int32 ONE = new Int32(1);

	public static final Int32[] ZERO_TWO_TWENTY = new Int32[] { ZERO, ONE,
			new Int32(2), new Int32(3), new Int32(4), new Int32(5),
			new Int32(6), new Int32(7), new Int32(8), new Int32(9),
			new Int32(10), new Int32(11), new Int32(12), new Int32(13),
			new Int32(14), new Int32(15), new Int32(16), new Int32(17),
			new Int32(18), new Int32(19), new Int32(20) };

	private final int v;

	private class DInt32 extends Int32 {
		private final Type type;

		public DInt32(int v, Type type) {
			super(v);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public Int32(String v) throws QueryException {
		try {
			this.v = Integer.parseInt(v);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}

	public Int32(Integer v) {
		this.v = v;
	}

	public Int32(int v) {
		this.v = v;
	}

	public static IntNumeric parse(String str) throws QueryException {
		str = Whitespace.collapseTrimOnly(str);
		int length = str.length();

		try {
			if ((length > 0) && (str.charAt(0) == '+')) {
				str = str.substring(1, length--);
			}
			if (length < 9) {
				return new Int32(Integer.parseInt(str));
			} else if (length < 18) {
				long lValue = Long.parseLong(str);
				return ((lValue <= Integer.MAX_VALUE) && (lValue >= Integer.MIN_VALUE)) ? new Int32(
						(int) lValue)
						: new Int64(lValue);
			} else {
				BigDecimal dValue = new BigDecimal(str);

				if (dValue.scale() > 0) {
					throw new QueryException(
							ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
							"Cannot cast %s to xs:integer", str);
				}

				return new Int(dValue);
			}
		} catch (NumberFormatException e) {
			throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:integer", str);
		}
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
		return validate(Type.INR, new DInt32(v, type));
	}

	@Override
	public IntNumeric inc() {
		return ((v < 20) && (v > 0)) ? ZERO_TWO_TWENTY[v + 1]
				: (v != Integer.MAX_VALUE) ? new Int32(v + 1) : new Int64(
						((long) v) + 1);
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return ((v != 0) && (v != Integer.MAX_VALUE) && (v != Integer.MIN_VALUE));
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof Int32) {
			return (v < ((Int32) other).v) ? -1 : (v == ((Int32) other).v) ? 0
					: 1;
		}
		if (other instanceof Numeric) {
			return -other.cmp(this);
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic other) {
		if (other instanceof Int32) {
			return (v < ((Int32) other).v) ? -1 : (v == ((Int32) other).v) ? 0
					: 1;
		}
		return -((AbstractNumeric) other).atomicCmpInternal(this);
	}

	@Override
	public String stringValue() {
		return Integer.toString(v);
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
		return v;
	}

	@Override
	public Numeric add(Numeric other) throws QueryException {
		if (other instanceof IntNumeric) {
			if (other instanceof LonNumeric) {
				if (other instanceof Int32) {
					return addInt(v, other.intValue());
				}
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
				if (other instanceof Int32) {
					return subtractInt(v, other.intValue());
				}
				return subtractLong(v, other.longValue());
			}
			return other.add(this);
		} else if (other instanceof DecNumeric) {
			return subtractBigDecimal(new BigDecimal(v), other.decimalValue(),
					false);
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
				if (other instanceof Int32) {
					return multiplyInt(v, other.intValue());
				}
				return multiplyLong(v, other.longValue());
			}
			return other.multiply(this);
		} else if (other instanceof DecNumeric) {
			return multiplyBigDecimal(new BigDecimal(v), other.decimalValue(),
					true);
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
				if (other instanceof Int32) {
					return divideInt(v, other.intValue());
				}
				return divideLong(v, other.longValue());
			}
			return other.add(this);
		} else if (other instanceof DecNumeric) {
			return divideBigDecimal(new BigDecimal(v), other.decimalValue(),
					false);
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
				if (other instanceof Int32) {
					return idivideInt(v, other.intValue());
				}
				return idivideLong(v, other.longValue());
			}
			return other.add(this);
		} else if (other instanceof DecNumeric) {
			return idivideBigDecimal(new BigDecimal(v), other.decimalValue(),
					false);
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
				if (other instanceof Int32) {
					return modInt(v, other.intValue());
				}
				return modLong(v, other.longValue());
			}
			return other.add(this);
		} else if (other instanceof DecNumeric) {
			return modBigDecimal(new BigDecimal(v), other.decimalValue(), false);
		} else if (other instanceof Dbl) {
			return modDouble(v, other.doubleValue());
		} else {
			return divideFloat(v, other.floatValue());
		}
	}

	@Override
	public Numeric negate() throws QueryException {
		return (v != Integer.MIN_VALUE) ? new Int32(-v) : new Int64(-(long) v);
	}

	public Numeric round() throws QueryException {
		return this;
	}

	public Numeric abs() throws QueryException {
		return (v >= 0) ? this : (v != Integer.MIN_VALUE) ? new Int32(-v)
				: new Int(-(long) v);
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

	@Override
	public int hashCode() {
		return new Integer(v).hashCode();
	}
}
