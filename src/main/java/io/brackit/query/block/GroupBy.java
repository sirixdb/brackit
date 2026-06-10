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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Str;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.util.simd.VectorOps;
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
import io.brackit.query.util.Cfg;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.aggregator.Grouping;
import io.brackit.query.util.sort.TupleSerializer;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class GroupBy implements Block {

  private static final long DEFAULT_MEMORY_BUDGET = Cfg.asLong("io.brackit.query.groupby.memory_budget",
                                                               Runtime.getRuntime().maxMemory() / 4);

  final int[] groupSpecs; // positions of grouping variables
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final boolean sequential;
  final long memoryBudget;

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

  /**
   * Grouping key with SIMD-optimized comparison for string and numeric keys.
   */
  private static class Key {
    final int hash;
    final Atomic[] val;

    Key(Atomic[] val) {
      this.val = val;
      this.hash = computeHash(val);
    }

    private static int computeHash(Atomic[] val) {
      int h = 1;
      for (Atomic a : val) {
        h = 31 * h + (a != null ? a.hashCode() : 0);
      }
      return h;
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
      if (!(obj instanceof Key k)) {
        return false;
      }

      // Fast path: hash mismatch
      if (this.hash != k.hash) {
        return false;
      }

      // Fast path: same array reference
      if (val == k.val) {
        return true;
      }

      // Length check
      if (val.length != k.val.length) {
        return false;
      }

      // Single-key optimization with SIMD
      if (val.length == 1) {
        Atomic a = val[0];
        Atomic b = k.val[0];

        if (a == b) {
          return true;
        }
        if (a == null || b == null) {
          return false;
        }

        // SIMD-optimized string comparison
        if (a instanceof Str s1 && b instanceof Str s2) {
          return VectorOps.stringEquals(s1.getUtf8Bytes(), s2.getUtf8Bytes());
        }

        // Fast path for Int64 comparison
        if (a instanceof Int64 i1 && b instanceof Int64 i2) {
          return i1.longValue() == i2.longValue();
        }

        return a.atomicCmp(b) == 0;
      }

      // General multi-key path
      for (int i = 0; i < val.length; i++) {
        Atomic a1 = val[i];
        Atomic a2 = k.val[i];
        if (a1 == a2) {
          continue; // Same reference or both null
        }
        if (a1 == null || a2 == null || a1.atomicCmp(a2) != 0) {
          return false;
        }
      }
      return true;
    }
  }

  private class HashGroupBy extends ConcurrentSink {
    final Sink sink;
    final ConcurrentHashMap<Key, Grouping> map;
    final AtomicLong currentSize = new AtomicLong();

    // Spill state
    private static final int NUM_PARTITIONS = 64;
    volatile boolean spilled;
    File[] partitionFiles;
    OutputStream[] partitionStreams;

    // Raw tuple buffer for spill correctness
    final java.util.concurrent.ConcurrentLinkedQueue<Tuple> rawTupleBuffer =
        new java.util.concurrent.ConcurrentLinkedQueue<>();

    HashGroupBy(Sink sink) {
      this.sink = sink;
      this.map = new ConcurrentHashMap<>();
    }

    public Sink partition(Sink stopAt) {
      return new HashGroupBy(sink.partition(stopAt));
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      if (spilled) {
        writeRawToPartitions(buf, len);
        return;
      }

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
        rawTupleBuffer.add(buf[i]);
        currentSize.addAndGet(TupleSerializer.estimateSize(buf[i]));
      }

      if (currentSize.get() > memoryBudget) {
        switchToSpillMode();
      }
    }

    private synchronized void switchToSpillMode() throws QueryException {
      if (spilled) {
        return;
      }
      try {
        initPartitionFiles();
        // Write ALL buffered raw tuples to partition files (includes initial batch)
        Tuple t;
        while ((t = rawTupleBuffer.poll()) != null) {
          Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
          int hash = new Key(gks).hash;
          int partition = (hash & 0x7FFFFFFF) % NUM_PARTITIONS;
          TupleSerializer.write(partitionStreams[partition], t);
        }
        map.clear();
        currentSize.set(0);
        spilled = true;
      } catch (IOException e) {
        throw new QueryException(e, io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    private void writeRawToPartitions(Tuple[] buf, int len) throws QueryException {
      try {
        for (int i = 0; i < len; i++) {
          Atomic[] gks = Grouping.groupingKeys(groupSpecs, buf[i]);
          int hash = new Key(gks).hash;
          int partition = (hash & 0x7FFFFFFF) % NUM_PARTITIONS;
          TupleSerializer.write(partitionStreams[partition], buf[i]);
        }
      } catch (IOException e) {
        throw new QueryException(e, io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    private void initPartitionFiles() throws IOException {
      if (partitionFiles == null) {
        partitionFiles = new File[NUM_PARTITIONS];
        partitionStreams = new OutputStream[NUM_PARTITIONS];
        for (int i = 0; i < NUM_PARTITIONS; i++) {
          partitionFiles[i] = File.createTempFile("blk-grp-" + i + "-", ".spill");
          partitionFiles[i].deleteOnExit();
          partitionStreams[i] = new BufferedOutputStream(new FileOutputStream(partitionFiles[i], true));
        }
      }
    }

    @Override
    protected void doEnd() throws QueryException {
      try {
        sink.begin();

        if (spilled) {
          // In spill mode, map is empty (all tuples went directly to partitions)
          map.clear();
          // Close partition streams
          for (int i = 0; i < NUM_PARTITIONS; i++) {
            if (partitionStreams[i] != null) {
              partitionStreams[i].close();
              partitionStreams[i] = null;
            }
          }
          // Process each partition: read, re-aggregate, emit
          int bufSize = 512;
          Tuple[] buf = new Tuple[bufSize];
          for (int p = 0; p < NUM_PARTITIONS; p++) {
            if (partitionFiles[p] == null || !partitionFiles[p].exists() || partitionFiles[p].length() == 0) {
              continue;
            }
            Map<Key, Grouping> partMap = new LinkedHashMap<>();
            try (var in = new BufferedInputStream(new FileInputStream(partitionFiles[p]))) {
              Tuple t;
              while ((t = TupleSerializer.read(in)) != null) {
                Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
                Key key = new Key(gks);
                Grouping grp = partMap.computeIfAbsent(key,
                                                       k -> new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs));
                grp.add(gks, t);
              }
            }
            if (!partitionFiles[p].delete()) {
              // Best-effort spill-file cleanup — nothing to do if it is already gone.
            }
            int len = 0;
            for (var entry : partMap.entrySet()) {
              buf[len++] = emit(entry.getValue());
              if (len == bufSize) {
                sink.output(buf, len);
                len = 0;
              }
            }
            if (len > 0) {
              sink.output(buf, len);
            }
            partMap.clear();
          }
        } else {
          // No spill — emit directly from in-memory map
          Iterator<Key> it = map.keySet().iterator();
          int bufSize = 512;
          Tuple[] buf = new Tuple[bufSize];
          int len = 0;
          while (it.hasNext()) {
            Key key = it.next();
            Grouping grp = map.get(key);
            it.remove();
            buf[len++] = emit(grp);
            if (len == bufSize) {
              sink.output(buf, len);
              len = 0;
            }
          }
          if (len > 0) {
            sink.output(buf, len);
          }
        }
        sink.end();
      } catch (IOException e) {
        throw new QueryException(e, io.brackit.query.ErrorCode.BIT_DYN_INT_ERROR);
      } finally {
        map.clear();
        cleanupPartitions();
      }
    }

    @Override
    protected void doFail() throws QueryException {
      sink.fail();
      map.clear();
      cleanupPartitions();
    }

    private void cleanupPartitions() {
      if (partitionStreams != null) {
        for (int i = 0; i < partitionStreams.length; i++) {
          if (partitionStreams[i] != null) {
            try {
              partitionStreams[i].close();
            } catch (IOException ignored) {
            }
            partitionStreams[i] = null;
          }
        }
      }
      if (partitionFiles != null) {
        for (File f : partitionFiles) {
          if (f != null && f.exists() && !f.delete()) {
            // Best-effort spill-file cleanup — nothing to do if it is already gone.
          }
        }
        partitionFiles = null;
      }
    }

    private Tuple emit(Grouping grp) throws QueryException {
      Tuple t = grp.emit();
      grp.clear();
      return t;
    }
  }

  public GroupBy(Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential) {
    this(dftAgg, addAggs, grpSpecCnt, sequential, DEFAULT_MEMORY_BUDGET);
  }

  public GroupBy(Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential, long memoryBudget) {
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.sequential = sequential;
    this.memoryBudget = memoryBudget;
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