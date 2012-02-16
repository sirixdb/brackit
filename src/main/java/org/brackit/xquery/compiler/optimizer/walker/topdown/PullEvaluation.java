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

import static org.brackit.xquery.compiler.XQ.TypedVariableBinding;
import static org.brackit.xquery.compiler.XQ.Variable;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.compiler.translator.Binding;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 * 
 */
public class PullEvaluation extends Walker {

	@Override
	protected AST visit(AST join) {
		if ((join.getType() != XQ.Join) || (join.getProperty("group") != null)) {
			return join;
		}
		AST post = join.getChild(2).getChild(0);
		boolean hasPost = (post.getType() != XQ.End);
		QNm check = (QNm) join.getProperty("check");

		if ((hasPost) && (join.checkProperty("leftJoin"))) {
			QNm postJoinVar = createCheckVarName();
			AST count = new AST(XQ.Count);
			AST runVarBinding = new AST(TypedVariableBinding);
			runVarBinding.addChild(new AST(Variable, postJoinVar));
			count.addChild(runVarBinding);
			if (check != null) {
				count.setProperty("check", check);
			}

			AST tmp = join.getChild(0);
			while (tmp.getType() != XQ.End) {
				tmp = tmp.getLastChild();
			}
			tmp.getParent().replaceChild(tmp.getChildIndex(), count);
			count.addChild(tmp);

			tmp = post;
			while (tmp.getType() != XQ.End) {
				tmp.setProperty("check", postJoinVar);
				tmp = tmp.getLastChild();
			}
			join.setProperty("check", postJoinVar);
		}

		// prepend an artificial count for
		// marking the join group boundaries
		QNm joingroupVar = createGroupVarName();
		join.setProperty("group", joingroupVar);

		AST count = new AST(XQ.Count);
		AST runVarBinding = new AST(TypedVariableBinding);
		runVarBinding.addChild(new AST(Variable, joingroupVar));
		count.addChild(runVarBinding);
		if (check != null) {
			count.setProperty("check", check);
		}
		count.addChild(join.copyTree());
		join.getParent().replaceChild(join.getChildIndex(), count);
		return join.getParent();
	}

	private int tableJoinGroupVar;
	private int checkVar;

	private QNm createGroupVarName() {
		return new QNm("_joingroup;" + (tableJoinGroupVar++));
	}

	private QNm createCheckVarName() {
		return new QNm("_check;" + (checkVar++));
	}

}
