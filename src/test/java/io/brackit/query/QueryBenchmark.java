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

import java.util.Random;

import io.brackit.query.compiler.CompileChain;
import io.brackit.query.function.json.JSONParser;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;

/**
 * End-to-end query benchmark measuring pure query execution time
 * (no JVM startup overhead).
 *
 * <p>Generates in-memory JSON datasets and runs representative queries
 * at various sizes, reporting throughput in operations/second.</p>
 *
 * <p>Run with:
 * <pre>
 * mvn test-compile exec:java \
 *   -Dexec.mainClass="io.brackit.query.QueryBenchmark" \
 *   -Dexec.classpathScope=test
 * </pre>
 *
 * @author Brackit Project Team
 */
public class QueryBenchmark {

  private static final int WARMUP = 5;
  private static final int ITERATIONS = 20;
  private static final int[] SIZES = {100, 1_000, 10_000, 50_000};

  public static void main(String[] args) throws Exception {
    System.out.println("==============================================");
    System.out.println("  Brackit Query Engine Benchmark");
    System.out.println("==============================================");
    System.out.println();

    for (int size : SIZES) {
      System.out.printf("--- Dataset size: %,d records ---%n", size);
      String flatJson = generateFlatArray(size);
      String joinJson = generateJoinData(size);

      Item flatItem = new JSONParser(flatJson).parse();
      Item joinItem = new JSONParser(joinJson).parse();

      benchQuery("field access", flatItem, "$$[0].name");

      benchQuery("filter", flatItem, "for $u in $$[] where $u.age > 30 return $u.name");

      benchQuery("group by + count", flatItem,
          "for $u in $$[] group by $d := $u.dept return {$d: count($u)}");

      benchQuery("sum aggregation", flatItem, "sum(for $u in $$[] return $u.score)");

      benchQuery("hash-join", joinItem, "for $o in $$.orders[], $c in $$.customers[] "
          + "where $o.customer_id eq $c.id return {$c.name: $o.amount}");

      benchQuery("join + group + sort", joinItem,
          "for $o in $$.orders[], $c in $$.customers[] " + "where $o.customer_id eq $c.id "
              + "group by $name := $c.name " + "let $total := sum($o.amount) "
              + "order by $total descending " + "return {\"customer\": $name, \"total\": $total}");

      System.out.println();
    }
  }

  private static void benchQuery(String label, Item contextItem, String queryString)
      throws Exception {
    CompileChain chain = new CompileChain();
    Query query = new Query(chain, queryString);

    // Warmup
    for (int i = 0; i < WARMUP; i++) {
      consumeResult(query, contextItem);
    }

    // Measure
    long totalNanos = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.nanoTime();
      consumeResult(query, contextItem);
      totalNanos += System.nanoTime() - start;
    }

    double avgMs = (totalNanos / (double) ITERATIONS) / 1_000_000.0;
    double opsPerSec = ITERATIONS / (totalNanos / 1_000_000_000.0);

    System.out.printf("  %-25s  %8.2f ms avg  %8.0f ops/s%n", label, avgMs, opsPerSec);
  }

  private static void consumeResult(Query query, Item contextItem) throws QueryException {
    QueryContext ctx = new BrackitQueryContext();
    ctx.setContextItem(contextItem);
    Sequence result = query.execute(ctx);
    if (result != null) {
      try (Iter it = result.iterate()) {
        while (it.next() != null) {
          // drain
        }
      }
    }
  }

  private static String generateFlatArray(int n) {
    Random rng = new Random(42);
    String[] depts = {"Eng", "Sales", "Mkt", "Ops", "HR"};
    StringBuilder sb = new StringBuilder(n * 80);
    sb.append('[');
    for (int i = 0; i < n; i++) {
      if (i > 0)
        sb.append(',');
      sb.append(String.format(
          "{\"id\":%d,\"name\":\"user_%d\",\"age\":%d,\"score\":%.2f,\"dept\":\"%s\",\"active\":%s}",
          i, i, rng.nextInt(18, 66), rng.nextDouble() * 100, depts[rng.nextInt(depts.length)],
          rng.nextBoolean()));
    }
    sb.append(']');
    return sb.toString();
  }

  private static String generateJoinData(int n) {
    Random rng = new Random(42);
    String[] tiers = {"gold", "silver", "bronze"};
    String[] cats = {"Electronics", "Furniture", "Clothing", "Food"};
    StringBuilder sb = new StringBuilder(n * 120);
    sb.append("{\"customers\":[");
    for (int i = 0; i < n; i++) {
      if (i > 0)
        sb.append(',');
      sb.append(String.format("{\"id\":%d,\"name\":\"customer_%d\",\"tier\":\"%s\"}", i, i,
          tiers[rng.nextInt(tiers.length)]));
    }
    sb.append("],\"orders\":[");
    int orderCount = n * 3;
    for (int i = 0; i < orderCount; i++) {
      if (i > 0)
        sb.append(',');
      sb.append(String.format("{\"id\":%d,\"customer_id\":%d,\"amount\":%.2f,\"category\":\"%s\"}",
          i, rng.nextInt(n), rng.nextDouble() * 490 + 10, cats[rng.nextInt(cats.length)]));
    }
    sb.append("]}");
    return sb.toString();
  }
}
