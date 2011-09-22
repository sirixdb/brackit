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
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Time extends AbstractTimeInstant {
	private final byte hour;
	private final byte minute;
	private final int micros;
	private final DTD timezone;

	public Time(byte hour, byte minute, int micros, DTD timezone) {
		this.hour = hour;
		this.minute = minute;
		this.micros = micros;
		this.timezone = timezone;
	}

	public Time(DateTime dateTime) {
		hour = dateTime.getHours();
		minute = dateTime.getMinutes();
		micros = dateTime.getMicros();
		timezone = dateTime.getTimezone();
	}

	public Time(String str) throws QueryException {
		byte hour = 0;
		byte minute = 0;
		int micros = 0;
		DTD timezone = null;

		str = Whitespace.collapseTrimOnly(str);
		char[] charArray = str.toCharArray();
		int pos = 0;
		int length = charArray.length;

		// parse hour
		int start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		int end = pos;
		int v = (end - start == 2) ? Integer
				.parseInt(str.substring(start, end)) : -1;
		if ((v < 0) || (v > 24)) // attention 24 is only allowed if the minutes
		// are zero!
		{
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:time: illegal hour", str);
		}
		hour = (byte) v;

		// consume ':'
		if ((pos >= length) || (charArray[pos++] != ':')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:time", str);
		}

		// parse hour
		start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		end = pos;
		v = (end - start == 2) ? Integer.parseInt(str.substring(start, end))
				: -1;
		if ((v < 0) || (v > 59)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:time: illegal minute", str);
		}
		minute = (byte) v;

		// consume ':'
		if ((pos >= length) || (charArray[pos++] != ':')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:time", str);
		}

		start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		end = pos;
		v = (end - start == 2) ? Integer.parseInt(str.substring(start, end))
				: -1;
		if ((v < 0) || (v > 59)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:time: illegal seconds", str);
		}
		micros = v * 1000000;

		if ((pos < length) && (charArray[pos] == '.')) {
			start = ++pos;
			while ((pos < length) && ('0' <= charArray[pos])
					&& (charArray[pos] <= '9'))
				pos++;
			end = pos;
			int l = end - start;
			v = (start != end) ? Integer.parseInt(str.substring(start, start
					+ Math.min(l, 6))) : -1; // drop nano seconds

			if (v == -1) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:time: illegal seconds", str);
			}
			if (v > 0) {
				for (int i = 0; i < 6 - l; i++)
					v *= 10;
				micros += v;
			}
		}

		// fix it up first before checking for optional timezone
		if (hour == 24) {
			if (minute != 0) {
				throw new QueryException(
						ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:dateTime: 24 hours is not allowed when minutes is non-zero",
						str);
			}
		}

		if (pos < length) {
			timezone = parseTimezone(str, charArray, pos, length);
		}

		this.hour = hour;
		this.minute = minute;
		this.micros = micros;
		this.timezone = timezone;
	}

	@Override
	public int atomicCode() {
		return Type.TIME_CODE;
	}

	@Override
	public Type type() {
		return Type.TIME;
	}

	public Time add(DTD dayTimeDuration) throws QueryException {
		return (Time) add(dayTimeDuration.isNegative(), dayTimeDuration,
				timezone);
	}

	public Time subtract(DTD dayTimeDuration) throws QueryException {
		return (Time) add(!dayTimeDuration.isNegative(), dayTimeDuration,
				timezone);
	}
	
	public DTD subtract(Time time) throws QueryException {
		return super.subtract(time);
	}

	@Override
	public int cmp(Atomic atomic) throws QueryException {
		if (!(atomic instanceof Time)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Cannot compare '%s with '%s'", type(), atomic.type());
		}
		return cmp((Time) atomic);
	}

	@Override
	public String stringValue() {
		String hTmp = ((hour < 10) ? "0" : "") + hour;
		String minTmp = ((minute < 10) ? "0" : "") + minute;
		String tzTmp = timezoneString();

		int seconds = micros / 1000000;
		int remainder = (micros - (seconds * 1000000));
		String sTmp = ((seconds < 10) ? "0" : "") + String.valueOf(seconds);
		if (remainder != 0) {
			int cut = 1;
			while ((remainder / 10) == 0)
				remainder /= 10; // cut trailing zeros
			sTmp += ":" + remainder;
		}
		return String.format("%s:%s:%s%s", hTmp, minTmp, sTmp, tzTmp);
	}

	public short getYear() {
		return 0;
	}

	public byte getMonth() {
		return 0;
	}

	public byte getDay() {
		return 0;
	}

	public byte getHours() {
		return hour;
	}

	public byte getMinutes() {
		return minute;
	}

	public int getMicros() {
		return micros;
	}

	public DTD getTimezone() {
		return timezone;
	}

	@Override
	protected AbstractTimeInstant create(short year, byte month, byte day,
			byte hours, byte minutes, int micros, DTD timezone) {
		return new Time(hours, minutes, micros, timezone);
	}
}
