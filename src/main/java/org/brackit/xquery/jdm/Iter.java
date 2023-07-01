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
package org.brackit.xquery.jdm;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.sequence.ItemIter;

/**
 * <p>
 * An {@link Iter} iterates over a sequence of {@link Item Items}.
 * </p>
 *
 * <p>
 * One must ensure to always close an iterator, e.g., by following
 * this coding pattern:
 * </p>
 *
 * <pre>
 * Iter it = s.iterate();
 * try {
 * ...
 * } finally {
 * it.close();
 * }
 * </pre>
 * <p>
 * Or better by using try-with-resources:
 *
 * <pre>
 * try (Iter it = s.iterate()) {
 * ...
 * }
 * </pre>
 *
 * @author Sebastian Baechle
 */
public interface Iter extends AutoCloseable {
  /**
   * Get the next item
   *
   * @throws QueryException if anything goes wrong while getting the next item
   */
  Item next();

  /**
   * Skip the next {@code i} items
   *
   * @throws QueryException if anything goes wrong while skippint the next
   *                        {@code i} items
   */
  void skip(IntNumeric i);

  /**
   * <p>
   * Split separate iterator for the first half of the
   * remaining items. Implementations should try to
   * split into equally-sized halves of at least <code>min</code>
   * items, but buffer at most <code>max</code> items in memory
   * if they need to.
   * </p>
   * <p>
   * Of the returned iterators, the last one is the
   * repositioned current iterator.
   * </p>
   */
  default Split split(int min, int max) {
    final Item[] buf = new Item[max];
    int i = 0;
    while ((buf[i] = next()) != null && ++i < max)
      ;
    Iter head = new ItemIter(buf, 0, i);
    Iter tail = (i < min) ? null : this;
    return new Split(head, tail, true);
  }

  /**
   * Close the iterator to release all resources.
   */
  void close();

  class Split {
    public final Iter head;
    public final Iter tail;
    public final boolean serial;

    public Split(Iter head, Iter tail, boolean serial) {
      this.head = head;
      this.tail = tail;
      this.serial = serial;
    }
  }
}
