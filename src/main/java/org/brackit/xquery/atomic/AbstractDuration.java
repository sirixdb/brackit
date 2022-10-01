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

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractDuration extends AbstractAtomic implements Duration {
  @Override
  public int hashCode() {
    throw new RuntimeException("Not implemented yet");
  }

  @Override
  public int cmp(Atomic atomic) throws QueryException {
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s with '%s'",
                             type(),
                             atomic.type());
  }

  @Override
  public final boolean eq(Atomic atomic) throws QueryException {
    if (atomic instanceof Duration) {
      Duration other = (Duration) atomic;
      return isNegative() == other.isNegative() && getYears() == other.getYears() && getMonths()
          == other.getMonths() && getDays() == other.getDays() && getHours() == other.getHours() && getMinutes()
          == other.getMinutes() && getMicros() == other.getMicros();
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s with '%s'",
                             type(),
                             atomic.type());
  }

  protected abstract boolean zeroMonthsWhenZero();

  @Override
  public final String stringValue() {
    boolean fieldSet = false;
    boolean timeSet = false;
    StringBuilder out = !isNegative() ? new StringBuilder("P") : new StringBuilder("-P");
    short years = getYears();
    byte months = getMonths();
    short days = getDays();
    byte hours = getHours();
    byte minutes = getMinutes();
    int micros = getMicros();

    if (years != 0) {
      out.append(years);
      out.append('Y');
      fieldSet = true;
    }
    if (months != 0) {
      out.append(months);
      out.append('M');
      fieldSet = true;
    }
    if (days != 0) {
      out.append(days);
      out.append('D');
      fieldSet = true;
    }
    if (hours != 0) {
      if (!timeSet) {
        out.append('T');
        timeSet = true;
      }
      out.append(hours);
      out.append('H');
      fieldSet = true;
    }
    if (minutes != 0) {
      if (!timeSet) {
        out.append('T');
        timeSet = true;
      }
      out.append(minutes);
      out.append('M');
      fieldSet = true;
    }
    if (micros != 0) {
      if (!timeSet) {
        out.append('T');
        timeSet = true;
      }
      int seconds = micros / 1000000;
      out.append(seconds);
      int remainder = micros - seconds * 1000000;
      if (remainder != 0) {
        out.append('.');
        out.append(remainder);
      }
      out.append('S');
      fieldSet = true;
    }
    if (!fieldSet) {
      out.append(zeroMonthsWhenZero() ? "0M" : "T0S");
    }

    return out.toString();
  }

  @Override
  public final boolean booleanValue() throws QueryException {
    throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Effective boolean value of duration is undefined.");
  }
}
