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
package org.brackit.xquery.record;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.array.DArray;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Array;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 * 
 */
public class ArrayRecord extends AbstractRecord {
	private static final Sequence EMPTY = new ItemSequence();
	// two arrays for key/value mapping
	// if lookup costs dominate because the compiler cannot
	// exploit positional access as alternative, we should
	// switch to a more efficient (hash) map.
	private final QNm[] fields;
	private final Sequence[] vals;

	public ArrayRecord(QNm[] fields, Sequence[] values) {
		this.fields = fields;
		this.vals = values;
	}

	@Override
	public Sequence get(QNm field) throws QueryException {
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].atomicCmp(field) == 0) {
				return vals[i];
			}
		}
		return null;
	}

	@Override
	public Sequence value(IntNumeric i) throws QueryException {
		try {
			// TODO ensure that index is not out of int range
			return (vals[i.intValue()]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid field index: %s", i);
		}
	}

	@Override
	public Sequence value(int i) throws QueryException {
		try {
			return (vals[i]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid field index: %s", i);
		}
	}

	@Override
	public Array names() throws QueryException {
		return new DArray(fields);
	}

	@Override
	public Array values() throws QueryException {
		return new DArray(vals);
	}

	@Override
	public QNm name(IntNumeric i) throws QueryException {
		try {
			// TODO ensure that index is not out of int range
			return (fields[i.intValue()]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid field index: %s", i);
		}
	}

	@Override
	public QNm name(int i) throws QueryException {
		try {
			return (fields[i]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid field index: %s", i);
		}
	}

	@Override
	public IntNumeric length() throws QueryException {
		int l = vals.length;
		return (l <= 20) ? Int32.ZERO_TWO_TWENTY[l] : new Int32(l);
	}

	@Override
	public int len() throws QueryException {
		return vals.length;
	}
}
