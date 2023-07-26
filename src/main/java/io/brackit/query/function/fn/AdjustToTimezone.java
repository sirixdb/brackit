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
package io.brackit.query.function.fn;

import io.brackit.query.atomic.DateTime;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.module.StaticContext;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.DTD;
import io.brackit.query.atomic.Date;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Time;
import io.brackit.query.function.AbstractFunction;

/**
 * Implements timezone adjustment functions
 * fn:adjust-dateTime-to-timezone($arg1), fn:adjust-dateTime-to-timezone($arg1,
 * $arg2), fn:adjust-date-to-timezone($arg1), fn:adjust-date-to-timezone($arg1,
 * $arg2), fn:adjust-time-to-timezone($arg1), and
 * fn:adjust-time-to-timezone($arg1, $arg2), as per
 * http://www.w3.org/TR/xpath-functions/#func-adjust-dateTime-to-timezone ff.
 *
 * @author Max Bechtold
 */
public class AdjustToTimezone extends AbstractFunction {
  public static enum Source {
    DATE_TIME, DATE, TIME
  }

  ;

  private Source source;

  public AdjustToTimezone(QNm name, Source source, Signature signature) {
    super(name, signature, true);
    this.source = source;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args.length == 0 || args[0] == null) {
      return null;
    }

    DTD timezone;
    if (args.length > 1) {
      timezone = (DTD) args[1];
      if (timezone != null && (timezone.getMinutes() != 0 || timezone.getDays() != 0 || timezone.getHours() > 14)) {
        throw new QueryException(ErrorCode.ERR_INVALID_TIMEZONE, "Invalid value for timezone.");
      }
    } else {
      timezone = ctx.getImplicitTimezone();
    }

    switch (source) {
      case DATE_TIME:
        io.brackit.query.atomic.DateTime dt = (io.brackit.query.atomic.DateTime) args[0];
        io.brackit.query.atomic.DateTime dtNew;

        if (dt.getTimezone() == null && timezone == null) {
          dtNew = dt;
        } else if (dt.getTimezone() == null) {
          dtNew = new io.brackit.query.atomic.DateTime(dt.getYear(),
                                                       dt.getMonth(),
                                                       dt.getDay(),
                                                       dt.getHours(),
                                                       dt.getMinutes(),
                                                       dt.getMicros(),
                                                       timezone);
        } else if (timezone == null) {
          dtNew = new io.brackit.query.atomic.DateTime(dt.getYear(),
                                                       dt.getMonth(),
                                                       dt.getDay(),
                                                       dt.getHours(),
                                                       dt.getMinutes(),
                                                       dt.getMicros(),
                                                       null);
        } else {
          byte old = dt.getTimezone().getHours();
          if (dt.getTimezone().isNegative()) {
            old *= -1;
          }

          byte nw = timezone.getHours();
          if (timezone.isNegative()) {
            nw *= -1;
          }

          int diff = nw - old;
          if (diff == 0) {
            return dt;
          }

          dtNew = dt.add(new DTD(diff < 0, (short) 0, (byte) Math.abs(diff), (byte) 0, 0));
          dtNew = new DateTime(dtNew.getYear(),
                               dtNew.getMonth(),
                               dtNew.getDay(),
                               dtNew.getHours(),
                               dtNew.getMinutes(),
                               dtNew.getMicros(),
                               timezone);
        }

        return dtNew;

      case DATE:
        Date date = (Date) args[0];
        Date dateNew;

        if (date.getTimezone() == null && timezone == null) {
          dateNew = date;
        } else if (date.getTimezone() == null) {
          dateNew = new Date(date.getYear(), date.getMonth(), date.getDay(), timezone);
        } else if (timezone == null) {
          dateNew = new Date(date.getYear(), date.getMonth(), date.getDay(), null);
        } else {
          byte old = date.getTimezone().getHours();
          if (date.getTimezone().isNegative()) {
            old *= -1;
          }

          byte nw = timezone.getHours();
          if (timezone.isNegative()) {
            nw *= -1;
          }

          int diff = nw - old;
          if (diff == 0) {
            return date;
          }

          dateNew = date.add(new DTD(diff < 0, (short) 0, (byte) Math.abs(diff), (byte) 0, 0));
          dateNew = new Date(dateNew.getYear(), dateNew.getMonth(), dateNew.getDay(), timezone);
        }

        return dateNew;

      case TIME:
        Time time = (Time) args[0];
        Time timeNew;

        if (time.getTimezone() == null && timezone == null) {
          timeNew = time;
        } else if (time.getTimezone() == null) {
          timeNew = new Time(time.getHours(), time.getMinutes(), time.getMicros(), timezone);
        } else if (timezone == null) {
          timeNew = new Time(time.getHours(), time.getMinutes(), time.getMicros(), null);
        } else {
          byte old = time.getTimezone().getHours();
          if (time.getTimezone().isNegative()) {
            old *= -1;
          }

          byte nw = timezone.getHours();
          if (timezone.isNegative()) {
            nw *= -1;
          }

          int diff = nw - old;
          if (diff == 0) {
            return time;
          }

          timeNew = time.add(new DTD(diff < 0, (short) 0, (byte) Math.abs(diff), (byte) 0, 0));
          timeNew = new Time(timeNew.getHours(), timeNew.getMinutes(), timeNew.getMicros(), timezone);
        }

        return timeNew;
    }

    return null;
  }

}
