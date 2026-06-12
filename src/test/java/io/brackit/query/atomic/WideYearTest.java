package io.brackit.query.atomic;

import io.brackit.query.QueryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Year fields with more than four digits (XSD: a year is {@code -?([1-9][0-9]{3,}|0[0-9]{3})}).
 *
 * <p>Previously the temporal parsers rejected NEGATIVE years with more than four digits (the sign
 * does not restrict the number of digits) while wrongly accepting leading zeros in 5+ digit years
 * (which the schema spec prohibits).
 */
public class WideYearTest {

  @Test
  public void negativeWideYearsParseAndRoundTrip() {
    assertEquals("-10000-01-01", new Date("-10000-01-01").stringValue());
    assertEquals("-10000-01-01T00:00:00", new DateTime("-10000-01-01T00:00:00").stringValue());
    assertEquals("-12025", new GYE("-12025").stringValue());
    assertEquals("-12025-06", new GYM("-12025-06").stringValue());
  }

  @Test
  public void positiveWideYearsStillParse() {
    assertEquals("10000-01-01", new Date("10000-01-01").stringValue());
    assertEquals("12025", new GYE("12025").stringValue());
  }

  @Test
  public void fourDigitYearsUnchanged() {
    assertEquals("2026-05-01", new Date("2026-05-01").stringValue());
    assertEquals("-0044-03-15", new Date("-0044-03-15").stringValue());
  }

  @Test
  public void leadingZerosInWideYearsRejected() {
    assertThrows(QueryException.class, () -> new Date("010000-01-01"));
    assertThrows(QueryException.class, () -> new Date("-010000-01-01"));
    assertThrows(QueryException.class, () -> new DateTime("010000-01-01T00:00:00"));
    assertThrows(QueryException.class, () -> new GYE("012345"));
    assertThrows(QueryException.class, () -> new GYM("012345-06"));
  }

  @Test
  public void wideYearsOrderCorrectly() throws QueryException {
    assertTrue(new Date("-10000-01-01").cmp(new Date("-0044-01-01")) < 0);
    assertTrue(new Date("10000-01-01").cmp(new Date("9999-12-31")) > 0);
  }
}
