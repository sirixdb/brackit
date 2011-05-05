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

		// AST join = opEx.getChild(0);
		//		
		// if (join.getType() != XQueryParser.Join)
		// {
		// return node;
		// }

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
	 * Find/introduce a run variable in the copy of the current left input for
	 * grouping
	 */
	private String checkRunVar(AST in) {
		String runVarName;
		while (true) {
			if (in.getType() == XQueryParser.LetBind) {
				// skip let binds in left input branch
				in = in.getChild(0);
			} else if (in.getType() == XQueryParser.ForBind) {
				// use/introduce pos var of for bind for grouping after left
				// join
				if (in.getChildCount() == 3) {
					runVarName = createRunVarName(refNumber(in.getChild(1)
							.getChild(0)));
					AST runVarBinding = new AST(
							XQueryParser.TypedVariableBinding,
							"TypedVariableBinding");
					runVarBinding.addChild(new AST(XQueryParser.Variable,
							runVarName));
					in.insertChild(2, runVarBinding); // stopIndex < startIndex
					// means insert at
					// startIndex!
				} else {
					runVarName = in.getChild(2).getChild(0).getValue();
				}
				break;
			} else {
				// We are at a non-flowr node in the left input branch, e.g.
				// Start, Join, LeftJoin
				// and introduce an artificial enumeration node for the grouping
				AST enumerator = new AST(XQueryParser.Count, "Count");
				runVarName = createEnumerateVarName();
				in.getParent().replaceChild(0, enumerator);
				AST runVarBinding = new AST(XQueryParser.TypedVariableBinding,
						"TypedVariableBinding");
				runVarBinding.addChild(new AST(XQueryParser.Variable,
						runVarName));
				enumerator.addChild(in);
				enumerator.addChild(runVarBinding);
				break;
			}
		}
		return runVarName;
	}

	/*
	 * Create the liftet input pipeline from the input branch of the operator
	 * expression
	 */
	private AST liftInput(AST node) {
		AST opEx = node.getChild(2);
		String leftJoinVarName = null;
		boolean convertedTopJoinToLeftJoin = false;
		AST leftInput = null;
		AST left = null;
		AST tmp = opEx.getChild(0);

		while (tmp.getType() != XQueryParser.Start) {
			AST toAdd = tmp.copy();
			for (int i = 1; i < tmp.getChildCount(); i++) {
				toAdd.addChild(tmp.getChild(i).copyTree());
			}

			if (tmp.getType() == XQueryParser.Join) {
				// if (convertedTopJoinToLeftJoin)
				// {
				// throw new IllegalStateException("Can this happen???"); //
				// FIXME
				// }
				// top join in cascade must be converted to left join
				toAdd.setProperty("leftJoin", "true");
				convertedTopJoinToLeftJoin = true;

				// get left join variable name from right branch
				AST r = toAdd.getChild(1); // left input branch is still missing
				// -> right branch is 2nd child
				while (r.getType() != XQueryParser.Start) {
					r = r.getChild(0);
				}
				leftJoinVarName = r.getParent().getChild(1).getChild(0)
						.getValue();
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

		AST sequencer = new AST(XQueryParser.GroupBy, "GroupBy");
		sequencer.addChild(leftInput);
		sequencer.addChild(node.getChild(1).copyTree());
		String runVarName = checkRunVar(initialLeftInput);
		sequencer.addChild(new AST(XQueryParser.VariableRef, runVarName));
		sequencer.addChild(createBindExpression(leftJoinVarName, opEx
				.getChild(1)));

		return sequencer;
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