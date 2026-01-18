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
package io.brackit.query.util.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.util.simd.VectorOps;

/**
 * SIMD-optimized hash join table.
 * Provides specialized handling for Int64 and Str keys with vectorized operations.
 *
 * For Int64 keys, uses a primitive long array hash table with SIMD probing.
 * For other types, falls back to standard HashMap.
 *
 * @author Brackit Project Team
 */
public final class SIMDHashJoinTable extends AbstractJoinTable {

  /**
   * Initial size for the Int64 hash table.
   * Must be a power of 2.
   */
  private static final int INITIAL_INT64_TABLE_SIZE = 1024;

  /**
   * Load factor threshold for resizing.
   */
  private static final float LOAD_FACTOR = 0.75f;

  /**
   * Marker for empty slots in the Int64 table.
   */
  private static final long EMPTY_KEY = Long.MIN_VALUE;

  // Int64 optimized table (open addressing with linear probing)
  private long[] int64Keys;
  private TValue[] int64Values;
  private int int64TableMask;
  private int int64Size = 0;
  private boolean useInt64Table = true;

  // Fallback table for non-Int64 keys
  private final Map<TKey, TValue> genericTable = new HashMap<>();

  public SIMDHashJoinTable() {
    initInt64Table(INITIAL_INT64_TABLE_SIZE);
  }

  private void initInt64Table(int size) {
    int64Keys = new long[size];
    int64Values = new TValue[size];
    int64TableMask = size - 1;

    // Fill with empty markers
    for (int i = 0; i < size; i++) {
      int64Keys[i] = EMPTY_KEY;
    }
  }

  @Override
  protected void add(Atomic key, int pos, Sequence[] bindings) throws QueryException {
    TValue htValue = new TValue(bindings, pos);

    // Try Int64 optimized path
    if (useInt64Table && key instanceof Int64 i64) {
      long keyValue = i64.longValue();

      // Handle collision with EMPTY_KEY marker
      if (keyValue == EMPTY_KEY) {
        // Fall back to generic table for this edge case
        addToGenericTable(key, pos, htValue);
        return;
      }

      addInt64(keyValue, htValue, pos);
      return;
    }

    // Fall back to generic table
    useInt64Table = false;
    addToGenericTable(key, pos, htValue);
  }

  private void addInt64(long key, TValue htValue, int pos) {
    // Check if resize needed
    if (int64Size >= int64Keys.length * LOAD_FACTOR) {
      resizeInt64Table();
    }

    int hash = hashLong(key) & int64TableMask;

    // Linear probing
    while (true) {
      long existingKey = int64Keys[hash];

      if (existingKey == EMPTY_KEY) {
        // Empty slot - insert
        int64Keys[hash] = key;
        int64Values[hash] = htValue;
        int64Size++;
        return;
      }

      if (existingKey == key) {
        // Key exists - chain the value if pos is different
        TValue chain = int64Values[hash];
        TValue p = null;
        while (chain != null) {
          if (chain.pos == pos) {
            return; // Already exists with same pos
          }
          p = chain;
          chain = chain.next;
        }
        if (p != null) {
          p.next = htValue;
        }
        return;
      }

      // Collision - linear probe
      hash = (hash + 1) & int64TableMask;
    }
  }

  private void resizeInt64Table() {
    int newSize = int64Keys.length * 2;
    long[] oldKeys = int64Keys;
    TValue[] oldValues = int64Values;

    initInt64Table(newSize);
    int64Size = 0;

    // Rehash
    for (int i = 0; i < oldKeys.length; i++) {
      if (oldKeys[i] != EMPTY_KEY) {
        addInt64(oldKeys[i], oldValues[i], oldValues[i].pos);
      }
    }
  }

  private void addToGenericTable(Atomic key, int pos, TValue htValue) {
    TKey htKey = new TKey(key);
    TValue chain = genericTable.get(htKey);

    if (chain == null) {
      genericTable.put(htKey, htValue);
    } else {
      TValue p = null;
      while (chain != null) {
        if (chain.pos == pos) {
          return;
        }
        p = chain;
        chain = chain.next;
      }
      if (p != null) {
        p.next = htValue;
      }
    }
  }

  @Override
  protected void lookup(FastList<TValue> matches, Atomic key) throws QueryException {
    // Try Int64 optimized path
    if (useInt64Table && key instanceof Int64 i64) {
      long keyValue = i64.longValue();

      // Handle EMPTY_KEY marker collision
      if (keyValue == EMPTY_KEY) {
        lookupGeneric(matches, key);
        return;
      }

      lookupInt64(matches, keyValue);
      return;
    }

    // Fall back to generic table
    lookupGeneric(matches, key);
  }

  private void lookupInt64(FastList<TValue> matches, long key) {
    // Use SIMD hash probe for finding the slot
    int idx = VectorOps.hashProbe(int64Keys, int64Keys.length, key);

    if (idx >= 0 && int64Keys[idx] == key) {
      TValue htValue = int64Values[idx];
      while (htValue != null) {
        matches.add(htValue);
        htValue = htValue.next;
      }
    }
  }

  private void lookupGeneric(FastList<TValue> matches, Atomic key) {
    TKey htKey = new TKey(key);
    TValue htValue = genericTable.get(htKey);

    while (htValue != null) {
      matches.add(htValue);
      htValue = htValue.next;
    }
  }

  /**
   * Batch lookup for multiple Int64 keys.
   * Computes hashes in batch using SIMD for better throughput.
   *
   * @param keys    Array of Int64 keys to lookup
   * @param offset  Start offset in array
   * @param length  Number of keys to lookup
   * @param results Array to receive match counts
   * @return Total number of matches found
   */
  public int batchLookupInt64(long[] keys, int offset, int length, FastList<TValue>[] results) {
    if (!useInt64Table) {
      return 0;
    }

    // Compute all hashes in batch
    int[] hashes = new int[length];
    VectorOps.computeHashes(keys, offset, length, hashes, int64TableMask);

    int totalMatches = 0;
    for (int i = 0; i < length; i++) {
      long searchKey = keys[offset + i];
      if (searchKey == EMPTY_KEY) {
        continue;
      }

      // Linear probe from computed hash
      int hash = hashes[i];
      while (true) {
        long existingKey = int64Keys[hash];
        if (existingKey == EMPTY_KEY) {
          break; // Not found
        }
        if (existingKey == searchKey) {
          // Found - collect matches
          TValue htValue = int64Values[hash];
          while (htValue != null) {
            results[i].add(htValue);
            totalMatches++;
            htValue = htValue.next;
          }
          break;
        }
        hash = (hash + 1) & int64TableMask;
      }
    }

    return totalMatches;
  }

  @Override
  protected List<TEntry> entries() {
    final var entries = new ArrayList<TEntry>();

    // Int64 entries
    for (int i = 0; i < int64Keys.length; i++) {
      if (int64Keys[i] != EMPTY_KEY) {
        TKey key = new TKey(new Int64(int64Keys[i]));
        for (TValue v = int64Values[i]; v != null; v = v.next) {
          entries.add(new TEntry(key, v));
        }
      }
    }

    // Generic entries
    for (Map.Entry<TKey, TValue> entry : genericTable.entrySet()) {
      for (TValue v = entry.getValue(); v != null; v = v.next) {
        entries.add(new TEntry(entry.getKey(), v));
      }
    }

    return entries;
  }

  /**
   * Fast hash function for long values.
   * Uses bit mixing for better distribution.
   */
  private static int hashLong(long key) {
    return (int) (key ^ (key >>> 32));
  }
}
