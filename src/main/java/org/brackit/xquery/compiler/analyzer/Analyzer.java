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
package org.brackit.xquery.compiler.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.ModuleResolver;
import org.brackit.xquery.compiler.Target;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.expr.DeclVariable;
import org.brackit.xquery.expr.DefaultCtxItem;
import org.brackit.xquery.expr.Variable;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.module.DecimalFormat;
import org.brackit.xquery.module.Functions;
import org.brackit.xquery.module.LibraryModule;
import org.brackit.xquery.module.MainModule;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.module.Variables;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.util.log.Logger;
import org.brackit.xquery.xdm.Function;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.XMLChar;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AnyNodeType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.AttributeType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.CommentType;
import org.brackit.xquery.xdm.type.DocumentType;
import org.brackit.xquery.xdm.type.ElementType;
import org.brackit.xquery.xdm.type.FunctionType;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.NSNameWildcardTest;
import org.brackit.xquery.xdm.type.NSWildcardNameTest;
import org.brackit.xquery.xdm.type.PIType;
import org.brackit.xquery.xdm.type.SequenceType;
import org.brackit.xquery.xdm.type.TextType;

/**
 * Straight-forward, recursive descent parser.
 * 
 * @author Sebastian Baechle
 * 
 */
public class Analyzer {

	private enum DefaultNS {
		EMPTY, FUNCTION, ELEMENT_OR_TYPE, PRAGMA
	}

	private interface Deferment {
		void process() throws QueryException;
	}

	private static final Logger log = Logger.getLogger(Analyzer.class);

	private final ModuleResolver resolver;
	private final List<Target> targets;
	private final VarScopes variables;

	// AST subtrees that are checked deferred
	private List<Deferment> deferred;
	private StaticContext ctx;
	private Module module;

	public Analyzer(ModuleResolver resolver, AST xquery) throws QueryException {
		this.resolver = resolver;
		this.variables = new VarScopes();
		this.targets = new ArrayList<Target>();
		this.deferred = new ArrayList<Deferment>();
		module(xquery.getChild(0));
	}

	public Module getModule() {
		return module;
	}

	public List<Target> getTargets() {
		return targets;
	}

	private void module(AST module) throws QueryException {
		if (module.getType() == XQ.LibraryModule) {
			libraryModule(module);
		} else {
			mainModule(module);
		}
	}

	private void libraryModule(AST module) throws QueryException {
		LibraryModule lm = new LibraryModule();
		this.module = lm;
		this.ctx = lm.getStaticContext();
		AST ns = module.getChild(0);
		String prefix = ns.getChild(0).getStringValue();
		String uri = ns.getChild(1).getStringValue();
		lm.setTargetNS(uri);
		// TODO check prefix according to XQuery 4.2
		lm.getStaticContext().getNamespaces().declare(prefix, uri);
		if (module.getChildCount() == 2) {
			prolog(module.getChild(1));
		}
	}

	private void mainModule(AST module) throws QueryException {
		MainModule mm = new MainModule();
		this.module = mm;
		this.ctx = mm.getStaticContext();
		AST prologOrBody = module.getChild(0);
		if (prolog(prologOrBody)) {
			prologOrBody = module.getChild(1);
		}
		queryBody(prologOrBody);
		targets.add(new Target(ctx, prologOrBody.getChild(0), mm, true));
	}

	private boolean prolog(AST prolog) throws QueryException {
		if (prolog.getType() != XQ.Prolog) {
			return false;
		}
		for (int i = 0; i < prolog.getChildCount(); i++) {
			AST decl = prolog.getChild(i);
			boolean ok = (defaultNamespaceDecl(decl) || setter(decl)
					|| namespaceDecl(decl) || importDecl(decl)
					|| contextItemDecl(decl) || annotatedDecl(decl) || optionDecl(decl));
		}
		for (Deferment d : deferred) {
			d.process();
		}
		return true;
	}

	private boolean defaultNamespaceDecl(AST decl) throws QueryException {
		if (decl.getType() == XQ.DefaultElementNamespace) {
			String uri = decl.getChild(0).getStringValue();
			ctx.getNamespaces().setDefaultElementNamespace(uri);
			return true;
		} else if (decl.getType() == XQ.DefaultFunctionNamespace) {
			String uri = decl.getChild(0).getStringValue();
			ctx.getNamespaces().setDefaultFunctionNamespace(uri);
			return true;
		} else {
			return false;
		}
	}

	private boolean setter(AST decl) throws QueryException {
		return (boundarySpaceDecl(decl) || defaultCollationDecl(decl)
				|| baseURIDecl(decl) || constructionDecl(decl)
				|| orderingModeDecl(decl) || emptyOrderDecl(decl)
				/* Begin XQuery Update Facility 1.0 */
				|| revalidationDecl(decl)
				/* Begin XQuery Update Facility 1.0 */
				|| copyNamespacesDecl(decl) || decimalFormatDecl(decl));
	}

	boolean declaredCo = false;

	private boolean boundarySpaceDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.BoundarySpaceDeclaration) {
			return false;
		}
		if (declaredCo) {
			throw new QueryException(
					ErrorCode.ERR_BOUNDARY_SPACE_ALREADY_DECLARED,
					"Boundary-space already declared");
		}
		AST mode = decl.getChild(0);
		if (mode.getType() == XQ.BoundarySpaceModePreserve) {
			ctx.setBoundarySpaceStrip(false);
		} else {
			ctx.setBoundarySpaceStrip(true);
		}
		declaredCo = true;
		return true;
	}

	private boolean defaultCollationDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.BoundarySpaceDeclaration) {
			return false;
		}
		String col = decl.getChild(0).getStringValue();
		if (!col.equals(StaticContext.UNICODE_COLLATION)) {
			throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION,
					"Unsupported collation: %s", col);
		}

		ctx.setDefaultCollation(col);
		return true;
	}

	private boolean declaredBaseURI = false;

	private boolean baseURIDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.BaseURIDeclaration) {
			return false;
		}
		if (declaredBaseURI) {
			throw new QueryException(ErrorCode.ERR_BASE_URI_ALREADY_DECLARED,
					"Base URI already declared");
		}
		String uri = decl.getChild(0).getStringValue();
		ctx.setBaseURI(new AnyURI(uri));
		declaredBaseURI = true;
		return true;
	}

	private boolean declaredConstructionMode = false;

	private boolean constructionDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.ConstructionDeclaration) {
			return false;
		}
		if (declaredConstructionMode) {
			throw new QueryException(ErrorCode.ERR_BASE_URI_ALREADY_DECLARED,
					"Base URI already declared");
		}
		AST mode = decl.getChild(0);
		if (mode.getType() == XQ.ConstructionModePreserve) {
			ctx.setConstructionModeStrip(false);
		} else {
			ctx.setConstructionModeStrip(true);
		}
		declaredConstructionMode = true;
		return true;
	}

	private boolean declaredOrderingMode = false;

	private boolean orderingModeDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.ConstructionDeclaration) {
			return false;
		}
		if (declaredOrderingMode) {
			throw new QueryException(
					ErrorCode.ERR_ORDERING_MODE_ALREADY_DECLARED,
					"Ordering mode already declared");
		}
		AST mode = decl.getChild(0);
		if (mode.getType() == XQ.OrderingModeOrdered) {
			ctx.setOrderingModeOrdered(true);
		} else {
			ctx.setOrderingModeOrdered(false);
		}
		declaredOrderingMode = true;
		return true;
	}

	private boolean declaredEmptyOrder = false;

	private boolean emptyOrderDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.ConstructionDeclaration) {
			return false;
		}
		if (declaredEmptyOrder) {
			throw new QueryException(
					ErrorCode.ERR_EMPTY_ORDER_ALREADY_DECLARED,
					"Empty order mode already declared");
		}
		AST mode = decl.getChild(0);
		if (mode.getType() == XQ.EmptyOrderModeGreatest) {
			ctx.setEmptyOrderGreatest(true);
		} else {
			ctx.setEmptyOrderGreatest(false);
		}
		declaredEmptyOrder = true;
		return true;
	}

	// Begin XQuery Update Facility 1.0

	private boolean revalidationDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.RevalidationDeclaration) {
			return false;
		}
		// TODO
		throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
	}

	// End XQuery Update Facility 1.0

	private boolean declaredCopyNamespaces = false;

	private boolean copyNamespacesDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.CopyNamespacesDeclaration) {
			return false;
		}
		if (declaredCopyNamespaces) {
			throw new QueryException(
					ErrorCode.ERR_COPY_NAMESPACES_ALREADY_DECLARED,
					"Copy-namespaces already declared");
		}

		AST mode = decl.getChild(0);
		if (preserveMode(mode)) {
			if (decl.getChildCount() == 2) {
				inheritMode(mode);
			}
		} else {
			inheritMode(mode);
		}
		declaredCopyNamespaces = true;
		return true;
	}

	private boolean preserveMode(AST mode) throws QueryException {
		if (mode.getType() == XQ.CopyNamespacesPreserveModePreserve) {
			ctx.setCopyNSPreserve(true);
		} else if (mode.getType() == XQ.CopyNamespacesPreserveModeNoPreserve) {
			ctx.setCopyNSPreserve(false);
		} else {
			return false;
		}
		return true;
	}

	private boolean inheritMode(AST mode) throws QueryException {
		if (mode.getType() == XQ.CopyNamespacesInheritModeInherit) {
			ctx.setCopyNSInherit(true);
		} else if (mode.getType() == XQ.CopyNamespacesInheritModeNoInherit) {
			ctx.setCopyNSInherit(false);
		} else {
			return false;
		}
		return true;
	}

	private boolean declaredDecimalFormatDefault = false;

	private boolean decimalFormatDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.DecimalFormatDeclaration) {
			return false;
		}
		AST format = decl.getChild(0);
		if (format.getType() == XQ.DecimalFormatDefault) {
			if (declaredDecimalFormatDefault) {
				throw new QueryException(
						ErrorCode.ERR_DECIMAL_FORMAT_ALREADY_DECLARED,
						"Default decimal-format already declared");
			}
			declaredDecimalFormatDefault = true;
			DecimalFormat df = new DecimalFormat();
			for (int i = 1; i < decl.getChildCount(); i++) {
				dfProperty(df, decl.getChild(i));
			}
			ctx.setDefaultDecimalFormat(df);
		} else {
			QNm name = (QNm) format.getValue();
			// expand and update AST
			name = expand(name, DefaultNS.EMPTY);
			format.setValue(name);
			if (ctx.getDecimalFormat(name) != null) {
				throw new QueryException(
						ErrorCode.ERR_DECIMAL_FORMAT_ALREADY_DECLARED,
						"Decimal-format already declared: %s", name);
			}
			DecimalFormat df = new DecimalFormat();
			for (int i = 1; i < decl.getChildCount(); i++) {
				dfProperty(df, decl.getChild(i));
			}
			ctx.setDecimalFormat(name, df);
		}
		return true;
	}

	private boolean dfProperty(DecimalFormat df, AST dfProperty) {
		int type = dfProperty.getType();
		if (type == XQ.DecimalFormatPropertyDecimalSeparator) {
			df.setDecimalSeparator(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyGroupingSeparator) {
			df.setGroupingSeparator(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyInfinity) {
			df.setInfinity(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyMinusSign) {
			df.setMinusSign(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyNaN) {
			df.setNaN(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyPercent) {
			df.setPercent(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyPerMille) {
			df.setPerMille(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyZeroDigit) {
			df.setZeroDigit(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyDigit) {
			df.setDigitSign(dfProperty.getStringValue());
		} else if (type == XQ.DecimalFormatPropertyPatternSeparator) {
			df.setPatternSeparator(dfProperty.getStringValue());
		} else {
			return false;
		}
		return true;
	}

	private boolean namespaceDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.NamespaceDeclaration) {
			return false;
		}
		String prefix = decl.getChild(0).getStringValue();
		String uri = decl.getChild(1).getStringValue();

		if ((Namespaces.XML_PREFIX.equals(prefix))
				|| (Namespaces.XMLNS_PREFIX.equals(prefix))) {
			throw new QueryException(
					ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"The prefix '%s' must not be used in a namespace declaration",
					prefix);
		}
		if ((Namespaces.XML_NSURI.equals(uri))
				|| (Namespaces.XMLNS_NSURI.equals(uri))) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"The URI '%s' must not be used in a namespace declaration",
					uri);
		}
		Namespaces ns = ctx.getNamespaces();
		if ((ns.resolve(prefix) != null) && (!ns.isPredefined(prefix))) {
			throw new QueryException(
					ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
					"Namespace prefix '%s' is already bound to '%s", prefix,
					uri);
		}
		ns.declare(prefix, uri);
		return true;
	}

	private boolean importDecl(AST decl) throws QueryException {
		return (schemaImport(decl) || moduleImport(decl));
	}

	private boolean schemaImport(AST decl) throws QueryException {
		if (decl.getType() != XQ.SchemaImport) {
			return false;
		}
		throw new QueryException(
				ErrorCode.ERR_SCHEMA_IMPORT_FEATURE_NOT_SUPPORTED,
				"Schema import is not supported.");
	}

	private boolean moduleImport(AST decl) throws QueryException {
		if (decl.getType() != XQ.ModuleImport) {
			return false;
		}
		AST ns = decl.getChild(0);
		String uri;
		if (ns.getType() == XQ.NamespaceDeclaration) {
			String prefix = ns.getChild(0).getStringValue();
			uri = ns.getChild(1).getStringValue();
			if ((Namespaces.XML_PREFIX.equals(prefix))
					|| (Namespaces.XMLNS_PREFIX.equals(prefix))) {
				throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
						"The prefix '%s' must not be used for a module import",
						prefix);
			}
			if (ctx.getNamespaces().resolve(prefix) != null) {
				throw new QueryException(
						ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
						"Namespace prefix '%s' is already bound to '%s",
						prefix, uri);
			}
			// declare module namespace prefix
			ctx.getNamespaces().declare(prefix, uri);
		} else {
			uri = ns.getStringValue();
		}
		if (uri.isEmpty()) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"Module import with empty target namespace");
		}
		for (Module m : module.getImportedModules()) {
			if (m.getTargetNS().equals(uri)) {
				throw new QueryException(
						ErrorCode.ERR_MULTIPLE_IMPORTS_IN_SAME_NS,
						"Multiple imports of module namespace: %s", uri);
			}
		}
		String[] locs = new String[decl.getChildCount() - 1];
		for (int i = 0; i < locs.length; i++) {
			locs[i] = decl.getChild(i + 1).getStringValue();
		}
		List<Module> modules = resolver.resolve(uri, locs);
		if (modules.isEmpty()) {
			throw new QueryException(ErrorCode.ERR_SCHEMA_OR_MODULE_NOT_FOUND,
					"Module '%s' not found", uri);
		}
		Variables lvars = module.getVariables();
		Functions lfuns = ctx.getFunctions();
		for (Module m : modules) {
			// check variables and functions to import
			Variables ivars = m.getVariables();
			Functions ifuns = m.getStaticContext().getFunctions();
			checkImports(lvars, ivars, lfuns, ifuns);
			// do import
			lvars.importVariables(ivars);
			lfuns.importFunctions(ifuns);
			module.importModule(m);
		}
		return true;
	}

	private void checkImports(Variables lvars, Variables ivars,
			Functions lfuns, Functions ifuns) throws QueryException {
		for (Variable ivar : ivars.getDeclaredVariables()) {
			if (lvars.isDeclared(ivar.getName())) {
				throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
						"Import variable $%s has already been declared", ivar
								.getName());
			}
		}
		Map<QNm, Function[]> ifunMap = ifuns.getDeclaredFunctions();
		for (Entry<QNm, Function[]> ifun : ifunMap.entrySet()) {
			QNm name = ifun.getKey();
			Function[] ifu = ifun.getValue();
			for (int i = 0; i < ifu.length; i++) {
				int argc = ifu[i].getSignature().getParams().length;
				if (lfuns.resolve(name, argc) != null) {
					throw new QueryException(
							ErrorCode.ERR_MULTIPLE_FUNCTION_DECLARATIONS,
							"Found multiple declarations of function %s",
							ifu[i].getName());
				}
			}
		}
	}

	private boolean contextItemDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.ContextItemDeclaration) {
			return false;
		}
		DefaultCtxItem var = (DefaultCtxItem) module.getVariables().resolve(
				Namespaces.FS_DOT);
		ItemType type = itemType(decl.getChild(0));
		var.setType(type);
		AST defaultValue = null;
		AST extVarOrDefaultVal = decl.getChild(1);
		if (extVarOrDefaultVal.getType() == XQ.ExternalVariable) {
			if (decl.getChildCount() == 3) {
				defaultValue = decl.getChild(2);
			}
		} else {
			var.setExternal(false);
			defaultValue = extVarOrDefaultVal;
		}
		if (defaultValue != null) {
			// TODO check circle!!!
			expr(defaultValue);
			targets.add(new Target(ctx, defaultValue, var, false));
		}

		return true;
	}

	private boolean annotatedDecl(AST decl) throws QueryException {
		return (varDecl(decl) || functionDecl(decl));
	}

	private boolean varDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.TypedVariableDeclaration) {
			return false;
		}
		boolean declaredPrivateOrPublic = false;
		int pos = 0;
		AST child = decl.getChild(pos++);
		while (child.getType() == XQ.Annotation) {
			String annotation = child.getStringValue();
			if ((annotation == "%public") || (annotation == "%private")) {
				if (declaredPrivateOrPublic) {
					throw new QueryException(
							ErrorCode.ERR_VAR_PRIVATE_OR_PUBLIC_ALREADY_DECLARED,
							"Variable has already been declared private or public");
				}
				declaredPrivateOrPublic = true;
			}
			// TODO process annotations
			log.warn("Ingoring variable annotation " + annotation);
			child = decl.getChild(pos++);
		}
		QNm name = (QNm) child.getValue();
		// expand and update AST
		name = expand(name, DefaultNS.EMPTY);
		child.setValue(name);
		if (module.getVariables().isDeclared(name)) {
			throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
					"Variable $%s has already been declared", name);
		}
		String targetNS = module.getTargetNS();
		if ((targetNS != null) && (!targetNS.equals(name.getNamespaceURI()))) {
			throw new QueryException(
					ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
					"Declared variable $%s is not in library module namespace: %s",
					name, targetNS);
		}
		child = decl.getChild(pos++);
		SequenceType type;
		boolean external = false;
		if (child.getType() == XQ.SequenceType) {
			type = typeDeclaration(child);
			child = decl.getChild(pos++);
		} else {
			type = new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany);
		}

		AST defaultValue = null;
		if (child.getType() == XQ.ExternalVariable) {
			external = true;
			if (pos < decl.getChildCount()) {
				defaultValue = decl.getChild(pos++);
			}
		} else {
			defaultValue = child;
		}

		DeclVariable var = module.getVariables().declare(name, type, external);

		if (defaultValue != null) {
			// defer default value because it
			// might depend on other variables
			final AST tmp = defaultValue;
			deferred.add(new Deferment() {
				@Override
				public void process() throws QueryException {
					exprSingle(tmp);
				}
			});
			targets.add(new Target(ctx, defaultValue, var, false));
		}

		return true;
	}

	private boolean functionDecl(AST decl) throws QueryException {
		if (decl.getType() != XQ.FunctionDecl) {
			return false;
		}

		boolean declaredPrivateOrPublic = false;
		// Begin XQuery Update 1.0
		boolean updating = false;
		// End XQuery Update 1.0
		int pos = 0;
		AST child = decl.getChild(pos++);
		while (child.getType() == XQ.Annotation) {
			String annotation = child.getStringValue();
			if ((annotation == "%public") || (annotation == "%private")) {
				if (declaredPrivateOrPublic) {
					throw new QueryException(
							ErrorCode.ERR_FUN_PRIVATE_OR_PUBLIC_ALREADY_DECLARED,
							"Function has already been declared private or public");
				}
				declaredPrivateOrPublic = true;
			}
			// TODO process annotations
			log.warn("Ingoring function annotation " + annotation);
			child = decl.getChild(pos++);
		}

		// function name
		QNm name = (QNm) child.getValue();
		// expand and update AST
		name = expand(name, DefaultNS.FUNCTION);
		child.setValue(name);
		if (name.getNamespaceURI().isEmpty()) {
			throw new QueryException(ErrorCode.ERR_FUNCTION_DECL_NOT_IN_NS,
					"Function %s is not in a namespace", name);
		}

		String targetNS = module.getTargetNS();
		if ((targetNS != null) && (!targetNS.equals(name.getNamespaceURI()))) {
			throw new QueryException(
					ErrorCode.ERR_FUN_OR_VAR_NOT_IN_TARGET_NS,
					"Declared function %s is not in library module namespace: %s",
					name, targetNS);
		}

		String uri = name.getNamespaceURI();
		if ((uri.equals(Namespaces.XML_NSURI))
				|| (uri.equals(Namespaces.XS_NSURI))
				|| (uri.equals(Namespaces.XSI_NSURI))
				|| (uri.equals(Namespaces.FN_NSURI))
				|| (uri.equals(Namespaces.FNMATH_NSURI))) {
			throw new QueryException(
					ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
					"Declared function %s is in illegal namespace: %s", name,
					uri);
		}

		// parameters
		int noOfParameters = (decl.getChildCount() - pos - 2);
		final AST[] params = new AST[noOfParameters];
		QNm[] pNames = new QNm[noOfParameters];
		SequenceType[] pTypes = new SequenceType[noOfParameters];
		for (int i = 0; i < noOfParameters; i++) {
			child = decl.getChild(pos++);
			params[i] = child;
			pNames[i] = (QNm) child.getChild(0).getValue();
			// expand and update AST
			pNames[i] = expand(pNames[i], DefaultNS.EMPTY);
			child.getChild(0).setValue(pNames[i]);
			for (int j = 0; j < i; j++) {
				if (pNames[i].atomicCmp(pNames[j]) == 0) {
					throw new QueryException(
							ErrorCode.ERR_DUPLICATE_FUN_PARAMETER,
							"Duplicate parameter in declared function %s: %s",
							name, pNames[j]);
				}
			}
			if (child.getChildCount() == 2) {
				pTypes[i] = sequenceType(child.getChild(1));
			} else {
				pTypes[i] = SequenceType.ITEM_SEQUENCE;
			}
		}

		// result type
		child = decl.getChild(pos++);
		SequenceType resultType = sequenceType(child);
		child = decl.getChild(pos++);

		// register function beforehand to support recursion
		Signature signature = new Signature(resultType, pTypes);
		UDF udf = new UDF(name, signature, updating);
		ctx.getFunctions().declare(udf);

		// defer function body because functions
		// can depend on declared variables and other
		// declared or imported functions
		final AST body = child;
		deferred.add(new Deferment() {
			@Override
			public void process() throws QueryException {
				openScope();
				for (AST param : params) {
					QNm name = (QNm) param.getChild(0).getValue();
					name = bind(name);
					param.getChild(0).setValue(name);
				}
				offerScope();
				functionBody(body);
				closeScope();
			}
		});

		targets.add(new Target(ctx, decl, udf, udf.isUpdating()));

		return true;
	}

	private void functionBody(AST body) throws QueryException {
		enclosedExpr(body);
	}

	private boolean optionDecl(AST option) throws QueryException {
		if (option.getType() != XQ.OptionDeclaration) {
			return false;
		}
		QNm name = (QNm) option.getChild(0).getValue();
		// expand and update AST
		name = expand(name, DefaultNS.EMPTY);
		option.getChild(0).setValue(name);
		Str value = (Str) option.getChild(1).getValue();
		module.addOption(name, value);
		return true;
	}

	private boolean queryBody(AST body) throws QueryException {
		if (body.getType() != XQ.QueryBody) {
			return false;
		}
		expr(body.getChild(0));
		return true;
	}

	private boolean expr(AST expr) throws QueryException {
		if (expr.getType() != XQ.SequenceExpr) {
			exprSingle(expr);
		} else {
			for (int i = 0; i < expr.getChildCount(); i++) {
				exprSingle(expr.getChild(i));
			}
		}
		return true;
	}

	private boolean exprSingle(AST expr) throws QueryException {
		return (flowrExpr(expr) || quantifiedExpr(expr) || switchExpr(expr)
				|| typeswitchExpr(expr) || ifExpr(expr) || tryCatchExpr(expr) ||
				/* Begin XQuery Update Facility 1.0 */
				insertExpr(expr) || deleteExpr(expr) || renameExpr(expr)
				|| replaceExpr(expr) || transformExpr(expr) ||
		/* End XQuery Update Facility 1.0 */
		orExpr(expr));
	}

	// Begin XQuery Update Facility 1.0
	private boolean insertExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.InsertExpr) {
			return false;
		}
		AST src = expr.getChild(1);
		exprSingle(src);
		AST target = expr.getChild(2);
		exprSingle(target);
		return true;
	}

	private boolean deleteExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.DeleteExpr) {
			return false;
		}
		AST target = expr.getChild(0);
		exprSingle(target);
		return true;
	}

	private boolean renameExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.RenameExpr) {
			return false;
		}
		AST target = expr.getChild(0);
		exprSingle(target);
		AST newName = expr.getChild(1);
		exprSingle(newName);
		return true;
	}

	private boolean replaceExpr(AST expr) throws QueryException {
		if (expr.getType() == XQ.ReplaceValueExpr) {
			AST target = expr.getChild(0);
			exprSingle(target);
			AST replacement = expr.getChild(1);
			exprSingle(replacement);
		} else if (expr.getType() == XQ.ReplaceNodeExpr) {
			AST target = expr.getChild(0);
			exprSingle(target);
			AST replacement = expr.getChild(1);
			exprSingle(replacement);
		} else {
			return false;
		}
		return true;
	}

	private boolean transformExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.TransformExpr) {
			return false;
		}
		openScope();
		int pos = 0;
		while (pos < expr.getChildCount() - 2) {
			AST binding = expr.getChild(pos++);
			QNm name = (QNm) binding.getChild(0).getValue();
			// expand, bind and update AST
			name = expand(name, DefaultNS.EMPTY);
			name = bind(name);
			binding.getChild(0).setValue(name);
			exprSingle(binding.getChild(1));
		}
		offerScope();
		AST modify = expr.getChild(pos++);
		exprSingle(modify);
		AST ret = expr.getChild(pos);
		exprSingle(ret);
		closeScope();
		return true;
	}

	// End XQuery Update Facility 1.0

	private boolean flowrExpr(AST flwor) throws QueryException {
		if (flwor.getType() != XQ.FlowrExpr) {
			return false;
		}
		int scopeCount = scopeCount();
		initialClause(flwor.getChild(0));
		for (int i = 1; i < flwor.getChildCount() - 1; i++) {
			intermediateClause(flwor.getChild(i));
		}
		exprSingle(flwor.getChild(flwor.getChildCount() - 1).getChild(0));
		for (int i = scopeCount(); i > scopeCount; i--) {
			closeScope();
		}
		return true;
	}

	private boolean initialClause(AST clause) throws QueryException {
		return (forClause(clause) || letClause(clause) || windowClause(clause));
	}

	private boolean forClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.ForClause) {
			return false;
		}
		forBinding(clause);
		return true;
	}

	private boolean forBinding(AST clause) throws QueryException {
		openScope();
		int pos = 0;
		AST child = clause.getChild(pos++);
		typedVarBinding(child);
		child = clause.getChild(pos++);
		if (child.getType() == XQ.AllowingEmpty) {
			child = clause.getChild(pos++);
		}
		if (child.getType() == XQ.TypedVariableBinding) {
			positionalVar(child);
			child = clause.getChild(pos++);
		}
		exprSingle(child);
		offerScope();
		return true;
	}

	private boolean typedVarBinding(AST binding) throws QueryException {
		QNm name = (QNm) binding.getChild(0).getValue();
		// expand, bind and update AST
		name = expand(name, DefaultNS.EMPTY);
		name = bind(name);
		binding.getChild(0).setValue(name);
		SequenceType stype;
		if (binding.getChildCount() >= 2) {
			stype = typeDeclaration(binding.getChild(1));
		} else {
			stype = new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany);
		}
		return true;
	}

	private SequenceType typeDeclaration(AST decl) throws QueryException {
		return sequenceType(decl);
	}

	private boolean positionalVar(AST binding) throws QueryException {
		QNm name = (QNm) binding.getChild(0).getValue();
		// expand, bind and update AST
		name = expand(name, DefaultNS.EMPTY);
		name = bind(name);
		binding.getChild(0).setValue(name);
		SequenceType stype = new SequenceType(AtomicType.INR, Cardinality.One);
		return true;
	}

	private boolean letClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.LetClause) {
			return false;
		}
		letBinding(clause);
		return true;
	}

	private boolean letBinding(AST clause) throws QueryException {
		openScope();
		typedVarBinding(clause.getChild(0));
		exprSingle(clause.getChild(1));
		offerScope();
		return true;
	}

	private boolean windowClause(AST clause) throws QueryException {
		return (tumblingWindowClause(clause) || slidingWindowClause(clause));
	}

	private boolean tumblingWindowClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.TumblingWindowClause) {
			return false;
		}
		openScope();
		typedVarBinding(clause.getChild(0));
		exprSingle(clause.getChild(1));
		windowStartCondition(clause.getChild(2));
		if (clause.getChildCount() >= 4) {
			windowEndCondition(clause.getChild(3));
		}
		// TODO check all variable names are distinct
		offerScope();
		return true;
	}

	private boolean windowStartCondition(AST cond) throws QueryException {
		openScope();
		windowVars(cond.getChild(0));
		offerScope();
		exprSingle(cond.getChild(1));
		closeScope();
		return true;
	}

	private boolean windowEndCondition(AST cond) throws QueryException {
		openScope();
		windowVars(cond.getChild(0));
		offerScope();
		exprSingle(cond.getChild(1));
		closeScope();
		return true;
	}

	private void windowVars(AST windowVars) throws QueryException {
		for (int i = 0; i < windowVars.getChildCount(); i++) {
			QNm name = (QNm) windowVars.getChild(i).getChild(0).getValue();
			// expand, bind and update AST
			name = expand(name, DefaultNS.EMPTY);
			name = bind(name);
			windowVars.getChild(i).getChild(0).setValue(name);
		}
	}

	private boolean slidingWindowClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.SlidingWindowClause) {
			return false;
		}
		openScope();
		typedVarBinding(clause.getChild(0));
		exprSingle(clause.getChild(1));
		windowStartCondition(clause.getChild(2));
		windowEndCondition(clause.getChild(3));
		// TODO check all variable names are distinct
		offerScope();
		return true;
	}

	private boolean intermediateClause(AST clause) throws QueryException {
		return (initialClause(clause) || whereClause(clause)
				|| groupByClause(clause) || orderByClause(clause) || countClause(clause));
	}

	private boolean whereClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.WhereClause) {
			return false;
		}
		exprSingle(clause.getChild(0));
		return true;
	}

	private boolean groupByClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.GroupByClause) {
			return false;
		}
		for (int i = 0; i < clause.getChildCount(); i++) {
			AST groupBySpec = clause.getChild(i);
			QNm name = (QNm) groupBySpec.getChild(0).getValue();
			// expand resolve and update AST
			name = expand(name, DefaultNS.EMPTY);
			name = resolve(name);
			groupBySpec.getChild(0).setValue(name);
			// TODO check for err:XQST0094
			if (groupBySpec.getChildCount() >= 2) {
				String col = groupBySpec.getChild(1).getStringValue();
				if (!col.equals(StaticContext.UNICODE_COLLATION)) {
					throw new QueryException(
							ErrorCode.ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE,
							"Unknown collation in group-by clause: %s", col);
				}
			}
		}
		return true;
	}

	private boolean orderByClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.OrderByClause) {
			return false;
		}
		for (int i = 0; i < clause.getChildCount(); i++) {
			AST orderBySpec = clause.getChild(i);
			exprSingle(orderBySpec.getChild(0));
			for (int j = 1; j < orderBySpec.getChildCount(); j++) {
				if (orderBySpec.getChild(j).getType() == XQ.Collation) {
					String col = orderBySpec.getChild(j).getStringValue();
					if (!col.equals(StaticContext.UNICODE_COLLATION)) {
						throw new QueryException(
								ErrorCode.ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE,
								"Unknown collation in order-by clause: %s", col);
					}
				}
			}
		}
		return true;
	}

	private boolean countClause(AST clause) throws QueryException {
		if (clause.getType() != XQ.CountClause) {
			return false;
		}
		QNm name = (QNm) clause.getChild(0).getValue();
		// expand, bind and update AST
		name = expand(name, DefaultNS.EMPTY);
		name = bind(name);
		clause.getChild(0).setValue(name);
		return true;
	}

	private boolean quantifiedExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.QuantifiedExpr) {
			return false;
		}
		int scopeCount = scopeCount();
		// child 0 is quantifier type
		for (int i = 1; i < expr.getChildCount() - 1; i += 2) {
			openScope();
			typedVarBinding(expr.getChild(i));
			exprSingle(expr.getChild(i + 1));
			offerScope();
		}
		exprSingle(expr.getChild(expr.getChildCount() - 1));
		for (int i = scopeCount(); i > scopeCount; i--) {
			closeScope();
		}
		return true;
	}

	private boolean switchExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.SwitchExpr) {
			return false;
		}
		expr(expr.getChild(0));
		for (int i = 1; i < expr.getChildCount() - 1; i++) {
			switchClause(expr.getChild(i));
		}
		exprSingle(expr.getChild(expr.getChildCount() - 1));
		return true;
	}

	private boolean switchClause(AST clause) throws QueryException {
		for (int i = 0; i < clause.getChildCount() - 1; i++) {
			exprSingle(clause.getChild(i));
		}
		exprSingle(clause.getChild(clause.getChildCount() - 1));
		return true;
	}

	private boolean typeswitchExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.TypeSwitch) {
			return false;
		}
		openScope();
		expr(expr.getChild(0));
		for (int i = 1; i < expr.getChildCount() - 2; i++) {
			caseClause(expr.getChild(i));
		}
		// handle default case as case clause
		caseClause(expr.getChild(expr.getChildCount() - 2));
		exprSingle(expr.getChild(expr.getChildCount() - 1));
		closeScope();
		return true;
	}

	private boolean caseClause(AST clause) throws QueryException {
		openScope();
		QNm name = (QNm) clause.getChild(0).getValue();
		// expand, bind and update AST
		name = expand(name, DefaultNS.EMPTY);
		name = bind(name);
		clause.getChild(0).setValue(name);

		for (int i = 1; i < clause.getChildCount() - 1; i++) {
			sequenceType(clause.getChild(i));
		}
		offerScope();
		exprSingle(clause.getChild(clause.getChildCount() - 1));
		closeScope();
		return true;
	}

	private boolean ifExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.IfExpr) {
			return false;
		}
		exprSingle(expr.getChild(0));
		exprSingle(expr.getChild(1));
		exprSingle(expr.getChild(2));
		return true;
	}

	private boolean tryCatchExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.TryCatchExpr) {
			return false;
		}
		expr(expr.getChild(0));
		for (int i = 0; i < expr.getChildCount(); i++) {
			tryClause(expr.getChild(i));
		}
		return true;
	}

	private boolean tryClause(AST clause) throws QueryException {
		openScope();
		catchErrorList(clause.getChild(0));
		catchVars(clause.getChild(1));
		offerScope();
		expr(clause.getChild(2));
		closeScope();
		return true;
	}

	private void catchErrorList(AST errorList) throws QueryException {
		for (int i = 0; i < errorList.getChildCount(); i++) {
			nameTest(errorList.getChild(i), false);
		}
	}

	private void catchVars(AST catchVar) throws QueryException {
		for (int i = 0; i < catchVar.getChildCount(); i++) {
			QNm name = (QNm) catchVar.getChild(i).getValue();
			// expand and update AST
			name = expand(name, DefaultNS.EMPTY);
			catchVar.getChild(i).setValue(name);
		}
	}

	private boolean orExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.OrExpr) {
			return andExpr(expr);
		}
		andExpr(expr.getChild(0));
		andExpr(expr.getChild(1));
		return true;
	}

	private boolean andExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.AndExpr) {
			return comparisonExpr(expr);
		}
		comparisonExpr(expr.getChild(0));
		comparisonExpr(expr.getChild(1));
		return true;
	}

	private boolean comparisonExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.ComparisonExpr) {
			return rangeExpr(expr);
		}
		rangeExpr(expr.getChild(1));
		rangeExpr(expr.getChild(2));
		return true;
	}

	private boolean rangeExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.RangeExpr) {
			return additiveExpr(expr);
		}
		additiveExpr(expr.getChild(0));
		additiveExpr(expr.getChild(1));
		return true;
	}

	private boolean additiveExpr(AST expr) throws QueryException {
		if ((expr.getType() != XQ.ArithmeticExpr)
				|| ((expr.getChild(0).getType() != XQ.AddOp) && (expr.getChild(
						0).getType() != XQ.SubtractOp))) {
			return multiplicativeExpr(expr);
		}
		multiplicativeExpr(expr.getChild(1));
		multiplicativeExpr(expr.getChild(2));
		return true;
	}

	private boolean multiplicativeExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.ArithmeticExpr) {
			return unionExpr(expr);
		}
		unionExpr(expr.getChild(1));
		unionExpr(expr.getChild(2));
		return true;
	}

	private boolean unionExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.UnionExpr) {
			return intersectExpr(expr);
		}
		intersectExpr(expr.getChild(0));
		intersectExpr(expr.getChild(1));
		return true;
	}

	private boolean intersectExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.IntersectExpr) {
			return instanceOfExpr(expr);
		}
		instanceOfExpr(expr.getChild(0));
		instanceOfExpr(expr.getChild(1));
		return true;
	}

	private boolean instanceOfExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.InstanceofExpr) {
			return treatExpr(expr);
		}
		treatExpr(expr.getChild(0));
		sequenceType(expr.getChild(1));
		return true;
	}

	private SequenceType sequenceType(AST stype) throws QueryException {
		if (stype.getType() == XQ.EmptySequenceType) {
			return SequenceType.EMPTY_SEQUENCE;
		}
		ItemType itype = itemType(stype.getChild(0));
		Cardinality card = Cardinality.ZeroOrMany;
		if (stype.getChildCount() == 2) {
			card = occurrenceIndicator(stype.getChild(1));
		}
		return new SequenceType(itype, card);
	}

	private ItemType anyKind(AST kind) throws QueryException {
		if (kind.getType() != XQ.ItemType) {
			return null;
		}
		return AnyItemType.ANY;
	}

	private Cardinality occurrenceIndicator(AST card) {
		if (card.getType() == XQ.CardinalityZeroOrOne) {
			return Cardinality.ZeroOrOne;
		} else if (card.getType() == XQ.CardinalityZeroOrMany) {
			return Cardinality.ZeroOrMany;
		} else {
			return Cardinality.OneOrMany;
		}
	}

	private ItemType itemType(AST itype) throws QueryException {
		ItemType type = kindTest(itype);
		type = (type != null) ? type : anyKind(itype);
		type = (type != null) ? type : functionTest(itype);
		type = (type != null) ? type : atomicOrUnionType(itype);
		type = (type != null) ? type : parenthesizedItemType(itype);
		return type;
	}

	private ItemType functionTest(AST type) throws QueryException {
		if (type.getType() != XQ.FunctionTest) {
			return null;
		}
		int pos = 0;
		AST annotationOrTest = type.getChild(pos++);
		while (annotationOrTest.getType() == XQ.Annotation) {
			QNm name = (QNm) annotationOrTest.getChild(0).getValue();
			// expands to default function namespace
			// if no prefix is present
			// expand and update AST
			name = expand(name, DefaultNS.FUNCTION);
			annotationOrTest.getChild(0).setValue(name);
			String uri = name.getNamespaceURI();
			if ((uri.equals(Namespaces.XML_NSURI))
					|| (uri.equals(Namespaces.XS_NSURI))
					|| (uri.equals(Namespaces.XSI_NSURI))
					|| (uri.equals(Namespaces.FN_NSURI))
					|| (uri.equals(Namespaces.FNMATH_NSURI))) {
				throw new QueryException(
						ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
						"Function declaration %s is in illegal namespace: %s",
						name, uri);
			}
		}
		if (annotationOrTest.getType() == XQ.AnyFunctionType) {
			SequenceType any = new SequenceType(AnyItemType.ANY,
					Cardinality.ZeroOrMany);
			return new FunctionType(new Signature(any, true, false, any));
		} else if (annotationOrTest.getType() == XQ.TypedFunctionType) {
			return typedFunctionTest(annotationOrTest);
		} else {
			return null;
		}
	}

	private ItemType typedFunctionTest(AST test) throws QueryException {
		SequenceType[] params = new SequenceType[test.getChildCount() - 1];
		for (int i = 0; i < test.getChildCount() - 1; i++) {
			params[i] = sequenceType(test.getChild(i));
		}
		SequenceType resType = sequenceType(test
				.getChild(test.getChildCount() - 1));
		return new FunctionType(new Signature(resType, params));
	}

	private ItemType atomicOrUnionType(AST type) throws QueryException {
		QNm name = (QNm) type.getChild(0).getValue();
		// expand and update AST
		name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
		type.getChild(0).setValue(name);
		Type t = ctx.getTypes().resolveType(name);
		return new AtomicType(t);
	}

	private ItemType parenthesizedItemType(AST type) throws QueryException {
		return itemType(type);
	}

	private SequenceType singleType(AST type) throws QueryException {
		ItemType aouType = atomicOrUnionType(type.getChild(0));
		Cardinality card = Cardinality.One;
		if ((type.getChildCount() >= 2)
				&& (type.getChild(1).getType() == XQ.CardinalityZeroOrOne)) {
			card = Cardinality.ZeroOrOne;
		}
		return new SequenceType(aouType, card);
	}

	private boolean treatExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.TreatExpr) {
			return castableExpr(expr);
		}
		castableExpr(expr.getChild(0));
		sequenceType(expr.getChild(1));
		return true;
	}

	private boolean castableExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.CastableExpr) {
			return castExpr(expr);
		}
		castExpr(expr.getChild(0));
		singleType(expr.getChild(1));
		return true;
	}

	private boolean castExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.CastExpr) {
			return unaryExpr(expr);
		}
		unaryExpr(expr.getChild(0));
		singleType(expr.getChild(1));
		return true;
	}

	private boolean unaryExpr(AST expr) throws QueryException {
		return valueExpr(expr);
	}

	private boolean valueExpr(AST expr) throws QueryException {
		return (validateExpr(expr) || pathExpr(expr) || extensionExpr(expr));
	}

	private boolean extensionExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.ExtensionExpr) {
			return false;
		}
		for (int i = 0; i < expr.getChildCount(); i++) {
			AST pragmaOrExpr = expr.getChild(i);
			if (pragmaOrExpr.getType() == XQ.Pragma) {
				QNm name = (QNm) pragmaOrExpr.getChild(0).getValue();
				// expand and update AST
				name = expand(name, DefaultNS.PRAGMA);
				pragmaOrExpr.getChild(0).setValue(name);
			} else {
				expr(pragmaOrExpr);
			}
		}
		return true;
	}

	private boolean pathExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.PathExpr) {
			return stepExpr(expr);
		}
		for (int i = 0; i < expr.getChildCount(); i++) {
			stepExpr(expr.getChild(i));
		}
		return true;
	}

	private boolean stepExpr(AST expr) throws QueryException {
		return (postFixExpr(expr) || axisStep(expr));
	}

	private boolean axisStep(AST expr) throws QueryException {
		if (expr.getType() != XQ.StepExpr) {
			return false;
		}
		// child 0 is the axis
		nodeTest(expr.getChild(1), expr.getChild(0).getType() == XQ.ATTRIBUTE);
		for (int i = 2; i < expr.getChildCount(); i++) {
			predicate(expr.getChild(i));
		}
		return true;
	}

	private ItemType nodeTest(AST nodeTest, boolean attributeAxis)
			throws QueryException {
		ItemType test = kindTest(nodeTest);
		test = (test != null) ? test : nameTest(nodeTest, !attributeAxis);
		return test;
	}

	private ItemType kindTest(AST kindTest) throws QueryException {
		ItemType test = documentTest(kindTest);
		test = (test != null) ? test : elementTest(kindTest);
		test = (test != null) ? test : attributeTest(kindTest);
		test = (test != null) ? test : schemaElementTest(kindTest);
		test = (test != null) ? test : schemaAttributeTest(kindTest);
		test = (test != null) ? test : piTest(kindTest);
		test = (test != null) ? test : commentTest(kindTest);
		test = (test != null) ? test : textTest(kindTest);
		test = (test != null) ? test : namespaceNodeTest(kindTest);
		test = (test != null) ? test : anyKindTest(kindTest);
		return test;
	}

	private DocumentType documentTest(AST kindTest) throws QueryException {
		if (kindTest.getType() != XQ.KindTestDocument) {
			return null;
		}
		if (kindTest.getChildCount() == 0) {
			return new DocumentType();
		}
		AST child = kindTest.getChild(0);
		ElementType test = elementTest(child);
		test = (test != null) ? test : schemaElementTest(kindTest);
		return new DocumentType(test);
	}

	private ElementType elementTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestElement) {
			return null;
		}
		boolean nilled = false;
		Type type = null;
		QNm name = null;
		AST child = test.getChild(0);
		if (child.getType() == XQ.Wildcard) {
			name = null;
		} else if (child.getType() == XQ.QNm) {
			name = (QNm) child.getValue();
			// expand and update AST
			name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
			child.setValue(name);
		}
		if (test.getChildCount() >= 2) {
			child = test.getChild(1);
			QNm typeName = (QNm) child.getValue();
			// expand and update AST
			typeName = expand(typeName, DefaultNS.ELEMENT_OR_TYPE);
			child.setValue(typeName);
			type = ctx.getTypes().resolveType(typeName);
			if (test.getChildCount() >= 3) {
				child = test.getChild(2);
				nilled = (child.getType() == XQ.Nilled);
			}
		}
		return new ElementType(name, type);
	}

	private AttributeType attributeTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestAttribute) {
			return null;
		}
		Type type = null;
		QNm name = null;
		AST child = test.getChild(0);
		if (child.getType() == XQ.Wildcard) {
			name = null;
		} else if (child.getType() == XQ.QNm) {
			name = (QNm) child.getValue();
			// expand and update AST
			name = expand(name, DefaultNS.EMPTY);
			child.setValue(name);
		}
		if (test.getChildCount() >= 2) {
			child = test.getChild(1);
			QNm typeName = (QNm) child.getValue();
			// expand and update AST
			typeName = expand(typeName, DefaultNS.ELEMENT_OR_TYPE);
			child.setValue(typeName);
			type = ctx.getTypes().resolveType(typeName);
		}
		return new AttributeType(name, type);
	}

	private ElementType schemaElementTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestSchemaElement) {
			return null;
		}
		AST child = test.getChild(0);
		QNm name = (QNm) child.getValue();
		// expand and update AST
		name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
		child.setValue(name);
		Type type = ctx.getTypes().resolveType(name);
		return new ElementType(name, type);
	}

	private AttributeType schemaAttributeTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestSchemaAttribute) {
			return null;
		}
		AST child = test.getChild(0);
		QNm name = (QNm) child.getValue();
		// expand and update AST
		name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
		child.setValue(name);
		Type type = ctx.getTypes().resolveType(name);
		return new AttributeType(name, type);
	}

	private ItemType piTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestPi) {
			return null;
		}
		String target = test.getChild(0).getStringValue();
		target = Whitespace.normalizeXML11(target);
		if (!XMLChar.isNCName(target)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"PI target name is not a valid NCName: %s", target);
		}
		return new PIType(target);
	}

	private ItemType commentTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestComment) {
			return null;
		}
		return new CommentType();
	}

	private ItemType textTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestComment) {
			return null;
		}
		return new TextType();
	}

	private ItemType namespaceNodeTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestNamespaceNode) {
			return null;
		}
		throw new QueryException(
				ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
				"Namespace test not implemented yet");
	}

	private ItemType anyKindTest(AST test) throws QueryException {
		if (test.getType() != XQ.KindTestAnyKind) {
			return null;
		}
		return AnyNodeType.ANY_NODE;
	}

	private ItemType nameTest(AST test, boolean element) throws QueryException {
		if (test.getType() != XQ.NameTest) {
			return null;
		}
		ItemType type = wildcard(test.getChild(0), element);
		if (type != null) {
			return type;
		}
		if (element) {
			QNm name = (QNm) test.getChild(0).getValue();
			// expand and update AST
			name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
			test.getChild(0).setValue(name);
			return new ElementType(name);
		} else {
			QNm name = (QNm) test.getChild(0).getValue();
			// expand and update AST
			name = expand(name, DefaultNS.EMPTY);
			test.getChild(0).setValue(name);
			return new AttributeType(name);
		}
	}

	private ItemType wildcard(AST test, boolean element) throws QueryException {
		if (test.getType() == XQ.Wildcard) {
			return new ElementType();
		} else if (test.getType() == XQ.NSWildcardNameTest) {
			Kind kind = (element) ? Kind.ELEMENT : Kind.ATTRIBUTE;
			return new NSWildcardNameTest(kind, test.getStringValue());
		} else if (test.getType() == XQ.NSNameWildcardTest) {
			Kind kind = (element) ? Kind.ELEMENT : Kind.ATTRIBUTE;
			return new NSNameWildcardTest(kind, test.getStringValue());
		} else {
			return null;
		}
	}

	private void predicate(AST predicate) throws QueryException {
		expr(predicate.getChild(0));
	}

	private boolean validateExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.ValidateExpr) {
			return false;
		}
		if (expr.getChild(0).getType() == XQ.QNm) {
			QNm name = (QNm) expr.getChild(0).getValue();
			// expand and update AST
			name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
			expr.getChild(0).setValue(name);
			ctx.getTypes().resolveType(name);
		}
		expr(expr.getChild(1));
		return true;
	}

	private boolean postFixExpr(AST expr) throws QueryException {
		if (expr.getType() == XQ.FilterExpr) {
			expr(expr.getChild(0));
			for (int i = 1; i < expr.getChildCount(); i++) {
				predicate(expr.getChild(i));
			}
		} else if (expr.getType() == XQ.DynamicFunctionCallExpr) {
			expr(expr.getChild(0));
			for (int i = 1; i < expr.getChildCount(); i++) {
				argument(expr.getChild(i));
			}
		} else {
			return primaryExpr(expr);
		}
		return true;
	}

	private boolean primaryExpr(AST expr) throws QueryException {
		return (literal(expr) || varRef(expr) || parenthesizedExpr(expr)
				|| contextItemExpr(expr) || functionCall(expr)
				|| orderedExpr(expr) || unorderedExpr(expr)
				|| constructor(expr) || functionItemExpr(expr));
	}

	private boolean literal(AST expr) throws QueryException {
		return (numericLiteral(expr) || (expr.getType() == XQ.Str));
	}

	private boolean varRef(AST expr) throws QueryException {
		if (expr.getType() != XQ.VariableRef) {
			return false;
		}
		QNm name = (QNm) expr.getValue();
		// expand, resolve and update AST
		name = expand(name, DefaultNS.EMPTY);
		name = resolve(name);
		expr.setValue(name);
		return true;
	}

	private boolean parenthesizedExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.ParenthesizedExpr) {
			return false;
		}
		if (expr.getChildCount() != 0) {
			expr(expr.getChild(0));
		}
		return true;
	}

	private boolean contextItemExpr(AST expr) {
		return (expr.getType() == XQ.ContextItemExpr);
	}

	private boolean functionCall(AST expr) throws QueryException {
		if (expr.getType() != XQ.FunctionCall) {
			return false;
		}
		QNm name = (QNm) expr.getValue();
		// expand and update AST
		name = expand(name, DefaultNS.FUNCTION);
		expr.setValue(name);

		if (replaceFunctionCall(expr, name)) {
			return true;
		}

		for (int i = 0; i < expr.getChildCount(); i++) {
			argument(expr.getChild(i));
		}
		Function fun = ctx.getFunctions().resolve(name, expr.getChildCount());
		if (fun == null) {
			String argp = (expr.getChildCount() > 0) ? "?" : "";
			for (int i = 1; i < expr.getChildCount(); i++) {
				argp += ", ?";
			}
			throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
					"Unknown function: %s(%s)", name, argp);
		}
		return true;
	}

	private boolean replaceFunctionCall(AST expr, QNm name)
			throws QueryException {
		int argc = expr.getChildCount();
		if ((name.equals(Functions.FN_POSITION))
				|| (name.equals(Functions.FN_LAST))) {
			if (argc != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, argc);
			}
			// change expr to variable reference
			expr.setType(XQ.VariableRef);
			QNm newName = name.equals(Functions.FN_POSITION) ? Namespaces.FS_POSITION
					: Namespaces.FS_LAST;
			expr.setValue(newName);
			return true;
		}
		if ((name.equals(Functions.FN_TRUE) || name.equals(Functions.FN_FALSE))) {
			if (argc != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, argc);
			}
			// change expr to boolean literal
			expr.setType(XQ.Bool);
			Bool val = (name.equals(Functions.FN_TRUE)) ? Bool.TRUE
					: Bool.FALSE;
			expr.setValue(val);
			return true;
		}
		if (name.equals(Functions.FN_DEFAULT_COLLATION)) {
			if (argc != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, argc);
			}
			// change expr to string literal
			expr.setType(XQ.Str);
			expr.setValue(new Str(ctx.getDefaultCollation()));
			return true;
		}
		if (name.equals(Functions.FN_STATIC_BASE_URI)) {
			if (argc != 0) {
				throw new QueryException(ErrorCode.ERR_UNDEFINED_FUNCTION,
						"Illegal number of parameters for function %s() : %s'",
						name, argc);
			}
			// change expr to uri literal
			expr.setType(XQ.AnyURI);
			expr.setValue(ctx.getBaseURI());
			return true;
		}
		return false;
	}

	private void argument(AST argument) throws QueryException {
		if (argument.getType() != XQ.ArgumentPlaceHolder) {
			exprSingle(argument);
		}
	}

	private boolean orderedExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.OrderedExpr) {
			return false;
		}
		// TODO change order mode in static context
		expr(expr);
		return true;
	}

	private boolean unorderedExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.UnorderedExpr) {
			return false;
		}
		// TODO change order mode in static context
		expr(expr);
		return true;
	}

	private boolean constructor(AST expr) throws QueryException {
		return (directConstructor(expr) || computedConstructor(expr));
	}

	private boolean directConstructor(AST expr) throws QueryException {
		return (dirElemConstructor(expr) || dirCommentConstructor(expr) || dirPIConstructor(expr));
	}

	private boolean dirElemConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.DirElementConstructor) {
			return false;
		}
		// create new static context
		StaticContext pctx = ctx;
		this.ctx = new NestedContext(pctx);

		QNm name = (QNm) expr.getChild(0).getValue();

		// pre-check content sequence for direct
		// namespace attributes
		AST cseq = expr.getChild(1);
		for (int i = 0; i < cseq.getChildCount(); i++) {
			AST att = cseq.getChild(i);
			if (att.getType() != XQ.DirAttributeConstructor) {
				break;
			}
			QNm attName = (QNm) att.getChild(0).getValue();
			if ("xmlns".equals(attName.getPrefix())) {
				String prefix = attName.getLocalName();
				String uri = extractURIFromDirNSAttContent(att.getChild(0));
				checkDirNSAttBinding(prefix, uri);
				ctx.getNamespaces().declare(prefix, uri);
				// delete from context sequence
				// and prepend prefixed namespace declaration
				// in element constructor
				cseq.deleteChild(i--);
				AST nsDecl = new AST(XQ.NamespaceDeclaration);
				nsDecl.addChild(new AST(XQ.Str, prefix));
				nsDecl.addChild(new AST(XQ.AnyURI, uri));
				expr.insertChild(0, nsDecl);
			} else if ("xmlns".equals(attName.getLocalName())) {
				String uri = extractURIFromDirNSAttContent(att.getChild(0));
				ctx.getNamespaces().setDefaultElementNamespace(uri);
				// delete from context sequence
				// and prepend prefixed namespace declaration
				// in element constructor
				cseq.deleteChild(i--);
				AST nsDecl = new AST(XQ.NamespaceDeclaration);
				nsDecl.addChild(new AST(XQ.AnyURI, uri));
				expr.insertChild(0, nsDecl);
			}
		}

		// expand element name and update AST
		name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
		expr.getChild(0).setValue(name);

		// merge adjacent string literals and
		// strip boundary whitespace if requested
		int merge = 0;
		for (int i = 0; i < cseq.getChildCount(); i++) {
			AST c = cseq.getChild(i);
			if (c.getType() == XQ.Str) {
				if ((ctx.isBoundarySpaceStrip())
						&& (c.checkProperty("boundaryWS"))) {
					cseq.deleteChild(i--);
					merge = 0;
					continue;
				} else {
					merge++;
					continue;
				}
			}
			if (merge > 1) {
				StringBuilder buf = new StringBuilder();
				int firstStrPos = i - merge;
				AST sc = cseq.getChild(firstStrPos);
				buf.append(sc.getStringValue());
				for (int j = 0; j < merge - 1; j++) {
					buf.append(cseq.getChild(firstStrPos + 1).getStringValue());
					cseq.deleteChild(firstStrPos + 1);
				}
				sc.setValue(buf.toString());
				i -= (merge - 1);
			}
			merge = 0;
		}

		// finally process non-direct-namespace-attribute content
		AST p = null;
		for (int i = 0; i < cseq.getChildCount(); i++) {
			AST c = cseq.getChild(i);
			if (c.getType() == XQ.DirAttributeConstructor) {
				dirAttribute(c);
			} else {
				dirElementContent(c);
			}
			p = c;
		}

		// restore static context
		this.ctx = pctx;
		return true;
	}

	private String extractURIFromDirNSAttContent(AST content)
			throws QueryException {
		StringBuilder uri = new StringBuilder();
		for (int i = 0; i < content.getChildCount(); i++) {
			AST c = content.getChild(i);
			if (c.getType() == XQ.EnclosedExpr) {
				throw new QueryException(
						ErrorCode.ERR_ENCLOSED_EXPR_IN_NS_ATTRIBUTE,
						"Illegal enclosed expression in direct namespace attribute");
			}
			uri.append(c.getStringValue());
		}
		String eolNormalized = Whitespace.normalizeXML11(uri.toString());
		String wsNormalized = Whitespace.replace(eolNormalized);
		return wsNormalized;
	}

	private void checkDirNSAttBinding(String prefix, String uri)
			throws QueryException {
		if (Namespaces.XML_PREFIX.equals(prefix)) {
			if (Namespaces.XML_NSURI.equals(uri)) {
				throw new QueryException(
						ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
						"Illegal mapping of the prefix '%s' to the namespace URI '%s'",
						Namespaces.XML_PREFIX, uri);
			}
		} else if (Namespaces.XMLNS_PREFIX.equals(prefix)) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"Illegal namespace prefix '%s'", Namespaces.XMLNS_PREFIX);
		} else if (Namespaces.XML_NSURI.equals(uri)) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"Illegal namespace URI '%s'", Namespaces.XMLNS_NSURI);
		}
	}

	private void dirAttribute(AST dirAtt) throws QueryException {
		QNm name = (QNm) dirAtt.getChild(0).getValue();
		// expand and update AST
		name = expand(name, DefaultNS.EMPTY);
		dirAtt.getChild(0).setValue(name);
		AST content = dirAtt.getChild(1);

		// TODO checks?
		// TODO concat?
		for (int i = 0; i < content.getChildCount(); i++) {
			AST c = content.getChild(0);
			if (c.getType() != XQ.Str) {
				enclosedExpr(c);
			}
		}
	}

	private boolean dirElementContent(AST content) throws QueryException {
		return (directConstructor(content) || (content.getType() == XQ.Str) || enclosedExpr(content));
	}

	private boolean dirCommentConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.DirCommentConstructor) {
			return false;
		}
		// TODO check comment content?
		return true;
	}

	private boolean dirPIConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.DirPIConstructor) {
			return false;
		}
		// TODO check PI target and content?
		return true;
	}

	private boolean computedConstructor(AST expr) throws QueryException {
		return (compDocConstructor(expr) || compElemConstructor(expr)
				|| compAttrConstructor(expr) || compNamespaceConstructor(expr)
				|| compTextConstructor(expr) || compCommentConstructor(expr) || compPIConstructor(expr));
	}

	private boolean compDocConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompDocumentConstructor) {
			return false;
		}
		expr(expr.getChild(0));
		return true;
	}

	private boolean compElemConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompElementConstructor) {
			return false;
		}
		AST nameExpr = expr.getChild(0);
		if (nameExpr.getType() == XQ.QNm) {
			QNm name = (QNm) nameExpr.getValue();
			name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
			nameExpr.setValue(name);
		} else {
			expr(nameExpr);
		}

		AST cseq = expr.getChild(1);
		if (cseq.getChildCount() == 1) {
			expr(cseq.getChild(0));
		}
		return true;
	}

	private boolean compAttrConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompAttributeConstructor) {
			return false;
		}
		AST nameExpr = expr.getChild(0);
		if (nameExpr.getType() == XQ.QNm) {
			QNm name = (QNm) nameExpr.getValue();
			name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
			nameExpr.setValue(name);
		} else {
			expr(nameExpr);
		}

		AST cseq = expr.getChild(1);
		if (cseq.getChildCount() == 1) {
			expr(cseq.getChild(0));
		}
		return true;
	}

	private boolean compNamespaceConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompNamespaceConstructor) {
			return false;
		}
		AST prefixExpr = expr.getChild(0);
		if (prefixExpr.getType() != XQ.Str) {
			expr(prefixExpr);
		}
		if (expr.getChildCount() == 2) {
			expr(expr.getChild(1));
		}
		return true;
	}

	private boolean compTextConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompTextConstructor) {
			return false;
		}
		expr(expr.getChild(0));
		return true;
	}

	private boolean compCommentConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompCommentConstructor) {
			return false;
		}
		expr(expr.getChild(0));
		return true;
	}

	private boolean compPIConstructor(AST expr) throws QueryException {
		if (expr.getType() != XQ.CompPIConstructor) {
			return false;
		}
		AST prefixExpr = expr.getChild(0);
		if (prefixExpr.getType() != XQ.Str) {
			expr(prefixExpr);
		}
		if (expr.getChildCount() == 2) {
			expr(expr.getChild(1));
		}
		return true;
	}

	private boolean functionItemExpr(AST expr) throws QueryException {
		return (literalFunctionItem(expr) || inlineFunction(expr));
	}

	private boolean literalFunctionItem(AST expr) throws QueryException {
		if (expr.getType() != XQ.LiteralFuncItem) {
			return false;
		}
		QNm name = (QNm) expr.getChild(0).getValue();
		// expand and update AST
		name = expand(name, DefaultNS.FUNCTION);
		expr.getChild(0).setValue(name);
		int argc = ((Numeric) expr.getChild(1).getValue()).intValue();
		// TODO lookup and checks
		return true;
	}

	private boolean inlineFunction(AST expr) throws QueryException {
		if (expr.getType() != XQ.InlineFuncItem) {
			return false;
		}
		throw new QueryException(
				ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
				"Inline functions not implemented yet.");
	}

	private boolean enclosedExpr(AST expr) throws QueryException {
		if (expr.getType() != XQ.EnclosedExpr) {
			return false;
		}
		expr(expr.getChild(0));
		return true;
	}

	private boolean numericLiteral(AST literal) throws QueryException {
		return (integerLiteral(literal) || decimalLiteral(literal) || (doubleLiteral(literal)));
	}

	private boolean doubleLiteral(AST literal) throws QueryException {
		return (literal.getType() == XQ.Dbl);
	}

	private boolean decimalLiteral(AST literal) throws QueryException {
		return (literal.getType() == XQ.Dec);
	}

	private boolean integerLiteral(AST literal) throws QueryException {
		return (literal.getType() == XQ.Int);
	}

	private QNm expand(QNm name, DefaultNS mode) throws QueryException {
		String prefix = name.getPrefix();
		String uri;
		Namespaces ns = ctx.getNamespaces();
		if (mode == DefaultNS.ELEMENT_OR_TYPE) {
			if (prefix == null) {
				return new QNm(ns.getDefaultElementNamespace(), null, name
						.getLocalName());
			} else if ((uri = ns.resolve(prefix)) != null) {
				return new QNm(uri, prefix, name.getLocalName());
			}
		} else if (mode == DefaultNS.FUNCTION) {
			if (prefix == null) {
				return new QNm(ns.getDefaultFunctionNamespace(), null, name
						.getLocalName());
			} else if ((uri = ns.resolve(prefix)) != null) {
				return new QNm(uri, prefix, name.getLocalName());
			}
		} else if (mode == DefaultNS.PRAGMA) {
			// pragmas aren't resolved to the empty default namespace
			if ((prefix == null) && (!name.getNamespaceURI().isEmpty())) {
				return name;
			} else if ((uri = ns.resolve(prefix)) != null) {
				return new QNm(uri, prefix, name.getLocalName());
			}
		} else {
			if (prefix == null) {
				return name;
			} else if ((uri = ns.resolve(prefix)) != null) {
				return new QNm(uri, prefix, name.getLocalName());
			}
		}
		throw new QueryException(ErrorCode.ERR_UNDEFINED_NAMESPACE_PREFIX,
				"Undefined namespace prefix: '%s'", prefix);
	}

	private QNm bind(QNm name) throws QueryException {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Declare variable " + name);
		}
		if (variables.check(name)) {
			throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
					"Variable $%s has already been declared.", name);
		}
		return variables.declare(name);
	}

	private QNm resolve(QNm name) throws QueryException {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Declare variable " + name);
		}
		QNm resolved = variables.resolve(name);
		if (resolved == null) {
			Variable dVar = module.getVariables().resolve(name);
			if (dVar != null) {
				return name;
			}
		}
		if (resolved == null) {
			throw new QueryException(ErrorCode.ERR_UNDEFINED_REFERENCE,
					"Variable $%s has not been declared.", name);
		}
		return resolved;
	}

	private void openScope() throws QueryException {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Open scope");
		}
		variables.openScope();
	}

	private void offerScope() throws QueryException {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Offer scope");
		}
		variables.offerScope();
	}

	private void closeScope() throws QueryException {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Close scope");
		}
		variables.closeScope();
	}

	private int scopeCount() {
		return variables.scopeCount();
	}
}