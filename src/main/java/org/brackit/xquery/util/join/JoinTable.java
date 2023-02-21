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
package org.brackit.xquery.util.join;

import java.util.List;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public abstract class JoinTable {
  static class TKey implements Comparable<TKey> {
    final Atomic atomic;

    TKey(Atomic atomic) {
      this.atomic = atomic;
    }

    @Override
    public boolean equals(Object obj) {
      boolean b = (obj instanceof TKey) && (((TKey) obj).atomic.atomicCmp(atomic) == 0);
      return b;
    }

    @Override
    public int compareTo(TKey k) {
      return atomic.atomicCmp(k.atomic);
    }

    @Override
    public int hashCode() {
      return atomic.hashCode();
    }

    @Override
    public String toString() {
      return atomic.toString();
    }
  }

  static class TValue implements Comparable<TValue> {
    final Sequence[] bindings;
    final int pos;
    TValue next; // next pointer to chain matches with different pos

    TValue(Sequence[] bindings, int pos) {
      this.bindings = bindings;
      this.pos = pos;
    }

    @Override
    public int compareTo(TValue o) {
      return pos < o.pos ? -1 : (pos == o.pos) ? 0 : 1;
    }

    @Override
    public String toString() {
      return bindings.toString() + "@" + pos;
    }
  }

  static class TEntry implements Comparable<TEntry> {
    final TKey key;
    final TValue value;

    public TEntry(TKey key, TValue value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public int compareTo(TEntry o) {
      int res = 0;
      return ((key == o.key) || ((res = key.compareTo(o.key)) != 0)) ? res : value.compareTo(o.value);
    }
  }

  protected final FastList<Sequence[]> sortAndDeduplicate(FastList<TValue> in) throws QueryException {
    in.sort();
    FastList<Sequence[]> out = new FastList<Sequence[]>();
    TValue p = null;
    int inSize = in.getSize();
    for (int i = 0; i < inSize; i++) {
      TValue v = in.get(i);
      if ((p == null) || (p.pos < v.pos)) {
        out.add(v.bindings);
      }
      p = v;
    }
    return out;
  }

  protected abstract void add(Atomic key, int pos, Sequence[] bindings) throws QueryException;

  protected abstract void lookup(FastList<TValue> matches, Atomic key) throws QueryException;

  protected abstract List<TEntry> entries();
}