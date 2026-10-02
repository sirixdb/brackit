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
package io.brackit.query.operator;

import java.util.Arrays;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.util.Cmp;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.compiler.translator.Reference;
import io.brackit.query.util.join.FastList;
import io.brackit.query.util.join.MultiTypeJoinTable;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Sequence;

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
      Tuple cursorTuple;

      Cursor cursor1 = right.create(ctx, tuple);
      try {
        cursor1.open(ctx);
        while ((cursorTuple = cursor1.next(ctx)) != null) {
          Sequence keys = isGCmp ? rightExpr.evaluate(ctx, cursorTuple) : rightExpr.evaluateToItem(ctx, cursorTuple);
          if (keys != null) {
            Sequence[] tmp = cursorTuple.array();
            Sequence[] bindings = Arrays.copyOfRange(tmp, size, tmp.length);
            table.add(keys, bindings, pos++);
          }
        }
      } finally {
        cursor1.close(ctx);
      }
      table.seal();
    }
  }

  final Operator left;
  final Operator right;

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
    return createCursor(left.create(ctx, tuple), tuple.getSize());
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    return createCursor(left.create(ctx, buf, len), buf[0].getSize());
  }

  private Cursor createCursor(Cursor cursor, int inputSize) {
    // The right pipeline is compiled with the left bindings in scope and must
    // be opened with a left output tuple. Tuple width is not row cardinality:
    // swapping sides here breaks correlation, binding positions, result order,
    // comparison direction and left-join iteration checks.
    int leftSize = left.tupleWidth(inputSize);
    int rightPad = right.tupleWidth(leftSize) - leftSize;
    return new TableJoinCursor(cursor, leftSize, rightPad);
  }

  @Override
  public int tupleWidth(int initSize) {
    return left.tupleWidth(initSize) + right.tupleWidth(initSize) - initSize;
  }

  public Reference group() {
    return pos -> groupVar = pos;
  }
}
