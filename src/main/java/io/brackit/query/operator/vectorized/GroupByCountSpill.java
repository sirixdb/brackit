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
package io.brackit.query.operator.vectorized;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Disk-spill support for the vectorized parallel group-by-count
 * ({@link ParallelGroupByExec}). When a worker's distinct-key set grows past its
 * budget, its partial {@code (key -> count)} pairs are radix-partitioned by key hash
 * into {@value #NUM_PARTITIONS} per-worker files; the same key always lands in the same
 * partition across all workers. The merge then processes one partition at a time, so the
 * peak heap is bounded by a single partition's distinct keys rather than the whole
 * group-by. Counts are mergeable, so spilling partial aggregates and summing them is
 * exact.
 */
final class GroupByCountSpill {

  static final int NUM_PARTITIONS = 64;

  private GroupByCountSpill() {
  }

  static int partition(String key) {
    return (key.hashCode() & 0x7FFFFFFF) % NUM_PARTITIONS;
  }

  /**
   * A single worker's spill files: one append stream per partition, created lazily so an
   * unused partition costs nothing.
   */
  static final class Writer implements Closeable {
    private final Path[] files = new Path[NUM_PARTITIONS];
    private final DataOutputStream[] outs = new DataOutputStream[NUM_PARTITIONS];
    private boolean used;

    void add(String key, long count) throws IOException {
      final int p = partition(key);
      DataOutputStream out = outs[p];
      if (out == null) {
        final Path f = Files.createTempFile("vgbc-p" + p + "-", ".spill");
        files[p] = f;
        out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(f)));
        outs[p] = out;
        used = true;
      }
      final byte[] kb = key.getBytes(StandardCharsets.UTF_8);
      out.writeInt(kb.length);
      out.write(kb);
      out.writeLong(count);
    }

    boolean used() {
      return used;
    }

    Path file(int partition) {
      return files[partition];
    }

    /** Flush and close the write streams, keeping the files for the merge phase. */
    void finishWriting() throws IOException {
      for (int i = 0; i < outs.length; i++) {
        if (outs[i] != null) {
          outs[i].close();
          outs[i] = null;
        }
      }
    }

    @Override
    public void close() {
      finishWritingQuietly();
      for (final Path f : files) {
        if (f != null) {
          try {
            Files.deleteIfExists(f);
          } catch (IOException ignored) {
          }
        }
      }
    }

    private void finishWritingQuietly() {
      try {
        finishWriting();
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Read every writer's file for {@code partition} and fold the {@code (key -> count)}
   * pairs into {@code target}, summing duplicates.
   */
  static void mergePartition(List<Writer> writers, int partition, Map<String, long[]> target) throws IOException {
    for (final Writer w : writers) {
      final Path f = w.file(partition);
      if (f == null) {
        continue;
      }
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(f)))) {
        while (true) {
          final int len;
          try {
            len = in.readInt();
          } catch (EOFException eof) {
            break;
          }
          final byte[] kb = new byte[len];
          in.readFully(kb);
          final long count = in.readLong();
          final String key = new String(kb, StandardCharsets.UTF_8);
          final long[] cur = target.get(key);
          if (cur == null) {
            target.put(key, new long[] { count });
          } else {
            cur[0] += count;
          }
        }
      }
    }
  }
}
