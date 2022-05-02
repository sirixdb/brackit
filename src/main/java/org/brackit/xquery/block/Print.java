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
package org.brackit.xquery.block;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.expr.PrintExpr;

import java.io.PrintStream;

/**
 * @author Sebastian Baechle
 */
public class Print implements Block {

  public static class PrintSink extends ChainedSink {
    private final PrintStream out;
    private final Sink sink;
    private int count;

    public PrintSink(Sink sink, PrintStream out) {
      this.sink = sink;
      this.out = out;
    }

    @Override
    protected ChainedSink doFork() {
      return new PrintSink((sink != null) ? sink.fork() : null, out);
    }

    @Override
    protected ChainedSink doPartition(Sink stopAt) {
      return new PrintSink((sink != null) ? sink.partition(stopAt) : null, out);
    }

    @Override
    protected void doOutput(Tuple[] buf, int len) throws QueryException {
      for (int i = 0; i < len; i++) {
        count++;
        out.println(PrintExpr.asString(buf[i]));
      }
      if (sink != null) {
        sink.output(buf, len);
      }
    }

    @Override
    protected void doBegin() throws QueryException {
      if (sink != null) {
        sink.begin();
      }
      out.println(">>>>");
    }

    @Override
    protected void doEnd() throws QueryException {
      if (sink != null) {
        sink.end();
      }
      out.println("<<<<");
    }

    @Override
    protected void doFail() throws QueryException {
      if (sink != null) {
        sink.fail();
      }
    }

    @Override
    protected void doFirstBegin() throws QueryException {
      count = 0;
      out.println("--- ");
    }

    @Override
    protected void doFinalEnd() throws QueryException {
      out.println("---");
      out.print(count);
      out.println(" results");
      out.flush();
    }
  }

  private final PrintStream out;

  public Print(PrintStream out) {
    this.out = out;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    return new PrintSink(sink, out);
  }

  @Override
  public int outputWidth(int initSize) {
    return initSize;
  }
}