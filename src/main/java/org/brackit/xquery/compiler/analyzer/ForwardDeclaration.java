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
package org.brackit.xquery.compiler.analyzer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.Target;
import org.brackit.xquery.compiler.Unit;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.analyzer.AbstractAnalyzer.DefaultNS;
import org.brackit.xquery.expr.DeclVariable;
import org.brackit.xquery.function.UDF;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.xdm.Function;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
abstract class ForwardDeclaration extends ExprAnalyzer {

	protected final Unit unit;
	protected Set<Unit> deps;
	protected int ctxItemLevel;

	public ForwardDeclaration(Module module, Unit unit) {
		super(module);
		this.unit = unit;
	}

	abstract Target process() throws QueryException;

	Unit getUnit() {
		return unit;
	}
	
	@SuppressWarnings("unchecked")
	Set<Unit> dependsOn() {
		return (deps != null) ? deps : Collections.EMPTY_SET;
	}

	@Override
	protected boolean varRef(AST expr) throws QueryException {
		if (expr.getType() != XQ.VariableRef) {
			return false;
		}
		QNm name = (QNm) expr.getValue();
		if (!super.varRef(expr)) {
			return false;
		}
		name = expand(name, DefaultNS.EMPTY);
		if (!variables.check(name)) {
			DeclVariable dependency = (DeclVariable) module.getVariables()
					.resolve(name);
			if (deps == null) {
				deps = new HashSet<Unit>();
			}
			deps.add(dependency);
		}
		return true;
	}

	@Override
	protected boolean functionCall(AST expr) throws QueryException {
		if (!super.functionCall(expr)) {
			return false;
		}
		// re-check because function call might have been replaced,
		// e.g., fn:true() -> Bool.TRUE
		if (expr.getType() != XQ.FunctionCall) {
			return true;
		}
		QNm name = (QNm) expr.getValue();
		Function fun = sctx.getFunctions().resolve(name, expr.getChildCount());
		if (fun instanceof UDF) {
			if (deps == null) {
				deps = new HashSet<Unit>();
			}
			deps.add((UDF) fun);
		}
		return true;
	}
	
	@Override
	protected void openContextItemScope() throws QueryException {
		ctxItemLevel++;
	}

	@Override
	protected void closeContextItemScope() throws QueryException {
		ctxItemLevel--;
	}

	@Override
	protected void referContextItem() throws QueryException {
		if (ctxItemLevel == 0) {
			if (deps == null) {
				deps = new HashSet<Unit>();
			}
			deps.add(module.getVariables().getDftCtxItem());
		}
	}
}