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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.module.Namespaces;

/**
 * @author Sebastian Baechle
 * 
 */
public class Projection extends Walker {

	BindingTable table;

	@Override
	protected AST prepare(AST root) {
		table = new BindingTable();
		table.openScope();
		return super.prepare(root);
	}

	@Override
	protected AST visit(AST node) {
		if (node.getType() == XQ.PipeExpr) {
			walkPipelineDown(node);
			// don't visit this subtree
			return null;
		} else if (node.getType() == XQ.VariableRef) {
			table.resolve((QNm) node.getValue());
			return null;
		} else if (node.getType() == XQ.TypeSwitch) {
			typeswitchExpr(node);
			return null;
		} else if (node.getType() == XQ.QuantifiedExpr) {
			quantifiedExpr(node);
			return null;
		} else if (node.getType() == XQ.TransformExpr) {
			transformExpr(node);
			return null;
		} else if (node.getType() == XQ.TryCatchExpr) {
			tryCatchExpr(node);
			return null;
		} else if (node.getType() == XQ.FilterExpr) {
			filterExpr(node);
			return null;
		} else if (node.getType() == XQ.PathExpr) {
			pathExpr(node);
			return null;
		} else if (node.getType() == XQ.CompDocumentConstructor) {
			documentExpr(node);
			return null;
		} else if ((node.getType() == XQ.DirElementConstructor)
				|| (node.getType() == XQ.CompElementConstructor)) {
			elementExpr(node);
			return null;
		} else {
			return node;
		}
	}

	/*
	 * Walk pipeline up and check for each operator, which bindings of its
	 * output will be used by upstream operators. All unused output bindings can
	 * be projected. Note, we assume in general that operators do never produce
	 * a binding which is not used. Nevertheless, this mechanism still admits
	 * that later at compilation an operator creates superfluous bindings.
	 */
	protected void walkPipelineDown(AST node) {
		switch (node.getType()) {
		case XQ.PipeExpr:
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

		table.openScope();
		AST out = node.getLastChild();
		if (out.getType() == XQ.End) {
			// solely visit return expression
			walkInternal(out.getChild(0));
		} else {
			// visit parent to check if it references
			// any variables of this operator
			walkPipelineDown(out);
		}
		List<QNm> project = table.closeScopeAndProject();
		node.setProperty("project", project);
	}

	private static class BindingTable {
		static class Scope {
			static class Node {
				final QNm var;
				boolean resolved;
				Node next;

				Node(QNm var, Node next) {
					this.var = var;
					this.next = next;
				}

				public String toString() {
					return var + (resolved ? "!" : "?");
				}
			}

			final Scope parent;
			Node lvars; // local bound and referenced vars
			Node evars; // referenced vars from ancestor scope

			Scope(Scope parent) {
				this.parent = parent;
			}

			public String toString() {
				StringBuilder s = new StringBuilder();
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

			public void bind(QNm var) {
				Scope.Node p = null;
				Scope.Node n = lvars;
				while (n != null) {
					p = n;
					n = n.next;
				}
				if (p == null) {
					lvars = new Scope.Node(var, null);
				} else {
					p.next = new Scope.Node(var, null);
				}
			}

			boolean resolve(QNm var) {
				for (Scope.Node n = lvars; n != null; n = n.next) {
					if (n.var.atomicCmp(var) == 0) {
						n.resolved = true;
						return true;
					}
				}
				for (Scope.Node n = evars; n != null; n = n.next) {
					if (n.var.atomicCmp(var) == 0) {
						n.resolved = true;
						return true;
					}
				}
				// first reference to a variable which is
				// either defined in an ancestor scope or in
				// a declared variable. If it can be resolved
				// by the parent, we register it as an external
				// variable reference, which will need to be included
				// the output projection of the parent scope
				if ((parent != null) && (parent.resolve(var))) {
					evars = new Node(var, evars);
					return true;
				}
				return false;
			}
		}

		Scope scope;

		void bind(QNm var) {
			scope.bind(var);
		}

		void openScope() {
			scope = new Scope(scope);
		}

		void closeScope() {
			scope = scope.parent;
		}

		List<QNm> closeScopeAndProject() {
			List<QNm> proj = null;
			for (Scope.Node n = scope.evars; n != null; n = n.next) {
				if (proj == null) {
					proj = new ArrayList<QNm>();
				}
				proj.add(n.var);
			}
			scope = scope.parent;
			return (proj != null) ? proj : Collections.EMPTY_LIST;
		}

		void resolve(QNm var) {
			scope.resolve(var);
		}
	}

	protected void forBind(AST node) {
		// get names of binding variable and optional position variable
		QNm forVar = (QNm) node.getChild(0).getChild(0).getValue();
		QNm posVar = null;
		AST posBindingOrSourceExpr = node.getChild(1);
		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVar = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
			posBindingOrSourceExpr = node.getChild(2);
		}

		// visit binding expression
		walkInternal(posBindingOrSourceExpr);

		// bind variables
		table.bind(forVar);
		if (posVar != null) {
			table.bind(posVar);
		}

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void letBind(AST node) {
		// get name of binding variable
		QNm letVar = (QNm) node.getChild(0).getChild(0).getValue();

		// visit binding expression
		walkInternal(node.getChild(1));

		// bind variable
		table.bind(letVar);

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void select(AST node) {
		// visit predicate expression
		walkInternal(node.getChild(0));

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void groupBy(AST node) {
		// resolve grouping variables
		int groupSpecCount = node.getChildCount() - 1;
		for (int i = 0; i < groupSpecCount; i++) {
			QNm grpVar = (QNm) node.getChild(i).getChild(0).getValue();
			table.resolve(grpVar);
		}

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void orderBy(AST node) {
		// visit order by expressions
		int orderBySpecCount = node.getChildCount() - 1;
		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i);
			walkInternal(orderBy.getChild(0));
		}

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void count(AST node) {
		// get name of binding variable
		QNm countVar = (QNm) node.getChild(0).getChild(0).getValue();

		// bind variable
		table.bind(countVar);

		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop);
		}
	}

	protected void typeswitchExpr(AST expr) {
		walkInternal(expr.getChild(0));
		table.openScope();
		for (int i = 1; i < expr.getChildCount(); i++) {
			// handle default case as case clause
			caseClause(expr.getChild(i));
		}
		table.closeScope();
	}

	protected void caseClause(AST clause) {
		table.openScope();
		AST varOrType = clause.getChild(0);
		if (varOrType.getType() == XQ.Variable) {
			QNm name = (QNm) varOrType.getValue();
			table.bind(name);
		}
		// skip intermediate nodes reflecting sequence types....
		walkInternal(clause.getChild(clause.getChildCount() - 1));
		table.closeScope();
	}

	protected void quantifiedExpr(AST expr) {
		int scopeCount = 0;
		// child 0 is quantifier type
		for (int i = 1; i < expr.getChildCount() - 1; i += 2) {
			table.openScope();
			QNm name = (QNm) expr.getChild(i).getChild(0).getValue();
			table.bind(name);
			walkInternal(expr.getChild(i + 1));
			scopeCount++;
		}
		walkInternal(expr.getChild(expr.getChildCount() - 1));
		for (int i = 0; i <= scopeCount; i++) {
			table.closeScope();
		}
	}

	protected void transformExpr(AST expr) {
		table.openScope();
		int pos = 0;
		while (pos < expr.getChildCount() - 2) {
			AST binding = expr.getChild(pos++);
			QNm name = (QNm) binding.getChild(0).getValue();
			table.bind(name);
			walkInternal(binding.getChild(1));
		}
		AST modify = expr.getChild(pos++);
		walkInternal(modify);
		walkInternal(expr.getChild(pos));
		table.closeScope();
	}

	protected void tryCatchExpr(AST expr) {
		walkInternal(expr.getChild(0));
		table.openScope();
		table.bind(Namespaces.ERR_CODE);
		table.bind(Namespaces.ERR_DESCRIPTION);
		table.bind(Namespaces.ERR_VALUE);
		table.bind(Namespaces.ERR_MODULE);
		table.bind(Namespaces.ERR_LINE_NUMBER);
		table.bind(Namespaces.ERR_COLUMN_NUMBER);
		for (int i = 7; i < expr.getChildCount(); i++) {
			// child i,0 is catch error list
			walkInternal(expr.getChild(i).getChild(0));
		}
		table.closeScope();
	}

	protected void filterExpr(AST expr) {
		walkInternal(expr.getChild(0));
		for (int i = 1; i < expr.getChildCount(); i++) {
			table.openScope();
			table.bind(Namespaces.FS_DOT);
			table.bind(Namespaces.FS_POSITION);
			table.bind(Namespaces.FS_LAST);
			walkInternal(expr.getChild(i));
			table.closeScope();
		}
	}

	protected void pathExpr(AST expr) {
		for (int i = 0; i < expr.getChildCount(); i++) {
			walkInternal(expr.getChild(i));
			table.openScope();
		}
		for (int i = 0; i < expr.getChildCount(); i++) {
			table.closeScope();
		}
	}

	protected void documentExpr(AST node) {
		table.openScope();
		table.bind(Namespaces.FS_PARENT);
		walkInternal(node.getChild(0));
		table.closeScope();
	}

	protected void elementExpr(AST node) {
		int pos = 0;
		while (node.getChild(pos).getType() == XQ.NamespaceDeclaration) {
			pos++;
		}
		walkInternal(node.getChild(pos++));
		table.openScope();
		table.bind(Namespaces.FS_PARENT);
		// visit content sequence
		walkInternal(node.getChild(pos++));
		table.closeScope();
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
