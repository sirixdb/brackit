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
 * Regression test for the JSONiq array constructor {@code [ Expr ]}. A multi-item
 * operand must expand to one array member per item, whatever sequence kind produced
 * it. Previously only the {@code (a,b,c)} comma constructor was flattened, so an
 * array built from a FLWOR ({@code [ for $i in ... return ... ]}) stored the whole
 * sequence as a single member and every subsequent access ({@code []} unboxing,
 * indexing, {@code count}) failed with {@code XPTY0004}.
 */
public final class ArrayConstructorFlattenTest extends XQueryBaseTest {

  private String query(String q) throws Exception {
    try (var out = new ByteArrayOutputStream()) {
      new Query(q).serialize(ctx, new PrintStream(out));
      return out.toString(StandardCharsets.UTF_8);
    }
  }

  @Test
  public void flworOperandFlattensToMembers() throws Exception {
    assertEquals("[1,2,3]", query("[ for $i in 1 to 3 return $i ]"));
    assertEquals("1 2 3", query("[ for $i in 1 to 3 return $i ][]"));
    assertEquals("3", query("count([ for $i in 1 to 3 return $i ][])"));
    assertEquals("[{\"v\":1},{\"v\":2},{\"v\":3}]", query("[ for $i in 1 to 3 return { \"v\": $i } ]"));
  }

  @Test
  public void commaSequenceStillFlattens() throws Exception {
    assertEquals("[1,2,3]", query("[ (1, 2, 3) ]"));
    assertEquals("[1,2,3,4]", query("[ 1, (2, 3), 4 ]"));
  }

  @Test
  public void singleItemsAndNestedArraysStayOneMember() throws Exception {
    assertEquals("[{\"a\":1}]", query("[ { \"a\": 1 } ]"));
    assertEquals("[[1,2,3]]", query("[ [1, 2, 3] ]")); // nested array is one item, one member
    assertEquals("[1,2,3]", query("[1, 2, 3]"));
  }

  @Test
  public void postGroupProjectionOverFlworArray() throws Exception {
    // The shape the bug originally surfaced through: group-by + sum of a projected
    // field, over an array built by a FLWOR.
    final String q = """
        let $data := [ for $i in 1 to 4 return { "d": $i mod 2, "v": $i } ]
        return string-join(
          for $u in $data[] let $g := $u.d group by $g order by $g
          return $g || ":" || sum($u.v), ",")
        """;
    assertEquals("0:6,1:4", query(q)); // g=0: v 2+4=6; g=1: v 1+3=4
  }
}
