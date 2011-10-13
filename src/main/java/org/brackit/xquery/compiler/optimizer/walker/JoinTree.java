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

import static org.brackit.xquery.compiler.XQ.Join;
import static org.brackit.xquery.compiler.XQ.Start;

import java.util.HashSet;

import org.brackit.xquery.compiler.AST;

/**
 * @author Sebastian Baechle
 * 
 */
public class JoinTree extends PipelineVarTracker {

	HashSet<AST> dontTouch = new HashSet<AST>();

	@Override
	protected AST prepare(AST root) {
		collectVars(root);
		return root;
	}

	@Override
	protected AST visit(AST node) {
		if (node.getType() != Join) {
			return node;
		}
		if (dontTouch.contains(node)) {
			return node;
		}
		AST in = node.getChild(0);
		if (in.getType() != Join) {
			return node;
		}
		if (node.getProperty("group") != null) {
			System.err
					.println("Look at me I'am a sample query for JoinTree rewriting with non-empty S0");
			// TODO S0 is not empty
			return node;
		}
		AST lJoinExpr = node.getChild(1).getChild(1);
		VarRef refs = varRefs(lJoinExpr, null);

		AST minBind = in.getChild(2);
		while (minBind.getType() != Start) {
			minBind = minBind.getChild(0);
		}
		minBind = minBind.getParent();

		if (refs.first().var.bndNo < bindingNo(minBind)) {
			return node;
		}

		AST join = new AST(Join);
		join.addChild(in.getChild(2).copyTree());
		join.addChild(node.getChild(1).copyTree());
		join.addChild(node.getChild(2).copyTree());
		in.replaceChild(2, join);

		node.getParent().replaceChild(node.getChildIndex(), in);
		dontTouch.add(in);
		return in;
	}
}
