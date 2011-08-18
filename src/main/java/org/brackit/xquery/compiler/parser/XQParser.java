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
		XQAST xquery = new XQAST(XQAST.XQuery);
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

		XQAST module = new XQAST(XQAST.LibraryModule);
		XQAST nsDecl = new XQAST(XQAST.NamespaceDeclaration);
		XQAST ncname = new XQAST(XQAST.Literal);
		ncname.addChild(ncn);
		nsDecl.addChild(ncname);
		XQAST str = new XQAST(XQAST.Literal);
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
		XQAST module = new XQAST(XQAST.MainModule);
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
			decl = new XQAST(XQAST.DefaultElementNamespace);
		} else {
			consumeSkipWS("function");
			decl = new XQAST(XQAST.DefaultFunctionNamespace);
		}
		consumeSkipWS("namespace");
		XQAST uri = uriLiteral(false, true);
		XQAST literal = new XQAST(XQAST.Literal);
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
		XQAST decl = new XQAST(XQAST.BoundarySpaceDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new XQAST(XQAST.BoundarySpaceModePreserve));
		} else {
			consumeSkipWS("strip");
			decl.addChild(new XQAST(XQAST.BoundarySpaceModeStrip));
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
		XQAST decl = new XQAST(XQAST.CollationDeclaration);
		XQAST coll = new XQAST(XQAST.Literal);
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
		XQAST decl = new XQAST(XQAST.BaseURIDeclaration);
		XQAST coll = new XQAST(XQAST.Literal);
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
		XQAST decl = new XQAST(XQAST.ConstructionDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new XQAST(XQAST.ConstructionModePreserve));
		} else {
			consumeSkipWS("strip");
			decl.addChild(new XQAST(XQAST.ConstructionModeStrip));
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
		XQAST decl = new XQAST(XQAST.OrderingModeDeclaration);
		if (attemptSkipWS("ordered")) {
			decl.addChild(new XQAST(XQAST.OrderingModeOrdered));
		} else {
			consume("unordered");
			decl.addChild(new XQAST(XQAST.OrderingModeUnordered));
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
		XQAST decl = new XQAST(XQAST.EmptyOrderDeclaration);
		if (attemptSkipWS("greatest")) {
			decl.addChild(new XQAST(XQAST.EmptyOrderModeGreatest));
		} else {
			consume("least");
			decl.addChild(new XQAST(XQAST.EmptyOrderModeLeast));
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
		XQAST decl = new XQAST(XQAST.CopyNamespacesDeclaration);
		decl.addChild(preserveMode());
		consumeSkipWS(",");
		decl.addChild(inheritMode());

		return decl;
	}

	private XQAST preserveMode() throws QueryException {
		if (attemptSkipWS("preserve")) {
			return new XQAST(XQAST.CopyNamespacesPreserveModePreserve);
		} else {
			consumeSkipWS("no-preserve");
			return new XQAST(XQAST.CopyNamespacesPreserveModeNoPreserve);
		}
	}

	private XQAST inheritMode() throws QueryException {
		if (attemptSkipWS("inherit")) {
			return new XQAST(XQAST.CopyNamespacesInheritModeInherit);
		} else {
			consumeSkipWS("no-inherit");
			return new XQAST(XQAST.CopyNamespacesInheritModeNoInherit);
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
		XQAST decl = new XQAST(XQAST.DecimalFormatDeclaration);
		;
		XQAST[] dfProperties = new XQAST[0];
		XQAST dfPropertyName;
		while ((dfPropertyName = dfPropertyName()) != null) {
			consumeSkipWS("=");
			XQAST value = new XQAST(XQAST.Literal);
			value.addChild(stringLiteral(false, true));
			XQAST dfp = new XQAST(XQAST.DecimalFormatProperty);
			dfp.addChild(dfPropertyName);
			dfp.addChild(value);
			add(dfProperties, dfp);
		}
		decl.addChildren(dfProperties);
		return decl;
	}

	private XQAST dfPropertyName() {
		if (attemptSkipWS("decimal-separator")) {
			return new XQAST(XQAST.DecimalFormatPropertyDecimalSeparator);
		} else if (attemptSkipWS("grouping-separator")) {
			return new XQAST(XQAST.DecimalFormatPropertyGroupingSeparator);
		} else if (attemptSkipWS("infinity")) {
			return new XQAST(XQAST.DecimalFormatPropertyInfinity);
		} else if (attemptSkipWS("minus-sign")) {
			return new XQAST(XQAST.DecimalFormatPropertyMinusSign);
		} else if (attemptSkipWS("NaN")) {
			return new XQAST(XQAST.DecimalFormatPropertyNaN);
		} else if (attemptSkipWS("percent")) {
			return new XQAST(XQAST.DecimalFormatPropertyPercent);
		} else if (attemptSkipWS("per-mille")) {
			return new XQAST(XQAST.DecimalFormatPropertyPerMille);
		} else if (attemptSkipWS("zero-digit")) {
			return new XQAST(XQAST.DecimalFormatPropertyZeroDigit);
		} else if (attemptSkipWS("digit")) {
			return new XQAST(XQAST.DecimalFormatPropertyDigit);
		} else if (attemptSkipWS("pattern-separator")) {
			return new XQAST(XQAST.DecimalFormatPropertyPatternSeparator);
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
		XQAST ncnLiteral = new XQAST(XQAST.Literal);
		ncnLiteral.addChild(ncname);
		consumeSkipWS("=");
		XQAST uri = uriLiteral(false, true);
		XQAST uriLiteral = new XQAST(XQAST.Literal);
		uriLiteral.addChild(uri);
		XQAST decl = new XQAST(XQAST.NamespaceDeclaration);
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
		XQAST imp = new XQAST(XQAST.SchemaImport);
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
			XQAST ns = new XQAST(XQAST.Namespace);
			ns.addChild(ncname);
			return ns;
		}
		la = laSkipWS("default");
		consumeSkipWS("element");
		consume("namespace");
		return new XQAST(XQAST.DefaultElementNamespace);
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
			XQAST ns = new XQAST(XQAST.Namespace);
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
		XQAST imp = new XQAST(XQAST.ModuleImport);
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
		XQAST ctxItemDecl = new XQAST(XQAST.ContextItemDeclaration);
		if (attemptSkipWS("as")) {
			ctxItemDecl.addChild(itemType());
		}
		if (attemptSkipWS(":=")) {
			ctxItemDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			ctxItemDecl.addChild(new XQAST(XQAST.ExternalVariable));
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
		XQAST annDecl = new XQAST(XQAST.AnnotatedDecl);
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
		XQAST varDecl = new XQAST(XQAST.TypedVariableDeclaration);
		varDecl.addChild(new XQAST(XQAST.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			varDecl.addChild(typeDecl);
		}
		if (attemptSkipWS(":=")) {
			varDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			varDecl.addChild(new XQAST(XQAST.ExternalVariable));
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
		XQAST funcDecl = new XQAST(XQAST.TypedVariableDeclaration);
		funcDecl.addChild(new XQAST(XQAST.Qname, varName));
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
			funcDecl.addChild(new XQAST(XQAST.ExternalFunction));
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
		XQAST decl = new XQAST(XQAST.OptionDeclaration);
		XQAST name = new XQAST(XQAST.Literal);
		name.addChild(eqnameLiteral(false, true));
		XQAST value = new XQAST(XQAST.Literal);
		value.addChild(stringLiteral(false, true));
		decl.addChild(name);
		decl.addChild(value);
		return decl;
	}

	private XQAST queryBody() throws QueryException {
		XQAST expr = expr();
		XQAST body = new XQAST(XQAST.QueryBody);
		body.addChild(expr);
		return body;
	}

	private XQAST expr() throws QueryException {
		XQAST first = exprSingle();
		if (!attemptSkipWS(",")) {
			return first;
		}
		XQAST sequenceExpr = new XQAST(XQAST.SequenceExpr);
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
		if (initialClause == null) {
			return null;
		}
		XQAST flworExpr = new XQAST(XQAST.FlowrExpr);
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
		XQAST forClause = new XQAST(XQAST.ForClause);
		forClause.addChild(typedVarBinding());
		if (attemptSkipWS("allowing")) {
			consumeSkipWS("empty");
			forClause.addChild(new XQAST(XQAST.AllowingEmpty));
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
		XQAST binding = new XQAST(XQAST.TypedVariableBinding);
		binding.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST posVarBinding = new XQAST(XQAST.TypedVariableBinding);
		posVarBinding.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST letClause = new XQAST(XQAST.LetClause);
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
		if (laSkipWS(la, "sliding") == null) {
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
		XQAST clause = new XQAST(XQAST.TumblingWindowClause);
		
		consumeSkipWS("$");
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST binding = new XQAST(XQAST.TypedVariableBinding);
		binding.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST cond = new XQAST(XQAST.WindowStartCondition);
		cond.addChildren(windowVars());
		consumeSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}
	
	private XQAST windowEndCondition() throws QueryException {		
		boolean only = attemptSkipWS("only");		
		consumeSkipWS("end");
		XQAST cond = new XQAST(XQAST.WindowEndCondition);
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
			XQAST binding = new XQAST(XQAST.TypedVariableBinding);
			binding.addChild(new XQAST(XQAST.Variable, varName));
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
			XQAST binding = new XQAST(XQAST.PreviousItemBinding);
			binding.addChild(new XQAST(XQAST.Variable, varName));
			add(vars, binding);
		}
		if (attemptSkipWS("next")) {
			consumeSkipWS("$");
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			XQAST binding = new XQAST(XQAST.NextItemBinding);
			binding.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST clause = new XQAST(XQAST.SlidingWindowClause);
		
		consumeSkipWS("$");
		XQAST eqname = eqnameLiteral(false, false);
		String varName = declare(eqname.getValue());
		XQAST binding = new XQAST(XQAST.TypedVariableBinding);
		binding.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST whereClause = new XQAST(XQAST.WhereClause);
		whereClause.addChild(exprSingle());
		return whereClause;
	}

	private XQAST groupByClause() throws QueryException {
		if (!attemptSkipWS("group")) {
			return null;
		}
		consumeSkipWS("by");
		XQAST groupByClause = new XQAST(XQAST.GroupByClause);
		do {
			consumeSkipWS("$");
			XQAST gs = new XQAST(XQAST.GroupBySpec);
			String varName = resolve(eqnameLiteral(false, false).getValue());
			gs.addChild(new XQAST(XQAST.VariableRef, varName));
			if (attemptSkipWS("collation")) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQAST.Collation);
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
		XQAST orderByClause = new XQAST(XQAST.OrderByClause);
		do {
			consumeSkipWS("$");
			XQAST os = new XQAST(XQAST.OrderBySpec);
			os.addChild(exprSingle());
			if (attemptSkipWS("ascending")) {
				XQAST obk = new XQAST(XQAST.OrderByKind);
				obk.addChild(new XQAST(XQAST.ASCENDING));
				os.addChild(obk);
			} else if (attemptSkipWS("descending")) {
				XQAST obk = new XQAST(XQAST.OrderByKind);
				obk.addChild(new XQAST(XQAST.DESCENDING));
				os.addChild(obk);
			}
			if (attemptSkipWS("empty")) {
				if (attemptSkipWS("greatest")) {
					XQAST obem = new XQAST(XQAST.OrderByEmptyMode);
					obem.addChild(new XQAST(XQAST.GREATEST));
					os.addChild(obem);
				} else if (attemptSkipWS("least")) {
					XQAST obem = new XQAST(XQAST.OrderByEmptyMode);
					obem.addChild(new XQAST(XQAST.LEAST));
					os.addChild(obem);
				}
			}
			if (attemptSkipWS("collation")) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQAST.Collation);
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
		XQAST countClause = new XQAST(XQAST.CountClause);
		countClause.addChild(new XQAST(XQAST.Variable, varName));
		return countClause;
	}

	private XQAST returnExpr() throws QueryException {
		if (!attemptSkipWS("return")) {
			return null;
		}
		XQAST returnExpr = new XQAST(XQAST.ReturnExpr);
		returnExpr.addChild(exprSingle());
		return returnExpr;
	}

	private XQAST quantifiedExpr() throws QueryException {
		XQAST quantifier;
		if (attemptSkipWS("some")) {
			quantifier = new XQAST(XQAST.SomeQuantifier);
		} else if (attemptSkipWS("every")) {
			quantifier = new XQAST(XQAST.EveryQuantifier);
		} else {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS("$") == null) {
			return null;
		}
		XQAST qExpr = new XQAST(XQAST.QuantifiedExpr);
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
		XQAST sExpr = new XQAST(XQAST.SwitchExpr);
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
		XQAST clause = new XQAST(XQAST.SwitchClause);
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
		XQAST tsExpr = new XQAST(XQAST.TypeSwitch);
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
		XQAST clause = new XQAST(XQAST.TypeSwitchCase);
		clause.addChild(exprSingle());
		if (attemptSkipWS("$")) {
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			clause.addChild(new XQAST(XQAST.Variable, varName));
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
		XQAST ifExpr = new XQAST(XQAST.IfExpr);
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
		XQAST tcExpr = new XQAST(XQAST.TryCatchExpr);
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
		XQAST clause = new XQAST(XQAST.CatchClause);
		clause.addChild(catchErrorList());
		clause.addChild(catchVars());
		consumeSkipWS("{");
		clause.addChild(expr());
		consumeSkipWS("}");
		return clause;
	}

	private XQAST catchErrorList() throws QueryException {
		XQAST list = new XQAST(XQAST.CatchErrorList);
		do {
			list.addChild(nameTest());
		} while (attemptSkipWS("|"));
		return list;
	}

	private XQAST catchVars() throws QueryException {
		consumeSkipWS("(");
		XQAST vars = new XQAST(XQAST.CatchVar);
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
		XQAST expr = new XQAST(XQAST.OrExpr);
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
		XQAST expr = new XQAST(XQAST.AndExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST comparisonExpr() throws QueryException {
		XQAST first = rangeExpr();
		XQAST cmp;
		if (attemptSkipWS("=")) {
			cmp = new XQAST(XQAST.GeneralCompEQ);
		} else if (attemptSkipWS("!=")) {
			cmp = new XQAST(XQAST.GeneralCompNE);
		} else if (attemptSkipWS("<")) {
			cmp = new XQAST(XQAST.GeneralCompLT);
		} else if (attemptSkipWS("<=")) {
			cmp = new XQAST(XQAST.GeneralCompLE);
		} else if (attemptSkipWS(">")) {
			cmp = new XQAST(XQAST.GeneralCompGT);
		} else if (attemptSkipWS(">=")) {
			cmp = new XQAST(XQAST.GeneralCompGE);
		} else if (attemptSkipWS("eq")) {
			cmp = new XQAST(XQAST.ValueCompEQ);
		} else if (attemptSkipWS("neq")) {
			cmp = new XQAST(XQAST.ValueCompNE);
		} else if (attemptSkipWS("lt")) {
			cmp = new XQAST(XQAST.ValueCompLT);
		} else if (attemptSkipWS("le")) {
			cmp = new XQAST(XQAST.ValueCompLE);
		} else if (attemptSkipWS("gt")) {
			cmp = new XQAST(XQAST.ValueCompGT);
		} else if (attemptSkipWS("ge")) {
			cmp = new XQAST(XQAST.ValueCompGE);
		} else if (attemptSkipWS("is")) {
			cmp = new XQAST(XQAST.NodeCompIs);
		} else if (attemptSkipWS("<<")) {
			cmp = new XQAST(XQAST.NodeCompPrecedes);
		} else if (attemptSkipWS(">>")) {
			cmp = new XQAST(XQAST.NodeCompFollows);
		} else {
			return first;
		}
		XQAST second = comparisonExpr();
		XQAST expr = new XQAST(XQAST.ComparisonExpr);
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
		XQAST expr = new XQAST(XQAST.RangeExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST additiveExpr() throws QueryException {
		XQAST first = multiplicativeExpr();
		XQAST op;
		if (attemptSkipWS("+")) {
			op = new XQAST(XQAST.AddOp);
		} else if (attemptSkipWS("-")) {
			op = new XQAST(XQAST.SubtractOp);
		} else {
			return first;
		}
		XQAST second = multiplicativeExpr();
		XQAST expr = new XQAST(XQAST.ArithmeticExpr);
		expr.addChild(first);
		expr.addChild(op);
		expr.addChild(second);
		return expr;
	}

	private XQAST multiplicativeExpr() throws QueryException {
		XQAST first = unionExpr();
		XQAST op;
		if (attemptSkipWS("*")) {
			op = new XQAST(XQAST.MultiplyOp);
		} else if (attemptSkipWS("/")) {
			op = new XQAST(XQAST.DivideOp);
		} else if (attemptSkipWS("idiv")) {
			op = new XQAST(XQAST.IDivideOp);
		} else if (attemptSkipWS("mod")) {
			op = new XQAST(XQAST.DivideOp);
		} else {
			return first;
		}
		XQAST second = unionExpr();
		XQAST expr = new XQAST(XQAST.ArithmeticExpr);
		expr.addChild(first);
		expr.addChild(op);
		expr.addChild(second);
		return expr;
	}

	private XQAST unionExpr() throws QueryException {
		XQAST first = intersectExpr();
		if ((!attemptSkipWS("union")) && (!attemptSkipWS("|"))) {
			return first;
		}
		XQAST second = intersectExpr();
		XQAST expr = new XQAST(XQAST.UnionExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private XQAST intersectExpr() throws QueryException {
		XQAST first = instanceOfExpr();
		XQAST expr;
		if (attemptSkipWS("intersect")) {
			expr = new XQAST(XQAST.IntersectExpr);
		} else if (attemptSkipWS("except")) {
			expr = new XQAST(XQAST.ExceptExpr);
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
		XQAST expr = new XQAST(XQAST.InstanceofExpr);
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
		XQAST typeDecl = new XQAST(XQAST.SequenceType);
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
		return new XQAST(XQAST.EmptySequenceType);
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
		return new XQAST(XQAST.ItemType);
	}

	private XQAST occurrenceIndicator() {
		if (attemptSkipWS("?")) {
			return new XQAST(XQAST.CardinalityZeroOrOne);
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQAST.CardinalityZeroOrMany);
		}
		if (attemptSkipWS("+")) {
			return new XQAST(XQAST.CardinalityOneOrMany);
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
				funcTest = new XQAST(XQAST.FunctionTest);
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
			funcTest = new XQAST(XQAST.FunctionTest);
		}
		funcTest.addChild(test);
		return funcTest;
	}

	private XQAST annotation() throws QueryException {
		if (!attemptSkipWS("%")) {
			return null;
		}
		XQAST name = eqnameLiteral(false, true);
		XQAST ann = new XQAST(XQAST.Annotation, name.getValue());
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
		return new XQAST(XQAST.AnyFunctionType);
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
		XQAST typedFunc = new XQAST(XQAST.TypedFunctionType);
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
		XQAST type = new XQAST(XQAST.SingleType);
		type.addChild(aouType);
		if (attemptSkipWS("?")) {
			type.addChild(new XQAST(XQAST.Optional));
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
		XQAST expr = new XQAST(XQAST.TreatExpr);
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
		XQAST expr = new XQAST(XQAST.CastableExpr);
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
		XQAST expr = new XQAST(XQAST.CastExpr);
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
		XQAST first = new XQAST(XQAST.Literal);
		first.addChild(new XQAST(XQAST.Int, "-1"));
		XQAST expr = new XQAST(XQAST.ArithmeticExpr);
		expr.addChild(first);
		expr.addChild(new XQAST(XQAST.MultiplyOp));
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
		XQAST eExpr = new XQAST(XQAST.ExtensionExpr);
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
		XQAST pragma = new XQAST(XQAST.Pragma);
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
		XQAST pathExpr = new XQAST(XQAST.PathExpr);
		pathExpr.addChildren(path);
		return pathExpr;
	}

	private XQAST descendantOrSelfNode() {
		XQAST dosn = new XQAST(XQAST.StepExpr);
		XQAST axisSpec = new XQAST(XQAST.AxisSpec);
		axisSpec.addChild(new XQAST(XQAST.DESCENDANT_OR_SELF));
		dosn.addChild(new XQAST(XQAST.KindTestAnyKind));
		dosn.addChild(axisSpec);
		return dosn;
	}

	private XQAST fnRootTreatAsDocument() {
		XQAST treat = new XQAST(XQAST.TreatExpr);
		XQAST call = new XQAST(XQAST.FunctionCall, "fn:root");
		XQAST step = new XQAST(XQAST.StepExpr);
		XQAST axisSpec = new XQAST(XQAST.AxisSpec);
		axisSpec.addChild(new XQAST(XQAST.SELF));
		step.addChild(new XQAST(XQAST.KindTestAnyKind));
		step.addChild(axisSpec);
		XQAST seqType = new XQAST(XQAST.SequenceType);
		seqType.addChild(new XQAST(XQAST.KindTestDocument));
		call.addChild(step);
		treat.addChild(call);
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
		XQAST stepExpr = new XQAST(XQAST.StepExpr);
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
		XQAST axisSpec = new XQAST(XQAST.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new XQAST[] { axisSpec, nodeTest() };
	}

	private XQAST forwardAxis() {
		Token la;
		XQAST axis;
		if ((la = laSkipWS("child")) != null) {
			axis = new XQAST(XQAST.CHILD);
		} else if ((la = laSkipWS("descendant")) != null) {
			axis = new XQAST(XQAST.DESCENDANT);
		} else if ((la = laSkipWS("attribute")) != null) {
			axis = new XQAST(XQAST.ATTRIBUTE);
		} else if ((la = laSkipWS("self")) != null) {
			axis = new XQAST(XQAST.SELF);
		} else if ((la = laSkipWS("descendant-or-self")) != null) {
			axis = new XQAST(XQAST.DESCENDANT_OR_SELF);
		} else if ((la = laSkipWS("following-sibling")) != null) {
			axis = new XQAST(XQAST.FOLLOWING_SIBLING);
		} else if ((la = laSkipWS("following")) != null) {
			axis = new XQAST(XQAST.FOLLOWING);
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
			XQAST axisSpec = new XQAST(XQAST.AxisSpec);
			axisSpec.addChild(new XQAST(XQAST.ATTRIBUTE));
			return new XQAST[] { axisSpec, nodeTest };
		} else {
			XQAST axisSpec = new XQAST(XQAST.AxisSpec);
			axisSpec.addChild(new XQAST(XQAST.CHILD));
			return new XQAST[] { axisSpec, nodeTest };
		}
	}

	private XQAST[] reverseStep() throws QueryException {
		XQAST forwardAxis = reverseAxis();
		if (forwardAxis == null) {
			return abbrevReverseStep();
		}
		XQAST axisSpec = new XQAST(XQAST.AxisSpec);
		axisSpec.addChild(forwardAxis);
		return new XQAST[] { axisSpec, nodeTest() };
	}

	private XQAST reverseAxis() {
		Token la;
		XQAST axis;
		if ((la = laSkipWS("parent")) != null) {
			axis = new XQAST(XQAST.PARENT);
		} else if ((la = laSkipWS("ancestor")) != null) {
			axis = new XQAST(XQAST.ANCESTOR);
		} else if ((la = laSkipWS("preceding-sibling")) != null) {
			axis = new XQAST(XQAST.PRECEDING_SIBLING);
		} else if ((la = laSkipWS("preceding")) != null) {
			axis = new XQAST(XQAST.PRECEDING);
		} else if ((la = laSkipWS("ancestor-or-self")) != null) {
			axis = new XQAST(XQAST.ANCESTOR_OR_SELF);
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
		XQAST axisSpec = new XQAST(XQAST.AxisSpec);
		axisSpec.addChild(new XQAST(XQAST.PARENT));
		XQAST nameTest = new XQAST(XQAST.NameTest);
		nameTest.addChild(new XQAST(XQAST.Wildcard));
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
		XQAST docTest = new XQAST(XQAST.KindTestDocument);
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
				nilled = new XQAST(XQAST.Nilled);
			}
		}
		consumeSkipWS(")");
		XQAST elTest = new XQAST(XQAST.KindTestElement);
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
				nilled = new XQAST(XQAST.Nilled);
			}
		}
		consumeSkipWS(")");
		XQAST attTest = new XQAST(XQAST.KindTestAttribute);
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
		XQAST test = new XQAST(XQAST.KindTestSchemaElement);
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
		XQAST test = new XQAST(XQAST.KindTestSchemaAttribute);
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
		XQAST test = new XQAST(XQAST.KindTestPi);
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
		return new XQAST(XQAST.KindTestComment);
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
		return new XQAST(XQAST.KindTestText);
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
		return new XQAST(XQAST.KindTestNamespaceNode);
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
		return new XQAST(XQAST.KindTestAnyKind);
	}

	private XQAST elementNameOrWildcard() throws QueryException {
		XQAST enow = eqnameLiteral(true, true);
		if (enow != null) {
			return enow;
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQAST.Wildcard);
		}
		return null;
	}

	private XQAST attributeNameOrWildcard() throws QueryException {
		XQAST anow = eqnameLiteral(true, true);
		if (anow != null) {
			return anow;
		}
		if (attemptSkipWS("*")) {
			return new XQAST(XQAST.Wildcard);
		}
		return null;
	}

	private XQAST nameTest() throws QueryException {
		XQAST test = eqnameLiteral(true, true);
		test = (test != null) ? test : wildcard();
		if (test == null) {
			return null;
		}
		XQAST nameTest = new XQAST(XQAST.NameTest);
		nameTest.addChild(test);
		return nameTest;
	}

	private XQAST wildcard() throws QueryException {
		if (attemptSkipWS("*:")) {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			XQAST wbc = new XQAST(XQAST.WildcardBeforeColon);
			wbc.addChild(ncname);
			return wbc;
		} else if (attemptSkipWS("*")) {
			return new XQAST(XQAST.Wildcard);
		} else {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			if (!attempt(":*")) {
				return null;
			}
			XQAST wba = new XQAST(XQAST.WildcardAfterColon);
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
		XQAST pred = new XQAST(XQAST.Predicate);
		pred.addChild(expr());
		consume("]");
		return pred;

	}

	private XQAST validateExpr() throws QueryException {
		if (!attemptSkipWS("validate")) {
			return null;
		}
		XQAST vExpr = new XQAST(XQAST.ValidateExpr);
		if (attemptSkipWS("lax")) {
			vExpr.addChild(new XQAST(XQAST.ValidateLax));
		} else if (attemptSkipWS("strict")) {
			vExpr.addChild(new XQAST(XQAST.ValidateStrict));
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
				XQAST filterExpr = new XQAST(XQAST.FilterExpr);
				filterExpr.addChild(expr);
				filterExpr.addChild(predicate);
				expr = filterExpr;
				continue;
			}
			XQAST[] argumentList = argumentList();
			if ((argumentList != null) && (argumentList.length > 0)) {
				XQAST dynFuncCallExpr = new XQAST(XQAST.DynamicFunctionCallExpr);
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
		XQAST arg = argument();
		if (arg != null) {
			add(args, arg);
			while (attemptSkipWS(",")) {
				add(args, argument());
			}
		}
		consumeSkipWS(")");
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
		XQAST literal = new XQAST(XQAST.Literal);
		literal.addChild(lit);
		return lit;
	}

	private XQAST varRef() throws QueryException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		return new XQAST(XQAST.VariableRef, resolve(eqnameLiteral(false, false)
				.getValue()));
	}

	private XQAST parenthesizedExpr() throws QueryException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		if (attemptSkipWS(")")) {
			return new XQAST(XQAST.EmptySequence);
		}
		XQAST expr = expr();
		consumeSkipWS(")");
		return expr;
	}

	private XQAST contextItemExpr() {
		if (!attemptSkipWS(".")) {
			return null;
		}
		return new XQAST(XQAST.ContextItemExpr);
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
		if (laSkipWS(la, "(") != null) {
			return null;
		}
		consume(la);
		XQAST call = new XQAST(XQAST.FunctionCall, funcName);
		call.addChildren(argumentList());
		return call;
	}

	private XQAST argument() throws QueryException {
		// changed order to match '?' greedy
		if (attempt("?")) {
			return new XQAST(XQAST.ArgumentPlaceHolder);
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
		XQAST orderedExpr = new XQAST(XQAST.OrderedExpr);
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
		XQAST unorderedExpr = new XQAST(XQAST.UnorderedExpr);
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
		if ((la("</") != null) || (la("<?") != null)
				|| (!attempt("<"))) {
			return null;
		}
		skipS();
		XQAST stag = qnameLiteral(false, false);
		XQAST elem = new XQAST(XQAST.CompElementConstructor);
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(stag);
		elem.addChild(lit);
		XQAST cseq = new XQAST(XQAST.ContentSequence);
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
		XQAST etag = qnameLiteral(false, false);
		pop(etag.getValue());
		skipS();
		consume(">");
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
		XQAST att = new XQAST(XQAST.CompAttributeConstructor);
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(qname);
		att.addChild(lit);
		XQAST cseq = new XQAST(XQAST.ContentSequence);
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
				XQAST lit = new XQAST(XQAST.Literal);
				lit.addChild(new XQAST(XQAST.Str, "\""));
				return lit;
			}
			return null;
		}
		return quotAttrValueContent();
	}

	private XQAST quotAttrValueContent() throws QueryException {
		String content = consumeQuotAttrContent();
		if (content != null) {
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, content));
			return lit;
		}
		return commonContent();
	}

	private XQAST aposAttrValue() throws QueryException {
		Token la = la("'");
		if (la != null) {
			if (la(la, "'") != null) {
				consume("''");
				XQAST lit = new XQAST(XQAST.Literal);
				lit.addChild(new XQAST(XQAST.Str, "'"));
				return lit;
			}
			return null;
		}
		return aposAttrValueContent();
	}

	private XQAST aposAttrValueContent() throws QueryException {
		String content = consumeAposAttrContent();
		if (content != null) {
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, content));
			return lit;
		}
		return commonContent();
	}

	private XQAST commonContent() throws QueryException {
		XQAST c = predefEntityRef();
		c = (c != null) ? c : charRef();
		c = (c != null) ? c : escapeCurly();
		c = (c != null) ? c : enclosedExpr();
		return c;
	}

	private XQAST predefEntityRef() throws QueryException {
		String ref;
		try {
			ref = consumePredefEntityRef();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (ref == null) {
			return null;
		}
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, ref));
		return lit;
	}

	private XQAST charRef() throws QueryException {
		String ref;
		try {
			ref = consumeCharRef();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (ref == null) {
			return null;
		}
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, ref));
		return lit;
	}

	private XQAST escapeCurly() {
		String curly;
		if (attempt("{{")) {
			curly = "{";
		} else if (attempt("}}")) {
			curly = "}";
		} else {
			return null;
		}
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, curly));
		return lit;
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
		String content = attemptCDataSectionContents();
		consume("]]>");
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, content));
		return lit;
	}

	private XQAST elementContentChar() {
		String content = consumeElemContentChar();
		if (content == null) {
			return null;
		}
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, content));
		return lit;
	}

	private XQAST dirCommentConstructor() throws QueryException {
		if (!attempt("<!--")) {
			return null;
		}
		String content;
		try {
			content = consumeCommentContents();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		consume("-->");
		XQAST comment = new XQAST(XQAST.CompCommentConstructor);
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(new XQAST(XQAST.Str, content));
		comment.addChild(lit);
		return comment;
	}

	private XQAST dirPIConstructor() throws QueryException {
		// "<?" PITarget (S DirPIContents)? "?>"
		if (!attempt("<?")) {
			return null;
		}
		String target;
		try {
			target = consumePITarget();
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST piCon = new XQAST(XQAST.DirPIConstructor);
		piCon.addChild(new XQAST(XQAST.PITarget, target));
		if (skipS()) {
			String content = comsumePIContents();
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, content));
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
		XQAST doc = new XQAST(XQAST.CompDocumentConstructor);
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
			elem = new XQAST(XQAST.CompElementConstructor);
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Qname, la2.string()));
			elem.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			elem = new XQAST(XQAST.CompElementConstructor);
			elem.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQAST.ContentSequence);
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
			attr = new XQAST(XQAST.CompAttributeConstructor);
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Qname, la2.string()));
			attr.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			attr = new XQAST(XQAST.CompAttributeConstructor);
			attr.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQAST.ContentSequence);
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
			la2 = laNCName(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST ns;
		if (la2 != null) {
			consume(la);
			consume(la2);
			ns = new XQAST(XQAST.CompNamespaceConstructor);
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, la2.string()));
			ns.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			ns = new XQAST(XQAST.CompNamespaceConstructor);
			ns.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQAST.ContentSequence);
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
		XQAST doc = new XQAST(XQAST.CompTextConstructor);
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
		XQAST doc = new XQAST(XQAST.CompCommentConstructor);
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
			la2 = laNCName(la, true);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		XQAST pi;
		if (la2 != null) {
			consume(la);
			consume(la2);
			pi = new XQAST(XQAST.CompProcessingInstructionConstructor);
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, la2.string()));
			pi.addChild(lit);
		} else {
			la2 = laSkipWS("{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			pi = new XQAST(XQAST.CompProcessingInstructionConstructor);
			pi.addChild(expr());
			consumeSkipWS("}");
		}
		consumeSkipWS("{");
		XQAST conSeq = new XQAST(XQAST.ContentSequence);
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
			return null;
		}
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS("#");
		if (la2 == null) {
			return null;
		}
		XQAST eqname = new XQAST(XQAST.Qname, la.string());
		consume(la);
		consume(la2);
		XQAST no = integerLiteral(false, true);
		XQAST litFunc = new XQAST(XQAST.LiteralFuncItem);
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
		XQAST inlineFunc = new XQAST(XQAST.InlineFuncItem);
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
		XQAST decl = new XQAST(XQAST.TypedVariableDeclaration);
		decl.addChild(new XQAST(XQAST.Variable, varName));
		XQAST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			decl.addChild(typeDecl);
		}
		return decl;
	}

	private XQAST stringLiteral(boolean conditional, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laStringSkipWS(conditional) : laString(conditional);
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		}
		if (la == null) {
			if (conditional) {
				return null;
			}
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected string literal: '%s'", paraphrase());
		}
		consume(la);
		return new XQAST(XQAST.Str, la.string());
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
			la = (skipWS) ? laNCNameSkipWS(cond) : laNCName(cond);
		} catch (Exception e) {
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
		return new XQAST(XQAST.Qname, la.string());
	}

	private XQAST eqnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laQNameSkipWS(cond) : laQName(cond);
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
		return new XQAST(XQAST.Qname, la.string());
	}

	private XQAST qnameLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laQNameSkipWS(cond) : laQName(cond);
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
		return new XQAST(XQAST.Qname, la.string());
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
		return new XQAST(XQAST.Dbl, la.string());
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
		return new XQAST(XQAST.Dec, la.string());
	}

	private XQAST integerLiteral(boolean cond, boolean skipWS)
			throws QueryException {
		Token la;
		try {
			la = (skipWS) ? laIntegerSkipWS(cond) : laInteger(cond);
		} catch (Exception e) {
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
		return new XQAST(XQAST.Int, la.string());
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
		return (la == null) ? null
				: new XQAST(XQAST.PragmaContent, la.string());
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
