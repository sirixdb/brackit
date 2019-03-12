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
package org.brackit.xquery.sequence;

import java.util.Comparator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.util.sort.TupleSort;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class SortedNodeSequence extends LazySequence {
	private final Comparator<Tuple> cmp;
	private final Sequence in;	
	private final boolean dedup;

	public SortedNodeSequence(Comparator<Tuple> cmp, Sequence in, boolean dedup) {
		this.cmp = cmp;
		this.in = in;
		this.dedup = dedup;
	}

	@Override
	public Iter iterate() {
		return new BaseIter() {
			// TODO -1 means no external sort
			final TupleSort sort = new TupleSort(cmp, -1);
			final Sequence source = in;

			Stream<? extends Tuple> sorted;
			Node<?> p;
			Node<?> n;

			@Override
			public Item next() throws QueryException {
				if (sorted == null) {
					loadAndSort();
					sorted = sort.stream();
					n = (Node<?>) sorted.next();
				}

				while (n != null) {
					if ((dedup) && (p != null) && (p.cmp(n) == 0)) {
						n = (Node<?>) sorted.next();
					}
					Node<?> deliver = n;
					p = n;
					n = (Node<?>) sorted.next();
					return deliver;
				}
				return null;
			}

			@Override
			public void close() {
				if (sorted != null) {
					sorted.close();
				}
			}

			private void loadAndSort() throws QueryException {
				loadSorter(source, sort);
			}

			private void loadSorter(Sequence sequence, final TupleSort sort)
					throws QueryException {
				if (sequence instanceof Item) {
					if (!(sequence instanceof Node<?>)) {
						throw new QueryException(
								ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
								"Illegal atomic item found in sequence: '%s'",
								sequence);
					}
					sort.add((Item) sequence);
				} else {
					Item item;
					Iter it = sequence.iterate();
					try {
						while ((item = it.next()) != null) {
							if (!(item instanceof Node<?>)) {
								throw new QueryException(
										ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
										"Illegal atomic item found in sequence: '%s'",
										item);
							}
							sort.add(item);
						}
					} finally {
						it.close();
					}
				}
				sort.sort();
			}
		};
	}
}
