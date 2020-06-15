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

import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.compiler.optimizer.walker.Walker;
import org.brackit.xquery.util.Cmp;

/**
 * @author Sebastian Baechle
 */
public class JoinToSelectConversion extends Walker {

  @Override
  protected AST visit(AST join) {
    if ((join.getType() != XQ.Join) || (join.checkProperty("leftJoin"))) {
      return join;
    }
    AST lEnd = join.getChild(0).getChild(0);
    AST rEnd = join.getChild(1).getChild(0);
    if ((lEnd.getType() != XQ.End) || (rEnd.getType() != XQ.End)) {
      return join;
    }

    Cmp cmp = (Cmp) join.getProperty("cmp");
    boolean isGCmp = join.checkProperty("GCmp");
    AST predicate = new AST(XQ.ComparisonExpr);
    predicate.addChild(new AST(CmpUtil.type(cmp, isGCmp)));
    predicate.addChild(lEnd.getChild(0));
    predicate.addChild(rEnd.getChild(0));
    AST select = new AST(XQ.Selection);
    select.addChild(predicate);
    select.addChild(join.getLastChild());
    join.getParent().replaceChild(join.getChildIndex(), select);
    return join.getParent();
  }
}
