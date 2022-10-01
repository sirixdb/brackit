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
import org.brackit.xquery.atomic.Counter;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.ItemType;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class TypedSequence extends LazySequence {

  private static final Int32 TWO = Int32.ZERO_TWO_TWENTY[2];

  private final class TypedIter extends BaseIter {
    Cardinality card = type.getCardinality();
    ItemType iType = type.getItemType();
    Counter pos = new Counter();
    Iter s;

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
            "Item of invalid type in typed sequence (expected %s): %s",
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
  // volatile field because safe is evaluated lazy
  volatile boolean safe;

  public TypedSequence(SequenceType type, Sequence arg) {
    this.type = type;
    this.arg = arg;
  }

  @Override
  public Iter iterate() {
    if (safe) {
      return arg.iterate();
    }
    return new TypedIter();
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

  public static Sequence toTypedSequence(SequenceType sType, Sequence s) {
    if (s == null) {
      if (sType.getCardinality().moreThanZero()) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Invalid empty-sequence()");
      }
      return null;
    } else if (s instanceof Item) {
      // short-circuit wrapping of single item parameter
      ItemType itemType = sType.getItemType();

      if (!itemType.matches((Item) s)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
            "Item of invalid type %s in typed sequence (expected %s): %s",
            ((Item) s).itemType(),
            itemType,
            s);
      }

      return s;
    } else {
      return new TypedSequence(sType, s);
    }
  }

  public static Item toTypedItem(SequenceType sType, Item item) {
    if (item == null) {
      if (sType.getCardinality().moreThanZero()) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Invalid empty-sequence()");
      }
      return null;
    } else {
      // short-circuit wrapping of single item parameter
      ItemType itemType = sType.getItemType();

      if (!itemType.matches(item)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
            "Item of invalid type %s in typed sequence (expected %s): %s",
            item.itemType(),
            itemType,
            item);
      }

      return item;
    }
  }

  public static Item toTypedItem(SequenceType sType, Sequence s) {
    if (s == null) {
      if (sType.getCardinality().moreThanZero()) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Invalid empty-sequence()");
      }
      return null;
    } else if (s instanceof Item item) {
      // short-circuit wrapping of single item parameter
      ItemType itemType = sType.getItemType();

      if (!itemType.matches(item)) {
        throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
            "Item of invalid type %s in typed sequence (expected %s): %s",
            item.itemType(),
            itemType,
            item);
      }

      return item;
    } else {
      try (Iter it = s.iterate()) {
        Item item = it.next();
        if (it.next() != null) {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
              "Cannot convert %s typed sequence %s to single item",
              sType,
              s);
        }
        return toTypedItem(sType, item);
      }
    }
  }
}