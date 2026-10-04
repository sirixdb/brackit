package io.brackit.query.expr;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.type.AnyItemType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.jsonitem.array.DArray;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreatValueRepresentationTest extends XQueryBaseTest {
  @ParameterizedTest
  @EnumSource(value = Cardinality.class, names = { "One", "ZeroOrOne", "ZeroOrMany", "OneOrMany" })
  void validatedItemsKeepTheirIdentity(Cardinality cardinality) {
    SequenceType type = new SequenceType(AnyItemType.ANY, cardinality);
    for (Sequence item : List.of(new DArray(List.of()),
                                 new DArray(List.of(new Int32(10))),
                                 new DArray(List.of(new Int32(10), new Int32(20))),
                                 Int32.ONE,
                                 new Str("value"))) {
      Treat treat = new Treat(item, type);
      assertSame(item, treat.evaluate(ctx, null));
      assertSame(item, treat.evaluateToItem(ctx, null));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "{}.missing treat as item()?", "{}.missing treat as item()*",
      "{}.missing treat as empty-sequence()" })
  void validatedMissingValuesRemainNull(String query) {
    assertNull(new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "[]", "[10]", "[10,20]" })
  void arraysRemainOneItemForFunctionAndNestedTreatConsumers(String array) {
    String value = array + " treat as item()";
    for (String query : List.of("declare function local:f($x as item()) { $x instance of array() }; local:f(" + value
        + ")",
                                "let $f := function($x as item()) { $x instance of array() } return $f(" + value + ")",
                                "declare function local:f($x as item(), $y as item()) { $x instance of array() }; let $g := local:f("
                                    + value + ",?) return $g(0)",
                                "let $f := function($x as item(), $y as item()) { $x instance of array() } let $g := $f("
                                    + value + ",?) return $g(0)",
                                "((" + value + ") treat as array()) instance of array()")) {
      ResultChecker.dCheck(Bool.TRUE, new Query(query).execute(ctx));
    }
  }

  @ParameterizedTest
  @CsvSource({ "'[]',0", "'[10]',1", "'[10,20]',2" })
  void navigationUsesTheValidatedArrayItem(String array, int members) {
    ResultChecker.dCheck(new Int32(members), new Query("count((" + array + " treat as item())[])").execute(ctx));
    ResultChecker.dCheck(members == 0 ? null : new Int32(10),
                         new Query("(" + array + " treat as item())[0]").execute(ctx));
    ResultChecker.dCheck(new Int32(members),
                         new Query("count(((" + array + " treat as item()) treat as array())[])").execute(ctx));
    ResultChecker.dCheck(new Int32(10), new Query("({\"value\":10} treat as item()).value").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "string({}.missing treat as item()?) eq ''",
      "string(({}.missing treat as item()?) treat as item()?) eq ''",
      "declare function local:f($x as item()?) { empty($x) }; local:f({}.missing treat as item()?)",
      "let $f := function($x as item()?) { empty($x) } return $f({}.missing treat as item()?)",
      "declare function local:f($x as item()?, $y as item()) { empty($x) }; let $g := local:f({}.missing treat as item()?,?) return $g(0)",
      "let $f := function($x as item()?, $y as item()) { empty($x) } let $g := $f({}.missing treat as item()?,?) return $g(0)",
      "empty(({}.missing treat as item()?)[0])", "empty(({}.missing treat as item()?)[])" })
  void optionalEmptyTreatRemainsEmptyForEveryConsumer(String query) {
    ResultChecker.dCheck(Bool.TRUE, new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "declare function local:f($x as item()) { 0 }; local:f((1,2) treat as item())",
      "let $f := function($x as item()) { 0 } return $f((1,2) treat as item())",
      "declare function local:f($x as item(), $y as item()) { 0 }; let $g := local:f((1,2) treat as item(),?) return $g(0)",
      "let $f := function($x as item(), $y as item()) { 0 } let $g := $f((1,2) treat as item(),?) return $g(0)",
      "((1,2) treat as item()?) treat as item()?", "{}.missing treat as item()", "[10] treat as xs:integer" })
  void invalidTreatStillTranslatesImmediateAndDeferredErrors(String query) {
    QueryException error = assertThrows(QueryException.class, () -> {
      Sequence result = new Query(query).execute(ctx);
      if (result != null) {
        try (Iter iter = result.iterate()) {
          while (iter.next() != null) {
          }
        }
      }
    });
    assertEquals(ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, error.getCode());
  }
}
