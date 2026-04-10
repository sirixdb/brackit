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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for branchless selection vector operations.
 */
public class SelectionVectorTest {

  @Test
  public void testIdentityInit() {
    SelectionVector sv = new SelectionVector(16);
    sv.initIdentity(10);
    assertEquals(10, sv.getSize());
    for (int i = 0; i < 10; i++) {
      assertEquals(i, sv.getSelected()[i]);
    }
  }

  @Test
  public void testFilterEqLong_allMatch() {
    SelectionVector sv = new SelectionVector(8);
    long[] values = { 5, 5, 5, 5, 5 };
    int count = sv.filterEqLong(values, 5, 5);
    assertEquals(5, count);
  }

  @Test
  public void testFilterEqLong_noneMatch() {
    SelectionVector sv = new SelectionVector(8);
    long[] values = { 1, 2, 3, 4, 5 };
    int count = sv.filterEqLong(values, 5, 42);
    assertEquals(0, count);
  }

  @Test
  public void testFilterEqLong_someMatch() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 5, 2, 5, 3, 5 };
    int count = sv.filterEqLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(1, sv.getSelected()[0]);
    assertEquals(3, sv.getSelected()[1]);
    assertEquals(5, sv.getSelected()[2]);
  }

  @Test
  public void testFilterGtLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 10, 2, 20, 3, 30 };
    int count = sv.filterGtLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(1, sv.getSelected()[0]);
    assertEquals(3, sv.getSelected()[1]);
    assertEquals(5, sv.getSelected()[2]);
  }

  @Test
  public void testFilterLtLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 1, 10, 2, 20, 3, 30 };
    int count = sv.filterLtLong(values, 6, 5);
    assertEquals(3, count);
    assertEquals(0, sv.getSelected()[0]);
    assertEquals(2, sv.getSelected()[1]);
    assertEquals(4, sv.getSelected()[2]);
  }

  @Test
  public void testFilterGtDouble() {
    SelectionVector sv = new SelectionVector(16);
    double[] values = { 1.0, 10.0, 2.0, 20.0, 3.0, 30.0 };
    int count = sv.filterGtDouble(values, 6, 5.0);
    assertEquals(3, count);
  }

  @Test
  public void testTightenEqLong() {
    SelectionVector sv = new SelectionVector(16);
    long[] values = { 5, 10, 5, 20, 5, 30 };

    // First filter: > 0 (all pass)
    sv.filterGtLong(values, 6, 0);
    assertEquals(6, sv.getSize());

    // Tighten: keep only == 5
    int count = sv.tightenEqLong(values, 5);
    assertEquals(3, count);
    assertEquals(0, sv.getSelected()[0]);
    assertEquals(2, sv.getSelected()[1]);
    assertEquals(4, sv.getSelected()[2]);
  }

  @Test
  public void testValidityMaskApplication() {
    SelectionVector sv = new SelectionVector(128);
    sv.initIdentity(64);

    // Create validity mask: only even positions valid
    long[] mask = new long[1];
    for (int i = 0; i < 64; i += 2) {
      mask[0] |= (1L << i);
    }

    int count = sv.applyValidityMask(mask);
    assertEquals(32, count);
    for (int i = 0; i < count; i++) {
      assertEquals(i * 2, sv.getSelected()[i]);
    }
  }

  @Test
  public void testLargeVector() {
    int n = 2048;
    SelectionVector sv = new SelectionVector(n);
    long[] values = new long[n];
    for (int i = 0; i < n; i++) {
      values[i] = i;
    }

    // Filter: > 1000
    int count = sv.filterGtLong(values, n, 1000);
    assertEquals(1047, count);
    assertEquals(1001, sv.getSelected()[0]);
    assertEquals(2047, sv.getSelected()[count - 1]);
  }
}
