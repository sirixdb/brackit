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

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Int64;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.Cmp;
import io.brackit.query.util.join.FastList;
import io.brackit.query.util.simd.VectorOps;

import java.util.Arrays;

/**
 * Block-based nested loop join implementation.
 *
 * @author Sebastian Baechle
 */
public class NLJoin implements Block {
  final Block left;
  final Block right;
  final Expr lExpr;
  final Expr rExpr;
  final Cmp cmp;
  final boolean isGCmp;
  final boolean leftJoin;
  final int pad;

  public NLJoin(Block left, Block right, Expr lExpr, Expr rExpr, Cmp cmp, boolean isGCmp, boolean leftJoin) {
    this.left = left;
    this.right = right;
    this.lExpr = lExpr;
    this.rExpr = rExpr;
    this.cmp = cmp;
    this.isGCmp = isGCmp;
    this.leftJoin = leftJoin;
    this.pad = right.outputWidth(0);
  }

  @Override
  public Sink create(QueryContext ctx, Sink sink) throws QueryException {
    return new NLJoinSink(ctx, sink);
  }

  @Override
  public int outputWidth(int inputWidth) {
    return left.outputWidth(inputWidth) + pad;
  }

  private class NLJoinSink implements Sink {
    // Batch size for output - larger batches reduce function call overhead
    private static final int OUTPUT_BATCH_SIZE = 512;

    final QueryContext ctx;
    final Sink sink;
    final Sequence[] padding;
    // Reusable output buffer
    private Tuple[] outBuf = new Tuple[OUTPUT_BATCH_SIZE];
    private int outLen = 0;

    public NLJoinSink(QueryContext ctx, Sink sink) {
      this.ctx = ctx;
      this.sink = sink;
      this.padding = new Sequence[pad];
    }

    @Override
    public void output(Tuple[] buf, int len) throws QueryException {
      // Reusable collector to reduce object allocation
      RightCollector collector = new RightCollector(ctx);

      for (int i = 0; i < len; i++) {
        Tuple leftTuple = buf[i];
        int leftSize = leftTuple.getSize();
        Sequence lKey = isGCmp ? lExpr.evaluate(ctx, leftTuple) : lExpr.evaluateToItem(ctx, leftTuple);

        // Reset and configure collector for this left tuple
        collector.reset(leftTuple, leftSize, lKey);
        Sink rightSink = right.create(ctx, collector);
        rightSink.begin();
        rightSink.output(new Tuple[] { leftTuple }, 1);
        rightSink.end();

        if (collector.matches.isEmpty() && leftJoin) {
          // No matches - emit padded tuple for left join
          addToOutput(leftTuple.concat(padding));
        } else {
          // Add all matches in batched manner
          for (int j = 0; j < collector.matches.getSize(); j++) {
            addToOutput(collector.matches.get(j));
          }
        }
      }

      // Flush remaining output
      flushOutput();
    }

    private void addToOutput(Tuple t) throws QueryException {
      outBuf[outLen++] = t;
      if (outLen == OUTPUT_BATCH_SIZE) {
        sink.output(outBuf, outLen);
        outLen = 0;
      }
    }

    private void flushOutput() throws QueryException {
      if (outLen > 0) {
        sink.output(outBuf, outLen);
        outLen = 0;
      }
    }

    @Override
    public Sink fork() {
      return new NLJoinSink(ctx, sink.fork());
    }

    @Override
    public Sink partition(Sink stopAt) {
      return new NLJoinSink(ctx, sink.partition(stopAt));
    }

    @Override
    public void end() throws QueryException {
      sink.end();
    }

    @Override
    public void begin() throws QueryException {
      sink.begin();
    }

    @Override
    public void fail() throws QueryException {
      sink.fail();
    }

    private class RightCollector implements Sink {
      final QueryContext ctx;
      // Use FastList for better performance - pre-allocate for expected matches
      FastList<Tuple> matches;
      // Reusable binding array to avoid repeated allocations
      private Sequence[] bindingsBuffer;
      // Batch buffer for SIMD comparisons
      private long[] rightKeyBuffer;
      private static final int SIMD_THRESHOLD = 16;

      Tuple leftTuple;
      int leftSize;
      Sequence lKey;

      RightCollector(QueryContext ctx) {
        this.ctx = ctx;
      }

      void reset(Tuple leftTuple, int leftSize, Sequence lKey) {
        this.leftTuple = leftTuple;
        this.leftSize = leftSize;
        this.lKey = lKey;
        // Create new FastList - still more efficient than ArrayList due to less overhead
        this.matches = new FastList<>(64);
      }

      @Override
      public void output(Tuple[] buf, int len) throws QueryException {
        // Pre-ensure capacity for potential matches
        matches.ensureAdditional(len);

        // Try SIMD-optimized path for Int64 equality comparisons
        if (!isGCmp && cmp == Cmp.eq && lKey instanceof Int64 lKeyInt && len >= SIMD_THRESHOLD) {
          outputVectorizedInt64(buf, len, lKeyInt.longValue());
          return;
        }

        // Standard path
        for (int i = 0; i < len; i++) {
          Tuple rightTuple = buf[i];
          Sequence rKey = isGCmp ? rExpr.evaluate(ctx, rightTuple) : rExpr.evaluateToItem(ctx, rightTuple);

          boolean match = isGCmp ? cmp.gCmp(ctx, lKey, rKey) : cmp.vCmp(ctx, (Item) lKey, (Item) rKey);

          if (match) {
            addMatch(rightTuple);
          }
        }
      }

      /**
       * SIMD-optimized equality comparison for Int64 keys.
       */
      private void outputVectorizedInt64(Tuple[] buf, int len, long leftKey) throws QueryException {
        // Allocate or reuse buffer
        if (rightKeyBuffer == null || rightKeyBuffer.length < len) {
          rightKeyBuffer = new long[Math.max(len, 256)];
        }

        // Extract right keys to primitive array
        int validCount = 0;
        int[] validIndices = new int[len];
        for (int i = 0; i < len; i++) {
          Tuple rightTuple = buf[i];
          Sequence rKey = rExpr.evaluateToItem(ctx, rightTuple);
          if (rKey instanceof Int64 rKeyInt) {
            rightKeyBuffer[validCount] = rKeyInt.longValue();
            validIndices[validCount] = i;
            validCount++;
          } else {
            // Mixed types - fall back to scalar comparison for this tuple
            boolean match = cmp.vCmp(ctx, (Item) lKey, (Item) rKey);
            if (match) {
              addMatch(buf[i]);
            }
          }
        }

        // Count matches using SIMD
        int matchCount = VectorOps.countEquals(rightKeyBuffer, 0, validCount, leftKey);

        if (matchCount > 0) {
          // Collect matching tuples
          for (int i = 0; i < validCount; i++) {
            if (rightKeyBuffer[i] == leftKey) {
              addMatch(buf[validIndices[i]]);
            }
          }
        }
      }

      private void addMatch(Tuple rightTuple) {
        // Extract right-side bindings (everything after left tuple)
        Sequence[] tmp = rightTuple.array();
        int bindingsLen = tmp.length - leftSize;
        // Reuse bindings buffer if possible
        if (bindingsBuffer == null || bindingsBuffer.length != bindingsLen) {
          bindingsBuffer = new Sequence[bindingsLen];
        }
        System.arraycopy(tmp, leftSize, bindingsBuffer, 0, bindingsLen);
        matches.add(leftTuple.concat(bindingsBuffer));
      }

      @Override
      public Sink fork() {
        return this;
      }

      @Override
      public Sink partition(Sink stopAt) {
        return this;
      }

      @Override
      public void end() throws QueryException {
      }

      @Override
      public void begin() throws QueryException {
      }

      @Override
      public void fail() throws QueryException {
      }
    }
  }
}
