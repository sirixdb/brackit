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
package org.brackit.xquery.expr;

import org.brackit.xquery.*;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.Record;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastian Baechle
 */
public class DerefExpr implements Expr {

  final Expr record;
  final Expr[] fields;

  public DerefExpr(Expr record, Expr[] fields) {
    this.record = record;
    this.fields = fields;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    final var sequence = record.evaluate(ctx, tuple);

    for (int index = 0; index < fields.length && sequence != null; index++) {
      final var resultSequence = processSequence(ctx, tuple, sequence, index);

      if (resultSequence != null) {
        return resultSequence;
      }
    }

    return null;
  }

  private Sequence processSequence(QueryContext ctx, Tuple tuple, Sequence sequence, int index) {
    if (sequence instanceof Array) {
      return processArray(ctx, tuple, getSequenceValues(ctx, tuple, (Array) sequence, fields[index]));
    } else if (sequence instanceof Record) {
      return processRecord(sequence, index, ctx, tuple);
    } else if (sequence instanceof LazySequence) {
      return processLazySequence(ctx, tuple, sequence, index);
    }
    return null;
  }

  private Sequence processLazySequence(QueryContext ctx, Tuple tuple, Sequence sequence, int index) {
    return new LazySequence() {
      @Override
      public Iter iterate() {
        Iter iter = sequence.iterate();

        return new BaseIter() {
          Iter nestedIter;

          @Override
          public Item next() {
            Item item = null;
            if (nestedIter != null) {
              item = nextItem(nestedIter);
            }
            if (item == null) {
              item = nextItem(iter);
            }

            return item;
          }

          private Item nextItem(Iter iter) {
            Item item;
            while ((item = iter.next()) != null) {
              if (iter == nestedIter) {
                return item;
              }
              var resultItem = processSequence(ctx, tuple, item, index);

              if (resultItem == null) {
                continue;
              }

              if (resultItem instanceof LazySequence) {
                nestedIter = resultItem.iterate();
                resultItem = next();

                if (resultItem == null) {
                  continue;
                }
              }

              return resultItem.evaluateToItem(ctx, tuple);
            }

            return null;
          }

          @Override
          public void close() {
          }
        };
      }
    };
  }

  private Sequence processArray(QueryContext ctx, Tuple tuple, final List<Sequence> values) {
    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          int i;

          @Override
          public Item next() {
            if (i < values.size()) {
              return values.get(i++).evaluateToItem(ctx, tuple);
            }
            return null;
          }

          @Override
          public void close() {
          }
        };
      }
    };
  }

  private Sequence processRecord(Sequence sequence, int index, QueryContext ctx, Tuple tuple) {
    final Record record = (Record) sequence;
    final Item field = fields[index].evaluateToItem(ctx, tuple);

    if (field == null) {
      return null;
    }

    return getSequenceByRecordField(record, field);
  }

  private List<Sequence> getSequenceValues(QueryContext ctx, Tuple t, Array sequence, Expr field1) {
    // TODO: Think about if it makes sense to get the result sequence with an iterator instead of materialize everything

    final var vals = new ArrayList<Sequence>();
    for (Sequence value : sequence.values()) {
      Sequence val = value.evaluate(ctx, t);
      if (val instanceof Array) {
        vals.addAll(getSequenceValues(ctx, t, (Array) val, field1));
        continue;
      }
      if (!(val instanceof Record)) {
        continue;
      }
      Record record = (Record) val;
      Item field = field1.evaluateToItem(ctx, t);
      if (field == null) {
        continue;
      }
      final var sequenceByRecordField = getSequenceByRecordField(record, field);
      if (sequenceByRecordField != null) {
        vals.add(sequenceByRecordField);
      }
    }
    return vals;
  }

  private Sequence getSequenceByRecordField(Record record, Item field) {
    Sequence sequence;
    if (field instanceof QNm) {
      sequence = record.get((QNm) field);
    } else if (field instanceof IntNumeric) {
      sequence = record.value((IntNumeric) field);
    } else {
      throw new QueryException(Bits.BIT_ILLEGAL_RECORD_FIELD, "Illegal record field reference: %s", field);
    }
    return sequence;
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    return ExprUtil.asItem(evaluate(ctx, tuple));
  }

  @Override
  public boolean isUpdating() {
    if (record.isUpdating()) {
      return true;
    }
    for (Expr f : fields) {
      if (f.isUpdating()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    StringBuilder s = new StringBuilder();
    for (Expr f : fields) {
      s.append("=>");
      s.append(f);
    }
    return s.toString();
  }

  public static void main(String[] args) throws QueryException {
    // a:1, b:2, c:3 , {x:1}, d:5,
    new XQuery("let $n := <x><y>yval</y></x> return { \"e\" : { \"m\": \"mvalue\", \"n\":$n}}=>e=>n/y").serialize(new BrackitQueryContext(),
                                                                                                                  System.out);
  }
}
