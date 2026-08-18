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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.aggregator.Aggregate;

/**
 * Pins the memory discipline of {@link SpillableGroupBy}: a group table that does not fit its
 * budget must spill its overflow groups to disk and still answer exactly, rather than growing
 * until the heap ends.
 *
 * <p>The operator used to carry the whole partitioning apparatus as dead code — {@code
 * writeToPartition} and {@code initPartitionFiles} had no callers, the size counter was never
 * incremented and the budget was never read — so every generic-pipeline {@code group by} was an
 * unbounded hash table. A 100M-row grouping over ~40M distinct keys died with an OOM instead of
 * degrading to a slow answer.
 *
 * <p>Every case below runs the same input through the same operator twice, once with a budget
 * that fits and once with one that cannot, and requires the two answers to agree. The tiny-budget
 * arm admits a single group per pass, which also drives the recursive repartitioning of a
 * partition that does not fit on its own.
 */
final class SpillableGroupByTest {

  /** Group table budget that no run of this test can exceed. */
  private static final long BUDGET_FITS = 64L * 1024 * 1024;

  /** Group table budget that forces a spill after the first group of every pass. */
  private static final long BUDGET_ONE_GROUP = 1;

  /** Enough keys that the forced-spill arm has to repartition several times. */
  private static final int DISTINCT_KEYS = 331;

  private static final int ROWS = 4993;

  @Test
  void spilledGroupingAnswersLikeTheResidentOne() throws QueryException {
    final Map<String, long[]> resident = group(BUDGET_FITS);
    final Map<String, long[]> spilled = group(BUDGET_ONE_GROUP);

    assertEquals(DISTINCT_KEYS, resident.size(), "one group per distinct key");
    assertEquals(resident.keySet(), spilled.keySet(), "spilling must not lose or invent a group");
    for (final Map.Entry<String, long[]> e : resident.entrySet()) {
      final long[] expected = e.getValue();
      final long[] actual = spilled.get(e.getKey());
      assertEquals(expected[0], actual[0], "count of group " + e.getKey());
      assertEquals(expected[1], actual[1], "sum of group " + e.getKey());
      assertEquals(expected[2], actual[2], "min of group " + e.getKey());
    }
  }

  @Test
  void everyRowIsCountedExactlyOnce() throws QueryException {
    long counted = 0;
    for (final long[] agg : group(BUDGET_ONE_GROUP).values()) {
      counted += agg[0];
    }
    assertEquals(ROWS, counted, "a spilled row must be aggregated exactly once");
  }

  @Test
  void spillFilesAreRemovedWhenTheCursorIsDrained() throws QueryException {
    final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    final int before = countSpillFiles(tmpDir);
    group(BUDGET_ONE_GROUP);
    assertEquals(before, countSpillFiles(tmpDir), "spill files must not survive the cursor");
  }

  /**
   * The forced-spill arm must really spill: an equal answer proves nothing on its own, since the
   * defect being guarded here was an operator that answered correctly right up to the point where
   * it exhausted the heap.
   */
  @Test
  void theForcedArmSpillsAndTheFittingArmDoesNot() throws QueryException {
    assertTrue(spilledTuples(BUDGET_ONE_GROUP) > 0, "a one-group budget must push tuples to disk");
    assertEquals(0, spilledTuples(BUDGET_FITS), "a grouping that fits must not touch the disk");
  }

  /** Runs the grouping to exhaustion and reports how many tuples went to a partition file. */
  private static long spilledTuples(long budget) throws QueryException {
    final QueryContext ctx = new BrackitQueryContext();
    final SpillableGroupBy.SpillableHashGroupByCursor cursor = drivenCursor(budget, ROWS, ctx);
    try {
      while (cursor.next(ctx) != null) {
        // drain
      }
      return cursor.spilledTuples();
    } finally {
      cursor.close(ctx);
    }
  }

  @Test
  void emptyInputYieldsNoGroup() throws QueryException {
    assertTrue(group(BUDGET_ONE_GROUP, 0).isEmpty(), "no input, no group");
  }

  @Test
  void theBudgetIsReadFromTheConfiguration() {
    final String previous = System.getProperty(SpillableGroupBy.MEMORY_BUDGET_CFG);
    try {
      System.setProperty(SpillableGroupBy.MEMORY_BUDGET_CFG, "4711");
      assertEquals(4711L, SpillableGroupBy.defaultMemoryBudget(), "the budget knob must be live");
    } finally {
      if (previous == null) {
        System.clearProperty(SpillableGroupBy.MEMORY_BUDGET_CFG);
      } else {
        System.setProperty(SpillableGroupBy.MEMORY_BUDGET_CFG, previous);
      }
    }
  }

  private static Map<String, long[]> group(long budget) throws QueryException {
    return group(budget, ROWS);
  }

  /**
   * Groups {@code rows} generated rows by their key column and returns count, sum and min of the
   * value column per key, as read back from the emitted tuples.
   */
  private static Map<String, long[]> group(long budget, int rows) throws QueryException {
    final QueryContext ctx = new BrackitQueryContext();
    final Cursor cursor = drivenCursor(budget, rows, ctx);
    final Map<String, long[]> groups = new HashMap<>();
    try {
      Tuple t;
      while ((t = cursor.next(ctx)) != null) {
        final String key = ((Str) t.get(KeyValueSource.KEY_POS)).stringValue();
        final long count = ((Numeric) t.get(KeyValueSource.WIDTH)).longValue();
        final long sum = ((Numeric) t.get(KeyValueSource.WIDTH + 1)).longValue();
        final long min = ((Numeric) t.get(KeyValueSource.WIDTH + 2)).longValue();
        assertNull(groups.put(key, new long[] { count, sum, min }), "group " + key + " was emitted twice");
      }
    } finally {
      cursor.close(ctx);
    }
    return groups;
  }

  /** Builds the operator over {@code rows} generated tuples and returns its opened cursor. */
  private static SpillableGroupBy.SpillableHashGroupByCursor drivenCursor(long budget, int rows, QueryContext ctx)
      throws QueryException {
    final KeyValueSource source = new KeyValueSource(rows);
    final SpillableGroupBy groupBy = new SpillableGroupBy(source,
                                                          Aggregate.SINGLE,
                                                          new Aggregate[] { Aggregate.COUNT, Aggregate.SUM,
                                                              Aggregate.MIN },
                                                          1,
                                                          false,
                                                          budget);
    groupBy.group(0).setPos(KeyValueSource.KEY_POS);
    for (int i = 0; i < 3; i++) {
      groupBy.aggregate(i).setPos(KeyValueSource.VALUE_POS);
    }
    final SpillableGroupBy.SpillableHashGroupByCursor cursor = (SpillableGroupBy.SpillableHashGroupByCursor) groupBy
                                                                                                                    .create(ctx,
                                                                                                                            new TupleImpl(new Sequence[KeyValueSource.WIDTH]));
    cursor.open(ctx);
    return cursor;
  }

  private static int countSpillFiles(File dir) {
    final String[] names = dir.list((d, name) -> name.startsWith("grp-part-") && name.endsWith(".spill"));
    return names == null ? 0 : names.length;
  }

  /** Emits {@code rows} two-column tuples whose key column cycles over {@link #DISTINCT_KEYS}. */
  private static final class KeyValueSource implements Operator {
    static final int KEY_POS = 0;
    static final int VALUE_POS = 1;
    static final int WIDTH = 2;

    private final int rows;

    KeyValueSource(int rows) {
      this.rows = rows;
    }

    @Override
    public Cursor create(QueryContext ctx, Tuple tuple) {
      return new KeyValueCursor(rows);
    }

    @Override
    public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
      return new KeyValueCursor(rows);
    }

    @Override
    public int tupleWidth(int initSize) {
      return WIDTH;
    }
  }

  private static final class KeyValueCursor implements Cursor {
    private final int rows;
    private int pos;

    KeyValueCursor(int rows) {
      this.rows = rows;
    }

    @Override
    public void open(QueryContext ctx) {
      pos = 0;
    }

    @Override
    public void close(QueryContext ctx) {
    }

    @Override
    public Tuple next(QueryContext ctx) {
      if (pos >= rows) {
        return null;
      }
      final int row = pos++;
      // Interleave the keys so that no group arrives contiguously: a group that survives this
      // has really been looked up in the table (or read back from its partition), not just
      // accumulated while it happened to be the resident one.
      final Sequence[] columns = new Sequence[KeyValueSource.WIDTH];
      columns[KeyValueSource.KEY_POS] = new Str("key-" + (row % DISTINCT_KEYS));
      columns[KeyValueSource.VALUE_POS] = new Int64(row);
      return new TupleImpl(columns);
    }
  }

  /** Expected aggregates, computed independently of the operator. */
  @Test
  void aggregatesMatchAHandComputedReference() throws QueryException {
    final Map<String, long[]> expected = new HashMap<>();
    for (int row = 0; row < ROWS; row++) {
      final long[] agg = expected.computeIfAbsent("key-" + (row % DISTINCT_KEYS),
                                                  k -> new long[] { 0, 0, Long.MAX_VALUE });
      agg[0]++;
      agg[1] += row;
      agg[2] = Math.min(agg[2], row);
    }

    final List<String> mismatches = new ArrayList<>();
    final Map<String, long[]> actual = group(BUDGET_ONE_GROUP);
    for (final Map.Entry<String, long[]> e : expected.entrySet()) {
      final long[] want = e.getValue();
      final long[] got = actual.get(e.getKey());
      if (got == null || got[0] != want[0] || got[1] != want[1] || got[2] != want[2]) {
        mismatches.add(e.getKey());
      }
    }
    assertTrue(mismatches.isEmpty(), "spilled aggregates differ for: " + mismatches);
  }
}
