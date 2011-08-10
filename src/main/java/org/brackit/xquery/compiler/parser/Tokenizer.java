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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.XMLChar;

/**
 * 
 * @author Sebastian Baechle
 *
 */
public class Tokenizer {

	private int pos;
	private final int end;
	private final char[] input;

	protected class Token {
		final int start;
		final int end;

		public Token(int start, int end) {
			this.start = start;
			this.end = end;
		}

		String string() {
			return new String(input, start, end - start);
		}

		public String toString() {
			return string();
		}
	}

	public Tokenizer(String s) {
		this.input = s.toCharArray();
		this.end = input.length;
	}
	
	protected Token la(String token) {
		return la(pos, token);
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

	protected boolean attemptSkipWS(String token) {
		Token la = laSkipWS(pos, token);
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

	protected void consumeSkipWS(String token) throws QueryException {
		Token la = laSkipWS(pos, token);
		if (la == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected '%s': '%s'", token, paraphrase());
		}
		consume(la);
	}

	protected void consume(Token token) {
		System.out.println("Consuming " + token + " (to [" + token.start + ":"
				+ token.end + "]/" + end + ")");
		pos = token.end;
	}

	protected void consume(String token) throws QueryException {
		Token la = la(pos, token);
		if (la == null) {
			throw new QueryException(ErrorCode.ERR_PARSING_ERROR,
					"Expected '%s' after: '%s'", token, paraphrase());
		}
		consumeSkipWS(token);
	}

	protected String paraphrase() {
		// TODO Auto-generated method stub
		return null;
	}

	protected Token laWS() {
		int ws = ws(pos);
		return (ws > 0) ? new Token(pos, ws) : null;
	}
	
	protected Token laS(Token token) {
		return laS(token.end);
	}
	
	protected Token laS() {
		return laS(pos);
	}
	
	private Token laS(int pos) {
		int s = s(pos);
		return (s > 0) ? new Token(pos, s) : null;
	}

	protected boolean skipS() {
		int s = s(pos);
		if (s <= 0) {
			return false;
		}
		System.out.println("Skipping whitespace from " + pos + " to "
				+ (pos + s));
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
				p = comment(p);
				continue;
			}
			break;
		}
		return p - from;
	}

	private int comment(int from) {
		throw new RuntimeException();
	}

	protected Token laEQName(boolean cond) throws Exception {
		return laEQName(pos, cond);
	}

	protected Token laEQNameSkipWS(boolean cond) throws Exception {
		return laEQName(pos + ws(pos), cond);
	}

	protected Token laEQName(Token token, boolean cond) throws Exception {
		return laEQName(token.end, cond);
	}
	
	protected Token laEQNameSkipWS(Token token, boolean cond) throws Exception {
		return laEQName(token.end + ws(token.end), cond);
	}

	private Token laEQName(int pos, boolean cond) throws Exception {
		Token la = laQName(pos, cond);
		if (la != null) {
			return la;
		}
		la = laString(pos, cond);
		if (la == null) {
			return null;
		}
		int e = la.end;
		Token la2;
		if ((e >= end) || ((input[e++]) != ':')
				|| ((la2 = laNCName(e, cond)) == null)) {
			if (cond) {
				return null;
			}
			throw new Exception(String.format("Illegal EQName '%s': %s",
					new String(input, pos, e - pos), paraphrase()));
		}
		e = la2.end;
		return new Token(pos, e);
	}

	protected Token laQName(boolean cond) throws Exception {
		return laQName(pos, cond);
	}
	
	protected Token laQNameSkipWS(Token token, boolean cond) throws Exception {
		return laQName(token.end + ws(token.end), cond);
	}

	protected Token laQNameSkipWS(boolean cond) throws Exception {
		return laQName(pos + ws(pos), cond);
	}

	protected Token laQName(Token token, boolean cond) throws Exception {
		return laQName(token.end, cond);
	}

	private Token laQName(int pos, boolean cond) throws Exception {
		Token la = laNCName(pos, cond);
		if (la == null) {
			return null;
		}
		int e = la.end;
		if (e >= end) {
			return la;
		}
		char c = input[e++];
		if (c != ':') {
			return la;
		}
		e++;
		Token la2 = laNCName(e, cond);
		if (la2 == null) {
			if (cond) {
				return null;
			}
			throw new Exception(String.format("Illegal QName '%s': %s",
					new String(input, pos, e - pos), paraphrase()));
		}
		e = la2.end;
		return new Token(pos, e);
	}

	protected Token laNCName(boolean cond) {
		return laNCName(pos, cond);
	}

	protected Token laNCNameSkipWS(boolean cond) {
		return laNCName(pos + ws(pos), cond);
	}

	protected Token laNCName(Token token, boolean cond) {
		return laNCName(token.end, cond);
	}

	private Token laNCName(int pos, boolean cond) {
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
		if (len == 0) {
			return null;
		}
		return new Token(pos, pos + len);
	}

	protected Token laString(boolean cond) throws Exception {
		return laString(pos, cond);
	}

	protected Token laStringSkipWS(boolean cond) throws Exception {
		return laString(pos + ws(pos), cond);
	}

	protected Token laString(Token token, boolean cond) throws Exception {
		return laString(token.end, cond);
	}

	private Token laString(int pos, boolean cond) throws Exception {
		int e = pos;
		if (e >= end) {
			return null;
		}
		char begin = input[e++];
		if ((begin != '"') && (begin != '\'')) {
			return null;
		}
		int len = 0;
		while (e < end) {
			char c = input[e++];
			if (c == begin) {
				pos = e;
				e += len;
				return new Token(pos, e);
			}
			// TODO PredefinedEntityRef, CharRef etc.
			len++;
		}
		if (cond) {
			return null;
		}
		throw new Exception(String.format(
				"Unclosed string literal at position %s: '%s'", position(),
				paraphrase()));
	}

	protected Token laDouble(boolean cond) throws Exception {
		return laDouble(pos, cond);
	}

	protected Token laDoubleSkipWS(boolean cond) throws Exception {
		return laDouble(pos + ws(pos), cond);
	}

	protected Token laDouble(Token token, boolean cond) throws Exception {
		return laDouble(token.end, cond);
	}

	private Token laDouble(int pos, boolean cond) throws Exception {
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
					throw new Exception(String.format(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase()));
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
				throw new Exception(String.format(
						"Invalid numerical literal '%s': %s", new String(input,
								pos, e - pos), paraphrase()));
			}
			c = input[e++];
			if ((c == '-') || (c == '+')) {
				len++;
				if (e >= end) {
					if (cond) {
						return null;
					}
					throw new Exception(String.format(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase()));
				}
				c = input[e++];
			}
			if ((c < '0') || (c > '9')) {
				if (cond) {
					return null;
				}
				throw new Exception(String.format(
						"Invalid numerical literal '%s': %s", new String(input,
								pos, e - pos), paraphrase()));
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
		return (len == 0) ? null : new Token(pos, pos + len);
	}

	protected Token laDecimal(boolean cond) throws Exception {
		return laDecimal(pos, cond);
	}

	protected Token laDecimalSkipWS(boolean cond) throws Exception {
		return laDecimal(pos + ws(pos), cond);
	}

	protected Token laDecimal(Token token, boolean cond) throws Exception {
		return laDecimal(token.end, cond);
	}

	private Token laDecimal(int pos, boolean cond) throws Exception {
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
					throw new Exception(String.format(
							"Invalid numerical literal '%s': %s", new String(
									input, pos, e - pos), paraphrase()));
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
		return (len == 0) ? null : new Token(pos, pos + len);
	}

	protected Token laInteger(boolean cond) throws Exception {
		return laInteger(pos, cond);
	}

	protected Token laIntegerSkipWS(boolean cond) throws Exception {
		return laInteger(pos + ws(pos), cond);
	}

	protected Token laInteger(Token token, boolean cond) throws Exception {
		return laInteger(token.end, cond);
	}

	private Token laInteger(int pos, boolean cond) throws Exception {
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
		return (len == 0) ? null : new Token(pos, pos + len);
	}

	protected Token laPragma(boolean cond) throws Exception {
		return laPragma(pos, cond);
	}

	private Token laPragma(int pos, boolean cond) throws Exception {
		int e = pos;
		if (e >= end) {
			return null;
		}
		int len = 0;
		while (e < end) {
			char c = input[e++];
			if (c == '#') {
				pos = e;
				e += len;
				return new Token(pos, e);
			} else if (!XMLChar.isChar(c)) {
				if (cond) {
					return null;
				}
				throw new Exception(String.format(
						"Invalid pragma content '%s': %s", new String(input,
								pos, e - pos), paraphrase()));
			}
			len++;
		}
		if (cond) {
			return null;
		}
		throw new Exception(String.format(
				"Unclosed pragma content at position %s: '%s'", position(),
				paraphrase()));
	}

	private String position() {
		return "" + pos;
	}

	protected String consumeQuotAttrContent() {
		return consumeEscapedAttrValue('"');
	}

	protected String consumeAposAttrContent() {
		return consumeEscapedAttrValue('\'');
	}

	private String consumeEscapedAttrValue(char escapeChar) {
		int e = pos;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c = input[e++];
		while (e < end) {
			c = input[e++];
			if ((c == escapeChar) || (c == '{') || (c == '}') || (c == '<')
					|| (c == '&') || (!XMLChar.isChar(c))) {
				break;
			} else {
				len++;
			}
		}
		pos += len;
		return new String(input, e, len);
	}

	protected String consumeElemContentChar() {
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
		pos += len;
		return new String(input, s, len);
	}

	protected String attemptCDataSectionContents() {
		int s = pos;
		int e = s;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c = input[e++];
		while (e < end) {
			c = input[e++];
			if ((c == ']')
					&& ((end - e <= 2) || (input[e + 1] != ']') || (input[e + 2] != '>'))) {
				break;
			}
			if (!XMLChar.isChar(c)) {
				break;
			} else {
				len++;
			}
		}
		pos += len;
		return new String(input, s, len);
	}

	protected String consumePredefEntityRef() throws Exception {
		int e = pos;
		if ((end - e <= 4) || (input[e++] != '&')) {
			return null;
		}
		if ((input[e] == 'l') && (input[e + 1] == 't') && (input[e + 2] == ';')) {
			pos += 4;
			return "<";
		}
		if ((input[e] == 'g') && (input[e + 1] == 't') && (input[e + 2] == ';')) {
			pos += 4;
			return ">";
		}
		if (end - e >= 4) {
			if ((input[e] == 'a') && (input[e + 1] == 'l')
					&& (input[e + 2] == 't') && (input[e + 3] == ';')) {
				pos += 5;
				return "'";
			}
		}
		if (end - e >= 5) {
			if ((input[e] == 'a') && (input[e + 1] == 'p')
					&& (input[e + 2] == 'o') && (input[e + 3] == 's')
					&& (input[e + 4] == ';')) {
				pos += 6;
				return "'";
			}
			if ((input[e] == 'q') && (input[e + 1] == 'u')
					&& (input[e + 2] == 'o') && (input[e + 3] == 't')
					&& (input[e + 4] == ';')) {
				pos += 6;
				return "\"";
			}
		}
		throw new Exception(String.format(
				"Illegal PredefinedEntityRef '%s': %s", new String(input, pos,
						6), paraphrase()));
	}

	protected String consumeCharRef() throws Exception {
		int e = pos;
		if ((end - e <= 4) || (input[e++] != '&') || (input[e++] != '#')) {
			return null;
		}
		int len = 0;
		int s = e;
		char c;
		if (input[e] == 'x') { // hex
			e++; // consume 'x'
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
		} else {
			while (e < end) {
				c = input[e++];
				if (!((('0' <= c) && (c <= '9')) || (('a' <= c) && (c <= 'z')) || (('A' <= c) && (c <= 'Z')))) {
					if ((c != ';') || (len == 0)) {
						return null;
					}
					break;
				}
			}
		}
		String tmp = new String(input, s, len);
		char charRef;
		try {
			charRef = (char) Integer.parseInt(tmp);
		} catch (NumberFormatException e1) {
			throw new Exception(String.format(
					"Illegal Unicode codepoint '%s': %s", tmp, paraphrase()));
		}
		if (!XMLChar.isChar(charRef)) {
			throw new Exception(String.format(
					"Illegal Unicode codepoint '%s': %s", tmp, paraphrase()));
		}
		pos = s + len;
		return String.valueOf(charRef);
	}

	protected String consumeCommentContents() throws Exception {
		int e = pos;
		if (e >= end) {
			return null;
		}
		int len = 0;
		char c = input[e++];
		while (e < end) {
			c = input[e++];
			if ((c == '-') && (e + 1 < end) && (input[e + 1] == '-')) {
				throw new Exception(String.format(
						"Illegal '--' in XML comment: %s", paraphrase()));
			}
			if (!XMLChar.isChar(c)) {
				break;
			} else {
				len++;
			}
		}
		pos += len;
		return new String(input, e, len);
	}
	
    protected String consumePITarget() throws Exception {
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
                throw new Exception(String.format("PITarget must not be '%s': %s",
                                target, paraphrase()));
        }
        return target;
    }
    
    protected String comsumePIContents() {
        int e = pos;
        if (e >= end) {
                return null;
        }
        int len = 0;
        char c;
        while (e < end) {
                c = input[e++];
                if ((c == '?') && (e + 1 < end) && (input[e + 1] == '>')) {
                        break;
                }
                if (!XMLChar.isChar(c)) {
                        break;
                } else {
                        len++;
                }
        }
        pos += len;
        return new String(input, e, len);
    }   
}                                                                                                                                                                                                                                                                                                     
