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
package org.brackit.xquery.operator;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class Count extends Check implements Operator {
  private final Operator in;
  private boolean bind = true;

  private class CountCursor implements Cursor {
    private final Cursor c;
    private IntNumeric pos;
    private Tuple t;

    public CountCursor(Cursor c) {
      this.c = c;
    }

    @Override
    public void close(QueryContext ctx) {
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) {
      Tuple prev = t;
      t = c.next(ctx);

      if (t == null) {
        return null;
      }

      if (check) {
        if (dead(t)) {
          pos = Int32.ZERO;
          return t.concat((Sequence) null);
        }
        if (prev == null || separate(prev, t)) {
          pos = Int32.ZERO;
        }
      }

      return t.concat(pos = pos.inc());
    }

    @Override
    public void open(QueryContext ctx) {
      c.open(ctx);
      t = null;
      pos = Int32.ZERO;
    }
  }

  public Count(Operator in) {
    this.in = in;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    return bind ? new CountCursor(in.create(ctx, tuple)) : in.create(ctx, tuple);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    return bind ? new CountCursor(in.create(ctx, buf, len)) : in.create(ctx, buf, len);
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize) + 1;
  }

  public void bind(boolean bind) {
    this.bind = bind;
  }
}
