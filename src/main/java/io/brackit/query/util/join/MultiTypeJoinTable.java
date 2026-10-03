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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.util.Cmp;
import io.brackit.query.ErrorCode;
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
 * threads at once, as the block pipeline does. A table that was not sealed
 * cannot be probed.
 * <p>
 * A probe key is compared with a numeric build key of another type in the
 * wider of the two types, and with an untyped build key in its own type
 * (xs:double if it is numeric). Where that takes a copy of the build keys in
 * the other type, the copy is made by the first probe that needs it and not by
 * {@link #seal()}: a join whose keys are all of one type needs no copy at all,
 * and making every copy up front would cost that common case a multiple of
 * its build keys in memory for copies no probe ever reads.
 * <p>
 * What the probe path pays for that is one volatile read of the copies made so
 * far, which hold one entry per type a probe has asked for. Each of those
 * entries is made exactly once, however many threads probe: the first probe
 * that needs a copy claims the entry with a compare-and-set and makes the copy
 * without holding a lock, and the probes that need the same copy meanwhile
 * wait for that one instead of making their own, so the work and the memory
 * stay those of a single copy no matter how wide the probe side is. They wait
 * through {@link ForkJoinPool#managedBlock}, not on a monitor, so a pool
 * probing the table keeps its parallelism while they do (see
 * {@link io.brackit.query.block.MutexSink} for what a plain monitor costs it
 * here). A copy that cannot be made - an untyped build key that the probe's
 * type cannot be cast to - fails the probe making it and every probe waiting
 * for it with the same error, and is not attempted again.
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

  // One copy of the build keys in one other type. The probe that puts the
  // entry in place makes the copy, and the probes that find it in place wait
  // for whatever it ends up holding: the copy, or the error that making it
  // raised.
  private static final class Copy implements ForkJoinPool.ManagedBlocker {
    private final CountDownLatch made = new CountDownLatch(1);

    private volatile AbstractJoinTable table;

    private volatile Throwable failure;

    private AbstractJoinTable make(Type type, Function<Type, AbstractJoinTable> copier) throws QueryException {
      try {
        AbstractJoinTable copy = copier.apply(type);
        table = copy;
        return copy;
      } catch (Throwable e) {
        failure = e;
        throw e;
      } finally {
        made.countDown();
      }
    }

    private AbstractJoinTable get() throws QueryException {
      AbstractJoinTable copy = table;
      if (copy != null) {
        return copy;
      }
      try {
        ForkJoinPool.managedBlock(this);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR, "Interrupted while waiting for the join build keys");
      }
      Throwable failed = failure;
      if (failed instanceof RuntimeException unchecked) {
        throw unchecked;
      }
      if (failed instanceof Error error) {
        throw error;
      }
      return table;
    }

    @Override
    public boolean block() throws InterruptedException {
      made.await();
      return true;
    }

    @Override
    public boolean isReleasable() {
      return made.getCount() == 0;
    }
  }

  // The copies of the build keys made so far, as a probe reads them: one
  // volatile read of the holding reference takes the whole set, and looking a
  // type up in it writes nothing.
  private static final class Copies {
    static final Copies NONE = new Copies(new Type[0], new Copy[0]);

    // The primitive type constants that "tables" is keyed by, which a probe
    // key and a build key of the same type share, so they are compared by
    // identity.
    private final Type[] types;

    private final Copy[] copies;

    private Copies(Type[] types, Copy[] copies) {
      this.types = types;
      this.copies = copies;
    }

    private Copy get(Type type) {
      for (int i = 0; i < types.length; i++) {
        if (types[i] == type) {
          return copies[i];
        }
      }
      return null;
    }

    private Copies with(Type type, Copy copy) {
      Type[] grownTypes = Arrays.copyOf(types, types.length + 1);
      Copy[] grownCopies = Arrays.copyOf(copies, copies.length + 1);
      grownTypes[types.length] = type;
      grownCopies[copies.length] = copy;
      return new Copies(grownTypes, grownCopies);
    }
  }

  // Copies of numeric build keys in a wider numeric type, by that type. These
  // must stay out of "tables": a probe of the narrower type must never see a
  // widened key, only a probe of the wider type may.
  private final AtomicReference<Copies> widened = new AtomicReference<>(Copies.NONE);

  // Copies of the untyped build keys in the type a probe key compares them in,
  // by that type. These stay out of "tables" too: an untyped probe key is
  // compared with an untyped build key as a string, never in another type.
  private final AtomicReference<Copies> converted = new AtomicReference<>(Copies.NONE);

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

    // xs:anyURI is promoted to xs:string for comparisons on either side.
    if (type == Type.AURI || (!isGCmp && type == Type.UNA)) {
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

    if (type == Type.AURI || (!isGCmp && type == Type.UNA)) {
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
    }
  }

  // The numeric build keys narrower than the given type as copies in that
  // type, or null if there are none.
  private AbstractJoinTable widenedTo(Type type) throws QueryException {
    if (!hasKeysNarrowerThan(type)) {
      return null;
    }
    return copyIn(widened, type, this::widen);
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
    AbstractJoinTable widenedKeys = createTable();
    copy(integers, widenedKeys, type);
    if (type != Type.DEC) {
      copy(decimals, widenedKeys, type);
    }
    if (type == Type.DBL) {
      copy(floats, widenedKeys, type);
    }
    widenedKeys.seal();
    return widenedKeys;
  }

  // The untyped build keys as copies in the given type, or null if there are
  // none. A key that cannot be cast fails every probe that asks for the type.
  private AbstractJoinTable untypedAs(Type type) throws QueryException {
    if (untyped == null) {
      return null;
    }
    return copyIn(converted, type, this::convertUntyped);
  }

  private AbstractJoinTable convertUntyped(Type type) throws QueryException {
    AbstractJoinTable convertedKeys = createTable();
    copy(untyped, convertedKeys, type);
    convertedKeys.seal();
    return convertedKeys;
  }

  // The one copy of the build keys in the given type: made here if this probe
  // is the one that claims it, waited for if another probe claimed it first.
  private static AbstractJoinTable copyIn(AtomicReference<Copies> copies, Type type,
      Function<Type, AbstractJoinTable> copier) throws QueryException {
    Copies current = copies.get();
    Copy copy = current.get(type);
    if (copy != null) {
      return copy.get();
    }
    Copy claim = new Copy();
    while (!copies.compareAndSet(current, current.with(type, claim))) {
      current = copies.get();
      copy = current.get(type);
      if (copy != null) {
        return copy.get();
      }
    }
    return claim.make(type, copier);
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
   * handing the table to them, in the thread that built it; a table that was
   * not sealed cannot be probed.
   */
  public final void seal() {
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
    if (!sealed) {
      throw new IllegalStateException("The join table is not sealed");
    }
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
