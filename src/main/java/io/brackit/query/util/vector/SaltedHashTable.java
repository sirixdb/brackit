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
 * Cache-friendly hash table using linear probing with salt-based fast rejection.
 *
 * Inspired by DuckDB's aggregate hash table design:
 * <ul>
 *   <li><b>Linear probing</b> for cache-line friendly collision resolution.
 *       Sequential memory access during probing benefits from hardware prefetching.</li>
 *   <li><b>Salt bits</b> in each slot for fast rejection without following pointers.
 *       The upper 16 bits of the hash are stored as a "salt". During probing,
 *       salt is compared first; with a uniform hash, this rejects 65535/65536
 *       of false positives without a cache miss to the actual key.</li>
 *   <li><b>Flat arrays</b> instead of linked entries. The directory is a single
 *       {@code long[]} where each entry packs salt + index into 8 bytes.</li>
 * </ul>
 *
 * Entry format (64 bits):
 * <pre>
 *   [16 bits: salt] [16 bits: reserved] [32 bits: payload index]
 * </pre>
 *
 * @author Brackit Project Team
 */
public final class SaltedHashTable {

  private static final long EMPTY = 0L;
  private static final int SALT_SHIFT = 48;
  private static final long SALT_MASK = 0xFFFF_0000_0000_0000L;
  private static final long INDEX_MASK = 0x0000_0000_FFFF_FFFFL;

  private static final float LOAD_FACTOR = 0.7f;
  private static final int MIN_CAPACITY = 64;

  private long[] directory;
  private int mask;
  private int size;
  private int capacity;

  // Parallel arrays for payload storage (cache-friendly row layout)
  private long[] keys;
  private int[] payloads; // indices into external data structures

  public SaltedHashTable() {
    this(MIN_CAPACITY);
  }

  public SaltedHashTable(int initialCapacity) {
    this.capacity = nextPowerOfTwo(Math.max(initialCapacity, MIN_CAPACITY));
    this.mask = capacity - 1;
    this.directory = new long[capacity];
    this.keys = new long[capacity];
    this.payloads = new int[capacity];
    this.size = 0;
  }

  /**
   * Insert a key-payload pair. Returns the payload index if the key already exists.
   */
  public int putIfAbsent(long key, int payload) {
    if (size >= capacity * LOAD_FACTOR) {
      resize();
    }

    int hash = hash(key);
    long salt = extractSalt(hash);
    int slot = hash & mask;

    while (true) {
      long entry = directory[slot];

      if (entry == EMPTY) {
        // Empty slot - insert
        directory[slot] = salt | (size & INDEX_MASK);
        keys[size] = key;
        payloads[size] = payload;
        size++;
        return -1; // new entry
      }

      // Salt check: fast rejection without reading the key
      if ((entry & SALT_MASK) == salt) {
        int idx = (int) (entry & INDEX_MASK);
        if (keys[idx] == key) {
          return payloads[idx]; // existing entry
        }
      }

      // Linear probe - sequential memory access, cache-line friendly
      slot = (slot + 1) & mask;
    }
  }

  /**
   * Look up a key. Returns the payload, or -1 if not found.
   */
  public int get(long key) {
    int hash = hash(key);
    long salt = extractSalt(hash);
    int slot = hash & mask;

    while (true) {
      long entry = directory[slot];

      if (entry == EMPTY) {
        return -1; // not found
      }

      // Salt check first - rejects most non-matching slots without a cache miss
      if ((entry & SALT_MASK) == salt) {
        int idx = (int) (entry & INDEX_MASK);
        if (keys[idx] == key) {
          return payloads[idx];
        }
      }

      slot = (slot + 1) & mask;
    }
  }

  /**
   * Look up or insert: returns payload index for the key,
   * assigning the given default if absent.
   */
  public int getOrInsert(long key, int defaultPayload) {
    if (size >= capacity * LOAD_FACTOR) {
      resize();
    }

    int hash = hash(key);
    long salt = extractSalt(hash);
    int slot = hash & mask;

    while (true) {
      long entry = directory[slot];

      if (entry == EMPTY) {
        directory[slot] = salt | (size & INDEX_MASK);
        keys[size] = key;
        payloads[size] = defaultPayload;
        size++;
        return defaultPayload;
      }

      if ((entry & SALT_MASK) == salt) {
        int idx = (int) (entry & INDEX_MASK);
        if (keys[idx] == key) {
          return payloads[idx];
        }
      }

      slot = (slot + 1) & mask;
    }
  }

  /**
   * Batch probe: look up multiple keys at once.
   * Writing results to an output array avoids per-key method call overhead.
   */
  public void batchGet(long[] searchKeys, int offset, int length, int[] results) {
    for (int i = 0; i < length; i++) {
      results[i] = get(searchKeys[offset + i]);
    }
  }

  public int getSize() {
    return size;
  }

  public long getKey(int index) {
    return keys[index];
  }

  public int getPayload(int index) {
    return payloads[index];
  }

  public void setPayload(int index, int payload) {
    payloads[index] = payload;
  }

  public void clear() {
    java.util.Arrays.fill(directory, EMPTY);
    size = 0;
  }

  private void resize() {
    int newCapacity = capacity * 2;
    long[] oldDirectory = directory;
    long[] oldKeys = keys;
    int[] oldPayloads = payloads;
    int oldSize = size;

    this.capacity = newCapacity;
    this.mask = newCapacity - 1;
    this.directory = new long[newCapacity];
    this.keys = new long[newCapacity];
    this.payloads = new int[newCapacity];
    this.size = 0;

    // Reinsert all existing entries
    for (int i = 0; i < oldSize; i++) {
      long key = oldKeys[i];
      int hash = hash(key);
      long salt = extractSalt(hash);
      int slot = hash & mask;

      while (directory[slot] != EMPTY) {
        slot = (slot + 1) & mask;
      }

      directory[slot] = salt | (size & INDEX_MASK);
      keys[size] = key;
      payloads[size] = oldPayloads[i];
      size++;
    }
  }

  /**
   * Hash function using bit mixing (Murmur3 finalizer).
   * Provides better distribution than simple XOR shift.
   */
  private static int hash(long key) {
    key ^= key >>> 33;
    key *= 0xFF51AFD7ED558CCDL;
    key ^= key >>> 33;
    key *= 0xC4CEB9FE1A85EC53L;
    key ^= key >>> 33;
    return (int) key;
  }

  private static long extractSalt(int hash) {
    return ((long) (hash >>> 16) & 0xFFFFL) << SALT_SHIFT;
  }

  private static int nextPowerOfTwo(int n) {
    n--;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return n + 1;
  }
}
