/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.walker.topdown;

import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
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

    // Collect all operators in the chain
    List<FilterInfo> filters = new ArrayList<>();
    List<String> groupFields = new ArrayList<>();
    // Declared variable names for LetBinds that feed group-by — used to verify
    // count-distinct patterns (return expr must be a VarRef matching one of these).
    List<QNm> letBindVars = new ArrayList<>();
    String orderField = null;
    String orderDirection = null;
    boolean hasGroupBy = false;
    boolean hasOrderBy = false;

    AST current = forBind.getLastChild();
    while (current != null && current.getType() != XQ.End) {
      switch (current.getType()) {
        case XQ.Selection -> extractFilters(current, filters);
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

    if (!filters.isEmpty()) {
      FilterInfo f1 = filters.getFirst();
      applyFilter(pipeExpr, f1, false);
      if (filters.size() > 1) {
        applyFilter(pipeExpr, filters.get(1), true);
      }

      // If no group-by and no order-by, this is a filtered count pattern
      if (!hasGroupBy && !hasOrderBy) {
        pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT, Boolean.TRUE);
      }
    }

    if (hasOrderBy && orderField != null) {
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_ORDERBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_FIELD, orderField);
      pipeExpr.setProperty(VectorizedScanAnnotation.ORDER_DIRECTION, orderDirection);
    }

    // Pure aggregate candidate: ForBind -> Return($u.field), no group-by, no order-by,
    // no filters. The enclosing FunctionCall (sum/avg/min/max/count) fills in AGGREGATE_FUNC
    // at compile time; the field is known now.
    if (!hasGroupBy && !hasOrderBy && filters.isEmpty() && returnField != null) {
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
    // a multiple of the distinct-count (e.g., return ($d, $d)).
    if (hasGroupBy && !hasOrderBy && filters.isEmpty() && groupFields.size() == 1 && !letBindVars.isEmpty()
        && letBindVars.getFirst() != null) {
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

  private void applyFilter(AST node, FilterInfo f, boolean isSecond) {
    if (isSecond) {
      node.setProperty(VectorizedScanAnnotation.FILTER2_FIELD, f.field);
      node.setProperty(VectorizedScanAnnotation.FILTER2_OP, f.op);
      if (f.longValue != null)
        node.setProperty(VectorizedScanAnnotation.FILTER2_VALUE, f.longValue);
      if (f.stringValue != null)
        node.setProperty(VectorizedScanAnnotation.FILTER2_STRING_VALUE, f.stringValue);
    } else {
      node.setProperty(VectorizedScanAnnotation.FILTER_FIELD, f.field);
      node.setProperty(VectorizedScanAnnotation.FILTER_OP, f.op);
      if (f.longValue != null)
        node.setProperty(VectorizedScanAnnotation.FILTER_VALUE, f.longValue);
      if (f.stringValue != null)
        node.setProperty(VectorizedScanAnnotation.FILTER_STRING_VALUE, f.stringValue);
    }
  }

  // ==================== Filter extraction ====================

  private record FilterInfo(String field, String op, Long longValue, String stringValue) {
  }

  private void extractFilters(AST selection, List<FilterInfo> filters) {
    if (selection.getChildCount() < 1)
      return;
    extractFromPredicate(selection.getChild(0), filters);
  }

  private void extractFromPredicate(AST node, List<FilterInfo> filters) {
    if (node == null)
      return;

    // AND: recurse into both sides
    if (node.getType() == XQ.AndExpr) {
      for (int i = 0; i < node.getChildCount(); i++) {
        extractFromPredicate(node.getChild(i), filters);
      }
      return;
    }

    // Comparison operators — two forms:
    // 1. Direct: GeneralCompGT(leftExpr, rightExpr)
    // 2. Wrapped: ComparisonExpr(GeneralCompGT, leftExpr, rightExpr)
    String op = getComparisonOp(node.getType());
    AST leftOperand;
    AST rightOperand;

    if (op != null && node.getChildCount() >= 2) {
      leftOperand = node.getChild(0);
      rightOperand = node.getChild(1);
    } else if (node.getType() == XQ.ComparisonExpr && node.getChildCount() >= 3) {
      op = getComparisonOp(node.getChild(0).getType());
      leftOperand = node.getChild(1);
      rightOperand = node.getChild(2);
    } else {
      leftOperand = null;
      rightOperand = null;
    }

    if (op != null && leftOperand != null) {
      // Try: $var.field OP literal
      String field = extractFieldFromDeref(leftOperand);
      Long longVal = extractIntegerLiteral(rightOperand);
      String strVal = extractStringLiteral(rightOperand);

      if (field != null && (longVal != null || strVal != null)) {
        filters.add(new FilterInfo(field, op, longVal, strVal));
        return;
      }

      // Try reversed: literal OP $var.field
      field = extractFieldFromDeref(rightOperand);
      longVal = extractIntegerLiteral(leftOperand);
      strVal = extractStringLiteral(leftOperand);

      if (field != null && (longVal != null || strVal != null)) {
        filters.add(new FilterInfo(field, reverseOp(op), longVal, strVal));
        return;
      }
    }

    // Recurse for nested expressions
    for (int i = 0; i < node.getChildCount(); i++) {
      extractFromPredicate(node.getChild(i), filters);
    }
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
