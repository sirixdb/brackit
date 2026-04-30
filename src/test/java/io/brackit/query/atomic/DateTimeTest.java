package io.brackit.query.atomic;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.SplittableRandom;

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

  // =========================================================================
  // DTD subtract regressions (addInternal sign-bit corruption + wrong newNegative)
  // =========================================================================

  /**
   * Pre-fix this returned {@code -PT127H} because {@code newNegative} was decided only
   * by the sign of {@code newDays}. When the days components cancelled, the routine
   * stored the negative {@code newHours} byte directly in the DTD's high-bit-encoded
   * sign field, producing {@code 0xFF & 0x7F == 127}.
   */
  @Test
  public void dtdSubtract_smallerMinusLarger_yieldsCorrectNegativeMagnitude() throws Exception {
    assertEquals("-PT1H", new DTD("PT1H").subtract(new DTD("PT2H")).toString());
    assertEquals("-PT30M", new DTD("PT30M").subtract(new DTD("PT1H")).toString());
    assertEquals("-PT1S", new DTD("PT0S").subtract(new DTD("PT1S")).toString());
  }

  @Test
  public void dtdAdd_negativeOperand_renormalizesProperly() throws Exception {
    assertEquals("PT1H", new DTD("-PT1H").add(new DTD("PT2H")).toString());
    assertEquals("-PT1H", new DTD("PT1H").add(new DTD("-PT2H")).toString());
    assertEquals("PT15M", new DTD("-PT30M").add(new DTD("PT45M")).toString());
  }

  // =========================================================================
  // YMD subtract regressions (analogous addInternal bug)
  // =========================================================================

  /**
   * Pre-fix this returned {@code -P1Y122M} because {@code newNegative} looked only at
   * {@code newYears}, and the negate of months by {@code *= -1} on a {@code byte}
   * collided with YMD's high-bit-encoded sign field.
   */
  @Test
  public void ymdSubtract_smallerMinusLarger_yieldsCorrectNegativeMagnitude() throws Exception {
    assertEquals("-P6M", new YMD("P6M").subtract(new YMD("P1Y")).toString());
    assertEquals("-P1Y", new YMD("P1Y").subtract(new YMD("P2Y")).toString());
  }

  // =========================================================================
  // DateTime + duration boundary cases (off-by-one borrow on January and day=0)
  // =========================================================================

  /**
   * Pre-fix {@code 2026-05-01T01:00:00Z - PT2H} returned {@code 2026-05-00T23:00:00Z}
   * (invalid 0th-of-May) because the day-borrow loop checked {@code newDays < 0}
   * instead of {@code newDays < 1}.
   */
  @Test
  public void dateTimeSubtract_dayTimeDuration_acrossMonthBoundary() throws Exception {
    DateTime a = new DateTime("2026-05-01T01:00:00Z");
    DTD twoHours = new DTD("PT2H");
    assertEquals("2026-04-30T23:00:00Z", a.subtract(twoHours).toString());
  }

  /**
   * Pre-fix subtracting from January would call {@code maxDayInMonth(year, 0)} (the
   * lookup happened before the year/month wrap), producing wrong day counts for
   * year-boundary borrows. Correct: roll to December of the previous year.
   */
  @Test
  public void dateTimeSubtract_dayTimeDuration_acrossYearBoundary() throws Exception {
    DateTime a = new DateTime("2025-01-01T01:00:00Z");
    DTD twoHours = new DTD("PT2H");
    assertEquals("2024-12-31T23:00:00Z", a.subtract(twoHours).toString());
  }

  /** Symmetric case: forward roll across year boundary (December into January). */
  @Test
  public void dateTimeAdd_dayTimeDuration_acrossYearBoundary() throws Exception {
    DateTime a = new DateTime("2024-12-31T23:00:00Z");
    DTD twoHours = new DTD("PT2H");
    assertEquals("2025-01-01T01:00:00Z", a.add(twoHours).toString());
  }

  // ─────────────────────────── Property tests ───────────────────────────
  //
  // 10 000 randomized samples per invariant. The total-micros / total-months
  // round-trip is the contract proven in docs/formal-verification.md
  // (Inv 1.1a, 1.2a, 1.3a). Fixed seeds keep the run deterministic; if a
  // future refactor breaks the contract, the failure surfaces with a specific
  // input pair.

  private static final long MICROS_PER_MIN = 60_000_000L;
  private static final long MICROS_PER_HOUR = MICROS_PER_MIN * 60L;
  private static final long MICROS_PER_DAY = MICROS_PER_HOUR * 24L;

  /**
   * Random DTD covering full positive and negative dynamic range. Bounded so the
   * pairwise sum stays within long range with margin.
   */
  private static DTD randomDtd(final SplittableRandom rng) {
    final boolean negative = rng.nextBoolean();
    final short days = (short) rng.nextInt(0, 1 << 14);
    final byte hours = (byte) rng.nextInt(0, 24);
    final byte minutes = (byte) rng.nextInt(0, 60);
    final int micros = rng.nextInt(0, (int) MICROS_PER_MIN);
    return new DTD(negative, days, hours, minutes, micros);
  }

  /** Reify a DTD as its signed total-micros count, the contract's measure. */
  private static long totalMicros(final DTD d) {
    final long magnitude = (long) d.getDays() * MICROS_PER_DAY + (long) d.getHours() * MICROS_PER_HOUR + (long) d
                                                                                                                 .getMinutes()
        * MICROS_PER_MIN + d.getMicros();
    return d.isNegative() ? -magnitude : magnitude;
  }

  @Test
  public void property_dtdAdd_preservesTotalMicros() throws Exception {
    final SplittableRandom rng = new SplittableRandom(0xD7DADD0L);
    for (int i = 0; i < 10_000; i++) {
      final DTD a = randomDtd(rng);
      final DTD b = randomDtd(rng);
      final long expected = totalMicros(a) + totalMicros(b);
      final DTD actual = a.add(b);
      assertEquals(expected, totalMicros(actual), "iteration " + i + ": " + a + " + " + b + " = " + actual);
    }
  }

  @Test
  public void property_dtdSubtract_preservesTotalMicros() throws Exception {
    final SplittableRandom rng = new SplittableRandom(0xD7D5BB1AC7L);
    for (int i = 0; i < 10_000; i++) {
      final DTD a = randomDtd(rng);
      final DTD b = randomDtd(rng);
      final long expected = totalMicros(a) - totalMicros(b);
      final DTD actual = a.subtract(b);
      assertEquals(expected, totalMicros(actual), "iteration " + i + ": " + a + " - " + b + " = " + actual);
    }
  }

  @Test
  public void property_dtdAdd_resultIsCanonical() throws Exception {
    // Inv 1.1b: hours <24, minutes <60, micros <60M.
    final SplittableRandom rng = new SplittableRandom(0xCAFE0001CL);
    for (int i = 0; i < 10_000; i++) {
      final DTD r = randomDtd(rng).add(randomDtd(rng));
      assertTrue(r.getHours() >= 0 && r.getHours() < 24, "non-canonical hours: " + r);
      assertTrue(r.getMinutes() >= 0 && r.getMinutes() < 60, "non-canonical minutes: " + r);
      assertTrue(r.getMicros() >= 0 && r.getMicros() < MICROS_PER_MIN, "non-canonical micros: " + r);
    }
  }

  /** Random YMD over a representative range. */
  private static YMD randomYmd(final SplittableRandom rng) {
    final boolean negative = rng.nextBoolean();
    final short years = (short) rng.nextInt(0, 1 << 13);
    final byte months = (byte) rng.nextInt(0, 12);
    return new YMD(negative, years, months);
  }

  private static long totalMonths(final YMD y) {
    final long magnitude = (long) y.getYears() * 12L + y.getMonths();
    return y.isNegative() ? -magnitude : magnitude;
  }

  @Test
  public void property_ymdAdd_preservesTotalMonths() throws Exception {
    final SplittableRandom rng = new SplittableRandom(0x47DADD7L);
    for (int i = 0; i < 10_000; i++) {
      final YMD a = randomYmd(rng);
      final YMD b = randomYmd(rng);
      final long expected = totalMonths(a) + totalMonths(b);
      final YMD actual = a.add(b);
      assertEquals(expected, totalMonths(actual), "iteration " + i + ": " + a + " + " + b + " = " + actual);
    }
  }

  @Test
  public void property_ymdSubtract_preservesTotalMonths() throws Exception {
    final SplittableRandom rng = new SplittableRandom(0x47D5BB1AC7L);
    for (int i = 0; i < 10_000; i++) {
      final YMD a = randomYmd(rng);
      final YMD b = randomYmd(rng);
      final long expected = totalMonths(a) - totalMonths(b);
      final YMD actual = a.subtract(b);
      assertEquals(expected, totalMonths(actual), "iteration " + i + ": " + a + " - " + b + " = " + actual);
    }
  }

  /**
   * Property: subtracting two dateTimes a, b returns a duration whose total micros
   * equals (a - b). Uses the proleptic Gregorian calendar via {@link LocalDate} to
   * compute the expected day diff, then composes with the time-of-day to get the
   * full expected instant difference.
   */
  /**
   * Property: subtracting two dateTimes a, b returns a duration whose total micros
   * equals (a - b). After widening DTD.days from short to int (Inv 1.4 in
   * docs/formal-verification.md), any realistic dateTime span fits in DTD without
   * overflow — Integer.MAX_VALUE days is ~5.8 million years.
   */
  @Test
  public void property_dateTimeSubtract_preservesSignedInstantDifference() throws Exception {
    final SplittableRandom rng = new SplittableRandom(0xD75BB1AC7L);
    final LocalDate centre = LocalDate.of(2000, 1, 1);
    // Span well past the prior short-days range (±32k days) so the property exercises
    // the int-widening fix end-to-end.
    final int maxOffset = Short.MAX_VALUE * 4;
    for (int i = 0; i < 10_000; i++) {
      final LocalDate dateA = centre.plusDays(rng.nextInt(-maxOffset, maxOffset + 1));
      final LocalDate dateB = centre.plusDays(rng.nextInt(-maxOffset, maxOffset + 1));
      final int hourA = rng.nextInt(0, 24);
      final int hourB = rng.nextInt(0, 24);
      final int minA = rng.nextInt(0, 60);
      final int minB = rng.nextInt(0, 60);
      final int microA = rng.nextInt(0, (int) MICROS_PER_MIN);
      final int microB = rng.nextInt(0, (int) MICROS_PER_MIN);

      final DateTime a = new DateTime((short) dateA.getYear(),
                                      (byte) dateA.getMonthValue(),
                                      (byte) dateA.getDayOfMonth(),
                                      (byte) hourA,
                                      (byte) minA,
                                      microA,
                                      AbstractTimeInstant.UTC_TIMEZONE);
      final DateTime b = new DateTime((short) dateB.getYear(),
                                      (byte) dateB.getMonthValue(),
                                      (byte) dateB.getDayOfMonth(),
                                      (byte) hourB,
                                      (byte) minB,
                                      microB,
                                      AbstractTimeInstant.UTC_TIMEZONE);

      final long dayDiff = dateA.toEpochDay() - dateB.toEpochDay();
      final long expected = dayDiff * MICROS_PER_DAY + ((long) hourA - hourB) * MICROS_PER_HOUR + ((long) minA - minB)
          * MICROS_PER_MIN + ((long) microA - microB);

      final DTD actual = a.subtract(b);
      final int iter = i;
      assertEquals(expected, totalMicros(actual), () -> "iteration " + iter + ": " + a + " - " + b + " = " + actual);
    }
  }
}
