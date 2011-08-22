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

import static org.brackit.xquery.compiler.XQ.Count;
import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.GroupBySpec;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.ReturnExpr;
import static org.brackit.xquery.compiler.XQ.Start;
import static org.brackit.xquery.compiler.XQ.TypedVariableBinding;
import static org.brackit.xquery.compiler.XQ.Variable;
import static org.brackit.xquery.compiler.XQ.VariableRef;

import org.brackit.xquery.compiler.AST;

/**
 * Lift a nested operator pipline, i.e., a ReturnExpr child of a LetBind. The
 * lifted pipeline is embraced by a count operator at the bottom and
 * an group by at the top to preserve the semantics of binding a whole sequence
 * to the let-bound variable instead instead of single items.
 * 
 * @author Sebastian Baechle
 * 
 */
public class LetBindLift extends Walker {
	private int artificialEnumerateVarCount;

	@Override
	protected AST visit(AST node) {
		if (node.getType() != LetBind) {
			return node;
		}

		AST opEx = node.getChild(2);

		if (opEx.getType() != ReturnExpr) {
			return node;
		}

		AST liftet = liftInput(node);
		node.getParent().replaceChild(node.getChildIndex(), liftet);

		return node.getParent();
	}

	/*
	 * Create the lifted input pipeline from the input branch of the operator
	 * expression
	 */
	private AST liftInput(AST node) {
		AST opEx = node.getChild(2);
		AST in = null;
		AST lifted = null;

		// create variable name for new group
		String grpVarName = createEnumerateVarName();
		
		// create lifted copy
		AST nested = opEx.getChild(0);
		for (AST tmp = nested; tmp.getType() != Start; tmp = tmp.getChild(0)) {
			AST toAdd = tmp.copy();
			for (int i = 1; i < tmp.getChildCount(); i++) {
				toAdd.addChild(tmp.getChild(i).copyTree());
			}
			toAdd.setProperty("check", grpVarName);
			if (in == null) {
				in = toAdd;
			} else {
				lifted.insertChild(0, toAdd);
			}
			lifted = toAdd;
		}

		// copy old input and add count operator for the grouping
		AST oldIn = node.getChild(0).copyTree();
		AST count = new AST(Count, "Count");		
		AST runVarBinding = new AST(TypedVariableBinding,
				"TypedVariableBinding");
		runVarBinding.addChild(new AST(Variable, grpVarName));		
		count.addChild(oldIn);
		count.addChild(runVarBinding);
		if (oldIn.getProperty("check") != null) {
			count.setProperty("check", oldIn.getProperty("check"));
		}
		
		// lifted part consumes old input
		lifted.insertChild(0, count);

		// let bind the return expression
		AST letBind = new AST(LetBind, "LetBind");
		letBind.addChild(in);
		letBind.addChild(node.getChild(1).copyTree());
		letBind.addChild(opEx.getChild(1).copyTree());
		letBind.setProperty("check", grpVarName);

		// group the let bind
		AST groupBy = new AST(GroupBy, "GroupBy");
		groupBy.addChild(letBind);
		AST groupBySpec = new AST(GroupBySpec, "GroupBySpec");
		groupBySpec.addChild(new AST(VariableRef, grpVarName));
		groupBy.addChild(groupBySpec);
		groupBy.setProperty("check", grpVarName);
		groupBy.setProperty("onlyLast", "true");
		return groupBy;
	}

	private String createEnumerateVarName() {
		return "_check;" + (artificialEnumerateVarCount++);
	}
}