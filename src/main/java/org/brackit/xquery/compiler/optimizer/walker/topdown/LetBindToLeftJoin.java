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

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.util.Cmp;

/**
 * @author Sebastian Baechle
 * 
 */
public class LetBindToLeftJoin extends ScopeWalker {

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

		AST top = bindingExpr.getChild(0).getChild(0);
		return convertToLeftJoin(node, top);
	}

	private AST convertToLeftJoin(AST let, AST top) {
		AST insertJoinAfter = let.getParent();
		while (insertJoinAfter.getType() != XQ.Start) {
			insertJoinAfter = insertJoinAfter.getParent();
		}

		VarRef varRefs = findVarRefs(top);
		if (varRefs != null) {
			// nested join input is not completely independent:
			// find out which is the latest-bound
			// variable that the right input branch depends on
			Scope[] sortScopes = sortScopes(varRefs);
			Scope rightInScope = findScope(let);
			for (int i = sortScopes.length - 1; i >= 0; i--) {
				Scope scope = sortScopes[i];
				if (scope.compareTo(rightInScope) < 0) {
					insertJoinAfter = scope.node;
					break;
				}
			}
		}

		AST lorig = let;
		AST lorigp;
		while ((lorigp = lorig.getParent()) != insertJoinAfter) {
			// ensure that we do not leave the current pipeline
			if (lorigp.getType() == XQ.Start) {
				insertJoinAfter = lorigp;
				break;
			}
			lorig = lorigp;
		}

		// start the left input with a count for the grouping
		AST leftIn = new AST(XQ.Start);
		QNm grpVarName = createGroupVarName();
		AST count = new AST(XQ.Count);
		AST runVarBinding = new AST(XQ.TypedVariableBinding);
		runVarBinding.addChild(new AST(XQ.Variable, grpVarName));
		count.addChild(runVarBinding);
		leftIn.addChild(count);
		AST copy = count;

		while (lorig != let) {
			AST toAdd = lorig.copy();
			for (int i = 0; i < lorig.getChildCount() - 1; i++) {
				toAdd.addChild(lorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			lorig = lorig.getLastChild();
		}

		// now append the final end with the
		// count variable as join key expression
		AST lend = new AST(XQ.End);
		lend.addChild(new AST(XQ.VariableRef, grpVarName));
		copy.addChild(lend);

		// copy the nested pipeline as right input
		AST oorig = top;
		AST rightIn = new AST(XQ.Start);
		copy = rightIn;

		while (oorig.getType() != XQ.End) {
			AST toAdd = oorig.copy();
			for (int i = 0; i < oorig.getChildCount() - 1; i++) {
				toAdd.addChild(oorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			oorig = oorig.getLastChild();
		}

		// convert the the nested return expression to a let bind
		// and concatenate it to the right input
		AST letVarBinding = let.getChild(0).copyTree();
		// TODO fix cardinality of binding if necessary
		AST rlet = new AST(LetBind);
		rlet.addChild(letVarBinding);
		rlet.addChild(oorig.getChild(0).copyTree());
		copy.addChild(rlet);

		// group the let-bound return values
		// (only the binding of the final let will be used)
		AST groupBy = new AST(GroupBy);
		AST groupBySpec = new AST(XQ.GroupBySpec);
		groupBySpec.addChild(new AST(XQ.VariableRef, grpVarName));
		groupBy.addChild(groupBySpec);
		rlet.addChild(groupBy);

		// use the grouped count variable as right join key
		AST end = new AST(XQ.End);
		end.addChild(new AST(XQ.VariableRef, grpVarName));
		groupBy.addChild(end);

		// finally assemble left join
		AST ljoin = new AST(XQ.Join);
		ljoin.setProperty("leftJoin", Boolean.TRUE);
		ljoin.addChild(leftIn);
		ljoin.setProperty("cmp", Cmp.eq);
		ljoin.setProperty("GCmp", false);
		ljoin.addChild(rightIn);
		ljoin.addChild(let.getLastChild().copyTree());

		int replaceAt = insertJoinAfter.getChildCount() - 1;
		insertJoinAfter.replaceChild(replaceAt, ljoin);
		snapshot();
		refreshScopes(insertJoinAfter, true);
		return insertJoinAfter;
	}

	private QNm createGroupVarName() {
		return new QNm("_check;" + (artificialGroupVarCount++));
	}
}