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

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;

/**
 * @author Sebastian Baechle
 * 
 */
public class TopDownPipeline extends Walker {
	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.FlowrExpr) {
			return node;
		}

		AST pipeExpr = new AST(XQ.PipeExpr);
		pipeExpr.addChild(pipeline(node, 0));
		node.getParent().replaceChild(node.getChildIndex(), pipeExpr);

		return pipeExpr;
	}

	private AST pipeline(AST node, int pos) {
		switch (node.getChild(pos).getType()) {
		case XQ.ForClause:
			return pipelineClause(node, pos, XQ.ForBind);
		case XQ.LetClause:
			return pipelineClause(node, pos, XQ.LetBind);
		case XQ.WhereClause:
			return pipelineClause(node, pos, XQ.Selection);
		case XQ.OrderByClause:
			return pipelineClause(node, pos, XQ.OrderBy);
		case XQ.CountClause:
			return pipelineClause(node, pos, XQ.Count);
		case XQ.GroupByClause:
			return pipelineClause(node, pos, XQ.GroupBy);
		case XQ.ReturnClause:
			AST end = new AST(XQ.End);
			end.addChild(node.getChild(pos).getChild(0).copyTree());
			return end;
		default:
			throw new IllegalStateException();
		}
	}

	private AST pipelineClause(AST node, int pos, int opType) {
		AST clause = node.getChild(pos);
		AST op = new AST(opType);
		for (int i = 0; i < clause.getChildCount(); i++) {
			op.addChild(clause.getChild(i).copyTree());
		}
		AST out = pipeline(node, pos + 1);
		if (out != null) {
			op.addChild(out);
		}
		return op;
	}
}
