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
package org.brackit.xquery.function.fn;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.function.Signature;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;

/**
 * Implementation of predefined functions fn:matches($arg1, $arg2),
 * fn:matches($arg1, $arg2, $arg3), fn:replace($arg1, $arg2, $arg3),
 * fn:replace($arg1, $arg2, $arg3, $arg4), fn:tokenize($arg1, $arg2), and
 * fn:tokenize($arg1, $arg2, $arg3) as per
 * http://www.w3.org/TR/xpath-functions/#func-matches,
 * http://www.w3.org/TR/xpath-functions/#func-replace, and
 * http://www.w3.org/TR/xpath-functions/#func-tokenize. Also note corrections in
 * http://www.w3.org/XML/2007/qt-errata/xpath-functions-errata.html.
 * 
 * @author Max Bechtold
 * 
 */
public class RegEx extends AbstractFunction {
	private final static List<Character> WHITESPACE = Arrays.asList(Character
			.toChars(0x09)[0], Character.toChars(0x0A)[0], Character
			.toChars(0x0D)[0], Character.toChars(0x20)[0]);

	public static enum Mode {
		MATCH, REPLACE, TOKENIZE
	};

	private Mode mode;

	public RegEx(QNm name, Mode mode, Signature signature) {
		super(name, signature, true);
		this.mode = mode;
	}

	@Override
	public Sequence execute(QueryContext ctx, Sequence[] args)
			throws QueryException {
		boolean removeWhitespace = false;
		int flagMask = Pattern.UNIX_LINES;

		if (mode != Mode.REPLACE && args.length > 2 || args.length > 3) {
			String flags = ((Str) args[(mode == Mode.REPLACE ? 3 : 2)]).str;

			if (flags.contains("x")) {
				removeWhitespace = true;
				flags = flags.replace("x", "");
			}

			if (flags.contains("s")) {
				flagMask |= Pattern.DOTALL;
				flags = flags.replace("s", "");
			}

			if (flags.contains("m")) {
				flagMask |= Pattern.MULTILINE;
				flags = flags.replace("m", "");
			}

			if (flags.contains("i")) {
				flagMask |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
				flags = flags.replace("i", "");
			}

			if (!flags.isEmpty()) {
				throw new QueryException(
						ErrorCode.ERR_INVALID_REGULAR_EXPRESSION_FLAGS,
						"Unknown flags specified.");
			}
		}

		String input = (args[0] != null ? ((Str) args[0]).str : "");
		Str p = ((Str) args[1]);

		if (mode != Mode.MATCH && Pattern.matches(p.str, "")) {
			throw (new QueryException(
					ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING,
					"Pattern matches empty string."));
		}

		if (mode == Mode.TOKENIZE && input.isEmpty()) {
			return null;
		}

		Pattern pattern;
		try {
			pattern = Pattern.compile(
					adaptRegEx(p, flagMask, removeWhitespace), flagMask);
		} catch (PatternSyntaxException e) {
			throw (new QueryException(e,
					ErrorCode.ERR_INVALID_REGULAR_EXPRESSION));
		}
		Matcher matcher = pattern.matcher(input);

		switch (mode) {
		case MATCH:
			return new Bool(matcher.matches());

		case REPLACE:
			String replace = ((Str) args[2]).str;

			// Disallowed in replacement string: backslash or dollar sign as
			// only character in string, or dollar sign not preceded by 
			// backslash and not followed by a digit, or backslash not 
			// preceded by backslash and not followed by a dollar sign
			String pat = "(\\$|\\\\|.*[^\\\\]\\$\\D.*|.*[^\\\\]\\\\[^\\$].*)";
			if (Pattern.matches(pat, replace)) {
				throw (new QueryException(
						ErrorCode.ERR_INVALID_REPLACEMENT_STRING,
						"Replacement string matches makes illegal " +
						"use of chars '\\' or '$'."));
			}

			StringBuffer sb = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(sb, replace);
			}
			matcher.appendTail(sb);

			return new Str(sb.toString());

		case TOKENIZE:
			String[] tokens = pattern.split(input, -1);
			Str[] items = new Str[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				items[i] = new Str(tokens[i]);
			}
			return new ItemSequence(items);
		default:
			return null;
		}
	}

	/**
	 * Method for adapting Java regex functions to XQuery functions
	 * requirements. This includes pattern matching using substrings (Java
	 * patterns are implicitly anchored at begin/end of string), error raising
	 * for invalid back references (which Java silently skips), a more sensitive
	 * whitespace removal (unlike Java keep whitespace in character classes),
	 * and no support for pure, i.e. uncapturing groups. Additionally, this
	 * method checks for dangling round and square brackets (which are not
	 * allowed in XQuery, but treated as literals in Java).</br> This method
	 * optionally removes all whitespace except for whitespace in character
	 * classes, see flag 'x' in http://www.w3.org/TR/xpath-functions/#flags
	 */
	private String adaptRegEx(Str regex, int flagMask, boolean removeWhitespace)
			throws QueryException {
		StringBuilder sb = new StringBuilder();
		boolean escaped = false;
		boolean groupStart = false;
		int completeGroups = 0;
		int backRef = 0;
		int charClassDepth = 0;
		int groupDepth = 0;

		for (char c : regex.str.toCharArray()) {
			if (escaped) {
				if (backRef == 0 && c == '0') {
					throw new QueryException(
							ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
							"Reference to group 0 not allowed");
				} else if (c >= '0' && c <='9') {
					if (charClassDepth > 0) {
						throw new QueryException(
								ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
								"Back references in character class expressions" +
								" are disallowed.");
					}
					backRef = backRef * 10 
								+ Integer.parseInt(Character.toString(c));
					continue;
				}
			}

			if (backRef > 0) {
				// Check back reference that just ended
				if (backRef > completeGroups) {
					throw new QueryException(
							ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
							"Back reference to nonexisting or unfinished group.");
				} else {
					backRef = 0;
					escaped = false;
				}
			}

			if (c == '\\' && !escaped) {
				// Not preceded by backslash
				escaped = true;
				groupStart = false;
				continue;
			}

			if (c == '(' && !escaped) {
				groupStart = true;
				groupDepth++;
				escaped = false;
				continue;
			}

			if (c == '?' && !escaped && groupStart) {
				throw new QueryException(
						ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
						"Pure groups are not supported in XQuery regular expressions.");
			} else if (c == ')' && !escaped) {
				if (--groupDepth < 0) {
					throw new QueryException(
							ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
							"Invalid sequence of brackets.");
				}
				completeGroups++;
			} else if (c == '[' && !escaped) {
				charClassDepth++;
			} else if (c == ']' && !escaped) {
				if (--charClassDepth < 0) {
					throw new QueryException(
							ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
							"Invalid sequence of brackets.");
				}
			} else if (removeWhitespace) {
				// Remove whitespace outside of character classes
				if (charClassDepth == 0 && WHITESPACE.contains(c)) {
					// Don't touch boolean flags
					continue;
				}

				sb.append(c);
			}

			groupStart = false;
			escaped = false;
		}
		
		// Check for trailing '\' (only valid with subsequent characters)
		if (escaped && backRef == 0) {
			throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
					"Trailing backslash character in pattern.");
		}

		// Check back reference if that was last token in pattern
		if (backRef > 0 && backRef > completeGroups) {
			throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
					"Back reference to nonexisting or unfinished group.");
		}

		// Check for dangling brackets
		if (charClassDepth != 0 || groupDepth != 0) {
			throw new QueryException(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION,
					"Pattern contains dangling brackets.");
		}

		if (!removeWhitespace) {
			sb.append(regex.str);
		}

		if (mode == Mode.MATCH) {
			// Adapt for XQuery substring matching by extending pattern
			if (sb.charAt(0) != '^'
					|| ((flagMask & Pattern.MULTILINE) == Pattern.MULTILINE)) {
				if ((flagMask & Pattern.DOTALL) == Pattern.DOTALL) {
					sb.insert(0, ".*");
				} else {
					sb.insert(0, "(?s:.*)");
				}
			}

			if (sb.charAt(sb.length() - 1) != '$'
					|| ((flagMask & Pattern.MULTILINE) == Pattern.MULTILINE)) {
				if ((flagMask & Pattern.DOTALL) == Pattern.DOTALL) {
					sb.append(".*");
				} else {
					sb.append("(?s:.*)");
				}
			}
		}

		return sb.toString();
	}

}
