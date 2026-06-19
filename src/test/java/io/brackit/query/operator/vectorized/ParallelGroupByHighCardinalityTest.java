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

import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Item;
import io.brackit.query.jsonitem.object.CompactObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the fixed-size byte-key intern table in {@link ParallelGroupByExec}.
 *
 * <p>The per-thread open-addressing table ({@code INTERN_CAPACITY = 1024}, stride-31
 * probing) had no occupancy bound. A chunk holding more than 1024 distinct group keys
 * filled every slot, after which the {@code while (true)} probe for the next new key found
 * neither a free slot nor a match and spun forever — a CPU-bound hang on any
 * high-cardinality grouping. (The 1BRC source it was modelled on tops out at ~413 distinct
 * keys, so the ceiling was never reached there.)
 *
 * <p>The fix caps occupancy and counts the high-cardinality tail in an overflow map, which
 * keeps free slots in the table so the probe always terminates. This test drives far more
 * than 1024 distinct keys per chunk and asserts the path both TERMINATES (preemptive
 * timeout) and stays exact (one group per key, counts preserved).
 */
final class ParallelGroupByHighCardinalityTest {

  @Test
  void highCardinalityGroupByCountTerminatesAndStaysExact() throws Exception {
    // One chunk per core; size the data so every chunk sees > 1024 distinct keys (the
    // count that used to overflow the 1024-slot table into an infinite probe).
    final int cores = Runtime.getRuntime().availableProcessors();
    final int distinct = Math.max(4096, 1400 * cores);
    final int repeats = 2; // each key written twice → expected count per group = 2

    final Path file = Files.createTempFile("pgb-highcard-", ".json");
    try {
      writeDataset(file, distinct, repeats);

      final List<Item> groups = assertTimeoutPreemptively(Duration.ofSeconds(120),
                                                          () -> ParallelGroupByExec.executeGroupByCount(file, "city"),
                                                          "high-cardinality group-by-count must terminate (intern-table probe must stay bounded)");

      // A group map is keyed by the (unique) city string, so size == distinct already
      // proves every key produced exactly one group — none dropped, none duplicated.
      assertEquals(distinct, groups.size(), "one group per distinct key");

      final QNm countQnm = new QNm("count");
      long total = 0;
      for (final Item item : groups) {
        final long count = ((Int64) ((CompactObject) item).get(countQnm)).longValue();
        assertEquals(repeats, count, "each distinct key must be counted exactly " + repeats + " times");
        total += count;
      }
      assertEquals((long) distinct * repeats, total, "total record count must be preserved");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  /** Write a JSON array of {@code distinct} flat objects, each emitted {@code repeats} times. */
  private static void writeDataset(final Path file, final int distinct, final int repeats) throws IOException {
    try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      w.write("[\n");
      for (int i = 0; i < distinct; i++) {
        // Repeat each key consecutively so the surplus (overflowed) keys are also
        // incremented in the overflow map, exercising that path too.
        final String record = "{\"city\":\"c" + String.format("%07d", i) + "\"}\n";
        for (int r = 0; r < repeats; r++) {
          w.write(record);
        }
      }
      w.write("]\n");
    }
  }
}
