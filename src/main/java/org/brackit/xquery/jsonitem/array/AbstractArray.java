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
package org.brackit.xquery.jsonitem.array;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.FlatteningSequence;
import org.brackit.xquery.jdm.AbstractItem;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.json.Array;
import org.brackit.xquery.jdm.type.ArrayType;
import org.brackit.xquery.jdm.type.ItemType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractArray extends AbstractItem implements Array {

  @Override
  public ItemType itemType() throws QueryException {
    return ArrayType.ARRAY;
  }

  @Override
  public Atomic atomize() throws QueryException {
    throw new QueryException(ErrorCode.ERR_ITEM_HAS_NO_TYPED_VALUE, "The atomized value of array items is undefined");
  }

  @Override
  public boolean booleanValue() throws QueryException {
    throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
        "Effective boolean value of array items is undefined.");
  }

  @Override
  public Iter iterate() {
    return new BaseIter() {
      private List<Sequence> sequences;
      private final Deque<Item> flatteningSequences = new ArrayDeque<>();

      private int index;

      @Override
      public Item next() {
        if (sequences == null) {
          sequences = values();
        } else if (!flatteningSequences.isEmpty()) {
          return flatteningSequences.removeFirst();
        }

        if (index < sequences.size()) {
          final var sequence = sequences.get(index++);
          if (sequence instanceof FlatteningSequence) {
            try (final var iter = sequence.iterate()) {
              Item item;
              while ((item = iter.next()) != null) {
                flatteningSequences.addLast(item);
              }
              return flatteningSequences.removeFirst();
            }
          }
          return (Item) sequence;
        }

        return null;
      }

      @Override
      public void close() {
      }

      @Override
      public Split split(int min, int max) throws QueryException {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }
}