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

import static org.brackit.xquery.compiler.parser.XQueryParser.ComparisonExpr;
import static org.brackit.xquery.compiler.parser.XQueryParser.Count;
import static org.brackit.xquery.compiler.parser.XQueryParser.ForBind;
import static org.brackit.xquery.compiler.parser.XQueryParser.GeneralCompGE;
import static org.brackit.xquery.compiler.parser.XQueryParser.GeneralCompGT;
import static org.brackit.xquery.compiler.parser.XQueryParser.GeneralCompLE;
import static org.brackit.xquery.compiler.parser.XQueryParser.GeneralCompLT;
import static org.brackit.xquery.compiler.parser.XQueryParser.GeneralCompNE;
import static org.brackit.xquery.compiler.parser.XQueryParser.Join;
import static org.brackit.xquery.compiler.parser.XQueryParser.LetBind;
import static org.brackit.xquery.compiler.parser.XQueryParser.NodeCompFollows;
import static org.brackit.xquery.compiler.parser.XQueryParser.NodeCompIs;
import static org.brackit.xquery.compiler.parser.XQueryParser.NodeCompPrecedes;
import static org.brackit.xquery.compiler.parser.XQueryParser.Selection;
import static org.brackit.xquery.compiler.parser.XQueryParser.Start;
import static org.brackit.xquery.compiler.parser.XQueryParser.TypedVariableBinding;
import static org.brackit.xquery.compiler.parser.XQueryParser.ValueCompGE;
import static org.brackit.xquery.compiler.parser.XQueryParser.ValueCompGT;
import static org.brackit.xquery.compiler.parser.XQueryParser.ValueCompLE;
import static org.brackit.xquery.compiler.parser.XQueryParser.ValueCompLT;
import static org.brackit.xquery.compiler.parser.XQueryParser.ValueCompNE;
import static org.brackit.xquery.compiler.parser.XQueryParser.Variable;
import static org.brackit.xquery.compiler.parser.XQueryParser.VariableRef;

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
public class JoinRewriter extends Walker {

	private static class VarRef {
		final String name;
		final int no;
		VarRef next;
		VarRef prev;

		VarRef(String name, int no) {
			this.name = name;
			this.no = no;
		}

		public VarRef last() {
			VarRef tmp = this;
			while (tmp.next != null) {
				tmp = tmp.next;
			}
			return tmp;
		}

		public VarRef first() {
			VarRef tmp = this;
			while (tmp.prev != null) {
				tmp = tmp.prev;
			}
			return tmp;
		}

		public VarRef prev() {
			return prev;
		}

		public VarRef next() {
			return next;
		}

		public VarRef insert(String name, int no) {
			VarRef c = this;
			while (c.no > no) {
				if (c.prev != null) {
					c = c.prev;
				} else {
					break;
				}
			}
			while (c.no < no) {
				if ((c.next != null) && (c.next.no < no)) {
					c = c.next;
				} else {
					break;
				}
			}
			if (c.no == no) {
				return c;
			}
			VarRef n = new VarRef(name, no);
			if (c.no < no) {
				n.next = c.next;
				c.next = n;
				n.prev = c;
				if (n.next != null) {
					n.next.prev = n;
				}
			} else {
				n.next = c;
				c.prev = n;
			}
			return n;
		}

		public String chain() {
			VarRef p = first();
			StringBuilder s = new StringBuilder();
			s.append(p);
			for (VarRef r = p.next(); r != null; r = r.next()) {
				s.append(" <-> " + r);
			}
			return s.toString();
		}

		public String toString() {
			return no + ":'" + name + "'";
		}
	}

	public static void main(String[] args) {
		for (int i = 1; i <= 4; i++) {
			int no = (i - 1) * 2 + 1;
			if (no > 1) {
				VarRef[] r = new VarRef[] { new VarRef("2", 2),
						new VarRef("4", 4), new VarRef("6", 6) };
				r[0].next = r[1];
				r[1].prev = r[0];
				r[1].next = r[2];
				r[2].prev = r[1];
				VarRef t = r[((no - 1) / 2) - 1];
				System.out.print("Insert " + no + " at " + t.no + ": ");
				System.out.println(t.insert(Integer.toString(no), no).chain());
			}
			if (no < 7) {
				VarRef[] r = new VarRef[] { new VarRef("2", 2),
						new VarRef("4", 4), new VarRef("6", 6) };
				r[0].next = r[1];
				r[1].prev = r[0];
				r[1].next = r[2];
				r[2].prev = r[1];
				VarRef t = r[((no + 1) / 2) - 1];
				System.out.print("Insert " + no + " at " + t.no + ": ");
				System.out.println(t.insert(Integer.toString(no), no).chain());
			}
		}
	}

	private int artificialGroupCountVarCount;

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
		if (s2VarRefs.last().no < s1VarRefs.last().no) {
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
			if (ref.no <= s1VarRefs.last().no) {
				break;
			}
			s2 = ref;
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
				int no = varNo(op.getChild(pos++).getChild(0).getValue());
				if (no <= s2.no) {
					atS2 = true;
				}
			} else if (op.getType() != Selection) {
				return select;
			}
			// collect var refs
			while (pos < op.getChildCount()) {
				s2VarRefs = varRefs(op.getChild(pos++), s2VarRefs);
			}
			// adjust S2
			for (VarRef ref = s2.prev(); ref != null; ref = ref.prev()) {
				if (ref.no <= s1VarRefs.last().no) {
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
			if ((s0 == null) || (ref.no > s0.no)) {
				s1 = ref;
				break;
			}
		}

		// check if S1 and S2 overlap
		if (s1 == null) {
			return select;
		}

		// divide pipeline in left and right input
		final AST rightIn = select.getChild(0).copyTree();
		AST tmp = rightIn;
		for (int i = 0; i < s2len - 1; i++) {
			tmp = rightIn.getChild(0);
		}
		final AST leftIn = tmp.getChild(0);
		// cut off left part from right
		tmp.replaceChild(0, new AST(Start, "Start"));

		// build join
		AST join = new AST(Join, "Join");
		AST condition = new AST(ComparisonExpr, "ComparisonExpr");
		condition.addChild(comparison.copy());
		condition.addChild(s1Expr.copyTree());
		condition.addChild(s2Expr.copyTree());
		join.addChild(leftIn);
		join.addChild(condition);
		join.addChild(rightIn);
		
		// Convert to left join 
		// when we are in a lifted part
		if (select.getProperty("check") != null) {
			join.setProperty("check", select.getProperty("check"));
			join.setProperty("leftJoin", "true");
		}

		select.getParent().replaceChild(select.getChildIndex(), join);

		if (s0 != null) {
			determineGroup(join, s0);
		}

		return select.getParent();
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
		while (tmp.getChildCount() > 0) {
			tmp = tmp.getChild(0);
			// TODO window clause
			if ((tmp.getType() == ForBind) || (tmp.getType() == LetBind)) {
				int declNumber = varNo(tmp.getChild(1).getChild(0)
						.getValue());
				if (declNumber <= s0.no) {
					join.setProperty("group", introduceCount(tmp));
					return;
				}
			}
		}
		// S2 is independent in this pipeline
		// -> everything is fine
	}

	private String introduceCount(AST in) {
		// introduce an artificial enumeration node for the grouping
		AST count = new AST(Count, "Count");
		String grpVarName = createGroupCountVarName();
		AST runVarBinding = new AST(TypedVariableBinding,
				"TypedVariableBinding");
		runVarBinding.addChild(new AST(Variable, grpVarName));
		count.addChild(in.copyTree());
		count.addChild(runVarBinding);
		in.getParent().replaceChild(0, count);
		return grpVarName;
	}

	private VarRef varRefs(AST node, VarRef refs) {
		if (node.getType() == VariableRef) {
			int no = varNo(node.getValue());
			if (no == -1) {
				return refs;
			}
			if (refs == null) {
				return new VarRef(node.getValue(), no);
			}
			return refs.insert(node.getValue(), no).first();
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			refs = varRefs(node.getChild(i++), refs);
		}
		return refs;
	}

	private static int varNo(String text) {
		int index = text.lastIndexOf(";");
		return (index != -1) ? Integer.parseInt(text.substring(index + 1)) : -1;
	}

	private String createGroupCountVarName() {
		return "_group;" + (artificialGroupCountVarCount++);
	}
}