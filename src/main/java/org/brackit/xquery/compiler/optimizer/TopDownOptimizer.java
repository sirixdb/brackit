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

import org.brackit.xquery.QueryException;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.optimizer.walker.topdown.JoinGroupDemarcation;
import org.brackit.xquery.compiler.optimizer.walker.topdown.JoinRewriter;
import org.brackit.xquery.compiler.optimizer.walker.topdown.LeftJoinLifting;
import org.brackit.xquery.compiler.optimizer.walker.topdown.LeftJoinRemoval;
import org.brackit.xquery.compiler.optimizer.walker.topdown.LetBindToLeftJoin;
import org.brackit.xquery.compiler.optimizer.walker.topdown.PredicateMerge;
import org.brackit.xquery.compiler.optimizer.walker.topdown.PredicateSplit;
import org.brackit.xquery.compiler.optimizer.walker.topdown.PullEvaluation;
import org.brackit.xquery.compiler.optimizer.walker.topdown.SelectPullup;
import org.brackit.xquery.compiler.optimizer.walker.topdown.TopDownPipeline;
import org.brackit.xquery.module.StaticContext;

/**
 * @author Sebastian Baechle
 * 
 */
public class TopDownOptimizer extends DefaultOptimizer {

	public TopDownOptimizer() {
		super(new ArrayList<Stage>());
		stages.add(new Simplification());
		stages.add(new Pipelining());
		stages.add(new Reordering());
		if (JOIN_DETECTION) {
			stages.add(new JoinRecognition());
		}
		if (UNNEST) {
			stages.add(new Unnest());
		}
//		if (JOIN_DETECTION) {
//			stages.add(new JoinProcessing());
//		}
		stages.add(new FinalizePipeline());
		stages.add(new Finalize());
	}

	private static class Pipelining implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new TopDownPipeline().walk(ast);
			return ast;
		}
	}

	private static class Reordering implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new PredicateSplit().walk(ast);
			ast = new SelectPullup().walk(ast);
			return ast;
		}
	}

	private static class JoinRecognition implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new JoinRewriter().walk(ast);
			return ast;
		}
	}

	private static class JoinProcessing implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new JoinGroupDemarcation().walk(ast);
			return ast;
		}
	}

	private static class Unnest implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new LetBindToLeftJoin().walk(ast);
			ast = new LeftJoinLifting().walk(ast);
			ast = new LeftJoinRemoval().walk(ast);
			return ast;
		}
	}

	private static class FinalizePipeline implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) throws QueryException {
			ast = new PredicateMerge().walk(ast);
			ast = new JoinGroupDemarcation().walk(ast);
			ast = new PullEvaluation().walk(ast);
			return ast;
		}
	}
}
