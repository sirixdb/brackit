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
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Null;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.record.ArrayRecord;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * @author Johannes Lichtenberger
 */
public final class JsonTest extends XQueryBaseTest {

  private static final Path JSON_RESOURCES = Paths.get("src", "test", "resources", "json");

  @Test
  public void testDerefExpr() throws IOException {
    final URI docUri = JSON_RESOURCES.resolve("multiple-revisions.json").toUri();
    final var query = String.format("let $result := jn:parse(io:read('%s'))=>sirix[[2]]=>revision=>tada[.=>foo=>baz = 'bar'] return $result", docUri.toString());
    final var result = query(query);
    assertEquals("[{\"foo\":\"bar\"},{\"baz\":false},\"boo\",{},[{\"foo\":[true,{\"baz\":\"bar\"}]}]]", result);
  }

  @Test
  public void testSimpleRecord() throws IOException {
    final var query = "{\"key\":jn:null()}";
    final var result = query(query);
    assertEquals("{\"key\":null}", result);
  }

  @Test
  public void testNestedDerefsWithArrays() throws IOException {
    final var query =
        "[{\"key\":0},{\"value\":[{\"key\":{\"boolean\":true()}},{\"newkey\":\"yes\"}]},{\"key\":\"hey\",\"value\":false()}]=>value=>key=>boolean";
    final var result = query(query);
    assertEquals("true", result);
  }

  @Test
  public void testArray() throws IOException {
    final var query =
        "{\"foo\": [\"bar\", jn:null(), 2.33],\"bar\": {\"hello\": \"world\", \"helloo\": true()},\"baz\": \"hello\",\"tada\": [{\"foo\": \"bar\"}, {\"baz\": false()}, \"boo\", {}, []]}=>foo";
    final var result = query(query);
    assertEquals("[\"bar\",null,2.33]", result);
  }

  @Test
  public void nestedExpressionsTest() throws IOException {
    final var json = Files.readString(JSON_RESOURCES.resolve("user_profiles.json"));
    final var query = json + "=>websites=>description";
    final var result = query(query);
    assertEquals("work tutorials", result);
  }

  @Test
  public void nestedExpressionsWithPredicateTest() throws IOException {
    final var json = Files.readString(JSON_RESOURCES.resolve("user_profiles.json"));
    final var query = json + "=>websites[[]][.=>description eq \"work\"]{description}";
    final var result = query(query);
    assertEquals("{\"description\":\"work\"}", result);
  }

  @Test
  public void arbitraryExpressionsTest() throws IOException {
    final var query = "{ \"a\" : concat('f', 'oo') , 'b' : 1+1, \"c\" : [1,2,3] }";
    final var result = query(query);
    assertEquals("{\"a\":\"foo\",\"b\":2,\"c\":[1,2,3]}", result);
  }

  @Test
  public void singleQuotedFieldValuesTest() throws IOException {
    final var query = "{ 'b': 2, \"c\": 3 }";
    final var result = query(query);
    assertEquals("{\"b\":2,\"c\":3}", result);
  }

  @Test
  public void arrayForLoop1Test() throws IOException {
    final var query = "let $values := [{\"key\": \"hey\"}, {\"key\": 0}]\n" + "for $i in $values\n"
        + "where $i=>key instance of xs:integer and $i=>key eq 0 \n return $i";
    final var result = query(query);
    assertEquals("{\"key\":0}", result);
  }

  @Test
  public void arrayForLoop2Test() throws IOException {
    final var query = "let $values := [\"foo\",0,true(),jn:null()]\n" + "for $i in $values\n" + "return $i";
    final var result = query(query);
    assertEquals("foo 0 true null", result);
  }

  @Test
  public void arrayUnboxing1Test() throws IOException {
    final var query =
        "let $json := [\"Sunday\", \"Monday\", \"Tuesday\", \"Wednesday\", \"Thursday\", \"Friday\", \"Saturday\"] return $json[[]]";
    final var result = query(query);
    assertEquals("Sunday Monday Tuesday Wednesday Thursday Friday Saturday", result);
  }

  @Test
  public void arrayUnboxing2Test() throws IOException {
    final var query =
        "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ], [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]] return $json[[]]";
    final var result = query(query);
    assertEquals("[\"mercury\",\"venus\",\"earth\",\"mars\"][\"monday\",\"tuesday\",\"wednesday\",\"thursday\"]",
                 result);
  }

  @Test
  public void arraySizeTest() throws IOException {
    final var query = "let $json := [\"mercury\",\"venus\",\"earth\",\"mars\"]\n return bit:len($json)";
    final var result = query(query);
    assertEquals("4", result);
  }

  @Test
  public void nestedArrayTest() throws IOException {
    final var query =
        "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ], [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]] return $json[[0]]";
    final var result = query(query);
    assertEquals("[\"mercury\",\"venus\",\"earth\",\"mars\"]", result);
  }

  @Test
  public void comletelyNestedArrayTest() throws IOException {
    final var query =
        "let $json := [[ \"mercury\", \"venus\", \"earth\", \"mars\" ], [ \"monday\", \"tuesday\", \"wednesday\", \"thursday\" ]] return $json[[1]][[1]]";
    final var result = query(query);
    assertEquals("tuesday", result);
  }

  @Test
  public void recordProjectionTest() throws IOException {
    final var query = "let $json := {\"key\": 3, \"foo\": 0} return $json{key}";
    final var result = query(query);
    assertEquals("{\"key\":3}", result);
  }

  @Test
  public void forEachInRecordTest() throws IOException {
    final var query = "let $json := {\"key\": 3, \"foo\": 0} for $key in bit:fields($json) where $json=>$key eq 3\n"
        + "return { $key: $json=>$key }";
    final var result = query(query);
    assertEquals("{\"key\":3}", result);
  }

  @Test
  public void forEachInArrayTest() throws IOException {
    final var query = "for $i in [{\"key\": 3}, {\"key\": 0}] where $i=>key eq 0 return $i";
    final var result = query(query);
    assertEquals("{\"key\":0}", result);
  }

  @Test
  public void arrayTest() throws IOException {
    final var query = "[\"foo\",0,true(),jn:null()]";
    final var result = query(query);
    assertEquals("[\"foo\",0,true,null]", result);
  }

  @Test
  public void arrayValuesTest() throws IOException {
    final var query = "let $array := [\"foo\",0,true(),jn:null()] for $i in $array return $i";
    final var result = query(query);
    assertEquals("foo 0 true null", result);
  }

  @Test
  public void composableTest() {
    final var query = "{\"foo\":jn:null(),\"bar\":(1,2)}";
    final var resultSequence = new XQuery(query).execute(ctx);
    ResultChecker.check(new ItemSequence(new ArrayRecord(new QNm[] { new QNm("foo"), new QNm("bar") },
                                                         new Sequence[] { new Null(),
                                                             new DArray(new Int32(1), new Int32(2)) })),
                        resultSequence);
  }

  @Test
  public void testObjects() throws IOException {
    final var query =
        "    let $object1 := { \"Captain\" : \"Kirk\" }\n" + "    let $object2 := { \"First officer\" : \"Spock\" }\n"
            + "    return ($object1, \" \", $object2)";
    final var result = query(query);
    assertEquals("{\"Captain\":\"Kirk\"} {\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects1() throws IOException {
    final var query =
        "    let $object1 := { \"Captain\" : \"Kirk\" }\n" + "    let $object2 := { \"First officer\" : \"Spock\" }\n"
            + "    return {| { \"foobar\": $object1 }, $object2 |}";
    final var result = query(query);
    assertEquals("{\"foobar\":{\"Captain\":\"Kirk\"},\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects2() throws IOException {
    final var query =
        "let $object1 := { \"Captain\" : \"Kirk\" }\n" + "let $object2 := { \"First officer\" : \"Spock\" }\n"
            + "return {{ \"foobar\": $object1 }, $object2 }";
    final var result = query(query);
    assertEquals("{\"foobar\":{\"Captain\":\"Kirk\"},\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects3() throws IOException {
    final var query = "let $r := { \"x\":1, \"y\":2 } return {| $r, { \"z\":3 } |}";
    final var result = query(query);
    assertEquals("{\"x\":1,\"y\":2,\"z\":3}", result);
  }

  @Test
  public void testComposeObjects4() throws IOException {
    final var query =
        "let $object1 := { \"Captain\" : \"Kirk\" }\n" + "let $object2 := { \"First officer\" : \"Spock\" }\n"
            + "return {| $object1, $object2 |}";
    final var result = query(query);
    assertEquals("{\"Captain\":\"Kirk\",\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects5() throws IOException {
    final var query =
        "let $object1 := { \"Captain\" : \"Kirk\" } let $object2 := { \"First officer\" : \"Spock\" } return { $object1, $object2 }";
    final var result = query(query);
    assertEquals("{\"Captain\":\"Kirk\",\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testObjectLookup() throws IOException {
    final var query = "let $x := { \"eyes\": \"blue\", \"hair\": \"fuchsia\" }\n"
        + "        let $y := { \"eyes\": \"brown\", \"hair\": \"brown\" }\n"
        + "        return { \"eyes\": $x=>eyes, \"hair\": $y=>hair }";
    final var result = query(query);
    assertEquals("{\"eyes\":\"blue\",\"hair\":\"brown\"}", result);
  }

  @Test
  public void testGetAllKeys() throws IOException {
    final var query =
        "let $seq := (\"foo\", [ 1, 2, 3 ], { \"a\" : 1, \"b\" : 2 }, { \"a\" : 3, \"c\" : 4 }) return jn:keys($seq)";
    final var result = query(query);
    assertEquals("a b c", result);
  }

  @Test
  public void predicateClauseTest() throws IOException {
    final var query = "{\"key\": 3, \"foo\": 0}[.=>key eq 3]";
    final var result = query(query);
    assertEquals("{\"key\":3,\"foo\":0}", result);
  }

  @Test
  public void dynamicPairsTest1() throws IOException {
    final var query =
        "{ for $d at $i in (\"Sunday\", \"Monday\", \"Tuesday\", \"Wednesday\", \"Thursday\", \"Friday\", \"Saturday\" ) return { $d : $i } }";
    final var result = query(query);
    assertEquals("{\"Sunday\":1,\"Monday\":2,\"Tuesday\":3,\"Wednesday\":4,\"Thursday\":5,\"Friday\":6,\"Saturday\":7}",
                 result);
  }

  @Test
  public void dynamicPairsTest2() throws IOException {
    final var query = "{| for $i in 1 to 10 return { concat(\"Square of \", $i) : $i * $i } |}";
    final var result = query(query);
    assertEquals(
        "{\"Square of 1\":1,\"Square of 2\":4,\"Square of 3\":9,\"Square of 4\":16,\"Square of 5\":25,\"Square of 6\":36,\"Square of 7\":49,\"Square of 8\":64,\"Square of 9\":81,\"Square of 10\":100}",
        result);
  }

  @Test
  public void dynamicPairsTest3() throws IOException {
    final var query = "let $a := ({\"height\": 5.2}, {\"eyes\": \"blue\"}) return {| $a |}";
    final var result = query(query);
    assertEquals("{\"height\":5.2,\"eyes\":\"blue\"}", result);
  }

  private String query(final String query) throws IOException {
    try (final var out = new ByteArrayOutputStream()) {
      new XQuery(query).serialize(ctx, new PrintStream(out));
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }
}
