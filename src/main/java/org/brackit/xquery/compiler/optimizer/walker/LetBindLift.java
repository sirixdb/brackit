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
 * Lift a nested operator pipline, i.e., a ReturnExpr child of a LetBind. The
 * lifted pipeline is embraced by an optional count operator at the bottom and
 * an group by at the top to preserve the semantics of binding a whole sequence
 * to the let-bound variable instead instead of single items.
 * 
 * Note that the lifting converts lifted joins the left joins.
 * 
 * @author Sebastian Baechle
 * 
 */
public class LetBindLift extends Walker {
	private int artificialRunVarCount;
	private int artificialEnumerateVarCount;

	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQueryParser.LetBind) {
			return node;
		}

		AST opEx = node.getChild(2);

		if (opEx.getType() != XQueryParser.ReturnExpr) {
			return node;
		}

		AST liftet = liftInput(node);
		node.getParent().replaceChild(node.getChildIndex(), liftet);

		return node.getParent();
	}

	private AST createBindExpression(String leftJoinVarName, AST expression) {
		if ((leftJoinVarName == null)
				|| (expression.getType() == XQueryParser.VariableRef)) {
			return expression.copyTree();
		} else {
			AST ifThenElse = new AST(XQueryParser.IfExpr, "IfExpr");
			ifThenElse.addChild(new AST(XQueryParser.VariableRef,
					leftJoinVarName));
			ifThenElse.addChild(expression.copyTree());
			ifThenElse.addChild(new AST(XQueryParser.EmptySequence,
					"EmptySequence"));
			return ifThenElse;
		}
	}

	/*
	 * Find/introduce a variable in the copy of the current left input for
	 * grouping
	 */
	private String checkGroupingVar(AST in) {
		String grpVarName;
		while (true) {
			if (in.getType() == XQueryParser.LetBind) {
				// skip let binds in left input branch
				in = in.getChild(0);
			} else if (in.getType() == XQueryParser.ForBind) {
				// use/introduce pos var of for bind for grouping
				if (in.getChildCount() == 3) {
					grpVarName = createRunVarName(refNumber(in.getChild(1)
							.getChild(0)));
					AST runVarBinding = new AST(
							XQueryParser.TypedVariableBinding,
							"TypedVariableBinding");
					runVarBinding.addChild(new AST(XQueryParser.Variable,
							grpVarName));
					in.insertChild(2, runVarBinding); // stopIndex < startIndex
					// means insert at
					// startIndex!
				} else {
					grpVarName = in.getChild(2).getChild(0).getValue();
				}
				break;
			} else {
				// We are at a non-binding node in the left input branch, e.g.
				// Select, Start, Join, LeftJoin
				// and introduce an artificial enumeration node for the grouping
				AST count = new AST(XQueryParser.Count, "Count");
				grpVarName = createEnumerateVarName();
				in.getParent().replaceChild(0, count);
				AST runVarBinding = new AST(XQueryParser.TypedVariableBinding,
						"TypedVariableBinding");
				runVarBinding.addChild(new AST(XQueryParser.Variable,
						grpVarName));
				count.addChild(in);
				count.addChild(runVarBinding);
				String check = in.getProperty("check");
				if (check != null) {
					count.setProperty("check", check);
				}
				break;
			}
		}
		return grpVarName;
	}

	/*
	 * Create the liftet input pipeline from the input branch of the operator
	 * expression
	 */
	private AST liftInput(AST node) {
		AST opEx = node.getChild(2);
		String leftJoinVarName = null;
		AST leftInput = null;
		AST left = null;
		AST tmp = opEx.getChild(0);

		while (tmp.getType() != XQueryParser.Start) {
			AST toAdd = tmp.copy();
			for (int i = 1; i < tmp.getChildCount(); i++) {
				toAdd.addChild(tmp.getChild(i).copyTree());
			}

			if (tmp.getType() == XQueryParser.Join) {
				// lifted join in cascade must be converted to left join
				toAdd.setProperty("leftJoin", "true");
			}
			if (tmp.getType() == XQueryParser.ForBind) {
				// lifted for bind must preserve input
				toAdd.setProperty("preserve", "true");
			}

			if (leftInput == null) {
				leftInput = toAdd;
			} else {
				left.insertChild(0, toAdd);
			}
			left = toAdd;
			tmp = tmp.getChild(0);
		}

		AST initialLeftInput = node.getChild(0).copyTree();
		if (leftInput == null) {
			leftInput = initialLeftInput;
		} else {
			left.insertChild(0, initialLeftInput);
		}
		String grpVarName = checkGroupingVar(initialLeftInput);

		// walk up and add condition to operators
		String forBindVarName = null;
		AST tmp3 = initialLeftInput;
		while (tmp3 != null) {
			if (tmp3.getType() == XQueryParser.ForBind) {
				if (Boolean.parseBoolean(tmp3.getProperty("preserve"))) {
					forBindVarName = tmp3.getChild(1).getChild(0).getValue();
				}
				break;
			}
			tmp3 = tmp3.getChild(0);
		}
		AST tmp2 = left;
		while (tmp2 != null) {
			if (forBindVarName != null) {
				tmp2.setProperty("check", forBindVarName);
			}
			if (tmp2.getType() == XQueryParser.ForBind) {
				forBindVarName = tmp2.getChild(1).getChild(0).getValue();
			}
			if ((tmp2.getType() == XQueryParser.OrderBy)
					|| (tmp2.getType() == XQueryParser.Selection)) {
				tmp2.setProperty("group", grpVarName);
			}
			tmp2 = tmp2.getParent();
		}

		AST groupBy = new AST(XQueryParser.GroupBy, "GroupBy");
		groupBy.addChild(leftInput);
		groupBy.addChild(node.getChild(1).copyTree());
		groupBy.addChild(new AST(XQueryParser.VariableRef, grpVarName));
		groupBy
				.addChild(createBindExpression(leftJoinVarName, opEx
						.getChild(1)));
		if (forBindVarName != null) {
			groupBy.setProperty("check", forBindVarName);
		}
		return groupBy;
	}

	private int refNumber(AST node) {
		return Integer.parseInt(node.getValue().substring(
				node.getValue().lastIndexOf(";") + 1));
	}

	private String createEnumerateVarName() {
		return "_enum;" + (artificialEnumerateVarCount++);
	}

	private String createRunVarName(int number) {
		return "_pos;" + (artificialRunVarCount++) + ";" + number;
	}
}