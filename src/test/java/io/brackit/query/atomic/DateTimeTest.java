package io.brackit.query.atomic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DateTimeTest {
  @Test
  public void testParseString() {
    new DateTime("2020-05-06T11:07:21");
  }

  /**
   * Regression: pre-fix, subtracting two dateTimes that fell on the same day with the
   * same hour added a phantom +29 days +24 hours to the result, because the borrow logic
   * landed in the wrong branch when {@code ehour == a.getHours()}. Verify the small-gap
   * cases explicitly.
   */
  @Test
  public void subtractSameDaySameHour_subSecondGap() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:33:26.047000Z");
    DateTime b = new DateTime("2026-04-30T16:33:25.607367Z");
    DTD diff = a.subtract(b);
    assertEquals("PT0.439633S", diff.toString());
  }

  @Test
  public void subtractSameDaySameHour_oneSecondGap() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:33:26Z");
    DateTime b = new DateTime("2026-04-30T16:33:25Z");
    DTD diff = a.subtract(b);
    assertEquals("PT1S", diff.toString());
  }

  @Test
  public void subtractSameInstant_isZero() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:33:26Z");
    DateTime b = new DateTime("2026-04-30T16:33:26Z");
    DTD diff = a.subtract(b);
    assertEquals("PT0S", diff.toString());
  }

  /**
   * Regression: minute borrow used to be {@code minutes *= -1}, which only happens to
   * give the right answer for borrowed values that are exactly half the modulus. For a
   * 35-minute gap (10 - 35 = -25 → should become 35), the negate yielded 25.
   */
  @Test
  public void subtractAcrossHour_minuteBorrowIsCorrect() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:10:00Z");
    DateTime b = new DateTime("2026-04-30T15:35:00Z");
    DTD diff = a.subtract(b);
    assertEquals("PT35M", diff.toString());
  }

  @Test
  public void subtractAcrossDayBoundary() throws Exception {
    DateTime a = new DateTime("2026-05-01T01:00:00Z");
    DateTime b = new DateTime("2026-04-30T23:00:00Z");
    DTD diff = a.subtract(b);
    assertEquals("PT2H", diff.toString());
  }

  @Test
  public void subtractAcrossYearBoundary() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:00:00Z");
    DateTime b = new DateTime("2025-04-30T16:00:00Z");
    DTD diff = a.subtract(b);
    assertEquals("P365D", diff.toString());
  }

  @Test
  public void subtractAcrossLeapYear() throws Exception {
    // 2024 is a leap year; 2024-03-01 minus 2024-02-01 should be 29 days, not 28.
    DateTime a = new DateTime("2024-03-01T00:00:00Z");
    DateTime b = new DateTime("2024-02-01T00:00:00Z");
    DTD diff = a.subtract(b);
    assertEquals("P29D", diff.toString());
  }

  @Test
  public void subtractNegative_keepsAbsoluteMagnitudeAndSign() throws Exception {
    // a < b, so the result should be negative.
    DateTime a = new DateTime("2026-04-30T15:35:00Z");
    DateTime b = new DateTime("2026-04-30T16:10:00Z");
    DTD diff = a.subtract(b);
    assertTrue(diff.toString().startsWith("-"), "expected negative duration, got " + diff);
    assertEquals("-PT35M", diff.toString());
  }

  /**
   * The xs:dayTimeDuration P7D comparison from the use-case fraud-detection query.
   * A sub-second gap must NOT compare greater than 7 days.
   */
  @Test
  public void subSecondGap_isNotGreaterThan7Days() throws Exception {
    DateTime a = new DateTime("2026-04-30T16:33:26.047000Z");
    DateTime b = new DateTime("2026-04-30T16:33:25.607367Z");
    DTD diff = a.subtract(b);
    DTD sevenDays = new DTD(false, (short) 7, (byte) 0, (byte) 0, 0);
    assertTrue(diff.cmp(sevenDays) < 0, "sub-second diff should be less than P7D, got " + diff);
  }
}
