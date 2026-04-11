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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the mechanical sympathy vector infrastructure:
 * SelectionVector, SaltedHashTable, ColumnVector, ValidityMask, DataChunk.
 */
public class MechanicalSympathyTest {

  // ==================== SelectionVector Tests ====================

  @Test
  public void testSelectionVector_identity() {
    SelectionVector sv = new SelectionVector(16);
    sv.initIdentity(10);
    assertEquals(10, sv.getSize());
    for (int i = 0; i < 10; i++) {
      assertEquals(i, sv.getSelected()[i]);
    }
  }

  @Test
  public void testSelectionVector_filterEqLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 5, 2, 5, 3, 5 };
    int count = sv.filterEqLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(1, sv.getSelected()[0]);
    assertEquals(3, sv.getSelected()[1]);
    assertEquals(5, sv.getSelected()[2]);
  }

  @Test
  public void testSelectionVector_filterGtLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 10, 2, 20, 3, 30 };
    int count = sv.filterGtLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(1, sv.getSelected()[0]);
    assertEquals(3, sv.getSelected()[1]);
    assertEquals(5, sv.getSelected()[2]);
  }

  @Test
  public void testSelectionVector_filterLtLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 10, 2, 20, 3, 30 };
    int count = sv.filterLtLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(0, sv.getSelected()[0]);
    assertEquals(2, sv.getSelected()[1]);
    assertEquals(4, sv.getSelected()[2]);
  }

  @Test
  public void testSelectionVector_cascading() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 5, 10, 5, 20, 5, 30 };
    sv.filterGtLong(values, 6, 0);
    assertEquals(6, sv.getSize());
    int count = sv.tightenEqLong(values, 5);
    assertEquals(3, count);
    assertEquals(0, sv.getSelected()[0]);
    assertEquals(2, sv.getSelected()[1]);
    assertEquals(4, sv.getSelected()[2]);
  }

  @Test
  public void testSelectionVector_large() {
    int n = 2048;
    SelectionVector sv = new SelectionVector(n);
    long[] values = new long[n];
    for (int i = 0; i < n; i++) {
      values[i] = i;
    }
    int count = sv.filterGtLong(values, n, 1000);
    assertEquals(1047, count);
    assertEquals(1001, sv.getSelected()[0]);
    assertEquals(2047, sv.getSelected()[count - 1]);
  }

  // ==================== SaltedHashTable Tests ====================

  @Test
  public void testSaltedHashTable_putAndGet() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.putIfAbsent(42, 100));
    assertEquals(100, ht.get(42));
  }

  @Test
  public void testSaltedHashTable_missing() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.get(42));
  }

  @Test
  public void testSaltedHashTable_duplicate() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.putIfAbsent(42, 100));
    assertEquals(100, ht.putIfAbsent(42, 200));
    assertEquals(100, ht.get(42));
  }

  @Test
  public void testSaltedHashTable_multipleKeys() {
    SaltedHashTable ht = new SaltedHashTable();
    for (int i = 1; i <= 100; i++) {
      assertEquals(-1, ht.putIfAbsent(i, i * 10));
    }
    assertEquals(100, ht.getSize());
    for (int i = 1; i <= 100; i++) {
      assertEquals(i * 10, ht.get(i));
    }
  }

  @Test
  public void testSaltedHashTable_negativeKeys() {
    SaltedHashTable ht = new SaltedHashTable();
    ht.putIfAbsent(-1, 1);
    ht.putIfAbsent(-100, 2);
    assertEquals(1, ht.get(-1));
    assertEquals(2, ht.get(-100));
  }

  @Test
  public void testSaltedHashTable_resize() {
    SaltedHashTable ht = new SaltedHashTable(64);
    for (int i = 1; i <= 1000; i++) {
      ht.putIfAbsent(i, i);
    }
    assertEquals(1000, ht.getSize());
    for (int i = 1; i <= 1000; i++) {
      assertEquals(i, ht.get(i), "Missing key: " + i);
    }
  }

  @Test
  public void testSaltedHashTable_getOrInsert() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(100, ht.getOrInsert(42, 100));
    assertEquals(100, ht.getOrInsert(42, 200));
    assertEquals(1, ht.getSize());
  }

  @Test
  public void testSaltedHashTable_zeroKey() {
    // Regression: key 0 must work correctly (its hash is 0, salt is 0)
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.putIfAbsent(0, 999));
    assertEquals(999, ht.get(0));
  }

  @Test
  public void testSaltedHashTable_batchGet() {
    SaltedHashTable ht = new SaltedHashTable();
    for (int i = 1; i <= 50; i++) {
      ht.putIfAbsent(i, i * 10);
    }
    long[] searchKeys = { 1, 10, 20, 30, 40, 99 };
    int[] results = new int[6];
    ht.batchGet(searchKeys, 0, 6, results);
    assertEquals(10, results[0]);
    assertEquals(100, results[1]);
    assertEquals(200, results[2]);
    assertEquals(300, results[3]);
    assertEquals(400, results[4]);
    assertEquals(-1, results[5]);
  }

  @Test
  public void testSaltedHashTable_clear() {
    SaltedHashTable ht = new SaltedHashTable();
    ht.putIfAbsent(1, 10);
    ht.putIfAbsent(2, 20);
    assertEquals(2, ht.getSize());
    ht.clear();
    assertEquals(0, ht.getSize());
    assertEquals(-1, ht.get(1));
  }

  @Test
  public void testSaltedHashTable_stress() {
    SaltedHashTable ht = new SaltedHashTable(128);
    Random rng = new Random(42);
    int n = 5000;
    long[] inserted = new long[n];
    for (int i = 0; i < n; i++) {
      long key = rng.nextLong();
      if (key == 0) {
        key = 1; // avoid degenerate case for simpler test
      }
      inserted[i] = key;
      ht.putIfAbsent(key, i);
    }
    for (int i = 0; i < n; i++) {
      int result = ht.get(inserted[i]);
      assertTrue(result >= 0, "Missing key at index " + i);
    }
  }

  // ==================== ColumnVector Tests ====================

  @Test
  public void testColumnVector_flatLong() {
    ColumnVector v = ColumnVector.ofLong(16);
    for (int i = 0; i < 10; i++) {
      v.setLong(i, i * 100L);
    }
    v.setSize(10);
    assertEquals(10, v.getSize());
    assertEquals(500L, v.getLong(5));
  }

  @Test
  public void testColumnVector_flatDouble() {
    ColumnVector v = ColumnVector.ofDouble(16);
    for (int i = 0; i < 10; i++) {
      v.setDouble(i, i * 1.5);
    }
    v.setSize(10);
    assertEquals(7.5, v.getDouble(5), 0.001);
  }

  @Test
  public void testColumnVector_constantLong() {
    ColumnVector v = ColumnVector.constantLong(42, 2048);
    assertEquals(2048, v.getSize());
    assertEquals(ColumnVector.VectorType.CONSTANT, v.getVectorType());
    assertEquals(42L, v.getLong(0));
    assertEquals(42L, v.getLong(1000));
    assertEquals(42L, v.getLong(2047));
  }

  @Test
  public void testColumnVector_flattenConstant() {
    ColumnVector v = ColumnVector.constantLong(7, 4);
    v.flatten();
    assertEquals(ColumnVector.VectorType.FLAT, v.getVectorType());
    long[] data = v.getLongData();
    for (int i = 0; i < 4; i++) {
      assertEquals(7L, data[i]);
    }
  }

  @Test
  public void testColumnVector_reset() {
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
  public void testValidityMask_and() {
    long[] a = { 0b1111L };
    long[] b = { 0b1010L };
    long[] result = ValidityMask.and(a, b, 1);
    assertNotNull(result);
    assertEquals(0b1010L, result[0]);
  }

  @Test
  public void testValidityMask_andWithNull() {
    long[] a = { 0xFFFFFFFFFFFFFFFEL };
    // null means all-valid
    long[] result1 = ValidityMask.and(null, a, 1);
    assertNotNull(result1);
    assertEquals(a[0], result1[0]);

    long[] result2 = ValidityMask.and(a, null, 1);
    assertNotNull(result2);
    assertEquals(a[0], result2[0]);

    // Both null = all valid = null
    assertNull(ValidityMask.and(null, null, 1));
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
  public void testDataChunk_create() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64, ColumnVector.DataType.DOUBLE);
    assertEquals(2, chunk.getColumnCount());
    assertNotNull(chunk.getColumn(0));
    assertNotNull(chunk.getColumn(1));
  }

  @Test
  public void testDataChunk_setSize() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64, ColumnVector.DataType.DOUBLE);
    chunk.setSize(100);
    assertEquals(100, chunk.getSize());
    assertEquals(100, chunk.getColumn(0).getSize());
    assertEquals(100, chunk.getColumn(1).getSize());
  }

  @Test
  public void testDataChunk_selection() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64);
    chunk.setSize(10);
    chunk.initSelection();
    assertEquals(10, chunk.getSelectedCount());
  }

  @Test
  public void testDataChunk_reset() {
    DataChunk chunk = DataChunk.create(ColumnVector.DataType.INT64);
    chunk.setSize(1);
    chunk.reset();
    assertEquals(0, chunk.getSize());
  }
}
