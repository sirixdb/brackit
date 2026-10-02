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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * A join table for build keys of mixed types.
 * <p>
 * The table is filled by {@link #add(Sequence, Sequence[], int)}, which is not
 * thread-safe, and completed by {@link #seal()}. From then on the build keys
 * never change and {@link #probe(Sequence)} may be called by any number of
 * threads at once, as the block pipeline does.
 * <p>
 * A probe key is compared with a numeric build key of another type in the
 * wider of the two types, and with an untyped build key in its own type
 * (xs:double if it is numeric). Where that takes a copy of the build keys in
 * the other type, the first probe that needs the copy makes it. A copy is made
 * at most once and never changes once another probe can see it.
 *
 * @author Sebastian Baechle
 */
public class MultiTypeJoinTable {

  private final Cmp cmp;

  private final boolean isGCmp;

  private final boolean skipSort;

  // The build keys by their primitive type, in the order the types came.
  private final Map<Type, AbstractJoinTable> tables = new LinkedHashMap<>();

  // What a probe reads of the build keys. All of it is written once by seal()
  // before "sealed" is set, and a probe reads "sealed" first.
  private AbstractJoinTable integers;

  private AbstractJoinTable decimals;

  private AbstractJoinTable floats;

  private AbstractJoinTable doubles;

  private AbstractJoinTable untyped;

  private Type[] nonNumericTypes;

  private AbstractJoinTable[] nonNumericTables;

  private boolean numericPresent;

  private volatile boolean sealed;

  // Copies of numeric build keys in a wider numeric type, by that type. These
  // must stay out of "tables": a probe of the narrower type must never see a
  // widened key, only a probe of the wider type may.
  private final ConcurrentHashMap<Type, AbstractJoinTable> widenedTables = new ConcurrentHashMap<>();

  // Copies of the untyped build keys in the type a probe key compares them in,
  // by that type. These stay out of "tables" too: an untyped probe key is
  // compared with an untyped build key as a string, never in another type.
  private final ConcurrentHashMap<Type, AbstractJoinTable> convertedTables = new ConcurrentHashMap<>();

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
  }

  private void probeItem(FastList<TValue> matches, Item key) throws QueryException {
    Atomic atomic = key.atomize();
    Type type = atomic.type().getPrimitiveBase();

    if (!isGCmp && type == Type.UNA) {
      atomic = Cast.cast(null, atomic, Type.STR, false);
      type = Type.STR;
    }

    if (type == Type.UNA) {
      for (int i = 0; i < nonNumericTypes.length; i++) {
        probeCast(matches, atomic, nonNumericTypes[i], nonNumericTables[i], null);
      }
      if (numericPresent) {
        // An untyped probe reaches every numeric build type through xs:double,
        // so this is the only cast lookup that must include the widened keys.
        probeCast(matches, atomic, Type.DBL, doubles, widenedTo(Type.DBL));
      }
    } else if (type.isNumeric()) {
      // Untyped build keys are compared with a numeric probe key as xs:double.
      AbstractJoinTable untypedDoubles = untypedAs(Type.DBL);

      if (type == Type.DBL) {
        lookupIn(matches, doubles, atomic);
        lookupIn(matches, untypedDoubles, atomic);
        lookupIn(matches, widenedTo(Type.DBL), atomic);
      } else if (type == Type.FLO) {
        probeCast(matches, atomic, Type.DBL, doubles, untypedDoubles);
        lookupIn(matches, floats, atomic);
        lookupIn(matches, widenedTo(Type.FLO), atomic);
      } else if (type == Type.DEC) {
        probeCast(matches, atomic, Type.DBL, doubles, untypedDoubles);
        probeCast(matches, atomic, Type.FLO, floats, null);
        lookupIn(matches, decimals, atomic);
        lookupIn(matches, widenedTo(Type.DEC), atomic);
      } else if (type == Type.INR) {
        probeCast(matches, atomic, Type.DBL, doubles, untypedDoubles);
        probeCast(matches, atomic, Type.FLO, floats, null);
        probeCast(matches, atomic, Type.DEC, decimals, null);
        lookupIn(matches, integers, atomic);
      }
    } else {
      // Untyped build keys are compared with any other probe key in its type.
      AbstractJoinTable untypedOfType = untypedAs(type);

      lookupIn(matches, tables.get(type), atomic);
      lookupIn(matches, untypedOfType, atomic);

      if (type == Type.STR) {
        probeCast(matches, atomic, Type.AURI, tables.get(Type.AURI), null);
      } else if (type == Type.AURI) {
        probeCast(matches, atomic, Type.STR, tables.get(Type.STR), null);
      }
    }
  }

  // The numeric build keys narrower than the given type as copies in that
  // type, or null if there are none.
  private AbstractJoinTable widenedTo(Type type) throws QueryException {
    if (!hasKeysNarrowerThan(type)) {
      return null;
    }
    AbstractJoinTable widened = widenedTables.get(type);
    return widened != null ? widened : widenedTables.computeIfAbsent(type, this::widen);
  }

  // xs:integer is narrower than xs:decimal, which is narrower than xs:float,
  // which is narrower than xs:double.
  private boolean hasKeysNarrowerThan(Type type) {
    if (integers != null) {
      return true;
    }
    if (type == Type.DEC) {
      return false;
    }
    return decimals != null || (type == Type.DBL && floats != null);
  }

  private AbstractJoinTable widen(Type type) throws QueryException {
    AbstractJoinTable widened = createTable();
    copy(integers, widened, type);
    if (type != Type.DEC) {
      copy(decimals, widened, type);
    }
    if (type == Type.DBL) {
      copy(floats, widened, type);
    }
    widened.seal();
    return widened;
  }

  // The untyped build keys as copies in the given type, or null if there are
  // none. A key that cannot be cast fails every probe that asks for the type.
  private AbstractJoinTable untypedAs(Type type) throws QueryException {
    if (untyped == null) {
      return null;
    }
    AbstractJoinTable converted = convertedTables.get(type);
    return converted != null ? converted : convertedTables.computeIfAbsent(type, this::convertUntyped);
  }

  private AbstractJoinTable convertUntyped(Type type) throws QueryException {
    AbstractJoinTable converted = createTable();
    copy(untyped, converted, type);
    converted.seal();
    return converted;
  }

  private static void copy(AbstractJoinTable from, AbstractJoinTable to, Type type) throws QueryException {
    if (from == null) {
      return;
    }
    for (TEntry entry : from.entries()) {
      to.add(Cast.cast(null, entry.key.atomic, type, false), entry.value.pos, entry.value.bindings);
    }
  }

  private static void probeCast(FastList<TValue> matches, Atomic atomic, Type type, AbstractJoinTable table,
      AbstractJoinTable copies) throws QueryException {
    if (table == null && copies == null) {
      return;
    }
    Atomic key = Cast.cast(null, atomic, type, false);
    lookupIn(matches, table, key);
    lookupIn(matches, copies, key);
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
    if (sealed) {
      throw new IllegalStateException("The join table is sealed");
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

  /**
   * Completes the table: no key can be added afterwards, and probes only read
   * it. Whoever builds a table that several threads probe calls this before
   * handing the table to them. A table that was not sealed is sealed by its
   * first probe.
   */
  public final void seal() {
    if (!sealed) {
      sealOnce();
    }
  }

  private synchronized void sealOnce() {
    if (sealed) {
      return;
    }
    Type[] types = new Type[tables.size()];
    AbstractJoinTable[] typeTables = new AbstractJoinTable[types.length];
    int size = 0;
    for (Map.Entry<Type, AbstractJoinTable> entry : tables.entrySet()) {
      entry.getValue().seal();
      if (!entry.getKey().isNumeric()) {
        types[size] = entry.getKey();
        typeTables[size++] = entry.getValue();
      }
    }
    nonNumericTypes = Arrays.copyOf(types, size);
    nonNumericTables = Arrays.copyOf(typeTables, size);
    integers = tables.get(Type.INR);
    decimals = tables.get(Type.DEC);
    floats = tables.get(Type.FLO);
    doubles = tables.get(Type.DBL);
    untyped = tables.get(Type.UNA);
    numericPresent = integers != null || decimals != null || floats != null || doubles != null;
    sealed = true;
  }

  public final FastList<Sequence[]> probe(Sequence keys) throws QueryException {
    if (keys == null) {
      return FastList.emptyList();
    }
    if (!sealed) {
      sealOnce();
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
