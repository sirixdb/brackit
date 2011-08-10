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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;

/**
 * @author Sebastian Baechle
 *
 */
public class FlatteningSequence implements Sequence {

	protected final Sequence[] seqs;

	public FlatteningSequence(Sequence... seqs) {
		this.seqs = seqs;
	}

	@Override
	public boolean booleanValue(QueryContext ctx) throws QueryException {
		return (seqs.length > 0) ? (seqs[0] instanceof Node<?>) ? true
				: seqs[0].booleanValue(ctx) : false;
	}

	@Override
	public IntegerNumeric size(QueryContext ctx) throws QueryException {
		IntegerNumeric size = Int32.ZERO;
		for (Sequence s : seqs) {
			size = (IntegerNumeric) size.add(s.size(ctx));
		}
		return size;
	}

	@Override
	public Iter iterate() {
		return new FlatteningIter() {
			int pos = 0;
			@Override
			protected Sequence nextSequence() throws QueryException {
				return (pos < seqs.length) ? seqs[pos++] : null;
			}
		};
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (seqs.length > 0) {
			sb.append(seqs[0]);
			for (int i = 1; i < seqs.length; i++) {
				sb.append(",");
				sb.append(seqs[i]);
			}
		}
		return sb.toString();
	}
}
