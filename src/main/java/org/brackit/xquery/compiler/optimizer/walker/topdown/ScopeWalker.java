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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.util.dot.DotContext;
import org.brackit.xquery.util.dot.DotNode;
import org.brackit.xquery.util.dot.DotUtil;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.DocumentType;
import org.brackit.xquery.xdm.type.ElementType;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 * 
 */
public abstract class ScopeWalker extends Walker {

	protected static SequenceType ONE_INTEGER = new SequenceType(
			AtomicType.INR, Cardinality.One);

	protected static SequenceType ONE_ITEM = new SequenceType(AnyItemType.ANY,
			Cardinality.One);

	protected static SequenceType ITEMS = new SequenceType(AnyItemType.ANY,
			Cardinality.ZeroOrMany);

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
		case XQ.PipeExpr:
			pipeExpr(node);
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
	
	private void pipeExpr(AST node) {
		table.openScope(node, false);
		walkInspect(node.getChild(0), true, false);
		table.closeScope();
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
		return findVarRefs(null, node);
	}

	protected final VarRef findVarRefs(Var toVar, AST node) {
		if (node.getType() == XQ.VariableRef) {
			QNm name = (QNm) node.getValue();
			if ((toVar != null) && (name.atomicCmp(toVar.var) != 0)) {
				return null;
			}
			Scope s = findScope(node);
			Var var = s.resolve(name);
			if (var == null) {
				// var ref to declared variable or function parameter
				// if not, it's a bug!
				// System.out.println("Did not find " + var + " in any scope");
				return null;
			}
			return new VarRef(var, node, s);
		}
		VarRef varRefs = null;
		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			VarRef tmp = findVarRefs(toVar, child);
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

	protected static class Var {
		final Scope scope;
		final QNm var;
		final SequenceType type;

		Var(Scope scope, QNm var, SequenceType type) {
			this.scope = scope;
			this.var = var;
			this.type = type;
		}

		public String toString() {
			return var.toString();
		}
	}

	protected static class VarRef {
		final Var var;
		final AST ref;
		final Scope refScope;
		VarRef next;

		public VarRef(Var var, AST ref, Scope refScope) {
			this.var = var;
			this.ref = ref;
			this.refScope = refScope;
		}

		public String toString() {
			return var.toString();
		}
	}

	protected static class Scope implements Comparable<Scope> {
		private static class Node extends Var {
			Node next;

			Node(Scope scope, QNm var, SequenceType type, Node next) {
				super(scope, var, type);
				this.next = next;
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

		private void bind(QNm var, SequenceType type) {
			Scope.Node p = null;
			Scope.Node n = lvars;
			while (n != null) {
				if (n.var.atomicCmp(var) == 0) {
					return;
				}
				p = n;
				n = n.next;
			}
			if (p == null) {
				lvars = new Scope.Node(this, var, type, null);
			} else {
				p.next = new Scope.Node(this, var, type, null);
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

		protected Var resolve(QNm var) {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				if (n.var.atomicCmp(var) == 0) {
					return n;
				}
			}
			return (parent != null) ? (parent.resolve(var)) : null;
		}

		protected Var get(QNm var) {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				if (n.var.atomicCmp(var) == 0) {
					return n;
				}
			}
			return null;
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

		public List<Var> localBindings() {
			if (lvars == null) {
				return Collections.EMPTY_LIST;
			}
			ArrayList<Var> bindings = new ArrayList<Var>();
			for (Scope.Node n = lvars; n != null; n = n.next) {
				bindings.add(n);
			}
			return bindings;
		}

		public void promoteToParent() {
			for (Scope.Node n = lvars; n != null; n = n.next) {
				parent.bind(n.var, n.type);
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

		void bind(QNm var, SequenceType type) {
			scope.bind(var, type);
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

		List<Var> inScopeBindings() {
			return scope.localBindings();
		}

		List<Var> inPipelineBindings() {
			ArrayList<Var> bindings = new ArrayList<Var>();
			Scope s = scope;
			while (s.inPipeline) {
				bindings.addAll(s.localBindings());
				s = s.parent;
			}
			return bindings;
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
		table.bind(forVar, ONE_ITEM);
		if (posVar != null) {
			table.bind(posVar, ONE_INTEGER);
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
		table.bind(letVar, ITEMS);

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

		int pos = 0;
		Set<QNm> groupVars = new HashSet<QNm>();
		while (node.getChild(pos).getType() == XQ.GroupBySpec) {
			AST varRef = node.getChild(pos).getChild(0);
			if (!bindOnly) {
				walkInspect(varRef, true, bindOnly);
			}
			groupVars.add((QNm) varRef.getValue());
			pos++;
		}
		// groupby rebinds all non-grouped pipeline variables
		for (Var var : table.inPipelineBindings()) {
			if (!groupVars.contains(var.var)) {
				table.bind(var.var, new SequenceType(var.type.getItemType(),
						Cardinality.ZeroOrMany));
			}
		}
		// bind additional aggregation specs
		while (node.getChild(pos).getType() == XQ.AggregateSpec) {
			AST aggSpec = node.getChild(pos);
			for (int i = 1; i < aggSpec.getChildCount(); i++) {
				AST aggBnd = aggSpec.getChild(i);
				AST typedVar = aggBnd.getChild(0);
				QNm aggVar = (QNm) typedVar.getChild(0).getValue();
				SequenceType aggVarType = ONE_ITEM;
				table.bind(aggVar, aggVarType);
			}
			pos++;
		}

		if (!bindOnly) {
			walkInspect(node.getLastChild(), true, bindOnly);
		}
		if (newScope) {
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
		table.bind(countVar, ONE_INTEGER);

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
		List<Var> leftInBinding = getPipelineBindings(node.getChild(0));
		List<Var> rightInBinding = getPipelineBindings(node.getChild(1));
		List<Var> postBinding = getPipelineBindings(node.getChild(2));

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
			for (Var var : leftInBinding) {
				table.bind(var.var, var.type);
			}
			for (Var var : rightInBinding) {
				table.bind(var.var, var.type);
			}
			// visit post
			walkInspect(postStart.getChild(0), true, bindOnly);
			table.closeScope();

			// start nested scope for output
			AST outStart = node.getChild(3);
			table.openScope(outStart, true);
			for (Var var : leftInBinding) {
				table.bind(var.var, var.type);
			}
			for (Var var : rightInBinding) {
				table.bind(var.var, var.type);
			}
			for (Var var : postBinding) {
				table.bind(var.var, var.type);
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
			for (Var var : leftInBinding) {
				table.bind(var.var, var.type);
			}
			for (Var var : rightInBinding) {
				table.bind(var.var, var.type);
			}
			for (Var var : postBinding) {
				table.bind(var.var, var.type);
			}
		}
	}

	private List<Var> getPipelineBindings(AST input) {
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

		List<Var> bindings = table.inScopeBindings();
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
			table.bind(name, ITEMS);
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
			table.bind(name, ONE_ITEM);
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
			table.bind(name, ITEMS);
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
			table.bind((QNm) expr.getChild(i).getChild(0).getValue(), ONE_ITEM);
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
			table.bind(Bits.FS_DOT, ONE_ITEM);
			table.bind(Bits.FS_POSITION, ONE_INTEGER);
			table.bind(Bits.FS_LAST, ONE_INTEGER);
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
	}

	private void stepExpr(AST expr) {
		walkInspect(expr.getChild(0), true, false);
		walkInspect(expr.getChild(1), true, false);
		for (int i = 2; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			table.bind(Bits.FS_DOT, ONE_ITEM);
			table.bind(Bits.FS_POSITION, ONE_INTEGER);
			table.bind(Bits.FS_LAST, ONE_INTEGER);
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
	}

	private void pathExpr(AST expr) {
		table.openScope(expr, false);
		for (int i = 0; i < expr.getChildCount(); i++) {
			table.openScope(expr.getChild(i), false);
			if (i > 0) {
				table.bind(Bits.FS_DOT, ONE_ITEM);
				table.bind(Bits.FS_POSITION, ONE_INTEGER);
				table.bind(Bits.FS_LAST, ONE_INTEGER);
			}
			walkInspect(expr.getChild(i), true, false);
			table.closeScope();
		}
		table.closeScope();
	}

	private void documentExpr(AST node) {
		table.openScope(node, false);
		table.bind(Bits.FS_PARENT, new SequenceType(DocumentType.DOC,
				Cardinality.One));
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
		table.bind(Bits.FS_PARENT, new SequenceType(ElementType.ELEMENT,
				Cardinality.One));
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
			tmp[pos++] = ref.var.scope;
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

	/*
	 * create a sorted and duplicate-free array of variable accesses
	 */
	protected VarRef[] sortVarRefs(VarRef varRefs) {
		if (varRefs == null) {
			return new VarRef[0];
		}
		int cnt = 0;
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			cnt++;
		}
		int pos = 0;
		VarRef[] tmp = new VarRef[cnt];
		for (VarRef ref = varRefs; ref != null; ref = ref.next) {
			tmp[pos++] = ref;
		}
		Arrays.sort(tmp, new Comparator<VarRef>() {
			@Override
			public int compare(VarRef o1, VarRef o2) {
				return o1.var.scope.compareTo(o2.var.scope);
			}
		});
		return tmp;
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
