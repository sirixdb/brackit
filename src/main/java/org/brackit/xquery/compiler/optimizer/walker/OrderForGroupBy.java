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

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;

/**
 * Insert an orderBy clause in front of a groupBy clause that orders the tuple
 * stream in according to the grouping specification.
 * 
 * @author Sebastian Baechle
 * 
 */
public class OrderForGroupBy extends Walker {

	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.GroupByClause) {
			return node;
		}

		// check if prev sibling is already the needed group by
		AST prev = node.getParent().getChild(node.getChildIndex() - 1);
		if (prev.getType() == XQ.OrderByClause) {
			if (checkOrderBy(node, prev)) {
				return node;
			}
		}

		// introduce order by
		AST orderBy = new AST(XQ.OrderByClause);
		for (int i = 0; i < node.getChildCount() - 1; i++) {
			AST groupBySpec = node.getChild(i);
			AST orderBySpec = new AST(XQ.OrderBySpec);
			for (int j = 0; j < groupBySpec.getChildCount(); j++) {
				orderBySpec.addChild(groupBySpec.getChild(0).copyTree());
			}
			orderBy.addChild(orderBySpec);
		}

		node.setProperty("sequential", Boolean.TRUE);
		node.getParent().insertChild(node.getChildIndex(), orderBy);
		return orderBy;
	}

	private boolean checkOrderBy(AST groupBy, AST orderBy) {
		if (groupBy.getChildCount() - 1 != orderBy.getChildCount()) {
			return false;
		}
		for (int i = 0; i < groupBy.getChildCount() - 1; i++) {
			QNm groupByVar = (QNm) groupBy.getChild(i).getChild(0).getValue();
			QNm orderByVar = (QNm) orderBy.getChild(i).getChild(0).getValue();
			if (!groupByVar.equals(orderByVar)) {
				return false;
			}
		}
		return true;
	}
}
