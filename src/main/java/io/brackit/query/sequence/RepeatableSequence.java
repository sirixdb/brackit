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
package io.brackit.query.sequence;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;

final class RepeatableSequence extends LazySequence {
  private static final Int32 TWO = Int32.ZERO_TO_TWENTY[2];
  private final Sequence source;
  private final Item first;
  private final Item second;

  private RepeatableSequence(Sequence source, Item first, Item second) {
    this.source = source;
    this.first = first;
    this.second = second;
  }

  static Sequence normalize(Sequence source) {
    try {
      if (source == null || source instanceof Item || source instanceof RepeatableSequence
          || source instanceof FunctionResultSequence) {
        return source;
      }
      IntNumeric size = source.knownSize();
      if (size != null && size.cmp(TWO) >= 0) {
        return new RepeatableSequence(source, null, null);
      }
      try (Iter it = source.iterate()) {
        Item first = it.next();
        if (first == null) {
          return null;
        }
        Item second = it.next();
        return second == null ? first : new RepeatableSequence(source, first, second);
      }
    } catch (StackOverflowError error) {
      throw new QueryException(error, ErrorCode.BIT_DYN_RT_STACK_OVERFLOW);
    }
  }

  @Override
  public boolean isRepeatable() {
    return true;
  }

  @Override
  public IntNumeric knownSize() {
    return source.knownSize();
  }

  @Override
  public IntNumeric size() {
    return source.size();
  }

  @Override
  public Item get(IntNumeric position) {
    if (first != null) {
      if (position.cmp(Int32.ONE) == 0) {
        return first;
      }
      if (position.cmp(TWO) == 0) {
        return second;
      }
    }
    return source.get(position);
  }

  @Override
  public Iter iterate() {
    if (first == null) {
      return source.iterate();
    }
    return new BaseIter() {
      int position;
      Iter tail;
      boolean closed;

      @Override
      public Item next() {
        if (closed) {
          return null;
        }
        if (position < 2) {
          return position++ == 0 ? first : second;
        }
        if (tail == null) {
          tail = source.iterate();
          tail.skip(TWO);
        }
        return tail.next();
      }

      @Override
      public void skip(IntNumeric count) {
        if (closed || count.cmp(Int32.ZERO) <= 0) {
          return;
        }
        IntNumeric remaining = new Int32(2 - position);
        if (count.cmp(remaining) <= 0) {
          position += count.intValue();
          return;
        }
        count = (IntNumeric) count.subtract(remaining);
        position = 2;
        if (tail == null) {
          tail = source.iterate();
          count = (IntNumeric) count.add(TWO);
        }
        tail.skip(count);
      }

      @Override
      public void close() {
        if (!closed) {
          closed = true;
          if (tail != null) {
            tail.close();
          }
        }
      }
    };
  }
}
