/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 */
package io.brackit.query.compiler.optimizer.walker.topdown;

import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;
import io.brackit.query.compiler.optimizer.Stage;
import io.brackit.query.compiler.optimizer.VectorizedScanAnnotation;
import io.brackit.query.module.StaticContext;

/**
 * Optimizer stage that detects eligible group-by patterns and annotates the
 * AST for vectorized execution.
 * <p>
 * Detected pattern:
 * <pre>
 * PipeExpr
 * Start
 * ForBind [source = $$[] or collection(...)[][]]
 * LetBind [source = $var.FIELD]
 * GroupBy [spec = $letVar]
 * End [return = ...]
 * </pre>
 * <p>
 * When detected, sets {@link VectorizedScanAnnotation#VECTORIZED_GROUPBY} and
 * {@link VectorizedScanAnnotation#GROUPBY_FIELD} on the PipeExpr node. The
 * translator then delegates to the registered {@code VectorizedExecutor}.
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

    // Look for PipeExpr nodes
    if (node.getType() == XQ.PipeExpr) {
      tryAnnotate(node);
    }

    // Recurse into children
    for (int i = 0; i < node.getChildCount(); i++) {
      walkAndAnnotate(node.getChild(i));
    }
  }

  /**
   * Try to match the pattern: Start → ForBind → LetBind → GroupBy → End
   * and extract the group-by field name.
   */
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

    // ForBind → next (could be LetBind or GroupBy)
    AST next = forBind.getLastChild();

    // Optional LetBind(s) before GroupBy
    String groupField = null;
    while (next != null && next.getType() == XQ.LetBind) {
      // Try to extract field name from the let binding source
      // LetBind children: [TypedVariableBinding, SourceExpr, NextOp]
      if (next.getChildCount() >= 2) {
        String field = extractFieldFromDeref(next.getChild(1));
        if (field != null) {
          groupField = field;
        }
      }
      next = next.getLastChild();
    }

    // Must have GroupBy
    if (next == null || next.getType() != XQ.GroupBy)
      return;
    if (groupField == null)
      return;

    // Must end with End (the return clause)
    AST afterGroupBy = next.getLastChild();
    if (afterGroupBy == null || afterGroupBy.getType() != XQ.End)
      return;

    // Pattern matched! Annotate the PipeExpr
    pipeExpr.setProperty(VectorizedScanAnnotation.VECTORIZED_GROUPBY, Boolean.TRUE);
    pipeExpr.setProperty(VectorizedScanAnnotation.GROUPBY_FIELD, groupField);
  }

  /**
   * Extract a field name from a deref expression like $var.city
   */
  private String extractFieldFromDeref(AST node) {
    if (node == null)
      return null;

    // Direct deref: $var.field
    if (node.getType() == XQ.DerefExpr) {
      if (node.getChildCount() >= 2) {
        AST fieldNode = node.getChild(node.getChildCount() - 1);
        if (fieldNode.getValue() instanceof io.brackit.query.atomic.QNm qnm) {
          return qnm.getLocalName();
        }
        if (fieldNode.getValue() instanceof String s) {
          return s;
        }
      }
    }

    // Recurse into children
    for (int i = 0; i < node.getChildCount(); i++) {
      String result = extractFieldFromDeref(node.getChild(i));
      if (result != null)
        return result;
    }
    return null;
  }
}
