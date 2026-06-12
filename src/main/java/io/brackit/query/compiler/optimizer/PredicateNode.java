/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer;

import java.util.Collections;
import java.util.List;

/**
 * Immutable predicate-expression tree that vectorized executors can evaluate
 * against a tuple batch. Replaces the historical shape-specific executor SPI
 * ({@code executeFilterCount}, {@code executeFilterCount2},
 * {@code executeFilterCountAndBool}, {@code executeFilterCount2AndBool}, ...)
 * with a single representation that can express arbitrary conjunctions,
 * disjunctions, negations and leaf comparisons.
 *
 * <p>This mirrors the design of Umbra / DuckDB / ClickHouse / Velox physical
 * Filter operators: the operator doesn't care which specific combination of
 * predicates a query has; it walks the tree (or, in Umbra's case, JIT-compiles
 * it) and evaluates it against the batch. The executor SPI therefore only
 * needs {@code executePredicateCount}, {@code executePredicateAggregate}, and
 * {@code executePredicateGroupByCount}, regardless of how many AND / OR /
 * NOT conjuncts and operand shapes appear in the predicate.
 *
 * <p>Instances are produced by the optimizer-stage walker (see
 * {@code VectorizedGroupByDetection}) and consumed by registered executors
 * (e.g. SirixDB's {@code SirixVectorizedExecutor}). Unrepresentable predicates
 * leave the annotation off the AST so the generic Volcano pipeline handles
 * the query.
 */
public sealed interface PredicateNode permits PredicateNode.NumCmp, PredicateNode.FpCmp, PredicateNode.DecCmp, PredicateNode.StrEq, PredicateNode.BoolRef, PredicateNode.And, PredicateNode.Or, PredicateNode.Not, PredicateNode.AlwaysTrue, PredicateNode.AlwaysFalse {

  /**
   * Numeric comparison {@code $u.field <op> literal}. {@code op} is one of
   * {@code "gt"}, {@code "lt"}, {@code "ge"}, {@code "le"}, {@code "eq"},
   * {@code "ne"}.
   */
  record NumCmp(String field, String op, long value) implements PredicateNode {
  }

  /**
   * Floating-point numeric comparison {@code $u.field <op> literal} where the
   * literal is an {@code xs:double}. {@code op} uses the same encoding as
   * {@link NumCmp}.
   *
   * <p>SEMANTICS CONTRACT (mirrors the interpreter's numeric promotion for an
   * xs:double operand): a document value {@code v} satisfies the leaf iff
   * {@code Double.compare(v.doubleValue(), value) <op> 0} — integer values
   * promote via {@code (double) v} (including the interpreter-sanctioned
   * precision loss above {@code 2^53}), doubles compare directly, decimals
   * via {@code BigDecimal#doubleValue()}. This is exactly what brackit's
   * {@code Int64#cmp} / {@code Dbl#cmp} / {@code Dec#cmp} compute when the
   * other operand is a {@code DblNumeric}.
   */
  record FpCmp(String field, String op, double value) implements PredicateNode {
  }

  /**
   * Exact decimal comparison {@code $u.field <op> literal} where the literal
   * is a non-integral (or non-long-representable) {@code xs:decimal}.
   * {@code op} uses the same encoding as {@link NumCmp}.
   *
   * <p>SEMANTICS CONTRACT (mirrors the interpreter's dispatch on the VALUE's
   * type — {@code IntNumeric extends DecNumeric}, so integer and decimal
   * document values compare against an xs:decimal EXACTLY in decimal space,
   * while doubles promote the decimal):
   * <ul>
   * <li>integer value {@code v}: {@code BigDecimal.valueOf(v).compareTo(value)}
   * (exact — {@code Int64#cmp}'s {@code DecNumeric} branch);</li>
   * <li>decimal value {@code v}: {@code v.compareTo(value)} (exact —
   * {@code Dec#cmp});</li>
   * <li>double value {@code v}:
   * {@code Double.compare(v, value.doubleValue())} ({@code Dbl#cmp}'s
   * promotion of the decimal operand).</li>
   * </ul>
   * Long-representable integral decimals are normalized to {@link NumCmp} at
   * detection; this leaf carries the irreducible cases (9.99, values beyond
   * long range, sub-double precision) without any loss.
   */
  record DecCmp(String field, String op, java.math.BigDecimal value) implements PredicateNode {
    public DecCmp {
      java.util.Objects.requireNonNull(value, "value");
    }
  }

  /** String equality {@code $u.field eq "literal"}. */
  record StrEq(String field, String value) implements PredicateNode {
  }

  /**
   * Effective-boolean-value of a field deref, i.e. the XQuery {@code where $u.field}
   * on a JSON-boolean value. Equivalent to {@code field eq true}.
   */
  record BoolRef(String field) implements PredicateNode {
  }

  /**
   * Conjunction. Empty children list is {@link AlwaysTrue} by convention (the
   * zero-element AND identity); callers should prefer producing {@link AlwaysTrue}
   * directly. Short-circuit evaluation is permitted.
   */
  record And(List<PredicateNode> children) implements PredicateNode {
    public And {
      children = List.copyOf(children);
    }
  }

  /**
   * Disjunction. Empty children list is {@link AlwaysFalse} by convention.
   */
  record Or(List<PredicateNode> children) implements PredicateNode {
    public Or {
      children = List.copyOf(children);
    }
  }

  /** Logical negation. */
  record Not(PredicateNode child) implements PredicateNode {
  }

  /** Constant {@code true} — used by optimizers after simplification. */
  record AlwaysTrue() implements PredicateNode {
    public static final AlwaysTrue INSTANCE = new AlwaysTrue();
  }

  /** Constant {@code false}. */
  record AlwaysFalse() implements PredicateNode {
    public static final AlwaysFalse INSTANCE = new AlwaysFalse();
  }

  // ---------------- convenience builders ----------------

  static PredicateNode and(List<PredicateNode> parts) {
    if (parts.isEmpty())
      return AlwaysTrue.INSTANCE;
    if (parts.size() == 1)
      return parts.getFirst();
    return new And(parts);
  }

  static PredicateNode or(List<PredicateNode> parts) {
    if (parts.isEmpty())
      return AlwaysFalse.INSTANCE;
    if (parts.size() == 1)
      return parts.getFirst();
    return new Or(parts);
  }

  // ---------------- introspection helpers ----------------

  /**
   * Collect the set of field names this predicate touches. Useful for the
   * executor to pre-resolve nameKeys / page regions before the hot scan.
   */
  default void collectFields(java.util.Set<String> into) {
    switch (this) {
      case NumCmp n -> into.add(n.field);
      case FpCmp f -> into.add(f.field);
      case DecCmp d -> into.add(d.field);
      case StrEq s -> into.add(s.field);
      case BoolRef b -> into.add(b.field);
      case And a -> {
        for (PredicateNode c : a.children)
          c.collectFields(into);
      }
      case Or o -> {
        for (PredicateNode c : o.children)
          c.collectFields(into);
      }
      case Not n -> n.child.collectFields(into);
      case AlwaysTrue t -> {
      }
      case AlwaysFalse f -> {
      }
    }
  }

  // ---------------- missing-field (sparse-data) analysis ----------------

  /**
   * {@code true} iff this predicate is PROVABLY FALSE for every record on
   * which {@code field} is absent, regardless of the other fields' values.
   *
   * <p>Anchor-based scan executors (e.g. SirixDB's page scan) iterate records
   * via one "anchor" field's slots and therefore never visit a record that
   * lacks the anchor. That is only sound when records missing the anchor
   * cannot satisfy the predicate. Comparison/EBV leaves over a missing field
   * evaluate over the empty sequence and are false in XQuery, so:
   * <ul>
   * <li>a leaf on {@code field} is provably false when {@code field} is missing,</li>
   * <li>a leaf on a different field may be anything,</li>
   * <li>{@code And} is false if ANY child is provably false,</li>
   * <li>{@code Or} is false only if ALL children are provably false,</li>
   * <li>{@code Not} is false iff its child is provably TRUE — the De Morgan
   * landmine: {@code not($u.x > 5)} over a record missing {@code x} is
   * {@code not(false) = true}.</li>
   * </ul>
   */
  default boolean excludesRecordsMissingField(String field) {
    return switch (this) {
      case NumCmp n -> n.field.equals(field);
      case FpCmp f -> f.field.equals(field);
      case DecCmp d -> d.field.equals(field);
      case StrEq s -> s.field.equals(field);
      case BoolRef b -> b.field.equals(field);
      case And a -> {
        for (PredicateNode c : a.children) {
          if (c.excludesRecordsMissingField(field))
            yield true;
        }
        yield false;
      }
      case Or o -> {
        for (PredicateNode c : o.children) {
          if (!c.excludesRecordsMissingField(field))
            yield false;
        }
        // Vacuously: an empty Or is AlwaysFalse by convention.
        yield true;
      }
      case Not n -> n.child.satisfiedWhenFieldMissing(field);
      case AlwaysTrue t -> false;
      case AlwaysFalse f -> true;
    };
  }

  /**
   * {@code true} iff this predicate is PROVABLY TRUE for every record on
   * which {@code field} is absent, regardless of the other fields' values.
   * Dual of {@link #excludesRecordsMissingField(String)} — needed for the
   * {@code Not} case.
   */
  default boolean satisfiedWhenFieldMissing(String field) {
    return switch (this) {
      case NumCmp n -> false;
      case FpCmp f -> false;
      case DecCmp d -> false;
      case StrEq s -> false;
      case BoolRef b -> false;
      case And a -> {
        for (PredicateNode c : a.children) {
          if (!c.satisfiedWhenFieldMissing(field))
            yield false;
        }
        yield true;
      }
      case Or o -> {
        for (PredicateNode c : o.children) {
          if (c.satisfiedWhenFieldMissing(field))
            yield true;
        }
        yield false;
      }
      case Not n -> n.child.excludesRecordsMissingField(field);
      case AlwaysTrue t -> true;
      case AlwaysFalse f -> false;
    };
  }

  /**
   * First field (in {@link #collectFields} order) that is a SOUND scan anchor
   * for this predicate: records missing it are provably excluded, so an
   * anchor-based executor that never visits them still produces exactly the
   * generic pipeline's result. Returns {@code null} when no such field exists
   * (e.g. {@code $u.a > 1 or $u.b > 1}) — callers must then fail closed and
   * leave the query to the generic pipeline.
   */
  default String findSoundAnchorField() {
    final java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>();
    collectFields(fields);
    for (String f : fields) {
      if (excludesRecordsMissingField(f))
        return f;
    }
    return null;
  }

  /** Singleton AlwaysTrue as a {@link List}-friendly constant. */
  List<PredicateNode> EMPTY = Collections.emptyList();
}
