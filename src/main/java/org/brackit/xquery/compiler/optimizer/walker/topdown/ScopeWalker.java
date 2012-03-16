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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		walkInspect(root, true, false);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount++));
		}
		return super.prepare(root);
	}

	protected final void refreshScopes(AST node, boolean pipeline) {
		Scope s = findScope(node);
		// A "start scope" of a join must not be refreshed alone
		if ((s.node.getType() == XQ.Start)
				&& (s.node.getParent().getType() == XQ.Join)) {
			s = s.parent;
		}
		table.reset(s);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount)
							+ "reset");
		}
		walkInspect(s.node, false, false);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(table.rootScope.dot(), XQuery.DEBUG_DIR,
					getClass().getSimpleName() + "_scopes_" + (dotCount++));
		}
	}

	protected Set<AST> getScopes() {
		return table.getScopes();
	}

	private final void walkInspect(AST node, boolean newScope, boolean bindOnly) {
		if (inspect(node, newScope, bindOnly)) {
			for (int i = 0; i < node.getChildCount(); i++) {
				AST child = node.getChild(i);
				walkInspect(child, newScope, bindOnly);
			}
		}
	}

	private boolean inspect(AST node, boolean newScope, boolean bindOnly) {
		switch (node.getType()) {
		case XQ.TypeSwitch:
			typeswitchExpr(node);
			return false;
		case XQ.QuantifiedExpr:
			quantifiedExpr(node);
			return false;
		case XQ.TransformExpr:
			transformExpr(node);
			return false;
		case XQ.TryCatchExpr:
			tryCatchExpr(node);
			return false;
		case XQ.FilterExpr:
			filterExpr(node);
			return false;
		case XQ.PathExpr:
			pathExpr(node);
			return false;
		case XQ.StepExpr:
			stepExpr(node);
			return false;
		case XQ.CompDocumentConstructor:
			documentExpr(node);
			return false;
		case XQ.DirElementConstructor:
		case XQ.CompElementConstructor:
			elementExpr(node);
			return false;
		case XQ.Start:
			start(node, newScope, bindOnly);
			return false;
		case XQ.ForBind:
			forBind(node, newScope, bindOnly);
			return false;
		case XQ.LetBind:
			letBind(node, newScope, bindOnly);
			return false;
		case XQ.Selection:
			select(node, newScope, bindOnly);
			return false;
		case XQ.OrderBy:
			orderBy(node, newScope, bindOnly);
			return false;
		case XQ.Join:
			join(node, newScope, bindOnly);
			return false;
		case XQ.GroupBy:
			groupBy(node, newScope, bindOnly);
			return false;
		case XQ.Count:
			count(node, newScope, bindOnly);
			return false;
		case XQ.End:
			end(node, newScope, bindOnly);
			return false;
		default:
			return true;
		}
	}

	protected final Scope findScope(AST node) {
		AST tmp = node;
		do {
			Scope s = table.scopemap.get(tmp);
			if (s != null) {
				return s;
			}
		} while ((tmp = tmp.getParent()) != null);
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

		protected boolean resolveLocal(QNm var) {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				if (n.var.atomicCmp(var) == 0) {
					return true;
				}
			}
			return false;
		}

		protected Scope resolve(QNm var) {
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

		public void promoteToParent() {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				parent.bind(n.var);
			}
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

		void reset(Scope s) {
			for (Scope cs = s.firstChild; cs != null; cs = cs.next) {
				remove(cs);
			}
			scope = s;
			s.lvars = null;
		}

		protected void remove(Scope s) {
			scopemap.remove(s.node);
			for (Scope cs = s.firstChild; cs != null; cs = cs.next) {
				remove(cs);
			}
			s.lvars = null;
			s.firstChild = null;
			Scope ps = s.parent.firstChild;
			if (ps == s) {
				s.parent.firstChild = s.next;
			} else {
				while (ps.next != s) {
					ps = ps.next;
				}
				ps.next = s.next;
			}
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
			for (Scope cs = scope.firstChild; cs != null; cs = cs.next) {
				remove(cs);
			}
			scopemap.remove(scope);
			scope.parent.dropLastChild();
			scope = scope.parent;
		}

		void resolve(QNm var) {
			scope.resolve(var);
		}

		List<QNm> inScopeBindings() {
			return scope.localBindings();
		}

		Set<AST> getScopes() {
			return scopemap.keySet();
		}

		void promoteBindings() {
			scope.promoteToParent();
		}
	}

	private void forBind(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// get names of binding variable and optional position variable
		QNm forVar = (QNm) node.getChild(0).getChild(0).getValue();
		QNm posVar = null;
		AST posBindingOrSourceExpr = node.getChild(1);
		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVar = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
			posBindingOrSourceExpr = node.getChild(2);
		}

		if (!bindOnly) {
			// visit binding expression
			walkInspect(posBindingOrSourceExpr, true, false);
		}

		// bind variables
		table.bind(forVar);
		if (posVar != null) {
			table.bind(posVar);
		}

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void letBind(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// get name of binding variable
		QNm letVar = (QNm) node.getChild(0).getChild(0).getValue();

		if (!bindOnly) {
			// visit binding expression
			walkInspect(node.getChild(1), true, false);
		}

		// bind variable
		table.bind(letVar);

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void select(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		if (!bindOnly) {
			// visit predicate expression
			walkInspect(node.getChild(0), true, false);
		}

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void groupBy(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// group by does not declare variables

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
			table.closeScope();
		}
	}

	private void orderBy(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// visit order by expressions
		int orderBySpecCount = node.getChildCount() - 1;
		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i);
			walkInspect(orderBy.getChild(0), true, false);
		}

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void count(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// get name of binding variable
		QNm countVar = (QNm) node.getChild(0).getChild(0).getValue();

		// bind variable
		table.bind(countVar);

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void start(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void end(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		if (!bindOnly) {
			if (node.getChildCount() != 0) {
				walkInspect(node.getLastChild(), true, bindOnly);
			}
		}
		if (newScope) {
			table.closeScope();
		}
	}

	private void join(AST node, boolean newScope, boolean bindOnly) {
		if (newScope) {
			table.openScope(node, true);
		}

		// collect all bindings provided by left and right join branch
		List<QNm> leftInBinding = getPipelineBindings(node.getChild(0));
		List<QNm> rightInBinding = getPipelineBindings(node.getChild(1));
		List<QNm> postBinding = getPipelineBindings(node.getChild(2));

		if (!bindOnly) {
			// visit left input
			walkInspect(node.getChild(0), true, false);
			// visit right input
			walkInspect(node.getChild(1), true, false);
		}

		if (!bindOnly) {
			// start nested scope for post join
			// and "bind" variables of join outputs
			AST postStart = node.getChild(2);
			table.openScope(postStart, true);
			for (QNm var : leftInBinding) {
				table.bind(var);
			}
			for (QNm var : rightInBinding) {
				table.bind(var);
			}
			// visit post
			walkInspect(postStart.getChild(0), true, bindOnly);
			table.closeScope();

			// start nested scope for output
			AST outStart = node.getChild(3);
			table.openScope(outStart, true);
			for (QNm var : leftInBinding) {
				table.bind(var);
			}
			for (QNm var : rightInBinding) {
				table.bind(var);
			}
			for (QNm var : postBinding) {
				table.bind(var);
			}
			walkInspect(outStart.getChild(0), true, bindOnly);
			table.closeScope();
			table.closeScope();
		} else {
			// "pretend" to bind variables from both
			// left and right input and output
			// Note: A name collision will not
			// bind a variable twice
			// in this joined scope twice!
			for (QNm var : leftInBinding) {
				table.bind(var);
			}
			for (QNm var : rightInBinding) {
				table.bind(var);
			}
			for (QNm var : postBinding) {
				table.bind(var);
			}
		}
	}

	private List<QNm> getPipelineBindings(AST input) {
		table.openScope(input, true);

		for (AST node = input; node.getType() != XQ.End; node = node
				.getLastChild()) {
			switch (node.getType()) {
			case XQ.Start:
				break;
			case XQ.ForBind:
				forBind(node, false, true);
				break;
			case XQ.LetBind:
				letBind(node, false, true);
				break;
			case XQ.Selection:
				select(node, false, true);
				break;
			case XQ.OrderBy:
				orderBy(node, false, true);
				break;
			case XQ.Join:
				join(node, false, true);
				break;
			case XQ.GroupBy:
				groupBy(node, false, true);
				break;
			case XQ.Count:
				count(node, false, true);
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
		walkInspect(expr.getChild(0), true, false);
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
		walkInspect(clause.getChild(clause.getChildCount() - 1), true, false);
		table.closeScope();
	}

	private void quantifiedExpr(AST expr) {
		table.openScope(expr, false);
		// child 0 is quantifier type
		for (int i = 1; i < expr.getChildCount() - 1; i++) {
			table.openScope(expr.getChild(i), false);
			walkInspect(expr.getChild(i).getChild(1), true, false);
			QNm name = (QNm) expr.getChild(i).getChild(0).getChild(0)
					.getValue();
			table.bind(name);
			// trick: promote local bindings
			table.promoteBindings();
			table.closeScope();
		}
		table.closeScope();
	}

	private void transformExpr(AST expr) {
		table.openScope(expr, false);
		int pos = 0;
		while (pos < expr.getChildCount() - 2) {
			AST binding = expr.getChild(pos++);
			table.openScope(binding, false);
			QNm name = (QNm) binding.getChild(0).getValue();
			table.bind(name);
			walkInspect(binding.getChild(1), true, false);
			// trick: promote local bindings
			table.promoteBindings();
			table.closeScope();
		}
		AST modify = expr.getChild(pos++);
		walkInspect(modify, true, false);
		walkInspect(expr.getChild(pos), true, false);
		table.closeScope();
	}

	private void tryCatchExpr(AST expr) {
		walkInspect(expr.getChild(0), true, false);
		table.openScope(expr, false);
		for (int i = 1; i < 7; i++) {
			table.bind((QNm) expr.getChild(i).getChild(0).getValue());
		}
		for (int i = 7; i < expr.getChildCount(); i++) {
			// child i,0 is catch error list
			walkInspect(expr.getChild(i).getChild(0), true, false);
		}
		table.closeScope();
	}

	private void filterExpr(AST expr) {
		walkInspect(expr.getChild(0), true, false);
		for (int i = 1; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			table.bind(Namespaces.FS_DOT);
			table.bind(Namespaces.FS_POSITION);
			table.bind(Namespaces.FS_LAST);
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
	}

	private void stepExpr(AST expr) {
		walkInspect(expr.getChild(0), true, false);
		walkInspect(expr.getChild(1), true, false);
		for (int i = 2; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			table.bind(Namespaces.FS_DOT);
			table.bind(Namespaces.FS_POSITION);
			table.bind(Namespaces.FS_LAST);
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
	}

	private void pathExpr(AST expr) {
		table.openScope(expr, false);
		for (int i = 0; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			if (i > 0) {
				table.bind(Namespaces.FS_DOT);
				table.bind(Namespaces.FS_POSITION);
				table.bind(Namespaces.FS_LAST);
			}
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
		table.closeScope();
	}

	private void documentExpr(AST node) {
		table.openScope(node, false);
		table.bind(Namespaces.FS_PARENT);
		walkInspect(node.getChild(0), true, false);
		table.closeScope();
	}

	private void elementExpr(AST node) {
		int pos = 0;
		while (node.getChild(pos).getType() == XQ.NamespaceDeclaration) {
			pos++;
		}
		walkInspect(node.getChild(pos++), true, false);
		table.openScope(node, false);
		table.bind(Namespaces.FS_PARENT);
		// visit content sequence
		walkInspect(node.getChild(pos++), true, false);
		table.closeScope();
	}

	/*
	 * create a sorted and duplicate-free array of accessed scopes
	 */
	protected Scope[] sortScopes(VarRef varRefs) {
		if (varRefs == null) {
			return new Scope[0];
		}
		int cnt = 0;
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			cnt++;
		}
		int pos = 0;
		Scope[] tmp = new Scope[cnt];
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			tmp[pos++] = ref.referredScope;
		}
		Arrays.sort(tmp);
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
