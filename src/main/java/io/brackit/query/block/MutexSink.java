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

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Sebastian Baechle
 */
public abstract class MutexSink extends ConcurrentSink {

  public abstract static class Out {
  }

  /**
   * A {@link ReentrantLock} acquired through {@link ForkJoinPool#managedBlock}, not a
   * {@code synchronized} block.
   *
   * <p>Every parallel pipeline funnels its output through this one lock, and the threads doing so
   * are ForkJoin workers. A worker blocked on a plain monitor is invisible to the pool — it cannot
   * compensate, so each blocked worker permanently removes one thread's worth of parallelism, while
   * the workers that are not blocked sit in {@link java.util.concurrent.ForkJoinTask#join()}
   * waiting for subtasks that now have no free worker to run them. Measured on a 3.48 M-record scan
   * at parallelism 19: 5 workers BLOCKED on this monitor, 13 parked in join, 1 runnable — the
   * pipeline never finished.
   *
   * <p>{@code managedBlock} exists for exactly this: it tells the pool a worker is about to block on
   * something the pool does not manage, so it starts a replacement and holds its target
   * parallelism. The lock remains reentrant, so a nested {@code output()} on the same thread behaves
   * as the {@code synchronized} block did.
   */
  private final ReentrantLock lock = new ReentrantLock();

  protected abstract void doOutput(Out out) throws QueryException;

  protected abstract Out doPreOutput(Tuple[] buf, int len) throws QueryException;

  @Override
  public void output(Tuple[] buf, int len) throws QueryException {
    Out out = doPreOutput(buf, len);
    acquire();
    try {
      doOutput(out);
    } finally {
      lock.unlock();
    }
  }

  private void acquire() throws QueryException {
    // Fast path: uncontended, or already held by this thread (reentrant).
    if (lock.tryLock()) {
      return;
    }
    try {
      ForkJoinPool.managedBlock(new LockBlocker(lock));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR, "Interrupted while acquiring the output lock");
    }
  }

  /** Acquires {@code lock} in a way the ForkJoinPool can account for. */
  private static final class LockBlocker implements ForkJoinPool.ManagedBlocker {
    private final ReentrantLock lock;
    private boolean acquired;

    private LockBlocker(final ReentrantLock lock) {
      this.lock = lock;
    }

    @Override
    public boolean block() throws InterruptedException {
      if (!acquired) {
        lock.lockInterruptibly();
        acquired = true;
      }
      return true;
    }

    @Override
    public boolean isReleasable() {
      return acquired || (acquired = lock.tryLock());
    }
  }
}