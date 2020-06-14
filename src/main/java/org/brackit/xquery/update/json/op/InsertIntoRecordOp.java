package org.brackit.xquery.update.json.op;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.update.op.OpType;
import org.brackit.xquery.update.op.UpdateOp;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.StructuredItem;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.Record;

public final class InsertIntoRecordOp implements UpdateOp {

  private final Record target;

  private final Record source;

  public InsertIntoRecordOp(final Record target, final Record source) {
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
