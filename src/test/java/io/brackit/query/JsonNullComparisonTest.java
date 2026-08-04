/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JSONiq's comparison rules for {@code null}, taken verbatim from the specification's
 * "Basic Operations" section:
 *
 * <blockquote>
 * "null can be compared for equality or inequality to anything - it is only equal to itself so that
 * false is returned when comparing it for equality with any non-null atomic. True is returned when
 * comparing it with non-equality with any non-null atomic."
 * <br>
 * "For ordering operators (lt, le, gt, ge), null is considered the smallest possible value (like in
 * JavaScript)."
 * </blockquote>
 *
 * <p>The comparison is TOTAL: null against a non-null is never a type error. Every per-type
 * {@code cmp}/{@code eq} implementation raises {@code XPTY0004} on a type it does not recognise, so
 * before {@link io.brackit.query.util.Cmp#aCmp} handled null itself, a single null row turned an
 * ordinary filter over a nullable JSON field into a failed query.
 *
 * <p>The expected values below are the specification's own worked examples.
 */
public final class JsonNullComparisonTest extends XQueryBaseTest {

  /** The spec's example: {@code 1 eq null, "foo" ne null, null eq null} ⇒ {@code false true true}. */
  @Test
  public void specEqualityExamples() throws IOException {
    assertEquals("false true true", query("1 eq jn:null(), \"foo\" ne jn:null(), jn:null() eq jn:null()"));
  }

  /** The spec's example: {@code 1 lt null} ⇒ {@code false} — null is the smallest value. */
  @Test
  public void specOrderingExample() throws IOException {
    assertEquals("false", query("1 lt jn:null()"));
  }

  /** Equality: null is equal only to itself, whatever the other type is. */
  @Test
  public void nullIsEqualOnlyToItself() throws IOException {
    assertEquals("false", query("jn:null() eq \"foo\""));
    assertEquals("false", query("jn:null() eq 1"));
    assertEquals("false", query("jn:null() eq true()"));
    assertEquals("true", query("jn:null() ne \"foo\""));
    assertEquals("true", query("jn:null() ne 1"));
    assertEquals("false", query("jn:null() ne jn:null()"));
  }

  /** Ordering: null sits below every non-null value, and ties only with itself. */
  @Test
  public void nullOrdersBelowEverything() throws IOException {
    assertEquals("true", query("jn:null() lt 1"));
    assertEquals("true", query("jn:null() le 1"));
    assertEquals("false", query("jn:null() gt 1"));
    assertEquals("false", query("jn:null() ge 1"));
    assertEquals("true", query("jn:null() lt \"foo\""));
    assertEquals("false", query("\"foo\" lt jn:null()"));
    // Ties with itself: le and ge hold, lt and gt do not.
    assertEquals("true", query("jn:null() le jn:null()"));
    assertEquals("true", query("jn:null() ge jn:null()"));
    assertEquals("false", query("jn:null() lt jn:null()"));
    assertEquals("false", query("jn:null() gt jn:null()"));
  }

  /** The empty sequence keeps its own rule — it is not null, and comparing it yields nothing. */
  @Test
  public void theEmptySequenceIsNotNull() throws IOException {
    assertEquals("", query("() eq jn:null()"));
    assertEquals("", query("() lt 1"));
  }

  private String query(final String query) throws IOException {
    try (final var out = new ByteArrayOutputStream()) {
      new Query(query).serialize(ctx, new PrintStream(out));
      return out.toString(StandardCharsets.UTF_8);
    }
  }
}
