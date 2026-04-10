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

import io.brackit.query.atomic.Int64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ColumnVector, DataChunk, and ValidityMask.
 */
public class ColumnVectorTest {

  @Test
  public void testFlatLongVector() {
    ColumnVector v = ColumnVector.ofLong(16);
    for (int i = 0; i < 10; i++) {
      v.setLong(i, i * 100);
    }
    v.setSize(10);
    assertEquals(10, v.getSize());
    assertEquals(500, v.getLong(5));
  }

  @Test
  public void testFlatDoubleVector() {
    ColumnVector v = ColumnVector.ofDouble(16);
    for (int i = 0; i < 10; i++) {
      v.setDouble(i, i * 1.5);
    }
    v.setSize(10);
    assertEquals(7.5, v.getDouble(5), 0.001);
  }

  @Test
  public void testGenericVector() {
    ColumnVector v = ColumnVector.ofGeneric(8);
    v.setGeneric(0, new Int64(42));
    v.setGeneric(1, new Int64(99));
    v.setSize(2);
    assertEquals(new Int64(42), v.getGeneric(0));
  }

  @Test
  public void testConstantLongVector() {
    ColumnVector v = ColumnVector.constantLong(42, 2048);
    assertEquals(2048, v.getSize());
    assertEquals(ColumnVector.VectorType.CONSTANT, v.getVectorType());
    assertEquals(42, v.getLong(0));
    assertEquals(42, v.getLong(1000));
    assertEquals(42, v.getLong(2047));
  }

  @Test
  public void testConstantDoubleVector() {
    ColumnVector v = ColumnVector.constantDouble(3.14, 1024);
    assertEquals(1024, v.getSize());
    assertEquals(3.14, v.getDouble(500), 0.001);
  }

  @Test
  public void testFlattenConstant() {
    ColumnVector v = ColumnVector.constantLong(7, 4);
    v.flatten();
    assertEquals(ColumnVector.VectorType.FLAT, v.getVectorType());
    long[] data = v.getLongData();
    for (int i = 0; i < 4; i++) {
      assertEquals(7, data[i]);
    }
  }

  @Test
  public void testReset() {
    ColumnVector v = ColumnVector.ofLong(8);
    v.setLong(0, 100);
    v.setSize(1);
    v.reset();
    assertEquals(0, v.getSize());
    assertEquals(ColumnVector.VectorType.FLAT, v.getVectorType());
  }

  // ==================== ValidityMask Tests ====================

  @Test
  public void testValidityMask_allValid() {
    ValidityMask mask = new ValidityMask(64);
    assertTrue(mask.allValid());
    assertTrue(mask.isValid(0));
    assertTrue(mask.isValid(63));
    assertEquals(64, mask.countValid());
  }

  @Test
  public void testValidityMask_setNull() {
    ValidityMask mask = new ValidityMask(64);
    mask.setNull(10);
    assertFalse(mask.allValid());
    assertFalse(mask.isValid(10));
    assertTrue(mask.isValid(9));
    assertTrue(mask.isValid(11));
    assertEquals(63, mask.countValid());
    assertEquals(1, mask.countNull());
  }

  @Test
  public void testValidityMask_setValid() {
    ValidityMask mask = new ValidityMask(64);
    mask.setNull(10);
    mask.setValid(10);
    assertTrue(mask.isValid(10));
    assertEquals(64, mask.countValid());
  }

  @Test
  public void testValidityMask_multiWord() {
    ValidityMask mask = new ValidityMask(128);
    mask.setNull(0);
    mask.setNull(64);
    mask.setNull(127);
    assertEquals(125, mask.countValid());
    assertFalse(mask.isValid(0));
    assertFalse(mask.isValid(64));
    assertFalse(mask.isValid(127));
    assertTrue(mask.isValid(1));
    assertTrue(mask.isValid(63));
    assertTrue(mask.isValid(65));
  }

  @Test
  public void testValidityMask_andBothNull() {
    // null AND null = null (all valid)
    assertNull(ValidityMask.and(null, null, 1));
  }

  @Test
  public void testValidityMask_andOneNull() {
    long[] a = { 0xFFFFFFFFFFFFFFFEL };
    assertSame(a, ValidityMask.and(null, a, 1));
    assertSame(a, ValidityMask.and(a, null, 1));
  }

  @Test
  public void testValidityMask_and() {
    long[] a = { 0b1111L };
    long[] b = { 0b1010L };
    long[] result = ValidityMask.and(a, b, 1);
    assertEquals(0b1010L, result[0]);
  }

  @Test
  public void testValidityMask_reset() {
    ValidityMask mask = new ValidityMask(64);
    mask.setNull(5);
    assertFalse(mask.allValid());
    mask.reset();
    assertTrue(mask.allValid());
  }

  // ==================== DataChunk Tests ====================

  @Test
  public void testDataChunkCreate() {
    DataChunk chunk = DataChunk.create(
        ColumnVector.DataType.INT64,
        ColumnVector.DataType.DOUBLE,
        ColumnVector.DataType.GENERIC);
    assertEquals(3, chunk.getColumnCount());
    assertNotNull(chunk.getColumn(0));
    assertNotNull(chunk.getColumn(1));
    assertNotNull(chunk.getColumn(2));
  }

  @Test
  public void testDataChunkSetSize() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64, ColumnVector.DataType.DOUBLE);
    chunk.setSize(100);
    assertEquals(100, chunk.getSize());
    assertEquals(100, chunk.getColumn(0).getSize());
    assertEquals(100, chunk.getColumn(1).getSize());
  }

  @Test
  public void testDataChunkSelection() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64);
    chunk.setSize(10);
    chunk.initSelection();
    assertEquals(10, chunk.getSelectedCount());
  }

  @Test
  public void testDataChunkReset() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64);
    ColumnVector col = chunk.getColumn(0);
    col.setLong(0, 42);
    chunk.setSize(1);
    chunk.reset();
    assertEquals(0, chunk.getSize());
  }
}
