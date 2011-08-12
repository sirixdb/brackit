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
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 *
 */
public class Int extends AbstractNumeric implements IntNumeric {
	private final BigDecimal v;

	private class DInt extends Int {
		private final Type type;

		public DInt(BigDecimal v, Type type) {
			super(v);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public Int(String v) throws QueryException {
		try {
			this.v = new BigDecimal(v);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}

	public Int(BigDecimal v) {
		this.v = v.setScale(0, RoundingMode.FLOOR);
	}

	public Int(double v) {
		this.v = new BigDecimal((int) v);
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
		return validate(Type.INR, new DInt(v, type));
	}

	@Override
	public IntNumeric inc() {
		return new Int(v.add(BigDecimal.ONE));
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return (!v.equals(BigDecimal.ZERO));
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof DecNumeric) {
			return v.compareTo(((Numeric) other).decimalValue());
		} else if (other instanceof DblNumeric) {
			return Double.compare(v.doubleValue(), ((Numeric) other)
					.doubleValue());
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
			return Double.compare(v.doubleValue(), ((Numeric) other)
					.doubleValue());
		} else {
			return Float
					.compare(v.floatValue(), ((Numeric) other).floatValue());
		}
	}

	@Override
	public String stringValue() {
		return v.toString();
	}

	public BigDecimal decimalValue() {
		return v;
	}

	public BigDecimal integerValue() {
		return v;
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
			return addBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DblNumeric) {
			return addDouble(v.doubleValue(), other.doubleValue());
		} else {
			return addFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric subtract(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return subtractBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DblNumeric) {
			return subtractDouble(v.doubleValue(), other.doubleValue());
		} else {
			return subtractFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric multiply(Numeric other) throws QueryException {
		if (other instanceof IntNumeric) {
			return multiplyBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DecNumeric) {
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
			return divideBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DblNumeric) {
			return divideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return divideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric idiv(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return idivideBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DblNumeric) {
			return idivideDouble(v.doubleValue(), other.doubleValue());
		} else {
			return idivideFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric mod(Numeric other) throws QueryException {
		if (other instanceof DecNumeric) {
			return modBigDecimal(v, other.decimalValue(), false);
		} else if (other instanceof DblNumeric) {
			return modDouble(v.doubleValue(), other.doubleValue());
		} else {
			return modFloat(v.floatValue(), other.floatValue());
		}
	}

	@Override
	public Numeric negate() throws QueryException {
		return new Int(v.negate());
	}

	public Numeric round() throws QueryException {
		return this;
	}

	@Override
	public Numeric abs() throws QueryException {
		return (v.signum() >= 0) ? this : new Int(v.negate());
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
		return v.hashCode();
	}
}