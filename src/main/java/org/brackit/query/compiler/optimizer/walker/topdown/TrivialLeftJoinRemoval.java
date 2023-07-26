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
package org.brackit.query.compiler.optimizer.walker.topdown;

import static org.brackit.query.compiler.XQ.TypedVariableBinding;
import static org.brackit.query.compiler.XQ.Variable;

import java.util.ArrayList;
import java.util.List;

import org.brackit.query.atomic.Bool;
import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.AST;
import org.brackit.query.compiler.XQ;
import org.brackit.query.compiler.optimizer.walker.Walker;

/**
 * @author Sebastian Baechle
 */
public final class TrivialLeftJoinRemoval extends Walker {

  private int checkVar;

  private List<QNm> appendCheck(List<QNm> checks, QNm var) {
    final List<QNm> list = checks == null ? new ArrayList<>() : new ArrayList<>(checks);
    list.add(var);
    return list;
  }

  private QNm createCheckVarName() {
    return new QNm("_lcheck;" + (checkVar++));
  }

  @Override
  protected AST visit(AST join) {
    if (join.getType() != XQ.Join || !join.checkProperty("leftJoin")) {
      return join;
    }

    AST leftIn = join.getChild(0).getChild(0);
    AST rightIn = join.getChild(1).getChild(0);
    if ((leftIn.getType() != XQ.End) || (!constTrueJoinKey(leftIn.getChild(0))) || (!constTrueJoinKey(findEnd(rightIn)
                                                                                                                      .getChild(0)))) {
      return join;
    }

    @SuppressWarnings("unchecked") List<QNm> check = (List<QNm>) join.getProperty("check");

    QNm checkVar = createCheckVarName();
    AST count = new AST(XQ.Count);
    AST runVarBinding = new AST(TypedVariableBinding);
    runVarBinding.addChild(new AST(Variable, checkVar));
    count.addChild(runVarBinding);
    if (check != null) {
      count.setProperty("check", check);
    }

    List<QNm> check2 = appendCheck(check, checkVar);

    AST appendTo = count;
    appendTo = copyPipeline(check2, appendTo, rightIn);
    appendTo = copyPipeline(check2, appendTo, join.getChild(2).getChild(0));
    appendTo.addChild(join.getChild(3).copyTree());

    AST parent = join.getParent();
    parent.replaceChild(join.getChildIndex(), count);
    return parent;
  }

  private AST copyPipeline(List<QNm> check, AST parent, AST node) {
    while (node.getType() != XQ.End) {
      AST tmp = node.copy();
      for (int i = 0; i < node.getChildCount() - 1; i++) {
        tmp.addChild(node.getChild(i).copyTree());
      }
      if (tmp.getType() == XQ.Join) {
        tmp.setProperty("leftJoin", Boolean.TRUE);
      }
      tmp.setProperty("check", check);
      parent.addChild(tmp);
      parent = tmp;
      node = node.getLastChild();
    }
    return parent;
  }

  private AST findEnd(AST node) {
    AST tmp = node;
    while (tmp.getType() != XQ.End) {
      tmp = tmp.getLastChild();
    }
    return tmp;
  }

  private boolean constTrueJoinKey(AST joinKey) {
    return joinKey.getType() == XQ.Bool && ((Bool) joinKey.getValue()).bool;
  }
}
