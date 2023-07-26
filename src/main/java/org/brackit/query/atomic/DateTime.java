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
package org.brackit.query.atomic;

import java.util.Calendar;
import java.util.TimeZone;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.util.Whitespace;
import org.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class DateTime extends AbstractTimeInstant {
  private final short year;
  private final byte month;
  private final byte day;
  private final byte hour;
  private final byte minute;
  private final int micros;
  private final DTD timezone;

  public DateTime(short year, byte month, byte day, byte hour, byte minute, int micros, DTD timezone) {
    this.year = year;
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minute = minute;
    this.micros = micros;
    this.timezone = timezone;
  }

  public DateTime(String str) throws QueryException {
    short year;
    byte month;
    byte day;
    byte hour;
    byte minute;
    int micros;
    DTD timezone = null;

    str = Whitespace.collapseTrimOnly(str);
    char[] charArray = str.toCharArray();
    int pos = 0;
    int length = charArray.length;

    // parse variable length year
    boolean negative = false;
    if (pos == length || charArray[pos] == '-') {
      negative = true;
      pos++;
    }

    int start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    int end = pos;

    if (end - start < 4) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    } else if (end - start > 4 && negative) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }
    int v = start != end ? Integer.parseInt(str.substring(start, end)) : -1; // parse leading value

    if (v > Short.MAX_VALUE || v == 0) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }
    year = negative ? (short) -v : (short) v;

    // consume '-'
    if (pos >= length || charArray[pos++] != '-') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }

    // parse month
    start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    end = pos;
    v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
    if (v < 1 || v > 12) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: illegal month",
                               str);
    }
    month = (byte) v;

    // consume '-'
    if (pos >= length || charArray[pos++] != '-') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }

    // parse day
    start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    end = pos;
    v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
    if (v < 1 || v > 31) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: illegal day",
                               str);
    }
    day = (byte) v;

    // consume 'T'
    if (pos >= length || charArray[pos++] != 'T') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }

    // parse hour
    start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    end = pos;
    v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
    if (v < 0 || v > 24) // attention 24 is only allowed if the minutes
    // are zero!
    {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: illegal hour",
                               str);
    }
    hour = (byte) v;

    // consume ':'
    if (pos >= length || charArray[pos++] != ':') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }

    // parse hour
    start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    end = pos;
    v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
    if (v < 0 || v > 59) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: illegal minute",
                               str);
    }
    minute = (byte) v;

    // consume ':'
    if (pos >= length || charArray[pos++] != ':') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dateTime", str);
    }

    start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    end = pos;
    v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
    if (v < 0 || v > 59) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: illegal seconds",
                               str);
    }
    micros = v * 1000000;

    if (pos < length && charArray[pos] == '.') {
      start = ++pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      int l = end - start;
      v = start != end ? Integer.parseInt(str.substring(start, start + Math.min(l, 6))) : -1; // drop nano seconds

      if (v == -1) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Cannot cast '%s' to xs:dateTime: illegal seconds",
                                 str);
      }
      if (v > 0) {
        for (int i = 0; i < 6 - l; i++) {
          v *= 10;
        }
        micros += v;
      }
    }

    // fix it up first before checking for optional timezone
    byte maxDaysInMonth = maxDayInMonth(year, month);

    if (day > maxDaysInMonth) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                               "Cannot cast '%s' to xs:dateTime: Day %s does not exist in month %s of year %s",
                               day,
                               month,
                               year,
                               str);
    }

    if (hour == 24) {
      if (minute != 0) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Cannot cast '%s' to xs:dateTime: 24 hours is not allowed when minutes is non-zero",
                                 str);
      }

      if (day < maxDaysInMonth) {
        day++;
        hour = 0;
      } else {
        month++;
        day = 1;
        hour = 0;

        if (month == 13) {
          year++;
          month = 1;
        }
      }
    }

    if (pos < length) {
      timezone = parseTimezone(str, charArray, pos, length);
    }

    this.year = year;
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minute = minute;
    this.micros = micros;
    this.timezone = timezone;
  }

  public DateTime(DTD timezone) {
    int utcDiff = timezone.getHours() * 60 + (timezone.isNegative() ? -1 : 1) * timezone.getMinutes();
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.add(Calendar.MINUTE, utcDiff);
    this.year = (short) cal.get(Calendar.YEAR);
    this.month = (byte) (cal.get(Calendar.MONTH) + 1);
    this.day = (byte) cal.get(Calendar.DAY_OF_MONTH);
    this.hour = (byte) cal.get(Calendar.HOUR_OF_DAY);
    this.minute = (byte) cal.get(Calendar.MINUTE);
    this.micros = (cal.get(Calendar.SECOND) * 1000 + cal.get(Calendar.MILLISECOND)) * 1000;
    this.timezone = timezone;
  }

  @Override
  public Type type() {
    return Type.DATI;
  }

  public DateTime add(DTD dayTimeDuration) throws QueryException {
    return (DateTime) add(dayTimeDuration.isNegative(), dayTimeDuration, timezone);
  }

  public DateTime add(YMD yearMonthDuration) throws QueryException {
    return (DateTime) add(yearMonthDuration.isNegative(), yearMonthDuration, timezone);
  }

  public DateTime subtract(DTD dayTimeDuration) throws QueryException {
    return (DateTime) add(!dayTimeDuration.isNegative(), dayTimeDuration, timezone);
  }

  public DateTime subtract(YMD yearMonthDuration) throws QueryException {
    return (DateTime) add(!yearMonthDuration.isNegative(), yearMonthDuration, timezone);
  }

  public DTD subtract(DateTime dateTime) throws QueryException {
    return super.subtract(dateTime);
  }

  @Override
  public int cmp(Atomic atomic) throws QueryException {
    if (!(atomic instanceof DateTime)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Cannot compare '%s with '%s'",
                               type(),
                               atomic.type());
    }
    return cmp((DateTime) atomic);
  }

  @Override
  public int atomicCode() {
    return Type.DATI_CODE;
  }

  @Override
  public String stringValue() {
    String yTmp = year < 0 ? "-" : "" + (year < 10 ? "000" : year < 100 ? "00" : year < 1000 ? "0" : "") + year;
    String mTmp = (month < 10 ? "0" : "") + month;
    String dTmp = (day < 10 ? "0" : "") + day;
    String hTmp = (hour < 10 ? "0" : "") + hour;
    String minTmp = (minute < 10 ? "0" : "") + minute;
    String tzTmp = timezoneString();

    int seconds = micros / 1000000;
    int remainder = micros - seconds * 1000000;
    String sTmp = (seconds < 10 ? "0" : "") + String.valueOf(seconds);
    if (remainder != 0) {
      while (remainder / 10 == 0)
        remainder /= 10; // cut trailing zeros
      sTmp += ":" + remainder;
    }
    return String.format("%s-%s-%sT%s:%s:%s%s", yTmp, mTmp, dTmp, hTmp, minTmp, sTmp, tzTmp);
  }

  @Override
  public short getYear() {
    return year;
  }

  @Override
  public byte getMonth() {
    return month;
  }

  @Override
  public byte getDay() {
    return day;
  }

  @Override
  public byte getHours() {
    return hour;
  }

  @Override
  public byte getMinutes() {
    return minute;
  }

  @Override
  public int getMicros() {
    return micros;
  }

  @Override
  public DTD getTimezone() {
    return timezone;
  }

  @Override
  protected AbstractTimeInstant create(short year, byte month, byte day, byte hours, byte minutes, int micros,
      DTD timezone) {
    return new DateTime(year, month, day, hours, minutes, micros, timezone);
  }
}
