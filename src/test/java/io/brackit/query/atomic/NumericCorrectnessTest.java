package io.brackit.query.atomic;

import io.brackit.query.Query;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.jdm.Sequence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for numeric correctness bugs (integer/decimal promotion, mod, overflow, abs,
 * effective boolean value) found in the 2026-06-10 production-readiness audit.
 */
public class NumericCorrectnessTest extends XQueryBaseTest {

  private static Dec dec(String s) {
    return new Dec(new BigDecimal(s));
  }

  // --- integer + decimal must NOT truncate (was 1 + 2.5 -> 3) ---

  @Test
  public void intPlusDecimal() {
    ResultChecker.dCheck(dec("3.5"), new Query("1 + 2.5").execute(ctx));
  }

  @Test
  public void intMinusDecimal() {
    ResultChecker.dCheck(dec("-1.5"), new Query("1 - 2.5").execute(ctx));
  }

  @Test
  public void decimalPlusInt() {
    ResultChecker.dCheck(dec("3.5"), new Query("2.5 + 1").execute(ctx));
  }

  @Test
  public void pureIntStaysInteger() {
    ResultChecker.dCheck(new Int32(3), new Query("1 + 2").execute(ctx));
  }

  // --- decimal division scale (was `1.0 div 2.0` -> 0, `10.2 div 4` -> 2.6:
  // the result scale was computed as a.scale() - b.scale(), the negated
  // MULTIPLICATION identity, and HALF_EVEN rounding destroyed terminating
  // quotients) ---

  @Test
  public void decimalDivDecimalTerminating() {
    ResultChecker.dCheck(dec("0.5"), new Query("1.0 div 2.0").execute(ctx));
  }

  @Test
  public void decimalDivIntTerminating() {
    ResultChecker.dCheck(dec("2.55"), new Query("10.2 div 4").execute(ctx));
  }

  @Test
  public void decimalDivIntNonTerminating() {
    // Non-terminating quotients round to the 18-digit scale the integer
    // division path uses.
    ResultChecker.dCheck(dec("0.033333333333333333"), new Query("0.1 div 3").execute(ctx));
  }

  @Test
  public void avgDecimalsTerminating() {
    // avg((10.2, ...)) routes through Dec#div — was rounded to one decimal digit.
    ResultChecker.dCheck(dec("2.55"), new Query("avg((3, 3.7, 2, 1.5))").execute(ctx));
  }

  @Test
  public void integerDivUnchanged() {
    ResultChecker.dCheck(dec("2.5"), new Query("5 div 2").execute(ctx));
  }

  // --- sum / avg over mixed int+decimal (was sum((1,2.5,3)) -> 6) ---

  @Test
  public void sumMixed() {
    ResultChecker.dCheck(dec("6.5"), new Query("sum((1, 2.5, 3))").execute(ctx));
  }

  @Test
  public void avgMixedIsDecimalGreaterThanTwo() {
    final Sequence r = new Query("avg((1, 2.5, 3))").execute(ctx);
    final Numeric n = (Numeric) r;
    // 6.5 / 3 = 2.1666... — must be a decimal strictly between 2 and 3, not the truncated 2.
    assertTrue(n.decimalValue().compareTo(new BigDecimal("2")) > 0 && n.decimalValue().compareTo(new BigDecimal("3"))
        < 0, "avg should be ~2.1667, got " + n);
  }

  // --- mod with a decimal operand (was 7.5 mod 2 -> 1) ---

  @Test
  public void modDecimalDividend() {
    ResultChecker.dCheck(dec("1.5"), new Query("7.5 mod 2").execute(ctx));
  }

  @Test
  public void modDecimalDivisorValue() {
    // 7 mod 2.5 = 2.0
    final Numeric n = (Numeric) new Query("7 mod 2.5").execute(ctx);
    assertEquals(0, n.decimalValue().compareTo(new BigDecimal("2.0")), "got " + n);
  }

  @Test
  public void modPureIntStaysInteger() {
    ResultChecker.dCheck(new Int32(1), new Query("7 mod 2").execute(ctx));
  }

  // --- fn:abs of a negative decimal must not truncate (was abs(-2.5) -> 2) ---

  @Test
  public void absNegativeDecimal() {
    ResultChecker.dCheck(dec("2.5"), new Query("abs(-2.5)").execute(ctx));
  }

  // --- direct atomic edge cases (paths not reachable via small literals) ---

  @Test
  public void int64OverflowEscalates() throws Exception {
    // Long.MAX + 1 must escalate to a BigDecimal-backed Int (was silently wrapping to Long.MIN).
    final Numeric r = new Int64(Long.MAX_VALUE).add(new Int32(1));
    assertEquals(new BigDecimal("9223372036854775808"), r.decimalValue());
  }

  @Test
  public void bareIntOperandDivIdivMod() throws Exception {
    final Int bigTwo = new Int(new BigDecimal(2));
    // div: 7 / 2 = 3.5 ; idiv: 3 ; mod: 1 — previously all computed 7 + 2 = 9.
    assertEquals(0, ((Numeric) new Int32(7).div(bigTwo)).decimalValue().compareTo(new BigDecimal("3.5")));
    assertEquals(new BigDecimal("3"), ((Numeric) new Int32(7).idiv(bigTwo)).decimalValue());
    assertEquals(new BigDecimal("1"), ((Numeric) new Int32(7).mod(bigTwo)).decimalValue());
  }

  @Test
  public void floatEffectiveBooleanValue() throws Exception {
    assertTrue(new Flt(2.0f).booleanValue(), "finite non-zero float EBV must be true");
    assertFalse(new Flt(0.0f).booleanValue(), "zero float EBV must be false");
    assertFalse(new Flt(Float.POSITIVE_INFINITY).booleanValue(), "INF float EBV must be false");
  }

  @Test
  public void int32BoundaryEffectiveBooleanValue() throws Exception {
    assertTrue(new Int32(Integer.MAX_VALUE).booleanValue(), "MAX_VALUE is non-zero -> EBV true");
    assertTrue(new Int32(Integer.MIN_VALUE).booleanValue(), "MIN_VALUE is non-zero -> EBV true");
  }
}
