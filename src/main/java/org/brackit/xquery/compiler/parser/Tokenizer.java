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

import java.math.BigInteger;
import java.util.Arrays;

import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.util.log.Logger;
import org.brackit.xquery.xdm.XMLChar;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Tokenizer {

	private static final Logger log = Logger.getLogger(Tokenizer.class);

	private int pos;
	private int lastScanEnd;
	private final int end;
	private final char[] input;

	public class TokenizerException extends Exception {
		public TokenizerException(String message, Object... args) {
			super(String.format(message, args));
		}

		public TokenizerException(Exception e, String message, Object... args) {
			super(String.format(message, args), e);
		}
	}

	public class MismatchException extends TokenizerException {
		public MismatchException(String... expected) {
			super("Expected one of %s: '%s'", Arrays.toString(expected),
					paraphrase());
		}
	}

	public class IllegalCharRefException extends TokenizerException {
		public IllegalCharRefException(String charRef) {
			super("Illegal Unicode codepoint %s: '%s'", charRef, paraphrase());
		}
	}

	protected class Token {
		final int start;
		final int end;

		public Token(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public String string() {
			return new String(input, start, end - start);
		}

		public String toString() {
			return string();
		}
	}

	protected class StringToken extends Token {
		final String s;

		public StringToken(int start, int end, String s) {
			super(start, end);
			this.s = s;
		}

		public String string() {
			return s;
		}
	}

	protected class EQNameToken extends Token {
		final String uri;
		final String prefix;
		final String ncname;

		public EQNameToken(int start, int end, String uri, String prefix,
				String ncname) {
			super(start, end);
			this.uri = uri;
			this.ncname = ncname;
			this.prefix = prefix;
		}

		public String uri() {
			return uri;
		}

		public String ncname() {
			return ncname;
		}

		public String prefix() {
			return prefix;
		}

		public QNm qname() {
			return new QNm(uri, prefix, ncname);
		}

		public String string() {
			return (uri != null) ? ("\"" + uri + "\":")
					+ ((prefix != null) ? prefix + ":" + ncname : ncname)
					: ((prefix != null) ? prefix + ":" + ncname : ncname);
		}
	}

	public Tokenizer(String s) {
		this.input = s.toCharArray();
		this.end = input.length;
	}

	protected int position() {
		return pos;
	}

	protected void resetTo(int pos) throws TokenizerException {
		if ((pos < 0) || (pos > end)) {
			throw new TokenizerException("Illegal position: %s", pos);
		}
		this.pos = pos;
	}

	protected Token la(String token) {
		return la(pos, token);
	}

	protected Token la(Token prev, String token) {
		return la(prev.end, token);
	}

	protected Token laSkipS(String token) {
		return laSkipS(pos, token);
	}

	protected Token laSkipS(Token prev, String token) {
		return laSkipWS(prev.end, token);
	}

	private Token laSkipS(int from, String token) {
		int s = from + s(from);
		int e = s;
		int len = token.length();
		if (end - e < len) {
			return null;
		}
		for (int i = 0; i < len; i++) {
			if (token.charAt(i) != input[e++]) {
				return null;
			}
		}
		return new Token(s, e);
	}

	protected Token laSkipWS(String token) {
		return laSkipWS(pos, token);
	}

	protected Token laSkipWS(Token prev, String token) {
		return laSkipWS(prev.end, token);
	}

	private Token laSkipWS(int from, String token) {
		int s = from + ws(from);
		int e = s;
		int len = token.length();
		if (end - e < len) {
			return null;
		}
		for (int i = 0; i < len; i++) {
			if (token.charAt(i) != input[e++]) {
				return null;
			}
		}
		return new Token(s, e);
	}

	protected Token laSymSkipWS(String token) {
		return laSymSkipWS(pos, token);
	}
	
	protected Token laSymSkipS(Token prev, String token) {
		return laSymSkipS(prev.end, token);
	}
	
	private Token laSymSkipS(int from, String token) {
		return laSym(from + s(from), token);
	}
	
	protected Token laSymSkipS(String token) {
		return laSymSkipWS(pos + s(pos), token);
	}

	protected Token laSymSkipWS(Token prev, String token) {
		return laSymSkipWS(prev.end, token);
	}

	private Token laSymSkipWS(int from, String token) {
		return laSym(from + ws(from), token);
	}

	private Token laSym(int pos, String token) {
		int s = pos;
		int e = s;
		int len = token.length();
		if (end - e < len) {
			return null;
		}
		for (int i = 0; i < len; i++) {
			if (token.charAt(i) != input[e++]) {
				return null;
			}
		}
		boolean isSym = (e == end) || (isSymDel(input[e]));
		return isSym ? new Token(s, e) : null;
	}

	private boolean isSymDel(char c) {
		// char is (prefix of) a symbol separator (whitespace or comment)
		// see A.2.2 Terminal Delimitation for details
		return ((XMLChar.isWS(c)) || (c == '(') || (isDelChar(c)));
	}

	private boolean isDelChar(char c) {
		// char is (prefix of) delimiting terminal symbol
		// see A.2.2 Terminal Delimitation for details
		return ((XMLChar.isWS(c)) || (c == '!') || (c == '\'') || (c == '"')
				|| (c == '#') || (c == '$') || (c == '%') || (c == '(')
				|| (c == ')') || (c == '*') || (c == '+') || (c == ',')
				|| (c == '-') || (c == '.') || (c == '/') || (c == ':')
				|| (c == ';') || (c == '<') || (c == '=') || (c == '>')
				|| (c == '?') || (c == '@') || (c == '[') || (c == ']')
				|| (c == '{') || (c == '|') || (c == '}'));
	}

	protected Token la(int from, String token) {
		int s = from;
		int e = s;
		int len = token.length();
		if (end - e < len) {
			return null;
		}
		for (int i = 0; i < len; i++) {
			if (token.charAt(i) != input[e++]) {
				return null;
			}
		}
		return new Token(s, e);
	}

	protected boolean attemptWS() {
		Token la = laWS();
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}
	
	protected boolean attemptS() {
		Token la = laS();
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}

	protected boolean attemptSkipWS(String token) {
		Token la = laSkipWS(pos, token);
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}
	
	protected boolean attemptSkipS(String token) {
		Token la = laSkipS(pos, token);
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}

	protected boolean attemptSymSkipWS(String token) {
		Token la = laSymSkipWS(pos, token);
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}
	
	protected boolean attemptSymSkipS(String token) {
		Token la = laSymSkipS(pos, token);
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}

	protected boolean attempt(String token) {
		Token la = la(pos, token);
		if (la == null) {
			return false;
		}
		consume(la);
		return true;
	}

	protected void consumeEOF() throws TokenizerException {
		int ws = ws(pos);
		int p = pos + ws;
		// ignore trailing '\0'
		while ((p < end) && (input[p] == '\u0000'))
			p++;
		if (p != end) {
			throw new TokenizerException("Expected end of query: %s",
					paraphrase());
		}
	}

	protected void consumeSymSkipWS(String token) throws TokenizerException {
		Token la = laSymSkipWS(pos, token);
		if (la == null) {
			throw new TokenizerException("Expected '%s': '%s'", token,
					paraphrase());
		}
		consume(la);
	}

	protected void consumeSkipWS(String token) throws TokenizerException {
		Token la = laSkipWS(pos, token);
		if (la == null) {
			throw new TokenizerException("Expected '%s': '%s'", token,
					paraphrase());
		}
		consume(la);
	}

	protected void consume(Token token) {
		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Consuming " + token + " (to [" + token.start + ":"
					+ token.end + "]/" + end + ")");
		}
		pos = token.end;
	}

	protected void consume(String token) throws TokenizerException {
		Token la = la(pos, token);
		if (la == null) {
			throw new TokenizerException("Expected '%s' after: '%s'", token,
					paraphrase());
		}
		consumeSkipWS(token);
	}

	protected String paraphrase() {
		int line = 0;
		int linePos = 0;
		int p = 0;
		while (p < pos) {
			linePos++;
			if (input[p++] == '\n') {
				line++;
				linePos++;
			}
		}
		char[] preBuf = new char[60];
		final int preLen = 60 - 1;
		int start = preLen;
		p = (pos == end) ? pos - 1 : pos;
		while ((p >= 0) && (start > 0)) {
			if ((!XMLChar.isWS(input[p]))
					|| ((start < preLen) && (!XMLChar.isWS(preBuf[start + 1])))) {
				preBuf[start] = input[p];
			}
			p--;
			start--;
		}
		String paraphrase = new String(preBuf, start + 1, preLen - start);
		return String.format("[%s:%s|%s:%s] %s", line, linePos, pos, end,
				paraphrase);
	}

	protected Token laWS() {
		int ws = ws(pos);
		return (ws > 0) ? new Token(pos, pos + ws) : null;
	}

	protected Token laS(Token token) {
		return laS(token.end);
	}

	protected Token laS() {
		return laS(pos);
	}

	private Token laS(int pos) {
		int s = s(pos);
		return (s > 0) ? new Token(pos, pos + s) : null;
	}

	protected boolean skipS() {
		int s = s(pos);
		if (s <= 0) {
			return false;
		}

		if ((XQuery.DEBUG) && (log.isDebugEnabled())) {
			log.debug("Skipping whitespace from " + pos + " to " + (pos + s));
		}
		pos += s;
		return true;
	}

	private int s(int from) {
		int p = from;
		char c;
		while (p < end) {
			c = input[p];
			// skip whitespace characters
			if ((c == ' ') || (c == '\r') || (c == '\t') || (c == '\n')) {
				p++;
				continue;
			}
			break;
		}
		return p - from;
	}

	private int ws(int from) {
		int p = from;
		char c;
		while (p < end) {
			c = input[p];
			// skip whitespace characters
			if ((c == ' ') || (c == '\r') || (c == '\t') || (c == '\n')) {
				p++;
				continue;
			}
			// check for (nested) comment
			if ((c == '(') && (p < end) && (input[p + 1] == ':')) {
				int len = comment(p);
				if (len == 0) {
					p++;
					break;
				}
				p = p + len;
				continue;
			}
			break;
		}
		return p - from;
	}

	private int comment(int from) {
		int e = from;
		char c = input[e++];
		if ((c != '(') || (e >= end) || ((c = input[e++]) != ':')) {
			return 0;
		}
		int len = 2; // the starting '(:'
		int depth = 0;
		while (e < end) {
			char p = c;
			c = input[e++];
			len++;
			if (c == ':') {
				if (p == '(') {
					depth++; // open nested comment
				}
			} else if (c == ')') {
				if (p == ':') {
					if (depth == 0) {
						return len;
					}
					depth--; // close nested comment
				}
			}
		}
		return 0;
	}

	protected EQNameToken laQName() {
		return laQName(pos);
	}

	protected EQNameToken laQNameSkipWS(Token token) {
		return laQName(token.end + ws(token.end));
	}

	protected EQNameToken laQNameSkipWS() {
		return laQName(pos + ws(pos));
	}

	protected EQNameToken laQName(Token token) {
		return laQName(token.end);
	}

	private EQNameToken laQName(int pos) {
		Token la = laNCName(pos);
		if (la == null) {
			return null;
		}
		int e = la.end;
		if (e >= end) {
			return new EQNameToken(la.start, la.end, null, null, la.string());
		}
		char c = input[e++];
		if (c != ':') {
			return new EQNameToken(la.start, la.end, null, null, la.string());
		}
		Token la2 = laNCName(e);
		if (la2 == null) {
			// TODO illegal? throw exception?
			return new EQNameToken(la.start, la.end, null, null, la.string());
		}
		e = la2.end;
		return new EQNameToken(pos, e, null, la.string(), la2.string());
	}

	protected EQNameToken laEQName(boolean cond) throws TokenizerException {
		return laEQName(pos, cond);
	}

	protected EQNameToken laEQNameSkipWS(Token token, boolean cond)
			throws TokenizerException {
		return laEQName(token.end + ws(token.end), cond);
	}

	protected EQNameToken laEQNameSkipWS(boolean cond)
			throws TokenizerException {
		return laEQName(pos + ws(pos), cond);
	}

	protected EQNameToken laEQName(Token token, boolean cond)
			throws TokenizerException {
		return laEQName(token.end, cond);
	}

	private EQNameToken laEQName(int pos, boolean cond)
			throws TokenizerException {
		EQNameToken la = laQName(pos);
		if (la != null) {
			return la;
		}
		Token uri = laString(pos, cond);
		if (uri == null) {
			return null;
		}
		Token colon = la(uri, ":");
		if (colon == null) {
			return null;
		}
		Token ncname = laNCName(colon);
		if (ncname == null) {
			// TODO illegal? throw exception?
			return la;
		}
		return new EQNameToken(pos, ncname.end, uri.string(), null,
				ncname.string());
	}

	protected Token laNCName() {
		return laNCName(pos);
	}

	protected Token laNCNameSkipWS() {
		return laNCName(pos + ws(pos));
	}
	
	protected Token laNCNameSkipS() {
		return laNCName(pos + s(pos));
	}

	protected Token laNCName(Token token) {
		return laNCName(token.end);
	}

	protected Token laNCNameSkipWS(Token token) {
		return laNCName(token.end + ws(token.end));
	}

	private Token laNCName(int pos) {
		int e = pos;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c = input[e++];
		if ((c == ':') || (!XMLChar.isNameStartChar(c))) {
			return null;
		}
		len++;
		while (e < end) {
			c = input[e++];
			if ((c == ':') || (!XMLChar.isNameChar(c))) {
				break;
			}
			len++;
		}
		if ((len == 0) || ((e != end) && (!isDelChar(input[e - 1])))) {
			return null;
		}
		return new Token(pos, pos + len);
	}

	protected Token laDouble(boolean cond) throws TokenizerException {
		return laDouble(pos, cond);
	}

	protected Token laDoubleSkipWS(boolean cond) throws TokenizerException {
		return laDouble(pos + ws(pos), cond);
	}

	protected Token laDouble(Token token, boolean cond)
			throws TokenizerException {
		return laDouble(token.end, cond);
	}

	private Token laDouble(int pos, boolean cond) throws TokenizerException {
		int e = pos;
		int len = 0;
		char c = 0;
		if (e >= end) {
			return null;
		}
		while (e < end) {
			c = input[e++];
			if ((c >= '0') && (c <= '9')) {
				len++;
			} else {
				break;
			}
		}
		if (c == '.') {
			len++;
			if (len == 1) {
				// at least one digit must follow the period
				if ((e >= end) || ((c = input[e++]) < '0') || (c > '9')) {
					if (cond) {
						return null;
					}
					throw new TokenizerException(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase());
				}
				len++;
			}
			// remaining digits after period are optional
			while (e < end) {
				c = input[e++];
				if ((c >= '0') && (c <= '9')) {
					len++;
				} else {
					break;
				}
			}
		}
		if ((len > 0) && ((c == 'e') || (c == 'E'))) {
			len++;
			if (e >= end) {
				if (cond) {
					return null;
				}
				throw new TokenizerException(
						"Invalid numerical literal '%s': %s", new String(input,
								pos, e - pos), paraphrase());
			}
			c = input[e++];
			if ((c == '-') || (c == '+')) {
				len++;
				if (e >= end) {
					if (cond) {
						return null;
					}
					throw new TokenizerException(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase());
				}
				c = input[e++];
			}
			if ((c < '0') || (c > '9')) {
				if (cond) {
					return null;
				}
				throw new TokenizerException(
						"Invalid numerical literal '%s': %s", new String(input,
								pos, e - pos), paraphrase());
			}
			len++;
			// remaining digits after are optional
			while (e < end) {
				c = input[e++];
				if ((c >= '0') && (c <= '9')) {
					len++;
				} else {
					break;
				}
			}
		}
		if ((len == 0) || ((e != end) && (!isDelChar(input[e - 1])))) {
			return null;
		}
		return new Token(pos, pos + len);
	}

	protected Token laDecimal(boolean cond) throws TokenizerException {
		return laDecimal(pos, cond);
	}

	protected Token laDecimalSkipWS(boolean cond) throws TokenizerException {
		return laDecimal(pos + ws(pos), cond);
	}

	protected Token laDecimal(Token token, boolean cond)
			throws TokenizerException {
		return laDecimal(token.end, cond);
	}

	private Token laDecimal(int pos, boolean cond) throws TokenizerException {
		int e = pos;
		int len = 0;
		char c = 0;
		if (e >= end) {
			return null;
		}
		while (e < end) {
			c = input[e++];
			if ((c >= '0') && (c <= '9')) {
				len++;
			} else if (c == 'E') {
				// found exponent
				// should be parsed as double
				return null;
			} else {
				break;
			}
		}
		if (c == '.') {
			len++;
			if (len == 1) {
				// at least one digit must follow the period
				if ((e >= end) || ((c = input[e++]) < '0') || (c > '9')) {
					if (cond) {
						return null;
					}
					throw new TokenizerException(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase());
				}
				len++;
			}
			// remaining digits after period are optional
			while (e < end) {
				c = input[e++];
				if ((c >= '0') && (c <= '9')) {
					len++;
				} else {
					break;
				}
			}
		}
		if ((len == 0) || ((e != end) && (!isDelChar(input[e - 1])))) {
			return null;
		}
		return new Token(pos, pos + len);
	}

	protected Token laInteger(boolean cond) {
		return laInteger(pos, cond);
	}

	protected Token laIntegerSkipWS(boolean cond) {
		return laInteger(pos + ws(pos), cond);
	}

	protected Token laInteger(Token token, boolean cond) {
		return laInteger(token.end, cond);
	}

	private Token laInteger(int pos, boolean cond) {
		int e = pos;
		int len = 0;
		char c = 0;
		if (e >= end) {
			return null;
		}
		while (e < end) {
			c = input[e++];
			if ((c >= '0') && (c <= '9')) {
				len++;
			} else if (c == '.') {
				// found decimal point
				// should be parsed as floating point number
				return null;
			} else {
				break;
			}
		}
		if ((len == 0) || ((e != end) && (!isDelChar(input[e - 1])))) {
			return null;
		}
		return new Token(pos, pos + len);
	}

	protected Token laString(boolean cond) throws TokenizerException {
		return laString(pos, cond);
	}

	protected Token laStringSkipWS(boolean cond) throws TokenizerException {
		return laString(pos + ws(pos), cond);
	}

	protected Token laString(Token token, boolean cond)
			throws TokenizerException {
		return laString(token.end, cond);
	}

	private Token laString(int pos, boolean cond) throws TokenizerException {
		Token begin = la(pos, "'");
		if (begin != null) {
			String s = scanAposStringLiteral(begin.end, cond);
			Token end = la(lastScanEnd, "'");
			if (end != null) {
				return new StringToken(pos, end.end, s);
			} else {
//				if (cond) {
//					return null;
//				}
				throw new TokenizerException("Unclosed string literal: %s",
						paraphrase());
			}
		} else {
			begin = la(pos, "\"");
			if (begin != null) {
				String s = scanQuotStringLiteral(begin.end, cond);
				Token end = la(lastScanEnd, "\"");
				if (end != null) {
					return new StringToken(pos, end.end, s);
				} else {
//					if (cond) {
//						return null;
//					}
					throw new TokenizerException("Unclosed string literal: %s",
							paraphrase());
				}
			}
		}
		return null;
	}

	protected Token laPragma(boolean cond) throws TokenizerException {
		return laPragma(pos, cond);
	}

	private Token laPragma(int pos, boolean cond) throws TokenizerException {
		int s = pos;
		int e = pos;
		if (e >= end) {
			return null;
		}
		int len = 0;
		while (e < end) {
			int c = input[e++];
			if ((c == '#') && (e < end) && (input[e] == ')')) {
				return new Token(s, s + len);
			} else if (!XMLChar.isChar(c)) {
				if (cond) {
					return null;
				}
				throw new TokenizerException("Invalid pragma content '%s': %s",
						new String(input, pos, e - pos), paraphrase());
			}
			len++;
		}
		if (cond) {
			return null;
		}
		throw new TokenizerException(
				"Unclosed pragma content at position %s: '%s'", pos,
				paraphrase());
	}

	protected Token laQuotAttrContentChar() {
		int s = pos;
		String content = scanAttrContentChar(s, '"', "\"\"", "\"");
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laAposAttrContentChar() {
		int s = pos;
		String content = scanAttrContentChar(s, '\'', "''", "'");
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laPredefEntityRef(boolean cond) throws TokenizerException {
		int s = pos;
		String content = scanPredefEntityRef(s, cond);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laCharRef(boolean cond) throws TokenizerException {
		int s = pos;
		String content = scanCharRef(s, cond);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laEscapeQuot() {
		int s = pos;
		String content = scanEscape(s, '"', "\"");
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laEscapeApos() {
		int s = pos;
		String content = scanEscape(s, '\'', "'");
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laEscapeCurly() {
		int s = pos;
		String content = scanEscape(s, '{', "{");
		if (content == null) {
			content = scanEscape(s, '}', "}");
			if (content == null) {
				return null;
			}
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laCommentContents(boolean cond) throws TokenizerException {
		int s = pos;
		String content = scanCommentContents(s, cond);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laPITarget(boolean cond) throws TokenizerException {
		int s = pos;
		String content = scanPITarget(s, cond);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laPIContents() {
		int s = pos;
		String content = scanPIContents(s);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laCDataSectionContents() throws TokenizerException {
		int s = pos;
		String content = scanCDataSectionContents(s);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	protected Token laElemContentChar() {
		int s = pos;
		String content = scanElemContentChar(s);
		if (content == null) {
			return null;
		}
		return new StringToken(s, lastScanEnd, content);
	}

	private String scanAttrContentChar(int pos, char escapeChar,
			String escapeStr, String replace) {
		int s = pos;
		int e = pos;
		if (e >= end) {
			return null;
		}
		boolean hasEscapes = false;
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if ((c == escapeChar) && (e < end) && (input[e] == escapeChar)) {
				hasEscapes = true;
				e++;
				len += 2;
			} else if ((c == escapeChar) || (c == '{') || (c == '}')
					|| (c == '<') || (c == '&') || (!XMLChar.isChar(c))) {
				break;
			} else {
				len++;
			}
		}
		if (len == 0) {
			return null;
		}
		lastScanEnd = pos + len;
		String content = new String(input, s, len);
		if (hasEscapes) {
			// TODO this can be easily improved to avoid
			// the pattern matching
			content = content.replace(escapeStr, replace);
		}
		return content;
	}

	private String scanElemContentChar(int pos) {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if ((c == '{') || (c == '}') || (c == '<') || (c == '&')
					|| (!XMLChar.isChar(c))) {
				break;
			} else {
				len++;
			}
		}
		if (len == 0) {
			return null;
		}
		lastScanEnd = pos + len;
		return new String(input, s, len);
	}

	private String scanCDataSectionContents(int pos) throws TokenizerException {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if (c == ']') {
				if (end - e <= 1) {
					return null;
				}
				if ((input[e] == ']') && (input[e + 1] == '>')) {
					break;
				}
			}
			if (!XMLChar.isChar(c)) {
				throw new TokenizerException(
						"Illegal character in CDATA section: '%s': %s", c,
						paraphrase());
			} else {
				len++;
			}
		}
		lastScanEnd = pos + len;
		return new String(input, s, len);
	}

	private String scanPredefEntityRef(int pos, boolean cond)
			throws TokenizerException {
		int e = pos;
		if ((input[e++] != '&') || (input[e] == '#')) {
			return null;
		}
		if (end - e <= 3) {
			throw new TokenizerException("Illegal PredefinedEntityRef '%s': %s",
					new String(input, pos, Math.min(4, end - pos)), paraphrase());
		}
		if ((input[e] == 'l') && (input[e + 1] == 't') && (input[e + 2] == ';')) {
			lastScanEnd = pos + 4;
			return "<";
		}
		if ((input[e] == 'g') && (input[e + 1] == 't') && (input[e + 2] == ';')) {
			lastScanEnd = pos + 4;
			return ">";
		}
		if (end - e >= 4) {
			if ((input[e] == 'a') && (input[e + 1] == 'm')
					&& (input[e + 2] == 'p') && (input[e + 3] == ';')) {
				lastScanEnd = pos + 5;
				return "&";
			}
		}
		if (end - e >= 5) {
			if ((input[e] == 'a') && (input[e + 1] == 'p')
					&& (input[e + 2] == 'o') && (input[e + 3] == 's')
					&& (input[e + 4] == ';')) {
				lastScanEnd = pos + 6;
				return "'";
			}
			if ((input[e] == 'q') && (input[e + 1] == 'u')
					&& (input[e + 2] == 'o') && (input[e + 3] == 't')
					&& (input[e + 4] == ';')) {
				lastScanEnd = pos + 6;
				return "\"";
			}
		}
//		if (cond) {
//			return null;
//		}
		throw new TokenizerException("Illegal PredefinedEntityRef '%s': %s",
				new String(input, pos, 6), paraphrase());
	}

	private String scanCharRef(int pos, boolean cond) throws TokenizerException {
		int e = pos;
		if ((input[e++] != '&') || (input[e++] != '#')) {
			return null;
		}
		if (end - e <= 3) {			
			String charRef = new String(input, pos, Math.min(4, end - pos));
			throw new TokenizerException("Illegal Unicode character reference '%s' %s: '%s'", charRef, paraphrase());
		}
		int len = 0;
		int s = e;
		char c;
		int radix = 10;
		if (input[e] == 'x') { // hex
			radix = 16;
			s++;
			e++; // consume 'x'
			while (e < end) {
				c = input[e++];
				if (!((('0' <= c) && (c <= '9')) || (('a' <= c) && (c <= 'z')) || (('A' <= c) && (c <= 'Z')))) {
					if ((c != ';') || (len == 0)) {
						return null;
					}
					break;
				}
				len++;
			}
		} else {
			while (e < end) {
				c = input[e++];
				if (!((('0' <= c) && (c <= '9')))) {
					if ((c != ';') || (len == 0)) {
						return null;
					}
					break;
				}
				len++;
			}
		}
		String tmp = new String(input, s, len);
		BigInteger charRef;
		try {
			charRef = new BigInteger(tmp, radix);
		} catch (NumberFormatException e1) {
//			if (cond) {
//				return null;
//			}
			throw new TokenizerException("Illegal Unicode character reference '%s' %s: '%s'", tmp, paraphrase());
		}
		if ((charRef.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) || (!XMLChar.isChar(charRef.intValue()))) {
//			if (cond) {
//				return null;
//			}
			throw new IllegalCharRefException(tmp);
		}
		lastScanEnd = s + len + 1;
		return XMLChar.toString(charRef.intValue());
	}

	private String scanString(int pos, char escapeChar) {
		int e = pos;
		int s = e;
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if ((c == escapeChar) || (c == '&')) {
				break;
			} else {
				len++;
			}
		}
		if (len == 0) {
			return null;
		}
		lastScanEnd = pos + len;
		return new String(input, s, len);
	}

	private String scanAposStringLiteral(int pos, boolean cond)
			throws TokenizerException {
		lastScanEnd = pos;
		StringBuilder buf = new StringBuilder();
		int spos = pos;
		while (true) {
			String s = scanPredefEntityRef(spos, cond);
			s = (s != null) ? s : scanCharRef(spos, cond);
			s = (s != null) ? s : scanEscape(spos, '\'', "'");
			s = (s != null) ? s : scanString(spos, '\'');
			if (s != null) {
				buf.append(s);
				spos = lastScanEnd;
			} else {
				break;
			}
		}
		return buf.toString();
	}

	private String scanQuotStringLiteral(int pos, boolean cond)
			throws TokenizerException {
		lastScanEnd = pos;
		StringBuilder buf = new StringBuilder();
		int spos = pos;
		while (true) {
			String s = scanPredefEntityRef(spos, cond);
			s = (s != null) ? s : scanCharRef(spos, cond);
			s = (s != null) ? s : scanEscape(spos, '"', "\"");
			s = (s != null) ? s : scanString(spos, '"');
			if (s != null) {
				buf.append(s);
				spos = lastScanEnd;
			} else {
				break;
			}
		}
		return buf.toString();
	}

	private String scanEscape(int pos, char escapeChar, String escapeString) {
		int s = pos;
		int e = s;
		if ((end - e < 2)
				|| ((input[e++] != escapeChar) || (input[e++] != escapeChar))) {
			return null;
		}
		lastScanEnd = e;
		return escapeString;
	}

	private String scanCommentContents(int pos, boolean cond)
			throws TokenizerException {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if ((c == '-') && (e < end) && (input[e] == '-')) {
				if ((e + 1 < end) && (input[e + 1] == '>')) {
					break;
				}
				if (cond) {
					return null;
				}
				throw new TokenizerException("Illegal '--' in XML comment: %s",
						paraphrase());
			}
			if (!XMLChar.isChar(c)) {
				break;
			} else {
				len++;
			}
		}
		lastScanEnd = pos + len;
		return new String(input, s, len);
	}

	private String scanPITarget(int pos, boolean cond)
			throws TokenizerException {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c = input[e++];
		if (!XMLChar.isNameStartChar(c)) {
			return null;
		}
		len++;
		while (e < end) {
			c = input[e++];
			if (!XMLChar.isNameChar(c)) {
				break;
			}
			len++;
		}
		if (len == 0) {
			return null;
		}
		String target = new String(input, s, len);
		if ((target.length() == 3) && (target.toLowerCase().equals("xml"))) {
			if (cond) {
				return null;
			}
			throw new TokenizerException("PITarget must not be '%s': %s",
					target, paraphrase());
		}
		lastScanEnd = pos + len;
		return target;
	}

	private String scanPIContents(int pos) {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c;
		while (e < end) {
			c = input[e++];
			if ((c == '?') && (e < end) && (input[e] == '>')) {
				break;
			}
			if (!XMLChar.isChar(c)) {
				break;
			} else {
				len++;
			}
		}
		lastScanEnd = pos + len;
		return new String(input, s, len);
	}
}
