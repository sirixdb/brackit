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

import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.DotUtil;

public class Walker {
	private AST root;

	private int snapshot;

	private boolean restart;

	public final void walk(AST node) {
		snapshot = 0;
		root = node;
		walkInternal(node);
	}

	private AST walkInternal(AST node) {
		AST replacement = visit(node);
		if (replacement != node) {
			restart = true;
			return replacement;
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			AST subtree = walkInternal(child);

			if (subtree != child) {
				return subtree;
			} else if (restart) {
				i--;
				restart = false;
				snapshot();
			}
		}
		return node;
	}

	/**
	 * Visit a node to perform restructuring if desired. This method must return
	 * an ancestor of the given node where the walk should restart
	 */
	protected AST visit(AST node) {
		System.out.println("Visiting Node " + node.getValue());
		return node;
	}

	protected void snapshot() {
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(root.dot(), XQuery.DEBUG_DIR, getClass()
					.getSimpleName()
					+ "_" + (snapshot++));
		}
	}
}
