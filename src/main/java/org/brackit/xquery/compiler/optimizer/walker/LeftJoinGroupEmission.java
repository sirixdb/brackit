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
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * Inspects for the (common?) pattern of a left join, where the "run variable"
 * from the right branch, i.e., the inner loop, is grouped immediately after the
 * join. In this case, the join may emit the match group directly as a sequence
 * and bind the grouped variable directly.
 * 
 * Note: Further optimization is possible here when we remove the artificial
 * counter variable which introduced in the left input branch to compute the
 * grouping.
 * 
 * @author Sebastian Baechle
 * 
 */
public class LeftJoinGroupEmission extends Walker {
	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQueryParser.Join) {
			return node;
		}

		if (!Boolean.parseBoolean(node.getProperty("leftJoin"))) {
			return node;
		}

		// find name of unrolled for-loop variable in right branch
		String innerLoopVarName = innerLoopVarName(node);

		AST groupBy = node.getParent();
		if ((groupBy.getType() != XQueryParser.GroupBy)
				|| (groupBy.getChildCount() != 4)
				|| (!groupBy.getChild(3).getValue().equals(innerLoopVarName))) {
			return node;
		}

		String groupingSequenceVarName = groupBy.getChild(1).getChild(0)
				.getValue();

		AST groupinJoin = node.copyTree();
		groupinJoin.setProperty("emitGroup", "true");
		groupinJoin.insertChild(1, groupBy.getChild(1).copyTree());
		AST parent = groupBy.getParent();
		parent.replaceChild(groupBy.getChildIndex(), groupinJoin);

		// TODO remove the introduced run variable if possible

		return parent;
	}

	private String innerLoopVarName(AST node) {
		AST rightIn = node.getChild(node.getChildCount() - 1);
		while (rightIn.getChild(0).getType() != XQueryParser.Start) {
			rightIn = rightIn.getChild(0);
		}

		if (rightIn.getType() == XQueryParser.LetBind) {
			// a let bind may currently cause errors in grouping. check it
			throw new RuntimeException("CHECK ME");
		}

		// right in is now let-bind or for-bind
		return rightIn.getChild(1).getChild(0).getValue();
	}
}
