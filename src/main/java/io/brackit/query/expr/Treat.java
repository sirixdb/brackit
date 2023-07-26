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
package io.brackit.query.expr;

import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.TypedSequence;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class Treat implements Expr {
  private final Expr expr;

  private final SequenceType expected;

  public Treat(Expr expr, SequenceType expected) {
    this.expr = expr;
    this.expected = expected;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) {
    try {
      Sequence sequence = expr.evaluate(ctx, tuple);
      final Sequence typedSequence = TypedSequence.toTypedSequence(expected, sequence);
      return new AbstractSequence() {
        final Sequence s = typedSequence;

        @Override
        public IntNumeric size() {
          try {
            return s.size();
          } catch (QueryException e) {
            if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
              throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
            }
            throw e;
          }
        }

        @Override
        public Iter iterate() {
          return new BaseIter() {
            Iter it = s.iterate();

            @Override
            public Item next() {
              try {
                return it.next();
              } catch (QueryException e) {
                if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
                  throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
                }
                throw e;
              }
            }

            @Override
            public void close() {
              it.close();
            }
          };
        }

        @Override
        public boolean booleanValue() {
          try {
            return s.booleanValue();
          } catch (QueryException e) {
            if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
              throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
            }
            throw e;
          }
        }

        @Override
        public Item get(IntNumeric pos) {
          try {
            return s.get(pos);
          } catch (QueryException e) {
            if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
              throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
            }
            throw e;
          }
        }
      };
    } catch (QueryException e) {
      if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
        throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
      }
      throw e;
    }
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    try {
      Item item = expr.evaluateToItem(ctx, tuple);
      return TypedSequence.toTypedItem(expected, item);
    } catch (QueryException e) {
      if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
        throw new QueryException(e, ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE);
      }
      throw e;
    }
  }

  @Override
  public boolean isUpdating() {
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }
}
