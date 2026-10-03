package io.brackit.query;

import io.brackit.query.atomic.Int32;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonNavigationTypeTest extends XQueryBaseTest {
  @ParameterizedTest
  @ValueSource(strings = { "1()", "1(0)", "(<n/>)()", "(<n/>)(0)", "1('field')", "'text'('field')",
      "()()", "()(0)", "1(error())", "{}.missing()" })
  void dynamicNavigationSkipsEmptyAtomicAndXmlOperands(String query) {
    ResultChecker.dCheck(null, new Query(query).execute(ctx));
  }

  @Test
  void dynamicNavigationRetainsScalarPackingAndMatchingOperands() {
    ResultChecker.dCheck(new ItemSequence(Int32.ONE, new Int32(2)), new Query("(1,2)()").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)), new Query("[10,20]()").execute(ctx));
    ResultChecker.dCheck(new Int32(10), new Query("[10](0)").execute(ctx));
    ResultChecker.dCheck(new Int32(10), new Query("{\"field\":10}('field')").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1[0]", "1[]", "'text'[0]", "'text'[]", "<n/>[0]", "<n/>[]", "{}[0]", "{}[]", "(1)[0]",
      "('text')[0]", "(<n/>)[0]", "({})[0]", "(1)[]", "('text')[]", "(<n/>)[]", "({})[]", "(1, 'text', <n/>, {})[0]",
      "(1, 'text', <n/>, {})[]", "(1).a", "('text').a", "(<n/>).a", "([1]).a", "(1, 'text', <n/>, [1]).a" })
  void navigationSkipsNonMatchingItems(String query) {
    ResultChecker.dCheck(Int32.ZERO, new Query("count(" + query + ")").execute(ctx));
  }

  @Test
  void mixedSequencesPreserveMatchingArrayAndObjectResults() {
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, [10], {}, [20], 'text')[0]").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, [10], {}, [20], 'text')[]").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, {\"a\":10}, [], {\"a\":20}, 'text').a").execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "[10][1]", "([10])[1]", "[10][4294967296]", "([10])[4294967296]",
      "([10])[3000000000]", "(1, [10], [20])[4294967296]", "[10][18446744073709551616]",
      "([10])[18446744073709551616]", "(1, [10], [20])[18446744073709551616]",
      "[10][9223372036854775808]", "([10])[9223372036854775808]" })
  void positiveOutOfBoundsIndicesAreEmptyInEveryRepresentation(String query) {
    ResultChecker.dCheck(null, new Query(query).execute(ctx));
  }

  @ParameterizedTest
  @ValueSource(strings = { "[10][-2]", "([10])[-2]", "[10][-4294967296]", "([10])[-4294967296]",
      "([10])[-3000000000]", "(1, [10], [20])[-4294967296]", "[10][-18446744073709551616]",
      "([10])[-18446744073709551616]", "(1, [10], [20])[-18446744073709551616]",
      "[10][-9223372036854775809]", "([10])[-9223372036854775809]" })
  void negativeOvershootHasTheSameErrorInEveryRepresentation(String query) {
    QueryException error = assertThrows(QueryException.class,
                                       () -> ResultChecker.dCheck(null, new Query(query).execute(ctx)));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, error.getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "([10])[0]", "([10])[-1]", "(1, [], [10])[0]", "([10], [], 1)[0]",
      "(1, [], [10])[]", "([], [], [10], [])[]", "([10], [], [], 1)[]" })
  void emptyMappedResultsDoNotHideLaterValues(String query) {
    ResultChecker.dCheck(new Int32(10), new Query(query).execute(ctx));
  }

  @Test
  void unboxingResumesAfterNonemptyAndEmptyArrays() {
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20), new Int32(30)),
                         new Query("([], [10,20], [], 1, [], [30], [])[]").execute(ctx));
    ResultChecker.dCheck(Int32.ONE, new Query("count((1, [], [2])[])").execute(ctx));
    ResultChecker.dCheck(new Int32(30), new Query("([10], [20,30])[1]").execute(ctx));
  }
}
