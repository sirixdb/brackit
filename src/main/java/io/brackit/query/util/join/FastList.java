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
package io.brackit.query.util.join;

import java.util.Arrays;

/**
 * Array-based variant of a list. Faster than java.util.ArrayList for our
 * purposes, e.g., makes less error-checks.
 *
 * @author Sebastian Baechle
 */
public class FastList<E> {

  public static final FastList<Object> EMPTY_LIST = new FastList<>(0);

  private Object[] values;

  private int size;

  public FastList(int size) {
    values = new Object[size];
  }

  public FastList() {
    values = new Object[10];
  }

  @SuppressWarnings("unchecked")
  public static <T> FastList<T> emptyList() {
    return (FastList<T>) EMPTY_LIST;
  }

  public int getSize() {
    return size;
  }

  @SuppressWarnings("unchecked")
  public E get(int p) {
    return (E) values[p];
  }

  @SuppressWarnings("unused")
  public void addAll(E[] v, int off, int len) {
    capacity(size + len);
    System.arraycopy(v, off, values, size, len);
    size = size + len;
  }

  public void addAllSafe(Object[] v, int off, int len) {
    capacity(size + len);
    System.arraycopy(v, off, values, size, len);
    size = size + len;
  }

  /**
   * Grows the backing array geometrically, matching {@link #add(Object)}'s {@code *3/2+1}.
   *
   * <p>Growing to the exact requested capacity makes every bulk append reallocate and copy the
   * whole array, so appending n elements in batches of b costs O(n²/b) copies rather than O(n).
   * {@code BlockExpr.Return.doOutput} accumulates a whole result sequence through
   * {@link #addAllSafe(Object[], int, int)} one batch per pipeline flush, so on a 3.48 M-record
   * scan this was ~13,600 reallocations totalling ~2.4e10 element copies — the parallel query
   * pipeline never finished, and profiles showed 40 % of CPU in {@code memset}/{@code Arrays.copyOf}
   * plus ~20 % in G1. The single-element {@code add} path was already geometric; only the bulk path
   * was not.
   *
   * <p>{@code size} remains the logical length, so over-allocating capacity changes no observable
   * behaviour.
   */
  private void capacity(int capacity) {
    if (values.length < capacity) {
      final int grown = values.length * 3 / 2 + 1;
      values = Arrays.copyOf(values, Math.max(capacity, grown));
    }
  }

  public void sort() {
    Arrays.sort(values, 0, size);
  }

  public void add(E v) {
    if (size == values.length) {
      values = Arrays.copyOf(values, values.length * 3 / 2 + 1);
    }
    values[size++] = v;
  }

  public void addUnchecked(E v) {
    values[size++] = v;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void ensureAdditional(int len) {
    capacity(size + len);
  }
}