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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler.optimizer.walker.topdown;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;

/**
 * @author Sebastian Baechle
 * 
 */
public class JoinInnerLoopCache extends ScopeWalker {

	private int artificialGroupVarCount;

	@Override
	protected AST visit(AST join) {
		if (join.getType() != XQ.Join) {
			return join;
		}

		if (join.getProperty("group") != null) {
			return join;
		}

		VarRef varRefs = findVarRefs(join.getChild(2));
		if (varRefs == null) {
			// right join input is completely independent
			// -> great!
			return join;
		}

		// find out which is the latest-bound
		// variable that the right input branch depends on
		Scope[] sortScopes = sortScopes(varRefs);
		Scope rightInScope = findScope(join);
		for (int i = sortScopes.length - 1; i >= 0; i--) {
			if (sortScopes[i].compareTo(rightInScope) < 0) {
				// introduce count as join group marker
				QNm grpVarName = createGroupVarName();
				AST count = new AST(XQ.Count);
				AST runVarBinding = new AST(XQ.TypedVariableBinding);
				runVarBinding.addChild(new AST(XQ.Variable, grpVarName));
				count.addChild(runVarBinding);

				// add marker before copying the subtree
				join.setProperty("group", grpVarName);

				AST defScope = sortScopes[i].node;
				AST defScopeOut = defScope.getLastChild();
				count.addChild(defScopeOut.copyTree());

				defScope.replaceChild(defScope.getChildCount() - 1, count);
				snapshot();
				refreshScopes(defScope, true);
				break;
			}
		}

		return join;
	}

	private QNm createGroupVarName() {
		return new QNm("_group;" + (artificialGroupVarCount++));
	}
}
