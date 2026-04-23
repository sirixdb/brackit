/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.walker.topdown;

import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.optimizer.PredicateNode;
import io.brackit.query.compiler.optimizer.Stage;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.module.StaticContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Optimizer stage that detects FLWOR patterns eligible for vectorized execution.
 * <p>
 * Detected patterns:
 * <ol>
 * <li>Group-by: {@code for $u in SRC let $c := $u.F group by $c return ...}</li>
 * <li>Filtered count: {@code for $u in SRC where $u.F > N return ...}</li>
 * <li>Filtered group-by: {@code for $u in SRC where $u.F > N let $c := $u.G group by $c ...}</li>
 * <li>Multi-key group-by: {@code for $u let $c := $u.F, $d := $u.G group by $c, $d ...}</li>
 * <li>Sorted scan: {@code for $u in SRC order by $u.F descending return $u}</li>
 * <li>Top-N: {@code for $u in SRC order by $u.F return $u[0:N]}</li>
 * <li>String equality filter: {@code for $u where $u.city eq "NYC" return ...}</li>
 * <li>Compound predicate (AND): {@code where $u.age > 30 and $u.city eq "NYC"}</li>
 * <li>Existence check: {@code where exists($u.email)}</li>
 * </ol>
 */
public final class VectorizedGroupByDetection implements Stage {

  @Override
  public AST rewrite(StaticContext sctx, AST ast) {
    walkAndAnnotate(ast);
    return ast;
  }

  private void walkAndAnnotate(AST node) {
    if (node == null)
      return;
    if (node.getType() == XQ.PipeExpr) {
      tryAnnotate(node);
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      walkAndAnnotate(node.getChild(i));
    }
  }

  // ==================== Main pattern matcher ====================

  private void tryAnnotate(AST pipeExpr) {
    if (pipeExpr.getChildCount() < 1)
      return;
    AST chain = pipeExpr.getChild(0);
    if (chain.getType() != XQ.Start || chain.getChildCount() < 1) {
      return;
    }
    // The chain may have outer LetBinds (e.g. `let $doc := jn:doc(...) for $u in $doc[] ...`)
    // that wrap the actual ForBind. Walk down through LetBinds to find the ForBind.
    AST forBind = chain.getLastChild();
    while (forBind != null && forBind.getType() == XQ.LetBind) {
      forBind = forBind.getLastChild();
    }
    if (forBind == null || forBind.getType() != XQ.ForBind)
      return;

    List<String> groupFields = new ArrayList<>();
    // Declared variable names for LetBinds that feed group-by — used to verify
    // count-distinct patterns (return expr must be a VarRef matching one of these).
    List<QNm> letBindVars = new ArrayList<>();
    String orderField = null;
    String orderDirection = null;
    boolean hasGroupBy = false;
    boolean hasOrderBy = false;

    // Selection predicates accumulated across the chain. Multiple Selection
    // operators AND-conjoin by pipeline semantics. If ANY selection is not
    // representable as a PredicateNode we drop the annotation entirely, forcing
    // the generic Volcano pipeline — fail closed.
    List<PredicateNode> predicateConjuncts = new ArrayList<>();
    boolean predicateRepresentable = true;

    AST current = forBind.getLastChild();
    while (current != null && current.getType() != XQ.End) {
      switch (current.getType()) {
        case XQ.Selection -> {
          if (predicateRepresentable && current.getChildCount() >= 1) {
            PredicateNode pn = extractPredicate(current.getChild(0));
            if (pn == null) {
              predicateRepresentable = false;
            } else {
              predicateConjuncts.add(pn);
            }
          }
        }
        case XQ.LetBind -> {
          if (current.getChildCount() >= 2) {
            String field = extractFieldFromDeref(current.getChild(1));
            if (field != null) {
              groupFields.add(field);
              letBindVars.add(extractLetBindVarName(current));
            }
          }
        }
        case XQ.GroupBy -> hasGroupBy = true;
        case XQ.OrderBy -> {
          hasOrderBy = true;
          var order = extractOrderBy(current);
          if (order != null) {
            orderField = order.field;
            orderDirection = order.direction;
          }
        }
        default -> {
        }
      }
      current = current.getLastChild();
    }

    // If the return expression is a single DerefExpr `$u.field`, capture the field.
    // An enclosing sum()/avg()/min()/max() call can use this to vectorize
    // (see Compiler.functionCall interception).
    String returnField = current != null ? extractFieldFromDeref(current) : null;

    // ---- Annotate based on detected pattern ----

    if (hasGroupBy && !groupFields.isEmpty()) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, groupFields.getFirst());
      if (groupFields.size() > 1) {
        pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELDS_EXTRA,
                             groupFields.subList(1, groupFields.size()).toArray(new String[0]));
      }
    }

    final boolean hasPredicate = predicateRepresentable && !predicateConjuncts.isEmpty();
    if (hasPredicate) {
      pipeExpr.setProperty(VectorizedScanAnnotation.PREDICATE_TREE, PredicateNode.and(predicateConjuncts));
      // No group-by and no order-by → filtered count shape.
      if (!hasGroupBy && !hasOrderBy) {
        pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT, Boolean.TRUE);
      }
    }

    // Extract the loop variable's source path prefix (the expression supplying
    // $u). Executors combine it with per-predicate field names to scope queries
    // to a specific tree path — eliminates false matches against same-name
    // fields nested at other depths. Absent if the source isn't a simple path
    // expression; executors then fall back to a path-agnostic tree-walk.
    if (forBind.getChildCount() >= 2) {
      final String[] sourcePath = extractPathPrefix(forBind.getChild(1));
      if (sourcePath != null) {
        pipeExpr.setProperty(VectorizedScanAnnotation.SOURCE_PATH_PREFIX, sourcePath);
      }
    }

    if (hasOrderBy && orderField != null) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_FIELD, orderField);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_DIRECTION, orderDirection);
    }

    // Pure aggregate candidate: ForBind -> Return($u.field), no group-by, no order-by.
    // A predicate, if present, is carried via PREDICATE_TREE — the enclosing
    // sum/avg/min/max/count() call fills AGGREGATE_FUNC at compile time and the
    // dispatcher routes to executePredicateAggregate.
    if (!hasGroupBy && !hasOrderBy && returnField != null) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_AGGREGATE, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.AGGREGATE_FIELD, returnField);
    }

    // Count-distinct candidate: a single-key group-by whose return expression is
    // exactly the group key variable, e.g.:
    //   count(for $u in SRC let $d := $u.F group by $d return $d)
    // When wrapped in count(), the tuple count equals the number of distinct $d
    // values → answerable directly from a cardinality sketch (HLL) at query time.
    // Correctness guard: the return must be a VarRef whose QNm matches the first
    // group-by let-bind's declared variable; otherwise count(...) could return
    // a multiple of the distinct-count (e.g., return ($d, $d)). Also disallow
    // predicates — an HLL is unfiltered.
    if (hasGroupBy && !hasOrderBy && !hasPredicate && groupFields.size() == 1 && !letBindVars.isEmpty() && letBindVars
                                                                                                                      .getFirst()
        != null) {
      final QNm returnVar = current != null ? extractSoleVariableRef(current) : null;
      if (returnVar != null && returnVar.equals(letBindVars.getFirst())) {
        pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT_DISTINCT, Boolean.TRUE);
        pipeExpr.setProperty(VectorizedScanAnnotation.COUNT_DISTINCT_FIELD, groupFields.getFirst());
      }
    }
  }

  /** Get the declared QNm of a LetBind's variable binding — {@code null} if absent. */
  private QNm extractLetBindVarName(final AST letBind) {
    if (letBind.getChildCount() < 1)
      return null;
    final AST varBinding = letBind.getChild(0);
    if (varBinding.getChildCount() < 1)
      return null;
    final Object val = varBinding.getChild(0).getValue();
    return val instanceof QNm qnm ? qnm : null;
  }

  /**
   * Return the QNm of a {@link XQ#VariableRef} that is the sole non-structural node
   * in the return subtree. Returns {@code null} if there are zero or multiple VarRefs,
   * or if any non-VarRef expression node (e.g. FunctionCall, ArithmeticOp) is present
   * — preventing mis-detection of {@code return ($d, $d)} or {@code return f($d)}.
   */
  private QNm extractSoleVariableRef(final AST node) {
    final QNm[] found = { null };
    final boolean[] tooComplex = { false };
    scanForSoleVarRef(node, found, tooComplex);
    return tooComplex[0] ? null : found[0];
  }

  private void scanForSoleVarRef(final AST node, final QNm[] found, final boolean[] tooComplex) {
    if (node == null || tooComplex[0])
      return;
    final int type = node.getType();
    if (type == XQ.VariableRef) {
      final Object val = node.getValue();
      if (val instanceof QNm qnm) {
        if (found[0] != null && !found[0].equals(qnm)) {
          tooComplex[0] = true;
        } else {
          found[0] = qnm;
        }
      }
      return;
    }
    // Expression-bearing node types disqualify the pattern — `return $d + 1`,
    // `return concat($d, "x")`, `return ($d, $d)` etc.
    switch (type) {
      case XQ.FunctionCall, XQ.ArithmeticExpr, XQ.DerefExpr, XQ.PathExpr, XQ.SequenceExpr, XQ.RangeExpr, XQ.IfExpr,
          XQ.LetBind, XQ.ComparisonExpr -> {
        tooComplex[0] = true;
        return;
      }
      default -> {
        /* structural / pass-through — recurse into children */ }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      scanForSoleVarRef(node.getChild(i), found, tooComplex);
      if (tooComplex[0])
        return;
    }
  }

  // ==================== Source path-prefix extraction ====================

  /**
   * Extract the loop variable's source path as a list of step names. Each
   * array-descent ({@code [...][]}) is emitted as {@code "[]"}; each field
   * dereference emits the field's local name. The bottom of the traversal
   * must be a {@link XQ#VariableRef} or a {@link XQ#FunctionCall} whose result
   * is implicitly the document root (e.g. {@code jn:doc(...)}); both terminate
   * the prefix, meaning the path starts at whatever $doc root the query binds.
   *
   * <p>Returns {@code null} for any unrepresentable shape (arithmetic,
   * sequence constructor, if/then/else, etc.). Callers use absence of the
   * {@link VectorizedScanAnnotation#SOURCE_PATH_PREFIX} annotation as a
   * signal to use a path-agnostic fallback.
   *
   * <p>Production queries typically parse to one of:
   * <pre>
   * for $u in $doc[] → ArrayAccess(VariableRef, -1) ⇒ ["[]"]
   * for $u in $doc.items[] → ArrayAccess(DerefExpr, -1) ⇒ ["items", "[]"]
   * for $u in $doc[].items[] → ArrayAccess(DerefExpr(ArrayAccess(...), items), -1)
   * ⇒ ["[]", "items", "[]"]
   * for $u in $doc.items → DerefExpr(VariableRef, items) ⇒ ["items"]
   * for $u in jn:doc('db','r')[] → ArrayAccess(FunctionCall, -1) ⇒ ["[]"]
   * </pre>
   */
  private String[] extractPathPrefix(final AST sourceExpr) {
    if (sourceExpr == null) {
      return null;
    }
    final List<String> reversed = new ArrayList<>(4);
    AST current = sourceExpr;
    while (current != null) {
      final int type = current.getType();
      if (type == XQ.ArrayAccess) {
        // ArrayAccess(subject, index). Treat any index as a full-descent marker:
        // path-prefix scoping doesn't distinguish element positions, only depth.
        reversed.add("[]");
        current = current.getChildCount() > 0 ? current.getChild(0) : null;
        continue;
      }
      if (type == XQ.DerefExpr && current.getChildCount() >= 2) {
        final AST fieldNode = current.getChild(current.getChildCount() - 1);
        final String name = qnmLocalName(fieldNode.getValue());
        if (name == null) {
          return null;
        }
        reversed.add(name);
        current = current.getChild(0);
        continue;
      }
      if (type == XQ.VariableRef || type == XQ.FunctionCall) {
        // Terminal — the path starts from this root. The variable / function
        // identity itself is not part of the path scoping, since Sirix's
        // pathNodeKey paths are rooted at the document.
        break;
      }
      // Any other AST shape (arithmetic, comparison, sequence constructor,
      // if-expr, etc.) is not representable as a simple path prefix.
      return null;
    }
    // Reverse: traversal built the list innermost-first (closest to leaf),
    // callers want outermost-first (from document root toward the field).
    final String[] prefix = new String[reversed.size()];
    for (int i = 0; i < reversed.size(); i++) {
      prefix[i] = reversed.get(reversed.size() - 1 - i);
    }
    return prefix;
  }

  /** Extract the local name from a QNm or String value on an AST node; null otherwise. */
  private static String qnmLocalName(final Object value) {
    if (value instanceof QNm qnm) {
      return qnm.getLocalName();
    }
    if (value instanceof String s) {
      return s;
    }
    return null;
  }

  // ==================== Generic predicate-tree extraction ====================

  /**
   * Recursively build a {@link PredicateNode} from an AST predicate subtree.
   * Returns {@code null} if any part of the predicate isn't representable —
   * callers should treat {@code null} as "fall back to generic pipeline"
   * rather than silently dropping the unrepresentable clause.
   *
   * <p>Supported shapes:
   * <ul>
   * <li>{@code AndExpr(a, b, ...)} → {@link PredicateNode.And}
   * <li>{@code OrExpr(a, b, ...)} → {@link PredicateNode.Or}
   * <li>{@code NotExpr(x)} / {@code fn:not($x)} → {@link PredicateNode.Not}
   * <li>{@code $u.f OP literal} → {@link PredicateNode.NumCmp} / {@link PredicateNode.StrEq}
   * <li>{@code $u.f} (bare deref, EBV on JSON boolean) → {@link PredicateNode.BoolRef}
   * </ul>
   */
  private PredicateNode extractPredicate(AST node) {
    if (node == null)
      return null;

    final int type = node.getType();

    if (type == XQ.AndExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i));
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.and(kids);
    }
    if (type == XQ.OrExpr) {
      List<PredicateNode> kids = new ArrayList<>(node.getChildCount());
      for (int i = 0; i < node.getChildCount(); i++) {
        PredicateNode c = extractPredicate(node.getChild(i));
        if (c == null)
          return null;
        kids.add(c);
      }
      return PredicateNode.or(kids);
    }

    // Bare deref: EBV of a JSON boolean field.
    if (type == XQ.DerefExpr) {
      String bf = directDerefFieldName(node);
      return bf != null ? new PredicateNode.BoolRef(bf) : null;
    }

    // Comparison: either a direct GeneralCompGT(left, right) or
    // ComparisonExpr(cmpKind, left, right).
    String op = getComparisonOp(type);
    AST leftOperand, rightOperand;
    if (op != null && node.getChildCount() >= 2) {
      leftOperand = node.getChild(0);
      rightOperand = node.getChild(1);
    } else if (type == XQ.ComparisonExpr && node.getChildCount() >= 3) {
      op = getComparisonOp(node.getChild(0).getType());
      leftOperand = node.getChild(1);
      rightOperand = node.getChild(2);
    } else {
      return null;
    }
    if (op == null || leftOperand == null)
      return null;

    // $u.F OP literal — field on left.
    String field = directDerefFieldName(leftOperand);
    if (field != null) {
      Long lv = extractIntegerLiteral(rightOperand);
      if (lv != null)
        return new PredicateNode.NumCmp(field, op, lv);
      String sv = extractStringLiteral(rightOperand);
      if (sv != null && "eq".equals(op))
        return new PredicateNode.StrEq(field, sv);
      return null;
    }
    // literal OP $u.F — field on right. Reverse the comparison direction.
    field = directDerefFieldName(rightOperand);
    if (field != null) {
      Long lv = extractIntegerLiteral(leftOperand);
      if (lv != null)
        return new PredicateNode.NumCmp(field, reverseOp(op), lv);
      String sv = extractStringLiteral(leftOperand);
      if (sv != null && "eq".equals(op))
        return new PredicateNode.StrEq(field, sv);
    }
    return null;
  }

  private String getComparisonOp(int type) {
    return switch (type) {
      case XQ.GeneralCompGT, XQ.ValueCompGT -> "gt";
      case XQ.GeneralCompLT, XQ.ValueCompLT -> "lt";
      case XQ.GeneralCompGE, XQ.ValueCompGE -> "ge";
      case XQ.GeneralCompLE, XQ.ValueCompLE -> "le";
      case XQ.GeneralCompEQ, XQ.ValueCompEQ -> "eq";
      default -> null;
    };
  }

  private String reverseOp(String op) {
    return switch (op) {
      case "gt" -> "lt";
      case "lt" -> "gt";
      case "ge" -> "le";
      case "le" -> "ge";
      default -> op;
    };
  }

  // ==================== OrderBy extraction ====================

  private record OrderInfo(String field, String direction) {
  }

  private OrderInfo extractOrderBy(AST orderBy) {
    if (orderBy.getChildCount() < 1)
      return null;
    AST spec = orderBy.getChild(0);
    if (spec.getType() != XQ.OrderBySpec)
      return null;

    String field = spec.getChildCount() >= 1 ? extractFieldFromDeref(spec.getChild(0)) : null;
    String direction = "ascending";
    for (int i = 1; i < spec.getChildCount(); i++) {
      if (spec.getChild(i).getType() == XQ.OrderByKind) {
        Object val = spec.getChild(i).getValue();
        if (val != null)
          direction = val.toString().toLowerCase();
      }
    }
    return field != null ? new OrderInfo(field, direction) : null;
  }

  // ==================== Field name extraction ====================

  /** Field name for a node that is DIRECTLY a DerefExpr — no descent into children. */
  private String directDerefFieldName(AST node) {
    if (node == null || node.getType() != XQ.DerefExpr || node.getChildCount() < 2)
      return null;
    final AST fieldNode = node.getChild(node.getChildCount() - 1);
    final Object value = fieldNode.getValue();
    if (value instanceof QNm qnm)
      return qnm.getLocalName();
    if (value instanceof String s)
      return s;
    return null;
  }

  private String extractFieldFromDeref(AST node) {
    if (node == null)
      return null;
    if (node.getType() == XQ.DerefExpr && node.getChildCount() >= 2) {
      AST fieldNode = node.getChild(node.getChildCount() - 1);
      Object value = fieldNode.getValue();
      if (value instanceof QNm qnm)
        return qnm.getLocalName();
      if (value instanceof String s)
        return s;
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      String result = extractFieldFromDeref(node.getChild(i));
      if (result != null)
        return result;
    }
    return null;
  }

  // ==================== Literal extraction ====================

  private Long extractIntegerLiteral(AST node) {
    if (node == null)
      return null;
    if (node.getType() == XQ.Int || node.getType() == XQ.Dbl || node.getType() == XQ.Dec) {
      Object val = node.getValue();
      if (val instanceof Number n)
        return n.longValue();
      // Brackit numeric types (Int32, Int64, etc.) extend Numeric, not java.lang.Number
      if (val instanceof io.brackit.query.atomic.Numeric num)
        return num.longValue();
      if (val instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    Object val = node.getValue();
    if (val instanceof Number n)
      return n.longValue();
    if (val instanceof io.brackit.query.atomic.Numeric num)
      return num.longValue();
    return null;
  }

  private String extractStringLiteral(AST node) {
    if (node == null)
      return null;
    if (node.getType() == XQ.Str) {
      Object val = node.getValue();
      if (val instanceof String s)
        return s;
      // Brackit Str type
      if (val instanceof io.brackit.query.atomic.Str str)
        return str.stringValue();
      if (val != null)
        return val.toString();
    }
    return null;
  }
}
