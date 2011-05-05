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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.TokenStream;

/*
 * FIXME: due to a bug in ANTLR, we have to always assume CommonToken instances
 * are coming, because the EOF token is hard coded to be of type CommonToken,
 * ignoring the TokenLabelType option in the grammar. Also a problem with
 * fragment tokens, but it does not happen here
 */
public class XQueryTokenStream implements TokenStream {

	protected List<XQueryToken> buffer;

	/**
	 * The lexer rules for token generation are different when XML fragments are
	 * being parsed. Since there are no reserved keywords in XQuery, we need to
	 * switch between lexers whenever the input switches between XML and XQuery.
	 * The general tokenSource object serves as a pointer to the currently
	 * activated lexer, which is either lexerXML or lexerXQuery
	 */
	protected TokenSource tokenSource;
	protected XMLexer lexerXML;
	protected XQueryLexer lexerXQ;

	protected int channel = Token.DEFAULT_CHANNEL;

	protected int p = 0;

	protected int lastMarker = -1;

	protected boolean inXML = false;

	// TODO: remove this flags, or at least use log4j
	private static final boolean ENABLE_WARN = false;
	private static final boolean ENABLE_LOG = false;

	public XQueryTokenStream(XMLexer lexerXML, XQueryLexer lexerXQ) {
		this.lexerXML = lexerXML;
		this.lexerXQ = lexerXQ;

		this.buffer = new ArrayList<XQueryToken>(500);

		// XQuery lexer is default
		this.tokenSource = this.lexerXQ;
	}

	protected boolean ensureBufferSize(int n) {
		if (buffer.size() < n) {
			Token t = null;
			XQueryToken lastHiddenToken = null;

			do {
				t = tokenSource.nextToken();

				if (t instanceof XQueryToken) {
					XQueryToken xqToken = (XQueryToken) t;
					if (t.getChannel() != channel) {
						if (t != null) {
							xqToken.setPreviousHiddenToken(lastHiddenToken);
						}
						lastHiddenToken = xqToken;
					} else {
						// token will be appended to buffer, so index is the
						// current size
						t.setTokenIndex(buffer.size());

						if (lastHiddenToken != null) {
							xqToken.setPreviousHiddenToken(lastHiddenToken);
							lastHiddenToken = null;
						}
						buffer.add(xqToken);

						log("Token inserted into buffer from lexer "
								+ tokenSource.getClass().getSimpleName() + ": "
								+ t.toString());
					}
				} else {
					warn("Got token of type " + t.getClass().getSimpleName()
							+ " from Lexer, instead of XQueryToken");
				}
			} while (buffer.size() < n && t != null && t.getType() != Token.EOF);

			return (buffer.size() >= n);
		} else {
			return true;
		}
	}

	@Override
	public Token LT(int k) {
		int index = 0;
		if (k == 0) {
			return null;
		} else if (k < 0) {
			index = p + k;
		} else {
			index = p + k - 1;
		}

		return get(index);
	}

	@Override
	public Token get(int i) {
		if (i < 0) {
			return null;
		} else if (ensureBufferSize(i + 1)) {
			return buffer.get(i);
		} else {
			return Token.EOF_TOKEN;
		}
	}

	@Override
	public TokenSource getTokenSource() {
		return tokenSource;
	}

	@Override
	public String toString(int start, int stop) {
		if (start < 0 || stop < 0) {
			warn("Trying to get text from tokens with negative indexes. Returning null...");
			return null;
		}
		int endIndex = ensureBufferSize(stop + 1) ? stop : buffer.size() - 1;

		StringBuffer text = new StringBuffer();
		for (int i = start; i <= endIndex; i++) {
			XQueryToken t = (XQueryToken) get(i);

			// write content of preceding hidden tokens
			/*
			 * TODO: currently disabled, see if hidden tokens must actually be
			 * in text XQueryToken hiddenToken = t.getPreviousHiddenToken();
			 * while (hiddenToken != null) { text.append(hiddenToken.getText());
			 * hiddenToken = hiddenToken.getPreviousHiddenToken(); }
			 */

			text.append(t.getText());
		}

		return text.toString();
	}

	@Override
	public String toString(Token start, Token stop) {
		if (start != null && stop != null) {
			return toString(start.getTokenIndex(), stop.getTokenIndex());
		}
		warn("Trying to get text from null tokens with negative indexes. Returning null...");
		return null;
	}

	@Override
	public int LA(int i) {
		return LT(i).getType();
	}

	@Override
	public void consume() {
		p++;
		log("\tPointer advanced to position " + p);
	}

	@Override
	public String getSourceName() {
		return tokenSource.getSourceName();
	}

	@Override
	public int index() {
		return p;
	}

	@Override
	public int mark() {
		log("\tMarking current position");
		lastMarker = index();
		return lastMarker;
	}

	@Override
	public void release(int marker) {
		// nothing necessary
	}

	@Override
	public void rewind() {
		log("\tRewinding to last marker");
		rewind(lastMarker);
	}

	@Override
	public void rewind(int marker) {
		seek(marker);
	}

	@Override
	public void seek(int index) {
		p = index;
		log("\tSeeking pointer to position " + index);
	}

	@Override
	/**
	 * Doesn't make much sense, since size is only known until all tokens
	 * are placed in the buffer, which here only happens on demand. Method
	 * will therefore return the current number of tokens in buffer.
	 */
	public int size() {
		return buffer.size();
	}

	public void leaveXQuery() {
		log("Leave XQuery");
		tokenSource = lexerXML;
		inXML = true;
	}

	public void enterDirXml() {
		log("Enter XML level " + (lexerXML.stateStack.size() + 1));
		tokenSource = lexerXML;
		lexerXML.pushState(inXML);
		inXML = true;
	}

	public void leaveDirXml() {
		log("Leave XML level " + lexerXML.stateStack.size());
		if (!lexerXML.popState())
			enterXQuery();
	}

	public void enterXQuery() {
		log("Enter XQuery");
		tokenSource = lexerXQ;
		inXML = false;
	}

	protected void log(String message) {
		if (ENABLE_LOG) {
			System.out.println(message);
		}
	}

	protected void warn(String message) {
		if (ENABLE_WARN) {
			System.out.println("WARNING XQueryTokenStream: " + message);
		}
	}
}
