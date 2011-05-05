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
package org.brackit.xquery.compiler;

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.parser.XQueryParser;
import org.brackit.xquery.expr.ReturnExpr;
import org.brackit.xquery.expr.VCmpExpr.Cmp;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.ForBind;
import org.brackit.xquery.operator.GroupBy;
import org.brackit.xquery.operator.LetBind;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.operator.OrderBy;
import org.brackit.xquery.operator.Print;
import org.brackit.xquery.operator.Select;
import org.brackit.xquery.operator.Start;
import org.brackit.xquery.operator.TableJoin;
import org.brackit.xquery.sequence.type.SequenceType;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * Extended compiler for bottom-up compilation of unnested flwor expressions
 * 
 * @author Sebastian Baechle
 * 
 */
public class BottomUpCompiler extends Compiler {
	@Override
	public Module xquery(AST ast) throws QueryException {
		// new LetVariableRefPullup().walk(ast);
		return super.xquery(ast);
	}

	@Override
	protected Expr anyExpr(AST node) throws QueryException {
		if (node.getType() == XQueryParser.ReturnExpr) {
			// switch to bottom up compilation
			return returnExpr(node);
		}
		return super.anyExpr(node);
	}

	protected Expr returnExpr(AST node) throws QueryException {
		int initialBindSize = table.bound().length;
		Operator root = anyOp(node.getChild(0));
		Expr expr = anyExpr(node.getChild(1));

		// clear operator bindings
		int unbind = table.bound().length - initialBindSize;
		for (int i = 0; i < unbind; i++) {
			table.unbind();
		}

		return new ReturnExpr(root, expr);
	}

	protected Operator anyOp(AST node) throws QueryException {
		return _anyOp(node);
		// return new Print(_anyOp(in, node));
	}

	protected Operator _anyOp(AST node) throws QueryException {
		switch (node.getType()) {
		case XQueryParser.Start:
			return new Start();
		case XQueryParser.ForBind:
			return forBind(node);
		case XQueryParser.LetBind:
			return letBind(node);
		case XQueryParser.Selection:
			return select(node);
		case XQueryParser.OrderBy:
			return orderBy(node);
		case XQueryParser.Join:
			return join(node);
		case XQueryParser.GroupBy:
			return groupBy(node);
		case XQueryParser.Count:
			return count(node);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST operator node '%s' of type: %s", node, node
							.getType());
		}
	}

	private Operator groupBy(AST node) throws QueryException {
		Operator in = anyOp(node.getChild(0));
		AST letVarDecl = node.getChild(1);
		QNm letVarName = module.getNamespaces().qname(
				letVarDecl.getChild(0).getValue());
		SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
		if (letVarDecl.getChildCount() == 2) {
			letVarType = sequenceType(letVarDecl.getChild(1));
		}
		// compile expressions before binding
		Expr posExpr = anyExpr(node.getChild(2));
		Expr itemExpr = anyExpr(node.getChild(3));
		Binding binding = table.bind(letVarName, letVarType);

		return new GroupBy(in, new Expr[] { posExpr }, new Expr[] { itemExpr });
	}

	private Operator join(AST node) throws QueryException {
		boolean leftJoin = Boolean.parseBoolean(node.getProperty("leftJoin"));
		boolean skipSort = Boolean.parseBoolean(node.getProperty("skipSort"));
		boolean emitGroup = Boolean.parseBoolean(node.getProperty("emitGroup"));
		QNm groupingVarName = null;
		SequenceType groupingVarType = null;
		int initialBindSize = table.bound().length;
		int pos = 0;

		// compile left (outer) join branch
		Operator leftIn = anyOp(node.getChild(pos++));

		if (emitGroup) {
			AST groupingVarDecl = node.getChild(pos++);
			groupingVarName = module.getNamespaces().qname(
					groupingVarDecl.getChild(0).getValue());
			groupingVarType = SequenceType.ITEM_SEQUENCE;
			if (groupingVarDecl.getChildCount() == 2) {
				groupingVarType = sequenceType(groupingVarDecl.getChild(1));
			}
		}

		// get join type
		AST comparison = node.getChild(pos++);
		Cmp cmp = cmp(comparison.getChild(0));

		boolean isGcmp = false;

		switch (comparison.getChild(0).getType()) {
		case XQueryParser.GeneralCompEQ:
		case XQueryParser.GeneralCompGE:
		case XQueryParser.GeneralCompLE:
		case XQueryParser.GeneralCompLT:
		case XQueryParser.GeneralCompGT:
		case XQueryParser.GeneralCompNE:
			isGcmp = true;
		}

		Expr leftExpr = anyExpr(comparison.getChild(1));
		Binding[] leftBindings = Arrays.copyOfRange(table.bound(),
				initialBindSize, table.bound().length);

		// compile right (inner) join branch
		Operator rightIn = anyOp(node.getChild(pos++));
		Expr rightExpr = anyExpr(comparison.getChild(2));
		Binding[] rightBindings = Arrays.copyOfRange(table.bound(),
				initialBindSize, table.bound().length);

		if (emitGroup) {
			Binding runVarBinding = table
					.bind(groupingVarName, groupingVarType);
			// Fake binding of run variable because set-oriented processing
			// requires the variable anyway
			table.resolve(groupingVarName);
		}

		return new TableJoin(cmp, isGcmp, leftJoin, skipSort, emitGroup,
				leftIn, leftExpr, rightIn, rightExpr);
	}

	private Cmp cmp(AST cmpNode) throws QueryException {
		switch (cmpNode.getType()) {
		case XQueryParser.ValueCompEQ:
			return Cmp.eq;
		case XQueryParser.ValueCompGE:
			return Cmp.ge;
		case XQueryParser.ValueCompLE:
			return Cmp.le;
		case XQueryParser.ValueCompLT:
			return Cmp.lt;
		case XQueryParser.ValueCompGT:
			return Cmp.gt;
		case XQueryParser.ValueCompNE:
			return Cmp.ne;
		case XQueryParser.GeneralCompEQ:
			return Cmp.eq;
		case XQueryParser.GeneralCompGE:
			return Cmp.ge;
		case XQueryParser.GeneralCompLE:
			return Cmp.le;
		case XQueryParser.GeneralCompLT:
			return Cmp.lt;
		case XQueryParser.GeneralCompGT:
			return Cmp.gt;
		case XQueryParser.GeneralCompNE:
			return Cmp.ne;
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST comparison node '%s' of type: %s", cmpNode,
					cmpNode.getType());
		}
	}

	protected Operator forBind(AST node) throws QueryException {
		QNm posVarName = null;
		Expr sourceExpr = null;
		Operator in = anyOp(node.getChild(0));

		int forClausePos = 1; // child zero is the input
		AST forClause = node;
		AST runVarDecl = forClause.getChild(forClausePos++);
		QNm runVarName = module.getNamespaces().qname(
				runVarDecl.getChild(0).getValue());
		SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
		if (runVarDecl.getChildCount() == 2) {
			runVarType = sequenceType(runVarDecl.getChild(1));
		}
		AST posBindingOrSourceExpr = forClause.getChild(forClausePos++);

		if (posBindingOrSourceExpr.getType() == XQueryParser.TypedVariableBinding) {
			posVarName = module.getNamespaces().qname(
					posBindingOrSourceExpr.getChild(0).getValue());
			posBindingOrSourceExpr = forClause.getChild(forClausePos);
		}
		sourceExpr = expr(posBindingOrSourceExpr, true);

		Binding posBinding = null;
		Binding runVarBinding = table.bind(runVarName, runVarType);
		// Fake binding of run variable because set-oriented processing requires
		// the variable anyway
		table.resolve(runVarName);

		if (posVarName != null) {
			posBinding = table.bind(posVarName, SequenceType.INTEGER);
			// Fake binding of pos variable to simplify compilation.
			table.resolve(posVarName);
			// TODO Optimize and do not bind variable if not necessary
		}

		ForBind forBind = new ForBind(in, sourceExpr);
		if (posBinding != null) {
			forBind.bindPosition(posBinding.isReferenced());
		}

		return forBind;
	}

	protected Operator letBind(AST node) throws QueryException {
		Operator in = anyOp(node.getChild(0));
		int letClausePos = 1; // child zero is the input
		AST letClause = node;
		AST letVarDecl = letClause.getChild(letClausePos++);
		QNm letVarName = module.getNamespaces().qname(
				letVarDecl.getChild(0).getValue());
		SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
		if (letVarDecl.getChildCount() == 2) {
			letVarType = sequenceType(letVarDecl.getChild(1));
		}
		Expr sourceExpr = expr(letClause.getChild(letClausePos++), true);
		Binding binding = table.bind(letVarName, letVarType);

		// Fake binding of let variable because set-oriented processing requires
		// the variable anyway
		table.resolve(letVarName);
		LetBind letBind = new LetBind(in, sourceExpr);
		return letBind;
	}

	protected Operator count(AST node) throws QueryException {
		Operator in = anyOp(node.getChild(0));
		AST posVarDecl = node.getChild(1);
		QNm posVarName = module.getNamespaces().qname(
				posVarDecl.getChild(0).getValue());
		SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
		if (posVarDecl.getChildCount() == 2) {
			posVarType = sequenceType(posVarDecl.getChild(1));
		}
		Binding binding = table.bind(posVarName, posVarType);

		// Fake binding of let variable because set-oriented processing requires
		// the variable anyway
		table.resolve(posVarName);
		Count count = new Count(in);
		return count;
	}

	protected Operator select(AST node) throws QueryException {
		Operator in = anyOp(node.getChild(0));
		Expr expr = anyExpr(node.getChild(1));
		Select select = new Select(in, expr);
		return select;
	}

	private Operator orderBy(AST node) throws QueryException {
		Operator in = anyOp(node.getChild(0));

		int orderBySpecCount = node.getChildCount() - 1;
		Expr[] orderByExprs = new Expr[orderBySpecCount];
		int[] orderBySpec = new int[orderBySpecCount];
		int bindCount = table.bound().length;

		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i + 1);
			orderByExprs[i] = expr(orderBy.getChild(0), true);
			orderBySpec[i] = bindCount + i;
		}

		return new OrderBy(in, orderByExprs, orderBySpec);
	}

	protected Operator wrapDebugOutput(Operator root) {
		return new Print(root, System.out) {
			@Override
			public String asString(QueryContext ctx, Sequence sequence)
					throws QueryException {
				if (sequence == null) {
					return "";
				}
				if (sequence instanceof Item) {
					return (sequence instanceof Node<?>) ? nodeAsString(ctx,
							(Node<?>) sequence) : sequence.toString();
				}
				StringBuilder s = new StringBuilder("(");
				Iter it = sequence.iterate();
				try {
					for (Item item = it.next(); item != null; item = it.next()) {
						s.append((sequence instanceof Node<?>) ? nodeAsString(
								ctx, (Node<?>) sequence) : sequence.toString());
						s.append(", ");
					}
				} finally {
					s.append(")");
					it.close();
				}
				return s.toString();
			}

			private String nodeAsString(QueryContext ctx, Node<?> node) {
				try {
					switch (node.getKind()) {
					case ELEMENT:
						return "<" + node.getName() + ">";
					case ATTRIBUTE:
						return node.getName() + "='" + node.getValue() + "'";
					case DOCUMENT:
						return "doc(" + node.getCollection().getName() + ")";
					default:
						return node.getValue();
					}
				} catch (DocumentException e) {
					e.printStackTrace();
					return "";
				}
			}
		};
	}
}
