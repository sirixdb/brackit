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
package io.brackit.query.util.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.brackit.query.Tuple;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jsonitem.array.DArray;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.util.sort.TupleSerializer.SpillContext;

/**
 * A pipeline tuple carries every variable in scope, including the {@code let $doc := jn:doc(...)}
 * the query started from. Rendering such a column as JSON text per spilled row serializes the whole
 * document per row — which is how a 10M-row grouping ran out of heap inside the spill it had just
 * been taught to do. Within one JVM the spill files belong to the operator that wrote them, so an
 * oversized column is written as a reference to the live item instead.
 */
final class TupleSerializerSpillContextTest {

  /** Comfortably past the 64K-character inline limit once rendered. */
  private static final int BIG_ITEMS = 40_000;

  @Test
  void anOversizedColumnIsReferenced_notCopied() throws IOException {
    final Sequence big = bigArray();
    final Tuple tuple = new TupleImpl(new Sequence[] { big, new Str("small") });
    final SpillContext ctx = new SpillContext();

    final byte[] bytes = write(tuple, ctx);

    assertEquals(1, ctx.referencedItems(), "the oversized column must be held, not copied");
    assertTrue(bytes.length < 1024, "the spilled row must stay small, was " + bytes.length + " bytes");

    final Tuple back = TupleSerializer.read(new ByteArrayInputStream(bytes), ctx);
    assertSame(big, back.get(0), "a referenced column comes back as the very same item");
    assertEquals("small", ((Str) back.get(1)).stringValue());
  }

  @Test
  void theSameOversizedItemIsHeldOnlyOnce() throws IOException {
    final Sequence big = bigArray();
    final SpillContext ctx = new SpillContext();
    for (int row = 0; row < 16; row++) {
      write(new TupleImpl(new Sequence[] { big, new Int64(row) }), ctx);
    }
    assertEquals(1, ctx.referencedItems(), "one item, one reference, however many rows carry it");
  }

  @Test
  void smallColumnsStillTravelByValue() throws IOException {
    final Sequence small = new DArray(List.of(new Int64(1), new Int64(2), new Int64(3)));
    final SpillContext ctx = new SpillContext();

    final Tuple back = TupleSerializer.read(new ByteArrayInputStream(write(new TupleImpl(new Sequence[] { small }),
                                                                           ctx)), ctx);

    assertEquals(0, ctx.referencedItems(), "a small column must not pin anything");
    assertTrue(back.get(0) instanceof Array, "and it must come back as an array");
  }

  @Test
  void withoutASessionEverythingIsCopied() throws IOException {
    final Sequence big = bigArray();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    TupleSerializer.write(out, new TupleImpl(new Sequence[] { big }));
    assertTrue(out.size() > 64 * 1024, "no session means no references, so the column is written out in full");
  }

  private static byte[] write(Tuple tuple, SpillContext ctx) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    TupleSerializer.write(out, tuple, ctx);
    return out.toByteArray();
  }

  private static Sequence bigArray() {
    final List<Sequence> values = new ArrayList<>(BIG_ITEMS);
    for (int i = 0; i < BIG_ITEMS; i++) {
      values.add(new Int64(1_000_000_000L + i));
    }
    return new DArray(values);
  }
}
