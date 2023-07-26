package org.brackit.query.update.json.op;

import org.brackit.query.atomic.QNm;
import org.brackit.query.update.op.OpType;
import org.brackit.query.update.op.UpdateOp;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.StructuredItem;
import org.brackit.query.jdm.json.Array;
import org.brackit.query.jdm.json.Object;

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
