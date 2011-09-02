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

import static org.brackit.xquery.compiler.XQ.ComparisonExpr;
import static org.brackit.xquery.compiler.XQ.Count;
import static org.brackit.xquery.compiler.XQ.ForBind;
import static org.brackit.xquery.compiler.XQ.GeneralCompGE;
import static org.brackit.xquery.compiler.XQ.GeneralCompGT;
import static org.brackit.xquery.compiler.XQ.GeneralCompLE;
import static org.brackit.xquery.compiler.XQ.GeneralCompLT;
import static org.brackit.xquery.compiler.XQ.GeneralCompNE;
import static org.brackit.xquery.compiler.XQ.Join;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.NodeCompFollows;
import static org.brackit.xquery.compiler.XQ.NodeCompIs;
import static org.brackit.xquery.compiler.XQ.NodeCompPrecedes;
import static org.brackit.xquery.compiler.XQ.Selection;
import static org.brackit.xquery.compiler.XQ.Start;
import static org.brackit.xquery.compiler.XQ.TypedVariableBinding;
import static org.brackit.xquery.compiler.XQ.ValueCompGE;
import static org.brackit.xquery.compiler.XQ.ValueCompGT;
import static org.brackit.xquery.compiler.XQ.ValueCompLE;
import static org.brackit.xquery.compiler.XQ.ValueCompLT;
import static org.brackit.xquery.compiler.XQ.ValueCompNE;
import static org.brackit.xquery.compiler.XQ.Variable;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;

/**
 * Convert select operators with comparison predicates and operands with
 * non-overlapping variable scopes.
 * 
 * Lifted selects are converted to left joins and the grouping specification is
 * expanded as far as possible downwards the left input.
 * 
 * @author Sebastian Baechle
 * 
 */
public class JoinRewriter extends PipelineVarTracker {

	private int artificialGroupCountVarCount;

	@Override
	protected AST prepare(AST root) {
		collectVars(root);
		return root;
	}

	protected AST visit(AST select) {
		if (select.getType() != Selection) {
			return select;
		}
		AST predicate = select.getChild(1);

		if (predicate.getType() != ComparisonExpr) {
			return select;
		}
		AST comparison = predicate.getChild(0);

		switch (comparison.getType()) {
		case NodeCompFollows:
		case NodeCompIs:
		case NodeCompPrecedes:
		case GeneralCompNE:
		case ValueCompNE:
			return select;
		}

		// left side must be static
		AST s1Expr = predicate.getChild(1);
		VarRef s1VarRefs = varRefs(s1Expr, null);
		if (s1VarRefs == null) {
			return select;
		}
		// right side must not be static
		AST s2Expr = predicate.getChild(2);
		VarRef s2VarRefs = varRefs(s2Expr, null);
		if (s2VarRefs == null) {
			return select;
		}

		// ensure that left side of comparison
		// corresponds always to the left branch of the join
		if (s2VarRefs.last().var.bndNo < s1VarRefs.last().var.bndNo) {
			// swap left and right in comparison
			AST tmpAst = s1Expr;
			s1Expr = s2Expr;
			s2Expr = tmpAst;
			VarRef tmpMinVarRef = s1VarRefs;
			s1VarRefs = s2VarRefs;
			s2VarRefs = tmpMinVarRef;
			comparison = swapCmp(comparison);
		}

		// set S2 to the lowest var ref of the right
		// expression that is larger than the greatest of the left
		VarRef s2 = null;
		for (VarRef ref = s2VarRefs.last(); ref != null; ref = ref.prev()) {
			if (ref.var.bndNo <= s1VarRefs.last().var.bndNo) {
				break;
			}
			s2 = ref;
		}

		// left and right refer to same max variable
		if (s2 == null) {
			return select;
		}

		// check operators in S2 and collect var refs
		AST op = select;
		int s2len = 0;
		boolean atS2 = false;
		while (!atS2) {
			op = op.getChild(0);
			s2len++;
			int pos = 1;
			// TODO window clause
			if ((op.getType() == ForBind) || (op.getType() == LetBind)) {
				// TODO move check to super class
				int no = bindNo((QNm) op.getChild(pos++).getChild(0).getValue());
				if (no <= s2.var.no) {
					atS2 = true;
				}
			} else if ((op.getType() != Selection) && (op.getType() != Join)) {
				return select;
			}
			// collect var refs
			while (pos < op.getChildCount()) {
				s2VarRefs = varRefs(op.getChild(pos++), s2VarRefs);
			}
			// adjust S2
			for (VarRef ref = s2.prev(); ref != null; ref = ref.prev()) {
				if (ref.var.bndNo <= s1VarRefs.last().var.bndNo) {
					break;
				}
				s2 = ref;
				atS2 = false;
			}
		}

		VarRef s0 = s2.prev();

		// S1 starts with the lowest var ref of the left
		// expression that is larger than S0
		VarRef s1 = null;
		for (VarRef ref = s1VarRefs; ref != null; ref = ref.next()) {
			if ((s0 == null) || (ref.var.bndNo > s0.var.bndNo)) {
				s1 = ref;
				break;
			}
		}

		// check if S1 and S2 overlap
		if (s1 == null) {
			return select;
		}

		// divide pipeline in left and right input
		AST rightIn = select.getChild(0).copyTree();
		AST tmp = rightIn;
		for (int i = 0; i < s2len - 1; i++) {
			tmp = tmp.getChild(0);
		}
		AST leftIn = tmp.getChild(0);
		// cut off left part from right
		tmp.replaceChild(0, new AST(Start, "Start"));

		// upstream selects which access S2 but not S1
		// can be included in the right input
		AST parent = select.getParent();
		AST child = select;
		while ((parent.getType() == Selection) && (filtersS2(parent, s2, s1))) {
			AST include = parent.copy();
			include.addChild(rightIn);
			include.addChild(parent.getChild(1).copyTree());
			rightIn = include;
			child = parent;
			parent = parent.getParent();
		}

		// build join
		AST join = new AST(Join, "Join");
		AST condition = new AST(ComparisonExpr, "ComparisonExpr");
		condition.addChild(comparison.copy());
		condition.addChild(s1Expr.copyTree());
		condition.addChild(s2Expr.copyTree());
		join.addChild(leftIn);
		join.addChild(condition);
		join.addChild(rightIn);

		QNm check = (QNm) select.getProperty("check");
		if (check != null) {
			// convert to left join
			// when we are in a lifted part
			join.setProperty("check", check);
			join.setProperty("leftJoin", Boolean.TRUE);
			// remove checks in right branch
			for (AST tmp2 = rightIn; tmp2.getType() != Start; tmp2 = tmp2
					.getChild(0)) {
				QNm check2 = (QNm) tmp2.getProperty("check");
				if (check.equals(check2)) {
					op.delProperty("check");
				}
			}
		}

		// check grouping condition i.e. binding
		// of max max in S0
		if (s0 != null) {
			determineGroup(join, s0);
		}

		parent.replaceChild(child.getChildIndex(), join);

		return parent;
	}

	private boolean filtersS2(AST select, VarRef s2, VarRef s1) {
		boolean accessVarInS2 = false;
		boolean accessVarInS1 = false;
		VarRef refs = varRefs(select.getChild(1), null);
		for (VarRef ref = refs; ref != null; ref = ref.next) {
			if (ref.var.bndNo >= s2.var.bndNo) {
				accessVarInS2 = true;
			} else if (ref.var.bndNo >= s1.var.bndNo) {
				accessVarInS1 = true;
			}
		}
		return accessVarInS2 && !accessVarInS1;
	}

	private AST swapCmp(AST comparison) {
		switch (comparison.getType()) {
		case GeneralCompGE:
			comparison = new AST(GeneralCompLE, "GeneralCompLE");
			break;
		case GeneralCompGT:
			comparison = new AST(GeneralCompLT, "GeneralCompLT");
			break;
		case GeneralCompLE:
			comparison = new AST(GeneralCompGE, "GeneralCompGE");
			break;
		case GeneralCompLT:
			comparison = new AST(GeneralCompGT, "GeneralCompGT");
			break;
		case ValueCompGE:
			comparison = new AST(ValueCompLE, "ValueCompLE");
			break;
		case ValueCompGT:
			comparison = new AST(ValueCompLT, "ValueCompLT");
			break;
		case ValueCompLE:
			comparison = new AST(ValueCompGE, "ValueCompGE");
			break;
		case ValueCompLT:
			comparison = new AST(ValueCompGT, "ValueCompGT");
			break;
		}
		return comparison;
	}

	private void determineGroup(AST join, VarRef s0) {
		// find iteration group of max free ref
		AST tmp = join;
		while (tmp.getType() != Start) {
			tmp = tmp.getChild(0);
			// TODO window clause
			if ((tmp.getType() == ForBind) || (tmp.getType() == LetBind)) {
				int bindingNo = bindNo((QNm) tmp.getChild(1).getChild(0)
						.getValue());
				if (bindingNo <= s0.var.bndNo) {
					while (tmp.getType() == LetBind) {
						tmp = tmp.getChild(0);
					}
					if (tmp.getType() != Start) {
						join.setProperty("group", introduceCount(tmp));
					}
					return;
				}
			}
		}
		// S2 is independent in this pipeline
		// -> everything is fine
	}

	private QNm introduceCount(AST in) {
		// introduce an artificial enumeration node for the grouping
		AST count = new AST(Count, "Count");
		QNm grpVarName = createGroupCountVarName();
		AST runVarBinding = new AST(TypedVariableBinding,
				"TypedVariableBinding");
		runVarBinding.addChild(new AST(Variable, grpVarName));
		count.addChild(in.copyTree());
		count.addChild(runVarBinding);
		in.getParent().replaceChild(0, count);
		return grpVarName;
	}

	protected int bindNo(QNm name) {
		Var var = findVar(name);
		return (var != null) ? var.bndNo : -1;
	}

	private QNm createGroupCountVarName() {
		return new QNm("_group;" + (artificialGroupCountVarCount++));
	}
}