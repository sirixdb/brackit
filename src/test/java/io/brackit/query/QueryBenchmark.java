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
 * (no JVM startup overhead) at large dataset sizes.
 *
 * <p>Generates in-memory JSON datasets from 10k to 1M records and runs
 * representative queries that exercise filtering, grouping, aggregation,
 * hash-joins, and multi-stage pipelines. This demonstrates Brackit's
 * optimizer and SIMD-accelerated operations at scale.</p>
 *
 * <p>Run with:
 * <pre>
 * mvn test-compile exec:java \
 *   -Dexec.mainClass="io.brackit.query.QueryBenchmark" \
 *   -Dexec.classpathScope=test
 * </pre>
 *
 * <p>Override sizes with -Dexec.args="10000 50000 100000"</p>
 *
 * @author Brackit Project Team
 */
public class QueryBenchmark {

  private static final int WARMUP = 3;
  private static final int ITERATIONS = 10;
  private static final int[] DEFAULT_SIZES = {10_000, 100_000, 500_000, 1_000_000};

  private static final String[] DEPTS =
      {"Engineering", "Sales", "Marketing", "Operations", "HR", "Finance", "Legal", "Support"};
  private static final String[] CITIES =
      {"New York", "London", "Tokyo", "Berlin", "Sydney", "Toronto", "Mumbai", "Sao Paulo"};
  private static final String[] TIERS = {"platinum", "gold", "silver", "bronze"};
  private static final String[] CATEGORIES =
      {"Electronics", "Furniture", "Clothing", "Food", "Books", "Sports", "Tools", "Garden"};
  private static final String[] REGIONS =
      {"NA-East", "NA-West", "EU-West", "EU-East", "APAC", "LATAM"};

  public static void main(String[] args) throws Exception {
    int[] sizes = DEFAULT_SIZES;
    if (args.length > 0) {
      sizes = new int[args.length];
      for (int i = 0; i < args.length; i++) {
        sizes[i] = Integer.parseInt(args[i].replace("_", "").replace(",", ""));
      }
    }

    System.out.println("==================================================================");
    System.out.println("  Brackit Query Engine Benchmark — Large-Scale JSON Processing");
    System.out.println("==================================================================");
    System.out.println();
    System.out.printf("  Warmup: %d iterations, Measurement: %d iterations%n", WARMUP, ITERATIONS);
    System.out.printf("  JVM: %s %s%n", System.getProperty("java.vm.name"),
        System.getProperty("java.vm.version"));
    long maxMem = Runtime.getRuntime().maxMemory();
    System.out.printf("  Max heap: %,d MB%n", maxMem / (1024 * 1024));
    System.out.println();

    for (int size : sizes) {
      runBenchmarkSuite(size);
    }

    System.out.println("==================================================================");
    System.out.println("  Key takeaways");
    System.out.println("==================================================================");
    System.out.println();
    System.out.println("  - Hash-join times should scale linearly O(n+m), not O(n*m).");
    System.out.println("  - Group by uses hash aggregation, not sort-based grouping.");
    System.out.println("  - The 'report' query (join+group+agg+sort) is a realistic");
    System.out.println("    analytics workload that exercises the full optimizer pipeline.");
    System.out.println("  - Compare these times with jq using examples/benchmark.sh");
    System.out.println();
  }

  private static void runBenchmarkSuite(int size) throws Exception {
    System.out.println("==================================================================");
    System.out.printf("  Dataset: %,d records%n", size);
    System.out.println("==================================================================");

    int orderCount = size * 5;
    long beforeMem = usedMemoryMB();

    System.out.printf("  Generating flat array (%,d records)...%n", size);
    long genStart = System.nanoTime();
    String flatJson = generateFlatArray(size);
    Item flatItem = new JSONParser(flatJson).parse();
    // Allow GC of the raw string
    flatJson = null;
    long flatGenMs = (System.nanoTime() - genStart) / 1_000_000;

    System.out.printf("  Generating join data (%,d customers, %,d orders)...%n", size,
        orderCount);
    genStart = System.nanoTime();
    String joinJson = generateJoinData(size);
    Item joinItem = new JSONParser(joinJson).parse();
    joinJson = null;
    long joinGenMs = (System.nanoTime() - genStart) / 1_000_000;

    long afterMem = usedMemoryMB();
    System.out.printf("  Data generated in %,dms + %,dms (mem: ~%,d MB)%n%n", flatGenMs,
        joinGenMs, afterMem - beforeMem);

    System.out.printf("  %-30s  %10s  %10s  %10s%n", "Query", "Avg (ms)", "Min (ms)", "ops/s");
    System.out.println(
        "  ------------------------------------------------------------------------------------");

    // -- Flat array queries --

    benchQuery("filter (scan + predicate)", flatItem,
        "for $u in $$[] where $u.age > 40 and $u.active return $u.name");

    benchQuery("group by + 3 aggregates", flatItem,
        "for $u in $$[] " + "group by $d := $u.dept "
            + "return {\"dept\": $d, \"count\": count($u), "
            + "\"avg_salary\": avg($u.salary), \"avg_score\": avg($u.score)}");

    benchQuery("group by 2 keys + sort", flatItem,
        "for $u in $$[] where $u.active " + "group by $d := $u.dept, $c := $u.city "
            + "let $total := sum($u.salary) " + "order by $total descending "
            + "return {\"dept\": $d, \"city\": $c, \"headcount\": count($u), "
            + "\"total_salary\": $total}");

    benchQuery("5-way aggregation", flatItem,
        "let $data := $$[] " + "return {\"total_salary\": sum($data.salary), "
            + "\"avg_age\": avg($data.age), " + "\"min_score\": min($data.score), "
            + "\"max_score\": max($data.score), " + "\"count\": count($data)}");

    // -- Join queries --

    benchQuery("hash-join", joinItem,
        "for $o in $$.orders[], $c in $$.customers[] " + "where $o.customer_id eq $c.id "
            + "return {\"order\": $o.id, \"customer\": $c.name, \"amount\": $o.amount}");

    benchQuery("join + group + agg + sort", joinItem,
        "for $o in $$.orders[], $c in $$.customers[] " + "where $o.customer_id eq $c.id "
            + "group by $tier := $c.tier, $cat := $o.category "
            + "let $revenue := sum($o.amount) " + "let $qty := sum($o.quantity) "
            + "order by $revenue descending "
            + "return {\"tier\": $tier, \"category\": $cat, \"revenue\": $revenue, "
            + "\"units\": $qty, \"orders\": count($o)}");

    benchQuery("join + filter + top-N", joinItem,
        "(for $o in $$.orders[], $c in $$.customers[] " + "where $o.customer_id eq $c.id "
            + "and $c.tier eq \"platinum\" " + "and $o.amount > 500 "
            + "order by $o.amount descending " + "return {\"customer\": $c.name, "
            + "\"amount\": $o.amount, \"category\": $o.category})[0:20]");

    System.out.println();
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
    long minNanos = Long.MAX_VALUE;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.nanoTime();
      consumeResult(query, contextItem);
      long elapsed = System.nanoTime() - start;
      totalNanos += elapsed;
      minNanos = Math.min(minNanos, elapsed);
    }

    double avgMs = (totalNanos / (double) ITERATIONS) / 1_000_000.0;
    double minMs = minNanos / 1_000_000.0;
    double opsPerSec = ITERATIONS / (totalNanos / 1_000_000_000.0);

    System.out.printf("  %-30s  %10.1f  %10.1f  %10.1f%n", label, avgMs, minMs, opsPerSec);
  }

  private static long consumeResult(Query query, Item contextItem) throws QueryException {
    QueryContext ctx = new BrackitQueryContext();
    ctx.setContextItem(contextItem);
    Sequence result = query.execute(ctx);
    long count = 0;
    if (result != null) {
      try (Iter it = result.iterate()) {
        while (it.next() != null) {
          count++;
        }
      }
    }
    return count;
  }

  private static long usedMemoryMB() {
    Runtime rt = Runtime.getRuntime();
    rt.gc();
    return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
  }

  // ---------- data generators ----------

  private static String generateFlatArray(int n) {
    Random rng = new Random(42);
    StringBuilder sb = new StringBuilder(n * 160);
    sb.append('[');
    for (int i = 0; i < n; i++) {
      if (i > 0)
        sb.append(',');
      sb.append("{\"id\":").append(i);
      sb.append(",\"name\":\"user_").append(i).append('"');
      sb.append(",\"age\":").append(rng.nextInt(18, 66));
      sb.append(",\"score\":").append(String.format("%.2f", rng.nextDouble() * 100));
      sb.append(",\"salary\":").append(String.format("%.2f", rng.nextDouble() * 170000 + 30000));
      sb.append(",\"dept\":\"").append(DEPTS[rng.nextInt(DEPTS.length)]).append('"');
      sb.append(",\"city\":\"").append(CITIES[rng.nextInt(CITIES.length)]).append('"');
      sb.append(",\"active\":").append(rng.nextBoolean());
      sb.append(",\"level\":").append(rng.nextInt(1, 11));
      sb.append('}');
    }
    sb.append(']');
    return sb.toString();
  }

  private static String generateJoinData(int n) {
    Random rng = new Random(42);
    int orderCount = n * 5;
    StringBuilder sb = new StringBuilder(n * 80 + orderCount * 120);
    sb.append("{\"customers\":[");
    for (int i = 0; i < n; i++) {
      if (i > 0)
        sb.append(',');
      sb.append("{\"id\":").append(i);
      sb.append(",\"name\":\"customer_").append(i).append('"');
      sb.append(",\"tier\":\"").append(TIERS[rng.nextInt(TIERS.length)]).append('"');
      sb.append(",\"region\":\"").append(REGIONS[rng.nextInt(REGIONS.length)]).append('"');
      sb.append('}');
    }
    sb.append("],\"orders\":[");
    for (int i = 0; i < orderCount; i++) {
      if (i > 0)
        sb.append(',');
      sb.append("{\"id\":").append(i);
      sb.append(",\"customer_id\":").append(rng.nextInt(n));
      sb.append(",\"amount\":").append(String.format("%.2f", rng.nextDouble() * 1995 + 5));
      sb.append(",\"category\":\"").append(CATEGORIES[rng.nextInt(CATEGORIES.length)]).append('"');
      sb.append(",\"region\":\"").append(REGIONS[rng.nextInt(REGIONS.length)]).append('"');
      sb.append(",\"quantity\":").append(rng.nextInt(1, 51));
      sb.append('}');
    }
    sb.append("]}");
    return sb.toString();
  }
}
