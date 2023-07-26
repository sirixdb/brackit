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
package org.brackit.query.function.fn;

import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.atomic.Int32;
import org.brackit.query.atomic.IntNumeric;
import org.brackit.query.atomic.QNm;
import org.brackit.query.function.AbstractFunction;
import org.brackit.query.module.StaticContext;
import org.brackit.query.sequence.BaseIter;
import org.brackit.query.sequence.LazySequence;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Iter;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Signature;

/**
 * Implementation of predefined function fn:remove($arg1, $arg2) as per
 * http://www.w3.org/TR/xpath-functions/#func-remove
 *
 * @author Max Bechtold
 */
public class Remove extends AbstractFunction {

  public Remove(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    final Sequence s = args[0];
    if (s == null) {
      return null;
    }

    final IntNumeric pos = (IntNumeric) args[1];

    return new LazySequence() {
      final Sequence seq = s;
      final IntNumeric p = pos;

      @Override
      public Iter iterate() {
        return new BaseIter() {
          private IntNumeric next = Int32.ONE;
          private Iter it;

          @Override
          public Item next() throws QueryException {
            if (it == null) {
              it = seq.iterate();
            }

            if (next.cmp(p) == 0) {
              next = next.inc();
              ;
              it.next();
            }
            next = next.inc();
            return it.next();
          }

          @Override
          public void close() {
            it.close();
          }
        };
      }
    };
  }
}
