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
/**
 * 
 */
package org.brackit.xquery.util;

import java.util.Comparator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;

public class TupleComparator implements Comparator<Tuple> {
	private final QueryContext ctx;

	private final int[] orderBySpec;

	public TupleComparator(QueryContext ctx, int[] orderBySpec) {
		this.ctx = ctx;
		this.orderBySpec = orderBySpec;
	}

	@Override
	public int compare(Tuple o1, Tuple o2) {
		try {
			for (int orderPos : orderBySpec) {
				// remember we must not care about pos 0 because this is the
				// runVar item itself
				int position = (orderPos > 0) ? orderPos : -orderPos;
				int res = TupleUtil.compare(ctx, o1, o2, position, position);

				if (res != 0)
					return (orderPos > 0) ? res : -1;
			}

			return 0;
		} catch (QueryException e) {
			if (e.getCode() == ErrorCode.BIT_DYN_RT_ILLEGAL_COMPARISON_ERROR) {
				throw new ClassCastException(e.getMessage());
			} else {
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}
		}
	}
}