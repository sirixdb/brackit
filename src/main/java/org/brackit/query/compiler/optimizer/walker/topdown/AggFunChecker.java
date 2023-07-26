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

import static org.brackit.query.module.Namespaces.FN_NSURI;
import static org.brackit.query.module.Namespaces.FN_PREFIX;

import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.AST;
import org.brackit.query.compiler.XQ;

/**
 * @author Sebastian Baechle
 */
public abstract class AggFunChecker extends ScopeWalker {

  protected static final QNm FN_COUNT = new QNm(FN_NSURI, FN_PREFIX, "count");
  protected static final QNm FN_SUM = new QNm(FN_NSURI, FN_PREFIX, "sum");
  protected static final QNm FN_AVG = new QNm(FN_NSURI, FN_PREFIX, "avg");
  protected static final QNm FN_MIN = new QNm(FN_NSURI, FN_PREFIX, "min");
  protected static final QNm FN_MAX = new QNm(FN_NSURI, FN_PREFIX, "max");
  protected static final QNm[] aggFuns = new QNm[] { FN_COUNT, FN_SUM, FN_AVG, FN_MIN, FN_MAX };
  protected static final int[] aggFunMap = new int[] { XQ.CountAgg, XQ.SumAgg, XQ.AvgAgg, XQ.MinAgg, XQ.MaxAgg };

  protected QNm replaceRef(AST node, QNm name) {
    node.getParent().replaceChild(node.getChildIndex(), new AST(XQ.VariableRef, name));
    return name;
  }

  protected int aggFunType(int type) {
    return switch (type) {
      case XQ.CountAgg -> 0;
      case XQ.SumAgg -> 1;
      case XQ.AvgAgg -> 2;
      case XQ.MinAgg -> 3;
      case XQ.MaxAgg -> 4;
      default -> throw new RuntimeException("Unexpected aggregate function type: " + type);
    };
  }

}