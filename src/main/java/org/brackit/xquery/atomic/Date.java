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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Date extends AbstractTimeInstant {
	private final short year;
	private final byte month;
	private final byte day;
	private final DTD timezone;

	public Date(short year, byte month, byte day, DTD timezone) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.timezone = timezone;
	}

	public Date(String str) throws QueryException {
		short year = 0;
		byte month = 0;
		byte day = 0;
		DTD timezone = null;

		str = Whitespace.collapseTrimOnly(str);
		char[] charArray = str.toCharArray();
		int pos = 0;
		int length = charArray.length;

		// parse variable length year
		boolean negative = false;
		if ((pos == length) || (charArray[pos] == '-')) {
			negative = true;
			pos++;
		}

		int start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		int end = pos;

		if (end - start < 4) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date", str);
		} else if ((end - start > 4) && (negative)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date", str);
		}
		int v = (start != end) ? Integer.parseInt(str.substring(start, end))
				: -1; // parse leading value

		if ((v > Short.MAX_VALUE) || (v == 0)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date", str);
		}
		year = (negative) ? (short) -v : (short) v;

		// consume '-'
		if ((pos >= length) || (charArray[pos++] != '-')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date", str);
		}

		// parse month
		start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		end = pos;
		v = (end - start == 2) ? Integer.parseInt(str.substring(start, end))
				: -1;
		if ((v < 1) || (v > 12)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date: illegal month", str);
		}
		month = (byte) v;

		// consume '-'
		if ((pos >= length) || (charArray[pos++] != '-')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date", str);
		}

		// parse day
		start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		end = pos;
		v = (end - start == 2) ? Integer.parseInt(str.substring(start, end))
				: -1;
		if ((v < 1) || (v > 31)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date: illegal day", str);
		}
		day = (byte) v;

		// fix it up first before checking for optional timezone
		byte maxDaysInMonth = maxDayInMonth(year, month);

		if (day > maxDaysInMonth) {
			throw new QueryException(
					ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:date: Day %s does not exist in month %s of year %s",
					day, month, year, str);
		}

		if (pos < length) {
			timezone = parseTimezone(str, charArray, pos, length);
		}

		this.year = year;
		this.month = month;
		this.day = day;
		this.timezone = timezone;
	}

	public Date(DateTime dateTime) {
		this.year = dateTime.getYear();
		this.month = dateTime.getMonth();
		this.day = dateTime.getDay();
		this.timezone = dateTime.getTimezone();
	}

	@Override
	public Type type() {
		return Type.DATE;
	}

	public Date add(DTD dayTimeDuration) throws QueryException {
		return (Date) add(dayTimeDuration.isNegative(), dayTimeDuration,
				timezone);
	}

	public Date add(YMD yearMonthDuration) throws QueryException {
		return (Date) add(yearMonthDuration.isNegative(), yearMonthDuration,
				timezone);
	}

	public Date subtract(DTD dayTimeDuration) throws QueryException {
		return (Date) add(!dayTimeDuration.isNegative(), dayTimeDuration,
				timezone);
	}

	public Date subtract(YMD yearMonthDuration) throws QueryException {
		return (Date) add(!yearMonthDuration.isNegative(), yearMonthDuration,
				timezone);
	}
	
	public DTD subtract(Date date) throws QueryException {
		return super.subtract(date);
	}

	@Override
	public int cmp(Atomic atomic) throws QueryException {
		if (!(atomic instanceof Date)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Cannot compare '%s with '%s'", type(), atomic.type());
		}
		return cmp((Date) atomic);
	}

	@Override
	public int atomicCode() {
		return Type.DATE_CODE;
	}

	@Override
	public String stringValue() {
		String yTmp = (year < 0) ? "-" : ""
				+ ((year < 10) ? "000" : (year < 100) ? "00"
						: (year < 1000) ? "0" : "") + year;
		String mTmp = ((month < 10) ? "0" : "") + month;
		String dTmp = ((day < 10) ? "0" : "") + day;
		String tzTmp = timezoneString();

		return String.format("%s-%s-%s%s", yTmp, mTmp, dTmp, tzTmp);
	}

	public short getYear() {
		return year;
	}

	public byte getMonth() {
		return month;
	}

	public byte getDay() {
		return day;
	}

	public byte getHours() {
		return 0;
	}

	public byte getMinutes() {
		return 0;
	}

	public int getMicros() {
		return 0;
	}

	public DTD getTimezone() {
		return timezone;
	}

	@Override
	protected AbstractTimeInstant create(short year, byte month, byte day,
			byte hours, byte minutes, int micros, DTD timezone) {
		return new Date(year, month, day, timezone);
	}
}
