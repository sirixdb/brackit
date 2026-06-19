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
package io.brackit.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.brackit.query.compiler.translator.SequentialPipelineStrategy;

/**
 * Verifies that {@link io.brackit.query.operator.SpillableGroupBy} produces results
 * identical to the in-memory hash group-by once its disk-spill path is actually
 * engaged. Spilling is forced by shrinking the per-operator memory budget (read at
 * operator construction from {@code io.brackit.query.groupby.memory_budget}) to one
 * byte, so every group beyond the first overflows to a partition file and the
 * recursive repartitioning is exercised end to end.
 */
public final class SpillableGroupBySpillTest extends XQueryBaseTest {

  private static final String BUDGET = "io.brackit.query.groupby.memory_budget";

  /** A group-by over 2000 rows / 64 groups, ordered by key for a deterministic result. */
  private static final String GROUP_QUERY = """
      string-join(
        for $x in 1 to 2000
        let $g := $x mod 64
        group by $g
        order by $g
        return $g || ":" || count($x) || ":" || sum($x),
        ", ")
      """;

  @BeforeEach
  public void disableVectorizedExecutor() {
    // Force the sequential SpillableGroupBy path: with no executor registered the
    // compiler cannot rewrite the group-by to a vectorized operator, so the spill
    // logic under test is actually exercised. (The static is process-wide.)
    SequentialPipelineStrategy.clearThreadVectorizedExecutor();
    SequentialPipelineStrategy.setVectorizedExecutor(null);
  }

  private String run(String query) throws Exception {
    try (var out = new ByteArrayOutputStream()) {
      new Query(query).serialize(ctx, new PrintStream(out));
      return out.toString(StandardCharsets.UTF_8);
    }
  }

  private String withBudget(long bytes, String query) throws Exception {
    final String prev = System.getProperty(BUDGET);
    System.setProperty(BUDGET, Long.toString(bytes));
    try {
      return run(query);
    } finally {
      if (prev == null) {
        System.clearProperty(BUDGET);
      } else {
        System.setProperty(BUDGET, prev);
      }
    }
  }

  @Test
  public void spilledResultMatchesInMemory() throws Exception {
    final String inMemory = withBudget(Long.MAX_VALUE, GROUP_QUERY);
    final String spilled = withBudget(1L, GROUP_QUERY);
    // Byte-identical: every group lands in the right bucket with the right aggregate,
    // and nothing is lost, duplicated, or mis-merged across the spill/merge boundary.
    assertEquals(inMemory, spilled);
    // 64 distinct groups survive the spill.
    assertEquals(64, spilled.split(", ").length);
  }

  @Test
  public void spilledAggregatesPreserveTotals() throws Exception {
    // Total row count is conserved across the partitioned merge (sum of group sizes).
    final String rows = "sum(for $x in 1 to 2000 let $g := $x mod 64 group by $g return count($x))";
    assertEquals("2000", withBudget(1L, rows));
    // Global value sum is conserved (1 + 2 + ... + 2000 = 2001000).
    final String total = "sum(for $x in 1 to 2000 let $g := $x mod 64 group by $g return sum($x))";
    assertEquals("2001000", withBudget(1L, total));
  }

  @Test
  public void allTuplesShareOneGroup() throws Exception {
    // A single hot key must aggregate in place under a 1-byte budget without
    // spill-looping (an existing group always folds in, never overflows).
    // sum(1..5000) = 12502500.
    final String query = """
        for $x in 1 to 5000
        let $g := 7
        group by $g
        return $g || ":" || count($x) || ":" || sum($x)
        """;
    assertEquals("7:5000:12502500", withBudget(1L, query));
  }

  @Test
  public void emptyInputProducesNoGroups() throws Exception {
    final String query = "count(for $x in () let $g := $x mod 64 group by $g return $g)";
    assertEquals("0", withBudget(1L, query));
  }
}
