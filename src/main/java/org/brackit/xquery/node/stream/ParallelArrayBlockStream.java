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
package org.brackit.xquery.node.stream;

import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Stream;

/**
 * @param <E>
 * @author Sebastian Baechle
 */
public class ParallelArrayBlockStream<E> implements Stream<E> {
  private final Stream<? extends E> stream;

  private volatile boolean finished;

  private volatile DocumentException error;

  // private int takes;
  //
  // private int enqueueRetries;
  //
  // private int dequeueRetries;

  private Object[] currentBuffer;

  private Object[][] freeQueue;

  private volatile int freeQueueStart;

  private volatile int freeQueueEnd;

  private int pos = 0;

  public ParallelArrayBlockStream(Stream<? extends E> stream) {
    super();
    this.stream = stream;
    int noOfBuffers = 3;
    this.freeQueue = new Object[noOfBuffers][2000];
    this.finished = false;
    freeQueueStart = noOfBuffers - 1;
    freeQueueEnd = 0;

    new Thread() {
      public void run() {
        // System.out.println("internal starting");
        fill();
        // System.out.println("internal stopping");
      }
    }.start();
  }

  private void fill() {
    try {
      int pos = 0;
      Object[] buffer = freeQueue[0];
      int length = buffer.length;

      E e;
      while ((e = stream.next()) != null) {
        buffer[pos++] = e;

        if (pos == length) {
          // offer filled and take next
          buffer = enqueue();
          length = buffer.length;
          pos = 0;
        }
      }
      enqueue();
      finished = true;
      stream.close();
    } catch (DocumentException e) {
      error = e;
      finished = true;
    }
  }

  private Object[] enqueue() {
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

  private Object[] dequeue() {
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
  public void close() {
    finished = true;
    // System.out.println("Takes: " + takes);
    // System.out.println("EnqueueRetries: " + enqueueRetries);
    // System.out.println("DequeueRetries: " + dequeueRetries);
  }

  @Override
  public E next() throws DocumentException {
    DocumentException deliverError = error; // volatile read

    if (deliverError != null) {
      error = null;
      throw deliverError;
    }

    if ((currentBuffer == null) || (pos == currentBuffer.length)) {
      currentBuffer = dequeue();
      pos = 0;
    }

    E current = (E) currentBuffer[pos];
    currentBuffer[pos++] = null;

    return current;
  }
}
