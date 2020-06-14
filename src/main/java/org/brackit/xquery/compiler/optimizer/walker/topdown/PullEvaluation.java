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

import static org.brackit.xquery.compiler.XQ.TypedVariableBinding;
import static org.brackit.xquery.compiler.XQ.Variable;

import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;

/**
 * @author Sebastian Baechle
 * 
 */
public class PullEvaluation extends Walker {

	private int checkVar;

	private List<QNm> appendCheck(List<QNm> checks, QNm var) {
		final List<QNm> list = (checks == null) ? new ArrayList<>()
				: new ArrayList<>(checks);
		list.add(var);
		return list;
	}

	private QNm createCheckVarName() {
		return new QNm("_check;" + (checkVar++));
	}

	@Override
	protected AST visit(AST join) {
		if ((join.getType() != XQ.Join) || (!join.checkProperty("leftJoin"))) {
			return join;
		}
		AST post = join.getChild(2);
		boolean hasPost = (post.getChild(0).getType() != XQ.End);
		if (!hasPost) {
			return join;
		}

		@SuppressWarnings("unchecked")
		List<QNm> check = (List<QNm>) join.getProperty("check");

		// prepend a check counter to the left input
		QNm postJoinVar = createCheckVarName();
		AST count = new AST(XQ.Count);
		AST runVarBinding = new AST(TypedVariableBinding);
		runVarBinding.addChild(new AST(Variable, postJoinVar));
		count.addChild(runVarBinding);
		if (check != null) {
			count.setProperty("check", check);
		}

		AST lstart = join.getChild(0);
		AST left = lstart.getChild(0);
		lstart.replaceChild(0, count);
		count.addChild(left);
		
		// add check markers to the join and the post-join part with
		List<QNm> check2 = appendCheck(check, postJoinVar);
		
		// add check markers to the left input
		AST tmp = left;
		while (tmp.getType() != XQ.End) {
			tmp.setProperty("check", check2);
			tmp = tmp.getLastChild();
		}
		
		tmp = post;
		while (tmp.getType() != XQ.End) {
			tmp.setProperty("check", check2);
			tmp = tmp.getLastChild();
		}
		join.setProperty("check", check2);

		snapshot();
		return join;
	}
}
