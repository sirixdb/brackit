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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;

/**
 * @author Sebastian Baechle
 * 
 */
public class Projection extends ScopeWalker {

	HashMap<AST, Set<QNm>> scopes = new HashMap<AST, Set<QNm>>();

	@Override
	protected AST prepare(AST root) {
		root = super.prepare(root);
		for (AST scope : getScopes()) {
			scopes.put(scope, null);
		}
		return root;
	}

	@Override
	protected AST visit(AST node) {
		if (node.getType() == XQ.VariableRef) {
			QNm var = (QNm) node.getValue();
			Scope scope = findScope(node);
			Scope bindingScope = scope.resolve(var);
			propagateVarRef(var, scope, bindingScope);
		} else if (node.getType() == XQ.Join) {
			QNm groupVar = (QNm) node.getProperty("group");
			if (groupVar != null) {
				System.out.println("FIXME");
				Scope scope = findScope(node);
				Scope bindingScope = scope.resolve(groupVar);
				propagateVarRef(groupVar, scope, bindingScope);
			}
		}
		return node;
	}

	protected void propagateVarRef(QNm var, Scope scope, Scope bindingScope) {
		Scope tmp = scope;
		while (true) {
			Set<QNm> proj = scopes.get(tmp.node);
			if (proj == null) {
				proj = new HashSet<QNm>();
				scopes.put(tmp.node, proj);
			}
			proj.add(var);

			if (tmp == bindingScope) {
				break;
			}
			tmp = tmp.parent;
		}
	}

	@Override
	protected AST finish(AST root) {
		for (Entry<AST, Set<QNm>> scope : scopes.entrySet()) {
			Set<QNm> proj = scope.getValue();
			List<QNm> l = (proj != null) ? new ArrayList<QNm>(proj)
					: Collections.EMPTY_LIST;
			scope.getKey().setProperty("XXX project", l);
		}
		return root;
	}
}
