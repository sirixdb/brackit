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

import static org.brackit.xquery.compiler.parser.XQueryParser.Count;
import static org.brackit.xquery.compiler.parser.XQueryParser.ForBind;
import static org.brackit.xquery.compiler.parser.XQueryParser.GroupBy;
import static org.brackit.xquery.compiler.parser.XQueryParser.LetBind;
import static org.brackit.xquery.compiler.parser.XQueryParser.ReturnExpr;

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
		while (tmp.getType() != ReturnExpr) {
			if (tmp.getType() == GroupBy) {
				break;
			} else if (tmp.getType() == Count) {
				// TODO pushup is OK when binding is let
				break;
			} else {
				VarRef refs = null;
				for (int i = 1; i < tmp.getChildCount(); i++) {
					refs = varRefs(tmp.getChild(i), refs);
				}
				if ((refs != null) && declares(node, refs.first())) {
					break;
				}
			}
			tmp = tmp.getParent();
		}
		if (tmp == in) {
			return node;
		}
		parent.replaceChild(0, node.getChild(0));
		node.replaceChild(0, tmp.getChild(0));
		tmp.replaceChild(0, node);
		pushed.add(node);
		return parent;
	}
}
