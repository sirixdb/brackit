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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class GMon extends AbstractTimeInstant {
	private final byte month;

	private final DTD timezone;

	public GMon(byte month, DTD timezone) {
		this.month = month;
		this.timezone = timezone;
	}

	public GMon(String str) throws QueryException {
		byte month = 0;
		DTD timezone = null;

		str = Whitespace.collapseTrimOnly(str);
		char[] charArray = str.toCharArray();
		int pos = 0;
		int length = charArray.length;

		// consume '--'
		if ((pos + 1 >= length) || (charArray[pos++] != '-')
				|| (charArray[pos++] != '-')) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:gMonth", str);
		}

		// parse month
		int start = pos;
		while ((pos < length) && ('0' <= charArray[pos])
				&& (charArray[pos] <= '9'))
			pos++;
		int end = pos;
		int v = (end - start == 2) ? Integer
				.parseInt(str.substring(start, end)) : -1;
		if ((v < 1) || (v > 12)) {
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast '%s' to xs:gMonth: illegal month", str);
		}
		month = (byte) v;

		if (pos < length) {
			timezone = parseTimezone(str, charArray, pos, length);
		}

		this.month = month;
		this.timezone = timezone;
	}

	@Override
	public int atomicCode() {
		return Type.GMON_CODE;
	}

	@Override
	protected AbstractTimeInstant create(short year, byte month, byte day,
			byte hours, byte minutes, int micros, DTD timezone) {
		return new GMon(month, timezone);
	}

	@Override
	public byte getDay() {
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
	public byte getMonth() {
		return month;
	}

	@Override
	public int getMicros() {
		return 0;
	}

	@Override
	public DTD getTimezone() {
		return timezone;
	}

	@Override
	public short getYear() {
		return 0;
	}

	@Override
	public int cmp(Atomic atomic) throws QueryException {
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s with '%s'", type(), atomic.type());
	}

	@Override
	public boolean eq(Atomic atomic) throws QueryException {
		if (!(atomic instanceof GMon)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Cannot compare '%s with '%s'", type(), atomic.type());
		}
		return (cmp((GMon) atomic) == 0);
	}

	@Override
	public String stringValue() {
		String mTmp = ((month < 10) ? "0" : "") + month;
		String tzTmp = timezoneString();

		return String.format("--%s%s", mTmp, tzTmp);
	}

	@Override
	public Type type() {
		return Type.GMON;
	}
}
