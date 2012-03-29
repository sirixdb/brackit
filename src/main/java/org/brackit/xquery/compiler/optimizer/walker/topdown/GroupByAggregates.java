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
package org.brackit.xquery.compiler.optimizer.walker.topdown;

import static org.brackit.xquery.module.Namespaces.FN_NSURI;
import static org.brackit.xquery.module.Namespaces.FN_PREFIX;

import java.util.Map;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.compiler.CompileChain;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.DefaultOptimizer;
import org.brackit.xquery.compiler.optimizer.Optimizer;
import org.brackit.xquery.compiler.optimizer.Stage;
import org.brackit.xquery.compiler.optimizer.TopDownOptimizer;
import org.brackit.xquery.module.StaticContext;

/**
 * @author Sebastian Baechle
 * 
 */
public class GroupByAggregates extends ScopeWalker {

	private static final QNm FN_COUNT = new QNm(FN_NSURI, FN_PREFIX, "count");
	private static final QNm FN_SUM = new QNm(FN_NSURI, FN_PREFIX, "sum");
	private static final QNm FN_AVG = new QNm(FN_NSURI, FN_PREFIX, "avg");
	private static final QNm FN_MIN = new QNm(FN_NSURI, FN_PREFIX, "min");
	private static final QNm FN_MAX = new QNm(FN_NSURI, FN_PREFIX, "max");

	private static final QNm[] aggFuns = new QNm[] { FN_COUNT, FN_SUM, FN_AVG,
			FN_MIN, FN_MAX };

	private static final int[] aggFunMap = new int[] { XQ.CountAgg, XQ.SumAgg,
			XQ.AvgAgg, XQ.MinAgg, XQ.MaxAgg };

	@Override
	protected AST visit(AST node) {
		if (node.getType() != XQ.GroupBy) {
			return node;
		}

		AST dftAgg = node.getChild(node.getChildCount() - 2);
		AST dftAggType = dftAgg.getChild(0);
		if (dftAggType.getType() == XQ.SequenceAgg) {
			// Switch to aggregate type "single" as new default.
			// This reduces the grouping overhead for variables
			// which are not accessed at all.
			dftAgg.replaceChild(0, new AST(XQ.SingleAgg));
		} else if (dftAggType.getType() != XQ.SingleAgg) {
			// There's already a specialized aggregation type in place.
			// It seems unlikely that further optimization is necessary
			// so just exit the rule.
			return node;
		}

		// Now define the substitute aggregate bindings
		// for non-grouping variables, which are really
		// referenced after the grouping.
		for (Var var : findScope(node).localBindings()) {
			VarRef refs = findVarRefs(var, node.getLastChild());
			AST aggSpec = getAggSpec(node, var);
			introduceAggBindings(aggSpec, var, refs);
		}
		snapshot();
		return node;
	}

	private AST getAggSpec(AST node, Var var) {
		AST aggSpec = new AST(XQ.AggregateSpec);
		aggSpec.addChild(new AST(XQ.VariableRef, var.var));
		node.insertChild(node.getChildCount() - 2, aggSpec);
		return aggSpec;
	}

	/*
	 * Inspect the references to the given non-grouping variable and introduce
	 * substitute bindings for the appropriate aggregate.
	 * 
	 * Example 1: The non-grouping variable $v is referred to by the expression
	 * fn:count($v). Here we introduce a new substitute variable binding
	 * $count;v which is the computed count aggregate of grouped $v's and
	 * replace the function expression with a variable reference to $count;v
	 * 
	 * Example 2: The non-grouping variable $w is referenced in the expressions
	 * fn:avg($w) and $w[position() > 3]. Here we create two substitute bindings
	 * $avg;w and $w for the aggregated average of all $w values and the
	 * aggregated sequence of all $w's respectively.
	 * 
	 * The names for the substitution variables are simple concatenations of the
	 * aggregate function name and the variable name with a ";" in between. This
	 * are actually illegal QNm's but ensures that we do not produce variable
	 * name collisions. If a particular aggregate binding already exists, e.g.,
	 * from a previous optimizer rule, we simply reuse that variable to avoid
	 * that an aggregate is computed twice.
	 */
	private void introduceAggBindings(AST aggSpec, Var var, VarRef refs) {
		QNm seqAggVar = null;
		QNm[] aggFunVars = new QNm[aggFuns.length];

		// collect all pre-existing aggregate bindings for re-use
		for (int i = 1; i < aggSpec.getChildCount(); i++) {
			AST binding = aggSpec.getChild(i);
			QNm name = (QNm) binding.getChild(0).getChild(0).getValue();
			int type = binding.getChild(1).getType();
			if (type == XQ.SequenceAgg) {
				seqAggVar = name;
			} else {
				int aggFunType = aggFunType(type);
				aggFunVars[aggFunType] = name;
			}
		}

		// inspect all references
		for (VarRef ref = refs; ref != null; ref = ref.next) {
			AST p = ref.ref.getParent();
			boolean isAggFun = false;
			if (p.getType() == XQ.FunctionCall) {
				QNm fun = (QNm) p.getValue();
				for (int i = 0; i < aggFuns.length; i++) {
					QNm aggFun = aggFuns[i];
					if (fun.atomicCmp(aggFun) == 0) {
						// create function aggregate binding if necessary
						if (aggFunVars[i] == null) {
							QNm subsitute = new QNm(Bits.BIT_NSURI, null,
									aggFun.getLocalName() + ";"
											+ var.var.getLocalName());
							aggFunVars[i] = subsitute;
							AST agg = createBinding(subsitute, aggFunMap[i]);
							aggSpec.addChild(agg);
						}
						// replace whole function call with
						// sequence aggregate binding
						replaceRef(p, aggFunVars[i]);
						isAggFun = true;
						break;
					}
				}
			}
			if (!isAggFun) {
				// create sequence aggregate binding if necessary
				if (seqAggVar == null) {
					seqAggVar = var.var;
					AST agg = createBinding(var.var, XQ.SequenceAgg);
					aggSpec.addChild(agg);
				}
				// change variable ref directly to sequence aggregate binding
				replaceRef(ref.ref, seqAggVar);
			}
		}
	}

	private AST createBinding(QNm subsitute, int type) {
		AST agg = new AST(XQ.AggregateBinding);
		AST binding = new AST(XQ.TypedVariableBinding);
		binding.addChild(new AST(XQ.Variable, subsitute));
		agg.addChild(binding);
		agg.addChild(new AST(type));
		return agg;
	}

	private QNm replaceRef(AST node, QNm name) {
		node.getParent().replaceChild(node.getChildIndex(),
				new AST(XQ.VariableRef, name));
		return name;
	}

	private int aggFunType(int type) {
		switch (type) {
		case XQ.CountAgg:
			return 0;
		case XQ.SumAgg:
			return 1;
		case XQ.AvgAgg:
			return 2;
		case XQ.MinAgg:
			return 3;
		case XQ.MaxAgg:
			return 4;
		case XQ.SequenceAgg:
		default:
			throw new RuntimeException("Unexpected aggregate function type: "
					+ type);
		}
	}

	public static void main(String[] args) throws QueryException {
		DefaultOptimizer.UNNEST = false;
		CompileChain cc = new CompileChain() {

			@Override
			protected Optimizer getOptimizer(Map<QNm, Str> options) {
				return new TopDownOptimizer(options) {
					{
						stages.add(stages.size() - 2, new Stage() {
							@Override
							public AST rewrite(StaticContext sctx, AST ast)
									throws QueryException {
								return new GroupByAggregates().walk(ast);
							}
						});
					}
				};
			}
		};
		XQuery xq = new XQuery(
				cc,
				"let $x:= 1 "
						+ "let $y:= (for $a in (1 to 10) for $b in ($a to $a + 2) group by $b let $c := if ($x eq 1) then $b else () return <r b='{$c}' cnt='{count($a)}' vals='{$a}'/>) "
						+ "return $y");
		xq.setPrettyPrint(true);
		xq.serialize(new QueryContext(), System.out);
	}
}
