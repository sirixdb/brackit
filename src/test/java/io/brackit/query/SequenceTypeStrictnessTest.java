package io.brackit.query;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
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
      "count(() treat as item())" })
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
      "declare function local:f($x as empty-sequence()) { $x }; local:f(1)" })
  void declarationsEnforceCardinality(String query) {
    QueryException error = assertThrows(QueryException.class, () -> consume(query));
    assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, error.getCode(), error.getMessage());
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
