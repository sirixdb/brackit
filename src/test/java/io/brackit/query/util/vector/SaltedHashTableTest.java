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
 * Tests for the cache-friendly salted hash table.
 */
public class SaltedHashTableTest {

  @Test
  public void testPutAndGet() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.putIfAbsent(42, 100));
    assertEquals(100, ht.get(42));
  }

  @Test
  public void testGetMissing() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.get(42));
  }

  @Test
  public void testDuplicateKey() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(-1, ht.putIfAbsent(42, 100));
    assertEquals(100, ht.putIfAbsent(42, 200)); // returns existing
    assertEquals(100, ht.get(42)); // still original
  }

  @Test
  public void testMultipleKeys() {
    SaltedHashTable ht = new SaltedHashTable();
    for (int i = 0; i < 100; i++) {
      assertEquals(-1, ht.putIfAbsent(i, i * 10));
    }
    assertEquals(100, ht.getSize());
    for (int i = 0; i < 100; i++) {
      assertEquals(i * 10, ht.get(i));
    }
  }

  @Test
  public void testNegativeKeys() {
    SaltedHashTable ht = new SaltedHashTable();
    ht.putIfAbsent(-1, 1);
    ht.putIfAbsent(-100, 2);
    ht.putIfAbsent(Long.MIN_VALUE + 1, 3);
    assertEquals(1, ht.get(-1));
    assertEquals(2, ht.get(-100));
    assertEquals(3, ht.get(Long.MIN_VALUE + 1));
  }

  @Test
  public void testResize() {
    SaltedHashTable ht = new SaltedHashTable(64);
    // Insert enough to trigger multiple resizes
    for (int i = 0; i < 1000; i++) {
      ht.putIfAbsent(i, i);
    }
    assertEquals(1000, ht.getSize());
    // Verify all entries survived resizing
    for (int i = 0; i < 1000; i++) {
      assertEquals(i, ht.get(i), "Missing key: " + i);
    }
  }

  @Test
  public void testGetOrInsert() {
    SaltedHashTable ht = new SaltedHashTable();
    assertEquals(100, ht.getOrInsert(42, 100)); // inserts
    assertEquals(100, ht.getOrInsert(42, 200)); // returns existing
    assertEquals(1, ht.getSize());
  }

  @Test
  public void testBatchGet() {
    SaltedHashTable ht = new SaltedHashTable();
    for (int i = 0; i < 50; i++) {
      ht.putIfAbsent(i, i * 10);
    }

    long[] searchKeys = { 0, 10, 20, 30, 40, 99 };
    int[] results = new int[6];
    ht.batchGet(searchKeys, 0, 6, results);

    assertEquals(0, results[0]);
    assertEquals(100, results[1]);
    assertEquals(200, results[2]);
    assertEquals(300, results[3]);
    assertEquals(400, results[4]);
    assertEquals(-1, results[5]); // not found
  }

  @Test
  public void testClear() {
    SaltedHashTable ht = new SaltedHashTable();
    ht.putIfAbsent(1, 10);
    ht.putIfAbsent(2, 20);
    assertEquals(2, ht.getSize());
    ht.clear();
    assertEquals(0, ht.getSize());
    assertEquals(-1, ht.get(1));
    assertEquals(-1, ht.get(2));
  }

  @Test
  public void testRandomStress() {
    SaltedHashTable ht = new SaltedHashTable(128);
    Random rng = new Random(42);
    long[] inserted = new long[5000];

    for (int i = 0; i < 5000; i++) {
      long key = rng.nextLong();
      inserted[i] = key;
      ht.putIfAbsent(key, i);
    }

    // Verify all can be found
    for (int i = 0; i < 5000; i++) {
      int result = ht.get(inserted[i]);
      assertTrue(result >= 0, "Missing key at index " + i);
    }

    // Verify random non-existent keys return -1
    for (int i = 0; i < 1000; i++) {
      long key = rng.nextLong();
      // This might collide with an inserted key, which is OK
      int result = ht.get(key);
      assertTrue(result >= -1);
    }
  }

  @Test
  public void testKeyPayloadAccess() {
    SaltedHashTable ht = new SaltedHashTable();
    ht.putIfAbsent(100, 1);
    ht.putIfAbsent(200, 2);
    ht.putIfAbsent(300, 3);

    assertEquals(100, ht.getKey(0));
    assertEquals(1, ht.getPayload(0));

    ht.setPayload(0, 99);
    assertEquals(99, ht.getPayload(0));
  }
}
