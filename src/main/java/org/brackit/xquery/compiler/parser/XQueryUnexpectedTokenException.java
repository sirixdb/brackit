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

import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

public class XQueryUnexpectedTokenException extends RecognitionException {
	private static final long serialVersionUID = 2490469494230060870L;

	public int expecting = Token.INVALID_TOKEN_TYPE;
	protected final BaseRecognizer parser;

	/** Used for remote debugger deserialization */
	public XQueryUnexpectedTokenException() {
		this.parser = null;
	}

	public XQueryUnexpectedTokenException(BaseRecognizer parser, int expecting,
			IntStream input) {
		super(input);
		this.parser = parser;
		this.expecting = expecting;
	}

	public String toString() {
		try {
			String result = "Expecting token "
					+ parser.getTokenNames()[expecting] + "(" + expecting
					+ "), but got "
					+ parser.getTokenNames()[getUnexpectedType()] + "("
					+ getUnexpectedType() + ")";
			return result;
		} catch (ArrayIndexOutOfBoundsException e) {
			return "Unexpected token exception with unidentifiable token was thrown: expected "
					+ expecting + " got " + getUnexpectedType();
		}
	}
}
