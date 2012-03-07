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

import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.dot.DotUtil;

/**
 * 
 * @author Sebastian Baechle
 *
 */
public class Walker {

	protected final StaticContext sctx;
	private AST root;	
	private int snapshot;
	private boolean restart;

	public Walker() {
		this.sctx = null;
	}
	
	public Walker(StaticContext sctx) {
		this.sctx = sctx;
	}

	public final AST walk(AST node) {
		snapshot = 0;
		root = node;
		root = prepare(root);
		AST result = walkInternal(root);
		while (result != root) {
			root = result;
			result = walkInternal(root);
		}
		root = result;
		root = finish(root);
		return root;
	}

	private AST walkInternal(AST node) {
		AST replacement = visit(node);
		if (replacement != node) {
			restart = true;
			return replacement;
		}
		// BEWARE: child count can change when a rewrite
		// introduces new siblings before the current node 
		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			AST result = walkInternal(child);
			while (result != child) {
				if (result.getParent() == node) {
					child = result;
					result = walkInternal(child);
				} else {
					// propagate
					return result;
				}
			}
			if (restart) {
				i--;
				restart = false;
				snapshot();
			}
		}
		return node;
	}
	
	protected AST prepare(AST root) {
		return root;
	}
	
	protected AST finish(AST root) {
		return root;
	}

	/**
	 * Visit the node this perform restructuring. This method must return
	 * the node closest to the root that was modified/replaced/inserted
	 * in the AST to ensure that the tree traversal is restarted at the correct node.
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
