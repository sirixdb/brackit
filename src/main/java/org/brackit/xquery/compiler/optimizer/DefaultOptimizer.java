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
package org.brackit.xquery.compiler.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.optimizer.walker.DoSNStepMerger;
import org.brackit.xquery.compiler.optimizer.walker.JoinRewriter2;
import org.brackit.xquery.compiler.optimizer.walker.JoinSortElimination;
import org.brackit.xquery.compiler.optimizer.walker.LetBindLift;
import org.brackit.xquery.compiler.optimizer.walker.LetVariableRefPullup;
import org.brackit.xquery.compiler.optimizer.walker.OrderForGroupBy;
import org.brackit.xquery.compiler.optimizer.walker.UnnestRewriter;
import org.brackit.xquery.compiler.parser.DotUtil;
import org.brackit.xquery.util.Cfg;

/**
 * @author Sebastian Baechle
 * 
 */
public class DefaultOptimizer implements Optimizer {

	public static final String VARIABLE_PULLUP_CFG = "org.brackit.xquery.variablePullup";

	public static final String JOIN_DETECTION_CFG = "org.brackit.xquery.joinDetection";

	public static final String UNNEST_CFG = "org.brackit.xquery.unnest";

	public static boolean UNNEST = Cfg.asBool(UNNEST_CFG, false);

	public static boolean VARIABLE_PULLUP = Cfg.asBool(
			VARIABLE_PULLUP_CFG, false);

	public static boolean JOIN_DETECTION = Cfg.asBool(JOIN_DETECTION_CFG,
			false);

	private List<Stage> stages = new ArrayList<Stage>();

	public DefaultOptimizer() {
		stages.add(new Simplification());
		if (UNNEST) {
			stages.add(new Pipelining());
		}
		if (JOIN_DETECTION) {
			stages.add(new JoinRecognition());
		}
	}

	@Override
	public List<Stage> getStages() {
		return stages;
	}

	@Override
	public AST optimize(AST ast) throws QueryException {
		for (Stage stage : stages) {
			ast = stage.rewrite(ast);
		}
		return ast;
	}

	private static class Simplification implements Stage {
		public AST rewrite(AST ast) {
			new DoSNStepMerger().walk(ast);
			new OrderForGroupBy().walk(ast);

			if (VARIABLE_PULLUP) {
				new LetVariableRefPullup().walk(ast);
			}

			if (XQuery.DEBUG) {
				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
						"standardrewrite");
			}

			return ast;
		}
	}

	private static class Pipelining implements Stage {
		public AST rewrite(AST ast) throws QueryException {
			new UnnestRewriter().walk(ast);

			if (XQuery.DEBUG) {
				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
						"unnestrewrite");
			}

			new LetBindLift().walk(ast);

			if (XQuery.DEBUG) {
				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
						"letbindliftrewrite");
			}
			
			return ast;
		}
	}

	private static class JoinRecognition implements Stage {
		public AST rewrite(AST ast) throws QueryException {

			new JoinRewriter2().walk(ast);

			if (XQuery.DEBUG) {
				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
						"joinrewrite");
			}

			new JoinSortElimination().walk(ast);

			if (XQuery.DEBUG) {
				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
						"joinsorteliminationrewrite");
			}

//			new LeftJoinGroupEmission().walk(ast);
//
//			if (XQuery.DEBUG) {
//				DotUtil.drawDotToFile(ast.dot(), XQuery.DEBUG_DIR,
//						"joingroupemissionrewrite");
//			}

			return ast;
		}
	}
}
