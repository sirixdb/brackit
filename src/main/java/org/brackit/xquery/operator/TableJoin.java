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
package org.brackit.xquery.operator;

import java.util.Arrays;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.compiler.translator.Reference;
import org.brackit.xquery.util.Cmp;
import org.brackit.xquery.util.join.FastList;
import org.brackit.xquery.util.join.MultiTypeJoinTable;
import org.brackit.xquery.jdm.Expr;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public final class TableJoin extends Check implements Operator {
  private class TableJoinCursor implements Cursor {
    final Cursor cursor;
    final Sequence[] padding;
    final int size;
    private Tuple prev;
    private Tuple next;
    MultiTypeJoinTable table;
    Atomic tgk; // grouping key of current table
    Tuple tuple;
    FastList<Sequence[]> it;
    int itPos = 0;
    int itSize = 0;

    public TableJoinCursor(Cursor cursor, int size, int pad) {
      this.cursor = cursor;
      this.size = size;
      this.padding = new Sequence[pad];
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      cursor.open(ctx);
      it = null;
    }

    @Override
    public void close(QueryContext ctx) {
      cursor.close(ctx);
      it = null;
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      if (it != null && itPos < itSize) {
        return tuple.concat(it.get(itPos++));
      }

      while ((tuple = next) != null || (tuple = cursor.next(ctx)) != null) {
        next = null;
        if (check && dead(tuple)) {
          prev = tuple.concat(padding);
          return prev;
        }
        if (groupVar >= 0) {
          Atomic gk = (Atomic) tuple.get(groupVar);
          if (tgk != null && tgk.atomicCmp(gk) != 0) {
            table = null;
          }
        }
        if (table == null) {
          buildTable(ctx, tuple);
        }
        final Sequence keys = isGCmp ? leftExpr.evaluate(ctx, tuple) : leftExpr.evaluateToItem(ctx, tuple);
        final FastList<Sequence[]> matches = table.probe(keys);

        it = matches;
        itPos = 0;
        itSize = matches.getSize();

        if (itPos < itSize) {
          prev = tuple.concat(matches.get(itPos++));
          return prev;
        } else if (leftJoin) {
          if (check) {
            // predicate is not fulfilled, but we must keep
            // lifted iteration group alive for "left-join"
            // semantics:
            // skip if previously returned tuple was in same
            // iteration group
            if (prev != null && !separate(prev, tuple)) {
              continue;
            }
            next = cursor.next(ctx);
            // skip if next tuple is in same iteration group
            if (next != null && !separate(tuple, next)) {
              continue;
            }
            // emit "dead" tuple where "check" field is switched-off
            // for pass-through in upstream operators
            prev = tuple.conreplace(padding, local(), null);
          } else {
            prev = tuple.concat(padding);
          }
          return prev;
        }
      }
      table = null;
      return null;
    }

    protected void buildTable(QueryContext ctx, Tuple tuple) throws QueryException {
      table = new MultiTypeJoinTable(cmp, isGCmp, skipSort);
      if (groupVar >= 0) {
        tgk = (Atomic) tuple.get(groupVar);
      }
      int pos = 1;
      boolean isLeftSizeBiggerOrEqualToRightSize = leftSize >= rightSize;
      Tuple cursorTuple;

      Cursor cursor1 = isLeftSizeBiggerOrEqualToRightSize ? right.create(ctx, tuple) : left.create(ctx, tuple);
      try {
        cursor1.open(ctx);
        while ((cursorTuple = cursor1.next(ctx)) != null) {
          Sequence keys;

          if (isLeftSizeBiggerOrEqualToRightSize) {
            keys = isGCmp ? rightExpr.evaluate(ctx, cursorTuple) : rightExpr.evaluateToItem(ctx, cursorTuple);
          } else {
            keys = isGCmp ? leftExpr.evaluate(ctx, cursorTuple) : leftExpr.evaluateToItem(ctx, cursorTuple);
          }
          if (keys != null) {
            Sequence[] tmp = cursorTuple.array();
            Sequence[] bindings = Arrays.copyOfRange(tmp, size, tmp.length);
            table.add(keys, bindings, pos++);
          }
        }
      } finally {
        cursor1.close(ctx);
      }
    }
  }

  final Operator left;
  final Operator right;

  int leftSize;

  int rightSize;

  final Expr rightExpr;
  final Expr leftExpr;
  final boolean leftJoin;
  final Cmp cmp;
  final boolean isGCmp;
  final boolean skipSort;
  int groupVar = -1;

  public TableJoin(Cmp cmp, boolean isGCmsp, boolean leftJoin, boolean skipSort, Operator l, Expr lExpr, Operator r,
      Expr rExpr) {
    this.cmp = cmp;
    this.isGCmp = isGCmsp;
    this.leftJoin = leftJoin;
    this.skipSort = skipSort;
    this.left = l;
    this.right = r;
    this.rightExpr = rExpr;
    this.leftExpr = lExpr;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    leftSize = left.tupleWidth(tuple.getSize());
    rightSize = right.tupleWidth(tuple.getSize());
    int lPad = leftSize - tuple.getSize();
    int rPad = rightSize - tuple.getSize();

    if (leftSize >= rightSize) {
      return new TableJoinCursor(left.create(ctx, tuple), leftSize, rPad);
    }
    return new TableJoinCursor(right.create(ctx, tuple), rightSize, lPad);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    leftSize = left.tupleWidth(buf[0].getSize());
    rightSize = right.tupleWidth(buf[0].getSize());
    int lPad = leftSize - buf[0].getSize();
    int rPad = rightSize - buf[0].getSize();

    if (leftSize >= rightSize) {
      return new TableJoinCursor(left.create(ctx, buf, len), leftSize, rPad);
    }
    return new TableJoinCursor(right.create(ctx, buf, len), rightSize, lPad);
  }

  @Override
  public int tupleWidth(int initSize) {
    return left.tupleWidth(initSize) + right.tupleWidth(initSize) - initSize;
  }

  public Reference group() {
    return pos -> groupVar = pos;
  }
}
