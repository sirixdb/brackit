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

import io.brackit.query.jdm.Sequence;

/**
 * Cache-friendly columnar vector for batch processing.
 *
 * Inspired by DuckDB's Vector type, this stores column data in contiguous
 * primitive arrays to maximize CPU cache utilization. Data is processed in
 * batches of up to {@link #DEFAULT_CAPACITY} (2048) values, chosen so that
 * all vectors in a typical operator fit within L1 cache.
 *
 * Supports multiple vector types for compressed execution:
 * <ul>
 *   <li>{@link VectorType#FLAT} - Standard contiguous array</li>
 *   <li>{@link VectorType#CONSTANT} - Single value for all entries (broadcasts)</li>
 *   <li>{@link VectorType#DICTIONARY} - Index array into a compact dictionary</li>
 * </ul>
 *
 * @author Brackit Project Team
 */
public final class ColumnVector {

  public static final int DEFAULT_CAPACITY = 2048;

  public enum DataType {
    INT64, DOUBLE, STRING, GENERIC
  }

  public enum VectorType {
    FLAT, CONSTANT, DICTIONARY
  }

  private final DataType dataType;
  private VectorType vectorType;
  private int size;

  // Flat storage - only one is used at a time based on dataType
  private long[] longData;
  private double[] doubleData;
  private Sequence[] genericData;

  // Validity mask: null means all valid
  private final ValidityMask validity;

  // Constant vector: single value for all entries
  private long constantLong;
  private double constantDouble;
  private Sequence constantGeneric;

  // Dictionary vector: indices into a compact dictionary
  private int[] dictIndices;
  private ColumnVector dictData;

  private ColumnVector(DataType dataType, int capacity) {
    this.dataType = dataType;
    this.vectorType = VectorType.FLAT;
    this.size = 0;
    this.validity = new ValidityMask(capacity);

    switch (dataType) {
      case INT64 -> this.longData = new long[capacity];
      case DOUBLE -> this.doubleData = new double[capacity];
      case STRING, GENERIC -> this.genericData = new Sequence[capacity];
    }
  }

  private ColumnVector(DataType dataType) {
    this.dataType = dataType;
    this.vectorType = VectorType.CONSTANT;
    this.size = 0;
    this.validity = new ValidityMask(0);
  }

  public static ColumnVector ofLong(int capacity) {
    return new ColumnVector(DataType.INT64, capacity);
  }

  public static ColumnVector ofLong() {
    return ofLong(DEFAULT_CAPACITY);
  }

  public static ColumnVector ofDouble(int capacity) {
    return new ColumnVector(DataType.DOUBLE, capacity);
  }

  public static ColumnVector ofDouble() {
    return ofDouble(DEFAULT_CAPACITY);
  }

  public static ColumnVector ofGeneric(int capacity) {
    return new ColumnVector(DataType.GENERIC, capacity);
  }

  public static ColumnVector ofGeneric() {
    return ofGeneric(DEFAULT_CAPACITY);
  }

  /**
   * Create a constant vector that represents a single value broadcast to all rows.
   * No array allocation needed - extremely memory efficient for literal values.
   */
  public static ColumnVector constantLong(long value, int size) {
    ColumnVector v = new ColumnVector(DataType.INT64);
    v.constantLong = value;
    v.size = size;
    return v;
  }

  public static ColumnVector constantDouble(double value, int size) {
    ColumnVector v = new ColumnVector(DataType.DOUBLE);
    v.constantDouble = value;
    v.size = size;
    return v;
  }

  // ==================== Accessors ====================

  public DataType getDataType() {
    return dataType;
  }

  public VectorType getVectorType() {
    return vectorType;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long[] getLongData() {
    return longData;
  }

  public double[] getDoubleData() {
    return doubleData;
  }

  public Sequence[] getGenericData() {
    return genericData;
  }

  public ValidityMask getValidity() {
    return validity;
  }

  // ==================== Flat Data Access ====================

  public long getLong(int idx) {
    if (vectorType == VectorType.CONSTANT) {
      return constantLong;
    }
    if (vectorType == VectorType.DICTIONARY) {
      return dictData.getLong(dictIndices[idx]);
    }
    return longData[idx];
  }

  public double getDouble(int idx) {
    if (vectorType == VectorType.CONSTANT) {
      return constantDouble;
    }
    if (vectorType == VectorType.DICTIONARY) {
      return dictData.getDouble(dictIndices[idx]);
    }
    return doubleData[idx];
  }

  public Sequence getGeneric(int idx) {
    if (vectorType == VectorType.CONSTANT) {
      return constantGeneric;
    }
    if (vectorType == VectorType.DICTIONARY) {
      return dictData.getGeneric(dictIndices[idx]);
    }
    return genericData[idx];
  }

  public void setLong(int idx, long value) {
    longData[idx] = value;
  }

  public void setDouble(int idx, double value) {
    doubleData[idx] = value;
  }

  public void setGeneric(int idx, Sequence value) {
    genericData[idx] = value;
  }

  /**
   * Flatten this vector to FLAT type.
   * If already flat, this is a no-op. For CONSTANT vectors, expands to
   * a full array. Useful before in-place modifications.
   */
  public void flatten() {
    if (vectorType == VectorType.FLAT) {
      return;
    }
    if (vectorType == VectorType.CONSTANT) {
      flattenConstant();
    } else if (vectorType == VectorType.DICTIONARY) {
      flattenDictionary();
    }
    vectorType = VectorType.FLAT;
  }

  private void flattenConstant() {
    switch (dataType) {
      case INT64 -> {
        if (longData == null || longData.length < size) {
          longData = new long[size];
        }
        java.util.Arrays.fill(longData, 0, size, constantLong);
      }
      case DOUBLE -> {
        if (doubleData == null || doubleData.length < size) {
          doubleData = new double[size];
        }
        java.util.Arrays.fill(doubleData, 0, size, constantDouble);
      }
      case STRING, GENERIC -> {
        if (genericData == null || genericData.length < size) {
          genericData = new Sequence[size];
        }
        java.util.Arrays.fill(genericData, 0, size, constantGeneric);
      }
    }
  }

  private void flattenDictionary() {
    switch (dataType) {
      case INT64 -> {
        if (longData == null || longData.length < size) {
          longData = new long[size];
        }
        long[] src = dictData.longData;
        for (int i = 0; i < size; i++) {
          longData[i] = src[dictIndices[i]];
        }
      }
      case DOUBLE -> {
        if (doubleData == null || doubleData.length < size) {
          doubleData = new double[size];
        }
        double[] src = dictData.doubleData;
        for (int i = 0; i < size; i++) {
          doubleData[i] = src[dictIndices[i]];
        }
      }
      case STRING, GENERIC -> {
        if (genericData == null || genericData.length < size) {
          genericData = new Sequence[size];
        }
        Sequence[] src = dictData.genericData;
        for (int i = 0; i < size; i++) {
          genericData[i] = src[dictIndices[i]];
        }
      }
    }
  }

  /**
   * Reset vector for reuse without re-allocating arrays.
   */
  public void reset() {
    size = 0;
    vectorType = VectorType.FLAT;
    validity.reset();
    dictIndices = null;
    dictData = null;
  }
}
