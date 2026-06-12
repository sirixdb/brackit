package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for fn:format-dateTime, fn:format-date, and fn:format-time, covering the examples of
 * XQuery F&amp;O 3.1 section 9.8.4.8 (adapted where the spec example text contradicts its own
 * input data) plus edge cases: bracket escapes, width modifiers, fractional second
 * truncation/padding, negative years, timezone presentations, and the spec'd error conditions
 * (err:FOFD1340 and err:FOFD1350).
 */
public class FormatDateTimeTest extends XQueryBaseTest {

  /** The spec's example date: a Tuesday. */
  private static final String DATE = "2002-12-31";
  private static final String TIME = "15:58:45.762";
  private static final String DATE_TIME = "2002-12-31T15:58:45.762";

  private static String fd(String picture) {
    return "format-date(xs:date('" + DATE + "'), '" + picture + "')";
  }

  private static String fd5(String picture, String lang, String cal) {
    return "format-date(xs:date('" + DATE + "'), '" + picture + "', " + lang + ", " + cal + ", ())";
  }

  private static String ft(String picture) {
    return "format-time(xs:time('" + TIME + "'), '" + picture + "')";
  }

  private static String fdt(String picture) {
    return "format-dateTime(xs:dateTime('" + DATE_TIME + "'), '" + picture + "')";
  }

  private void check(String expected, String query) {
    ResultChecker.dCheck(new Str(expected), new Query(query).execute(ctx));
  }

  private void checkErr(QNm code, String query) {
    final QueryException ex = assertThrows(QueryException.class, () -> new Query(query).execute(ctx), query);
    assertEquals(code, ex.getCode(), query);
  }

  // -------------------------------------------------------------------------------------------
  // Spec examples (F&O 3.1, 9.8.4.8)
  // -------------------------------------------------------------------------------------------

  @Test
  public void specExamplesFormatDate() {
    check("2002-12-31", fd("[Y0001]-[M01]-[D01]"));
    check("12-31-2002", fd("[M]-[D]-[Y]"));
    check("31-12-2002", fd("[D]-[M]-[Y]"));
    check("31 XII 2002", fd("[D1] [MI] [Y]"));
    check("31st December, 2002", fd5("[D1o] [MNn], [Y]", "'en'", "()"));
    check("31 DEC 2002", fd5("[D01] [MN,*-3] [Y0001]", "'en'", "()"));
    check("December 31, 2002", fd5("[MNn] [D], [Y]", "'en'", "()"));
    check("[2002-12-31]", fd("[[[Y0001]-[M01]-[D01]]]"));
    // the spec displays "Two Thousand and Three" for this picture, but its example input date is
    // 2002-12-31 (the original XSLT 2.0 example used the year 2003)
    check("Two Thousand and Two", fd5("[YWw]", "'en'", "()"));
  }

  @Test
  public void specExamplesFormatTime() {
    check("3:58 PM", "format-time(xs:time('" + TIME + "'), '[h]:[m01] [PN]', 'en', (), ())");
    check("3:58:45 pm", "format-time(xs:time('" + TIME + "'), '[h]:[m01]:[s01] [Pn]', 'en', (), ())");
    check("15:58", ft("[H01]:[m01]"));
    check("15:58:45.762", ft("[H01]:[m01]:[s01].[f001]"));
    check("15:58:45 GMT+02:00",
          "format-time(xs:time('15:58:45.762+02:00'), '[H01]:[m01]:[s01] [z,6-6]', 'en', (), ())");
  }

  @Test
  public void specExamplesFormatDateTime() {
    check("3.58pm on Tuesday, 31st December", fdt("[h].[m01][Pn] on [FNn], [D1o] [MNn]"));
    check("12/31/2002 at 15:58:45", fdt("[M01]/[D01]/[Y0001] at [H01]:[m01]:[s01]"));
  }

  // -------------------------------------------------------------------------------------------
  // Empty sequence and literal handling
  // -------------------------------------------------------------------------------------------

  @Test
  public void emptyValueYieldsEmptySequence() {
    ResultChecker.dCheck(Bool.TRUE, new Query("empty(format-date((), '[Y]'))").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("empty(format-time((), '[H]'))").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("empty(format-dateTime((), '[Y]', (), (), ()))").execute(ctx));
  }

  @Test
  public void literalOnlyAndEscapedBrackets() {
    check("plain text", fd("plain text"));
    check("", fd(""));
    check("[Y]", fd("[[Y]]"));
    check("a[b]c", fd("a[[b]]c"));
    check("31[", fd("[D][["));
  }

  @Test
  public void whitespaceInsideMarkerIsIgnored() {
    check("2002-12-31", fd("[Y 0001]-[M 01]-[D 01]"));
  }

  // -------------------------------------------------------------------------------------------
  // Width modifiers and year truncation
  // -------------------------------------------------------------------------------------------

  @Test
  public void yearTruncation() {
    check("02", fd("[Y01]"));
    check("02", fd("[Y,2-2]"));
    check("2002", fd("[Y]"));
    check("2002", fd("[Y,2]"));
    check("0031", "format-date(xs:date('0031-01-01'), '[Y0001]')");
    // grouping separator in the year pattern; the trailing ,* is the width modifier
    check("2,008", "format-date(xs:date('2008-01-01'), '[Y9,999,*]')");
  }

  @Test
  public void widthModifiers() {
    check("031", fd("[D,3]"));
    check("31", fd("[D,2]"));
    check("Dec", fd("[MNn,3-3]"));
    check("DECEMBER", fd("[MN]"));
    check("december ", fd("[Mn,9]"));
    check("Tue", fdt("[FNn,3-3]"));
  }

  @Test
  public void invalidWidthModifiers() {
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y,0]"));
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y,3-2]"));
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y,]"));
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y,x]"));
  }

  // -------------------------------------------------------------------------------------------
  // Component coverage
  // -------------------------------------------------------------------------------------------

  @Test
  public void dayInYearWeekInYearWeekInMonth() {
    check("365", fd("[d]"));
    // 2002-12-31 falls into ISO week 1 of 2003
    check("1", fd("[W]"));
    // spec example: 29 January 2013 falls in week 5 of the month, and so does 1 February 2013
    check("5", "format-date(xs:date('2013-01-29'), '[w]')");
    check("5", "format-date(xs:date('2013-02-01'), '[w]')");
    // ISO week of year: week 1 of 2013 starts on Monday 2012-12-31
    check("5", "format-date(xs:date('2013-01-29'), '[W]')");
    check("1", "format-date(xs:date('2013-01-01'), '[W]')");
  }

  @Test
  public void dayOfWeek() {
    check("tuesday", fdt("[F]"));
    check("Tuesday", fdt("[FNn]"));
    check("TUESDAY", fdt("[FN]"));
    // ISO numbering: Monday=1 ... Sunday=7
    check("2", fdt("[F1]"));
    check("6", "format-date(xs:date('2000-01-01'), '[F1]')"); // a Saturday
  }

  @Test
  public void twelveHourClockAndAmPm() {
    check("12 am", "format-time(xs:time('00:30:00'), '[h] [P]')");
    check("12 pm", "format-time(xs:time('12:00:00'), '[h] [P]')");
    check("1 pm", "format-time(xs:time('13:00:00'), '[h] [P]')");
    check("11 AM", "format-time(xs:time('11:59:59'), '[h] [PN]')");
    check("Pm", "format-time(xs:time('13:00:00'), '[PNn]')");
  }

  @Test
  public void ordinalAndOtherNumberings() {
    check("31st", fd("[D1o]"));
    check("1st", "format-date(xs:date('2002-12-01'), '[D1o]')");
    check("2nd", "format-date(xs:date('2002-12-02'), '[D1o]')");
    check("3rd", "format-date(xs:date('2002-12-03'), '[D1o]')");
    check("11th", "format-date(xs:date('2002-12-11'), '[D1o]')");
    check("thirty-first", fd("[Dwo]"));
    check("xii", fd("[Mi]"));
    check("e", "format-date(xs:date('2002-05-01'), '[Ma]')");
  }

  // -------------------------------------------------------------------------------------------
  // Fractional seconds
  // -------------------------------------------------------------------------------------------

  @Test
  public void fractionalSeconds() {
    check("762", ft("[f]"));
    check("762", ft("[f1]"));
    check("76", ft("[f01]"));
    check("7620", ft("[f0001]"));
    check("7", ft("[f1,1-1]"));
    check("76", ft("[f,2-2]"));
    check("762000", ft("[f,6-6]"));
    check("5", "format-time(xs:time('10:00:00.5'), '[f1]')");
    check("500", "format-time(xs:time('10:00:00.5'), '[f001]')");
    check("000", "format-time(xs:time('10:00:00'), '[f001]')");
    check("0", "format-time(xs:time('10:00:00'), '[f]')");
    // 1 mandatory digit plus optional digits: truncates, strips trailing zeroes down to one digit
    check("762", ft("[f1##]"));
    check("5", "format-time(xs:time('10:00:00.5'), '[f1##]')");
  }

  @Test
  public void fractionalSecondsGroupingSeparatorIsRejected() {
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, ft("[f0,0]"));
  }

  // -------------------------------------------------------------------------------------------
  // Negative years and eras
  // -------------------------------------------------------------------------------------------

  @Test
  public void negativeYears() {
    // the year component formats the absolute year value; the era carries the BC/AD distinction
    check("0044 BC", "format-date(xs:date('-0044-03-15'), '[Y0001] [E]')");
    check("44", "format-date(xs:date('-0044-03-15'), '[Y]')");
    check("2002 AD", fd("[Y] [E]"));
    // the ISO calendar's era is a minus sign for negative years, an empty string otherwise
    check("-0044", "format-date(xs:date('-0044-03-15'), '[E][Y0001]', (), 'ISO', ())");
    check("2002", fd5("[E][Y]", "()", "'ISO'"));
  }

  @Test
  public void wideYears() {
    check("12345", "format-date(xs:date('12345-06-07'), '[Y]')");
    // four-digit pattern: the year is output modulo 10^4
    check("2345", "format-date(xs:date('12345-06-07'), '[Y0001]')");
  }

  @Test
  public void calendarComponent() {
    check("AD", fd("[C]"));
    check("ISO", fd5("[C]", "()", "'ISO'"));
  }

  // -------------------------------------------------------------------------------------------
  // Timezones
  // -------------------------------------------------------------------------------------------

  private static String fdtTz(String tz, String picture) {
    return "format-dateTime(xs:dateTime('2002-12-31T12:00:00" + tz + "'), '" + picture + "')";
  }

  @Test
  public void timezoneNumericPresentations() {
    check("+05:30", fdtTz("+05:30", "[Z]"));
    check("-05:00", fdtTz("-05:00", "[Z]"));
    check("+00:00", fdtTz("Z", "[Z]"));
    check("-10", fdtTz("-10:00", "[Z0]"));
    check("-5", fdtTz("-05:00", "[Z0]"));
    check("+0", fdtTz("Z", "[Z0]"));
    check("+5:30", fdtTz("+05:30", "[Z0]"));
    check("-5:00", fdtTz("-05:00", "[Z0:00]"));
    check("+05:30", fdtTz("+05:30", "[Z00:00]"));
    check("-0500", fdtTz("-05:00", "[Z0000]"));
    check("+0530", fdtTz("+05:30", "[Z0000]"));
    check("+13:00", fdtTz("+13:00", "[Z]"));
  }

  @Test
  public void timezoneTraditionalModifierAndMilitary() {
    check("Z", fdtTz("Z", "[Z00:00t]"));
    check("-05:00", fdtTz("-05:00", "[Z00:00t]"));
    check("W", fdtTz("-10:00", "[ZZ]"));
    check("R", fdtTz("-05:00", "[ZZ]"));
    check("Z", fdtTz("Z", "[ZZ]"));
    check("A", fdtTz("+01:00", "[ZZ]"));
    check("K", fdtTz("+10:00", "[ZZ]"));
    // offsets without a military letter fall back to the 01:01 presentation
    check("+05:30", fdtTz("+05:30", "[ZZ]"));
    check("+13:00", fdtTz("+13:00", "[ZZ]"));
  }

  @Test
  public void timezoneGmtPrefix() {
    check("GMT+05:30", fdtTz("+05:30", "[z]"));
    check("GMT-10:00", fdtTz("-10:00", "[z]"));
    check("GMT+00:00", fdtTz("Z", "[z]"));
    check("GMT+1", fdtTz("+01:00", "[z0]"));
  }

  @Test
  public void timezoneAbsentFromValue() {
    // not an error: the timezone component produces no output, except military "J" (local time)
    check("", fdt("[Z]"));
    check("", fdt("[z]"));
    check("12:00", "format-time(xs:time('12:00:00'), '[H01]:[m01][Z01:01]')");
    check("J", fdt("[ZZ]"));
    // a timezone name needs a timezone database; the spec'd fallback is the 01:01 format
    check("+05:30", fdtTz("+05:30", "[ZN]"));
  }

  // -------------------------------------------------------------------------------------------
  // Language, calendar, and place arguments
  // -------------------------------------------------------------------------------------------

  @Test
  public void unsupportedLanguageFallsBackLoudly() {
    check("[Language: en]December", fd5("[MNn]", "'de'", "()"));
    check("December", fd5("[MNn]", "'en'", "()"));
    check("December", fd5("[MNn]", "'EN-GB'", "()"));
    check("December", fd5("[MNn]", "()", "()"));
  }

  @Test
  public void unsupportedCalendarFallsBackLoudly() {
    check("[Calendar: AD]31 December 2002", fd5("[D] [MNn] [Y]", "'en'", "'OS'"));
    check("[Calendar: AD]2002", fd5("[Y]", "()", "'AH'"));
    check("2002", fd5("[Y]", "()", "'AD'"));
    // a namespaced calendar EQName is implementation-defined territory: unsupported here
    check("[Calendar: AD]2002", fd5("[Y]", "()", "'Q{http://example.org}cal'"));
  }

  @Test
  public void invalidCalendarIsRejected() {
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd5("[Y]", "()", "'XYZ'"));
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd5("[Y]", "()", "'not a name'"));
  }

  @Test
  public void placeIsAcceptedButUnused() {
    check("2002", "format-date(xs:date('" + DATE + "'), '[Y]', (), (), 'America/New_York')");
  }

  // -------------------------------------------------------------------------------------------
  // Error conditions
  // -------------------------------------------------------------------------------------------

  @Test
  public void invalidPictureSyntax() {
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y")); // unclosed marker
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("a]b")); // lone closing bracket
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[]")); // empty marker
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Q]")); // unknown component
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fd("[Y0#1]")); // optional digit after mandatory
    // exotic timezone presentation modifiers fail loudly instead of guessing
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fdtTz("+05:30", "[Z00000]"));
    checkErr(ErrorCode.ERR_INVALID_DATETIME_PICTURE, fdtTz("+05:30", "[Z0:0:0]"));
  }

  @Test
  public void componentNotAvailableInValueType() {
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, fd("[H]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, fd("[m]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, fd("[P]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, ft("[Y]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, ft("[M]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, ft("[D]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, ft("[F]"));
    checkErr(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE, ft("[E]"));
  }
}
