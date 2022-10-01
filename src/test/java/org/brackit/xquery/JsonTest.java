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

import org.brackit.xquery.compiler.CompileChain;
import org.brackit.xquery.jsonitem.array.DArray;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Null;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.jsonitem.object.ArrayObject;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Johannes Lichtenberger
 */
public final class JsonTest extends XQueryBaseTest {

  private static final Path JSON_RESOURCES = Paths.get("src", "test", "resources", "json");

  @Ignore
  @Test
  public void testDescVarDeref() throws IOException {
    final String query = """
        let $object := {"blabla":{"foo":{"baz":{"foo":"bar"}}}}
        let $foo := "foo"
        let $baz := "baz"
        let $sequence := $object=.$foo.$baz.foo
        return $sequence
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void testVarDeref() throws IOException {
    final String query = """
        let $object := {"foo":{"baz":{"foo":"bar"}}}
        let $foo := "foo"
        let $baz := "baz"
        let $sequence := $object.$foo.$baz.foo
        return $sequence
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void testVarDeref2() throws IOException {
    final String query = """
        let $array := [true,false,"true",{"foo":["tada",{"baz":["yes","no",null],"bar": null, "foobar":"text"},{"baz":true},{"baz":{"foo":"bar"}}]}]
        let $sequence := $array[].foo[]
        let $baz := "baz"
        let $sequence2 := $sequence.$baz.foo
        return $sequence2
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void testDeref() throws IOException {
    final String query = """
        let $object := {"foo":{"baz":{"foo":"bar"}}}
        let $foo := "foo"
        let $baz := "baz"
        let $sequence := $object.foo.baz.foo
        return $sequence
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void customModule() throws IOException {
    final var compileChain = new CompileChain();
    try (final var out = new ByteArrayOutputStream()) {
      final Path currentRelativePath =
          Paths.get("").resolve("src").resolve("test").resolve("resources").resolve("modules").resolve("sort.xq");
      final String currentPath = currentRelativePath.toAbsolutePath().toString();
      final String query = """
          import module namespace sort = "https://sirix.io/ns/sort" at "%path";
                    
          sort:qsort((7,8,4,5,6,9,3,2,0,1))
          """.replace("%path", currentPath);

      final var xquery = new XQuery(compileChain, query);
      xquery.serialize(ctx, new PrintStream(out));
    }
  }

  @Test
  public void ampersandInFieldAndValue() throws IOException {
    final String query = """
        {"bar & baz":"foo & bar"}
        """;
    final var result = query(query);
    assertEquals("{\"bar & baz\":\"foo & bar\"}", result);
  }

  @Test
  public void random() throws IOException {
    final String query = """
        let $array := [true,false,"true",{"foo":["tada",{"baz":["yes","no",null],"bar": null, "foobar":"text"},{"baz":true}]}]
        let $sequence := $array[].foo[[1]]{baz,foobar}
        let $resultArray := for $item in bit:values($sequence)
                            where $item instance of array()
                            return $item
        return $resultArray
        """;
    final var result = query(query);
    assertEquals("[\"yes\",\"no\",null]", result);
  }

  @Test
  public void join() throws IOException {
    final String query = """
        let $stores :=
        [
          { "store number" : 1, "state" : "MA" },
          { "store number" : 2, "state" : "MA" },
          { "store number" : 3, "state" : "CA" },
          { "store number" : 4, "state" : "CA" }
        ]
        let $sales := [
           { "product" : "broiler", "store number" : 1, "quantity" : 20  },
           { "product" : "toaster", "store number" : 2, "quantity" : 100 },
           { "product" : "toaster", "store number" : 2, "quantity" : 50 },
           { "product" : "toaster", "store number" : 3, "quantity" : 50 },
           { "product" : "blender", "store number" : 3, "quantity" : 100 },
           { "product" : "blender", "store number" : 3, "quantity" : 150 },
           { "product" : "socks", "store number" : 1, "quantity" : 500 },
           { "product" : "socks", "store number" : 2, "quantity" : 10 },
           { "product" : "shirt", "store number" : 3, "quantity" : 10 }
        ]
        let $join :=
          for $store in $stores, $sale in $sales
          where $store."store number" = $sale."store number"
          return {
            "nb" : $store."store number",
            "state" : $store.state,
            "sold" : $sale.product
          }
        return [$join]
        """;
    final var result = query(query);
    assertEquals(Files.readString(JSON_RESOURCES.resolve("joinresult.json")), result);
  }

  @Ignore
  @Test
  public void testDynamicFunction() throws IOException {
    final String query = """
        xquery version "3.0";
        declare namespace db="http://sirix.io/xquery/db";
        declare function db:map($func, $list) {
            for $item in $list return $func($item)
        };
        let $fun := function($x) { $x * $x }
        return db:map($fun, 1 to 5)
                """;
    final var result = query(query);
    assertEquals("{\"foo\":0} bar {\"baz\":true}", result);
  }

  @Test
  public void testInlineFunction() throws IOException {
    final String query = """
        let $fun := function($x, $z) { $x * $x * $z }
        return $fun(2,3)
                """;
    final var result = query(query);
    assertEquals("12", result);
  }

  @Test
  public void testInlineFunctionClosure1() throws IOException {
    final String query = """
        let $c := 4
        let $d := 5
        let $e := function($a) { $a }
        let $fun := function($a, $b) { ($a * $c) + ($b * $d) + $e(5) }
        let $result := $fun(2,3)
        return $result * 3
        """;
    final var result = query(query);
    assertEquals("84", result);
  }

  @Test
  public void testInlineFunctionClosure2() throws IOException {
    final String query = """
        let $c := 4
        let $d := 5
        let $e := function($a) { $a }
        let $fun := function($a, $b, $d) { ($a * $c) + ($b * $d) + $e(5) }
        return $fun(2,3,7)
            """;
    final var result = query(query);
    assertEquals("34", result);
  }

  @Test
  public void testFunction1() throws IOException {
    final String query = """
         declare function local:foobar($x as xs:integer,$y as xs:integer,$z as xs:integer) as xs:integer {
             $x * $y * $z
         };
         local:foobar(2,3,7)
            """;
    final var result = query(query);
    assertEquals("42", result);
  }

  @Ignore
  @Test
  public void testPartialFunctionApplication1() throws IOException {
    final String query = """
         declare function local:foobar($x as xs:integer,$y as xs:integer,$z as xs:integer) as xs:integer {
             $x * $y * $z
         };
         let $partFunc := local:foobar(2,?,7)
         return $partFunc(1)
            """;
    final var result = query(query);
    assertEquals("14", result);
  }

  @Ignore
  @Test
  public void testPartialFunctionApplication2() throws IOException {
    final String query = """
        let $fun := function($a, $b, $c) { $a * $b * $c }
        let $fun1 := $fun(2,?,7)
        return $fun1(1)
            """;
    final var result = query(query);
    assertEquals("34", result);
  }

  @Ignore
  @Test
  public void testFunction() throws IOException {
    final String query = """
        let $fun := function($a, $b, $c) { $a * $b * $c }
        let $fun1 := $fun(2,?,7)
        return $fun1(1)
            """;
    final var result = query(query);
    assertEquals("34", result);
  }

  @Test
  public void testObjectWithNullAndBooleans() throws IOException {
    final String query = """
          {"foo":null,"tada":"bar","bar":true,"baz":false}
        """;
    final var result = query(query);
    assertEquals("{\"foo\":null,\"tada\":\"bar\",\"bar\":true,\"baz\":false}", result);
  }

  @Test
  public void arrayUnboxing1() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true()}]
          return $array()
        """;
    final var result = query(query);
    assertEquals("{\"foo\":0} bar {\"baz\":true}", result);
  }

  @Test
  public void objectUnboxing() throws IOException {
    final String query = """
          let $object := {"foo": 0, "bar": true(), "baz":"tada"}
          return $object()
        """;
    final var result = query(query);
    assertEquals("foo bar baz", result);
  }

  @Test
  public void objectLookup() throws IOException {
    final String query = """
          let $object := {"foo": 0, "bar": true(), "baz":"tada"}
          return $object("foo")
        """;
    final var result = query(query);
    assertEquals("0", result);
  }

  @Test
  public void arrayLookup() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true()}]
          return $array(0)
        """;
    final var result = query(query);
    assertEquals("{\"foo\":0}", result);
  }

  @Test
  public void arrayIndex() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true()}]
          return $array[[1]]
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void negativeArrayIndex() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true}]
          return $array[[-1]]
        """;
    final var result = query(query);
    assertEquals("{\"baz\":true}", result);
  }

  @Test
  public void negativeArrayIndex1() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true}]
          return $array[[-2]]
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void negativeArrayIndex2() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true}]
          return $array[[-3]]
        """;
    final var result = query(query);
    assertEquals("{\"foo\":0}", result);
  }

  @Test
  public void negativeArrayIndex3() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true}]
          return $array[[-1*-1]]
        """;
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void spreadOperator() throws IOException {
    final String query = """
          [=(1 to 5)]
        """;
    final var result = query(query);
    assertEquals("[1,2,3,4,5]", result);
  }

  @Test
  public void derefExprWithNegativeArrayIndexExpr() throws IOException {
    final String query = """
          [true,false,"true",{"foo":["tada",{"baz":"yes"},{"baz":true}]}][].foo[[-1]].baz
        """;
    final var result = query(query);
    assertEquals("true", result);
  }

  @Test
  public void arrayUnboxing2() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[]
        """;
    final var result = query(query);
    assertEquals("{\"foo\":0} bar {\"baz\":true}", result);
  }

  @Test
  public void arrayUnboxing3() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[]]
        """;
    final var result = query(query);
    assertEquals("{\"foo\":0} bar {\"baz\":true}", result);
  }

  @Test
  public void arrayIndexSlice1() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:1]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void flattenAsCsv() throws IOException {
    final String query = """
          let $array := [{"foo":0,"ddd":"tztz"},{"bar":"hello","zzz":null},{"baz":true,"zzz":"yes"}]
          let $value := for $object in $array
                        return
                          let $fields := bit:fields($object)
                          let $len := bit:len($fields)
                          for $field at $pos in $fields
                          return if ($pos < $len) then (
                            $object.$field || ","
                          ) else (
                            $object.$field || "\n"
                          )
          return string-join($value,"")
        """;
    final var result = query(query);
    assertEquals("""
                     0,tztz
                     hello,null
                     true,yes
                     """.stripIndent(), result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:1:1]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement2() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:1:2]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement3() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:3:2]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement4() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:3:3]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement5() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:3:3]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement6() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[::1]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},\"bar\",{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement7() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[::2]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement8() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[::3]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement9() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[1::3]]
        """;
    final var result = query(query);
    assertEquals("[\"bar\"]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement10() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:1:3]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement11() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[::]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},\"bar\",{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement12() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[1::]]
        """;
    final var result = query(query);
    assertEquals("[\"bar\",{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement13() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[::-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},\"bar\",{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement14() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:-3:-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},\"bar\"]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement15() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:-4:-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},\"bar\",{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement16() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:-5:-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},\"bar\",{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement17() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:-5:-2]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement18() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-1:-5:-2]]
        """;
    final var result = query(query);
    assertEquals("[{\"baz\":true},{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement19() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-2:-5:-2]]
        """;
    final var result = query(query);
    assertEquals("[\"bar\"]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement20() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-2:0:-2]]
        """;
    final var result = query(query);
    assertEquals("[\"bar\"]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement21() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-3::-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement22() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-3:0:-1]]
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement23() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-3:-4:-1]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void arrayIndexSlice1WithIncrement24() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[-3:-1:-1]]
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void arrayIndexSlice() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[0:-2]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0}]", result);
  }

  @Test
  public void allItemsInArray() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[:]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},\"bar\",{\"baz\":true}]", result);
  }

  @Test
  public void emptyArrayIndexSlice1() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[2:2]]
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void emptyArrayIndexSlice2() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[3:2]]
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void emptyArrayIndexSlice3() throws IOException {
    final String query = """
          let $array := [{"foo": 0}, "bar", {"baz": true()}]
          return $array[[4:2]]
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void arrayIndexSlice2() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true()}]
          return $array[[1:]]
        """;
    final var result = query(query);
    assertEquals("[\"bar\",{\"baz\":true}]", result);
  }

  @Test
  public void arrayIndexSlice3() throws IOException {
    final String query = """
          let $array := [{"foo": 0},"bar",{"baz":true()}]
          return $array[[:2]]
        """;
    final var result = query(query);
    assertEquals("[{\"foo\":0},\"bar\"]", result);
  }

  @Test
  public void objectConstructorWithConcatExpr() throws IOException {
    final String query = """
          {|
            for $i in 1 to 3
            return { "foo" || $i : $i }
          |}
        """;
    final var result = query(query);
    assertEquals("{\"foo1\":1,\"foo2\":2,\"foo3\":3}", result);
  }

  @Test
  public void objectConstructorWithConcatExpr1() throws IOException {
    final String query = """
          {|
            for $i in 1 to 3
            return { "foo" || () || $i : $i }
          |}
        """;
    final var result = query(query);
    assertEquals("{\"foo1\":1,\"foo2\":2,\"foo3\":3}", result);
  }

  @Test
  public void functionOverloading() throws IOException {
    final String query = """
        declare function local:dummy($test) {
            $test
        };
              
        declare function local:dummy() {
            local:dummy("test")
        };
              
        local:dummy()
          """;
    final var result = query(query);
    assertEquals("test", result);
  }

  @Test
  public void remoteUrl() throws IOException {
    final String query = """
        let $logs := jn:doc('https://raw.githubusercontent.com/sirixdb/brackit/master/logs.json')[]
        let $total-count := count($logs)
                
        return for $log in $logs
            let $status := $log.status
            group by $status
            order by count($log) descending
            return {
                xs:string($status): {
                    "count": count($log),
                    "fraction": count($log) div $total-count[1]
                }
            }
        """.stripIndent();
    final var result = query(query);
    assertEquals(
        "{\"200\":{\"count\":409,\"fraction\":0.818}} {\"404\":{\"count\":56,\"fraction\":0.112}} {\"500\":{\"count\":35,\"fraction\":0.07}}",
        result);
  }

  @Test
  public void remoteUrlCollection() throws IOException {
    final String query = """
        let $logs := jn:collection('https://raw.githubusercontent.com/sirixdb/brackit/master/logs.json')[]
        let $total-count := count($logs)
                
        return for $log in $logs
            let $status := $log.status
            group by $status
            order by count($log) descending
            return {
                xs:string($status): {
                    "count": count($log),
                    "fraction": count($log) div $total-count[1]
                }
            }
        """.stripIndent();
    final var result = query(query);
    assertEquals(
        "{\"200\":{\"count\":409,\"fraction\":0.818}} {\"404\":{\"count\":56,\"fraction\":0.112}} {\"500\":{\"count\":35,\"fraction\":0.07}}",
        result);
  }

  @Test
  public void jsonParserEmptyArray() throws IOException {
    final String query = """
          jn:parse('[]')
        """;
    final var result = query(query);
    assertEquals("[]", result);
  }

  @Test
  public void jsonParserNegativeNumber() throws IOException {
    final String query = """
          jn:parse('[ 1, -1,    1.1,  -2.5]')
        """;
    final var result = query(query);
    assertEquals("[1,-1,1.1,-2.5]", result);
  }

  @Test
  public void concatExpr() throws IOException {
    final String query = """
         1 || 2 || 'foobar'
        """;
    final var result = query(query);
    assertEquals("12foobar", result);
  }

  @Test
  public void jsonParserEmptyRecord() throws IOException {
    final String query = """
          jn:parse('{}')
        """;
    final var result = query(query);
    assertEquals("{}", result);
  }

  @Test
  public void jsonParserNull() throws IOException {
    final String query = "jn:parse('null')";
    final var result = query(query);
    assertEquals("null", result);
  }

  @Test
  public void renameObjectField() throws IOException {
    final String query = """
          let $object := {"foo": 0}
          return rename json $object.foo as "bar"
        """;
    query(query);
  }

  @Test
  public void appendToArray() throws IOException {
    final String query = """
          append json (1, 2, 3) into ["foo", true(), false(), jn:null()]
        """;
    query(query);
  }

  @Test
  public void insertIntoArray() throws IOException {
    final String query = """
          insert json (1, 2, 3) into ["foo", true(), false(), jn:null()] at position 2
        """;
    query(query);
  }

  @Test
  public void insertIntoObject1() throws IOException {
    final String query = """
          insert json {"foo": not(true), "baz": null} into {"bar": false}
        """;
    query(query);
  }

  @Test
  public void insertIntoObject2() throws IOException {
    final String query = """
          let $object := {"bar": false()}
          return insert json {"foo": not(true()), "baz": jn:null()} into $object
        """;
    query(query);
  }

  @Test
  public void removeFromObject() throws IOException {
    final String query = """
          delete json {"foo": not(true()), "baz": jn:null()}.foo
        """;
    query(query);
  }

  @Test
  public void removeFromArray() throws IOException {
    final String query = """
          delete json ["foo", 0, 1][[1]]
        """;
    query(query);
  }

  @Test
  public void replaceObjectValue() throws IOException {
    final String query = """
          replace json value of {"foo": not(true()), "baz": jn:null()}.foo with 1
        """;
    query(query);
  }

  @Test
  public void replaceArrayValue() throws IOException {
    final String query = """
          replace json value of ["foo", 0, 1][[2]] with "bar"
        """;
    query(query);
  }

  @Test
  public void testNegativeArrayIndex() throws IOException {
    final var query = """
          let $array := [true,false,"true",{"foo":["tada",{"baz":"yes"},{"baz":true}]}]
          return $array[].foo[[-1]].baz
        """;
    final var result = query(query);
    assertEquals("true", result);
  }

  @Ignore
  @Test
  public void testDescendantDerefExpr() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + "=.revision.tada[..foo.baz = 'bar']";
    final var result = query(query);
    assertEquals("[{\"foo\":\"bar\"},{\"baz\":false},\"boo\",{},[{\"foo\":[true,{\"baz\":\"bar\"}]}]]", result);
  }

  @Test
  public void testDerefExpr() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix[].revision.tada[$$[[4]][].foo[[0]] = true()]";
    final var result = query(query);
    assertEquals("[{\"foo\":\"bar\"},{\"baz\":false},\"boo\",{},[{\"foo\":[true,{\"baz\":\"bar\"}]}]]", result);
  }

  @Test
  public void testDerefExpr1() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix[[2]].revision.tada[$$[][].foo[].baz eq 'bar']";
    final var result = query(query);
    assertEquals("[{\"foo\":\"bar\"},{\"baz\":false},\"boo\",{},[{\"foo\":[true,{\"baz\":\"bar\"}]}]]", result);
  }

  @Test
  public void testDerefExpr2() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix[].revision.tada[].foo";
    final var result = query(query);
    assertEquals("bar bar bar", result);
  }

  @Test
  public void testDerefExpr3() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix(2).revision.tada(4)(0).foo(1).baz";
    final var result = query(query);
    assertEquals("bar", result);
  }

  @Test
  public void testDerefExpr4() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix[[2]].revision.tada[[4]].foo[[1]].baz";
    final var result = query(query);
    assertEquals("", result);
  }

  @Test
  public void testDerefExpr5() throws IOException {
    final String json = Files.readString(JSON_RESOURCES.resolve("multiple-revisions.json"));
    final var query = json + ".sirix[[2]].revision.tada[[4]][[0]].foo[]";
    final var result = query(query);
    assertEquals("true {\"baz\":\"bar\"}", result);
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
        "[{\"key\":0},{\"value\":[{\"key\":{\"boolean\":true()}},{\"newkey\":\"yes\"}]},{\"key\":\"hey\",\"value\":false()}][].value[].key.boolean";
    final var result = query(query);
    assertEquals("true", result);
  }

  @Test
  public void testArray() throws IOException {
    final var query =
        "{\"foo\": [\"bar\", jn:null(), 2.33],\"bar\": {\"hello\": \"world\", \"helloo\": true()},\"baz\": \"hello\",\"tada\": [{\"foo\": \"bar\"}, {\"baz\": false()}, \"boo\", {}, []]}.foo";
    final var result = query(query);
    assertEquals("[\"bar\",null,2.33]", result);
  }

  @Test
  public void nestedExpressionsTest() throws IOException {
    final var json = Files.readString(JSON_RESOURCES.resolve("user_profiles.json"));
    final var query = json + ".websites[].description";
    final var result = query(query);
    assertEquals("work tutorials", result);
  }

  @Test
  public void nestedExpressionsWithPredicateTest() throws IOException {
    final var json = Files.readString(JSON_RESOURCES.resolve("user_profiles.json"));
    final var query = json + ".websites[[]][$$.description eq \"work\"]{description}";
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
    final var query = """
            let $values := [{"key": "hey"}, {"key": 0}]
            for $i in $values
            where $i.key instance of xs:integer and $i.key eq 0
            return $i
        """;
    final var result = query(query);
    assertEquals("{\"key\":0}", result);
  }

  @Test
  public void arrayForLoop2Test() throws IOException {
    final var query = """
            let $values := ["foo",0,true(),jn:null()]
            for $i in $values
            return $i
        """;
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
    assertEquals("[\"mercury\",\"venus\",\"earth\",\"mars\"] [\"monday\",\"tuesday\",\"wednesday\",\"thursday\"]",
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
    final var query = "let $json := {\"key\": 3, \"foo\": 0} for $key in bit:fields($json) where $json.$key eq 3\n"
        + "return { $key: $json.$key }";
    final var result = query(query);
    assertEquals("{\"key\":3}", result);
  }

  @Test
  public void forEachInArrayTest() throws IOException {
    final var query = "for $i in [{\"key\": 3}, {\"key\": 0}] where $i.key eq 0 return $i";
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
    ResultChecker.check(new ItemSequence(new ArrayObject(new QNm[] { new QNm("foo"), new QNm("bar") },
                                                         new Sequence[] { new Null(),
                                                             new DArray(List.of(new Int32(1), new Int32(2))) })),
                        resultSequence);
  }

  @Test
  public void testObjects() throws IOException {
    final var query = """
        let $object1 := { "Captain" : "Kirk" }
        let $object2 := { "First officer" : "Spock" }
        return ($object1, " ", $object2)""".indent(4);
    final var result = query(query);
    assertEquals("{\"Captain\":\"Kirk\"}   {\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects1() throws IOException {
    final var query = """
            let $object1 := { "Captain" : "Kirk" }
            let $object2 := { "First officer" : "Spock" }
            return {| { "foobar": $object1 }, $object2 |}
        """;
    final var result = query(query);
    assertEquals("{\"foobar\":{\"Captain\":\"Kirk\"},\"First officer\":\"Spock\"}", result);
  }

  @Test
  public void testComposeObjects2() throws IOException {
    final var query = """
            let $object1 := { "Captain" : "Kirk" }
            let $object2 := { "First officer" : "Spock" }
            return {{ "foobar": $object1 }, $object2 }
        """;
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
    final var query = """
            let $object1 := { "Captain" : "Kirk" }
            let $object2 := { "First officer" : "Spock" }
            return {| $object1, $object2 |}
        """;
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
    final var query = """
            let $x := { "eyes": "blue", "hair": "fuchsia" }
            let $y := { "eyes": "brown", "hair": "brown" }
            return { "eyes": $x.eyes, "hair": $y.hair }
        """;
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
    final var query = "{\"key\": 3, \"foo\": 0}[$$.key eq 3]";
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
      return out.toString(StandardCharsets.UTF_8);
    }
  }
}
