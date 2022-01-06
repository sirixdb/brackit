/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.expr;

import java.util.*;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.BrackitQueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.record.ArrayObject;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.Object;

/**
 * @author Sebastian Baechle
 */
public class ObjectExpr implements Expr {

  public static abstract class Field {
    abstract Object evaluate(QueryContext ctx, Tuple t) throws QueryException;

    abstract boolean isUpdating();

    boolean isVacuous() {
      return false;
    }
  }

  public static class ObjectField extends Field {
    final Expr expr;

    public ObjectField(Expr expr) {
      this.expr = expr;
    }

    @Override
    public Object evaluate(QueryContext ctx, Tuple t) {
      Sequence i = expr.evaluate(ctx, t);
      if (i instanceof Object) {
        return (Object) i;
      } else {
        final var names = new ArrayList<QNm>();
        final var values = new ArrayList<Sequence>();
        final var iter = i.iterate();
        Item item;
        while ((item = iter.next()) != null) {
          if (!(item instanceof Object)) {
            throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                     "Illegal item type in record constructor: %s",
                                     item.itemType());
          }

          final Object record = (Object) item;
          Collections.addAll(names, record.names().values().toArray(new QNm[0]));
          Collections.addAll(values, record.values().values().toArray(new Sequence[0]));
        }

        return new ArrayObject(names.toArray(new QNm[0]), values.toArray(new Sequence[0]));
      }
    }

    @Override
    boolean isUpdating() {
      return expr.isUpdating();
    }
  }

  public static class KeyValueField extends Field {
    final Expr nameExpr;
    final Expr valueExpr;

    public KeyValueField(Expr name, Expr expr) {
      this.nameExpr = name;
      this.valueExpr = expr;
    }

    @Override
    public Object evaluate(QueryContext ctx, Tuple t) {
      Sequence names = nameExpr.evaluateToItem(ctx, t);
      Sequence val = valueExpr.evaluateToItem(ctx, t);

      if (names instanceof Str) {
        return new ArrayObject(new QNm[] { new QNm(((Str) names).stringValue()) }, new Sequence[] { val });
      }

      return new ArrayObject(new QNm[] { (QNm) names.get(0) }, new Sequence[] { val });
    }

    @Override
    boolean isUpdating() {
      return valueExpr.isUpdating();
    }
  }

  final Field[] fields;

  public ObjectExpr(Field[] fields) {
    this.fields = fields;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple t) {
    return evaluateToItem(ctx, t);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple t) {
    Set<QNm> dedup = new HashSet<>();
    QNm[] names = new QNm[fields.length];
    Sequence[] vals = new Sequence[fields.length];
    int pos = 0;
    for (int i = 0; i < fields.length; i++) {
      Object res = fields[i].evaluate(ctx, t);
      for (int j = 0; j < res.len(); j++) {
        QNm name = res.name(j);
        if (!dedup.add(name)) {
          throw new QueryException(Bits.BIT_DUPLICATE_RECORD_FIELD, "Duplicate field name: %s", name);
        }
        Sequence val = res.value(j);

        if (pos == names.length) {
          names = Arrays.copyOfRange(names, 0, ((names.length * 3) / 2) + 1);
          vals = Arrays.copyOfRange(vals, 0, ((vals.length * 3) / 2) + 1);
        }
        names[pos] = name;
        vals[pos] = val;
        pos++;
      }
    }
    if (pos < names.length) {
      names = Arrays.copyOfRange(names, 0, pos);
      vals = Arrays.copyOfRange(vals, 0, pos);
    }
    return new ArrayObject(names, vals);
  }

  @Override
  public boolean isUpdating() {
    for (Field e : this.fields) {
      if (e.isUpdating()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isVacuous() {
    for (Field e : this.fields) {
      if (!e.isVacuous()) {
        return false;
      }
    }
    return true;
  }

  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append("[");
    boolean first = true;
    for (int i = 0; i < fields.length; i++) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      out.append(fields[i].toString());
    }
    out.append("]");
    return out.toString();
  }

  public static void main(String[] args) {
    new XQuery("{ a:1, b:2, c:3 , {x:1}, y:5, z : {foo : 'bar'}, 'aha' : 2, 'h h' : 5 }").serialize(new BrackitQueryContext(),
                                                                                                    System.out);
  }
}
