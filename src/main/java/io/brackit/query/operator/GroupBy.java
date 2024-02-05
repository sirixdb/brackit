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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.compiler.translator.Reference;
import io.brackit.query.expr.RangeExpr;
import io.brackit.query.expr.SequenceExpr;
import io.brackit.query.util.aggregator.Grouping;

/**
 * @author Sebastian Baechle
 */
@SuppressWarnings("StatementWithEmptyBody")
public class GroupBy extends Check implements Operator {
  final Operator in;
  final int[] groupSpecs; // positions of grouping variables
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final boolean sequential;

  public GroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential) {
    this.in = in;
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.sequential = sequential;
  }

  private class SequentialGroupBy implements Cursor {
    final Cursor c;
    final Grouping grp;
    Tuple next;

    public SequentialGroupBy(Cursor c, int tupleSize) {
      this.c = c;
      this.grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      c.open(ctx);
    }

    @Override
    public void close(QueryContext ctx) {
      grp.clear();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      Tuple t;
      if ((t = next) == null && (t = c.next(ctx)) == null) {
        return null;
      }
      next = null;

      // pass through
      if (check && dead(t)) {
        return grp.singleEmit(t);
      }

      grp.add(t);
      while ((next = c.next(ctx)) != null) {
        if (check && separate(t, next)) {
          break;
        }
        if (!grp.add(next)) {
          break;
        }
      }

      Tuple emit = grp.emit();
      grp.clear();
      return emit;
    }
  }

  private static class Key {
    final int hash;
    final Atomic[] val;

    Key(Atomic[] val) {
      this.val = val;
      this.hash = Arrays.hashCode(val);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      return Arrays.toString(val);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Key k) {
        for (int i = 0; i < val.length; i++) {
          Atomic a1 = val[i];
          Atomic a2 = k.val[i];
          if (a1 == null && a2 != null || a2 == null || a1.atomicCmp(a2) != 0) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }

  private class HashGroupBy implements Cursor {
    final Cursor c;
    final int tupleSize;
    final Map<Key, Grouping> map;
    Tuple next;
    Iterator<Key> it;

    public HashGroupBy(Cursor c, int tupleSize) {
      this.c = c;
      this.tupleSize = tupleSize;
      this.map = new LinkedHashMap<>();
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      c.open(ctx);
    }

    @Override
    public void close(QueryContext ctx) {
      map.clear();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      while (true) {
        // output groups
        if (it != null) {
          if (it.hasNext()) {
            Key key = it.next();
            Grouping grp = map.get(key);
            it.remove();
            return emit(grp);
          } else {
            it = null;
            map.clear();
          }
        }

        // load groups
        Tuple t;
        if ((t = next) != null || (t = c.next(ctx)) != null) {
          if (check && dead(t)) {
            if (map.isEmpty()) {
              next = null;
              Grouping grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
              grp.add(t);
              return grp.emit();
            } else {
              // keep next and output grouping map first
              it = map.keySet().iterator();
              continue;
            }
          }

          add(t);
          while ((next = c.next(ctx)) != null) {
            if (check && separate(t, next)) {
              break;
            }
            add(next);
          }
          it = map.keySet().iterator();
        } else {
          return null;
        }
      }
    }

    private void add(Tuple t) throws QueryException {
      Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
      Key key = new Key(gks);
      Grouping grp = map.get(key);
      if (grp == null) {
        grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
        map.put(key, grp);
      }
      grp.add(gks, t);
    }

    private Tuple emit(Grouping grp) throws QueryException {
      Tuple t = grp.emit();
      grp.clear();
      return t;
    }
  }

  private class AllGroupBy implements Cursor {
    final Cursor c;
    final Grouping grp;
    Tuple next;

    public AllGroupBy(Cursor c, int tupleSize) {
      this.c = c;
      this.grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      c.open(ctx);
    }

    @Override
    public void close(QueryContext ctx) {
      grp.clear();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      while (true) {
        // output groups
        if (grp.getSize() > 0) {
          Tuple emit = grp.emit();
          grp.clear();
          return emit;
        }

        // load groups
        Tuple t;
        if ((t = next) != null || (t = c.next(ctx)) != null) {
          if (check && dead(t)) {
            if (grp.getSize() == 0) {
              next = null;
              return grp.singleEmit(t);
            } else {
              // keep next and output grouping map first
              continue;
            }
          }

          grp.add(null, t);
          while ((next = c.next(ctx)) != null) {
            if (check && separate(t, next)) {
              break;
            }
            grp.add(null, next);
          }
        } else {
          return null;
        }
      }
    }
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    Cursor c = in.create(ctx, tuple);
    int tupleSize = in.tupleWidth(tuple.getSize());
    if (groupSpecs.length == 0) {
      return new AllGroupBy(c, tupleSize);
    } else if (sequential) {
      return new SequentialGroupBy(c, tupleSize);
    } else {
      return new HashGroupBy(c, tupleSize);
    }
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    Cursor c = in.create(ctx, buf, len);
    int tupleSize = in.tupleWidth(buf[0].getSize());
    if (groupSpecs.length == 0) {
      return new AllGroupBy(c, tupleSize);
    } else if (sequential) {
      return new SequentialGroupBy(c, tupleSize);
    } else {
      return new HashGroupBy(c, tupleSize);
    }
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize) + addAggs.length;
  }

  public Reference group(final int groupSpecNo) {
    return pos -> groupSpecs[groupSpecNo] = pos;
  }

  public Reference aggregate(final int addAggNo) {
    return pos -> addAggSpecs[addAggNo] = pos;
  }

  public static void main(String[] args) throws Exception {
    Start s = new Start();
    ForBind forBind = new ForBind(s, new RangeExpr(new Int32(1), new Int32(10)), false);
    ForBind forBind2 = new ForBind(forBind, new SequenceExpr(new Str("a"), new Str("b"), new Str("c")), false);
    forBind.bindVariable(true);
    GroupBy groupBy = new GroupBy(forBind2,
                                  Aggregate.SEQUENCE,
                                  new Aggregate[] { Aggregate.AVG, Aggregate.SUM },
                                  1,
                                  true);
    groupBy.group(0).setPos(1);
    Print p = new Print(groupBy, System.out);
    QueryContext ctx = new BrackitQueryContext();
    Cursor c = p.create(ctx, TupleImpl.EMPTY_TUPLE);
    c.open(ctx);
    try {
      while (c.next(ctx) != null)
        ;
    } finally {
      c.close(ctx);
    }
  }
}
