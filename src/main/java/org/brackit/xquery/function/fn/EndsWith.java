package org.brackit.xquery.function.fn;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;

public class EndsWith extends AbstractFunction {
    public EndsWith(QNm name, Signature signature) {
        super(name, signature, true);
    }

    public boolean isZeroLength(Sequence[] args) {
        if (args[0] == null || ((Item) args[0]).atomize().stringValue().length() == 0 && args[1] == null
                || ((Item) args[1]).atomize().stringValue().length() > 0) {
            return false;
        } else if (args[1] == null || ((Item) args[1]).atomize().stringValue().length() == 0 && args[0] == null
                || ((Item) args[0]).atomize().stringValue().length() > 0) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) {
        if (isZeroLength(args) == Boolean.TRUE) {
            return Int32.ONE;
        }
        if (isZeroLength(args) == Boolean.FALSE) {
            return Int32.ZERO;
        }
        String first = ((Item) args[0]).atomize().stringValue();
        String second = ((Item) args[1]).atomize().stringValue();
        boolean result = first.contains(second);
        return (result == true) ? Int32.ONE : Int32.ZERO;

    }

}
