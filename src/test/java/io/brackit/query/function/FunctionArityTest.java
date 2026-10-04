package io.brackit.query.function;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Function;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.module.Functions;
import io.brackit.query.module.Namespaces;
import io.brackit.query.operator.TupleImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FunctionArityTest extends XQueryBaseTest {
  private Function builtin(String name, int arity) {
    return new Functions().resolve(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, name), arity);
  }

  private void consume(String query) {
    Sequence result = new Query(query).execute(ctx);
    if (result != null) {
      try (Iter iter = result.iterate()) {
        while (iter.next() != null) {
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "let $f := function() { 1 } return $f(?)", "let $f := function() { 1 } return $f(1,?)",
      "let $f := function($x) { $x } return $f(1,?)", "let $f := function($x,$y) { $x } return $f(?)",
      "let $f := function($x,$y) { $x } return $f(1,2,?)", "let $f := function() { 1 } return $f(1)",
      "let $f := function($x) { 1 } return $f()", "let $f := function($x) { 1 } return $f(1,2)",
      "let $f := function($x,$y) { 1 } return $f()", "let $f := function($x,$y) { 1 } return $f(1)",
      "let $f := function($x,$y) { 1 } return $f(1,2,3)", "let $c := 10 let $f := function() { $c } return $f(?)",
      "let $c := 10 let $f := function($x,$y) { $c } return $f(1)",
      "let $f := function($x,$y) { $x } let $g := $f(1,?) return $g()",
      "let $f := function($x,$y) { $x } let $g := $f(1,?) return $g(2,3)",
      "let $f := function($x,$y,$z) { $x } let $g := $f(1,?,?) return $g(?)",
      "let $g := concat('a',?) return $g('b','c')" })
  void dynamicAndPartialCallsRejectWrongArity(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare function local:f() { 1 }; local:f(?)",
      "declare function local:f($x,$y) { 1 }; local:f(?)", "declare function local:f($x) { 1 }; local:f(1,?)",
      "declare function local:f($x) { 1 }; local:f()", "concat(?)", "concat()" })
  void staticResolutionRetainsItsArityErrors(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_UNDEFINED_FUNCTION, error.getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare function local:f() { 10 }; local:f()", "let $f := function() { 10 } return $f()",
      "let $c := 10 let $f := function() { $c } return $f()",
      "let $f := function($x as xs:integer) { $x } return $f(10)",
      "let $c := 3 let $f := function($x as xs:integer,$y as xs:integer) { $c+$x+$y } return $f(2,5)",
      "declare function local:f($x as xs:integer,$y as xs:integer) { $x+$y }; let $g := local:f(3,?) return $g(7)",
      "let $f := function($x as xs:integer,$y as xs:integer) { $x+$y } let $g := $f(3,?) return $g(7)",
      "declare function local:f($x as xs:integer,$y as xs:integer,$z as xs:integer) { $x+$y+$z }; let $g := local:f(3,?,?) let $h := $g(2,?) return $h(5)" })
  void validFixedCapturedAndPartialCallsKeepTheirResults(String query) {
    ResultChecker.dCheck(new Int32(10), new Query(query).execute(ctx));
  }

  @Test
  void directConstructionChecksBoundAndPlaceholderCountsBeforeIndexing() {
    Function zero = builtin("true", 0);
    QueryException placeholder = assertThrows(QueryException.class,
                                              () -> new PartiallyAppliedFunction(zero,
                                                                                 new Sequence[] { null },
                                                                                 new int[] { 0 }));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, placeholder.getCode());
    QueryException bound = assertThrows(QueryException.class,
                                        () -> new PartiallyAppliedFunction(zero,
                                                                           new Sequence[] { Int32.ONE },
                                                                           new int[0]));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, bound.getCode());
    QueryException ordinary = assertThrows(QueryException.class, () -> new FunctionExpr(null, zero, Int32.ONE));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, ordinary.getCode());
    ResultChecker.dCheck(Bool.TRUE, new FunctionExpr(null, zero).evaluate(ctx, new TupleImpl()));
  }

  @ParameterizedTest
  @ValueSource(strings = { "$f()", "$f('a')", "$f(?)", "$f('a',?)('b','c')" })
  void variadicCallsRequireTheirFixedArguments(String call) {
    ctx.bind(new QNm("f"), builtin("concat", 2));
    QueryException error = assertThrows(QueryException.class, () -> consume("declare variable $f external; " + call));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
  }

  @Test
  void variadicCallsKeepMinimumRepeatedAndPartialArguments() {
    ctx.bind(new QNm("f"), builtin("concat", 2));
    String declaration = "declare variable $f external; ";
    ResultChecker.dCheck(new Str("ab"), new Query(declaration + "$f('a','b')").execute(ctx));
    ResultChecker.dCheck(new Str("abc"), new Query(declaration + "$f('a','b','c')").execute(ctx));
    ResultChecker.dCheck(new Str("abcd"), new Query(declaration + "$f('a','b','c','d')").execute(ctx));
    ResultChecker.dCheck(new Str("ab"), new Query(declaration + "let $g := $f('a',?) return $g('b')").execute(ctx));
    ResultChecker.dCheck(new Str("abcd"),
                         new Query(declaration + "let $g := $f('a','b',?,?) return $g('c','d')").execute(ctx));
    ResultChecker.dCheck(new Str("abc"),
                         new Query(declaration + "let $g := $f('a',?,?) let $h := $g('b',?) return $h('c')").execute(
                                                                                                                     ctx));
    ResultChecker.dCheck(new Str("abcd"), new Query("let $g := concat('a','b',?,?) return $g('c','d')").execute(ctx));
  }

  @Test
  void implicitContextCallsHaveZeroExplicitArguments() {
    ctx.bind(new QNm("f"), builtin("string-length", 0));
    ctx.setContextItem(new Str("abc"));
    ResultChecker.dCheck(Bool.TRUE, new Query("declare variable $f external; $f() eq 3").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("string-length() eq 3").execute(ctx));
    QueryException error = assertThrows(QueryException.class, () -> consume("declare variable $f external; $f('abc')"));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode());
    ctx.setContextItem(null);
    QueryException missing = assertThrows(QueryException.class, () -> consume("declare variable $f external; $f()"));
    assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, missing.getCode());
  }
}
