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

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.util.Whitespace;
import org.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class Dur extends AbstractDuration {
  private final short years;

  private final byte months; // 0..11 -> year wrap on overflow, highest bit
  // used to indicate negative duration

  private final short days; // no wrap to month on overflow

  private final byte hours; // 0..23 -> day wrap on overflow

  private final byte minutes; // 0..59 -> hour wrap on overflow

  private final int micros; // 0..59,999,999 -> minute wrap on overflow

  private class DDur extends Dur {
    private final Type type;

    public DDur(boolean negative, short years, byte months, short days, byte hours, byte minutes, int micros,
        Type type) {
      super(negative, years, months, days, hours, minutes, micros);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public Dur(boolean negative, short years, byte months, short days, byte hours, byte minutes, int micros) {
    this.years = years;
    this.months = !negative ? months : (byte) (months | 0x80);
    this.days = days;
    this.hours = hours;
    this.minutes = minutes;
    this.micros = micros;
  }

  public Dur(String str) throws QueryException {
    boolean negative = false;
    short years = 0;
    byte months = 0; // 0..11 -> year wrap on overflow, highest bit used to
    // indicate negative duration
    short days = 0; // no wrap to month on overflow
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
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:duration", str);
    }

    int start = pos;
    while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
      pos++;
    int end = pos;
    int sectionTerminator = pos < length ? charArray[pos++] : -1;
    int v = start != end ? Integer.parseInt(str.substring(start, end)) : -1; // parse leading value

    if (sectionTerminator == 'Y') {
      if (v > Short.MAX_VALUE) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Cannot cast '%s' to xs:duration: component too large",
                                 str);
      }

      years = (short) v;

      start = pos;
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      sectionTerminator = pos < length ? charArray[pos++] : -1;
      v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;
    }

    if (sectionTerminator == 'M' && v > -1) {
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
      while (pos < length && '0' <= charArray[pos] && charArray[pos] <= '9')
        pos++;
      end = pos;
      sectionTerminator = pos < length ? charArray[pos++] : -1;
      v = start != end ? Integer.parseInt(str.substring(start, end)) : -1;
    }

    if (sectionTerminator == 'D' && v > -1) {
      if (v > Short.MAX_VALUE) {
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                 "Cannot cast '%s' to xs:duration: component too large",
                                 str);
      }

      days = (short) v;

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
        throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:duration", str);
      }

      if (sectionTerminator == 'H' && v > -1) {
        int newDays = days + v / 24;
        v = v % 24;

        if (newDays > Short.MAX_VALUE) {
          throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                   "Cannot cast '%s' to xs:duration: component too large",
                                   str);
        }

        days = (short) newDays;
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

        if (newDays > Short.MAX_VALUE) {
          throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                   "Cannot cast '%s' to xs:duration: component too large",
                                   str);
        }

        days = (short) newDays;
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

        if (newDays > Short.MAX_VALUE) {
          throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
                                   "Cannot cast '%s' to xs:duration: component too large",
                                   str);
        }

        days = (short) newDays;
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
            sectionTerminator = pos < length ? charArray[pos] : -1;
          } else {
            sectionTerminator = 'X';
          }
        } else {
          sectionTerminator = -1;
        }
      }
    }

    if (sectionTerminator != -1) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:duration", str);
    }

    this.years = years;
    this.months = !negative ? months : (byte) (months | 0x80);
    this.days = days;
    this.hours = hours;
    this.minutes = minutes;
    this.micros = micros;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return new DDur(months < 0, years, (byte) (months & 0x7F), days, hours, minutes, micros, type);
  }

  @Override
  public int cmp(Atomic atomic) throws QueryException {
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s with '%s'",
                             type(),
                             atomic.type());
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    AbstractDuration other = (AbstractDuration) atomic;
    if (getYears() != other.getYears()) {
      return getYears() - other.getYears();
    }
    if (getMonths() != other.getMonths()) {
      return getMonths() - other.getMonths();
    }
    if (getDays() != other.getDays()) {
      return getDays() - other.getDays();
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
    return 0;
  }

  @Override
  public int atomicCode() {
    return Type.DUR_CODE;
  }

  @Override
  protected boolean zeroMonthsWhenZero() {
    return true;
  }

  @Override
  public Type type() {
    return Type.DUR;
  }

  @Override
  public boolean isNegative() {
    return months < 0;
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
    return days;
  }

  @Override
  public byte getHours() {
    return hours;
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
