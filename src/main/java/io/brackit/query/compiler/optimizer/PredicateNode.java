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
public sealed interface PredicateNode permits PredicateNode.NumCmp, PredicateNode.StrEq, PredicateNode.BoolRef, PredicateNode.And, PredicateNode.Or, PredicateNode.Not, PredicateNode.AlwaysTrue, PredicateNode.AlwaysFalse {

  /**
   * Numeric comparison {@code $u.field <op> literal}. {@code op} is one of
   * {@code "gt"}, {@code "lt"}, {@code "ge"}, {@code "le"}, {@code "eq"},
   * {@code "ne"}.
   */
  record NumCmp(String field, String op, long value) implements PredicateNode {
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

  /** Singleton AlwaysTrue as a {@link List}-friendly constant. */
  List<PredicateNode> EMPTY = Collections.emptyList();
}
