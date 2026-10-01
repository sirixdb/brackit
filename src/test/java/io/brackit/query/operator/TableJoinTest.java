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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryContext;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.expr.BoundVariable;
import io.brackit.query.expr.SequenceExpr;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.Cmp;

/** Exercises the operator directly so optimizer rewrites cannot hide either width arrangement. */
public class TableJoinTest {
  private final QueryContext ctx = new BrackitQueryContext();

  private static Stream<Arguments> widthsAndInputs() {
    var cases = new ArrayList<Arguments>();
    // The right branch adds two columns. These used to choose right, left (tie), left.
    for (int leftColumns : new int[] { 1, 2, 3 }) {
      for (boolean buffered : new boolean[] { false, true }) {
        for (boolean general : new boolean[] { false, true }) {
          cases.add(Arguments.of(leftColumns, buffered, general));
        }
      }
    }
    return cases.stream();
  }

  private static Expr values(int... values) {
    Expr[] exprs = new Expr[values.length];
    for (int i = 0; i < values.length; i++) {
      exprs[i] = new Int32(values[i]);
    }
    return new SequenceExpr(exprs);
  }

  private static Expr variable(int pos) {
    return new BoundVariable(new QNm("v" + pos), pos);
  }

  private TableJoin join(int leftColumns, boolean general, boolean outer, Cmp cmp, Expr leftValues, Expr rightValues) {
    Operator left = new ForBind(new Start(), leftValues, false);
    for (int i = 1; i < leftColumns; i++) {
      left = new LetBind(left, new Str("L" + i));
    }
    Operator right = new LetBind(new ForBind(new Start(), rightValues, false), new Str("R"));
    Expr leftKey = variable(1);
    Expr rightKey = variable(1 + leftColumns);
    if (general) {
      // Multiple matching key pairs must still emit each matching row only once.
      leftKey = new SequenceExpr(leftKey, leftKey);
      rightKey = new SequenceExpr(rightKey, rightKey);
    }
    return new TableJoin(cmp, general, outer, false, left, leftKey, right, rightKey);
  }

  private List<List<String>> read(TableJoin join, boolean buffered, int... groups) {
    Tuple[] inputs = new Tuple[groups.length];
    for (int i = 0; i < groups.length; i++) {
      inputs[i] = new TupleImpl(new Int32(groups[i]));
    }
    Cursor cursor = buffered ? join.create(ctx, inputs, inputs.length) : join.create(ctx, inputs[0]);
    var result = new ArrayList<List<String>>();
    cursor.open(ctx);
    try {
      Tuple tuple;
      while ((tuple = cursor.next(ctx)) != null) {
        assertEquals(join.tupleWidth(1), tuple.getSize());
        var row = new ArrayList<String>();
        for (Sequence value : tuple.array()) {
          row.add(value == null ? null : ((Atomic) value).stringValue());
        }
        result.add(row);
      }
    } finally {
      cursor.close(ctx);
    }
    return result;
  }

  private static List<String> row(Integer group, int left, Integer right, int leftColumns) {
    var row = new ArrayList<String>();
    row.add(group == null ? null : group.toString());
    row.add(Integer.toString(left));
    for (int i = 1; i < leftColumns; i++) {
      row.add("L" + i);
    }
    row.add(right == null ? null : right.toString());
    row.add(right == null ? null : "R");
    return row;
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void equalityKeepsBindingPositionsOrderAndDuplicates(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, false, Cmp.eq, values(3, 1, 2, 1), values(1, 2, 1));
    assertEquals(List.of(row(42, 1, 1, columns),
                         row(42, 1, 1, columns),
                         row(42, 2, 2, columns),
                         row(42, 1, 1, columns),
                         row(42, 1, 1, columns)), read(join, buffered, 42));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void outerJoinPadsUnmatchedLeftRows(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(3, 1, 2, 4), values(1, 2, 1));
    assertEquals(List.of(row(42, 3, null, columns),
                         row(42, 1, 1, columns),
                         row(42, 1, 1, columns),
                         row(42, 2, 2, columns),
                         row(42, 4, null, columns)), read(join, buffered, 42));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void emptyRightStillPreservesEveryLeftRow(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(3, 1), values());
    assertEquals(List.of(row(42, 3, null, columns), row(42, 1, null, columns)), read(join, buffered, 42));
    assertEquals(List.of(), read(join(columns, general, false, Cmp.eq, values(3, 1), values()), buffered, 42));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void emptyLeftProducesNoRows(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(), values(1, 2));
    assertEquals(List.of(), read(join, buffered, 42));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void orderedComparisonsKeepOperandDirectionAndBoundaries(int columns, boolean buffered, boolean general) {
    for (Cmp cmp : new Cmp[] { Cmp.lt, Cmp.le, Cmp.gt, Cmp.ge }) {
      for (int[] rightValues : new int[][] { {}, { 2 }, { 2, 2 }, { 3, 1, 2, 1 } }) {
        var expected = new ArrayList<List<String>>();
        for (int left : new int[] { 0, 1, 2, 3, 4 }) {
          for (int right : rightValues) {
            boolean match = switch (cmp) {
              case lt -> left < right;
              case le -> left <= right;
              case gt -> left > right;
              case ge -> left >= right;
              default -> throw new AssertionError(cmp);
            };
            if (match) {
              expected.add(row(42, left, right, columns));
            }
          }
        }
        var join = join(columns, general, false, cmp, values(0, 1, 2, 3, 4), values(rightValues));
        assertEquals(expected, read(join, buffered, 42), cmp.toString());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void groupChangesRebuildTheCorrelatedTable(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(1, 2), variable(0));
    join.group().setPos(0);
    var expected = new ArrayList<List<String>>();
    int[] groups = buffered ? new int[] { 1, 2, 1 } : new int[] { 1 };
    for (int group : groups) {
      expected.add(row(group, 1, group == 1 ? 1 : null, columns));
      expected.add(row(group, 2, group == 2 ? 2 : null, columns));
    }
    assertEquals(expected, read(join, buffered, groups));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void checkedOuterJoinEmitsOnlyOneDeadRowForAnUnmatchedGroup(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(3, 4), values(1, 2));
    join.check().setPos(0);
    assertEquals(List.of(row(null, 4, null, columns)), read(join, buffered, 42));
  }

  @ParameterizedTest
  @MethodSource("widthsAndInputs")
  public void checkedOuterJoinDoesNotAddDeadRowsToAMatchedGroup(int columns, boolean buffered, boolean general) {
    var join = join(columns, general, true, Cmp.eq, values(3, 1, 4), values(1, 2, 1));
    join.check().setPos(0);
    assertEquals(List.of(row(42, 1, 1, columns), row(42, 1, 1, columns)), read(join, buffered, 42));
  }
}
