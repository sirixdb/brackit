package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for fn:adjust-dateTime-to-timezone, fn:adjust-date-to-timezone, and
 * fn:adjust-time-to-timezone with sub-hour timezone offsets.
 *
 * <p>Previously (a) the timezone argument was rejected with FODT0003 whenever it had a minutes
 * component (PT5H30M is a perfectly valid timezone), and (b) the adjustment arithmetic dropped
 * the minutes of BOTH the old and the new offset, so values carrying a sub-hour timezone (e.g.
 * +05:30 from a parsed literal) were silently shifted to a wrong instant.
 */
public class AdjustToTimezoneTest extends XQueryBaseTest {

  private static String adjust(String value, String tz) {
    return "string(adjust-dateTime-to-timezone(xs:dateTime('" + value + "'), xs:dayTimeDuration('" + tz + "')))";
  }

  @Test
  public void subHourTargetTimezone() {
    ResultChecker.dCheck(new Str("2026-05-01T15:30:00+05:30"),
                         new Query(adjust("2026-05-01T10:00:00Z", "PT5H30M")).execute(ctx));
    ResultChecker.dCheck(new Str("2026-05-01T06:30:00-03:30"),
                         new Query(adjust("2026-05-01T10:00:00Z", "-PT3H30M")).execute(ctx));
    ResultChecker.dCheck(new Str("15:30:00+05:30"),
                         new Query("string(adjust-time-to-timezone(xs:time('10:00:00Z'), xs:dayTimeDuration('PT5H30M')))").execute(ctx));
    ResultChecker.dCheck(new Str("2026-04-30-10:30"),
                         new Query("string(adjust-date-to-timezone(xs:date('2026-05-01Z'), xs:dayTimeDuration('-PT10H30M')))").execute(ctx));
  }

  @Test
  public void subHourSourceTimezone() {
    // The input's +05:30 offset must be honoured in full: 10:00+05:30 is 04:30Z (the old code
    // dropped the 30 minutes and produced 05:00Z).
    ResultChecker.dCheck(new Str("2026-05-01T04:30:00Z"),
                         new Query(adjust("2026-05-01T10:00:00+05:30", "PT0S")).execute(ctx));
  }

  @Test
  public void adjustingBackAndForthPreservesInstant() {
    ResultChecker.dCheck(new Str("2026-05-01T10:00:00Z"),
                         new Query("string(adjust-dateTime-to-timezone("
                             + "adjust-dateTime-to-timezone(xs:dateTime('2026-05-01T10:00:00Z'), xs:dayTimeDuration('PT5H45M'))"
                             + ", xs:dayTimeDuration('PT0S')))").execute(ctx));
  }

  @Test
  public void wholeHourTimezonesUnchanged() {
    ResultChecker.dCheck(new Str("2026-05-01T15:00:00+05:00"),
                         new Query(adjust("2026-05-01T10:00:00Z", "PT5H")).execute(ctx));
    ResultChecker.dCheck(new Str("2026-05-02T00:00:00+14:00"),
                         new Query(adjust("2026-05-01T10:00:00Z", "PT14H")).execute(ctx));
  }

  @Test
  public void invalidTimezonesRejected() {
    for (String tz : new String[] { "PT14H1M", "PT15H", "-PT15H", "PT1H1S", "PT1H0.5S", "P1D" }) {
      QueryException ex = assertThrows(QueryException.class,
                                       () -> new Query(adjust("2026-05-01T10:00:00Z", tz)).execute(ctx),
                                       tz);
      assertEquals(ErrorCode.ERR_INVALID_TIMEZONE, ex.getCode(), tz);
    }
  }
}
