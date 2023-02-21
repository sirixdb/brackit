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

import java.util.Arrays;
import java.util.List;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class SortedJoinTable extends AbstractJoinTable {
  private final Cmp cmp;

  private TEntry[] entries = new TEntry[50];

  private int size;

  private boolean sorted;

  public SortedJoinTable(Cmp cmp) {
    this.cmp = cmp;
  }

  @Override
  protected void add(Atomic key, int pos, Sequence[] bindings) throws QueryException {
    if (size == entries.length) {
      entries = Arrays.copyOf(entries, (entries.length * 3) / 2 + 1);
    }
    entries[size++] = new TEntry(new TKey(key), new TValue(bindings, pos));
  }

  @Override
  protected void lookup(FastList<TValue> matches, Atomic key) throws QueryException {
    if (!sorted) {
      Arrays.sort(entries, 0, size);
      sorted = true;
    }

    if (cmp == Cmp.eq) {
      equalLookup(matches, key);
    } else if ((cmp == Cmp.le) || (cmp == Cmp.lt)) {
      lessLookup(matches, key);
    } else if ((cmp == Cmp.ge) || (cmp == Cmp.gt)) {
      greaterLookup(matches, key);
    } else {
      throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
    }
  }

  private void lessLookup(FastList<TValue> matches, Atomic key) {
    TKey tKey = new TKey(key);
    int lower = 0;
    int upper = size - 1;
    while (lower < upper) {
      int mid = lower + (upper - lower) / 2;
      int p = entries[mid].key.compareTo(tKey);
      if (((cmp == Cmp.le) && (p >= 0)) || (p > 0)) {
        upper = mid;
      } else {
        lower = mid + 1;
      }
    }

    matches.ensureAdditional(size - lower);
    for (int i = lower; i < size; i++) {
      matches.addUnchecked(entries[i].value);
    }
  }

  private void greaterLookup(FastList<TValue> matches, Atomic key) {
    TKey tKey = new TKey(key);
    int lower = 0;
    int upper = size - 1;
    while (lower < upper) {
      int mid = lower + (upper - lower + 1) / 2;
      int p = entries[mid].key.compareTo(tKey);
      if (((cmp == Cmp.gt) && (p >= 0)) || (p > 0)) {
        upper = mid - 1;
      } else {
        lower = mid;
      }
    }

    matches.ensureAdditional(lower + 1);
    for (int i = 0; i < lower + 1; i++) {
      matches.addUnchecked(entries[i].value);
    }
  }

  private void equalLookup(FastList<TValue> matches, Atomic key) {
    TKey tKey = new TKey(key);
    int res = -1;
    int lower = 0;
    int upper = size - 1;
    int pos = 0;
    while (lower <= upper) {
      pos = lower + (upper - lower) / 2;
      res = entries[pos].key.compareTo(tKey);
      if (res == 0) {
        break;
      } else if (res < 0) {
        lower = pos + 1;
      } else {
        upper = pos - 1;
      }
    }

    if (res != 0) {
      return;
    }

    // System.out.print("Matches for " + tKey + ":");
    int i = pos;
    do {
      // System.out.print(" " + entries[i].key);
      matches.add(entries[i].value);
    } while ((++i < size) && (entries[i].key.compareTo(tKey) == 0));

    i = pos - 1;
    while ((i >= 0) && (entries[i].key.compareTo(tKey) == 0)) {
      // System.out.print(" " + entries[i].key);
      matches.add(entries[i--].value);
    }
    // System.out.println();
  }

  @Override
  protected List<TEntry> entries() {
    return Arrays.asList(Arrays.copyOfRange(entries, 0, size));
  }
}
