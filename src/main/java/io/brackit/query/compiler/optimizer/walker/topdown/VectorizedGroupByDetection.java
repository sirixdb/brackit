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

/**
 * Optimizer stage that detects FLWOR patterns eligible for vectorized execution
 * and annotates the AST.
 * <p>
 * Detected patterns (from simplest to most complex):
 * <ol>
 * <li>{@code for $u in SRC group by $c := $u.F return {$c, count($u)}}
 * → VECTORIZED_GROUPBY with GROUPBY_FIELD</li>
 * <li>{@code for $u in SRC where $u.F op VALUE return ...}
 * → VECTORIZED_COUNT with FILTER_FIELD, FILTER_OP, FILTER_VALUE</li>
 * <li>{@code for $u in SRC where $u.F op VALUE group by $c := $u.G return ...}
 * → VECTORIZED_GROUPBY with GROUPBY_FIELD + FILTER_FIELD/OP/VALUE</li>
 * <li>{@code for $u in SRC order by $u.F return $u}
 * → VECTORIZED_ORDERBY with ORDER_FIELD (future)</li>
 * </ol>
 * <p>
 * The pattern walker is tolerant: it walks the operator chain (Start → ForBind →
 * Selection? → LetBind* → GroupBy? → OrderBy? → End) and collects all annotations
 * it can extract. The translator/executor decides which combinations it supports.
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

  private void tryAnnotate(AST pipeExpr) {
    if (pipeExpr.getChildCount() < 1)
      return;

    AST chain = pipeExpr.getChild(0);
    if (chain.getType() != XQ.Start)
      return;
    if (chain.getChildCount() < 1)
      return;

    // Start → ForBind
    AST forBind = chain.getLastChild();
    if (forBind.getType() != XQ.ForBind)
      return;

    // Walk the operator chain collecting annotations
    AST current = forBind.getLastChild();
    String filterField = null;
    String filterOp = null;
    Long filterValue = null;
    String groupField = null;
    String orderField = null;
    String orderDirection = null;
    boolean hasGroupBy = false;
    boolean hasOrderBy = false;

    while (current != null && current.getType() != XQ.End) {
      switch (current.getType()) {
        case XQ.Selection -> {
          // WHERE clause: extract field, operator, value
          var filter = extractFilter(current);
          if (filter != null) {
            filterField = filter.field;
            filterOp = filter.op;
            filterValue = filter.value;
          }
        }
        case XQ.LetBind -> {
          // LET $c := $u.field — extract field name for group-by
          if (current.getChildCount() >= 2) {
            String field = extractFieldFromDeref(current.getChild(1));
            if (field != null) {
              groupField = field;
            }
          }
        }
        case XQ.GroupBy -> {
          hasGroupBy = true;
        }
        case XQ.OrderBy -> {
          hasOrderBy = true;
          var order = extractOrderBy(current);
          if (order != null) {
            orderField = order.field;
            orderDirection = order.direction;
          }
        }
      }
      current = current.getLastChild();
    }

    // Determine which vectorized pattern applies
    if (hasGroupBy && groupField != null) {
      // Group-by pattern (with optional filter)
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, groupField);

      if (filterField != null && filterOp != null && filterValue != null) {
        pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_FIELD, filterField);
        pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_OP, filterOp);
        pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_VALUE, filterValue);
      }
    } else if (filterField != null && filterOp != null && filterValue != null && !hasGroupBy && !hasOrderBy) {
      // Pure filtered count pattern
      pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_COUNT, Boolean.TRUE);
      pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_FIELD, filterField);
      pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_OP, filterOp);
      pipeExpr.setProperty(VectorizedScanAnnotation.FILTER_VALUE, filterValue);
    }

    // OrderBy annotation (for future vectorized sort)
    if (hasOrderBy && orderField != null) {
      pipeExpr.setProperty("VECTORIZED_ORDERBY", Boolean.TRUE);
      pipeExpr.setProperty("VECTORIZED_ORDER_FIELD", orderField);
      pipeExpr.setProperty("VECTORIZED_ORDER_DIRECTION", orderDirection);
    }
  }

  // ==================== Filter extraction ====================

  private record FilterInfo(String field, String op, long value) {
  }

  /**
   * Extract filter from a Selection node.
   * Handles: $var.field > N, $var.field < N, $var.field >= N, etc.
   */
  private FilterInfo extractFilter(AST selection) {
    if (selection.getChildCount() < 1)
      return null;
    AST predicate = selection.getChild(0);
    return extractComparison(predicate);
  }

  private FilterInfo extractComparison(AST node) {
    if (node == null)
      return null;

    int type = node.getType();

    // General comparisons: >, <, >=, <=, =
    String op = switch (type) {
      case XQ.GeneralCompGT -> "gt";
      case XQ.GeneralCompLT -> "lt";
      case XQ.GeneralCompGE -> "ge";
      case XQ.GeneralCompLE -> "le";
      case XQ.GeneralCompEQ -> "eq";
      // Value comparisons
      case XQ.ValueCompGT -> "gt";
      case XQ.ValueCompLT -> "lt";
      case XQ.ValueCompGE -> "ge";
      case XQ.ValueCompLE -> "le";
      case XQ.ValueCompEQ -> "eq";
      default -> null;
    };

    if (op != null && node.getChildCount() >= 2) {
      // Left side should be a deref ($var.field), right side a literal
      String field = extractFieldFromDeref(node.getChild(0));
      Long value = extractIntegerLiteral(node.getChild(1));

      if (field != null && value != null) {
        return new FilterInfo(field, op, value);
      }

      // Try reversed: literal op $var.field
      field = extractFieldFromDeref(node.getChild(1));
      value = extractIntegerLiteral(node.getChild(0));
      if (field != null && value != null) {
        // Reverse the operator
        String reversedOp = switch (op) {
          case "gt" -> "lt";
          case "lt" -> "gt";
          case "ge" -> "le";
          case "le" -> "ge";
          default -> op;
        };
        return new FilterInfo(field, reversedOp, value);
      }
    }

    // Recurse into children (for AND/OR wrapping)
    for (int i = 0; i < node.getChildCount(); i++) {
      FilterInfo result = extractComparison(node.getChild(i));
      if (result != null)
        return result;
    }
    return null;
  }

  // ==================== OrderBy extraction ====================

  private record OrderInfo(String field, String direction) {
  }

  private OrderInfo extractOrderBy(AST orderBy) {
    // OrderBy children are OrderBySpec nodes
    if (orderBy.getChildCount() < 1)
      return null;
    AST spec = orderBy.getChild(0);
    if (spec.getType() != XQ.OrderBySpec)
      return null;

    String field = null;
    String direction = "ascending"; // default

    // OrderBySpec children: [expr, OrderByKind?, OrderByEmptyMode?]
    if (spec.getChildCount() >= 1) {
      field = extractFieldFromDeref(spec.getChild(0));
    }
    // Check for ascending/descending
    for (int i = 1; i < spec.getChildCount(); i++) {
      AST child = spec.getChild(i);
      if (child.getType() == XQ.OrderByKind) {
        Object val = child.getValue();
        if (val != null) {
          direction = val.toString().toLowerCase();
        }
      }
    }

    if (field != null) {
      return new OrderInfo(field, direction);
    }
    return null;
  }

  // ==================== Field name extraction ====================

  private String extractFieldFromDeref(AST node) {
    if (node == null)
      return null;

    if (node.getType() == XQ.DerefExpr) {
      if (node.getChildCount() >= 2) {
        AST fieldNode = node.getChild(node.getChildCount() - 1);
        Object value = fieldNode.getValue();
        if (value instanceof QNm qnm) {
          return qnm.getLocalName();
        }
        if (value instanceof String s) {
          return s;
        }
      }
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

    if (node.getType() == XQ.Int) {
      Object val = node.getValue();
      if (val instanceof Number n)
        return n.longValue();
      if (val instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }

    // Try numeric literal types
    if (node.getType() == XQ.Dbl || node.getType() == XQ.Dec) {
      Object val = node.getValue();
      if (val instanceof Number n)
        return n.longValue();
    }

    // Check value directly
    Object val = node.getValue();
    if (val instanceof Number n)
      return n.longValue();

    return null;
  }
}
