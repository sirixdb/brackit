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
package org.brackit.xquery.array;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Array;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 * 
 */
public class DRArray extends AbstractArray {
	private final Sequence[] vals;
	private final int start;
	private final int end;

	public DRArray(Sequence[] vals, int start, int end) throws QueryException {
		if ((start < 0) || (start > end) || (start >= vals.length)) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid array start index: %s", start);
		}
		if ((end < 0) || (end > vals.length)) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid array end index: %s", start);
		}
		this.vals = vals;
		this.start = start;
		this.end = end;

	}

	@Override
	public Sequence at(IntNumeric i) throws QueryException {
		try {
			// TODO ensure that index is not out of int range
			int ii = start + i.intValue();
			if (ii >= end) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Invalid array index: %s", i);
			}
			return (vals[ii]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid array index: %s", i);
		}
	}

	@Override
	public Sequence at(int i) throws QueryException {
		try {
			int ii = start + i;
			if (ii >= end) {
				throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
						"Invalid array index: %s", i);
			}
			return (vals[ii]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
					"Invalid array index: %s", i);
		}
	}

	@Override
	public IntNumeric length() throws QueryException {
		int l = end - start;
		return (l <= 20) ? Int32.ZERO_TWO_TWENTY[l] : new Int32(l);
	}

	@Override
	public int len() throws QueryException {
		return end - start;
	}

	@Override
	public Array range(IntNumeric from, IntNumeric to) throws QueryException {
		// TODO ensure that indexes are not out of int range
		return new DRArray(vals, start + from.intValue(), start + to.intValue());
	}
}