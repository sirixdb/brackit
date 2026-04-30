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
package io.brackit.query.atomic;

import java.util.TimeZone;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractTimeInstant extends AbstractAtomic implements TimeInstant {
  public static final DTD UTC_TIMEZONE = new DTD(false, (byte) 0, (byte) 0, (byte) 0, (byte) 0);

  public static final DTD MIN_TIMEZONE = new DTD(true, (byte) 14, (byte) 0, (byte) 0, (byte) 0);

  public static final DTD MAX_TIMEZONE = new DTD(false, (byte) 14, (byte) 0, (byte) 0, (byte) 0);

  public static DTD LOCAL_TIMEZONE;

  static {
    int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
    int hours = fQuotient(offset, 3600000);
    int remainder = modulo(offset, 3600000);
    int minutes = fQuotient(remainder, 60000);
    remainder = modulo(remainder, 60000);
    int micros = remainder * 1000;
    LOCAL_TIMEZONE = new DTD(offset < 0, 0, (byte) hours, (byte) minutes, micros);
  }

  @Override
  public int hashCode() {
    throw new RuntimeException("Not implemented yet");
  }

  protected DTD parseTimezone(String str, char[] charArray, int pos, int length) throws QueryException {
    boolean negative = false;
    byte hour = 0;
    byte minute = 0;

    if (charArray[pos] == 'Z') {
      // UTC
      pos++;
    } else if (charArray[pos] == '+' || charArray[pos] == '-') {
      negative = charArray[pos++] == '-';

      // parse hour
      int start = pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      int end = pos;
      int v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
      if (v < 0 || v > 24) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Cannot cast '%s' to xs:dateTime: illegal hour",
                                 str);
      }
      hour = (byte) v;

      // consume ':'
      if (pos >= length || charArray[pos++] != ':') {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Illegal hour in timezone: %s", str);
      }

      // parse minute
      start = pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      v = end - start == 2 ? Integer.parseInt(str.substring(start, end)) : -1;
      if (v < 0 || v > 59) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Illegal minute in timezone: %s", str);
      }
      minute = (byte) v;
    } else {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Illegal timezone: %s", str);
    }

    if (pos != length) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Illegal timezone: %s", str);
    }

    if (hour == 0 && minute == 0) {
      return UTC_TIMEZONE;
    }

    return new DTD(negative, 0, hour, minute, 0);
  }

  protected int cmp(AbstractTimeInstant other) {
    boolean aHasTZ = false;
    boolean bHasTZ = false;
    AbstractTimeInstant a = this;
    AbstractTimeInstant b = other;

    if (a.getTimezone() != null && (a.getTimezone().getHours() != 0 || a.getTimezone().getMinutes() != 0)) {
      a = new DateTime(a.getYear(),
                       a.getMonth(),
                       a.getDay(),
                       a.getHours(),
                       a.getMinutes(),
                       a.getMicros(),
                       a.getTimezone()).canonicalize();
      aHasTZ = true;
    }
    if (b.getTimezone() != null && (b.getTimezone().getHours() != 0 || b.getTimezone().getMinutes() != 0)) {
      b = new DateTime(b.getYear(),
                       b.getMonth(),
                       b.getDay(),
                       b.getHours(),
                       b.getMinutes(),
                       b.getMicros(),
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

  protected AbstractTimeInstant add(boolean negate, Duration duration, DTD newTimezone) {
    short durationYears = duration.getYears();
    byte durationMonths = duration.getMonths();
    int durationDays = duration.getDays();
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
    int newDays = (getDay() > maxDayInMonth ? maxDayInMonth : getDay() < 1 ? 1 : getDay()) + durationDays + carry;

    // Day-of-month is 1..maxDayInMonth (NOT 0..maxDayInMonth-1). Two pre-fix bugs:
    //
    //  * `newDays < 0` let newDays == 0 escape the loop, producing invalid 0th-of-month
    //    dates (e.g. `2026-05-01 - PT2H` came out as `2026-05-00T23:00:00`).
    //  * `maxDayInMonth(newYear, newMonth - 1)` computed the previous month's day count
    //    before adjusting for January, where newMonth-1 == 0 — undefined month index.
    //    The newYear/newMonth update happened AFTER the lookup, so a borrow at January
    //    of any year sampled month 0 of the same year.
    //
    // Compute the previous/next (year, month) FIRST, then look up its day count.
    while (true) {
      if (newDays < 1) {
        int prevMonth = newMonth - 1;
        int prevYear = newYear;
        if (prevMonth < 1) {
          prevMonth = 12;
          prevYear -= 1;
        }
        newDays += maxDayInMonth(prevYear, prevMonth);
        newMonth = prevMonth;
        newYear = prevYear;
      } else if (newDays > maxDayInMonth(newYear, newMonth)) {
        newDays -= maxDayInMonth(newYear, newMonth);
        int nextMonth = newMonth + 1;
        int nextYear = newYear;
        if (nextMonth > 12) {
          nextMonth = 1;
          nextYear += 1;
        }
        newMonth = nextMonth;
        newYear = nextYear;
      } else {
        break;
      }
    }

    return create((short) newYear,
                  (byte) newMonth,
                  (byte) newDays,
                  (byte) newHours,
                  (byte) newMinutes,
                  newMicros,
                  newTimezone);
  }

  protected DTD subtract(AbstractTimeInstant b) {
    AbstractTimeInstant a = this;

    if (a.getTimezone() != null) {
      a = a.canonicalize();
    }
    if (b.getTimezone() != null) {
      b = b.canonicalize();
    }

    // Ensure a >= b so the rest of the routine computes |a - b|; remember the sign.
    boolean negative = false;
    if (a.cmp(b) < 0) {
      AbstractTimeInstant tmp = a;
      a = b;
      b = tmp;
      negative = true;
    }

    // Date subtraction via Julian Day Numbers — eliminates the field-by-field borrow logic
    // (and its bugs) for the y/m/d component. Time-of-day is handled with a simple borrow.
    long dayDiff = julianDayNumber(a.getYear(), a.getMonth(), a.getDay()) - julianDayNumber(b.getYear(),
                                                                                            b.getMonth(),
                                                                                            b.getDay());

    int micros = a.getMicros() - b.getMicros();
    int minutes = a.getMinutes() - b.getMinutes();
    int hours = a.getHours() - b.getHours();

    if (micros < 0) {
      micros += 60_000_000;
      minutes -= 1;
    }
    if (minutes < 0) {
      minutes += 60;
      hours -= 1;
    }
    if (hours < 0) {
      hours += 24;
      dayDiff -= 1;
    }
    // dayDiff cannot go negative here because a >= b after the swap.

    // DTD's days field is now int (widened from short); Integer.MAX_VALUE days is
    // ~5.8 million years, more than enough for any realistic dateTime subtraction. We
    // still defend against overflow on the long → int narrowing.
    if (dayDiff > Integer.MAX_VALUE) {
      throw new QueryException(ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION,
                               "xs:dateTime subtraction yields a duration of %d days, exceeding Integer.MAX_VALUE",
                               dayDiff);
    }

    return new DTD(negative, (int) dayDiff, (byte) hours, (byte) minutes, micros);
  }

  /**
   * Convert a proleptic Gregorian (year, month, day) to a Julian Day Number.
   * Uses the Fliegel–Van Flandern algorithm. Valid for any date in the proleptic
   * Gregorian calendar.
   */
  private static long julianDayNumber(int year, int month, int day) {
    int a = (14 - month) / 12;
    long y = (long) year + 4800L - a;
    int m = month + 12 * a - 3;
    return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
  }

  private static int fQuotient(int a, int low, int high) {
    return fQuotient(a - low, high - low);
  }

  private static int modulo(int a, int b) {
    return a - fQuotient(a, b) * b;
  }

  private static int fQuotient(int a, int b) {
    return a >= 0 ? a / b : a / b * b == a ? a / b : a / b - 1;
  }

  private static int modulo(int a, int low, int high) {
    return modulo(a - low, high - low) + low;
  }

  @Override
  public final int atomicCmpInternal(Atomic atomic) {
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
    return tz == null ? otz == null ? 0 : -1 : otz == null ? 1 : tz.atomicCmpInternal(otz);
  }

  protected abstract AbstractTimeInstant create(short year, byte month, byte day, byte hours, byte minutes, int micros,
      DTD timezone);

  protected byte maxDayInMonth(int year, int month) {
    int m = month % 13;
    int y = year + month / 13;

    if (m == 2) {
      return y % 400 == 0 || y % 100 != 0 && y % 4 == 0 ? (byte) 29 : (byte) 28;
    } else if (m == 4 || m == 6 || m == 9 || m == 11) {
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
      String tzHTmp = (tzHours < 10 ? "0" : "") + tzHours;
      byte tzMinutes = timezone.getMinutes();
      String tzMinTmp = (tzMinutes < 10 ? "0" : "") + tzMinutes;
      tzTmp = tzHours == 0 && tzMinutes == 0 ? "Z" : (timezone.isNegative() ? "-" : "+") + tzHTmp + ":" + tzMinTmp;
    }
    return tzTmp;
  }

  @Override
  public AbstractTimeInstant canonicalize() {
    DTD timezone = getTimezone();
    if (timezone == null || timezone.getDays() == 0 && timezone.getHours() == 0) {
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
                             "Effective boolean value of '%s' is undefined.",
                             type());
  }
}
