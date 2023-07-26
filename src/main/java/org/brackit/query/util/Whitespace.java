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
package org.brackit.query.util;

/**
 * @author Sebastian Baechle
 */
public class Whitespace {
  /**
   * Apply XML Schema Part 2 Whitespace replace facet: Replace all #xA, #xD
   * and #x9 with #x20
   */
  public static String replace(String str) {
    // make a fast scan first to check if normalization is necessary
    int end = str.length();
    int pos = 0;

    char c;
    while ((pos < end) && ((((c = str.charAt(pos)) == '\r') || (c == '\t') || (c == '\n'))))
      pos++;

    if (pos == end) {
      return str;
    }

    char[] chars = str.toCharArray();
    StringBuilder buf = new StringBuilder(end);
    buf.append(chars, 0, pos);

    while (pos < end) {
      c = chars[pos++];
      if ((c == '\r') || (c == '\t') || (c == '\n')) {
        buf.append(' ');
      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  /**
   * Apply XML Schema Part 2 Whitespace replace facet: Replace all #xA, #xD
   * and #x9 with #x20 and trim leading and trailing #x20's. Use this when
   * trimming is sufficient, e.g,. for casting from string, when intermediate
   * WS will create an error anyways.
   */
  public static String collapseTrimOnly(String str) {
    int start = 0;
    int length = str.length();
    int end = length;

    char c;
    while ((start < length) && ((((c = str.charAt(start))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      start++;
    while ((end > start) && ((((c = str.charAt(end - 1))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      end--;

    return ((start == 0) && (end == length)) ? str : str.substring(start, end);
  }

  /**
   * Apply XML Schema Part 2 Whitespace replace facet: Replace all #xA, #xD
   * and #x9 with #x20
   */
  public static String collapse(String str) {
    // make a fast scan first to check if normalization is necessary
    int length = str.length();
    int end = length;
    int start = 0;

    // trim leading and trailing WS
    char c;
    while ((start < length) && ((((c = str.charAt(start))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      start++;
    while ((end > start) && ((((c = str.charAt(end - 1))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      end--;

    int pos = start;
    char p = 0;
    while ((pos < end) && (((c = str.charAt(pos)) != '\r') && (c != '\t') && (c != '\n') && ((c != ' ') || (c != p)))) {
      p = c;
      pos++;
    }

    if (pos == end) {
      return ((start == 0) && (end == length)) ? str : str.substring(start, end);
    }

    char[] chars = str.toCharArray();
    StringBuilder buf = new StringBuilder(end);
    buf.append(chars, start, pos - start);

    while (pos < end) {
      c = chars[pos++];
      if (((c == '\r') || (c == '\t') || (c == '\n') || (c == ' '))) {
        if (p != ' ') {
          buf.append(' ');
        }
        p = ' ';
      } else {
        buf.append(c);
        p = c;
      }
    }
    return buf.toString();
  }

  /**
   * Apply XML Schema Part 2 Whitespace replace facet: Replace all #xA, #xD
   * and #x9 with #x20 but directly remove all #x20
   */
  public static String fullcollapse(String str) {
    // make a fast scan first to check if normalization is necessary
    int length = str.length();
    int end = length;
    int start = 0;

    // trim leading and trailing WS
    char c;
    while ((start < length) && ((((c = str.charAt(start))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      start++;
    while ((end > start) && ((((c = str.charAt(end - 1))) == ' ') || (c == '\r') || (c == '\t') || (c == '\n')))
      end--;

    int pos = start;
    while ((pos < end) && (((c = str.charAt(pos)) != '\r') && (c != '\t') && (c != '\n') && (c != ' '))) {
      pos++;
    }

    if (pos == end) {
      return ((start == 0) && (end == length)) ? str : str.substring(start, end);
    }

    char[] chars = str.toCharArray();
    StringBuilder buf = new StringBuilder(end);
    buf.append(chars, start, pos);

    while (pos < end) {
      c = chars[pos++];
      if ((c != ' ') && (c != '\r') && (c != '\t') && (c != '\n')) {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  /**
   * Trim boundary whitespace (#xA, #x20, #xD, #x9)
   */
  public static String trimBoundaryWS(String str, boolean fromHead, boolean fromTail) {
    int start = 0;
    int end = str.length();
    int trimTo = str.length();
    char c;
    if (fromHead) {
      while ((start < trimTo) && (((c = str.charAt(start)) == ' ') || (c == '\n') || (c == '\t') || (c == '\r'))) {
        start++;
      }
    }
    if (fromTail) {
      while ((trimTo > start) && (((c = str.charAt(trimTo - 1)) == ' ') || (c == '\n') || (c == '\t') || (c == '\r')))
        trimTo--;
    }

    return ((start != 0) || (end != trimTo)) ? str.substring(start, trimTo) : str;
  }

  /**
   * Normalize end-of-line according to XML 1.1
   */
  public static String normalizeXML11(String str) {
    // make a fast scan first to check if normalization is necessary
    int end = str.length();
    int pos = 0;

    char c;
    while ((pos < end) && ((c = str.charAt(pos)) != '\r') && (c != '\u0085') && (c != '\u2028'))
      pos++;

    if (pos == end) {
      return str;
    }

    char[] chars = str.toCharArray();
    StringBuilder buf = new StringBuilder(end);
    buf.append(chars, 0, pos);

    while (pos < end) {
      c = chars[pos++];
      if (c == '\r') {
        if ((pos < end) && ((chars[pos] == '\n') || (chars[pos] == '\u0085'))) {
          pos++;
        }
        buf.append('\n');
      } else if ((c == '\u0085') || (c == '\u2028')) {
        buf.append('\n');
      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  /**
   * Normalize end-of-line according to XML 1.0
   */
  public static String normalizeXML10(String str) {
    // make a fast scan first to check if normalization is necessary
    int end = str.length();
    int pos = 0;

    char c;
    while ((pos < end) && ((c = str.charAt(pos)) != '\r'))
      pos++;

    if (pos == end) {
      return str;
    }

    char[] chars = str.toCharArray();
    StringBuilder buf = new StringBuilder(end);
    buf.append(chars, 0, pos);

    while (pos < end) {
      c = chars[pos++];
      if (c == '\r') {
        if ((pos < end) && (chars[pos] == '\n')) {
          pos++;
        }
        buf.append('\n');
      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  public static boolean isWS(String str) {
    int len = str.length();
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if ((c != ' ') && (c != '\n') && (c != '\t') && (c != '\r')) {
        return false;
      }
    }
    return true;
  }
}
