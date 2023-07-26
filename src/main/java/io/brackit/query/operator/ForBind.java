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

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;

/**
 * @author Sebastian Baechle
 */
public class ForBind extends Check implements Operator {
  final Operator in;
  final Expr bind;
  final boolean allowingEmpty;
  boolean bindVar = true;
  boolean bindPos = false;

  private class ForBindCursor implements Cursor {
    private final Cursor c;
    private IntNumeric pos;
    private Tuple t;
    private Iter it;

    public ForBindCursor(Cursor c) {
      this.c = c;
    }

    @Override
    public void close(QueryContext ctx) {
      if (it != null) {
        it.close();
      }
      it = null;
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) {
      while (true) {
        if (it != null) {
          Item item = it.next();
          if (item != null) {
            return emit(t, item);
          }
          it.close();
          it = null;
        }
        if ((t = c.next(ctx)) == null) {
          return null;
        }
        if (check && dead(t)) {
          Tuple tmp = passthrough(t);
          t = null;
          return tmp;
        }
        Sequence s = bind.evaluate(ctx, t);
        pos = Int32.ZERO;
        if (s == null) {
          Tuple tmp = allowingEmpty ? emit(t, null) : check ? passthroughUncheck(t, local()) : null;
          t = null;
          return tmp;
        } else if (s instanceof Item && !(s instanceof Array)) {
          return emit(t, s);
        } else {
          it = s.iterate();
          Item i = it.next();
          if (i != null) {
            return emit(t, i);
          }
          it.close();
          it = null;
          if (allowingEmpty) {
            Tuple tmp = emit(i, null);
            t = null;
            return tmp;
          } else if (check) {
            Tuple tmp = passthroughUncheck(t, local());
            t = null;
            return tmp;
          }
        }
      }
    }

    private Tuple emit(Tuple t, Sequence item) {
      if (bindVar) {
        if (bindPos) {
          return t.concat(new Sequence[] { item, item != null ? (pos = pos.inc()) : pos });
        } else {
          return t.concat(item);
        }
      } else if (bindPos) {
        return t.concat(item != null ? (pos = pos.inc()) : pos);
      } else {
        return t;
      }
    }

    private Tuple passthrough(Tuple t) {
      if (bindVar) {
        if (bindPos) {
          return t.concat(new Sequence[2]);
        } else {
          return t.concat((Sequence) null);
        }
      } else if (bindPos) {
        return t.concat((Sequence) null);
      } else {
        return t;
      }
    }

    private Tuple passthroughUncheck(Tuple t, int check) {
      if (bindVar) {
        if (bindPos) {
          return t.conreplace(new Sequence[2], check, null);
        } else {
          return t.conreplace((Sequence) null, check, null);
        }
      } else if (bindPos) {
        return t.conreplace((Sequence) null, check, null);
      } else {
        return t;
      }
    }

    @Override
    public void open(QueryContext ctx) {
      if (it != null) {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "ForBind already opened");
      }
      c.open(ctx);
    }
  }

  public ForBind(Operator in, Expr bind, boolean allowingEmpty) {
    this.in = in;
    this.bind = bind;
    this.allowingEmpty = allowingEmpty;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) {
    return new ForBindCursor(in.create(ctx, tuple));
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) {
    return new ForBindCursor(in.create(ctx, buf, len));
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize) + (bindVar ? 1 : 0) + (bindPos ? 1 : 0);
  }

  public void bindVariable(boolean bindVariable) {
    this.bindVar = bindVariable;
  }

  public void bindPosition(boolean bindPos) {
    this.bindPos = bindPos;
  }
}
