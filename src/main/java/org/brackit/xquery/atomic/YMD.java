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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class YMD extends AbstractDuration {
	private final short years;

	private final byte months; // 0..11 -> year wrap on overflow, highest bit

	// used to indicate negative duration

	private class DYMDur extends YMD {
		private final Type type;

		public DYMDur(boolean negative, short year, byte month, Type type) {
			super(negative, year, month);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public YMD(boolean negative, short years, byte months) {
		this.years = years;
		this.months = (!negative) ? months : (byte) (months | 0x80);
	}

	public YMD(String str) throws QueryException {
		boolean negative = false;
		short years = 0;
		byte months = 0; // 0..11 -> year wrap on overflow, highest bit used to
		// indicate negative duration

		char[] charArray = str.toCharArray();
		int pos = 0;
		int length = charArray.length;
		while ((pos < length) && (str.charAt(pos) == ' '))
			pos++; // strip leading whitespace

		if ((pos == length) || (charArray[pos] == '-')) {
			negative = true;
			pos++;
		}

		if (((length - pos) < 3) || (charArray[pos++] != 'P')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:duration", str);
		}

		int start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		int end = pos;
		int sectionTerminator = (pos < length) ? charArray[pos++] : -1;
		int v = (start != end) ? Integer.parseInt(str.substring(start, end))
				: -1; // parse leading value

		if (sectionTerminator == 'Y') {
			if (v > Short.MAX_VALUE) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:duration: component too large",
						str);
			}

			years = (short) v;

			start = pos;
			while ((pos < length) && ('0' <= charArray[pos])
					&& (charArray[pos] <= '9'))
				pos++;
			end = pos;
			sectionTerminator = (pos < length) ? charArray[pos++] : -1;
			v = (start != end) ? Integer.parseInt(str.substring(start, end))
					: -1;
		}

		if ((sectionTerminator == 'M') && (v > -1)) {
			int newYears = years + v / 12;
			v = v % 12;

			if (newYears > Short.MAX_VALUE) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:duration: component too large",
						str);
			}

			months |= v;
			years = (short) newYears;

			start = pos;
			while ((pos < length) && ('0' <= charArray[pos])
					&& (charArray[pos] <= '9'))
				pos++;
			end = pos;
			sectionTerminator = (pos < length) ? charArray[pos++] : -1;
		}

		if (sectionTerminator != -1) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:duration", str);
		}

		this.years = years;
		this.months = (!negative) ? months : (byte) (months | 0x80);
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		return type.instanceOf(Type.YMD) ? new DYMDur(months < 0, years,
				(byte) (months & 0x7F), type) : new Dur(months < 0, years,
				(byte) (months & 0x7F), (short) 0, (byte) 0, (byte) 0, 0)
				.asType(type);
	}

	@Override
	protected boolean zeroMonthsWhenZero() {
		return true;
	}

	@Override
	public int cmp(Atomic atomic) throws QueryException {
		// Note: The base type xs:duration is not ordered, i.e. it does
		// not support the <,<=,>,>= relationships but only tests for
		// equality. It must be ensured by the caller that an appropriate
		// error is raised.
		if (!(atomic instanceof YMD)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Cannot compare '%s with '%s'", type(), atomic.type());
		}
		return atomicCmpInternal(atomic);
	}

	@Override
	protected int atomicCmpInternal(Atomic atomic) {
		YMD other = (YMD) atomic;
		int sign = months & 0x80;
		int oSign = other.months & 0x80;
		if (sign != oSign) {
			return (sign < oSign) ? -1 : 1;
		}
		int res = (years - other.years);
		if (res != 0) {
			return res;
		}
		res = ((months & 0x7F) - ((other.months & 0x7F)));
		return res;
	}

	@Override
	public int atomicCode() {
		return Type.YMD_CODE;
	}

	public YMD add(YMD other) throws QueryException {
		return addInternal(other.isNegative(), other.getYears(), other
				.getMonths());
	}

	public YMD subtract(YMD other) throws QueryException {
		return addInternal(!other.isNegative(), other.getYears(), other
				.getMonths());
	}

	public YMD multiply(Dbl dbl) throws QueryException {
		double v = dbl.doubleValue();

		if (v == Double.NaN) {
			throw new QueryException(ErrorCode.ERR_PARAMETER_NAN);
		}
		if ((v == Double.NEGATIVE_INFINITY) || (v == Double.POSITIVE_INFINITY)) {
			throw new QueryException(
					ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
		}

		long newYears = Math.round(getYears() * v);
		long newMonths = Math.round(getMonths() * v);
		boolean newNegative = (isNegative() ^ (v < 0));

		if (isNegative() ^ newNegative) {
			newYears *= -1;
			newMonths *= -1;
		}

		newYears += (newMonths / 12);
		newMonths %= 12;

		if (newYears > Short.MAX_VALUE) {
			throw new QueryException(
					ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
		}

		return new YMD(newNegative, (short) newYears, (byte) newMonths);
	}

	public YMD divide(Dbl dbl) throws QueryException {
		double v = dbl.doubleValue();

		if (v == Double.NaN) {
			throw new QueryException(ErrorCode.ERR_PARAMETER_NAN);
		}
		if ((v == Double.NEGATIVE_INFINITY) || (v == Double.POSITIVE_INFINITY)) {
			return new YMD(false, (short) 0, (byte) 0);
		}

		long newYears = Math.round(getYears() / v);
		long newMonths = Math.round(getMonths() / v);
		boolean newNegative = (isNegative() ^ (v < 0));

		if (isNegative() ^ newNegative) {
			newYears *= -1;
			newMonths *= -1;
		}

		newYears += (newMonths / 12);
		newMonths %= 12;

		if (newYears > Short.MAX_VALUE) {
			throw new QueryException(
					ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
		}

		return new YMD(newNegative, (short) newYears, (byte) newMonths);
	}

	public Dbl divide(YMD dur) throws QueryException {
		int a = getYears() * 12 + getMonths();
		int b = dur.getYears() * 12 + dur.getMonths();

		if (b == 0) {
			throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
		}

		return new Dbl(a / b);
	}

	private YMD addInternal(boolean n2, short y2, byte m2)
			throws QueryException {
		boolean n1 = isNegative();
		byte m1 = getMonths();
		short y1 = getYears();

		if (n1) {
			m1 *= -1;
			y1 *= -1;
		}
		if (n2) {
			m2 *= -1;
			y2 *= -1;
		}

		int newMonths = m1 + m2;
		int newYears = y1 + y2;
		boolean newNegative = newYears < 0;

		if (newNegative) {
			newYears *= -1;
			newMonths *= -1;
		}

		newYears += (newMonths / 12);
		newMonths %= 12;

		if (newYears > Short.MAX_VALUE) {
			throw new QueryException(
					ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
		}

		return new YMD(newNegative, (short) newYears, (byte) newMonths);
	}

	@Override
	public Type type() {
		return Type.YMD;
	}

	@Override
	public boolean isNegative() {
		return (months < 0);
	}

	@Override
	public byte getMonths() {
		return (byte) (months & 0x7F);
	}

	@Override
	public short getYears() {
		return years;
	}

	@Override
	public short getDays() {
		return 0;
	}

	@Override
	public byte getHours() {
		return 0;
	}

	@Override
	public byte getMinutes() {
		return 0;
	}

	@Override
	public int getMicros() {
		return 0;
	}
}
