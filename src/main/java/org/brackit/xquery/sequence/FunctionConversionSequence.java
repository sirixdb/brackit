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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Counter;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.expr.Cast;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.Type;
import org.brackit.xquery.jdm.type.AtomicType;
import org.brackit.xquery.jdm.type.Cardinality;
import org.brackit.xquery.jdm.type.ItemType;
import org.brackit.xquery.jdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class FunctionConversionSequence extends LazySequence {

  private static final Int32 TWO = Int32.ZERO_TO_TWENTY[2];

  private final class AtomicTypedIter extends BaseIter {
    final Cardinality card;
    final AtomicType iType;
    final Type expected;
    Counter pos = new Counter();
    Iter s;

    AtomicTypedIter(Cardinality card, AtomicType iType) {
      this.card = card;
      this.iType = iType;
      this.expected = iType.getType();
    }

    @Override
    public Item next() {
      if (s == null) {
        s = arg.iterate();
      }

      Item item = s.next();
      if (item == null) {
        if ((pos.cmp(Int32.ZERO) == 0) && (card.moreThanZero())) {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Invalid empty typed sequence (expected %s)",
                                   card);
        }
        safe = true; // remember that sequence type is OK
        return null;
      }

      pos.inc();
      if ((card == Cardinality.Zero) || ((pos.cmp(TWO) == 0) && (card.atMostOne()))) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Invalid cardinality of typed sequence (expected %s): >= %s",
                                 card,
                                 pos);
      }

      // See XQuery 3.1.5 Function Calls
      Atomic atomic = item.atomize();
      Type type = atomic.type();

      if ((type == Type.UNA) && (expected != Type.UNA)) {
        if ((builtin) && (expected.isNumeric())) {
          atomic = Cast.cast(null, atomic, Type.DBL, false);
        } else if ((expected.instanceOf(Type.QNM)) || (expected.instanceOf(Type.NOT))) {
          throw new QueryException(ErrorCode.ERR_TYPE_CAST_TO_NAMESPACE_SENSITIVE_TYPE,
                                   "Cannot cast %s to namespace-sensitive type %s",
                                   type,
                                   expected);
        } else {
          atomic = Cast.cast(null, atomic, expected, false);
        }
      } else if (!iType.matches(atomic)) {
        if ((expected.isNumeric()) && (type.isNumeric())) {
          atomic = Cast.cast(null, atomic, expected, false);
        } else if ((expected.instanceOf(Type.STR)) && (type.instanceOf(Type.AURI))) {
          atomic = Cast.cast(null, atomic, expected, false);
        } else {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Item of invalid atomic type in typed sequence (expected %s): %s",
                                   iType,
                                   atomic);
        }
      }

      return atomic;
    }

    @Override
    public void skip(IntNumeric i) {
      if (s == null) {
        s = arg.iterate();
      }
      s.skip(i);
    }

    @Override
    public void close() {
      if (s != null) {
        s.close();
      }
    }
  }

  private final class TypedIter extends BaseIter {
    final Cardinality card;
    final ItemType iType;
    Counter pos = new Counter();
    Iter s;

    TypedIter(Cardinality card, ItemType iType) {
      this.card = card;
      this.iType = iType;
    }

    @Override
    public Item next() {
      if (s == null) {
        s = arg.iterate();
      }

      Item item = s.next();
      if (item == null) {
        if ((pos.cmp(Int32.ZERO) == 0) && (card.moreThanZero())) {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Invalid empty typed sequence (expected %s)",
                                   card);
        }
        safe = true; // remember that sequence type is OK
        return null;
      }

      pos.inc();
      if ((card == Cardinality.Zero) || ((pos.cmp(TWO) == 0) && (card.atMostOne()))) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Invalid cardinality of typed sequence (expected %s): >= %s",
                                 card,
                                 pos);
      }
      if (!iType.matches(item)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Item of invalid type %s in typed sequence (expected %s): %s",
                                 item.itemType(),
                                 iType,
                                 item);
      }

      return item;
    }

    @Override
    public void skip(IntNumeric i) {
      if (s == null) {
        s = arg.iterate();
      }
      s.skip(i);
    }

    @Override
    public void close() {
      if (s != null) {
        s.close();
      }
    }
  }

  final SequenceType type;
  final Sequence arg;
  final boolean builtin;
  // volatile field because safe is evaluated lazy
  volatile boolean safe;

  public FunctionConversionSequence(SequenceType type, Sequence arg, boolean builtin) {
    this.type = type;
    this.arg = arg;
    this.builtin = builtin;
  }

  @Override
  public Iter iterate() {
    if (safe) {
      return arg.iterate();
    }
    Cardinality card = type.getCardinality();
    ItemType iType = type.getItemType();
    if (iType instanceof AtomicType) {
      return new AtomicTypedIter(card, (AtomicType) iType);
    } else {
      return new TypedIter(card, iType);
    }
  }

  @Override
  public Item get(IntNumeric pos) {
    Item item = arg.get(pos);
    if (safe) { // volatile read
      return item;
    }
    Cardinality card = type.getCardinality();
    if (item == null) {
      if (card.moreThanZero()) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Invalid empty typed sequence (expected %s)",
                                 card);
      }
    } else {
      if ((pos.cmp(Int32.ONE) > 0) && (card.atMostOne())) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Invalid cardinality of typed sequence (expected %s): >= %s",
                                 card,
                                 pos);
      }
      if ((pos.cmp(Int32.ZERO) > 0) && (card == Cardinality.Zero)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Invalid cardinality of typed sequence (expected %s): >= %s",
                                 card,
                                 pos);
      }
      if (!type.getItemType().matches(item)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Item of invalid type in typed sequence (expected %s): %s",
                                 type.getItemType(),
                                 item);
      }
    }
    return item;
  }

  /**
   * See XQuery 3.1.5 Function Calls (function conversion rules) and compare
   * with TypedSequence.
   */
  public static Sequence asTypedSequence(SequenceType sType, Sequence s, boolean builtin) {
    if (s == null) {
      if (sType.getCardinality().moreThanZero()) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Invalid empty-sequence()");
      }
      return null;
    } else if (s instanceof Item) {
      // short-circuit wrapping of single item parameter
      ItemType iType = sType.getItemType();
      if (iType instanceof AtomicType) {
        Atomic atomic = ((Item) s).atomize();
        Type expected = ((AtomicType) iType).getType();
        Type type = atomic.type();

        if ((type == Type.UNA) && (expected != Type.UNA)) {
          if ((builtin) && (expected.isNumeric())) {
            atomic = Cast.cast(null, atomic, expected, false);
          } else if ((expected.instanceOf(Type.QNM)) || (expected.instanceOf(Type.NOT))) {
            throw new QueryException(ErrorCode.ERR_TYPE_CAST_TO_NAMESPACE_SENSITIVE_TYPE,
                                     "Cannot cast %s to namespace-sensitive type %s",
                                     type,
                                     expected);
          } else {
            atomic = Cast.cast(null, atomic, expected, false);
          }
        } else if (!iType.matches(atomic)) {
          if ((expected.isNumeric()) && (type.isNumeric())) {
            atomic = Cast.cast(null, atomic, expected, false);
          } else if ((expected.instanceOf(Type.STR)) && (type.instanceOf(Type.AURI))) {
            atomic = Cast.cast(null, atomic, expected, false);
          } else {
            throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                     "Item of invalid atomic type in typed sequence (expected %s): %s",
                                     iType,
                                     atomic);
          }
        }

        return atomic;
      } else if (!iType.matches((Item) s)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                 "Item of invalid type in typed sequence (expected %s): %s",
                                 iType,
                                 s);
      }

      return s;
    } else {
      Sequence ts = new FunctionConversionSequence(sType, s, builtin);

      if (sType.getCardinality().atMostOne()) {
        try (Iter it = ts.iterate()) {
          return it.next();
        }
      }

      return ts;
    }
  }
}