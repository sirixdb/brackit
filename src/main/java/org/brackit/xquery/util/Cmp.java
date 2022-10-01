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
package org.brackit.xquery.util;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

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
      return null;
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