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
package org.brackit.xquery.jdm;

/**
 * @author Sebastian Baechle
 */
public class XMLChar {

  // TODO: Howto add range [#x10000-#xEFFFF]?
  public static final String NAME_START_CHAR_WITHOUT_COLON_PATTERN =
      "[A-Z]|_|[a-z]|[\u00C0-\u00D6]|[\u00D8-\u00F6]|[\u00F8-\u02FF]|[\u0370-\u037D]|[\u037F-\u1FFF]|[\u200C-\u200D]|[\u2070-\u218F]|[\u2C00-\u2FEF]|[\u3001-\uD7FF]|[\uF900-\uFDCF]|[\uFDF0-\uFFFD]";

  public static final String NAME_CHAR_OTHER_PATTERN = "|-|\\.|[0-9]|\u00B7|[\u0300-\u036F]|[\u203F-\u2040]";

  public static final String NAME_START_CHAR_PATTERN = ":|" + NAME_START_CHAR_WITHOUT_COLON_PATTERN;

  public static final String NAME_CHAR_PATTERN = NAME_START_CHAR_PATTERN + "|" + NAME_CHAR_OTHER_PATTERN;

  public static final String NMTOKEN_PATTERN = "(" + NAME_CHAR_PATTERN + ")+";

  public static final String NAME_PATTERN = "(" + NAME_START_CHAR_PATTERN + ")(" + NAME_CHAR_PATTERN + ")*";

  public static final String NCNAME_PATTERN = "(" + NAME_START_CHAR_WITHOUT_COLON_PATTERN + "|"
      + NAME_CHAR_OTHER_PATTERN + ")+";

  public static boolean isNameStartChar(int c) {
    return c == ':' || 'A' <= c && c <= 'Z' || c == '_' || 'a' <= c && c <= 'z' || '\u00C0' <= c && c <= '\u00D6'
        || '\u00D8' <= c && c <= '\u00F6' || '\u00F8' <= c && c <= '\u02FF' || '\u0370' <= c && c <= '\u037D'
        || '\u037F' <= c && c <= '\u1FFF' || '\u200C' <= c && c <= '\u200D' || '\u2070' <= c && c <= '\u218F'
        || '\u2C00' <= c && c <= '\u2FEF' || '\u3001' <= c && c <= '\uD7FF' || '\uF900' <= c && c <= '\uFDCF'
        || '\uFDF0' <= c && c <= '\uFFFD' || 0x10000 <= c && c <= 0xEFFFF;
  }

  public static boolean isNameChar(int c) {
    return isNameStartChar(c) || c == '-' || c == '.' || '0' <= c && c <= '9' || c == '\u00B7' || '\u0300' <= c && c
        <= '\u036F' || '\u203F' <= c && c <= '\u2040';
  }

  public static boolean isWS(int c) {
    return c == ' ' || c == '\r' || c == '\t' || c == '\n';
  }

  public static boolean isChar(int c) {
    return c == '\t' || c == '\n' || c == '\r' || '\u0020' <= c && c <= '\uD7FF' || '\uE000' <= c && c <= '\uFFFD'
        || 0x10000 <= c && c <= 0xEFFFF;
  }

  public static String toString(int c) {
    return new String(Character.toChars(c));
  }

  public static boolean isQName(String s) {
    int pos = s.indexOf(':');
    if (pos == -1) {
      return isNCName(s);
    } else if (pos > 0 && pos < s.length() - 1) {
      return isNCName(s, 0, pos) && isNCName(s, pos + 1, s.length());
    }
    return false;
  }

  public static boolean isNCName(String s) {
    return isNCName(s, 0, s.length());
  }

  public static boolean isNCName(String s, int start, int end) {
    if (end <= start) {
      return false;
    }
    int i = start;
    char c = s.charAt(i++);
    if (c == ':' || c == '.' || !isNameStartChar(c)) {
      return false;
    }
    while (i < end) {
      c = s.charAt(i++);
      if (c == ':' || c == '.' || !isNameChar(c)) {
        return false;
      }
    }
    return true;
  }
}
