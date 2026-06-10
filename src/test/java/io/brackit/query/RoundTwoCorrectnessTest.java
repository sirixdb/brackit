package io.brackit.query;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for the 2026-06-10 correctness audit (non-numeric brackit bugs): surrogate-pair
 * codepoints, negative/BCE year serialization, fractional-second serialization, fn:translate
 * delete/identity semantics, empty + whitespace-flag regex handling, yearMonthDuration scaling, and
 * fn:subsequence with a non-positive start.
 */
public class RoundTwoCorrectnessTest extends XQueryBaseTest {

  // U+1D54F MATHEMATICAL DOUBLE-STRUCK CAPITAL X (a non-BMP / surrogate-pair character).
  private static final String X = "𝕏";

  @Test
  public void stringToCodepointsHandlesNonBmp() {
    ResultChecker.dCheck(new Int32(1), new Query("count(string-to-codepoints('" + X + "'))").execute(ctx));
    ResultChecker.dCheck(new Int32(120143), new Query("string-to-codepoints('" + X + "')").execute(ctx));
  }

  @Test
  public void stringLengthAndSubstringCountCodepoints() {
    ResultChecker.dCheck(new Int32(2), new Query("fn:string-length('" + X + X + "')").execute(ctx));
    // substring positions count codepoints; the non-BMP char must not be split.
    ResultChecker.dCheck(new Str("abc"), new Query("fn:substring('" + X + "abc', 2)").execute(ctx));
    ResultChecker.dCheck(new Str(X), new Query("fn:substring('" + X + "abc', 1, 1)").execute(ctx));
  }

  @Test
  public void codepointsRoundTripNonBmp() {
    ResultChecker.dCheck(new Str(X), new Query("codepoints-to-string(string-to-codepoints('" + X + "'))").execute(ctx));
  }

  @Test
  public void negativeYearKeepsMagnitude() {
    ResultChecker.dCheck(new Str("-0001-05-15"), new Query("string(xs:date('-0001-05-15'))").execute(ctx));
    ResultChecker.dCheck(new Str("-0044"), new Query("string(xs:gYear('-0044'))").execute(ctx));
    ResultChecker.dCheck(new Str("-0001-05-15T10:00:00"),
                         new Query("string(xs:dateTime('-0001-05-15T10:00:00'))").execute(ctx));
  }

  @Test
  public void fractionalSecondsSerializeAndRoundTrip() {
    ResultChecker.dCheck(new Str("2026-05-01T10:00:05.25"),
                         new Query("string(xs:dateTime('2026-05-01T10:00:05.25'))").execute(ctx));
    // The serialized form must re-parse to the same value (the old ':'-separated form did not).
    ResultChecker.dCheck(new Str("2026-05-01T10:00:05.25"),
                         new Query("string(xs:dateTime(string(xs:dateTime('2026-05-01T10:00:05.25'))))").execute(ctx));
    ResultChecker.dCheck(new Str("09:08:07.000123"), new Query("string(xs:time('09:08:07.000123'))").execute(ctx));
  }

  @Test
  public void translateIdentityAndDelete() {
    // Empty map leaves the string unchanged (was "").
    ResultChecker.dCheck(new Str("abcdef"), new Query("fn:translate('abcdef', '', 'xyz')").execute(ctx));
    // Mapped characters with no replacement counterpart are DELETED (were passed through).
    ResultChecker.dCheck(new Str("xyef"), new Query("fn:translate('abcdef', 'abcd', 'xy')").execute(ctx));
    ResultChecker.dCheck(new Str("AAA"), new Query("fn:translate('--aaa--', 'a-', 'A')").execute(ctx));
  }

  @Test
  public void base64RoundTrips() {
    // "Hello" -> SGVsbG8= ; the value must round-trip (was mangled to SGV2bG8, padding dropped).
    ResultChecker.dCheck(new Str("SGVsbG8="), new Query("string(xs:base64Binary('SGVsbG8='))").execute(ctx));
    ResultChecker.dCheck(new Str("SGVsbG8="),
                         new Query("string(xs:base64Binary(string(xs:base64Binary('SGVsbG8='))))").execute(ctx));
  }

  @Test
  public void emptyRegexMatches() {
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('abc', '')").execute(ctx));
  }

  @Test
  public void whitespaceFlagKeepsBracketsAndClasses() {
    // With the 'x' flag, whitespace is stripped but the character class must survive — the brackets
    // were previously dropped, turning [0-9] into the literal "0-9".
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('5', '[0-9]', 'x')").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("fn:matches('a', '[0-9]', 'x')").execute(ctx));
  }

  @Test
  public void yearMonthDurationScalesTotalMonths() {
    ResultChecker.dCheck(new Str("P6M"), new Query("string(xs:yearMonthDuration('P1Y') div 2)").execute(ctx));
    ResultChecker.dCheck(new Str("P1Y6M"), new Query("string(xs:yearMonthDuration('P3Y') div 2)").execute(ctx));
    ResultChecker.dCheck(new Str("P9M"), new Query("string(xs:yearMonthDuration('P1Y6M') div 2)").execute(ctx));
    ResultChecker.dCheck(new Str("P1Y6M"), new Query("string(xs:yearMonthDuration('P1Y') * 1.5)").execute(ctx));
  }

  @Test
  public void utcVsNonUtcTemporalComparison() {
    // A UTC (Z) value compared against a non-UTC value was wrongly treated as timezone-less.
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("xs:dateTime('2026-05-01T03:00:00+02:00') eq xs:dateTime('2026-05-01T01:00:00Z')").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("xs:dateTime('2026-05-01T03:00:00+02:00') gt xs:dateTime('2026-05-01T00:30:00Z')").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE,
                         new Query("xs:dateTime('2026-05-01T03:00:00+02:00') lt xs:dateTime('2026-05-01T00:30:00Z')").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("xs:time('14:00:00+02:00') eq xs:time('12:00:00Z')").execute(ctx));
  }

  @Test
  public void subHourTimezoneNormalizes() {
    // +00:30 must normalize during canonicalization: 01:30+00:30 == 01:00Z (the old guard skipped
    // normalization whenever the offset hours were 0).
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("xs:dateTime('2026-05-01T01:30:00+00:30') eq xs:dateTime('2026-05-01T01:00:00Z')").execute(ctx));
  }

  @Test
  public void durationFractionalSecondsCanonical() {
    ResultChecker.dCheck(new Str("PT0.5S"), new Query("string(xs:dayTimeDuration('PT0.5S'))").execute(ctx));
    // 0.05s = 50000 micros: must keep the leading zero (was rendered ".50000" = 0.5s).
    ResultChecker.dCheck(new Str("PT0.05S"), new Query("string(xs:dayTimeDuration('PT0.05S'))").execute(ctx));
  }

  @Test
  public void jnParseDuplicateKeysLastWins() {
    // Duplicate object keys: last value wins, and the object holds a single "a" (was keeping both,
    // producing invalid JSON whose lookup disagreed with its serialization).
    ResultChecker.dCheck(new Int32(2), new Query("jn:parse('{\"a\":1,\"a\":2}').a").execute(ctx));
    ResultChecker.dCheck(new Int32(1), new Query("count(jn:keys(jn:parse('{\"a\":1,\"a\":2}')))").execute(ctx));
  }

  @Test
  public void decimalRejectsExponent() {
    // xs:decimal's lexical space has no exponent.
    assertThrows(QueryException.class, () -> new Query("xs:decimal('1.0E2')").execute(ctx));
  }

  @Test
  public void qnameCastFromString() {
    // Unprefixed: a no-namespace QName (was XQDY0074 — the 1-arg QNm ctor never split the string
    // and normalized the prefix to "", so every string cast died in prefix resolution).
    ResultChecker.dCheck(Bool.TRUE, new Query("xs:QName('foo') eq fn:QName('', 'foo')").execute(ctx));
    // Prefixed with a declared namespace: the prefix must RESOLVE (eq compares URI + local name).
    ResultChecker.dCheck(Bool.TRUE,
                         new Query("declare namespace p = 'urn:x'; xs:QName('p:foo') eq fn:QName('urn:x', 'q:foo')").execute(ctx));
    // Malformed lexical QName -> proper cast error (was a raw IllegalStateException).
    assertThrows(QueryException.class, () -> new Query("xs:QName(':foo')").execute(ctx));
  }

  @Test
  public void subsequenceNonPositiveStart() {
    ResultChecker.dCheck(new Int32(2), new Query("count(fn:subsequence((1,2,3,4,5,6,7,8), -2, 5))").execute(ctx));
    ResultChecker.dCheck(new Str("1 2"),
                         new Query("string-join(for $x in fn:subsequence((1,2,3,4,5,6,7,8), -2, 5) return string($x), ' ')").execute(ctx));
    ResultChecker.dCheck(new Str("1 2"),
                         new Query("string-join(for $x in fn:subsequence((1,2,3,4,5,6,7,8), 0, 3) return string($x), ' ')").execute(ctx));
  }
}
