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
package org.brackit.xquery.compiler.parser;

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;

/**
 * Straight-forward, recursive descent parser.
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQParser extends Tokenizer {

	private static final String[] RESERVED_FUNC_NAMES = new String[] {
			"attribute", "comment", "document-node", "element",
			"empty-sequence", "funtion", "if", "item", "namespace-node",
			"node", "processing-instruction", "schema-attribute",
			"schema-element", "switch", "text", "typeswitch" };

	private String encoding = "UTF-8";

	private String version = "3.0";

	private boolean update;

	private VarScopes variables;

	public XQParser(String query) {
		super(query);
	}

	public XQAST parse() throws QueryException {
		XQAST module = module();
		if (module == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"No module found");
		}
		consumeEOF();
		XQAST xquery = new XQAST(XQ.XQuery);
		xquery.addChild(module);
		return xquery;
	}

	public void setXQVersion(String version) throws QueryException {
		if ("3.0".equals(version)) {
			this.version = version;
		} else if ("1.0".equals(version)) {
			this.version = version;
		} else if ("1.1".equals(version)) {
			this.version = version;
		} else {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"unsupported version: " + version);
		}
	}

	private void setEncoding(String encoding) {
		System.out.println("set encoding " + encoding);
	}

	private XQAST module() throws QueryException {
		versionDecl();
		XQAST module = libraryModule();
		if (module == null) {
			module = mainModule();
		}
		return module;
	}

	private boolean versionDecl() throws QueryException {
		if (!attemptSkipWS("xquery")) {
			return false;
		}
		boolean vDecl = false;
		boolean eDecl = false;
		if (attemptSkipWS("version")) {
			setXQVersion(stringLiteral(false, true).getValue());
			vDecl = true;
		}
		if (attemptSkipWS("encoding")) {
			setEncoding(stringLiteral(false, true).getValue());
			eDecl = true;
		}
		if ((!vDecl) && (!eDecl)) {
			mismatch("version", "encoding");
		}
		consumeSkipWS(";");
		return true;
	}

	private XQAST libraryModule() throws QueryException {
		Token la = laSkipWS("module");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST ncn = ncnameLiteral(false, true);
		consumeSkipWS("=");
		XQAST uriLiteral = uriLiteral(false, true);

		XQAST module = new XQAST(XQ.LibraryModule);
		XQAST nsDecl = new XQAST(XQ.NamespaceDeclaration);
		XQAST ncname = new XQAST(XQ.Literal);
		ncname.addChild(ncn);
		nsDecl.addChild(ncname);
		XQAST str = new XQAST(XQ.Literal);
		str.addChild(uriLiteral);
		nsDecl.addChild(str);
		XQAST[] prolog = prolog();
		if (prolog != null) {
			module.addChildren(prolog);
		}
		return module;
	}

	private XQAST mainModule() throws QueryException {
		XQAST[] prolog = prolog();
		XQAST body = queryBody();
		XQAST module = new XQAST(XQ.MainModule);
		if (prolog != null) {
			module.addChildren(prolog);
		}
		module.addChild(body);
		return module;
	}

	private XQAST[] prolog() throws QueryException {
		XQAST[] defs = new XQAST[0];
		while (true) {
			XQAST def = defaultNamespaceDecl();
			def = (def != null) ? def : setter();
			def = (def != null) ? def : namespaceDecl();
			def = (def != null) ? def : importDecl();
			if (def != null) {
				consumeSkipWS(";");
				add(defs, def);
			} else {
				break;
			}
		}
		while (true) {
			XQAST def = contextItemDecl();
			def = (def != null) ? def : annotatedDecl();
			def = (def != null) ? def : optionDecl();
			if (def != null) {
				consumeSkipWS(";");
				add(defs, def);
			} else {
				break;
			}
		}

		return defs;
	}

	private XQAST defaultNamespaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		boolean isElemDecl = attemptSkipWS("element");
		XQAST decl;
		if (isElemDecl) {
			decl = new XQAST(XQ.DefaultElementNamespace);
		} else {
			consumeSkipWS("function");
			decl = new XQAST(XQ.DefaultFunctionNamespace);
		}
		consumeSkipWS("namespace");
		XQAST uri = uriLiteral(false, true);
		XQAST literal = new XQAST(XQ.Literal);
		literal.addChild(uri);
		decl.addChild(literal);
		return decl;
	}

	private XQAST setter() throws QueryException {
		XQAST setter = boundarySpaceDecl();
		setter = (setter != null) ? setter : defaultCollationDecl();
		setter = (setter != null) ? setter : baseURIDecl();
		setter = (setter != null) ? setter : constructionDecl();
		setter = (setter != null) ? setter : orderingModeDecl();
		setter = (setter != null) ? setter : emptyOrderDecl();
		setter = (setter != null) ? setter : copyNamespacesDecl();
		setter = (setter != null) ? setter : decimalFormatDecl();
		return setter;
	}

	private XQAST boundarySpaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "boundary-space");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.BoundarySpaceDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new XQAST(XQ.BoundarySpaceModePreserve));
		} else {
			consumeSkipWS("strip");
			decl.addChild(new XQAST(XQ.BoundarySpaceModeStrip));
		}
		return decl;
	}

	private XQAST defaultCollationDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "collation");
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		XQAST decl = new XQAST(XQ.CollationDeclaration);
		XQAST coll = new XQAST(XQ.Literal);
		coll.addChild(uriLiteral(false, true));
		decl.addChild(coll);
		return decl;
	}

	private XQAST baseURIDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "base-uri");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.BaseURIDeclaration);
		XQAST coll = new XQAST(XQ.Literal);
		coll.addChild(uriLiteral(false, true));
		decl.addChild(coll);
		return decl;
	}

	private XQAST constructionDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "construction");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.ConstructionDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new XQAST(XQ.ConstructionModePreserve));
		} else {
			consumeSkipWS("strip");
			decl.addChild(new XQAST(XQ.ConstructionModeStrip));
		}
		return decl;
	}

	private XQAST orderingModeDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "ordering");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.OrderingModeDeclaration);
		if (attemptSkipWS("ordered")) {
			decl.addChild(new XQAST(XQ.OrderingModeOrdered));
		} else {
			consume("unordered");
			decl.addChild(new XQAST(XQ.OrderingModeUnordered));
		}
		return decl;
	}

	private XQAST emptyOrderDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "order");
		if (la3 == null) {
			return null;
		}
		Token la4 = laSkipWS(la3, "empty");
		if (la4 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consume(la4);
		XQAST decl = new XQAST(XQ.EmptyOrderDeclaration);
		if (attemptSkipWS("greatest")) {
			decl.addChild(new XQAST(XQ.EmptyOrderModeGreatest));
		} else {
			consume("least");
			decl.addChild(new XQAST(XQ.EmptyOrderModeLeast));
		}
		return decl;
	}

	private XQAST copyNamespacesDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "copy-namespaces");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.CopyNamespacesDeclaration);
		decl.addChild(preserveMode());
		consumeSkipWS(",");
		decl.addChild(inheritMode());

		return decl;
	}

	private XQAST preserveMode() throws QueryException {
		if (attemptSkipWS("preserve")) {
			return new XQAST(XQ.CopyNamespacesPreserveModePreserve);
		} else {
			consumeSkipWS("no-preserve");
			return new XQAST(XQ.CopyNamespacesPreserveModeNoPreserve);
		}
	}

	private XQAST inheritMode() throws QueryException {
		if (attemptSkipWS("inherit")) {
			return new XQAST(XQ.CopyNamespacesInheritModeInherit);
		} else {
			consumeSkipWS("no-inherit");
			return new XQAST(XQ.CopyNamespacesInheritModeNoInherit);
		}
	}

	private XQAST decimalFormatDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "default");
		Token la3 = laSkipWS((la2 != null) ? la2 : la, "decimal-format");
		if (la3 == null) {
			return null;
		}
		consume(la);
		if (la2 != null) {
			consume(la2);
		}
		consume(la3);
		XQAST decl = new XQAST(XQ.DecimalFormatDeclaration);
		;
		XQAST[] dfProperties = new XQAST[0];
		XQAST dfPropertyName;
		while ((dfPropertyName = dfPropertyName()) != null) {
			consumeSkipWS("=");
			XQAST value = new XQAST(XQ.Literal);
			value.addChild(stringLiteral(false, true));
			XQAST dfp = new XQAST(XQ.DecimalFormatProperty);
			dfp.addChild(dfPropertyName);
			dfp.addChild(value);
			add(dfProperties, dfp);
		}
		decl.addChildren(dfProperties);
		return decl;
	}

	private XQAST dfPropertyName() {
		if (attemptSkipWS("decimal-separator")) {
			return new XQAST(XQ.DecimalFormatPropertyDecimalSeparator);
		} else if (attemptSkipWS("grouping-separator")) {
			return new XQAST(XQ.DecimalFormatPropertyGroupingSeparator);
		} else if (attemptSkipWS("infinity")) {
			return new XQAST(XQ.DecimalFormatPropertyInfinity);
		} else if (attemptSkipWS("minus-sign")) {
			return new XQAST(XQ.DecimalFormatPropertyMinusSign);
		} else if (attemptSkipWS("NaN")) {
			return new XQAST(XQ.DecimalFormatPropertyNaN);
		} else if (attemptSkipWS("percent")) {
			return new XQAST(XQ.DecimalFormatPropertyPercent);
		} else if (attemptSkipWS("per-mille")) {
			return new XQAST(XQ.DecimalFormatPropertyPerMille);
		} else if (attemptSkipWS("zero-digit")) {
			return new XQAST(XQ.DecimalFormatPropertyZeroDigit);
		} else if (attemptSkipWS("digit")) {
			return new XQAST(XQ.DecimalFormatPropertyDigit);
		} else if (attemptSkipWS("pattern-separator")) {
			return new XQAST(XQ.DecimalFormatPropertyPatternSeparator);
		} else {
			return null;
		}
	}

	private XQAST namespaceDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST ncname = ncnameLiteral(false, true);
		XQAST ncnLiteral = new XQAST(XQ.Literal);
		ncnLiteral.addChild(ncname);
		consumeSkipWS("=");
		XQAST uri = uriLiteral(false, true);
		XQAST uriLiteral = new XQAST(XQ.Literal);
		uriLiteral.addChild(uri);
		XQAST decl = new XQAST(XQ.NamespaceDeclaration);
		decl.addChild(ncnLiteral);
		decl.addChild(uriLiteral);
		return decl;
	}

	private XQAST importDecl() throws QueryException {
		XQAST importDecl = schemaImport();
		return (importDecl != null) ? importDecl : moduleImport();
	}

	private XQAST schemaImport() throws QueryException {
		Token la = laSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "schema");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST prefix = schemaPrefix();
		XQAST uri = uriLiteral(false, true);
		XQAST[] locs = new XQAST[0];
		if (attemptSkipWS("at")) {
			XQAST locUri;
			while ((locUri = uriLiteral(true, true)) != null) {
				add(locs, locUri);
			}
		}
		XQAST imp = new XQAST(XQ.SchemaImport);
		if (prefix != null) {
			imp.addChild(prefix);
		}
		imp.addChild(uri);
		imp.addChildren(locs);
		return imp;
	}

	private XQAST schemaPrefix() throws QueryException {
		Token la = laSkipWS("namespace");
		if (la != null) {
			consume(la);
			XQAST ncname = ncnameLiteral(false, true);
			consumeSkipWS("=");
			XQAST ns = new XQAST(XQ.Namespace);
			ns.addChild(ncname);
			return ns;
		}
		la = laSkipWS("default");
		consumeSkipWS("element");
		consume("namespace");
		return new XQAST(XQ.DefaultElementNamespace);
	}

	private XQAST moduleImport() throws QueryException {
		Token la = laSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "module");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST prefix = null;
		Token la3 = laSkipWS("namespace");
		if (la != null) {
			consume(la3);
			XQAST ncname = ncnameLiteral(false, true);
			consumeSkipWS("=");
			XQAST ns = new XQAST(XQ.Namespace);
			ns.addChild(ncname);
			prefix = ns;
		}
		XQAST uri = uriLiteral(false, true);
		XQAST[] locs = new XQAST[0];
		if (attemptSkipWS("at")) {
			XQAST locUri;
			while ((locUri = uriLiteral(true, true)) != null) {
				add(locs, locUri);
			}
		}
		XQAST imp = new XQAST(XQ.ModuleImport);
		if (prefix != null) {
			imp.addChild(prefix);
		}
		imp.addChild(uri);
		imp.addChildren(locs);
		return imp;
	}

	private XQAST contextItemDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "context");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS("item");
		XQAST ctxItemDecl = new XQAST(XQ.ContextItemDeclaration);
		if (attemptSkipWS("as")) {
			ctxItemDecl.addChild(itemType());
		}
		if (attemptSkipWS(":=")) {
			ctxItemDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			ctxItemDecl.addChild(new XQAST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				ctxItemDecl.addChild(varDefaultValue());
			}
		}
		return ctxItemDecl;
	}

	private XQAST varValue() throws QueryException {
		return exprSingle();
	}

	private XQAST varDefaultValue() throws QueryException {
		return exprSingle();
	}

	private XQAST annotatedDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		// perform look ahead
		if ((laSkipWS("%") == null) && (laSkipWS("variable") == null)
				&& (laSkipWS("function") == null)) {
			return null;
		}
		consume(la);
		XQAST[] anns = new XQAST[0];
		XQAST ann = annotation();
		while (ann != null) {
			add(anns, ann);
			ann = annotation();
		}
		XQAST annDecl = new XQAST(XQ.AnnotatedDecl);
		annDecl.addChildren(anns);
		XQAST decl = varDecl();
		decl = (decl != null) ? decl : functionDecl();
		return annDecl;
	}

	private XQAST varDecl() throws QueryException {
		if (!attemptSkipWS("variable")) {
			return null;
		}
		consumeSkipWS("$");
		String varName = declare(eqnameLiteral(false, false).getValue());
		XQAST varDecl = new XQAST(XQ.TypedVariableDeclaration);
		varDecl.addChild(new XQAST(XQ.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			varDecl.addChild(typeDecl);
		}
		if (attemptSkipWS(":=")) {
			varDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			varDecl.addChild(new XQAST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				varDecl.addChild(varDefaultValue());
			}
		}
		return varDecl;
	}

	private XQAST functionDecl() throws QueryException {
		if (!attemptSkipWS("function")) {
			return null;
		}
		String varName = declare(eqnameLiteral(false, true).getValue());
		XQAST funcDecl = new XQAST(XQ.TypedVariableDeclaration);
		funcDecl.addChild(new XQAST(XQ.Qname, varName));
		consume("(");
		do {
			XQAST param = param();
			if (param == null) {
				break;
			}
			funcDecl.addChild(param);
		} while (attemptSkipWS(","));
		consume(")");
		if (attemptSkipWS("as")) {
			funcDecl.addChild(sequenceType());
		}
		if (attemptSkipWS("external")) {
			funcDecl.addChild(new XQAST(XQ.ExternalFunction));
		} else {
			funcDecl.addChild(functionBody());
		}
		return funcDecl;
	}

	private XQAST functionBody() throws QueryException {
		return enclosedExpr();
	}

	private XQAST optionDecl() throws QueryException {
		Token la = laSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "option");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST decl = new XQAST(XQ.OptionDeclaration);
		XQAST name = new XQAST(XQ.Literal);
		name.addChild(eqnameLiteral(false, true));
		XQAST value = new XQAST(XQ.Literal);
		value.addChild(stringLiteral(false, true));
		decl.addChild(name);
		decl.addChild(value);
		return decl;
	}

	private XQAST queryBody() throws QueryException {
		XQAST expr = expr();
		XQAST body = new XQAST(XQ.QueryBody);
		body.addChild(expr);
		return body;
	}

	private XQAST expr() throws QueryException {
		XQAST first = exprSingle();
		if (!attemptSkipWS(",")) {
			return first;
		}
		XQAST sequenceExpr = new XQAST(XQ.SequenceExpr);
		sequenceExpr.addChild(first);
		do {
			sequenceExpr.addChild(exprSingle());
		} while (attemptSkipWS(","));
		return sequenceExpr;
	}

	private XQAST exprSingle() throws QueryException {
		XQAST expr = flowrExpr();
		expr = (expr != null) ? expr : quantifiedExpr();
		expr = (expr != null) ? expr : switchExpr();
		expr = (expr != null) ? expr : typeswitchExpr();
		expr = (expr != null) ? expr : ifExpr();
		expr = (expr != null) ? expr : tryCatchExpr();
		expr = (expr != null) ? expr : orExpr();
		// TODO update expressions
		if (expr == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Non-expression faced: %s", paraphrase());
		}
		return expr;
	}

	private XQAST flowrExpr() throws QueryException {
		XQAST[] initialClause = initialClause();
		if ((initialClause == null) || (initialClause.length == 0)) {
			return null;
		}
		XQAST flworExpr = new XQAST(XQ.FlowrExpr);
		flworExpr.addChildren(initialClause);
		XQAST[] intermediateClause;
		while ((intermediateClause = intermediateClause()) != null) {
			flworExpr.addChildren(intermediateClause);
		}
		XQAST returnExpr = returnExpr();
		flworExpr.addChild(returnExpr);
		return flworExpr;
	}

	private XQAST[] initialClause() throws QueryException {
		XQAST[] clause = forClause();
		clause = (clause != null) ? clause : letClause();
		clause = (clause != null) ? clause : windowClause();
		return clause;
	}

	private XQAST[] forClause() throws QueryException {
		Token la = laSkipWS("for");
		if (la == null) {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'for'
		XQAST[] forClauses = new XQAST[0];
		do {
			forClauses = add(forClauses, forBinding());
		} while (attemptSkipWS(","));
		return forClauses;
	}

	private XQAST forBinding() throws QueryException {
		XQAST forClause = new XQAST(XQ.ForClause);
		forClause.addChild(typedVarBinding());
		if (attemptSkipWS("allowing")) {
			consumeSkipWS("empty");
			forClause.addChild(new XQAST(XQ.AllowingEmpty));
		}
		XQAST posVar = positionalVar();
		if (posVar != null) {
			forClause.addChild(posVar);
		}
		consumeSkipWS("in");
		forClause.addChild(exprSingle());
		return forClause;
	}

	private XQAST typedVarBinding() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST binding = new XQAST(XQ.TypedVariableBinding);
		binding.addChild(new XQAST(XQ.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		return binding;
	}

	private XQAST typeDeclaration() throws QueryException {
		if (!attemptSkipWS("as")) {
			return null;
		}
		return sequenceType();
	}

	private XQAST positionalVar() throws QueryException {
		if (!attemptSkipWS("at")) {
			return null;
		}
		consumeSkipWS("$");
		String varName = declare(eqnameLiteral(false, false).getValue());
		XQAST posVarBinding = new XQAST(XQ.TypedVariableBinding);
		posVarBinding.addChild(new XQAST(XQ.Variable, varName));
		return posVarBinding;
	}

	private String declare(String qname) {
		System.out.println("Declare " + qname);
		return qname;
	}

	private String resolve(String qname) {
		System.out.println("Resolve " + qname);
		return qname;
	}

	private XQAST[] letClause() throws QueryException {
		Token la = laSkipWS("let");
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'let'
		XQAST letClause = new XQAST(XQ.LetClause);
		letClause.addChild(typedVarBinding());
		consumeSkipWS(":=");
		letClause.addChild(exprSingle());
		return new XQAST[] { letClause };
	}

	private XQAST[] windowClause() throws QueryException {
		Token la = laSkipWS("for");
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, "sliding") != null) {
			consume(la);
			return new XQAST[] { tumblingWindowClause() };
		}
		if (laSkipWS(la, "tumbling") != null) {
			consume(la);
			return new XQAST[] { slidingWindowClause() };
		}
		return null;
	}

	private XQAST tumblingWindowClause() throws QueryException {
		Token la = laSkipWS("sliding");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST clause = new XQAST(XQ.TumblingWindowClause);

		consumeSkipWS("$");
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST binding = new XQAST(XQ.TypedVariableBinding);
		binding.addChild(new XQAST(XQ.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		if ((laSkipWS("only") != null) || (laSkipWS("end") != null)) {
			clause.addChild(windowEndCondition());
		}
		return clause;
	}

	private XQAST windowStartCondition() throws QueryException {
		consumeSkipWS("start");
		XQAST cond = new XQAST(XQ.WindowStartCondition);
		cond.addChildren(windowVars());
		consumeSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private XQAST windowEndCondition() throws QueryException {
		boolean only = attemptSkipWS("only");
		consumeSkipWS("end");
		XQAST cond = new XQAST(XQ.WindowEndCondition);
		cond.setProperty("only", Boolean.toString(only));
		cond.addChildren(windowVars());
		consumeSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private XQAST[] windowVars() throws QueryException {
		XQAST[] vars = new XQAST[0];
		if (attemptSkipWS("$")) {
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			XQAST binding = new XQAST(XQ.TypedVariableBinding);
			binding.addChild(new XQAST(XQ.Variable, varName));
			add(vars, binding);
		}
		XQAST posVar = positionalVar();
		if (posVar != null) {
			add(vars, posVar);
		}
		if (attemptSkipWS("previous")) {
			consumeSkipWS("$");
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			XQAST binding = new XQAST(XQ.PreviousItemBinding);
			binding.addChild(new XQAST(XQ.Variable, varName));
			add(vars, binding);
		}
		if (attemptSkipWS("next")) {
			consumeSkipWS("$");
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			XQAST binding = new XQAST(XQ.NextItemBinding);
			binding.addChild(new XQAST(XQ.Variable, varName));
			add(vars, binding);
		}
		return vars;
	}

	private XQAST slidingWindowClause() throws QueryException {
		Token la = laSkipWS("tumbling");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST clause = new XQAST(XQ.SlidingWindowClause);

		consumeSkipWS("$");
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST binding = new XQAST(XQ.TypedVariableBinding);
		binding.addChild(new XQAST(XQ.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		clause.addChild(windowEndCondition());
		return null;
	}

	private XQAST[] intermediateClause() throws QueryException {
		XQAST[] clauses = initialClause();
		if (clauses != null) {
			return clauses;
		}
		XQAST clause = whereClause();
		clause = (clause != null) ? clause : groupByClause();
		clause = (clause != null) ? clause : orderByClause();
		clause = (clause != null) ? clause : countClause();
		return (clause != null) ? new XQAST[] { clause } : null;
	}

	private XQAST whereClause() throws QueryException {
		if (!attemptSkipWS("where")) {
			return null;
		}
		XQAST whereClause = new XQAST(XQ.WhereClause);
		whereClause.addChild(exprSingle());
		return whereClause;
	}

	private XQAST groupByClause() throws QueryException {
		if (!attemptSkipWS("group")) {
			return null;
		}
		consumeSkipWS("by");
		XQAST groupByClause = new XQAST(XQ.GroupByClause);
		do {
			consumeSkipWS("$");
			XQAST gs = new XQAST(XQ.GroupBySpec);
			String varName = resolve(eqnameLiteral(false, false).getValue());
			gs.addChild(new XQAST(XQ.VariableRef, varName));
			if (attemptSkipWS("collation")) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQ.Collation);
				collation.addChild(uriLiteral);
				gs.addChild(collation);
			}
		} while (attemptSkipWS(","));
		return groupByClause;
	}

	private XQAST orderByClause() throws QueryException {
		if (attemptSkipWS("stable")) {
			consumeSkipWS("order");
		} else if (!attemptSkipWS("order")) {
			return null;
		}
		consumeSkipWS("by");
		XQAST orderByClause = new XQAST(XQ.OrderByClause);
		do {
			consumeSkipWS("$");
			XQAST os = new XQAST(XQ.OrderBySpec);
			os.addChild(exprSingle());
			if (attemptSkipWS("ascending")) {
				XQAST obk = new XQAST(XQ.OrderByKind);
				obk.addChild(new XQAST(XQ.ASCENDING));
				os.addChild(obk);
			} else if (attemptSkipWS("descending")) {
				XQAST obk = new XQAST(XQ.OrderByKind);
				obk.addChild(new XQAST(XQ.DESCENDING));
				os.addChild(obk);
			}
			if (attemptSkipWS("empty")) {
				if (attemptSkipWS("greatest")) {
					XQAST obem = new XQAST(XQ.OrderByEmptyMode);
					obem.addChild(new XQAST(XQ.GREATEST));
					os.addChild(obem);
				} else if (attemptSkipWS("least")) {
					XQAST obem = new XQAST(XQ.OrderByEmptyMode);
					obem.addChild(new XQAST(XQ.LEAST));
					os.addChild(obem);
				}
			}
			if (attemptSkipWS("collation")) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQ.Collation);
				collation.addChild(uriLiteral);
				os.addChild(collation);
			}
		} while (attemptSkipWS(","));
		return orderByClause;
	}

	private XQAST countClause() throws QueryException {
		Token la = laSkipWS("count");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "$");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		String varName = declare(eqnameLiteral(false, false).getValue());
		XQAST countClause = new XQAST(XQ.CountClause);
		countClause.addChild(new XQAST(XQ.Variable, varName));
		return countClause;
	}

	private XQAST returnExpr() throws QueryException {
		if (!attemptSkipWS("return")) {
			return null;
		}
		XQAST returnExpr = new XQAST(XQ.ReturnExpr);
		returnExpr.addChild(exprSingle());
		return returnExpr;
	}

	private XQAST quantifiedExpr() throws QueryException {
		XQAST quantifier;
		if (attemptSkipWS("some")) {
			quantifier = new XQAST(XQ.SomeQuantifier);
		} else if (attemptSkipWS("every")) {
			quantifier = new XQAST(XQ.EveryQuantifier);
		} else {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS("$") == null) {
			return null;
		}
		XQAST qExpr = new XQAST(XQ.QuantifiedExpr);
		qExpr.addChild(quantifier);
		qExpr.addChild(typedVarBinding());
		consumeSkipWS("in");
		qExpr.addChild(exprSingle());
		while (attemptSkipWS(",")) {
			XQAST binding = typedVarBinding();
			if (binding == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Expected variable binding: %s", paraphrase());
			}
			qExpr.addChild(binding);
			consumeSkipWS("in");
			qExpr.addChild(exprSingle());
		}
		consumeSkipWS("satisfies");
		qExpr.addChild(exprSingle());
		return qExpr;
	}

	private XQAST switchExpr() throws QueryException {
		Token la = laSkipWS("switch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST sExpr = new XQAST(XQ.SwitchExpr);
		sExpr.addChild(expr());
		consumeSkipWS(")");
		XQAST clause = switchClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected switch clause: %s", paraphrase());
		}
		sExpr.addChild(clause);
		while ((clause = switchClause()) != null) {
			sExpr.addChild(clause);
		}
		consumeSkipWS("default");
		consumeSkipWS("return");
		sExpr.addChild(exprSingle());
		return sExpr;
	}

	private XQAST switchClause() throws QueryException {
		if (!attemptSkipWS("case")) {
			return null;
		}
		XQAST clause = new XQAST(XQ.SwitchClause);
		clause.addChild(exprSingle());
		consumeSkipWS("return");
		clause.addChild(exprSingle());
		return clause;
	}

	private XQAST typeswitchExpr() throws QueryException {
		Token la = laSkipWS("typeswitch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST tsExpr = new XQAST(XQ.TypeSwitch);
		tsExpr.addChild(expr());
		consumeSkipWS(")");
		XQAST clause = caseClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected case clause: %s", paraphrase());
		}
		tsExpr.addChild(clause);
		while ((clause = caseClause()) != null) {
			tsExpr.addChild(clause);
		}
		consumeSkipWS("default");
		consumeSkipWS("return");
		tsExpr.addChild(exprSingle());
		return tsExpr;
	}

	private XQAST caseClause() throws QueryException {
		if (!attemptSkipWS("case")) {
			return null;
		}
		XQAST clause = new XQAST(XQ.TypeSwitchCase);
		clause.addChild(exprSingle());
		if (attemptSkipWS("$")) {
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			clause.addChild(new XQAST(XQ.Variable, varName));
			consumeSkipWS("as");
		}
		do {
			clause.addChild(sequenceType());
		} while (attemptSkipWS("|"));
		consumeSkipWS("return");
		clause.addChild(exprSingle());
		return clause;
	}

	private XQAST ifExpr() throws QueryException {
		Token la = laSkipWS("if");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST ifExpr = new XQAST(XQ.IfExpr);
		ifExpr.addChild(exprSingle());
		consumeSkipWS(")");
		consumeSkipWS("then");
		ifExpr.addChild(exprSingle());
		consumeSkipWS("else");
		ifExpr.addChild(exprSingle());
		return ifExpr;
	}

	private XQAST tryCatchExpr() throws QueryException {
		Token la = laSkipWS("try");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST tcExpr = new XQAST(XQ.TryCatchExpr);
		tcExpr.addChild(expr());
		consumeSkipWS("}");
		XQAST clause = tryClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected try clause: %s", paraphrase());
		}
		tcExpr.addChild(clause);
		while ((clause = tryClause()) != null) {
			tcExpr.addChild(clause);
		}
		return tcExpr;
	}

	private XQAST tryClause() throws QueryException {
		if (!attemptSkipWS("catch")) {
			return null;
		}
		XQAST clause = new XQAST(XQ.CatchClause);
		clause.addChild(catchErrorList());
		clause.addChild(catchVars());
		consumeSkipWS("{");
		clause.addChild(expr());
		consumeSkipWS("}");
		return clause;
	}

	private XQAST catchErrorList() throws QueryException {
		XQAST list = new XQAST(XQ.CatchErrorList);
		do {
			list.addChild(nameTest());
		} while (attemptSkipWS("|"));
		return list;
	}

	private XQAST catchVars() throws QueryException {
		consumeSkipWS("(");
		XQAST vars = new XQAST(XQ.CatchVar);
		consumeSkipWS("$");
		vars.addChild(eqnameLiteral(false, false));
		if (attemptSkipWS(",")) {
			consumeSkipWS("$");
			vars.addChild(eqnameLiteral(false, false));
			if (attemptSkipWS(",")) {
				consumeSkipWS("$");
				vars.addChild(eqnameLiteral(false, false));
			}
		}
		consumeSkipWS(")");
		return vars;
	}

	private XQAST orExpr() throws QueryException {
		XQAST first = andExpr();
		if (!attemptSkipWS("or")) {
			return first;
		}
		XQAST second = andExpr();
		XQAST expr = new XQAST(XQ.OrExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST andExpr() throws QueryException {
		XQAST first = comparisonExpr();
		if (!attemptSkipWS("and")) {
			return first;
		}
		XQAST second = comparisonExpr();
		XQAST expr = new XQAST(XQ.AndExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST comparisonExpr() throws QueryException {
		XQAST first = rangeExpr();
		XQAST cmp;
		if (attemptSkipWS("=")) {
			cmp = new XQAST(XQ.GeneralCompEQ);
		} else if (attemptSkipWS("!=")) {
			cmp = new XQAST(XQ.GeneralCompNE);
		} else if (attemptSkipWS("<")) {
			cmp = new XQAST(XQ.GeneralCompLT);
		} else if (attemptSkipWS("<=")) {
			cmp = new XQAST(XQ.GeneralCompLE);
		} else if (attemptSkipWS(">")) {
			cmp = new XQAST(XQ.GeneralCompGT);
		} else if (attemptSkipWS(">=")) {
			cmp = new XQAST(XQ.GeneralCompGE);
		} else if (attemptSkipWS("eq")) {
			cmp = new XQAST(XQ.ValueCompEQ);
		} else if (attemptSkipWS("neq")) {
			cmp = new XQAST(XQ.ValueCompNE);
		} else if (attemptSkipWS("lt")) {
			cmp = new XQAST(XQ.ValueCompLT);
		} else if (attemptSkipWS("le")) {
			cmp = new XQAST(XQ.ValueCompLE);
		} else if (attemptSkipWS("gt")) {
			cmp = new XQAST(XQ.ValueCompGT);
		} else if (attemptSkipWS("ge")) {
			cmp = new XQAST(XQ.ValueCompGE);
		} else if (attemptSkipWS("is")) {
			cmp = new XQAST(XQ.NodeCompIs);
		} else if (attemptSkipWS("<<")) {
			cmp = new XQAST(XQ.NodeCompPrecedes);
		} else if (attemptSkipWS(">>")) {
			cmp = new XQAST(XQ.NodeCompFollows);
		} else {
			return first;
		}
		XQAST second = comparisonExpr();
		XQAST expr = new XQAST(XQ.ComparisonExpr);
		expr.addChild(first);
		expr.addChild(cmp);
		expr.addChild(second);
		return expr;
	}

	private XQAST rangeExpr() throws QueryException {
		XQAST first = additiveExpr();
		if (!attemptSkipWS("to")) {
			return first;
		}
		XQAST second = additiveExpr();
		XQAST expr = new XQAST(XQ.RangeExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST additiveExpr() throws QueryException {
		XQAST first = multiplicativeExpr();
		while (true) {
			XQAST op;
			if (attemptSkipWS("+")) {
				op = new XQAST(XQ.AddOp);
			} else if (attemptSkipWS("-")) {
				op = new XQAST(XQ.SubtractOp);
			} else {
				return first;
			}
			XQAST second = multiplicativeExpr();
			XQAST expr = new XQAST(XQ.ArithmeticExpr);
			expr.addChild(first);
			expr.addChild(op);
			expr.addChild(second);
			first = expr;
		}
	}

	private XQAST multiplicativeExpr() throws QueryException {
		XQAST first = unionExpr();
		while (true) {
			XQAST op;
			if (attemptSkipWS("*")) {
				op = new XQAST(XQ.MultiplyOp);
			} else if (attemptSkipWS("div")) {
				op = new XQAST(XQ.DivideOp);
			} else if (attemptSkipWS("idiv")) {
				op = new XQAST(XQ.IDivideOp);
			} else if (attemptSkipWS("mod")) {
				op = new XQAST(XQ.DivideOp);
			} else {
				return first;
			}
			XQAST second = unionExpr();
			XQAST expr = new XQAST(XQ.ArithmeticExpr);
			expr.addChild(first);
			expr.addChild(op);
			expr.addChild(second);
			first = expr;
		}
	}

	private XQAST unionExpr() throws QueryException {
		XQAST first = intersectExpr();
		if ((!attemptSkipWS("union")) && (!attemptSkipWS("|"))) {
			return first;
		}
		XQAST second = intersectExpr();
		XQAST expr = new XQAST(XQ.UnionExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST intersectExpr() throws QueryException {
		XQAST first = instanceOfExpr();
		XQAST expr;
		if (attemptSkipWS("intersect")) {
			expr = new XQAST(XQ.IntersectExpr);
		} else if (attemptSkipWS("except")) {
			expr = new XQAST(XQ.ExceptExpr);
		} else {
			return first;
		}
		XQAST second = instanceOfExpr();
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST instanceOfExpr() throws QueryException {
		XQAST first = treatExpr();
		if (!attemptSkipWS("instance")) {
			return first;
		}
		consumeSkipWS("of");
		XQAST type = sequenceType();
		XQAST expr = new XQAST(XQ.InstanceofExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST sequenceType() throws QueryException {
		XQAST type = emptySequence();
		XQAST occInd = null;
		if (type == null) {
			type = itemType();
			occInd = occurrenceIndicator();
		}
		XQAST typeDecl = new XQAST(XQ.SequenceType);
		typeDecl.addChild(type);
		if (occInd != null) {
			typeDecl.addChild(occInd);
		}
		return typeDecl;
	}

	private XQAST emptySequence() throws QueryException {
		Token la = laSkipWS("empty-sequence");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.EmptySequenceType);
	}

	private XQAST anyKind() throws QueryException {
		Token la = laSkipWS("item");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.ItemType);
	}

	private XQAST occurrenceIndicator() {
		if (attemptSkipWS("?")) {
			return new XQAST(XQ.CardinalityZeroOrOne);
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQ.CardinalityZeroOrMany);
		}
		if (attemptSkipWS("+")) {
			return new XQAST(XQ.CardinalityOneOrMany);
		}
		return null;
	}

	private XQAST itemType() throws QueryException {
		XQAST type = kindTest();
		type = (type != null) ? type : anyKind();
		type = (type != null) ? type : functionTest();
		type = (type != null) ? type : atomicOrUnionType();
		type = (type != null) ? type : parenthesizedItemType();
		return type;
	}

	private XQAST functionTest() throws QueryException {
		XQAST funcTest = null;
		XQAST ann;
		while ((ann = annotation()) != null) {
			if (funcTest == null) {
				funcTest = new XQAST(XQ.FunctionTest);
			}
			funcTest.addChild(ann);
		}
		XQAST test = anyFunctionTest();
		test = (test != null) ? test : typedFunctionTest();
		if (test == null) {
			if (funcTest != null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Expected function test: %s", paraphrase());
			}
			return null;
		}
		if (funcTest == null) {
			funcTest = new XQAST(XQ.FunctionTest);
		}
		funcTest.addChild(test);
		return funcTest;
	}

	private XQAST annotation() throws QueryException {
		if (!attemptSkipWS("%")) {
			return null;
		}
		XQAST name = eqnameLiteral(false, true);
		XQAST ann = new XQAST(XQ.Annotation, name.getValue());
		if (attemptSkipWS("(")) {
			do {
				ann.addChild(stringLiteral(false, true));
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		return ann;
	}

	private XQAST anyFunctionTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, "*");
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consumeSkipWS(")");
		return new XQAST(XQ.AnyFunctionType);
	}

	private XQAST typedFunctionTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST typedFunc = new XQAST(XQ.TypedFunctionType);
		if (!attemptSkipWS(")")) {
			do {
				typedFunc.addChild(sequenceType());
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		consumeSkipWS("as");
		typedFunc.addChild(sequenceType());
		return typedFunc;
	}

	private XQAST atomicOrUnionType() throws QueryException {
		return eqnameLiteral(true, true);
	}

	private XQAST parenthesizedItemType() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		XQAST itemType = itemType();
		consumeSkipWS(")");
		return itemType;
	}

	private XQAST singleType() throws QueryException {
		XQAST aouType = atomicOrUnionType();
		if (aouType == null) {
			return null;
		}
		XQAST type = new XQAST(XQ.SingleType);
		type.addChild(aouType);
		if (attemptSkipWS("?")) {
			type.addChild(new XQAST(XQ.Optional));
		}
		return type;
	}

	private XQAST treatExpr() throws QueryException {
		XQAST first = castableExpr();
		if (!attemptSkipWS("treat")) {
			return first;
		}
		consumeSkipWS("as");
		XQAST type = sequenceType();
		XQAST expr = new XQAST(XQ.TreatExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST castableExpr() throws QueryException {
		XQAST first = castExpr();
		if (!attemptSkipWS("castable")) {
			return first;
		}
		consumeSkipWS("as");
		XQAST type = singleType();
		XQAST expr = new XQAST(XQ.CastableExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST castExpr() throws QueryException {
		XQAST first = unaryExpr();
		if (!attemptSkipWS("cast")) {
			return first;
		}
		consumeSkipWS("as");
		XQAST type = singleType();
		XQAST expr = new XQAST(XQ.CastExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST unaryExpr() throws QueryException {
		int minusCount = 0;
		while (true) {
			if (attemptSkipWS("+")) {
				continue;
			}
			if (attemptSkipWS("-")) {
				minusCount++;
				continue;
			}
			break;
		}
		if ((minusCount & 1) == 0) {
			return valueExpr();
		}
		XQAST second = valueExpr();
		XQAST first = new XQAST(XQ.Literal);
		first.addChild(new XQAST(XQ.Int, "-1"));
		XQAST expr = new XQAST(XQ.ArithmeticExpr);
		expr.addChild(first);
		expr.addChild(new XQAST(XQ.MultiplyOp));
		expr.addChild(second);
		return expr;
	}

	private XQAST valueExpr() throws QueryException {
		XQAST expr = validateExpr();
		expr = (expr != null) ? expr : pathExpr();
		expr = (expr != null) ? expr : extensionExpr();
		return expr;
	}

	private XQAST extensionExpr() throws QueryException {
		XQAST pragma = pragma();
		if (pragma == null) {
			return null;
		}
		XQAST eExpr = new XQAST(XQ.ExtensionExpr);
		eExpr.addChild(pragma);
		while ((pragma = pragma()) != null) {
			eExpr.addChild(pragma);
		}
		consumeSkipWS("{");
		if (!attemptSkipWS("}")) {
			eExpr.addChild(expr());
			consumeSkipWS("}");
		}
		return eExpr;
	}

	private XQAST pragma() throws QueryException {
		if (!attemptSkipWS("(#")) {
			return null;
		}
		XQAST pragma = new XQAST(XQ.Pragma);
		attemptWS();
		pragma.addChild(qnameLiteral(false, false));
		if (!attemptWS()) {
			pragma.addChild(pragmaContent());
		}
		consume("#)");
		return pragma;
	}

	private XQAST pathExpr() throws QueryException {
		// treatment of initial '/' and '//' is
		// delayed to relativePathExpr
		return relativePathExpr();
	}

	private XQAST relativePathExpr() throws QueryException {
		XQAST[] path = null;
		XQAST step;
		if (attemptSkipWS("//")) {
			step = stepExpr();
			if (step == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Incomplete path step: %s", paraphrase());
			}
			// initial '//' is translated to
			// fn:root(self::node()) treat as
			// document-node())/descendant-or-self::node()/
			XQAST treat = fnRootTreatAsDocument();
			XQAST dosn = descendantOrSelfNode();
			path = new XQAST[] { treat, dosn, step };
		} else if (attemptSkipWS("/")) {
			step = stepExpr();
			if (step == null) {
				// leading-lone-slash rule:
				// single '/' is translated to
				// (fn:root(self::node()) treat as document-node())
				return fnRootTreatAsDocument();
			}
			// initial '/' is translated to
			// (fn:root(self::node()) treat as document-node())/
			XQAST treat = fnRootTreatAsDocument();
			path = new XQAST[] { treat, step };
		} else {
			step = stepExpr();
			if (step == null) {
				return null;
			}
			path = new XQAST[] { step };
		}

		while (true) {
			if (attemptSkipWS("//")) {
				// intermediate '//' is translated to
				// descendant-or-self::node()/
				path = add(path, descendantOrSelfNode());
			} else if (!attemptSkipWS("/")) {
				break;
			}
			step = stepExpr();
			if (step == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Incomplete path step: %s", paraphrase());
			}
			path = add(path, step);
		}
		if (path.length == 1) {
			return path[0];
		}
		XQAST pathExpr = new XQAST(XQ.PathExpr);
		pathExpr.addChildren(path);
		return pathExpr;
	}

	private XQAST descendantOrSelfNode() {
		XQAST dosn = new XQAST(XQ.StepExpr);
		XQAST axisSpec = new XQAST(XQ.AxisSpec);
		axisSpec.addChild(new XQAST(XQ.DESCENDANT_OR_SELF));
		dosn.addChild(new XQAST(XQ.KindTestAnyKind));
		dosn.addChild(axisSpec);
		return dosn;
	}

	private XQAST fnRootTreatAsDocument() {
		XQAST treat = new XQAST(XQ.TreatExpr);
		XQAST call = new XQAST(XQ.FunctionCall, "fn:root");
		XQAST step = new XQAST(XQ.StepExpr);
		XQAST axisSpec = new XQAST(XQ.AxisSpec);
		axisSpec.addChild(new XQAST(XQ.SELF));
		step.addChild(new XQAST(XQ.KindTestAnyKind));
		step.addChild(axisSpec);
		XQAST seqType = new XQAST(XQ.SequenceType);
		seqType.addChild(new XQAST(XQ.KindTestDocument));
		call.addChild(step);
		treat.addChild(call);
		treat.addChild(seqType);
		return treat;
	}

	private XQAST stepExpr() throws QueryException {
		XQAST expr = postFixExpr();
		if (expr != null) {
			return expr;
		}
		return axisStep();
	}

	private XQAST axisStep() throws QueryException {
		XQAST[] step = forwardStep();
		if (step == null) {
			step = reverseStep();
		}
		if (step == null) {
			return null;
		}
		XQAST[] predicateList = predicateList();
		XQAST stepExpr = new XQAST(XQ.StepExpr);
		stepExpr.addChildren(step);
		if (predicateList != null) {
			stepExpr.addChildren(predicateList);
		}
		return stepExpr;
	}

	private XQAST[] forwardStep() throws QueryException {
		XQAST forwardAxis = forwardAxis();
		if (forwardAxis == null) {
			return abbrevForwardStep();
		}
		XQAST axisSpec = new XQAST(XQ.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new XQAST[] { axisSpec, nodeTest() };
	}

	private XQAST forwardAxis() {
		Token la;
		XQAST axis;
		if ((la = laSkipWS("child")) != null) {
			axis = new XQAST(XQ.CHILD);
		} else if ((la = laSkipWS("descendant")) != null) {
			axis = new XQAST(XQ.DESCENDANT);
		} else if ((la = laSkipWS("attribute")) != null) {
			axis = new XQAST(XQ.ATTRIBUTE);
		} else if ((la = laSkipWS("self")) != null) {
			axis = new XQAST(XQ.SELF);
		} else if ((la = laSkipWS("descendant-or-self")) != null) {
			axis = new XQAST(XQ.DESCENDANT_OR_SELF);
		} else if ((la = laSkipWS("following-sibling")) != null) {
			axis = new XQAST(XQ.FOLLOWING_SIBLING);
		} else if ((la = laSkipWS("following")) != null) {
			axis = new XQAST(XQ.FOLLOWING);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, "::");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private XQAST[] abbrevForwardStep() throws QueryException {
		boolean attributeAxis = false;
		if (attemptSkipWS("@")) {
			attributeAxis = true;
		} else {
			// look ahead if node test will
			// be attribute or schema-attribute test
			Token la = laSkipWS("attribute");
			if (la == null) {
				la = laSkipWS("schema-attribute");
			}
			if ((la != null) && (laSkipWS(la, "(") != null)) {
				attributeAxis = true;
			}
		}
		XQAST nodeTest = nodeTest();
		if (nodeTest == null) {
			return null;
		}
		if (attributeAxis) {
			XQAST axisSpec = new XQAST(XQ.AxisSpec);
			axisSpec.addChild(new XQAST(XQ.ATTRIBUTE));
			return new XQAST[] { axisSpec, nodeTest };
		} else {
			XQAST axisSpec = new XQAST(XQ.AxisSpec);
			axisSpec.addChild(new XQAST(XQ.CHILD));
			return new XQAST[] { axisSpec, nodeTest };
		}
	}

	private XQAST[] reverseStep() throws QueryException {
		XQAST forwardAxis = reverseAxis();
		if (forwardAxis == null) {
			return abbrevReverseStep();
		}
		XQAST axisSpec = new XQAST(XQ.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new XQAST[] { axisSpec, nodeTest() };
	}

	private XQAST reverseAxis() {
		Token la;
		XQAST axis;
		if ((la = laSkipWS("parent")) != null) {
			axis = new XQAST(XQ.PARENT);
		} else if ((la = laSkipWS("ancestor")) != null) {
			axis = new XQAST(XQ.ANCESTOR);
		} else if ((la = laSkipWS("preceding-sibling")) != null) {
			axis = new XQAST(XQ.PRECEDING_SIBLING);
		} else if ((la = laSkipWS("preceding")) != null) {
			axis = new XQAST(XQ.PRECEDING);
		} else if ((la = laSkipWS("ancestor-or-self")) != null) {
			axis = new XQAST(XQ.ANCESTOR_OR_SELF);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, "::");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private XQAST[] abbrevReverseStep() {
		if (!attemptSkipWS("..")) {
			return null;
		}
		XQAST axisSpec = new XQAST(XQ.AxisSpec);
		axisSpec.addChild(new XQAST(XQ.PARENT));
		XQAST nameTest = new XQAST(XQ.NameTest);
		nameTest.addChild(new XQAST(XQ.Wildcard));
		return new XQAST[] { axisSpec, nameTest };
	}

	private XQAST nodeTest() throws QueryException {
		XQAST test = kindTest();
		test = (test != null) ? test : nameTest();
		return test;
	}

	private XQAST kindTest() throws QueryException {
		XQAST test = documentTest();
		test = (test != null) ? test : elementTest();
		test = (test != null) ? test : attributeTest();
		test = (test != null) ? test : schemaElementTest();
		test = (test != null) ? test : schemaAttributeTest();
		test = (test != null) ? test : piTest();
		test = (test != null) ? test : commentTest();
		test = (test != null) ? test : textTest();
		test = (test != null) ? test : namespaceNodeTest();
		test = (test != null) ? test : anyKindTest();
		return test;
	}

	private XQAST documentTest() throws QueryException {
		Token la = laSkipWS("document-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST elTest = elementTest();
		XQAST schemaElTest = (elTest != null) ? elTest : schemaElementTest();
		consumeSkipWS(")");
		XQAST docTest = new XQAST(XQ.KindTestDocument);
		if (elTest != null) {
			docTest.addChild(elTest);
		}
		if (schemaElTest != null) {
			docTest.addChild(schemaElTest);
		}
		return docTest;
	}

	private XQAST elementTest() throws QueryException {
		Token la = laSkipWS("element");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST enow = elementNameOrWildcard();
		XQAST tn = null;
		XQAST nilled = null;
		if ((enow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new XQAST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		XQAST elTest = new XQAST(XQ.KindTestElement);
		if (enow != null) {
			elTest.addChild(enow);
		}
		if (tn != null) {
			elTest.addChild(tn);
		}
		if (nilled != null) {
			elTest.addChild(nilled);
		}
		return elTest;
	}

	private XQAST attributeTest() throws QueryException {
		Token la = laSkipWS("attribute");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST anow = attributeNameOrWildcard();
		XQAST tn = null;
		XQAST nilled = null;
		if ((anow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new XQAST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		XQAST attTest = new XQAST(XQ.KindTestAttribute);
		if (anow != null) {
			attTest.addChild(anow);
		}
		if (tn != null) {
			attTest.addChild(tn);
		}
		if (nilled != null) {
			attTest.addChild(nilled);
		}
		return attTest;
	}

	private XQAST schemaElementTest() throws QueryException {
		Token la = laSkipWS("schema-element");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		XQAST test = new XQAST(XQ.KindTestSchemaElement);
		test.addChild(name);
		return test;
	}

	private XQAST schemaAttributeTest() throws QueryException {
		Token la = laSkipWS("schema-attribute");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		XQAST test = new XQAST(XQ.KindTestSchemaAttribute);
		test.addChild(name);
		return test;
	}

	private XQAST piTest() throws QueryException {
		Token la = laSkipWS("processing-instruction");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = ncnameLiteral(true, true);
		name = (name != null) ? name : stringLiteral(false, true);
		consumeSkipWS(")");
		XQAST test = new XQAST(XQ.KindTestPi);
		test.addChild(name);
		return test;
	}

	private XQAST commentTest() throws QueryException {
		Token la = laSkipWS("comment");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.KindTestComment);
	}

	private XQAST textTest() throws QueryException {
		Token la = laSkipWS("text");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.KindTestText);
	}

	private XQAST namespaceNodeTest() throws QueryException {
		Token la = laSkipWS("namespace-node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.KindTestNamespaceNode);
	}

	private XQAST anyKindTest() throws QueryException {
		Token la = laSkipWS("node");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(")");
		return new XQAST(XQ.KindTestAnyKind);
	}

	private XQAST elementNameOrWildcard() throws QueryException {
		XQAST enow = eqnameLiteral(true, true);
		if (enow != null) {
			return enow;
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQ.Wildcard);
		}
		return null;
	}

	private XQAST attributeNameOrWildcard() throws QueryException {
		XQAST anow = eqnameLiteral(true, true);
		if (anow != null) {
			return anow;
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQ.Wildcard);
		}
		return null;
	}

	private XQAST nameTest() throws QueryException {
		XQAST test = eqnameLiteral(true, true);
		test = (test != null) ? test : wildcard();
		if (test == null) {
			return null;
		}
		XQAST nameTest = new XQAST(XQ.NameTest);
		nameTest.addChild(test);
		return nameTest;
	}

	private XQAST wildcard() throws QueryException {
		if (attemptSkipWS("*:")) {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			XQAST wbc = new XQAST(XQ.WildcardBeforeColon);
			wbc.addChild(ncname);
			return wbc;
		} else if (attemptSkipWS("*")) {
			return new XQAST(XQ.Wildcard);
		} else {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			if (!attempt(":*")) {
				return null;
			}
			XQAST wba = new XQAST(XQ.WildcardAfterColon);
			wba.addChild(ncname);
			return wba;
		}
	}

	private XQAST[] predicateList() throws QueryException {
		XQAST[] predicates = new XQAST[0];
		XQAST predicate;
		while ((predicate = predicate()) != null) {
			add(predicates, predicate);
		}
		return predicates;
	}

	private XQAST predicate() throws QueryException {
		if (!attemptSkipWS("[")) {
			return null;
		}
		XQAST pred = new XQAST(XQ.Predicate);
		pred.addChild(expr());
		consume("]");
		return pred;

	}

	private XQAST validateExpr() throws QueryException {
		if (!attemptSkipWS("validate")) {
			return null;
		}
		XQAST vExpr = new XQAST(XQ.ValidateExpr);
		if (attemptSkipWS("lax")) {
			vExpr.addChild(new XQAST(XQ.ValidateLax));
		} else if (attemptSkipWS("strict")) {
			vExpr.addChild(new XQAST(XQ.ValidateStrict));
		} else if (attemptSkipWS("type")) {
			vExpr.addChild(eqnameLiteral(false, true));
		} else {
			mismatch("lax", "strict", "type");
		}
		consumeSkipWS("{");
		vExpr.addChild(expr());
		consumeSkipWS("}");
		return vExpr;
	}

	private XQAST postFixExpr() throws QueryException {
		XQAST expr = primaryExpr();
		while (true) {
			XQAST predicate = predicate();
			if (predicate != null) {
				XQAST filterExpr = new XQAST(XQ.FilterExpr);
				filterExpr.addChild(expr);
				filterExpr.addChild(predicate);
				expr = filterExpr;
				continue;
			}
			XQAST[] argumentList = argumentList();
			if ((argumentList != null) && (argumentList.length > 0)) {
				XQAST dynFuncCallExpr = new XQAST(XQ.DynamicFunctionCallExpr);
				dynFuncCallExpr.addChild(expr);
				dynFuncCallExpr.addChildren(argumentList);
				expr = dynFuncCallExpr;
				continue;
			}
			break;
		}
		return expr;
	}

	private XQAST[] argumentList() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		XQAST[] args = new XQAST[0];
		while (!attemptSkipWS(")")) {
			if (args.length > 0) {
				consumeSkipWS(",");
			}
			add(args, argument());
		}
		return args;
	}

	private XQAST primaryExpr() throws QueryException {
		XQAST expr = literal();
		expr = (expr != null) ? expr : varRef();
		expr = (expr != null) ? expr : parenthesizedExpr();
		expr = (expr != null) ? expr : contextItemExpr();
		expr = (expr != null) ? expr : functionCall();
		expr = (expr != null) ? expr : orderedExpr();
		expr = (expr != null) ? expr : unorderedExpr();
		expr = (expr != null) ? expr : constructor();
		expr = (expr != null) ? expr : functionItemExpr();
		return expr;
	}

	private XQAST literal() throws QueryException {
		XQAST lit = numericLiteral();
		lit = (lit != null) ? lit : stringLiteral(true, true);
		if (lit == null) {
			return null;
		}
		XQAST literal = new XQAST(XQ.Literal);
		literal.addChild(lit);
		return lit;
	}

	private XQAST varRef() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		return new XQAST(XQ.VariableRef, resolve(eqnameLiteral(false, false)
				.getValue()));
	}

	private XQAST parenthesizedExpr() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		if (attemptSkipWS(")")) {
			return new XQAST(XQ.EmptySequence);
		}
		XQAST expr = expr();
		consumeSkipWS(")");
		return expr;
	}

	private XQAST contextItemExpr() {
		if (!attemptSkipWS(".")) {
			return null;
		}
		return new XQAST(XQ.ContextItemExpr);
	}

	private XQAST functionCall() throws QueryException {
		Token la;
		try {
			la = laEQNameSkipWS(true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			return null;
		}
		String funcName = la.string();
		if (isReservedFuncName(funcName)) {
			return null;
		}
		if (laSkipWS(la, "(") == null) {
			return null;
		}
		consume(la);
		XQAST call = new XQAST(XQ.FunctionCall, funcName);
		call.addChildren(argumentList());
		return call;
	}

	private XQAST argument() throws QueryException {
		// changed order to match '?' greedy
		if (attempt("?")) {
			return new XQAST(XQ.ArgumentPlaceHolder);
		}
		return exprSingle();
	}

	private boolean isReservedFuncName(String string) {
		for (String fun : RESERVED_FUNC_NAMES) {
			if (fun.equals(string)) {
				return true;
			}
		}
		return false;
	}

	private XQAST orderedExpr() throws QueryException {
		Token la = laSkipWS("ordered");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST expr = expr();
		consumeSkipWS("}");
		XQAST orderedExpr = new XQAST(XQ.OrderedExpr);
		orderedExpr.addChild(expr);
		return orderedExpr;
	}

	private XQAST unorderedExpr() throws QueryException {
		Token la = laSkipWS("unordered");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST expr = expr();
		consumeSkipWS("}");
		XQAST unorderedExpr = new XQAST(XQ.UnorderedExpr);
		unorderedExpr.addChild(expr);
		return unorderedExpr;
	}

	private XQAST constructor() throws QueryException {
		XQAST con = directConstructor();
		con = (con != null) ? con : computedConstructor();
		return con;
	}

	private XQAST directConstructor() throws QueryException {
		XQAST con = dirElemConstructor();
		con = (con != null) ? con : dirCommentConstructor();
		con = (con != null) ? con : dirPIConstructor();
		return con;
	}

	private XQAST dirElemConstructor() throws QueryException {
		if ((laSkipWS("</") != null) || (laSkipWS("<?") != null) || (!attemptSkipWS("<"))) {
			return null;
		}
//		skipS();
		XQAST stag = qnameLiteral(false, true);
		XQAST elem = new XQAST(XQ.CompElementConstructor);
		XQAST lit = new XQAST(XQ.Literal);
		lit.addChild(stag);
		elem.addChild(lit);
		XQAST cseq = new XQAST(XQ.ContentSequence);
		elem.addChild(cseq);
		XQAST att;
		while ((att = dirAttribute()) != null) {
			cseq.addChild(att);
		}
		if (attemptSkipWS("/>")) {
			return elem;
		}
		consume(">");
		push(stag.getValue());
		XQAST content;
		while ((content = dirElemContent()) != null) {
			cseq.addChild(content);
		}
		consume("</");
		XQAST etag = qnameLiteral(false, true);
		pop(etag.getValue());
//		skipS();
		consumeSkipWS(">");
		return elem;
	}

	private XQAST dirAttribute() throws QueryException {
		skipS();
		XQAST qname = qnameLiteral(true, false);
		if (qname == null) {
			return null;
		}
		skipS();
		consume("=");
		skipS();
		XQAST att = new XQAST(XQ.CompAttributeConstructor);
		XQAST lit = new XQAST(XQ.Literal);
		lit.addChild(qname);
		att.addChild(lit);
		XQAST cseq = new XQAST(XQ.ContentSequence);
		att.addChild(cseq);
		XQAST val;
		if (attempt("\"")) {
			while ((val = quotAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume("\"");
		} else {
			consume("'");
			while ((val = aposAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume("'");
		}
		return att;
	}

	private XQAST quotAttrValue() throws QueryException {
		Token la = la("\"");
		if (la != null) {
			if (la(la, "\"") != null) {
				consume("\"\"");
				XQAST lit = new XQAST(XQ.Literal);
				lit.addChild(new XQAST(XQ.Str, "\""));
				return lit;
			}
			return null;
		}
		return quotAttrValueContent();
	}

	private XQAST quotAttrValueContent() throws QueryException {
		Token content = laQuotAttrContentChar();
		if (content != null) {
			consume(content);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, content.string()));
			return lit;
		}
		return commonContent();
	}

	private XQAST aposAttrValue() throws QueryException {
		Token la = la("'");
		if (la != null) {
			if (la(la, "'") != null) {
				consume("''");
				XQAST lit = new XQAST(XQ.Literal);
				lit.addChild(new XQAST(XQ.Str, "'"));
				return lit;
			}
			return null;
		}
		return aposAttrValueContent();
	}

	private XQAST aposAttrValueContent() throws QueryException {
		Token content = laAposAttrContentChar();
		if (content != null) {
			consume(content);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, content.string()));
			return lit;
		}
		return commonContent();
	}

	private XQAST commonContent() throws QueryException {
		Token c;
		try {
			c = laPredefEntityRef(false);
			c = (c != null) ? c : laCharRef(false);
			c = (c != null) ? c : laEscapeCurly();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (c != null) {
			consume(c);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, c.string()));
			return lit;
		}
		return enclosedExpr();
	}

	private XQAST dirElemContent() throws QueryException {
		XQAST c = directConstructor();
		c = (c != null) ? c : cDataSection();
		c = (c != null) ? c : commonContent();
		c = (c != null) ? c : elementContentChar();
		return c;
	}

	private XQAST cDataSection() throws QueryException {
		if (!attempt("<![CDATA[")) {
			return null;
		}
		Token content = laCDataSectionContents();
		consume(content);
		consume("]]>");
		XQAST lit = new XQAST(XQ.Literal);
		lit.addChild(new XQAST(XQ.Str, content.string()));
		return lit;
	}

	private XQAST elementContentChar() {
		Token content = laElemContentChar();
		if (content == null) {
			return null;
		}
		consume(content);
		XQAST lit = new XQAST(XQ.Literal);
		lit.addChild(new XQAST(XQ.Str, content.string()));
		return lit;
	}

	private XQAST dirCommentConstructor() throws QueryException {
		if (!attempt("<!--")) {
			return null;
		}
		Token content;
		try {
			content = laCommentContents(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(content);
		consume("-->");
		XQAST comment = new XQAST(XQ.CompCommentConstructor);
		XQAST lit = new XQAST(XQ.Literal);
		lit.addChild(new XQAST(XQ.Str, content.string()));
		comment.addChild(lit);
		return comment;
	}

	private XQAST dirPIConstructor() throws QueryException {
		// "<?" PITarget (S DirPIContents)? "?>"
		if (!attempt("<?")) {
			return null;
		}
		Token target;
		try {
			target = laPITarget(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(target);
		XQAST piCon = new XQAST(XQ.DirPIConstructor);
		piCon.addChild(new XQAST(XQ.PITarget, target.string()));
		if (skipS()) {
			Token content;
			try {
				content = laPIContents();
			} catch (RuntimeException e) {
				throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
						.getMessage());
			}
			consume(content);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, content.string()));
			piCon.addChild(lit);
		}
		consume("?>");
		return piCon;
	}

	private XQAST computedConstructor() throws QueryException {
		XQAST c = compDocConstructor();
		c = (c != null) ? c : compElemConstructor();
		c = (c != null) ? c : compAttrConstructor();
		c = (c != null) ? c : compNamespaceConstructor();
		c = (c != null) ? c : compTextConstructor();
		c = (c != null) ? c : compCommentConstructor();
		c = (c != null) ? c : compPIConstructor();
		return c;
	}

	private XQAST compDocConstructor() throws QueryException {
		Token la = laSkipWS("document");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQ.CompDocumentConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private XQAST compElemConstructor() throws QueryException {
		Token la = laSkipWS("element");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laEQNameSkipWS(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST elem;
		if (la2 != null) {
			consume(la);
			consume(la2);
			elem = new XQAST(XQ.CompElementConstructor);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Qname, la2.string()));
			elem.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			elem = new XQAST(XQ.CompElementConstructor);
			elem.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQ.ContentSequence);
		elem.addChild(conSeq);
		XQAST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return elem;
	}

	private XQAST compAttrConstructor() throws QueryException {
		Token la = laSkipWS("attribute");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laEQNameSkipWS(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST attr;
		if (la2 != null) {
			consume(la);
			consume(la2);
			attr = new XQAST(XQ.CompAttributeConstructor);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Qname, la2.string()));
			attr.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			attr = new XQAST(XQ.CompAttributeConstructor);
			attr.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQ.ContentSequence);
		attr.addChild(conSeq);
		XQAST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return attr;
	}

	private XQAST compNamespaceConstructor() throws QueryException {
		Token la = laSkipWS("namespace");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laNCName(la);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST ns;
		if (la2 != null) {
			consume(la);
			consume(la2);
			ns = new XQAST(XQ.CompNamespaceConstructor);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, la2.string()));
			ns.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			ns = new XQAST(XQ.CompNamespaceConstructor);
			ns.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQ.ContentSequence);
		ns.addChild(conSeq);
		XQAST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return ns;
	}

	private XQAST compTextConstructor() throws QueryException {
		Token la = laSkipWS("text");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQ.CompTextConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private XQAST compCommentConstructor() throws QueryException {
		Token la = laSkipWS("comment");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQ.CompCommentConstructor);
		doc.addChild(expr());
		consume("}");
		return doc;
	}

	private XQAST compPIConstructor() throws QueryException {
		Token la = laSkipWS("processing-instruction");
		if (la == null) {
			return null;
		}
		Token la2;
		try {
			la2 = laNCName(la);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST pi;
		if (la2 != null) {
			consume(la);
			consume(la2);
			pi = new XQAST(XQ.CompProcessingInstructionConstructor);
			XQAST lit = new XQAST(XQ.Literal);
			lit.addChild(new XQAST(XQ.Str, la2.string()));
			pi.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			pi = new XQAST(XQ.CompProcessingInstructionConstructor);
			pi.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQ.ContentSequence);
		pi.addChild(conSeq);
		XQAST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS("}");
		return pi;
	}

	private XQAST functionItemExpr() throws QueryException {
		XQAST funcItem = literalFunctionItem();
		funcItem = (funcItem != null) ? funcItem : inlineFunction();
		return funcItem;
	}

	private XQAST literalFunctionItem() throws QueryException {
		Token la;
		try {
			la = laEQNameSkipWS(true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("#");
		if (la2 == null) {
			return null;
		}
		XQAST eqname = new XQAST(XQ.Qname, la.string());
		consume(la);
		consume(la2);
		XQAST no = integerLiteral(false, true);
		XQAST litFunc = new XQAST(XQ.LiteralFuncItem);
		litFunc.addChild(eqname);
		litFunc.addChild(no);
		return litFunc;
	}

	private XQAST inlineFunction() throws QueryException {
		Token la = laSkipWS("function");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST inlineFunc = new XQAST(XQ.InlineFuncItem);
		do {
			XQAST param = param();
			if (param == null) {
				break;
			}
			inlineFunc.addChild(param);
		} while (attemptSkipWS(","));
		consumeSkipWS(")");
		if (attemptSkipWS("as")) {
			inlineFunc.addChild(sequenceType());
		}
		inlineFunc.addChild(enclosedExpr());
		return inlineFunc;
	}

	private XQAST enclosedExpr() throws QueryException {
		if (!attemptSkipWS("{")) {
			return null;
		}
		XQAST expr = expr();
		consumeSkipWS("}");
		return expr;
	}

	private XQAST param() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST decl = new XQAST(XQ.TypedVariableDeclaration);
		decl.addChild(new XQAST(XQ.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			decl.addChild(typeDecl);
		}
		return decl;
	}

	private XQAST stringLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laStringSkipWS(cond) : laString(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected string literal: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Str, la.string());
	}

	private XQAST uriLiteral(boolean conditional, boolean skipWS)
			throws QueryException {
		return stringLiteral(conditional, skipWS);
	}

	private XQAST numericLiteral() throws QueryException {
		// re-ordered for greedy match
		XQAST lit = doubleLiteral(true, true);
		lit = (lit != null) ? lit : decimalLiteral(true, true);
		lit = (lit != null) ? lit : integerLiteral(true, true);
		return lit;
	}

	private XQAST ncnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laNCNameSkipWS() : laNCName();
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected NCName: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Qname, la.string());
	}

	private XQAST eqnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laEQNameSkipWS(cond) : laEQName(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Qname, la.string());
	}

	private XQAST qnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laQNameSkipWS() : laQName();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Qname, la.string());
	}

	private XQAST doubleLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laDoubleSkipWS(cond) : laDouble(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected double value: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Dbl, la.string());
	}

	private XQAST decimalLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laDecimalSkipWS(cond) : laDecimal(cond);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected decimal value: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Dec, la.string());
	}

	private XQAST integerLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laIntegerSkipWS(cond) : laInteger(cond);
		} catch (RuntimeException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected integer value: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQ.Int, la.string());
	}

	private XQAST pragmaContent() throws QueryException {
		Token la;
		try {
			la = laPragma(false);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume(la);
		return (la == null) ? null : new XQAST(XQ.PragmaContent, la.string());
	}

	private XQAST[] add(XQAST[] asts, XQAST ast) {
		int len = asts.length;
		asts = Arrays.copyOf(asts, len + 1);
		asts[len] = ast;
		return asts;
	}

	private void push(String name) {

	}

	private void pop(String name) {

	}

	protected void mismatch(String... expected) throws QueryException {
		throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
				"Expected one of %s: '%s'", Arrays.toString(expected),
				paraphrase());
	}
}
