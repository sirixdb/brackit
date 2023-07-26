package io.brackit.query.update.json.op;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.StructuredItem;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.update.op.OpType;
import io.brackit.query.update.op.UpdateOp;

/**
 * @author Johannes Lichtenberger
 */
public final class InsertIntoRecordOp implements UpdateOp {

  private final Object target;

  private final Object source;

  public InsertIntoRecordOp(final Object target, final Object source) {
    this.target = target;
    this.source = source;
  }

  @Override
  public StructuredItem getTarget() {
    return target;
  }

  @Override
  public void apply() {
    final Array names = source.names();

    names.values().forEach(name -> {
      final Sequence value = source.get((QNm) name);
      target.insert((QNm) name, value);
    });
  }

  @Override
  public OpType getType() {
    return OpType.INSERT_INTO;
  }
}
