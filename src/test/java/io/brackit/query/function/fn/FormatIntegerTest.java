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
 * Tests for fn:format-integer (XQuery F&amp;O 3.1, section 4.6.1): decimal digit patterns with
 * grouping separators, alphabetic/roman/word sequences, ordinal modifiers, and err:FODF1310.
 */
public class FormatIntegerTest extends XQueryBaseTest {

  private void check(String expected, String query) {
    ResultChecker.dCheck(new Str(expected), new Query(query).execute(ctx));
  }

  private static String fi(String value, String picture) {
    // double quotes: pictures may legitimately contain apostrophes as grouping separators
    return "format-integer(" + value + ", \"" + picture + "\")";
  }

  @Test
  public void decimalDigitPatterns() {
    check("123", fi("123", "1"));
    check("00123", fi("123", "00000"));
    // '000', '001', and '999' are equivalent: any digit is a mandatory digit sign
    check("00123", fi("123", "00999"));
    check("0", fi("0", "1"));
    check("-123", fi("-123", "1"));
    check("300", fi("300", "01")); // numbers are never truncated
  }

  @Test
  public void groupingSeparators() {
    check("1,000,000", fi("1000000", "#,##0"));
    check("15", fi("15", "#,##0"));
    check("1'000'000", fi("1000000", "0'000"));
    check("0'015", fi("15", "0'000"));
    check("1,234,567", fi("1234567", "1,111"));
    // irregular separators are not extrapolated
    check("1234-56-7", fi("1234567", "1-11-1"));
  }

  @Test
  public void ordinalModifier() {
    check("1st", fi("1", "1;o"));
    check("2nd", fi("2", "1;o"));
    check("3rd", fi("3", "1;o"));
    check("4th", fi("4", "1;o"));
    check("11th", fi("11", "1;o"));
    check("12th", fi("12", "1;o"));
    check("13th", fi("13", "1;o"));
    check("21st", fi("21", "1;o"));
    check("0021st", fi("21", "0001;o"));
    // the parenthesized variation selector is accepted and ignored
    check("1st", fi("1", "1;o(-er)"));
    check("1", fi("1", "1;c"));
  }

  @Test
  public void alphabetic() {
    check("a", fi("1", "a"));
    check("z", fi("26", "a"));
    check("aa", fi("27", "a"));
    check("ab", fi("28", "a"));
    check("B", fi("2", "A"));
    check("AA", fi("27", "A"));
    // zero is outside the alphabetic range: falls back to format token "1"
    check("0", fi("0", "a"));
    check("-b", fi("-2", "a"));
  }

  @Test
  public void roman() {
    check("i", fi("1", "i"));
    check("iv", fi("4", "i"));
    check("ix", fi("9", "i"));
    check("xiv", fi("14", "i"));
    check("XLII", fi("42", "I"));
    check("mcmxcix", fi("1999", "i"));
    check("MMXXVI", fi("2026", "I"));
    check("0", fi("0", "i"));
  }

  @Test
  public void words() {
    check("one", fi("1", "w"));
    check("fifteen", fi("15", "w"));
    check("twenty-one", fi("21", "w"));
    check("one hundred", fi("100", "w"));
    check("one hundred and two", fi("102", "w"));
    check("two thousand and three", fi("2003", "w"));
    check("one thousand two hundred and thirty-four", fi("1234", "w"));
    check("one million", fi("1000000", "w"));
    check("THIRTY-SIX", fi("36", "W"));
    check("Two Thousand and Two", fi("2002", "Ww"));
    check("minus five", fi("-5", "w"));
    check("zero", fi("0", "w"));
  }

  @Test
  public void ordinalWords() {
    check("first", fi("1", "w;o"));
    check("second", fi("2", "w;o"));
    check("third", fi("3", "w;o"));
    check("fourth", fi("4", "w;o"));
    check("fifth", fi("5", "w;o"));
    check("eighth", fi("8", "w;o"));
    check("ninth", fi("9", "w;o"));
    check("twelfth", fi("12", "w;o"));
    check("twentieth", fi("20", "w;o"));
    check("twenty-first", fi("21", "w;o"));
    check("one hundredth", fi("100", "w;o"));
    check("One Hundred and Second", fi("102", "Ww;o"));
  }

  @Test
  public void unknownSequenceFallsBackToDecimal() {
    // unsupported numbering sequences must be formatted with the format token "1"
    check("5", fi("5", "x"));
    check("5", fi("5", "①"));
    // a lone "#" contains no digit, so it is a (unsupported) numbering sequence token, not an
    // invalid decimal digit pattern
    check("1", fi("1", "#"));
  }

  @Test
  public void emptyValueYieldsZeroLengthString() {
    check("", fi("()", "1"));
  }

  @Test
  public void unsupportedLanguageIsNotAnError() {
    // only English is supported; an unrecognized $lang falls back without error
    check("one", "format-integer(1, 'w', 'de')");
    check("one", "format-integer(1, 'w', ())");
  }

  @Test
  public void invalidPictures() {
    for (final String picture : new String[] { "", ",1", "1,", "1,,1", "#1#", "1;x", "0٠" /* mixed families */ }) {
      final QueryException ex = assertThrows(QueryException.class,
                                             () -> new Query(fi("1", picture)).execute(ctx),
                                             picture);
      assertEquals(ErrorCode.ERR_INVALID_FORMAT_INTEGER_PICTURE, ex.getCode(), picture);
    }
  }

  @Test
  public void nonAsciiDigitFamily() {
    // Arabic-Indic digit family: the digit family of the pattern is used in the output
    check("١٥", fi("15", "١"));
    check("٠١٥", fi("15", "١١١"));
  }
}
