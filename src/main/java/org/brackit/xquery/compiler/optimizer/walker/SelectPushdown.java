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
import static org.brackit.xquery.compiler.XQ.GroupBy;
import static org.brackit.xquery.compiler.XQ.Selection;
import static org.brackit.xquery.compiler.XQ.Start;

import java.util.HashSet;

import org.brackit.xquery.compiler.AST;

/**
 * Push select operators downstream in a pipeline to reduce
 * number of tuples in a pipeline.
 * 
 * @author Sebastian Baechle
 * 
 */
public class SelectPushdown extends PipelineVarTracker {

	private HashSet<AST> pushed = new HashSet<AST>();

	@Override
	protected AST prepare(AST root) {
		collectVars(root);
		return root;
	}

	@Override
	protected AST visit(AST node) {
		if (node.getType() != Selection) {
			return node;
		}
		if (pushed.contains(node)) {
			return node;
		}
		VarRef refs = varRefs(node.getChild(1), null);
		final AST parent = node.getParent();
		final AST in = node.getChild(0);
		AST tmp = in;
		while (tmp.getType() != Start) {
			if (tmp.getType() == GroupBy) {
				// TODO Pushdown is OK if bindings are grouping keys
				break;
			} else if (tmp.getType() == Count) {
				break;
			} else if ((refs != null) && declares(tmp, refs.first())) {
				break;
			}
			tmp = tmp.getChild(0);
		}
		if (tmp == in) {
			return node;
		}
		tmp.getParent().replaceChild(0, node);
		node.replaceChild(0, tmp);
		parent.replaceChild(0, in);
		pushed.add(node);
		return in;
	}
}
