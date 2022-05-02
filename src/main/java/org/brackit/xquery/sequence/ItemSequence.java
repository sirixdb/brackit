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
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class ItemSequence extends AbstractSequence {
  protected final Item[] items;

  public ItemSequence(Item... items) {
    this.items = items;
  }

  @Override
  public boolean booleanValue() {
    if (items.length == 0) {
      return false;
    }
    if (items[0] instanceof Node<?>) {
      return true;
    }
    if (items.length > 1) {
      throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
          "Effective boolean value is undefined " + "for sequences with two or more items "
              + "not starting with a node");
    }
    return items[0].booleanValue();
  }

  @Override
  public IntNumeric size() {
    return new Int32(items.length);
  }

  @Override
  public Item get(IntNumeric pos) {
    if ((Int32.ZERO.cmp(pos) >= 0) || (size().cmp(pos) < 0)) {
      return null;
    }
    return items[pos.intValue() - 1];
  }

  @Override
  public Iter iterate() {
    return new BaseIter() {
      int pos = 0;

      @Override
      public Item next() {
        return (pos < items.length) ? items[pos++] : null;
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

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (items.length > 0) {
      sb.append(items[0]);
      for (int i = 1; i < items.length; i++) {
        sb.append(",");
        sb.append(items[i]);
      }
    }
    return sb.toString();
  }
}