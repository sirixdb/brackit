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
package io.brackit.query.util.format;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;

/**
 * Implements the integer formatting rules of <code>fn:format-integer</code> (XQuery F&amp;O 3.1,
 * section 4.6.1). The same machinery is reused by the date/time formatting functions for their
 * numeric presentation modifiers.
 *
 * <p>Supported primary format tokens: decimal-digit-patterns (mandatory digits of any single
 * Unicode decimal digit family, optional digit signs <code>#</code>, and grouping separators),
 * <code>a</code>/<code>A</code> (alphabetic), <code>i</code>/<code>I</code> (roman numerals),
 * and <code>w</code>/<code>W</code>/<code>Ww</code> (English words). Any other token falls back
 * to <code>1</code>, as required by the spec for unsupported numbering sequences.</p>
 *
 * <p>The format modifier (<code>;</code>-separated) supports cardinal/ordinal selection
 * (<code>c</code>/<code>o</code>, with an optionally parenthesized variation string that is
 * accepted and ignored) and accepts <code>a</code>/<code>t</code> (the choice between alphabetic
 * and traditional numbering is implementation-defined; this implementation always uses the
 * alphabetic interpretation). Ordinal numbering is implemented for English (suffixes
 * <code>1st 2nd 3rd ...</code> for decimal digit patterns, and spelled-out ordinals for word
 * tokens); for other tokens the ordinal request is ignored as the spec prescribes.</p>
 *
 * <p>Implementation-defined bounds: roman numerals support 1..999999 and words support values
 * with an absolute value below 10^19; outside these ranges the value is formatted with the
 * fallback token <code>1</code> (the spec only mandates support up to 1000).</p>
 */
public final class IntegerFormatter {

  private static final String[] UNITS = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
      "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
      "nineteen" };

  private static final String[] TENS = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty",
      "ninety" };

  private static final String[] SCALES = { "", "thousand", "million", "billion", "trillion", "quadrillion",
      "quintillion" };

  private static final int[] ROMAN_VALUES = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };

  private static final String[] ROMAN_SYMBOLS = { "m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv",
      "i" };

  private IntegerFormatter() {
  }

  /**
   * Implements the full <code>fn:format-integer</code> picture: a primary format token optionally
   * followed by a <code>;</code>-separated format modifier.
   */
  public static String formatInteger(BigInteger value, String picture) {
    final String primary;
    final String modifier;
    final int semicolon = picture.lastIndexOf(';');
    if (semicolon >= 0) {
      primary = picture.substring(0, semicolon);
      modifier = picture.substring(semicolon + 1);
    } else {
      primary = picture;
      modifier = "";
    }
    if (primary.isEmpty()) {
      throw new QueryException(ErrorCode.ERR_INVALID_FORMAT_INTEGER_PICTURE,
                               "The primary format token of fn:format-integer must not be zero-length");
    }
    if (!modifier.matches("([co](\\(.+\\))?)?[at]?")) {
      throw new QueryException(ErrorCode.ERR_INVALID_FORMAT_INTEGER_PICTURE,
                               "Invalid format modifier in fn:format-integer picture: '%s'",
                               modifier);
    }
    final boolean ordinal = !modifier.isEmpty() && modifier.charAt(0) == 'o';
    return format(value, primary, ordinal);
  }

  /**
   * Formats a value with a single primary format token (no <code>;</code> modifier part). Used
   * directly by the date/time formatting functions, which carry the cardinal/ordinal selection in
   * their own (second) presentation modifier.
   */
  public static String format(BigInteger value, String token, boolean ordinal) {
    if (containsDecimalDigit(token)) {
      return formatDecimalPattern(value, token, ordinal);
    }
    switch (token) {
      case "A":
        return formatBounded(value, ordinal, v -> alphabetic(v).toUpperCase(), v -> v.signum() > 0);
      case "a":
        return formatBounded(value, ordinal, IntegerFormatter::alphabetic, v -> v.signum() > 0);
      case "I":
        return formatBounded(value, ordinal, v -> roman(v).toUpperCase(), IntegerFormatter::inRomanRange);
      case "i":
        return formatBounded(value, ordinal, IntegerFormatter::roman, IntegerFormatter::inRomanRange);
      case "w":
        return formatWords(value, ordinal, WordCase.LOWER);
      case "W":
        return formatWords(value, ordinal, WordCase.UPPER);
      case "Ww":
        return formatWords(value, ordinal, WordCase.TITLE);
      default:
        // Unsupported numbering sequence: the spec mandates falling back to the format token "1".
        return formatDecimalPattern(value, "1", ordinal);
    }
  }

  /**
   * True iff the token contains at least one Unicode decimal digit (category Nd) and must hence be
   * interpreted as a decimal digit pattern.
   */
  public static boolean containsDecimalDigit(String token) {
    for (int i = 0; i < token.length();) {
      final int cp = token.codePointAt(i);
      if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        return true;
      }
      i += Character.charCount(cp);
    }
    return false;
  }

  // ---------------------------------------------------------------------------------------------
  // Decimal digit patterns
  // ---------------------------------------------------------------------------------------------

  /**
   * A parsed decimal-digit-pattern: counts of mandatory/optional digit signs, the digit family,
   * and the grouping separator template.
   */
  static final class DecimalPattern {
    final int mandatory;
    final int optional;
    /** Codepoint of the zero digit of the mandatory digit family ('0' when none specified). */
    final int zeroDigit;
    /** Regular grouping (extrapolated): size of group, or -1 if not regular. */
    final int groupSize;
    final int groupChar;
    /** Irregular grouping: (position, separator codepoint) pairs; empty if none/regular. */
    final List<int[]> separators;

    DecimalPattern(int mandatory, int optional, int zeroDigit, int groupSize, int groupChar, List<int[]> separators) {
      this.mandatory = mandatory;
      this.optional = optional;
      this.zeroDigit = zeroDigit;
      this.groupSize = groupSize;
      this.groupChar = groupChar;
      this.separators = separators;
    }
  }

  static DecimalPattern parseDecimalPattern(String token) {
    int mandatory = 0;
    int optional = 0;
    int zeroDigit = -1;
    boolean mandatorySeen = false;
    boolean lastWasSeparator = false;
    // Separator positions counted as the number of digit signs to the right of the separator.
    final List<int[]> separatorsLeftToRight = new ArrayList<>();
    final List<Integer> digitSignsBeforeSeparator = new ArrayList<>();

    final int length = token.length();
    int digitSigns = 0;
    for (int i = 0; i < length;) {
      final int cp = token.codePointAt(i);
      final int charCount = Character.charCount(cp);
      if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        final int familyZero = cp - Character.digit(cp, 10);
        if (zeroDigit == -1) {
          zeroDigit = familyZero;
        } else if (zeroDigit != familyZero) {
          throw invalidPattern(token, "all mandatory digit signs must belong to the same digit family");
        }
        mandatory++;
        mandatorySeen = true;
        digitSigns++;
        lastWasSeparator = false;
      } else if (cp == '#') {
        if (mandatorySeen) {
          throw invalidPattern(token, "optional digit signs must precede all mandatory digit signs");
        }
        optional++;
        digitSigns++;
        lastWasSeparator = false;
      } else if (isGroupingSeparator(cp)) {
        if (i == 0) {
          throw invalidPattern(token, "a grouping separator must not appear at the start");
        }
        if (lastWasSeparator) {
          throw invalidPattern(token, "adjacent grouping separators are not allowed");
        }
        separatorsLeftToRight.add(new int[] { -1, cp });
        digitSignsBeforeSeparator.add(digitSigns);
        lastWasSeparator = true;
      } else {
        throw invalidPattern(token, "invalid character '" + new String(Character.toChars(cp)) + "'");
      }
      i += charCount;
    }
    if (mandatory == 0) {
      throw invalidPattern(token, "at least one mandatory digit sign is required");
    }
    if (lastWasSeparator) {
      throw invalidPattern(token, "a grouping separator must not appear at the end");
    }

    // Convert "digit signs before separator" into "digit signs to the right of the separator".
    for (int i = 0; i < separatorsLeftToRight.size(); i++) {
      separatorsLeftToRight.get(i)[0] = digitSigns - digitSignsBeforeSeparator.get(i);
    }

    // Determine whether the grouping separators are "regular" and can be extrapolated.
    int groupSize = -1;
    int groupChar = -1;
    if (!separatorsLeftToRight.isEmpty()) {
      boolean regular = true;
      final int candidateChar = separatorsLeftToRight.get(0)[1];
      int minPosition = Integer.MAX_VALUE;
      for (final int[] sep : separatorsLeftToRight) {
        if (sep[1] != candidateChar) {
          regular = false;
          break;
        }
        minPosition = Math.min(minPosition, sep[0]);
      }
      if (regular) {
        final int g = minPosition;
        // every separator position must be a multiple of g
        for (final int[] sep : separatorsLeftToRight) {
          if (sep[0] % g != 0) {
            regular = false;
            break;
          }
        }
        // every positive multiple of g below the number of digit signs must be a separator position
        if (regular) {
          for (int multiple = g; multiple < digitSigns; multiple += g) {
            boolean found = false;
            for (final int[] sep : separatorsLeftToRight) {
              if (sep[0] == multiple) {
                found = true;
                break;
              }
            }
            if (!found) {
              regular = false;
              break;
            }
          }
        }
        if (regular) {
          groupSize = g;
          groupChar = candidateChar;
        }
      }
    }

    return new DecimalPattern(mandatory,
                              optional,
                              zeroDigit == -1 ? '0' : zeroDigit,
                              groupSize,
                              groupChar,
                              groupSize != -1 ? List.of() : separatorsLeftToRight);
  }

  private static QueryException invalidPattern(String token, String reason) {
    return new QueryException(ErrorCode.ERR_INVALID_FORMAT_INTEGER_PICTURE,
                              "Invalid decimal digit pattern '%s': %s",
                              token,
                              reason);
  }

  static boolean isGroupingSeparator(int cp) {
    // any non-alphanumeric character: category other than Nd, Nl, No, Lu, Ll, Lt, Lm, or Lo
    return switch (Character.getType(cp)) {
      case Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER, Character.UPPERCASE_LETTER,
          Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER, Character.MODIFIER_LETTER, Character.OTHER_LETTER ->
        false;
      default -> true;
    };
  }

  private static String formatDecimalPattern(BigInteger value, String token, boolean ordinal) {
    final DecimalPattern pattern = parseDecimalPattern(token);
    final boolean negative = value.signum() < 0;
    final BigInteger abs = value.abs();

    // S1/S2: decimal representation, left-padded with zeroes to the number of mandatory digits
    final StringBuilder digits = new StringBuilder(abs.toString());
    while (digits.length() < pattern.mandatory) {
      digits.insert(0, '0');
    }

    // S3: translate into the requested digit family
    if (pattern.zeroDigit != '0') {
      for (int i = 0; i < digits.length(); i++) {
        // safe: family zero digits of all Nd families used here are BMP codepoints
        digits.setCharAt(i, (char) (pattern.zeroDigit + (digits.charAt(i) - '0')));
      }
    }

    // S4: insert grouping separators (position counted from the right-hand end)
    final StringBuilder grouped = new StringBuilder();
    final int digitCount = digits.length();
    for (int i = 0; i < digitCount; i++) {
      final int positionFromRight = digitCount - i;
      if (i > 0 && separatorAt(pattern, positionFromRight) != -1) {
        grouped.appendCodePoint(separatorAt(pattern, positionFromRight));
      }
      grouped.append(digits.charAt(i));
    }

    // S5: ordinal suffix (English)
    String result = grouped.toString();
    if (ordinal) {
      result += ordinalSuffix(abs);
    }
    return negative ? "-" + result : result;
  }

  private static int separatorAt(DecimalPattern pattern, int positionFromRight) {
    if (pattern.groupSize > 0) {
      return positionFromRight % pattern.groupSize == 0 ? pattern.groupChar : -1;
    }
    for (final int[] sep : pattern.separators) {
      if (sep[0] == positionFromRight) {
        return sep[1];
      }
    }
    return -1;
  }

  private static String ordinalSuffix(BigInteger abs) {
    final int mod100 = abs.mod(BigInteger.valueOf(100)).intValue();
    if (mod100 >= 11 && mod100 <= 13) {
      return "th";
    }
    return switch (mod100 % 10) {
      case 1 -> "st";
      case 2 -> "nd";
      case 3 -> "rd";
      default -> "th";
    };
  }

  // ---------------------------------------------------------------------------------------------
  // Alphabetic and roman sequences
  // ---------------------------------------------------------------------------------------------

  private interface BoundedFormatter {
    String apply(BigInteger value);
  }

  private interface RangeCheck {
    boolean test(BigInteger value);
  }

  private static String formatBounded(BigInteger value, boolean ordinal, BoundedFormatter formatter, RangeCheck range) {
    final BigInteger abs = value.abs();
    if (!range.test(abs) || abs.signum() == 0) {
      // out of the supported range: fall back to format token "1" per spec
      return formatDecimalPattern(value, "1", ordinal);
    }
    // ordinal numbering is not supported for these sequences; the request is ignored (cardinal)
    final String formatted = formatter.apply(abs);
    return value.signum() < 0 ? "-" + formatted : formatted;
  }

  private static boolean inRomanRange(BigInteger abs) {
    return abs.signum() > 0 && abs.compareTo(BigInteger.valueOf(999_999)) <= 0;
  }

  /** Bijective base-26 sequence: a b c ... z aa ab ... */
  private static String alphabetic(BigInteger value) {
    final StringBuilder sb = new StringBuilder();
    BigInteger n = value;
    final BigInteger twentySix = BigInteger.valueOf(26);
    while (n.signum() > 0) {
      n = n.subtract(BigInteger.ONE);
      final BigInteger[] divRem = n.divideAndRemainder(twentySix);
      sb.append((char) ('a' + divRem[1].intValue()));
      n = divRem[0];
    }
    return sb.reverse().toString();
  }

  private static String roman(BigInteger value) {
    int n = value.intValueExact();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < ROMAN_VALUES.length; i++) {
      while (n >= ROMAN_VALUES[i]) {
        sb.append(ROMAN_SYMBOLS[i]);
        n -= ROMAN_VALUES[i];
      }
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------------------------------------
  // English words
  // ---------------------------------------------------------------------------------------------

  private enum WordCase {
    LOWER, UPPER, TITLE
  }

  private static String formatWords(BigInteger value, boolean ordinal, WordCase wordCase) {
    final BigInteger abs = value.abs();
    if (abs.compareTo(BigInteger.TEN.pow(19)) >= 0) {
      // beyond the supported (implementation-defined) range: fall back to format token "1"
      return formatDecimalPattern(value, "1", ordinal);
    }
    String words = cardinalWords(abs);
    if (ordinal) {
      words = toOrdinalWords(words);
    }
    if (value.signum() < 0) {
      words = "minus " + words;
    }
    return switch (wordCase) {
      case LOWER -> words;
      case UPPER -> words.toUpperCase();
      case TITLE -> titleCase(words);
    };
  }

  /**
   * English cardinal number words using the "hundred and two" convention (cf. the spec example
   * "Two Thousand and Three").
   */
  private static String cardinalWords(BigInteger value) {
    if (value.signum() == 0) {
      return UNITS[0];
    }
    // split into groups of three digits, most significant first
    final String digits = value.toString();
    final int groupCount = (digits.length() + 2) / 3;
    final int[] groups = new int[groupCount];
    int end = digits.length();
    for (int i = groupCount - 1; i >= 0; i--) {
      final int start = Math.max(0, end - 3);
      groups[i] = Integer.parseInt(digits.substring(start, end));
      end = start;
    }

    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < groupCount; i++) {
      final int group = groups[i];
      if (group == 0) {
        continue;
      }
      final int scale = groupCount - 1 - i;
      if (sb.length() > 0) {
        // "and" joins a final tens-and-units group to the preceding larger parts
        if (scale == 0 && group < 100) {
          sb.append(" and ");
        } else {
          sb.append(' ');
        }
      }
      sb.append(groupWords(group));
      if (scale > 0) {
        sb.append(' ').append(SCALES[scale]);
      }
    }
    return sb.toString();
  }

  /** Words for 1..999. */
  private static String groupWords(int group) {
    final StringBuilder sb = new StringBuilder();
    final int hundreds = group / 100;
    final int rest = group % 100;
    if (hundreds > 0) {
      sb.append(UNITS[hundreds]).append(" hundred");
      if (rest > 0) {
        sb.append(" and ");
      }
    }
    if (rest > 0) {
      if (rest < 20) {
        sb.append(UNITS[rest]);
      } else {
        sb.append(TENS[rest / 10]);
        if (rest % 10 > 0) {
          sb.append('-').append(UNITS[rest % 10]);
        }
      }
    }
    return sb.toString();
  }

  /** Converts the final word of an English cardinal into its ordinal form. */
  private static String toOrdinalWords(String cardinal) {
    int lastWordStart = 0;
    for (int i = cardinal.length() - 1; i >= 0; i--) {
      final char c = cardinal.charAt(i);
      if (c == ' ' || c == '-') {
        lastWordStart = i + 1;
        break;
      }
    }
    final String head = cardinal.substring(0, lastWordStart);
    final String last = cardinal.substring(lastWordStart);
    final String ordinalLast = switch (last) {
      case "zero" -> "zeroth";
      case "one" -> "first";
      case "two" -> "second";
      case "three" -> "third";
      case "five" -> "fifth";
      case "eight" -> "eighth";
      case "nine" -> "ninth";
      case "twelve" -> "twelfth";
      default -> last.endsWith("y") ? last.substring(0, last.length() - 1) + "ieth" : last + "th";
    };
    return head + ordinalLast;
  }

  /** Title-case: capitalize the first letter of each (space- or hyphen-separated) word but "and". */
  private static String titleCase(String words) {
    final StringBuilder sb = new StringBuilder(words.length());
    boolean atWordStart = true;
    for (int i = 0; i < words.length(); i++) {
      final char c = words.charAt(i);
      if (c == ' ' || c == '-') {
        sb.append(c);
        atWordStart = true;
      } else if (atWordStart) {
        // keep the conjunction "and" in lower-case ("Two Thousand and Three")
        if (c == 'a' && words.startsWith("and", i) && (i + 3 == words.length() || words.charAt(i + 3) == ' ')) {
          sb.append(c);
        } else {
          sb.append(Character.toUpperCase(c));
        }
        atWordStart = false;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
