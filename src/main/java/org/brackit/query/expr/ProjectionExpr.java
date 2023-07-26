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
package org.brackit.query.expr;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.BrackitQueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.Tuple;
import org.brackit.query.Query;
import org.brackit.query.atomic.Atomic;
import org.brackit.query.atomic.IntNumeric;
import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.Bits;
import org.brackit.query.jsonitem.object.ArrayObject;
import org.brackit.query.jdm.Expr;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.json.Object;

/**
 * @author Sebastian Baechle
 */
public final class ProjectionExpr implements Expr {

  private final Expr record;

  private final Expr[] fields;

  public ProjectionExpr(Expr record, Expr[] fields) {
    this.record = record;
    this.fields = fields;
  }

  @Override
  public Sequence evaluate(QueryContext ctx, Tuple t) {
    return evaluateToItem(ctx, t);
  }

  @Override
  public Item evaluateToItem(QueryContext ctx, Tuple t) {
    Sequence s = record.evaluateToItem(ctx, t);
    if (!(s instanceof Object)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Source expression in projection is not a record: %s",
                               s);
    }
    Object r = (Object) s;
    QNm[] names = new QNm[fields.length];
    Sequence[] vals = new Sequence[fields.length];
    for (int i = 0; i < fields.length; i++) {
      Item f = fields[i].evaluateToItem(ctx, t);
      if (f == null) {
        return null;
      }
      if (f instanceof QNm) {
        names[i] = (QNm) f;
        vals[i] = r.get((QNm) f);
      } else if (f instanceof IntNumeric) {
        names[i] = r.name(i);
        vals[i] = r.value((IntNumeric) f);
      } else if (f instanceof Atomic) {
        final QNm name = new QNm(((Atomic) f).asStr().toString());
        names[i] = name;
        vals[i] = r.get(name);
      } else {
        throw new QueryException(Bits.BIT_ILLEGAL_OBJECT_FIELD, "Illegal record field reference: %s", f);
      }
    }
    return new ArrayObject(names, vals);
  }

  @Override
  public boolean isUpdating() {
    if (record.isUpdating()) {
      return true;
    }
    for (Expr f : fields) {
      if (f.isUpdating()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isVacuous() {
    return false;
  }

  public String toString() {
    StringBuilder s = new StringBuilder();
    for (Expr f : fields) {
      s.append("=>");
      s.append(f);
    }
    return s.toString();
  }

  public static void main(String[] args) {
    new Query("let $a:= 1 let $b:= {'b':2.0} let $n := <x><y>yval</y></x> return {a:$a, $b, c:'3'}{a,c}=>c").serialize(new BrackitQueryContext(),
                                                                                                                       System.out);
  }
}
