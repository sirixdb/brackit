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
package org.brackit.xquery.operator;

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class TupleImpl implements Tuple {
	public static final Tuple EMPTY_TUPLE = new TupleImpl();

	private final Sequence[] sequences;

	public TupleImpl() {
		sequences = new Sequence[0];
	}

	public TupleImpl(Sequence t) {
		sequences = new Sequence[] { t };
	}

	public TupleImpl(Sequence[] t) {
		sequences = Arrays.copyOf(t, t.length);
	}

	@Override
	public Tuple project(int... positions) throws QueryException {
		Sequence[] projected = new Sequence[positions.length];
		int targetPos = 0;
		for (int pos : positions) {
			projected[targetPos++] = get(pos);
		}
		return new TupleImpl(projected);
	}

	@Override
	public Tuple project(int start, int end) throws QueryException {
		if ((start < 0) || (start >= sequences.length)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					start);
		}
		if ((end < start) || (end >= sequences.length)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					end);
		}
		return new TupleImpl(Arrays.copyOfRange(sequences, start, end));
	}

	@Override
	public Tuple replace(int position, Sequence s) throws QueryException {
		if ((position < 0) || (position >= sequences.length)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					position);
		}
		Sequence[] tmp = Arrays.copyOf(sequences, sequences.length);
		tmp[position] = s;
		return new TupleImpl(tmp);
	}

	@Override
	public Tuple concat(Sequence s) throws QueryException {
		Sequence[] tmp = Arrays.copyOf(sequences, sequences.length + 1);
		tmp[sequences.length] = s;
		return new TupleImpl(tmp);
	}

	@Override
	public Tuple concat(Sequence[] s) throws QueryException {
		Sequence[] tmp = Arrays.copyOf(sequences, sequences.length + s.length);
		System.arraycopy(s, 0, tmp, sequences.length, s.length);
		return new TupleImpl(tmp);
	}
	
	@Override
	public Tuple conreplace(Sequence con, int position, Sequence s)
			throws QueryException {
		int nLen = sequences.length + 1;
		if ((position < 0) || (position >= nLen)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					position);
		}
		Sequence[] tmp = Arrays.copyOf(sequences, nLen);
		tmp[sequences.length] = s;
		tmp[position] = s;
		return new TupleImpl(tmp);
	}

	@Override
	public Tuple conreplace(Sequence[] con, int position, Sequence s)
			throws QueryException {
		int nLen = sequences.length + con.length;
		if ((position < 0) || (position >= nLen)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					position);
		}
		Sequence[] tmp = Arrays.copyOf(sequences, nLen);
		System.arraycopy(con, 0, tmp, sequences.length, con.length);
		tmp[position] = s;
		return new TupleImpl(tmp);
	}

	@Override
	public Sequence[] array() {
		return sequences;
	}

	@Override
	public Sequence get(int position) throws QueryException {
		if ((position < 0) || (position >= sequences.length)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					position);
		}

		return sequences[position];
	}

	@Override
	public int getSize() {
		return sequences.length;
	}

	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("[");
		for (int i = 0; i < sequences.length; i++) {
			if (i > 0)
				out.append(", ");

			out.append(sequences[i]);
		}
		out.append("]");
		return out.toString();
	}
}
