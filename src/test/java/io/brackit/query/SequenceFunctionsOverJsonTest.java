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
package io.brackit.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The general sequence functions fn:subsequence, fn:reverse, fn:remove and fn:insert-before operate
 * on {@code item()*} (XPath F&amp;O §14) and must NOT atomize their input. They used to be predefined
 * with an additional {@code xs:anyAtomicType*} signature that took precedence, so the function
 * conversion atomized every item — a sequence of objects or arrays then failed with
 * {@code FOTY0012 ("The atomized value of record items is undefined")}. These tests pin that the
 * functions now pass structured items through untouched, while atomic inputs still work.
 */
public final class SequenceFunctionsOverJsonTest extends XQueryBaseTest {

  /** An unboxed array of three objects: {@code ({"a":1}, {"a":2}, {"a":3})}. */
  private static final String OBJS = "([{\"a\":1},{\"a\":2},{\"a\":3}][])";

  private String query(String q) throws Exception {
    try (var out = new ByteArrayOutputStream()) {
      new Query(q).serialize(ctx, new PrintStream(out));
      return out.toString(StandardCharsets.UTF_8);
    }
  }

  @Test
  public void subsequenceOverObjects() throws Exception {
    assertEquals("2", query("count(subsequence(" + OBJS + ", 1, 2))"));
    assertEquals("2 3", query("for $x in subsequence(" + OBJS + ", 2, 2) return $x.a"));
    // two-arg form (no length) keeps the rest of the sequence
    assertEquals("2 3", query("for $x in subsequence(" + OBJS + ", 2) return $x.a"));
  }

  @Test
  public void reverseOverObjects() throws Exception {
    assertEquals("3 2 1", query("for $x in reverse(" + OBJS + ") return $x.a"));
  }

  @Test
  public void removeOverObjects() throws Exception {
    assertEquals("1 3", query("for $x in remove(" + OBJS + ", 2) return $x.a"));
  }

  @Test
  public void insertBeforeOverObjects() throws Exception {
    assertEquals("1 9 2 3", query("for $x in insert-before(" + OBJS + ", 2, {\"a\":9}) return $x.a"));
  }

  @Test
  public void overArrayItems() throws Exception {
    // arrays are also non-atomizable; subsequence must pass them through
    assertEquals("2", query("count(subsequence(([[1],[2],[3]][]), 1, 2))"));
  }

  @Test
  public void atomicInputsStillWork() throws Exception {
    // regression guard: the item()* signature must not change atomic behaviour
    assertEquals("20 30", query("subsequence((10,20,30,40,50), 2, 2)"));
    assertEquals("5 4 3 2 1", query("reverse((1,2,3,4,5))"));
    assertEquals("1 3 4", query("remove((1,2,3,4), 2)"));
  }
}
