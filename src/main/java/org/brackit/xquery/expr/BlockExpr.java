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
import org.brackit.xquery.block.Block;
import org.brackit.xquery.block.FJControl;
import org.brackit.xquery.block.MutexSink;
import org.brackit.xquery.block.SerialValve;
import org.brackit.xquery.block.Sink;
import org.brackit.xquery.sequence.FlatteningSequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.util.forkjoin.Task;
import org.brackit.xquery.util.join.FastList;
import org.brackit.xquery.util.serialize.SerializationHandler;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class BlockExpr implements Expr {

  private final Block block;
  private final Expr expr;
  private final boolean ordered;

  public BlockExpr(Block block, Expr expr, boolean ordered) {
    this.block = block;
    this.expr = expr;
    this.ordered = ordered;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple t) throws QueryException {
    Return rs = new Return(ctx, expr);
    Sink end = ordered ? new SerialValve(FJControl.PERMITS, rs) : rs;
    Sink start = block.create(ctx, end);

    EvalBlock task = new EvalBlock(t, start);
    FJControl.POOL.submit(task).join();

    return rs.asSequence();
  }

  public void serialize(QueryContext ctx, Tuple t, SerializationHandler handler) throws QueryException {
    SerializerReturn rs = new SerializerReturn(ctx, expr, handler);
    Sink end = ordered ? new SerialValve(FJControl.PERMITS, rs) : rs;
    Sink start = block.create(ctx, end);

    EvalBlock task = new EvalBlock(t, start);
    FJControl.POOL.submit(task).join();
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple t) throws QueryException {
    return ExprUtil.asItem(evaluate(ctx, t));
  }

  @Override
  public boolean isUpdating() {
    // TODO
    return false;
  }

  @Override
  public boolean isVacuous() {
    // TODO
    return false;
  }

  private static final class EvalBlock extends Task {
    private final Tuple t;
    private final Sink start;

    private EvalBlock(Tuple t, Sink start) {
      this.t = t;
      this.start = start;
    }

    @Override
    public void compute() throws QueryException {
      start.begin();
      try {
        start.output(new Tuple[] { t }, 1);
        start.end();
      } catch (QueryException e) {
        start.fail();
        throw e;
      }
    }
  }

  private static class SerializerReturn extends MutexSink {
    final QueryContext ctx;
    final Expr expr;
    final SerializationHandler handler;

    private static class Result extends Out {
      final Tuple[] buf;
      final int len;

      private Result(Tuple[] buf, int len) {
        this.buf = buf;
        this.len = len;
      }
    }

    public SerializerReturn(QueryContext ctx, Expr expr, SerializationHandler handler) {
      this.ctx = ctx;
      this.expr = expr;
      this.handler = handler;
    }

    @Override
    public Sink partition(Sink stopAt) {
      return fork();
    }

    @Override
    protected Out doPreOutput(Tuple[] buf, int len) throws QueryException {
      int nlen = 0;
      for (int i = 0; i < len; i++) {
        Sequence s = expr.evaluate(ctx, buf[i]);
        if (s != null) {
          buf[nlen++] = s;
        }
      }
      return new Result(buf, nlen);
    }

    @Override
    protected void doOutput(Out out) throws QueryException {
      Result res = (Result) out;
      for (Tuple t : res.buf) {
        if (t != null) {
          Sequence s = (Sequence) t;
          if (s instanceof Item) {
            handler.item((Item) s);
          } else {
            try (Iter it = s.iterate()) {
              for (Item i = it.next(); i != null; i = it.next()) {
                handler.item(i);
              }
            }
          }
        }
      }
    }

    @Override
    protected void doBegin() throws QueryException {
      handler.begin();
    }

    @Override
    protected void doEnd() throws QueryException {
      handler.end();
    }
  }

  private static class Return extends MutexSink {
    final QueryContext ctx;
    final Expr expr;
    final FastList<Sequence> buf;

    private static class Result extends Out {
      final Tuple[] buf;
      final int len;

      private Result(Tuple[] buf, int len) {
        this.buf = buf;
        this.len = len;
      }
    }

    public Return(QueryContext ctx, Expr expr) {
      this.ctx = ctx;
      this.expr = expr;
      this.buf = new FastList<>();
    }

    @Override
    public Sink partition(Sink stopAt) {
      return fork();
    }

    @Override
    protected Out doPreOutput(Tuple[] buf, int len) throws QueryException {
      int nlen = 0;
      for (int i = 0; i < len; i++) {
        Sequence s = expr.evaluate(ctx, buf[i]);
        if (s != null) {
          buf[nlen++] = s;
        }
      }
      return new Result(buf, nlen);
    }

    @Override
    protected void doOutput(Out out) throws QueryException {
      Result res = (Result) out;
      this.buf.addAllSafe(res.buf, 0, res.len);
    }

    @Override
    protected void doBegin() {
    }

    @Override
    protected void doEnd() throws QueryException {
    }

    Sequence asSequence() {
      return new FlatteningSequence() {
        final int len = buf.getSize();

        @Override
        protected Sequence sequence(int pos) throws QueryException {
          return pos < len ? buf.get(pos) : null;
        }
      };
    }
  }
}