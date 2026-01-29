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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the BrackitJq command-line interface.
 *
 * @author Brackit Project Team
 */
public class BrackitJqTest {

  private static final Path JSON_RESOURCES = Path.of("src", "test", "resources", "json");

  /**
   * Helper to run bjq with given args and stdin, capturing stdout.
   */
  private String runBjq(String[] args, String stdinContent) {
    InputStream in = stdinContent != null
        ? new ByteArrayInputStream(stdinContent.getBytes(StandardCharsets.UTF_8))
        : InputStream.nullInputStream();

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);

    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8);

    int exitCode = BrackitJq.run(args, in, out, err);

    return outContent.toString(StandardCharsets.UTF_8).trim();
  }

  /**
   * Helper that also returns exit code.
   */
  private record BjqResult(String output, String error, int exitCode) {
  }

  private BjqResult runBjqWithExitCode(String[] args, String stdinContent) {
    InputStream in = stdinContent != null
        ? new ByteArrayInputStream(stdinContent.getBytes(StandardCharsets.UTF_8))
        : InputStream.nullInputStream();

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outContent, true, StandardCharsets.UTF_8);

    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errContent, true, StandardCharsets.UTF_8);

    int exitCode = BrackitJq.run(args, in, out, err);

    return new BjqResult(outContent.toString(StandardCharsets.UTF_8).trim(),
                         errContent.toString(StandardCharsets.UTF_8).trim(),
                         exitCode);
  }

  // ==================== Basic Query Tests ====================

  @Test
  public void testIdentityQuery() {
    // Use compact mode for exact comparison
    String result = runBjq(new String[] { "-c", "$$" }, "{\"foo\": \"bar\"}");
    assertEquals("{\"foo\":\"bar\"}", result);
  }

  @Test
  public void testFieldAccess() {
    String result = runBjq(new String[] { "$$.foo" }, "{\"foo\": \"bar\"}");
    assertEquals("bar", result);
  }

  @Test
  public void testNestedFieldAccess() {
    String result = runBjq(new String[] { "$$.a.b.c" }, "{\"a\": {\"b\": {\"c\": 42}}}");
    assertEquals("42", result);
  }

  @Test
  public void testArrayAccess() {
    String result = runBjq(new String[] { "$$[1]" }, "[\"a\", \"b\", \"c\"]");
    assertEquals("b", result);
  }

  @Test
  public void testArrayUnboxing() {
    String result = runBjq(new String[] { "$$[]" }, "[1, 2, 3]");
    // Each item should be on its own line
    assertTrue(result.contains("1"));
    assertTrue(result.contains("2"));
    assertTrue(result.contains("3"));
  }

  @Test
  public void testArraySlice() {
    String result = runBjq(new String[] { "-c", "$$[0:2]" }, "[1, 2, 3, 4, 5]");
    assertEquals("[1,2]", result);
  }

  @Test
  public void testObjectProjection() {
    String result = runBjq(new String[] { "$${name,age}" }, "{\"name\": \"Alice\", \"age\": 30, \"city\": \"NYC\"}");
    assertTrue(result.contains("\"name\""));
    assertTrue(result.contains("\"age\""));
  }

  // ==================== Null Input Mode Tests ====================

  @Test
  public void testNullInputGenerateObject() {
    String result = runBjq(new String[] { "-n", "-c", "{\"hello\": \"world\"}" }, null);
    assertEquals("{\"hello\":\"world\"}", result);
  }

  @Test
  public void testNullInputGenerateArray() {
    String result = runBjq(new String[] { "-n", "-c", "[1, 2, 3]" }, null);
    assertEquals("[1,2,3]", result);
  }

  @Test
  public void testNullInputWithExpression() {
    String result = runBjq(new String[] { "-n", "1 + 2 + 3" }, null);
    assertEquals("6", result);
  }

  // ==================== Compact Output Tests ====================

  @Test
  public void testCompactOutput() {
    String result = runBjq(new String[] { "-c", "$$" }, "{\"a\": 1, \"b\": 2}");
    // Compact output should not have extra whitespace
    assertEquals("{\"a\":1,\"b\":2}", result);
  }

  // ==================== Raw Output Tests ====================

  @Test
  public void testRawStringOutput() {
    String result = runBjq(new String[] { "-r", "$$.name" }, "{\"name\": \"Alice\"}");
    // Raw output: no quotes around string
    assertEquals("Alice", result);
  }

  @Test
  public void testRawOutputNonString() {
    String result = runBjq(new String[] { "-r", "$$.count" }, "{\"count\": 42}");
    // Non-strings should be output normally
    assertEquals("42", result);
  }

  // ==================== File Input Tests ====================

  @Test
  public void testFileInput() throws Exception {
    Path testFile = JSON_RESOURCES.resolve("user_profiles.json");
    if (Files.exists(testFile)) {
      String result = runBjq(new String[] { "$$.first_name", testFile.toString() }, null);
      assertEquals("Sammy", result);
    }
  }

  // ==================== FLWOR Expression Tests ====================

  @Test
  public void testFlworExpression() {
    String query = "for $item in $$[] return $item * 2";
    String result = runBjq(new String[] { query }, "[1, 2, 3]");
    assertTrue(result.contains("2"));
    assertTrue(result.contains("4"));
    assertTrue(result.contains("6"));
  }

  @Test
  public void testFlworWithFilter() {
    String query = "for $item in $$[] where $item > 2 return $item";
    String result = runBjq(new String[] { query }, "[1, 2, 3, 4, 5]");
    assertTrue(result.contains("3"));
    assertTrue(result.contains("4"));
    assertTrue(result.contains("5"));
  }

  // ==================== Complex Query Tests ====================

  @Test
  public void testPredicateFilter() {
    String result = runBjq(new String[] { "$$[][?$$.active eq true()]" },
                           "[{\"name\": \"a\", \"active\": true}, {\"name\": \"b\", \"active\": false}]");
    assertTrue(result.contains("\"name\":\"a\"") || result.contains("\"name\": \"a\""));
  }

  @Test
  public void testGroupBy() {
    String query = """
        let $items := $$[]
        for $item in $items
        let $type := $item.type
        group by $type
        return { $type: count($item) }
        """;
    String input = "[{\"type\": \"a\"}, {\"type\": \"b\"}, {\"type\": \"a\"}]";
    String result = runBjq(new String[] { query }, input);
    assertTrue(result.contains("\"a\"") && result.contains("\"b\""));
  }

  // ==================== Help and Version Tests ====================

  @Test
  public void testHelpOption() {
    String result = runBjq(new String[] { "--help" }, null);
    assertTrue(result.contains("Usage:"));
    assertTrue(result.contains("bjq"));
  }

  @Test
  public void testVersionOption() {
    String result = runBjq(new String[] { "--version" }, null);
    assertTrue(result.contains("bjq") || result.contains("Brackit"));
  }

  // ==================== Edge Cases ====================

  @Test
  public void testEmptyObject() {
    String result = runBjq(new String[] { "$$" }, "{}");
    assertEquals("{}", result);
  }

  @Test
  public void testEmptyArray() {
    String result = runBjq(new String[] { "-c", "$$" }, "[]");
    assertEquals("[]", result);
  }

  @Test
  public void testNullValue() {
    String result = runBjq(new String[] { "$$.value" }, "{\"value\": null}");
    assertEquals("null", result);
  }

  @Test
  public void testBooleanValues() {
    String result = runBjq(new String[] { "$$.flag" }, "{\"flag\": true}");
    assertEquals("true", result);
  }

  @Test
  public void testNumericValues() {
    String result = runBjq(new String[] { "$$.pi" }, "{\"pi\": 3.14159}");
    assertTrue(result.startsWith("3.14"));
  }

  @Test
  public void testNegativeArrayIndex() {
    String result = runBjq(new String[] { "$$[-1]" }, "[\"a\", \"b\", \"c\"]");
    assertEquals("c", result);
  }

  // ==================== Built-in Function Tests ====================

  @Test
  public void testCountFunction() {
    String result = runBjq(new String[] { "count($$[])" }, "[1, 2, 3, 4, 5]");
    assertEquals("5", result);
  }

  @Test
  public void testSumFunction() {
    String result = runBjq(new String[] { "sum($$[])" }, "[1, 2, 3, 4, 5]");
    assertEquals("15", result);
  }

  @Test
  public void testKeysFunction() {
    String result = runBjq(new String[] { "keys($$)" }, "{\"a\": 1, \"b\": 2}");
    assertTrue(result.contains("a"));
    assertTrue(result.contains("b"));
  }

  @Test
  public void testConcatFunction() {
    String result = runBjq(new String[] { "concat($$.first, ' ', $$.last)" },
                           "{\"first\": \"John\", \"last\": \"Doe\"}");
    assertEquals("John Doe", result);
  }

  // ==================== Exit Code Tests ====================

  @Test
  public void testSuccessExitCode() {
    BjqResult result = runBjqWithExitCode(new String[] { "$$" }, "{}");
    assertEquals(0, result.exitCode());
  }

  @Test
  public void testUsageErrorExitCode() {
    BjqResult result = runBjqWithExitCode(new String[] {}, "{}");
    assertEquals(2, result.exitCode());
  }

  @Test
  public void testInvalidOptionExitCode() {
    BjqResult result = runBjqWithExitCode(new String[] { "--invalid-option" }, "{}");
    assertEquals(2, result.exitCode());
  }

  // ==================== FLWOR Expression Tests ====================

  @Test
  public void testFlworIterateFilterTransform() {
    String query = "for $u in $$.users[] where $u.age > 21 return $u.name";
    String input =
        "{\"users\": [{\"name\": \"Alice\", \"age\": 25}, {\"name\": \"Bob\", \"age\": 17}, {\"name\": \"Carol\", \"age\": 30}]}";
    String result = runBjq(new String[] { "-r", query }, input);
    assertTrue(result.contains("Alice"));
    assertTrue(result.contains("Carol"));
    assertTrue(!result.contains("Bob"));
  }

  @Test
  public void testFlworGroupByWithAggregation() {
    String query = "for $item in $$[] let $cat := $item.category group by $cat return {$cat: count($item)}";
    String input =
        "[{\"category\": \"A\", \"val\": 1}, {\"category\": \"B\", \"val\": 2}, {\"category\": \"A\", \"val\": 3}]";
    String result = runBjq(new String[] { "-c", query }, input);
    assertTrue(result.contains("\"A\":2") || result.contains("\"A\": 2"));
    assertTrue(result.contains("\"B\":1") || result.contains("\"B\": 1"));
  }

  @Test
  public void testFlworOrderBy() {
    String query = "for $p in $$.products[] order by $p.price return $p.name";
    String input =
        "{\"products\": [{\"name\": \"C\", \"price\": 30}, {\"name\": \"A\", \"price\": 10}, {\"name\": \"B\", \"price\": 20}]}";
    String result = runBjq(new String[] { "-r", query }, input);
    // Should be ordered: A, B, C
    int posA = result.indexOf("A");
    int posB = result.indexOf("B");
    int posC = result.indexOf("C");
    assertTrue(posA < posB && posB < posC);
  }

  @Test
  public void testFlworOrderByDescending() {
    String query = "for $p in $$.products[] order by $p.price descending return $p.name";
    String input =
        "{\"products\": [{\"name\": \"C\", \"price\": 30}, {\"name\": \"A\", \"price\": 10}, {\"name\": \"B\", \"price\": 20}]}";
    String result = runBjq(new String[] { "-r", query }, input);
    // Should be ordered: C, B, A (descending by price)
    int posA = result.indexOf("A");
    int posB = result.indexOf("B");
    int posC = result.indexOf("C");
    assertTrue(posC < posB && posB < posA);
  }

  @Test
  public void testFlworJoin() {
    String query = "let $orders := $$.orders[] let $customers := $$.customers[] "
        + "for $o in $orders, $c in $customers where $o.cid eq $c.id "
        + "return {\"order\": $o.id, \"customer\": $c.name}";
    String input = "{\"orders\": [{\"id\": 1, \"cid\": 100}, {\"id\": 2, \"cid\": 101}], "
        + "\"customers\": [{\"id\": 100, \"name\": \"Alice\"}, {\"id\": 101, \"name\": \"Bob\"}]}";
    String result = runBjq(new String[] { "-c", query }, input);
    assertTrue(result.contains("\"order\":1") || result.contains("\"order\": 1"));
    assertTrue(result.contains("\"customer\":\"Alice\"") || result.contains("\"customer\": \"Alice\""));
  }

  // ==================== User-Defined Functions Tests ====================

  @Test
  public void testUserDefinedFunction() {
    String query = "declare function local:double($x) { $x * 2 }; for $n in $$[] return local:double($n)";
    String input = "[1, 2, 3]";
    String result = runBjq(new String[] { query }, input);
    assertTrue(result.contains("2"));
    assertTrue(result.contains("4"));
    assertTrue(result.contains("6"));
  }

  @Test
  public void testRecursiveFunction() {
    String query = "declare function local:factorial($n) { " + "if ($n le 1) then 1 else $n * local:factorial($n - 1) "
        + "}; local:factorial(10)";
    String result = runBjq(new String[] { "-n", query }, null);
    assertEquals("3628800", result);
  }

  // ==================== Anonymous Functions & Closures Tests ====================

  @Test
  public void testAnonymousFunction() {
    String query = "let $mult := function($x, $y) { $x * $y } return $mult(6, 7)";
    String result = runBjq(new String[] { "-n", query }, null);
    assertEquals("42", result);
  }

  @Test
  public void testClosure() {
    String query = "let $factor := 10 let $scale := function($x) { $x * $factor } return $scale(5)";
    String result = runBjq(new String[] { "-n", query }, null);
    assertEquals("50", result);
  }

  // ==================== Built-in Functions Tests ====================

  @Test
  public void testAvgFunction() {
    String result = runBjq(new String[] { "avg($$[])" }, "[10, 20, 30]");
    assertEquals("20", result);
  }

  @Test
  public void testStringJoinFunction() {
    String result = runBjq(new String[] { "string-join($$.tags[], \", \")" }, "{\"tags\": [\"a\", \"b\", \"c\"]}");
    assertEquals("a, b, c", result);
  }

  @Test
  public void testDistinctValuesFunction() {
    String result = runBjq(new String[] { "distinct-values($$[])" }, "[1, 2, 2, 3, 3, 3]");
    assertTrue(result.contains("1"));
    assertTrue(result.contains("2"));
    assertTrue(result.contains("3"));
  }

  @Test
  public void testContainsFunction() {
    String result = runBjq(new String[] { "contains($$.text, \"error\")" }, "{\"text\": \"An error occurred\"}");
    assertEquals("true", result);
  }

  @Test
  public void testCurrentDateTimeFunction() {
    String result = runBjq(new String[] { "-n", "exists(fn:current-dateTime())" }, null);
    assertEquals("true", result);
  }

  // ==================== Object Construction Tests ====================

  @Test
  public void testDynamicFieldNames() {
    // keys() returns strings for JSON objects
    String query = "{| for $k in keys($$) return {upper-case($k): $$.$k} |}";
    String input = "{\"name\": \"test\"}";
    String result = runBjq(new String[] { "-c", query }, input);
    assertTrue(result.contains("\"NAME\":\"test\"") || result.contains("\"NAME\": \"test\""));
  }

  @Test
  public void testCombineObjects() {
    // {| |} combines object fields but errors on duplicate keys
    // Use non-overlapping keys to demonstrate the spread syntax
    String query = "{| $$.base, $$.extra |}";
    String input = "{\"base\": {\"a\": 1, \"b\": 2}, \"extra\": {\"c\": 3, \"d\": 4}}";
    String result = runBjq(new String[] { "-c", query }, input);
    assertTrue(result.contains("\"a\":1") || result.contains("\"a\": 1"));
    assertTrue(result.contains("\"c\":3") || result.contains("\"c\": 3"));
  }

  // ==================== Conditional Logic Tests ====================

  @Test
  public void testConditionalExpression() {
    String query = "for $p in $$[] return if ($p.stock > 0) then {\"available\": $p.name} else {\"out\": $p.name}";
    String input = "[{\"name\": \"A\", \"stock\": 5}, {\"name\": \"B\", \"stock\": 0}]";
    String result = runBjq(new String[] { "-c", query }, input);
    assertTrue(result.contains("\"available\":\"A\"") || result.contains("\"available\": \"A\""));
    assertTrue(result.contains("\"out\":\"B\"") || result.contains("\"out\": \"B\""));
  }

  // ==================== Quantified Expressions Tests ====================

  @Test
  public void testSomeExpression() {
    String result = runBjq(new String[] { "some $x in $$[] satisfies $x > 100" }, "[1, 50, 150]");
    assertEquals("true", result);
  }

  @Test
  public void testSomeExpressionFalse() {
    String result = runBjq(new String[] { "some $x in $$[] satisfies $x > 100" }, "[1, 50, 75]");
    assertEquals("false", result);
  }

  @Test
  public void testEveryExpression() {
    String result = runBjq(new String[] { "every $x in $$[] satisfies $x > 0" }, "[1, 5, 10]");
    assertEquals("true", result);
  }

  @Test
  public void testEveryExpressionFalse() {
    String result = runBjq(new String[] { "every $x in $$[] satisfies $x > 0" }, "[1, -5, 10]");
    assertEquals("false", result);
  }

  // ==================== Type Checking Tests ====================

  @Test
  public void testInstanceOfInteger() {
    String result = runBjq(new String[] { "$$[][?$$ instance of xs:integer]" }, "[1, \"two\", 3, true]");
    assertTrue(result.contains("1"));
    assertTrue(result.contains("3"));
    assertTrue(!result.contains("two"));
  }

  // ==================== Array Spread Operator Tests ====================

  @Test
  public void testArraySpreadOperator() {
    String result = runBjq(new String[] { "-c", "[=(1 to 5)]" }, "{}");
    assertEquals("[1,2,3,4,5]", result);
  }

  // ==================== Python-style Slice Tests ====================

  @Test
  public void testSliceWithStep() {
    String result = runBjq(new String[] { "-c", "$$[::2]" }, "[0, 1, 2, 3, 4, 5]");
    assertEquals("[0,2,4]", result);
  }

  @Test
  public void testSliceReverse() {
    String result = runBjq(new String[] { "-c", "$$[::-1]" }, "[1, 2, 3]");
    assertEquals("[3,2,1]", result);
  }
}
