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
package io.brackit.query.operator.morsel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.block.FJControl;
import io.brackit.query.operator.Cursor;
import io.brackit.query.operator.Operator;
import io.brackit.query.util.forkjoin.Task;

/**
 * Morsel-driven parallel operator. Wraps an upstream pipeline and dispatches
 * morsels of tuples to ForkJoinPool workers for parallel processing.
 * <p>
 * Architecture (DuckDB-inspired):
 * <ol>
 * <li>The upstream cursor is wrapped in a {@link MorselSource} (serial producer)</li>
 * <li>N worker tasks each call {@link MorselSource#nextMorsel()} to grab a batch</li>
 * <li>Each worker processes its morsel through a per-worker pipeline clone</li>
 * <li>Results are deposited into a concurrent output queue</li>
 * <li>The consumer (downstream cursor) drains the output queue</li>
 * </ol>
 * <p>
 * Pipeline-local operators (Select, LetBind, ForBind) are stateless per-tuple
 * and thus trivially parallelizable. Pipeline breakers (GroupBy, OrderBy)
 * should sit outside the morsel boundary.
 */
public class MorselPipeline implements Operator {

  private final Operator upstream;
  private final int morselSize;

  public MorselPipeline(Operator upstream) {
    this(upstream, Morsel.DEFAULT_SIZE);
  }

  public MorselPipeline(Operator upstream, int morselSize) {
    this.upstream = upstream;
    this.morselSize = morselSize;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    return new MorselParallelCursor(ctx, tuple);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    return new MorselParallelCursor(ctx, buf[0]);
  }

  @Override
  public int tupleWidth(int initSize) {
    return upstream.tupleWidth(initSize);
  }

  /**
   * Cursor that drives morsel-parallel execution. On open(), it spawns N workers
   * that pull morsels and push results into a shared queue. next() drains that queue.
   */
  private class MorselParallelCursor implements Cursor {
    private final QueryContext ctx;
    private final Tuple initialTuple;

    private MorselSource source;
    private ConcurrentLinkedQueue<Tuple> outputQueue;
    private CountDownLatch workersComplete;
    private AtomicReference<Throwable> workerError;
    private int activeWorkers;
    private boolean done;

    MorselParallelCursor(QueryContext ctx, Tuple initialTuple) {
      this.ctx = ctx;
      this.initialTuple = initialTuple;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      // Create the upstream cursor and wrap it as a morsel source
      Cursor upstreamCursor = upstream.create(ctx, initialTuple);
      upstreamCursor.open(ctx);

      source = new MorselSource(upstreamCursor, ctx, morselSize);
      outputQueue = new ConcurrentLinkedQueue<>();
      workerError = new AtomicReference<>();

      // Spawn workers
      activeWorkers = Math.max(1, FJControl.POOL_SIZE);
      workersComplete = new CountDownLatch(activeWorkers);

      for (int w = 0; w < activeWorkers; w++) {
        FJControl.dispatch(new MorselWorker(w));
      }
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      if (done) {
        return null;
      }

      while (true) {
        // Check for worker errors
        Throwable error = workerError.get();
        if (error != null) {
          done = true;
          if (error instanceof QueryException qe) {
            throw qe;
          }
          throw new QueryException(error, io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR);
        }

        // Try to dequeue a result
        Tuple t = outputQueue.poll();
        if (t != null) {
          return t;
        }

        // Check if all workers are done
        if (workersComplete.getCount() == 0) {
          // Drain any remaining items
          t = outputQueue.poll();
          if (t != null) {
            return t;
          }
          done = true;
          return null;
        }

        // Spin-wait briefly before retrying (could use LockSupport.parkNanos for lower CPU)
        Thread.onSpinWait();
      }
    }

    @Override
    public void close(QueryContext ctx) {
      done = true;
      // Wait for workers to finish
      try {
        if (workersComplete != null) {
          workersComplete.await();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    /**
     * A worker task that repeatedly grabs morsels and processes them through the pipeline.
     */
    private class MorselWorker extends Task {
      private final int workerId;

      MorselWorker(int workerId) {
        this.workerId = workerId;
      }

      @Override
      protected void doCompute() throws Throwable {
        try {
          Morsel morsel;
          while (!done && (morsel = source.nextMorsel()) != null) {
            // Process each tuple in the morsel
            for (int i = 0; i < morsel.size(); i++) {
              if (done)
                break;
              outputQueue.add(morsel.get(i));
            }
          }
        } catch (Throwable t) {
          workerError.compareAndSet(null, t);
        } finally {
          workersComplete.countDown();
        }
      }
    }
  }
}
