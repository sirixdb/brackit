package org.brackit.xquery.update.json.op;

import org.brackit.xquery.update.op.OpType;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.StructuredItem;
import org.brackit.xquery.jdm.json.Array;

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
