package io.brackit.query;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for the JSONiq {@code [? ... ]} filter over literal (non-store) items.
 *
 * <p>A bare member lookup such as {@code $a[][?$$.price]} used to return an empty sequence
 * whenever the member values were numeric: the predicate machinery applied XQuery's
 * numeric-equals-position rule to the looked-up value, so {@code price} was compared against the
 * context position instead of being reduced to its effective boolean value. String and boolean
 * members were unaffected (they always took the EBV path), which made the failure data-dependent.
 *
 * <p>Semantics under test: a JSONiq {@code [? ... ]} predicate that references the context item
 * ({@code $$}) is a pure truthiness filter — the predicate value's effective boolean value decides,
 * with brackit's usual EBV rules (missing member/empty sequence, {@code null}, {@code false},
 * {@code 0}, {@code ""} are all falsy). Context-item independent predicates such as {@code [?1]},
 * {@code [?last()]}, or {@code [?position() gt 1]} keep XQuery's positional semantics, as do plain
 * {@code [...]} predicates in XQuery syntax mode.
 */
public final class JsoniqFilterPredicateTest extends XQueryBaseTest {

  private static final String BOOKS =
      "let $a := [{\"title\":\"A\",\"price\":12.5},{\"title\":\"B\"},{\"title\":\"C\",\"price\":42}] return ";

  private static final String A_AND_C = "{\"title\":\"A\",\"price\":12.5} {\"title\":\"C\",\"price\":42}";

  @Test
  public void bareNumericMemberFiltersByTruthiness() {
    // The original wrong-result repro: objects with a (numeric) price member are kept.
    assertEquals(A_AND_C, query(BOOKS + "$a[][?$$.price]"));
  }

  @Test
  public void bareNumericMemberOnSingleObject() {
    // Single-item input goes through evaluateToItem: 3 is truthy, not "position 3".
    assertEquals("{\"key\":3,\"foo\":0}", query("{\"key\": 3, \"foo\": 0}[?$$.key]"));
    assertEquals("", query("{\"key\": 3, \"foo\": 0}[?$$.foo]"));
  }

  @Test
  public void bareStringMemberKeepsAllObjects() {
    assertEquals("{\"title\":\"A\",\"price\":12.5} {\"title\":\"B\"} {\"title\":\"C\",\"price\":42}",
                 query(BOOKS + "$a[][?$$.title]"));
  }

  @Test
  public void missingMemberIsExcluded() {
    assertEquals("", query("[{\"a\":1},{\"a\":2}][][?$$.nope]"));
  }

  @Test
  public void nullMemberIsExcluded() {
    // EBV of JSON null is false (matches brackit's Null.booleanValue() convention).
    assertEquals("{\"x\":1}", query("[{\"x\":null},{\"x\":1}][][?$$.x]"));
  }

  @Test
  public void falsyMemberValuesAreExcluded() {
    assertEquals("{\"x\":true}", query("[{\"x\":false},{\"x\":true}][][?$$.x]"));
    assertEquals("{\"x\":7}", query("[{\"x\":0},{\"x\":7}][][?$$.x]"));
    assertEquals("{\"x\":\"y\"}", query("[{\"x\":\"\"},{\"x\":\"y\"}][][?$$.x]"));
  }

  @Test
  public void numericValueEqualToPositionIsNotPositional() {
    // {"a":2} sits at position 2: under the old positional rule it was (accidentally) kept and
    // {"a":5} dropped; under filter semantics both are truthy.
    assertEquals("{\"a\":2} {\"a\":5}", query("[{\"a\":2},{\"a\":5}][][?$$.a]"));
  }

  @Test
  public void bareContextItemFiltersByTruthiness() {
    assertEquals("1 2", query("(0, 1, 2)[?$$]"));
  }

  @Test
  public void predicateAgreesWithFlworWhere() {
    // Differential sanity: [?pred($$)] must agree with "for ... where pred($b)".
    final String[][] cases = { { "$a[][?$$.price]", "for $b in $a[] where $b.price return $b" }, { "$a[][?$$.title]",
        "for $b in $a[] where $b.title return $b" }, { "$a[][?$$.nope]", "for $b in $a[] where $b.nope return $b" }, {
            "$a[][?$$.price gt 10]", "for $b in $a[] where $b.price gt 10 return $b" } };
    for (String[] c : cases) {
      assertEquals(query(BOOKS + c[1]), query(BOOKS + c[0]), "predicate vs FLWOR for " + c[0]);
    }
    assertEquals(query("for $b in [{\"x\":null},{\"x\":1}][] where $b.x return $b"),
                 query("[{\"x\":null},{\"x\":1}][][?$$.x]"));
    assertEquals(query("for $b in [{\"x\":false},{\"x\":0},{\"x\":\"\"},{\"x\":\"y\"}][] where $b.x return $b"),
                 query("[{\"x\":false},{\"x\":0},{\"x\":\"\"},{\"x\":\"y\"}][][?$$.x]"));
  }

  @Test
  public void comparisonPredicatesAreUnchanged() {
    assertEquals(A_AND_C, query(BOOKS + "$a[][?$$.price gt 10]"));
    assertEquals(A_AND_C, query(BOOKS + "$a[][?boolean($$.price)]"));
    assertEquals(A_AND_C, query(BOOKS + "$a[][?exists($$.price)]"));
  }

  @Test
  public void contextIndependentPredicatesStayPositional() {
    // No $$ reference: XQuery positional semantics must be preserved.
    assertEquals("b", query("(\"a\", \"b\", \"c\")[?2]"));
    assertEquals("a", query("(\"a\", \"b\", \"c\")[?1]"));
    assertEquals("c", query("(\"a\", \"b\", \"c\")[?last()]"));
    assertEquals("b c", query("(\"a\", \"b\", \"c\")[?position() gt 1]"));
    assertEquals("20", query("(10, 20, 30)[?2]"));
  }

  @Test
  public void chainedFiltersCompose() {
    assertEquals("{\"title\":\"A\",\"price\":12.5}", query(BOOKS + "$a[][?$$.price][?$$.title eq \"A\"]"));
  }

  @Test
  public void xquerySyntaxPredicatesKeepPositionalSemantics() {
    // In XQuery syntax mode the plain [...] predicate retains the spec rule: a numeric
    // (even a context-dependent one) is compared against the context position.
    assertEquals("20", query("xquery version \"3.0\";\n(10, 20, 30)[2]"));
    // Only 2 sits at its own position; positional-by-value must still apply in XQuery mode.
    assertEquals("2", query("xquery version \"3.0\";\n(3, 2, 1)[.]"));
  }

  private String query(final String query) {
    try (final var out = new ByteArrayOutputStream()) {
      new Query(query).serialize(ctx, new PrintStream(out));
      return out.toString(StandardCharsets.UTF_8);
    } catch (final java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}
