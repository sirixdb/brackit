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
package org.brackit.query.util.join;

import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.util.join.AbstractJoinTable.TValue;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Iter;
import org.brackit.query.jdm.Sequence;

/**
 * @author Sebastian Baechle
 */
public class SingleTypeJoinTable {
  protected final QueryContext ctx;

  protected final AbstractJoinTable table;

  public SingleTypeJoinTable(QueryContext ctx, AbstractJoinTable table) {
    this.ctx = ctx;
    this.table = table;
  }

  protected final FastList<Sequence[]> sortAndDeduplicate(FastList<TValue> in) throws QueryException {
    in.sort();
    FastList<Sequence[]> out = new FastList<Sequence[]>();
    TValue p = null;
    int inSize = in.getSize();
    for (int i = 0; i < inSize; i++) {
      TValue v = in.get(i);
      if ((p == null) || (p.pos < v.pos)) {
        out.add(v.bindings);
      }
      p = v;
    }
    return out;
  }

  public final void add(Sequence keys, Sequence[] bindings, int pos) throws QueryException {
    if (keys instanceof Item) {
      table.add(((Item) keys).atomize(), pos, bindings);
    } else {
      try (final Iter it = keys.iterate()) {
        Item key;
        while ((key = it.next()) != null) {
          table.add(key.atomize(), pos, bindings);
        }
      }
    }
  }

  public final FastList<Sequence[]> probe(Sequence keys) throws QueryException {
    if (keys == null) {
      return FastList.emptyList();
    }

    FastList<TValue> matches = new FastList<TValue>();

    if (keys instanceof Item) {
      table.lookup(matches, ((Item) keys).atomize());
    } else {
      try (final Iter it = keys.iterate()) {
        Item key;
        while ((key = it.next()) != null) {
          table.lookup(matches, key.atomize());
        }
      }
    }

    if (matches.isEmpty()) {
      return FastList.emptyList();
    }

    return sortAndDeduplicate(matches);
  }
}
