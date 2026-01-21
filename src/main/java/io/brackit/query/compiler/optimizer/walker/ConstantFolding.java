/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.compiler.optimizer.walker;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Dec;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;

/**
 * Constant folding optimization pass that evaluates compile-time constant
 * expressions and replaces them with their computed literal values.
 *
 * <p>This walker folds:
 * <ul>
 * <li>Arithmetic expressions (+, -, *, /, div, idiv, mod)</li>
 * <li>Value comparisons (eq, ne, lt, le, gt, ge)</li>
 * <li>Boolean expressions (and, or) with short-circuit evaluation</li>
 * <li>If expressions with constant conditions (dead code elimination)</li>
 * <li>String concatenation (||)</li>
 * </ul>
 *
 * @author Brackit Project Team
 */
public class ConstantFolding extends Walker {

  private boolean changed;

  /**
   * Returns true if any constants were folded during the last walk.
   */
  public boolean hasChanged() {
    return changed;
  }

  /**
   * Resets the change flag before starting a new walk.
   */
  public void resetChanged() {
    changed = false;
  }

  /**
   * Walks the AST multiple times until no more changes are made.
   * This handles nested constant expressions like "a" || "b" || "c".
   */
  public static AST walkUntilStable(AST ast) {
    ConstantFolding folder = new ConstantFolding();
    AST result = ast;
    do {
      folder.resetChanged();
      result = folder.walk(result);
    } while (folder.hasChanged());
    return result;
  }

  @Override
  protected AST visit(AST node) {
    return switch (node.getType()) {
      case XQ.ArithmeticExpr -> foldArithmetic(node);
      case XQ.ComparisonExpr -> foldComparison(node);
      case XQ.AndExpr -> foldAnd(node);
      case XQ.OrExpr -> foldOr(node);
      case XQ.IfExpr -> foldIf(node);
      case XQ.StringConcatExpr -> foldStringConcat(node);
      default -> node;
    };
  }

  /**
   * Checks if the AST node represents a constant literal value.
   */
  private boolean isConstant(AST node) {
    int type = node.getType();
    return type == XQ.Int || type == XQ.Dbl || type == XQ.Dec || type == XQ.Str || type == XQ.Bool || type == XQ.Null;
  }

  /**
   * Checks if the AST node represents a numeric constant.
   */
  private boolean isNumericConstant(AST node) {
    int type = node.getType();
    return type == XQ.Int || type == XQ.Dbl || type == XQ.Dec;
  }

  /**
   * Checks if the AST node represents a boolean constant.
   */
  private boolean isBooleanConstant(AST node) {
    return node.getType() == XQ.Bool;
  }

  /**
   * Checks if the AST node represents a string constant.
   */
  private boolean isStringConstant(AST node) {
    return node.getType() == XQ.Str;
  }

  /**
   * Extracts the Numeric value from a constant node.
   */
  private Numeric getNumericValue(AST node) {
    Object value = node.getValue();
    if (value instanceof Numeric) {
      return (Numeric) value;
    }
    return null;
  }

  /**
   * Extracts the boolean value from a Bool constant node.
   */
  private Boolean getBooleanValue(AST node) {
    Object value = node.getValue();
    if (value instanceof Bool) {
      return ((Bool) value).bool;
    }
    return null;
  }

  /**
   * Extracts the string value from a Str constant node.
   */
  private String getStringValue(AST node) {
    Object value = node.getValue();
    if (value instanceof Str) {
      return ((Str) value).stringValue();
    }
    return null;
  }

  /**
   * Creates a literal AST node from an Atomic value.
   */
  private AST createLiteralNode(Atomic value) {
    if (value instanceof Int32 || value instanceof Int64 || value instanceof io.brackit.query.atomic.Int) {
      return new AST(XQ.Int, value);
    } else if (value instanceof Dbl) {
      return new AST(XQ.Dbl, value);
    } else if (value instanceof Dec) {
      return new AST(XQ.Dec, value);
    } else if (value instanceof Bool) {
      return new AST(XQ.Bool, value);
    } else if (value instanceof Str) {
      return new AST(XQ.Str, value);
    }
    return null;
  }

  /**
   * Replaces a node in its parent with a replacement node and returns
   * the replacement node for continued traversal.
   */
  private AST replaceNode(AST node, AST replacement) {
    AST parent = node.getParent();
    if (parent != null) {
      parent.replaceChild(node.getChildIndex(), replacement);
      changed = true;
      snapshot();
    }
    return replacement;
  }

  /**
   * Folds arithmetic expressions where both operands are constant.
   * Handles: +, -, *, /, div, idiv, mod
   */
  private AST foldArithmetic(AST node) {
    if (node.getChildCount() < 3) {
      return node;
    }

    AST opNode = node.getChild(0);
    AST left = node.getChild(1);
    AST right = node.getChild(2);

    if (!isNumericConstant(left) || !isNumericConstant(right)) {
      return node;
    }

    Numeric leftNum = getNumericValue(left);
    Numeric rightNum = getNumericValue(right);

    if (leftNum == null || rightNum == null) {
      return node;
    }

    try {
      Numeric result = switch (opNode.getType()) {
        case XQ.AddOp -> leftNum.add(rightNum);
        case XQ.SubtractOp -> leftNum.subtract(rightNum);
        case XQ.MultiplyOp -> leftNum.multiply(rightNum);
        case XQ.DivideOp -> leftNum.div(rightNum);
        case XQ.IDivideOp -> leftNum.idiv(rightNum);
        case XQ.ModulusOp -> leftNum.mod(rightNum);
        default -> null;
      };

      if (result != null) {
        AST replacement = createLiteralNode((Atomic) result);
        if (replacement != null) {
          return replaceNode(node, replacement);
        }
      }
    } catch (QueryException e) {
      // Preserve expression for runtime error (e.g., division by zero)
    }

    return node;
  }

  /**
   * Folds value comparison expressions where both operands are constant.
   * Handles: eq, ne, lt, le, gt, ge
   */
  private AST foldComparison(AST node) {
    if (node.getChildCount() < 3) {
      return node;
    }

    AST cmpNode = node.getChild(0);
    AST left = node.getChild(1);
    AST right = node.getChild(2);

    // Only fold value comparisons with constant operands
    if (!isConstant(left) || !isConstant(right)) {
      return node;
    }

    Object leftValue = left.getValue();
    Object rightValue = right.getValue();

    if (!(leftValue instanceof Atomic leftAtomic) || !(rightValue instanceof Atomic rightAtomic)) {
      return node;
    }

    try {
      Boolean result = switch (cmpNode.getType()) {
        case XQ.ValueCompEQ -> leftAtomic.cmp(rightAtomic) == 0;
        case XQ.ValueCompNE -> leftAtomic.cmp(rightAtomic) != 0;
        case XQ.ValueCompLT -> leftAtomic.cmp(rightAtomic) < 0;
        case XQ.ValueCompLE -> leftAtomic.cmp(rightAtomic) <= 0;
        case XQ.ValueCompGT -> leftAtomic.cmp(rightAtomic) > 0;
        case XQ.ValueCompGE -> leftAtomic.cmp(rightAtomic) >= 0;
        default -> null;
      };

      if (result != null) {
        AST replacement = new AST(XQ.Bool, result ? Bool.TRUE : Bool.FALSE);
        return replaceNode(node, replacement);
      }
    } catch (QueryException e) {
      // Preserve expression for runtime error (e.g., type mismatch)
    }

    return node;
  }

  /**
   * Folds AND expressions with constant operands.
   * Implements short-circuit evaluation:
   * - false and X -> false
   * - true and X -> X
   * - X and false -> false
   * - X and true -> X
   */
  private AST foldAnd(AST node) {
    if (node.getChildCount() < 2) {
      return node;
    }

    AST left = node.getChild(0);
    AST right = node.getChild(1);

    // Left operand is constant
    if (isBooleanConstant(left)) {
      Boolean leftVal = getBooleanValue(left);
      if (leftVal != null) {
        if (!leftVal) {
          // false and X -> false
          AST replacement = new AST(XQ.Bool, Bool.FALSE);
          return replaceNode(node, replacement);
        } else {
          // true and X -> X
          AST replacement = right.copyTree();
          return replaceNode(node, replacement);
        }
      }
    }

    // Right operand is constant
    if (isBooleanConstant(right)) {
      Boolean rightVal = getBooleanValue(right);
      if (rightVal != null) {
        if (!rightVal) {
          // X and false -> false
          AST replacement = new AST(XQ.Bool, Bool.FALSE);
          return replaceNode(node, replacement);
        } else {
          // X and true -> X
          AST replacement = left.copyTree();
          return replaceNode(node, replacement);
        }
      }
    }

    return node;
  }

  /**
   * Folds OR expressions with constant operands.
   * Implements short-circuit evaluation:
   * - true or X -> true
   * - false or X -> X
   * - X or true -> true
   * - X or false -> X
   */
  private AST foldOr(AST node) {
    if (node.getChildCount() < 2) {
      return node;
    }

    AST left = node.getChild(0);
    AST right = node.getChild(1);

    // Left operand is constant
    if (isBooleanConstant(left)) {
      Boolean leftVal = getBooleanValue(left);
      if (leftVal != null) {
        if (leftVal) {
          // true or X -> true
          AST replacement = new AST(XQ.Bool, Bool.TRUE);
          return replaceNode(node, replacement);
        } else {
          // false or X -> X
          AST replacement = right.copyTree();
          return replaceNode(node, replacement);
        }
      }
    }

    // Right operand is constant
    if (isBooleanConstant(right)) {
      Boolean rightVal = getBooleanValue(right);
      if (rightVal != null) {
        if (rightVal) {
          // X or true -> true
          AST replacement = new AST(XQ.Bool, Bool.TRUE);
          return replaceNode(node, replacement);
        } else {
          // X or false -> X
          AST replacement = left.copyTree();
          return replaceNode(node, replacement);
        }
      }
    }

    return node;
  }

  /**
   * Folds if expressions with constant conditions.
   * - if (true) then T else E -> T
   * - if (false) then T else E -> E
   */
  private AST foldIf(AST node) {
    if (node.getChildCount() < 3) {
      return node;
    }

    AST condition = node.getChild(0);
    AST thenBranch = node.getChild(1);
    AST elseBranch = node.getChild(2);

    if (isBooleanConstant(condition)) {
      Boolean condVal = getBooleanValue(condition);
      if (condVal != null) {
        AST replacement = condVal ? thenBranch.copyTree() : elseBranch.copyTree();
        return replaceNode(node, replacement);
      }
    }

    return node;
  }

  /**
   * Folds string concatenation expressions where all operands are constant.
   * "a" || "b" || "c" -> "abc"
   */
  private AST foldStringConcat(AST node) {
    int childCount = node.getChildCount();
    if (childCount < 2) {
      return node;
    }

    // Check if all children are string constants
    boolean allConstant = true;
    for (int i = 0; i < childCount; i++) {
      if (!isStringConstant(node.getChild(i))) {
        allConstant = false;
        break;
      }
    }

    if (allConstant) {
      // All children are constants, concatenate them all
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < childCount; i++) {
        String str = getStringValue(node.getChild(i));
        if (str == null) {
          return node;
        }
        sb.append(str);
      }
      AST replacement = new AST(XQ.Str, new Str(sb.toString()));
      return replaceNode(node, replacement);
    }

    return node;
  }
}
