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

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.CompileChain;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.Accessor;
import org.brackit.xquery.expr.AndExpr;
import org.brackit.xquery.expr.ArithmeticExpr;
import org.brackit.xquery.expr.ArithmeticExpr.ArithmeticOp;
import org.brackit.xquery.expr.AttributeExpr;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.expr.Castable;
import org.brackit.xquery.expr.CommentExpr;
import org.brackit.xquery.expr.DocumentExpr;
import org.brackit.xquery.expr.ElementExpr;
import org.brackit.xquery.expr.EmptyExpr;
import org.brackit.xquery.expr.ExceptExpr;
import org.brackit.xquery.expr.FilterExpr;
import org.brackit.xquery.expr.GCmpExpr;
import org.brackit.xquery.expr.IfExpr;
import org.brackit.xquery.expr.InstanceOf;
import org.brackit.xquery.expr.IntersectExpr;
import org.brackit.xquery.expr.NodeCmpExpr;
import org.brackit.xquery.expr.NodeCmpExpr.NodeCmp;
import org.brackit.xquery.expr.OrExpr;
import org.brackit.xquery.expr.PIExpr;
import org.brackit.xquery.expr.PathStepExpr;
import org.brackit.xquery.expr.PipeExpr;
import org.brackit.xquery.expr.RangeExpr;
import org.brackit.xquery.expr.SequenceExpr;
import org.brackit.xquery.expr.StepExpr;
import org.brackit.xquery.expr.SwitchExpr;
import org.brackit.xquery.expr.TextExpr;
import org.brackit.xquery.expr.Treat;
import org.brackit.xquery.expr.TryCatchExpr;
import org.brackit.xquery.expr.TypeswitchExpr;
import org.brackit.xquery.expr.UnionExpr;
import org.brackit.xquery.expr.VCmpExpr;
import org.brackit.xquery.function.FunctionExpr;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.ForBind;
import org.brackit.xquery.operator.GroupBy;
import org.brackit.xquery.operator.LetBind;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.operator.OrderBy;
import org.brackit.xquery.operator.Select;
import org.brackit.xquery.operator.Start;
import org.brackit.xquery.update.Delete;
import org.brackit.xquery.update.Insert;
import org.brackit.xquery.update.Insert.InsertType;
import org.brackit.xquery.update.Rename;
import org.brackit.xquery.update.ReplaceNode;
import org.brackit.xquery.update.ReplaceValue;
import org.brackit.xquery.update.Transform;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.util.sort.Ordering.OrderModifier;
import org.brackit.xquery.xdm.Axis;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Function;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AnyNodeType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.AttributeType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.CommentType;
import org.brackit.xquery.xdm.type.DocumentType;
import org.brackit.xquery.xdm.type.ElementType;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.NSNameWildcardTest;
import org.brackit.xquery.xdm.type.NSWildcardNameTest;
import org.brackit.xquery.xdm.type.NodeType;
import org.brackit.xquery.xdm.type.PIType;
import org.brackit.xquery.xdm.type.SequenceType;
import org.brackit.xquery.xdm.type.TextType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Compiler implements Translator {

	protected static class ClauseBinding {
		final ClauseBinding in;
		final Operator operator;
		final Binding[] bindings;

		ClauseBinding(ClauseBinding in, Operator operator, Binding... bindings) {
			this.in = in;
			this.operator = operator;
			this.bindings = bindings;
		}

		void unbind() {
			if (in != null) {
				in.unbind();
			}
		}
	}

	protected VariableTable table;

	protected StaticContext ctx;

	public Compiler() {
	}

	public Expr expression(Module module, StaticContext ctx, AST expr,
			boolean allowUpdate) throws QueryException {
		this.table = new VariableTable(module);
		this.ctx = ctx;
		Expr e = expr(expr, !allowUpdate);
		table.resolvePositions();
		return e;
	}

	public Expr function(Module module, StaticContext ctx, UDF udf,
			QNm[] params, AST expr, boolean allowUpdate) throws QueryException {
		this.table = new VariableTable(module);
		this.ctx = ctx;
		// bind parameter
		SequenceType[] types = udf.getSignature().getParams();
		for (int i = 0; i < params.length; i++) {
			table.bind(params[i], types[i]);
		}
		// ensure fixed parameter positions
		for (int i = 0; i < params.length; i++) {
			table.resolve(params[i]);
		}
		// compile body
		Expr body = expr(expr, !allowUpdate);
		// unbind parameters
		for (int i = 0; i < params.length; i++) {
			table.unbind();
		}
		table.resolvePositions();
		return body;
	}

	protected Expr expr(AST node, boolean disallowUpdatingExpr)
			throws QueryException {
		Expr expr = anyExpr(node);

		if ((disallowUpdatingExpr) && (expr.isUpdating())) {
			throw new QueryException(
					ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
					"Illegal nested update expression");
		}

		return expr;
	}

	protected Expr anyExpr(AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.FlowrExpr:
			return flowrExpr(node);
		case XQ.QuantifiedExpr:
			return quantifiedExpr(node);
		case XQ.EnclosedExpr:
		case XQ.ParenthesizedExpr:
		case XQ.SequenceExpr:
			return sequenceExpr(node);
		case XQ.Str:
			return new Str(Whitespace.normalizeXML11(node.getStringValue()));
		case XQ.Int:
		case XQ.Dbl:
		case XQ.Dec:
		case XQ.QNm:
		case XQ.AnyURI:
		case XQ.Bool:
			return (Atomic) node.getValue();
		case XQ.VariableRef:
			return variableRefExpr(node);
		case XQ.ArithmeticExpr:
			return arithmeticExpr(node);
		case XQ.ComparisonExpr:
			return comparisonExpr(node);
		case XQ.RangeExpr:
			return rangeExpr(node);
		case XQ.AndExpr:
			return andExpr(node);
		case XQ.OrExpr:
			return orExpr(node);
		case XQ.CastExpr:
			return castExpr(node);
		case XQ.CastableExpr:
			return castableExpr(node);
		case XQ.TreatExpr:
			return treatExpr(node);
		case XQ.InstanceofExpr:
			return instanceOfExpr(node);
		case XQ.TypeSwitch:
			return typeswitchExpr(node);
		case XQ.IfExpr:
			return ifExpr(node);
		case XQ.SwitchExpr:
			return switchExpr(node);
		case XQ.FilterExpr:
			return filterExpr(node);
		case XQ.DirElementConstructor:
		case XQ.CompElementConstructor:
			return elementExpr(node);
		case XQ.DirAttributeConstructor:
		case XQ.CompAttributeConstructor:
			return attributeExpr(node);
		case XQ.DirCommentConstructor:
		case XQ.CompCommentConstructor:
			return commentExpr(node);
		case XQ.CompTextConstructor:
			return textExpr(node);
		case XQ.CompDocumentConstructor:
			return documentExpr(node);
		case XQ.DirPIConstructor:
		case XQ.CompPIConstructor:
			return piExpr(node);
		case XQ.FunctionCall:
			return functionCall(node);
		case XQ.PathExpr:
			return pathExpr(node);
		case XQ.StepExpr:
			return stepExpr(node);
		case XQ.ContextItemExpr:
			return table.resolve(Namespaces.FS_DOT);
		case XQ.InsertExpr:
			return insertExpr(node);
		case XQ.DeleteExpr:
			return deleteExpr(node);
		case XQ.ReplaceNodeExpr:
		case XQ.ReplaceValueExpr:
			return replaceExpr(node);
		case XQ.RenameExpr:
			return renameExpr(node);
		case XQ.TransformExpr:
			return transformExpr(node);
		case XQ.OrderedExpr:
			return anyExpr(node.getChild(0));
		case XQ.UnorderedExpr:
			return anyExpr(node.getChild(0));
		case XQ.UnionExpr:
			return unionExpr(node);
		case XQ.ExceptExpr:
			return exceptExpr(node);
		case XQ.IntersectExpr:
			return intersectExpr(node);
		case XQ.TryCatchExpr:
			return tryCatchExpr(node);
		case XQ.ExtensionExpr:
			return extensionExpr(node);
		case XQ.ValidateExpr:
			throw new QueryException(
					ErrorCode.ERR_SCHEMA_VALIDATION_FEATURE_NOT_SUPPORTED,
					"Schema validation feature is not supported.");
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST expr node '%s' of type: %s", node,
					node.getType());
		}
	}

	protected Expr tryCatchExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);

		Binding code = table.bind(
				(QNm) node.getChild(1).getChild(0).getValue(),
				new SequenceType(AtomicType.QNM, Cardinality.One));
		Binding desc = table.bind(
				(QNm) node.getChild(2).getChild(0).getValue(),
				new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne));
		Binding value = table.bind((QNm) node.getChild(3).getChild(0)
				.getValue(), new SequenceType(AnyItemType.ANY,
				Cardinality.ZeroOrMany));
		Binding module = table.bind((QNm) node.getChild(4).getChild(0)
				.getValue(), new SequenceType(AtomicType.STR,
				Cardinality.ZeroOrOne));
		Binding lineNo = table.bind((QNm) node.getChild(5).getChild(0)
				.getValue(), new SequenceType(AtomicType.INR,
				Cardinality.ZeroOrOne));
		Binding colNo = table.bind((QNm) node.getChild(6).getChild(0)
				.getValue(), new SequenceType(AtomicType.INR,
				Cardinality.ZeroOrOne));

		int catchCount = node.getChildCount() - 7;
		TryCatchExpr.ErrorCatch[][] catches = new TryCatchExpr.ErrorCatch[catchCount][];
		Expr[] handler = new Expr[catchCount];
		for (int i = 0; i < catchCount; i++) {
			AST clause = node.getChild(i + 7);
			AST catchErrorList = clause.getChild(0);
			int errorCount = catchErrorList.getChildCount();
			catches[i] = new TryCatchExpr.ErrorCatch[errorCount];
			for (int j = 0; j < errorCount; j++) {
				AST nametest = catchErrorList.getChild(j);
				catches[i][j] = tryCatchNameTest(nametest);
			}
			handler[i] = expr(clause.getChild(1), true);
		}

		for (int i = 0; i < 6; i++) {
			table.unbind();
		}

		return new TryCatchExpr(expr, catches, handler, code.isReferenced(),
				desc.isReferenced(), value.isReferenced(),
				module.isReferenced(), lineNo.isReferenced(),
				colNo.isReferenced());
	}

	protected TryCatchExpr.ErrorCatch tryCatchNameTest(AST child)
			throws QueryException {
		AST name = child.getChild(0);
		switch (name.getType()) {
		case XQ.Wildcard:
			return new TryCatchExpr.Wildcard();
		case XQ.NSWildcardNameTest:
			return new TryCatchExpr.NSWildcard(name.getStringValue());
		case XQ.NSNameWildcardTest:
			return new TryCatchExpr.NameWildcard(name.getStringValue());
		default:
			QNm qnm = (QNm) name.getValue();
			return new TryCatchExpr.Name(qnm.getLocalName(),
					qnm.getNamespaceURI());
		}
	}

	protected Expr extensionExpr(AST node) throws QueryException {
		return (node.getChildCount() == 2) ? anyExpr(node.getChild(1))
				: new EmptyExpr();
	}

	protected Expr unionExpr(AST node) throws QueryException {
		Expr firstExpr = expr(node.getChild(0), true);
		Expr secondExpr = expr(node.getChild(1), true);
		return new UnionExpr(firstExpr, secondExpr);
	}

	protected Expr exceptExpr(AST node) throws QueryException {
		Expr firstExpr = expr(node.getChild(0), true);
		Expr secondExpr = expr(node.getChild(1), true);
		return new ExceptExpr(firstExpr, secondExpr);
	}

	protected Expr intersectExpr(AST node) throws QueryException {
		Expr firstExpr = expr(node.getChild(0), true);
		Expr secondExpr = expr(node.getChild(1), true);
		return new IntersectExpr(firstExpr, secondExpr);
	}

	protected Expr castExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		AST type = node.getChild(1);
		AST aouType = type.getChild(0);
		Type targetType = resolveType((QNm) aouType.getChild(0).getValue(),
				true);
		boolean allowEmptySequence = ((type.getChildCount() == 2) && (type
				.getChild(1).getType() == XQ.CardinalityZeroOrOne));
		StaticContext sctx = node.getStaticContext();
		return new Cast(sctx, expr, targetType, allowEmptySequence);
	}

	protected Expr castableExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		AST type = node.getChild(1);
		AST aouType = type.getChild(0);
		Type targetType = resolveType((QNm) aouType.getChild(0).getValue(),
				true);
		boolean allowEmptySequence = ((type.getChildCount() == 2) && (type
				.getChild(1).getType() == XQ.CardinalityZeroOrOne));
		StaticContext sctx = node.getStaticContext();
		return new Castable(sctx, expr, targetType, allowEmptySequence);
	}

	protected Expr treatExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		SequenceType sequenceType = sequenceType(node.getChild(1));
		return new Treat(expr, sequenceType);
	}

	protected Expr instanceOfExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		SequenceType sequenceType = sequenceType(node.getChild(1));
		return new InstanceOf(expr, sequenceType);
	}

	protected Expr typeswitchExpr(AST node) throws QueryException {
		Expr operandExpr = expr(node.getChild(0), false);
		if (operandExpr.isUpdating()) {
			throw (new QueryException(
					ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
					"Operand expression of typeswitch expression must not be updating."));
		}

		boolean updating = false;
		int vacOrUpdate = 0;
		int cases = node.getChildCount() - 2;
		Expr[] caseExprs = (cases > 0 ? new Expr[cases] : null);
		SequenceType[] caseTypes = (cases > 0 ? new SequenceType[cases] : null);
		boolean[] varRefs = new boolean[cases + 1];

		for (int i = 0; i < cases; i++) {
			AST caseNode = node.getChild(i + 1);
			AST firstChild = caseNode.getChild(0);
			int c = 0;
			QNm varName = null;
			Binding binding = null;

			if (firstChild.getType() == XQ.Variable) {
				c++;
				varName = (QNm) firstChild.getValue();
			}

			caseTypes[i] = sequenceType(caseNode.getChild(c++));

			if (varName != null) {
				binding = table.bind(varName, caseTypes[i]);
			}

			caseExprs[i] = expr(caseNode.getChild(c++), false);

			if (varName != null) {
				if (binding.isReferenced()) {
					varRefs[i] = true;
				}
				table.unbind();
			}

			if (caseExprs[i].isVacuous()) {
				vacOrUpdate++;
			}

			if (caseExprs[i].isUpdating()) {
				updating = true;
				vacOrUpdate++;
			}
		}

		AST defaultNode = node.getChild(node.getChildCount() - 1);
		AST firstChild = defaultNode.getChild(0);
		int c = 0;
		QNm varName = null;
		Binding binding = null;

		if (firstChild.getType() == XQ.Variable) {
			c++;
			varName = (QNm) firstChild.getValue();
			binding = table.bind(varName, SequenceType.ITEM_SEQUENCE);
		}

		Expr defaultExpr = expr(defaultNode.getChild(c), false);

		if (varName != null) {
			if (binding.isReferenced()) {
				varRefs[varRefs.length - 1] = true;
			}
			table.unbind();
		}

		if (defaultExpr.isVacuous()) {
			vacOrUpdate++;
		}

		if (defaultExpr.isUpdating()) {
			updating = true;
			vacOrUpdate++;
		}

		if (updating && vacOrUpdate < cases + 1) {
			throw (new QueryException(
					ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
					"One updating expression in a typeswitch case requires all branches to be updating or vacuous expressions."));
		}

		return new TypeswitchExpr(operandExpr, caseExprs, caseTypes, varRefs,
				defaultExpr, updating, vacOrUpdate == cases + 1);
	}

	protected Expr filterExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		int noOfPredicates = node.getChildCount() - 1;
		Expr[] predicates = new Expr[noOfPredicates];
		boolean[] bindItem = new boolean[noOfPredicates];
		boolean[] bindPos = new boolean[noOfPredicates];
		boolean[] bindSize = new boolean[noOfPredicates];

		for (int i = 0; i < noOfPredicates; i++) {
			Binding itemBinding = table.bind(Namespaces.FS_DOT,
					SequenceType.ITEM);
			Binding posBinding = table.bind(Namespaces.FS_POSITION,
					SequenceType.INTEGER);
			Binding sizeBinding = table.bind(Namespaces.FS_LAST,
					SequenceType.INTEGER);
			predicates[i] = expr(node.getChild(1 + i).getChild(0), true);
			table.unbind();
			table.unbind();
			table.unbind();
			bindItem[i] = itemBinding.isReferenced();
			bindPos[i] = posBinding.isReferenced();
			bindSize[i] = sizeBinding.isReferenced();
		}

		return new FilterExpr(expr, predicates, bindItem, bindPos, bindSize);
	}

	protected Expr insertExpr(AST node) throws QueryException {
		InsertType insertType;

		AST typeNode = node.getChild(0);
		switch (typeNode.getType()) {
		case XQ.InsertInto:
			insertType = InsertType.INTO;
			break;
		case XQ.InsertBefore:
			insertType = InsertType.BEFORE;
			break;
		case XQ.InsertAfter:
			insertType = InsertType.AFTER;
			break;
		case XQ.InsertFirst:
			insertType = InsertType.FIRST;
			break;
		case XQ.InsertLast:
			insertType = InsertType.LAST;
			break;
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST expr node '%s' of type: %s", typeNode,
					typeNode.getType());
		}

		Expr sourceExpr = expr(node.getChild(1), true);
		Expr targetExpr = expr(node.getChild(2), true);

		return new Insert(sourceExpr, targetExpr, insertType);
	}

	protected Expr deleteExpr(AST node) throws QueryException {
		Expr targetExpr = expr(node.getChild(0), true);
		return new Delete(targetExpr);
	}

	protected Expr replaceExpr(AST node) throws QueryException {
		boolean replaceNode = (node.getType() == XQ.ReplaceNodeExpr);
		Expr targetExpr = expr(node.getChild(0), true);
		Expr sourceExpr = expr(node.getChild(1), true);
		return (replaceNode) ? new ReplaceNode(sourceExpr, targetExpr)
				: new ReplaceValue(sourceExpr, targetExpr);
	}

	protected Expr renameExpr(AST node) throws QueryException {
		Expr targetExpr = expr(node.getChild(0), true);
		Expr sourceExpr = expr(node.getChild(1), true);
		return new Rename(node.getStaticContext(), sourceExpr, targetExpr);
	}

	protected Expr transformExpr(AST node) throws QueryException {
		AST current = null;

		QNm varName;
		int childCount = node.getChildCount();
		Expr[] sourceExprs = new Expr[childCount - 2];
		Binding[] bindings = new Binding[childCount - 2];
		boolean[] referenced = new boolean[childCount - 2];
		int c = 0;

		while ((current = node.getChild(c)).getType() == XQ.CopyVariableBinding) {
			varName = (QNm) current.getChild(0).getValue();
			sourceExprs[c] = expr(current.getChild(1), true);
			bindings[c++] = table.bind(varName, SequenceType.ITEM);
		}

		Expr modifyExpr = expr(current, false);

		if (!modifyExpr.isUpdating() && !modifyExpr.isVacuous()) {
			throw (new QueryException(
					ErrorCode.ERR_UPDATING_OR_VACUOUS_EXPR_REQUIRED,
					"Modify clause must not contain an expression that is non-updating and non-vacuous."));
		}

		Expr returnExpr = expr(node.getChild(++c), true);

		for (int i = 0; i < childCount - 2; i++) {
			if (bindings[i].isReferenced()) {
				referenced[i] = true;
			}
			table.unbind();
		}

		return new Transform(sourceExprs, referenced, modifyExpr, returnExpr);
	}

	protected Expr quantifiedExpr(AST node) throws QueryException {
		int childCount = node.getChildCount();
		int pos = 0;
		AST child = node.getChild(pos++);
		boolean someQuantified = (child.getType() == XQ.SomeQuantifier);
		Expr bindingSequenceExpr = quantifiedBindings(new Start(), node, pos);
		Function function;

		if (someQuantified) {
			function = CompileChain.BIT_SOME_FUNC;
		} else {
			function = CompileChain.BIT_EVERY_FUNC;
		}

		return new IfExpr(new FunctionExpr(node.getStaticContext(), function,
				bindingSequenceExpr), Bool.TRUE, Bool.FALSE);
	}

	protected Expr quantifiedBindings(Operator in, AST node, int pos)
			throws QueryException {
		AST child = node.getChild(pos++);

		if (child.getType() == XQ.TypedVariableBinding) {
			QNm runVarName = (QNm) child.getChild(0).getValue();
			SequenceType type = SequenceType.ITEM_SEQUENCE;
			if (child.getChildCount() == 2) {
				type = sequenceType(child.getChild(1));
			}
			Expr sourceExpr = expr(node.getChild(pos++), true);
			ForBind forBind = new ForBind(in, sourceExpr, false);

			Binding runVarBinding = table.bind(runVarName, type);
			Expr returnExpr = quantifiedBindings(forBind, node, pos);
			table.unbind();
			forBind.bindVariable(runVarBinding.isReferenced());
			forBind.bindPosition(false);
			return returnExpr;
		} else {
			return new PipeExpr(in, expr(child, true));
		}
	}

	protected Expr functionCall(AST node) throws QueryException {
		int childCount = node.getChildCount();
		QNm name = (QNm) node.getValue();

		Function function = ctx.getFunctions().resolve(name, childCount);
		Expr[] args;

		if (childCount > 0) {
			args = new Expr[childCount];
			for (int i = 0; i < childCount; i++) {
				AST arg = node.getChild(i);
				if (arg.getType() == XQ.ArgumentPlaceHolder) {
					throw new QueryException(
							ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
							"Partial function application is not supported yet");
				}
				args[i] = expr(arg, true);
			}
		} else if (function.getSignature().defaultIsContextItem()) {
			Expr contextItemRef = table.resolve(Namespaces.FS_DOT);
			args = new Expr[] { contextItemRef };
		} else {
			args = new Expr[0];
		}

		return new FunctionExpr(node.getStaticContext(), function, args);
	}

	protected Expr documentExpr(AST node) throws QueryException {
		boolean bind = false;
		Binding binding = table.bind(Namespaces.FS_PARENT, SequenceType.ITEM);
		Expr contentExpr = expr(node.getChild(0), false);
		table.unbind();
		bind = binding.isReferenced();
		return new DocumentExpr(contentExpr, bind);
	}

	protected Expr elementExpr(AST node) throws QueryException {
		int pos = 0;
		int nsCnt = 0;
		while (node.getChild(nsCnt).getType() == XQ.NamespaceDeclaration) {
			nsCnt++;
		}
		ElementExpr.NS[] ns = new ElementExpr.NS[nsCnt];
		for (int i = 0; i < nsCnt; i++) {
			AST nsDecl = node.getChild(pos++);
			if (nsDecl.getChildCount() == 2) {
				String prefix = nsDecl.getChild(0).getStringValue();
				String uri = nsDecl.getChild(1).getStringValue();
				ns[i] = new ElementExpr.NS(prefix, uri);
			} else {
				String uri = nsDecl.getChild(0).getStringValue();
				ns[i] = new ElementExpr.NS(null, uri);
			}
		}
		Expr nameExpr = expr(node.getChild(pos++), true);
		boolean appendOnly = appendOnly(node);
		boolean bind = false;
		Expr[] contentExpr;

		if (node.getChildCount() > 0) {
			Binding binding = table.bind(Namespaces.FS_PARENT,
					SequenceType.ITEM);
			contentExpr = contentSequence(node.getChild(pos++));
			table.unbind();
			bind = binding.isReferenced();
		} else {
			contentExpr = new Expr[0];
		}

		StaticContext sctx = node.getStaticContext();
		return new ElementExpr(sctx, nameExpr, ns, contentExpr, bind,
				appendOnly);
	}

	protected Expr[] contentSequence(AST node) throws QueryException {
		int childCount = node.getChildCount();

		if (childCount == 0) {
			return new Expr[0];
		}

		int size = 0;
		Expr[] subExprs = new Expr[childCount];
		String merged = null;

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);
			if ((child.getType() == XQ.Str)) {
				merged = (merged == null) ? child.getStringValue() : merged
						+ child.getStringValue();
			} else {
				if ((merged != null) && (!merged.isEmpty())) {
					subExprs[size++] = new Str(merged);
				}
				merged = null;
				subExprs[size++] = expr(child, true);
			}
		}

		if ((merged != null) && (!merged.isEmpty())) {
			subExprs[size++] = new Str(merged);
		}

		return Arrays.copyOf(subExprs, size);
	}

	protected Expr attributeExpr(AST node) throws QueryException {
		Expr nameExpr = expr(node.getChild(0), true);
		Expr[] contentExpr = (node.getChildCount() > 1) ? contentSequence(node
				.getChild(1)) : new Expr[0];
		StaticContext sctx = node.getStaticContext();
		return new AttributeExpr(sctx, nameExpr, contentExpr, appendOnly(node));
	}

	protected Expr commentExpr(AST node) throws QueryException {
		Expr contentExpr = expr(node.getChild(0), true);
		return new CommentExpr(contentExpr, appendOnly(node));
	}

	protected Expr textExpr(AST node) throws QueryException {
		Expr contentExpr = expr(node.getChild(0), true);
		return new TextExpr(contentExpr, appendOnly(node));
	}

	protected Expr piExpr(AST node) throws QueryException {
		Expr nameExpr = expr(node.getChild(0), true);
		Expr contentExpr = (node.getChildCount() > 1) ? expr(node.getChild(1),
				true) : new EmptyExpr();
		return new PIExpr(nameExpr, contentExpr, appendOnly(node));
	}

	private boolean appendOnly(AST node) throws QueryException {
		AST parent = node.getParent();
		if (parent == null) {
			return false;
		}
		if (parent.getType() == XQ.ContentSequence) {
			parent = parent.getParent();
		}

		boolean parentIsConstructor = (parent.getType() == XQ.CompElementConstructor)
				|| (parent.getType() == XQ.CompDocumentConstructor);

		if (parentIsConstructor) {
			table.resolve(Namespaces.FS_PARENT);
		}

		return parentIsConstructor;
	}

	protected Expr andExpr(AST node) throws QueryException {
		Expr firstExpr = expr(node.getChild(0), true);
		Expr secondExpr = expr(node.getChild(1), true);
		return new AndExpr(firstExpr, secondExpr);
	}

	protected Expr orExpr(AST node) throws QueryException {
		Expr firstExpr = expr(node.getChild(0), true);
		Expr secondExpr = expr(node.getChild(1), true);
		return new OrExpr(firstExpr, secondExpr);
	}

	protected Expr ifExpr(AST node) throws QueryException {
		Expr condExpr = expr(node.getChild(0), true);
		Expr ifExpr = expr(node.getChild(1), false);
		Expr elseExpr = expr(node.getChild(2), false);

		if (ifExpr.isUpdating()) {
			if ((!elseExpr.isUpdating()) && (!elseExpr.isVacuous())) {
				throw new QueryException(
						ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
						"Single updating if branch is not allowed");
			}
		} else if (elseExpr.isUpdating()) {
			if ((!ifExpr.isUpdating()) && (!ifExpr.isVacuous())) {
				throw new QueryException(
						ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
						"Single updating else branch is not allowed");
			}
		}

		return new IfExpr(condExpr, ifExpr, elseExpr);
	}

	protected Expr switchExpr(AST node) throws QueryException {
		Expr opExpr = expr(node.getChild(0), true);
		Expr[][] cases = new Expr[node.getChildCount() - 2][];
		for (int i = 1; i < node.getChildCount() - 1; i++) {
			AST caseClause = node.getChild(i);
			Expr[] caseOps = new Expr[caseClause.getChildCount()];
			for (int j = 0; j < caseClause.getChildCount(); j++) {
				caseOps[j] = expr(caseClause.getChild(j), false);
			}
			cases[i - 1] = caseOps;
		}
		Expr dftExpr = expr(node.getChild(node.getChildCount() - 1), false);
		return new SwitchExpr(opExpr, cases, dftExpr);
	}

	protected Expr variableRefExpr(AST node) throws QueryException {
		return table.resolve((QNm) node.getValue());
	}

	protected Expr rangeExpr(AST node) throws QueryException {
		Expr firstArg = expr(node.getChild(0), true);
		Expr secondArg = expr(node.getChild(1), true);
		return new RangeExpr(firstArg, secondArg);
	}

	protected Expr comparisonExpr(AST node) throws QueryException {
		Expr firstArg = expr(node.getChild(1), true);
		Expr secondArg = expr(node.getChild(2), true);
		AST cmpNode = node.getChild(0);
		switch (cmpNode.getType()) {
		case XQ.ValueCompEQ:
			return new VCmpExpr(Cmp.eq, firstArg, secondArg);
		case XQ.ValueCompGE:
			return new VCmpExpr(Cmp.ge, firstArg, secondArg);
		case XQ.ValueCompLE:
			return new VCmpExpr(Cmp.le, firstArg, secondArg);
		case XQ.ValueCompLT:
			return new VCmpExpr(Cmp.lt, firstArg, secondArg);
		case XQ.ValueCompGT:
			return new VCmpExpr(Cmp.gt, firstArg, secondArg);
		case XQ.ValueCompNE:
			return new VCmpExpr(Cmp.ne, firstArg, secondArg);
		case XQ.GeneralCompEQ:
			return new GCmpExpr(Cmp.eq, firstArg, secondArg);
		case XQ.GeneralCompGE:
			return new GCmpExpr(Cmp.ge, firstArg, secondArg);
		case XQ.GeneralCompLE:
			return new GCmpExpr(Cmp.le, firstArg, secondArg);
		case XQ.GeneralCompLT:
			return new GCmpExpr(Cmp.lt, firstArg, secondArg);
		case XQ.GeneralCompGT:
			return new GCmpExpr(Cmp.gt, firstArg, secondArg);
		case XQ.GeneralCompNE:
			return new GCmpExpr(Cmp.ne, firstArg, secondArg);
		case XQ.NodeCompIs:
			return new NodeCmpExpr(NodeCmp.is, firstArg, secondArg);
		case XQ.NodeCompFollows:
			return new NodeCmpExpr(NodeCmp.following, firstArg, secondArg);
		case XQ.NodeCompPrecedes:
			return new NodeCmpExpr(NodeCmp.preceding, firstArg, secondArg);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected comparison: '%s'", cmpNode);
		}
	}

	protected Expr arithmeticExpr(AST node) throws QueryException {
		ArithmeticOp op = null;
		switch (node.getChild(0).getType()) {
		case XQ.AddOp:
			op = ArithmeticOp.PLUS;
			break;
		case XQ.SubtractOp:
			op = ArithmeticOp.MINUS;
			break;
		case XQ.MultiplyOp:
			op = ArithmeticOp.MULT;
			break;
		case XQ.DivideOp:
			op = ArithmeticOp.DIV;
			break;
		case XQ.IDivideOp:
			op = ArithmeticOp.IDIV;
			break;
		case XQ.ModulusOp:
			op = ArithmeticOp.MOD;
		}

		Expr firstArg = expr(node.getChild(1), true);
		Expr secondArg = expr(node.getChild(2), true);
		return new ArithmeticExpr(op, firstArg, secondArg);
	}

	/*
	 * The compilation of path expressions is a bit tricky. A path of the form
	 * E1/E2/../EN must be evaluated with "left-deep semantics", i.e.,
	 * ((E1/E2)/..)/EN and each step EI needs to have the current context item
	 * (focus, $fs:dot) bound, from the preceding step EI-1.
	 */
	protected Expr pathExpr(AST node) throws QueryException {
		Expr e1 = expr(node.getChild(0), true);
		for (int i = 1; i < node.getChildCount(); i++) {
			Binding itemBinding = table.bind(Namespaces.FS_DOT,
					SequenceType.NODE);
			Binding posBinding = table.bind(Namespaces.FS_POSITION,
					SequenceType.INTEGER);
			Binding sizeBinding = table.bind(Namespaces.FS_LAST,
					SequenceType.INTEGER);
			AST step = node.getChild(i);
			Expr e2 = expr(step, true);

			table.unbind();
			table.unbind();
			table.unbind();

			boolean bindItem = itemBinding.isReferenced();
			boolean bindPos = posBinding.isReferenced();
			boolean bindSize = sizeBinding.isReferenced();
			boolean lastStep = (i + 1 == node.getChildCount());
			boolean skipDDO = step.checkProperty("skipDDO");
			boolean checkInput = step.checkProperty("checkInput");
			e1 = new PathStepExpr(e1, e2, bindItem, bindPos, bindSize,
					lastStep, skipDDO, checkInput);
		}
		return e1;
	}

	protected Expr stepExpr(AST node) throws QueryException {
		AST child = node.getChild(0);
		Accessor axis;

		if (child.getType() == XQ.AxisSpec) {
			axis = axis(child.getChild(0));
			child = node.getChild(1);
		} else {
			axis = Accessor.CHILD;
		}

		Expr in = table.resolve(Namespaces.FS_DOT);
		NodeType test = nodeTest(child, axis.getAxis());

		int noOfPredicates = Math.max(node.getChildCount() - 2, 0);
		Expr[] filter = new Expr[noOfPredicates];
		boolean[] bindItem = new boolean[noOfPredicates];
		boolean[] bindPos = new boolean[noOfPredicates];
		boolean[] bindSize = new boolean[noOfPredicates];

		for (int i = 0; i < noOfPredicates; i++) {
			Binding itemBinding = table.bind(Namespaces.FS_DOT,
					SequenceType.ITEM);
			Binding posBinding = table.bind(Namespaces.FS_POSITION,
					SequenceType.INTEGER);
			Binding sizeBinding = table.bind(Namespaces.FS_LAST,
					SequenceType.INTEGER);
			filter[i] = expr(node.getChild(2 + i).getChild(0), true);
			table.unbind();
			table.unbind();
			table.unbind();
			bindItem[i] = itemBinding.isReferenced();
			bindPos[i] = posBinding.isReferenced();
			bindSize[i] = sizeBinding.isReferenced();
		}

		return new StepExpr(axis, test, in, filter, bindItem, bindPos,
				bindSize);
	}

	protected Accessor axis(AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.CHILD:
			return Accessor.CHILD;
		case XQ.DESCENDANT:
			return Accessor.DESCENDANT;
		case XQ.DESCENDANT_OR_SELF:
			return Accessor.DESCENDANT_OR_SELF;
		case XQ.ATTRIBUTE:
			return Accessor.ATTRIBUTE;
		case XQ.PARENT:
			return Accessor.PARENT;
		case XQ.ANCESTOR:
			return Accessor.ANCESTOR;
		case XQ.ANCESTOR_OR_SELF:
			return Accessor.ANCESTOR_OR_SELF;
		case XQ.FOLLOWING_SIBLING:
			return Accessor.FOLLOWING_SIBLING;
		case XQ.FOLLOWING:
			return Accessor.FOLLOWING;
		case XQ.PRECEDING:
			return Accessor.PRECEDING;
		case XQ.PRECEDING_SIBLING:
			return Accessor.PRECEDING_SIBLING;
		case XQ.SELF:
			return Accessor.SELF;
		default:
			throw new QueryException(
					ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
					"Suport for document axis '%s' not implemented yet", node);
		}
	}

	protected SequenceType sequenceType(AST node) throws QueryException {
		AST child = node.getChild(0);
		if (child.getType() == XQ.EmptySequenceType) {
			return SequenceType.EMPTY_SEQUENCE;
		}

		ItemType itemType = itemType(child);
		Cardinality cardinality = Cardinality.One;

		if (node.getChildCount() == 2) {
			switch (node.getChild(1).getType()) {
			case XQ.CardinalityOneOrMany:
				cardinality = Cardinality.OneOrMany;
				break;
			case XQ.CardinalityZeroOrMany:
				cardinality = Cardinality.ZeroOrMany;
				break;
			case XQ.CardinalityZeroOrOne:
				cardinality = Cardinality.ZeroOrOne;
				break;
			default:
				cardinality = Cardinality.One;
			}
		}

		return new SequenceType(itemType, cardinality);
	}

	protected ItemType itemType(AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.ItemType:
			return AnyItemType.ANY;
		case XQ.AtomicOrUnionType:
			return atomicOrUnionType(node);
		default:
			return kindTest(node);
		}
	}

	protected ItemType atomicOrUnionType(AST node) throws QueryException {
		Type type = resolveType((QNm) node.getChild(0).getValue(), false);
		return new AtomicType(type);
	}

	protected NodeType nodeTest(AST node, Axis axis) throws QueryException {
		if (node.getType() == XQ.NameTest) {
			return nameTest(node, axis);
		} else {
			return kindTest(node);
		}
	}

	protected NodeType kindTest(AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.KindTestAnyKind:
			return AnyNodeType.ANY_NODE;
		case XQ.KindTestText:
			return new TextType();
		case XQ.KindTestElement:
			return elementTest(node);
		case XQ.KindTestAttribute:
			return attributeTest(node);
		case XQ.KindTestComment:
			return new CommentType();
		case XQ.KindTestDocument:
			return documentTest(node);
		case XQ.KindTestPi:
			if (node.getChildCount() == 0) {
				return new PIType();
			} else {
				return new PIType(node.getChild(0).getStringValue());
			}
		case XQ.KindTestSchemaElement:
			return schemaElementTest(node);
		case XQ.KindTestSchemaAttribute:
			return schemaAttributeTest(node);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"KindTest translation not implemented yet.");
		}
	}

	protected AttributeType schemaAttributeTest(AST child)
			throws QueryException {
		QNm qname = (QNm) child.getChild(0).getValue();
		return new AttributeType(qname, ctx.getTypes().resolveSchemaType(qname));
	}

	protected ElementType schemaElementTest(AST child) throws QueryException {
		QNm qname = (QNm) child.getChild(0).getValue();
		return new ElementType(qname, ctx.getTypes().resolveSchemaType(qname));
	}

	protected DocumentType documentTest(AST child) throws QueryException {
		if (child.getChildCount() == 0)
			return new DocumentType();
		else if (child.getChild(0).getType() == XQ.KindTestElement)
			return new DocumentType(elementTest(child.getChild(0)));
		else
			return new DocumentType(schemaElementTest(child.getChild(0)));
	}

	protected AttributeType attributeTest(AST child) throws QueryException {
		if (child.getChildCount() == 0)
			return new AttributeType();
		else if (child.getChildCount() == 1)
			return new AttributeType(qNameOrWildcard(child.getChild(0)));
		else
			return new AttributeType(qNameOrWildcard(child.getChild(0)),
					resolveType((QNm) child.getChild(1).getValue(), false));
	}

	protected ElementType elementTest(AST child) throws QueryException {
		if (child.getChildCount() == 0)
			return new ElementType();
		else if (child.getChildCount() == 1)
			return new ElementType(qNameOrWildcard(child.getChild(0)));
		else
			return new ElementType(qNameOrWildcard(child.getChild(0)),
					resolveType((QNm) child.getChild(1).getValue(), false));
	}

	protected QNm qNameOrWildcard(AST name) throws QueryException {
		return (name.getType() == XQ.Wildcard) ? null : (QNm) name.getValue();
	}

	protected NodeType nameTest(AST child, Axis axis) throws QueryException {
		AST name = child.getChild(0);
		switch (name.getType()) {
		case XQ.Wildcard:
			if (axis != Axis.ATTRIBUTE) {
				return new ElementType(null);
			} else {
				return new AttributeType(null);
			}
		case XQ.NSWildcardNameTest:
			return new NSWildcardNameTest(
					(axis == Axis.ATTRIBUTE) ? Kind.ATTRIBUTE : Kind.ELEMENT,
					name.getStringValue());
		case XQ.NSNameWildcardTest:
			return new NSNameWildcardTest(
					(axis == Axis.ATTRIBUTE) ? Kind.ATTRIBUTE : Kind.ELEMENT,
					ctx.getNamespaces().resolve(name.getStringValue()));
		default:
			if (axis != Axis.ATTRIBUTE) {
				return new ElementType((QNm) name.getValue());
			} else {
				return new AttributeType((QNm) name.getValue());
			}
		}
	}

	protected Type resolveType(QNm qname, boolean atomic) throws QueryException {
		if (atomic) {
			return ctx.getTypes().resolveAtomicType(qname);
		} else {
			return ctx.getTypes().resolveType(qname);
		}
	}

	protected Expr sequenceExpr(AST node) throws QueryException {
		boolean allVacouousOrUpdating = false;
		Expr[] subExpr = new Expr[node.getChildCount()];
		for (int i = 0; i < node.getChildCount(); i++) {
			subExpr[i] = expr(node.getChild(i), false);

			if (subExpr[i].isUpdating()) {
				if ((!allVacouousOrUpdating) && (i > 0)) {
					// check if all preceding expressions are vacuous
					for (int j = 0; j < i; j++) {
						if (!subExpr[j].isVacuous()) {
							throw new QueryException(
									ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
									"Illegal nested updating expression.");
						}
					}
				}
				allVacouousOrUpdating = true;
			} else if (allVacouousOrUpdating) {
				if (!subExpr[i].isVacuous()) {
					throw new QueryException(
							ErrorCode.ERR_UPDATE_ILLEGAL_NESTED_UPDATE,
							"Illegal nested updating expression.");
				}
			}
		}
		return new SequenceExpr(subExpr);
	}

	protected Expr flowrExpr(AST node) throws QueryException {
		int childCount = node.getChildCount();
		ClauseBinding cb = flowrClause(new ClauseBinding(null, new Start()),
				node, 0, childCount - 2);
		Expr returnExpr = expr(node.getChild(childCount - 1).getChild(0), false);
		cb.unbind();
		Expr pipeExpr = new PipeExpr(cb.operator, returnExpr);
		return pipeExpr;
	}

	private ClauseBinding flowrClause(ClauseBinding in, AST node, int pos,
			int maxPos) throws QueryException {
		ClauseBinding cb = in;

		while (pos <= maxPos) {
			AST clause = node.getChild(pos++);
			switch (clause.getType()) {
			case XQ.ForClause:
				cb = forClause(clause, cb);
				break;
			case XQ.LetClause:
				cb = letClause(clause, cb);
				break;
			case XQ.WhereClause:
				cb = whereClause(clause, cb);
				break;
			case XQ.OrderByClause:
				cb = orderByClause(clause, cb);
				break;
			case XQ.CountClause:
				cb = countClause(clause, cb);
				break;
			case XQ.GroupByClause:
				cb = groupByClause(clause, cb);
				break;
			default:
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
						"Unknown flowr clause type: %s", clause);
			}
		}

		return cb;
	}

	protected ClauseBinding countClause(AST node, ClauseBinding in)
			throws QueryException {
		AST countVarDecl = node.getChild(0);
		QNm posVarName = (QNm) countVarDecl.getChild(0).getValue();
		SequenceType posVarType = SequenceType.ITEM_SEQUENCE;
		if (countVarDecl.getChildCount() == 2) {
			posVarType = sequenceType(countVarDecl.getChild(1));
		}
		final Binding binding = table.bind(posVarName, posVarType);
		final Count count = new Count(in.operator);

		return new ClauseBinding(in, count, binding) {
			@Override
			public void unbind() {
				super.unbind();
				count.bind(binding.isReferenced());
				table.unbind();
			}
		};
	}

	protected ClauseBinding orderByClause(AST node, ClauseBinding in)
			throws QueryException {
		int orderBySpecCount = node.getChildCount();
		Expr[] orderByExprs = new Expr[orderBySpecCount];
		OrderModifier[] orderBySpec = new OrderModifier[orderBySpecCount];
		for (int i = 0; i < orderBySpecCount; i++) {
			AST orderBy = node.getChild(i);
			orderByExprs[i] = expr(orderBy.getChild(0), true);
			orderBySpec[i] = orderModifier(orderBy);
		}
		OrderBy orderBy = new OrderBy(in.operator, orderByExprs, orderBySpec);
		return new ClauseBinding(in, orderBy);
	}

	protected OrderModifier orderModifier(AST orderBy) {
		boolean asc = true;
		boolean emptyLeast = true;
		String collation = null;
		for (int i = 1; i < orderBy.getChildCount(); i++) {
			AST modifier = orderBy.getChild(i);
			if (modifier.getType() == XQ.OrderByKind) {
				AST direction = modifier.getChild(0);
				asc = (direction.getType() == XQ.ASCENDING);
			} else if (modifier.getType() == XQ.OrderByEmptyMode) {
				AST empty = modifier.getChild(0);
				emptyLeast = (empty.getType() == XQ.LEAST);
			} else if (modifier.getType() == XQ.Collation) {
				collation = modifier.getChild(0).getStringValue();
			}
		}
		return new OrderModifier(asc, emptyLeast, collation);
	}

	protected ClauseBinding groupByClause(AST node, ClauseBinding in)
			throws QueryException {
		int groupSpecCount = node.getChildCount();
		GroupBy groupBy = new GroupBy(in.operator, groupSpecCount, false);
		for (int i = 0; i < groupSpecCount; i++) {
			QNm grpVarName = (QNm) node.getChild(i).getChild(0).getValue();
			table.resolve(grpVarName, groupBy.group(i));
		}
		return new ClauseBinding(in, groupBy);
	}

	protected ClauseBinding whereClause(AST node, ClauseBinding in)
			throws QueryException {
		Expr expr = anyExpr(node.getChild(0));
		Select select = new Select(in.operator, expr);
		return new ClauseBinding(in, select);
	}

	protected ClauseBinding letClause(AST node, ClauseBinding in)
			throws QueryException {
		int letClausePos = 0;
		AST letClause = node;
		AST letVarDecl = letClause.getChild(letClausePos++);
		QNm letVarName = (QNm) letVarDecl.getChild(0).getValue();
		SequenceType letVarType = SequenceType.ITEM_SEQUENCE;
		if (letVarDecl.getChildCount() == 2) {
			letVarType = sequenceType(letVarDecl.getChild(1));
		}
		Expr sourceExpr = expr(letClause.getChild(letClausePos++), true);
		final Binding binding = table.bind(letVarName, letVarType);
		final LetBind letBind = new LetBind(in.operator, sourceExpr);

		return new ClauseBinding(in, letBind, binding) {
			@Override
			public void unbind() {
				super.unbind();
				letBind.bind(binding.isReferenced());
				table.unbind();
			}
		};
	}

	protected ClauseBinding forClause(AST node, ClauseBinding in)
			throws QueryException {
		QNm posVarName = null;
		int forClausePos = 0;
		AST forClause = node;
		AST runVarDecl = forClause.getChild(forClausePos++);
		QNm runVarName = (QNm) runVarDecl.getChild(0).getValue();
		SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
		if (runVarDecl.getChildCount() == 2) {
			runVarType = sequenceType(runVarDecl.getChild(1));
		}
		AST posBindingOrSourceExpr = forClause.getChild(forClausePos++);

		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVarName = (QNm) posBindingOrSourceExpr.getChild(0).getValue();
			posBindingOrSourceExpr = forClause.getChild(forClausePos);
		}
		Expr sourceExpr = expr(posBindingOrSourceExpr, true);

		final Binding runVarBinding = table.bind(runVarName, runVarType);
		final Binding posBinding = (posVarName != null) ? table.bind(
				posVarName, SequenceType.INTEGER) : null;
		final ForBind forBind = new ForBind(in.operator, sourceExpr, false);

		return new ClauseBinding(in, forBind, runVarBinding, posBinding) {
			@Override
			public void unbind() {
				super.unbind();
				if (posBinding != null) {
					forBind.bindPosition(posBinding.isReferenced());
					table.unbind();
				}
				forBind.bindVariable(runVarBinding.isReferenced());
				table.unbind();
			}
		};
	}
}
