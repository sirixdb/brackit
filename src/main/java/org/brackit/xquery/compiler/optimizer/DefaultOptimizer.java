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
import java.util.Map;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.compiler.optimizer.walker.DoSNStepMerger;
import org.brackit.xquery.compiler.optimizer.walker.OrderForGroupBy;
import org.brackit.xquery.compiler.optimizer.walker.PathDDOElimination;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.Cfg;

/**
 * @author Sebastian Baechle
 * 
 */
public class DefaultOptimizer implements Optimizer {

	public static final QNm SEQUENTIAL_GROUPBY = new QNm(Bits.BIT_NSURI,
			Bits.BIT_PREFIX, "sequential-groupby");

	public static final String JOIN_DETECTION_CFG = "org.brackit.xquery.joinDetection";

	public static final String UNNEST_CFG = "org.brackit.xquery.unnest";

	public static boolean UNNEST = Cfg.asBool(UNNEST_CFG, true);

	public static boolean JOIN_DETECTION = Cfg.asBool(JOIN_DETECTION_CFG, true);

	protected final List<Stage> stages;
	protected final Map<QNm, Str> options;

	public DefaultOptimizer(Map<QNm, Str> options) {
		stages = new ArrayList<>();
		stages.add(new Simplification());
		stages.add(new Finalize());
		this.options = options;
	}

	protected DefaultOptimizer(Map<QNm, Str> options, List<Stage> stages) {
		this.stages = stages;
		this.options = options;
	}

	@Override
	public List<Stage> getStages() {
		return stages;
	}

	@Override
	public AST optimize(StaticContext sctx, AST ast) {
		for (Stage stage : stages) {
			ast = stage.rewrite(sctx, ast);
		}
		return ast;
	}

	protected class Simplification implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) {
			ast = new DoSNStepMerger().walk(ast);
			if (enabled(SEQUENTIAL_GROUPBY)) {
				ast = new OrderForGroupBy().walk(ast);
			}
			return ast;
		}
	}

	protected class Finalize implements Stage {
		public AST rewrite(StaticContext sctx, AST ast) {
			ast = new PathDDOElimination(sctx).walk(ast);
			return ast;
		}
	}

	protected boolean enabled(QNm option) {
		Str opt = options.get(option);
		return ((opt != null) && Boolean.parseBoolean(opt.stringValue()));
	}
}
