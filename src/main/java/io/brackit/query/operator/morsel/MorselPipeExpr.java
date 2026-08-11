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

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.SplittableSequence;
import io.brackit.query.operator.Cursor;
import io.brackit.query.operator.Operator;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.util.ExprUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Morsel-driven parallel replacement for {@link io.brackit.query.expr.PipeExpr}.
 *
 * <p>The pipeline it replaces is pull-based and single-consumer: one thread asks the operator chain
 * for a tuple, evaluates the return expression on it, and repeats. Profiling that loop on a 3.5 M
 * record JSON corpus put 58-59 % of its time in the leaf scan and the rest above it, with under 6 %
 * in Brackit's own code — so the work worth spreading is the scan and the per-tuple navigation it
 * feeds, not the plumbing. This class spreads both: <b>every worker runs the entire chain</b>,
 * including the return expression, over its own piece of the source.
 *
 * <p>The piece comes from {@link SplittableSequence}, which the storage layer implements in
 * whatever unit is cheap for it. That is the part an earlier morsel design got wrong by wrapping a
 * serial cursor in a {@code synchronized} dispenser: the workers copied tuples a single thread had
 * already produced, so the scan never left that thread and the boundary added lock contention, a
 * queue node per tuple and a spinning consumer on top of it. Measured, that arrangement ran 3-8x
 * SLOWER than no parallelism at all.
 *
 * <p><b>Order is not preserved.</b> Splits are consumed as they complete, so this is only installed
 * for pipelines a caller has already declared order-insensitive.
 *
 * <p>When the source does not implement {@link SplittableSequence}, or is too small to be worth
 * splitting, iteration falls back to exactly the serial pipeline — so installing this can cost the
 * evaluation of the bind expression, and nothing else.
 *
 * @author The SirixDB authors
 */
public final class MorselPipeExpr implements Expr {

  /**
   * Items handed over per queue element.
   *
   * <p>Batched because the previous design enqueued one node per tuple: on this corpus that is
   * 3.5 M queue nodes of pure garbage, which showed up as GC rather than as the hand-off it is.
   */
  private static final int BATCH = Integer.getInteger("brackit.morsel.batch", 1024);

  /** Batches in flight before producers block, per worker. Bounds memory on a large result. */
  private static final int QUEUE_DEPTH = Integer.getInteger("brackit.morsel.queueDepth", 4);

  /** Default worker count; a split source may ask for fewer. */
  private static final int WORKERS = Integer.getInteger("brackit.morsel.workers",
                                                        Runtime.getRuntime().availableProcessors());

  /**
   * A dedicated pool rather than the fork/join pool.
   *
   * <p>Workers block on the hand-off queue by design, and a {@code ForkJoinPool} cannot compensate
   * for a worker blocked outside its {@code ManagedBlocker} protocol — an earlier version of this
   * code deadlocked exactly that way, with five workers BLOCKED and thirteen parked waiting on
   * subtasks that had no thread left to run them.
   */
  private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
    final Thread t = new Thread(runnable, "brackit-morsel");
    t.setDaemon(true);
    return t;
  });

  /** Sentinel telling the consumer that one worker has finished. */
  private static final Object DONE = new Object();

  /**
   * {@code -Dbrackit.morsel.debug=true} reports, per iteration, what the source turned out to be
   * and how many ways it split. Worth having permanently: a source that silently declines to split
   * is indistinguishable from one that split badly by looking at the runtime alone.
   */
  private static final boolean DEBUG = Boolean.getBoolean("brackit.morsel.debug");

  private final Operator op;
  private final Expr expr;
  private final SplitAwareExpr leafBind;

  public MorselPipeExpr(final Operator op, final Expr expr, final SplitAwareExpr leafBind) {
    this.op = op;
    this.expr = expr;
    this.leafBind = leafBind;
  }

  @Override
  public Sequence evaluate(final QueryContext ctx, final Tuple tuple) throws QueryException {
    return new MorselSequence(ctx, tuple);
  }

  @Override
  public Item evaluateToItem(final QueryContext ctx, final Tuple tuple) throws QueryException {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    return expr.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  @Override
  public String toString() {
    return MorselPipeExpr.class.getSimpleName();
  }

  private final class MorselSequence extends LazySequence {
    private final QueryContext ctx;
    private final Tuple tuple;

    MorselSequence(final QueryContext ctx, final Tuple tuple) {
      this.ctx = ctx;
      this.tuple = tuple;
    }

    @Override
    public Iter iterate() {
      // Evaluate the bind expression ONCE, through the wrapped expression rather than the
      // split-aware wrapper: this runs on the consumer thread, which never holds a split, but going
      // direct keeps that independent of how the thread-local happens to be set.
      final Sequence source = leafBind.delegate().evaluate(ctx, tuple);
      final int splits = source instanceof SplittableSequence splittable
          ? Math.max(1, Math.min(WORKERS, splittable.splitCount(WORKERS)))
          : 1;
      if (DEBUG) {
        System.err.printf("[morsel] source=%s splittable=%s splits=%d%n",
                          source == null ? "null" : source.getClass().getName(),
                          source instanceof SplittableSequence,
                          splits);
      }
      if (splits <= 1) {
        return new SerialIter(ctx, tuple);
      }
      return new ParallelIter(ctx, tuple, (SplittableSequence) source, splits);
    }
  }

  /**
   * The serial pipeline, used whenever the source will not split.
   *
   * <p>Deliberately a copy of {@code PipeExpr}'s iterator rather than a delegation to it: the
   * source has already been evaluated here, and re-entering {@code PipeExpr} would evaluate the
   * bind expression a second time.
   */
  private final class SerialIter extends BaseIter {
    private final QueryContext ctx;
    private final Tuple tuple;
    private Cursor cursor;
    private Iter it;

    SerialIter(final QueryContext ctx, final Tuple tuple) {
      this.ctx = ctx;
      this.tuple = tuple;
    }

    @Override
    public Item next() throws QueryException {
      while (true) {
        if (it != null) {
          final Item i = it.next();
          if (i != null) {
            return i;
          }
          it.close();
          it = null;
        } else if (cursor == null) {
          // No binding: the leaf re-evaluates its own expression, which is the same source this
          // iterator was handed. Cheaper than it looks — evaluating an unboxed array builds a view,
          // it does not read anything — and it keeps the serial path free of thread-local lifetime
          // concerns entirely.
          cursor = op.create(ctx, tuple);
          cursor.open(ctx);
        }
        final Tuple t = cursor.next(ctx);
        if (t == null) {
          return null;
        }
        final Sequence s = expr.evaluate(ctx, t);
        if (s == null) {
          continue;
        }
        if (s instanceof Item item) {
          return item;
        }
        it = s.iterate();
      }
    }

    @Override
    public void close() {
      if (it != null) {
        it.close();
        it = null;
      }
      if (cursor != null) {
        cursor.close(ctx);
        cursor = null;
      }
    }
  }

  /** Fans the chain out over the source's splits and drains the results in completion order. */
  private final class ParallelIter extends BaseIter {
    private final BlockingQueue<Object> queue;
    private final CountDownLatch finished;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final int workers;

    private volatile boolean cancelled;
    private Item[] batch;
    private int batchPos;
    private int batchLen;
    private int workersDone;
    private boolean exhausted;

    ParallelIter(final QueryContext ctx, final Tuple tuple, final SplittableSequence source, final int splits) {
      this.workers = splits;
      this.queue = new ArrayBlockingQueue<>(Math.max(2, splits * QUEUE_DEPTH));
      this.finished = new CountDownLatch(splits);
      for (int i = 0; i < splits; i++) {
        final int index = i;
        POOL.execute(() -> runSplit(ctx, tuple, source.split(index, splits)));
      }
    }

    private void runSplit(final QueryContext workerCtx, final Tuple tuple, final Sequence split) {
      Cursor cursor = null;
      // Held for the ENTIRE run, not just around create/open. ForBind evaluates its bind expression
      // inside next(), the first time it is asked for a tuple — not when the cursor is built — so a
      // binding released after open() is already gone by the time the leaf looks for it. That
      // mistake does not fail loudly: every worker simply scans the whole source, and the query
      // returns N times the right answer.
      final Sequence previous = leafBind.bind(split);
      try {
        cursor = op.create(workerCtx, tuple);
        cursor.open(workerCtx);
        Item[] out = new Item[BATCH];
        int n = 0;
        Tuple t;
        while (!cancelled && (t = cursor.next(workerCtx)) != null) {
          final Sequence s = expr.evaluate(workerCtx, t);
          if (s == null) {
            continue;
          }
          if (s instanceof Item item) {
            out[n++] = item;
            if (n == BATCH) {
              if (!publish(out, n)) {
                return;
              }
              out = new Item[BATCH];
              n = 0;
            }
          } else {
            try (final Iter inner = s.iterate()) {
              Item item;
              while ((item = inner.next()) != null) {
                out[n++] = item;
                if (n == BATCH) {
                  if (!publish(out, n)) {
                    return;
                  }
                  out = new Item[BATCH];
                  n = 0;
                }
              }
            }
          }
        }
        if (n > 0) {
          publish(out, n);
        }
      } catch (final Throwable e) {
        failure.compareAndSet(null, e);
      } finally {
        if (cursor != null) {
          try {
            cursor.close(workerCtx);
          } catch (final RuntimeException ignored) {
            // A close failure must not mask the real error, nor stop the latch from dropping.
          }
        }
        leafBind.bind(previous);
        finished.countDown();
        // Unblock a consumer that is waiting on an empty queue for this worker's sentinel. The
        // latch above is what actually ends the run, so this only has to arrive eventually — but
        // the queue is BOUNDED, and a plain offer() drops the sentinel whenever it is full, which
        // is exactly when a worker finishes behind a backlog. The consumer would then fall back on
        // its poll timeout, paying it once per worker at the end of every fan-out.
        offerUntilCancelled(DONE);
      }
    }

    /** @return {@code false} once the consumer has stopped caring, so the worker can stop early. */
    private boolean publish(final Item[] items, final int len) {
      return offerUntilCancelled(len == items.length ? items : trim(items, len));
    }

    /**
     * Waits for a slot rather than dropping the payload on a full queue.
     *
     * <p>Cannot deadlock: the consumer only stops draining after {@link #close}, which sets
     * {@code cancelled} and clears the queue.
     *
     * @return {@code false} if the payload was never handed over because the consumer stopped
     *         caring (or this thread was interrupted)
     */
    private boolean offerUntilCancelled(final Object payload) {
      while (!cancelled) {
        try {
          if (queue.offer(payload, 50, TimeUnit.MILLISECONDS)) {
            return true;
          }
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
      return false;
    }

    private Item[] trim(final Item[] items, final int len) {
      final Item[] exact = new Item[len];
      System.arraycopy(items, 0, exact, 0, len);
      return exact;
    }

    @Override
    public Item next() throws QueryException {
      while (true) {
        if (batch != null && batchPos < batchLen) {
          return batch[batchPos++];
        }
        batch = null;
        rethrowFailure();
        if (exhausted) {
          return null;
        }
        final Object taken;
        try {
          taken = queue.poll(50, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
        }
        if (taken == null) {
          // Nothing yet. Only conclude the run is over once every worker has signalled AND the
          // queue has drained, otherwise a slow worker's last batch is silently dropped.
          if (finished.getCount() == 0 && queue.isEmpty()) {
            exhausted = true;
            rethrowFailure();
            return null;
          }
          continue;
        }
        if (taken == DONE) {
          if (++workersDone >= workers && queue.isEmpty()) {
            exhausted = true;
            rethrowFailure();
            return null;
          }
          continue;
        }
        batch = (Item[]) taken;
        batchPos = 0;
        batchLen = batch.length;
      }
    }

    private void rethrowFailure() throws QueryException {
      final Throwable e = failure.get();
      if (e == null) {
        return;
      }
      cancelled = true;
      if (e instanceof QueryException qe) {
        throw qe;
      }
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }

    @Override
    public void close() {
      cancelled = true;
      // Drain so a worker blocked in publish() can observe the cancellation and exit; without this
      // an early close (a positional predicate, an exception upstream) would hang on the latch.
      queue.clear();
      try {
        while (!finished.await(50, TimeUnit.MILLISECONDS)) {
          queue.clear();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      batch = null;
    }
  }
}
