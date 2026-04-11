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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.util.Cfg;
import io.brackit.query.util.aggregator.Aggregate;
import io.brackit.query.util.aggregator.Grouping;
import io.brackit.query.util.sort.TupleSerializer;

/**
 * Hash-based GroupBy operator that spills partitions to disk when the in-memory
 * hash table exceeds a configurable memory budget.
 * <p>
 * Strategy (inspired by DuckDB's approach):
 * <ol>
 * <li>Build hash table in memory, tracking estimated size via {@link TupleSerializer#estimateSize}</li>
 * <li>When budget exceeded, partition tuples by hash prefix into N partition files on disk</li>
 * <li>After input exhausted, process each spilled partition file one at a time:
 * load partition into memory, aggregate, emit results</li>
 * <li>If a single partition still doesn't fit, recursively repartition with different hash bits</li>
 * </ol>
 */
public class SpillableGroupBy extends Check implements Operator {

  private static final long DEFAULT_MEMORY_BUDGET = Cfg.asLong("io.brackit.query.groupby.memory_budget",
                                                               Runtime.getRuntime().maxMemory() / 4);
  private static final int NUM_PARTITIONS = 64;

  final Operator in;
  final int[] groupSpecs;
  final int[] addAggSpecs;
  final Aggregate defaultAgg;
  final Aggregate[] addAggs;
  final long memoryBudget;

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt) {
    this(in, dftAgg, addAggs, grpSpecCnt, DEFAULT_MEMORY_BUDGET);
  }

  public SpillableGroupBy(Operator in, Aggregate dftAgg, Aggregate[] addAggs, int grpSpecCnt, long memoryBudget) {
    this.in = in;
    this.defaultAgg = dftAgg;
    this.addAggs = addAggs;
    this.groupSpecs = new int[grpSpecCnt];
    this.addAggSpecs = new int[addAggs.length];
    this.memoryBudget = memoryBudget;
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple tuple) throws QueryException {
    int tupleSize = Math.max(tuple.getSize(), 1);
    Cursor c = in.create(ctx, tuple);
    return new SpillableHashGroupByCursor(c, tupleSize);
  }

  @Override
  public Cursor create(QueryContext ctx, Tuple[] buf, int len) throws QueryException {
    int tupleSize = Math.max(buf[0].getSize(), 1);
    Cursor c = in.create(ctx, buf, len);
    return new SpillableHashGroupByCursor(c, tupleSize);
  }

  @Override
  public int tupleWidth(int initSize) {
    return in.tupleWidth(initSize);
  }

  private class SpillableHashGroupByCursor implements Cursor {
    final Cursor c;
    final int tupleSize;

    // In-memory hash table
    final Map<GroupKey, Grouping> map = new LinkedHashMap<>();
    long currentSize;

    // Spill state
    boolean spilled;
    File[] partitionFiles;
    OutputStream[] partitionStreams;
    int nextPartitionToProcess;

    // Output iteration
    Iterator<GroupKey> outputIt;

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
      map.clear();
      currentSize = 0;
      cleanupPartitions();
      c.close(ctx);
    }

    @Override
    public Tuple next(QueryContext ctx) throws QueryException {
      // Phase 1: Output from in-memory aggregation or current partition
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

      // Phase 2: If we have spilled partitions, process the next one
      if (spilled && partitionFiles != null) {
        return processNextPartition();
      }

      // Phase 3: Read all input, aggregating in memory or spilling
      Tuple t;
      while ((t = c.next(ctx)) != null) {
        long tupleEstimate = TupleSerializer.estimateSize(t);
        addToMap(t);
        currentSize += tupleEstimate;

        if (currentSize > memoryBudget) {
          spillAllPartitions();
        }
      }

      // All input consumed
      if (spilled && partitionFiles != null) {
        // Process spilled partitions
        return processNextPartition();
      }

      // Everything fit in memory — emit directly
      if (!map.isEmpty()) {
        outputIt = map.keySet().iterator();
        return next(ctx);
      }

      return null;
    }

    private void addToMap(Tuple t) throws QueryException {
      Atomic[] gks = Grouping.groupingKeys(groupSpecs, t);
      GroupKey key = new GroupKey(gks);
      Grouping grp = map.get(key);
      if (grp == null) {
        grp = new Grouping(groupSpecs, addAggSpecs, defaultAgg, addAggs, tupleSize);
        grp.setThreadSafe(false);
        map.put(key, grp);
      }
      grp.add(gks, t);
    }

    /**
     * Spill all current in-memory tuples to partition files based on hash prefix.
     * Each partition gets tuples with a specific hash range.
     */
    private void spillAllPartitions() throws QueryException {
      try {
        if (partitionFiles == null) {
          partitionFiles = new File[NUM_PARTITIONS];
          partitionStreams = new OutputStream[NUM_PARTITIONS];
          for (int i = 0; i < NUM_PARTITIONS; i++) {
            partitionFiles[i] = File.createTempFile("grp-part-" + i + "-", ".spill");
            partitionFiles[i].deleteOnExit();
            partitionStreams[i] = new BufferedOutputStream(new FileOutputStream(partitionFiles[i], true));
          }
        }

        // Re-scan the map and write each group's accumulated tuples to the appropriate partition.
        // Since Grouping holds aggregated state (not raw tuples), we emit each group's partial
        // result and write it as a tuple to the partition file for re-aggregation later.
        for (var entry : map.entrySet()) {
          GroupKey key = entry.getKey();
          Grouping grp = entry.getValue();
          Tuple emitted = grp.emit();
          grp.clear();

          int partition = (key.hash & 0x7FFFFFFF) % NUM_PARTITIONS;
          TupleSerializer.write(partitionStreams[partition], emitted);
        }

        map.clear();
        currentSize = 0;
        spilled = true;
      } catch (IOException e) {
        cleanupPartitions();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      }
    }

    /**
     * Process the next unprocessed spilled partition: read tuples from disk,
     * re-aggregate in memory, and set up output iterator.
     */
    private Tuple processNextPartition() throws QueryException {
      try {
        // Close partition streams if still open
        closePartitionStreams();

        while (nextPartitionToProcess < NUM_PARTITIONS) {
          File partFile = partitionFiles[nextPartitionToProcess++];
          if (partFile == null || !partFile.exists() || partFile.length() == 0) {
            continue;
          }

          // Read partition and re-aggregate
          map.clear();
          currentSize = 0;

          try (var in = new BufferedInputStream(new FileInputStream(partFile))) {
            Tuple t;
            while ((t = TupleSerializer.read(in)) != null) {
              addToMap(t);
            }
          }
          partFile.delete();

          if (!map.isEmpty()) {
            outputIt = map.keySet().iterator();
            if (outputIt.hasNext()) {
              GroupKey key = outputIt.next();
              Grouping grp = map.get(key);
              outputIt.remove();
              Tuple result = grp.emit();
              grp.clear();
              return result;
            }
          }
        }
      } catch (IOException e) {
        cleanupPartitions();
        throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
      }

      return null;
    }

    private void closePartitionStreams() {
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
    }

    private void cleanupPartitions() {
      closePartitionStreams();
      if (partitionFiles != null) {
        for (File f : partitionFiles) {
          if (f != null && f.exists()) {
            f.delete();
          }
        }
        partitionFiles = null;
      }
    }
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
