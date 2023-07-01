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

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Lock-free deque according to
 * "David Chase, Yossi Lev: Dynamic Circular Work-Stealing Deque".
 *
 * @author Sebastian Baechle
 */
public class CircularDeque<E> implements Deque<E> {
  static class CircularArray<E> {
    private final int log_size;
    private final Object[] segment;

    CircularArray(int log_size) {
      this.log_size = log_size;
      this.segment = new Object[1 << this.log_size];
    }

    long size() {
      return 1L << this.log_size;
    }

    @SuppressWarnings("unchecked")
    E get(long i) {
      return (E) segment[(int) (i % size())];
    }

    void put(long i, E o) {
      segment[(int) (i % size())] = o;
    }

    CircularArray<E> grow(long b, long t) {
      CircularArray<E> a = new CircularArray<>(this.log_size + 1);
      for (long i = t; i < b; i++) {
        a.put(i, this.get(i));
      }
      return a;
    }
  }

  @SuppressWarnings("rawtypes")
  private static final AtomicLongFieldUpdater<CircularDeque> UPDATER = AtomicLongFieldUpdater.newUpdater(
                                                                                                         CircularDeque.class,
                                                                                                         "top");

  private static final int INITIAL_CAPACITY = 6; // log size -> actual size =
  // 2^6
  private volatile long bottom;
  private volatile long top;
  private volatile CircularArray<E> activeArray;

  CircularDeque() {
    activeArray = new CircularArray<E>(INITIAL_CAPACITY);
  }

  CircularDeque(int maxCapacity) {
    int c = 1;
    while (c < maxCapacity) {
      c <<= 1;
    }
    activeArray = new CircularArray<>(c);
  }

  @SuppressWarnings("unchecked")
  public void add(E o) {
    // TODO Optimize with CASing queue "empty"
    CircularArray<E> a = this.activeArray;
    Object[] tmp = new Object[(int) a.size()];
    int len = 0;
    for (E x; (x = poll()) != null;) {
      tmp[len++] = x;
    }
    push(o);
    for (int i = len - 1; i >= 0; i--) {
      push((E) tmp[i]);
    }
  }

  public void push(E o) {
    long b = this.bottom;
    long t = this.top;
    CircularArray<E> a = this.activeArray;
    long size = b - t;
    if (size >= a.size() - 1) {
      a = a.grow(b, t);
      this.activeArray = a;
    }
    a.put(b, o);
    bottom = b + 1;
  }

  public E poll() {
    long b = this.bottom;
    CircularArray<E> a = this.activeArray;
    b = b - 1;
    this.bottom = b;
    long t = this.top;
    long size = b - t;
    if (size < 0) {
      bottom = t;
      return null;
    }
    E o = a.get(b);
    if (size > 0)
      return o;
    if (!UPDATER.compareAndSet(this, t, t + 1))
      o = null;
    this.bottom = t + 1;
    return o;
  }

  public E pollLast() {
    long t = this.top;
    long b = this.bottom;
    CircularArray<E> a = this.activeArray;
    long size = b - t;
    if (size <= 0)
      return null;
    E o = a.get(t);
    if (!UPDATER.compareAndSet(this, t, t + 1))
      return null;
    return o;
  }

  @Override
  public int size() {
    long t = this.top;
    long b = this.bottom;
    return (int) (b - t);
  }
}