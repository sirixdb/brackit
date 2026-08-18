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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.translator.Reference;
import io.brackit.query.util.Cfg;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.aggregator.Grouping;
import io.brackit.query.util.log.Logger;
import io.brackit.query.util.sort.TupleSerializer;
import io.brackit.query.util.sort.TupleSerializer.SpillContext;

/**
 * Hash-based GroupBy operator that keeps the group table inside a memory budget by spilling the
 * tuples of the groups that do not fit to partition files on disk.
 * <p>
 * The group table is the accumulator: one map entry, one {@link Grouping} and one aggregator per
 * binding position per <em>distinct group</em>, and every SINGLE aggregator pins the first item it
 * saw. Over a high-cardinality grouping key that is hundreds of bytes times tens of millions of
 * groups, so a table that is allowed to grow freely ends the process.
 * <p>
 * Strategy:
 * <ol>
 * <li>Admit new groups into the in-memory table while its estimated retained size stays below
 * {@code io.brackit.query.groupby.memory_budget} (default: a quarter of {@code -Xmx}), and while
 * the heap-pressure backstop stays quiet.</li>
 * <li>Once the budget is reached, stop admitting <em>new</em> groups: their tuples are written to
 * one of {@value #NUM_PARTITIONS} partition files chosen by the group key's hash. Tuples of groups
 * that are already resident keep aggregating in memory. A group is therefore either wholly
 * resident or wholly on disk — no partial aggregate ever needs to be merged.</li>
 * <li>When the input is exhausted, emit the resident groups, then re-aggregate one spilled
 * partition at a time under the same budget. A partition that does not fit either spills again,
 * repartitioned with the next level's hash; since a pass always admits at least one group, the
 * remaining work shrinks strictly and the recursion terminates.</li>
 * </ol>
 * Nothing is written while the table fits: the partition files are created on the first overflow,
 * so a group-by that fits in memory pays one comparison per new group and no I/O at all.
 * <p>
 * A spilled tuple round-trips through {@link TupleSerializer}, which renders items it has no
 * binary form for as JSON text. Such an item comes back value-equal but as a fresh in-memory item,
 * so node identity does not survive a spill.
 *
 * @author Sebastian Baechle
 */
public class SpillableGroupBy extends Check implements Operator {

  private static final Logger log = Logger.getLogger(SpillableGroupBy.class);

  /** Config key of the in-memory budget for the group table, in bytes. */
  public static final String MEMORY_BUDGET_CFG = "io.brackit.query.groupby.memory_budget";

  private static final int NUM_PARTITIONS = 64;

  private static final int PARTITION_MASK = NUM_PARTITIONS - 1;

  private static final int IO_BUFFER_BYTES = 64 * 1024;

  /** New-group admissions between two heap-pressure probes. A power of two: it is masked, not divided. */
  private static final int HEAP_PROBE_INTERVAL = 8192;

  /** Percentage of the maximum heap above which the backstop stops admitting new groups. */
  private static final long HEAP_PRESSURE_PERCENT = 80;

  /** The backstop never trips below this estimate — a small group table is never the problem. */
  private static final long HEAP_PRESSURE_FLOOR_BYTES = 32L * 1024 * 1024;

  /**
   * Retained heap of one resident group without its key: the map entry, the {@link GroupKey}, the
   * {@link Grouping} shell and the fixed part of its three per-position arrays.
   */
  private static final long GROUP_BASE_BYTES = 128;

  /**
   * Retained heap a resident group adds per binding position: three array slots, the aggregator
   * object, and an allowance for the item a SINGLE aggregator pins. Deliberately generous — a
   * pinned record item measured ~350 bytes (object, its field hash map and the map's nodes) in the
   * group table this budget exists for, and overshooting the estimate only spills earlier, while
   * undershooting it is the bug this class had.
   */
  private static final long GROUP_BYTES_PER_POSITION = 112;

  /** Retained heap of one grouping-key atomic that is not a string. */
  private static final long KEY_ATOM_BYTES = 32;

  /** Retained heap of a string grouping key, apart from its characters. */
  private static final long KEY_STRING_BYTES = 48;

  final Operator in;
  final int[] groupSpecs;
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final long memoryBudget;

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential) {
    this(in, dftAgg, addAggs, grpSpecCnt, sequential, defaultMemoryBudget());
  }

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, boolean sequential,
      long memoryBudget) {
    if (memoryBudget <= 0) {
      throw new IllegalArgumentException("memory budget must be positive: " + memoryBudget);
    }
    this.in = in;
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.memoryBudget = memoryBudget;
  }

  /**
   * The configured group-table budget, read per operator so that a system property set after this
   * class was loaded still takes effect.
   */
  public static long defaultMemoryBudget() {
    return Cfg.asLong(MEMORY_BUDGET_CFG, Runtime.getRuntime().maxMemory() / 4);
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

  /** A partition file waiting to be re-aggregated, and the level whose hash split it. */
  private record SpillFile(File file, int level) {
  }

  class SpillableHashGroupByCursor implements Cursor {
    final Cursor c;
    final int tupleSize;

    /** Estimated retained heap of one resident group, apart from its key. */
    final long groupBytes;

    // In-memory hash table of the pass in flight
    final Map<GroupKey, Grouping> map = new LinkedHashMap<>();
    long residentBytes;
    boolean admitting = true;
    int admissionsSinceProbe;

    /** Partition writer of the pass in flight; null while everything fits. */
    PartitionWriter writer;

    /** Spilled partitions still to be re-aggregated, oldest first. */
    final Deque<SpillFile> pending = new ArrayDeque<>();

    /**
     * Keeps the columns that are too large to copy alive for as long as a partition file may
     * still reference them — that is, for the life of the cursor.
     */
    final SpillContext spillCtx = new SpillContext();

    /** Repartitioning level of the pass in flight; reading the operator's input is level 0. */
    int passLevel;

    /** Tuples written to a partition file, over all passes. Zero for a grouping that fit. */
    long spilledTuples;

    boolean reportedSpill;

    // Output iteration
    Iterator<GroupKey> outputIt;
    Tuple next;

    SpillableHashGroupByCursor(Cursor c, int tupleSize) {
      this.c = c;
      this.tupleSize = tupleSize;
      this.groupBytes = GROUP_BASE_BYTES + (long) (tupleSize + addAggs.length) * GROUP_BYTES_PER_POSITION;
    }

    @Override
    public void open(QueryContext ctx) throws QueryException {
      c.open(ctx);
    }

    @Override
    public void close(QueryContext ctx) {
      clearResident();
      discardSpillFiles();
      spillCtx.clear();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      while (true) {
        // Phase 1: Output the groups of the pass in flight
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
          clearResident();
        }

        // Phase 2: Groups that did not fit are on disk — re-aggregate one partition per pass
        if (writer != null || !pending.isEmpty()) {
          collectSpillFiles();
          if (loadNextPartition()) {
            continue;
          }
        }

        // Phase 3: Read input, respecting check/dead/separate semantics
        Tuple t;
        if ((t = next) != null || (t = c.next(ctx)) != null) {
          // Dead tuple pass-through (matches GroupBy.HashGroupBy behavior)
          if (check && dead(t)) {
            if (map.isEmpty()) {
              next = null;
              Grouping grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
              grp.add(t);
              return grp.emit();
            } else {
              // Output accumulated groups first, keep this tuple for next call
              outputIt = map.keySet().iterator();
              continue;
            }
          }

          addOrSpill(t);

          // Read remaining tuples for this group segment
          while ((next = c.next(ctx)) != null) {
            if (check && separate(t, next)) {
              break;
            }
            addOrSpill(next);
          }

          // Resident groups go out first; whatever spilled is drained by phase 2 afterwards.
          outputIt = map.keySet().iterator();
        } else {
          return null;
        }
      }
    }

    /**
     * Aggregate a tuple into its group. A group that is already resident always aggregates in
     * memory; a new group is admitted while the budget allows and its tuples go to a partition
     * file once it does not.
     */
    private void addOrSpill(Tuple t) throws QueryException {
      final Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
      final GroupKey key = new GroupKey(gks);
      final Grouping grp = map.get(key);
      if (grp != null) {
        grp.add(gks, t);
        return;
      }
      if (canAdmit()) {
        final Grouping fresh = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
        fresh.setThreadSafe(false);
        map.put(key, fresh);
        residentBytes += groupBytes + estimateKeyBytes(gks);
        fresh.add(gks, t);
        return;
      }
      writeToPartition(t, key.hash);
    }

    /** Whether one more group fits into the in-memory table of the pass in flight. */
    private boolean canAdmit() {
      if (!admitting) {
        return false;
      }
      if (map.isEmpty()) {
        // One group always fits. This is what makes the partition passes terminate: every pass
        // takes at least one group out of the remaining work.
        return true;
      }
      if (residentBytes >= memoryBudget) {
        stopAdmitting("group table reached its budget of " + memoryBudget + " bytes");
        return false;
      }
      if ((++admissionsSinceProbe & (HEAP_PROBE_INTERVAL - 1)) == 0 && residentBytes >= HEAP_PRESSURE_FLOOR_BYTES
          && heapUnderPressure()) {
        // The per-group estimate cannot know what an aggregator pins, so the heap itself has the
        // last word: spilling early is slow, running out of heap is fatal.
        stopAdmitting("heap use passed " + HEAP_PRESSURE_PERCENT + "% of the maximum");
        return false;
      }
      return true;
    }

    private void stopAdmitting(String reason) {
      admitting = false;
      if (!reportedSpill) {
        reportedSpill = true;
        if (log.isInfoEnabled()) {
          log.info("group-by spilling to disk: " + reason + " with " + map.size() + " groups resident");
        }
      }
    }

    /**
     * Writes a tuple of a group that did not fit to its partition file.
     * <p>
     * BEHAVIOUR BOUNDARY, for whoever consumes the groups this operator emits: a spilled tuple
     * comes back through {@link TupleSerializer}, so a column it has no binary form for is
     * rendered as JSON text and re-parsed. Such a column returns <em>value-equal but not
     * identical</em> — node identity does not survive a spill, and a group whose tuples spilled
     * therefore emits a fresh in-memory item where a resident group emits the stored node.
     * (Columns too large to copy are the exception: they are passed by reference through
     * {@code spillCtx} and do keep their identity.) Nothing downstream of a {@code group by} may
     * rely on identity or on document order of the non-grouping columns.
     */
    private void writeToPartition(Tuple t, int hash) throws QueryException {
      try {
        if (writer == null) {
          writer = new PartitionWriter(passLevel + 1);
        }
        writer.write(t, hash, spillCtx);
        spilledTuples++;
      } catch (IOException e) {
        discardSpillFiles();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    /**
     * How many tuples this grouping had to write to disk. Zero means the group table stayed
     * within its budget and no partition file was ever created.
     */
    long spilledTuples() {
      return spilledTuples;
    }

    /** Close the pass's partition files and queue the non-empty ones for re-aggregation. */
    private void collectSpillFiles() throws QueryException {
      final PartitionWriter w = writer;
      if (w == null) {
        return;
      }
      writer = null;
      try {
        w.close();
      } catch (IOException e) {
        w.deleteFiles();
        discardSpillFiles();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      }
      w.collectInto(pending);
    }

    /**
     * Re-aggregate the next spilled partition into the in-memory table and set up its output.
     * Returns {@code false} once no spilled work is left.
     */
    private boolean loadNextPartition() throws QueryException {
      while (!pending.isEmpty()) {
        final SpillFile part = pending.poll();
        passLevel = part.level();
        clearResident();

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(part.file()),
                                                                              IO_BUFFER_BYTES))) {
          Tuple t;
          while ((t = TupleSerializer.read(in, spillCtx)) != null) {
            addOrSpill(t);
          }
        } catch (IOException e) {
          discardSpillFiles();
          throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
        }
        if (!part.file().delete()) {
          // Best-effort spill-file cleanup — nothing to do if it is already gone.
        }

        // Whatever did not fit into this pass becomes work of the next level.
        collectSpillFiles();

        if (!map.isEmpty()) {
          outputIt = map.keySet().iterator();
          return true;
        }
      }
      passLevel = 0;
      return false;
    }

    private void clearResident() {
      map.clear();
      residentBytes = 0;
      admitting = true;
      admissionsSinceProbe = 0;
    }

    private void discardSpillFiles() {
      final PartitionWriter w = writer;
      writer = null;
      if (w != null) {
        try {
          w.close();
        } catch (IOException ignored) {
          // Best-effort: the files are deleted right below anyway.
        }
        w.deleteFiles();
      }
      SpillFile part;
      while ((part = pending.poll()) != null) {
        if (!part.file().delete()) {
          // Best-effort spill-file cleanup — nothing to do if it is already gone.
        }
      }
    }
  }

  /** Writes overflow tuples of one pass into {@value #NUM_PARTITIONS} hash partitions. */
  private static final class PartitionWriter {
    private final int level;
    private final File[] files = new File[NUM_PARTITIONS];
    private final DataOutputStream[] streams = new DataOutputStream[NUM_PARTITIONS];

    PartitionWriter(int level) {
      this.level = level;
    }

    void write(Tuple t, int hash, SpillContext ctx) throws IOException {
      final int partition = partitionOf(hash, level);
      DataOutputStream out = streams[partition];
      if (out == null) {
        final File file = File.createTempFile("grp-part-" + partition + "-", ".spill");
        file.deleteOnExit();
        files[partition] = file;
        out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), IO_BUFFER_BYTES));
        streams[partition] = out;
      }
      TupleSerializer.write(out, t, ctx);
    }

    void close() throws IOException {
      IOException failure = null;
      for (int i = 0; i < streams.length; i++) {
        final DataOutputStream out = streams[i];
        if (out != null) {
          streams[i] = null;
          try {
            out.close();
          } catch (IOException e) {
            failure = e;
          }
        }
      }
      if (failure != null) {
        throw failure;
      }
    }

    /** Hand the written partitions over as pending work; empty ones are deleted right away. */
    void collectInto(Deque<SpillFile> pending) {
      for (int i = 0; i < files.length; i++) {
        final File file = files[i];
        if (file == null) {
          continue;
        }
        files[i] = null;
        if (file.length() == 0) {
          if (!file.delete()) {
            // Best-effort spill-file cleanup — nothing to do if it is already gone.
          }
        } else {
          pending.add(new SpillFile(file, level));
        }
      }
    }

    void deleteFiles() {
      for (int i = 0; i < files.length; i++) {
        final File file = files[i];
        if (file != null) {
          files[i] = null;
          if (!file.delete()) {
            // Best-effort spill-file cleanup — nothing to do if it is already gone.
          }
        }
      }
    }
  }

  /**
   * Spread the group-key hash over the partitions, mixing in the level so that a partition which
   * has to spill again splits differently than the pass that wrote it.
   */
  private static int partitionOf(int hash, int level) {
    int h = hash ^ (level * 0x9E3779B9);
    h ^= h >>> 16;
    h *= 0x85EBCA6B;
    h ^= h >>> 13;
    return h & PARTITION_MASK;
  }

  private static boolean heapUnderPressure() {
    final Runtime runtime = Runtime.getRuntime();
    final long max = runtime.maxMemory();
    if (max == Long.MAX_VALUE) {
      return false; // no bound configured, so there is nothing to stay below
    }
    return runtime.totalMemory() - runtime.freeMemory() > max / 100L * HEAP_PRESSURE_PERCENT;
  }

  /** Estimated retained heap of one grouping key. */
  private static long estimateKeyBytes(Atomic[] gks) {
    long bytes = 16 + 8L * gks.length; // the Atomic[] itself
    for (final Atomic a : gks) {
      if (a == null) {
        continue;
      }
      bytes += (a instanceof Str s) ? KEY_STRING_BYTES + s.stringValue().length() : KEY_ATOM_BYTES;
    }
    return bytes;
  }

  /**
   * Grouping key wrapper for hash map lookup.
   */
  private static class GroupKey {
    final int hash;
    final Atomic[] val;

    GroupKey(Atomic[] val) {
      this.val = val;
      int h = 1;
      for (Atomic a : val) {
        h = 31 * h + (a != null ? a.hashCode() : 0);
      }
      this.hash = h;
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
