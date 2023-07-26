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
package io.brackit.query.operator;

import io.brackit.query.util.Cmp;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;

import java.util.Arrays;

/**
 * @author Sebastian Baechle
 */
public class NLJoin implements Operator {

  final Operator l;
  final Operator r;
  final Expr rExpr;
  final Expr lExpr;
  final boolean leftJoin;
  final Cmp cmp;
  final boolean isGCmp;

  public NLJoin(Operator l, Operator r, Expr lExpr, Expr rExpr, Cmp cmp, boolean isGCmp, boolean leftJoin) {
    this.l = l;
    this.r = r;
    this.rExpr = rExpr;
    this.lExpr = lExpr;
    this.cmp = cmp;
    this.isGCmp = isGCmp;
    this.leftJoin = leftJoin;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    int lSize = l.tupleWidth(tuple.getSize());
    int pad = r.tupleWidth(tuple.getSize()) - tuple.getSize();
    return new NLJoinCursor(l.create(ctx, tuple), lSize, pad);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    int lSize = l.tupleWidth(buf[0].getSize());
    int pad = r.tupleWidth(buf[0].getSize()) - buf[0].getSize();
    return new NLJoinCursor(l.create(ctx, buf, len), lSize, pad);
  }

  @Override
  public int tupleWidth(int initSize) {
    return l.tupleWidth(initSize) + r.tupleWidth(initSize) - initSize;
  }

  private class NLJoinCursor implements Cursor {
    private final Cursor lc;
    private final int lSize;
    private final Sequence[] padding;
    private Tuple lt;
    private Sequence lKey;
    private boolean lMatch;
    private Cursor rc;

    private NLJoinCursor(Cursor lc, int lSize, int pad) {
      this.lc = lc;
      this.lSize = lSize;
      this.padding = new Sequence[pad];
    }

    @Override
    public void open(QueryContext ctx) {
      lc.open(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) {
      Tuple rt;
      Sequence rKey;
      while (true) {
        if (rc == null) {
          lt = lc.next(ctx);
          if (lt == null) {
            return null;
          }
          lMatch = false;
          lKey = isGCmp ? lExpr.evaluate(ctx, lt) : lExpr.evaluateToItem(ctx, lt);
          rc = r.create(ctx, lt);
          rc.open(ctx);
        }
        while ((rt = rc.next(ctx)) != null) {
          rKey = isGCmp ? rExpr.evaluate(ctx, rt) : rExpr.evaluateToItem(ctx, rt);

          boolean res = isGCmp ? cmp.gCmp(ctx, lKey, rKey) : cmp.vCmp(ctx, (Item) lKey, (Item) rKey);

          if (res) {
            Sequence[] tmp = rt.array();
            Sequence[] bindings = Arrays.copyOfRange(tmp, lSize, tmp.length);
            lMatch = true;
            return lt.concat(bindings);
          }
        }
        rc.close(ctx);
        rc = null;
        if (leftJoin && !lMatch) {
          return lt.concat(padding);
        }
      }
    }

    @Override
    public void close(QueryContext ctx) {
      if (rc != null) {
        rc.close(ctx);
      }
      lc.close(ctx);
    }
  }
}
