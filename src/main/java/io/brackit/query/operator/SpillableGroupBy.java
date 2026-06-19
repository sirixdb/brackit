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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.compiler.translator.Reference;
import io.brackit.query.util.Cfg;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.aggregator.Grouping;
import io.brackit.query.util.sort.TupleSerializer;

/**
 * Hash-based GroupBy operator that bounds its memory footprint by spilling raw
 * tuples to disk when the in-memory hash table exceeds a configurable budget.
 * <p>
 * Strategy (a radix hash-aggregation spill, in the spirit of DuckDB / PostgreSQL
 * {@code HashAgg}):
 * <ol>
 * <li>Aggregate into an in-memory hash table, tracking its estimated size via
 * {@link TupleSerializer#estimateSize}.</li>
 * <li>A tuple whose group is <em>already</em> in the table is always folded in —
 * that costs no new memory. Once the table reaches the budget, a tuple carrying a
 * <em>new</em> group is instead appended, raw, to one of {@value #NUM_PARTITIONS}
 * partition files chosen by a hash of its grouping key. A given group therefore
 * lives entirely in memory or entirely in a single partition, never split.</li>
 * <li>After the input is drained, the complete in-memory groups are emitted, then
 * each non-empty partition is re-aggregated, one at a time, by the same routine.</li>
 * <li>If a single partition still does not fit, its re-aggregation spills again to
 * child partitions under a depth-rotated hash, so the bits that select the partition
 * differ at each level. Recursion is depth-first and is capped at
 * {@value #MAX_SPILL_DEPTH} levels (a termination guard against pathological
 * hash-code collisions); beyond the cap a partition is aggregated in memory.</li>
 * </ol>
 * If nothing ever exceeds the budget, no file is created and the operator behaves
 * exactly like an in-memory hash group-by.
 */
public class SpillableGroupBy extends Check implements Operator {

  /** System property overriding the per-operator memory budget (bytes). */
  private static final String MEMORY_BUDGET_CFG = "io.brackit.query.groupby.memory_budget";
  private static final int NUM_PARTITIONS = 64;
  /** Recursion-depth guard; only reachable under adversarial hash-code collisions. */
  private static final int MAX_SPILL_DEPTH = 16;

  final Operator in;
  final int[] groupSpecs;
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final long memoryBudget;

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential) {
    this(in,
         dftAgg,
         addAggs,
         grpSpecCnt,
         sequential,
         Cfg.asLong(MEMORY_BUDGET_CFG, Runtime.getRuntime().maxMemory() / 4));
  }

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential,
      long memoryBudget) {
    this.in = in;
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.memoryBudget = memoryBudget;
  }

  public Reference group(final int groupSpecNo) {
    return pos -> groupSpecs[groupSpecNo] = pos;
  }

  public Reference aggregate(final int addAggNo) {
    return pos -> addAggSpecs[addAggNo] = pos;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    Cursor c = in.create(ctx, tuple);
    int tupleSize = in.tupleWidth(tuple.getSize());
    return new SpillableHashGroupByCursor(c, tupleSize);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    Cursor c = in.create(ctx, buf, len);
    int tupleSize = in.tupleWidth(buf[0].getSize());
    return new SpillableHashGroupByCursor(c, tupleSize);
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize) + addAggs.length;
  }

  /** Select a partition for {@code keyHash}; the bit window varies with {@code depth}. */
  private static int partitionFor(int keyHash, int depth) {
    int rotated = Integer.rotateLeft(keyHash, (depth & 31) * 5);
    return (rotated & 0x7FFFFFFF) % NUM_PARTITIONS;
  }

  /** A spilled partition file awaiting re-aggregation, tagged with its spill depth. */
  private record Partition(File file, int depth) {
  }

  private class SpillableHashGroupByCursor implements Cursor {
    final Cursor c;
    final int tupleSize;

    /** In-memory hash table for the aggregation pass currently in progress. */
    final Map<GroupKey, Grouping> map = new LinkedHashMap<>();
    /** Estimated bytes occupied by the distinct groups currently in {@link #map}. */
    long currentSize;

    /**
     * Lookahead tuple held across calls. When an iteration-scope boundary
     * ({@code check && separate}) ends a segment, the tuple that crossed the boundary
     * is the first tuple of the next segment and is parked here until then.
     */
    Tuple next;

    /**
     * Overflow partition files for the pass in progress. A new group that would push
     * the table past the budget has its raw tuples appended here, keyed by a
     * depth-rotated hash of its grouping key, and re-aggregated in a later pass.
     * Created lazily, so a query that never overflows touches the disk not at all.
     */
    File[] spillFiles;
    OutputStream[] spillStreams;
    int spillChildDepth;

    /** Partition files awaiting a (re-)aggregation pass, processed depth-first. */
    final ArrayDeque<Partition> pending = new ArrayDeque<>();

    /** Emission iterator over the groups of the pass currently being drained. */
    Iterator<GroupKey> outputIt;

    /** Reused, single-threaded lookup probe — keeps the per-tuple path allocation-free. */
    final GroupKey probe = new GroupKey();

    SpillableHashGroupByCursor(Cursor c, int tupleSize) {
      this.c = c;
      this.tupleSize = tupleSize;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      c.open(ctx);
    }

    @Override
    public void close(QueryContext ctx) {
      cleanup();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      while (true) {
        // (1) Drain the groups of the pass currently held in memory.
        if (outputIt != null) {
          if (outputIt.hasNext()) {
            GroupKey key = outputIt.next();
            Grouping grp = map.get(key);
            outputIt.remove();
            Tuple result = grp.emit();
            grp.clear();
            return result;
          }
          outputIt = null;
          map.clear();
          currentSize = 0;
        }

        // (2) Re-aggregate the next spilled partition of the current segment (deepest first).
        Partition p = pending.pollLast();
        if (p != null) {
          aggregatePartition(p);
          finishPass();
          outputIt = map.keySet().iterator();
          continue;
        }

        // (3) The current segment is fully emitted; load the next one. This honours the
        // iteration-nesting demarcation of the legacy GroupBy (which SpillableGroupBy
        // replaces): under an enclosing iteration (check), a "dead" left-join-padded tuple
        // forms its own singleton group, and a segment ends at the first tuple that is
        // "separate" from the segment's start. The previous segment is fully drained at
        // this point (table empty, no pending partitions), so a dead tuple emits directly
        // and a fresh segment starts with clean spill state.
        Tuple t = next;
        next = null;
        if (t == null) {
          t = c.next(ctx);
        }
        if (t == null) {
          return null;
        }
        if (check && dead(t)) {
          Grouping grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
          grp.add(t);
          return grp.emit();
        }

        admitOrSpill(t, 0);
        while ((next = c.next(ctx)) != null) {
          if (check && separate(t, next)) {
            break;
          }
          admitOrSpill(next, 0);
        }
        finishPass();
        outputIt = map.keySet().iterator();
      }
    }

    /**
     * Aggregate {@code t} into the in-memory table if its group is already present
     * or the table is still within budget; otherwise append its raw tuple to the
     * overflow partition for its key, to be re-aggregated in a later pass.
     *
     * @param depth recursion depth of the current pass (0 for the upstream input)
     */
    private void admitOrSpill(Tuple t, int depth) throws QueryException {
      Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
      probe.reset(gks); // computes the key hash once; reused for lookup and partitioning
      Grouping grp = map.get(probe);
      if (grp != null) {
        grp.add(gks, t); // existing group — folds in with no new memory, no allocation
        return;
      }
      // Always admit at least one new group per pass (guarantees forward progress);
      // admit more while under budget; force in-memory once the rotation is exhausted.
      long est = TupleSerializer.estimateSize(t);
      if (map.isEmpty() || currentSize + est <= memoryBudget || depth >= MAX_SPILL_DEPTH) {
        grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
        grp.setThreadSafe(false);
        map.put(new GroupKey(gks, probe.hash), grp); // immutable key only on insert
        grp.add(gks, t);
        currentSize += est;
      } else {
        spill(t, probe.hash, depth + 1);
      }
    }

    private void spill(Tuple t, int keyHash, int childDepth) throws QueryException {
      try {
        if (spillFiles == null) {
          spillFiles = new File[NUM_PARTITIONS];
          spillStreams = new OutputStream[NUM_PARTITIONS];
          spillChildDepth = childDepth;
        }
        int part = partitionFor(keyHash, childDepth);
        if (spillStreams[part] == null) {
          File f = File.createTempFile("grp-spill-d" + childDepth + "-p" + part + "-", ".tmp");
          f.deleteOnExit();
          spillFiles[part] = f;
          spillStreams[part] = new BufferedOutputStream(new FileOutputStream(f));
        }
        TupleSerializer.write(spillStreams[part], t);
      } catch (IOException e) {
        cleanup();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    /** Read a spilled partition back and re-aggregate it into the in-memory table. */
    private void aggregatePartition(Partition p) throws QueryException {
      try (var in = new BufferedInputStream(new FileInputStream(p.file()))) {
        Tuple t;
        while ((t = TupleSerializer.read(in)) != null) {
          admitOrSpill(t, p.depth());
        }
      } catch (IOException e) {
        cleanup();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      } finally {
        p.file().delete();
      }
    }

    /** Close the pass's spill streams and queue its non-empty partitions for re-aggregation. */
    private void finishPass() {
      if (spillStreams == null) {
        return;
      }
      for (int i = 0; i < NUM_PARTITIONS; i++) {
        if (spillStreams[i] != null) {
          try {
            spillStreams[i].close();
          } catch (IOException ignored) {
          }
        }
        File f = spillFiles[i];
        if (f != null) {
          if (f.length() > 0) {
            pending.addLast(new Partition(f, spillChildDepth));
          } else {
            f.delete();
          }
        }
      }
      spillFiles = null;
      spillStreams = null;
      spillChildDepth = 0;
    }

    private void cleanup() {
      map.clear();
      currentSize = 0;
      if (spillStreams != null) {
        for (OutputStream s : spillStreams) {
          if (s != null) {
            try {
              s.close();
            } catch (IOException ignored) {
            }
          }
        }
        spillStreams = null;
      }
      if (spillFiles != null) {
        for (File f : spillFiles) {
          if (f != null) {
            f.delete();
          }
        }
        spillFiles = null;
      }
      for (Partition p : pending) {
        p.file().delete();
      }
      pending.clear();
    }
  }

  /**
   * Grouping-key wrapper for hash-table lookup. Stored keys are immutable; a single
   * mutable instance per cursor is reused as a lookup probe (via {@link #reset}) so the
   * per-tuple aggregation path allocates no key for the common existing-group hit.
   * {@code final} lets the JIT devirtualise {@link #hashCode}/{@link #equals}.
   */
  private static final class GroupKey {
    int hash;
    Atomic[] val;

    /** Immutable stored key. */
    GroupKey(Atomic[] val) {
      this(val, computeHash(val));
    }

    /** Immutable stored key reusing an already-computed hash (avoids a second pass). */
    GroupKey(Atomic[] val, int hash) {
      this.val = val;
      this.hash = hash;
    }

    /** Reusable lookup probe; call {@link #reset} before each {@code map.get}. */
    GroupKey() {
    }

    void reset(Atomic[] val) {
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
    public boolean equals(Object obj) {
      if (obj == this)
        return true;
      if (!(obj instanceof GroupKey k))
        return false;
      if (hash != k.hash || val.length != k.val.length)
        return false;
      for (int i = 0; i < val.length; i++) {
        Atomic a = val[i];
        Atomic b = k.val[i];
        if (a == b)
          continue;
        if (a == null || b == null || a.atomicCmp(b) != 0)
          return false;
      }
      return true;
    }
  }
}
