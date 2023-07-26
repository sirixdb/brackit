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
package io.brackit.query.operator;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;

/**
 * @author Sebastian Baechle
 */
public class Parallelizer implements Operator {
  private static class ParallelizerCursor implements Cursor {
    private final Cursor c;

    private final QueryContext ctx;

    private volatile boolean finished;

    private volatile QueryException error;

    // private int takes;
    //
    // private int enqueueRetries;
    //
    // private int dequeueRetries;

    private Tuple current;

    private Tuple[] currentBuffer;

    private Tuple[][] freeQueue;

    private volatile int freeQueueStart;

    private volatile int freeQueueEnd;

    private int pos = 0;

    ParallelizerCursor(Cursor c, QueryContext ctx) {
      this.c = c;
      this.ctx = ctx;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      int noOfBuffers = 3;
      this.freeQueue = new Tuple[noOfBuffers][2000];
      this.finished = false;
      freeQueueStart = noOfBuffers - 1;
      freeQueueEnd = 0;

      new Thread(() -> {
        // System.out.println("internal starting");
        fill();
        // System.out.println("internal stopping");
      }).start();
    }

    private void fill() {
      try {
        c.open(ctx);
        int pos = 0;
        Object[] buffer = freeQueue[0];
        int length = buffer.length;
        Tuple t;

        while ((t = c.next(ctx)) != null) {
          buffer[pos++] = t;

          if (pos == length) {
            if (finished) {
              break;
            }

            // offer filled and take next
            buffer = enqueue();
            length = buffer.length;
            pos = 0;
          }
        }
        enqueue();
        finished = true;
      } catch (QueryException e) {
        error = e;
        finished = true;
      } finally {
        c.close(ctx);
      }
    }

    private Tuple[] enqueue() {
      int queueStart = freeQueueStart; // volatile read
      int queueEnd = freeQueueEnd; // volatile read

      int newQueueEnd = (queueEnd + 1) % freeQueue.length;

      while (newQueueEnd == queueStart) {
        // spin until one more free
        queueStart = freeQueueStart; // volatile read
        // enqueueRetries++;
      }

      // take one from queue
      freeQueueEnd = newQueueEnd;
      return freeQueue[newQueueEnd];
    }

    private Tuple[] dequeue() {
      int queueStart = freeQueueStart; // volatile read
      int queueEnd = freeQueueEnd; // volatile read

      int newQueueStart = (queueStart + 1) % freeQueue.length;

      while (newQueueStart == queueEnd) {
        // spin until one more free
        queueEnd = freeQueueEnd; // volatile read
        // dequeueRetries++;
      }

      // take one from queue
      freeQueueStart = newQueueStart;
      return freeQueue[newQueueStart];
    }

    @Override
    public void close(QueryContext ctx) {
      finished = true;
      // System.out.println("Takes: " + takes);
      // System.out.println("EnqueueRetries: " + enqueueRetries);
      // System.out.println("DequeueRetries: " + dequeueRetries);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      QueryException deliverError = error; // volatile read

      if (deliverError != null) {
        error = null;
        throw deliverError;
      }

      if (currentBuffer == null || pos == currentBuffer.length) {
        currentBuffer = dequeue();
        pos = 0;
      }

      current = currentBuffer[pos];
      currentBuffer[pos++] = null;

      if (current == null) {
        return null;
      }

      // takes++;
      Tuple deliver = current;
      current = null;
      return deliver;
    }
  }

  private final Operator in;

  public Parallelizer(Operator in) {
    this.in = in;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    return new ParallelizerCursor(in.create(ctx, tuple), ctx);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    return new ParallelizerCursor(in.create(ctx, buf, len), ctx);
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize);
  }
}
