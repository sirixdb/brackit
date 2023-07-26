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
package org.brackit.query.operator;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;

/**
 * @author Sebastian Baechle
 */
public class Start implements Operator {
  public static class StartCursor implements Cursor {
    final Tuple start;
    boolean open = false;
    boolean deliver = false;

    public StartCursor(Tuple start) {
      this.start = start;
    }

    @Override
    public void close(QueryContext ctx) {
      open = false;
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      if (!open) {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR);
      }

      if (!deliver) {
        return null;
      }

      deliver = false;
      return start;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      open = true;
      deliver = true;
    }
  }

  public static class BufferStartCursor implements Cursor {
    Tuple[] buf;
    final int len;
    int pos = -1;

    public BufferStartCursor(Tuple[] buf, int len) {
      this.buf = buf;
      this.len = len;
    }

    @Override
    public void close(QueryContext ctx) {
      buf = null; // allow gc
      pos = -1;
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      if (pos < 0) {
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR);
      }
      if (pos < len) {
        Tuple t = buf[pos];
        buf[pos++] = null; // allow gc
        return t;
      } else {
        return null;
      }
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      pos = 0;
    }
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    return new StartCursor(tuple);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    return new BufferStartCursor(buf, len);
  }

  @Override
  public int tupleWidth(int initSize) {
    return initSize;
  }
}