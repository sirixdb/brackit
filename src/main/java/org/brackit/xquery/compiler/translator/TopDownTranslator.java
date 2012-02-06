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
package org.brackit.xquery.compiler.translator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.PipeExpr;
import org.brackit.xquery.expr.VCmpExpr.Cmp;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.ForBind;
import org.brackit.xquery.operator.GroupBy;
import org.brackit.xquery.operator.LetBind;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.operator.OrderBy;
import org.brackit.xquery.operator.OrderBy.OrderModifier;
import org.brackit.xquery.operator.Print;
import org.brackit.xquery.operator.Select;
import org.brackit.xquery.operator.Start;
import org.brackit.xquery.operator.TableJoin;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Extended compiler for bottom-up compilation of unnested flwor expressions
 * 
 * @author Sebastian Baechle
 * 
 */
public class TopDownTranslator extends Compiler {

	public TopDownTranslator() {
		super();
	}

	@Override
	protected Expr anyExpr(AST node) throws QueryException {
		if (node.getType() == XQ.PipeExpr) {
			// switch to bottom up compilation
			return pipeExpr(node);
		}
		return super.anyExpr(node);
	}

	protected Expr pipeExpr(AST node) throws QueryException {
		int initialBindSize = table.bound().length;
		Operator root = anyOp(new Start(), node.getChild(0));

		// for simpler scoping, the return expression is
		// at the right-most leaf
		AST returnExpr = node.getChild(0);
		while (returnExpr.getType() != XQ.End) {
			returnExpr = returnExpr.getLastChild();
		}
		Expr expr = anyExpr(returnExpr.getChild(0));

		// clear operator bindings
		int unbind = table.bound().length - initialBindSize;
		for (int i = 0; i < unbind; i++) {
			table.unbind();
		}

		return new PipeExpr(root, expr);
	}

	protected Operator anyOp(Operator in, AST node) throws QueryException {
		return _anyOp(in, node);
		// return new Print(_anyOp(in, node));
	}

	protected Operator _anyOp(Operator in, AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.End:
			return in;
		case XQ.ForBind:
			return forBind(in, node);
		case XQ.LetBind:
			return letBind(in, node);
		case XQ.Selection:
			return select(in, node);
		case XQ.OrderBy:
			return orderBy(in, node);
		case XQ.GroupBy:
			return groupBy(in, node);
		case XQ.Count:
			return count(in, node);
		case XQ.Join:
			return join(in, node);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST operator node '%s' of type: %s", node,
					node.getType());
		}
	}

	protected Operator groupBy(Operator in, AST node) throws QueryException {
		int groupSpecCount = node.getChildCount() - 1;
		boolean onlyLast = node.checkProperty("onlyLast");
		QNm[] grpVars = new QNm[groupSpecCount];
		GroupBy groupBy = new GroupBy(in, groupSpecCount, onlyLast);
		for (int i = 0; i < groupSpecCount; i++) {
			grpVars[i] = (QNm) node.getChild(i).getChild(0).getValue();
			table.resolve(grpVars[i], groupBy.group(i));
		}
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, groupBy.check());
		}
		return anyOp(groupBy, node.getLastChild());
	}

	protected Operator join(Operator in, AST node) throws QueryException {
		// compile left (outer) join branch
		int pos = 0;
		Operator leftIn = anyOp(in, node.getChild(pos++));

		// get join type
		AST comparison = node.getChild(pos++);
		Cmp cmp = cmp(comparison.getChild(0));

		boolean isGcmp = false;
		switch (comparison.getChild(0).getType()) {
		case XQ.GeneralCompEQ:
		case XQ.GeneralCompGE:
		case XQ.GeneralCompLE:
		case XQ.GeneralCompLT:
		case XQ.GeneralCompGT:
		case XQ.GeneralCompNE:
			isGcmp = true;
		}

		Expr leftExpr = anyExpr(comparison.getChild(1));

		// compile right (inner) join branch
		Operator rightIn = anyOp(new Start(), node.getChild(pos++));
		Expr rightExpr = anyExpr(comparison.getChild(2));

		boolean leftJoin = node.checkProperty("leftJoin");
		boolean skipSort = node.checkProperty("skipSort");
		TableJoin join = new TableJoin(cmp, isGcmp, leftJoin, skipSort, leftIn,
				leftExpr, rightIn, rightExpr);

		QNm prop = (QNm) node.getProperty("group");
		if (prop != null) {
			table.resolve(prop, join.group());
		}
		prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, join.check());
		}

		return anyOp(join, node.getLastChild());
	}

	private Cmp cmp(AST cmpNode) throws QueryException {
		switch (cmpNode.getType()) {
		case XQ.ValueCompEQ:
			return Cmp.eq;
		case XQ.ValueCompGE:
			return Cmp.ge;
		case XQ.ValueCompLE:
			return Cmp.le;
		case XQ.ValueCompLT:
			return Cmp.lt;
		case XQ.ValueCompGT:
			return Cmp.gt;
		case XQ.ValueCompNE:
			return Cmp.ne;
		case XQ.GeneralCompEQ:
			return Cmp.eq;
		case XQ.GeneralCompGE:
			return Cmp.ge;
		case XQ.GeneralCompLE:
			return Cmp.le;
		case XQ.GeneralCompLT:
			return Cmp.lt;
		case XQ.GeneralCompGT:
			return Cmp.gt;
		case XQ.GeneralCompNE:
			return Cmp.ne;
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST comparison node '%s' of type: %s", cmpNode,
					cmpNode.getType());
		}
	}

	protected Operator forBind(Operator in, AST node) throws QueryException {
		int pos = 0;
		AST runVarDecl = node.getChild(pos++);
		QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();
		SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
		if (runVarDecl.getChildCount() == 2) {
			runVarType = sequenceType(runVarDecl.getChild(1));
		}
		AST posBindingOrSourceExpr = node.getChild(pos++);
		QNm posVarName = null;
		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
			posBindingOrSourceExpr = node.getChild(pos++);
		}
		Expr sourceExpr = expr(posBindingOrSourceExpr, true);

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
		ForBind forBind = new ForBind(in, sourceExpr, false);
		if (posBinding != null) {
			forBind.bindPosition(posBinding.isReferenced());
		}
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, forBind.check());
		}
		return anyOp(forBind, node.getLastChild());
	}

	protected Operator letBind(Operator in, AST node) throws QueryException {
		int pos = 0;
		AST letVarDecl = node.getChild(pos++);
		QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
		SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
		if (letVarDecl.getChildCount() == 2) {
			letVarType = sequenceType(letVarDecl.getChild(1));
		}
		Expr sourceExpr = expr(node.getChild(pos++), true);
		Binding binding = table.bind(letVarName, letVarType);

		// Fake binding of let variable because set-oriented processing requires
		// the variable anyway
		table.resolve(letVarName);
		LetBind letBind = new LetBind(in, sourceExpr);
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, letBind.check());
		}
		return anyOp(letBind, node.getLastChild());
	}

	protected Operator count(Operator in, AST node) throws QueryException {
		int pos = 0;
		AST posVarDecl = node.getChild(pos++);
		QNm posVarName = (QNm) posVarDecl.getChild(0).getValue();
		SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
		if (posVarDecl.getChildCount() == 2) {
			posVarType = sequenceType(posVarDecl.getChild(1));
		}
		Binding binding = table.bind(posVarName, posVarType);

		// Fake binding of let variable because set-oriented processing requires
		// the variable anyway
		table.resolve(posVarName);
		Count count = new Count(in);
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, count.check());
		}
		return anyOp(count, node.getLastChild());
	}

	protected Operator select(Operator in, AST node) throws QueryException {
		int pos = 0;
		Expr expr = anyExpr(node.getChild(pos++));
		Select select = new Select(in, expr);
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, select.check());
		}
		return anyOp(select, node.getLastChild());
	}

	protected Operator orderBy(Operator in, AST node) throws QueryException {
		int orderBySpecCount = node.getChildCount() - 1;
		Expr[] orderByExprs = new Expr[orderBySpecCount];
		OrderModifier[] orderBySpec = new OrderModifier[orderBySpecCount];
		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i);
			orderByExprs[i] = expr(orderBy.getChild(0), true);
			orderBySpec[i] = orderModifier(orderBy);
		}
		OrderBy orderBy = new OrderBy(in, orderByExprs, orderBySpec);
		QNm prop = (QNm) node.getProperty("check");
		if (prop != null) {
			table.resolve(prop, orderBy.check());
		}
		return anyOp(orderBy, node.getLastChild());
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
						return node.getValue().stringValue();
					}
				} catch (DocumentException e) {
					e.printStackTrace();
					return "";
				}
			}
		};
	}
}
