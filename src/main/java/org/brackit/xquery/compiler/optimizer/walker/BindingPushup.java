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
package org.brackit.xquery.compiler.optimizer.walker;

import static org.brackit.xquery.compiler.XQ.Count;
import static org.brackit.xquery.compiler.XQ.ForBind;
import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.LetBind;
import static org.brackit.xquery.compiler.XQ.PipeExpr;

import java.util.HashSet;

import org.brackit.xquery.compiler.AST;

/**
 * Push variable bindings upstream in a pipeline to reduce
 * size (tuple width) and number of tuples in a pipeline.
 * 
 * @author Sebastian Baechle
 * 
 */
public class BindingPushup extends PipelineVarTracker {

	private HashSet<AST> pushed = new HashSet<AST>();
	
	@Override
	protected AST prepare(AST root) {
		collectVars(root);
		return root;
	}
	
	@Override
	protected AST visit(AST node) {
		// TODO window clause
		if ((node.getType() != ForBind) && (node.getType() != LetBind)) {
			return node;
		}
		if (pushed.contains(node)) {
			return node;
		}
		final AST parent = node.getParent();
		final AST in = parent;
		AST tmp = in;
		while (tmp.getType() != PipeExpr) {
			if (tmp.getType() == GroupBy) {
				break;
			} else if ((tmp.getType() == Count) && (!pushableAfterCount(node, tmp))) {
				break;
			} else if ((tmp.getType() == ForBind) && (tmp.getType() == ForBind)) {
				// TODO switching ForBinds is legal if order does not matter
				// e.g., because of a following order by or because 
				// the static context is unordered
				break;
			} else if (dependsOn(tmp, node)){
				break;
			}
			tmp = tmp.getParent();
		}
		if (tmp == in) {
			return node;
		}
		push(node, tmp);
		return parent;
	}

	protected void push(AST node, AST newParent) {
		AST parent = node.getParent();
		parent.replaceChild(0, node.getChild(0));
		node.replaceChild(0, newParent.getChild(0));
		newParent.replaceChild(0, node);
		pushed.add(node);
	}

	protected boolean pushableAfterCount(AST binding, AST count) {
		return (binding.getType() == LetBind);
	}
}
