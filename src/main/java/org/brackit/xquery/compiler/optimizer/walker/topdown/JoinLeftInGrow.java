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

import static org.brackit.xquery.compiler.XQ.Join;
import static org.brackit.xquery.compiler.XQ.PipeExpr;
import static org.brackit.xquery.compiler.XQ.Start;

import org.brackit.xquery.compiler.AST;

/**
 * @author Sebastian Baechle
 * 
 */
public class JoinLeftInGrow extends ScopeWalker {

	@Override
	protected AST visit(AST node) {
		if (node.getType() != Join) {
			return node;
		}

		// find closest scope from which
		// right input is independent of
		VarRef refs = findVarRefs(node.getChild(1));
		Scope[] scopes = sortScopes(refs);
		Scope local = findScope(node);

		AST stopAt = null;
		for (int i = scopes.length - 1; i >= 0; i--) {
			Scope scope = scopes[i];
			if (scope.compareTo(local) < 0) {
				stopAt = scope.node;
				break;
			}
		}

		// locate closest pipeline node in AST from which
		// right input is independent, but stop if
		// this pipeline part is input of another join
		AST parent = node.getParent();
		AST anc = parent;
		while ((anc != stopAt)
				&& (anc.getParent().getType() != PipeExpr)
				&& ((anc.getParent().getType() != Join) || (anc.getChildIndex() == 2))) {
			anc = anc.getParent();
		}

		if (anc == parent) {
			return node;
		}

		AST lorig = anc.getLastChild();
		AST leftIn = new AST(Start);
		AST copy = leftIn;
		while (lorig != node) {
			AST toAdd = lorig.copy();
			for (int i = 0; i < lorig.getChildCount() - 1; i++) {
				toAdd.addChild(lorig.getChild(i).copyTree());
			}
			copy.addChild(toAdd);
			copy = toAdd;
			lorig = lorig.getLastChild();
		}
		copy.addChild(node.getChild(0).getChild(0));

		node.replaceChild(0, leftIn);
		anc.replaceChild(anc.getChildCount() - 1, node);
		refreshScopes(anc, true);
		return anc;

	}
}
