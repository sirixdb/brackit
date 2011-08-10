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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class LetVariableRefPullup extends Walker {
	private static final Logger log = Logger
			.getLogger(LetVariableRefPullup.class);

	private final Map<String, Variable> variables = new HashMap<String, Variable>();

	private int substitutionCount;

	private static class Variable {
		private final int btype;
		private final AST bind;
		private final int number;
		private final List<AST> refs = new ArrayList<AST>();

		public Variable(int type, AST decl, int number) {
			this.btype = type;
			this.bind = decl;
			this.number = number;
		}
	}

	@Override
	protected AST visit(AST node) {
		if (node.getType() == XQueryParser.TypedVariableBinding) {
			AST name = node.getChild(0);
			variables.put(name.getValue(), new Variable(node.getParent()
					.getType(), node, refNumber(name)));
			if (log.isDebugEnabled()) {
				log.debug(String.format(
						"Found variable binding %s of binding type %s", name
								.getValue(), node.getParent().getType()));
			}
		} else if (node.getType() == XQueryParser.VariableRef) {
			Variable var = variables.get(node.getValue());

			if ((var != null) && (var.btype == XQueryParser.LetClause)) {
				return pullUp(var, node);
			}
		}
		return node;
	}

	private int refNumber(AST node) {
		return Integer.parseInt(node.getValue().substring(
				node.getValue().lastIndexOf(";") + 1));
	}

	private AST pullUp(Variable var, AST node) {
		AST subtree = node.getParent();

		while ((subtree.getType() == XQueryParser.ContentSequence)
				|| (subtree.getType() == XQueryParser.SequenceExpr)
				|| (subtree.getType() == XQueryParser.ForClause)
				|| (subtree.getType() == XQueryParser.JoinClause)) {
			subtree = subtree.getParent();
		}

		if (!isRelocatable(var.number, subtree)) {
			return node;
		}
		// create new let clause encapsulating the subtree rooted at parent
		String substitueVariableName = substitutionName(var.number);
		AST letClause = new AST(XQueryParser.LetClause, "LetClause");
		AST typedVarBinding = new AST(XQueryParser.TypedVariableBinding,
				"TypedVariableBinding");
		AST substituteVariable = new AST(XQueryParser.Variable,
				substitueVariableName);
		typedVarBinding.addChild(substituteVariable);
		letClause.addChild(typedVarBinding);
		AST clonedSubtree = subtree.copyTree();
		letClause.addChild(clonedSubtree);

		// copy subtree expression to after let triggering let expression
		AST bindingLetClause = var.bind.getParent();
		AST bindingFlowr = bindingLetClause.getParent();
		if (bindingFlowr.getChild(0).getType() == XQueryParser.ForClause) {
			// insert let clause after last let clause in binding flowr
			int insertPos = bindingLetClause.getChildIndex();
			int bindingFlowrChildCount = bindingFlowr.getChildCount();
			while (bindingFlowr.getChild(++insertPos).getType() == XQueryParser.LetClause)
				;

			AST[] following = new AST[bindingFlowrChildCount - insertPos];
			int followingPos = 0;
			for (int i = insertPos; i < bindingFlowrChildCount; i++) {
				following[followingPos++] = bindingFlowr.getChild(insertPos);
				bindingFlowr.deleteChild(insertPos);
			}
			bindingFlowr.addChild(letClause);
			for (AST f : following) {
				bindingFlowr.addChild(f);
			}
		} else {
			AST bindingReturnClause = bindingFlowr.getChild(bindingFlowr
					.getChildCount() - 1);
			AST flowr = new AST(XQueryParser.FlowrExpr, "FlowrExpr");
			AST returnClause = new AST(XQueryParser.ReturnClause,
					"ReturnClause");
			returnClause.addChild(bindingReturnClause.getChild(0));
			flowr.addChild(letClause);
			flowr.addChild(returnClause);
			bindingReturnClause.replaceChild(0, flowr);
		}

		// substitute subtree with new variable
		AST variableRef = new AST(XQueryParser.VariableRef,
				substitueVariableName);
		subtree.getParent().replaceChild(subtree.getChildIndex(), variableRef);

		System.out.println("Pullup " + var.bind.getChild(0).getValue());

		snapshot();

		return subtree.getParent();
	}

	private String substitutionName(int number) {
		return "_sub;" + (substitutionCount++) + ";" + number;
	}

	private boolean isRelocatable(int maxAllowedRefNumber, AST node) {
		if ((node.getType() == XQueryParser.VariableRef)
				&& (refNumber(node) > maxAllowedRefNumber)) {
			return false;
		}
		if (node.getType() == XQueryParser.ContextItemExpr) {
			return false;
		}
		// TODO resolve function properly
		if ((node.getType() == XQueryParser.FunctionCall)
				&& ((node.getValue().equals("position()"))
						|| (node.getValue().equals("last()"))
						|| (node.getValue().equals("fn:position()")) || (node
						.getValue().equals("fn:last()")))) {
			return false;
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			if (!isRelocatable(maxAllowedRefNumber, node.getChild(i))) {
				return false;
			}
		}

		return true;
	}
}