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
/*=============================================================================

 Copyright 2009 Nikolay Ognyanov

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 =============================================================================*/
/*
 This file is a derivate of the original XQuery grammar provided by Nikolay Ognyanov.  
 */
package org.brackit.xquery.compiler.parser;

import java.util.Stack;

import org.antlr.runtime.BitSet;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.brackit.xquery.ErrorCode;

/**
 * Base class for generated XQuery parser. Has the following roles:
 * <ol>
 * <li>Produce tokens of custom type.</li>
 * <li>Provide control over behavior upon errors.</li>
 * <li>Switch between XQuery and direct XML lexers.</li>
 * <li>Provide utilities for "add-on" parsing of details which can not or should
 * better not be handled in the generated parser.</li>
 * </ol>
 */
public class ParserBase extends org.antlr.runtime.Parser {
	private static final int ERROR_PARAPHRASE_LENGTH = 40;

	/**
	 * Utility container class for object pairs.
	 * 
	 * @param <F>
	 *            Type of the first element in the pair.
	 * @param <S>
	 *            Type of the second element in the pair.
	 */
	class Pair<F, S> {
		private F first;
		private S second;

		public Pair(F first, S second) {
			this.first = first;
			this.second = second;
		}

		public Pair() {
		}

		public F getFirst() {
			return first;
		}

		public void setFirst(F first) {
			this.first = first;
		}

		public S getSecond() {
			return second;
		}

		public void setSecond(S second) {
			this.second = second;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((first == null) ? 0 : first.hashCode());
			result = prime * result
					+ ((second == null) ? 0 : second.hashCode());
			return result;
		}

		@SuppressWarnings("all")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair other = (Pair) obj;
			if (first == null) {
				if (other.first != null)
					return false;
			} else if (!first.equals(other.first))
				return false;
			if (second == null) {
				if (other.second != null)
					return false;
			} else if (!second.equals(other.second))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "[" + (first != null ? first.toString() : "null") + ", "
					+ (second != null ? second.toString() : "null") + "]";
		}
	}

	private CharStream source;
	// private Stack<TokenSource> lexerStack = new Stack<TokenSource>();
	private Stack<String> elemStack = new Stack<String>();
	private boolean breakOnError = true;
	@SuppressWarnings("all")
	private int NCName;
	private int Colon;

	public ParserBase(XQueryTokenStream input) {
		this(input, new RecognizerSharedState());
	}

	public ParserBase(TokenStream input, RecognizerSharedState state) {
		super(input, state);
		source = ((Lexer) input.getTokenSource()).getCharStream();
	}

	/**
	 * Set a flag which determines error handling behavior. If the flag is true
	 * then a runtime exception is thrown upon error. Default value is "true"
	 * 
	 * @param breakOnError
	 *            the value to be set
	 */
	public void setBreakOnError(boolean breakOnError) {
		this.breakOnError = breakOnError;
	}

	/**
	 * Retrieves the value of the error handling flag.
	 * 
	 * @return value of the flag
	 */
	public boolean getBreakOnError() {
		return breakOnError;
	}

	/**
	 * A placeholder for implementation of custom error message handling.
	 */
	@Override
	public void emitErrorMessage(String message) {
		super.emitErrorMessage(message);
	}

	/**
	 * Overriden in order to provide control of error handling through
	 * {@link setBreakOnError}.
	 */
	@Override
	public void reportError(RecognitionException e) {
		super.reportError(e);
		if (breakOnError) {
			throw new RuntimeException(getErrorHeader(e) + " "
					+ getErrorMessage(e, getTokenNames()), e);
		}
	}

	public String errorMessage(String query, RecognitionException e) {
		String context = "";
		try {
			// System.out.println(Arrays.toString(query.toCharArray()));
			// System.out.println("Length " + query.length());
			// System.out.println("Error at " + e.charPositionInLine +
			// " in line " + e.line);
			int start = 0;
			// int linebreakAt = 0;
			// while ((linebreakAt = query.indexOf('\n', linebreakAt)) > -1)
			// System.out.println("Linebreak at " + linebreakAt++);
			for (int i = 0; i < e.line - 1; i++) {
				// System.err.println(start + " -> " + (query.indexOf('\n',
				// start)));
				start = query.indexOf('\n', start);
			}
			// System.out.println("Char at current start " +
			// query.charAt(start));
			int absPos = start + e.charPositionInLine;
			// System.out.println("abs pos " + absPos);
			start = Math.max(0, absPos - ERROR_PARAPHRASE_LENGTH);
			absPos = Math.max(absPos, Math.min(start + ERROR_PARAPHRASE_LENGTH,
					query.length()));
			// System.out.println("Start is " + start);
			// System.out.println("End is  " + absPos);
			context = query.substring(start, absPos).replace('\n', ' ');
		} catch (RuntimeException re) {
			System.err.println(re);
		}
		String msg = getErrorHeader(e) + "..." + context + ": "
				+ getErrorMessage(e, getTokenNames());
		return msg;
	}

	/**
	 * Work around an ANTLR bug that causes a NullPointerException when trying
	 * to recover at the end of the input stream. Also ensure that inserted
	 * tokens are of type XQueryToken.
	 */
	@Override
	protected Object getMissingSymbol(IntStream input, RecognitionException re,
			int expectedTokenType, BitSet follow) {
		String tokenText = null;

		if (expectedTokenType == Token.EOF) {
			tokenText = "<missing EOF>";
		} else if (expectedTokenType >= 0
				&& expectedTokenType < getTokenNames().length) {
			tokenText = "<missing " + getTokenNames()[expectedTokenType] + ">";
		} else {
			throw new Error("invalid expectedTokenType " + expectedTokenType);
		}

		XQueryToken t = new XQueryToken(expectedTokenType, tokenText);
		Token current = ((TokenStream) input).LT(1);
		if (current == null || current.getType() == Token.EOF) {
			current = ((TokenStream) input).LT(-1);
		}
		if (current != null) {
			// If there are any other position-related fields in your MyToken
			// class, set them here.
			t.setLine(current.getLine());
			t.setCharPositionInLine(current.getCharPositionInLine());
		}
		t.setChannel(DEFAULT_TOKEN_CHANNEL);

		return t;
	}

	/**
	 * Convinience method for throwing XQRecognitionException.
	 */
	protected void raiseError(String message) throws RecognitionException {
		throw new XQueryRecognitionException(ErrorCode.ERR_PARSING_ERROR,
				input, message);
	}

	/**
	 * Notifies the base parser that generated parser enters a direct XML
	 * element declaration. The action taken is to instantiate and start using a
	 * new XMLexer.
	 */
	protected void enterDirXml() {
		// lexerStack.push(input.getTokenSource());
		// XMLexer XMLexer = new XMLexer(source);
		// ((XQueryTokenStream) input).setSource(XMLexer);
		// ((CommonTokenStream) input).setTokenSource(XMLexer);
		((XQueryTokenStream) input).enterDirXml();
	}

	/**
	 * Notifies the base parser that generated parser leaves a direct XML
	 * element definition. The action taken is to discard current XMLexer in use
	 * and switch back to the lexer used before it was instantiated.
	 */
	protected void leaveDirXml() {
		((XQueryTokenStream) input).leaveDirXml();
	}

	/**
	 * Notifies base parser that generated parser enters section of computed
	 * xquery code embedded within direct XML. The action taken is to
	 * instantiate and start using a new instance of XQLexer.
	 */
	protected void enterXQuery() {
		// lexerStack.push(input.getTokenSource());
		// XQueryLexer xqueryLexer = new XQueryLexer(source);
		// ((XQueryTokenStream) input).setSource(xqueryLexer);
		// ((CommonTokenStream) input).setTokenSource(xqueryLexer);
		((XQueryTokenStream) input).enterXQuery();
	}

	/**
	 * Notifies base parser that generated parser leaves section of computed
	 * xquery code embedded within direct XML. The action taken is to discard
	 * current XQLexer and switch back to the lexer used befeore it was
	 * instantiated.
	 */
	protected void leaveXQuery() {
		// popLexer();
		((XQueryTokenStream) input).leaveXQuery();
	}

	private void popLexer() {
		// TokenSource tokenSource = lexerStack.pop();
		// ((LazyTokenStream) input).setSource(tokenSource);
	}

	/**
	 * An empty hook. Called when generated parser encounters encoding
	 * declaration. According to W3C recommendation handling of such declaration
	 * is implementation dependent, so here it is left to language prpcessor
	 * designers...
	 */
	protected void checkEncoding() {
		// String encoding = input.get(input.index()).getText();
		// System.out.println("Encoding: " + encoding);
	}

	/**
	 * Check that current token is not preceded by blank space and throw error
	 * if it is.
	 * 
	 * @throws RecognitionException
	 */
	protected void noSpaceBefore() throws RecognitionException {
		// if (((XQueryToken) input.get(input.index())).getHiddenPredecessor()
		// != null) {
		// raiseError("Space not allowed before '"
		// + input.get(input.index()).getText() + "'.");
		// }
	}

	/**
	 * Check that current token, if preceded by specified other token, is
	 * separated from it by blank space and throw error if it is not.
	 * 
	 * @param previous
	 *            the kind of preceding token for which the check is to be made
	 */
	protected void needSpaceBetween(int previous) throws RecognitionException {
		// if ((input.LA(-1) == previous)
		// && !(((XQueryToken) input.get(input.index())).getHiddenPredecessor()
		// != null)) {
		// raiseError("Space required before "
		// + input.get(input.index()).getText() + "'.");
		// }
	}

	/**
	 * Push direct xml element on stack, so that later its name can be compared
	 * to name of the closing tag (if any).
	 */
	protected void pushElemName(String tag) {
		elemStack.push(tag);
		// System.out.println("pushing element " + tag);
	}

	/**
	 * Pop direct xml element from stack. Called if element is terminated
	 * immediately by '/>'
	 */
	protected void popElemName() {
		elemStack.pop();
	}

	/**
	 * Check whether name of closing direct xml element tag matches name of
	 * opening tag. Throw error if names do not match.
	 * 
	 * @throws RecognitionException
	 */
	protected void matchElemName(String tag) throws RecognitionException {
		String lastTag = elemStack.pop();
		if (!lastTag.equals(tag)) {
			raiseError("Closing tag name " + tag
					+ " does not match the opening tag name " + lastTag);
		}
	}

	private String getQName(int index) throws RecognitionException {
		if ((index < 2) || input.get(index - 1).getType() != Colon) {
			return input.get(index).getText();
		} else {
			// if(input.get(index - 2).getType() != NCName) {
			// raiseError("Parser internal error.");
			// }
			return input.get(index - 2).getText()
					+ input.get(index - 1).getText()
					+ input.get(index).getText();
		}
	}

	/**
	 * Set values of some token codes needed by "add-on" parsing methods.
	 * 
	 * @param NCName
	 *            code of the NCName token
	 * @param Colon
	 *            code of the Colon token
	 * @return
	 */
	protected boolean setTokenCodes(int NCName, int Colon) {
		this.NCName = NCName;
		this.Colon = Colon;
		return false;
	}

	/**
	 * Extract content of direct xml comment constructor parsed by generated
	 * parser. Throws error if contenct contains the forbidden sequence '--'.
	 * 
	 * @return comment content
	 * @throws RecognitionException
	 */
	protected String parseDirComment() throws RecognitionException {
		Token token = input.get(input.index());
		String content = token.getText();
		content = content.substring(4, content.length() - 3);
		int length = content.length();
		if (length > 0
				&& (content.contains("--") || content.charAt(length - 1) == '-')) {
			raiseError("String '--' not allowed in xml comment.");
		}

		return content;
	}

	/**
	 * Extract content of direct xml CDATA section constructor parsed by the
	 * generated parser.
	 * 
	 * @return CDATA section content.
	 * 
	 * @throws RecognitionException
	 */
	protected String parseCData() throws RecognitionException {
		String text = input.get(input.index()).getText();
		String content = text.substring(9);
		content = content.substring(0, content.length() - 3);

		return content;
	}

	/**
	 * Extract target and content of direct xml processing instruction parsed by
	 * the generated parser. Throws excetion if procesing instruction target is
	 * invalid (equal to 'xml' ignoring case).
	 * 
	 * @return pair of strings containing processing instruction target and
	 *         content.
	 * 
	 * @throws RecognitionException
	 */
	protected Pair<String, String> parseDirPI() throws RecognitionException {
		String text = input.get(input.index()).getText();
		if (text.charAt(2) <= '\u0020') {
			raiseError("Procesing instruction may not start with wihte space.");
		}
		int limit = text.length() - 2;
		int i = 2;
		while ((i < limit) && (text.charAt(i) > '\u0020')) {
			++i;
		}
		String target = text.substring(2, i);
		if (target.equalsIgnoreCase("xml")) {
			raiseError(target + " is not a valid processing instruction name.");
		}
		while (text.charAt(i) <= '\u0020') {
			++i;
		}
		String content = text.substring(i, limit);

		return new Pair<String, String>(target, content);
	}

	/**
	 * Extract name and content of direct xml pragma constructor parsed by the
	 * generated parser.
	 * 
	 * @return a pair where the first element is pair of prefix and name
	 *         constituting a QName and second element is pragma text content.
	 * 
	 * @throws RecognitionException
	 */
	protected Pair<Pair<String, String>, String> parsePragma()
			throws RecognitionException {
		String text = input.get(input.index()).getText();
		int start = 2;
		while (text.charAt(start) <= '\u0020') {
			++start;
		}
		int limit = text.length() - 2;
		int i = start;
		while ((i < limit) && (text.charAt(i) > '\u0020')) {
			++i;
		}
		String prefix = "";
		String target = text.substring(start, i);
		if (text.charAt(i) == ':') {
			while ((i < limit) && (text.charAt(i) > '\u0020')) {
				++i;
			}
			prefix = target;
			target = text.substring(2, i);
		}
		while (text.charAt(i) <= '\u0020') {
			++i;
		}
		String content = text.substring(i, limit);

		return new Pair<Pair<String, String>, String>(new Pair<String, String>(
				prefix, target), content);
	}
}
