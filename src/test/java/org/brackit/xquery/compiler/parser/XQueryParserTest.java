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

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;
import org.apache.log4j.Logger;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.parser.DotUtil;
import org.brackit.xquery.compiler.parser.XMLexer;
import org.brackit.xquery.compiler.parser.XQueryLexer;
import org.brackit.xquery.compiler.parser.XQueryParser;
import org.brackit.xquery.compiler.parser.XQueryTokenStream;
import org.junit.Test;

public class XQueryParserTest {
	private static final Logger log = Logger.getLogger(XQueryParser.class);

	private static final boolean DEBUG = true;

	@Test
	public void stepBug() throws Exception {
		parse("empty-sequence(./homepage/text())");
	}

	@Test
	public void elementWithEmptyAttribute() throws Exception {
		parse("<a b=''/>");
	}

	private void parse(String query) throws RecognitionException {
		ANTLRStringStream source = new ANTLRStringStream(query);
		XQueryLexer lexerXQ = new XQueryLexer(source);
		XMLexer lexerXML = new XMLexer(source);
		XQueryTokenStream tokenStream = new XQueryTokenStream(lexerXML, lexerXQ);

		XQueryParser parser = new XQueryParser(tokenStream);
		Tree ast = (Tree) parser.mainModule().getTree();

		if (DEBUG) {
			DotUtil.drawToDotFile(ast, XQueryParser.tokenNames,
					XQuery.DEBUG_DIR, "parsed");
		}
	}
}
