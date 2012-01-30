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
import java.math.RoundingMode;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Dec extends AbstractNumeric implements DecNumeric {
	private final BigDecimal v;

	private class DDec extends Dec {
		private final Type type;

		public DDec(BigDecimal v, Type type) {
			super(v);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public Dec(String str) throws QueryException {
		try {
			this.v = new BigDecimal(Whitespace.collapseTrimOnly(str));
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:decimal", str);
		}
	}

	public Dec(BigDecimal v) {
		this.v = v;
	}

	public Dec(Dec v) {
		this.v = v.v;
	}

	@Override
	public Type type() {
		return Type.DEC;
	}

	@Override
	public IntNumeric asIntNumeric() {
		try {
			v.toBigIntegerExact();
			return new Int(v);
		} catch (ArithmeticException e) {
			return null;
		}
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		return new DDec(v, type);
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return (v.intValue() != 0);
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof DecNumeric) {
			return v.compareTo(((Numeric) other).decimalValue());
		} else if (other instanceof DblNumeric) {
			return Double.compare(v.doubleValue(),
					((Numeric) other).doubleValue());
		} else if (other instanceof FltNumeric) {
			return Float
					.compare(v.floatValue(), ((Numeric) other).floatValue());
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic other) {
		if (other instanceof DecNumeric) {
			return v.compareTo(((Numeric) other).decimalValue());
		} else if (other instanceof DblNumeric) {
			return Double.compare(v.doubleValue(),
					((Numeric) other).doubleValue());
		} else {
			return Float
					.compare(v.floatValue(), ((Numeric) other).floatValue());
		}
	}

	@Override
	public String stringValue() {
		String s = v.toPlainString();
		return (v.scale() <= 0) ? s : killTrailingZeros(s);
	}

	public static void main(String[] args) throws Exception {
		System.out
				.println(new Dec(
						"0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001")
						.toString());

		System.out.println(new Dec(new BigDecimal(-0.0d)).toString());
		System.out.println(new Dec("-2300.44004000"));
		System.out.println(new Dec("40"));
		System.out.println(new Dec("40.0"));
	}

	public BigDecimal decimalValue() {
		return v;
	}

	public BigDecimal integerValue() {
		return v.setScale(0, RoundingMode.FLOOR);
	}

	@Override
	public double doubleValue() {
		return v.doubleValue();
	}

	@Override
	public float floatValue() {
		return v.floatValue();
	}

	@Override
	public long longValue() {
		return v.longValue();
	}

	@Override
	public int intValue() {
		return v.intValue();
	}

	@Override
	public Numeric add(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return addBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return addDouble(v.doubleValue(), other.doubleValue());
		} else {
			return addFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric subtract(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return subtractBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return subtractDouble(v.doubleValue(), other.doubleValue());
		} else {
			return subtractFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric multiply(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return multiplyBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return multiplyDouble(v.doubleValue(), other.doubleValue());
		} else {
			return multiplyFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric div(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return divideBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return divideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return divideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric idiv(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return idivideBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return idivideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return idivideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric mod(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return modBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DblNumeric) {
			return modDouble(v.doubleValue(), other.doubleValue());
		} else {
			return modFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric negate() throws QueryException {
		return new Dec(v.negate());
	}

	public Numeric round() throws QueryException {
		return (v.signum() >= 0) ? new Dec(v.setScale(0, RoundingMode.HALF_UP))
				: new Dec(v.setScale(0, RoundingMode.DOWN));
	}

	@Override
	public Numeric abs() throws QueryException {
		return (v.signum() >= 0) ? this : new Int(v.negate());
	}

	@Override
	public Numeric ceiling() throws QueryException {
		return new Dec(v.setScale(0, RoundingMode.CEILING));
	}

	@Override
	public Numeric floor() throws QueryException {
		return new Dec(v.setScale(0, RoundingMode.FLOOR));
	}

	@Override
	public Numeric roundHalfToEven(int precision) throws QueryException {
		BigDecimal bd = v.scaleByPowerOfTen(precision);
		bd = bd.setScale(0, BigDecimal.ROUND_HALF_EVEN);
		return new Dec(bd.scaleByPowerOfTen(-precision));
	}

	@Override
	public int hashCode() {
		return new Double(v.doubleValue()).hashCode();
	}
}