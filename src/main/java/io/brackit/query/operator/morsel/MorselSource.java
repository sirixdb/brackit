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

import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.operator.Cursor;

/**
 * Thread-safe morsel dispenser. Multiple worker threads call {@link #nextMorsel()}
 * concurrently; each gets a disjoint batch of tuples from the upstream {@link Cursor}.
 * <p>
 * The upstream cursor is serialized: only one thread fills a morsel at a time.
 * This is the "single producer, multiple consumer" pattern from DuckDB's morsel-driven
 * parallelism model.
 */
public final class MorselSource {

  private final Cursor upstream;
  private final QueryContext ctx;
  private final int morselSize;
  private volatile boolean exhausted;

  public MorselSource(Cursor upstream, QueryContext ctx, int morselSize) {
    this.upstream = upstream;
    this.ctx = ctx;
    this.morselSize = morselSize;
  }

  public MorselSource(Cursor upstream, QueryContext ctx) {
    this(upstream, ctx, Morsel.DEFAULT_SIZE);
  }

  /**
   * Get the next morsel of tuples. Returns null when the source is exhausted.
   * Thread-safe: synchronized on the upstream cursor.
   */
  public synchronized Morsel nextMorsel() {
    if (exhausted) {
      return null;
    }

    Morsel m = new Morsel(morselSize);
    for (int i = 0; i < morselSize; i++) {
      Tuple t = upstream.next(ctx);
      if (t == null) {
        exhausted = true;
        break;
      }
      m.add(t);
    }

    return m.size() > 0 ? m : null;
  }

  /**
   * Check if the source has been fully consumed.
   */
  public boolean isExhausted() {
    return exhausted;
  }
}
