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
package org.brackit.xquery.expr;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.jdm.Expr;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 *
 */
public class PrintExpr implements Expr {

  private static int maxSize = 20;

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    return evaluateToItem(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    String str = asString(tuple);
    return new Str(str);
  }

  public static String asString(Tuple tuple) throws QueryException {
    StringBuilder out = new StringBuilder();
    int size = tuple.getSize();
    out.append("|");
    for (int i = 0; i < size; i++) {
      out.append(' ');
      Sequence s = tuple.get(i);
      String str = (s != null) ? s.toString() : "()";
      str = shrinkOrPad(str);
      out.append(str);
      out.append(" |");
    }
    String str = out.toString();
    return str;
  }

  private static String shrinkOrPad(String s) {
    int length = s.length();

    if (length == maxSize) {
      return s;
    }
    if (length > maxSize) {
      return s.substring(0, maxSize);
    }

    int toAdd = (maxSize - length);
    char[] result = new char[maxSize];

    int i = 0;
    while (i < (toAdd / 2)) {
      result[i++] = ' ';
    }
    System.arraycopy(s.toCharArray(), 0, result, i, length);
    int j = i + length;
    while (i++ < toAdd) {
      result[j++] = ' ';
    }
    return new String(result);

  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}