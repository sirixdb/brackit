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
package io.brackit.query.util.vector;

/**
 * DuckDB-style DataChunk: a batch of columnar vectors plus selection state.
 *
 * A DataChunk holds multiple {@link ColumnVector}s (one per column) along with
 * a shared {@link SelectionVector} indicating active rows. All vectors share
 * the same row count and selection state.
 *
 * This is the fundamental unit of data exchange in vectorized execution.
 * Operators process entire DataChunks rather than individual tuples,
 * amortizing virtual dispatch and enabling SIMD operations on primitive arrays.
 *
 * The standard capacity is 2048 rows, chosen so all column vectors
 * in a typical operator fit within L1 cache (2048 x 8 bytes = 16KB per column).
 *
 * @author Brackit Project Team
 */
public final class DataChunk {

  public static final int DEFAULT_CAPACITY = ColumnVector.DEFAULT_CAPACITY;

  private final ColumnVector[] columns;
  private final SelectionVector selection;
  private int size;
  private final int capacity;

  public DataChunk(int columnCount) {
    this(columnCount, DEFAULT_CAPACITY);
  }

  public DataChunk(int columnCount, int capacity) {
    this.columns = new ColumnVector[columnCount];
    this.selection = new SelectionVector(capacity);
    this.capacity = capacity;
    this.size = 0;
  }

  /**
   * Create a DataChunk with pre-configured column types.
   */
  public static DataChunk create(ColumnVector.DataType... types) {
    DataChunk chunk = new DataChunk(types.length);
    for (int i = 0; i < types.length; i++) {
      chunk.columns[i] = switch (types[i]) {
        case INT64 -> ColumnVector.ofLong();
        case DOUBLE -> ColumnVector.ofDouble();
        case STRING, GENERIC -> ColumnVector.ofGeneric();
      };
    }
    return chunk;
  }

  // ==================== Column Management ====================

  public ColumnVector getColumn(int index) {
    return columns[index];
  }

  public void setColumn(int index, ColumnVector column) {
    columns[index] = column;
  }

  public int getColumnCount() {
    return columns.length;
  }

  // ==================== Size & Selection ====================

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
    for (ColumnVector col : columns) {
      if (col != null) {
        col.setSize(size);
      }
    }
  }

  public int getCapacity() {
    return capacity;
  }

  public SelectionVector getSelection() {
    return selection;
  }

  /**
   * Initialize selection to identity (all rows active).
   */
  public void initSelection() {
    selection.initIdentity(size);
  }

  /**
   * Get the effective row count (after selection).
   */
  public int getSelectedCount() {
    return selection.getSize();
  }

  // ==================== Reset ====================

  /**
   * Reset chunk for reuse without reallocating column arrays.
   */
  public void reset() {
    size = 0;
    for (ColumnVector col : columns) {
      if (col != null) {
        col.reset();
      }
    }
  }
}
