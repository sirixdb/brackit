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

import org.apache.log4j.Logger;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.parser.XQueryParser;

/**
 * Convert select operators with comparison predicates and operands with
 * non-overlapping variable scopes.
 * 
 * Lifted selects are converted to left joins and the grouping specification is
 * expanded as far as possible downwards the left input.
 * 
 * @author Sebastian Baechle
 * 
 */
public class JoinRewriter2 extends Walker {
	
	private int artificialRunVarCount;
	
	private static final Logger log = Logger
			.getLogger(LetVariableRefPullup.class);

	protected AST visit(AST node) {
		if (node.getType() != XQueryParser.Selection) {
			return node;
		}
		AST predicate = node.getChild(1);

		if (predicate.getType() != XQueryParser.ComparisonExpr) {
			return node;
		}

		AST comparison = predicate.getChild(0);

		switch (comparison.getType()) {
		case XQueryParser.NodeCompFollows:
		case XQueryParser.NodeCompIs:
		case XQueryParser.NodeCompPrecedes:
		case XQueryParser.GeneralCompNE:
		case XQueryParser.ValueCompNE:
			return node;
		}

		AST leftChild = predicate.getChild(1);
		AST rightChild = predicate.getChild(2);
		int minLeftVarRefNumber = minVarRefNumber(leftChild);
		int minRightVarRefNumber = minVarRefNumber(rightChild);

		if ((minLeftVarRefNumber < 0) || (minRightVarRefNumber < 0)
				|| (minLeftVarRefNumber == minRightVarRefNumber)) {
			// comparison is not dependent on independent bound variables
			return node;
		}

		if (minLeftVarRefNumber > minRightVarRefNumber) {
			// switch left and right in comparison to ensure
			// that left side of comparison corresponds always to the
			// left branch of the join
			int tmpRefNumber = minLeftVarRefNumber;
			minLeftVarRefNumber = minRightVarRefNumber;
			minRightVarRefNumber = tmpRefNumber;

			AST tmpAst = leftChild;
			leftChild = rightChild;
			rightChild = tmpAst;

			switch (comparison.getType()) {
			case XQueryParser.GeneralCompGE:
				comparison = new AST(XQueryParser.GeneralCompLE,
						"GeneralCompLE");
				break;
			case XQueryParser.GeneralCompGT:
				comparison = new AST(XQueryParser.GeneralCompLT,
						"GeneralCompLT");
				break;
			case XQueryParser.GeneralCompLE:
				comparison = new AST(XQueryParser.GeneralCompGE,
						"GeneralCompGE");
				break;
			case XQueryParser.GeneralCompLT:
				comparison = new AST(XQueryParser.GeneralCompGT,
						"GeneralCompGT");
				break;
			case XQueryParser.ValueCompGE:
				comparison = new AST(XQueryParser.ValueCompLE, "ValueCompLE");
				break;
			case XQueryParser.ValueCompGT:
				comparison = new AST(XQueryParser.ValueCompLT, "ValueCompLT");
				break;
			case XQueryParser.ValueCompLE:
				comparison = new AST(XQueryParser.ValueCompGE, "ValueCompGE");
				break;
			case XQueryParser.ValueCompLT:
				comparison = new AST(XQueryParser.ValueCompGT, "ValueCompGT");
				break;
			}
		}

		// divide pipeline in left and right input
		AST right = node.getChild(0).copyTree();
		AST tmp = right;
		while (!isDeclarationOf(tmp, minRightVarRefNumber)) {
			tmp = tmp.getChild(0);
		}
		AST left = tmp.getChild(0);
		AST start = new AST(XQueryParser.Start, "Start");
		tmp.replaceChild(0, start);

		// adjust right pipeline
		String check = null;
		AST tmp2 = start.getParent();
		while (tmp2 != null) {
			// check property has to be propagated
			// to the join
			if (check == null) {
				check = tmp2.getProperty("check");
				if (check != null) {
					tmp2.setProperty("check", null);
				}
			}
			if (tmp2.getType() == XQueryParser.ForBind) {
				// preserve is not necessary anymore because
				// this for bind is no longer part of the
				// main pipeline where the iteration has
				// to be preserved
				if (Boolean.parseBoolean(tmp2.getProperty("preserve"))) {
					tmp2.setProperty("preserve", null);
					// remove all upstream checks for this for binding
					tmp2 = tmp2.getParent();
					while ((tmp2 != null)
							&& (tmp2.getType() != XQueryParser.ForBind)) {
						tmp2.setProperty("check", null);
						tmp2 = tmp2.getParent();
					}
				}
				break;
			}
			tmp2 = tmp2.getParent();
		}

		// build join
		AST join = new AST(XQueryParser.Join, "Join");
		AST condition = new AST(XQueryParser.ComparisonExpr, "ComparisonExpr");
		condition.addChild(comparison.copy());
		condition.addChild(leftChild.copyTree());
		condition.addChild(rightChild.copyTree());
		join.addChild(left);
		join.addChild(condition);
		join.addChild(right);

		// perform a left join if the select was lifted
		if (node.getProperty("check") != null) {
			join.setProperty("leftJoin", "true");
		}
		// joins have to be performed within the same iteration group
		String group = node.getProperty("group");
		if (group != null) {
			join.setProperty("group", group);
		}
		if (check != null) {
			join.setProperty("check", check);
		}

		if (group != null) {
			extendGroup(join);
		}

		node.getParent().replaceChild(node.getChildIndex(), join);
		return node.getParent();
	}

	/*
	 * Find max variable reference to "free" variables in the right branch,
	 * i.e., variable numbers less than the first binding in the right branch.
	 * If it is less than the current grouping, we can extend the group up to
	 * the iteration scope of the max var.
	 */
	private void extendGroup(AST join) {
		int groupNumber = varNumber(join.getProperty("group"));

		// first go down right branch
		// and find the first binding
		AST start = join.getChild(2);
		while (start.getChildCount() > 0) {
			start = start.getChild(0);
		}
		AST first = start.getParent();
		int minBind = varNumber(first.getChild(1).getChild(0).getValue());

		// find max reference to "free" variable
		int maxFreeRef = maxFreeVarRefNumber(join.getChild(2), minBind);
		
		// "free" variable lives inside the iteration group 
		// and thus may change with each iteration ->
		// keep grouping as is
		if (maxFreeRef >= groupNumber) {
			return;
		}

		// if this stays null then right 
		// branch is completely independent
		String groupName = null;
		
		// find iteration group of max free ref
		AST tmp = join;
		while (tmp.getChildCount() > 0) {
			tmp = tmp.getChild(0);
			if (tmp.getType() == XQueryParser.ForBind) {
				int declNumber = varNumber(tmp.getChild(1).getChild(0)
						.getValue());
				if (declNumber != maxFreeRef) {
					continue;
				}
				groupName = checkGroupVar(tmp);

			} else if (tmp.getType() == XQueryParser.LetBind) {
				int declNumber = varNumber(tmp.getChild(1).getChild(0)
						.getValue());
				if (declNumber != maxFreeRef) {
					continue;
				}
			}
		}
		join.setProperty("group", groupName);
	}

	private String checkGroupVar(AST forBind) {
		// use/introduce pos var of for bind for grouping
		if (forBind.getChildCount() == 3) {
			String grpVarName = createRunVarName(varNumber(forBind.getChild(1)
					.getChild(0).getValue()));
			AST runVarBinding = new AST(
					XQueryParser.TypedVariableBinding,
					"TypedVariableBinding");
			runVarBinding.addChild(new AST(XQueryParser.Variable,
					grpVarName));
			forBind.insertChild(2, runVarBinding); // stopIndex < startIndex
			// means insert at
			// startIndex!
			return grpVarName;
		} else {
			return forBind.getChild(2).getChild(0).getValue();
		}
	}


	private int maxFreeVarRefNumber(AST node, int minBind) {
		int max = -1;

		if (node.getType() == XQueryParser.VariableRef) {
			int varNumber = varNumber(node.getValue());
			if (varNumber < minBind) {
				max = varNumber;
			}
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i++);
			int maxFreeRef = maxFreeVarRefNumber(child, minBind);
			if (maxFreeRef < minBind) {
				max = (max >= 0) ? Math.max(max, maxFreeRef) : maxFreeRef;
			}
		}

		return max;
	}

	private boolean isDeclarationOf(AST node, int varNumber) {
		if ((node.getType() != XQueryParser.ForBind)
				&& (node.getType() != XQueryParser.LetBind)) {
			return false;
		}
		int refNumber = varNumber(node.getChild(1).getChild(0).getValue());

		return (refNumber == varNumber);
	}

	private int minVarRefNumber(AST node) {
		int min = (node.getType() == XQueryParser.VariableRef) ? varNumber(node
				.getValue()) : -1;

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i++);
			int minVarRefNumber = minVarRefNumber(child);
			if (minVarRefNumber >= 0) {
				min = (min >= 0) ? Math.min(min, minVarRefNumber)
						: minVarRefNumber;
			}
		}

		return min;
	}

	private int varNumber(String text) {
		return Integer.parseInt(text.substring(text.lastIndexOf(";") + 1));
	}
	
	private String createRunVarName(int number) {
		return "_pos;" + (artificialRunVarCount++) + ";" + number;
	}
}