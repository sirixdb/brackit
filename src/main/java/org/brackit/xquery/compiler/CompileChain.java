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
package org.brackit.xquery.compiler;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.optimizer.DefaultOptimizer;
import org.brackit.xquery.compiler.optimizer.Optimizer;
import org.brackit.xquery.compiler.parser.DotUtil;
import org.brackit.xquery.compiler.parser.Parser;
import org.brackit.xquery.compiler.parser.XQParser;
import org.brackit.xquery.compiler.translator.PipelineCompiler;
import org.brackit.xquery.compiler.translator.Translator;
import org.brackit.xquery.module.Module;

/**
 * Compiles an {@link Module XQuery module}.
 * 
 * @author Sebastian Baechle
 * 
 */
public class CompileChain {
	
	public CompileChain() {
	}

	protected Parser getParser() {
		return new Parser() {
			@Override
			public AST parse(String query) throws QueryException {
				return new XQParser(query).parse();
			}
		};
	}

	protected Optimizer getOptimizer() {
		return new DefaultOptimizer();
	}

	protected Translator getTranslator() {
		return new PipelineCompiler(getModuleResolver());
	}

	protected ModuleResolver getModuleResolver() {
		return new BaseResolver();
	}

	public Module compile(String query) throws QueryException {
		if (XQuery.DEBUG) {
			System.out.println(String.format("Compiling query:\n%s", query));
		}
		AST ast = getParser().parse(query);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR, "parsed");
		}
		ast = getOptimizer().optimize(ast);
		Module module = getTranslator().translate(ast);
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR, "xquery");
		}
		return module;
	}
}
