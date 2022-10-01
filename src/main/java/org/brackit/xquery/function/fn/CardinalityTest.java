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
package org.brackit.xquery.function.fn;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.Cardinality;

/**
 * @author Sebastian Baechle
 */
public class CardinalityTest extends AbstractFunction {
  private final Cardinality cardinality;

  public CardinalityTest(QNm name, Signature signature, Cardinality cardinality) {
    super(name, signature, true);
    this.cardinality = cardinality;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (cardinality == Cardinality.One) {
      return exactlyOnce(args);
    } else if (cardinality == Cardinality.OneOrMany) {
      return oneOrMore(args);
    } else if (cardinality == Cardinality.ZeroOrOne) {
      return zeroOrOne(args);
    } else {
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR,
                               "Unsupported cardinality test: %s",
                               cardinality);
    }
  }

  private Sequence zeroOrOne(Sequence[] args) throws QueryException {
    Sequence s = args[0];
    if (s != null && !(s instanceof Item)) {
      Iter it = s.iterate();
      try {
        if ((s = it.next()) != null && it.next() != null) {
          throw new QueryException(ErrorCode.ERR_ZERO_OR_ONE_FAILED,
                                   "fn:zero-or-one called with a sequence containing more than one item");
        }
      } finally {
        it.close();
      }
    }
    return s;
  }

  private Sequence oneOrMore(Sequence[] args) throws QueryException {
    Sequence s = args[0];
    if (s == null) {
      throw new QueryException(ErrorCode.ERR_ONE_OR_MORE_FAILED, "fn:one-or-more called with an empty sequence");
    } else if (!(s instanceof Item)) {
      Iter it = s.iterate();
      try {
        if (it.next() == null) {
          throw new QueryException(ErrorCode.ERR_ONE_OR_MORE_FAILED, "fn:one-or-more called with an empty sequence");
        }
      } finally {
        it.close();
      }
    }
    return s;
  }

  private Sequence exactlyOnce(Sequence[] args) throws QueryException {
    Sequence s = args[0];
    if (s == null) {
      throw new QueryException(ErrorCode.ERR_EXACTLY_ONCE_FAILED, "fn:exactly-one called with an empty sequence");
    } else if (!(s instanceof Item)) {
      Iter it = s.iterate();
      try {
        if ((s = it.next()) == null) {
          throw new QueryException(ErrorCode.ERR_EXACTLY_ONCE_FAILED, "fn:exactly-one called with an empty sequence");
        }
        if (it.next() != null) {
          throw new QueryException(ErrorCode.ERR_EXACTLY_ONCE_FAILED,
                                   "fn:exactly-one called with a sequence containing more than one item");
        }
      } finally {
        it.close();
      }
    }
    return s;
  }
}