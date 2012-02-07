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

import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.PipeExpr;

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

		AST bindingExpr = node.getChild(1);

		if (bindingExpr.getType() != PipeExpr) {
			return node;
		}

		AST join = bindingExpr.getChild(0).getChild(0);
		if (join.getType() != XQ.Join) {
			join = convertJoin(join);
		}
		AST leftJoin = convertToLeftJoin(node, join);

		AST start = node.getParent();
		while (start.getType() != XQ.Start) {
			start = start.getParent();
		}
		start.replaceChild(0, leftJoin);

		return leftJoin;
	}

	private AST convertJoin(AST node) {
		AST leftIn = new AST(XQ.Start);
		leftIn.addChild(new AST(XQ.End));

		// copy start of right (nested) input pipeline
		AST rorig = node;
		AST rightIn = new AST(XQ.Start);
		AST copy = rightIn;

		while (rorig.getType() != XQ.End) {
			AST toAdd = rorig.copy();
			for (int i = 0; i < rorig.getChildCount() - 1; i++) {
				toAdd.addChild(rorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			rorig = rorig.getLastChild();
		}
		copy.addChild(new AST(XQ.End));

		AST join = new AST(XQ.Join);
		join.addChild(leftIn);
		AST cmp = new AST(XQ.ComparisonExpr);
		cmp.addChild(new AST(XQ.ValueCompEQ));
		cmp.addChild(new AST(XQ.Bool, Bool.TRUE));
		cmp.addChild(new AST(XQ.Bool, Bool.TRUE));
		join.addChild(cmp);
		join.addChild(rightIn);
		join.addChild(rorig.copyTree());

		return join;
	}

	private AST convertToLeftJoin(AST node, AST join) {
		// copy upper-level input as left input
		AST lorig = node.getParent();
		while (lorig.getType() != XQ.Start) {
			lorig = lorig.getParent();
		}

		AST leftIn = lorig.copy();
		AST copy = leftIn;
		lorig = lorig.getLastChild();
		while (lorig != node) {
			AST toAdd = lorig.copy();
			for (int i = 0; i < lorig.getChildCount() - 1; i++) {
				toAdd.addChild(lorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			lorig = lorig.getLastChild();
		}

		// introduce a count for the grouping
		QNm grpVarName = createGroupVarName();
		AST count = new AST(XQ.Count);
		AST runVarBinding = new AST(XQ.TypedVariableBinding);
		runVarBinding.addChild(new AST(XQ.Variable, grpVarName));
		count.addChild(runVarBinding);
		count.addChild(new AST(XQ.End));
		copy.addChild(count);

		// copy right input of nested join as right input
		AST rorig = join.getChild(2).getChild(0);
		AST rightIn = new AST(XQ.Start);
		copy = rightIn;

		while (rorig.getType() != XQ.End) {
			AST toAdd = rorig.copy();
			for (int i = 0; i < rorig.getChildCount() - 1; i++) {
				toAdd.addChild(rorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			rorig = rorig.getLastChild();
		}
		copy.addChild(new AST(XQ.End));

		// assemble left join
		AST ljoin = new AST(XQ.Join);
		ljoin.setProperty("leftJoin", Boolean.TRUE);
		ljoin.addChild(leftIn);
		ljoin.addChild(join.getChild(1).copyTree());
		ljoin.addChild(rightIn);

		// copy output of join
		AST oorig = join.getChild(3);
		copy = ljoin;

		while (oorig.getType() != XQ.End) {
			AST toAdd = oorig.copy();
			for (int i = 0; i < oorig.getChildCount() - 1; i++) {
				toAdd.addChild(oorig.getChild(i).copyTree());
			}
			copy = toAdd;
			oorig = oorig.getLastChild();
		}

		// append let to bind the return expression
		AST letBind = new AST(LetBind);
		letBind.addChild(node.getChild(0).copyTree());
		letBind.addChild(oorig.getChild(0).copyTree());

		// group all let-bound return values 
		// (only the binding of the final let will be used)
		AST groupBy = new AST(GroupBy);
		AST groupBySpec = new AST(XQ.GroupBySpec);
		groupBySpec.addChild(new AST(XQ.VariableRef, grpVarName));
		groupBy.addChild(groupBySpec);
		letBind.addChild(groupBy);

		// continue with the normal after the grouping
		groupBy.addChild(node.getChild(2).copyTree());

		// concatenate let bind and trailing to join output
		copy.addChild(letBind);

		return ljoin;
	}

	private QNm createGroupVarName() {
		return new QNm("_check;" + (artificialGroupVarCount++));
	}
}