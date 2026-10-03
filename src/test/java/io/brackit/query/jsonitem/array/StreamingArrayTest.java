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
package io.brackit.query.jsonitem.array;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.jdm.Iter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingArrayTest {

  private StreamingArray array(String json) {
    var input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    return (StreamingArray) new StreamingJSONParser(input, 16).parse();
  }

  private void assertRereadFails(Runnable read) {
    QueryException error = assertThrows(QueryException.class, read::run);
    assertEquals(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, error.getCode());
    assertTrue(error.getMessage().contains("StreamingArray cannot be reread"));
  }

  @Test
  void exhaustedStreamCannotBeReread() {
    StreamingArray array = array("[1,2,3,4,5]");
    try (Iter iter = array.iterate()) {
      for (int i = 1; i <= 5; i++) {
        assertEquals(new Int32(i), iter.next());
      }
      assertNull(iter.next());
      assertNull(iter.next());
    }
    assertRereadFails(array::iterate);
    assertRereadFails(array::values);
    assertRereadFails(array::length);
    assertRereadFails(array::len);
    assertRereadFails(() -> array.at(0));
    assertRereadFails(() -> array.range(Int32.ZERO, Int32.ONE));
  }

  @Test
  void partiallyConsumedStreamCannotBeReread() {
    StreamingArray array = array("[1,2,3,4,5]");
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
    }
    assertRereadFails(array::iterate);
    assertRereadFails(() -> array.at(Int32.ZERO));
    assertRereadFails(array::values);
    assertRereadFails(array::length);
    assertRereadFails(array::len);
    assertRereadFails(() -> array.range(Int32.ZERO, Int32.ONE));
  }

  @Test
  void iteratorsCreatedBeforeStreamingCannotShareParserCursor() {
    StreamingArray array = array("[1,2,3]");
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      assertEquals(Int32.ONE, first.next());
      assertRereadFails(second::next);
      assertEquals(new Int32(2), first.next());
      assertEquals(new Int32(3), first.next());
      assertNull(first.next());
    }
  }

  @Test
  void cachedPrefixDoesNotPermitRereadingUncachedSuffix() {
    StreamingArray array = array("[1,2,3]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      assertEquals(Int32.ONE, first.next());
      assertEquals(Int32.ONE, second.next());
      assertEquals(new Int32(2), first.next());
      assertRereadFails(second::next);
      assertRereadFails(() -> array.at(0));
      assertEquals(new Int32(3), first.next());
      assertNull(first.next());
    }
  }

  @Test
  void fullyMaterializedArrayCanBeReadRepeatedly() {
    StreamingArray array = array("[1,2,3,4,5,6,7,8,9]");
    assertEquals(9, array.values().size());
    for (int pass = 0; pass < 2; pass++) {
      try (Iter iter = array.iterate()) {
        for (int i = 1; i <= 9; i++) {
          assertEquals(new Int32(i), iter.next());
        }
        assertNull(iter.next());
      }
    }
    assertEquals(new Int32(9), array.length());
    assertEquals(Int32.ONE, array.at(0));
  }

  @Test
  void readingOnlyCachedElementsDoesNotPreventMaterialization() {
    StreamingArray array = array("[1,2,3]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertEquals(3, array.values().size());
      assertEquals(new Int32(2), iter.next());
      assertEquals(new Int32(3), iter.next());
      assertNull(iter.next());
    }
    assertEquals(3, array.len());
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
    }
  }

  @Test
  void exhaustingFullyCachedPrefixRemainsReplayable() {
    StreamingArray array = array("[1]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertNull(iter.next());
    }
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertNull(iter.next());
    }
    assertEquals(1, array.len());
  }

  @Test
  void emptyArrayCanBeReadRepeatedly() {
    StreamingArray array = array("[]");
    for (int pass = 0; pass < 2; pass++) {
      try (Iter iter = array.iterate()) {
        assertNull(iter.next());
      }
    }
    assertEquals(0, array.len());
  }

  @Test
  void unusedIteratorDoesNotConsumeStream() {
    StreamingArray array = array("[1,2]");
    try (Iter unused = array.iterate()) {
    }
    assertEquals(2, array.values().size());
    assertEquals(Int32.ONE, array.at(0));
  }
}
