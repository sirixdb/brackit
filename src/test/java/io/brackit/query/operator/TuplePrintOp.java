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
package io.brackit.query.operator;

import java.io.PrintStream;

import io.brackit.query.jdm.Sequence;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * @author Sebastian Baechle
 */
public class TuplePrintOp implements Cursor {
  private final Cursor in;

  private final PrintStream out;

  private int maxSize = 20;

  private int count;

  public TuplePrintOp(Cursor in, PrintStream out) {
    this.in = in;
    this.out = out;
  }

  @Override
  public void close(QueryContext ctx) {
    in.close(ctx);
    out.println("---");
    out.print(count);
    out.println(" results");
  }

  @Override
  public Tuple next(QueryContext ctx) throws QueryException {
    Tuple next = in.next(ctx);
    if (next != null) {
      count++;
      int size = next.getSize();
      out.print("|");
      for (int i = 0; i < size; i++) {
        out.print(' ');
        Sequence sequence = next.get(i);
        String s = asString(ctx, sequence);
        s = shrinkOrPad(s);
        out.print(s);
        out.print(" |");
      }
      out.print('\n');
    }
    return next;
  }

  public String asString(QueryContext ctx, Sequence sequence) {
    return sequence.toString();
  }

  private String shrinkOrPad(String s) {
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
  public void open(QueryContext ctx) throws QueryException {
    in.open(ctx);
    count = 0;
  }
}
