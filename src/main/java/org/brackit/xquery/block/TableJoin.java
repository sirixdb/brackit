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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.block;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

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
import org.brackit.xquery.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class TableJoin implements Block {
  final Block l;
  final Block r;
  final Block o;
  final Expr rExpr;
  final Expr lExpr;
  final boolean leftJoin;
  final Cmp cmp;
  final boolean isGCmp;
  final boolean skipSort;
  final int pad;
  int groupVar = -1;

  boolean ordRight = org.brackit.xquery.util.Cfg.asBool("org.brackit.xquery.join.loadordered", true);
  int rPermits = FJControl.PERMITS;

  public TableJoin(Cmp cmp, boolean isGCmsp, boolean leftJoin, boolean skipSort, Block l, Expr lExpr, Block r,
      Expr rExpr, Block o) {
    this.cmp = cmp;
    this.isGCmp = isGCmsp;
    this.leftJoin = leftJoin;
    this.skipSort = skipSort;
    this.l = l;
    this.r = r;
    this.o = o;
    this.rExpr = rExpr;
    this.lExpr = lExpr;
    this.pad = r.outputWidth(0) + ((o != null) ? o.outputWidth(0) : 0);
  }

  @Override
  public int outputWidth(int initSize) {
    return l.outputWidth(initSize) + pad;
  }

  public Reference group() {
    return pos -> groupVar = pos;
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    Join join = new Join();
    PartitionEnd pe = null;
    if (o != null) {
      pe = new PartitionEnd(sink);
      sink = o.create(ctx, pe);
    }
    Sink probe = new Probe(sink, pe, ctx, join);
    Sink leftIn = l.create(ctx, probe);
    return new TableJoinSink(FJControl.PERMITS, ctx, leftIn, join);
  }

  private static class Join {
    volatile MultiTypeJoinTable table;
    volatile Atomic gk;
  }

  private final class TableJoinSink extends SerialSink {
    final QueryContext ctx;
    final Join join;
    Sink sink;

    public TableJoinSink(int permits, QueryContext ctx, Sink sink, Join join) {
      super(permits);
      this.ctx = ctx;
      this.sink = sink;
      this.join = join;
    }

    public TableJoinSink(Semaphore sem, QueryContext ctx, Sink sink, Join join) {
      super(sem);
      this.ctx = ctx;
      this.sink = sink;
      this.join = join;
    }

    @Override
    protected ChainedSink doFork() {
      return new TableJoinSink(sem, ctx, sink.fork(), join);
    }

    @Override
    protected ChainedSink doPartition(Sink stopAt) {
      return new TableJoinSink(sem, ctx, sink.partition(stopAt), join);
    }

    @Override
    protected void setPending(Tuple[] buf, int len) throws QueryException {
      output(buf, len, false);
    }

    @Override
    protected void doOutput(Tuple[] buf, int len) throws QueryException {
      output(buf, len, true);
    }

    private void output(Tuple[] buf, int len, boolean hasToken) throws QueryException {
      int end = 0;
      while (end < len) {
        int start = end;
        end = probeSize(buf, len, end);
        if (start >= end) {
          if (hasToken) {
            // load table with first tuple in probe window
            Tuple t = buf[start];
            System.out.println("START LOAD");
            load(t);
            System.out.println("END LOAD");
            end = start;
            continue;
          } else {
            Tuple[] remaining = Arrays.copyOfRange(buf, start, len);
            super.setPending(remaining, len - start);
            return;
          }
        }
        probe(Arrays.copyOfRange(buf, start, end));
      }
    }

    private void probe(Tuple[] buf) throws QueryException {
      Sink ss = sink;
      sink = sink.fork();
      ss.begin();
      ss.output(buf, buf.length);
      ss.end();
    }

    private void load(Tuple t) throws QueryException {
      int offset = t.getSize();
      MultiTypeJoinTable table = new MultiTypeJoinTable(cmp, isGCmp, skipSort);
      Sink load = new Load(ctx, table, offset);
      load = (ordRight) ? new SerialValve(rPermits, load) : load;
      Sink rightIn = r.create(ctx, load);
      rightIn.begin();
      try {
        rightIn.output(new Tuple[] { t }, 1);
        rightIn.end();
      } catch (QueryException e) {
        rightIn.fail();
        throw e;
      }
      join.gk = (groupVar >= 0) ? (Atomic) t.get(groupVar) : null;
      join.table = table;
    }

    private int probeSize(Tuple[] buf, int len, int end) throws QueryException {
      if (join.table == null) {
        return 0;
      }
      if (groupVar >= 0) {
        Atomic pgk = join.gk;
        Atomic gk = (Atomic) buf[end++].get(groupVar);
        if ((pgk == null) || (pgk.atomicCmp(gk) != 0)) {
          return 0; // we need to rebuild the new table
        }
        while (end < len) {
          Atomic ngk = (Atomic) buf[end].get(groupVar);
          if (ngk.atomicCmp(gk) != 0) {
            break;
          }
          end++;
        }
      } else {
        end = len;
      }
      return end;
    }

    @Override
    public void doBegin() throws QueryException {
      // do nothing
    }

    @Override
    public void doEnd() throws QueryException {
      sink.begin();
      sink.end();
    }

    @Override
    public void doFail() throws QueryException {
      sink.fail();
    }
  }

  private final class Probe implements Sink {
    final QueryContext ctx;
    final Join join;
    final Sequence[] padding;
    Sink sink;
    PartitionEnd pe;

    Probe(Sink sink, PartitionEnd pe, QueryContext ctx, Join join) {
      this.ctx = ctx;
      this.join = join;
      this.padding = new Sequence[pad];
      this.sink = sink;
      this.pe = pe;
    }

    @Override
    public Sink fork() {
      if (pe == null) {
        return new Probe(sink.fork(), null, ctx, join);
      }
      return partition(pe);
    }

    @Override
    public Sink partition(Sink stopAt) {
      if (pe == null) {
        return new Probe(sink.partition(stopAt), null, ctx, join);
      }
      Sink fork = sink.partition(stopAt);
      PartitionEnd fpe = pe.next;
      pe.next = null; // we don't need chaining anymore
      return new Probe(fork, fpe, ctx, join);
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      if (pe == null) {
        outputUnconditional(buf, len);
      } else {
        outputConditional(buf, len);
      }
    }

    private void outputUnconditional(Tuple[] buf, int len) throws QueryException {
      // fork out for future next calls
      Sink s = sink;
      sink = sink.fork();
      s.begin();
      for (int i = 0; i < len; i++) {
        Tuple t = buf[i];
        probe(t, s, s);
      }
      s.end();
    }

    private void outputConditional(Tuple[] buf, int len) throws QueryException {
      for (int i = 0; i < len; i++) {
        // create partitioned fork for future next calls
        Sink ss = sink;
        PartitionEnd spe = pe;
        sink = sink.partition(pe);
        pe = pe.next;
        spe.doBegin();
        ss.begin();
        Tuple t = buf[i];
        probe(t, ss, spe);
        ss.end();
        spe.doEnd();
      }
    }

    private void probe(Tuple t, Sink matchSink, Sink ljoinSink) throws QueryException {
      Sequence keys = (isGCmp) ? lExpr.evaluate(ctx, t) : lExpr.evaluateToItem(ctx, t);
      FastList<Sequence[]> matches = join.table.probe(keys);
      int itSize = matches.getSize();
      if (itSize > 0) {
        Tuple[] buf2 = new Tuple[itSize];
        for (int j = 0; j < itSize; j++) {
          buf2[j] = t.concat(matches.get(j));
        }
        matchSink.output(buf2, itSize);
      } else if (leftJoin) {
        Tuple[] buf2 = new Tuple[] { t.concat(padding) };
        ljoinSink.output(buf2, 1);
      }
    }

    @Override
    public void begin() throws QueryException {
      // do nothing
    }

    @Override
    public void end() throws QueryException {
      if (pe != null) {
        pe.doBegin();
      }
      sink.begin();
      sink.end();
      if (pe != null) {
        pe.doEnd();
      }
    }

    @Override
    public void fail() throws QueryException {
      sink.fail();
    }
  }

  private final class Load extends ConcurrentSink {
    final QueryContext ctx;
    final MultiTypeJoinTable table;
    final int offset;
    int pos = 1;

    Load(QueryContext ctx, MultiTypeJoinTable table, int offset) {
      this.ctx = ctx;
      this.table = table;
      this.offset = offset;
    }

    @Override
    public Sink partition(Sink stopAt) {
      return fork();
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      for (int i = 0; i < len; i++) {
        Tuple t = buf[i];
        Sequence keys = (isGCmp) ? rExpr.evaluate(ctx, t) : rExpr.evaluateToItem(ctx, t);
        if (keys != null) {
          Sequence[] tmp = t.array();
          Sequence[] bindings = Arrays.copyOfRange(tmp, offset, tmp.length);
          bindings[0] = null;
          bindings[1] = ((Node<?>) bindings[1].iterate().next()).getFirstChild().getFirstChild().getValue();
          table.add(keys, bindings, pos++);
        }
      }
    }
  }

  private static class PartitionEnd implements Sink {
    private final Sink out;
    private PartitionEnd next;

    PartitionEnd(Sink out) {
      this.out = out;
    }

    @Override
    public Sink fork() {
      return (next = new PartitionEnd(out.fork()));
    }

    @Override
    public Sink partition(Sink stopAt) {
      Sink nout = (stopAt == this) ? out.fork() : out.partition(stopAt);
      return (next = new PartitionEnd(nout));
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      out.output(buf, len);
    }

    @Override
    public void begin() throws QueryException {
      // do nothing
    }

    @Override
    public void end() throws QueryException {
      // do nothing
    }

    @Override
    public void fail() throws QueryException {
      out.fail();
    }

    void doBegin() throws QueryException {
      out.begin();
    }

    void doEnd() throws QueryException {
      out.end();
    }
  }
}