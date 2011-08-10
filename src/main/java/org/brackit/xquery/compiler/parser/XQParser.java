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
 * @author baechle
 * 
 */
public class XQParser extends Tokenizer {

	private static final String[] RESERVED_FUNC_NAMES = new String[] {
			"attribute", "comment", "document-node", "element",
			"empty-sequence", "funtion", "if", "item", "namespace-node",
			"node", "processing-instruction", "schema-attribute",
			"schema-element", "switch", "text", "typeswitch" };

	private static final String _XQUERY = "xquery";

	private static final String _VERSION = "version";

	private static final String _ENCODING = "encoding";

	private static final String _SEPARATOR = ";";

	private static final String _MODULE = "module";

	private static final String _NAMESPACE = "namespace";

	private static final String _EQ = "=";

	private static final String _FOR = "for";

	private static final String _DOLLAR = "$";

	private static final String _IN = "in";

	private static final String _COMMA = ",";

	private static final String _ALLOWING = "allowing";

	private static final String _EMPTY = "empty";

	private static final String _AT = "at";

	private static final String _LET = "let";

	private static final String _DEF = ":=";

	private static final String _WHERE = "where";

	private static final String _GROUP = "group";

	private static final String _BY = "by";

	private static final String _COLLATION = "collation";

	private static final String _RETURN = "return";

	private static final String _TUMBLING = "sliding";

	private static final String _SLIDING = "tumbling";

	private static final String _STABLE = "stable";

	private static final String _ORDER = "order";

	private static final String _ASCENDING = "ascending";

	private static final String _DESCENDING = "descending";

	private static final String _GREATEST = "greatest";

	private static final String _LEAST = "least";

	private static final String _COUNT = "count";

	private static final String _OR = "or";

	private static final String _AND = "and";

	private static final String _GCMP_EQ = "=";

	private static final String _GCMP_NE = "!=";

	private static final String _GCMP_LT = "<";

	private static final String _GCMP_LE = "<=";

	private static final String _GCMP_GT = ">";

	private static final String _GCMP_GE = ">=";

	private static final String _VCMP_EQ = "eq";

	private static final String _VCMP_NE = "neq";

	private static final String _VCMP_LT = "lt";

	private static final String _VCMP_LE = "le";

	private static final String _VCMP_GT = "gt";

	private static final String _VCMP_GE = "ge";

	private static final String _NCMP_IS = "is";

	private static final String _NCMP_PRECEDES = "<<";

	private static final String _NCMP_FOLLOWS = ">>";

	private static final String _TO = "to";

	private static final String _PLUS = "+";

	private static final String _MINUS = "-";

	private static final String _TIMES = "*";

	private static final String _DIV = "/";

	private static final String _IDIV = "idiv";

	private static final String _MOD = "mod";

	private static final String _UNION = "union";

	private static final String _PIPE = "|";

	private static final String _INTERSECT = "intersect";

	private static final String _EXCEPT = "except";

	private static final String _INSTANCE = "instance";

	private static final String _OF = "of";

	private static final String _TREAT = "treat";

	private static final String _AS = "as";

	private static final String _CASTABLE = "castable";

	private static final String _CAST = "cast";

	private static final String _CHILD = "child";

	private static final String _DESCENDANT = "descendant";

	private static final String _ATTRIBUTE = "attribute";

	private static final String _SELF = "self";

	private static final String _DESCENDANT_OR_SELF = "descendant-or-self";

	private static final String _FOLLOWING_SIBLING = "following-sibling";

	private static final String _FOLLOWING = "following";

	private static final String _PARENT = "parent";

	private static final String _ANCESTOR = "ancestor";

	private static final String _PRECEDING_SIBLING = "preceding-sibling";

	private static final String _PRECEDING = "preceding";

	private static final String _ANCESTOR_OR_SELF = "ancestor-or-self";

	private static final String _DOUBLECOLON = "::";

	private static final String _AD = "@";

	private static final String _SCHEMA_ATTRIBUTE = "schema-attribute";

	private static final String _LPAR = "(";

	private static final String _RPAR = ")";

	private static final String _WILDCARDCOLON = "*:";

	private static final String _WILDCARD = "*";

	private static final String _COLONWILDCARD = ":*";

	private static final String _SLASH = "/";

	private static final String _DOUBLESLASH = "//";

	private static final String _DOUBLEPERIOD = "..";

	private static final String _PERIOD = ".";

	private static final String _LCURLY = "{";

	private static final String _RCURLY = "}";

	private static final String _ORDERED = "ordered";

	private static final String _UNORDERED = "unordered";

	private static final String _QUESTIONMARK = "?";

	private static final String _ELEMENT = "element";

	private static final String _DOCUMENT_NODE = "document-node";

	private static final String _SCHEMA_ELEMENT = "schema-element";

	private static final String _PROCESSING_INSTRUCTION = "processing-instruction";

	private static final String _COMMENT = "comment";

	private static final String _TEXT = "text";

	private static final String _NAMESPACE_NODE = "namespace-node";

	private static final String _NODE = "node";

	private static final String _EMPTY_SEQUENCE = "empty-sequence";

	private static final String _ANY_KIND = "item";

	private static final String _SHARP = "#";

	private static final String _SOME = "some";

	private static final String _EVERY = "every";

	private static final String _SATISFIES = "satisfies";

	private static final String _SWITCH = "switch";

	private static final String _DEFAULT = "default";

	private static final String _CASE = "case";

	private static final String _TYPESWITCH = "typeswitch";

	private static final String _IF = "if";

	private static final String _THEN = "then";

	private static final String _ELSE = "else";

	private static final String _TRY = "try";

	private static final String _CATCH = "catch";

	private static final String _BEGIN_PRAGMA = "(#";

	private static final String _END_PRAGMA = "#)";

	private static final String _VALIDATE = "validate";

	private static final String _LAX = "lax";

	private static final String _STRICT = "strict";

	private static final String _TYPE = "type";

	private static final String _FUNCTION = "function";

	private static final String _LANGLE = "<";

	private static final String _RCLOSE = "/>";

	private static final String _RANGLE = ">";

	private static final String _LCLOSE = "</";

	private static final String _QUOT = "\"";

	private static final String _APOS = "'";

	private static final String _ESCAPE_QUOT = "\"\"";

	private static final String _ESCAPE_APOS = "''";

	private static final String _LCURLYLCURLY = "{{";

	private static final String _RCURLYRCURLY = "}}";
	
	private static final String _BEGIN_PI = "<?";
	
	private static final String _END_PI = "?>";

	private static final String _BEGIN_CDATA = "<![CDATA[";
	
	private static final String _END_CDATA = "]]>";

	private static final String _DOCUMENT = "document";

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
		// TODO assert no further input
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
					"Unknown XQuery version: " + version);
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
		if (!attemptSkipWS(_XQUERY)) {
			return false;
		}
		boolean vDecl = false;
		boolean eDecl = false;
		if (attemptSkipWS(_VERSION)) {
			setXQVersion(stringLiteral(false, true).getValue());
			vDecl = true;
		}
		if (attemptSkipWS(_ENCODING)) {
			setEncoding(stringLiteral(false, true).getValue());
			eDecl = true;
		}
		if ((!vDecl) && (!eDecl)) {
			mismatch(_VERSION, _ENCODING);
		}
		consumeSkipWS(_SEPARATOR);
		return true;
	}

	private XQAST libraryModule() throws QueryException {
		Token la = laSkipWS(_MODULE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _NAMESPACE);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST ncn = ncnameLiteral(false, true);
		consumeSkipWS(_EQ);
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
		// TODO Auto-generated method stub
		return null;
	}

	private XQAST queryBody() throws QueryException {
		XQAST expr = expr();
		XQAST body = new XQAST(XQAST.QueryBody);
		body.addChild(expr);
		return body;
	}

	private XQAST expr() throws QueryException {
		XQAST first = exprSingle();
		if (!attemptSkipWS(_COMMA)) {
			return first;
		}
		XQAST sequenceExpr = new XQAST(XQAST.SequenceExpr);
		sequenceExpr.addChild(first);
		do {
			sequenceExpr.addChild(exprSingle());
		} while (attemptSkipWS(_COMMA));
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
		Token la = laSkipWS(_FOR);
		if (la == null) {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS(la, _DOLLAR) == null) {
			return null;
		}
		consume(la); // consume 'for'
		XQAST[] forClauses = new XQAST[0];
		do {
			forClauses = add(forClauses, forBinding());
		} while (attemptSkipWS(_COMMA));
		return forClauses;
	}

	private XQAST forBinding() throws QueryException {
		XQAST forClause = new XQAST(XQAST.ForClause);
		forClause.addChild(typedVarBinding());
		if (attemptSkipWS(_ALLOWING)) {
			consumeSkipWS(_EMPTY);
			forClause.addChild(new XQAST(XQAST.AllowingEmpty));
		}
		if (attemptSkipWS(_AT)) {
			consumeSkipWS(_DOLLAR);
			String varName = declare(eqnameLiteral(false, false).getValue());
			XQAST posVarBinding = new XQAST(XQAST.TypedVariableBinding);
			posVarBinding.addChild(new XQAST(XQAST.Variable, varName));
			forClause.addChild(posVarBinding);
		}
		consumeSkipWS(_IN);
		forClause.addChild(exprSingle());
		return forClause;
	}

	private XQAST typedVarBinding() throws QueryException {
		if (!attemptSkipWS(_DOLLAR)) {
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
		if (!attemptSkipWS(_AS)) {
			return null;
		}
		return sequenceType();
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
		Token la = laSkipWS(_LET);
		if (la == null) {
			return null;
		}
		if (laSkipWS(la, _DOLLAR) == null) {
			return null;
		}
		consume(la); // consume 'let'
		XQAST letClause = new XQAST(XQAST.LetClause);
		letClause.addChild(typedVarBinding());
		consumeSkipWS(_DEF);
		letClause.addChild(exprSingle());
		return new XQAST[] { letClause };
	}

	private XQAST[] windowClause() {
		Token la = laSkipWS(_FOR);
		if (la == null) {
			return null;
		}
		if ((laSkipWS(la, _TUMBLING) == null)
				&& (laSkipWS(la, _SLIDING) == null)) {
			return null;
		}
		throw new RuntimeException();
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
		if (!attemptSkipWS(_WHERE)) {
			return null;
		}
		XQAST whereClause = new XQAST(XQAST.WhereClause);
		whereClause.addChild(exprSingle());
		return whereClause;
	}

	private XQAST groupByClause() throws QueryException {
		if (!attemptSkipWS(_GROUP)) {
			return null;
		}
		consumeSkipWS(_BY);
		XQAST groupByClause = new XQAST(XQAST.GroupByClause);
		do {
			consumeSkipWS(_DOLLAR);
			XQAST gs = new XQAST(XQAST.GroupBySpec);
			String varName = resolve(eqnameLiteral(false, false).getValue());
			gs.addChild(new XQAST(XQAST.VariableRef, varName));
			if (attemptSkipWS(_COLLATION)) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQAST.Collation);
				collation.addChild(uriLiteral);
				gs.addChild(collation);
			}
		} while (attemptSkipWS(_COMMA));
		return groupByClause;
	}

	private XQAST orderByClause() throws QueryException {
		if (attemptSkipWS(_STABLE)) {
			consumeSkipWS(_ORDER);
		} else if (!attemptSkipWS(_ORDER)) {
			return null;
		}
		consumeSkipWS(_BY);
		XQAST orderByClause = new XQAST(XQAST.OrderByClause);
		do {
			consumeSkipWS(_DOLLAR);
			XQAST os = new XQAST(XQAST.OrderBySpec);
			os.addChild(exprSingle());
			if (attemptSkipWS(_ASCENDING)) {
				XQAST obk = new XQAST(XQAST.OrderByKind);
				obk.addChild(new XQAST(XQAST.ASCENDING));
				os.addChild(obk);
			} else if (attemptSkipWS(_DESCENDING)) {
				XQAST obk = new XQAST(XQAST.OrderByKind);
				obk.addChild(new XQAST(XQAST.DESCENDING));
				os.addChild(obk);
			}
			if (attemptSkipWS(_EMPTY)) {
				if (attemptSkipWS(_GREATEST)) {
					XQAST obem = new XQAST(XQAST.OrderByEmptyMode);
					obem.addChild(new XQAST(XQAST.GREATEST));
					os.addChild(obem);
				} else if (attemptSkipWS(_LEAST)) {
					XQAST obem = new XQAST(XQAST.OrderByEmptyMode);
					obem.addChild(new XQAST(XQAST.LEAST));
					os.addChild(obem);
				}
			}
			if (attemptSkipWS(_COLLATION)) {
				XQAST uriLiteral = uriLiteral(false, true);
				XQAST collation = new XQAST(XQAST.Collation);
				collation.addChild(uriLiteral);
				os.addChild(collation);
			}
		} while (attemptSkipWS(_COMMA));
		return orderByClause;
	}

	private XQAST countClause() throws QueryException {
		Token la = laSkipWS(_COUNT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _DOLLAR);
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
		if (!attemptSkipWS(_RETURN)) {
			return null;
		}
		XQAST returnExpr = new XQAST(XQAST.ReturnExpr);
		returnExpr.addChild(exprSingle());
		return returnExpr;
	}

	private XQAST quantifiedExpr() throws QueryException {
		XQAST quantifier;
		if (attemptSkipWS(_SOME)) {
			quantifier = new XQAST(XQAST.SomeQuantifier);
		} else if (attemptSkipWS(_EVERY)) {
			quantifier = new XQAST(XQAST.EveryQuantifier);
		} else {
			return null;
		}
		// la to check if var binding follows
		if (laSkipWS(_DOLLAR) == null) {
			return null;
		}
		XQAST qExpr = new XQAST(XQAST.QuantifiedExpr);
		qExpr.addChild(quantifier);
		qExpr.addChild(typedVarBinding());
		consumeSkipWS(_IN);
		qExpr.addChild(exprSingle());
		while (attemptSkipWS(_COMMA)) {
			XQAST binding = typedVarBinding();
			if (binding == null) {
				throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
						"Expected variable binding: %s", paraphrase());
			}
			qExpr.addChild(binding);
			consumeSkipWS(_IN);
			qExpr.addChild(exprSingle());
		}
		consumeSkipWS(_SATISFIES);
		qExpr.addChild(exprSingle());
		return qExpr;
	}

	private XQAST switchExpr() throws QueryException {
		Token la = laSkipWS(_SWITCH);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST sExpr = new XQAST(XQAST.SwitchExpr);
		sExpr.addChild(expr());
		consumeSkipWS(_RPAR);
		XQAST clause = switchClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected switch clause: %s", paraphrase());
		}
		sExpr.addChild(clause);
		while ((clause = switchClause()) != null) {
			sExpr.addChild(clause);
		}
		consumeSkipWS(_DEFAULT);
		consumeSkipWS(_RETURN);
		sExpr.addChild(exprSingle());
		return sExpr;
	}

	private XQAST switchClause() throws QueryException {
		if (!attemptSkipWS(_CASE)) {
			return null;
		}
		XQAST clause = new XQAST(XQAST.SwitchClause);
		clause.addChild(exprSingle());
		consumeSkipWS(_RETURN);
		clause.addChild(exprSingle());
		return clause;
	}

	private XQAST typeswitchExpr() throws QueryException {
		Token la = laSkipWS(_TYPESWITCH);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST tsExpr = new XQAST(XQAST.TypeSwitch);
		tsExpr.addChild(expr());
		consumeSkipWS(_RPAR);
		XQAST clause = caseClause();
		if (clause == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Excpected case clause: %s", paraphrase());
		}
		tsExpr.addChild(clause);
		while ((clause = caseClause()) != null) {
			tsExpr.addChild(clause);
		}
		consumeSkipWS(_DEFAULT);
		consumeSkipWS(_RETURN);
		tsExpr.addChild(exprSingle());
		return tsExpr;
	}

	private XQAST caseClause() throws QueryException {
		if (!attemptSkipWS(_CASE)) {
			return null;
		}
		XQAST clause = new XQAST(XQAST.TypeSwitchCase);
		clause.addChild(exprSingle());
		if (attemptSkipWS(_DOLLAR)) {
			XQAST eqname = eqnameLiteral(false, false);
			String varName = declare(eqname.getValue());
			clause.addChild(new XQAST(XQAST.Variable, varName));
			consumeSkipWS(_AS);
		}
		do {
			clause.addChild(sequenceType());
		} while (attemptSkipWS(_PIPE));
		consumeSkipWS(_RETURN);
		clause.addChild(exprSingle());
		return clause;
	}

	private XQAST ifExpr() throws QueryException {
		Token la = laSkipWS(_IF);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST ifExpr = new XQAST(XQAST.IfExpr);
		ifExpr.addChild(exprSingle());
		consumeSkipWS(_RPAR);
		consumeSkipWS(_THEN);
		ifExpr.addChild(exprSingle());
		consumeSkipWS(_ELSE);
		ifExpr.addChild(exprSingle());
		return ifExpr;
	}

	private XQAST tryCatchExpr() throws QueryException {
		Token la = laSkipWS(_TRY);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST tcExpr = new XQAST(XQAST.TryCatchExpr);
		tcExpr.addChild(expr());
		consumeSkipWS(_RCURLY);
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
		if (!attemptSkipWS(_CATCH)) {
			return null;
		}
		XQAST clause = new XQAST(XQAST.CatchClause);
		clause.addChild(catchErrorList());
		clause.addChild(catchVars());
		consumeSkipWS(_LCURLY);
		clause.addChild(expr());
		consumeSkipWS(_RCURLY);
		return clause;
	}

	private XQAST catchErrorList() throws QueryException {
		XQAST list = new XQAST(XQAST.CatchErrorList);
		do {
			list.addChild(nameTest());
		} while (attemptSkipWS(_PIPE));
		return list;
	}

	private XQAST catchVars() throws QueryException {
		consumeSkipWS(_LPAR);
		XQAST vars = new XQAST(XQAST.CatchVar);
		consumeSkipWS(_DOLLAR);
		vars.addChild(eqnameLiteral(false, false));
		if (attemptSkipWS(_COMMA)) {
			consumeSkipWS(_DOLLAR);
			vars.addChild(eqnameLiteral(false, false));
			if (attemptSkipWS(_COMMA)) {
				consumeSkipWS(_DOLLAR);
				vars.addChild(eqnameLiteral(false, false));
			}
		}
		consumeSkipWS(_RPAR);
		return vars;
	}

	private XQAST orExpr() throws QueryException {
		XQAST first = andExpr();
		if (!attemptSkipWS(_OR)) {
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
		if (!attemptSkipWS(_AND)) {
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
		if (attemptSkipWS(_GCMP_EQ)) {
			cmp = new XQAST(XQAST.GeneralCompEQ);
		} else if (attemptSkipWS(_GCMP_NE)) {
			cmp = new XQAST(XQAST.GeneralCompNE);
		} else if (attemptSkipWS(_GCMP_LT)) {
			cmp = new XQAST(XQAST.GeneralCompLT);
		} else if (attemptSkipWS(_GCMP_LE)) {
			cmp = new XQAST(XQAST.GeneralCompLE);
		} else if (attemptSkipWS(_GCMP_GT)) {
			cmp = new XQAST(XQAST.GeneralCompGT);
		} else if (attemptSkipWS(_GCMP_GE)) {
			cmp = new XQAST(XQAST.GeneralCompGE);
		} else if (attemptSkipWS(_VCMP_EQ)) {
			cmp = new XQAST(XQAST.ValueCompEQ);
		} else if (attemptSkipWS(_VCMP_NE)) {
			cmp = new XQAST(XQAST.ValueCompNE);
		} else if (attemptSkipWS(_VCMP_LT)) {
			cmp = new XQAST(XQAST.ValueCompLT);
		} else if (attemptSkipWS(_VCMP_LE)) {
			cmp = new XQAST(XQAST.ValueCompLE);
		} else if (attemptSkipWS(_VCMP_GT)) {
			cmp = new XQAST(XQAST.ValueCompGT);
		} else if (attemptSkipWS(_VCMP_GE)) {
			cmp = new XQAST(XQAST.ValueCompGE);
		} else if (attemptSkipWS(_NCMP_IS)) {
			cmp = new XQAST(XQAST.NodeCompIs);
		} else if (attemptSkipWS(_NCMP_PRECEDES)) {
			cmp = new XQAST(XQAST.NodeCompPrecedes);
		} else if (attemptSkipWS(_NCMP_FOLLOWS)) {
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
		if (!attemptSkipWS(_TO)) {
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
		if (attemptSkipWS(_PLUS)) {
			op = new XQAST(XQAST.AddOp);
		} else if (attemptSkipWS(_MINUS)) {
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
		if (attemptSkipWS(_TIMES)) {
			op = new XQAST(XQAST.MultiplyOp);
		} else if (attemptSkipWS(_DIV)) {
			op = new XQAST(XQAST.DivideOp);
		} else if (attemptSkipWS(_IDIV)) {
			op = new XQAST(XQAST.IDivideOp);
		} else if (attemptSkipWS(_MOD)) {
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
		if ((!attemptSkipWS(_UNION)) && (!attemptSkipWS(_PIPE))) {
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
		if (attemptSkipWS(_INTERSECT)) {
			expr = new XQAST(XQAST.IntersectExpr);
		} else if (attemptSkipWS(_EXCEPT)) {
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
		if (!attemptSkipWS(_INSTANCE)) {
			return first;
		}
		consumeSkipWS(_OF);
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
		Token la = laSkipWS(_EMPTY_SEQUENCE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.EmptySequenceType);
	}

	private XQAST anyKind() throws QueryException {
		Token la = laSkipWS(_ANY_KIND);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.ItemType);
	}

	private XQAST occurrenceIndicator() {
		if (attemptSkipWS(_QUESTIONMARK)) {
			return new XQAST(XQAST.CardinalityZeroOrOne);
		}
		if (attemptSkipWS(_WILDCARD)) {
			return new XQAST(XQAST.CardinalityZeroOrMany);
		}
		if (attemptSkipWS(_PLUS)) {
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
		if (!attemptSkipWS(_SHARP)) {
			return null;
		}
		XQAST name = eqnameLiteral(false, true);
		XQAST ann = new XQAST(XQAST.Annotation, name.getValue());
		if (attemptSkipWS(_LPAR)) {
			do {
				ann.addChild(stringLiteral(false, true));
			} while (attemptSkipWS(_COMMA));
			consumeSkipWS(_RPAR);
		}
		return ann;
	}

	private XQAST anyFunctionTest() throws QueryException {
		Token la = laSkipWS(_NAMESPACE_NODE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		Token la3 = laSkipWS(la2, _WILDCARD);
		if (la3 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consume(la3);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.AnyFunctionType);
	}

	private XQAST typedFunctionTest() throws QueryException {
		Token la = laSkipWS(_NAMESPACE_NODE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST typedFunc = new XQAST(XQAST.TypedFunctionType);
		if (!attemptSkipWS(_RPAR)) {
			do {
				typedFunc.addChild(sequenceType());
			} while (attemptSkipWS(_COMMA));
			consumeSkipWS(_RPAR);
		}
		consumeSkipWS(_AS);
		typedFunc.addChild(sequenceType());
		return typedFunc;
	}

	private XQAST atomicOrUnionType() throws QueryException {
		return eqnameLiteral(true, true);
	}

	private XQAST parenthesizedItemType() throws QueryException {
		if (!attemptSkipWS(_LPAR)) {
			return null;
		}
		XQAST itemType = itemType();
		consumeSkipWS(_RPAR);
		return itemType;
	}

	private XQAST singleType() throws QueryException {
		XQAST aouType = atomicOrUnionType();
		if (aouType == null) {
			return null;
		}
		XQAST type = new XQAST(XQAST.SingleType);
		type.addChild(aouType);
		if (attemptSkipWS(_QUESTIONMARK)) {
			type.addChild(new XQAST(XQAST.Optional));
		}
		return type;
	}

	private XQAST treatExpr() throws QueryException {
		XQAST first = castableExpr();
		if (!attemptSkipWS(_TREAT)) {
			return first;
		}
		consumeSkipWS(_AS);
		XQAST type = sequenceType();
		XQAST expr = new XQAST(XQAST.TreatExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST castableExpr() throws QueryException {
		XQAST first = castExpr();
		if (!attemptSkipWS(_CASTABLE)) {
			return first;
		}
		consumeSkipWS(_AS);
		XQAST type = singleType();
		XQAST expr = new XQAST(XQAST.CastableExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST castExpr() throws QueryException {
		XQAST first = unaryExpr();
		if (!attemptSkipWS(_CAST)) {
			return first;
		}
		consumeSkipWS(_AS);
		XQAST type = singleType();
		XQAST expr = new XQAST(XQAST.CastExpr);
		expr.addChild(first);
		expr.addChild(type);
		return expr;
	}

	private XQAST unaryExpr() throws QueryException {
		int minusCount = 0;
		while (true) {
			if (attemptSkipWS(_PLUS)) {
				continue;
			}
			if (attemptSkipWS(_MINUS)) {
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
		consumeSkipWS(_LCURLY);
		if (!attemptSkipWS(_RCURLY)) {
			eExpr.addChild(expr());
			consumeSkipWS(_RCURLY);
		}
		return eExpr;
	}

	private XQAST pragma() throws QueryException {		
		if (!attemptSkipWS(_BEGIN_PRAGMA)) {
			return null;
		}
		XQAST pragma = new XQAST(XQAST.Pragma);
		attemptWS();
		pragma.addChild(qnameLiteral(false, false));
		if (!attemptWS()) {
			pragma.addChild(pragmaContent());
		}
		consume(_END_PRAGMA);
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
		if (attemptSkipWS(_DOUBLESLASH)) {
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
		} else if (attemptSkipWS(_SLASH)) {
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
			if (attemptSkipWS(_DOUBLESLASH)) {
				// intermediate '//' is translated to
				// descendant-or-self::node()/
				path = add(path, descendantOrSelfNode());
			} else if (!attemptSkipWS(_SLASH)) {
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
		if ((la = laSkipWS(_CHILD)) != null) {
			axis = new XQAST(XQAST.CHILD);
		} else if ((la = laSkipWS(_DESCENDANT)) != null) {
			axis = new XQAST(XQAST.DESCENDANT);
		} else if ((la = laSkipWS(_ATTRIBUTE)) != null) {
			axis = new XQAST(XQAST.ATTRIBUTE);
		} else if ((la = laSkipWS(_SELF)) != null) {
			axis = new XQAST(XQAST.SELF);
		} else if ((la = laSkipWS(_DESCENDANT_OR_SELF)) != null) {
			axis = new XQAST(XQAST.DESCENDANT_OR_SELF);
		} else if ((la = laSkipWS(_FOLLOWING_SIBLING)) != null) {
			axis = new XQAST(XQAST.FOLLOWING_SIBLING);
		} else if ((la = laSkipWS(_FOLLOWING)) != null) {
			axis = new XQAST(XQAST.FOLLOWING);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, _DOUBLECOLON);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private XQAST[] abbrevForwardStep() throws QueryException {
		boolean attributeAxis = false;
		if (attemptSkipWS(_AD)) {
			attributeAxis = true;
		} else {
			// look ahead if node test will
			// be attribute or schema-attribute test
			Token la = laSkipWS(_ATTRIBUTE);
			if (la == null) {
				la = laSkipWS(_SCHEMA_ATTRIBUTE);
			}
			if ((la != null) && (laSkipWS(la, _LPAR) != null)) {
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
		if ((la = laSkipWS(_PARENT)) != null) {
			axis = new XQAST(XQAST.PARENT);
		} else if ((la = laSkipWS(_ANCESTOR)) != null) {
			axis = new XQAST(XQAST.ANCESTOR);
		} else if ((la = laSkipWS(_PRECEDING_SIBLING)) != null) {
			axis = new XQAST(XQAST.PRECEDING_SIBLING);
		} else if ((la = laSkipWS(_PRECEDING)) != null) {
			axis = new XQAST(XQAST.PRECEDING);
		} else if ((la = laSkipWS(_ANCESTOR_OR_SELF)) != null) {
			axis = new XQAST(XQAST.ANCESTOR_OR_SELF);
		} else {
			return null;
		}
		Token la2 = laSkipWS(la, _DOUBLECOLON);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		return axis;
	}

	private XQAST[] abbrevReverseStep() {
		if (!attemptSkipWS(_DOUBLEPERIOD)) {
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
		Token la = laSkipWS(_DOCUMENT_NODE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST elTest = elementTest();
		XQAST schemaElTest = (elTest != null) ? elTest : schemaElementTest();
		consumeSkipWS(_RPAR);
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
		Token la = laSkipWS(_ELEMENT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST enow = elementNameOrWildcard();
		XQAST tn = null;
		XQAST nilled = null;
		if ((enow != null) && (attemptSkipWS(_COMMA))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS(_QUESTIONMARK)) {
				nilled = new XQAST(XQAST.Nilled);
			}
		}
		consumeSkipWS(_RPAR);
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
		Token la = laSkipWS(_ATTRIBUTE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST anow = attributeNameOrWildcard();
		XQAST tn = null;
		XQAST nilled = null;
		if ((anow != null) && (attemptSkipWS(_COMMA))) {
			tn = eqnameLiteral(true, true);
			if (attemptSkipWS(_QUESTIONMARK)) {
				nilled = new XQAST(XQAST.Nilled);
			}
		}
		consumeSkipWS(_RPAR);
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
		Token la = laSkipWS(_SCHEMA_ELEMENT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = eqnameLiteral(false, true);
		consumeSkipWS(_RPAR);
		XQAST test = new XQAST(XQAST.KindTestSchemaElement);
		test.addChild(name);
		return test;
	}

	private XQAST schemaAttributeTest() throws QueryException {
		Token la = laSkipWS(_SCHEMA_ATTRIBUTE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = eqnameLiteral(false, true);
		consumeSkipWS(_RPAR);
		XQAST test = new XQAST(XQAST.KindTestSchemaAttribute);
		test.addChild(name);
		return test;
	}

	private XQAST piTest() throws QueryException {
		Token la = laSkipWS(_PROCESSING_INSTRUCTION);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST name = ncnameLiteral(true, true);
		name = (name != null) ? name : stringLiteral(false, true);
		consumeSkipWS(_RPAR);
		XQAST test = new XQAST(XQAST.KindTestPi);
		test.addChild(name);
		return test;
	}

	private XQAST commentTest() throws QueryException {
		Token la = laSkipWS(_COMMENT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.KindTestComment);
	}

	private XQAST textTest() throws QueryException {
		Token la = laSkipWS(_TEXT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.KindTestText);
	}

	private XQAST namespaceNodeTest() throws QueryException {
		Token la = laSkipWS(_NAMESPACE_NODE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.KindTestNamespaceNode);
	}

	private XQAST anyKindTest() throws QueryException {
		Token la = laSkipWS(_NODE);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		consumeSkipWS(_RPAR);
		return new XQAST(XQAST.KindTestAnyKind);
	}

	private XQAST elementNameOrWildcard() throws QueryException {
		XQAST enow = eqnameLiteral(true, true);
		if (enow != null) {
			return enow;
		}
		if (attemptSkipWS(_WILDCARD)) {
			return new XQAST(XQAST.Wildcard);
		}
		return null;
	}

	private XQAST attributeNameOrWildcard() throws QueryException {
		XQAST anow = eqnameLiteral(true, true);
		if (anow != null) {
			return anow;
		}
		if (attemptSkipWS(_WILDCARD)) {
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
		if (attemptSkipWS(_WILDCARDCOLON)) {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			XQAST wbc = new XQAST(XQAST.WildcardBeforeColon);
			wbc.addChild(ncname);
			return wbc;
		} else if (attemptSkipWS(_WILDCARD)) {
			return new XQAST(XQAST.Wildcard);
		} else {
			XQAST ncname = ncnameLiteral(true, true);
			if (ncname == null) {
				return null;
			}
			if (!attempt(_COLONWILDCARD)) {
				return null;
			}
			XQAST wba = new XQAST(XQAST.WildcardAfterColon);
			wba.addChild(ncname);
			return wba;
		}
	}

	private XQAST[] predicateList() {
		// TODO Auto-generated method stub
		return null;
	}

	private XQAST validateExpr() throws QueryException {
		if (!attemptSkipWS(_VALIDATE)) {
			return null;
		}
		XQAST vExpr = new XQAST(XQAST.ValidateExpr);
		if (attemptSkipWS(_LAX)) {
			vExpr.addChild(new XQAST(XQAST.ValidateLax));
		} else if (attemptSkipWS(_STRICT)) {
			vExpr.addChild(new XQAST(XQAST.ValidateStrict));
		} else if (attemptSkipWS(_TYPE)) {
			vExpr.addChild(eqnameLiteral(false, true));
		} else {
			mismatch(_LAX, _STRICT, _TYPE);
		}
		consumeSkipWS(_LCURLY);
		vExpr.addChild(expr());
		consumeSkipWS(_RCURLY);
		return vExpr;
	}

	private XQAST postFixExpr() throws QueryException {
		XQAST primary = primaryExpr();
		return primary;
		// TODO
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
		if (!attemptSkipWS(_DOLLAR)) {
			return null;
		}
		return new XQAST(XQAST.VariableRef, resolve(eqnameLiteral(false, false)
				.getValue()));
	}

	private XQAST parenthesizedExpr() throws QueryException {
		if (!attemptSkipWS(_LPAR)) {
			return null;
		}
		if (attemptSkipWS(_RPAR)) {
			return new XQAST(XQAST.EmptySequence);
		}
		XQAST expr = expr();
		consumeSkipWS(_RPAR);
		return expr;
	}

	private XQAST contextItemExpr() {
		if (!attemptSkipWS(_PERIOD)) {
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
		Token la2 = laSkipWS(la, _LPAR);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST call = new XQAST(XQAST.FunctionCall, funcName);
		if (!attemptSkipWS(_RPAR)) {
			XQAST arg = argument();
			if (arg != null) {
				call.addChild(arg);
				while (attemptSkipWS(_COMMA)) {
					call.addChild(argument());
				}
			}
			consumeSkipWS(_RPAR);
		}
		return call;
	}

	private XQAST argument() throws QueryException {
		// changed order to match '?' greedy
		if (attempt(_QUESTIONMARK)) {
			return new XQAST(XQAST.PlaceHolder);
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
		Token la = laSkipWS(_ORDERED);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST expr = expr();
		consumeSkipWS(_RCURLY);
		XQAST orderedExpr = new XQAST(XQAST.OrderedExpr);
		orderedExpr.addChild(expr);
		return orderedExpr;
	}

	private XQAST unorderedExpr() throws QueryException {
		Token la = laSkipWS(_UNORDERED);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST expr = expr();
		consumeSkipWS(_RCURLY);
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
		if ((la(_LCLOSE) != null) || (la(_BEGIN_PI) != null) || (!attempt(_LANGLE))) {
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
		if (attemptSkipWS(_RCLOSE)) {
			return elem;
		}
		consume(_RANGLE);
		push(stag.getValue());
		XQAST content;
		while ((content = dirElemContent()) != null) {
			cseq.addChild(content);
		}
		consume(_LCLOSE);
		XQAST etag = qnameLiteral(false, false);
		pop(etag.getValue());
		skipS();
		consume(_RANGLE);
		return elem;
	}

	private XQAST dirAttribute() throws QueryException {
		skipS();
		XQAST qname = qnameLiteral(true, false);
		if (qname == null) {
			return null;
		}
		skipS();
		consume(_EQ);
		skipS();
		XQAST att = new XQAST(XQAST.CompAttributeConstructor);
		XQAST lit = new XQAST(XQAST.Literal);
		lit.addChild(qname);
		att.addChild(lit);
		XQAST cseq = new XQAST(XQAST.ContentSequence);
		att.addChild(cseq);
		XQAST val;
		if (attempt(_QUOT)) {
			while ((val = quotAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume(_QUOT);
		} else {
			consume(_APOS);
			while ((val = aposAttrValue()) != null) {
				cseq.addChild(val);
			}
			consume(_APOS);
		}
		return att;
	}

	private XQAST quotAttrValue() throws QueryException {
		if (attempt(_ESCAPE_QUOT)) {
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, _QUOT));
			return lit;
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
		if (attempt(_ESCAPE_APOS)) {
			XQAST lit = new XQAST(XQAST.Literal);
			lit.addChild(new XQAST(XQAST.Str, _APOS));
			return lit;
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
		if (attempt(_LCURLYLCURLY)) {
			curly = "{";
		} else if (attempt(_RCURLYRCURLY)) {
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
		if (!attempt(_BEGIN_CDATA)) {
			return null;
		}
		String content = attemptCDataSectionContents();
		consume(_END_CDATA);
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
		if (!attempt(_BEGIN_PI)) {
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
		consume(_END_PI);
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
		Token la = laSkipWS(_DOCUMENT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(_LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQAST.CompDocumentConstructor);
		doc.addChild(expr());
		consume(_RCURLY);
		return doc;
	}

	private XQAST compElemConstructor() throws QueryException {
		Token la = laSkipWS(_ELEMENT);
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
			la2 = laSkipWS(_LCURLY);
			if (la2 == null) {
				return null;
			}
			consume(la);
			consume(la2);
			elem = new XQAST(XQAST.CompElementConstructor);
			elem.addChild(expr());
			consumeSkipWS(_RCURLY);
		}
		consumeSkipWS(_LCURLY);
		XQAST conSeq = new XQAST(XQAST.ContentSequence);
		elem.addChild(conSeq);
		XQAST expr = expr();
		if (expr != null) {
			conSeq.addChild(expr);
		}
		consumeSkipWS(_RCURLY);
		return elem;
	}

	private XQAST compAttrConstructor() {
		// TODO Auto-generated method stub
		return null;
	}

	private XQAST compNamespaceConstructor() {
		// TODO Auto-generated method stub
		return null;
	}

	private XQAST compTextConstructor() throws QueryException {
		Token la = laSkipWS(_TEXT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(_LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQAST.CompTextConstructor);
		doc.addChild(expr());
		consume(_RCURLY);
		return doc;
	}

	private XQAST compCommentConstructor() throws QueryException {
		Token la = laSkipWS(_COMMENT);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(_LCURLY);
		if (la2 == null) {
			return null;
		}
		consume(la);
		consume(la2);
		XQAST doc = new XQAST(XQAST.CompCommentConstructor);
		doc.addChild(expr());
		consume(_RCURLY);
		return doc;
	}

	private XQAST compPIConstructor() {
		// TODO Auto-generated method stub
		return null;
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
		Token la2 = laSkipWS(_SHARP);
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
		Token la = laSkipWS(_FUNCTION);
		if (la == null) {
			return null;
		}
		Token la2 = laSkipWS(la, _LPAR);
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
		} while (attemptSkipWS(_COMMA));
		consumeSkipWS(_RPAR);
		if (attemptSkipWS(_AS)) {
			inlineFunc.addChild(sequenceType());
		}
		inlineFunc.addChild(enclosedExpr());
		return inlineFunc;
	}

	private XQAST enclosedExpr() throws QueryException {
		if (!attemptSkipWS(_LCURLY)) {
			return null;
		}
		XQAST expr = expr();
		consumeSkipWS(_RCURLY);
		return expr;
	}

	private XQAST param() throws QueryException {
		if (!attemptSkipWS(_DOLLAR)) {
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
