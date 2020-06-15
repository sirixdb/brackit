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
package org.brackit.xquery.operator;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class Select extends Check implements Operator {
  private final Operator in;
  final Expr predicate;

  public class SelectCursor implements Cursor {
    private final Cursor c;
    private Tuple prev;
    private Tuple next;

    public SelectCursor(Cursor c) {
      this.c = c;
    }

    @Override
    public void close(QueryContext ctx) {
      c.close(ctx);
    }

    public Tuple next(QueryContext ctx) {
      Tuple t;
      while (((t = next) != null) || (t = c.next(ctx)) != null) {
        next = null;
        if (check && dead(t)) {
          break;
        }
        Sequence p = predicate.evaluate(ctx, t);
        if ((p != null) && (p.booleanValue())) {
          break;
        }
        if (!check) {
          continue;
        }
        // predicate is not fulfilled but we must keep
        // lifted iteration group alive for "left-join" semantics.
        // skip if previously returned tuple was in same iteration group
        if ((prev != null) && (!separate(prev, t))) {
          continue;
        }
        next = c.next(ctx);
        // skip if next tuple is in same iteration group
        if ((next != null) && (!separate(t, next))) {
          continue;
        }
        // emit "dead" tuple where "check" field is switched-off
        // for pass-through in upstream operators
        t = t.replace(local(), null); // switch-off check var
        break;
      }
      prev = t;
      return t;
    }

    @Override
    public void open(QueryContext ctx) {
      c.open(ctx);
    }
  }

  public Select(Operator in, Expr predicate) {
    this.in = in;
    this.predicate = predicate;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    return new SelectCursor(in.create(ctx, tuple));
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    return new SelectCursor(in.create(ctx, buf, len));
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize);
  }
}
