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
package io.brackit.query.compiler.optimizer.walker.topdown;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.QNm;
import io.brackit.query.util.Cmp;
import io.brackit.query.compiler.AST;
import io.brackit.query.compiler.XQ;

/**
 * @author Sebastian Baechle
 */
public class LetBindToLeftJoin extends AggFunChecker {

  @Override
  protected AST visit(AST node) {
    if (node.getType() != XQ.LetBind) {
      return node;
    }

    AST bindingExpr = node.getChild(1);

    if (bindingExpr.getType() != XQ.PipeExpr) {
      return node;
    }

    return convertToLeftJoin(node);
  }

  private AST convertToLeftJoin(AST let) {
    AST insertJoinAfter = let.getParent();

    // use a trivial empty pipeline as left input
    // and use the constant boolean value "true" as
    // left join key expression
    AST leftIn = new AST(XQ.Start);
    AST lend = new AST(XQ.End);
    lend.addChild(new AST(XQ.Bool, Bool.TRUE));
    leftIn.addChild(lend);

    // copy the nested pipeline as right input
    AST rightIn = let.getChild(1).getChild(0).copyTree();

    // locate end of the nested pipeline
    // and create a copy of the return expression
    AST righInEnd = rightIn;
    while (righInEnd.getType() != XQ.End) {
      righInEnd = righInEnd.getLastChild();
    }
    AST letReturn = righInEnd.getChild(0).copyTree();

    // replace the return expression with a constant
    // boolean value "true" as right join key expression
    righInEnd.replaceChild(0, new AST(XQ.Bool, Bool.TRUE));

    // the post join pipeline uses a let-binding
    // for the original return expression
    // and then groups the let-bound return values
    // (only the binding of the final let will be used)
    AST post = new AST(XQ.Start);
    AST letVarBinding = let.getChild(0).copyTree();
    // TODO fix cardinality of binding if necessary
    AST rlet = new AST(XQ.LetBind);
    rlet.addChild(letVarBinding);
    rlet.addChild(letReturn);
    QNm letVar = (QNm) letVarBinding.getChild(0).getValue();
    AST groupBy = createGroupBy(letVar, let);
    rlet.addChild(groupBy);

    post.addChild(rlet);

    // finally assemble left join
    AST ljoin = createJoin(leftIn, rightIn, post, let.getLastChild().copyTree());

    // we must not sort if result is directly aggregated
    boolean skipSort = (groupBy.getChild(1).getChild(0).getType() != XQ.SequenceAgg);
    if (skipSort) {
      ljoin.setProperty("skipSort", Boolean.TRUE);
    }

    int replaceAt = insertJoinAfter.getChildCount() - 1;
    insertJoinAfter.replaceChild(replaceAt, ljoin);
    snapshot();
    refreshScopes(insertJoinAfter, true);
    return insertJoinAfter;
  }

  private AST createJoin(AST leftIn, AST rightIn, AST post, AST out) {
    AST ljoin = new AST(XQ.Join);
    ljoin.setProperty("leftJoin", Boolean.TRUE);
    ljoin.setProperty("cmp", Cmp.eq);
    ljoin.setProperty("GCmp", false);
    ljoin.addChild(leftIn);
    ljoin.addChild(rightIn);
    ljoin.addChild(post);
    ljoin.addChild(out);
    return ljoin;
  }

  private AST createGroupBy(QNm letVar, AST letBind) {
    int aggType = XQ.SequenceAgg;

    Var var = findScope(letBind).localBindings().get(0);
    VarRef refs = findVarRefs(var, letBind.getLastChild());
    if (refs == null) {
      // TODO Unused variable???? SingleAgg OK but currently causes errors
      // in GroupByAggregates....
      // aggType = XQ.SingleAgg;
    } else if (refs.next != null) {
      // TODO optimize me
    } else {
      AST p = refs.ref.getParent();
      if (p.getType() == XQ.FunctionCall) {
        QNm fun = (QNm) p.getValue();
        for (int i = 0; i < aggFuns.length; i++) {
          QNm aggFun = aggFuns[i];
          if (fun.atomicCmp(aggFun) == 0) {
            replaceRef(p, letVar);
            aggType = aggFunMap[i];
            break;
          }
        }
      }
    }

    AST groupBy = new AST(XQ.GroupBy);
    groupBy.setProperty("sequential", Boolean.TRUE);

    AST aggSpec = new AST(XQ.AggregateSpec);
    aggSpec.addChild(new AST(XQ.VariableRef, letVar));
    aggSpec.addChild(createBinding(letVar, aggType));
    groupBy.addChild(aggSpec);

    AST dftAgg = new AST(XQ.DftAggregateSpec);
    dftAgg.addChild(new AST(XQ.SingleAgg));
    groupBy.addChild(dftAgg);

    groupBy.addChild(new AST(XQ.End));

    return groupBy;
  }

  private AST createBinding(QNm name, int type) {
    AST agg = new AST(XQ.AggregateBinding);
    AST binding = new AST(XQ.TypedVariableBinding);
    binding.addChild(new AST(XQ.Variable, name));
    agg.addChild(binding);
    agg.addChild(new AST(type));
    return agg;
  }
}