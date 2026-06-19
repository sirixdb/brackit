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
package io.brackit.query.operator.vectorized;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Item;

/**
 * Regression tests for the vectorized parallel group-by-count on high-cardinality input.
 * <p>
 * The per-thread intern table is a fixed-size open-addressing table with a stride-31
 * probe; a chunk holding more distinct keys than the table can hold used to spin forever
 * (the probe is coprime to the capacity, so a full table never yields an empty slot). The
 * table now caps its load and spills the tail to an overflow map, which itself spills to
 * radix-partitioned files once it outgrows its budget. So a group-by with thousands of
 * distinct keys terminates with exact counts, memory-bounded, instead of hanging.
 */
public final class ParallelGroupByHighCardinalityTest {

  private static final String SPILL_THRESHOLD = "io.brackit.query.groupby.vectorized.spill_threshold";

  /** Build {@code [{"city":"cNNNN","n":1}, ...]} with {@code distinct} keys round-robined over rounds. */
  private static Path writeRoundRobin(int distinct, int rounds) throws Exception {
    final StringBuilder sb = new StringBuilder(distinct * rounds * 24);
    sb.append('[');
    boolean first = true;
    for (int r = 0; r < rounds; r++) {
      for (int k = 0; k < distinct; k++) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        sb.append("{\"city\":\"c").append(String.format("%04d", k)).append("\",\"n\":1}");
      }
    }
    sb.append(']');
    final Path file = Files.createTempFile("vec-highcard-", ".json");
    Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    return file;
  }

  private static void assertExactCounts(List<Item> result, int distinct, int rounds) {
    assertEquals(distinct, result.size(), "every distinct key must survive aggregation");
    final QNm countField = new QNm("count");
    long total = 0;
    for (Item item : result) {
      final long count = ((Int64) ((io.brackit.query.jdm.json.Object) item).get(countField)).longValue();
      assertEquals(rounds, count, "per-group count must be exact");
      total += count;
    }
    assertEquals((long) distinct * rounds, total, "no rows lost or double-counted");
  }

  @Test
  public void groupByCountWithMoreKeysThanInternTable() throws Exception {
    final int distinct = 1500; // > the 1024-slot intern table
    // Round-robin the keys so every contiguous chunk sees all of them (each chunk then
    // holds > 1024 distinct keys, which is the case that used to wedge the probe loop).
    final int rounds = Runtime.getRuntime().availableProcessors() + 3;
    final Path file = writeRoundRobin(distinct, rounds);
    try {
      // Without the fix this never returns; bound it so a regression fails fast.
      final List<Item> result = assertTimeoutPreemptively(Duration.ofSeconds(30),
                                                          () -> ParallelGroupByExec.executeGroupByCount(file, "city"));
      assertExactCounts(result, distinct, rounds);
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void groupByCountSpillsToDiskUnderTinyBudget() throws Exception {
    // A 50-key budget forces the overflow map to spill to partition files repeatedly,
    // exercising the radix spill + per-partition merge end to end. The result must still
    // be exact (no rows lost, double-counted, or mis-merged across the spill boundary).
    final int distinct = 1500;
    final int rounds = Runtime.getRuntime().availableProcessors() + 3;
    final Path file = writeRoundRobin(distinct, rounds);
    final String prev = System.getProperty(SPILL_THRESHOLD);
    System.setProperty(SPILL_THRESHOLD, "50");
    try {
      final List<Item> result = assertTimeoutPreemptively(Duration.ofSeconds(30),
                                                          () -> ParallelGroupByExec.executeGroupByCount(file, "city"));
      assertExactCounts(result, distinct, rounds);
    } finally {
      if (prev == null) {
        System.clearProperty(SPILL_THRESHOLD);
      } else {
        System.setProperty(SPILL_THRESHOLD, prev);
      }
      Files.deleteIfExists(file);
    }
  }
}
