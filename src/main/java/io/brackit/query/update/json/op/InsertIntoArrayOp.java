package io.brackit.query.update.json.op;

import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.StructuredItem;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.update.op.OpType;
import io.brackit.query.update.op.UpdateOp;

/**
 * @author Johannes Lichtenberger
 */
public final class InsertIntoArrayOp implements UpdateOp {

  private final Array target;

  private final Sequence source;

  private final int position;

  public InsertIntoArrayOp(final Array target, final Sequence source, final int position) {
    this.target = target;
    this.source = source;
    this.position = position;
  }

  @Override
  public StructuredItem getTarget() {
    return target;
  }

  @Override
  public void apply() {
    if (position == -1) {
      target.append(source);
    } else {
      target.insert(position, source);
    }
  }

  @Override
  public OpType getType() {
    return OpType.INSERT_INTO;
  }
}
