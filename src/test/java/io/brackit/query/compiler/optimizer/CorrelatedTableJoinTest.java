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
package io.brackit.query.compiler.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.brackit.query.Query;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.compiler.CompileChain;

/** Regression tests for compiled joins: correlated inner pipelines and mixed-type join keys. */
public class CorrelatedTableJoinTest extends XQueryBaseTest {
  private boolean joinDetection;

  private boolean unnest;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    joinDetection = DefaultOptimizer.JOIN_DETECTION;
    unnest = DefaultOptimizer.UNNEST;
    DefaultOptimizer.JOIN_DETECTION = true;
    DefaultOptimizer.UNNEST = true;
  }

  @AfterEach
  public void restoreOptimizerFlags() {
    DefaultOptimizer.JOIN_DETECTION = joinDetection;
    DefaultOptimizer.UNNEST = unnest;
  }

  private void assertQuery(String expected, String query) {
    var out = new ByteArrayOutputStream();
    new Query(new CompileChain(), query).serialize(ctx, new PrintStream(out));
    assertEquals(expected, out.toString(StandardCharsets.UTF_8), query);
  }

  @Test
  public void letBoundCorrelatedCount() {
    assertQuery("1 1 0", """
        for $c in [1,2,3][]
        let $m := (for $p in [1,2][] where $p eq $c and $p gt 0 return $p)
        return count($m)
        """);
  }

  @Test
  public void directlyNestedCorrelatedCount() {
    assertQuery("1 1 0", """
        for $c in [1,2,3][]
        return count(for $p in [1,2][] where $p eq $c and $p gt 0 return $p)
        """);
  }

  @Test
  public void correlatedAntiJoin() {
    assertQuery("3", """
        for $c in [1,2,3][]
        where empty(for $p in [1,2][] where $p eq $c and $p gt 0 return $p)
        return $c
        """);
  }

  @Test
  public void additionalLeftBindingMasksTheBug() {
    assertQuery("1 1 0", """
        for $c in [1,2,3][]
        let $m := (let $d := 0
                   for $p in [1,2][] where $p eq $c and $p gt 0 return $p + $d)
        return count($m)
        """);
  }

  // Zero extra bindings used to select the right-driven branch; one equalizes
  // widths, and two makes the left branch wider. All three have the same meaning.
  private String nested(int leftBindings) {
    String bindings = "";
    String result = "$p";
    for (int i = 0; i < leftBindings; i++) {
      bindings += "let $d" + i + " := 0 ";
      result += " + $d" + i;
    }
    return bindings + "for $p in [1,2,1][] where $p eq $c and $p gt 0 return " + result;
  }

  // An uncorrelated join builds its table once, so the probe sees the decimal
  // keys both before and after integer keys are promoted into them.
  @Test
  public void mixedNumericKeysInAscendingComparison() {
    assertQuery("1 5.5 2.5 5.5", """
        for $c in [1,2.5][]
        for $p in [1,5.5][]
        where $c lt $p
        return ($c, $p)
        """);
  }

  @Test
  public void mixedNumericKeysInDescendingComparison() {
    assertQuery("2.5 1", """
        for $c in [1,2.5][]
        for $p in [1,5.5][]
        where $c gt $p
        return ($c, $p)
        """);
  }

  // Every probe row of a narrower numeric type must reach the wider build
  // tables, not just the first one, in either join orientation.
  @ParameterizedTest
  @CsvSource(delimiter = '|', value = { "1.5 2.5 | for $c in (1.5, 2.5) for $p in (3.0e0) where $c lt $p return $c",
      "1.5 2.5 | for $c in (1.5e0, 2.5e0) for $p in (3.5) where $c lt $p return $c",
      "1.5 2.5 | for $c in (xs:float(1.5), xs:float(2.5)) for $p in (3.0e0) where $c lt $p return $c",
      "1.5 2.5 | for $c in (1.5e0, 2.5e0) for $p in (xs:float(3.5)) where $c lt $p return $c",
      "1.5 2.5 | for $c in (1.5, 2.5) for $p in (xs:float(3.5)) where $c lt $p return $c",
      "1.5 2.5 | for $c in (xs:float(1.5), xs:float(2.5)) for $p in (3.5) where $c lt $p return $c",
      "1.5 1.5 | for $c in (1.5, 1.5) for $p in (1.5e0) where $c eq $p return $c",
      "1.5 1.5 | for $c in (1.5e0, 1.5e0) for $p in (1.5) where $c eq $p return $c" })
  public void everyProbeReachesWiderNumericBuildTables(String expected, String query) {
    assertQuery(expected, query);
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2 })
  public void equalityPreservesOrderAndDuplicates(int leftBindings) {
    assertQuery("1 1 2 1 1", "for $c in [3,1,2,1][] return (" + nested(leftBindings) + ")");
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2 })
  public void correlatedCountAcrossWidths(int leftBindings) {
    assertQuery("0 2 1 2", "for $c in [3,1,2,1][] return count(" + nested(leftBindings) + ")");
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2 })
  public void antiJoinAcrossWidths(int leftBindings) {
    assertQuery("3", "for $c in [3,1,2,1][] where empty(" + nested(leftBindings) + ") return $c");
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2 })
  public void semiJoinAcrossWidths(int leftBindings) {
    assertQuery("1 2 1", "for $c in [3,1,2,1][] where exists(" + nested(leftBindings) + ") return $c");
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, 2 })
  public void letJoinKeepsUnmatchedOuterTuples(int leftBindings) {
    assertQuery("3 0 1 2 2 1 1 2",
                "for $c in [3,1,2,1][] let $m := " + nested(leftBindings) + " return ($c, count($m))");
  }

}
