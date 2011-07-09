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

import static org.brackit.xquery.compiler.parser.XQueryParser.FlowrExpr;
import static org.brackit.xquery.compiler.parser.XQueryParser.ForClause;
import static org.brackit.xquery.compiler.parser.XQueryParser.LetClause;
import static org.brackit.xquery.compiler.parser.XQueryParser.TypedVariableBinding;
import static org.brackit.xquery.compiler.parser.XQueryParser.VariableRef;
import static org.brackit.xquery.compiler.parser.XQueryParser.WhereClause;

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ExtractFLWOR extends Walker {

	private int extracedVarCount;

	@Override
	protected AST visit(AST node) {
		if (node.getType() != FlowrExpr) {
			return node;
		}

		final AST parent = node.getParent();
		if (parent.getType() == LetClause) {
			return node;
		} 
		AST anc = parent;
		while (anc != null) {
			if (isFLWORClause(anc)) {
				break;
			}
			anc = anc.getParent();
		}

		if (anc == null) {
			return node;
		}

		String varName = createVarName();
		AST binding = new AST(TypedVariableBinding, "TypedVariableBinding");
		binding.addChild(new AST(XQueryParser.Variable, varName));
		AST letClause = new AST(LetClause, "LetClause");		
		letClause.addChild(binding);
		letClause.addChild(node.copyTree());
		
		AST varRef = new AST(VariableRef, varName);
		parent.replaceChild(node.getChildIndex(), varRef);
		anc.getParent().insertChild(anc.getChildIndex(), letClause);
		return anc.getParent();
	}

	private boolean isFLWORClause(AST anc) {
		// TODO window clause
		return ((anc.getType() == WhereClause)
			|| (anc.getType() == LetClause)
			|| (anc.getType() == ForClause));		
	}

	private String createVarName() {
		return "_extracted;" + (extracedVarCount++);
	}
}
