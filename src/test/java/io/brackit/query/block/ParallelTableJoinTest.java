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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.Query;
import io.brackit.query.QueryContext;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Dec;
import io.brackit.query.atomic.Flt;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Una;
import io.brackit.query.compiler.BlockCompileChain;
import io.brackit.query.compiler.CompileChain;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.ItemSequence;

/**
 * The block pipeline builds a join table once and probes it from several
 * fork-join threads. Every run must return exactly the rows of the sequential
 * pipeline, however the first probes interleave.
 */
public class ParallelTableJoinTest {

  private static final String PROLOG = "declare variable $probe external; declare variable $build external; ";

  private static final int BUILD_SIZE = 20000;

  private static final int RUNS = 8;

  // Distinct build keys 1..BUILD_SIZE in a scrambled order, so that sorting them is real work.
  private static int buildValue(int i) {
    return (int) ((i * 7919L) % 20011) + 1;
  }

  private static Item numeric(int i, int value) {
    return switch (i % 4) {
      case 0 -> new Int32(value);
      case 1 -> new Dbl(value);
      case 2 -> new Flt(value);
      default -> new Dec(new BigDecimal(value));
    };
  }

  private static Item mixed(int i, int value) {
    return i % 5 == 4 ? new Una(Integer.toString(value)) : numeric(i, value);
  }

  private static Item[] items(int size, IntFunction<Item> item) {
    Item[] items = new Item[size];
    for (int i = 0; i < size; i++) {
      items[i] = item.apply(i);
    }
    return items;
  }

  private static Stream<Arguments> joins() {
    var joins = new ArrayList<Arguments>();
    for (boolean ordered : new boolean[] { false, true }) {
      // One key type: the sorted table used to be sorted by the first lookups, all at once.
      Item[] intBuild = items(BUILD_SIZE, i -> new Int32(buildValue(i)));
      Item[] intProbe = items(300, i -> new Int32(i + 1));
      long lessThanProbe = 0;
      for (int i = 0; i < 300; i++) {
        int c = i + 1;
        lessThanProbe += countBuild(b -> buildValue(b) < c);
      }
      joins.add(Arguments.of("integer keys, ordered comparison",
                             ordered,
                             "$c gt $p",
                             intProbe,
                             intBuild,
                             lessThanProbe));

      // Double probes of integer build keys need the build keys as doubles.
      Item[] doubleProbe = items(2000, i -> new Dbl(i + 1));
      joins.add(Arguments.of("double probes, integer build keys",
                             ordered,
                             "$c eq $p",
                             doubleProbe,
                             intBuild,
                             countBuild(b -> buildValue(b) <= 2000)));

      // Numeric probes of every type compare untyped build keys as doubles.
      Item[] untypedBuild = items(BUILD_SIZE, i -> new Una(Integer.toString(buildValue(i))));
      Item[] numericProbe = items(2000, i -> numeric(i, i + 1));
      joins.add(Arguments.of("numeric probes, untyped build keys",
                             ordered,
                             "$c = $p",
                             numericProbe,
                             untypedBuild,
                             countBuild(b -> buildValue(b) <= 2000)));

      // Every kind of copy at once, for a sorted table. An untyped probe key
      // compares an untyped build key as a string and any other one as a double.
      Item[] mixedBuild = items(BUILD_SIZE, i -> mixed(i, buildValue(i)));
      Item[] mixedProbe = items(100, i -> mixed(i, i + 1));
      long mixedMatches = 0;
      for (int i = 0; i < 100; i++) {
        int c = i + 1;
        boolean untypedProbe = i % 5 == 4;
        mixedMatches += countBuild(b -> untypedProbe && b % 5 == 4
            ? Integer.toString(c).compareTo(Integer.toString(buildValue(b))) > 0
            : c > buildValue(b));
      }
      joins.add(Arguments.of("mixed numeric and untyped keys",
                             ordered,
                             "$c > $p",
                             mixedProbe,
                             mixedBuild,
                             mixedMatches));
    }
    return joins.stream();
  }

  private static long countBuild(java.util.function.IntPredicate matches) {
    long count = 0;
    for (int b = 0; b < BUILD_SIZE; b++) {
      if (matches.test(b)) {
        count++;
      }
    }
    return count;
  }

  @ParameterizedTest(name = "{0}, ordered={1}")
  @MethodSource("joins")
  public void everyRunReturnsTheRowsOfTheSequentialPipeline(String name, boolean ordered, String predicate,
      Item[] probe, Item[] build, long expectedRows) {
    String query = PROLOG + "for $c in $probe for $p in $build where " + predicate + " return $p";
    List<String> expected = rows(new Query(new CompileChain(), query), probe, build);
    assertEquals(expectedRows, expected.size());

    Query parallel = new Query(new BlockCompileChain(ordered), query);
    for (int run = 0; run < RUNS; run++) {
      List<String> actual = assertTimeoutPreemptively(Duration.ofSeconds(60), () -> rows(parallel, probe, build));
      assertEquals(expected, actual, "run " + run);
    }
  }

  /** The rows, sorted: only which rows come back matters here, not their order. */
  private static List<String> rows(Query query, Item[] probe, Item[] build) {
    QueryContext ctx = new BrackitQueryContext();
    ctx.bind(new QNm("probe"), new ItemSequence(probe));
    ctx.bind(new QNm("build"), new ItemSequence(build));
    Sequence result = query.execute(ctx);
    var rows = new ArrayList<String>();
    if (result != null) {
      try (Iter it = result.iterate()) {
        Item item;
        while ((item = it.next()) != null) {
          rows.add(item.atomize().type() + " " + item.atomize().stringValue());
        }
      }
    }
    rows.sort(null);
    return rows;
  }
}
