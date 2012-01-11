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
package org.brackit.xquery.compiler.optimizer.walker;

import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.PipeExpr;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;

/**
 * Extended variant of binding pushup with support for lifted sections.
 * 
 * This additional round of binding pushup is especially useful for
 * join detection.
 * 
 * Pushing bindings _in_ a lifted section has to take care of the 
 * the grouping semantics into account, i.e., we have to check if 
 * we do not push a variable binding out-of-scope. 
 * 
 * @author Sebastian Baechle
 * 
 */
public class BindingPushupAfterLifting extends BindingPushup {

	@Override
	protected boolean pushableAfterCount(AST binding, AST count) {
		// A pushup is OK when binding is let and
		// a) the count is not used for lifting checks, or		
		// b) the let-bound variable is not used outside the lifted part
		if (binding.getType() != LetBind) {
			return false;
		}
		//check case a)
		AST parent = count.getParent();
		if (parent.getProperty("check") == null) {
			return true;
		}
		// check case b)
		// check if the let-bound variable is used after grouping
		// with count variable
		QNm countVar = (QNm) count.getChild(1).getChild(0).getValue();
		while (true) {
			while ((parent = parent.getParent()).getType() != GroupBy);
			if ((parent.checkProperty("onlyLast"))
				&& (parent.getProperty("check").equals(countVar))) {
				break;
			}
		}
		while ((parent = parent.getParent()).getType() != PipeExpr) {
			if (dependsOn(parent, binding)) {
				return false;
			}
		}		
		return true;
	}

	@Override
	protected void push(AST node, AST newParent) {
		super.push(node, newParent);
		QNm check = (QNm) node.getProperty("check");
		QNm pCheck = (QNm) newParent.getProperty("check");
		if ((check == null) && (pCheck != null)) {
			node.setProperty("check", pCheck);
		}
	}	
}
