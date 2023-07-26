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
package org.brackit.query.util.sort;

import java.util.Comparator;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;
import org.brackit.query.atomic.Atomic;
import org.brackit.query.expr.Cast;
import org.brackit.query.node.stream.TransformerStream;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Expr;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Stream;
import org.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class Ordering implements Comparator<Tuple> {

  final Expr[] orderByExprs;
  final OrderModifier[] modifier;
  int offset;
  TupleSort sort;

  public Ordering(Expr[] orderByExprs, OrderModifier[] modifier) {
    this.orderByExprs = orderByExprs;
    this.modifier = modifier;
  }

  public void add(QueryContext ctx, Tuple t) throws QueryException {
    if (sort == null) {
      offset = t.getSize();
      sort = new TupleSort(this, 1);
    }
    sort.add(t.concat(sortKeys(ctx, t)));
  }

  public void add(Sequence[] keys, Tuple t) throws QueryException {
    if (sort == null) {
      offset = t.getSize();
      sort = new TupleSort(this, 1);
    }
    sort.add(t.concat(keys));
  }

  public Stream<Tuple> sorted() throws QueryException {
    sort.sort();
    Stream<Tuple> s = new TransformerStream<>(sort.stream()) {
      @Override
      protected Tuple transform(Tuple next) throws DocumentException {
        try {
          return next.project(0, offset);
        } catch (QueryException e) {
          throw new DocumentException(e);
        }
      }
    };
    sort = null;
    return s;
  }

  public Sequence[] sortKeys(QueryContext ctx, Tuple t) throws QueryException {
    Sequence[] concat = new Sequence[orderByExprs.length];
    for (int i = 0; i < orderByExprs.length; i++) {
      Item item = orderByExprs[i].evaluateToItem(ctx, t);
      Atomic atomic = (item != null) ? item.atomize() : null;
      if ((atomic != null) && (atomic.type().instanceOf(Type.UNA))) {
        atomic = Cast.cast(null, atomic, Type.STR);
      }
      concat[i] = atomic;
    }
    return concat;
  }

  public static class OrderModifier {
    public OrderModifier(boolean asc, boolean emptyLeast, String collation) {
      this.ASC = asc;
      this.EMPTY_LEAST = emptyLeast;
      this.collation = collation;
    }

    public final boolean ASC;
    public final boolean EMPTY_LEAST;
    public final String collation;
  }

  @Override
  public int compare(Tuple o1, Tuple o2) {
    try {
      for (int i = 0; i < modifier.length; i++) {
        int pos = offset + i;
        Atomic lAtomic = (Atomic) o1.get(pos);
        Atomic rAtomic = (Atomic) o2.get(pos);

        if (lAtomic == null) {
          if (rAtomic != null) {
            return (modifier[i].EMPTY_LEAST) ? -1 : 1;
          }
        }
        if (rAtomic == null) {
          return (modifier[i].EMPTY_LEAST) ? 1 : -1;
        }

        int res = lAtomic.cmp(rAtomic);
        if (res != 0) {
          return (modifier[i].ASC) ? res : -res;
        }
      }
      return 0;
    } catch (QueryException e) {
      if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
        throw new ClassCastException(e.getMessage());
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public void clear() {
    if (sort != null) {
      sort.clear();
      sort = null;
    }
  }
}
