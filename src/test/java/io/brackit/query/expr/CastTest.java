/*
 * [New BSD License]
 *
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
package io.brackit.query.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.Query;
import io.brackit.query.atomic.AbstractTimeInstant;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.DTD;
import io.brackit.query.atomic.Date;
import io.brackit.query.atomic.DateTime;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Dur;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.YMD;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class CastTest extends XQueryBaseTest {
  @Test
  public void durationFromString() {
    Sequence res = new Query("xs:duration('P1Y20MT20S')").execute(ctx);
    ResultChecker.dCheck(new Dur(false, (short) 2, (byte) 8, (short) 0, (byte) 0, (byte) 0, 20000000), res);
  }

  @Test
  public void durationFromString2() {
    Sequence res = new Query("xs:duration('P1Y20M16DT23H1502M')").execute(ctx);
    ResultChecker.dCheck(new Dur(false, (short) 2, (byte) 8, (short) 18, (byte) 0, (byte) 2, 0), res);
  }

  @Test
  public void durationFromString3() {
    Sequence res = new Query("xs:duration('PT1M123.123456789S')").execute(ctx);
    ResultChecker.dCheck(new Dur(false, (short) 0, (byte) 0, (short) 0, (byte) 0, (byte) 3, 3123456), res);
  }

  @Test
  public void durationFromString4() {
    Sequence res = new Query("xs:duration('PT1M123.2S')").execute(ctx);
    ResultChecker.dCheck(new Dur(false, (short) 0, (byte) 0, (short) 0, (byte) 0, (byte) 3, 3200000), res);
  }

  @Test
  public void durationFromString5() {
    try {
      new Query("xs:duration('P1YM')").execute(ctx);
      fail("Invalid duration parsed");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
    }
  }

  @Test
  public void castDurationToYearMonthDuration() {
    Sequence res = new Query("xs:duration('P1Y20M16DT23H1502M') cast as xs:yearMonthDuration").execute(ctx);
    ResultChecker.dCheck(new YMD(false, (short) 2, (byte) 8), res);
  }

  @Test
  public void castDurationToDayTimeDuration() {
    Sequence res = new Query("xs:duration('P6Y9M21DT16H1M3.123456789S') cast as xs:dayTimeDuration").execute(ctx);
    ResultChecker.dCheck(new DTD(false, (short) 21, (byte) 16, (byte) 1, 3123456), res);
  }

  @Test
  public void divideDayTimeDuration() {
    Sequence res = new Query("xs:dayTimeDuration('-P12DT10H') div -0.5").execute(ctx);
    ResultChecker.dCheck(new DTD(false, (short) 24, (byte) 20, (byte) 0, 0), res);
  }

  @Test
  public void dateTimeAddYearMonthDuration() {
    Sequence res = new Query("xs:dateTime('1981-12-31T12:05:35.1234567') + xs:yearMonthDuration('P1Y2M')").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 1983, (byte) 2, (byte) 28, (byte) 12, (byte) 5, 35123456, null), res);
  }

  @Test
  public void dateTimeAddYearMonthDuration2() {
    Sequence res = new Query("xs:dateTime('1981-12-12T12:05:35.1234567') + xs:yearMonthDuration('-P1Y1M')").execute(
                                                                                                                    ctx);
    ResultChecker.dCheck(new DateTime((short) 1980, (byte) 11, (byte) 12, (byte) 12, (byte) 5, 35123456, null), res);
  }

  @Test
  public void dateTimeAddDayTImeDuration() {
    Sequence res = new Query("xs:dateTime('1981-12-12T12:05:35.1234567') + xs:dayTimeDuration('P19DT14H40.654321S')")
                                                                                                                     .execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 1982, (byte) 1, (byte) 1, (byte) 2, (byte) 6, 15777777, null), res);
  }

  @Test
  public void castDateTimeFromString() {
    Sequence res = new Query("'1981-11-11T12:05:35.1234567+07:00' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 1981,
                                      (byte) 11,
                                      (byte) 11,
                                      (byte) 12,
                                      (byte) 5,
                                      35123456,
                                      new DTD(false, (short) 0, (byte) 7, (byte) 0, 0)), res);
  }

  @Test
  public void castDateTimeFromString2() {
    Sequence res = new Query("'19811-11-11T12:05:35' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 19811, (byte) 11, (byte) 11, (byte) 12, (byte) 5, 35000000, null), res);
  }

  @Test
  public void castDateTimeFromString3() {
    Sequence res = new Query("'0333-03-01T09:05:35' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 333, (byte) 3, (byte) 1, (byte) 9, (byte) 5, 35000000, null), res);
  }

  @Test
  public void castDateTimeFromString4() {
    Sequence res = new Query("'-0005-03-01T09:05:35Z' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) -5,
                                      (byte) 3,
                                      (byte) 1,
                                      (byte) 9,
                                      (byte) 5,
                                      35000000,
                                      AbstractTimeInstant.UTC_TIMEZONE), res);
  }

  @Test
  public void castDateTimeFromString5() {
    try {
      new Query("'0000-03-01T09:05:35Z' cast as xs:dateTime").execute(ctx);
      fail("Invalid dateTime parsed");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
    }
  }

  @Test
  public void castDateTimeFromString6() {
    Sequence res = new Query("'2010-03-01T24:00:00Z' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 2010,
                                      (byte) 3,
                                      (byte) 2,
                                      (byte) 0,
                                      (byte) 0,
                                      0,
                                      AbstractTimeInstant.UTC_TIMEZONE), res);
  }

  @Test
  public void castDateTimeFromString7() {
    Sequence res = new Query("'2000-02-28T24:00:00Z' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 2000,
                                      (byte) 2,
                                      (byte) 29,
                                      (byte) 0,
                                      (byte) 0,
                                      0,
                                      AbstractTimeInstant.UTC_TIMEZONE), res);
  }

  @Test
  public void castDateTimeFromString8() {
    Sequence res = new Query("'2001-02-28T24:00:00Z' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 2001,
                                      (byte) 3,
                                      (byte) 1,
                                      (byte) 0,
                                      (byte) 0,
                                      0,
                                      AbstractTimeInstant.UTC_TIMEZONE), res);
  }

  @Test
  public void castDateTimeFromString9() {
    try {
      new Query("'0000-03-01T24:01:00Z' cast as xs:dateTime").execute(ctx);
      fail("Invalid dateTime parsed");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
    }
  }

  @Test
  public void castDateTimeFromString10() {
    Sequence res = new Query("'19811-11-11T12:05:35.1000' cast as xs:dateTime").execute(ctx);
    ResultChecker.dCheck(new DateTime((short) 19811, (byte) 11, (byte) 11, (byte) 12, (byte) 5, 35100000, null), res);
  }

  @Test
  public void castDateFromString() {
    Sequence res = new Query("'2002-10-09-11:00' cast as xs:date").execute(ctx);
    ResultChecker.dCheck(new Date((short) 2002, (byte) 10, (byte) 9, new DTD(true, (short) 0, (byte) 11, (byte) 0, 0)),
                         res);
  }

  @Test
  public void castDateFromString2() {
    Sequence res = new Query("'2002-10-10+13:00' cast as xs:date").execute(ctx);
    ResultChecker.dCheck(new Date((short) 2002,
                                  (byte) 10,
                                  (byte) 10,
                                  new DTD(false, (short) 0, (byte) 13, (byte) 0, 0)), res);
  }

  @Test
  public void subtractDateTimes() {
    Sequence res = new Query("xs:dateTime(\"2000-10-30T06:12:00\") -  xs:dateTime(\"1999-11-28T09:00:00\")").execute(
                                                                                                                     ctx);
    ResultChecker.dCheck(new DTD(false, (short) 336, (byte) 21, (byte) 12, 0), res);
  }

  @Test
  public void subtractDateTimes2() {
    Sequence res = new Query("xs:dateTime(\"2000-10-30T06:12:00\") -  xs:dateTime(\"1999-11-28T09:00:00Z\")").execute(
                                                                                                                      ctx);
    ResultChecker.dCheck(new DTD(false, (short) 336, (byte) 21, (byte) 12, 0), res);
  }

  @Test
  public void castStringAsDouble() {
    Sequence result = new Query("'-1.000E4' cast as xs:double").execute(ctx);
    ResultChecker.dCheck(new Dbl(-10000), result);
  }

  @Test
  public void doubleConstructorFunction() {
    Sequence result = new Query("xs:double('-1.000E4')").execute(ctx);
    ResultChecker.dCheck(new Dbl(-10000), result);
  }

  @Test
  public void castStringAsUnsignedByte() {
    Sequence result = new Query("'     255    ' cast as xs:unsignedByte").execute(ctx);
    ResultChecker.dCheck(new Int32(255).asType(Type.UBYT), result);
  }

  @Test
  public void illegalCastStringAsUnsignedByte() {
    try {
      new Query("'256' cast as xs:unsignedByte").execute(ctx);
      fail("Illegal cast not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
    }
  }

  @Test
  public void stringCastableAsDouble() {
    Sequence result = new Query("'-1.000E4' castable as xs:double").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void integerTreatAsDecimal() {
    Sequence result = new Query("3 treat as xs:decimal").execute(ctx);
    ResultChecker.dCheck(new Int32(3), result);
  }

  @Test
  public void integerInstanceOfDecimal() {
    Sequence result = new Query("3 instance of xs:decimal").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void illegalStringTreatAsDouble() {
    try {
      new Query("'foo' treat as xs:double").execute(ctx);
      fail("Illegal treat not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, e.getCode());
    }
  }

  @Test
  public void stringNotCastableAsDouble() {
    Sequence result = new Query("'Foo' castable as xs:double").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void castLegalEmptySequenceAsDouble() {
    Sequence result = new Query("() cast as xs:double?").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void castIllegalEmptySequenceAsDouble() {
    try {
      new Query("() cast as xs:double").execute(ctx);
      fail("Illegal case not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
    }
  }
}
