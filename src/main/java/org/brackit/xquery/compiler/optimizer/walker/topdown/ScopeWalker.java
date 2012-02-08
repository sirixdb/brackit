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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.util.dot.DotContext;
import org.brackit.xquery.util.dot.DotNode;
import org.brackit.xquery.util.dot.DotUtil;

/**
 * @author Sebastian Baechle
 * 
 */
public abstract class ScopeWalker extends Walker {

	private BindingTable table;

	private int dotCount;

	@Override
	protected AST prepare(AST root) {
		table = new BindingTable(root);
		walkInspect(root);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount++));
		}
		return super.prepare(root);
	}

	protected final void refreshScopes(AST node, boolean pipeline) {
		Scope s = findScope(node);
		table.resetTo(s);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount)
							+ "reset");
		}
		if (pipeline) {
			AST out = node.getLastChild();
			if (out.getType() != XQ.End) {
				inspectPipeline(out);
			}
		} else {
			walkInspect(s.node);
		}
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount++));
		}
	}

	protected final void walkInspect(AST node) {
		if (inspect(node)) {
			for (int i = 0; i < node.getChildCount(); i++) {
				AST child = node.getChild(i);
				walkInspect(child);
			}
		}
	}

	private boolean inspect(AST node) {
		if (node.getType() == XQ.PipeExpr) {
			inspectPipeline(node.getChild(0));
			// don't visit this subtree
			return false;
		} else if (node.getType() == XQ.TypeSwitch) {
			typeswitchExpr(node);
			return false;
		} else if (node.getType() == XQ.QuantifiedExpr) {
			quantifiedExpr(node);
			return false;
		} else if (node.getType() == XQ.TransformExpr) {
			transformExpr(node);
			return false;
		} else if (node.getType() == XQ.TryCatchExpr) {
			tryCatchExpr(node);
			return false;
		} else if (node.getType() == XQ.FilterExpr) {
			filterExpr(node);
			return false;
		} else if (node.getType() == XQ.PathExpr) {
			pathExpr(node);
			return false;
		} else if (node.getType() == XQ.CompDocumentConstructor) {
			documentExpr(node);
			return false;
		} else if ((node.getType() == XQ.DirElementConstructor)
				|| (node.getType() == XQ.CompElementConstructor)) {
			elementExpr(node);
			return false;
		} else {
			return true;
		}
	}

	private void inspectPipeline(AST node) {
		inspectPipeline(node, null);
	}

	/*
	 * Walk pipeline up and check for each operator, which bindings of its
	 * output will be used by upstream operators. All unused output bindings can
	 * be projected. Note, we assume in general that operators do never produce
	 * a binding which is not used. Nevertheless, this mechanism still admits
	 * that later at compilation an operator creates superfluous bindings.
	 */
	private void inspectPipeline(AST node, AST useAsReturn) {
		table.openScope(node, true);
		switch (node.getType()) {
		case XQ.Start:
			break;
		case XQ.ForBind:
			forBind(node);
			break;
		case XQ.LetBind:
			letBind(node);
			break;
		case XQ.Selection:
			select(node);
			break;
		case XQ.OrderBy:
			orderBy(node);
			break;
		case XQ.Join:
			// joins are non-linear
			// and must be handled as
			// special cases
			join(node);
			break;
		case XQ.GroupBy:
			groupBy(node);
			break;
		case XQ.Count:
			count(node);
			break;
		default:
			throw new RuntimeException("Illegal pipeline node "
					+ node.getStringValue());
		}

		AST out = node.getLastChild();
		if (out.getType() == XQ.End) {
			// visit local or provided return expression
			if ((useAsReturn == null) && (out.getChildCount() != 0)) {
				walkInspect(out.getChild(0));
			} else {
				walkInspect(useAsReturn);
			}
		} else {
			// visit output to check if it references
			// any variables of this operator
			inspectPipeline(out, useAsReturn);
		}
		table.closeScope();
	}

	protected final Scope findScope(AST node) {
		do {
			Scope s = table.scopemap.get(node);
			if (s != null) {
				return s;
			}
		} while ((node = node.getParent()) != null);
		return table.rootScope;
	}

	protected final VarRef findVarRefs(AST node) {
		if (node.getType() == XQ.VariableRef) {
			QNm var = (QNm) node.getValue();
			Scope s = findScope(node);
			Scope rs = s.resolve(var);
			if (rs == null) {
				System.out.println("Did not find " + var + " in any scope");
				return null;
			}
			return new VarRef(var, node, s, rs);
		}
		VarRef varRefs = null;
		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			VarRef tmp = findVarRefs(child);
			if (tmp != null) {
				if (varRefs == null) {
					varRefs = tmp;
				} else {
					VarRef p = varRefs;
					while (p.next != null) {
						p = p.next;
					}
					p.next = tmp;
				}
			}
		}
		return varRefs;
	}

	protected static class VarRef {
		final QNm var;
		final AST ref;
		final Scope refScope;
		final Scope referredScope;
		VarRef next;

		public VarRef(QNm var, AST ref, Scope refScope, Scope referredScope) {
			this.var = var;
			this.ref = ref;
			this.refScope = refScope;
			this.referredScope = referredScope;
		}

		public String toString() {
			return var.toString();
		}
	}

	protected static class Scope implements Comparable<Scope> {
		private static class Node {
			final QNm var;
			Node next;

			Node(QNm var, Node next) {
				this.var = var;
				this.next = next;
			}

			public String toString() {
				return var.toString();
			}
		}

		final boolean inPipeline;
		final AST node;
		final Scope parent;
		final int division;
		Scope firstChild;
		Scope next;
		Node lvars;

		private Scope(AST node, Scope parent, boolean inPipeline, int division) {
			this.node = node;
			this.parent = parent;
			this.inPipeline = inPipeline;
			this.division = division;
		}
		
		protected boolean isInPipeline() {
			return inPipeline;
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(numberString());
			s.append(":");
			s.append((node != null) ? node.toString() : "");
			s.append("[");
			for (Node n = lvars; n != null; n = n.next) {
				s.append(n);
				if (n.next != null) {
					s.append(" ; ");
				}
			}
			s.append("]");
			return s.toString();
		}

		private void bind(QNm var) {
			Scope.Node p = null;
			Scope.Node n = lvars;
			while (n != null) {
				if (n.var.atomicCmp(var) == 0) {
					System.out.println("Var " + var + "bound twice. OK?");
					return;
				}
				p = n;
				n = n.next;
			}
			if (p == null) {
				lvars = new Scope.Node(var, null);
			} else {
				p.next = new Scope.Node(var, null);
			}
		}

		private Scope resolve(QNm var) {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				if (n.var.atomicCmp(var) == 0) {
					return this;
				}
			}
			return (parent != null) ? (parent.resolve(var)) : null;
		}

		private Scope open(AST node, boolean inPipeline) {
			Scope s;
			if (firstChild == null) {
				firstChild = (s = new Scope(node, this, inPipeline, 1));
			} else {
				Scope p = firstChild;
				while (p.next != null) {
					p = p.next;
				}
				p.next = (s = new Scope(node, this, inPipeline, p.division + 1));
			}
			return s;
		}

		public List<QNm> localBindings() {
			if (lvars == null) {
				return Collections.EMPTY_LIST;
			}
			ArrayList<QNm> bindings = new ArrayList<QNm>();
			for (Scope.Node n = lvars; n != null; n = n.next) {
				bindings.add(n.var);
			}
			return bindings;
		}

		@Override
		public int compareTo(Scope other) {
			if (other == this) {
				return 0;
			}
			Scope cp = this;
			while (cp != null) {
				Scope lca = other;
				while (lca != null) {
					if (lca == cp) {
						// found least common ancestor
						// case 0: node == this is not allowed
						// case 1: lca is this
						if (lca == this)
							return -1;
						// case 2: lca is node
						if (lca == other)
							return 1;
						// case 3: c and lcap have the same parent
						// nodes must be in the same chain -> search
						// scan from context node to n
						if (division != other.division) {
							return division < other.division ? -1 : 1;
						}
					}
					lca = lca.parent;
				}
				cp = cp.parent;
			}
			return -1;
		}

		public String numberString() {
			String s = String.valueOf(division);
			Scope scope = this;
			while (scope.parent != null) {
				scope = scope.parent;
				s = scope.division + "." + s;
			}
			return s;
		}

		public String dot() {
			DotContext dt = new DotContext();
			toDot(0, dt);
			return dt.toDotString();
		}

		public void dot(File file) {
			DotContext dt = new DotContext();
			toDot(0, dt);
			dt.write(file);
		}

		private int toDot(int no, DotContext dt) {
			final int myNo = no++;
			String label = (node != null) ? node.toString() : "";
			DotNode node = dt.addNode(String.valueOf(myNo));
			node.addRow(label, null);
			node.addRow(numberString(), null);
			int i = 0;
			for (Scope.Node var = lvars; var != null; var = var.next) {
				node.addRow(String.valueOf(i++), var.var.toString());
			}
			for (Scope child = firstChild; child != null; child = child.next) {
				dt.addEdge(String.valueOf(myNo), String.valueOf(no));
				no = child.toDot(no, dt);
			}
			return no++;
		}

		public void display() {
			try {
				File file = File.createTempFile("scope", ".dot");
				file.deleteOnExit();
				dot(file);
				Runtime.getRuntime()
						.exec(new String[] { "/usr/bin/dotty",
								file.getAbsolutePath() }).waitFor();
				file.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		void dropLastChild() {
			if (firstChild == null) {
				return;
			}
			Scope p = null;
			Scope n = firstChild;
			while (n.next != null) {
				p = n;
				n = n.next;
			}
			if (p == null) {
				firstChild = null;
			} else {
				p.next = null;
			}
			
		}
	}

	protected static class BindingTable {
		final Map<AST, Scope> scopemap = new HashMap<AST, Scope>();
		final Scope rootScope;
		Scope scope;

		BindingTable(AST root) {
			rootScope = new Scope(root, null, false, 1);
			scope = rootScope;
		}

		public void resetTo(Scope s) {
			for (Scope cs = s.firstChild; cs != null; cs = cs.next) {
				scopemap.remove(cs.node);
				resetTo(cs);
			}
			s.lvars = null;
			s.firstChild = null;
			scope = s;
		}

		void bind(QNm var) {
			scope.bind(var);
		}

		void openScope(AST node, boolean inPipeline) {
			scope = scope.open(node, inPipeline);
			scopemap.put(node, scope);
		}

		void closeScope() {
			scope = scope.parent;
		}
		
		void dropScope() {
			scope.parent.dropLastChild();
			scopemap.remove(scope.node);
			scope = scope.parent;
		}

		void resolve(QNm var) {
			scope.resolve(var);
		}

		public List<QNm> inScopeBindings() {
			return scope.localBindings();
		}
	}

	private void forBind(AST node) {
		// get names of binding variable and optional position variable
		QNm forVar = (QNm) node.getChild(0).getChild(0).getValue();
		QNm posVar = null;
		AST posBindingOrSourceExpr = node.getChild(1);
		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVar = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
			posBindingOrSourceExpr = node.getChild(2);
		}

		// visit binding expression
		walkInspect(posBindingOrSourceExpr);

		// bind variables
		table.bind(forVar);
		if (posVar != null) {
			table.bind(posVar);
		}
	}

	private void letBind(AST node) {
		// get name of binding variable
		QNm letVar = (QNm) node.getChild(0).getChild(0).getValue();

		// visit binding expression
		walkInspect(node.getChild(1));

		// bind variable
		table.bind(letVar);
	}

	private void select(AST node) {
		// visit predicate expression
		walkInspect(node.getChild(0));
	}

	private void groupBy(AST node) {
		// group by does not declare variables
	}

	private void orderBy(AST node) {
		// visit order by expressions
		int orderBySpecCount = node.getChildCount() - 1;
		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i);
			walkInspect(orderBy.getChild(0));
		}
	}

	private void count(AST node) {
		// get name of binding variable
		QNm countVar = (QNm) node.getChild(0).getChild(0).getValue();

		// bind variable
		table.bind(countVar);
	}

	private void join(AST node) {
		// visit left input and use left join expression as "return"
		inspectPipeline(node.getChild(0), node.getChild(1).getChild(1));
		// visit right input and use right join expression as "return"
		inspectPipeline(node.getChild(2), node.getChild(1).getChild(2));

		// collect all bindings provided by left and right join branch
		List<QNm> leftInBinding = inspectJoinPipeline(node.getChild(0));
		List<QNm> rightInBinding = inspectJoinPipeline(node.getChild(2));
		
		// "pretend" to bind variables from both left and right input
		for (QNm var : leftInBinding) {
			table.bind(var);
		}

		// Note: A name collision will not 
		// bind a variable twice
		// in this joined scope twice! 
		for (QNm var : rightInBinding) {
			table.bind(var);
		}
	}

	private List<QNm> inspectJoinPipeline(AST input) {
		table.openScope(input, true);

		for (AST node = input; node.getType() != XQ.End; node = node
				.getLastChild()) {
			switch (node.getType()) {
			case XQ.Start:
				break;
			case XQ.ForBind:
				forBind(node);
				break;
			case XQ.LetBind:
				letBind(node);
				break;
			case XQ.Selection:
				select(node);
				break;
			case XQ.OrderBy:
				orderBy(node);
				break;
			case XQ.Join:
				join(node);
				break;
			case XQ.GroupBy:
				groupBy(node);
				break;
			case XQ.Count:
				count(node);
				break;
			default:
				throw new RuntimeException();
			}
		}

		List<QNm> bindings = table.inScopeBindings();
		table.dropScope();
		return bindings;
	}

	private void typeswitchExpr(AST expr) {
		walkInspect(expr.getChild(0));
		table.openScope(expr, false);
		for (int i = 1; i < expr.getChildCount(); i++) {
			// handle default case as case clause
			caseClause(expr.getChild(i));
		}
		table.closeScope();
	}

	private void caseClause(AST clause) {
		table.openScope(clause, false);
		AST varOrType = clause.getChild(0);
		if (varOrType.getType() == XQ.Variable) {
			QNm name = (QNm) varOrType.getValue();
			table.bind(name);
		}
		// skip intermediate nodes reflecting sequence types....
		walkInspect(clause.getChild(clause.getChildCount() - 1));
		table.closeScope();
	}

	private void quantifiedExpr(AST expr) {
		int scopeCount = 0;
		// child 0 is quantifier type
		for (int i = 1; i < expr.getChildCount() - 1; i += 2) {
			table.openScope(expr.getChild(i), false);
			QNm name = (QNm) expr.getChild(i).getChild(0).getValue();
			table.bind(name);
			walkInspect(expr.getChild(i + 1));
			scopeCount++;
		}
		walkInspect(expr.getChild(expr.getChildCount() - 1));
		for (int i = 0; i <= scopeCount; i++) {
			table.closeScope();
		}
	}

	private void transformExpr(AST expr) {
		table.openScope(expr, false);
		int pos = 0;
		while (pos < expr.getChildCount() - 2) {
			AST binding = expr.getChild(pos++);
			QNm name = (QNm) binding.getChild(0).getValue();
			table.bind(name);
			walkInspect(binding.getChild(1));
		}
		AST modify = expr.getChild(pos++);
		walkInspect(modify);
		walkInspect(expr.getChild(pos));
		table.closeScope();
	}

	private void tryCatchExpr(AST expr) {
		walkInspect(expr.getChild(0));
		table.openScope(expr, false);
		table.bind(Namespaces.ERR_CODE);
		table.bind(Namespaces.ERR_DESCRIPTION);
		table.bind(Namespaces.ERR_VALUE);
		table.bind(Namespaces.ERR_MODULE);
		table.bind(Namespaces.ERR_LINE_NUMBER);
		table.bind(Namespaces.ERR_COLUMN_NUMBER);
		for (int i = 7; i < expr.getChildCount(); i++) {
			// child i,0 is catch error list
			walkInspect(expr.getChild(i).getChild(0));
		}
		table.closeScope();
	}

	private void filterExpr(AST expr) {
		walkInspect(expr.getChild(0));
		for (int i = 1; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			table.bind(Namespaces.FS_DOT);
			table.bind(Namespaces.FS_POSITION);
			table.bind(Namespaces.FS_LAST);
			walkInspect(expr.getChild(i));
			table.closeScope();
		}
	}

	private void pathExpr(AST expr) {
		for (int i = 0; i < expr.getChildCount(); i++) {
			walkInspect(expr.getChild(i));
			table.openScope(expr.getChild(i), false);
		}
		for (int i = 0; i < expr.getChildCount(); i++) {
			table.closeScope();
		}
	}

	private void documentExpr(AST node) {
		table.openScope(node, false);
		table.bind(Namespaces.FS_PARENT);
		walkInspect(node.getChild(0));
		table.closeScope();
	}

	private void elementExpr(AST node) {
		int pos = 0;
		while (node.getChild(pos).getType() == XQ.NamespaceDeclaration) {
			pos++;
		}
		walkInspect(node.getChild(pos++));
		table.openScope(node, false);
		table.bind(Namespaces.FS_PARENT);
		// visit content sequence
		walkInspect(node.getChild(pos++));
		table.closeScope();
	}
	
	/*
	 * create a sorted and duplicate-free array of accessed scopes
	 */
	protected Scope[] sortScopes(VarRef varRefs) {
		int cnt = 0;
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			cnt++;
		}
		int pos = 0;
		Scope[] tmp = new Scope[cnt];
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			tmp[pos++] = ref.referredScope;
		}
		pos = 0;
		Scope p = tmp[pos++];
		for (int i = 1; i < cnt; i++) {
			Scope s = tmp[i];
			if (p.compareTo(s) != 0) {
				tmp[pos++] = s;
			}
		}
		return Arrays.copyOfRange(tmp, 0, pos);
	}

	public static void main(String[] args) throws Exception {
		String query = "for $X in 'foo' for $a in (1,2,3) "
				+ "let $b := $a "
				+ "let $c := $a "
				+ "let $d := $b "
				+ "let $e := if ($c) then (for $x in position() return $x[position()]) else () "
				+ "let $f := ($b,$c,$d,$e) " + "return $f";
		new XQuery(query).serialize(new QueryContext(), System.out);
	}
}
