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
package org.brackit.xquery.util.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class HashJoinTable extends JoinTable {
	private final HashMap<TKey, TValue> table = new HashMap<TKey, TValue>();

	protected void add(Atomic key, int pos, Sequence[] bindings)
			throws QueryException {
		TKey htKey = new TKey(key);
		TValue htValue = new TValue(bindings, pos);

		TValue chain = table.get(htKey);

		if (chain == null) {
			table.put(htKey, htValue);
		} else {
			TValue p = null;
			while (chain != null) {
				if (chain.pos == pos) {
					return;
				}
				p = chain;
				chain = chain.next;
			}
			p.next = htValue;
		}
	}

	protected void lookup(FastList<TValue> matches, Atomic key)
			throws QueryException {
		TKey htKey = new TKey(key);
		TValue htValue = table.get(htKey);

		if (htValue != null) {
			// System.out.print("Matches for " + htKey + ":");
			while (htValue != null) {
				// System.out.print(" " + htKey);
				matches.add(htValue);
				htValue = htValue.next;
			}
			// System.out.println();
		}
	}

	@Override
	protected List<TEntry> entries() {
		ArrayList<TEntry> entries = new ArrayList<TEntry>();
		for (Map.Entry<TKey, TValue> entry : table.entrySet()) {
			for (TValue v = entry.getValue(); v != null; v = v.next) {
				entries.add(new TEntry(entry.getKey(), v));
			}
		}
		return entries;
	}
}
