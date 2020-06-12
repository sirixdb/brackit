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
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public abstract class LazySequence extends AbstractSequence {
  // use volatile fields because
  // they are computed on demand
  private volatile IntNumeric size;
  private volatile Boolean bool;

  @Override
  public final boolean booleanValue() {
    Boolean b = bool; // volatile read
    if (b != null) {
      return b;
    }
    try (Iter s = iterate()) {
      Item n = s.next();
      if (n == null) {
        return (bool = false);
      }
      if (n instanceof Node<?>) {
        return (bool = true);
      }
      if (s.next() != null) {
        throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                                 "Effective boolean value is undefined " + "for sequences with two or more items "
                                     + "not starting with a node");
      }
      return (bool = n.booleanValue());

    }
  }

  @Override
  public final IntNumeric size() {
    IntNumeric si = size; // volatile read
    if (si != null) {
      return si;
    }
    final Counter count = new Counter();
    try (Iter s = iterate()) {
      while (s.next() != null) {
        count.inc();
      }
    }
    return (size = count.asIntNumeric());
  }

  @Override
  public Item get(IntNumeric pos) {
    IntNumeric si = size; // volatile read
    if ((si != null) && (si.cmp(pos) < 0)) {
      return null;
    }
    if (Int32.ZERO.cmp(pos) >= 0) {
      return null;
    }
    final Counter count = new Counter();
    try (Iter it = iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        if (count.inc().cmp(pos) == 0) {
          return item;
        }
      }
    }
    if (si == null) {
      size = count.asIntNumeric(); // remember size
    }
    return null;
  }
}
