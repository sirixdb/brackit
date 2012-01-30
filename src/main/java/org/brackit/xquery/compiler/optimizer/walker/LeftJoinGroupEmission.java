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
 * Inspects for the (common?) pattern of a left join
 * that is input of a let bind -> group by combination
 * that was introduced to lift a let bind pipeline.
 * In this case, the join may emit the match group directly as a sequence
 * and bind the grouped let bind variable directly.
 * 
 * @author Sebastian Baechle
 * 
 */
public class LeftJoinGroupEmission extends Walker {
	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.Join) {
			return node;
		}

		if (!node.checkProperty("leftJoin")) {
			return node;
		}
		
		AST letBind = node.getParent();
		if ((letBind.getType() != XQ.LetBind)) {
			return node;
		}
		AST groupBy = letBind.getParent();
		if ((groupBy.getType() != XQ.GroupBy) 
			|| (!groupBy.checkProperty("onlyLast"))) {
			return node;
		}
		
		// join and bind/group combo must not be
		// part of the same check level, i.e., be 
		// part of different liftings
		QNm joinCheck = (QNm) node.getProperty("check");
		QNm letCheck = (QNm) letBind.getProperty("check");
		if ((joinCheck != null) && (joinCheck.equals(letCheck))) {
			return node;
		}
		
		AST groupinJoin = node.copyTree();
		AST letCopy = letBind.copy();
		letCopy.addChild(new AST(XQ.Start));
		letCopy.addChild(letBind.getChild(1));
		letCopy.addChild(letBind.getChild(2));
		groupinJoin.addChild(letCopy);
		AST parent = groupBy.getParent();
		parent.replaceChild(groupBy.getChildIndex(), groupinJoin);
		return groupinJoin;
	}
}
