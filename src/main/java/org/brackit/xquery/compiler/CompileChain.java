/*
 * [New BSD License]
<<<<<<< HEAD
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
=======
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
>>>>>>> upstream/master
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
<<<<<<< HEAD
 *
=======
 *
>>>>>>> upstream/master
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

import java.util.Map;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.analyzer.Analyzer;
import org.brackit.xquery.compiler.optimizer.Optimizer;
import org.brackit.xquery.compiler.optimizer.TopDownOptimizer;
import org.brackit.xquery.compiler.parser.XQParser;
import org.brackit.xquery.compiler.translator.TopDownTranslator;
import org.brackit.xquery.compiler.translator.Translator;
import org.brackit.xquery.function.bit.ArrayValues;
import org.brackit.xquery.function.bit.BitFun;
import org.brackit.xquery.function.bit.Create;
import org.brackit.xquery.function.bit.Drop;
import org.brackit.xquery.function.bit.Eval;
import org.brackit.xquery.function.bit.Exists;
import org.brackit.xquery.function.bit.Fields;
import org.brackit.xquery.function.bit.Len;
import org.brackit.xquery.function.bit.Load;
import org.brackit.xquery.function.bit.Mkdir;
import org.brackit.xquery.function.bit.Now;
import org.brackit.xquery.function.bit.Parse;
import org.brackit.xquery.function.bit.Serialize;
import org.brackit.xquery.function.bit.Silent;
import org.brackit.xquery.function.bit.Store;
import org.brackit.xquery.function.bit.Values;
import org.brackit.xquery.function.io.Ls;
import org.brackit.xquery.function.io.Read;
import org.brackit.xquery.function.io.Readline;
import org.brackit.xquery.function.io.Write;
import org.brackit.xquery.function.io.Writeline;
import org.brackit.xquery.function.json.JSONParse;
import org.brackit.xquery.module.Functions;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.util.dot.DotUtil;

/**
 * Compiles an {@link Module XQuery module}.
 *
 * @author Sebastian Baechle
 *
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
    return new XQParser(query).parse();
  }

  public Module compile(String query) throws QueryException {
    if (XQuery.DEBUG) {
      System.out.println(String.format("Compiling:\n%s", query));
    }
    ModuleResolver resolver = getModuleResolver();
    AST parsed = parse(query);
    if (XQuery.DEBUG) {
      DotUtil.drawDotToFile(parsed.dot(), XQuery.DEBUG_DIR, "parsed");
    }
    Analyzer analyzer = new Analyzer(resolver, baseURI, parsed);
    AST xquery = analyzer.getAST();
    Module module = analyzer.getModules().get(0);
    Map<QNm, Str> options = module.getOptions();
    // optimize all targets of all modules
    for (Target t : analyzer.getTargets()) {
      t.optimize(getOptimizer(options));
    }
    if (XQuery.DEBUG) {
      DotUtil.drawDotToFile(xquery.dot(), XQuery.DEBUG_DIR, "xquery");
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
