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
package org.brackit.xquery.function.fn;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.Signature;

/**
 * Implementation of predefined function fn:translate($arg1, $arg2, $arg3) as
 * per http://www.w3.org/TR/xpath-functions/#func-translate.
 *
 * @author Max Bechtold
 */
public class StringTranslate extends AbstractFunction {

  public StringTranslate(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args[0] == null) {
      return Str.EMPTY;
    }

    String str = ((Str) args[0]).stringValue();
    String map = ((Str) args[1]).stringValue();
    String trans = ((Str) args[2]).stringValue();
    StringBuilder sb = new StringBuilder(str.length());

    if (map.isEmpty()) {
      return Str.EMPTY;
    }

    int copy = 0;

    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      int index = map.indexOf(c);
      if (index != -1 && index < trans.length()) {
        if (copy < i) {
          sb.append(str.substring(copy, i));
        }
        sb.append(trans.charAt(index));
        copy = i + 1;
      }
    }

    if (copy < str.length()) {
      sb.append(str.substring(copy));
    }

    return new Str(sb.toString());
  }

}
