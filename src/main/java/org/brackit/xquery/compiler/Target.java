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
import org.brackit.xquery.compiler.optimizer.Optimizer;
import org.brackit.xquery.compiler.translator.Translator;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Expr;

/**
 * 
 * A compilation target for a specific unit.
 * 
 * @author Sebastian Baechle
 * 
 */
public class Target {

	protected final Module module;
	protected final StaticContext sctx;
	protected final Unit unit;
	protected final boolean allowUpdate;
	protected AST ast;

	public Target(Module module, StaticContext sctx, AST ast, Unit unit, boolean allowUpdate) {
		this.module = module;
		this.sctx = sctx;
		this.ast = ast;
		this.unit = unit;
		this.allowUpdate = allowUpdate;
	}
	
	public void optimize(Optimizer optimizer) throws QueryException {
		ast = optimizer.optimize(sctx, ast);
	}
	
	public void translate(Translator translator) throws QueryException {
		Expr expr = translator.expression(module, sctx, ast, allowUpdate);
		unit.setExpr(expr);
	}

	public Module getModule() {
		return module;
	}

	public StaticContext getStaticContext() {
		return sctx;
	}

	public AST getAst() {
		return ast;
	}

	public boolean allowUpdate() {
		return allowUpdate;
	}
}
