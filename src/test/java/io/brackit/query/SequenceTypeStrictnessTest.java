package io.brackit.query;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SequenceTypeStrictnessTest extends XQueryBaseTest {
  @ParameterizedTest
  @ValueSource(strings = { "() instance of xs:integer", "(1,2) instance of xs:integer", "() instance of item()",
      "(1,2) instance of item()" })
  void omittedOccurrenceMeansOne(String query) {
    ResultChecker.dCheck(Bool.FALSE, new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1 instance of xs:integer", "() instance of xs:integer?", "(1,2) instance of xs:integer*",
      "(1,2) instance of xs:integer+", "(1,2) instance of item()*" })
  void explicitOccurrenceAndSingletonRemainValid(String query) {
    ResultChecker.dCheck(Bool.TRUE, new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "typeswitch((1,2)) case xs:integer return 1 default return 2",
      "typeswitch(()) case xs:integer return 1 default return 2" })
  void typeswitchUsesSingletonCases(String query) {
    ResultChecker.dCheck(new Int32(2), new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "exists((1,2) treat as item())", "exists((1,2) treat as item()?)",
      "count(() treat as item())", "exists(((1,2) treat as item())())",
      "exists(((1,2) treat as item()?)())", "{\"value\": ((1,2) treat as item())}",
      "[10][((0,1) treat as item())]", "([10])[((0,1) treat as item()?)]" })
  void treatRejectsWrongCardinalityBeforeFirstResult(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, error.getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare variable $x as xs:integer := (1,2); $x",
      "declare variable $x as xs:integer := (); $x", "let $x as item() := (1,2) return $x",
      "let $x as item() := () return $x", "let $x as item() := (1,2) return exists($x)",
      "declare function local:f($x as xs:integer) { $x }; local:f(1 to 2)",
      "declare function local:f($x as xs:integer) { $x }; local:f((1,2))",
      "declare function local:f($x as item()) { $x }; local:f((1,2))",
      "let $f := function($x as item()) { $x } return $f((1,2))",
      "let $c := 10 let $f := function($x as item()) { ($c,$x) } return $f((1,2))",
      "let $f := function($x as xs:integer) { $x } return $f((1,2))",
      "declare function local:f($x as xs:integer) { $x }; local:f(())",
      "declare function local:f() as xs:integer { (1,2) }; local:f()",
      "declare function local:f() as xs:integer { () }; local:f()",
      "declare function local:f($x as empty-sequence()) { 3 }; local:f((1))",
      "let $f := function($x as empty-sequence()) { 3 } return $f((1))",
      "declare function local:f($x as empty-sequence()) { $x }; local:f(1)" })
  void declarationsEnforceCardinality(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode(), error.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "declare function local:f($x as item(), $y as item()) { $y }; let $g := local:f((1,2),?) return $g(3)",
      "declare function local:f($x as item(), $y as item()) { $y }; let $g := local:f((),?) return $g(3)",
      "declare function local:f($x as xs:integer, $y as item()) { $y }; let $g := local:f((1,2),?) return $g(3)",
      "declare function local:f($x as item()?, $y as item()) { $y }; let $g := local:f((1,2),?) return $g(3)",
      "declare function local:f($x as empty-sequence(), $y as item()) { $y }; let $g := local:f(1,?) return $g(3)",
      "declare function local:f($x as empty-sequence(), $y as item()) { $y }; let $g := local:f((1),?) return $g(3)",
      "let $f := function($x as empty-sequence(), $y as item()) { $y } let $g := $f((1),?) return $g(3)",
      "declare function local:f($x as empty-sequence(), $y as item()) { $y }; exists(local:f((1),?))",
      "declare function local:f($x as xs:integer, $y as item()) { $y }; let $g := local:f('bad',?) return $g(3)",
      "declare function local:f($x as item(), $y as item(), $z as item()) { $x }; let $g := local:f(?, (1,2),?) return $g(3,4)",
      "declare function local:f($x as item(), $y as item()) { $x }; let $g := local:f(?, (1,2)) return $g(3)",
      "let $f := function($x as item(), $y as item()) { $y } let $g := $f((1,2),?) return $g(3)",
      "let $f := function($x as xs:integer, $y as item()) { $y } let $g := $f((),?) return $g(3)",
      "let $g := concat('a', 'b', (1,2),?) return $g('c')",
      "let $g := concat('a', 'b',?) return $g((1,2))",
      "declare function local:f($x as item(), $y as item()) { $y }; exists(local:f((1,2),?))" })
  void partialApplicationsValidateBoundArguments(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode(), error.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "let $c := 10 let $f := function($x as xs:integer*) { ($c,$x) } return $f((1,2))",
      "let $c := 10 let $f := function($x as xs:integer+) { ($c,$x) } return $f((1,2))",
      "let $f := function($x as xs:integer*) { (10,$x) } return $f((1,2))",
      "let $f := function($x as xs:integer+) { (10,$x) } return $f((1,2))" })
  void manyValuedArgumentsUseTheirOwnClosureSlot(String query) {
    ResultChecker.dCheck(new ItemSequence(new Int32(10), Int32.ONE, new Int32(2)), new Query(query).execute(ctx));
  }

  @Test
  void closureConversionSupportsMultipleCapturesAndArguments() {
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20), Int32.ONE, new Int32(2), new Int32(3)),
                         new Query("let $a := 10 let $b := 20 let $f := function($x as xs:integer*, $y as xs:integer+) { ($a,$b,$x,$y) } return $f((1,2),3)").execute(ctx));
    ResultChecker.dCheck(new Int32(10),
                         new Query("let $c := 10 let $f := function($x as xs:integer*) { ($c,$x) } return $f(())").execute(ctx));
  }

  @Test
  void validScalarTreatConsumersKeepTheirResults() {
    ResultChecker.dCheck(Int32.ONE, new Query("{\"value\": (1 treat as xs:integer)}.value").execute(ctx));
    ResultChecker.dCheck(Int32.ZERO,
                         new Query("count({\"value\": (() treat as item()?)}.value)").execute(ctx));
    ResultChecker.dCheck(new Int32(10), new Query("[10][(0 treat as xs:integer)]").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("exists(([1] treat as item())())").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "{\"value\": ((1,2) treat as item()*)}.value[]",
      "{\"value\": ((1,2) treat as item()+)}.value[]" })
  void manyValuedTreatRetainsScalarArrayPacking(String query) {
    ResultChecker.dCheck(new ItemSequence(Int32.ONE, new Int32(2)), new Query(query).execute(ctx));
  }

  @Test
  void validEmptySequenceArgumentsRemainEmptyInEveryCallPath() {
    ResultChecker.dCheck(new Int32(3),
                         new Query("declare function local:f($x as empty-sequence()) { 3 }; local:f(())").execute(ctx));
    ResultChecker.dCheck(new Int32(3),
                         new Query("let $f := function($x as empty-sequence()) { 3 } return $f(())").execute(ctx));
    ResultChecker.dCheck(new Int32(3),
                         new Query("let $f := function($x as empty-sequence(), $y as item()) { $y } let $g := $f((),?) return $g(3)").execute(ctx));
  }

  @Test
  void numericBuiltinsConvertUntypedArgumentsToTheirDeclaredTypes() {
    ResultChecker.dCheck(new Int32(2), new Query("remove((1,2), (<n>1</n>))").execute(ctx));
    ResultChecker.dCheck(new Int32(2), new Query("remove((1,2), <n>1</n>)").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(Int32.ONE, new Int32(3), new Int32(2)),
                         new Query("insert-before((1,2), (<n>2</n>), 3)").execute(ctx));
    ResultChecker.dCheck(new Int32(2), new Query("subsequence((1,2,3), (<n>2</n>), (<n>1</n>))").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("abs((<n>-1</n>)) instance of xs:double").execute(ctx));
  }

  @Test
  void validPartialApplicationsPreserveEmptyValuesConversionAndPlaceholders() {
    ResultChecker.dCheck(new Int32(3),
                         new Query("declare function local:f($x as item()?, $y as item()) { $y }; let $g := local:f((),?) return $g(3)").execute(ctx));
    ResultChecker.dCheck(new Int32(3),
                         new Query("declare function local:f($x as empty-sequence(), $y as item()) { $y }; let $g := local:f((),?) return $g(3)").execute(ctx));
    ResultChecker.dCheck(new Int32(6),
                         new Query("declare function local:f($x as xs:integer, $y as xs:integer, $z as xs:integer) { $x+$y+$z }; let $g := local:f(<n>1</n>,?,?) let $h := $g(2,?) return $h(3)").execute(ctx));
    ResultChecker.dCheck(new Int32(6),
                         new Query("let $f := function($x as xs:integer, $y as xs:integer, $z as xs:integer) { $x+$y+$z } let $g := $f(?, <n>2</n>,?) return $g(1,3)").execute(ctx));
    ResultChecker.dCheck(new Int32(2), new Query("let $g := remove((1,2),?) return $g(<n>1</n>)").execute(ctx));
    ResultChecker.dCheck(new Int32(2), new Query("let $g := remove(?, (<n>1</n>)) return $g((1,2))").execute(ctx));
    ResultChecker.dCheck(new Str("abcd"), new Query("let $g := concat('a', 'b',?,?) return $g('c','d')").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(Int32.ONE, new Int32(2), new Int32(3), Int32.ONE, new Int32(2), new Int32(4)),
                         new Query("declare function local:f($x as xs:integer*, $y as xs:integer) { ($x,$y) }; let $g := local:f((<n>1</n>,<n>2</n>),?) return ($g(3),$g(4))").execute(ctx));
  }

  private void consume(String query) {
    Sequence result = new Query(query).execute(ctx);
    if (result != null) {
      try (Iter it = result.iterate()) {
        while (it.next() != null) {
        }
      }
    }
  }
}
