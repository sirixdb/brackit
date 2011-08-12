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
package org.brackit.xquery.sequence;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Counter;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 * 
 */
public abstract class FlatteningSequence implements Sequence {

	private class FlatteningIter implements Iter {
		private Sequence s;
		private Iter it;
		private int pos;

		@Override
		public Item next() throws QueryException {
			while (true) {
				if (it != null) {
					Item res = it.next();
					if (res != null) {
						return res;
					}
					it.close();
					it = null;
				}

				s = sequence(pos++);

				if (s == null) {
					return null;
				}
				if (s instanceof Item) {
					// include single item in result
					return (Item) s;
				}
				// flatten out result
				it = s.iterate();
			}
		}

		@Override
		public void skip(IntNumeric i) throws QueryException {
			if (i.cmp(Int32.ZERO) <= 0) {
				return;
			}
			final Counter skipped = new Counter();
			// iterate current sequence to skip items
			if (it != null) {
				while (next() != null) {
					if (skipped.inc().cmp(i) >= 0) {
						return;
					}
				}
				it.close();
				it = null;
			}
			// skip over and in following sequences
			Numeric remaining = i.subtract(skipped.asIntNumeric());
			while ((s = sequence(pos++)) != null) {
				IntNumeric size = s.size();
				if (remaining.cmp(size) < 0) {
					it = s.iterate();
					it.skip((IntNumeric) remaining);
				}
				remaining = i.subtract(size);
			}
		}

		@Override
		public void close() {
			if (it != null) {
				it.close();
			}
		}
	}

	// use volatile fields because
	// they are computed on demand
	private volatile IntNumeric size;
	private volatile Boolean bool;

	/**
	 * Get next sequence to flatten out. If <code>null</code> is returned, the
	 * flattening stops.
	 */
	protected abstract Sequence sequence(int pos) throws QueryException;

	@Override
	public boolean booleanValue() throws QueryException {
		Boolean b = bool; // volatile read
		if (b != null) {
			return b;
		}
		Sequence s;
		int i = 0;
		while ((s = sequence(i++)) != null) {
			Item first;
			if (s == null) {
				continue;
			}
			if (s instanceof Item) {
				if (s instanceof Node<?>) {
					bool = Boolean.TRUE;
					return true;
				} else {
					first = (Item) s;
				}
			} else {
				Iter it = s.iterate();
				try {
					first = it.next();
					if (first == null) {
						continue;
					}
					if (first instanceof Node<?>) {
						this.bool = Boolean.TRUE;
						return true;
					}
					// assure that current subsequence
					// is a singleton
					if (it.next() != null) {
						throw new QueryException(
								ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
								"Effective boolean value is undefined "
										+ "for sequences with two or more items "
										+ "not starting with a node");
					}
				} finally {
					it.close();
				}
			}
			// ensure that following sequences are empty
			while ((s = sequence(i++)) != null) {
				if ((s != null) && (!s.size().eq(Int32.ZERO))) {
					throw new QueryException(
							ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
							"Effective boolean value is undefined "
									+ "for sequences with two or more items "
									+ "not starting with a node");
				}
			}
			return (bool = first.booleanValue());
		}
		return (bool = Boolean.FALSE);
	}

	@Override
	public IntNumeric size() throws QueryException {
		IntNumeric si = size; // volatile read
		if (si != null) {
			return si;
		}
		si = Int32.ZERO;
		Sequence s;
		int i = 0;
		while ((s = sequence(i++)) != null) {
			si = (IntNumeric) si.add(s.size());
		}
		return (size = si);
	}

	@Override
	public Item get(IntNumeric pos) throws QueryException {
		IntNumeric si = size; // volatile read
		if ((si != null) && (si.cmp(pos) < 0)) {
			return null;
		}
		if (Int32.ZERO.cmp(pos) >= 0) {
			return null;
		}
		IntNumeric psi = Int32.ZERO;
		si = Int32.ZERO;
		Sequence s;
		int i = 0;
		while ((s = sequence(i++)) != null) {
			psi = si;
			si = (IntNumeric) si.add(s.size());
			if (si.cmp(pos) >= 0) {
				return s.get((IntNumeric) pos.subtract(psi));
			}
		}
		size = si; // remember size
		return null;
	}

	@Override
	public Iter iterate() {
		return new FlatteningIter();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			Sequence s;
			int i = 0;
			if ((s = sequence(i++)) != null) {
				sb.append(s);
				while ((s = sequence(i++)) != null) {
					sb.append(",");
					sb.append(s);
				}
			}
		} catch (QueryException e) {
			sb.append("...");
		}
		return sb.toString();
	}
}
