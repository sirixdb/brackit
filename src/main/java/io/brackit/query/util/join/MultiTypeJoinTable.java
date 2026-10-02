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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.util.Cmp;
import io.brackit.query.QueryException;
import io.brackit.query.expr.Cast;
import io.brackit.query.util.join.AbstractJoinTable.TEntry;
import io.brackit.query.util.join.AbstractJoinTable.TValue;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class MultiTypeJoinTable {

  private final Cmp cmp;

  private final boolean isGCmp;

  private final boolean skipSort;

  private final Map<Type, AbstractJoinTable> tables = new HashMap<>();

  // Copies of build keys widened to a wider numeric type. These must stay out
  // of "tables": a probe of the narrower type must never see a widened key,
  // only a probe of the wider type may, so the two maps are looked up together
  // for the probed type and never merged.
  private final Map<Type, AbstractJoinTable> promotedTables = new HashMap<>();

  private final Set<Type> convertedUntypedAtomic = new HashSet<>();

  private final Set<Type> nonNumericTypes = new HashSet<>();

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

    if (!isGCmp && type == Type.UNA) {
      atomic = Cast.cast(null, atomic, Type.STR, false);
      type = Type.STR;
    }

    if (type == Type.UNA) {
      for (Type nnType : nonNumericTypes) {
        probeCast(matches, atomic, nnType, false);
      }
      if (numericPresent) {
        if (!promotedNumericToDbl) {
          addToTable(promotedTables, Type.INR, Type.DBL);
          addToTable(promotedTables, Type.DEC, Type.DBL);
          addToTable(promotedTables, Type.FLO, Type.DBL);
          promotedNumericToDbl = true;
        }
        // An untyped probe reaches every numeric build type through xs:double,
        // so this is the only lookup that must include the promoted keys.
        probeCast(matches, atomic, Type.DBL, true);
      }
    } else if (type.isNumeric()) {
      // convert all untyped to dbl and add them
      if (!convertedUntypedAtomicToDbl) {
        addToTable(tables, Type.UNA, Type.DBL);
        convertedUntypedAtomicToDbl = true;
      }

      if (type == Type.DBL) {
        if (!promotedNumericToDbl) {
          addToTable(promotedTables, Type.INR, Type.DBL);
          addToTable(promotedTables, Type.DEC, Type.DBL);
          addToTable(promotedTables, Type.FLO, Type.DBL);
          promotedNumericToDbl = true;
        }
      } else if (type == Type.FLO) {
        if (!promotedNumericToFlo) {
          addToTable(promotedTables, Type.INR, Type.FLO);
          addToTable(promotedTables, Type.DEC, Type.FLO);
          promotedNumericToFlo = true;
        }
        probeCast(matches, atomic, Type.DBL, false);
      } else if (type == Type.DEC) {
        if (!promotedNumericToDec) {
          addToTable(promotedTables, Type.INR, Type.DEC);
          promotedNumericToDec = true;
        }
        probeCast(matches, atomic, Type.DBL, false);
        probeCast(matches, atomic, Type.FLO, false);
      } else if (type == Type.INR) {
        probeCast(matches, atomic, Type.DBL, false);
        probeCast(matches, atomic, Type.FLO, false);
        probeCast(matches, atomic, Type.DEC, false);
      }

      probeAtomic(matches, atomic, type);
    } else {
      // convert all untyped to type and add them
      if (!convertedUntypedAtomic.contains(type)) {
        addToTable(tables, Type.UNA, type);
        convertedUntypedAtomic.add(type);
      }

      probeAtomic(matches, atomic, type);

      if (type == Type.STR) {
        probeCast(matches, atomic, Type.AURI, false);
      } else if (type == Type.AURI) {
        probeCast(matches, atomic, Type.STR, false);
      }
    }
  }

  private void addToTable(Map<Type, AbstractJoinTable> target, Type from, Type to) throws QueryException {
    AbstractJoinTable fromTable = tables.get(from);

    if (fromTable == null) {
      return;
    }

    AbstractJoinTable table = target.get(to);
    if (table == null) {
      table = createTable();
      target.put(to, table);
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
    lookupIn(matches, tables.get(type), atomic);
    lookupIn(matches, promotedTables.get(type), atomic);
  }

  private void probeCast(FastList<TValue> matches, Atomic atomic, Type type, boolean includePromoted)
      throws QueryException {
    AbstractJoinTable table = tables.get(type);
    AbstractJoinTable promoted = includePromoted ? promotedTables.get(type) : null;
    if (table == null && promoted == null) {
      return;
    }
    Atomic key = Cast.cast(null, atomic, type, false);
    lookupIn(matches, table, key);
    lookupIn(matches, promoted, key);
  }

  private static void lookupIn(FastList<TValue> matches, AbstractJoinTable table, Atomic key) throws QueryException {
    if (table != null) {
      table.lookup(matches, key);
    }
  }

  protected final FastList<Sequence[]> sortAndDeduplicate(FastList<TValue> in) throws QueryException {
    int inSize = in.getSize();
    if (inSize < 2) {
      FastList<Sequence[]> out = new FastList<>(inSize);
      for (int i = 0; i < inSize; i++) {
        out.add(in.get(i).bindings);
      }
      return out;
    } else if (skipSort) {
      // Order does not matter here, but a build row must still be emitted at
      // most once per probe: it is reachable through more than one type table,
      // and a general comparison probes with every item of the key sequence.
      FastList<Sequence[]> out = new FastList<>(inSize);
      int[] seen = new int[Integer.highestOneBit(inSize) << 2];
      for (int i = 0; i < inSize; i++) {
        TValue v = in.get(i);
        if (addSeenPos(seen, v.pos)) {
          out.add(v.bindings);
        }
      }
      return out;
    } else {
      in.sort();
      FastList<Sequence[]> out = new FastList<>();
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

  // Open-addressed set of build positions. Positions are assigned from 1 by
  // the join's table build loop, so 0 marks a free slot, and the table holds
  // more than twice as many slots as there are matches, so the probe sequence
  // always hits one.
  private static boolean addSeenPos(int[] seen, int pos) {
    int mask = seen.length - 1;
    int hash = pos * 0x9E3779B1;
    int slot = (hash ^ (hash >>> 16)) & mask;
    while (seen[slot] != 0) {
      if (seen[slot] == pos) {
        return false;
      }
      slot = (slot + 1) & mask;
    }
    seen[slot] = pos;
    return true;
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