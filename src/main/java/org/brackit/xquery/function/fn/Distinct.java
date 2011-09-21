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
package org.brackit.xquery.function.fn;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Distinct extends AbstractFunction {
	public Distinct(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, final QueryContext ctx, Sequence[] args)
			throws QueryException {
		if (args.length == 2) {
			Str collation = (Str) args[1];

			if (!collation.str
					.equals("http://www.w3.org/2005/xpath-functions/collation/codepoint")) {
				throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION,
						"Unsupported collation: %s", collation);
			}
		}

		final Sequence s = args[0];

		if ((s == null) || (s instanceof Item)) {
			return s;
		}

		return new LazySequence() {
			final Sequence inSeq = s;
			// volatile because it is computed
			// on demand
			volatile Set<Atomic> set;

			@Override
			public Iter iterate() {
				return new BaseIter() {
					Iterator<Atomic> it;

					@Override
					public Item next() throws QueryException {
						Set<Atomic> distinct = set; // volatile read
						if (distinct == null) {
							distinct = new LinkedHashSet<Atomic>();
							Iter it = inSeq.iterate();
							try {
								Item runVar;
								while ((runVar = it.next()) != null) {
									distinct.add((Atomic) runVar);
								}
							} finally {
								it.close();
							}
							set = distinct;
						}
						if (it == null) {
							it = distinct.iterator();
						}

						return (it.hasNext()) ? it.next() : null;
					}

					@Override
					public void close() {
						it = null;
					}
				};
			}
		};
	}
}
