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
package io.brackit.query.compiler;

import java.util.Map;

import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.analyzer.Analyzer;
import io.brackit.query.compiler.optimizer.Optimizer;
import io.brackit.query.compiler.optimizer.TopDownOptimizer;
import io.brackit.query.compiler.parser.JsoniqParser;
import io.brackit.query.compiler.translator.TopDownTranslator;
import io.brackit.query.compiler.translator.Translator;
import io.brackit.query.function.json.JSONParse;
import io.brackit.query.function.json.Keys;
import io.brackit.query.function.json.Size;
import io.brackit.query.module.Functions;
import io.brackit.query.module.Module;
import io.brackit.query.util.dot.DotUtil;
import io.brackit.query.function.bit.ArrayValues;
import io.brackit.query.function.bit.BitFun;
import io.brackit.query.function.bit.Create;
import io.brackit.query.function.bit.Drop;
import io.brackit.query.function.bit.Eval;
import io.brackit.query.function.bit.Exists;
import io.brackit.query.function.bit.Fields;
import io.brackit.query.function.bit.Len;
import io.brackit.query.function.bit.Load;
import io.brackit.query.function.bit.Mkdir;
import io.brackit.query.function.bit.Now;
import io.brackit.query.function.bit.Parse;
import io.brackit.query.function.bit.Serialize;
import io.brackit.query.function.bit.Silent;
import io.brackit.query.function.bit.Store;
import io.brackit.query.function.bit.Values;
import io.brackit.query.function.io.Ls;
import io.brackit.query.function.io.Read;
import io.brackit.query.function.io.Readline;
import io.brackit.query.function.io.Write;
import io.brackit.query.function.io.Writeline;

/**
 * Compiles an {@link Module XQuery module}.
 *
 * @author Sebastian Baechle
 */
public class CompileChain {

  static {
    // IO
    Functions.predefine(new Readline());
    Functions.predefine(new Writeline());
    Functions.predefine(new Read());
    Functions.predefine(new Write());
    Functions.predefine(new Ls(true));
    Functions.predefine(new Ls(false));
    // Internal
    Functions.predefine(BitFun.SOME_FUNC);
    Functions.predefine(BitFun.EVERY_FUNC);
    // Utility
    Functions.predefine(new Now());
    Functions.predefine(new Silent());
    Functions.predefine(new Parse());
    Functions.predefine(new Eval());
    Functions.predefine(new Serialize());
    Functions.predefine(new Len());
    Functions.predefine(new Fields());
    Functions.predefine(new Values());
    // Storage
    Functions.predefine(new Store(true));
    Functions.predefine(new Store(false));
    Functions.predefine(new Load(true));
    Functions.predefine(new Load(false));
    Functions.predefine(new Create());
    Functions.predefine(new Drop());
    Functions.predefine(new Mkdir());
    Functions.predefine(new Exists());
    Functions.predefine(new ArrayValues());
    // JSON
    Functions.predefine(new JSONParse());
    Functions.predefine(new Size());
    Functions.predefine(new Keys());
  }

  final AnyURI baseURI;
  final ModuleResolver resolver;

  public CompileChain() {
    this(new BaseResolver(), null);
  }

  public CompileChain(AnyURI baseURI) {
    this(new BaseResolver(), baseURI);
  }

  public CompileChain(ModuleResolver resolver) {
    this(resolver, null);
  }

  public CompileChain(ModuleResolver resolver, AnyURI baseURI) {
    this.resolver = resolver;
    this.baseURI = baseURI;
  }

  protected Optimizer getOptimizer(Map<QNm, Str> options) {
    return new TopDownOptimizer(options);
  }

  protected Translator getTranslator(Map<QNm, Str> options) {
    return new TopDownTranslator(options);
  }

  protected ModuleResolver getModuleResolver() {
    return resolver;
  }

  protected AST parse(String query) throws QueryException {
    return new JsoniqParser(query).parse();
  }

  public Module compile(String query) throws QueryException {
    if (Query.DEBUG) {
      System.out.printf("Compiling:\n%s%n", query);
    }
    ModuleResolver resolver = getModuleResolver();
    AST parsed = parse(query);
    if (Query.DEBUG) {
      DotUtil.drawDotToFile(parsed.dot(), Query.DEBUG_DIR, "parsed");
    }
    Analyzer analyzer = new Analyzer(resolver, baseURI, parsed);
    AST xquery = analyzer.getAST();
    Module module = analyzer.getModules().get(0);
    Map<QNm, Str> options = module.getOptions();
    // optimize all targets of all modules
    for (Target t : analyzer.getTargets()) {
      t.optimize(getOptimizer(options));
    }
    if (Query.DEBUG) {
      DotUtil.drawDotToFile(xquery.dot(), Query.DEBUG_DIR, "xquery");
    }
    // translate all targets of all modules
    for (Target t : analyzer.getTargets()) {
      t.translate(getTranslator(options));
    }
    // everything went fine - add compiled modules to library
    for (Module m : analyzer.getModules()) {
      if (m.getTargetNS() != null) {
        resolver.register(m.getTargetNS(), m);
      }
    }
    return module;
  }
}
