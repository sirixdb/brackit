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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.compiler.profiler;

import java.io.File;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.translator.PipelineCompiler;
import org.brackit.xquery.module.MainModule;
import org.brackit.xquery.operator.Operator;
import org.brackit.xquery.xdm.Expr;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ProfilingCompiler extends PipelineCompiler {

	private ProfilingNode parent; // used to chain expressions

	private ProfilingNode child; // used to chain operators

	private ProfileOperator pending; // "upcoming" parent operator to

	public class ProfilingMainModule extends MainModule {
		public static final String PLOT_TYPE = "svg";

		private ProfileExpr expr;

		public void setExpr(Expr rootExpr) {
			this.expr = (ProfileExpr) rootExpr;
			super.setExpr(rootExpr);
		}

		public void visualize(String outputDir) {
			DotContext dotCtx = new DotContext();
			expr.toDot(dotCtx);
			createDot(outputDir, "expr", dotCtx);
		}

		private void createDot(String outputDir, String name, DotContext dotCtx) {
			try {
				File f = File.createTempFile(name, "dot");
				dotCtx.write(f);

				f.deleteOnExit();

				String outfile = outputDir + "/" + name + "s." + PLOT_TYPE;
				String command = "dot -T" + PLOT_TYPE + " -o" + outfile + " "
						+ f;
				Process proc = Runtime.getRuntime().exec(command);
				proc.waitFor();
				f.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public ProfilingCompiler() {
		super();
	}

	@Override
	protected Expr anyExpr(AST node) throws QueryException {
		ProfileExpr profileExpr = new ProfileExpr();
		ProfilingNode savedParent = parent;
		parent = profileExpr;
		Expr e = super.anyExpr(node);
		profileExpr.setExpr(e);
		parent = savedParent;
		if (parent != null) {
			parent.addChild(profileExpr);
		}
		return profileExpr;
	}

	@Override
	protected Operator anyOp(AST node) throws QueryException {
		ProfileOperator profileOp = new ProfileOperator();
		ProfilingNode savedParent = parent;
		parent = profileOp;
		Operator op = super.anyOp(node);
		profileOp.setOp(op);
		parent = savedParent;
		if (parent != null) {
			parent.addChild(profileOp);
		}
		return profileOp;
	}
}
