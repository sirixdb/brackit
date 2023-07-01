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
package org.brackit.xquery.util.forkjoin;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Lock-free deque using Doug Lea's design of the ForkJoinWorker-internal
 * array-based work-stealing deque in OpenJDK 1.7.
 * <p>
 * Because we must not use OpenJDKs Unsafe class directly, we must use a
 * (slower) AtomicReferenceArray for our implementation.
 *
 * @author Sebastian Baechle
 */
public class CASDeque<E> implements Deque<E> {
  private static final int INITIAL_QUEUE_CAPACITY = 1 << 6;
  private static final int MAXIMUM_QUEUE_CAPACITY = 1 << 31;
  private volatile int queueBase;
  private volatile int queueTop;
  private volatile AtomicReferenceArray<E> queue;

  CASDeque() {
    this.queue = new AtomicReferenceArray<>(INITIAL_QUEUE_CAPACITY);
  }

  @SuppressWarnings("unchecked")
  public void add(E t) {
    AtomicReferenceArray<E> q = this.queue;
    Object[] tmp = new Object[q.length()];
    int len = 0;
    for (E x; (x = poll()) != null;) {
      tmp[len++] = x;
    }
    push(t);
    for (int i = len - 1; i >= 0; i--) {
      push((E) tmp[i]);
    }
  }

  public void push(E t) {
    AtomicReferenceArray<E> q;
    int s;
    int m;
    if ((q = queue) != null) { // ignore if queue removed
      // calc index with modulo (length is power of 2!)
      int i = (s = queueTop) & (m = q.length() - 1);
      q.set(i, t);
      queueTop = s + 1;
      s -= queueBase;
      if (s == m) {
        growQueue();
      }
    }
  }

  public E poll() {
    int m;
    AtomicReferenceArray<E> q = queue;
    if (q != null && (m = q.length() - 1) >= 0) {
      for (int s; (s = queueTop) != queueBase;) {
        int i = m & --s;
        E t = q.get(i);
        if (t == null) // lost to stealer
          break;
        if (q.compareAndSet(i, t, null)) {
          queueTop = s; // or putOrderedInt
          return t;
        }
      }
    }
    return null;
  }

  public E pollLast() {
    E t;
    AtomicReferenceArray<E> q;
    int b;
    int i;
    if (queueTop != (b = queueBase) && (q = queue) != null && // must read q after b
        (i = q.length() - 1 & b) >= 0 && (t = q.get(i)) != null && queueBase == b && q.compareAndSet(i, t, null)) {
      queueBase = b + 1;
      return t;
    }
    return null;
  }

  private void growQueue() {
    AtomicReferenceArray<E> oldQ = queue;
    int size = oldQ != null ? oldQ.length() << 1 : INITIAL_QUEUE_CAPACITY;
    if (size > MAXIMUM_QUEUE_CAPACITY)
      throw new RuntimeException("Queue capacity exceeded");
    if (size < INITIAL_QUEUE_CAPACITY)
      size = INITIAL_QUEUE_CAPACITY;
    AtomicReferenceArray<E> q = queue = new AtomicReferenceArray<>(size);
    int mask = size - 1;
    int top = queueTop;
    int oldMask;
    if (oldQ != null && (oldMask = oldQ.length() - 1) >= 0) {
      for (int b = queueBase; b != top; ++b) {
        int oldI = b & oldMask;
        E t = oldQ.get(oldI);
        if (t != null && oldQ.compareAndSet(b & oldMask, t, null)) {
          int i = b & mask;
          q.set(i, t);
        }
      }
    }
  }

  @Override
  public int size() {
    throw new RuntimeException("Not implemented yet");
  }
}