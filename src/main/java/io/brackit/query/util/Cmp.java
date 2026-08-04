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
package io.brackit.query.util;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Una;
import io.brackit.query.expr.Cast;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.atomic.Null;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public enum Cmp {
  eq, ne, lt, le, gt, ge;

  public Cmp swap() {
    switch (this) {
      case ge:
        return Cmp.le;
      case gt:
        return lt;
      case le:
        return ge;
      case lt:
        return gt;
    }
    return this;
  }

  /**
   * Compares two atomic values. This method performs type promotion if
   * necessary. We assume, however, that none of the types is of type
   * xs:untypedAtomic.
   */
  public boolean aCmp(QueryContext ctx, Atomic left, Atomic right) throws QueryException {
    // JSONiq gives null its own comparison rules, and they are total — never a type error:
    //
    //   "null can be compared for equality or inequality to anything - it is only equal to itself
    //    so that false is returned when comparing it for equality with any non-null atomic."
    //   "For ordering operators (lt, le, gt, ge), null is considered the smallest possible value
    //    (like in JavaScript)."
    //     -- JSONiq specification, Basic Operations; `1 eq null, "foo" ne null, null eq null`
    //        evaluates to `false true true` and `1 lt null` to `false`.
    //
    // Without this, the per-type cmp/eq implementations decide, and every one of them raises
    // XPTY0004 on a type it does not recognise — so a single null row turned an ordinary filter
    // over a nullable JSON field into a failed query.
    final boolean leftIsNull = left instanceof Null;
    final boolean rightIsNull = right instanceof Null;
    if (leftIsNull || rightIsNull) {
      if (this == Cmp.eq) {
        return leftIsNull && rightIsNull;
      }
      if (this == Cmp.ne) {
        return !(leftIsNull && rightIsNull);
      }
      // Ordering: null is the smallest value, so it ties only with itself.
      final int nullCompare = leftIsNull && rightIsNull ? 0 : leftIsNull ? -1 : 1;
      if (nullCompare == 0) {
        return this == Cmp.ge || this == Cmp.le;
      }
      return nullCompare < 0 ? this == Cmp.le || this == Cmp.lt : this == Cmp.ge || this == Cmp.gt;
    }

    if (this == Cmp.eq) {
      return left.eq(right);
    } else if (this == Cmp.ne) {
      return !left.eq(right);
    }

    int compare = left.cmp(right);
    boolean res;

    if (compare == 0) {
      res = this == Cmp.ge || this == Cmp.le;
    } else if (compare < 0) {
      res = this == Cmp.le || this == Cmp.lt;
    } else {
      res = this == Cmp.ge || this == Cmp.gt;
    }

    return res;
  }

  public boolean vCmp(QueryContext ctx, Item left, Item right) throws QueryException {
    left = left.atomize();
    right = right.atomize();

    if (left instanceof Una) {
      left = new Str(((Una) left).str);
    }

    if (right instanceof Una) {
      right = new Str(((Una) right).str);
    }

    boolean res = aCmp(ctx, (Atomic) left, (Atomic) right);
    return res;
  }

  public Bool vCmpAsBool(QueryContext ctx, Item left, Item right) throws QueryException {
    if (left == null || right == null) {
      return null;
    }
    boolean res = vCmp(ctx, left, right);
    return res ? Bool.TRUE : Bool.FALSE;
  }

  public boolean gCmp(QueryContext ctx, Sequence left, Sequence right) throws QueryException {
    // assume simple case and perform cheaper direct evaluation
    if (left instanceof Item && right instanceof Item) {
      return compareLeftAndRightAtomic(ctx, ((Item) left).atomize(), ((Item) right).atomize());
    }

    Iter ls = left.iterate();
    Iter rs = null;
    Item lItem;
    Item rItem;
    Atomic lAtomic;
    Atomic rAtomic;

    try {
      while ((lItem = ls.next()) != null) {
        lAtomic = lItem.atomize();

        rs = right.iterate();
        while ((rItem = rs.next()) != null) {
          rAtomic = rItem.atomize();

          boolean res = compareLeftAndRightAtomic(ctx, lAtomic, rAtomic);

          if (res) {
            return true;
          }
        }
        rs.close();
        rs = null;
      }
    } finally {
      ls.close();
      if (rs != null) {
        rs.close();
      }
    }

    return false;
  }

  public Bool gCmpAsBool(QueryContext ctx, Sequence left, Sequence right) throws QueryException {
    if (left == null || right == null) {
      // XQ 3.1 §3.7.2: a general comparison is the EXISTENTIAL quantification over the two
      // sequences — with an empty operand no pair exists, so the result is false() (an
      // xs:boolean), not the empty sequence. Returning null made count(() = 1) yield 0.
      return Bool.FALSE;
    }
    boolean res = gCmp(ctx, left, right);
    return res ? Bool.TRUE : Bool.FALSE;
  }

  private boolean compareLeftAndRightAtomic(QueryContext ctx, Atomic lAtomic, Atomic rAtomic) throws QueryException {
    Type lType = lAtomic.type();
    Type rType = rAtomic.type();

    if (lType.instanceOf(Type.UNA)) {
      if (rType.isNumeric()) {
        lAtomic = Dbl.parse(((Una) lAtomic).str);
      } else if (rType.instanceOf(Type.UNA) || rType.instanceOf(Type.STR)) {
        // Optimized: Avoid explicit cast
        /*
         * rAtomic = Cast.cast(ctx, rAtomic, Type.STR, false); lAtomic =
         * Cast.cast(ctx, lAtomic, Type.STR, false);
         */
      } else {
        lAtomic = Cast.cast(null, lAtomic, rAtomic.type(), false);
      }
    } else if (rType.instanceOf(Type.UNA)) {
      if (lType.isNumeric()) {
        rAtomic = Dbl.parse(((Una) rAtomic).str);
      } else if (lType.instanceOf(Type.STR)) {
        // Optimized: Avoid explicit cast
        /*
         * lAtomic = Cast.cast(ctx, lAtomic, Type.STR, false); rAtomic =
         * Cast.cast(ctx, rAtomic, Type.STR, false);
         */
      } else {
        rAtomic = Cast.cast(null, rAtomic, lAtomic.type(), false);
      }
    }

    return aCmp(ctx, lAtomic, rAtomic);
  }
}