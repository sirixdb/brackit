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
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Dec;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.module.Functions;
import org.brackit.xquery.util.log.Logger;

/**
 * Straight-forward, recursive descent parser.
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQParser extends Tokenizer {

	private static final Logger log = Logger.getLogger(XQParser.class);

	private static final String[] RESERVED_FUNC_NAMES = new String[] {
			"attribute", "comment", "document-node", "element",
			"empty-sequence", "function", "if", "item", "namespace-node",
			"node", "processing-instruction", "schema-attribute",
			"schema-element", "switch", "text", "typeswitch" };

	public class IllegalNestingException extends TokenizerException {
		private final String expected;

		public IllegalNestingException(String expected) {
			super("Expected closing tag <%s/>: '%s'", expected, paraphrase());
			this.expected = expected;
		}

		public String getExpected() {
			return expected;
		}
	}

	public class InvalidURIException extends TokenizerException {
		private final String uri;

		public InvalidURIException(String uri) {
			super("Invalid uri literal '%s': %s", uri, paraphrase());
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	private String version;

	public XQParser(String query) {
		super(query);
	}

	public AST parse() throws QueryException {
		try {
			AST module = module();
			if (module == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"No module found");
			}
			consumeEOF();
			AST xquery = new AST(XQ.XQuery);
			xquery.addChild(module);
			return xquery;
		} catch (IllegalCharRefException e) {
			throw new QueryException(e,
					ErrorCode.ERR_UNDEFINED_CHARACTER_REFERENCE, e.getMessage());
		} catch (InvalidURIException e) {
			throw new QueryException(e, ErrorCode.ERR_INVALID_URI_LITERAL,
					e.getMessage());
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR,
					e.getMessage());
		}
	}

	private void setXQVersion(String version) throws TokenizerException {
		if ("3.0".equals(version)) {
			this.version = version;
		} else if ("1.0".equals(version)) {
			this.version = version;
		} else if ("1.1".equals(version)) {
			this.version = version;
		} else {
			throw new TokenizerException("unsupported version '%s': %s",
					version, paraphrase());
		}
	}

	private void setEncoding(String encoding) {
		System.out.println("set encoding " + encoding);
	}

	private AST module() throws TokenizerException {
		versionDecl();
		AST module = libraryModule();
		if (module == null) {
			module = mainModule();
		}
		return module;
	}

	private boolean versionDecl() throws TokenizerException {
		Token la = laSymSkipWS("xquery");
		if (la == null) {
			return false;
		}
		Token la2 = laSymSkipWS(la, "version");
		if (la2 != null) {
			consume(la);
			consume(la2);
			setXQVersion(stringLiteral(false, true).getStringValue());
			if (attemptSymSkipWS("encoding")) {
				setEncoding(stringLiteral(false, true).getStringValue());
			}
		} else if ((la2 = laSymSkipWS(la, "encoding")) != null) {
			consume(la);
			consume(la2);
			setEncoding(stringLiteral(false, true).getStringValue());
		} else {
			return false;
		}
		consumeSkipWS(";");
		return true;
	}

	private AST libraryModule() throws TokenizerException {
		Token la = laSymSkipWS("module");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ncn = ncnameLiteral(false, true);
		consumeSkipWS("=");
		AST uri = uriLiteral(false, true);
		consumeSkipWS(";");

		AST module = new AST(XQ.LibraryModule);
		AST nsDecl = new AST(XQ.NamespaceDeclaration);
		nsDecl.addChild(ncn);
		nsDecl.addChild(uri);
		module.addChild(nsDecl);
		AST prolog = prolog();
		if (prolog != null) {
			module.addChild(prolog);
		}
		return module;
	}

	private AST mainModule() throws TokenizerException {
		AST prolog = prolog();
		AST body = queryBody();
		AST module = new AST(XQ.MainModule);
		if (prolog != null) {
			module.addChild(prolog);
		}
		module.addChild(body);
		return module;
	}

	private AST prolog() throws TokenizerException {
		AST prolog = new AST(XQ.Prolog);
		while (true) {
			AST def = defaultNamespaceDecl();
			def = (def != null) ? def : setter();
			def = (def != null) ? def : namespaceDecl();
			def = (def != null) ? def : importDecl();
			if (def != null) {
				consumeSkipWS(";");
				prolog.addChild(def);
			} else {
				break;
			}
		}
		while (true) {
			AST def = contextItemDecl();
			def = (def != null) ? def : annotatedDecl();
			def = (def != null) ? def : optionDecl();
			if (def != null) {
				consumeSkipWS(";");
				prolog.addChild(def);
			} else {
				break;
			}
		}
		return prolog;
	}

	private AST defaultNamespaceDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		boolean element = true;
		Token la3 = laSymSkipWS(la2, "element");
		if (la3 == null) {
			if ((la3 = laSymSkipWS(la2, "function")) == null) {
				return null;
			}
			element = false;
		}
		consume(la);
		consume(la2);
		consume(la3);
		AST decl;
		if (element) {
			decl = new AST(XQ.DefaultElementNamespace);
		} else {
			decl = new AST(XQ.DefaultFunctionNamespace);
		}
		consumeSkipWS("namespace");
		AST uri = uriLiteral(false, true);
		decl.addChild(uri);
		return decl;
	}

	private AST setter() throws TokenizerException {
		AST setter = boundarySpaceDecl();
		setter = (setter != null) ? setter : defaultCollationDecl();
		setter = (setter != null) ? setter : baseURIDecl();
		setter = (setter != null) ? setter : constructionDecl();
		setter = (setter != null) ? setter : orderingModeDecl();
		setter = (setter != null) ? setter : emptyOrderDecl();
		// Begin XQuery Update Facility 1.0
		setter = (setter != null) ? setter : revalidationDecl();
		// Begin XQuery Update Facility 1.0
		setter = (setter != null) ? setter : copyNamespacesDecl();
		setter = (setter != null) ? setter : decimalFormatDecl();
		return setter;
	}

	private AST boundarySpaceDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "boundary-space");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.BoundarySpaceDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new AST(XQ.BoundarySpaceModePreserve));
		} else if (attemptSkipWS("strip")) {
			decl.addChild(new AST(XQ.BoundarySpaceModeStrip));
		} else {
			throw new MismatchException("preserve", "strip");
		}
		return decl;
	}

	private AST defaultCollationDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSymSkipWS(la2, "collation");
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		AST decl = new AST(XQ.CollationDeclaration);
		decl.addChild(uriLiteral(false, true));
		return decl;
	}

	private AST baseURIDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "base-uri");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.BaseURIDeclaration);
		decl.addChild(uriLiteral(false, true));
		return decl;
	}

	private AST constructionDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "construction");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.ConstructionDeclaration);
		if (attemptSkipWS("preserve")) {
			decl.addChild(new AST(XQ.ConstructionModePreserve));
		} else if (attemptSkipWS("strip")) {
			decl.addChild(new AST(XQ.ConstructionModeStrip));
		} else {
			throw new MismatchException("preserve", "strip");
		}
		return decl;
	}

	private AST orderingModeDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "ordering");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.OrderingModeDeclaration);
		if (attemptSkipWS("ordered")) {
			decl.addChild(new AST(XQ.OrderingModeOrdered));
		} else if (attemptSkipWS("unordered")) {
			decl.addChild(new AST(XQ.OrderingModeUnordered));
		} else {
			throw new MismatchException("ordered", "unordered");
		}
		return decl;
	}

	private AST emptyOrderDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "default");
		if (la2 == null) {
			return null;
		}
		Token la3 = laSymSkipWS(la2, "order");
		if (la3 == null) {
			return null;
		}
		Token la4 = laSymSkipWS(la3, "empty");
		if (la4 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consume(la4);
		AST decl = new AST(XQ.EmptyOrderDeclaration);
		if (attemptSkipWS("greatest")) {
			decl.addChild(new AST(XQ.EmptyOrderModeGreatest));
		} else if (attemptSkipWS("least")) {
			decl.addChild(new AST(XQ.EmptyOrderModeLeast));
		} else {
			throw new MismatchException("greatest", "least");
		}
		return decl;
	}

	// Begin XQuery Update Facility 1.0
	private AST revalidationDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "revalidation");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.RevalidationDeclaration);
		if (attemptSkipWS("strict")) {
			decl.addChild(new AST(XQ.RevalidationModeStrict));
		} else if (attemptSkipWS("lax")) {
			decl.addChild(new AST(XQ.RevalidationModeLax));
		} else if (attemptSkipWS("skip")) {
			decl.addChild(new AST(XQ.RevalidationModeSkip));
		} else {
			throw new MismatchException("strict", "lax", "skip");
		}
		return decl;
	}

	// End XQuery Update Facility 1.0

	private AST copyNamespacesDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "copy-namespaces");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.CopyNamespacesDeclaration);
		decl.addChild(preserveMode());
		consumeSkipWS(",");
		decl.addChild(inheritMode());

		return decl;
	}

	private AST preserveMode() throws TokenizerException {
		if (attemptSkipWS("preserve")) {
			return new AST(XQ.CopyNamespacesPreserveModePreserve);
		} else if (attemptSkipWS("no-preserve")) {
			return new AST(XQ.CopyNamespacesPreserveModeNoPreserve);
		} else {
			throw new MismatchException("preserve", "no-preserve");
		}
	}

	private AST inheritMode() throws TokenizerException {
		if (attemptSkipWS("inherit")) {
			return new AST(XQ.CopyNamespacesInheritModeInherit);
		} else if (attemptSkipWS("no-inherit")) {
			return new AST(XQ.CopyNamespacesInheritModeNoInherit);
		} else {
			throw new MismatchException("inherit", "no-inherit");
		}
	}

	private AST decimalFormatDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}

		AST format;
		Token la2 = laSymSkipWS(la, "decimal-format");
		if (la2 != null) {
			consume(la);
			consume(la2);
			format = eqnameLiteral(false, true);
		} else if ((la2 = laSymSkipWS(la, "default")) != null) {
			Token la3 = laSymSkipWS(la2, "decimal-format");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			format = new AST(XQ.DecimalFormatDefault);
		} else {
			return null;
		}
		AST decl = new AST(XQ.DecimalFormatDeclaration);
		decl.addChild(format);
		AST[] dfProperties = new AST[0];
		AST dfPropertyName;
		while ((dfPropertyName = dfPropertyName()) != null) {
			consumeSkipWS("=");
			AST value = stringLiteral(false, true);
			AST dfp = new AST(XQ.DecimalFormatProperty);
			dfp.addChild(dfPropertyName);
			dfp.addChild(value);
			dfProperties = add(dfProperties, dfp);
		}
		decl.addChildren(dfProperties);
		return decl;
	}

	private AST dfPropertyName() {
		if (attemptSkipWS("decimal-separator")) {
			return new AST(XQ.DecimalFormatPropertyDecimalSeparator);
		} else if (attemptSkipWS("grouping-separator")) {
			return new AST(XQ.DecimalFormatPropertyGroupingSeparator);
		} else if (attemptSkipWS("infinity")) {
			return new AST(XQ.DecimalFormatPropertyInfinity);
		} else if (attemptSkipWS("minus-sign")) {
			return new AST(XQ.DecimalFormatPropertyMinusSign);
		} else if (attemptSkipWS("NaN")) {
			return new AST(XQ.DecimalFormatPropertyNaN);
		} else if (attemptSkipWS("percent")) {
			return new AST(XQ.DecimalFormatPropertyPercent);
		} else if (attemptSkipWS("per-mille")) {
			return new AST(XQ.DecimalFormatPropertyPerMille);
		} else if (attemptSkipWS("zero-digit")) {
			return new AST(XQ.DecimalFormatPropertyZeroDigit);
		} else if (attemptSkipWS("digit")) {
			return new AST(XQ.DecimalFormatPropertyDigit);
		} else if (attemptSkipWS("pattern-separator")) {
			return new AST(XQ.DecimalFormatPropertyPatternSeparator);
		} else {
			return null;
		}
	}

	private AST namespaceDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "namespace");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST prefix = ncnameLiteral(false, true);
		consumeSkipWS("=");
		AST uri = uriLiteral(false, true);
		AST decl = new AST(XQ.NamespaceDeclaration);
		decl.addChild(prefix);
		decl.addChild(uri);
		return decl;
	}

	private AST importDecl() throws TokenizerException {
		AST importDecl = schemaImport();
		return (importDecl != null) ? importDecl : moduleImport();
	}

	private AST schemaImport() throws TokenizerException {
		Token la = laSymSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "schema");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST schemaPrefix = schemaPrefix();
		if (schemaPrefix == null) {
			schemaPrefix = new AST(XQ.NamespaceDeclaration);
		}
		AST uri = uriLiteral(false, true);
		AST[] locs = new AST[0];
		if (attemptSymSkipWS("at")) {

			do {
				AST locUri = uriLiteral(true, true);
				locs = add(locs, locUri);
			} while (attemptSkipWS(","));
		}
		AST imp = new AST(XQ.SchemaImport);
		schemaPrefix.addChild(uri);
		imp.addChild(schemaPrefix);
		imp.addChildren(locs);
		return imp;
	}

	private AST schemaPrefix() throws TokenizerException {
		Token la = laSkipWS("namespace");
		if (la != null) {
			consume(la);
			AST ncname = ncnameLiteral(false, true);
			consumeSkipWS("=");
			AST ns = new AST(XQ.NamespaceDeclaration);
			ns.addChild(ncname);
			return ns;
		}
		la = laSymSkipWS("default");
		if (la == null) {
			return null;
		}
		consume(la);
		consumeSymSkipWS("element");
		consumeSymSkipWS("namespace");
		return new AST(XQ.DefaultElementNamespace);
	}

	private AST moduleImport() throws TokenizerException {
		Token la = laSymSkipWS("import");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "module");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ns = new AST(XQ.NamespaceDeclaration);
		AST prefix = null;
		Token la3 = laSkipWS("namespace");
		if (la != null) {
			consume(la3);
			prefix = ncnameLiteral(false, true);
			consumeSkipWS("=");
			ns.addChild(prefix);
		}
		AST uri = uriLiteral(false, true);
		ns.addChild(uri);
		AST[] locs = new AST[0];
		if (attemptSkipWS("at")) {
			AST locUri;
			while ((locUri = uriLiteral(true, true)) != null) {
				locs = add(locs, locUri);
			}
		}
		AST imp = new AST(XQ.ModuleImport);
		imp.addChild(ns);
		imp.addChildren(locs);
		return imp;
	}

	private AST contextItemDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "context");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSymSkipWS("item");
		AST ctxItemDecl = new AST(XQ.ContextItemDeclaration);
		if (attemptSymSkipWS("as")) {
			ctxItemDecl.addChild(itemType());
		} else {
			// default type is item()
			ctxItemDecl.addChild(new AST(XQ.ItemType));
		}
		if (attemptSkipWS(":=")) {
			ctxItemDecl.addChild(varValue());
		} else {
			consumeSymSkipWS("external");
			ctxItemDecl.addChild(new AST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				ctxItemDecl.addChild(varDefaultValue());
			}
		}
		return ctxItemDecl;
	}

	private AST varValue() throws TokenizerException {
		return exprSingle();
	}

	private AST varDefaultValue() throws TokenizerException {
		return exprSingle();
	}

	private AST annotatedDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		// perform look ahead
		if ((laSkipWS(la, "%") == null)
				&& (laSymSkipWS(la, "variable") == null)
				&& (laSymSkipWS(la, "function") == null)
				// Begin XQuery Update Facility 1.0
				&& (laSymSkipWS(la, "updating") == null)
		// End XQuery Update Facility 1.0
		) {
			return null;
		}
		consume(la);
		AST[] anns = new AST[0];
		AST ann;
		while ((ann = annotation()) != null) {
			anns = add(anns, ann);
		}
		AST decl = varDecl();
		decl = (decl != null) ? decl : functionDecl();
		for (AST a : anns) {
			decl.insertChild(0, a);
		}
		return decl;
	}

	private AST varDecl() throws TokenizerException {
		if (!attemptSymSkipWS("variable")) {
			return null;
		}
		consumeSkipWS("$");
		QNm varName = eqname(false, true);
		AST varDecl = new AST(XQ.TypedVariableDeclaration);
		varDecl.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			varDecl.addChild(typeDecl);
		}
		if (attemptSkipWS(":=")) {
			varDecl.addChild(varValue());
		} else {
			consumeSkipWS("external");
			varDecl.addChild(new AST(XQ.ExternalVariable));
			if (attemptSkipWS(":=")) {
				varDecl.addChild(varDefaultValue());
			}
		}
		return varDecl;
	}

	private AST functionDecl() throws TokenizerException {
		if (!attemptSymSkipWS("function")) {
			return null;
		}
		AST qname = eqnameLiteral(false, true);
		AST funcDecl = new AST(XQ.FunctionDecl);
		funcDecl.addChild(qname);
		consumeSkipWS("(");
		do {
			AST param = param();
			if (param == null) {
				break;
			}
			funcDecl.addChild(param);
		} while (attemptSkipWS(","));
		consumeSkipWS(")");
		if (attemptSymSkipWS("as")) {
			funcDecl.addChild(sequenceType());
		} else {
			// add item()* as default result type
			AST typeDecl = defaultFunctionResultType();
			funcDecl.addChild(typeDecl);
		}
		if (attemptSkipWS("external")) {
			funcDecl.addChild(new AST(XQ.ExternalFunction));
		} else {
			funcDecl.addChild(functionBody());
		}
		return funcDecl;
	}

	private AST defaultFunctionResultType() {
		AST typeDecl = new AST(XQ.SequenceType);
		typeDecl.addChild(new AST(XQ.ItemType));
		typeDecl.addChild(new AST(XQ.CardinalityZeroOrMany));
		return typeDecl;
	}

	private AST functionBody() throws TokenizerException {
		return enclosedExpr(true);
	}

	private AST optionDecl() throws TokenizerException {
		Token la = laSymSkipWS("declare");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "option");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST decl = new AST(XQ.OptionDeclaration);
		decl.addChild(eqnameLiteral(false, true));
		decl.addChild(stringLiteral(false, true));
		return decl;
	}

	private AST queryBody() throws TokenizerException {
		AST expr = expr();
		AST body = new AST(XQ.QueryBody);
		body.addChild(expr);
		return body;
	}

	private AST expr() throws TokenizerException {
		AST first = exprSingle();
		if (!attemptSkipWS(",")) {
			return first;
		}
		AST sequenceExpr = new AST(XQ.SequenceExpr);
		sequenceExpr.addChild(first);
		do {
			AST e = exprSingle();
			sequenceExpr.addChild(e);
		} while (attemptSkipWS(","));
		return sequenceExpr;
	}

	private AST exprSingle() throws TokenizerException {
		AST expr = flowrExpr();
		expr = (expr != null) ? expr : quantifiedExpr();
		expr = (expr != null) ? expr : switchExpr();
		expr = (expr != null) ? expr : typeswitchExpr();
		expr = (expr != null) ? expr : ifExpr();
		expr = (expr != null) ? expr : tryCatchExpr();
		// Begin XQuery Update Facility 1.0
		expr = (expr != null) ? expr : insertExpr();
		expr = (expr != null) ? expr : deleteExpr();
		expr = (expr != null) ? expr : renameExpr();
		expr = (expr != null) ? expr : replaceExpr();
		expr = (expr != null) ? expr : transformExpr();
		// End XQuery Update Facility 1.0
		expr = (expr != null) ? expr : orExpr();
		if (expr == null) {
			throw new TokenizerException("Non-expression faced: %s",
					paraphrase());
		}
		return expr;
	}

	// Begin XQuery Update Facility 1.0
	private AST insertExpr() throws TokenizerException {
		Token la = laSymSkipWS("insert");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "node");
		if (la2 == null) {
			la2 = laSymSkipWS(la, "nodes");
		}
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST src = exprSingle();
		AST targetChoice = insertExprTargetChoice();
		AST target = exprSingle();
		AST expr = new AST(XQ.InsertExpr);
		expr.addChild(targetChoice);
		expr.addChild(src);
		expr.addChild(target);
		return expr;
	}

	private AST insertExprTargetChoice() throws TokenizerException {
		if (attemptSymSkipWS("as")) {
			if (attemptSymSkipWS("first")) {
				consumeSymSkipWS("into");
				return new AST(XQ.InsertFirst);
			} else if (attemptSymSkipWS("last")) {
				consumeSymSkipWS("into");
				return new AST(XQ.InsertLast);
			} else {
				throw new MismatchException("first", "last");
			}
		} else if (attemptSymSkipWS("into")) {
			return new AST(XQ.InsertInto);
		} else if (attemptSymSkipWS("after")) {
			return new AST(XQ.InsertAfter);
		} else if (attemptSymSkipWS("before")) {
			return new AST(XQ.InsertBefore);
		} else {
			throw new MismatchException("as", "after", "before");
		}
	}

	private AST deleteExpr() throws TokenizerException {
		Token la = laSymSkipWS("delete");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "node");
		if (la2 == null) {
			la2 = laSymSkipWS(la, "nodes");
		}
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST target = exprSingle();
		AST expr = new AST(XQ.DeleteExpr);
		expr.addChild(target);
		return expr;
	}

	private AST renameExpr() throws TokenizerException {
		Token la = laSymSkipWS("rename");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "node");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST target = exprSingle();
		consumeSymSkipWS("as");
		AST newNameExpr = exprSingle();
		AST expr = new AST(XQ.RenameExpr);
		expr.addChild(target);
		expr.addChild(newNameExpr);
		return expr;
	}

	private AST replaceExpr() throws TokenizerException {
		Token la = laSymSkipWS("replace");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "value");
		if (la2 != null) {
			Token la3 = laSymSkipWS(la2, "of");
			if (la3 == null) {
				return null;
			}
			Token la4 = laSymSkipWS(la3, "node");
			if (la4 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			consume(la4);
			AST target = exprSingle();
			AST expr = new AST(XQ.ReplaceValueExpr);
			consumeSymSkipWS("with");
			AST newExpr = exprSingle();
			expr.addChild(target);
			expr.addChild(newExpr);
			return expr;
		} else {
			la2 = laSymSkipWS(la, "node");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			AST target = exprSingle();
			AST expr = new AST(XQ.ReplaceNodeExpr);
			consumeSymSkipWS("with");
			AST newExpr = exprSingle();
			expr.addChild(target);
			expr.addChild(newExpr);
			return expr;
		}
	}

	private AST transformExpr() throws TokenizerException {
		Token la = laSymSkipWS("copy");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "$");
		if (la2 == null) {
			return null;
		}
		consume(la); // consume 'copy'
		AST expr = new AST(XQ.TransformExpr);
		// add all copy variables
		do {
			consumeSkipWS("$");
			QNm varName = eqname(false, true);
			consumeSkipWS(":=");
			AST exprSingle = exprSingle();
			AST binding = new AST(XQ.CopyVariableBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			binding.addChild(exprSingle);
			expr.addChild(binding);
		} while (attemptSkipWS(","));
		consumeSymSkipWS("modify");
		expr.addChild(exprSingle());
		consumeSymSkipWS("return");
		expr.addChild(exprSingle());
		return expr;
	}

	// End XQuery Update Facility 1.0

	private AST flowrExpr() throws TokenizerException {
		AST[] initialClause = initialClause();
		if ((initialClause == null) || (initialClause.length == 0)) {
			return null;
		}
		AST flworExpr = new AST(XQ.FlowrExpr);
		flworExpr.addChildren(initialClause);
		AST[] intermediateClause;
		while ((intermediateClause = intermediateClause()) != null) {
			flworExpr.addChildren(intermediateClause);
		}
		AST returnExpr = returnClause();
		flworExpr.addChild(returnExpr);
		return flworExpr;
	}

	private AST[] initialClause() throws TokenizerException {
		AST[] clause = forClause();
		clause = (clause != null) ? clause : letClause();
		clause = (clause != null) ? clause : windowClause();
		return clause;
	}

	private AST[] forClause() throws TokenizerException {
		Token la = laSymSkipWS("for");
		if (la == null) {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'for'
		AST[] forClauses = new AST[0];
		do {
			forClauses = add(forClauses, forBinding());
		} while (attemptSkipWS(","));
		return forClauses;
	}

	private AST forBinding() throws TokenizerException {
		AST forClause = new AST(XQ.ForClause);
		forClause.addChild(typedVarBinding());
		if (attemptSymSkipWS("allowing")) {
			consumeSymSkipWS("empty");
			forClause.addChild(new AST(XQ.AllowingEmpty));
		}
		AST posVar = positionalVar();
		if (posVar != null) {
			forClause.addChild(posVar);
		}
		consumeSymSkipWS("in");
		forClause.addChild(exprSingle());
		return forClause;
	}

	private AST typedVarBinding() throws TokenizerException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		QNm varName = eqname(false, true);
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		return binding;
	}

	private AST typeDeclaration() throws TokenizerException {
		if (!attemptSymSkipWS("as")) {
			return null;
		}
		return sequenceType();
	}

	private AST positionalVar() throws TokenizerException {
		if (!attemptSymSkipWS("at")) {
			return null;
		}
		consumeSkipWS("$");
		QNm varName = eqname(false, true);
		AST posVarBinding = new AST(XQ.TypedVariableBinding);
		posVarBinding.addChild(new AST(XQ.Variable, varName));
		return posVarBinding;
	}

	private AST[] letClause() throws TokenizerException {
		Token la = laSymSkipWS("let");
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, "$") == null) {
			return null;
		}
		consume(la); // consume 'let'
		AST[] letClauses = new AST[0];
		do {
			letClauses = add(letClauses, letBinding());
		} while (attemptSkipWS(","));
		return letClauses;
	}

	private AST letBinding() throws TokenizerException {
		AST letClause = new AST(XQ.LetClause);
		letClause.addChild(typedVarBinding());
		consumeSkipWS(":=");
		letClause.addChild(exprSingle());
		return letClause;
	}

	private AST[] windowClause() throws TokenizerException {
		Token la = laSymSkipWS("for");
		if (la == null) {
			return null;
		}
		if (laSymSkipWS(la, "sliding") != null) {
			consume(la);
			AST clause = tumblingWindowClause();
			return new AST[] { clause };
		}
		if (laSymSkipWS(la, "tumbling") != null) {
			consume(la);
			AST clause = slidingWindowClause();
			return new AST[] { clause };
		}
		return null;
	}

	private AST tumblingWindowClause() throws TokenizerException {
		Token la = laSymSkipWS("sliding");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST clause = new AST(XQ.TumblingWindowClause);
		consumeSkipWS("$");
		QNm varName = eqname(false, true);
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSymSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		if ((laSymSkipWS("only") != null) || (laSymSkipWS("end") != null)) {
			clause.addChild(windowEndCondition());
		}
		return clause;
	}

	private AST windowStartCondition() throws TokenizerException {
		consumeSymSkipWS("start");
		AST cond = new AST(XQ.WindowStartCondition);
		cond.addChild(windowVars());
		consumeSymSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private AST windowEndCondition() throws TokenizerException {
		boolean only = attemptSymSkipWS("only");
		consumeSymSkipWS("end");
		AST cond = new AST(XQ.WindowEndCondition);
		cond.setProperty("only", only);
		cond.addChild(windowVars());
		consumeSymSkipWS("when");
		cond.addChild(exprSingle());
		return cond;
	}

	private AST windowVars() throws TokenizerException {
		AST vars = new AST(XQ.WindowVars);
		if (attemptSkipWS("$")) {
			QNm varName = eqname(false, true);
			AST binding = new AST(XQ.TypedVariableBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars.addChild(binding);
		}
		AST posVar = positionalVar();
		if (posVar != null) {
			vars.addChild(posVar);
		}
		if (attemptSymSkipWS("previous")) {
			consumeSkipWS("$");
			QNm varName = eqname(false, true);
			AST binding = new AST(XQ.PreviousItemBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars.addChild(binding);
		}
		if (attemptSymSkipWS("next")) {
			consumeSkipWS("$");
			QNm varName = eqname(false, true);
			AST binding = new AST(XQ.NextItemBinding);
			binding.addChild(new AST(XQ.Variable, varName));
			vars.addChild(binding);
		}
		return vars;
	}

	private AST slidingWindowClause() throws TokenizerException {
		Token la = laSymSkipWS("tumbling");
		if (la == null) {
			return null;
		}
		Token la2 = laSymSkipWS(la, "window");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST clause = new AST(XQ.SlidingWindowClause);

		consumeSkipWS("$");
		QNm varName = eqname(false, true);
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			binding.addChild(typeDecl);
		}
		clause.addChild(binding);
		consumeSymSkipWS("in");
		clause.addChild(exprSingle());
		clause.addChild(windowStartCondition());
		clause.addChild(windowEndCondition());
		return null;
	}

	private AST[] intermediateClause() throws TokenizerException {
		AST[] clauses = initialClause();
		if (clauses != null) {
			return clauses;
		}
		AST clause = whereClause();
		clause = (clause != null) ? clause : groupByClause();
		clause = (clause != null) ? clause : orderByClause();
		clause = (clause != null) ? clause : countClause();
		return (clause != null) ? new AST[] { clause } : null;
	}

	private AST whereClause() throws TokenizerException {
		if (!attemptSymSkipWS("where")) {
			return null;
		}
		AST whereClause = new AST(XQ.WhereClause);
		whereClause.addChild(exprSingle());
		return whereClause;
	}

	private AST groupByClause() throws TokenizerException {
		if (!attemptSymSkipWS("group")) {
			return null;
		}
		consumeSymSkipWS("by");
		AST groupByClause = new AST(XQ.GroupByClause);
		do {
			consumeSkipWS("$");
			AST gs = new AST(XQ.GroupBySpec);
			QNm varName = eqname(false, true);
			gs.addChild(new AST(XQ.VariableRef, varName));
			if (attemptSymSkipWS("collation")) {
				AST uriLiteral = uriLiteral(false, true);
				AST collation = new AST(XQ.Collation);
				collation.addChild(uriLiteral);
				gs.addChild(collation);
			}
		} while (attemptSkipWS(","));
		return groupByClause;
	}

	private AST orderByClause() throws TokenizerException {
		if (attemptSymSkipWS("stable")) {
			consumeSymSkipWS("order");
		} else if (!attemptSymSkipWS("order")) {
			return null;
		}
		consumeSymSkipWS("by");
		AST clause = new AST(XQ.OrderByClause);
		do {
			AST os = new AST(XQ.OrderBySpec);
			clause.addChild(os);
			os.addChild(exprSingle());
			if (attemptSymSkipWS("ascending")) {
				AST obk = new AST(XQ.OrderByKind);
				obk.addChild(new AST(XQ.ASCENDING));
				os.addChild(obk);
			} else if (attemptSymSkipWS("descending")) {
				AST obk = new AST(XQ.OrderByKind);
				obk.addChild(new AST(XQ.DESCENDING));
				os.addChild(obk);
			}
			if (attemptSymSkipWS("empty")) {
				if (attemptSymSkipWS("greatest")) {
					AST obem = new AST(XQ.OrderByEmptyMode);
					obem.addChild(new AST(XQ.GREATEST));
					os.addChild(obem);
				} else if (attemptSymSkipWS("least")) {
					AST obem = new AST(XQ.OrderByEmptyMode);
					obem.addChild(new AST(XQ.LEAST));
					os.addChild(obem);
				}
			}
			if (attemptSymSkipWS("collation")) {
				AST uriLiteral = uriLiteral(false, true);
				AST collation = new AST(XQ.Collation);
				collation.addChild(uriLiteral);
				os.addChild(collation);
			}
		} while (attemptSkipWS(","));
		return clause;
	}

	private AST countClause() throws TokenizerException {
		Token la = laSymSkipWS("count");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "$");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		QNm varName = eqname(false, true);
		AST countClause = new AST(XQ.CountClause);
		countClause.addChild(new AST(XQ.Variable, varName));
		return countClause;
	}

	private AST returnClause() throws TokenizerException {
		consumeSymSkipWS("return");
		AST returnExpr = new AST(XQ.ReturnClause);
		returnExpr.addChild(exprSingle());
		return returnExpr;
	}

	private AST quantifiedExpr() throws TokenizerException {
		AST quantifier;
		if (attemptSymSkipWS("some")) {
			quantifier = new AST(XQ.SomeQuantifier);
		} else if (attemptSymSkipWS("every")) {
			quantifier = new AST(XQ.EveryQuantifier);
		} else {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS("$") == null) {
			return null;
		}
		AST qExpr = new AST(XQ.QuantifiedExpr);
		qExpr.addChild(quantifier);
		qExpr.addChild(typedVarBinding());
		consumeSymSkipWS("in");
		qExpr.addChild(exprSingle());
		while (attemptSkipWS(",")) {
			AST binding = typedVarBinding();
			if (binding == null) {
				throw new TokenizerException("Expected variable binding: %s",
						paraphrase());
			}
			qExpr.addChild(binding);
			consumeSymSkipWS("in");
			qExpr.addChild(exprSingle());
		}
		consumeSymSkipWS("satisfies");
		qExpr.addChild(exprSingle());
		return qExpr;
	}

	private AST switchExpr() throws TokenizerException {
		Token la = laSymSkipWS("switch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST sExpr = new AST(XQ.SwitchExpr);
		sExpr.addChild(expr());
		consumeSkipWS(")");
		AST clause = switchClause();
		if (clause == null) {
			throw new TokenizerException("Expected switch clause: %s",
					paraphrase());
		}
		sExpr.addChild(clause);
		while ((clause = switchClause()) != null) {
			sExpr.addChild(clause);
		}
		consumeSymSkipWS("default");
		consumeSymSkipWS("return");
		sExpr.addChild(exprSingle());
		return sExpr;
	}

	private AST switchClause() throws TokenizerException {
		if (!attemptSymSkipWS("case")) {
			return null;
		}
		AST clause = new AST(XQ.SwitchClause);
		do {
			clause.addChild(exprSingle());
		} while (!attemptSymSkipWS("return"));
		clause.addChild(exprSingle());
		return clause;
	}

	private AST typeswitchExpr() throws TokenizerException {
		Token la = laSymSkipWS("typeswitch");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST tsExpr = new AST(XQ.TypeSwitch);
		tsExpr.addChild(expr());
		consumeSkipWS(")");
		AST clause = caseClause();
		if (clause == null) {
			throw new TokenizerException("Expected case clause: %s",
					paraphrase());
		}
		tsExpr.addChild(clause);
		while ((clause = caseClause()) != null) {
			tsExpr.addChild(clause);
		}
		consumeSymSkipWS("default");
		AST dftClause = new AST(XQ.TypeSwitchCase);
		if (attemptSkipWS("$")) {
			QNm varName = eqname(false, true);
			dftClause.addChild(new AST(XQ.Variable, varName));
		}
		consumeSymSkipWS("return");
		dftClause.addChild(exprSingle());
		tsExpr.addChild(dftClause);
		return tsExpr;
	}

	private AST caseClause() throws TokenizerException {
		if (!attemptSymSkipWS("case")) {
			return null;
		}
		AST clause = new AST(XQ.TypeSwitchCase);
		if (attemptSkipWS("$")) {
			QNm varName = eqname(false, true);
			clause.addChild(new AST(XQ.Variable, varName));
			consumeSymSkipWS("as");
		}
		do {
			clause.addChild(sequenceType());
		} while (attemptSkipWS("|"));
		consumeSymSkipWS("return");
		clause.addChild(exprSingle());
		return clause;
	}

	private AST ifExpr() throws TokenizerException {
		Token la = laSymSkipWS("if");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "(");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST ifExpr = new AST(XQ.IfExpr);
		ifExpr.addChild(exprSingle());
		consumeSkipWS(")");
		consumeSymSkipWS("then");
		ifExpr.addChild(exprSingle());
		consumeSymSkipWS("else");
		ifExpr.addChild(exprSingle());
		return ifExpr;
	}

	private AST tryCatchExpr() throws TokenizerException {
		Token la = laSymSkipWS("try");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST tcExpr = new AST(XQ.TryCatchExpr);
		tcExpr.addChild(expr());
		consumeSkipWS("}");
		AST clause = tryClause();
		if (clause == null) {
			throw new TokenizerException("Expected try clause: %s",
					paraphrase());
		}
		tcExpr.addChild(clause);
		while ((clause = tryClause()) != null) {
			tcExpr.addChild(clause);
		}
		return tcExpr;
	}

	private AST tryClause() throws TokenizerException {
		if (!attemptSymSkipWS("catch")) {
			return null;
		}
		AST clause = new AST(XQ.CatchClause);
		clause.addChild(catchErrorList());
		consumeSkipWS("{");
		clause.addChild(expr());
		consumeSkipWS("}");
		return clause;
	}

	private AST catchErrorList() throws TokenizerException {
		AST list = new AST(XQ.CatchErrorList);
		do {
			list.addChild(nameTest(false));
		} while (attemptSkipWS("|"));
		return list;
	}

	private AST orExpr() throws TokenizerException {
		AST first = andExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			if (!attemptSymSkipWS("or")) {
				return first;
			}
			AST second = andExpr();
			AST expr = new AST(XQ.OrExpr);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST andExpr() throws TokenizerException {
		AST first = comparisonExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			if (!attemptSymSkipWS("and")) {
				return first;
			}
			AST second = comparisonExpr();
			AST expr = new AST(XQ.AndExpr);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST comparisonExpr() throws TokenizerException {
		AST first = rangeExpr();
		if (first == null) {
			return null;
		}
		AST cmp;
		if (attemptSkipWS("=")) {
			cmp = new AST(XQ.GeneralCompEQ);
		} else if (attemptSkipWS("!=")) {
			cmp = new AST(XQ.GeneralCompNE);
		} else if (attemptSkipWS("<=")) {
			cmp = new AST(XQ.GeneralCompLE);
		} else if (attemptSkipWS("<<")) {
			cmp = new AST(XQ.NodeCompPrecedes);
		} else if (attemptSkipWS("<")) {
			cmp = new AST(XQ.GeneralCompLT);
		} else if (attemptSkipWS(">=")) {
			cmp = new AST(XQ.GeneralCompGE);
		} else if (attemptSkipWS(">>")) {
			cmp = new AST(XQ.NodeCompFollows);
		} else if (attemptSkipWS(">")) {
			cmp = new AST(XQ.GeneralCompGT);
		} else if (attemptSymSkipWS("eq")) {
			cmp = new AST(XQ.ValueCompEQ);
		} else if (attemptSymSkipWS("ne")) {
			cmp = new AST(XQ.ValueCompNE);
		} else if (attemptSymSkipWS("lt")) {
			cmp = new AST(XQ.ValueCompLT);
		} else if (attemptSymSkipWS("le")) {
			cmp = new AST(XQ.ValueCompLE);
		} else if (attemptSymSkipWS("gt")) {
			cmp = new AST(XQ.ValueCompGT);
		} else if (attemptSymSkipWS("ge")) {
			cmp = new AST(XQ.ValueCompGE);
		} else if (attemptSymSkipWS("is")) {
			cmp = new AST(XQ.NodeCompIs);
		} else {
			return first;
		}
		AST second = comparisonExpr();
		AST expr = new AST(XQ.ComparisonExpr);
		expr.addChild(cmp);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST rangeExpr() throws TokenizerException {
		AST first = additiveExpr();
		if (first == null) {
			return null;
		}
		if (!attemptSymSkipWS("to")) {
			return first;
		}
		AST second = additiveExpr();
		AST expr = new AST(XQ.RangeExpr);
		expr.addChild(first);
		expr.addChild(second);
		return expr;
	}

	private AST additiveExpr() throws TokenizerException {
		AST first = multiplicativeExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			AST op;
			if (attemptSkipWS("+")) {
				op = new AST(XQ.AddOp);
			} else if (attemptSkipWS("-")) {
				op = new AST(XQ.SubtractOp);
			} else {
				return first;
			}
			AST second = multiplicativeExpr();
			AST expr = new AST(XQ.ArithmeticExpr);
			expr.addChild(op);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST multiplicativeExpr() throws TokenizerException {
		AST first = unionExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			AST op;
			if (attemptSkipWS("*")) {
				op = new AST(XQ.MultiplyOp);
			} else if (attemptSymSkipWS("div")) {
				op = new AST(XQ.DivideOp);
			} else if (attemptSymSkipWS("idiv")) {
				op = new AST(XQ.IDivideOp);
			} else if (attemptSymSkipWS("mod")) {
				op = new AST(XQ.ModulusOp);
			} else {
				return first;
			}
			AST second = unionExpr();
			AST expr = new AST(XQ.ArithmeticExpr);
			expr.addChild(op);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST unionExpr() throws TokenizerException {
		AST first = intersectExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			if ((!attemptSymSkipWS("union")) && (!attemptSkipWS("|"))) {
				return first;
			}
			AST second = intersectExpr();
			AST expr = new AST(XQ.UnionExpr);
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST intersectExpr() throws TokenizerException {
		AST first = instanceOfExpr();
		if (first == null) {
			return null;
		}
		while (true) {
			AST expr;
			if (attemptSymSkipWS("intersect")) {
				expr = new AST(XQ.IntersectExpr);
			} else if (attemptSymSkipWS("except")) {
				expr = new AST(XQ.ExceptExpr);
			} else {
				return first;
			}
			AST second = instanceOfExpr();
			expr.addChild(first);
			expr.addChild(second);
			first = expr;
		}
	}

	private AST instanceOfExpr() throws TokenizerException {
		AST first = treatExpr();
		if (first == null) {
			return null;
		}
		if (!attemptSymSkipWS("instance")) {
			return first;
		}
		consumeSymSkipWS("of");
		AST type = sequenceType();
		AST expr = new AST(XQ.InstanceofExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST sequenceType() throws TokenizerException {
		AST type = emptySequence();
		AST occInd = null;
		if (type == null) {
			type = itemType();
			occInd = occurrenceIndicator();
		}
		AST typeDecl = new AST(XQ.SequenceType);
		typeDecl.addChild(type);
		if (occInd != null) {
			typeDecl.addChild(occInd);
		}
		return typeDecl;
	}

	private AST emptySequence() throws TokenizerException {
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
		return new AST(XQ.EmptySequenceType);
	}

	private AST anyKind() throws TokenizerException {
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
		return new AST(XQ.ItemType);
	}

	private AST occurrenceIndicator() {
		if (attemptSkipWS("?")) {
			return new AST(XQ.CardinalityZeroOrOne);
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.CardinalityZeroOrMany);
		}
		if (attemptSkipWS("+")) {
			return new AST(XQ.CardinalityOneOrMany);
		}
		return null;
	}

	private AST itemType() throws TokenizerException {
		AST type = kindTest();
		type = (type != null) ? type : anyKind();
		type = (type != null) ? type : functionTest();
		type = (type != null) ? type : atomicOrUnionType();
		type = (type != null) ? type : parenthesizedItemType();
		return type;
	}

	private AST functionTest() throws TokenizerException {
		AST funcTest = null;
		AST ann;
		while ((ann = annotation()) != null) {
			if (funcTest == null) {
				funcTest = new AST(XQ.FunctionTest);
			}
			funcTest.addChild(ann);
		}
		AST test = anyFunctionTest();
		test = (test != null) ? test : typedFunctionTest();
		if (test == null) {
			if (funcTest != null) {
				throw new TokenizerException("Expected function test: %s",
						paraphrase());
			}
			return null;
		}
		if (funcTest == null) {
			funcTest = new AST(XQ.FunctionTest);
		}
		funcTest.addChild(test);
		return funcTest;
	}

	private AST annotation() throws TokenizerException {
		// Begin XQuery Update Facility 1.0
		// treat old-school updating keyword as special "annotation"
		if (attemptSymSkipWS("updating")) {
			return new AST(XQ.Annotation, "updating");
		}
		// End XQuery Update Facility 1.0
		if (!attemptSkipWS("%")) {
			return null;
		}
		QNm eqname = eqname(false, true);
		AST ann = new AST(XQ.Annotation, eqname);
		if (attemptSkipWS("(")) {
			do {
				ann.addChild(stringLiteral(false, true));
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		return ann;
	}

	private AST anyFunctionTest() throws TokenizerException {
		Token la = laSkipWS("function");
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
		return new AST(XQ.AnyFunctionType);
	}

	private AST typedFunctionTest() throws TokenizerException {
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
		AST typedFunc = new AST(XQ.TypedFunctionType);
		if (!attemptSkipWS(")")) {
			do {
				typedFunc.addChild(sequenceType());
			} while (attemptSkipWS(","));
			consumeSkipWS(")");
		}
		consumeSymSkipWS("as");
		typedFunc.addChild(sequenceType());
		return typedFunc;
	}

	private AST atomicOrUnionType() throws TokenizerException {
		AST eqname = eqnameLiteral(true, true);
		AST aouType = new AST(XQ.AtomicOrUnionType);
		aouType.addChild(eqname);
		return aouType;
	}

	private AST parenthesizedItemType() throws TokenizerException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		AST itemType = itemType();
		consumeSkipWS(")");
		return itemType;
	}

	private AST singleType() throws TokenizerException {
		AST aouType = atomicOrUnionType();
		if (aouType == null) {
			return null;
		}
		AST type = new AST(XQ.SequenceType);
		type.addChild(aouType);
		if (attemptSkipWS("?")) {
			type.addChild(new AST(XQ.CardinalityZeroOrOne));
		}
		return type;
	}

	private AST treatExpr() throws TokenizerException {
		AST first = castableExpr();
		if (first == null) {
			return null;
		}
		if (!attemptSymSkipWS("treat")) {
			return first;
		}
		consumeSymSkipWS("as");
		AST type = sequenceType();
		AST expr = new AST(XQ.TreatExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST castableExpr() throws TokenizerException {
		AST first = castExpr();
		if (first == null) {
			return null;
		}
		if (!attemptSymSkipWS("castable")) {
			return first;
		}
		consumeSymSkipWS("as");
		AST type = singleType();
		AST expr = new AST(XQ.CastableExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST castExpr() throws TokenizerException {
		AST first = unaryExpr();
		if (first == null) {
			return null;
		}
		if (!attemptSymSkipWS("cast")) {
			return first;
		}
		consumeSymSkipWS("as");
		AST type = singleType();
		AST expr = new AST(XQ.CastExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private AST unaryExpr() throws TokenizerException {
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
		AST expr = new AST(XQ.ArithmeticExpr);
		expr.addChild(new AST(XQ.MultiplyOp));
		expr.addChild(new AST(XQ.Int, Int32.N_ONE));
		expr.addChild(valueExpr());
		return expr;
	}

	private AST valueExpr() throws TokenizerException {
		AST expr = validateExpr();
		expr = (expr != null) ? expr : pathExpr();
		expr = (expr != null) ? expr : extensionExpr();
		return expr;
	}

	private AST extensionExpr() throws TokenizerException {
		AST pragma = pragma();
		if (pragma == null) {
			return null;
		}
		AST eExpr = new AST(XQ.ExtensionExpr);
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

	private AST pragma() throws TokenizerException {
		if (!attemptSkipWS("(#")) {
			return null;
		}
		AST pragma = new AST(XQ.Pragma);
		attemptWS();
		pragma.addChild(eqnameLiteral(false, false));
		if (attemptWS()) {
			pragma.addChild(pragmaContent());
		}
		consume("#)");
		return pragma;
	}

	private AST pathExpr() throws TokenizerException {
		// treatment of initial '/' and '//' is
		// delayed to relativePathExpr
		return relativePathExpr();
	}

	private AST relativePathExpr() throws TokenizerException {
		AST[] path = null;
		AST step;
		if (attemptSkipWS("//")) {
			step = stepExpr();
			if (step == null) {
				throw new TokenizerException("Incomplete path step: %s",
						paraphrase());
			}
			// initial '//' is translated to
			// (fn:root(self::node()) treat as
			// document-node())/descendant-or-self::node()/
			AST treat = fnRootTreatAsDocument();
			AST dosn = descendantOrSelfNode();
			path = new AST[] { treat, dosn, step };
		} else if (attemptSkipWS("/")) {
			step = stepExpr();
			if (step == null) {
				if (!checkLeadingLoneSlash()) {
					throw new TokenizerException("Incomplete path step: %s",
							paraphrase());
				}
				// single '/' is translated to
				// (fn:root(self::node()) treat as document-node())
				return fnRootTreatAsDocument();
			}
			// initial '/' is translated to
			// (fn:root(self::node()) treat as document-node())/
			AST treat = fnRootTreatAsDocument();
			path = new AST[] { treat, step };
		} else {
			step = stepExpr();
			if (step == null) {
				return null;
			}
			path = new AST[] { step };
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
				throw new TokenizerException("Incomplete path step: %s",
						paraphrase());
			}
			path = add(path, step);
		}
		if (path.length == 1) {
			return path[0];
		}
		AST pathExpr = new AST(XQ.PathExpr);
		pathExpr.addChildren(path);
		return pathExpr;
	}

	private boolean checkLeadingLoneSlash() {
		// leading-lone-slash rule:
		// check if next token can form the start of
		// a relative path expression...
		return ((laSkipWS("*") == null) && (laSkipWS("<") == null)
				&& (laNCNameSkipWS() == null) && (laQNameSkipWS() == null)
				&& (laSkipWS("\"") == null) && (laSkipWS("'") == null));
	}

	private AST descendantOrSelfNode() {
		AST dosn = new AST(XQ.StepExpr);
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.DESCENDANT_OR_SELF));
		dosn.addChild(axisSpec);
		dosn.addChild(new AST(XQ.KindTestAnyKind));
		return dosn;
	}

	private AST fnRootTreatAsDocument() {
		AST treat = new AST(XQ.TreatExpr);
		AST call = new AST(XQ.FunctionCall, Functions.FN_ROOT);
		AST step = new AST(XQ.StepExpr);
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.SELF));
		step.addChild(axisSpec);
		step.addChild(new AST(XQ.KindTestAnyKind));
		AST seqType = new AST(XQ.SequenceType);
		seqType.addChild(new AST(XQ.KindTestDocument));
		call.addChild(step);
		treat.addChild(call);
		treat.addChild(seqType);
		AST parenthesized = new AST(XQ.ParenthesizedExpr);
		parenthesized.addChild(treat);
		return parenthesized;
	}

	private AST stepExpr() throws TokenizerException {
		AST expr = postFixExpr();
		if (expr != null) {
			return expr;
		}
		return axisStep();
	}

	private AST axisStep() throws TokenizerException {
		AST[] step = forwardStep();
		if (step == null) {
			step = reverseStep();
		}
		if (step == null) {
			return null;
		}
		AST[] predicateList = predicateList();
		AST stepExpr = new AST(XQ.StepExpr);
		stepExpr.addChildren(step);
		if (predicateList != null) {
			stepExpr.addChildren(predicateList);
		}
		return stepExpr;
	}

	private AST[] forwardStep() throws TokenizerException {
		AST axis = forwardAxis();
		if (axis == null) {
			return abbrevForwardStep();
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(axis);
		return new AST[] { axisSpec, nodeTest(axis.getType() == XQ.ATTRIBUTE) };
	}

	private AST forwardAxis() {
		Token la;
		AST axis;
		if ((la = laSkipWS("child")) != null) {
			axis = new AST(XQ.CHILD);
		} else if ((la = laSkipWS("descendant-or-self")) != null) {
			axis = new AST(XQ.DESCENDANT_OR_SELF);
		} else if ((la = laSkipWS("descendant")) != null) {
			axis = new AST(XQ.DESCENDANT);
		} else if ((la = laSkipWS("attribute")) != null) {
			axis = new AST(XQ.ATTRIBUTE);
		} else if ((la = laSkipWS("self")) != null) {
			axis = new AST(XQ.SELF);
		} else if ((la = laSkipWS("following-sibling")) != null) {
			axis = new AST(XQ.FOLLOWING_SIBLING);
		} else if ((la = laSkipWS("following")) != null) {
			axis = new AST(XQ.FOLLOWING);
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

	private AST[] abbrevForwardStep() throws TokenizerException {
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
		// look ahead to avoid eager matching of first part
		// of reverse-axis as name test (e.g. ancestor::)
		Token la = laEQNameSkipWS(true);
		if ((la != null) && (laSkipWS(la, "::") != null)) {
			return null;
		}

		AST nodeTest = nodeTest(attributeAxis);
		if (nodeTest == null) {
			return null;
		}
		if (attributeAxis) {
			AST axisSpec = new AST(XQ.AxisSpec);
			axisSpec.addChild(new AST(XQ.ATTRIBUTE));
			return new AST[] { axisSpec, nodeTest };
		} else {
			AST axisSpec = new AST(XQ.AxisSpec);
			axisSpec.addChild(new AST(XQ.CHILD));
			return new AST[] { axisSpec, nodeTest };
		}
	}

	private AST[] reverseStep() throws TokenizerException {
		AST axis = reverseAxis();
		if (axis == null) {
			return abbrevReverseStep();
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(axis);
		return new AST[] { axisSpec, nodeTest(axis.getType() == XQ.ATTRIBUTE) };
	}

	private AST reverseAxis() {
		Token la;
		AST axis;
		if ((la = laSkipWS("parent")) != null) {
			axis = new AST(XQ.PARENT);
		} else if ((la = laSkipWS("ancestor-or-self")) != null) {
			axis = new AST(XQ.ANCESTOR_OR_SELF);
		} else if ((la = laSkipWS("ancestor")) != null) {
			axis = new AST(XQ.ANCESTOR);
		} else if ((la = laSkipWS("preceding-sibling")) != null) {
			axis = new AST(XQ.PRECEDING_SIBLING);
		} else if ((la = laSkipWS("preceding")) != null) {
			axis = new AST(XQ.PRECEDING);
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

	private AST[] abbrevReverseStep() {
		if (!attemptSkipWS("..")) {
			return null;
		}
		AST axisSpec = new AST(XQ.AxisSpec);
		axisSpec.addChild(new AST(XQ.PARENT));
		AST nameTest = new AST(XQ.NameTest);
		nameTest.addChild(new AST(XQ.Wildcard));
		return new AST[] { axisSpec, nameTest };
	}

	private AST nodeTest(boolean attributeAxis) throws TokenizerException {
		AST test = kindTest();
		test = (test != null) ? test : nameTest(!attributeAxis);
		return test;
	}

	private AST kindTest() throws TokenizerException {
		AST test = documentTest();
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

	private AST documentTest() throws TokenizerException {
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
		AST elTest = elementTest();
		AST schemaElTest = (elTest != null) ? elTest : schemaElementTest();
		consumeSkipWS(")");
		AST docTest = new AST(XQ.KindTestDocument);
		if (elTest != null) {
			docTest.addChild(elTest);
		}
		if (schemaElTest != null) {
			docTest.addChild(schemaElTest);
		}
		return docTest;
	}

	private AST elementTest() throws TokenizerException {
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
		AST enow = elementNameOrWildcard();
		AST tn = null;
		AST nilled = null;
		if ((enow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new AST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		AST elTest = new AST(XQ.KindTestElement);
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

	private AST attributeTest() throws TokenizerException {
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
		AST anow = attributeNameOrWildcard();
		AST tn = null;
		AST nilled = null;
		if ((anow != null) && (attemptSkipWS(","))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS("?")) {
				nilled = new AST(XQ.Nilled);
			}
		}
		consumeSkipWS(")");
		AST attTest = new AST(XQ.KindTestAttribute);
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

	private AST schemaElementTest() throws TokenizerException {
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
		AST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestSchemaElement);
		test.addChild(name);
		return test;
	}

	private AST schemaAttributeTest() throws TokenizerException {
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
		AST name = eqnameLiteral(false, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestSchemaAttribute);
		test.addChild(name);
		return test;
	}

	private AST piTest() throws TokenizerException {
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
		AST name = ncnameLiteral(true, true);
		name = (name != null) ? name : stringLiteral(true, true);
		consumeSkipWS(")");
		AST test = new AST(XQ.KindTestPi);
		if (name != null) {
			test.addChild(name);
		}
		return test;
	}

	private AST commentTest() throws TokenizerException {
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
		return new AST(XQ.KindTestComment);
	}

	private AST textTest() throws TokenizerException {
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
		return new AST(XQ.KindTestText);
	}

	private AST namespaceNodeTest() throws TokenizerException {
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
		return new AST(XQ.KindTestNamespaceNode);
	}

	private AST anyKindTest() throws TokenizerException {
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
		return new AST(XQ.KindTestAnyKind);
	}

	private AST elementNameOrWildcard() throws TokenizerException {
		AST enow = eqnameLiteral(true, true);
		if (enow != null) {
			return enow;
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		}
		return null;
	}

	private AST attributeNameOrWildcard() throws TokenizerException {
		AST anow = eqnameLiteral(true, true);
		if (anow != null) {
			return anow;
		}
		if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		}
		return null;
	}

	private AST nameTest(boolean element) throws TokenizerException {
		// Switched order of EQName and Wildcard of
		// the XQuery grammar because the EQName look
		// ahead could consume the NCName prefix of
		// a "NCNname ':' '*'" wildcard
		AST test = wildcard();
		test = (test != null) ? test : eqnameLiteral(true, true);
		if (test == null) {
			return null;
		}
		AST nameTest = new AST(XQ.NameTest);
		nameTest.addChild(test);
		return nameTest;
	}

	private AST wildcard() throws TokenizerException {
		Token la = laSkipWS("*:");
		if (la != null) {
			Token la2 = laNCName(la);
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			AST wbc = new AST(XQ.NSWildcardNameTest, la2.string());
			return wbc;
		} else if (attemptSkipWS("*")) {
			return new AST(XQ.Wildcard);
		} else {
			la = laNCNameSkipWS();
			if (la == null) {
				return null;
			}
			Token la2 = la(la, ":*");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			AST wba = new AST(XQ.NSNameWildcardTest, la.string());
			return wba;
		}
	}

	private AST[] predicateList() throws TokenizerException {
		AST[] predicates = new AST[0];
		AST predicate;
		while ((predicate = predicate()) != null) {
			predicates = add(predicates, predicate);
		}
		return predicates;
	}

	private AST predicate() throws TokenizerException {
		if (!attemptSkipWS("[")) {
			return null;
		}
		AST pred = new AST(XQ.Predicate);
		pred.addChild(expr());
		consumeSkipWS("]");
		return pred;

	}

	private AST validateExpr() throws TokenizerException {
		Token la = laSymSkipWS("validate");
		if (la == null) {
			return null;
		}
		AST mode = null;
		Token la2 = laSymSkipWS(la, "lax");
		if (la2 != null) {
			consume(la);
			consume(la2);
			mode = new AST(XQ.ValidateLax);
			consumeSkipWS("{");
		} else if ((la2 = laSymSkipWS(la, "strict")) != null) {
			consume(la);
			consume(la2);
			mode = new AST(XQ.ValidateStrict);
			consumeSkipWS("{");
		} else if ((la2 = laSymSkipWS(la, "strict")) != null) {
			consume(la);
			consume(la2);
			mode = eqnameLiteral(false, true);
			consumeSkipWS("{");
		} else if ((la2 = laSkipWS(la, "{")) != null) {
			consume(la);
			consume(la2);
			// default mode if not specified is strict
			mode = new AST(XQ.ValidateStrict);
		} else {
			return null;
		}

		AST vExpr = new AST(XQ.ValidateExpr);
		vExpr.addChild(mode);
		vExpr.addChild(expr());
		consumeSkipWS("}");
		return vExpr;
	}

	private AST postFixExpr() throws TokenizerException {
		AST expr = primaryExpr();
		if (expr == null) {
			return null;
		}
		while (true) {
			AST predicate = predicate();
			if (predicate != null) {
				AST filterExpr = new AST(XQ.FilterExpr);
				filterExpr.addChild(expr);
				filterExpr.addChild(predicate);
				expr = filterExpr;
				continue;
			}
			AST[] argumentList = argumentList();
			if ((argumentList != null) && (argumentList.length > 0)) {
				AST dynFuncCallExpr = new AST(XQ.DynamicFunctionCallExpr);
				dynFuncCallExpr.addChild(expr);
				dynFuncCallExpr.addChildren(argumentList);
				expr = dynFuncCallExpr;
				continue;
			}
			break;
		}
		return expr;
	}

	private AST[] argumentList() throws TokenizerException {
		if (!attemptSkipWS("(")) {
			return null;
		}
		AST[] args = new AST[0];
		while (!attemptSkipWS(")")) {
			if (args.length > 0) {
				consumeSkipWS(",");
			}
			args = add(args, argument());
		}
		return args;
	}

	private AST primaryExpr() throws TokenizerException {
		AST expr = literal();
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

	private AST literal() throws TokenizerException {
		AST lit = numericLiteral();
		if (lit != null) {
			return lit;
		}
		// A path step with an expanded QName
		// "uri-literal":NCNAME will be matched
		// as string literal...
		Token la = laStringSkipWS(true);
		if ((la == null) || (la(la, ":") != null)) {
			return null;
		}
		consume(la);
		return new AST(XQ.Str, new Str(la.string()));
	}

	private AST varRef() throws TokenizerException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		QNm varName = eqname(false, true);
		return new AST(XQ.VariableRef, varName);
	}

	private AST parenthesizedExpr() throws TokenizerException {
		// distinguish from extension expression
		// starting with '(#'
		Token la = laSkipWS("(");
		if ((la == null) || (la(la, "#") != null)) {
			return null;
		}
		AST expr = new AST(XQ.ParenthesizedExpr);
		consume(la);
		if (attemptSkipWS(")")) {
			return expr;
		}
		expr.addChild(expr());
		consumeSkipWS(")");
		return expr;
	}

	private AST contextItemExpr() {
		Token la = laSkipWS(".");
		// avoid to interpret parent axis ('..') as
		// context item expression
		if ((la == null) || (la(la, ".") != null)) {
			return null;
		}
		consume(la);
		return new AST(XQ.ContextItemExpr);
	}

	private AST functionCall() throws TokenizerException {
		EQNameToken la = laEQNameSkipWS(true);
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
		AST call = new AST(XQ.FunctionCall, la.qname());
		call.addChildren(argumentList());
		return call;
	}

	private AST argument() throws TokenizerException {
		// changed order to match '?' greedy
		if (attempt("?")) {
			return new AST(XQ.ArgumentPlaceHolder);
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

	private AST orderedExpr() throws TokenizerException {
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
		AST expr = expr();
		consumeSkipWS("}");
		AST orderedExpr = new AST(XQ.OrderedExpr);
		orderedExpr.addChild(expr);
		return orderedExpr;
	}

	private AST unorderedExpr() throws TokenizerException {
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
		AST expr = expr();
		consumeSkipWS("}");
		AST unorderedExpr = new AST(XQ.UnorderedExpr);
		unorderedExpr.addChild(expr);
		return unorderedExpr;
	}

	private AST constructor() throws TokenizerException {
		AST con = directConstructor(false);
		con = (con != null) ? con : computedConstructor();
		return con;
	}

	private AST directConstructor(boolean nested) throws TokenizerException {
		AST con = dirElemConstructor(nested);
		con = (con != null) ? con : dirCommentConstructor(nested);
		con = (con != null) ? con : dirPIConstructor(nested);
		return con;
	}

	private AST dirElemConstructor(boolean nested) throws TokenizerException {
		if (nested) {
			if ((la("</") != null) || (la("<?") != null) || (la("<!") != null)
					|| (!attempt("<"))) {
				return null;
			}
		} else {
			if ((laSkipWS("</") != null) || (laSkipWS("<?") != null)
					|| (laSkipWS("<!") != null) || (!attemptSkipWS("<"))) {
				return null;
			}
		}
		skipS();
		// name is expanded after (possible) declaration of in-scope namespaces
		AST stag = qnameLiteral(false, false);
		QNm name = (QNm) stag.getValue();
		AST elem = new AST(XQ.DirElementConstructor);
		elem.addChild(stag);

		AST cseq = new AST(XQ.ContentSequence);
		elem.addChild(cseq);
		AST att;
		while ((att = dirAttribute(true)) != null) {
			cseq.addChild(att);
		}

		skipS();

		if (!attempt("/>")) {
			consume(">");
			AST content;
			boolean checkBoundaryWS = true;
			while ((content = dirElementContent(checkBoundaryWS)) != null) {
				cseq.addChild(content);
				checkBoundaryWS = (content.getType() == XQ.DirElementConstructor)
						|| (content.getType() == XQ.DirCommentConstructor)
						|| (content.getType() == XQ.DirPIConstructor)
						|| (content.getType() == XQ.EnclosedExpr);
			}
			consume("</");
			skipS();
			EQNameToken la = laQName();
			// match nesting based on literal qname string
			if ((la == null) || (!la.string().equals(name.toString()))) {
				throw new IllegalNestingException(name.toString());
			}
			consume(la);
			skipS();
			consume(">");
		}

		return elem;
	}

	private AST dirAttribute(boolean expand) throws TokenizerException {
		skipS();
		// name is expanded later in parent element constructor
		AST attName = qnameLiteral(true, false);
		if (attName == null) {
			return null;
		}
		skipS();
		consume("=");
		skipS();
		AST att = new AST(XQ.DirAttributeConstructor);
		att.addChild(attName);
		AST cseq = new AST(XQ.ContentSequence);
		att.addChild(cseq);
		AST val;
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

	private AST quotAttrValue() throws TokenizerException {
		Token la = la("\"");
		if (la != null) {
			if (la(la, "\"") != null) {
				consume("\"\"");
				return new AST(XQ.Str, "\"");
			}
			return null;
		}
		return quotAttrValueContent();
	}

	private AST quotAttrValueContent() throws TokenizerException {
		Token content = laQuotAttrContentChar();
		if (content != null) {
			consume(content);
			return new AST(XQ.Str, content.string());
		}
		return commonContent();
	}

	private AST aposAttrValue() throws TokenizerException {
		Token la = la("'");
		if (la != null) {
			if (la(la, "'") != null) {
				consume("''");
				return new AST(XQ.Str, "'");
			}
			return null;
		}
		return aposAttrValueContent();
	}

	private AST aposAttrValueContent() throws TokenizerException {
		Token content = laAposAttrContentChar();
		if (content != null) {
			consume(content);
			return new AST(XQ.Str, content.string());
		}
		return commonContent();
	}

	private AST commonContent() throws TokenizerException {
		Token c = laPredefEntityRef(false);
		c = (c != null) ? c : laCharRef(false);
		c = (c != null) ? c : laEscapeCurly();
		if (c != null) {
			consume(c);
			return new AST(XQ.Str, c.string());
		}
		return enclosedExpr(false);
	}

	private AST dirElementContent(boolean checkBoundaryWS)
			throws TokenizerException {
		Token la;
		if ((checkBoundaryWS)
				&& ((((la = laSkipS("<")) != null) && ((la(la, "!") == null))) || (((la = laSkipS("{")) != null) && ((la(
						la, "{") == null)))) && ((la = laS()) != null)) {
			consume(la);
			AST boundaryWS = new AST(XQ.Str, la.string());
			boundaryWS.setProperty("boundaryWS", true);
			return boundaryWS;
		}
		AST c = directConstructor(true);
		c = (c != null) ? c : cDataSection();
		c = (c != null) ? c : commonContent();
		c = (c != null) ? c : elementContentChar();
		return c;
	}

	private AST cDataSection() throws TokenizerException {
		if (!attempt("<![CDATA[")) {
			return null;
		}
		Token content = laCDataSectionContents();
		consume(content);
		consume("]]>");
		return new AST(XQ.Str, content.string());
	}

	private AST elementContentChar() {
		Token content = laElemContentChar();
		if (content == null) {
			return null;
		}
		consume(content);
		return new AST(XQ.Str, content.string());
	}

	private AST dirCommentConstructor(boolean nested) throws TokenizerException {
		if (nested) {
			if (!attempt("<!--")) {
				return null;
			}
		} else {
			if (!attemptSkipWS("<!--")) {
				return null;
			}
		}
		Token content = laCommentContents(false);
		consume(content);
		consume("-->");
		AST comment = new AST(XQ.DirCommentConstructor);
		comment.addChild(new AST(XQ.Str, content.string()));
		return comment;
	}

	private AST dirPIConstructor(boolean nested) throws TokenizerException {
		if (nested) {
			if (!attempt("<?")) {
				return null;
			}
		} else {
			if (!attemptSkipWS("<?")) {
				return null;
			}
		}
		Token target = laPITarget(false);
		consume(target);
		AST piCon = new AST(XQ.DirPIConstructor);
		piCon.addChild(new AST(XQ.Str, target.string()));
		if (skipS()) {
			Token content = laPIContents();
			consume(content);
			piCon.addChild(new AST(XQ.Str, content.string()));
		}
		consume("?>");
		return piCon;
	}

	private AST computedConstructor() throws TokenizerException {
		AST c = compDocConstructor();
		c = (c != null) ? c : compElemConstructor();
		c = (c != null) ? c : compAttrConstructor();
		c = (c != null) ? c : compNamespaceConstructor();
		c = (c != null) ? c : compTextConstructor();
		c = (c != null) ? c : compCommentConstructor();
		c = (c != null) ? c : compPIConstructor();
		return c;
	}

	private AST compDocConstructor() throws TokenizerException {
		Token la = laSkipWS("document");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompDocumentConstructor);
		doc.addChild(expr());
		consumeSkipWS("}");
		return doc;
	}

	private AST compElemConstructor() throws TokenizerException {
		Token la = laSkipWS("element");
		if (la == null) {
			return null;
		}
		EQNameToken la2 = laEQNameSkipWS(la, true);
		AST elem;
		if (la2 != null) {
			Token la3 = laSkipWS(la2, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			elem = new AST(XQ.CompElementConstructor);
			QNm qname = la2.qname();
			elem.addChild(new AST(XQ.QNm, qname));
		} else {
			Token la3 = laSkipWS(la, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la3);
			elem = new AST(XQ.CompElementConstructor);
			elem.addChild(expr());
			consumeSkipWS("}");
			consumeSkipWS("{");
		}
		AST conSeq = new AST(XQ.ContentSequence);
		elem.addChild(conSeq);
		if (!attemptSkipWS("}")) {
			AST expr = expr();
			if (expr != null) {
				conSeq.addChild(expr);
			}
			consumeSkipWS("}");
		}
		return elem;
	}

	private AST compAttrConstructor() throws TokenizerException {
		Token la = laSkipWS("attribute");
		if (la == null) {
			return null;
		}
		EQNameToken la2 = laEQNameSkipWS(la, true);
		AST attr;
		if (la2 != null) {
			Token la3 = laSkipWS(la2, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			attr = new AST(XQ.CompAttributeConstructor);
			QNm qname = la2.qname();
			attr.addChild(new AST(XQ.QNm, qname));
		} else {
			Token la3 = laSkipWS(la, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la3);
			attr = new AST(XQ.CompAttributeConstructor);
			attr.addChild(expr());
			consumeSkipWS("}");
			consumeSkipWS("{");
		}
		AST conSeq = new AST(XQ.ContentSequence);
		attr.addChild(conSeq);
		if (!attemptSkipWS("}")) {
			AST expr = expr();
			if (expr != null) {
				conSeq.addChild(expr);
			}
			consumeSkipWS("}");
		}
		return attr;
	}

	private AST compNamespaceConstructor() throws TokenizerException {
		Token la = laSkipWS("namespace");
		if (la == null) {
			return null;
		}
		Token la2 = laNCName(la);
		AST ns;
		if (la2 != null) {
			Token la3 = laSkipWS(la2, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			ns = new AST(XQ.CompNamespaceConstructor);
			ns.addChild(new AST(XQ.Str, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			ns = new AST(XQ.CompNamespaceConstructor);
			ns.addChild(expr());
			consumeSkipWS("}");
			consumeSkipWS("{");
		}
		if (!attemptSkipWS("}")) {
			AST expr = expr();
			if (expr != null) {
				ns.addChild(expr);
			}
			consumeSkipWS("}");
		}
		return ns;
	}

	private AST compTextConstructor() throws TokenizerException {
		Token la = laSkipWS("text");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompTextConstructor);
		doc.addChild(expr());
		consumeSkipWS("}");
		return doc;
	}

	private AST compCommentConstructor() throws TokenizerException {
		Token la = laSkipWS("comment");
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, "{");
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		AST doc = new AST(XQ.CompCommentConstructor);
		doc.addChild(expr());
		consumeSkipWS("}");
		return doc;
	}

	private AST compPIConstructor() throws TokenizerException {
		Token la = laSkipWS("processing-instruction");
		if (la == null) {
			return null;
		}
		Token la2 = laNCNameSkipWS(la);
		AST pi;
		if (la2 != null) {
			Token la3 = laSkipWS(la2, "{");
			if (la3 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			consume(la3);
			pi = new AST(XQ.CompPIConstructor);
			pi.addChild(new AST(XQ.Str, la2.string()));
		} else {
			la2 = laSkipWS(la, "{");
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			pi = new AST(XQ.CompPIConstructor);
			pi.addChild(expr());
			consumeSkipWS("}");
			consumeSkipWS("{");
		}
		if (!attemptSkipWS("}")) {
			AST expr = expr();
			if (expr != null) {
				pi.addChild(expr);
			}
			consumeSkipWS("}");
		}
		return pi;
	}

	private AST functionItemExpr() throws TokenizerException {
		AST funcItem = literalFunctionItem();
		funcItem = (funcItem != null) ? funcItem : inlineFunction();
		return funcItem;
	}

	private AST literalFunctionItem() throws TokenizerException {
		EQNameToken la = laEQNameSkipWS(true);
		if (la == null) {
			return null;
		}
		String funcName = la.string();
		if (isReservedFuncName(funcName)) {
			return null;
		}
		Token la2 = laSkipWS("#");
		if (la2 == null) {
			return null;
		}
		AST eqname = new AST(XQ.QNm, la.qname());
		consume(la);
		consume(la2);
		AST no = integerLiteral(false, true);
		AST litFunc = new AST(XQ.LiteralFuncItem);
		litFunc.addChild(eqname);
		litFunc.addChild(no);
		return litFunc;
	}

	private AST inlineFunction() throws TokenizerException {
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
		AST inlineFunc = new AST(XQ.InlineFuncItem);
		do {
			AST param = param();
			if (param == null) {
				break;
			}
			inlineFunc.addChild(param);
		} while (attemptSkipWS(","));
		consumeSkipWS(")");
		if (attemptSymSkipWS("as")) {
			inlineFunc.addChild(sequenceType());
		} else {
			// add item()* as default result type
			AST typeDecl = defaultFunctionResultType();
			inlineFunc.addChild(typeDecl);
		}
		inlineFunc.addChild(enclosedExpr(true));
		return inlineFunc;
	}

	private AST enclosedExpr(boolean skipWS) throws TokenizerException {
		if (skipWS) {
			if (!attemptSkipWS("{")) {
				return null;
			}
		} else if (!attempt("{")) {
			return null;
		}
		AST expr = new AST(XQ.EnclosedExpr);
		expr.addChild(expr());
		consumeSkipWS("}");
		return expr;
	}

	private AST param() throws TokenizerException {
		if (!attemptSkipWS("$")) {
			return null;
		}
		QNm varName = eqname(false, true);
		AST decl = new AST(XQ.TypedVariableDeclaration);
		decl.addChild(new AST(XQ.Variable, varName));
		AST typeDecl = typeDeclaration();
		if (typeDecl != null) {
			decl.addChild(typeDecl);
		}
		return decl;
	}

	private AST stringLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laStringSkipWS(cond) : laString(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected string literal: '%s'",
					paraphrase());
		}
		consume(la);
		return new AST(XQ.Str, new Str(la.string()));
	}

	private AST uriLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laStringSkipWS(cond) : laString(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected URI literal: '%s'",
					paraphrase());
		}
		consume(la);
		try {
			return new AST(XQ.AnyURI, new AnyURI(la.string()));
		} catch (QueryException e) {
			throw new InvalidURIException(la.string());
		}
	}

	private AST numericLiteral() throws TokenizerException {
		AST lit = integerLiteral(true, true);
		lit = (lit != null) ? lit : decimalLiteral(true, true);
		lit = (lit != null) ? lit : doubleLiteral(true, true);
		return lit;
	}

	private AST ncnameLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laNCNameSkipWS() : laNCName();
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected NCName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.Str, la.string());
	}

	private QNm eqname(boolean cond, boolean skipWS) throws TokenizerException {
		EQNameToken la = (skipWS) ? laEQNameSkipWS(cond) : laEQName(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return la.qname();
	}

	private AST eqnameLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		EQNameToken la = (skipWS) ? laEQNameSkipWS(cond) : laEQName(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.QNm, la.qname());
	}

	private AST qnameLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		EQNameToken la = (skipWS) ? laQNameSkipWS() : laQName();
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected QName: '%s'", paraphrase());
		}
		consume(la);
		return new AST(XQ.QNm, la.qname());
	}

	private AST doubleLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laDoubleSkipWS(cond) : laDouble(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected double literal: '%s'",
					paraphrase());
		}
		consume(la);
		try {
			return new AST(XQ.Dbl, new Dbl(la.string()));
		} catch (QueryException e) {
			// this should never happen...
			throw new TokenizerException(e,
					"Error parsing double literal: '%s'", paraphrase());
		}
	}

	private AST decimalLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laDecimalSkipWS(cond) : laDecimal(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected decimal literal: '%s'",
					paraphrase());
		}
		consume(la);
		try {
			return new AST(XQ.Dec, new Dec(la.string()));
		} catch (QueryException e) {
			// this should never happen...
			throw new TokenizerException(e,
					"Error parsing decimal literal: '%s'", paraphrase());
		}
	}

	private AST integerLiteral(boolean cond, boolean skipWS)
			throws TokenizerException {
		Token la = (skipWS) ? laIntegerSkipWS(cond) : laInteger(cond);
		if (la == null) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("Expected integer literal: '%s'",
					paraphrase());
		}
		consume(la);
		try {
			return new AST(XQ.Int, Int32.parse(la.string()));
		} catch (QueryException e) {
			// this should never happen...
			throw new TokenizerException(e,
					"Error parsing integer literal: '%s'", paraphrase());
		}
	}

	private AST pragmaContent() throws TokenizerException {
		Token la = laPragma(false);
		consume(la);
		return (la == null) ? null : new AST(XQ.PragmaContent, la.string());
	}

	private AST[] add(AST[] asts, AST ast) {
		int len = asts.length;
		asts = Arrays.copyOf(asts, len + 1);
		asts[len] = ast;
		return asts;
	}
}