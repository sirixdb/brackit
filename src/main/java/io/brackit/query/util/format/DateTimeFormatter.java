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
import java.util.Set;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.DTD;
import io.brackit.query.atomic.TimeInstant;
import io.brackit.query.jdm.XMLChar;
import io.brackit.query.util.Whitespace;

/**
 * Implements the date/time picture string of <code>fn:format-dateTime</code>,
 * <code>fn:format-date</code>, and <code>fn:format-time</code> (XQuery F&amp;O 3.1, section 9.8).
 *
 * <p>Faithfully implemented: literal text with <code>[[</code>/<code>]]</code> escapes; the
 * component specifiers <code>Y M D d F W w H h P m s f Z z C E</code> with decimal-digit-pattern,
 * name (<code>n</code>/<code>N</code>/<code>Nn</code>), roman, alphabetic, and word presentations;
 * second presentation modifiers <code>c</code>/<code>o</code> (cardinal/ordinal) and
 * <code>a</code>/<code>t</code> (for timezones, <code>t</code> selects "Z" for UTC); width
 * modifiers; year truncation (modulo 10^maxWidth); fractional second truncation/padding; and the
 * numeric, military (<code>Z</code>), and GMT-prefixed (<code>z</code>) timezone presentations.</p>
 *
 * <p>Implementation-defined choices (documented here once, asserted by the unit tests):</p>
 * <ul>
 * <li>The only supported language is English; any other non-empty <code>$language</code> falls
 * back to English with the spec-mandated <code>[Language: en]</code> output prefix.</li>
 * <li>The supported calendars are <code>AD</code> (default) and <code>ISO</code>. Other calendar
 * designators from the spec's list fall back to AD with the spec-mandated
 * <code>[Calendar: AD]</code> output prefix; a calendar QName in a namespace is likewise treated
 * as unsupported (fallback), and any other value is rejected with err:FOFD1340.</li>
 * <li><code>$place</code> is accepted by the function signatures but not used (no
 * geographical/timezone database); the spec permits falling back to the default place for
 * unrecognized values. The argument is therefore consumed in the function wrapper and never
 * reaches this formatter.</li>
 * <li>Day-of-week and week numbers use the ISO 8601 conventions (Monday=1, first-Thursday rule)
 * for both calendars; weeks-in-month follow the spec's Thursday rule.</li>
 * <li>The year component formats the absolute value of the year (per the spec's component table);
 * the era component yields <code>AD</code>/<code>BC</code> in the AD calendar (brackit's value
 * space has no year zero, so <code>-0044</code> is 44 BC) and the ISO convention (empty string or
 * <code>-</code>) in the ISO calendar. Era and calendar names ignore the name-case modifiers
 * (their output is implementation-defined).</li>
 * <li>Timezone names (<code>[ZN]</code>) are not available without a timezone database; the
 * spec-mandated fallback format <code>01:01</code> is used instead.</li>
 * <li>Grouping separators are not supported inside fractional-second patterns and are rejected
 * loudly with err:FOFD1340 (the spec leaves several of these combinations
 * implementation-defined).</li>
 * </ul>
 */
public final class DateTimeFormatter {

  /** All component specifiers defined by the spec, in table order. */
  private static final String ALL_COMPONENTS = "YMDdFWwHhPmsfZzCE";

  public enum Source {
    DATE_TIME(ALL_COMPONENTS, "xs:dateTime"), DATE("YMDdFWwCEZz", "xs:date"), TIME("HhPmsfZzC", "xs:time");

    final String components;
    final String typeName;

    Source(String components, String typeName) {
      this.components = components;
      this.typeName = typeName;
    }
  }

  private enum CalendarKind {
    AD, ISO
  }

  /** Spec-mandated output prefix when a requested calendar falls back to AD. */
  private static final String CALENDAR_FALLBACK_PREFIX = "[Calendar: AD]";

  /** Default (and fallback) numeric timezone presentation: signed hours and minutes. */
  private static final String DEFAULT_TIMEZONE_PRESENTATION = "01:01";

  /** Calendar designators enumerated by the spec (all valid; only AD and ISO are supported). */
  private static final Set<String> KNOWN_CALENDARS = Set.of("AD",
                                                            "AH",
                                                            "AME",
                                                            "AM",
                                                            "AP",
                                                            "AS",
                                                            "BE",
                                                            "CB",
                                                            "CE",
                                                            "CL",
                                                            "CS",
                                                            "EE",
                                                            "FE",
                                                            "ISO",
                                                            "JE",
                                                            "KE",
                                                            "KY",
                                                            "ME",
                                                            "MS",
                                                            "NS",
                                                            "OS",
                                                            "RS",
                                                            "SE",
                                                            "SH",
                                                            "SS",
                                                            "TE",
                                                            "VS",
                                                            "VE");

  private static final String[] MONTHS = { "January", "February", "March", "April", "May", "June", "July", "August",
      "September", "October", "November", "December" };

  /** Index 0 = Monday, matching the ISO day-of-week derived from the Julian day number. */
  private static final String[] DAYS = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

  private DateTimeFormatter() {
  }

  public static String format(Source source, TimeInstant value, String picture, String language, String calendar) {
    final StringBuilder out = new StringBuilder();
    appendLanguagePrefix(out, language);
    final CalendarKind calendarKind = resolveCalendar(out, calendar);
    appendFormattedPicture(out, source, value, calendarKind, picture);
    return out.toString();
  }

  private static void appendLanguagePrefix(StringBuilder out, String language) {
    if (language == null) {
      return;
    }
    final String lang = Whitespace.collapseTrimOnly(language);
    if (!lang.isEmpty() && !"en".equalsIgnoreCase(lang) && !lang.toLowerCase().startsWith("en-")) {
      // fallback to the supported language, identifying it as the spec requires
      out.append("[Language: en]");
    }
  }

  private static CalendarKind resolveCalendar(StringBuilder out, String calendar) {
    if (calendar == null) {
      return CalendarKind.AD;
    }
    final String cal = Whitespace.collapseTrimOnly(calendar);
    if (cal.isEmpty()) {
      return CalendarKind.AD;
    }
    if (cal.startsWith("Q{") || cal.indexOf(':') >= 0) {
      // namespaced EQName calendars are implementation-defined territory and unsupported here,
      // so the spec-mandated fallback representation is used
      out.append(CALENDAR_FALLBACK_PREFIX);
      return CalendarKind.AD;
    }
    if (!XMLChar.isNCName(cal)) {
      throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                               "Invalid calendar value '%s': not a valid EQName",
                               cal);
    }
    if ("AD".equals(cal)) {
      return CalendarKind.AD;
    }
    if ("ISO".equals(cal)) {
      return CalendarKind.ISO;
    }
    if (KNOWN_CALENDARS.contains(cal)) {
      out.append(CALENDAR_FALLBACK_PREFIX);
      return CalendarKind.AD;
    }
    throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE, "Unknown calendar designator '%s'", cal);
  }

  private static void appendFormattedPicture(StringBuilder out, Source source, TimeInstant value,
      CalendarKind calendarKind, String picture) {
    final int length = picture.length();
    int i = 0;
    while (i < length) {
      final char c = picture.charAt(i);
      if (c == '[') {
        if (isDoubled(picture, i)) {
          out.append('[');
          i += 2;
        } else {
          final int close = picture.indexOf(']', i + 1);
          if (close < 0) {
            throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                     "Invalid picture string '%s': unclosed variable marker",
                                     picture);
          }
          out.append(formatMarker(source, value, calendarKind, picture.substring(i + 1, close)));
          i = close + 1;
        }
      } else if (c == ']') {
        if (isDoubled(picture, i)) {
          out.append(']');
          i += 2;
        } else {
          throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                   "Invalid picture string '%s': ']' in literal text must be doubled",
                                   picture);
        }
      } else {
        out.append(c);
        i++;
      }
    }
  }

  /** True iff the character at {@code i} is immediately repeated (escaped bracket). */
  private static boolean isDoubled(String picture, int i) {
    return i + 1 < picture.length() && picture.charAt(i + 1) == picture.charAt(i);
  }

  // ---------------------------------------------------------------------------------------------
  // Variable markers
  // ---------------------------------------------------------------------------------------------

  private static final class Marker {
    char component;
    String first = "";
    char second = 0;
    boolean hasWidth;
    int minWidth = -1;
    int maxWidth = -1;
  }

  private static String formatMarker(Source source, TimeInstant value, CalendarKind calendarKind, String rawMarker) {
    // whitespace within a variable marker is ignored
    final StringBuilder collapsed = new StringBuilder(rawMarker.length());
    for (int i = 0; i < rawMarker.length(); i++) {
      final char c = rawMarker.charAt(i);
      if (!Character.isWhitespace(c)) {
        collapsed.append(c);
      }
    }
    final String body = collapsed.toString();
    if (body.isEmpty()) {
      throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE, "Empty variable marker in picture string");
    }

    final Marker marker = new Marker();
    marker.component = body.charAt(0);
    if (ALL_COMPONENTS.indexOf(marker.component) < 0) {
      throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                               "Invalid component specifier '%s' in picture string",
                               marker.component);
    }
    if (source.components.indexOf(marker.component) < 0) {
      throw new QueryException(ErrorCode.ERR_DATETIME_COMPONENT_NOT_AVAILABLE,
                               "Component specifier '%s' is not available in a value of type %s",
                               marker.component,
                               source.typeName);
    }

    String rest = body.substring(1);

    // the last comma (if any) introduces the width modifier; earlier commas are grouping separators
    final int lastComma = rest.lastIndexOf(',');
    if (lastComma >= 0) {
      parseWidth(marker, rest.substring(lastComma + 1));
      rest = rest.substring(0, lastComma);
    }

    // split first/second presentation modifiers
    if (rest.length() > 1) {
      final char last = rest.charAt(rest.length() - 1);
      if (last == 'a' || last == 't' || last == 'c' || last == 'o') {
        marker.second = last;
        rest = rest.substring(0, rest.length() - 1);
      }
    }
    marker.first = rest;

    return formatComponent(value, calendarKind, marker);
  }

  private static void parseWidth(Marker marker, String width) {
    if (!width.matches("(\\d+|\\*)(-(\\d+|\\*))?")) {
      throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                               "Invalid width modifier ',%s' in picture string",
                               width);
    }
    marker.hasWidth = true;
    final int dash = width.indexOf('-');
    final String min = dash < 0 ? width : width.substring(0, dash);
    final String max = dash < 0 ? "*" : width.substring(dash + 1);
    if (!"*".equals(min)) {
      marker.minWidth = Integer.parseInt(min);
      if (marker.minWidth < 1) {
        throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                 "Invalid width modifier ',%s': minimum width must be at least one",
                                 width);
      }
    }
    if (!"*".equals(max)) {
      marker.maxWidth = Integer.parseInt(max);
      if (marker.maxWidth < 1 || (marker.minWidth != -1 && marker.maxWidth < marker.minWidth)) {
        throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                 "Invalid width modifier ',%s': maximum width must be at least one and not less than the minimum width",
                                 width);
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Component dispatch
  // ---------------------------------------------------------------------------------------------

  private static String formatComponent(TimeInstant value, CalendarKind calendarKind, Marker marker) {
    return switch (marker.component) {
      case 'Y' -> formatYear(value, marker);
      case 'M' -> formatMonth(value, marker);
      case 'D' -> formatIntegerComponent(value.getDay(), marker, "1");
      case 'd' -> formatIntegerComponent(dayOfYear(value), marker, "1");
      case 'F' -> formatDayOfWeek(value, marker);
      case 'W' -> formatIntegerComponent(weekOfYear(value), marker, "1");
      case 'w' -> formatIntegerComponent(weekOfMonth(value), marker, "1");
      case 'H' -> formatIntegerComponent(value.getHours(), marker, "1");
      case 'h' -> formatIntegerComponent(value.getHours() % 12 == 0 ? 12 : value.getHours() % 12, marker, "1");
      case 'P' -> formatName(value.getHours() < 12 ? "am" : "pm", marker, "n");
      case 'm' -> formatIntegerComponent(value.getMinutes(), marker, "01");
      case 's' -> formatIntegerComponent(value.getMicros() / 1_000_000, marker, "01");
      case 'f' -> formatFractionalSeconds(value.getMicros() % 1_000_000, marker);
      case 'Z', 'z' -> formatTimezone(value, marker);
      // the output of the calendar component is implementation-defined: the designator in use
      case 'C' -> applyWidthToName(calendarKind == CalendarKind.ISO ? "ISO" : "AD", marker);
      case 'E' -> formatEra(value, calendarKind, marker);
      // unreachable: the component specifier has been validated against ALL_COMPONENTS
      default -> throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                          "Invalid component specifier '%s'",
                                          marker.component);
    };
  }

  private static String formatMonth(TimeInstant value, Marker marker) {
    if (isNameForm(effectiveFirst(marker, "1"))) {
      return formatName(MONTHS[value.getMonth() - 1], marker, "n");
    }
    return formatIntegerComponent(value.getMonth(), marker, "1");
  }

  private static String formatDayOfWeek(TimeInstant value, Marker marker) {
    if (isNameForm(effectiveFirst(marker, "n"))) {
      return formatName(DAYS[dayOfWeekIndex(value)], marker, "n");
    }
    // the index is 0..6, so the int addition cannot overflow; widen first anyway
    return formatIntegerComponent(dayOfWeekIndex(value) + 1L, marker, "n");
  }

  /**
   * Implementation-defined era names: AD/BC for the AD calendar; the ISO convention is a minus
   * sign for negative years and a zero-length string otherwise.
   */
  private static String formatEra(TimeInstant value, CalendarKind calendarKind, Marker marker) {
    final String era;
    if (calendarKind == CalendarKind.ISO) {
      era = value.getYear() < 0 ? "-" : "";
    } else {
      era = value.getYear() < 0 ? "BC" : "AD";
    }
    return applyWidthToName(era, marker);
  }

  private static String effectiveFirst(Marker marker, String defaultPresentation) {
    return marker.first.isEmpty() ? defaultPresentation : marker.first;
  }

  private static boolean isNameForm(String first) {
    return "n".equals(first) || "N".equals(first) || "Nn".equals(first);
  }

  // ---------------------------------------------------------------------------------------------
  // Integer-valued components
  // ---------------------------------------------------------------------------------------------

  private static String formatIntegerComponent(long componentValue, Marker marker, String defaultPresentation) {
    String first = effectiveFirst(marker, defaultPresentation);
    if (isNameForm(first)) {
      // a name form was requested for a component we cannot output by name:
      // the spec requires using the default presentation modifier instead
      first = IntegerFormatter.containsDecimalDigit(defaultPresentation) ? defaultPresentation : "1";
    }
    final boolean ordinal = marker.second == 'o';
    if (IntegerFormatter.containsDecimalDigit(first)) {
      final String pattern = adjustPatternToMinWidth(first, marker);
      return formatViaIntegerFormatter(BigInteger.valueOf(componentValue), pattern, ordinal);
    }
    // roman/alphabetic/words/unknown tokens: never truncated, padded to the minimum width
    final String result = formatViaIntegerFormatter(BigInteger.valueOf(componentValue), first, ordinal);
    return padRight(result, marker.minWidth);
  }

  private static String formatYear(TimeInstant value, Marker marker) {
    // the year component formats the absolute value of the year (per the spec's component
    // table) and the era component carries the BC/AD distinction
    long year = Math.abs((long) value.getYear());

    final String first = effectiveFirst(marker, "1");
    int n = -1;
    if (marker.maxWidth > 0) {
      n = marker.maxWidth;
    } else if (IntegerFormatter.containsDecimalDigit(first)) {
      final int digitSigns = countDigitSigns(first);
      if (digitSigns >= 2) {
        n = digitSigns;
      }
    }
    if (n > 0 && n < 19) {
      long modulus = 1;
      for (int i = 0; i < n; i++) {
        modulus *= 10;
      }
      year %= modulus;
    }
    return formatIntegerComponent(year, marker, "1");
  }

  private static int countDigitSigns(String pattern) {
    int count = 0;
    int i = 0;
    while (i < pattern.length()) {
      final int cp = pattern.codePointAt(i);
      if (cp == '#' || Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        count++;
      }
      i += Character.charCount(cp);
    }
    return count;
  }

  /** Mandatory digit count, the family's zero digit, and grouping presence of a digit pattern. */
  private record DigitPatternInfo(int mandatory, int zeroDigit, boolean hasGrouping) {
  }

  private static DigitPatternInfo scanDigitPattern(String pattern) {
    int mandatory = 0;
    int zeroDigit = '0';
    boolean hasGrouping = false;
    int i = 0;
    while (i < pattern.length()) {
      final int cp = pattern.codePointAt(i);
      if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        if (mandatory == 0) {
          zeroDigit = cp - Character.digit(cp, 10);
        }
        mandatory++;
      } else if (cp != '#') {
        hasGrouping = true;
      }
      i += Character.charCount(cp);
    }
    return new DigitPatternInfo(mandatory, zeroDigit, hasGrouping);
  }

  /**
   * Applies a width modifier to a decimal digit pattern: optional digit signs are converted to
   * mandatory ones starting from the right, then mandatory digit signs are prepended until the
   * minimum width is reached. The maximum width is ignored for integer-valued components.
   */
  private static String adjustPatternToMinWidth(String pattern, Marker marker) {
    if (!marker.hasWidth || marker.minWidth <= 0) {
      return pattern;
    }
    final DigitPatternInfo info = scanDigitPattern(pattern);
    if (info.hasGrouping() || info.mandatory() >= marker.minWidth) {
      // leave the pattern untouched: combining width modifiers with grouping separators is
      // implementation-defined
      return pattern;
    }
    int mandatory = info.mandatory();
    final StringBuilder sb = new StringBuilder(pattern);
    for (int i = sb.length() - 1; i >= 0 && mandatory < marker.minWidth; i--) {
      if (sb.charAt(i) == '#') {
        sb.setCharAt(i, (char) info.zeroDigit());
        mandatory++;
      }
    }
    while (mandatory < marker.minWidth) {
      sb.insert(0, (char) info.zeroDigit());
      mandatory++;
    }
    return sb.toString();
  }

  private static String formatViaIntegerFormatter(BigInteger value, String token, boolean ordinal) {
    try {
      return IntegerFormatter.format(value, token, ordinal);
    } catch (final QueryException e) {
      if (ErrorCode.ERR_INVALID_FORMAT_INTEGER_PICTURE.equals(e.getCode())) {
        // a syntactically invalid presentation modifier is a picture syntax error here. The
        // message is passed to the (Throwable, QNm, Object) constructor, which concatenates and
        // never re-format-interprets it (the already-formatted message may contain '%').
        throw new QueryException(e, ErrorCode.ERR_INVALID_DATETIME_PICTURE, e.getMessage());
      }
      throw e;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Names (months, days, am/pm)
  // ---------------------------------------------------------------------------------------------

  /** Formats a title-case base name according to the requested name form and width. */
  private static String formatName(String titleCaseName, Marker marker, String defaultPresentation) {
    final String first = effectiveFirst(marker, defaultPresentation);
    final String form = isNameForm(first) ? first : defaultPresentation;
    String name = switch (form) {
      case "N" -> titleCaseName.toUpperCase();
      case "Nn" -> Character.toUpperCase(titleCaseName.charAt(0)) + titleCaseName.substring(1).toLowerCase();
      default -> titleCaseName.toLowerCase();
    };
    return applyWidthToName(name, marker);
  }

  private static String applyWidthToName(String name, Marker marker) {
    if (marker.maxWidth > 0 && name.length() > marker.maxWidth) {
      // conventional English month/day abbreviations are prefixes of the full name
      name = name.substring(0, marker.maxWidth);
    }
    return padRight(name, marker.minWidth);
  }

  private static String padRight(String value, int minWidth) {
    if (minWidth <= 0 || value.length() >= minWidth) {
      return value;
    }
    final StringBuilder sb = new StringBuilder(value);
    while (sb.length() < minWidth) {
      sb.append(' ');
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------------------------------------
  // Fractional seconds
  // ---------------------------------------------------------------------------------------------

  /** Mandatory and optional digit counts plus the digit family of a fractional pattern. */
  private record FractionalPattern(int mandatory, int optional, int zeroDigit) {
  }

  /** A fractional pattern mirrors a decimal digit pattern: mandatory digits first, then '#'. */
  private static FractionalPattern parseFractionalPattern(String pattern) {
    int mandatory = 0;
    int optional = 0;
    int zeroDigit = '0';
    int i = 0;
    while (i < pattern.length()) {
      final int cp = pattern.codePointAt(i);
      if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        if (optional > 0) {
          throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                   "Invalid fractional seconds pattern '%s': mandatory digits must precede optional digit signs",
                                   pattern);
        }
        if (mandatory == 0) {
          zeroDigit = cp - Character.digit(cp, 10);
        }
        mandatory++;
      } else if (cp == '#') {
        optional++;
      } else {
        // grouping separators within fractional seconds are not supported: fail loudly rather
        // than risk a wrong rendering
        throw new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                                 "Unsupported character '%s' in fractional seconds pattern '%s'",
                                 new String(Character.toChars(cp)),
                                 pattern);
      }
      i += Character.charCount(cp);
    }
    return new FractionalPattern(mandatory, optional, zeroDigit);
  }

  private static String formatFractionalSeconds(int micros, Marker marker) {
    String pattern = effectiveFirst(marker, "1");
    if (!IntegerFormatter.containsDecimalDigit(pattern)) {
      // a presentation modifier without digits is implementation-defined: use the default
      pattern = "1";
    }
    final FractionalPattern parsed = parseFractionalPattern(pattern);
    final boolean singleMandatoryDigit = parsed.mandatory() == 1 && parsed.optional() == 0;
    int mandatory = parsed.mandatory();
    int optional = parsed.optional();
    int total = mandatory + optional;
    if (marker.hasWidth) {
      if (marker.minWidth > mandatory) {
        // optional signs become mandatory (from the left), further mandatory signs are appended
        final int converted = Math.min(optional, marker.minWidth - mandatory);
        optional -= converted;
        mandatory = marker.minWidth;
        total = mandatory + optional;
      }
      if (marker.maxWidth > 0 && total < marker.maxWidth) {
        total = marker.maxWidth;
      }
    }

    // six (zero-padded) decimal places, then drop insignificant trailing zeroes
    final StringBuilder digits = new StringBuilder(Integer.toString(micros));
    while (digits.length() < 6) {
      digits.insert(0, '0');
    }
    int significant = digits.length();
    while (significant > 0 && digits.charAt(significant - 1) == '0') {
      significant--;
    }
    if (!marker.hasWidth && singleMandatoryDigit) {
      // a single mandatory digit pattern extends to the actual precision of the value
      total = Math.max(1, significant);
    }

    // truncate towards zero, then pad with trailing zeroes up to the mandatory width
    final StringBuilder out = new StringBuilder(digits.substring(0, Math.min(significant, total)));
    while (out.length() < mandatory) {
      out.append('0');
    }
    if (parsed.zeroDigit() != '0') {
      for (int i = 0; i < out.length(); i++) {
        out.setCharAt(i, (char) (parsed.zeroDigit() + (out.charAt(i) - '0')));
      }
    }
    return out.toString();
  }

  // ---------------------------------------------------------------------------------------------
  // Timezones
  // ---------------------------------------------------------------------------------------------

  private static String formatTimezone(TimeInstant value, Marker marker) {
    final DTD timezone = value.getTimezone();
    String presentation = effectiveFirst(marker, DEFAULT_TIMEZONE_PRESENTATION);
    final String prefix = marker.component == 'z' ? "GMT" : "";

    if (timezone == null) {
      // a missing timezone produces no output, except in the military presentation
      if ("Z".equals(presentation)) {
        return padRight(prefix + "J", marker.minWidth);
      }
      return "";
    }

    final int hours = timezone.getHours();
    final int minutes = timezone.getMinutes();
    final boolean zeroOffset = hours == 0 && minutes == 0;
    final boolean negative = timezone.isNegative() && !zeroOffset;

    if ("Z".equals(presentation)) {
      final char letter = militaryTimezoneLetter(hours, minutes, zeroOffset, negative);
      if (letter != 0) {
        return padRight(prefix + letter, marker.minWidth);
      }
      // offsets without a military letter fall back to the 01:01 presentation
      presentation = DEFAULT_TIMEZONE_PRESENTATION;
    }

    if ("N".equals(presentation)) {
      // timezone names require a timezone database; the spec-mandated fallback is 01:01
      presentation = DEFAULT_TIMEZONE_PRESENTATION;
    }

    final String body;
    if (marker.second == 't' && zeroOffset) {
      body = "Z";
    } else {
      body = formatNumericTimezone(presentation, negative, hours, minutes);
    }
    return padRight(prefix + body, marker.minWidth);
  }

  /**
   * The military timezone letter (Z = +00:00, A..M = +01:00..+12:00 skipping J, N..Y =
   * -01:00..-12:00), or 0 when the offset has no letter.
   */
  private static char militaryTimezoneLetter(int hours, int minutes, boolean zeroOffset, boolean negative) {
    if (minutes != 0 || hours > 12) {
      return 0;
    }
    if (zeroOffset) {
      return 'Z';
    }
    if (negative) {
      return (char) ('N' + hours - 1);
    }
    final char candidate = (char) ('A' + hours - 1);
    // the letter J is reserved for local time and skipped in the military sequence
    return candidate >= 'J' ? (char) (candidate + 1) : candidate;
  }

  /** The digit groups of a numeric timezone presentation: hours, optional separator, minutes. */
  private record TimezoneGroups(String hours, String separator, String minutes) {
  }

  private static TimezoneGroups splitTimezonePresentation(String presentation) {
    final StringBuilder hourGroup = new StringBuilder();
    final StringBuilder minuteGroup = new StringBuilder();
    String separator = null;
    int i = 0;
    while (i < presentation.length()) {
      final int cp = presentation.codePointAt(i);
      if (Character.getType(cp) == Character.DECIMAL_DIGIT_NUMBER) {
        (separator == null ? hourGroup : minuteGroup).appendCodePoint(cp);
      } else if (separator == null && !hourGroup.isEmpty() && IntegerFormatter.isGroupingSeparator(cp)) {
        separator = new String(Character.toChars(cp));
      } else {
        throw unsupportedTimezonePresentation(presentation);
      }
      i += Character.charCount(cp);
    }
    if (hourGroup.isEmpty() || (separator != null && minuteGroup.isEmpty())) {
      throw unsupportedTimezonePresentation(presentation);
    }
    return new TimezoneGroups(hourGroup.toString(), separator, minuteGroup.toString());
  }

  private static String formatNumericTimezone(String presentation, boolean negative, int hours, int minutes) {
    final TimezoneGroups groups = splitTimezonePresentation(presentation);
    final String sign = negative ? "-" : "+";
    if (groups.separator() != null) {
      // e.g. 01:01 or 0.00: hours and minutes, always both
      return sign + IntegerFormatter.format(BigInteger.valueOf(hours), groups.hours(), false) + groups.separator()
          + IntegerFormatter.format(BigInteger.valueOf(minutes), groups.minutes(), false);
    }
    final int digitCount = groups.hours().length();
    if (digitCount <= 2) {
      // hours only; minutes are appended (colon-separated) when the offset is not whole hours
      String result = sign + IntegerFormatter.format(BigInteger.valueOf(hours), groups.hours(), false);
      if (minutes != 0) {
        result += ":" + IntegerFormatter.format(BigInteger.valueOf(minutes), "01", false);
      }
      return result;
    }
    if (digitCount <= 4) {
      // combined hours and minutes without separator, e.g. -0500
      return sign + IntegerFormatter.format(BigInteger.valueOf(hours * 100L + minutes), groups.hours(), false);
    }
    throw unsupportedTimezonePresentation(presentation);
  }

  private static QueryException unsupportedTimezonePresentation(String presentation) {
    return new QueryException(ErrorCode.ERR_INVALID_DATETIME_PICTURE,
                              "Unsupported timezone presentation modifier '%s'",
                              presentation);
  }

  // ---------------------------------------------------------------------------------------------
  // Calendar arithmetic (proleptic Gregorian, via Julian day numbers)
  // ---------------------------------------------------------------------------------------------

  /**
   * Fliegel–Van Flandern Julian day number. Like the engine's date arithmetic in
   * {@code AbstractTimeInstant}, the stored year value is fed in unchanged.
   */
  private static long julianDayNumber(int year, int month, int day) {
    final int a = (14 - month) / 12;
    final long y = year + 4800L - a;
    final int m = month + 12 * a - 3;
    return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L;
  }

  /** Inverse of {@link #julianDayNumber}: {year, month, day}. */
  private static int[] gregorianFromJdn(long jdn) {
    final long a = jdn + 32044;
    final long b = (4 * a + 3) / 146097;
    final long c = a - 146097 * b / 4;
    final long d = (4 * c + 3) / 1461;
    final long e = c - 1461 * d / 4;
    final long m = (5 * e + 2) / 153;
    final int day = (int) (e - (153 * m + 2) / 5 + 1);
    final int month = (int) (m + 3 - 12 * (m / 10));
    final int year = (int) (100 * b + d - 4800 + m / 10);
    return new int[] { year, month, day };
  }

  /** 0 = Monday ... 6 = Sunday. */
  private static int dayOfWeekIndex(TimeInstant value) {
    return (int) Math.floorMod(julianDayNumber(value.getYear(), value.getMonth(), value.getDay()), 7);
  }

  private static int dayOfYear(TimeInstant value) {
    return (int) (julianDayNumber(value.getYear(), value.getMonth(), value.getDay()) - julianDayNumber(value.getYear(),
                                                                                                       1,
                                                                                                       1)) + 1;
  }

  /** ISO 8601 week of year: the week (Monday-Sunday) containing the first Thursday is week 1. */
  private static int weekOfYear(TimeInstant value) {
    final long jdn = julianDayNumber(value.getYear(), value.getMonth(), value.getDay());
    final long thursday = jdn + (3 - Math.floorMod(jdn, 7));
    final int[] thursdayDate = gregorianFromJdn(thursday);
    final long jan1 = julianDayNumber(thursdayDate[0], 1, 1);
    return (int) ((thursday - jan1) / 7) + 1;
  }

  /**
   * Week of month: per the spec, a Monday-Sunday week belongs to the month containing its
   * Thursday, and the weeks of a month are numbered from 1.
   */
  private static int weekOfMonth(TimeInstant value) {
    final long jdn = julianDayNumber(value.getYear(), value.getMonth(), value.getDay());
    final long thursday = jdn + (3 - Math.floorMod(jdn, 7));
    final int[] thursdayDate = gregorianFromJdn(thursday);
    return (thursdayDate[2] - 1) / 7 + 1;
  }
}
