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

import io.brackit.query.QueryException;
import io.brackit.query.atomic.QNm;
import io.brackit.query.compiler.translator.Translator;
import io.brackit.query.function.UDF;
import io.brackit.query.jdm.Expr;
import io.brackit.query.module.Module;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.Target;

/**
 * @author Sebastian Baechle
 */
public class FunctionDecl extends ForwardDeclaration {
  final UDF udf;
  final QNm[] params;
  AST body;

  public FunctionDecl(Module module, UDF udf, QNm[] params, AST body) {
    super(module, udf);
    this.udf = udf;
    this.params = params;
    this.body = body;
  }

  public Target process() throws QueryException {
    for (int i = 0; i < params.length; i++) {
      params[i] = bind(params[i]);
    }
    functionBody(body);
    return new Target(module, sctx, body, unit, udf.isUpdating()) {

      @Override
      public void translate(Translator translator) throws QueryException {
        Expr expr = translator.function(module, sctx, udf, params, ast, allowUpdate);
        unit.setExpr(expr);
      }
    };
  }
}