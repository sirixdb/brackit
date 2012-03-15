/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.analyzer.Analyzer;
import org.brackit.xquery.compiler.optimizer.DefaultOptimizer;
import org.brackit.xquery.compiler.optimizer.Optimizer;
import org.brackit.xquery.compiler.parser.XQParser;
import org.brackit.xquery.compiler.translator.PipelineCompiler;
import org.brackit.xquery.compiler.translator.Translator;
import org.brackit.xquery.function.bit.Create;
import org.brackit.xquery.function.bit.Drop;
import org.brackit.xquery.function.bit.Eval;
import org.brackit.xquery.function.bit.Every;
import org.brackit.xquery.function.bit.Exists;
import org.brackit.xquery.function.bit.Load;
import org.brackit.xquery.function.bit.Mkdir;
import org.brackit.xquery.function.bit.Parse;
import org.brackit.xquery.function.bit.Serialize;
import org.brackit.xquery.function.bit.Silent;
import org.brackit.xquery.function.bit.Some;
import org.brackit.xquery.function.bit.Store;
import org.brackit.xquery.function.io.Ls;
import org.brackit.xquery.function.io.Read;
import org.brackit.xquery.function.io.Readline;
import org.brackit.xquery.function.io.Write;
import org.brackit.xquery.function.io.Writeline;
import org.brackit.xquery.module.Functions;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.util.dot.DotUtil;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * Compiles an {@link Module XQuery module}.
 * 
 * @author Sebastian Baechle
 * 
 */
public class CompileChain {

	public static final Every BIT_EVERY_FUNC = new Every(new QNm(
			Namespaces.XML_NSURI, Namespaces.BIT_PREFIX, "every"),
			new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
					new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)));

	public static final Some BIT_SOME_FUNC = new Some(new QNm(
			Namespaces.XML_NSURI, Namespaces.BIT_PREFIX, "some"),
			new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
					new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)));

	static {
		// IO
		Functions.predefine(new Readline());
		Functions.predefine(new Writeline());
		Functions.predefine(new Read());
		Functions.predefine(new Write());
		Functions.predefine(new Ls(true));
		Functions.predefine(new Ls(false));
		// Internal
		Functions.predefine(BIT_SOME_FUNC);
		Functions.predefine(BIT_EVERY_FUNC);
		// Utility
		Functions.predefine(new Silent());
		Functions.predefine(new Parse());
		Functions.predefine(new Eval());
		Functions.predefine(new Serialize());
		// Storage
		Functions.predefine(new Store(true));
		Functions.predefine(new Store(false));
		Functions.predefine(new Load(true));
		Functions.predefine(new Load(false));
		Functions.predefine(new Create());
		Functions.predefine(new Drop());
		Functions.predefine(new Mkdir());
		Functions.predefine(new Exists());
	}

	final AnyURI baseURI;

	public CompileChain() {
		baseURI = null;
	}

	public CompileChain(AnyURI baseURI) {
		this.baseURI = baseURI;
	}

	protected Optimizer getOptimizer() {
		return new DefaultOptimizer();
	}

	protected Translator getTranslator() {
		return new PipelineCompiler();
	}

	protected ModuleResolver getModuleResolver() {
		return new BaseResolver();
	}

	protected AST parse(String query) throws QueryException {
		return new XQParser(query).parse();
	}

	public Module compile(String query) throws QueryException {
		if (XQuery.DEBUG) {
			System.out.println(String.format("Compiling:\n%s", query));
		}
		ModuleResolver resolver = getModuleResolver();
		Analyzer analyzer = new Analyzer(resolver, baseURI, parse(query));
		AST xquery = analyzer.getAST();
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(xquery.dot(), XQuery.DEBUG_DIR, "parsed");
		}
		// optimize all targets of all modules
		for (Target t : analyzer.getTargets()) {
			t.optimize(getOptimizer());
		}
		// translate all targets of all modules
		for (Target t : analyzer.getTargets()) {
			t.translate(getTranslator());
		}
		// everything went fine - add compiled modules to library
		for (Module module : analyzer.getModules()) {
			if (module.getTargetNS() != null) {
				resolver.register(module.getTargetNS(), module);
			}
		}
		if (XQuery.DEBUG) {
			DotUtil.drawDotToFile(xquery.dot(), XQuery.DEBUG_DIR, "xquery");
		}
		return analyzer.getModules().get(0);
	}
}
