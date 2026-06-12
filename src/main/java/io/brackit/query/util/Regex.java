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
package io.brackit.query.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Str;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.jdm.Sequence;

/**
 * Backing implementation of predefined functions fn:matches($arg1, $arg2),
 * fn:matches($arg1, $arg2, $arg3), fn:replace($arg1, $arg2, $arg3),
 * fn:replace($arg1, $arg2, $arg3, $arg4), fn:tokenize($arg1, $arg2), and
 * fn:tokenize($arg1, $arg2, $arg3) as per
 * http://www.w3.org/TR/xpath-functions/#func-matches,
 * http://www.w3.org/TR/xpath-functions/#func-replace, and
 * http://www.w3.org/TR/xpath-functions/#func-tokenize. Also note corrections in
 * http://www.w3.org/XML/2007/qt-errata/xpath-functions-errata.html.
 *
 * @author Max Bechtold
 */
public class Regex {

  private final static List<Character> WHITESPACE = Arrays.asList(Character.toChars(0x09)[0],
                                                                  Character.toChars(0x0A)[0],
                                                                  Character.toChars(0x0D)[0],
                                                                  Character.toChars(0x20)[0]);

  public static enum Mode {
    MATCH, REPLACE, TOKENIZE
  }

  ;

  public static Sequence match(Mode mode, String input, String pattern, String replace, String flags)
      throws QueryException {

    // parse flags
    boolean removeWhitespace = false;
    boolean literal = false;
    int flagMask = Pattern.UNIX_LINES;
    if (flags != null) {
      if (flags.contains("q")) {
        literal = true;
        flags = flags.replace("q", "");
      }

      if (flags.contains("x")) {
        removeWhitespace = true;
        flags = flags.replace("x", "");
      }

      if (flags.contains("s")) {
        flagMask |= Pattern.DOTALL;
        flags = flags.replace("s", "");
      }

      if (flags.contains("m")) {
        flagMask |= Pattern.MULTILINE;
        flags = flags.replace("m", "");
      }

      if (flags.contains("i")) {
        flagMask |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        flags = flags.replace("i", "");
      }

      if (!flags.isEmpty()) {
        throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION_FLAGS, "Unknown flags specified.");
      }

      if (literal) {
        // Flag "q": all characters of the pattern represent themselves. It may be combined
        // with "i"; the "m", "s", and "x" flags have no effect in its presence.
        flagMask &= ~(Pattern.DOTALL | Pattern.MULTILINE);
        removeWhitespace = false;
      }
    }

    if (mode != Mode.MATCH) {
      // Pattern.matches runs on the RAW pattern OUTSIDE the compile try/catch below — an invalid
      // pattern (e.g. fn:tokenize('a','[')) threw a raw PatternSyntaxException instead of FORX0002.
      try {
        if (literal ? pattern.isEmpty() : Pattern.matches(pattern, "")) {
          throw (new QueryException(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING, "Pattern matches empty string."));
        }
      } catch (PatternSyntaxException e) {
        throw new QueryException(e, ErrorCode.ERR_INVALID_REGULAR_EXPRESSION);
      }
    }

    if (mode == Mode.TOKENIZE && input.isEmpty()) {
      return null;
    }

    Pattern cpattern;
    try {
      if (literal) {
        cpattern = Pattern.compile(pattern, flagMask | Pattern.LITERAL);
      } else {
        String regex = adaptRegEx(mode, pattern, flagMask, removeWhitespace);
        cpattern = Pattern.compile(regex, flagMask);
      }
    } catch (PatternSyntaxException e) {
      throw (new QueryException(e, ErrorCode.ERR_INVALID_REGULAR_EXPRESSION));
    }
    Matcher matcher = cpattern.matcher(input);

    switch (mode) {
      case MATCH:
        // fn:matches has substring semantics. For translated patterns adaptRegEx pads the
        // pattern with ".*" on both sides; a literal ("q") pattern must not be padded, so
        // search for it instead.
        return new Bool(literal ? matcher.find() : matcher.matches());
      case REPLACE:
        if (literal) {
          // Flag "q": the characters of the replacement string also represent themselves —
          // "$" and "\" have no special significance.
          StringBuffer literalSb = new StringBuffer();
          while (matcher.find()) {
            matcher.appendReplacement(literalSb, Matcher.quoteReplacement(replace));
          }
          matcher.appendTail(literalSb);
          return new Str(literalSb.toString());
        }

        // Disallowed in replacement string: backslash or dollar sign as
        // only character in string, or dollar sign not preceded by
        // backslash and not followed by a digit, or backslash not
        // preceded by backslash and not followed by a dollar sign
        String pat = "(\\$|\\\\|.*[^\\\\]\\$\\D.*|.*[^\\\\]\\\\[^\\$].*)";
        if (Pattern.matches(pat, replace)) {
          throw (new QueryException(ErrorCode.ERR_INVALID_REPLACEMENT_STRING,
                                    "Replacement string matches makes illegal " + "use of chars '\\' or '$'."));
        }

        StringBuffer sb = new StringBuffer();
        try {
          while (matcher.find()) {
            matcher.appendReplacement(sb, replace);
          }
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
          // A trailing lone '$' or '\' slips past the validation regex above (its alternatives
          // require a following char); appendReplacement then throws a raw Java exception — map
          // it to FORX0004 instead.
          throw new QueryException(e,
                                   ErrorCode.ERR_INVALID_REPLACEMENT_STRING,
                                   "Invalid replacement string: " + replace);
        }
        matcher.appendTail(sb);

        return new Str(sb.toString());

      case TOKENIZE:
        String[] tokens = cpattern.split(input, -1);
        Str[] items = new Str[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
          items[i] = new Str(tokens[i]);
        }
        return new ItemSequence(items);
      default:
        return null;
    }
  }

  /**
   * Method for adapting Java regex functions to XQuery functions
   * requirements. This includes pattern matching using substrings (Java
   * patterns are implicitly anchored at begin/end of string), error raising
   * for invalid back references (which Java silently skips), a more sensitive
   * whitespace removal (unlike Java keep whitespace in character classes),
   * and no support for pure, i.e. uncapturing groups. Additionally, this
   * method checks for dangling round and square brackets (which are not
   * allowed in XQuery, but treated as literals in Java).</br> This method
   * optionally removes all whitespace except for whitespace in character
   * classes, see flag 'x' in http://www.w3.org/TR/xpath-functions/#flags
   */
  private static String adaptRegEx(Mode mode, String regex, int flagMask, boolean removeWhitespace)
      throws QueryException {
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    boolean groupStart = false;
    int completeGroups = 0;
    int backRef = 0;
    int charClassDepth = 0;
    int groupDepth = 0;

    for (char c : regex.toCharArray()) {
      // Single carry-over chokepoint for whitespace-removal (x-flag) mode: every character except
      // to-be-stripped whitespace (outside character classes) is appended exactly once here. The
      // structural branches below previously each re-appended their character — a branch that
      // forgot silently corrupted the rebuilt pattern.
      if (removeWhitespace) {
        if (charClassDepth == 0 && WHITESPACE.contains(c)) {
          // Strip — and don't touch the boolean flags (an escape stays pending across stripped
          // whitespace, matching the spec's remove-before-parse semantics).
          continue;
        }
        sb.append(c);
      }

      if (escaped) {
        if (backRef == 0 && c == '0') {
          throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, "Reference to group 0 not allowed");
        } else if (c >= '0' && c <= '9') {
          if (charClassDepth > 0) {
            throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
                                     "Back references in character class expressions" + " are disallowed.");
          }
          backRef = backRef * 10 + Integer.parseInt(Character.toString(c));
          continue;
        }
      }

      if (backRef > 0) {
        // Check back reference that just ended
        if (backRef > completeGroups) {
          throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
                                   "Back reference to nonexisting or unfinished group.");
        } else {
          backRef = 0;
          escaped = false;
        }
      }

      if (c == '\\' && !escaped) {
        escaped = true;
        groupStart = false;
        continue;
      }

      if (c == '(' && !escaped) {
        groupStart = true;
        groupDepth++;
        escaped = false;
        continue;
      }

      if (c == '?' && !escaped && groupStart) {
        throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
                                 "Pure groups are not supported in XQuery regular expressions.");
      } else if (c == ')' && !escaped) {
        if (--groupDepth < 0) {
          throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, "Invalid sequence of brackets.");
        }
        completeGroups++;
      } else if (c == '[' && !escaped) {
        charClassDepth++;
      } else if (c == ']' && !escaped) {
        if (--charClassDepth < 0) {
          throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, "Invalid sequence of brackets.");
        }
      }

      groupStart = false;
      escaped = false;
    }

    // Check for trailing '\' (only valid with subsequent characters)
    if (escaped && backRef == 0) {
      throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, "Trailing backslash character in pattern.");
    }

    // Check back reference if that was last token in pattern
    if (backRef > 0 && backRef > completeGroups) {
      throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
                               "Back reference to nonexisting or unfinished group.");
    }

    // Check for dangling brackets
    if (charClassDepth != 0 || groupDepth != 0) {
      throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, "Pattern contains dangling brackets.");
    }

    if (!removeWhitespace) {
      sb.append(regex);
    }

    if (mode == Mode.MATCH) {
      // Adapt for XQuery substring matching by extending pattern. An EMPTY pattern is legal (it
      // matches every string) — guard the charAt() calls so it does not throw
      // StringIndexOutOfBoundsException.
      if (sb.isEmpty() || sb.charAt(0) != '^' || ((flagMask & Pattern.MULTILINE) == Pattern.MULTILINE)) {
        if ((flagMask & Pattern.DOTALL) == Pattern.DOTALL) {
          sb.insert(0, ".*");
        } else {
          sb.insert(0, "(?s:.*)");
        }
      }

      if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '$' || ((flagMask & Pattern.MULTILINE) == Pattern.MULTILINE)) {
        if ((flagMask & Pattern.DOTALL) == Pattern.DOTALL) {
          sb.append(".*");
        } else {
          sb.append("(?s:.*)");
        }
      }
    }

    return sb.toString();
  }
}
