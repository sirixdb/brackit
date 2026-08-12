package io.brackit.query.expr;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int32;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.NestedSequence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An operand that is neither an {@code ItemSequence} nor a {@code LazySequence} reaches
 * {@code ExprUtil.asItem}, which answers null for a sequence that iterates empty. The type-error
 * branch then used to ask that absent item for its type.
 *
 * <p>{@link NestedSequence} is such a sequence — the sequence aggregator builds them — and any
 * backend supplying its own {@code Sequence} implementation can be one too.
 */
public class ArrayAccessEmptyOperandTest extends XQueryBaseTest {

  private record Constant(Sequence value) implements Expr {
    @Override
    public Sequence evaluate(QueryContext ctx, Tuple tuple) throws QueryException {
      return value;
    }

    @Override
    public Item evaluateToItem(QueryContext ctx, Tuple tuple) throws QueryException {
      return (Item) value;
    }

    @Override
    public boolean isUpdating() {
      return false;
    }

    @Override
    public boolean isVacuous() {
      return false;
    }
  }

  @Test
  public void emptyOperandSequenceYieldsEmptySequence() {
    final Expr operand = new Constant(new NestedSequence());
    final Expr index = new Constant(new Int32(0));
    assertNull(new ArrayAccessExpr(operand, index).evaluate(ctx, null),
               "an operand that iterates empty must answer the empty sequence, not fail");
  }
}
