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
package org.brackit.xquery.compiler.profiler;

import java.util.concurrent.atomic.AtomicInteger;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.operator.Cursor;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.util.dot.DotNode;

/**
 * @author Sebastian Baechle
 */
public class ProfileOperator extends ProfilingNode implements Operator {
  private static final AtomicInteger idSource = new AtomicInteger(1);

  private final int id = idSource.getAndIncrement();

  private Operator op;

  private long total;

  private int openCnt;

  private int closeCnt;

  private int deliverCnt;

  private class StatOpCursor implements Cursor {
    final Cursor c;

    long time;

    public StatOpCursor(Cursor c) {
      this.c = c;
    }

    @Override
    public void close(QueryContext ctx) {
      c.close(ctx);
      closeCnt++;
      total += time;
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      long start = System.nanoTime();
      Tuple next = c.next(ctx);
      long end = System.nanoTime();
      time += (end - start);
      if (next != null)
        deliverCnt++;
      return next;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      openCnt++;
      c.open(ctx);
    }
  }

  public ProfileOperator() {
  }

  public void setOp(Operator op) {
    this.op = op;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    return new StatOpCursor(op.create(ctx, tuple));
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    return new StatOpCursor(op.create(ctx, buf, len));
  }

  @Override
  public int tupleWidth(int initSize) {
    return op.tupleWidth(initSize);
  }

  public String toString() {
    return op.getClass().getSimpleName();
  }

  @Override
  protected void addFields(DotNode node) {
    node.addRow("operator", op.getClass().getSimpleName());
    node.addRow("open / close", openCnt + " /" + closeCnt);
    node.addRow("delivered", deliverCnt);
    node.addRow("total time [ms]", total / 1000000);
    node.addRow("avg. time [ms]", (deliverCnt > 0) ? ((double) total) / (1000000 * deliverCnt) : -1);
  }

  @Override
  protected String getName() {
    return op.getClass().getSimpleName() + "_" + id;
  }
}