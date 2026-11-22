package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.util.ExprUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Property tests for fn:sum and fn:avg over randomly generated sequences of mixed numeric types.
 *
 * <p>The aggregator accumulates xs:long and xs:double runs in SIMD batches of 1024 values and
 * falls back to a generic per-item loop whenever the item type changes. The sequence lengths below
 * straddle that batch boundary on purpose, so a type change is exercised both inside a partially
 * filled buffer and across a flushed one - routes that fixed two- or three-item examples never
 * reach.
 *
 * <p>Oracles are independent of the aggregator: expected values come from exact {@link BigDecimal}
 * arithmetic over the generated operands, and the expected error comes from the W3C XQuery and
 * XPath Functions and Operators specification, which requires fn:sum and fn:avg to raise
 * err:FORG0006 when an operand is not of a type the sum can be applied to. Every generated operand
 * is an integral value of small magnitude, hence exactly representable as xs:double, so the
 * expected sum is independent of the summation order the batching happens to choose.
 */
public class SumAvgMixedSequencePropertyTest extends XQueryBaseTest {

  /** Fixed so a failure is reproducible; the seed and the operands are reported on failure. */
  private static final long SEED = 20_260_131L;

  /** Matches the aggregator's SIMD batch size, the structural edge these properties probe. */
  private static final int BATCH_SIZE = 1024;

  private static final String[] FUNCTIONS = { "sum", "avg" };

  /** Non-numeric operands; xs:untypedAtomic is excluded because the spec casts it to xs:double. */
  private static final String[] NON_NUMERIC = { "true", "false", "\"a\"", "xs:date(\"2020-01-01\")",
      "xs:dayTimeDuration(\"PT1H\")", "xs:anyURI(\"http://x\")" };

  /** avg divides, so an exact decimal expectation needs a tolerance; sum is compared exactly. */
  private static final double AVG_TOLERANCE = 1e-9;

  @ParameterizedTest(name = "sum/avg of {0} mixed numeric operands matches an exact oracle")
  @ValueSource(ints = { 1, 2, 3, 17, BATCH_SIZE - 1, BATCH_SIZE, BATCH_SIZE + 1, 2 * BATCH_SIZE, 2 * BATCH_SIZE + 1, 3
      * BATCH_SIZE + 7 })
  public void mixedNumericSequenceSumsToExactOracle(final int length) {
    final Random random = new Random(SEED + length);
    final List<Long> values = new ArrayList<>(length);
    final StringJoiner operands = new StringJoiner(", ", "(", ")");
    for (int i = 0; i < length; i++) {
      final long value = random.nextInt(-1_000_000, 1_000_001);
      values.add(value);
      operands.add(numericLiteral(value, random.nextInt(4)));
    }
    final String sequence = operands.toString();

    BigDecimal expectedSum = BigDecimal.ZERO;
    for (final long value : values) {
      expectedSum = expectedSum.add(BigDecimal.valueOf(value));
    }

    final String context = "seed=" + (SEED + length) + " length=" + length;
    assertEquals(expectedSum.doubleValue(),
                 numericResult("sum(" + sequence + ")").doubleValue(),
                 "sum must equal the exact oracle, " + context);

    final double expectedAvg = expectedSum.divide(BigDecimal.valueOf(length), MathContext.DECIMAL64).doubleValue();
    final double actualAvg = numericResult("avg(" + sequence + ")").doubleValue();
    assertTrue(Math.abs(actualAvg - expectedAvg) <= AVG_TOLERANCE * Math.max(1.0, Math.abs(expectedAvg)),
               "avg must equal the exact oracle within " + AVG_TOLERANCE + ", expected " + expectedAvg + " but was "
                   + actualAvg + ", " + context);
  }

  @ParameterizedTest(name = "a non-numeric operand among {0} numeric ones raises err:FORG0006")
  @ValueSource(ints = { 1, 2, 3, 17, BATCH_SIZE - 1, BATCH_SIZE, BATCH_SIZE + 1, 2 * BATCH_SIZE, 2 * BATCH_SIZE + 1, 3
      * BATCH_SIZE + 7 })
  public void nonNumericOperandAnywhereRaisesForg0006(final int length) {
    final Random random = new Random(SEED - length);
    for (final String bad : NON_NUMERIC) {
      final List<String> operands = new ArrayList<>(length + 1);
      for (int i = 0; i < length; i++) {
        operands.add(numericLiteral(random.nextInt(-1_000_000, 1_000_001), random.nextInt(4)));
      }
      final int position = random.nextInt(operands.size() + 1);
      operands.add(position, bad);
      final String sequence = "(" + String.join(", ", operands) + ")";

      for (final String function : FUNCTIONS) {
        final String context = "seed=" + (SEED - length) + " length=" + length + " operand=" + bad + " position="
            + position;
        final QueryException e = assertThrows(QueryException.class,
                                              () -> evaluateFully(function + "(" + sequence + ")"),
                                              context);
        assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e.getCode(), context);
      }
    }
  }

  /**
   * A sequence of a single numeric type stays inside one accumulation path for its whole length,
   * so a run longer than two batches drives the SIMD buffer through repeated fills, flushes and a
   * final partial flush, and {@code avg} additionally pins the item count that path maintains.
   * The mixed-type property above never builds such a run, because it switches type per item.
   */
  @ParameterizedTest(name = "sum/avg of a single-type run of {0} operands matches an exact oracle")
  @ValueSource(ints = { 0, 1, 2, 3 })
  public void singleTypeRunAcrossBatchBoundarySumsToExactOracle(final int kind) {
    final int length = 2 * BATCH_SIZE + 37;
    final Random random = new Random(SEED + 7L * kind);
    final StringJoiner operands = new StringJoiner(", ", "(", ")");
    BigDecimal expectedSum = BigDecimal.ZERO;
    for (int i = 0; i < length; i++) {
      final long value = random.nextInt(-1_000_000, 1_000_001);
      expectedSum = expectedSum.add(BigDecimal.valueOf(value));
      operands.add(numericLiteral(value, kind));
    }
    final String sequence = operands.toString();

    final String context = "kind=" + kind + " length=" + length;
    assertEquals(expectedSum.doubleValue(),
                 numericResult("sum(" + sequence + ")").doubleValue(),
                 "sum must equal the exact oracle, " + context);

    final double expectedAvg = expectedSum.divide(BigDecimal.valueOf(length), MathContext.DECIMAL64).doubleValue();
    final double actualAvg = numericResult("avg(" + sequence + ")").doubleValue();
    assertTrue(Math.abs(actualAvg - expectedAvg) <= AVG_TOLERANCE * Math.max(1.0, Math.abs(expectedAvg)),
               "avg must equal the exact oracle within " + AVG_TOLERANCE + ", expected " + expectedAvg + " but was "
                   + actualAvg + ", " + context);
  }

  /**
   * Renders {@code value} as one of the four numeric literal forms, one per accumulation path of
   * the aggregator: xs:integer and xs:decimal use the generic loop, xs:long and xs:double the SIMD
   * batch paths.
   */
  private static String numericLiteral(final long value, final int kind) {
    return switch (kind) {
      case 0 -> Long.toString(value);
      case 1 -> "xs:long(" + value + ")";
      case 2 -> value + "e0";
      default -> value + ".0";
    };
  }

  private Numeric numericResult(final String query) {
    return (Numeric) ExprUtil.asItem(new Query(query).execute(ctx)).atomize();
  }

  /** Query evaluation is lazy, so an error surfaces only once the whole result is consumed. */
  private void evaluateFully(final String query) {
    new Query(query).serialize(ctx, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
  }
}
