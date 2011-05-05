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
import java.util.Comparator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.util.TupleUtil;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Sort implements Cursor {
	private final Cursor in;

	private final int[] sortPositions;

	private Tuple[] buffer;

	private int size;

	private int pos;

	public Sort(Cursor in) {
		super();
		this.in = in;
		this.sortPositions = null;
	}

	public Sort(Cursor in, int[] sortPositions) {
		super();
		this.in = in;
		this.sortPositions = sortPositions;
	}

	@Override
	public void close(QueryContext ctx) {
		in.close(ctx);
		pos = -1;
		size = 0;
		buffer = null;
	}

	@Override
	public void open(QueryContext ctx) throws QueryException {
		in.open(ctx);
		pos = -1;
		size = 0;
		buffer = new Tuple[100];
	}

	@Override
	public Tuple next(final QueryContext ctx) throws QueryException {
		if (size == 0) {
			fillAndSort(ctx);
		}

		return (pos++ < size) ? buffer[pos] : null;
	}

	private void fillAndSort(final QueryContext ctx) throws QueryException {
		Tuple t = null;
		while ((t = in.next(ctx)) != null) {
			if (size == buffer.length) {
				buffer = Arrays.copyOf(buffer, (buffer.length * 3) / 2 + 1);
			}
			buffer[size++] = t;
		}

		try {
			Arrays.sort(buffer, 0, size, new Comparator<Tuple>() {

				@Override
				public int compare(Tuple o1, Tuple o2) {
					try {
						int[] sortPos = sortPositions;

						if (sortPos == null) {
							sortPos = new int[o1.getSize()];
							for (int i = 0; i < sortPos.length; i++) {
								sortPos[i] = i;
							}
						}

						for (int position : sortPos) {
							int res = TupleUtil.compare(ctx, o1, o2, position,
									position);

							if (res != 0)
								return res;
						}

						return 0;
					} catch (QueryException e) {
						if (e.getCode() == ErrorCode.BIT_DYN_RT_ILLEGAL_COMPARISON_ERROR) {
							throw new ClassCastException(e.getMessage());
						} else {
							throw new RuntimeException(e.getMessage());
						}
					}
				}
			});
		} catch (ClassCastException e) {
			throw new QueryException(
					ErrorCode.BIT_DYN_RT_ILLEGAL_COMPARISON_ERROR);
		} catch (RuntimeException e) {
			throw new QueryException(ErrorCode.BIT_DYN_DOCUMENT_ACCESS_ERROR);
		}
	}
}
