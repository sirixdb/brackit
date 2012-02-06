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
package org.brackit.xquery.compiler.optimizer.walker.topdown;

import static org.brackit.xquery.compiler.XQ.Count;
import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.GroupBySpec;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.PipeExpr;
import static org.brackit.xquery.compiler.XQ.TypedVariableBinding;
import static org.brackit.xquery.compiler.XQ.Variable;
import static org.brackit.xquery.compiler.XQ.VariableRef;

import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;

/**
 * @author Sebastian Baechle
 * 
 */
public class LetBindToLeftJoin extends Walker {
	private int artificialGroupVarCount;

	@Override
	protected AST visit(AST node) {
		if (node.getType() != LetBind) {
			return node;
		}

		AST opEx = node.getChild(1);

		if (opEx.getType() != PipeExpr) {
			return node;
		}

		AST leftJoin = convertToLeftJoin(node);
		node.getParent().replaceChild(node.getChildIndex(), leftJoin);

		return leftJoin;
	}

	private AST convertToLeftJoin(AST node) {
		// create variable name for new group
		QNm grpVarName = createGroupVarName();

		// copy start of nested pipeline
		AST orig = node.getChild(1).getChild(0);
		AST copyRoot = orig.copy();
		AST copy = copyRoot;

		orig = orig.getLastChild();
		while (orig.getType() != XQ.End) {
			AST toAdd = orig.copy();
			for (int i = 0; i < orig.getChildCount() - 1; i++) {
				toAdd.addChild(orig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			orig = orig.getLastChild();
		}

		// let bind the return expression
		AST letBind = new AST(LetBind);
		letBind.addChild(node.getChild(0).copyTree());
		letBind.addChild(orig.getChild(0).copyTree());
		// group all (only the binding of the final let will be used)
		AST groupBy = new AST(GroupBy);
		AST groupBySpec = new AST(GroupBySpec);
		groupBySpec.addChild(new AST(VariableRef, grpVarName));
		groupBy.addChild(groupBySpec);
		groupBy.addChild(new AST(XQ.End));
		letBind.addChild(groupBy);
		// append let bind to copied pipeline
		copy.addChild(letBind);

		AST join = new AST(XQ.Join);
		join.setProperty("leftJoin", Boolean.TRUE);
		join.setProperty("group", grpVarName);
		AST start = new AST(XQ.Start);
		start.addChild(new AST(XQ.End));
		join.addChild(start);
		AST cmp = new AST(XQ.ComparisonExpr);
		cmp.addChild(new AST(XQ.ValueCompEQ));
		cmp.addChild(new AST(XQ.Bool, Bool.TRUE));
		cmp.addChild(new AST(XQ.Bool, Bool.TRUE));
		join.addChild(cmp);
		join.addChild(copyRoot);
		join.addChild(node.getChild(2));

		AST count = new AST(Count);
		AST runVarBinding = new AST(TypedVariableBinding);
		runVarBinding.addChild(new AST(Variable, grpVarName));
		count.addChild(runVarBinding);
		count.addChild(join);
		return count;
	}
	
	private QNm createGroupVarName() {
		return new QNm("_check;" + (artificialGroupVarCount++));
	}
}