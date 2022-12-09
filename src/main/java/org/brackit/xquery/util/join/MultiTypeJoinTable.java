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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.util.join.AbstractJoinTable.TEntry;
import org.brackit.xquery.util.join.AbstractJoinTable.TValue;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * @author Sebastian Baechle
 */
public class MultiTypeJoinTable {

  private final Cmp cmp;

  private final boolean isGCmp;

  private final boolean skipSort;

  private final Map<Type, AbstractJoinTable> tables = new HashMap<Type, AbstractJoinTable>();

  private final Set<Type> convertedUntypedAtomic = new HashSet<Type>();

  private final Set<Type> nonNumericTypes = new HashSet<Type>();

  private boolean convertedUntypedAtomicToDbl;

  private boolean promotedNumericToDbl;

  private boolean promotedNumericToFlo;

  private boolean promotedNumericToDec;

  private boolean numericPresent;

  public MultiTypeJoinTable(Cmp cmp, boolean isGCmp, boolean skipSort) {
    this.cmp = cmp;
    this.isGCmp = isGCmp;
    this.skipSort = skipSort;
  }

  private AbstractJoinTable createTable() {
    return cmp == Cmp.eq ? new HashJoinTable() : new SortedJoinTable(cmp);
  }

  private void addItem(Item key, Sequence[] bindings, int pos) throws QueryException {
    Atomic atomic = key.atomize();
    Type type = atomic.type().getPrimitiveBase();

    if (!isGCmp && type == Type.UNA) {
      atomic = Cast.cast(null, atomic, Type.STR, false);
      type = Type.STR;
    }

    AbstractJoinTable table = tables.get(type);
    if (table == null) {
      table = createTable();
      tables.put(type, table);
    }
    table.add(atomic, pos, bindings);
    if (type.isNumeric()) {
      numericPresent = true;
    } else {
      nonNumericTypes.add(type);
    }
  }

  private void probeItem(FastList<TValue> matches, Item key) throws QueryException {
    Atomic atomic = key.atomize();
    Type type = atomic.type().getPrimitiveBase();

    if ((!isGCmp) && (type == Type.UNA)) {
      atomic = Cast.cast(null, atomic, Type.STR, false);
      type = Type.STR;
    }

    if (type == Type.UNA) {
      for (Type nnType : nonNumericTypes) {
        probeAtomic(matches, Cast.cast(null, atomic, nnType, false), nnType);
      }
      if (numericPresent) {
        if (!promotedNumericToDbl) {
          addToTable(Type.INR, Type.DBL);
          addToTable(Type.DEC, Type.DBL);
          addToTable(Type.FLO, Type.DBL);
          promotedNumericToDbl = true;
        }
        probeAtomic(matches, Cast.cast(null, atomic, Type.DBL, false), Type.DBL);
      }
    } else if (type.isNumeric()) {
      // convert all untyped to dbl and add them
      if (!convertedUntypedAtomicToDbl) {
        addToTable(Type.UNA, Type.DBL);
        convertedUntypedAtomicToDbl = true;
      }

      if ((type == Type.DBL) && (!promotedNumericToDbl)) {
        addToTable(Type.INR, Type.DBL);
        addToTable(Type.DEC, Type.DBL);
        addToTable(Type.FLO, Type.DBL);
        promotedNumericToDbl = true;
      } else if ((type == Type.FLO) && (!promotedNumericToFlo)) {
        addToTable(Type.INR, Type.FLO);
        addToTable(Type.DEC, Type.FLO);
        promotedNumericToFlo = true;
        probeAtomic(matches, Cast.cast(null, atomic, Type.DBL, false), Type.DBL);
      } else if ((type == Type.DEC) && (!promotedNumericToDec)) {
        addToTable(Type.INR, Type.DEC);
        promotedNumericToDec = true;
        probeAtomic(matches, Cast.cast(null, atomic, Type.DBL, false), Type.DBL);
        probeAtomic(matches, Cast.cast(null, atomic, Type.FLO, false), Type.FLO);
      } else if (type == Type.INR) {
        probeAtomic(matches, Cast.cast(null, atomic, Type.DBL, false), Type.DBL);
        probeAtomic(matches, Cast.cast(null, atomic, Type.FLO, false), Type.FLO);
        probeAtomic(matches, Cast.cast(null, atomic, Type.DEC, false), Type.DEC);
      }

      probeAtomic(matches, atomic, type);
    } else {
      // convert all untyped to type and add them
      if (!convertedUntypedAtomic.contains(type)) {
        addToTable(Type.UNA, type);
        convertedUntypedAtomic.add(type);
      }

      probeAtomic(matches, atomic, type);

      if (type == Type.STR) {
        probeAtomic(matches, Cast.cast(null, atomic, Type.AURI, false), Type.AURI);
      } else if (type == Type.AURI) {
        probeAtomic(matches, Cast.cast(null, atomic, Type.STR, false), Type.STR);
      }
    }
  }

  private void addToTable(Type from, Type to) throws QueryException {
    AbstractJoinTable fromTable = tables.get(from);

    if (fromTable == null) {
      return;
    }

    AbstractJoinTable table = tables.get(to);
    if (table == null) {
      table = createTable();
      tables.put(to, table);
    }

    for (TEntry entry : fromTable.entries()) {
      table.add(Cast.cast(null, entry.key.atomic, to, false), entry.value.pos, entry.value.bindings);
    }

    if (to.isNumeric()) {
      numericPresent = true;
    } else {
      nonNumericTypes.add(to);
    }
  }

  private void probeAtomic(FastList<TValue> matches, Atomic atomic, Type type) throws QueryException {
    AbstractJoinTable table = tables.get(type);
    if (table != null) {
      table.lookup(matches, atomic);
    }
  }

  protected final FastList<Sequence[]> sortAndDeduplicate(FastList<TValue> in) throws QueryException {
    int inSize = in.getSize();
    if ((skipSort) || (inSize < 2)) {
      FastList<Sequence[]> out = new FastList<Sequence[]>(inSize);
      for (int i = 0; i < inSize; i++) {
        out.add(in.get(i).bindings);
      }
      return out;
    } else {
      in.sort();
      FastList<Sequence[]> out = new FastList<Sequence[]>();
      TValue p = null;
      for (int i = 0; i < inSize; i++) {
        TValue v = in.get(i);
        if ((p == null) || (p.pos < v.pos)) {
          out.add(v.bindings);
        }
        p = v;
      }
      return out;
    }
  }

  public final void add(Sequence keys, Sequence[] bindings, int pos) throws QueryException {
    if (keys == null) {
      return;
    }
    if (keys instanceof Item item) {
      addItem(item, bindings, pos);
    } else {
      try (final Iter it = keys.iterate()) {
        Item key;
        while ((key = it.next()) != null) {
          addItem(key, bindings, pos);
        }
      }
    }
  }

  public final FastList<Sequence[]> probe(Sequence keys) throws QueryException {
    if (keys == null) {
      return FastList.emptyList();
    }

    final var matches = new FastList<TValue>();

    if (keys instanceof Item) {
      probeItem(matches, (Item) keys);
    } else {
      try (final Iter it = keys.iterate()) {
        Item key;
        while ((key = it.next()) != null) {
          probeItem(matches, key);
        }
      }
    }

    if (matches.isEmpty()) {
      return FastList.emptyList();
    }

    return sortAndDeduplicate(matches);
  }
}