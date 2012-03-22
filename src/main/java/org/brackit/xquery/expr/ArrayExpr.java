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

import java.util.Arrays;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.array.DRArray;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ArrayExpr implements Expr {

	final Expr[] expr;
	final boolean[] flatten;

	public ArrayExpr(Expr[] expr, boolean[] flatten) {
		this.expr = expr;
		this.flatten = flatten;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple t) throws QueryException {
		return evaluateToItem(ctx, t);
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple t) throws QueryException {
		Sequence[] vals = new Sequence[10];
		int pos = 0;
		for (int i = 0; i < expr.length; i++) {
			Sequence res = expr[i].evaluate(ctx, t);
			if (res == null) {
				continue;
			} else if ((!flatten[i]) || (res instanceof Item)) {
				if (pos == vals.length) {
					vals = Arrays.copyOfRange(vals, 0,
							((vals.length * 3) / 2) + 1);
				}
				vals[pos++] = res;
			} else {
				Iter it = res.iterate();
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
			}
		}
		return new DRArray(vals, 0, pos);
	}

	@Override
	public boolean isUpdating() {
		for (Expr e : this.expr) {
			if (e.isUpdating()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isVacuous() {
		for (Expr e : this.expr) {
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
		for (int i = 0; i < expr.length; i++) {
			if (!first) {
				out.append(", ");
			}
			first = false;
			if (flatten[i]) {
				out.append("=");
			}
			out.append(expr[i].toString());
		}
		out.append("]");
		return out.toString();
	}
	
	public static void main(String[] args) throws QueryException {
		new XQuery("[ 1, '2', 3, (1 > 0) cast as xs:boolean, 1.2343 + 5, =(1,2,3)  ][[4]]").serialize(new QueryContext(), System.out);
	}
}
