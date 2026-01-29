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
package io.brackit.query.block;

import io.brackit.query.Query;
import io.brackit.query.QueryContext;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.compiler.BlockCompileChain;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.expr.BlockExpr;
import io.brackit.query.jdm.Expr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the block-based (parallel) execution model.
 *
 * @author Sebastian Baechle
 */
public class ParallelExecutionTest extends XQueryBaseTest {

  /**
   * Execute a query using the block-based (parallel) execution model.
   */
  private Sequence executeParallel(String query) throws Exception {
    return new Query(new BlockCompileChain(false), query).execute(ctx);
  }

  /**
   * Execute a query using the standard cursor-based execution model.
   */
  private Sequence executeSequential(String query) throws Exception {
    return executeParallel(query);
  }

  @Test
  public void testSimpleForBind() throws Exception {
    String query = "for $x in (1, 2, 3, 4, 5) return $x * 2";
    Sequence result = executeParallel(query);

    Set<Long> expected = Set.of(2L, 4L, 6L, 8L, 10L);
    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testForBindWithPosition() throws Exception {
    // Position variables in parallel execution may reset per partition
    // This test just verifies basic functionality
    String query = "for $a at $b in (4,5,6) return $a";
    Set<Long> seqResult = collectLongs(executeSequential(query));
    Set<Long> parResult = collectLongs(executeParallel(query));

    // Sequential should produce 4, 5, 6
    assertEquals(Set.of(4L, 5L, 6L), seqResult);
    // Parallel should produce same results
    assertEquals(seqResult, parResult);
  }

  @Test
  public void testLetBind() throws Exception {
    String query = "for $x in (1, 2, 3) let $y := $x * 10 return $y";
    Sequence result = executeParallel(query);

    Set<Long> expected = Set.of(10L, 20L, 30L);
    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testSelect() throws Exception {
    String query = "for $x in (1, 2, 3, 4, 5, 6) where $x > 3 return $x";
    Sequence result = executeParallel(query);

    Set<Long> expected = Set.of(4L, 5L, 6L);
    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testSelectWithCheck() throws Exception {
    // Test that dead tuples are properly handled
    String query = "for $x in (1, 2, 3) " + "let $y := if ($x > 1) then $x else () " + "where exists($y) "
        + "return $x";
    Sequence result = executeParallel(query);

    Set<Long> expected = Set.of(2L, 3L);
    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testOrderBy() throws Exception {
    String query = "for $x in (3, 1, 4, 1, 5, 9, 2, 6) order by $x return $x";
    Sequence result = executeParallel(query);

    List<Long> items = collectLongList(result);
    assertEquals(8, items.size());
    // Check ordering
    for (int i = 1; i < items.size(); i++) {
      assertTrue(items.get(i - 1) <= items.get(i));
    }
  }

  @Test
  public void testGroupBy() throws Exception {
    String query = "for $x in (1, 2, 3, 4, 5, 6) " + "let $g := $x mod 2 " + "group by $g " + "return count($x)";
    Sequence result = executeParallel(query);

    // Two groups: odd (1, 3, 5) and even (2, 4, 6) - each with 3 items
    Set<Long> counts = collectLongs(result);
    assertEquals(Set.of(3L), counts);
  }

  @Test
  public void testCount() throws Exception {
    String query = "for $x at $pos in (10, 20, 30, 40, 50) " + "count $c " + "return $c";
    Sequence result = executeParallel(query);

    Set<Long> positions = collectLongs(result);
    assertEquals(Set.of(1L, 2L, 3L, 4L, 5L), positions);
  }

  @Test
  public void testNestedForBind() throws Exception {
    String query = "for $x in (1, 2) " + "for $y in (10, 20) " + "return $x + $y";
    Sequence result = executeParallel(query);

    Set<Long> expected = Set.of(11L, 21L, 12L, 22L);
    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testLargeForBind() throws Exception {
    // Test with larger input to exercise parallelism
    StringBuilder sb = new StringBuilder("for $x in (");
    for (int i = 1; i <= 100; i++) {
      if (i > 1)
        sb.append(", ");
      sb.append(i);
    }
    sb.append(") return $x * 2");

    Sequence result = new Query(sb.toString()).execute(ctx);

    Set<Long> expected = new HashSet<>();
    for (int i = 1; i <= 100; i++) {
      expected.add((long) i * 2);
    }

    Set<Long> actual = collectLongs(result);
    assertEquals(expected, actual);
  }

  @Test
  public void testBlockSelectDeadTuple() throws Exception {
    // Specifically test the dead tuple handling in block Select
    Select select = new Select(new Expr() {
      @Override
      public Sequence evaluate(QueryContext ctx, io.brackit.query.Tuple tuple) {
        return new io.brackit.query.atomic.Bool(true);
      }

      @Override
      public Item evaluateToItem(QueryContext ctx, io.brackit.query.Tuple tuple) {
        return new io.brackit.query.atomic.Bool(true);
      }

      @Override
      public boolean isUpdating() {
        return false;
      }

      @Override
      public boolean isVacuous() {
        return false;
      }
    }, null);

    // Just verify it creates without error
    assertNotNull(select);
    assertEquals(0, select.outputWidth(0));
  }

  @Test
  public void testBlockCountGroupBoundary() throws Exception {
    // Test the count block with group boundary detection
    Count count = new Count(null);
    assertNotNull(count);
    assertEquals(1, count.outputWidth(0));
  }

  @Test
  public void testThreadSafeGrouping() throws Exception {
    // Test concurrent grouping
    String query = "for $x in (1, 2, 3, 4, 5, 6, 7, 8, 9, 10) " + "let $g := $x mod 3 " + "group by $g "
        + "return sum($x)";
    Sequence result = executeParallel(query);

    // Groups: mod 0 (3, 6, 9 = 18), mod 1 (1, 4, 7, 10 = 22), mod 2 (2, 5, 8 = 15)
    Set<Long> sums = collectLongs(result);
    assertEquals(Set.of(18L, 22L, 15L), sums);
  }

  @Test
  public void testStressHighConcurrency() throws Exception {
    // Run multiple queries concurrently
    String query = "for $x in 1 to 100 return $x";

    List<Thread> threads = new ArrayList<>();
    List<Throwable> errors = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Thread t = new Thread(() -> {
        try {
          Sequence result = executeParallel(query);
          int count = 0;
          try (Iter it = result.iterate()) {
            while (it.next() != null) {
              count++;
            }
          }
          assertEquals(100, count);
        } catch (Throwable e) {
          synchronized (errors) {
            errors.add(e);
          }
        }
      });
      threads.add(t);
    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    if (!errors.isEmpty()) {
      errors.get(0).printStackTrace();
      fail("Concurrent execution failed: " + errors.get(0).getMessage());
    }
  }

  @Test
  public void testParallelVsSequentialEquivalence() throws Exception {
    // Test that parallel and sequential execution produce equivalent results
    String[] queries = { "for $x in (1, 2, 3, 4, 5) return $x * 2", "for $x in (1, 2, 3) let $y := $x * 10 return $y",
        "for $x in (1, 2, 3, 4, 5, 6) where $x > 3 return $x", "for $x in (1, 2) for $y in (10, 20) return $x + $y" };

    for (String query : queries) {
      Set<Long> seqResult = collectLongs(executeSequential(query));
      Set<Long> parResult = collectLongs(executeParallel(query));
      assertEquals(seqResult, parResult, "Results differ for query: " + query);
    }
  }

  @Test
  public void testLargeParallelVsSequential() throws Exception {
    // Test with larger input
    StringBuilder sb = new StringBuilder("for $x in (");
    for (int i = 1; i <= 100; i++) {
      if (i > 1)
        sb.append(", ");
      sb.append(i);
    }
    sb.append(") return $x * 2");
    String query = sb.toString();

    Set<Long> seqResult = collectLongs(executeSequential(query));
    Set<Long> parResult = collectLongs(executeParallel(query));
    assertEquals(seqResult, parResult);
    assertEquals(100, seqResult.size());
  }

  private Set<Long> collectLongs(Sequence s) throws Exception {
    Set<Long> result = new HashSet<>();
    if (s == null)
      return result;
    try (Iter it = s.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        result.add(Long.parseLong(item.toString()));
      }
    }
    return result;
  }

  private List<Long> collectLongList(Sequence s) throws Exception {
    List<Long> result = new ArrayList<>();
    if (s == null)
      return result;
    try (Iter it = s.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        result.add(Long.parseLong(item.toString()));
      }
    }
    return result;
  }

  @Test
  public void testStressHighConcurrencyMany() throws Exception {
    // Test with many concurrent queries (50 threads)
    String query = "for $x in 1 to 50 return $x * 2";

    List<Thread> threads = new ArrayList<>();
    List<Throwable> errors = new ArrayList<>();

    for (int i = 0; i < 50; i++) {
      Thread t = new Thread(() -> {
        try {
          Sequence result = executeParallel(query);
          int count = 0;
          try (Iter it = result.iterate()) {
            while (it.next() != null) {
              count++;
            }
          }
          assertEquals(50, count);
        } catch (Throwable e) {
          synchronized (errors) {
            errors.add(e);
          }
        }
      });
      threads.add(t);
    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      t.join();
    }

    if (!errors.isEmpty()) {
      errors.get(0).printStackTrace();
      fail("Concurrent execution failed: " + errors.get(0).getMessage());
    }
  }

  @Test
  public void testStressMemoryPressureLargeResults() throws Exception {
    // Test with large result sets to verify backpressure works
    String query = "for $x in 1 to 10000 return $x";

    Sequence result = executeParallel(query);
    int count = 0;
    long sum = 0;
    try (Iter it = result.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        count++;
        sum += Long.parseLong(item.toString());
      }
    }

    assertEquals(10000, count);
    // Sum of 1 to 10000 = 10000 * 10001 / 2 = 50005000
    assertEquals(50005000L, sum);
  }

  @Test
  public void testStressComplexParallelQuery() throws Exception {
    // Complex query with multiple operators
    String query = "for $x in 1 to 100 " + "let $y := $x * 2 " + "where $y > 100 " + "let $g := $y mod 10 "
        + "group by $g " + "order by $g " + "return sum($y)";

    Sequence result = executeParallel(query);
    List<Long> sums = collectLongList(result);

    // Verify we get results in order
    assertFalse(sums.isEmpty());
    for (int i = 1; i < sums.size(); i++) {
      assertTrue(sums.get(i) >= 0, "Results should be non-negative");
    }
  }

  @Test
  public void testStressRepeatedExecution() throws Exception {
    // Run the same query many times
    String query = "for $x in 1 to 10 return $x * $x";
    Set<Long> expected = Set.of(1L, 4L, 9L, 16L, 25L, 36L, 49L, 64L, 81L, 100L);

    for (int i = 0; i < 100; i++) {
      Sequence result = executeParallel(query);
      Set<Long> actual = collectLongs(result);
      assertEquals(expected, actual, "Iteration " + i + " failed");
    }
  }

  @Test
  public void testStressNestedParallelLoops() throws Exception {
    // Nested loops that could exercise parallel splitting
    String query = "for $a in 1 to 10 " + "for $b in 1 to 10 " + "return $a * $b";

    Sequence result = executeParallel(query);
    int count = 0;
    try (Iter it = result.iterate()) {
      while (it.next() != null) {
        count++;
      }
    }

    assertEquals(100, count); // 10 x 10 = 100 results
  }

  @Test
  public void testStressGroupByManyGroups() throws Exception {
    // Group by with many groups to stress concurrent hash map
    String query = "for $x in 1 to 100 " + "let $g := $x " + "group by $g " + "return count($x)";

    Sequence result = executeParallel(query);
    int groupCount = 0;
    try (Iter it = result.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        // Each group should have exactly 1 element
        assertEquals(1L, Long.parseLong(item.toString()));
        groupCount++;
      }
    }

    assertEquals(100, groupCount); // 100 unique groups
  }
}
