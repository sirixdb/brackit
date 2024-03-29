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
package io.brackit.query.function.fn;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.module.StaticContext;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.function.AbstractFunction;

/**
 * @author Max Bechtold
 */
public class Encode extends AbstractFunction {
  public enum Mode {
    ENCODE_FOR_URI, IRI_TO_URI, HTML_URI
  }

  private final static List<Character> IRI_ILLEGAL_ASCII = Arrays.asList('<', '>', '\"', '{', '}', '|', '\\', '^', '`');

  private Mode mode;

  public Encode(QNm name, Mode mode, Signature signature) {
    super(name, signature, true);
    this.mode = mode;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args[0] == null) {
      return Str.EMPTY;
    }

    StringBuilder sb = new StringBuilder();
    String str = ((Str) args[0]).stringValue();
    int copy = 0;

    switch (mode) {
      case ENCODE_FOR_URI:
        for (int i = 0; i < str.length(); i++) {
          int codePoint = str.codePointAt(i);

          // Test if c lies outside of unreserved character range (cf. RFC
          // 3986 sec. 2.3)
          if ((codePoint < 0x41 || codePoint > 0x5A) && (codePoint < 0x61 || codePoint > 0x7A) && codePoint != 0x2D
              && codePoint != 0x2E && codePoint != 0x5F && codePoint != 0x7E) {
            if (copy < i) {
              sb.append(str.substring(copy, i));
            }

            int charCount = Character.charCount(codePoint);
            String seq = String.valueOf(Character.toChars(codePoint));
            byte[] bytes = null;
            try {
              bytes = seq.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
            }

            String code = "";
            for (byte b : bytes) {
              code += Integer.toHexString(b & 0xff);
            }
            code = code.toUpperCase();

            if (code.length() % 2 == 1) {
              code = '0' + code;
            }

            int z = 0;
            while (z < code.length()) {
              sb.append('%');
              sb.append(code.substring(z, z += 2));
            }

            if (charCount == 2) {
              i++;
            }
            copy = i + 1;
          }

        }
        break;

      case IRI_TO_URI:
        for (int i = 0; i < str.length(); i++) {
          int codePoint = str.codePointAt(i);

          if (codePoint < 0x20 || codePoint > 0x7E || IRI_ILLEGAL_ASCII.contains(str.charAt(i))) {
            if (copy < i) {
              sb.append(str.substring(copy, i));
            }

            int charCount = Character.charCount(codePoint);
            String seq = String.valueOf(Character.toChars(codePoint));
            byte[] bytes = null;
            try {
              bytes = seq.getBytes(StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
            }

            String code = "";
            for (byte b : bytes) {
              code += Integer.toHexString(b & 0xff);
            }
            code = code.toUpperCase();

            if (code.length() % 2 == 1) {
              code = '0' + code;
            }

            int z = 0;
            while (z < code.length()) {
              sb.append('%');
              sb.append(code.substring(z, z += 2));
            }

            if (charCount == 2) {
              i++;
            }
            copy = i + 1;
          }
        }
        break;
    }

    if (copy < str.length()) {
      sb.append(str.substring(copy));
    }

    return new Str(sb.toString());

  }
}
