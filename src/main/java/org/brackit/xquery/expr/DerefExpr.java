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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.BrackitQueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.array.DArray;
import org.brackit.xquery.array.DRArray;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.Record;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Sebastian Baechle
 * 
 */
public class DerefExpr implements Expr {

	final Expr record;
	final Expr[] fields;

	public DerefExpr(Expr record, Expr[] fields) {
		this.record = record;
		this.fields = fields;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple t) throws QueryException {
		Sequence s = record.evaluateToItem(ctx, t);
		for (int i = 0; i < fields.length && s != null; i++) {
			if (s instanceof Array) {
				final var array = ((Array) s);
				Sequence[] vals = new Sequence[array.len()];
				int pos = 0;
				for (final Sequence value : array.values()) {
					final Sequence val = value.evaluateToItem(ctx, t);
					if (!(val instanceof Record)) {
						throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
								"Context item in navigation step is not a record: %s",
								s);
					}
					Record r = (Record) val;
					Item f = fields[i].evaluateToItem(ctx, t);
					if (f == null) {
						continue;
					}
					if (f instanceof QNm) {
						s = r.get((QNm) f);
					} else if (f instanceof IntNumeric) {
						s = r.value((IntNumeric) f);
					} else {
						throw new QueryException(Bits.BIT_ILLEGAL_RECORD_FIELD,
								"Illegal record field reference: %s", f);
					}

					vals[pos] = s;
					pos++;
				}

				return new DArray(vals);
			} else {
				if (!(s instanceof Record)) {
					throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
							"Context item in navigation step is not a record: %s", s);
				}
				Record r = (Record) s;
				Item f = fields[i].evaluateToItem(ctx, t);
				if (f == null) {
					return null;
				}
				if (f instanceof QNm) {
					s = r.get((QNm) f);
				} else if (f instanceof IntNumeric) {
					s = r.value((IntNumeric) f);
				} else {
					throw new QueryException(Bits.BIT_ILLEGAL_RECORD_FIELD, "Illegal record field reference: %s", f);
				}
			}
		}
		return s;
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		final var res = evaluate(ctx, tuple);
		if ((res == null) || (res instanceof Item)) {
			return (Item) res;
		}
		Sequence[] vals = new Sequence[fields.length];
		int pos = 0;
		final Iter it = res.iterate();
		try {
			Item item;
			while ((item = it.next()) != null) {
				if (pos == vals.length) {
					vals = Arrays.copyOfRange(vals, 0,
							((vals.length * 3) / 2) + 1);
				}
				vals[pos++] = item;
			}
		} finally {
			it.close();
		}
		return new DRArray(vals, 0, pos);
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
			s.append("->");
			s.append(f);
		}
		return s.toString();
	}

	public static void main(String[] args) throws QueryException {
		// a:1, b:2, c:3 , {x:1}, d:5,
		new XQuery(
				"let $n := <x><y>yval</y></x> return {e : {m:'mvalue', n:$n}}=>e=>n/y")
				.serialize(new BrackitQueryContext(), System.out);
	}
}
