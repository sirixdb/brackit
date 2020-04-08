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
package org.brackit.xquery;

import org.brackit.xquery.array.DArray;
import org.brackit.xquery.atomic.*;
import org.brackit.xquery.record.ArrayRecord;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/**
 * @author Johannes Lichtenberger
 */
public final class JsonTest extends XQueryBaseTest {
  @Test
  public void arrayUnboxing1Test() throws IOException {
    final var query = "let $json := [\"Sunday\", \"Monday\", \"Tuesday\", \"Wednesday\", \"Thursday\", \"Friday\", \"Saturday\"] return $json[[]]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("Sunday Monday Tuesday Wednesday Thursday Friday Saturday", content);
    }
  }

  @Test
  public void arrayUnboxing2Test() throws IOException {
    final var query = "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ],\n"
        + "          [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]\n" + "        ]\n return $json[[]]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("[\"mercury\",\"venus\",\"earth\",\"mars\"][\"monday\",\"tuesday\",\"wednesday\",\"thursday\"]", content);
    }
  }

  @Test
  public void arraySizeTest() throws IOException {
    final var query = "let $json := [\"mercury\",\"venus\",\"earth\",\"mars\"]\n return bit:len($json)";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("4", content);
    }
  }

  @Test
  public void nestedArrayTest() throws IOException {
    final var query = "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ],\n"
        + "          [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]\n" + "        ]\n return $json[[0]]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("[\"mercury\",\"venus\",\"earth\",\"mars\"]", content);
    }
  }

  @Test
  public void comletelyNestedArrayTest() throws IOException {
    final var query = "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ],\n"
        + "          [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]\n" + "        ]\n return $json[[1]][[1]]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("tuesday", content);
    }
  }

  @Test
  public void recordProjectionTest() throws IOException {
    final var query = "let $json := {\"key\": 3, \"foo\": 0}\n"
        + "return $json{key}";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"key\":3}", content);
    }
  }

  @Test
  public void forEachInRecordTest() throws IOException {
    final var query = "let $json := {\"key\": 3, \"foo\": 0}\nfor $key in bit:fields($json)\n where $json=>$key = 3\n"
        + "return { $key: $json=>$key }";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"key\":3}", content);
    }
  }

  @Test
  public void forEachInArrayTest() throws IOException {
    final var query = "for $i in [{\"key\": 3}, {\"key\": 0}]\n" + "where $i=>key = 0\n"
        + "return $i";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"key\":0}", content);
    }
  }

  @Test
  public void arrayTest() throws IOException {
    final var query = "[\"foo\",0,true(),jn:null()]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("[\"foo\",0,true,null]", content);
    }
  }

  @Test
  public void arrayValuesTest() throws IOException {
    final var query = "let $array := [\"foo\",0,true(),jn:null()]\nfor $i in $array\nreturn $i";
    final var resultSequence = new XQuery(query).execute(ctx);
    ResultChecker.check(new ItemSequence(new Str("foo"), new Int32(0), new Bool(true), new Null()), resultSequence);
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("foo 0 true null", content);
    }
  }

  @Test
  public void composableTest() {
    final var query = "{\"foo\":jn:null(),\"bar\":(1,2)}";
    final var resultSequence = new XQuery(query).execute(ctx);
    ResultChecker.check(new ItemSequence(new ArrayRecord(new QNm[] { new QNm("foo"), new QNm("bar") },
        new Sequence[] { new Null(), new DArray(new Int32(1), new Int32(2)) } )), resultSequence);
  }

  @Test
  public void testObjects() throws IOException {
    final var query = "    let $object1 := { \"Captain\" : \"Kirk\" }\n"
        + "    let $object2 := { \"First officer\" : \"Spock\" }\n" + "    return ($object1, \" \", $object2)";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"Captain\":\"Kirk\"} {\"First officer\":\"Spock\"}", content);
    }
  }

  @Test
  public void testComposeObjects1() throws IOException {
    final var query = "    let $object1 := { \"Captain\" : \"Kirk\" }\n"
        + "    let $object2 := { \"First officer\" : \"Spock\" }\n" + "    return { \"foobar\": $object1, $object2 }";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"foobar\":{\"Captain\":\"Kirk\"},\"First officer\":\"Spock\"}", content);
    }
  }

  @Test
  public void testComposeObjects2() throws IOException {
    final var query = "let $r := { \"x\":1, \"y\":2 } return {| $r, { \"z\":3 } |}";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"x\":1,\"y\":2,\"z\":3}", content);
    }
  }

  @Test
  public void testComposeObjects3() throws IOException {
    final var query = "          let $object1 := { \"Captain\" : \"Kirk\" }\n"
        + "          let $object2 := { \"First officer\" : \"Spock\" }\n" + "          return {| $object1, $object2 |}";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"Captain\":\"Kirk\",\"First officer\":\"Spock\"}", content);
    }
  }

  @Test
  public void testObjectLookup() throws IOException {
    final var query = "let $x := { \"eyes\": \"blue\", \"hair\": \"fuchsia\" }\n"
        + "        let $y := { \"eyes\": \"brown\", \"hair\": \"brown\" }\n"
        + "        return { \"eyes\": $x=>eyes, \"hair\": $y=>hair }";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"eyes\":\"blue\",\"hair\":\"brown\"}", content);
    }
  }

  @Test
  public void testGetAllKeys() throws IOException {
    final var query = "let $seq := (\"foo\", [ 1, 2, 3 ], { \"a\" : 1, \"b\" : 2 }, { \"a\" : 3, \"c\" : 4 })\n"
        + "          return jn:keys($seq)";
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("a b c", content);
    }
  }

  @Test
  public void predicateClauseTest() throws IOException {
    final var query = "{\"key\": 3, \"foo\": 0}[.=>key=3]";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"key\":3,\"foo\":0}", content);
    }
  }

  @Test
  public void dynamicPairsTest() throws IOException {
    final var query = "{| for $i in 1 to 10 return { concat(\"Square of \", $i) : $i * $i } |}";

    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      final var content = new String(out.toByteArray(), StandardCharsets.UTF_8);
      assertEquals("{\"Square of 1\":1,\"Square of 2\":4,\"Square of 3\":9,\"Square of 4\":16,\"Square of 5\":25,\"Square of 6\":36,\"Square of 7\":49,\"Square of 8\":64,\"Square of 9\":81,\"Square of 10\":100}", content);
    }
  }
}
