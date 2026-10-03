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
package io.brackit.query.jsonitem.array;

import java.util.ArrayList;
import java.util.List;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.sequence.BaseIter;

/**
 * A lazy array backed by a {@link StreamingJSONParser}. Elements are parsed on-demand
 * as the iterator advances, enabling processing of files that exceed available memory.
 * <p>
 * This array is read-only — mutation methods throw {@link UnsupportedOperationException}.
 * Operations that require random access ({@link #at(int)}, {@link #values()}, {@link #length()})
 * force materialization up to the requested index or fully. Once an iterator consumes an
 * uncached element, only that iterator may continue reading; other iterators, including
 * ones created earlier, and materialization or random access throw a {@link QueryException}.
 * Consumed uncached elements cannot be replayed. Fully materialize with {@link #values()}
 * or {@link #length()} before consuming uncached elements if multiple reads are required.
 * <p>
 * A parsing failure is terminal: the failing read propagates the original exception,
 * and every later read throws a {@link QueryException} with that exception as its cause,
 * including reads through the owning iterator or of cached elements.
 */
public final class StreamingArray extends AbstractArray {

  private final StreamingJSONParser parser;
  private final boolean wrappedInObject;

  // Parser consumption, ownership, cache updates, and failure state use this array's monitor.
  // Cache for materialized elements (for random access)
  private final List<Sequence> materialized = new ArrayList<>();
  private boolean fullyMaterialized;
  private boolean streamed;
  private RuntimeException parsingFailure;

  public StreamingArray(StreamingJSONParser parser) {
    this(parser, false);
  }

  public StreamingArray(StreamingJSONParser parser, boolean wrappedInObject) {
    this.parser = parser;
    this.wrappedInObject = wrappedInObject;
  }

  @Override
  public synchronized Iter iterate() {
    ensureReplayable();
    if (fullyMaterialized) {
      // Already materialized — iterate from cache
      return new BaseIter() {
        private int index = 0;

        @Override
        public Item next() {
          if (index < materialized.size()) {
            return (Item) materialized.get(index++);
          }
          return null;
        }

        @Override
        public void close() {
        }
      };
    }

    return new BaseIter() {
      private int index = 0;
      private boolean done = false;
      private boolean ownsStream = false;

      @Override
      public Item next() {
        synchronized (StreamingArray.this) {
          ensureReadable();
          if (done) {
            return null;
          }
          if (!ownsStream) {
            ensureReplayable();
          }
          if (index < materialized.size()) {
            return (Item) materialized.get(index++);
          }
          if (fullyMaterialized) {
            done = true;
            return null;
          }
          Item item = readNextElement();
          if (item == null) {
            done = true;
            fullyMaterialized = !streamed;
            return null;
          }
          if (!materialized.isEmpty()) {
            materialized.add(item);
          } else {
            streamed = true;
            ownsStream = true;
          }
          index++;
          return item;
        }
      }

      @Override
      public void close() {
      }
    };
  }

  @Override
  public synchronized List<Sequence> values() {
    materializeAll();
    return materialized;
  }

  @Override
  public synchronized Sequence at(int i) {
    Sequence value = atOrEmpty(i);
    if (value == null) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, "Invalid array index: %s", i);
    }
    return value;
  }

  public synchronized Sequence atOrEmpty(int i) {
    materializeUpTo(i);
    return i < 0 || i >= materialized.size() ? null : materialized.get(i);
  }

  @Override
  public Sequence at(IntNumeric index) {
    return at(index.intValue());
  }

  @Override
  public synchronized IntNumeric length() {
    materializeAll();
    int len = materialized.size();
    return len <= 20 ? Int32.ZERO_TO_TWENTY[len] : new Int32(len);
  }

  @Override
  public synchronized int len() {
    materializeAll();
    return materialized.size();
  }

  @Override
  public synchronized Array range(IntNumeric from, IntNumeric to) {
    materializeUpTo(to.intValue());
    List<Sequence> sub = materialized.subList(from.intValue(), to.intValue());
    return new DArray(sub);
  }

  // Mutation methods — not supported on a stream

  @Override
  public Array insert(int index, Sequence value) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array insert(IntNumeric index, Sequence value) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array append(Sequence value) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array replaceAt(int index, Sequence value) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array replaceAt(IntNumeric index, Sequence value) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array remove(int index) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  @Override
  public Array remove(IntNumeric index) {
    throw new UnsupportedOperationException("StreamingArray is read-only");
  }

  // ==================== Internal ====================

  private void ensureReadable() {
    if (parsingFailure != null) {
      throw new QueryException(parsingFailure,
                               ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "StreamingArray cannot be read after a parsing failure");
    }
  }

  private void ensureReplayable() {
    ensureReadable();
    if (streamed) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "StreamingArray cannot be reread after uncached elements have been consumed; materialize before iterating");
    }
  }

  private Item readNextElement() {
    ensureReadable();
    try {
      Item item = parser.nextArrayElement();
      if (item == null && wrappedInObject) {
        parser.skipTrailingObjectClose();
      }
      return item;
    } catch (RuntimeException e) {
      // The parser may already have advanced; forbid all later reads instead of exposing a suffix.
      parsingFailure = e;
      throw e;
    }
  }

  private void materializeAll() {
    ensureReplayable();
    if (fullyMaterialized) {
      return;
    }
    Item item;
    while ((item = readNextElement()) != null) {
      materialized.add(item);
    }
    fullyMaterialized = true;
  }

  private void materializeUpTo(int index) {
    ensureReplayable();
    while (!fullyMaterialized && materialized.size() <= index) {
      Item item = readNextElement();
      if (item == null) {
        fullyMaterialized = true;
        return;
      }
      materialized.add(item);
    }
  }
}
