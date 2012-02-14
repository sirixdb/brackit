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

import static org.brackit.xquery.compiler.XQ.Join;
import static org.brackit.xquery.compiler.XQ.Start;
import static org.brackit.xquery.compiler.XQ.End;

import org.brackit.xquery.compiler.AST;

/**
 * @author Sebastian Baechle
 * 
 */
public class JoinTree extends ScopeWalker {

	@Override
	protected AST visit(AST node) {
		if (node.getType() != Join) {
			return node;
		}
		return joinTree(node);
	}

	private AST joinTree(AST node) {
		int cnt = 0;
		AST tmp = node;
		while ((tmp.getType() == Join) && (!tmp.checkProperty("leftJoin"))) {
			tmp = tmp.getChild(0).getChild(0);
			cnt++;
		}
		int len = (cnt / 2);
		if (len < 1) {
			return node;
		}
		
		// copy first half of join path
		AST orig = node;
		AST right = new AST(Start); 
		AST copy = right;
		for (int i = 0; i < len; i++) {
			tmp = orig.copy();
			tmp.addChild(node.getChild(1));
			tmp.addChild(node.getChild(2));
			tmp.addChild(node.getChild(3));
			copy.insertChild(0, tmp);
			copy = tmp;
			orig = orig.getChild(0);
		}		
		right.insertChild(0, orig.getChild(2).getChild(0).copyTree());
		
		AST left = orig.getChild(0).copyTree(); 
		
		return node;
	}
}
