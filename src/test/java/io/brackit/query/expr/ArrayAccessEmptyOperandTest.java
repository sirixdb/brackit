package io.brackit.query.expr;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.Tuple;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int32;
import io.brackit.query.jdm.Expr;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.sequence.NestedSequence;
import io.brackit.query.sequence.ItemSequence;
import io.brackit.query.jsonitem.array.DArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

/**
 * An operand that iterates empty must remain empty when array navigation maps over it.
 * The former singleton dispatch used to ask an absent item for its type.
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
    try (Iter it = new ArrayAccessExpr(operand, index).evaluate(ctx, null).iterate()) {
      assertNull(it.next(), "an operand that iterates empty must answer the empty sequence, not fail");
    }
  }

  @Test
  public void unboxingSkipsEmptyMembersInItemAndMappedArrays() {
    DArray array = new DArray(Arrays.asList(null, new ItemSequence(), Int32.ONE, null));
    Expr index = new Constant(null);
    ResultChecker.dCheck(Int32.ONE, new ArrayAccessExpr(new Constant(array), index).evaluate(ctx, null));
    ResultChecker.dCheck(Int32.ONE,
                         new ArrayAccessExpr(new Constant(new ItemSequence(new DArray(List.of()), array)), index).evaluate(ctx,
                                                                                                                          null));
  }

  @Test
  public void mappedLookupSkipsEmptySelectedMembers() {
    DArray nullMember = new DArray(Arrays.asList((Sequence) null));
    DArray emptyMember = new DArray(List.of(new ItemSequence()));
    DArray nonempty = new DArray(List.of(Int32.ONE));
    Expr operand = new Constant(new ItemSequence(nullMember, emptyMember, nonempty));
    ResultChecker.dCheck(Int32.ONE, new ArrayAccessExpr(operand, new Constant(Int32.ZERO)).evaluate(ctx, null));
  }
}
