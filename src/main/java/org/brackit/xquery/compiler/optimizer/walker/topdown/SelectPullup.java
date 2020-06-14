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

import static org.brackit.xquery.compiler.XQ.Count;
import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.Selection;
import static org.brackit.xquery.compiler.XQ.Start;

import java.util.HashSet;
import java.util.Set;

import org.brackit.xquery.compiler.AST;

/**
 * Move select's upstream in a pipeline to reduce number of tuples in a
 * pipeline.
 * 
 * @author Sebastian Baechle
 * 
 */
public final class SelectPullup extends ScopeWalker {

	// used to avoid repeated pull of several selects
	// NOTE: This relies on the fact, that we do not
	// copy nodes while reorganizing the AST
	private final Set<AST> moved = new HashSet<>();

	@Override
	protected AST visit(AST node) {
		if (node.getType() != Selection) {
			return node;
		}
		if (moved.contains(node)) {
			return node;
		}
		
		// get the dependencies of the predicate
		AST parent;
		VarRef refs = findVarRefs(node.getChild(0));
		AST stopAt = null;
		if (refs != null) {
			// find reference to closest ancestor scope
			Scope[] scopes = sortScopes(refs);
			Scope local = findScope(node);
			for (int i = scopes.length - 1; i >= 0; i--) {
				Scope scope = scopes[i];
				if (scope.compareTo(local) < 0) {
					stopAt = scope.node;
					break;
				}
			}
		}
		
		// find the top-most scope in the pipeline
		// to which we can lift the selection
		AST tmp = node;			
		while ((parent = tmp.getParent()).getType() != Start) {
			if (tmp.getType() == GroupBy) {
				// TODO OK if all references are grouping keys
				break;
			} else if (tmp.getType() == Count) {
				break;
			} else if (parent == stopAt) {
				break;
			}
			tmp = parent;
		}
		if (parent == node.getParent()) {
			return node;
		}
		
		// swap the position in the pipeline:
		// 1. remove it from current position 
		// 3. place it on top the current downstream pipeline
		// 2. append the downstream pipeline
		node.getParent().replaceChild(node.getChildIndex(), node.getLastChild());
		parent.replaceChild(tmp.getChildIndex(), node);
		node.replaceChild(1, tmp);				
		
		moved.add(node);
		refreshScopes(parent, true);
		return parent;
	}
}