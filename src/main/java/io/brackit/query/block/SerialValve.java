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

import java.util.concurrent.Semaphore;

import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * A serial valve is a generic wrapper around a sink to ensure serialized,
 * order-preserving, single-threaded access.
 *
 * @author Sebastian Baechle
 */
public final class SerialValve extends SerialSink {
  final Sink sink;

  public SerialValve(int permits, Sink sink) {
    super(permits);
    this.sink = sink;
  }

  private SerialValve(Semaphore sem, Sink sink) {
    super(sem);
    this.sink = sink;
  }

  @Override
  protected ChainedSink doFork() {
    return new SerialValve(sem, sink);
  }

  @Override
  protected ChainedSink doPartition(Sink stopAt) {
    return new SerialValve(sem, sink.partition(stopAt));
  }

  @Override
  protected void doOutput(Tuple[] buf, int len) throws QueryException {
    sink.output(buf, len);
  }

  @Override
  protected void doFirstBegin() throws QueryException {
    sink.begin();
  }

  @Override
  protected void doFinalEnd() throws QueryException {
    sink.end();
  }

  @Override
  protected void doFail() throws QueryException {
    sink.fail();
  }
}
