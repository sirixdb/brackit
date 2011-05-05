/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
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
import java.math.RoundingMode;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

public class Dec extends AbstractNumeric implements DecimalNumeric {
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
	public Atomic asType(Type type) throws QueryException {
		return new DDec(v, type);
	}

	@Override
	public boolean booleanValue(QueryContext ctx) throws QueryException {
		return (v.intValue() != 0);
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return v.compareTo(((Numeric) other).decimalValue());
		} else if (other instanceof DoubleNumeric) {
			return Double.compare(v.doubleValue(), ((Numeric) other)
					.doubleValue());
		} else if (other instanceof FloatNumeric) {
			return Float
					.compare(v.floatValue(), ((Numeric) other).floatValue());
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic other) {
		if (other instanceof DecimalNumeric) {
			return v.compareTo(((Numeric) other).decimalValue());
		} else if (other instanceof DoubleNumeric) {
			return Double.compare(v.doubleValue(), ((Numeric) other)
					.doubleValue());
		} else {
			return Float
					.compare(v.floatValue(), ((Numeric) other).floatValue());
		}
	}

	@Override
	public String stringValue() {
		String killTrailingZeros = killTrailingZeros(v.toString());
		return killTrailingZeros;
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
		if (other instanceof DecimalNumeric) {
			return addBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
			return addDouble(v.doubleValue(), other.doubleValue());
		} else {
			return addFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric subtract(Numeric other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return subtractBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
			return subtractDouble(v.doubleValue(), other.doubleValue());
		} else {
			return subtractFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric multiply(Numeric other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return multiplyBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
			return multiplyDouble(v.doubleValue(), other.doubleValue());
		} else {
			return multiplyFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric div(Numeric other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return divideBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
			return divideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return divideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric idiv(Numeric other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return idivideBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
			return idivideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return idivideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric mod(Numeric other) throws QueryException {
		if (other instanceof DecimalNumeric) {
			return modBigDecimal(v, other.decimalValue(), true);
		} else if (other instanceof DoubleNumeric) {
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