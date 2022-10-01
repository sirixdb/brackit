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
package org.brackit.xquery.sequence;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractSequence implements Sequence {
  @Override
  public final Sequence evaluate(QueryContext ctx, Tuple context) {
    return this;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return ExprUtil.asItem(this);
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  @Override
  public Sequence[] array() {
    return new Sequence[] { this };
  }

  @Override
  public Sequence get(int position) {
    if (position != 0) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, position);
    }
    return this;
  }

  @Override
  public Tuple project(int... positions) {
    Sequence[] projected = new Sequence[positions.length];
    int targetPos = 0;
    for (int pos : positions) {
      projected[targetPos++] = get(pos);
    }
    return new TupleImpl(projected);
  }

  @Override
  public Tuple project(int start, int end) {
    if (start != 0) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, start);
    }
    if (end != 0) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, end);
    }
    return this;
  }

  @Override
  public Tuple replace(int position, Sequence s) {
    if (position != 0) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, position);
    }
    return new TupleImpl(s);
  }

  @Override
  public Tuple concat(Sequence s) {
    return new TupleImpl(new Sequence[] { this, s });
  }

  @Override
  public Tuple concat(Sequence[] s) {
    Sequence[] tmp = new Sequence[s.length + 1];
    tmp[0] = this;
    System.arraycopy(s, 0, tmp, 1, s.length);
    return new TupleImpl(tmp);
  }

  @Override
  public Tuple conreplace(Sequence con, int position, Sequence s) {
    if (position < 0 || position >= 2) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, position);
    }
    Sequence[] tmp = new Sequence[] { this, con };
    tmp[position] = s;
    return new TupleImpl(tmp);
  }

  @Override
  public Tuple conreplace(Sequence[] con, int position, Sequence s) {
    int nLen = con.length + 1;
    if (position < 0 || position >= nLen) {
      throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR, position);
    }
    Sequence[] tmp = new Sequence[nLen];
    tmp[0] = this;
    System.arraycopy(con, 0, tmp, 1, con.length);
    tmp[position] = s;
    return new TupleImpl(tmp);
  }

  @Override
  public int getSize() {
    return 1;
  }
}
