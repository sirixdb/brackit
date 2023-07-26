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
package io.brackit.query.block;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.compiler.translator.Reference;
import io.brackit.query.expr.BlockExpr;
import io.brackit.query.expr.PrintExpr;
import io.brackit.query.expr.RangeExpr;
import io.brackit.query.expr.SequenceExpr;
import io.brackit.query.function.FunctionExpr;
import io.brackit.query.function.bit.Delay;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.aggregator.Grouping;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class GroupBy implements Block {

  final int[] groupSpecs; // positions of grouping variables
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final boolean sequential;

  private class SequentialGroupBy extends SerialSink {
    final Sink sink;
    final Grouping grp;

    public SequentialGroupBy(int permits, Sink sink) {
      super(permits);
      this.sink = sink;
      this.grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs);
    }

    private SequentialGroupBy(Semaphore sem, Sink sink, Grouping grp) {
      super(sem);
      this.sink = sink;
      this.grp = grp;
    }

    @Override
    protected ChainedSink doPartition(Sink stopAt) {
      Grouping grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs);
      return new SequentialGroupBy(sem, sink.partition(stopAt), grp);
    }

    @Override
    protected SerialSink doFork() {
      return new SequentialGroupBy(sem, sink, grp);
    }

    @Override
    protected void doOutput(Tuple[] buf, int len) throws QueryException {
      for (int i = 0; i < len; i++) {
        Tuple t = buf[i];
        if (!grp.add(t)) {
          outputGroup();
          grp.add(t);
        }
      }
    }

    private void outputGroup() throws QueryException {
      Tuple out = grp.emit();
      sink.output(new Tuple[] { out }, 1);
      grp.clear();
    }

    @Override
    protected void doFirstBegin() throws QueryException {
      sink.begin();
    }

    @Override
    protected void doFinalEnd() throws QueryException {
      if (grp.getSize() > 0) {
        outputGroup();
      }
      sink.end();
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
          if (((a1 == null) && (a2 != null)) || (a2 == null) || (a1.atomicCmp(a2) != 0)) {
            return false;
          }
        }
        return true;
      }
      return false;
    }
  }

  private class HashGroupBy extends ConcurrentSink {
    final Sink sink;
    final ConcurrentHashMap<Key, Grouping> map;

    HashGroupBy(Sink sink) {
      this.sink = sink;
      this.map = new ConcurrentHashMap<>();
    }

    public Sink partition(Sink stopAt) {
      return new HashGroupBy(sink.partition(stopAt));
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      for (int i = 0; i < len; i++) {
        Atomic[] gks = Grouping.groupingKeys(groupSpecs, buf[i]);
        Key key = new Key(gks);
        Grouping grp = map.get(key);
        if (grp == null) {
          grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs);
          Grouping prev = map.putIfAbsent(key, grp);
          if (prev != null) {
            grp = prev;
          }
        }
        grp.add(key.val, buf[i]);
      }
    }

    @Override
    protected void doEnd() throws QueryException {
      try {
        sink.begin();
        Iterator<Key> it = map.keySet().iterator();
        int bufSize = 20;
        Tuple[] buf = new Tuple[bufSize];
        int len = 0;
        while (it.hasNext()) {
          Key key = it.next();
          Grouping grp = map.get(key);
          it.remove();
          buf[len++] = emit(grp);
          if (len == bufSize) {
            sink.output(buf, len);
            buf = new Tuple[bufSize];
            len = 0;
          }
        }
        if (len > 0) {
          sink.output(buf, len);
        }
        sink.end();
      } finally {
        map.clear();
      }
    }

    @Override
    protected void doFail() throws QueryException {
      sink.fail();
      map.clear();
    }

    private Tuple emit(Grouping grp) throws QueryException {
      Tuple t = grp.emit();
      grp.clear();
      return t;
    }
  }

  public GroupBy(Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential) {
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.sequential = sequential;
  }

  @Override
  public int outputWidth(int initSize) {
    return initSize + addAggs.length;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    if (sequential) {
      return new SequentialGroupBy(FJControl.PERMITS, sink);
    } else {
      return new HashGroupBy(sink);
    }
  }

  public Reference group(final int groupSpecNo) {
    return pos -> groupSpecs[groupSpecNo] = pos;
  }

  public Reference aggregate(final int addAggNo) {
    return pos -> addAggSpecs[addAggNo] = pos;
  }

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 20; i++) {
      FJControl.resizePool(4);
      ForBind forBind = new ForBind(new RangeExpr(new Int32(1), new Int32(10000000)), false);
      ForBind forBind2 = new ForBind(new SequenceExpr(new Str("a"), new Str("b"), new Str("c")), false);
      forBind.bindVariable(true);
      forBind2.bindVariable(true);
      GroupBy groupBy = new GroupBy(Aggregate.SINGLE, new Aggregate[] { Aggregate.COUNT }, 1, false);
      groupBy.group(0).setPos(0);
      LetBind delay = new LetBind(new FunctionExpr(null, new Delay(), Int32.ONE));
      Block block = new BlockChain(new Block[] { forBind2, forBind, delay, groupBy });
      long start = System.currentTimeMillis();
      Sequence res = new BlockExpr(block, new PrintExpr(), true).evaluate(new BrackitQueryContext(), new TupleImpl());
      Iter it = res.iterate();
      Item item;
      int cnt = 0;
      while ((item = it.next()) != null) {
        System.out.println(item);
        cnt++;
      }
      it.close();
      System.out.println("---");
      System.out.print(cnt);
      System.out.println(" results");
      long end = System.currentTimeMillis();
      System.out.println(end - start + " ms");
    }
  }
}