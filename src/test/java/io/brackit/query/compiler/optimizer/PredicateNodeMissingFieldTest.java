/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the missing-field (sparse-data) analysis on
 * {@link PredicateNode}: {@code excludesRecordsMissingField},
 * {@code satisfiedWhenFieldMissing} and {@code findSoundAnchorField}.
 *
 * <p>These drive the fail-closed sound-anchor guard in
 * {@code VectorizedGroupByDetection}: an anchor-based executor never visits a
 * record lacking the anchor field, which is only sound when such records are
 * provably excluded by the predicate.
 */
public class PredicateNodeMissingFieldTest {

  private static PredicateNode num(String f) {
    return new PredicateNode.NumCmp(f, "gt", 5L);
  }

  private static PredicateNode fp(String f) {
    return new PredicateNode.FpCmp(f, "gt", 5.5d);
  }

  private static PredicateNode dec(String f) {
    return new PredicateNode.DecCmp(f, "gt", new java.math.BigDecimal("5.5"));
  }

  private static PredicateNode str(String f) {
    return new PredicateNode.StrEq(f, "x");
  }

  private static PredicateNode bool(String f) {
    return new PredicateNode.BoolRef(f);
  }

  @Test
  void leavesExcludeTheirOwnMissingField() {
    for (PredicateNode leaf : List.of(num("a"), fp("a"), dec("a"), str("a"), bool("a"))) {
      assertTrue(leaf.excludesRecordsMissingField("a"), leaf + " must exclude records missing 'a'");
      assertFalse(leaf.satisfiedWhenFieldMissing("a"), leaf + " must not be satisfied when 'a' is missing");
      assertFalse(leaf.excludesRecordsMissingField("b"), leaf + " says nothing about records missing 'b'");
    }
  }

  @Test
  void andExcludesWhenAnyConjunctDoes() {
    PredicateNode and = new PredicateNode.And(List.of(num("a"), num("b")));
    assertTrue(and.excludesRecordsMissingField("a"));
    assertTrue(and.excludesRecordsMissingField("b"));
    assertEquals("a", and.findSoundAnchorField());
  }

  @Test
  void orExcludesOnlyWhenAllDisjunctsDo() {
    PredicateNode orDifferent = new PredicateNode.Or(List.of(num("a"), num("b")));
    assertFalse(orDifferent.excludesRecordsMissingField("a"));
    assertFalse(orDifferent.excludesRecordsMissingField("b"));
    assertNull(orDifferent.findSoundAnchorField(), "or over different fields has NO sound anchor");

    PredicateNode orSame = new PredicateNode.Or(List.of(num("a"), new PredicateNode.NumCmp("a", "lt", 0L)));
    assertTrue(orSame.excludesRecordsMissingField("a"));
    assertEquals("a", orSame.findSoundAnchorField());
  }

  @Test
  void notOverMissingFieldIsSatisfied_theDeMorganLandmine() {
    // not($u.a > 5) over a record missing `a`: the inner comparison evaluates
    // over the empty sequence → false → not(false) = TRUE. An anchor scan on
    // `a` would silently drop those matches.
    PredicateNode not = new PredicateNode.Not(num("a"));
    assertTrue(not.satisfiedWhenFieldMissing("a"));
    assertFalse(not.excludesRecordsMissingField("a"));
    assertNull(not.findSoundAnchorField());
  }

  @Test
  void notOverAndIsNotExcluding() {
    // not($u.a > 5 and $u.b > 5) over a record missing `a` is not(false) = true.
    PredicateNode not = new PredicateNode.Not(new PredicateNode.And(List.of(num("a"), num("b"))));
    assertFalse(not.excludesRecordsMissingField("a"));
    assertFalse(not.excludesRecordsMissingField("b"));
    assertNull(not.findSoundAnchorField());
  }

  @Test
  void doubleNegationRestoresExclusion() {
    PredicateNode notNot = new PredicateNode.Not(new PredicateNode.Not(num("a")));
    assertTrue(notNot.excludesRecordsMissingField("a"));
    assertEquals("a", notNot.findSoundAnchorField());
  }

  @Test
  void soundConjunctRescuesUnsoundOr() {
    // (a > 5 or b > 5) and c > 5 — `c` anchors soundly.
    PredicateNode p = new PredicateNode.And(List.of(new PredicateNode.Or(List.of(num("a"), num("b"))), num("c")));
    assertFalse(p.excludesRecordsMissingField("a"));
    assertFalse(p.excludesRecordsMissingField("b"));
    assertTrue(p.excludesRecordsMissingField("c"));
    assertEquals("c", p.findSoundAnchorField());
  }

  @Test
  void constantsBehave() {
    assertTrue(PredicateNode.AlwaysFalse.INSTANCE.excludesRecordsMissingField("a"));
    assertFalse(PredicateNode.AlwaysTrue.INSTANCE.excludesRecordsMissingField("a"));
    assertTrue(PredicateNode.AlwaysTrue.INSTANCE.satisfiedWhenFieldMissing("a"));
    // not(true) is always false → excludes everything.
    assertTrue(new PredicateNode.Not(PredicateNode.AlwaysTrue.INSTANCE).excludesRecordsMissingField("a"));
  }

  @Test
  void collectFieldsIncludesFpCmp() {
    final java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>();
    new PredicateNode.And(List.of(fp("s"), num("a"))).collectFields(fields);
    assertEquals(List.of("s", "a"), List.copyOf(fields));
  }
}
