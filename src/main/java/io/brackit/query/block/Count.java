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

import java.util.concurrent.Semaphore;

import io.brackit.query.atomic.Counter;
import io.brackit.query.atomic.Int32;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.operator.Check;

/**
 * @author Sebastian Baechle
 */
public class Count implements Block {
  final Check check;

  public Count() {
    this(null);
  }

  public Count(Check check) {
    this.check = check;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    return new CountSink(sink, check);
  }

  @Override
  public int outputWidth(int initSize) {
    return initSize + 1;
  }

  static class CountSink extends SerialSink {
    // Reusable output buffer to reduce GC pressure - grows as needed
    private static final int INITIAL_BUFFER_SIZE = 512;
    private Tuple[] outBuffer = new Tuple[INITIAL_BUFFER_SIZE];

    final Sink sink;
    final Check check;
    final Counter pos;
    Tuple prev;

    CountSink(Sink sink, Check check) {
      super(FJControl.PERMITS);
      this.sink = sink;
      this.check = check;
      this.pos = new Counter();
      this.prev = null;
    }

    CountSink(Semaphore sem, Sink sink, Check check, Counter pos, Tuple prev) {
      super(sem);
      this.sink = sink;
      this.check = check;
      this.pos = pos;
      this.prev = prev;
    }

    @Override
    protected ChainedSink doPartition(Sink stopAt) {
      return new CountSink(sem, sink.partition(stopAt), check, new Counter(), null);
    }

    @Override
    protected SerialSink doFork() {
      return new CountSink(sem, sink, check, pos, prev);
    }

    @Override
    protected void doOutput(Tuple[] buf, int len) throws QueryException {
      // Ensure output buffer is large enough, grow if needed
      if (outBuffer.length < len) {
        outBuffer = new Tuple[len];
      }
      Tuple[] out = outBuffer;

      for (int i = 0; i < len; i++) {
        Tuple t = buf[i];

        if (check != null) {
          // Handle dead tuple - append null position
          if (check.dead(t)) {
            pos.reset();
            out[i] = t.concat((Sequence) null);
            prev = t;
            continue;
          }
          // Reset position on group boundary
          if (prev == null || check.separate(prev, t)) {
            pos.reset();
          }
        }

        pos.inc();
        out[i] = t.concat(pos.asIntNumeric());
        prev = t;
      }
      sink.output(out, len);
    }

    @Override
    protected void doFirstBegin() throws QueryException {
      sink.begin();
    }

    @Override
    protected void doFinalEnd() throws QueryException {
      sink.end();
    }
  }
}
