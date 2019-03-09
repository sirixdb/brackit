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

import java.util.TimeZone;
import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Type;

/**
 *
 * @author Sebastian Baechle
 *
 */
public abstract class AbstractTimeInstant extends AbstractAtomic implements
		TimeInstant {
	public static final DTD UTC_TIMEZONE = new DTD(false, (byte) 0, (byte) 0,
			(byte) 0, (byte) 0);

	public static final DTD MIN_TIMEZONE = new DTD(true, (byte) 14, (byte) 0,
			(byte) 0, (byte) 0);

	public static final DTD MAX_TIMEZONE = new DTD(false, (byte) 14, (byte) 0,
			(byte) 0, (byte) 0);

	public static DTD LOCAL_TIMEZONE;

	static {
		int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
		int hours = fQuotient(offset, 3600000);
		int remainder = modulo(offset, 3600000);
		int minutes = fQuotient(remainder, 60000);
		remainder = modulo(remainder, 60000);
		int micros = remainder * 1000;
		LOCAL_TIMEZONE = new DTD(offset < 0, (short) 0, (byte) hours,
				(byte) minutes, micros);
	}

	@Override
	public int hashCode() {
		throw new RuntimeException("Not implemented yet");
	}

	protected DTD parseTimezone(String str, char[] charArray, int pos,
			int length) throws QueryException {
		boolean negative = false;
		byte hour = 0;
		byte minute = 0;

		if (charArray[pos] == 'Z') {
			// UTC
			pos++;
		} else if ((charArray[pos] == '+') || (charArray[pos] == '-')) {
			negative = (charArray[pos++] == '-');

			// parse hour
			int start = pos;
			while ((pos < length) && ('0' <= charArray[pos])
					&& (charArray[pos] <= '9'))
				pos++;
			int end = pos;
			int v = (end - start == 2) ? Integer.parseInt(str.substring(start,
					end)) : -1;
			if ((v < 0) || (v > 24)) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:dateTime: illegal hour", str);
			}
			hour = (byte) v;

			// consume ':'
			if ((pos >= length) || (charArray[pos++] != ':')) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Illegal hour in timezone: %s", str);
			}

			// parse minute
			start = pos;
			while ((pos < length) && ('0' <= charArray[pos])
					&& (charArray[pos] <= '9'))
				pos++;
			end = pos;
			v = (end - start == 2) ? Integer
					.parseInt(str.substring(start, end)) : -1;
			if ((v < 0) || (v > 59)) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Illegal minute in timezone: %s", str);
			}
			minute = (byte) v;
		} else {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Illegal timezone: %s", str);
		}

		if (pos != length) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Illegal timezone: %s", str);
		}

		if ((hour == 0) && (minute == 0)) {
			return UTC_TIMEZONE;
		}

		return new DTD(negative, (short) 0, hour, minute, 0);
	}

	protected int cmp(AbstractTimeInstant other) {
		boolean aHasTZ = false;
		boolean bHasTZ = false;
		AbstractTimeInstant a = this;
		AbstractTimeInstant b = other;

		if ((a.getTimezone() != null)
				&& ((a.getTimezone().getHours() != 0) || (a.getTimezone()
						.getMinutes() != 0))) {
			a = new DateTime(a.getYear(), a.getMonth(), a.getDay(),
					a.getHours(), a.getMinutes(), a.getMicros(),
					a.getTimezone()).canonicalize();
			aHasTZ = true;
		}
		if ((b.getTimezone() != null)
				&& ((b.getTimezone().getHours() != 0) || (b.getTimezone()
						.getMinutes() != 0))) {
			b = new DateTime(b.getYear(), b.getMonth(), b.getDay(),
					b.getHours(), b.getMinutes(), b.getMicros(),
					b.getTimezone()).canonicalize();
			bHasTZ = true;
		}

		if (!(aHasTZ ^ bHasTZ)) {
			return compareFields(a, b);
		} else if (aHasTZ) {
			AbstractTimeInstant b2 = b.add(false, MAX_TIMEZONE, UTC_TIMEZONE);
			int res = compareFields(a, b2);
			if (res < 0) {
				return res;
			}
			AbstractTimeInstant b3 = b.add(true, MIN_TIMEZONE, UTC_TIMEZONE);
			res = compareFields(a, b3);
			if (res > 0) {
				return res;
			}
			return 0; // undecidable
		} else {
			AbstractTimeInstant a2 = a.add(true, MIN_TIMEZONE, UTC_TIMEZONE);
			int res = compareFields(a2, b);
			if (res < 0) {
				return res;
			}
			AbstractTimeInstant a3 = a.add(false, MAX_TIMEZONE, UTC_TIMEZONE);
			res = compareFields(a3, b);
			if (res > 0) {
				return res;
			}
			return 0; // undecidable
		}
	}

	private int compareFields(TimeInstant a, TimeInstant b) {
		int res = a.getYear() - b.getYear();
		if (res != 0) {
			return res;
		}
		res = a.getMonth() - b.getMonth();
		if (res != 0) {
			return res;
		}
		res = a.getDay() - b.getDay();
		if (res != 0) {
			return res;
		}
		res = a.getHours() - b.getHours();
		if (res != 0) {
			return res;
		}
		res = a.getMinutes() - b.getMinutes();
		if (res != 0) {
			return res;
		}
		res = a.getMicros() - b.getMicros();
		return res;
	}

	protected AbstractTimeInstant add(boolean negate, Duration duration,
			DTD newTimezone) {
		short durationYears = duration.getYears();
		byte durationMonths = duration.getMonths();
		short durationDays = duration.getDays();
		byte durationHours = duration.getHours();
		byte durationMinutes = duration.getMinutes();
		int durationMicros = duration.getMicros();

		if (negate) {
			durationYears *= -1;
			durationMonths *= -1;
			durationDays *= -1;
			durationHours *= -1;
			durationMinutes *= -1;
			durationMicros *= -1;
		}

		int temp = getMonth() + durationMonths;
		int newMonth = modulo(temp, 1, 13);
		int carry = fQuotient(temp, 1, 13);
		int newYear = getYear() + durationYears + carry;

		temp = getMicros() + durationMicros;
		int newMicros = modulo(temp, 60000000);
		carry = fQuotient(temp, 60000000);

		temp = getMinutes() + durationMinutes + carry;
		int newMinutes = modulo(temp, 60);
		carry = fQuotient(temp, 60);

		temp = getHours() + durationHours + carry;
		int newHours = modulo(temp, 24);
		carry = fQuotient(temp, 24);

		byte maxDayInMonth = maxDayInMonth(newYear, newMonth);
		int newDays = ((getDay() > maxDayInMonth) ? maxDayInMonth
				: (getDay() < 1) ? 1 : getDay()) + durationDays + carry;

		while (true) {
			if (newDays < 0) {
				newDays += maxDayInMonth(newYear, newMonth - 1);
				carry = -1;
			} else if (newDays > maxDayInMonth(newYear, newMonth)) {
				newDays -= maxDayInMonth(newYear, newMonth);
				carry = 1;
			} else {
				break;
			}
			temp = newMonth + carry;
			newMonth = modulo(temp, 1, 13);
			newYear += fQuotient(temp, 1, 13);
		}

		return create((short) newYear, (byte) newMonth, (byte) newDays,
				(byte) newHours, (byte) newMinutes, newMicros, newTimezone);
	}

	protected DTD subtract(AbstractTimeInstant b) {
		boolean negative = false;
		AbstractTimeInstant a = this;

		if (a.getTimezone() != null) {
			a = a.canonicalize();
		}
		if (b.getTimezone() != null) {
			b = b.canonicalize();
		}

		if (a.cmp(b) <= 0) {
			a = b;
			b = this;
			negative = true;
		}

		int carry = 0;
		int micros = a.getMicros() - b.getMicros();
		if (micros < 0) {
			micros *= -1;
			carry = 1;
		}
		int minutes = a.getMinutes() - b.getMinutes() + carry;
		if (minutes < 0) {
			minutes *= -1;
			carry = 1;
		} else {
			carry = 0;
		}

		short days = 0;
		int hours = 0;
		int ehour = b.getHours() + carry;
		int eyear = b.getYear();
		int emonth = b.getMonth();
		int eday = b.getDay();

		if (ehour < a.getHours()) {
			hours = a.getHours() - ehour;
		} else {
			hours = (24 - ehour) + a.getHours();
			if (eday == maxDayInMonth(eyear, emonth)) {
				if (emonth == 12) {
					eyear++;
					emonth = 1;
				} else {
					emonth++;
				}
				eday = 1;
			} else {
				eday++;
			}
		}

		if (eyear < a.getYear()) {
			// advance days to 1st. of next month
			byte maxDayInMonth = maxDayInMonth(eyear, emonth);
			days += (maxDayInMonth - eday + 1);
			eday = 1;

			// advance months to next year
			while (++emonth <= 12) {
				byte maxDayInMonth2 = maxDayInMonth(eyear, emonth);
				days += maxDayInMonth2;
			}
			emonth = 1;

			// advance years
			while (++eyear < a.getYear()) {
				boolean isLeap = ((eyear % 400 == 0) || ((eyear % 100 != 0) && (eyear % 4 == 0)));
				days += (isLeap) ? 366 : 365;
			}
		}

		if (emonth < a.getMonth()) {
			// advance days to 1st. of next month
			byte maxDayInMonth = maxDayInMonth(eyear, emonth);
			days += (maxDayInMonth - eday + 1);
			eday = 1;

			// advance months
			while (++emonth < a.getMonth()) {
				byte maxDayInMonth2 = maxDayInMonth(eyear, emonth);
				days += maxDayInMonth2;
			}
		}

		if (eday < a.getDay()) {
			// advance days
			days += (a.getDay() - eday);
		}

		return new DTD(negative, days, (byte) hours, (byte) minutes, micros);
	}

	private static int fQuotient(int a, int low, int high) {
		return fQuotient(a - low, high - low);
	}

	private static int modulo(int a, int b) {
		return a - fQuotient(a, b) * b;
	}

	private static int fQuotient(int a, int b) {
		return (a >= 0) ? a / b : (((a / b) * b) == a) ? a / b : a / b - 1;
	}

	private static int modulo(int a, int low, int high) {
		return modulo(a - low, high - low) + low;
	}

	@Override
	protected final int atomicCmpInternal(Atomic atomic) {
		AbstractTimeInstant other = (AbstractTimeInstant) atomic;
		int res = cmp(other);

		if (res != 0) {
			return res;
		}

		if (getYear() != other.getYear()) {
			return getYear() - other.getYear();
		}
		if (getMonth() != other.getMonth()) {
			return getMonth() - other.getMonth();
		}
		if (getDay() != other.getDay()) {
			return getDay() - other.getDay();
		}
		if (getHours() != other.getHours()) {
			return getHours() - other.getHours();
		}
		if (getMinutes() != other.getMinutes()) {
			return getMinutes() - other.getMinutes();
		}
		if (getMicros() != other.getMicros()) {
			return getMicros() - other.getMicros();
		}
		DTD tz = getTimezone();
		DTD otz = other.getTimezone();
		return (tz == null) ? ((otz == null) ? 0 : -1) : (otz == null) ? 1 : tz
				.atomicCmpInternal(otz);
	}

	protected abstract AbstractTimeInstant create(short year, byte month,
			byte day, byte hours, byte minutes, int micros, DTD timezone);

	protected byte maxDayInMonth(int year, int month) {
		int m = (month % 13);
		int y = year + month / 13;

		if (m == 2) {
			return ((y % 400 == 0) || ((y % 100 != 0) && (y % 4 == 0))) ? (byte) 29
					: (byte) 28;
		} else if ((m == 4) || (m == 6) || (m == 9) || (m == 11)) {
			return 30;
		} else {
			return 31;
		}
	}

	protected String timezoneString() {
		DTD timezone = getTimezone();
		String tzTmp = "";
		if (timezone != null) {
			byte tzHours = timezone.getHours();
			String tzHTmp = ((tzHours < 10) ? "0" : "") + tzHours;
			byte tzMinutes = timezone.getMinutes();
			String tzMinTmp = ((tzMinutes < 10) ? "0" : "") + tzMinutes;
			tzTmp = ((tzHours == 0) && (tzMinutes == 0)) ? "Z" : ((timezone
					.isNegative()) ? "-" : "+") + tzHTmp + ":" + tzMinTmp;
		}
		return tzTmp;
	}

	@Override
	public AbstractTimeInstant canonicalize() {
		DTD timezone = getTimezone();
		if ((timezone == null)
				|| ((timezone.getDays() == 0) && (timezone.getHours() == 0))) {
			return this;
		}
		return add(!timezone.isNegative(), timezone, UTC_TIMEZONE);
	}

	@Override
	public final Atomic asType(Type type) throws QueryException {
		throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
	}

	@Override
	public final boolean booleanValue() throws QueryException {
		throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
				"Effective boolean value of '%s' is undefined.", type());
	}
}
