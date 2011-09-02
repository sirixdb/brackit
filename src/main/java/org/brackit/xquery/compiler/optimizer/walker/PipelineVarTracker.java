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
import static org.brackit.xquery.compiler.XQ.ForBind;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.VariableRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;

/**
 * Base class for {@link Walker} which need to keep track
 * of variable bindings and references within a pipeline.
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class PipelineVarTracker extends Walker {

	protected static class Var {
		final AST binding;
		final int bndNo;
		final QNm name;
		final int no;

		Var(AST binding, int bindingNo, QNm name, int no) {
			this.binding = binding;
			this.bndNo = bindingNo;
			this.name = name;
			this.no = no;
		}

		public String toString() {
			return no + ":'" + name + "'";
		}
	}

	protected static class VarRef {
		final Var var;
		VarRef next;
		VarRef prev;

		VarRef(Var var) {
			this.var = var;
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

		public VarRef insert(Var var) {
			VarRef c = this;
			while (c.var.no > var.no) {
				if (c.prev != null) {
					c = c.prev;
				} else {
					break;
				}
			}
			while (c.var.no < var.no) {
				if ((c.next != null) && (c.next.var.no < var.no)) {
					c = c.next;
				} else {
					break;
				}
			}
			if (c.var.no == var.no) {
				return c;
			}
			VarRef n = new VarRef(var);
			if (c.var.no < var.no) {
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
			return "ref(" + var + ")";
		}
	}

	private Map<QNm, Var> byName = new HashMap<QNm, Var>();

	private Map<AST, List<Var>> byNode = new HashMap<AST, List<Var>>();

	protected VarRef varRefs(AST node, VarRef refs) {
		if (node.getType() == VariableRef) {
			QNm name = (QNm) node.getValue();
			Var var = byName.get(name);
			if (var == null) {
				return refs;
			}
			if (refs == null) {
				return new VarRef(var);
			}
			return refs.insert(var).first();
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			refs = varRefs(node.getChild(i), refs);
		}
		// TODO include "check" and "group" properties?
		return refs;
	}

	protected void collectVars(AST root) {
		collectVarsInternal(root, 0);
	}

	int collectVarsInternal(AST node, int varcount) {
		int childCount = node.getChildCount();
		for (int i = 0; i < childCount; i++) {
			if ((node.getType() == ForBind)
					&& ((i == 1) || ((i == 2) && (childCount == 4)))) {
				register(node, (QNm) node.getChild(i).getChild(0).getValue(),
						varcount++);
			} else if ((node.getType() == LetBind) && (i == 1)) {
				register(node, (QNm) node.getChild(i).getChild(0).getValue(),
						varcount++);
			} else if ((node.getType() == Count) && (i == 1)) {
				register(node, (QNm) node.getChild(i).getChild(0).getValue(),
						varcount++);
			} else {
				AST child = node.getChild(i);
				varcount = collectVarsInternal(child, varcount);
			}
			// TODO windows clause
		}
		return varcount;
	}

	private void register(AST binding, QNm name, int no) {
		List<Var> vars = byNode.get(binding);
		int bindingNo;
		if (vars == null) {
			vars = new ArrayList<Var>(1);
			bindingNo = byNode.size();
			byNode.put(binding, vars);
		} else {
			bindingNo = vars.get(0).bndNo;
		}
		Var var = new Var(binding, bindingNo, name, no);
		byName.put(name, var);
		vars.add(var);
	}
	
	protected int bindingNo(AST node) {
		// TODO window clause
		if ((node.getType() != ForBind) 
				&& (node.getType() != LetBind)
				&& (node.getType() != Count)) {
			throw new RuntimeException();
		}
		return findVar((QNm) node.getChild(1).getChild(0).getValue()).bndNo;
	}
	
	protected Var findVar(QNm name) {
		return byName.get(name);
	}

	protected boolean declares(AST operator, VarRef refs) {
		// TODO window clause
		if ((operator.getType() != ForBind) 
				&& (operator.getType() != LetBind)
				&& (operator.getType() != Count)) {
			return false;
		}

		List<Var> vars = byNode.get(operator);
		if (vars == null) {
			return false;
		}
		for (Var var : vars) {
			for (VarRef ref = refs; ref != null; ref = ref.next()) {
				if (var.name.equals(ref.var.name)) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean dependsOn(AST operator, AST binding) {
		VarRef varRefs = null;
		for (int i = 1; i < operator.getChildCount(); i++) {
			varRefs = varRefs(operator.getChild(i), varRefs);
		}
		return ((varRefs != null) && (declares(binding, varRefs)));
	}
}