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
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.Tree;
import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.AST;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ANTLRParser implements Parser {
	private static class ASTBuilder {
		public final AST walk(Tree node) {
			return walkInternal(node);
		}

		private AST walkInternal(Tree node) {
			AST ast = new AST(node.getType(), node.getText());

			for (int i = 0; i < node.getChildCount(); i++) {
				Tree child = node.getChild(i);
				ast.addChild(walkInternal(child));
			}
			return ast;
		}
	}

	@Override
	public AST parse(String query) throws QueryException {
		ANTLRStringStream source = new ANTLRStringStream(query);
		XQueryLexer lexerXQ = new XQueryLexer(source);
		XMLexer lexerXML = new XMLexer(source);
		XQueryTokenStream tokenStream = new XQueryTokenStream(lexerXML, lexerXQ);

		Tree tree = parse(tokenStream, query);
		AST ast = new ASTBuilder().walk(tree);
		return ast;
	}

	private Tree parse(TokenStream tokenStream, String query)
			throws QueryException {
		XQueryParser parser = null;
		try {
			long start = System.currentTimeMillis();
			parser = new XQueryParser(tokenStream);
			Tree ast = (Tree) parser.module().getTree();
			long end = System.currentTimeMillis();

			if (XQuery.DEBUG) {
				DotUtil.drawToDotFile(ast, XQueryParser.tokenNames,
						XQuery.DEBUG_DIR, "parsed");
			}

			return ast;
		} catch (RuntimeException e) {
			if ((e.getCause() != null)
					&& (e.getCause() instanceof XQueryRecognitionException)) {
				throw new QueryException(e, ((XQueryRecognitionException) e
						.getCause()).getCode(), e.getMessage());
			}
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, e
					.getMessage());
		} catch (XQueryRecognitionException e) {
			throw new QueryException(e, e.getCode(), e.getMessage());
		} catch (RecognitionException e) {
			throw new QueryException(e, ErrorCode.ERR_PARSING_ERROR, parser
					.errorMessage(query, e));
		}
	}
}
