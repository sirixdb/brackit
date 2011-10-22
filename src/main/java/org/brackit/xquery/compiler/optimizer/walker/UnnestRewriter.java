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

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class UnnestRewriter extends Walker {
	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.FlowrExpr) {
			return node;
		}

		AST in = new AST(XQ.Start, "Start");
		AST[] unnested = unnestFlowr(in, node);
		AST opExpr = new AST(XQ.PipeExpr, "ReturnExpr");
		opExpr.addChild(unnested[0]);
		opExpr.addChild(unnested[1]);
		node.getParent().replaceChild(node.getChildIndex(), opExpr);
		snapshot();

		return node.getParent();
	}

	private AST[] unnestFlowr(AST in, AST node) {
		int childCount = node.getChildCount();
		for (int pos = 0; pos < childCount - 1; pos++) {
			AST clause = node.getChild(pos);

			switch (clause.getType()) {
			case XQ.ForClause:
				in = unnestClause(in, clause, XQ.ForBind, "ForBind");
				break;
			case XQ.LetClause:
				in = unnestClause(in, clause, XQ.LetBind, "LetBind");
				break;
			case XQ.WhereClause:
				in = unnestClause(in, clause, XQ.Selection,
						"Selection");
				break;
			case XQ.OrderByClause:
				in = unnestClause(in, clause, XQ.OrderBy, "OrderBy");
				break;
			case XQ.CountClause:
				in = unnestClause(in, clause, XQ.Count, "Count");
				break;
			case XQ.GroupByClause:
				in = unnestClause(in, clause, XQ.GroupBy, "GroupBy");
				break;
			default:
				throw new IllegalStateException();
			}
		}

		AST returnExpr = node.getChild(childCount - 1).getChild(0);

		if (returnExpr.getType() == XQ.FlowrExpr) {
			return unnestFlowr(in, returnExpr);
		} else {
			return new AST[] { in, returnExpr.copyTree() };
		}
	}

	private AST unnestClause(AST in, AST forClause, int unnestType,
			String unnestString) {
		AST letBind = new AST(unnestType, unnestString);
		letBind.addChild(in);
		for (int i = 0; i < forClause.getChildCount(); i++) {
			letBind.addChild(forClause.getChild(i).copyTree());
		}
		in = letBind;
		return in;
	}
}