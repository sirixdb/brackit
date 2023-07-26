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
package org.brackit.query.util.sort;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;
import org.brackit.query.util.Cfg;
import org.brackit.query.util.log.Logger;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Stream;

/**
 * Combination of main memory and external merge sort. The implementation is I/O
 * robust w.r.t. pre-sorted input, few inputs and performs well for large main
 * memory buffer sizes.
 *
 * @author Sebastian Baechle
 */
public class TupleSort {
  private static final Logger log = Logger.getLogger(TupleSort.class);

  private final long maxSize;

  private final Comparator<Tuple> comparator;

  private final File sortDir = new File(Cfg.asString("java.io.tmpdir"));

  private File[] runs;

  private Tuple[] buffer;

  private byte[] mergeBuffer;

  private int count;

  private int runCount;

  private long size;

  private OutputStream currentRun;

  private Tuple lastInRun;

  // statistics
  long leftMergeItemCount;

  long rightMergeItemCount;

  int mergeCount;

  private long directMergeSize;

  private int initialRuns;

  public TupleSort(Comparator<Tuple> comparator, long maxSize) {
    this.comparator = comparator;
    this.maxSize = maxSize;
    this.runs = new File[2];
    buffer = new Tuple[10];
  }

  public void add(Tuple item) throws QueryException {
    long itemSize = getSize(item);
    if ((maxSize > 0) && (size + itemSize > maxSize)) {
      writeRun();
    }

    if (count == buffer.length) {
      buffer = Arrays.copyOf(buffer, ((buffer.length * 3) / 2) + 1);
    }

    buffer[count++] = item;
    size += itemSize;
  }

  private long getSize(Tuple item) throws QueryException {
    // TODO
    return 0;
  }

  private void writeRun() throws QueryException {
    sortBuffer();

    if ((lastInRun != null) && (comparator.compare(lastInRun, buffer[0]) <= 0)) {
      System.out.println("append to run");
      appendToRun();
      return;
    }

    try {
      if (currentRun != null) {
        currentRun.close();
      }

      File run = File.createTempFile("sort", ".run", sortDir);
      run.deleteOnExit();

      if (log.isDebugEnabled()) {
        log.debug(String.format("Writing new run '%s'", run));
      }

      currentRun = new BufferedOutputStream(new FileOutputStream(run));

      for (int i = 0; i < count; i++) {
        lastInRun = buffer[i];
        writeItem(currentRun, lastInRun);
      }

      if (log.isDebugEnabled()) {
        log.debug(String.format("Wrote run '%s'", run));
      }

      if (runCount == runs.length) {
        runs = Arrays.copyOf(runs, ((runs.length * 3) / 2) + 1);
      }
      runs[runCount++] = run;
      size = 0;
      count = 0;
      initialRuns++;
    } catch (IOException e) {
      errorCleanup();
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }

  private void sortBuffer() throws QueryException {
    if (log.isTraceEnabled()) {
      log.trace(String.format("Start main memory sort of %s items.'", count));
    }

    try {
      Arrays.sort(buffer, 0, count, comparator);
    } catch (ClassCastException e) {
      // java.util.Comparator#compare() is expected to throw a
      // a ClassCastException when to items cannot be compared
      // to each other. This translates to a err:XPTY0004 in XQuery
      throw new QueryException(e, ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    } catch (RuntimeException e) {
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }

    if (log.isTraceEnabled()) {
      log.trace(String.format("Finished main memory sort of %s items", count));
    }
  }

  private void appendToRun() throws QueryException {
    try {
      for (int i = 0; i < count; i++) {
        lastInRun = buffer[i];
        writeItem(currentRun, lastInRun);
      }
      count = 0;
      size = 0;
    } catch (IOException e) {
      errorCleanup();
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }

  private void errorCleanup() {
    if (currentRun != null) {
      try {
        currentRun.close();
      } catch (IOException e1) {
        log.error(e1);
      }
    }

    for (File run : runs) {
      if ((run != null) && (run.exists())) {
        run.delete();
      }
    }
  }

  private Tuple readItem(InputStream in) throws IOException {
    // TODO
    return null;
  }

  private void writeItem(OutputStream out, Tuple item) throws IOException {
    // TODO
  }

  public Stream<Tuple> stream() {
    return (runCount == 0) ? mainMemorySortOnly() : mergeFinalRunAndBuffer();
  }

  public void sort() throws QueryException {
    sortBuffer();

    if (runCount > 0) {
      closeLastRun();
      mergeRuns();
    }
  }

  public void clear() {
    if (runCount > 0) {
      runs[0].delete();
    }
  }

  private void closeLastRun() throws QueryException {
    try {
      currentRun.close();
      lastInRun = null;
    } catch (IOException e) {
      errorCleanup();
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }

  private Stream<Tuple> mergeFinalRunAndBuffer() {
    final Tuple[] sBuffer = buffer;

    return new Stream<Tuple>() {
      private final Comparator<Tuple> cmp = comparator;

      private final Tuple[] sortedBuffer = sBuffer;

      private InputStream sorted;

      private Tuple left;

      private Tuple right;

      private int pos;

      @Override
      public void close() {
        try {
          sorted.close();
          clear();
        } catch (IOException e) {
          log.error(e);
        }
      }

      @Override
      public Tuple next() throws DocumentException {
        try {
          Tuple next;
          if (sorted == null) {
            sorted = new BufferedInputStream(new FileInputStream(runs[0]));
            left = readItem(sorted);
          }

          if (pos == 0) {
            right = (pos < count) ? sortedBuffer[pos++] : null;
          }

          if (left == null) {
            next = right;
            right = (pos < count) ? sortedBuffer[pos++] : null;
            return next;
          }

          if (right == null) {
            next = left;
            left = readItem(sorted);
            return next;
          }

          if (cmp.compare(left, right) <= 0) {
            next = left;
            left = readItem(sorted);
          } else {
            next = right;
            right = (pos < count) ? sortedBuffer[pos++] : null;
          }

          return next;
        } catch (IOException e) {
          throw new DocumentException(e);
        }
      }
    };
  }

  private void mergeRuns() throws QueryException {
    File[] newRuns = null;
    int mergePhase = 0;
    boolean forward = true;

    while (runCount > 1) {
      if (log.isDebugEnabled()) {
        log.debug(String.format("Starting merge phase %s type %s", mergePhase, forward));
      }

      if (mergeBuffer == null) {
        mergeBuffer = new byte[Math.max((int) maxSize / 20, 1024)];
      }

      if (log.isTraceEnabled()) {
        log.trace("Merge table");
        for (int i = 0; i < runCount; i++) {
          log.trace(String.format("%3s: %s", i, runs[i]));
        }
      }

      int merges = runCount / 2;
      boolean singleRun = runCount % 2 == 1;
      int newRunCount = merges + (singleRun ? 1 : 0);
      newRuns = new File[newRunCount];

      if (log.isDebugEnabled()) {
        log.debug(String.format("Merge %s -> %s (single run: %s)", runCount, newRunCount, singleRun));
      }

      try {
        if (forward) // all merge pairs sorted forwards, hottest at the
        // end
        {
          int pos = 0;

          if (singleRun) {
            for (int i = newRunCount - 1; i > 0; i--) {
              newRuns[pos++] = merge(runs[2 * i - 1], runs[2 * i]);
            }
            newRuns[newRunCount - 1] = runs[0];
          } else {
            for (int i = newRunCount - 1; i >= 0; i--) {
              newRuns[pos++] = merge(runs[2 * i], runs[2 * i + 1]);
            }
          }
        } else // all merge pairs sorted backwards, hottest at the end
        {
          int pos = 0;

          if (singleRun) {
            for (int i = newRunCount - 1; i > 0; i--) {
              newRuns[pos++] = merge(runs[2 * i], runs[2 * i - 1]);
            }
            newRuns[newRunCount - 1] = runs[0];
          } else {
            for (int i = newRunCount - 1; i >= 0; i--) {
              newRuns[pos++] = merge(runs[2 * i + 1], runs[2 * i]);
            }
          }
        }
      } catch (QueryException e) {
        for (File newRun : newRuns) {
          if ((newRun != null) && (newRun.exists())) {
            newRun.delete();
          }
        }
        throw e;
      }

      forward = !forward;
      runCount = newRunCount;
      runs = newRuns;

      if (log.isDebugEnabled()) {
        log.debug(String.format("Finished merge phase %s", mergePhase));
      }
      mergePhase++;
    }
  }

  private Stream<Tuple> mainMemorySortOnly() {
    final Tuple[] sorted = buffer;
    final int sortedCount = count;
    return new Stream<Tuple>() {
      private int pos;

      @Override
      public void close() {
      }

      @Override
      public Tuple next() throws DocumentException {
        return (pos < sortedCount) ? sorted[pos++] : null;
      }
    };
  }

  private File merge(File run1, File run2) throws QueryException {
    InputStream lIn = null;
    InputStream rIn = null;
    OutputStream out = null;

    try {
      mergeCount++;
      File run = File.createTempFile("sort", ".run", sortDir);
      run.deleteOnExit();

      if (log.isDebugEnabled()) {
        log.debug(String.format("Merging run '%s' and '%s' in new run '%s'", run1, run2, run));
      }

      lIn = new BufferedInputStream(new FileInputStream(run1));
      rIn = new BufferedInputStream(new FileInputStream(run2));
      Tuple left = null;
      Tuple right = null;

      out = new BufferedOutputStream(new FileOutputStream(run));

      left = readItem(lIn);
      right = readItem(rIn);

      while ((left != null) && (right != null)) {
        if (comparator.compare(left, right) <= 0) {
          writeItem(out, left);
          left = readItem(lIn);
          leftMergeItemCount++;
        } else {
          writeItem(out, right);
          right = readItem(rIn);
          rightMergeItemCount++;
        }
      }

      Tuple pending = (left == null) ? right : left;
      InputStream pendingIn = (left == null) ? rIn : lIn;

      if (pending != null) {
        writeItem(out, pending);
      }

      int read;
      while ((read = pendingIn.read(mergeBuffer)) > 0) {
        out.write(mergeBuffer, 0, read);
        directMergeSize += read;
      }

      run1.delete();
      run2.delete();

      if (log.isDebugEnabled()) {
        log.debug(String.format("Wrote run '%s'", run));
      }

      return run;
    } catch (IOException e) {
      errorCleanup();
      throw new DocumentException(e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e1) {
          log.error(e1);
        }
      }
      if (lIn != null) {
        try {
          lIn.close();
        } catch (IOException e1) {
          log.error(e1);
        }
      }
      if (rIn != null) {
        try {
          rIn.close();
        } catch (IOException e1) {
          log.error(e1);
        }
      }
    }
  }

  public String printStats() {
    StringBuilder out = new StringBuilder();
    out.append(String.format("# initial runs: %s # merges: %s", initialRuns, mergeCount));
    out.append("\n");
    out.append(String.format("Total left merge items: %10s Avg. left merge items per merge: %10.3f",
                             leftMergeItemCount,
                             (double) leftMergeItemCount / mergeCount));
    out.append("\n");
    out.append(String.format("Total right merge items: %10s Avg. right merge items per merge: %10.3f",
                             rightMergeItemCount,
                             (double) rightMergeItemCount / mergeCount));
    out.append("\n");
    out.append(String.format("Directly merged %10.2f MB", (double) directMergeSize / (1024 * 1024)));
    out.append("\n");
    return out.toString();
  }
}