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

import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.AST;
import org.brackit.query.compiler.XQ;

/**
 * @author Sebastian Baechle
 */
public class JoinGroupDemarcation extends ScopeWalker {

  private int tableJoinGroupVar;

  private QNm createGroupVarName() {
    return new QNm("_joingroup;" + (tableJoinGroupVar++));
  }

  @Override
  protected AST visit(AST join) {
    if ((join.getType() != XQ.Join) || (join.getProperty("group") != null)) {
      return join;
    }

    // find closest scope from which
    // right input is independent of
    VarRef refs = findVarRefs(join.getChild(1));
    Scope[] scopes = sortScopes(refs);
    Scope local = findScope(join);

    AST stopAt = null;
    for (int i = scopes.length - 1; i >= 0; i--) {
      Scope scope = scopes[i];
      if (scope.compareTo(local) < 0) {
        stopAt = scope.node;
        break;
      }
    }

    // locate farthest scope we can go to
    AST anc = join.getParent();
    boolean reachedStopAt = false;
    while (true) {
      reachedStopAt = (reachedStopAt || (anc == stopAt));
      if (anc.getType() == XQ.Start) {
        if (anc.getParent().getType() != XQ.Join) {
          return join;
        }
        anc = anc.getParent().getParent();
      } else if (anc.getType() == XQ.LetBind) {
        // let-bindings are "static" within an iteration;
        // we may safely increase the scope
        anc = anc.getParent();
      } else if (reachedStopAt) {
        break;
      } else {
        anc = anc.getParent();
      }
    }

    // prepend an artificial count for
    // marking the join group boundaries
    QNm joingroupVar = createGroupVarName();
    join.setProperty("group", joingroupVar);

    AST count = new AST(XQ.Count);
    AST runVarBinding = new AST(TypedVariableBinding);
    runVarBinding.addChild(new AST(Variable, joingroupVar));
    count.addChild(runVarBinding);
    count.addChild(anc.getLastChild().copyTree());
    anc.replaceChild(anc.getChildCount() - 1, count);
    refreshScopes(anc, true);
    return anc;
  }
}
