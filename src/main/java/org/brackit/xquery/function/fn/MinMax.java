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
package org.brackit.xquery.function.fn;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class MinMax extends AbstractFunction {
	private final boolean min;

	public MinMax(QNm name, Signature signature, boolean min) {
		super(name, signature, true);
		this.min = min;
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args)
			throws QueryException {
		if (args.length == 2) {
			Str collation = (Str) args[1];

			if (!collation.str
					.equals("http://www.w3.org/2005/xpath-functions/collation/codepoint")) {
				throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION,
						"Unsupported collation: %s", collation);
			}
		}

		Sequence seq = args[0];
		Item item;
		Atomic minmax = null;
		Type minmaxType = null;
		
		if (seq == null) {
			return null;
		}

		Iter in = seq.iterate();
		try {
			if ((item = in.next()) != null) {
				minmax = item.atomize();
				minmaxType = minmax.type();

				if (minmaxType == Type.UNA) {
					minmax = Cast.cast(null, minmax, Type.DBL, false);
					minmaxType = Type.DBL;
				}

				if (minmaxType.isNumeric()) {
					minmax = numericMinmax(ctx, in, minmax);
				} else if (minmaxType.instanceOf(Type.STR)) {
					minmax = stringMinmax(ctx, in, minmax);
				} else if (minmaxType.instanceOf(Type.YMD)
						|| minmaxType.instanceOf(Type.DTD)
						|| minmaxType.instanceOf(Type.DATE)
						|| minmaxType.instanceOf(Type.AURI)
						|| minmaxType.instanceOf(Type.BOOL)
						|| minmaxType.instanceOf(Type.DATE)
						|| minmaxType.instanceOf(Type.TIME)) {
					minmax = genericMinmax(ctx, in, minmax);
				} else {
					throw new QueryException(
							ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
							"Cannot compute min/max for items of type: %s",
							minmaxType);
				}
			}
		} finally {
			in.close();
		}

		return minmax;
	}

	private Atomic genericMinmax(QueryContext ctx, Iter in, Atomic minmax)
			throws QueryException {
		Item item;
		final Type minmaxType = minmax.type().getPrimitiveBase();

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (!type.instanceOf(minmaxType)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incomparable types in aggregate function: %s and %s.",
						minmaxType, type);
			}

			int res = minmax.cmp(s);

			if ((min) ? (res > 0) : (res < 0)) {
				minmax = s;
			}
		}

		return minmax;
	}

	private Atomic stringMinmax(QueryContext ctx, Iter in, Atomic minmax)
			throws QueryException {
		Item item;
		final Type minmaxType = minmax.type().getPrimitiveBase();

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (type == Type.AURI) {
				s = Cast.cast(null, s, Type.STR, false);
				type = Type.STR;
			} else if (!type.instanceOf(Type.STR)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incomparable types in aggregate function: %s and %s.",
						minmaxType, type);
			}

			int res = minmax.cmp(s);

			if ((min) ? (res > 0) : (res < 0)) {
				minmax = s;
			}
		}

		return minmax;
	}

	private Atomic numericMinmax(QueryContext ctx, Iter in, Atomic minmax)
			throws QueryException {
		Item item;
		final Type minmaxType = minmax.type();

		while ((item = in.next()) != null) {
			Atomic s = item.atomize();
			Type type = s.type();

			if (type == Type.UNA) {
				s = Cast.cast(null, s, Type.DBL, false);
				type = Type.DBL;
			}

			if (!(s instanceof Numeric)) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Incomparable types in aggregate function: %s and %s.",
						minmaxType, type);
			}

			int res = minmax.cmp(s);

			if ((min) ? (res > 0) : (res < 0)) {
				minmax = s;
			}
		}

		return minmax;
	}
}