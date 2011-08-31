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

import org.brackit.xquery.compiler.parser.Tokenizer;

/**
 * @author Sebastian Baechle
 *
 */
public class PathParser extends Tokenizer {

	private final Path<String> p;
	
	public PathParser(String s) {
		super(s);
		p = new Path<String>();
	}

	public Path<String> parse() throws PathException {
		try {
			startStep();
			while (axisStep());
			attributeStep();
			consumeEOF();
			return p;
		} catch (TokenizerException e) {
			throw new PathException(e, e.getMessage());
		}
	}

	private void startStep() {
		if (attempt("..")) {
			p.parent();			
		} else if (attempt(".")) {
			p.self();
		} else {
			 Token la = laQName();
			 if (la != null) {
				 consume(la);
				 p.self().child(la.toString());
			 }
		}
	}

	private boolean axisStep() throws TokenizerException {
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

	private boolean namedStep() throws TokenizerException {
		String s = null;
		Token la = la("//");
		if (la != null) {
			if (la(la, "@") != null) {
				return false;
			}
			consume(la);
			if (!attempt("*")) {
				la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				s = la.toString();
			}
			p.descendant(s);
			return true;
		} else if ((la = la("/")) != null) {
			if (la(la, "@") != null) {
				return false;
			}
			consume(la);
			if (!attempt("*")) {
				la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				s = la.toString();
			}
			p.child(s);
			return true;
		}
		return false;
	}

	private void attributeStep() throws TokenizerException {
		String s = null;
		if (attempt("//@")) {
			if (!attempt("*")) {
				Token la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				s = la.toString();
			}
			p.descendantAttribute(s);
		} else if (attempt("/@")) {			
			if (!attempt("*")) {
				Token la = laQName();
				if (la == null) {
					throw new MismatchException("Wildcard", "QName");
				}
				consume(la);
				s = la.toString();
			}
			p.attribute(s);
		}
	}
}
