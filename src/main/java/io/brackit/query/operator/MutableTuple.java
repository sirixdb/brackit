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

import java.util.Arrays;

import io.brackit.query.Tuple;
import io.brackit.query.jdm.Sequence;

/**
 * A mutable tuple that pre-allocates at the full pipeline width and supports
 * in-place column updates. Eliminates the per-tuple {@code Sequence[]} allocation
 * that {@link TupleImpl#concat} creates.
 * <p>
 * Usage: create once with the pipeline's total width, then reuse by calling
 * {@link #set(int, Sequence)} for each column. The {@link #concat} and
 * {@link #replace} methods return {@code this} instead of creating copies,
 * avoiding allocation entirely.
 * <p>
 * <b>WARNING:</b> Because this tuple is mutable, operators that hold references
 * to tuples across iterations (like GroupBy) must call {@link #snapshot()} to
 * get an immutable copy before the next iteration overwrites the values.
 */
public final class MutableTuple implements Tuple {

  private Sequence[] sequences;
  private int size; // logical size (may be less than sequences.length)

  public MutableTuple(int capacity) {
    this.sequences = new Sequence[capacity];
    this.size = 0;
  }

  public MutableTuple(Sequence[] initial) {
    this.sequences = initial;
    this.size = initial.length;
  }

  /**
   * Set a column value in-place. No allocation.
   */
  public void set(int position, Sequence s) {
    sequences[position] = s;
  }

  /**
   * Reset the logical size (for reuse across iterations).
   */
  public void resetSize(int newSize) {
    this.size = newSize;
  }

  /**
   * Create an immutable snapshot. Used by operators that hold tuple references
   * across iterations (GroupBy, OrderBy).
   */
  public TupleImpl snapshot() {
    return new TupleImpl(Arrays.copyOf(sequences, size));
  }

  // ==================== Tuple interface — in-place when possible ====================

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public Sequence get(int position) {
    return sequences[position];
  }

  @Override
  public Sequence[] array() {
    return Arrays.copyOf(sequences, size);
  }

  @Override
  public Tuple project(int... positions) {
    Sequence[] projected = new Sequence[positions.length];
    for (int i = 0; i < positions.length; i++) {
      projected[i] = sequences[positions[i]];
    }
    return new TupleImpl(projected);
  }

  @Override
  public Tuple project(int start, int end) {
    return new TupleImpl(Arrays.copyOfRange(sequences, start, end));
  }

  @Override
  public Tuple replace(int position, Sequence s) {
    sequences[position] = s;
    return this;
  }

  @Override
  public Tuple concat(Sequence s) {
    // In-place append if capacity allows
    if (size < sequences.length) {
      sequences[size] = s;
      size++;
      return this;
    }
    // Grow — rare, only happens if initial capacity was too small
    sequences = Arrays.copyOf(sequences, size + 4);
    sequences[size] = s;
    size++;
    return this;
  }

  @Override
  public Tuple concat(Sequence[] s) {
    if (size + s.length <= sequences.length) {
      System.arraycopy(s, 0, sequences, size, s.length);
      size += s.length;
      return this;
    }
    sequences = Arrays.copyOf(sequences, size + s.length + 4);
    System.arraycopy(s, 0, sequences, size, s.length);
    size += s.length;
    return this;
  }

  @Override
  public Tuple conreplace(Sequence con, int position, Sequence s) {
    if (size < sequences.length) {
      sequences[size] = con;
      size++;
    } else {
      sequences = Arrays.copyOf(sequences, size + 4);
      sequences[size] = con;
      size++;
    }
    sequences[position] = s;
    return this;
  }

  @Override
  public Tuple conreplace(Sequence[] con, int position, Sequence s) {
    if (size + con.length <= sequences.length) {
      System.arraycopy(con, 0, sequences, size, con.length);
      size += con.length;
    } else {
      sequences = Arrays.copyOf(sequences, size + con.length + 4);
      System.arraycopy(con, 0, sequences, size, con.length);
      size += con.length;
    }
    sequences[position] = s;
    return this;
  }
}
