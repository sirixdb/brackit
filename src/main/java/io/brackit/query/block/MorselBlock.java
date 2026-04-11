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
package io.brackit.query.block;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.operator.morsel.Morsel;
import io.brackit.query.util.forkjoin.Task;

/**
 * Morsel-driven parallel block. Buffers incoming tuples into morsel-sized
 * batches and dispatches each morsel to a ForkJoinPool worker that pushes
 * it through the downstream block chain.
 * <p>
 * This block sits at the boundary between the scan (which produces tuples)
 * and the rest of the pipeline. It splits the serial tuple stream into
 * parallel morsel-sized work units.
 * <p>
 * The downstream sink is forked per worker, ensuring thread safety.
 */
public class MorselBlock implements Block {

  private final int morselSize;

  public MorselBlock() {
    this(Morsel.DEFAULT_SIZE);
  }

  public MorselBlock(int morselSize) {
    this.morselSize = morselSize;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    return new MorselSink(ctx, sink);
  }

  @Override
  public int outputWidth(int inputWidth) {
    return inputWidth;
  }

  private class MorselSink implements Sink {
    private final QueryContext ctx;
    private final Sink downstream;

    // Buffer for accumulating a morsel
    private Tuple[] buffer;
    private int bufLen;

    // Track dispatched workers
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final java.util.concurrent.ConcurrentLinkedQueue<Task> tasks =
        new java.util.concurrent.ConcurrentLinkedQueue<>();

    MorselSink(QueryContext ctx, Sink downstream) {
      this.ctx = ctx;
      this.downstream = downstream;
      this.buffer = new Tuple[morselSize];
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      // Accumulate into morsel buffer
      for (int i = 0; i < len; i++) {
        buffer[bufLen++] = buf[i];
        if (bufLen == morselSize) {
          dispatchMorsel();
        }
      }
    }

    @Override
    public Sink fork() {
      return this; // fan-in: all forks write to the same morsel buffer
    }

    @Override
    public Sink partition(Sink stopAt) {
      return this;
    }

    @Override
    public void begin() throws QueryException {
      bufLen = 0;
    }

    @Override
    public void end() throws QueryException {
      // Flush remaining tuples
      if (bufLen > 0) {
        dispatchMorsel();
      }

      // Wait for all dispatched workers to complete
      Task task;
      while ((task = tasks.poll()) != null) {
        task.join();
      }

      // Propagate any worker error
      Throwable t = error.get();
      if (t != null) {
        if (t instanceof QueryException qe)
          throw qe;
        throw new QueryException(t, io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    @Override
    public void fail() throws QueryException {
      downstream.fail();
    }

    private void dispatchMorsel() throws QueryException {
      // Capture current buffer
      Tuple[] morsel = Arrays.copyOf(buffer, bufLen);
      int morselLen = bufLen;
      bufLen = 0;
      buffer = new Tuple[morselSize];

      Sink workerSink = downstream.fork();

      Task task = new Task() {
        @Override
        protected void doCompute() throws Throwable {
          try {
            workerSink.begin();
            workerSink.output(morsel, morselLen);
            workerSink.end();
          } catch (Throwable t) {
            error.compareAndSet(null, t);
            try {
              workerSink.fail();
            } catch (Throwable ignored) {
            }
          }
        }
      };
      tasks.add(task);
      FJControl.dispatch(task);
    }
  }
}
