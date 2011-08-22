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
package org.brackit.xquery.compiler.translator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Dec;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.ModuleResolver;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.Accessor;
import org.brackit.xquery.expr.AndExpr;
import org.brackit.xquery.expr.ArithmeticExpr;
import org.brackit.xquery.expr.AttributeExpr;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.expr.Castable;
import org.brackit.xquery.expr.CommentExpr;
import org.brackit.xquery.expr.DocumentExpr;
import org.brackit.xquery.expr.ElementExpr;
import org.brackit.xquery.expr.EmptyExpr;
import org.brackit.xquery.expr.ExceptExpr;
import org.brackit.xquery.expr.ExtVariable;
import org.brackit.xquery.expr.FilterExpr;
import org.brackit.xquery.expr.GCmpExpr;
import org.brackit.xquery.expr.IfExpr;
import org.brackit.xquery.expr.InstanceOf;
import org.brackit.xquery.expr.IntersectExpr;
import org.brackit.xquery.expr.NodeCmpExpr;
import org.brackit.xquery.expr.OrExpr;
import org.brackit.xquery.expr.PIExpr;
import org.brackit.xquery.expr.PathStepExpr;
import org.brackit.xquery.expr.RangeExpr;
import org.brackit.xquery.expr.ReturnExpr;
import org.brackit.xquery.expr.SequenceExpr;
import org.brackit.xquery.expr.StepExpr;
import org.brackit.xquery.expr.TextExpr;
import org.brackit.xquery.expr.TransformExpr;
import org.brackit.xquery.expr.Treat;
import org.brackit.xquery.expr.TypeswitchExpr;
import org.brackit.xquery.expr.UnionExpr;
import org.brackit.xquery.expr.VCmpExpr;
import org.brackit.xquery.expr.ArithmeticExpr.ArithmeticOp;
import org.brackit.xquery.expr.NodeCmpExpr.NodeCmp;
import org.brackit.xquery.expr.VCmpExpr.Cmp;
import org.brackit.xquery.function.Function;
import org.brackit.xquery.function.FunctionExpr;
import org.brackit.xquery.function.Signature;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.function.bit.Every;
import org.brackit.xquery.function.bit.Put;
import org.brackit.xquery.function.bit.Silent;
import org.brackit.xquery.function.bit.Some;
import org.brackit.xquery.function.io.Readline;
import org.brackit.xquery.function.io.Writeline;
import org.brackit.xquery.module.Functions;
import org.brackit.xquery.module.LibraryModule;
import org.brackit.xquery.module.MainModule;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.NamespaceDecl;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.operator.Count;
import org.brackit.xquery.operator.ForBind;
import org.brackit.xquery.operator.GroupBy;
import org.brackit.xquery.operator.LetBind;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.operator.OrderBy;
import org.brackit.xquery.operator.Select;
import org.brackit.xquery.operator.Start;
import org.brackit.xquery.operator.OrderBy.OrderModifier;
import org.brackit.xquery.sequence.type.AnyItemType;
import org.brackit.xquery.sequence.type.AnyKindType;
import org.brackit.xquery.sequence.type.AtomicType;
import org.brackit.xquery.sequence.type.AttributeType;
import org.brackit.xquery.sequence.type.Cardinality;
import org.brackit.xquery.sequence.type.CommentType;
import org.brackit.xquery.sequence.type.DocumentType;
import org.brackit.xquery.sequence.type.ElementType;
import org.brackit.xquery.sequence.type.ItemType;
import org.brackit.xquery.sequence.type.KindTest;
import org.brackit.xquery.sequence.type.PIType;
import org.brackit.xquery.sequence.type.SchemaAttributeType;
import org.brackit.xquery.sequence.type.SchemaElementType;
import org.brackit.xquery.sequence.type.SequenceType;
import org.brackit.xquery.sequence.type.TextType;
import org.brackit.xquery.update.Delete;
import org.brackit.xquery.update.Insert;
import org.brackit.xquery.update.Rename;
import org.brackit.xquery.update.ReplaceNode;
import org.brackit.xquery.update.ReplaceValue;
import org.brackit.xquery.update.Insert.InsertType;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Axis;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Compiler implements Translator {

	protected class ClauseBinding {
		final ClauseBinding in;
		final Operator operator;
		final Binding[] bindings;

		public ClauseBinding(ClauseBinding in, Operator operator,
				Binding... bindings) {
			this.in = in;
			this.operator = operator;
			this.bindings = bindings;
		}

		public void unbind() {
			if (in != null) {
				in.unbind();
			}
		}
	}

	private static final Every BIT_EVERY_FUNC = new Every(new QNm(
			Namespaces.XML_NSURI, Namespaces.BIT_PREFIX, "every"),
			new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
					new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)));

	private static final Some BIT_SOME_FUNC = new Some(new QNm(
			Namespaces.XML_NSURI, Namespaces.BIT_PREFIX, "some"),
			new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
					new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)));

	static {
		Functions.predefine(BIT_SOME_FUNC);
		Functions.predefine(BIT_EVERY_FUNC);
		Functions.predefine(new Put(Put.PUT, new Signature(new SequenceType(
				AtomicType.STR, Cardinality.One), new SequenceType(
				AtomicType.STR, Cardinality.One))));
		Functions.predefine(new Put(Put.PUT, new Signature(new SequenceType(
				AtomicType.STR, Cardinality.One), new SequenceType(
				AtomicType.STR, Cardinality.One), new SequenceType(
				AtomicType.STR, Cardinality.ZeroOrOne))));
		Functions.predefine(new Readline());
		Functions.predefine(new Writeline());
		Functions.predefine(new Silent());
	}

	protected final VariableTable table = new VariableTable();

	protected final ModuleResolver resolver;

	protected Module module;

	public Compiler(ModuleResolver resolver) {
		this.resolver = resolver;
	}

	public Module translate(AST ast) throws QueryException {
		if (ast.getChildCount() == 0) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"No module definition found");
		}
		AST module = ast.getChild(0);
		switch (module.getType()) {
		case XQ.MainModule:
			return mainmodule(module);
		case XQ.LibraryModule:
			return libraryModule(module);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST root '%s' of type: %s", ast, ast.getType());
		}
	}

	protected LibraryModule libraryModule(AST ast) throws QueryException {
		LibraryModule module = createLibraryModule();
		NamespaceDecl nsDecl = null;
		this.module = module;

		// target namespace declaration
		AST namespace = ast.getChild(0);
		String prefix = namespace.getChild(0).getChild(0).getValue();
		String nsURI = namespace.getChild(1).getChild(0).getValue();
		if ((nsURI == null) || (nsURI.isEmpty())) {
			throw new QueryException(ErrorCode.ERR_TARGET_NS_EMPTY,
					"The target namespace of a module must not be empty");
		}
		nsDecl = new NamespaceDecl(prefix, nsURI);
		module.getNamespaces().declare(prefix, nsURI);
		module.setTargetNS(nsDecl);

		// target ns is default ns for functions in this module
		module.getNamespaces().setDefaultFunctionNamespace(nsURI);

		prolog(ast.getChild(1));

		// verify that all declared variables are in the target ns
		for (ExtVariable var : module.getVariables().getDeclaredVariables()) {
			if (!nsURI.equals(var.getName().getNamespaceURI())) {
				throw new QueryException(
						ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
						"Declared variable '%s' is not in the module's target namespace '%s'",
						var.getName(), nsURI);
			}
		}
		// verify that all functions are in the target ns
		for (Function[] funs : module.getFunctions().getDeclaredFunctions()) {
			Function fun = funs[0];
			if (!nsURI.equals(fun.getName().getNamespaceURI())) {
				throw new QueryException(
						ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
						"Declared function '%s' is not in the module's target namespace '%s'",
						fun.getName(), nsURI);
			}
		}

		return module;
	}

	protected MainModule mainmodule(AST ast) throws QueryException {
		MainModule module = createMainModule();
		this.module = module;
		AST child = ast.getChild(0);
		if (child.getType() == XQ.Prolog) {
			prolog(child);
			child = ast.getChild(1);
		}
		if (child.getType() != XQ.QueryBody) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST node '%s' of type: %s", child, child
							.getType());
		}
		Expr root = expr(child.getChild(0), false);
		table.resolvePositions();
		module.setBody(root);
		return module;
	}

	protected MainModule createMainModule() {
		return new MainModule();
	}

	protected LibraryModule createLibraryModule() {
		return new LibraryModule();
	}

	protected void prolog(AST node) throws QueryException {
		HashSet<String> importedModules = new HashSet<String>();
		boolean defaultElementNSDeclared = false;
		boolean defaultFunctionNSDeclared = false;
		boolean boundarySpaceDeclared = false;
		boolean baseURIDeclared = false;
		boolean constructionModeDeclared = false;
		boolean orderingModeDeclared = false;
		boolean emptyOrderDeclared = false;
		boolean copyNamespacesDeclared = false;

		int childCount = node.getChildCount();
		for (int i = 0; i < childCount; i++) {
			AST child = node.getChild(i);

			switch (child.getType()) {
			case XQ.TypedVariableDeclaration:
				declareVariable(child);
				break;
			case XQ.FunctionDecl:
				declareFunction(child);
				break;
			case XQ.NamespaceDeclaration:
				declareNamespace(child);
				break;
			case XQ.OptionDeclaration:
				declareOption(child);
				break;
			case XQ.DefaultElementNamespace:
				if (defaultElementNSDeclared) {
					throw new QueryException(
							ErrorCode.ERR_DEFAULT_NS_ALREADY_DECLARED,
							"Default element namespace declared more than once");
				}
				declareDefaultNamespace(child);
				defaultElementNSDeclared = true;
				break;
			case XQ.DefaultFunctionNamespace:
				if (defaultFunctionNSDeclared) {
					throw new QueryException(
							ErrorCode.ERR_DEFAULT_NS_ALREADY_DECLARED,
							"Default function namespace declared more than once");
				}
				declareDefaultFunctionNamespace(child);
				defaultFunctionNSDeclared = true;
				break;
			case XQ.SchemaImport:
				throw new QueryException(
						ErrorCode.ERR_SCHEMA_IMPORT_FEATURE_NOT_SUPPORTED,
						"Schema import is not supported.");
			case XQ.ModuleImport:
				String nsURI = importModule(child);
				if (!importedModules.add(nsURI)) {
					throw new QueryException(
							ErrorCode.ERR_MULTIPLE_IMPORTS_IN_SAME_NS,
							"Multiple module imports of the same target namespace: '%s'",
							nsURI);
				}
				break;
			case XQ.BoundarySpaceDeclaration:
				if (boundarySpaceDeclared) {
					throw new QueryException(
							ErrorCode.ERR_BOUNDARY_SPACE_ALREADY_DECLARED,
							"Boundary space declared more than once");
				}
				declareBoundarySpace(child);
				defaultFunctionNSDeclared = true;
				break;
			case XQ.CollationDeclaration:
				declareDefaultCollation(node);
				break;
			case XQ.BaseURIDeclaration:
				if (baseURIDeclared) {
					throw new QueryException(
							ErrorCode.ERR_BASE_URI_ALREADY_DECLARED,
							"Base URI declared more than once");
				}
				declareBaseURI(child);
				baseURIDeclared = true;
				break;
			case XQ.ConstructionDeclaration:
				if (constructionModeDeclared) {
					throw new QueryException(
							ErrorCode.ERR_CONSTRUCTION_ALREADY_DECLARED,
							"Construction mode declared more than once");
				}
				declareConstructionMode(child);
				constructionModeDeclared = true;
				break;
			case XQ.OrderingModeDeclaration:
				if (orderingModeDeclared) {
					throw new QueryException(
							ErrorCode.ERR_ORDERING_MODE_ALREADY_DECLARED,
							"Ordering mode declared more than once");
				}
				declareOrderingMode(child);
				orderingModeDeclared = true;
				break;
			case XQ.EmptyOrderDeclaration:
				if (emptyOrderDeclared) {
					throw new QueryException(
							ErrorCode.ERR_EMPTY_ORDER_ALREADY_DECLARED,
							"Empty order declared more than once");
				}
				declareEmptyOrder(child);
				emptyOrderDeclared = true;
				break;
			case XQ.CopyNamespacesDeclaration:
				if (copyNamespacesDeclared) {
					throw new QueryException(
							ErrorCode.ERR_COPY_NAMESPACES_ALREADY_DECLARED,
							"Copy namespaces declared more than once");
				}
				declareCopyNamespaces(child);
				copyNamespacesDeclared = true;
				break;
			default:
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
						"Unexpected AST node '%s' of type: %s", child, child
								.getType());
			}
		}
	}

	private String importModule(AST ast) throws QueryException {
		String prefix = null;
		int pos = 0;
		AST child = ast.getChild(pos++);
		if (child.getType() == XQ.Namespace) {
			prefix = child.getChild(0).getValue();
			child = ast.getChild(pos++);
		}
		String nsURI = child.getValue();
		int childCount = ast.getChildCount();
		String locs[] = new String[childCount - pos];
		int locPos = 0;
		while (pos < childCount) {
			locs[locPos++] = child.getChild(0).getValue();
		}
		List<Module> modList = resolver.resolve(nsURI, locs);

		for (Module mod : modList) {
			// import module
			module.importModule(mod);
		}
		return nsURI;
	}

	protected void declareBoundarySpace(AST node) {
		String poilcy = node.getChild(0).getChild(0).getValue();
		module.setBoundarySpaceStrip("strip".equals(poilcy));
	}

	protected void declareDefaultCollation(AST node) {
		String collation = node.getChild(0).getChild(0).getValue();
		module.setDefaultCollation(collation);
	}

	protected void declareBaseURI(AST node) throws QueryException {
		String uri = node.getChild(0).getChild(0).getValue();
		module.setBaseURI(new AnyURI(uri));
	}

	protected void declareConstructionMode(AST node) {
		String mode = node.getChild(0).getChild(0).getValue();
		module.setConstructionModeStrip("strip".equals(mode));
	}

	protected void declareOrderingMode(AST node) {
		String mode = node.getChild(0).getChild(0).getValue();
		module.setOrderingModeOrdered("ordered".equals(mode));
	}

	protected void declareEmptyOrder(AST node) {
		String mode = node.getChild(0).getChild(0).getValue();
		module.setEmptyOrderGreatest("greatest".equals(mode));
	}

	protected void declareCopyNamespaces(AST node) {
		String preserve = node.getChild(0).getChild(0).getValue();
		module.setCopyNSPreserve("preserve".equals(preserve));
		String inherit = node.getChild(0).getChild(0).getValue();
		module.setCopyNSInherit("inherit".equals(inherit));
	}

	protected void declareDefaultNamespace(AST node) {
		String functionNS = node.getChild(0).getChild(0).getValue();
		module.getNamespaces().setDefaultElementNamespace(
				functionNS.isEmpty() ? null : functionNS);
	}

	protected void declareDefaultFunctionNamespace(AST node) {
		String functionNS = node.getChild(0).getChild(0).getValue();
		module.getNamespaces().setDefaultFunctionNamespace(
				functionNS.isEmpty() ? null : functionNS);
	}

	protected void declareNamespace(AST node) throws QueryException {
		String prefix = node.getChild(0).getChild(0).getValue();
		String uri = node.getChild(1).getChild(0).getValue();
		module.getNamespaces().declare(prefix, uri);
	}

	protected void declareOption(AST node) throws QueryException {
		QNm name = module.getNamespaces().qname(node.getChild(0).getValue());
		Str value = new Str(node.getChild(1).getChild(0).getValue());
		module.addOption(name, value);
	}

	protected void declareVariable(AST node) throws QueryException {
		int pos = 0;
		SequenceType type = null;
		AST child = node.getChild(pos++);
		QNm varName = module.getNamespaces().qname(child.getValue());
		child = node.getChild(pos++);
		if (child.getType() == XQ.SequenceType) {
			type = sequenceType(child);
			child = node.getChild(pos++);
		}
		if (child.getType() == XQ.ExternalVariable) {
			Expr expr = (child.getChildCount() == 1) ? expr(child.getChild(0),
					true) : null;
			module.getVariables().declare(
					(ExtVariable) table.declare(varName, expr, type, true));
		} else {
			Expr expr = expr(child, true);
			table.declare(varName, expr, type, false);
		}
	}

	protected void declareFunction(AST node) throws QueryException {
		SequenceType resultType = SequenceType.ITEM_SEQUENCE;
		boolean updating = false;

		int pos = 0;
		AST child = node.getChild(pos++);
		QNm name = module.getNamespaces().qname(child.getValue());
		child = node.getChild(pos++);
		String namespaceURI = name.getNamespaceURI();

		if (namespaceURI == null) {
			namespaceURI = module.getNamespaces().getDefaultFunctionNamespace();

			if (namespaceURI == null) {
				throw new QueryException(ErrorCode.ERR_FUNCTION_DECL_NOT_IN_NS,
						"Declared function '%s' is not in a namespace", name);
			}
			name = new QNm(namespaceURI, null, name.getLocalName());
		}
		if ((namespaceURI.equals(Namespaces.XML_NSURI))
				|| (namespaceURI.equals(Namespaces.XS_NSURI))
				|| (namespaceURI.equals(Namespaces.XSI_NSURI))
				|| (namespaceURI.equals(Namespaces.FN_NSURI))) {
			throw new QueryException(
					ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
					"Declared function '%s' is in illegal namespace '%s'",
					name, namespaceURI);
		}

		if (child.getType() == XQ.SequenceType) {
			resultType = sequenceType(child);
			child = node.getChild(pos++);
		}
		while (child.getType() == XQ.Annotation) {
			// TODO
			// ignore annotation
			child = node.getChild(pos++);
		}
		int noOfParameters = child.getChildCount();
		SequenceType[] parameterTypes = new SequenceType[noOfParameters];
		Binding[] bindings = new Binding[noOfParameters];
		for (int i = 0; i < noOfParameters; i++) {
			AST decl = child.getChild(i);
			SequenceType type = SequenceType.ITEM_SEQUENCE;
			QNm varName = module.getNamespaces().qname(
					decl.getChild(0).getValue());

			if (decl.getChildCount() == 2) {
				type = sequenceType(decl.getChild(1));
			}
			bindings[i] = table.bind(varName, type);
			parameterTypes[i] = type;
		}
		child = node.getChild(pos++);

		// Register function before compiling body to allow recursion
		Signature signature = new Signature(resultType, parameterTypes);
		UDF udf = new UDF(name, signature, updating);
		module.getFunctions().declare(udf);

		udf.setBody(expr(child, !updating));

		for (int i = 0; i < noOfParameters; i++) {
			table.unbind();
		}
		table.resolvePositions();
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
		case XQ.SequenceExpr:
			return sequenceExpr(node);
		case XQ.Int:
			return Int32.parse(node.getValue());
		case XQ.Str:
			return new Str(Whitespace.normalizeXML11(node.getValue()));
		case XQ.Dbl:
			return Dbl.parse(node.getValue());
		case XQ.Dec:
			return new Dec(node.getValue());
		case XQ.QNm:
			if (node.getValue().contains(":")) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
						"Namespaces are not supported yet.");
			}
			return new QNm(node.getValue());
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
		case XQ.FilterExpr:
			return filterExpr(node);
		case XQ.CompElementConstructor:
			return elementExpr(node);
		case XQ.CompAttributeConstructor:
			return attributeExpr(node);
		case XQ.CompCommentConstructor:
			return commentExpr(node);
		case XQ.CompTextConstructor:
			return textExpr(node);
		case XQ.CompDocumentConstructor:
			return documentExpr(node);
		case XQ.CompProcessingInstructionConstructor:
			return piExpr(node);
		case XQ.FunctionCall:
			return functionCall(node);
		case XQ.EmptySequence:
			return new SequenceExpr();
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
		case XQ.Pragma:
			return pragma(node);
		case XQ.ValidateExpr:
			throw new QueryException(
					ErrorCode.ERR_SCHEMA_VALIDATION_FEATURE_NOT_SUPPORTED,
					"Schema validation feature is not supported.");
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"Unexpected AST expr node '%s' of type: %s", node, node
							.getType());
		}
	}

	protected Expr pragma(AST node) throws QueryException {
		return (node.getChildCount() == 1) ? anyExpr(node.getChild(0))
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
		Type targetType = resolveType(aouType.getChild(0).getValue());
		boolean allowEmptySequence = ((type.getChildCount() == 2) && (type
				.getChild(1).getType() == XQ.CardinalityZeroOrOne));
		return new Cast(expr, targetType, allowEmptySequence);
	}

	protected Expr castableExpr(AST node) throws QueryException {
		Expr expr = expr(node.getChild(0), true);
		AST type = node.getChild(1);
		AST aouType = type.getChild(0);
		Type targetType = resolveType(aouType.getChild(0).getValue());
		boolean allowEmptySequence = ((type.getChildCount() == 2) && (type
				.getChild(1).getType() == XQ.CardinalityZeroOrOne));
		return new Castable(expr, targetType, allowEmptySequence);
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
				varName = module.getNamespaces().qname(firstChild.getValue());
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
			varName = module.getNamespaces().qname(firstChild.getValue());
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
		Binding itemBinding = table.bind(Namespaces.FS_DOT, SequenceType.ITEM);
		Binding posBinding = table.bind(Namespaces.FS_POSITION,
				SequenceType.INTEGER);
		Binding sizeBinding = table.bind(Namespaces.FS_LAST,
				SequenceType.INTEGER);

		for (int i = 0; i < noOfPredicates; i++) {
			predicates[i] = expr(node.getChild(1 + i).getChild(0), true);
		}

		table.unbind();
		table.unbind();
		table.unbind();

		return new FilterExpr(expr, predicates[0], itemBinding.isReferenced(),
				posBinding.isReferenced(), sizeBinding.isReferenced());
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
		return new Rename(sourceExpr, targetExpr);
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
			varName = module.getNamespaces().qname(
					current.getChild(0).getValue());
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

		return new TransformExpr(sourceExprs, referenced, modifyExpr,
				returnExpr);
	}

	protected Expr quantifiedExpr(AST node) throws QueryException {
		int childCount = node.getChildCount();
		int pos = 0;
		AST child = node.getChild(pos++);
		boolean someQuantified = (child.getType() == XQ.SomeQuantifier);
		Expr bindingSequenceExpr = quantifiedBindings(new Start(), node, pos);
		Function function;

		if (someQuantified) {
			function = BIT_SOME_FUNC;
		} else {
			function = BIT_EVERY_FUNC;
		}

		return new IfExpr(new FunctionExpr(function, bindingSequenceExpr),
				Bool.TRUE, Bool.FALSE);
	}

	protected Expr quantifiedBindings(Operator in, AST node, int pos)
			throws QueryException {
		AST child = node.getChild(pos++);

		if (child.getType() == XQ.TypedVariableBinding) {
			QNm runVarName = module.getNamespaces().qname(
					child.getChild(0).getValue());
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
			return new ReturnExpr(in, expr(child, true));
		}
	}

	protected Expr functionCall(AST node) throws QueryException {
		int childCount = node.getChildCount();
		QNm name = module.getNamespaces().functionQName(node.getValue());

		if ((name.equals(Functions.FN_POSITION))
				|| (name.equals(Functions.FN_LAST))) {
			if (childCount != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, childCount);
			}
			return table
					.resolve(name.equals(Functions.FN_POSITION) ? Namespaces.FS_POSITION
							: Namespaces.FS_LAST);
		}
		if (name.equals(Functions.FN_TRUE)) {
			if (childCount != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, childCount);
			}
			return Bool.TRUE;
		}
		if (name.equals(Functions.FN_FALSE)) {
			if (childCount != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, childCount);
			}
			return Bool.FALSE;
		}
		if (name.equals(Functions.FN_DEFAULT_COLLATION)) {
			if (childCount != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, childCount);
			}
			return new Str(module.getDefaultCollation());
		}
		if (name.equals(Functions.FN_STATIC_BASE_URI)) {
			if (childCount != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, childCount);
			}
			return module.getBaseURI();
		}

		Function function = module.getFunctions().resolve(name, childCount);
		Expr[] args;

		if (childCount > 0) {
			args = new Expr[childCount];
			for (int i = 0; i < childCount; i++) {
				args[i] = expr(node.getChild(i), true);
			}
		} else if (function.getSignature().defaultIsContextItem()) {
			Expr contextItemRef = table.resolve(Namespaces.FS_DOT);
			args = new Expr[] { contextItemRef };
		} else {
			args = new Expr[0];
		}

		return new FunctionExpr(function, args);
	}

	protected Expr documentExpr(AST node) throws QueryException {
		boolean bind = false;
		Expr[] contentExpr;

		if (node.getChildCount() > 0) {
			Binding binding = table.bind(Namespaces.FS_PARENT,
					SequenceType.ITEM);
			contentExpr = contentSequence(node.getChild(0));
			table.unbind();
			bind = binding.isReferenced();
		} else {
			contentExpr = new Expr[0];
		}

		return new DocumentExpr(contentExpr, bind);
	}

	protected Expr elementExpr(AST node) throws QueryException {
		Expr nameExpr = expr(node.getChild(0), true);
		boolean appendOnly = appendOnly(node);
		boolean bind = false;
		Expr[] contentExpr;

		if (node.getChildCount() > 0) {
			Binding binding = table.bind(Namespaces.FS_PARENT,
					SequenceType.ITEM);
			contentExpr = contentSequence(node.getChild(1));
			table.unbind();
			bind = binding.isReferenced();
		} else {
			contentExpr = new Expr[0];
		}

		return new ElementExpr(nameExpr, contentExpr, bind, appendOnly);
	}

	protected Expr[] contentSequence(AST node) throws QueryException {
		int childCount = node.getChildCount();

		if (childCount == 0) {
			return new Expr[0];
		}

		int size = 0;
		Expr[] subExprs = new Expr[childCount];
		String merged = null;
		boolean first = true;

		for (int i = 0; i < node.getChildCount(); i++) {
			AST child = node.getChild(i);

			if ((child.getType() == XQ.Str)) {
				merged = (merged == null) ? child.getValue() : merged
						+ child.getValue();
			} else {
				if (merged != null) {
					if ((first) && (module.isBoundarySpaceStrip())) {
						merged = Whitespace.trimBoundaryWS(merged, true, false);
					}
					if (!merged.isEmpty()) {
						subExprs[size++] = new Str(merged);
					}
					merged = null;
					first = true;
				}
				subExprs[size++] = expr(child, true);
			}
		}

		if (merged != null) {
			if (module.isBoundarySpaceStrip()) {
				merged = Whitespace.trimBoundaryWS(merged, first, true);
			}
			if (!merged.isEmpty()) {
				subExprs[size++] = new Str(merged);
			}
			merged = null;
		}

		return Arrays.copyOf(subExprs, size);
	}

	protected Expr attributeExpr(AST node) throws QueryException {
		Expr nameExpr = expr(node.getChild(0), true);
		Expr contentExpr = (node.getChildCount() > 1) ? new SequenceExpr(
				contentSequence(node.getChild(1))) : new EmptyExpr();
		return new AttributeExpr(nameExpr, contentExpr, appendOnly(node));
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

	protected Expr variableRefExpr(AST node) throws QueryException {
		return table.resolve(module.getNamespaces().qname(node.getValue()));
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
					"Unexpected AST comparison node '%s' of type: %s", cmpNode,
					cmpNode.getType());
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

	protected Expr pathExpr(AST node) throws QueryException {
		if (node.getChildCount() == 1) {
			return expr(node.getChild(0), true); // TODO Is this possible?
		} else {
			return pathExpr(node, 0);
		}
	}

	protected Expr pathExpr(AST node, int position) throws QueryException {
		AST child = node.getChild(position);
		int childCount = node.getChildCount();

		if (position + 1 == childCount) {
			return expr(child, true);
		} else {
			Expr step = expr(child, true);

			// System.out.println("-----------------");
			// System.out.println("Step " + (position+1) + "/" + childCount +
			// " step " + step);
			boolean sortResult = sortAfterStep(node, position);
			// System.out.println("Sort after next step : " + sortResult);
			// System.out.println("-----------------");

			Binding itemBinding = table.bind(Namespaces.FS_DOT,
					SequenceType.NODE);
			Binding posBinding = table.bind(Namespaces.FS_POSITION,
					SequenceType.INTEGER);
			Binding sizeBinding = table.bind(Namespaces.FS_LAST,
					SequenceType.INTEGER);

			Expr nextStep = pathExpr(node, position + 1);

			table.unbind();
			table.unbind();
			table.unbind();

			boolean bindItem = itemBinding.isReferenced();
			boolean bindPos = posBinding.isReferenced();
			boolean bindSize = sizeBinding.isReferenced();
			return new PathStepExpr(step, nextStep, sortResult, bindItem,
					bindPos, bindSize);
		}
	}

	/*
	 * Sorting after a step is not required if it is a) is a filter expression
	 * (e.g. function call fn:root()), or b) the axis is child, self or
	 * attribute
	 */
	protected boolean sortAfterStep(AST node, int position)
			throws QueryException {
		AST child = node.getChild(position);

		if (child.getType() != XQ.StepExpr) {
			return true;
		}

		Axis axis = axis(child.getChild(0).getChild(0)).getAxis();
		return ((axis != Axis.CHILD) && (axis != Axis.ATTRIBUTE) && (axis != Axis.SELF));
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

		int noOfPredicates = Math.max(node.getChildCount() - 2, 0);
		Expr[] predicates = new Expr[noOfPredicates];
		KindTest test = itemTest(child, axis.getAxis());
		Binding itemBinding = table.bind(Namespaces.FS_DOT, SequenceType.NODE);
		Binding posBinding = table.bind(Namespaces.FS_POSITION,
				SequenceType.INTEGER);
		Binding sizeBinding = table.bind(Namespaces.FS_LAST,
				SequenceType.INTEGER);

		for (int i = 0; i < noOfPredicates; i++) {
			predicates[i] = expr(node.getChild(2 + i).getChild(0), true);
		}

		table.unbind();
		table.unbind();
		table.unbind();

		return new StepExpr(axis, test, in, predicates, itemBinding
				.isReferenced(), posBinding.isReferenced(), sizeBinding
				.isReferenced());
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
		Type type = resolveType(node.getChild(0).getValue());
		return new AtomicType(type);
	}

	protected KindTest itemTest(AST node, Axis axis) throws QueryException {
		if (node.getType() == XQ.NameTest) {
			return nameTest(node, axis);
		} else {
			return kindTest(node);
		}
	}

	protected KindTest kindTest(AST node) throws QueryException {
		switch (node.getType()) {
		case XQ.KindTestAnyKind:
			return AnyKindType.ANY_NODE;
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
			return new PIType();
		case XQ.KindTestSchemaElement:
			return schemaElementTest(node);
		case XQ.KindTestSchemaAttribute:
			return schemaAttributeTest(node);
		default:
			throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
					"KindTest translation not implemented yet.");
		}
	}

	protected SchemaAttributeType schemaAttributeTest(AST child)
			throws QueryException {
		return new SchemaAttributeType(qNameOrWildcard(child.getChild(0)));
	}

	protected SchemaElementType schemaElementTest(AST child)
			throws QueryException {
		return new SchemaElementType(qNameOrWildcard(child.getChild(0)));
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
					resolveType(child.getChild(1).getValue()));
	}

	protected ElementType elementTest(AST child) throws QueryException {
		if (child.getChildCount() == 0)
			return new ElementType();
		else if (child.getChildCount() == 1)
			return new ElementType(qNameOrWildcard(child.getChild(0)));
		else
			return new ElementType(qNameOrWildcard(child.getChild(0)),
					resolveType(child.getChild(1).getValue()));
	}

	protected QNm qNameOrWildcard(AST name) throws QueryException {
		return (name.getType() == XQ.Wildcard) ? null : module.getNamespaces()
				.qname(name.getValue());
	}

	protected KindTest nameTest(AST child, Axis axis) throws QueryException {
		AST name = child.getChild(0);
		if (axis != Axis.ATTRIBUTE)
			return new ElementType(qNameOrWildcard(name));
		else
			return new AttributeType(qNameOrWildcard(name));
	}

	protected Type resolveType(String text) throws QueryException {
		QNm qname = module.getNamespaces().qname(text);
		return module.getTypes().resolveType(qname);
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
		Expr opExpr = new ReturnExpr(cb.operator, returnExpr);
		return opExpr;
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
		QNm posVarName = module.getNamespaces().qname(
				countVarDecl.getChild(0).getValue());
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
				collation = modifier.getChild(0).getValue();
			}
		}
		return new OrderModifier(asc, emptyLeast, collation);
	}

	protected ClauseBinding groupByClause(AST node, ClauseBinding in)
			throws QueryException {
		int groupSpecCount = node.getChildCount();
		GroupBy groupBy = new GroupBy(in.operator, groupSpecCount, false);
		for (int i = 0; i < groupSpecCount; i++) {
			String grpVarName = node.getChild(i).getChild(0).getValue();
			table.resolve(module.getNamespaces().qname(grpVarName), groupBy
					.group(i));
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
		QNm letVarName = module.getNamespaces().qname(
				letVarDecl.getChild(0).getValue());
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
		QNm runVarName = module.getNamespaces().qname(
				runVarDecl.getChild(0).getValue());
		SequenceType runVarType = SequenceType.ITEM_SEQUENCE;
		if (runVarDecl.getChildCount() == 2) {
			runVarType = sequenceType(runVarDecl.getChild(1));
		}
		AST posBindingOrSourceExpr = forClause.getChild(forClausePos++);

		if (posBindingOrSourceExpr.getType() == XQ.TypedVariableBinding) {
			posVarName = module.getNamespaces().qname(
					posBindingOrSourceExpr.getChild(0).getValue());
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
