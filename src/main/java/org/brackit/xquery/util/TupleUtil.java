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
package org.brackit.xquery.util;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class TupleUtil {

	public static final int compareAtomics(QueryContext ctx, Tuple left,
			Tuple right, int lPos, int rPos) throws QueryException {
		Atomic lAtomic = (Atomic) left.get(lPos);
		Atomic rAtomic = (Atomic) right.get(rPos);
		
		if (lAtomic == null) {
			return (rAtomic == null) ? 0 : -1;
		} else if (rAtomic == null) {
			return 1;
		}

		return lAtomic.cmp(rAtomic);
	}

	public static final int compareItems(QueryContext ctx, Tuple left,
			Tuple right, int lPos, int rPos) throws QueryException {
		Sequence rightSequence = (rPos == -1) ? right.get(0) : right.get(rPos);
		Sequence leftSequence = (lPos == -1) ? left.get(0) : left.get(lPos);

		if (!(leftSequence instanceof Item)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}

		if (!(rightSequence instanceof Item)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}

		if (leftSequence == null) {
			return (rightSequence == null) ? 0 : -1;
		} else if (rightSequence == null) {
			return 1;
		}

		return ((Item) leftSequence).atomize().cmp(
				((Item) rightSequence).atomize());
	}

	public static final int compare(QueryContext ctx, Tuple left, Tuple right,
			int lPos, int rPos) throws QueryException {
		Sequence rightSequence = (rPos == -1) ? right.get(0) : right.get(rPos);
		Sequence leftSequence = (lPos == -1) ? left.get(0) : left.get(lPos);

		if (leftSequence == null) {
			return (rightSequence == null) ? 0 : -1;
		} else if (rightSequence == null) {
			return 1;
		}

		if (leftSequence instanceof Item) {
			Atomic leftAtomic = ((Item) leftSequence).atomize();

			if (leftAtomic instanceof Una) {
				leftAtomic = new Str(((Una) leftAtomic).str);
			}

			if (rightSequence instanceof Item) {
				if (rightSequence == null) {
					return 1;
				}

				Atomic rightAtomic = ((Item) rightSequence).atomize();

				if (rightAtomic instanceof Una) {
					rightAtomic = new Str(((Una) rightAtomic).str);
				}

				return leftAtomic.cmp(rightAtomic);
			}

			Iter rStream = rightSequence.iterate();
			try {
				Item next = rStream.next();
				if (next == null) {
					return 1;
				}

				Atomic rightAtomic = next.atomize();

				if (rightAtomic instanceof Una) {
					rightAtomic = new Str(((Una) rightAtomic).str);
				}

				int res = leftAtomic.cmp(rightAtomic);
				return (res != 0) ? res : (rStream.next() != null) ? -1 : 0;
			} finally {
				rStream.close();
			}
		} else if (rightSequence instanceof Item) {
			Atomic rightAtomic = ((Item) rightSequence).atomize();

			if (rightAtomic instanceof Una) {
				rightAtomic = new Str(((Una) rightAtomic).str);
			}

			Iter lStream = leftSequence.iterate();
			try {
				Item next = lStream.next();
				if (next == null) {
					return -1;
				}

				Atomic leftAtomic = next.atomize();

				if (leftAtomic instanceof Una) {
					leftAtomic = new Str(((Una) leftAtomic).str);
				}

				int res = leftAtomic.cmp(rightAtomic);
				return (res != 0) ? res : (lStream.next() != null) ? 1 : 0;
			} finally {
				lStream.close();
			}
		} else {
			Iter lStream = leftSequence.iterate();
			;
			Iter rStream = null;
			Item lOK;
			Item rOK;

			try {
				rStream = rightSequence.iterate();
				lOK = lStream.next();
				rOK = rStream.next();

				while ((lOK != null) && (rOK != null)) {
					Atomic leftAtomic = lOK.atomize();
					Atomic rightAtomic = rOK.atomize();

					if (leftAtomic instanceof Una) {
						leftAtomic = new Str(((Una) leftAtomic).str);
					}
					if (rightAtomic instanceof Una) {
						rightAtomic = new Str(((Una) rightAtomic).str);
					}

					int result = leftAtomic.cmp(rightAtomic);

					if (result != 0) {
						return result;
					}

					lOK = lStream.next();
					rOK = rStream.next();
				}

				return ((lOK != null) ^ (rOK != null)) ? (lOK != null) ? 1 : -1
						: 0;
			} finally {
				lStream.close();
				if (rStream != null)
					rStream.close();
			}
		}
	}
}