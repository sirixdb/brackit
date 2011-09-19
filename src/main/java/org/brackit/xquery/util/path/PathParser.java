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
package org.brackit.xquery.util.path;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.parser.Tokenizer;
import org.brackit.xquery.module.Namespaces;

/**
 * @author Sebastian Baechle, Max Bechtold
 *
 */
public class PathParser extends Tokenizer {

	private final Path<QNm> p;
	
	private Namespaces namespaces;
	
	public PathParser(String s) {
		super(s);
		p = new Path<QNm>();
	}

	public Path<QNm> parse() throws PathException {
		try {
			nsPreamble();
			startStep();
			while (axisStep());
			attributeStep();
			consumeEOF();
			return p;
		} catch (TokenizerException e) {
			throw new PathException(e, e.getMessage());
		}
	}
	
	/**
	 * Allows for inlined namespace declarations of the form 
	 * ( "namespace" NCNAME "=" stringLiteral ";")&#042; preceding a path 
	 * expression.
	 */
	private void nsPreamble() throws TokenizerException, PathException {
		Token la = la("namespace");
		while (la != null) {
			consume(la);
			String prefix;
			String uri;
			Token pfx = laNCNameSkipWS();
			if (pfx == null) {
				throw new MismatchException("NCName");
			}
			prefix = pfx.toString();
			consume(pfx);
			
			if (!attemptSymSkipWS("=")) {
				throw new MismatchException("=");
			}
			
			Token uriToken = laStringSkipWS(false);
			if (uriToken == null) {
				throw new MismatchException("URI");
			}
			uri = uriToken.toString();
			consume(uriToken);
			
			if (!attemptSymSkipWS(";")) {
				throw new MismatchException(";");
			}
			
			if (namespaces == null) {
				namespaces = new Namespaces();
			}
			try {
				namespaces.declare(prefix, uri);
			} catch (QueryException e) {
				throw new PathException(e);
			}
			attemptWS();
			la = la("namespace");
		}
	}

	private void startStep() throws PathException {
		if (attempt("..")) {
			p.parent();			
		} else if (attempt(".")) {
			p.self();
		} else {
			 EQNameToken la = laQName();
			 if (la != null) {
				 consume(la);
				 p.self().child(expand(la.qname()));
			 }
		}
	}

	private boolean axisStep() throws TokenizerException, PathException {
		return ((parentStep()) || (selfStep()) || (namedStep()));
	}

	private boolean parentStep() {
		if (attempt("/..")) {
			p.parent();
			return true;
		}
		return false;
	}
	
	private boolean selfStep() {
		if (attempt("/.")) {
			p.self();
			return true;
		}
		return false;
	}

	private boolean namedStep() throws TokenizerException, PathException {
		QNm q = null;
		Token la = la("//");
		if (la != null) {
			if (la(la, "@") != null) {
				return false;
			}
			consume(la);
			if (!attempt("*")) {
				EQNameToken ela = laQName();
				if (ela == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(ela);
				q = ela.qname();
			}
			p.descendant(expand(q));
			return true;
		} else if ((la = la("/")) != null) {
			if (la(la, "@") != null) {
				return false;
			}
			consume(la);
			if (!attempt("*")) {
				EQNameToken ela = laQName();
				if (ela == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(ela);
				q = ela.qname();
			}
			p.child(expand(q));
			return true;
		}
		return false;
	}

	private void attributeStep() throws TokenizerException, PathException {
		QNm q = null;
		if (attempt("//@")) {
			if (!attempt("*")) {
				EQNameToken la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				q = la.qname();
			}
			p.descendantAttribute(expand(q));
		} else if (attempt("/@")) {			
			if (!attempt("*")) {
				EQNameToken la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				q = la.qname();
			}
			p.attribute(expand(q));
		}
	}
	
	/**
	 * Resolves prefixed named steps like 'bit:email' against the namespace 
	 * declarations in the preamble (if any).
	 * 
	 */
	private QNm expand(QNm qname) throws PathException {
		if (namespaces == null || qname.prefix == null) {
			return qname;
		}
		try {
			return namespaces.expand(qname.prefix, qname.localName);
		} catch (QueryException e) {
			throw new PathException(e);
		}
	}
}
