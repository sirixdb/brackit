/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
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

	public static int creations;

	private final Sequence[] tuples;

	public TupleImpl() {
		tuples = new Sequence[0];
		creations++;
	}

	public TupleImpl(Sequence t) {
		tuples = new Sequence[] { t };
		creations++;
	}

	public TupleImpl(Sequence[] t) {
		tuples = Arrays.copyOf(t, t.length);
		creations++;
	}

	public TupleImpl(Tuple leftTuple, Sequence item, Sequence pos, Sequence size)
			throws QueryException {
		Sequence[] left = leftTuple.array();
		int length = left.length;
		tuples = new Sequence[length + ((item != null) ? 1 : 0)
				+ ((pos != null) ? 1 : 0) + ((size != null) ? 1 : 0)];
		if (length > 0) {
			System.arraycopy(left, 0, tuples, 0, length);
		}
		if (item != null) {
			tuples[length++] = item;
		}
		if (pos != null) {
			tuples[length++] = pos;
		}
		if (size != null) {
			tuples[length++] = size;
		}
		creations++;
	}

	public TupleImpl(Tuple leftTuple, Sequence[] right) throws QueryException {
		Sequence[] left = leftTuple.array();
		tuples = new Sequence[left.length + right.length];

		if (left.length > 0) {
			System.arraycopy(left, 0, tuples, 0, left.length);
		}

		if (right.length > 0) {
			System.arraycopy(right, 0, tuples, left.length, right.length);
		}
		creations++;
	}

	public TupleImpl(Sequence[] s0, Sequence[] s1, Sequence[] s2)
			throws QueryException {
		tuples = new Sequence[s0.length + s1.length + s2.length];

		if (s0.length > 0) {
			System.arraycopy(s0, 0, tuples, 0, s0.length);
		}

		if (s1.length > 0) {
			System.arraycopy(s1, 0, tuples, s0.length, s1.length);
		}

		if (s2.length > 0) {
			System.arraycopy(s2, 0, tuples, s0.length + s1.length, s2.length);
		}
		creations++;
	}

	public TupleImpl(Sequence left, Sequence right) {
		tuples = new Sequence[] { left, right };
		creations++;
	}

	public TupleImpl(Tuple left, Sequence right) throws QueryException {
		int leftSize = (left != null) ? left.getSize() : 0;
		tuples = new Sequence[leftSize + 1];

		if (leftSize > 0) {
			System.arraycopy(left.array(), 0, tuples, 0, leftSize);
		}
		tuples[leftSize] = right;
		creations++;
	}

	public TupleImpl(Tuple left, Tuple right) throws QueryException {
		int leftSize = (left != null) ? left.getSize() : 0;
		int rightSize = (right != null) ? right.getSize() : 0;
		tuples = new Sequence[leftSize + rightSize];

		if (leftSize > 0) {
			System.arraycopy(left.array(), 0, tuples, 0, leftSize);
		}

		if (rightSize > 0) {
			System.arraycopy(right.array(), 0, tuples, leftSize, rightSize);
		}
		creations++;
	}

	public TupleImpl(Tuple left, Tuple right, int[] positions)
			throws QueryException {
		tuples = new Sequence[positions.length];
		int leftSize = left.getSize();

		for (int i = 0; i < tuples.length; i++) {
			int position = positions[i];
			tuples[i] = (position < leftSize) ? left.get(position) : right
					.get(position - leftSize);
		}
	}

	public TupleImpl(Tuple left, Tuple right, int[] leftPositions,
			int[] rightPositions) throws QueryException {
		tuples = new Sequence[leftPositions.length + rightPositions.length];

		for (int i = 0; i < leftPositions.length; i++) {
			tuples[i] = left.get(i);
		}

		for (int i = 0; i < rightPositions.length; i++) {
			tuples[leftPositions.length + i] = right.get(i);
		}
		creations++;
	}

	@Override
	public Tuple choose(int... positions) throws QueryException {
		Sequence[] projected = new Sequence[positions.length];
		int targetPos = 0;
		for (int pos : positions) {
			projected[targetPos++] = get(pos);
		}
		return new TupleImpl(projected);
	}

	@Override
	public Sequence[] array() {
		return tuples;
	}

	@Override
	public Sequence get(int position) throws QueryException {
		if ((position < 0) || (position >= tuples.length)) {
			throw new QueryException(ErrorCode.BIT_DYN_RT_OUT_OF_BOUNDS_ERROR,
					position);
		}

		return tuples[position];
	}

	@Override
	public int getSize() {
		return tuples.length;
	}

	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("[");
		for (int i = 0; i < tuples.length; i++) {
			if (i > 0)
				out.append(", ");

			out.append(tuples[i]);
		}
		out.append("]");
		return out.toString();
	}
}
