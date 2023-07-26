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
package io.brackit.query.function.bit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.type.ArrayType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.StaticContext;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.sequence.BaseIter;
import io.brackit.query.sequence.FlatteningSequence;
import io.brackit.query.sequence.LazySequence;
import io.brackit.query.util.annotation.FunctionAnnotation;
import io.brackit.query.QueryContext;
import io.brackit.query.compiler.Bits;
import io.brackit.query.function.AbstractFunction;

/**
 * @author Sebastian Baechle
 */
@FunctionAnnotation(description = "Returns the values of the given array.", parameters = "$array")
public class ArrayValues extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, "array-values");

  public ArrayValues() {
    this(DEFAULT_NAME);
  }

  public ArrayValues(QNm name) {
    super(name,
          new Signature(SequenceType.ITEM_SEQUENCE, new SequenceType(ArrayType.ARRAY, Cardinality.ZeroOrOne)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) {
    final Array array = (Array) args[0];

    if (array == null)
      return null;

    return new LazySequence() {
      @Override
      public Iter iterate() {
        return new BaseIter() {
          private List<Sequence> sequences;
          private Deque<Item> flatteningSequences = new ArrayDeque<>();

          private int index;

          @Override
          public Item next() {
            if (sequences == null) {
              sequences = array.values();
            } else if (!flatteningSequences.isEmpty()) {
              return flatteningSequences.removeFirst();
            }

            if (index < sequences.size()) {
              final var sequence = sequences.get(index++);
              if (sequence instanceof FlatteningSequence) {
                final var iter = sequence.iterate();
                Item item;
                while ((item = iter.next()) != null) {
                  flatteningSequences.addLast(item.evaluateToItem(ctx, new TupleImpl()));
                }
                return flatteningSequences.removeFirst();
              }
              return (Item) sequence;
            }

            return null;
          }

          @Override
          public void close() {
          }
        };
      }
    };
  }
}
