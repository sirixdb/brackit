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
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Dbl extends AbstractNumeric implements DblNumeric {
	public static final Dbl NaN = new Dbl(Double.NaN);

	public static final Dbl NINF = new Dbl(Double.NEGATIVE_INFINITY);

	public static final Dbl PINF = new Dbl(Double.POSITIVE_INFINITY);

	public double v;

	private class DDbl extends Dbl {
		private final Type type;

		public DDbl(double v, Type type) {
			super(v);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public Dbl(Double v) {
		this.v = v;
	}

	public Dbl(double v) {
		this.v = v;
	}

	public Dbl(Dbl v) {
		this.v = v.v;
	}

	public Dbl(String str) throws QueryException {
		double parsed1;
		try {
			str = Whitespace.collapseTrimOnly(str);
			parsed1 = Double.parseDouble(str);
		} catch (NumberFormatException e) {
			if (str.equals("INF")) {
				parsed1 = Double.POSITIVE_INFINITY;
			} else if (str.equals("-INF")) {
				parsed1 = Double.NEGATIVE_INFINITY;
			} else if (str.equals("NaN")) {
				parsed1 = Double.NaN;
			}
			throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:double", str);
		}
		double parsed = parsed1;
		this.v = parsed;
	}

	public static Dbl parse(String str) throws QueryException {
		try {
			str = Whitespace.collapseTrimOnly(str);
			double dbl = Double.parseDouble(str);
			return new Dbl(dbl);
		} catch (NumberFormatException e) {
			if (str.equals("INF")) {
				return Dbl.PINF;
			}
			if (str.equals("-INF")) {
				return Dbl.NINF;
			}
			if (str.equals("NaN")) {
				return Dbl.NaN;
			}
			throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:double", str);
		}
	}

	@Override
	public Type type() {
		return Type.DBL;
	}

	@Override
	public IntNumeric asIntNumeric() {
		if (Double.isNaN(v) || Double.isInfinite(v)) {
			return null;
		}
		long i = (long) v;
		double f = v - i;
		return (f == 0.0) ? new Int64(i) : null;
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		return validate(Type.DBL, new DDbl(v, type));
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return ((v != 0) && (!Double.isNaN(v)) && (!Double.isInfinite(v)));
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof Numeric) {
			// Implies numeric type promotion and substitution
			return Double.compare(v, ((Numeric) other).doubleValue());
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic atomic) {
		// Implies numeric type promotion and substitution
		return Double.compare(v, ((Numeric) atomic).doubleValue());
	}

	@Override
	public String stringValue() {
		if (Double.isNaN(v))
			return "NaN";
		if (Double.isInfinite(v))
			return (v > 0) ? "INF" : "-INF"; 
		if (v == 0)
			return (1/v == 1/0.0) ? "0" : "-0";
		return killTrailingZeros(((v > 0) && (v >= 1e-6) && (v < 1e6) || (-v >= 1e-6)
				&& (-v < 1e6)) ? DD.format(v) : SD.format(v));
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
		return (float) v;
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
		return addDouble(v, other.doubleValue());
	}

	@Override
	public Numeric subtract(Numeric other) throws QueryException {
		return subtractDouble(v, other.doubleValue());
	}

	@Override
	public Numeric multiply(Numeric other) throws QueryException {
		return multiplyDouble(v, other.doubleValue());
	}

	@Override
	public Numeric div(Numeric other) throws QueryException {
		return divideDouble(v, other.doubleValue());
	}

	@Override
	public Numeric idiv(Numeric other) throws QueryException {
		return idivideDouble(v, other.doubleValue());
	}

	@Override
	public Numeric mod(Numeric other) throws QueryException {
		return modDouble(v, other.doubleValue());
	}

	@Override
	public Numeric negate() throws QueryException {
		return new Dbl(-v);
	}

	@Override
	public Numeric round() throws QueryException {
		return ((Double.isInfinite(v)) || (v == 0) || (Double.isNaN(v))) ? this
				: new Dbl(Math.round(v));
	}

	@Override
	public Numeric abs() throws QueryException {
		return ((v >= 0) || (Double.isNaN(v))) ? this : new Dbl(Math.abs(v));
	}

	@Override
	public Numeric ceiling() throws QueryException {
		return ((Double.isInfinite(v)) || (v == 0) || (Double.isNaN(v))) ? this
				: new Dbl(Math.ceil(v));
	}

	@Override
	public Numeric floor() throws QueryException {
		return ((Double.isInfinite(v)) || (v == 0) || (Double.isNaN(v))) ? this
				: new Dbl(Math.floor(v));
	}

	@Override
	public Numeric roundHalfToEven(int precision) throws QueryException {
		if ((Double.isInfinite(v)) || (v == 0) || (Double.isNaN(v))) {
			return this;
		}

		double factor = Math.pow(10, precision);
		double scaled = v * factor;

		if (Double.isInfinite(scaled)) {
			BigDecimal bd = new BigDecimal(v);
			bd = bd.scaleByPowerOfTen(precision);
			bd = bd.setScale(0, BigDecimal.ROUND_HALF_EVEN);
			bd = bd.scaleByPowerOfTen(-precision);
			return new Dbl(bd.doubleValue());
		}

		scaled = Math.rint(scaled);
		return new Dbl(scaled / factor);
	}

	@Override
	public int hashCode() {
		return new Double(v).hashCode();
	}
}
