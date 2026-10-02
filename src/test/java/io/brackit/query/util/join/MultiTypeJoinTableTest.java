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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Dec;
import io.brackit.query.atomic.Flt;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.Cmp;

/** One join table probed by several threads at once, as the block pipeline does. */
public class MultiTypeJoinTableTest {

  private static final Cmp[] CMPS = { Cmp.eq, Cmp.lt, Cmp.le, Cmp.gt, Cmp.ge };

  /** The build position of every matching row, in the order the table returns them. */
  private static List<Integer> positions(FastList<Sequence[]> matches) {
    var positions = new ArrayList<Integer>();
    for (int i = 0; i < matches.getSize(); i++) {
      positions.add(((Int32) matches.get(i)[0]).intValue());
    }
    return positions;
  }

  private static Sequence[] row(int pos) {
    return new Sequence[] { new Int32(pos) };
  }

  /** An integer that holds up the first comparison with another gated integer in the gate's thread. */
  private static final class GatedInteger extends Int32 {
    private final Gate gate;

    GatedInteger(int v, Gate gate) {
      super(v);
      this.gate = gate;
    }

    @Override
    public int atomicCmpInternal(Atomic other) {
      if (other instanceof GatedInteger) {
        // two build keys are compared: the table is being sorted
        gate.pass();
      }
      return super.atomicCmpInternal(other);
    }
  }

  private static final class Gate {
    final CountDownLatch reached = new CountDownLatch(1);
    final CountDownLatch opened = new CountDownLatch(1);
    volatile Thread holdUp;

    void pass() {
      if (Thread.currentThread() == holdUp) {
        holdUp = null;
        reached.countDown();
        try {
          opened.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  // A probe that finds the table unsorted must not let a second probe search it
  // or sort it as well while the first one is still sorting. The first probe is
  // held up in the middle of sorting, the second one runs to its end or until it
  // waits for the first one, and only then does the first one go on.
  @Test
  @Timeout(30)
  public void probesNeverSeeATableBeingSorted() throws Exception {
    var gate = new Gate();
    var table = new MultiTypeJoinTable(Cmp.gt, false, false);
    int[] keys = { 5, 4, 3, 2, 1 };
    for (int i = 0; i < keys.length; i++) {
      table.add(new GatedInteger(keys[i], gate), row(i + 1), i + 1);
    }

    var first = new AtomicReference<Object>();
    var second = new AtomicReference<Object>();
    Thread a = new Thread(() -> first.set(probeOrFailure(table, new Int32(3))));
    gate.holdUp = a;
    a.start();
    while (!gate.reached.await(10, TimeUnit.MILLISECONDS) && a.isAlive()) {
      // the first probe sorts the table, unless the table was sorted already
    }
    Thread b = new Thread(() -> second.set(probeOrFailure(table, new Int32(3))));
    b.start();
    while (b.isAlive() && !waitsFor(b, a)) {
      Thread.onSpinWait();
    }
    gate.opened.countDown();
    a.join();
    b.join();

    // build keys 2 and 1 are less than 3, at build positions 4 and 5
    assertEquals(List.of(4, 5), first.get());
    assertEquals(List.of(4, 5), second.get());
    for (int probe = 0; probe <= 6; probe++) {
      var expected = new ArrayList<Integer>();
      for (int i = 0; i < keys.length; i++) {
        if (probe > keys[i]) {
          expected.add(i + 1);
        }
      }
      expected.sort(null);
      assertEquals(expected, positions(table.probe(new Int32(probe))), "probe " + probe);
    }
  }

  private static Object probeOrFailure(MultiTypeJoinTable table, Atomic key) {
    try {
      return positions(table.probe(key));
    } catch (RuntimeException e) {
      return e;
    }
  }

  private static boolean waitsFor(Thread waiting, Thread owner) {
    var info = ManagementFactory.getThreadMXBean().getThreadInfo(waiting.threadId());
    return info != null && info.getLockOwnerId() == owner.threadId();
  }

  private static Stream<Arguments> comparisons() {
    var cases = new ArrayList<Arguments>();
    for (Cmp cmp : CMPS) {
      for (boolean general : new boolean[] { false, true }) {
        for (boolean sealedByBuilder : new boolean[] { false, true }) {
          cases.add(Arguments.of(cmp, general, sealedByBuilder));
        }
      }
    }
    return cases.stream();
  }

  // Build and probe keys of every numeric type and untyped ones, so that the
  // probes need every kind of widened and converted copy of the build keys.
  private static Atomic mixedKey(int i, int value) {
    return switch (i % 5) {
      case 0 -> new Int32(value);
      case 1 -> new Dec(new BigDecimal(value).add(new BigDecimal("0.5")));
      case 2 -> new Flt(value);
      case 3 -> new Dbl(value + 0.25);
      default -> new Una(Integer.toString(value));
    };
  }

  private static MultiTypeJoinTable mixedTable(Cmp cmp, boolean general, int size) {
    var table = new MultiTypeJoinTable(cmp, general, false);
    for (int i = 0; i < size; i++) {
      table.add(mixedKey(i, (i * 7919) % size), row(i + 1), i + 1);
    }
    return table;
  }

  @ParameterizedTest
  @MethodSource("comparisons")
  @Timeout(60)
  public void concurrentProbesGetTheRowsOfSequentialProbes(Cmp cmp, boolean general, boolean sealedByBuilder)
      throws Exception {
    int size = 1500;
    Atomic[] probes = new Atomic[24];
    for (int i = 0; i < probes.length; i++) {
      probes[i] = mixedKey(i * 3 + 1, i * 37 % 60);
    }
    var sequentialTable = mixedTable(cmp, general, size);
    var expected = new ArrayList<List<Integer>>();
    for (Atomic probe : probes) {
      expected.add(positions(sequentialTable.probe(probe)));
    }

    int threads = 6;
    for (int round = 0; round < 6; round++) {
      var table = mixedTable(cmp, general, size);
      if (sealedByBuilder) {
        table.seal();
      }
      var start = new CountDownLatch(1);
      var failures = new ArrayList<String>();
      var workers = new ArrayList<Thread>();
      for (int t = 0; t < threads; t++) {
        int offset = t * 7;
        Thread worker = new Thread(() -> {
          try {
            start.await();
            for (int k = 0; k < probes.length; k++) {
              int i = (k + offset) % probes.length;
              var actual = positions(table.probe(probes[i]));
              if (!actual.equals(expected.get(i))) {
                synchronized (failures) {
                  failures.add("probe " + probes[i] + " got other rows than the sequential probe (" + actual.size()
                      + " instead of " + expected.get(i).size() + ")");
                }
              }
            }
          } catch (Throwable e) {
            synchronized (failures) {
              failures.add(e.toString());
            }
          }
        });
        workers.add(worker);
        worker.start();
      }
      start.countDown();
      for (Thread worker : workers) {
        worker.join();
      }
      assertEquals(List.of(), failures, "round " + round);
    }
  }

  /** An integer that counts how often it is cast, which takes its string value. */
  private static final class CountedInteger extends Int32 {
    private final AtomicInteger casts;

    CountedInteger(int v, AtomicInteger casts) {
      super(v);
      this.casts = casts;
    }

    @Override
    public String stringValue() {
      casts.incrementAndGet();
      return super.stringValue();
    }
  }

  // Every double probe needs the integer build keys as doubles. They are copied
  // once, by the first of the probes, and every other probe uses that copy.
  @Test
  @Timeout(60)
  public void buildKeysAreWidenedOnceForAllProbes() throws Exception {
    int size = 2000;
    var casts = new AtomicInteger();
    var table = new MultiTypeJoinTable(Cmp.eq, false, false);
    for (int i = 0; i < size; i++) {
      table.add(new CountedInteger(i, casts), row(i + 1), i + 1);
    }
    table.seal();
    assertEquals(0, casts.get());

    var start = new CountDownLatch(1);
    var rows = new AtomicInteger();
    var workers = new ArrayList<Thread>();
    for (int t = 0; t < 8; t++) {
      Thread worker = new Thread(() -> {
        try {
          start.await();
          for (int i = 0; i < size; i += 10) {
            rows.addAndGet(table.probe(new Dbl(i)).getSize());
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
      workers.add(worker);
      worker.start();
    }
    start.countDown();
    for (Thread worker : workers) {
      worker.join();
    }
    assertEquals(8 * size / 10, rows.get());
    assertEquals(size, casts.get());
  }

  // An untyped probe key is compared with an untyped build key as a string. A
  // numeric probe before it compares the untyped build keys as doubles, and that
  // must not leak into the untyped probe: "1.0" equals 1 but not "1".
  @Test
  public void earlierProbesDoNotChangeTheRowsOfLaterProbes() {
    Atomic[] build = { new Una("1"), new Una("01"), new Int32(2), new Str("abc") };
    Atomic[] probes = { new Int32(1), new Una("1.0"), new Una("1"), new Dbl(2), new Una("2.0"), new Str("abc") };
    for (Cmp cmp : CMPS) {
      var forward = new MultiTypeJoinTable(cmp, true, false);
      var backward = new MultiTypeJoinTable(cmp, true, false);
      for (int i = 0; i < build.length; i++) {
        forward.add(build[i], row(i + 1), i + 1);
        backward.add(build[i], row(i + 1), i + 1);
      }
      var forwardRows = new ArrayList<Object>();
      var backwardRows = new ArrayList<Object>();
      for (int i = 0; i < probes.length; i++) {
        forwardRows.add(probeOrFailure(forward, probes[i]));
        backwardRows.add(0, probeOrFailure(backward, probes[probes.length - 1 - i]));
      }
      assertEquals(forwardRows.toString(), backwardRows.toString(), cmp.toString());
    }

    var table = new MultiTypeJoinTable(Cmp.eq, true, false);
    for (int i = 0; i < build.length; i++) {
      table.add(build[i], row(i + 1), i + 1);
    }
    assertEquals(List.of(1, 2), positions(table.probe(new Int32(1))));
    assertEquals(List.of(), positions(table.probe(new Una("1.0"))));
    assertEquals(List.of(1), positions(table.probe(new Una("1"))));
    assertEquals(List.of(3), positions(table.probe(new Una("2.0"))));
  }

  // Untyped build keys are cast to the type of a probe key only when such a
  // probe comes. One that cannot be cast fails every probe that needs the cast,
  // and no other probe.
  @Test
  public void anUntypedBuildKeyThatCannotBeCastFailsOnlyProbesThatCastIt() {
    var table = new MultiTypeJoinTable(Cmp.eq, true, false);
    table.add(new Una("abc"), row(1), 1);
    table.add(new Una("1"), row(2), 2);
    table.seal();
    assertEquals(List.of(1), positions(table.probe(new Str("abc"))));
    assertThrows(QueryException.class, () -> table.probe(new Int32(1)));
    assertThrows(QueryException.class, () -> table.probe(new Dbl(1)));
    assertEquals(List.of(2), positions(table.probe(new Una("1"))));
    assertEquals(List.of(1), positions(table.probe(new Una("abc"))));
  }

  @Test
  public void keysCannotBeAddedOnceTheTableIsSealed() {
    var table = new MultiTypeJoinTable(Cmp.lt, false, false);
    table.add(new Int32(1), row(1), 1);
    table.seal();
    assertThrows(IllegalStateException.class, () -> table.add(new Int32(2), row(2), 2));

    var probed = new MultiTypeJoinTable(Cmp.lt, false, false);
    probed.add(new Int32(1), row(1), 1);
    assertEquals(List.of(1), positions(probed.probe(new Int32(0))));
    assertThrows(IllegalStateException.class, () -> probed.add(new Int32(2), row(2), 2));
  }
}
