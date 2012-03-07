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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.util.aggregator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int64;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.atomic.YMD;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;

/**
 * Aggregator for operations with fn:sum() and fn:avg() semantics.
 * 
 * @author Sebastian Baechle
 * 
 */
public class SumAvgAggregator implements Aggregator {
	private enum AggType {
		NUMERIC, YMD, DTD
	}

	final boolean avg;
	final Sequence defaultValue;
	long count;
	Atomic sum = null;
	AggType aggType = null;

	public SumAvgAggregator(boolean avg, Sequence defaultValue) {
		this.avg = avg;
		this.defaultValue = defaultValue;
	}

	@Override
	public Sequence getAggregate() throws QueryException {
		if (sum == null) {
			return (avg) ? null : defaultValue;
		}		
		if (aggType == AggType.NUMERIC) {
			sum = numericAggCalc((Numeric) sum, count);
		} else if (aggType == AggType.YMD) {
			sum = ymdAggCalc((YMD) sum, count);
		} else if (aggType == AggType.DTD) {
			sum = dtdAggCalc((DTD) sum, count);
		}
		return sum;
	}

	public void add(Sequence seq) throws QueryException {
		if (seq == null) {
			return;
		}
		if (seq instanceof Item) {
			addItem((Item) seq, (sum == null));
		} else {
			addSequence(seq, (sum == null));
		}
	}

	private void addSequence(Sequence seq, boolean first) throws QueryException {
		Item item;
		Iter in = seq.iterate();
		try {
			if (first) {
				if ((item = in.next()) != null) {
					addItem(item, first);
				} else {
					return;
				}
			}
			if (aggType == AggType.NUMERIC) {
				sum = numericSum(in, (Numeric) sum);
			} else if (aggType == AggType.YMD) {
				sum = ymdSum(in, (YMD) sum);
			} else if (aggType == AggType.DTD) {
				sum = dtdSum(in, (DTD) sum);
			}
		} finally {
			in.close();
		}
	}

	private void addItem(Item item, boolean first) throws QueryException {
		count++;
		if (!first) {
			if (aggType == AggType.NUMERIC) {
				sum = numericSum((Numeric) sum, item);
			} else if (aggType == AggType.YMD) {
				sum = ymdSum((YMD) sum, item);
			} else if (aggType == AggType.DTD) {
				sum = dtdSum((DTD) sum, item);
			}
		} else {
			sum = item.atomize();
			Type type = sum.type();

			if (type == Type.UNA) {
				sum = Cast.cast(null, sum, Type.DBL, false);
				type = Type.DBL;
			}
			if (type.isNumeric()) {
				aggType = AggType.NUMERIC;
			} else if (type.instanceOf(Type.YMD)) {
				aggType = AggType.YMD;
			} else if (type.instanceOf(Type.DTD)) {
				aggType = AggType.DTD;
			} else {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Cannot compute sum/avg for items of type: %s", type);
			}
		}
	}

	private Atomic numericSum(Iter in, Numeric sum) throws QueryException {
		Item item;
		while ((item = in.next()) != null) {
			sum = numericSum(sum, item);
			count++;
		}
		return sum;
	}

	private Numeric numericSum(Numeric sum, Item item) throws QueryException {
		Atomic s = item.atomize();
		Type type = s.type();

		if (type == Type.UNA) {
			s = Cast.cast(null, s, Type.DBL, false);
			type = Type.DBL;
		} else if (!(s instanceof Numeric)) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Incompatible types in aggregate function: %s and %s.",
					sum, type);
		}

		sum = sum.add((Numeric) s);
		return sum;
	}

	private Numeric numericAggCalc(Numeric sum, long count)
			throws QueryException {
		return (avg ? sum.div(new Int64(count)) : sum);
	}

	private Atomic ymdSum(Iter in, YMD sum) throws QueryException {
		Item item;
		while ((item = in.next()) != null) {
			sum = ymdSum(sum, item);
			count++;
		}
		return sum;
	}

	private YMD ymdSum(YMD sum, Item item) throws QueryException {
		Atomic s = item.atomize();
		Type type = s.type();

		if (type == Type.UNA) {
			s = Cast.cast(null, s, Type.YMD, false);
		} else if (!type.instanceOf(Type.YMD)) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Incompatible types in aggregate function: %s and %s.",
					Type.YMD, type);
		}

		sum = sum.add((YMD) s);
		return sum;
	}

	private YMD ymdAggCalc(YMD agg, long count) throws QueryException {
		return (avg ? agg.divide(new Dbl(count)) : agg);
	}

	private Atomic dtdSum(Iter in, DTD sum) throws QueryException {
		Item item;
		while ((item = in.next()) != null) {
			sum = dtdSum(sum, item);
			count++;
		}
		return sum;
	}

	private DTD dtdSum(DTD sum, Item item) throws QueryException {
		Atomic s = item.atomize();
		Type type = s.type();

		if (type == Type.UNA) {
			s = Cast.cast(null, s, Type.DTD, false);
		} else if (!type.instanceOf(Type.DTD)) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Incompatible types in aggregate function: %s and %s.",
					Type.DTD, type);
		}

		sum = sum.add((DTD) s);
		return sum;
	}

	private DTD dtdAggCalc(DTD agg, long count) throws QueryException {
		return (avg ? agg.divide(new Dbl(count)) : agg);
	}
}