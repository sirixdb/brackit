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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.block;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.brackit.query.QueryException;

/**
 * A {@link ConcurrentSink} creates a fan-in, i.e., {@link #fork()} returns this
 * sink again but the sink keeps track how many "virtual" sinks have been forked
 * and are active.
 *
 * @author Sebastian Baechle
 */
public abstract class ConcurrentSink implements Sink {

  protected final AtomicBoolean begin = new AtomicBoolean(false);
  protected final AtomicInteger alive = new AtomicInteger(1);

  protected void doBegin() throws QueryException {
  }

  protected void doEnd() throws QueryException {
  }

  protected void doFail() throws QueryException {
  }

  @Override
  public final Sink fork() {
    alive.incrementAndGet();
    return this;
  }

  @Override
  public final void end() throws QueryException {
    if (alive.decrementAndGet() == 0) {
      doEnd();
    }
  }

  @Override
  public final void begin() throws QueryException {
    if (!begin.get() && begin.compareAndSet(false, true)) {
      doBegin();
    }
  }

  @Override
  public final void fail() throws QueryException {
    doFail();
  }

}
