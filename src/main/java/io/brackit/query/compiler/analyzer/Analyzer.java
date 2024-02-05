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
package io.brackit.query.compiler.analyzer;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.*;
import io.brackit.query.expr.Variable;
import io.brackit.query.jdm.Function;
import io.brackit.query.module.*;
import io.brackit.query.module.Module;
import io.brackit.query.compiler.parser.JsoniqParser;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Sebastian Baechle
 */
public class Analyzer {

  private final ModuleResolver resolver;
  private final AnyURI baseURI;
  private final List<Target> targets;
  private final List<ForwardDeclaration> decls;
  private final List<Module> modules;
  private final AST ast;

  public Analyzer(ModuleResolver resolver, AnyURI baseURI, AST ast) throws QueryException {
    this.resolver = resolver;
    this.baseURI = baseURI;
    this.decls = new ArrayList<>();
    this.modules = new ArrayList<>();
    this.targets = new ArrayList<>();
    this.ast = ast;
    analyze();
  }

  public AST getAST() {
    return ast;
  }

  public List<Target> getTargets() {
    return targets;
  }

  public List<Module> getModules() {
    return modules;
  }

  protected void analyze() throws QueryException {
    module(ast.getChild(0));

    // process all forward declarations
    for (ForwardDeclaration decl : decls) {
      targets.add(decl.process());
    }
    // check for cyclic dependencies
    // in initializer expressions
    for (ForwardDeclaration decl : decls) {
      if (decl instanceof CtxItemDecl) {
        if (checkCycle(decl, decls)) {
          throw new QueryException(ErrorCode.ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER,
                                   "Context item declaration depends on context item");
        }
      }
      if (decl instanceof VariableDecl) {
        if (checkCycle(decl, decls)) {
          throw new QueryException(ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY,
                                   "Cyclic variable declaration: %s",
                                   decl.getUnit());
        }

      }
    }
  }

  private boolean checkCycle(ForwardDeclaration decl, List<ForwardDeclaration> decls) {
    // perform very simple breadth-first search
    // to calculate set of transitive dependencies
    // ... we should improve this when we have time ;-)
    Set<Unit> expanded = new HashSet<>();
    Deque<Unit> pending = new ArrayDeque<>();
    for (Unit dep : decl.dependsOn()) {
      if (dep == decl.getUnit()) {
        return true;
      }
      expanded.add(dep);
      pending.add(dep);
    }
    Unit unit;
    while ((unit = pending.poll()) != null) {
      for (ForwardDeclaration d : decls) {
        if (d.getUnit() == unit) {
          for (Unit dep : d.dependsOn()) {
            if (dep == decl.getUnit()) {
              return true;
            }
            if (expanded.add(dep)) {
              pending.add(dep);
            }
          }
          break;
        }
      }
    }
    return false;
  }

  protected Module module(AST module) throws QueryException {
    if (module.getType() == XQ.LibraryModule) {
      return libraryModule(module);
    } else {
      return mainModule(module);
    }
  }

  protected Module libraryModule(AST module) throws QueryException {
    LibraryModule lm = new LibraryModule();
    StaticContext sctx = lm.getStaticContext();
    sctx.setBaseURI(baseURI);
    module.setStaticContext(sctx);
    AST ns = module.getChild(0);
    String prefix = ns.getChild(0).getStringValue();
    String uri = ns.getChild(1).getStringValue();
    lm.setTargetNS(uri);
    // TODO check prefix according to XQuery 4.2
    sctx.getNamespaces().declare(prefix, uri);
    modules.add(lm);
    if (module.getChildCount() == 2) {
      PrologAnalyzer pa = new PrologAnalyzer(lm, module.getChild(1));
      decls.addAll(pa.getDeclarations());
      handleImports(lm, pa.getImports());
    }
    return lm;
  }

  protected Module mainModule(AST module) throws QueryException {
    MainModule mm = new MainModule();
    StaticContext sctx = mm.getStaticContext();
    sctx.setBaseURI(baseURI);
    module.setStaticContext(sctx);
    AST prologOrBody = module.getChild(0);
    modules.add(mm);
    if (prologOrBody.getType() == XQ.Prolog) {
      PrologAnalyzer pa = new PrologAnalyzer(mm, prologOrBody);
      decls.addAll(pa.getDeclarations());
      handleImports(mm, pa.getImports());
      prologOrBody = module.getChild(1);
    }
    decls.add(new BodyDecl(mm, prologOrBody.getChild(0)));
    return mm;
  }

  private void handleImports(Module module, List<PrologAnalyzer.Import> imports) throws QueryException {
    for (PrologAnalyzer.Import i : imports) {
      List<Module> toImport = findModules(i);
      Variables lvars = module.getVariables();
      Functions lfuns = module.getStaticContext().getFunctions();
      for (Module m : toImport) {
        // check variables and functions to import
        Variables ivars = m.getVariables();
        Functions ifuns = m.getStaticContext().getFunctions();
        checkImports(lvars, ivars, lfuns, ifuns);
        // do import
        lvars.importVariables(ivars);
        lfuns.importFunctions(ifuns);
        module.importModule(m);
      }
    }
  }

  private List<Module> findModules(PrologAnalyzer.Import i) throws QueryException {
    List<Module> toImport = new ArrayList<>(resolver.resolve(i.getURI(), i.getLocations()));
    // add in-flight modules
    for (Module inFlight : modules) {
      String targetNS = inFlight.getTargetNS();
      if (targetNS != null && targetNS.equals(i.getURI())) {
        toImport.add(inFlight);
      }
    }
    if (toImport.isEmpty()) {
      // try to load modules
      List<String> loaded;
      try {
        loaded = resolver.load(i.getURI(), i.getLocations());
        if (loaded.isEmpty()) {
          throw new QueryException(ErrorCode.ERR_SCHEMA_OR_MODULE_NOT_FOUND, "Module '%s' not found", i.getURI());
        }
      } catch (IOException e) {
        throw new QueryException(e, ErrorCode.ERR_SCHEMA_OR_MODULE_NOT_FOUND, "Error loading module '%s'", i.getURI());
      }
      for (String query : loaded) {
        AST ast = new JsoniqParser(query).parse();
        toImport.add(module(ast.getChild(0)));
      }
    }
    return toImport;
  }

  private void checkImports(Variables lvars, Variables ivars, Functions lfuns, Functions ifuns) throws QueryException {
    for (Variable ivar : ivars.getDeclaredVariables()) {
      if (lvars.isDeclared(ivar.getName())) {
        throw new QueryException(ErrorCode.ERR_DUPLICATE_VARIABLE_DECL,
                                 "Import variable $%s has already been declared",
                                 ivar.getName());
      }
    }
    Map<QNm, Function[]> ifunMap = ifuns.getDeclaredFunctions();
    for (Entry<QNm, Function[]> ifun : ifunMap.entrySet()) {
      QNm name = ifun.getKey();
      Function[] ifu = ifun.getValue();
      for (Function function : ifu) {
        int argc = function.getSignature().getParams().length;
        if (lfuns.resolve(name, argc) != null) {
          throw new QueryException(ErrorCode.ERR_MULTIPLE_FUNCTION_DECLARATIONS,
                                   "Found multiple declarations of function %s",
                                   function.getName());
        }
      }
    }
  }
}