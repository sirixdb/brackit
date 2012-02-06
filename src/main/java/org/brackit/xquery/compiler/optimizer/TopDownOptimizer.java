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
package org.brackit.xquery.compiler.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.optimizer.walker.DoSNStepMerger;
import org.brackit.xquery.compiler.optimizer.walker.OrderForGroupBy;
import org.brackit.xquery.compiler.optimizer.walker.PathDDOElimination;
import org.brackit.xquery.compiler.optimizer.walker.topdown.Projection;
import org.brackit.xquery.compiler.optimizer.walker.topdown.TopDownPipeline;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.Cfg;

/**
 * @author Sebastian Baechle
 * 
 */
public class TopDownOptimizer implements Optimizer {

	public static final String VARIABLE_PULLUP_CFG = "org.brackit.xquery.variablePullup";

	public static final String JOIN_DETECTION_CFG = "org.brackit.xquery.joinDetection";

	public static final String UNNEST_CFG = "org.brackit.xquery.unnest";

	public static boolean UNNEST = Cfg.asBool(UNNEST_CFG, true);

	public static boolean VARIABLE_PULLUP = Cfg.asBool(
			VARIABLE_PULLUP_CFG, false);

	public static boolean JOIN_DETECTION = Cfg.asBool(JOIN_DETECTION_CFG,
			true);

	private List<Stage> stages = new ArrayList<Stage>();

	public TopDownOptimizer() {
		stages.add(new Simplification());
		stages.add(new Pipelining());
		stages.add(new Reordering());
		if (UNNEST) {
			stages.add(new Unnest());
		}
		if (JOIN_DETECTION) {
			stages.add(new JoinRecognition());
		}
		stages.add(new Finalize());
	}

	@Override
	public List<Stage> getStages() {
		return stages;
	}

	@Override
	public AST optimize(StaticContext sctx, AST ast) throws QueryException {
		for (Stage stage : stages) {
			ast = stage.rewrite(sctx, ast);
		}
		return ast;
	}

	private static class Simplification implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) {
			ast = new DoSNStepMerger().walk(ast);
			ast = new OrderForGroupBy().walk(ast);
//			if (VARIABLE_PULLUP) {
//				ast = new LetVariableRefPullup().walk(ast);
//			}
//			ast = new ExtractFLWOR().walk(ast);
			return ast;
		}
	}

	private static class Pipelining implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new TopDownPipeline().walk(ast);
			return ast;
		}
	}
	
	private static class Reordering implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
//			ast = new ConjunctionSplitting().walk(ast);
//			ast = new SelectPushdown().walk(ast);
//			ast = new BindingPushup().walk(ast);
			return ast;
		}
	}

	private static class JoinRecognition implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
//			ast = new JoinRewriter().walk(ast);
//			ast = new JoinTree().walk(ast);
//			ast = new JoinTree().walk(ast);
//			ast = new JoinSortElimination().walk(ast);
//			ast = new LeftJoinGroupEmission().walk(ast);
			return ast;
		}
	}
	
	private static class Unnest implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
//			ast = new LetBindLift().walk(ast);
//			ast = new BindingPushupAfterLifting().walk(ast); // 2nd chance for pushing
			return ast;
		}
	}
	
	private static class Finalize implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
//			ast = new PredicateConjunction().walk(ast);
			ast = new PathDDOElimination(sctx).walk(ast);
			ast = new Projection().walk(ast);
			return ast;
		}
	}
}
