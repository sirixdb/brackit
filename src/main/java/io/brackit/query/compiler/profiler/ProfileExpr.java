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
package io.brackit.query.compiler.profiler;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.AbstractSequence;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.util.dot.DotNode;

/**
 * @author Sebastian Baechle
 */
public class ProfileExpr extends ProfilingNode implements Expr {
  private Expr e;

  private long itemTotal;

  private int itemCnt;

  private long seqTotal;

  private int seqCnt;

  private int seqIterCnt;

  private long seqIterTotal;

  private int seqDeliverCnt;

  private int seqSkipCnt;

  private long seqSkipTotal;

  private int evalBooleanValue;

  private int evalSize;

  private int evalGet;

  private class StatIter implements Iter {
    final Iter it;
    int delivered;
    int skipCnt;
    long time;
    long skipTime;

    StatIter(Iter it) {
      this.it = it;
    }

    public Item next() throws QueryException {
      long start = System.nanoTime();
      Item item = it.next();
      long end = System.nanoTime();
      if (item != null) {
        delivered++;
      }
      time += (end - start);
      return item;
    }

    @Override
    public void skip(IntNumeric i) throws QueryException {
      long start = System.nanoTime();
      it.skip(i);
      long end = System.nanoTime();
      skipTime += (end - start);
      skipCnt++;
    }

    public void close() {
      it.close();
      seqIterTotal += time;
      seqDeliverCnt += delivered;
      seqSkipCnt += skipCnt;
      seqSkipTotal += skipTime;
    }

    @Override
    public Split split(int min, int max) throws QueryException {
      // TODO Auto-generated method stub
      return null;
    }
  }

  private class StatSequence extends AbstractSequence {
    final Sequence s;

    public StatSequence(Sequence s) {
      this.s = s;
    }

    @Override
    public boolean booleanValue() throws QueryException {
      evalBooleanValue++;
      return s != null && s.booleanValue();
    }

    @Override
    public Iter iterate() {
      seqIterCnt++;
      return (s != null) ? new StatIter(s.iterate()) : new StatIter(new BaseIter() {
        @Override
        public void close() {
        }

        @Override
        public Item next() throws QueryException {
          return null;
        }
      });
    }

    @Override
    public IntNumeric size() throws QueryException {
      evalSize++;
      return (s != null) ? s.size() : Int32.ZERO;
    }

    @Override
    public Item get(IntNumeric pos) throws QueryException {
      evalGet++;
      return (s != null) ? s.get(pos) : null;
    }

  }

  ProfileExpr() {
  }

  void setExpr(Expr e) {
    this.e = e;
  }

  @Override
  protected void addFields(DotNode node) {
    node.addRow("expression", e.toString());
    node.addRow("eval (item)", itemCnt);
    node.addRow("total / avg. time eval (item) [ms]", itemTotal / 1000000);
    node.addRow("avg. time eval (item) [ms]", (itemCnt > 0) ? (double) itemTotal / ((double) 1000000 * itemCnt) : -1);
    node.addRow("eval (seq)", seqCnt);
    node.addRow("total time eval (seq) [ms]", seqTotal / 1000000);
    node.addRow("avg. time eval (seq) [ms]", (seqCnt > 0) ? (double) seqTotal / ((double) 1000000 * seqCnt) : -1);
    node.addRow("eval bool  (seq)", evalBooleanValue);
    node.addRow("eval size (seq)", evalSize);
    node.addRow("eval get (seq)", evalGet);
    node.addRow("iter (seq)", seqIterCnt);
    node.addRow("skip (seq)", seqSkipCnt);
    node.addRow("delivered by iter (seq)", seqDeliverCnt);
    node.addRow("total time iter (seq) [ms]", seqIterTotal / 1000000);
    node.addRow("avg. time iter (seq) [ms]",
                (seqIterCnt > 0) ? (double) seqIterTotal / ((double) 1000000 * seqIterCnt) : -1);
    node.addRow("total time skip (seq) [ms]", seqSkipTotal / 1000000);
  }

  @Override
  protected String getName() {
    return e.getClass().getSimpleName() + "_" + id;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
    long start = System.nanoTime();
    Sequence s = e.evaluate(ctx, tuple);
    long end = System.nanoTime();
    seqTotal += (end - start);
    seqCnt++;
    return new StatSequence(s);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
    long start = System.nanoTime();
    Item i = e.evaluateToItem(ctx, tuple);
    long end = System.nanoTime();
    itemTotal += (end - start);
    itemCnt++;
    return i;
  }

  public String toString() {
    return e.getClass().getSimpleName();
  }

  @Override
  public boolean isUpdating() {
    return e.isUpdating();
  }

  @Override
  public boolean isVacuous() {
    return e.isVacuous();
  }
}