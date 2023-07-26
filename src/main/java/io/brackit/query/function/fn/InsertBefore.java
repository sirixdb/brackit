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
package io.brackit.query.function.fn;

import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.module.StaticContext;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.function.AbstractFunction;

/**
 * Implementation of predefined function fn:insert-before($arg1, $arg2, $arg3)
 * as per http://www.w3.org/TR/xpath-functions/#func-insert-before
 *
 * @author Max Bechtold
 */
public class InsertBefore extends AbstractFunction {

  public InsertBefore(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    final Sequence s = args[0];
    IntNumeric p = (IntNumeric) args[1];
    final Sequence i = args[2];

    if (p.cmp(Int32.ONE) < 0) {
      p = Int32.ONE;
    }
    final IntNumeric pos = p;

    if (s == null) {
      return i;
    } else if (i == null) {
      return s;
    }

    return new LazySequence() {
      final Sequence seq = s;
      final Sequence ins = i;
      final IntNumeric p = pos;

      @Override
      public Iter iterate() {
        return new BaseIter() {
          private IntNumeric next = Int32.ONE;
          private Iter itSeq;
          private Iter itIns;

          @Override
          public Item next() throws QueryException {
            if (itSeq == null) {
              itSeq = seq.iterate();
              itIns = ins.iterate();
            }

            Item nextSeq;
            Item nextIns;

            if (next.cmp(p) < 0) {
              if ((nextSeq = itSeq.next()) != null) {
                next = next.inc();
                return nextSeq;
              } else {
                next = p;
              }
            }

            if (next.cmp(p) == 0) {
              if ((nextIns = itIns.next()) != null) {
                return nextIns;
              } else {
                next = next.inc().inc();
              }
            }

            return itSeq.next();
          }

          @Override
          public void close() {
            itSeq.close();
            itIns.close();
          }
        };
      }
    };
  }
}
