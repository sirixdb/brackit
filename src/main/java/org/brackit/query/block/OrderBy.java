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
package org.brackit.query.block;

import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;
import org.brackit.query.block.MutexSink.Out;
import org.brackit.query.util.sort.Ordering;
import org.brackit.query.util.sort.Ordering.OrderModifier;
import org.brackit.query.jdm.Expr;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Stream;

/**
 * @author Sebastian Baechle
 */
public class OrderBy implements Block {

  final Expr[] orderByExprs;
  final OrderModifier[] modifier;

  public OrderBy(Expr[] orderByExprs, OrderModifier[] modifier) {
    this.orderByExprs = orderByExprs;
    this.modifier = modifier;
  }

  @Override
  public int outputWidth(int initSize) {
    return initSize;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    return new OrderBySink(sink, ctx);
  }

  private static class Sortable extends Out {
    final Sequence[][] sortKeys;
    final Tuple[] buf;
    final int len;

    public Sortable(Sequence[][] sortKeys, Tuple[] buf, int len) {
      this.sortKeys = sortKeys;
      this.buf = buf;
      this.len = len;
    }
  }

  private class OrderBySink extends MutexSink {
    final Sink sink;
    final QueryContext ctx;
    final Ordering sort;

    OrderBySink(Sink sink, QueryContext ctx) {
      this.sink = sink;
      this.ctx = ctx;
      this.sort = new Ordering(orderByExprs, modifier);
    }

    public Sink partition(Sink stopAt) {
      return new OrderBySink(sink.partition(stopAt), ctx);
    }

    @Override
    protected Out doPreOutput(Tuple[] buf, int len) throws QueryException {
      Sequence[][] sortKeys = new Sequence[buf.length][];
      for (int i = 0; i < len; i++) {
        sortKeys[i] = sort.sortKeys(ctx, buf[i]);
      }
      return new Sortable(sortKeys, buf, len);
    }

    @Override
    protected void doOutput(Out out) throws QueryException {
      Sortable s = (Sortable) out;
      for (int i = 0; i < s.len; i++) {
        sort.add(s.sortKeys[i], s.buf[i]);
      }
    }

    @Override
    protected void doEnd() throws QueryException {
      Stream<? extends Tuple> s = sort.sorted();
      try {
        sink.begin();
        Tuple t;
        int bufSize = 20;
        Tuple[] buf = new Tuple[bufSize];
        int len = 0;
        while ((t = s.next()) != null) {
          buf[len++] = t;
          if (len == bufSize) {
            sink.output(buf, len);
            buf = new Tuple[bufSize];
            len = 0;
          }
        }
        if (len > 0) {
          sink.output(buf, len);
        }
        sink.end();
      } finally {
        sort.clear();
        s.close();
      }
    }

    @Override
    protected void doFail() throws QueryException {
      sink.fail();
      if (sort != null) {
        sort.clear();
      }
    }
  }
}