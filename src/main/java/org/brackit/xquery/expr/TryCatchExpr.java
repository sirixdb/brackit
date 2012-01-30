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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.util.ExprUtil;
import org.brackit.xquery.xdm.Expr;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 * 
 */
public class TryCatchExpr implements Expr {
	
	public interface ErrorCatch {
		public boolean matches(QNm qname);
	}

	public static class Wildcard implements ErrorCatch {
		@Override
		public boolean matches(QNm qname) {
			return true;
		}
	}

	public static class NameWildcard implements ErrorCatch {
		final String nsURI;

		public NameWildcard(String nsURI) {
			this.nsURI = nsURI;
		}

		@Override
		public boolean matches(QNm qname) {
			return (nsURI.equals(qname.getNamespaceURI()));
		}
	}

	public static class NSWildcard implements ErrorCatch {
		final String localname;

		public NSWildcard(String localname) {
			this.localname = localname;
		}

		@Override
		public boolean matches(QNm qname) {
			return (localname.equals(qname.getLocalName()));
		}
	}

	public static class Name implements ErrorCatch {
		final String localname;
		final String nsURI;

		public Name(String localname, String nsURI) {
			this.localname = localname;
			this.nsURI = nsURI;
		}

		@Override
		public boolean matches(QNm qname) {
			return ((localname.equals(qname.getLocalName())) && (nsURI
					.equals(qname.getNamespaceURI())));
		}
	}

	final Expr expr;
	final ErrorCatch[][] catches;
	final Expr[] handler;
	final boolean bindCode;
	final boolean bindDesc;
	final boolean bindValue;
	final boolean bindModule;
	final boolean bindLineNo;
	final boolean bindColNo;
	final int bindCount;

	public TryCatchExpr(Expr expr, ErrorCatch[][] catches, Expr[] handler,
			boolean bindCode, boolean bindDesc, boolean bindValue,
			boolean bindModule, boolean bindLineNo, boolean bindColNo) {
		this.expr = expr;
		this.catches = catches;
		this.handler = handler;
		this.bindCode = bindCode;
		this.bindDesc = bindDesc;
		this.bindValue = bindValue;
		this.bindModule = bindModule;
		this.bindLineNo = bindLineNo;
		this.bindColNo = bindColNo;
		int bindCount = 0;
		if (bindCode) {
			bindCount++;
		}
		if (bindDesc) {
			bindCount++;
		}
		if (bindValue) {
			bindCount++;
		}
		if (bindModule) {
			bindCount++;
		}
		if (bindLineNo) {
			bindCount++;
		}
		if (bindColNo) {
			bindCount++;
		}
		this.bindCount = bindCount;
	}

	@Override
	public Sequence evaluate(QueryContext ctx, Tuple tuple)
			throws QueryException {
		try {
			return expr.evaluate(ctx, tuple);
		} catch (QueryException e) {
			for (int i = 0; i < catches.length; i++) {
				if (catches[i].length == 0) {
					Tuple t = bindInfo(tuple, e);
					return handler[i].evaluate(ctx, t);
				}
				for (int j = 0; j < catches[i].length; j++) {
					if (catches[i][j].matches(e.getCode())) {
						Tuple t = bindInfo(tuple, e);
						return handler[i].evaluate(ctx, t);
					}
				}

			}
			throw e;
		}
	}

	private Tuple bindInfo(Tuple tuple, QueryException e) throws QueryException {
		Sequence[] info = new Sequence[bindCount];
		int pos = 0;
		if (bindCode) {
			info[pos++] = e.getCode();
		}
		if (bindDesc) {
			info[pos++] = new Str(e.getMessage());
		}
		if (bindValue) {
			info[pos++] = null;
		}
		if (bindModule) {
			info[pos++] = null;
		}
		if (bindLineNo) {
			info[pos++] = null;
		}
		if (bindColNo) {
			info[pos++] = null;
		}
		Tuple t = tuple.concat(info);
		return t;
	}

	@Override
	public Item evaluateToItem(QueryContext ctx, Tuple tuple)
			throws QueryException {
		return ExprUtil.asItem(evaluate(ctx, tuple));
	}

	@Override
	public boolean isUpdating() {
		if (expr.isUpdating()) {
			return true;
		}
		for (Expr h : handler) {
			if (h.isUpdating()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isVacuous() {
		return false;
	}
}
