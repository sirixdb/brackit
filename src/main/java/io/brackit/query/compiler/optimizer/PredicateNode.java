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
public sealed interface PredicateNode permits PredicateNode.NumCmp, PredicateNode.FpCmp, PredicateNode.DecCmp, PredicateNode.StrEq, PredicateNode.StrNe, PredicateNode.StrCmp, PredicateNode.StrContains, PredicateNode.ArrayContains, PredicateNode.BoolRef, PredicateNode.And, PredicateNode.Or, PredicateNode.Not, PredicateNode.AlwaysTrue, PredicateNode.AlwaysFalse {

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
   * String inequality {@code $u.field ne "literal"}.
   *
   * <p>A variant of its own rather than an operator on {@link StrEq}, and deliberately NOT
   * expressible as {@code Not(StrEq)}. Over a record MISSING the field the deref is the empty
   * sequence, a general comparison over it is false, and so this leaf is false — whereas
   * {@code Not(StrEq)} is true there. The two differ on exactly the records an anchored scan is
   * allowed to skip, which is why this one is soundly anchorable and a negation is not.
   *
   * <p>JSON null is a separate matter, and one a backend must handle rather than assume away: per
   * the JSONiq specification a null-valued field DOES satisfy this leaf ("True is returned when
   * comparing it with non-equality with any non-null atomic"; the spec's own example gives
   * {@code 1 eq null, "foo" ne null, null eq null} =&gt; {@code false true true}). A backend whose
   * kernels cannot tell a null from a missing field must therefore DECLINE this predicate over a
   * null-bearing column, not serve it.
   */
  record StrNe(String field, String value) implements PredicateNode {
  }

  /**
   * String ORDERING comparison {@code $u.field <op> "literal"}; {@code op} is one of {@code "gt"},
   * {@code "lt"}, {@code "ge"}, {@code "le"} only — equality stays {@link StrEq} and inequality
   * {@link StrNe}, whose missing-field/null contracts differ from each other and from this leaf.
   *
   * <p>COLLATION CONTRACT: the interpreter's general comparison over strings is {@code Str#cmp} =
   * {@code String.compareTo} = UTF-16 code-unit order. Unsigned UTF-8 byte order (what a
   * dictionary kernel naturally compares) is CODEPOINT order; the two differ exactly when a
   * supplementary character (U+10000 and above, a 4-byte UTF-8 sequence) meets a BMP character in
   * U+E000..U+FFFF. A backend comparing raw UTF-8 must detect that case (any 4-byte lead byte,
   * {@code (b & 0xFF) >= 0xF0}) and fall back to decoding, or it serves an order the interpreter
   * disagrees with.
   *
   * <p>NULL CONTRACT: JSONiq's total order makes null the SMALLEST value, so e.g.
   * {@code null le "x"} is TRUE — a kernel that reads null as missing-and-false must DECLINE this
   * leaf over a null-bearing column rather than serve it. Missing-field semantics are the ordinary
   * ones: the deref is the empty sequence, the comparison is false, so the leaf soundly anchors on
   * its field.
   */
  record StrCmp(String field, String op, String value) implements PredicateNode {
  }

  /**
   * Substring containment {@code fn:contains($u.field, "literal")} — the two-argument builtin
   * only; the three-argument (collation) form must never be represented here.
   *
   * <p>{@code fn:contains((), "x")} treats the empty sequence as {@code ""} and answers FALSE, so
   * the leaf is false on a record missing the field — which is exactly what makes it soundly
   * anchorable. {@code fn:not(contains(...))} is deliberately NOT a variant: it is TRUE on a
   * missing field, so representing it would require opting out of the presence-AND every mask
   * builder applies, and any site that forgot would under-count silently.
   *
   * <p>NULL CONTRACT: the interpreter raises a type error for {@code contains(null, "x")}; a
   * kernel would answer false. An error is not false — a backend must DECLINE this leaf over a
   * null-bearing column.
   *
   * <p>No collation subtlety, unlike {@link StrCmp}: UTF-8 is self-synchronizing, so a byte-wise
   * needle match IS a codepoint substring match.
   */
  record StrContains(String field, String value) implements PredicateNode {
  }

  /**
   * Membership in an array-valued field: {@code some $g in $u.field[] satisfies $g eq "literal"}.
   *
   * <p>Distinct from {@link StrEq} because the field holds a SEQUENCE, and a backend answering it
   * must test every element rather than one value — a difference that decides which column, and
   * which linkage, it has to read.
   *
   * <p>Soundly anchorable on {@code field}, and for the ordinary reason: a record that does not
   * carry the field carries no element either, so it cannot satisfy the quantifier. An empty array
   * likewise satisfies nothing, which is what makes the existential the anchorable direction and
   * a universal ({@code every ... satisfies}) not — that one is TRUE on a record with no array at
   * all, so it is not expressed here.
   */
  record ArrayContains(String field, String value) implements PredicateNode {
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
      case StrNe s -> into.add(s.field);
      case StrCmp c -> into.add(c.field);
      case StrContains c -> into.add(c.field);
      case ArrayContains c -> into.add(c.field);
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
      // A record without the field has no element to satisfy the quantifier — and an EMPTY
      // array satisfies nothing either, which is what makes the existential form the
      // anchorable one.
      case ArrayContains c -> c.field.equals(field);
      case StrEq s -> s.field.equals(field);
      // Anchorable for the same reason as StrEq, and the reason a NEGATION would not be: the deref
      // of a missing field is the empty sequence, and a general comparison over it is false. So a
      // record lacking the field cannot satisfy `field ne "x"` and an anchored scan may skip it.
      case StrNe s -> s.field.equals(field);
      // The empty-sequence comparison is false, exactly as for StrEq/StrNe.
      case StrCmp c -> c.field.equals(field);
      // contains((), lit) is contains("", lit) = false — the anchorable direction; the negation
      // (true on missing) is deliberately unrepresentable.
      case StrContains c -> c.field.equals(field);
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
      case ArrayContains c -> false;
      case StrEq s -> false;
      // FALSE, despite reading like "a missing field is surely not equal to x". The comparison is
      // over the empty sequence, which is false, not true. Answering true here would make
      // Not(StrNe) report provably-false-on-missing and an anchored scan would skip exactly the
      // records that satisfy it.
      case StrNe s -> false;
      // Same discipline: the empty-sequence comparison/containment is FALSE, never true.
      case StrCmp c -> false;
      case StrContains c -> false;
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

  /**
   * Whether a backend that DECOMPOSES this predicate — evaluating each branch over its own anchor
   * and combining the per-branch results — can serve it soundly, even when
   * {@link #findSoundAnchorField()} finds no single global anchor.
   *
   * <p>The single-anchor rule asks one field to exclude every non-matching record, which is what a
   * scan driven by one field's slots needs. Inclusion-exclusion asks strictly less:
   * {@code a > 1 or b > 1} is {@code |A| + |B| - |A and B|}, and each term is counted over ITS OWN
   * field's slots, so a record carrying only {@code b} is still visited — via {@code b}. Per node:
   * <ul>
   * <li>a leaf anchors on its own field;</li>
   * <li>{@code And} needs only ONE sound anchor, because a record missing that conjunct's field
   * cannot satisfy the conjunction; failing that, every conjunct must itself be decomposable and
   * the backend must intersect the per-conjunct results;</li>
   * <li>{@code Or} needs EVERY branch independently anchorable — the branch is what gets its own
   * scan;</li>
   * <li>{@code Not} is servable only as a COMPLEMENT, since a record missing {@code x} satisfies
   * {@code not(x > 5)}: the backend counts all records minus the child's matches, so the child
   * must itself be anchorable;</li>
   * <li>{@code AlwaysTrue} is NOT anchorable — it holds for a record with no fields at all, which
   * no field's slots enumerate.</li>
   * </ul>
   *
   * <p>This is a CAPABILITY question, not a correctness relaxation. It says the shape admits a
   * sound decomposition, not that any particular executor implements one — a backend that scans
   * from a single anchor must keep using {@link #findSoundAnchorField()} alone, and a backend that
   * claims decomposition it does not implement will silently under-count exactly as before.
   */
  default boolean isDecomposablyAnchorable() {
    return switch (this) {
      case NumCmp n -> true;
      case FpCmp f -> true;
      case DecCmp d -> true;
      case ArrayContains c -> true;
      case StrEq s -> true;
      case StrNe s -> true;
      case StrCmp c -> true;
      case StrContains c -> true;
      case BoolRef b -> true;
      // A conjunction is claimable only on a GLOBAL anchor: one conjunct whose field every
      // candidate record must carry. Deliberately no recursion into the children — a conjunction
      // of individually decomposable but unanchored parts, say
      // {@code (a gt 1 or b gt 1) and not(c)}, would need the INTERSECTION of two separately
      // decomposed result sets, which is a different capability from combining the branches of one
      // union and is not what a decomposing backend advertises here. Claiming it merely moves the
      // refusal from this optimizer to a query-time exception.
      case And a -> findSoundAnchorField() != null;
      case Or o -> {
        // An empty Or is AlwaysFalse by the convention above, but it carries no field to anchor
        // on and no branch to scan; leave it to the generic pipeline rather than special-casing.
        if (o.children.isEmpty())
          yield false;
        for (PredicateNode c : o.children) {
          if (!c.isDecomposablyAnchorable())
            yield false;
        }
        yield true;
      }
      case Not n -> n.child.isDecomposablyAnchorable();
      case AlwaysTrue t -> false;
      case AlwaysFalse f -> true;
    };
  }

  /** Singleton AlwaysTrue as a {@link List}-friendly constant. */
  List<PredicateNode> EMPTY = Collections.emptyList();
}
