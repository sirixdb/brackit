package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Int64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * fn:sum and fn:avg must reject a non-numeric operand among numeric operands with err:FORG0006,
 * whatever its position in the sequence and whichever numeric type precedes it (issue #131: a
 * boolean between two numbers escaped as a {@link ClassCastException}).
 */
public class SumAvgOperandTypeTest extends XQueryBaseTest {

  private static final String[] FUNCTIONS = { "sum", "avg" };

  /** Non-numeric operands that are not untypedAtomic (which is cast to xs:double instead). */
  private static final String[] NON_NUMERIC = { "true", "false", "\"a\"", "xs:date(\"2020-01-01\")",
      "xs:dayTimeDuration(\"PT1H\")", "xs:anyURI(\"http://x\")" };

  /**
   * Numeric operand lists, one per accumulation path of the aggregator: xs:int literals (generic
   * path), xs:long (long batch path), xs:double (double batch path), xs:decimal (generic path).
   */
  private static final String[][] NUMBERS = { { "-1", "-2" }, { "xs:long(-1)", "xs:long(-2)" }, { "-1e0", "-2e0" }, {
      "-1.5", "-2.5" } };

  static Stream<Arguments> invalidOperands() {
    final List<Arguments> cases = new ArrayList<>();
    for (final String fn : FUNCTIONS) {
      for (final String bad : NON_NUMERIC) {
        for (final String[] nums : NUMBERS) {
          cases.add(Arguments.of(fn, "first", "(" + bad + ", " + nums[0] + ", " + nums[1] + ")"));
          cases.add(Arguments.of(fn, "between", "(" + nums[0] + ", " + bad + ", " + nums[1] + ")"));
          cases.add(Arguments.of(fn, "second", "(" + nums[0] + ", " + bad + ")"));
          cases.add(Arguments.of(fn, "last", "(" + nums[0] + ", " + nums[1] + ", " + bad + ")"));
        }
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0}({2}) [{1}]")
  @MethodSource("invalidOperands")
  public void nonNumericOperandRaisesForg0006(final String fn, final String position, final String seq) {
    final QueryException e = assertThrows(QueryException.class, () -> evaluateFully(fn + "(" + seq + ")"));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e.getCode());
  }

  @ParameterizedTest(name = "{0} in group by")
  @MethodSource("functions")
  public void nonNumericOperandInGroupByRaisesForg0006(final String fn) {
    final String query = "for $x in (-1, true, -2) let $k := 1 group by $k return " + fn + "($x)";
    final QueryException e = assertThrows(QueryException.class, () -> evaluateFully(query));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e.getCode());
  }

  /** Serializes the whole result: query evaluation is lazy, so errors surface only on consumption. */
  private void evaluateFully(final String query) {
    new Query(query).serialize(ctx, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
  }

  static Stream<String> functions() {
    return Stream.of(FUNCTIONS);
  }

  // --- valid aggregates are unchanged ---

  @Test
  public void sumOfIntsUnchanged() {
    ResultChecker.dCheck(new Int32(-3), new Query("sum((-1, -2))").execute(ctx));
  }

  @Test
  public void sumOfLongsUnchanged() {
    ResultChecker.dCheck(new Int64(-3), new Query("sum((xs:long(-1), xs:long(-2)))").execute(ctx));
  }

  @Test
  public void sumOfDoublesUnchanged() {
    ResultChecker.dCheck(new Dbl(-3.0), new Query("sum((-1e0, -2e0))").execute(ctx));
  }

  @Test
  public void sumOfIntThenDoubleUnchanged() {
    ResultChecker.dCheck(new Dbl(1.5), new Query("sum((1, 0.5e0))").execute(ctx));
  }

  @Test
  public void sumWithUntypedAtomicSecondUnchanged() {
    ResultChecker.dCheck(new Dbl(3.0), new Query("sum((1, xs:untypedAtomic(\"2\")))").execute(ctx));
  }

  @Test
  public void avgOfIntsUnchanged() {
    ResultChecker.dCheck(new Dbl(1.5), new Query("avg((1e0, 2e0))").execute(ctx));
  }

  @Test
  public void emptySumAndAvgUnchanged() {
    ResultChecker.dCheck(new Int32(0), new Query("sum(())").execute(ctx));
    assertEquals(null, new Query("avg(())").execute(ctx));
  }
}
