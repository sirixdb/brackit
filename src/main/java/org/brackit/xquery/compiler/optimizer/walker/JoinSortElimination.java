/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler.optimizer.walker;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.module.Functions;

/**
 * Eliminates sorting in left joins when re-grouped items of the right branch,
 * i.e., the inner loop is accessed only by aggregation functions or functions
 * which do not depend on the proper order (currently only fn:count and
 * fn:distinct).
 * 
 * First, we check if the "run variable" from the right branch is grouped in a
 * group by above the join. Then we follow the operator path upwards and inspect
 * all variable references to the grouped variable.
 * 
 * @author Sebastian Baechle
 * 
 */
public class JoinSortElimination extends Walker {

	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.Join) {
			return node;
		}

		if (!node.checkProperty("leftJoin")) {
			return node;
		}

		AST letBind = node.getParent();
		if ((letBind.getType() != XQ.LetBind)) {
			return node;
		}
		AST groupBy = letBind.getParent();
		if ((groupBy.getType() != XQ.GroupBy)
				|| (!groupBy.checkProperty("onlyLast"))) {
			return node;
		}

		// check if grouped let bind variable is only used in uncritical
		// aggregation functions (e.g., count):
		// collect all references to grouped variable and check them
		QNm grpVarName = (QNm) letBind.getChild(1).getChild(0).getValue();
		AST parent = groupBy;
		while ((parent = parent.getParent()) != null) {
			for (int i = 1; i < parent.getChildCount(); i++) {
				if (checkForCriticalReference(parent.getChild(i), grpVarName)) {
					return node;
				}
			}
			if (parent.getType() == XQ.PipeExpr) {
				break;
			}
		}

		// no critical references found -> no need to sort
		node.setProperty("skipSort", Boolean.TRUE);
		return node;
	}

	private boolean checkForCriticalReference(AST node,
			QNm groupingSequenceVarName) {
		if ((node.getType() == XQ.VariableRef)
				&& (node.getValue().equals(groupingSequenceVarName))) {
			AST parentExpr = node.getParent();
			if ((parentExpr.getType() != XQ.FunctionCall)
					|| ((!((QNm) parentExpr.getValue())
							.equals(Functions.FN_COUNT) && (!((QNm) parentExpr
							.getValue()).equals(Functions.FN_DISTINCT))))) {
				return true;
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			if (checkForCriticalReference(node.getChild(i),
					groupingSequenceVarName)) {
				return true;
			}
		}
		return false;
	}

	private QNm innerLoopVarName(AST node) {
		AST rightIn = node.getChild(node.getChildCount() - 1);
		while (rightIn.getChild(0).getType() != XQ.Start) {
			rightIn = rightIn.getChild(0);
		}
		// right in is now let-bind or for-bind
		return (QNm) rightIn.getChild(1).getChild(0).getValue();
	}
}
