package io.brackit.query.atomic;

import io.brackit.query.jdm.Type;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Int32}/{@link Int64} must compare with and operate on ANY {@link DblNumeric}/
 * {@link FltNumeric} operand, dispatching on the numeric INTERFACE — not only on the concrete
 * {@link Dbl}/{@link Flt} classes. Externally supplied numerics (e.g. document-sourced numbers that
 * wrap and delegate to a {@code Dbl}) implement the interface but are not the concrete class, so a
 * concrete {@code instanceof Dbl} check would mis-route them: comparison threw
 * {@code err:XPTY0004 "Cannot compare 'xs:integer' with 'xs:double'"} and arithmetic silently
 * narrowed to {@code xs:float}.
 */
public class IntDoublePromotionTest {

  /** A {@link DblNumeric} that is NOT the concrete {@link Dbl} (mirrors a delegating wrapper). */
  private static final class ExternalDbl extends AbstractNumeric implements DblNumeric {
    private final Dbl delegate;

    ExternalDbl(final double v) {
      this.delegate = new Dbl(v);
    }

    @Override
    public Type type() {
      return delegate.type();
    }

    @Override
    public double doubleValue() {
      return delegate.doubleValue();
    }

    @Override
    public float floatValue() {
      return delegate.floatValue();
    }

    @Override
    public BigDecimal decimalValue() {
      return delegate.decimalValue();
    }

    @Override
    public BigDecimal integerValue() {
      return delegate.integerValue();
    }

    @Override
    public long longValue() {
      return delegate.longValue();
    }

    @Override
    public int intValue() {
      return delegate.intValue();
    }

    @Override
    public boolean booleanValue() {
      return delegate.booleanValue();
    }

    @Override
    public String stringValue() {
      return delegate.stringValue();
    }

    @Override
    public Atomic asType(final Type type) {
      return delegate.asType(type);
    }

    @Override
    public int cmp(final Atomic other) {
      return delegate.cmp(other);
    }

    @Override
    public int atomicCmpInternal(final Atomic other) {
      return delegate.atomicCmpInternal(other);
    }

    @Override
    public Numeric add(final Numeric other) {
      return delegate.add(other);
    }

    @Override
    public Numeric subtract(final Numeric other) {
      return delegate.subtract(other);
    }

    @Override
    public Numeric multiply(final Numeric other) {
      return delegate.multiply(other);
    }

    @Override
    public Numeric div(final Numeric other) {
      return delegate.div(other);
    }

    @Override
    public Numeric idiv(final Numeric other) {
      return delegate.idiv(other);
    }

    @Override
    public Numeric mod(final Numeric other) {
      return delegate.mod(other);
    }

    @Override
    public Numeric negate() {
      return delegate.negate();
    }

    @Override
    public Numeric round() {
      return delegate.round();
    }

    @Override
    public Numeric abs() {
      return delegate.abs();
    }

    @Override
    public Numeric floor() {
      return delegate.floor();
    }

    @Override
    public Numeric ceiling() {
      return delegate.ceiling();
    }

    @Override
    public Numeric roundHalfToEven(final int precision) {
      return delegate.roundHalfToEven(precision);
    }

    @Override
    public IntNumeric asIntNumeric() {
      return delegate.asIntNumeric();
    }
  }

  @Test
  public void int64ComparesWithNonConcreteDblNumeric() {
    final Int64 i = new Int64(5_000_000_000L);
    final ExternalDbl d = new ExternalDbl(3.7);
    assertTrue(i.cmp(d) > 0, "5e9 (Int64) gt 3.7 (DblNumeric)");
    assertTrue(d.cmp(i) < 0, "3.7 (DblNumeric) lt 5e9 (Int64)");
    assertTrue(i.atomicCmp(d) > 0, "atomicCmp must promote, not throw");
  }

  @Test
  public void int32ComparesWithNonConcreteDblNumeric() {
    final Int32 i = new Int32(4);
    final ExternalDbl d = new ExternalDbl(3.7);
    assertTrue(i.cmp(d) > 0);
    assertTrue(i.atomicCmp(d) > 0);
  }

  @Test
  public void intArithmeticWithNonConcreteDblNumericStaysDouble() {
    // 3 + 0.5 must promote to xs:double, not narrow to xs:float.
    assertEquals(Type.DBL, new Int32(3).add(new ExternalDbl(0.5)).type());
    assertEquals(Type.DBL, new Int64(5_000_000_000L).add(new ExternalDbl(0.5)).type());
    assertEquals(3.5, new Int32(3).add(new ExternalDbl(0.5)).doubleValue(), 1e-12);
  }
}
