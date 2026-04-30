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

import java.math.BigDecimal;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.util.Whitespace;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class DTD extends AbstractDuration {
  private final int days; // no wrap to month on overflow; widened from short so dateTime
                         // subtractions across long timespans don't wrap silently.

  private final byte hours; // 0..23 -> day wrap on overflow

  private final byte minutes; // 0..59 -> hour wrap on overflow

  private final int micros; // 0..59,999,999 -> minute wrap on overflow

  private class DTDDur extends DTD {
    private final Type type;

    public DTDDur(boolean negative, int days, byte hours, byte minutes, int micros, Type type) {
      super(negative, days, hours, minutes, micros);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public DTD(boolean negative, int days, byte hours, byte minutes, int micros) {
    this.days = days;
    this.hours = !negative ? hours : (byte) (hours | 0x80);
    this.minutes = minutes;
    this.micros = micros;
  }

  public DTD(String str) throws QueryException {
    boolean negative = false;
    int days = 0; // no wrap to month on overflow
    byte hours = 0; // 0..23 -> day wrap on overflow
    byte minutes = 0; // 0..59 -> hour wrap on overflow
    int micros = 0; // 0..59,999,999 -> minute wrap on overflow

    str = Whitespace.collapseTrimOnly(str);
    char[] charArray = str.toCharArray();
    int pos = 0;
    int length = charArray.length;

    if (pos == length || charArray[pos] == '-') {
      negative = true;
      pos++;
    }

    if (length - pos < 3 || charArray[pos++] != 'P') {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dayTimeDuration", str);
    }

    int start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    int end = pos;
    int sectionTerminator = pos < length ? charArray[pos++] : -1;
    int v = start != end ? Integer.parseInt(str.substring(start, end)) : -1; // parse leading value

    if (sectionTerminator == 'D' && v > -1) {
      days = v;

      start = pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      sectionTerminator = pos < length ? charArray[pos++] : -1;
      v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;
    }

    if (sectionTerminator == 'T') {
      start = pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      sectionTerminator = pos < length ? charArray[pos++] : -1;
      v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;

      if (sectionTerminator == -1) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dayTimeDuration", str);
      }

      if (sectionTerminator == 'H' && v > -1) {
        int newDays = days + v / 24;
        v = v % 24;

        days = newDays;
        hours = (byte) v;

        start = pos;
        while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
          pos++;
        end = pos;
        sectionTerminator = pos < length ? charArray[pos++] : -1;
        v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;
      }

      if (sectionTerminator == 'M' && v > -1) {
        int newDays = days + v / 1440;
        v = v % 1440;
        int newHours = hours + v / 60;
        v = v % 60;

        newDays += newHours / 24;
        newHours %= 24;

        days = newDays;
        hours = (byte) newHours;
        minutes = (byte) v;

        start = pos;
        while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
          pos++;
        end = pos;
        sectionTerminator = pos < length ? charArray[pos++] : -1;
        v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;
      }

      if ((sectionTerminator == '.' || sectionTerminator == 'S') && v > -1) {
        int newDays = days + v / 86400;
        v = v % 86400;
        int newHours = hours + v / 3600;
        v = v % 3600;
        int newMinutes = minutes + v / 60;
        v = v % 60;

        newHours += newMinutes / 60;
        newMinutes %= 60;
        newDays += newHours / 24;
        newHours %= 24;

        days = newDays;
        hours = (byte) newHours;
        minutes = (byte) newMinutes;
        micros = v * 1000000;

        if (sectionTerminator == '.') {
          start = pos;
          while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
            pos++;
          end = pos;
          sectionTerminator = pos < length ? charArray[pos++] : -1;
          int l = end - start;
          v = start != end ? Integer.parseInt(str.substring(start, start + Math.min(l, 6))) : -1; // drop nano seconds

          if (sectionTerminator == 'S' && v > -1) {
            if (v > 0) {
              for (int i = 0; i < 6 - l; i++) {
                v *= 10;
              }
              micros += v;
            }
            sectionTerminator = pos < length ? charArray[pos++] : -1;
          } else {
            sectionTerminator = 'X';
          }
        } else {
          sectionTerminator = -1;
        }
      }
    }

    if (sectionTerminator != -1) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:dayTimeDuration", str);
    }

    this.days = days;
    this.hours = !negative ? hours : (byte) (hours | 0x80);
    this.minutes = minutes;
    this.micros = micros;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return type.instanceOf(type)
        ? new DTDDur(hours < 0, days, (byte) (hours & 0x7F), minutes, micros, type)
        : new Dur(hours < 0, (short) 0, (byte) 0, days, (byte) (hours & 0x7F), minutes, micros).asType(type);
  }

  @Override
  protected boolean zeroMonthsWhenZero() {
    return false;
  }

  @Override
  public int cmp(Atomic atomic) throws QueryException {
    // Note: The base type xs:duration is not ordered, i.e. it does
    // not support the <,<=,>,>= relationships but only tests for
    // equality. It must be ensured by the caller that an appropriate
    // error is raised.
    if (!(atomic instanceof DTD)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Cannot compare '%s with '%s'",
                               type(),
                               atomic.type());
    }
    return atomicCmpInternal(atomic);
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    DTD other = (DTD) atomic;
    int sign = hours & 0x80;
    int oSign = other.hours & 0x80;
    if (sign != oSign) {
      return sign < oSign ? -1 : 1;
    }
    int res = days - other.days;
    if (res != 0) {
      return res;
    }
    res = (hours & 0x7F) - (other.hours & 0x7F);
    if (res != 0) {
      return res;
    }
    res = hours - other.hours;
    if (res != 0) {
      return res;
    }
    res = minutes - other.minutes;
    if (res != 0) {
      return res;
    }
    res = micros - other.micros;
    return res;
  }

  @Override
  public int atomicCode() {
    return Type.DTD_CODE;
  }

  @Override
  public Type type() {
    return Type.DTD;
  }

  public DTD add(DTD other) throws QueryException {
    return addInternal(other.isNegative(), other.getDays(), other.getHours(), other.getMinutes(), other.getMicros());
  }

  public DTD subtract(DTD other) throws QueryException {
    return addInternal(!other.isNegative(), other.getDays(), other.getHours(), other.getMinutes(), other.getMicros());
  }

  public DTD multiply(Dbl dbl) throws QueryException {
    double v = dbl.doubleValue();

    if (Double.isNaN(v)) {
      throw new QueryException(ErrorCode.ERR_PARAMETER_NAN);
    }
    if (Double.isInfinite(v)) {
      throw new QueryException(ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
    }

    long newDays = Math.round(getDays() * v);
    long newHours = Math.round(getHours() * v);
    long newMinutes = Math.round(getMinutes() * v);
    long newMicros = Math.round(getMicros() * v);
    boolean newNegative = isNegative() ^ v < 0;

    if (isNegative() ^ newNegative) {
      newDays *= -1;
      newHours *= -1;
      newMinutes *= -1;
      newMicros *= -1;
    }

    newMinutes += newMicros / 60000000;
    newMicros %= 60000000;
    newHours += newMinutes / 60;
    newMinutes %= 60;
    newDays += newHours / 24;
    newHours %= 24;

    if (newDays > Integer.MAX_VALUE) {
      throw new QueryException(ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
    }

    return new DTD(newNegative, (int) newDays, (byte) newHours, (byte) newMinutes, (int) newMicros);
  }

  public DTD divide(Dbl dbl) throws QueryException {
    double v = dbl.doubleValue();

    if (Double.isNaN(v)) {
      throw new QueryException(ErrorCode.ERR_PARAMETER_NAN);
    }
    if (Double.isInfinite(v)) {
      return new DTD(false, 0, (byte) 0, (byte) 0, 0);
    }

    long newDays = Math.round(getDays() / v);
    long newHours = Math.round(getHours() / v);
    long newMinutes = Math.round(getMinutes() / v);
    long newMicros = Math.round(getMicros() / v);
    boolean newNegative = isNegative() ^ v < 0;

    if (isNegative() ^ newNegative) {
      newDays *= -1;
      newHours *= -1;
      newMinutes *= -1;
      newMicros *= -1;
    }

    newMinutes += newMicros / 60000000;
    newMicros %= 60000000;
    newHours += newMinutes / 60;
    newMinutes %= 60;
    newDays += newHours / 24;
    newHours %= 24;

    if (newDays > Integer.MAX_VALUE) {
      throw new QueryException(ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
    }

    return new DTD(newNegative, (int) newDays, (byte) newHours, (byte) newMinutes, (int) newMicros);
  }

  public Numeric divide(DTD dur) throws QueryException {
    long a = ((getDays() * 24l + getHours()) * 60l + getMinutes()) * 60l * 1000000 + getMicros();
    long b = ((dur.getDays() * 24l + dur.getHours()) * 60l + dur.getMinutes()) * 60l * 1000000 + dur.getMicros();

    if (b == 0) {
      throw new QueryException(ErrorCode.ERR_DIVISION_BY_ZERO);
    }

    return new Dec(new BigDecimal(a)).div(new Dec(new BigDecimal(b)));
  }

  private DTD addInternal(boolean n2, int d2, byte h2, byte m2, int mic2) throws QueryException {
    // Convert each operand to a signed total-micros count, add, then renormalize. The
    // previous field-by-field implementation determined the result's sign from the
    // sign of newDays alone — incorrectly when the days components cancelled but the
    // sub-day components didn't (e.g. PT1H - PT2H = -PT1H), and it relied on `*= -1`
    // on a `byte` which truncated negative-hour values into the DTD's high-bit-encoded
    // sign field, producing `-PT127H`. This rewrite avoids both pitfalls — total-micros
    // arithmetic is unconditionally correct for any input within long range.
    final long total1 = totalMicros(isNegative(), getDays(), getHours(), getMinutes(), getMicros());
    final long total2 = totalMicros(n2, d2, h2, m2, mic2);
    long sum = total1 + total2;

    final boolean newNegative = sum < 0;
    if (newNegative) {
      sum = -sum;
    }

    final long newMicros = sum % MICROS_PER_MINUTE;
    final long newMinutes = (sum / MICROS_PER_MINUTE) % 60L;
    final long newHours = (sum / MICROS_PER_HOUR) % 24L;
    final long newDays = sum / MICROS_PER_DAY;

    if (newDays > Integer.MAX_VALUE) {
      throw new QueryException(ErrorCode.ERR_OVERFLOW_UNDERFLOW_IN_DURATION);
    }

    return new DTD(newNegative, (int) newDays, (byte) newHours, (byte) newMinutes, (int) newMicros);
  }

  /** Convert a (negative, days, hours, minutes, micros) tuple into total signed micros. */
  private static long totalMicros(final boolean negative, final int days, final byte hours, final byte minutes,
      final int micros) {
    final long total = (long) days * MICROS_PER_DAY + (long) hours * MICROS_PER_HOUR + (long) minutes
        * MICROS_PER_MINUTE + micros;
    return negative ? -total : total;
  }

  private static final long MICROS_PER_MINUTE = 60_000_000L;
  private static final long MICROS_PER_HOUR = MICROS_PER_MINUTE * 60L;
  private static final long MICROS_PER_DAY = MICROS_PER_HOUR * 24L;

  @Override
  public boolean isNegative() {
    return hours < 0;
  }

  @Override
  public byte getMonths() {
    return 0;
  }

  @Override
  public short getYears() {
    return 0;
  }

  @Override
  public int getDays() {
    return days;
  }

  @Override
  public byte getHours() {
    return (byte) (hours & 0x7F);
  }

  @Override
  public byte getMinutes() {
    return minutes;
  }

  @Override
  public int getMicros() {
    return micros;
  }
}
