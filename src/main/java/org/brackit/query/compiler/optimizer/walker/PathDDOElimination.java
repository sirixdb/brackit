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
package org.brackit.query.compiler.optimizer.walker;

import org.brackit.query.QueryException;
import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.AST;
import org.brackit.query.compiler.XQ;
import org.brackit.query.module.StaticContext;
import org.brackit.query.jdm.Function;

/**
 * <p>
 * This walker infers safety properties of path expressions, to reduce the need
 * for sorting and deduplicating the result of individual path steps.
 * </p>
 * <p>
 * In general, such inference rules are very complex, but this this class
 * is rather conservative and only checks for the frequent and more obvious cases.
 * </p>
 * <p>
 * For detailed theoretical information about this issue, checkout the paper of
 * Mary Fernández , Jan Hidders, Jérôme Siméon, and Roel Vercammen:
 * <em>Optimizing sorting and duplicate elimination in
 * XQuery path expressions</em>
 * </p>
 *
 * @author Sebastian Baechle
 */
public class PathDDOElimination extends Walker {

  public PathDDOElimination(StaticContext sctx) {
    super(sctx);
  }

  @Override
  protected AST visit(AST node) {
    if (node.getType() != XQ.PathExpr) {
      return node;
    }

    int stepCount = node.getChildCount();
    AST step = node.getChild(1);

    // special check for leading E1/E2
    if (isForwardStep(step)) {
      boolean isDescOrDescOSStep = isDescOrDescOSStep(step);
      boolean isLastStep = (stepCount == 2);
      if (!isDescOrDescOSStep || isLastStep) {
        AST firstStep = node.getChild(0);
        if (isAtomicOrEmpty(firstStep)) {
          step.setProperty("skipDDO", Boolean.TRUE);
        } else {
          step.setProperty("checkInput", Boolean.TRUE);
        }
      }
      if (isDescOrDescOSStep) {
        // be conservative:
        // stop trying to skip DDO after a '//'
        return node;
      }
    } else if (isBackwardStep(step)) {

    } else {

    }

    for (int i = 2; i < stepCount; i++) {
      step = node.getChild(i);
      if (isForwardStep(step)) {
        boolean isDescOrDescOSStep = isDescOrDescOSStep(step);
        boolean isLastStep = (i + 1 == stepCount);
        if ((!isDescOrDescOSStep) || (isLastStep)) {
          step.setProperty("skipDDO", Boolean.TRUE);
        }
        if (isDescOrDescOSStep) {
          // be conservative:
          // stop trying to skip DDO after a '//'
          return node;
        }
      } else if (isBackwardStep(step)) {

      } else {

      }
    }
    return node;
  }

  /**
   * Try to infer if this path step returns only a single item or the empty
   * sequence
   */
  private boolean isAtomicOrEmpty(AST step) {
    // TODO
    // Life would be great if we already had static typing...
    if (step.getType() == XQ.ContextItemExpr) {
      return true;
    }
    if (step.getType() == XQ.FunctionCall) {
      int childCount = step.getChildCount();
      QNm name = (QNm) step.getValue();
      Function fun = sctx.getFunctions().resolve(name, childCount);
      return fun.getSignature().getResultType().getCardinality().atMostOne();
    }
    if (step.getType() == XQ.VariableRef) {
      // TODO check if if we can derive information
      // about this variable (e.g., if for-bound)
    }
    return false;
  }

  protected boolean sortAfterStep(AST node, int position, int lastPosition) throws QueryException {
    AST child = node.getChild(position);

    if (child.getType() != XQ.StepExpr) {
      return true;
    }

    int axis = getAxis(child);
    if ((axis == XQ.CHILD) || (axis == XQ.ATTRIBUTE) || (axis == XQ.SELF)) {
      return true;
    }
    return false;
  }

  private boolean isForwardStep(AST step) {
    return ((step.getType() == XQ.StepExpr) && isForwardAxis(getAxis(step)));
  }

  private boolean isBackwardStep(AST step) {
    return ((step.getType() == XQ.StepExpr) && !isForwardAxis(getAxis(step)));
  }

  private boolean isDescOrDescOSStep(AST step) {
    if (step.getType() != XQ.StepExpr) {
      return false;
    }
    int axis = getAxis(step);
    return ((axis == XQ.DESCENDANT) || (axis == XQ.DESCENDANT_OR_SELF));
  }

  private boolean isForwardAxis(int axis) {
    return ((axis == XQ.CHILD) || (axis == XQ.DESCENDANT) || (axis == XQ.ATTRIBUTE) || (axis == XQ.SELF) || (axis
        == XQ.DESCENDANT_OR_SELF) || (axis == XQ.FOLLOWING_SIBLING) || (axis == XQ.FOLLOWING));
  }

  private boolean isReverseAxis(int axis) {
    return !isForwardAxis(axis);
  }

  private int getAxis(AST stepExpr) {
    return stepExpr.getChild(0).getChild(0).getType();
  }
}
