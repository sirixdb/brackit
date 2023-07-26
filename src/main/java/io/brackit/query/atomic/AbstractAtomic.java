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
package io.brackit.query.atomic;

import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.jdm.AbstractItem;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.ItemType;

/**
 * Base class for atomic items.
 *
 * @author Sebastian Baechle
 */
public abstract class AbstractAtomic extends AbstractItem implements Atomic, InternalAtomic {
  @Override
  public final Item evaluateToItem(QueryContext ctx, Tuple tuple) {
    return this;
  }

  @Override
  public final Atomic atomize() throws QueryException {
    return this;
  }

  @Override
  public abstract int hashCode();

  @Override
  public int compareTo(Atomic atomic) {
    if (this == atomic) {
      return 0;
    }
    int myCode = atomicCode();
    int oCode = atomic.atomicCode();
    if (myCode != oCode) {
      return myCode < oCode ? -1 : 1;
    }
    return atomicCmpInternal(atomic);
  }

  @Override
  public boolean eq(Atomic atomic) throws QueryException {
    // default implementation
    return cmp(atomic) == 0;
  }

  @Override
  public final int atomicCmp(Atomic atomic) {
    if (this == atomic) {
      return 0;
    }
    int myCode = atomicCode();
    int oCode = atomic.atomicCode();
    if (myCode != oCode) {
      return myCode < oCode ? -1 : 1;
    }
    return atomicCmpInternal(atomic);
  }

  @Override
  public final boolean equals(Object obj) {
    return obj == this || obj instanceof Atomic && atomicCmp((Atomic) obj) == 0;
  }

  @Override
  public String toString() {
    return stringValue();
  }

  @Override
  public Str asStr() {
    return new Str(stringValue());
  }

  @Override
  public Una asUna() {
    return new Una(stringValue());
  }

  @Override
  public ItemType itemType() throws QueryException {
    // TODO we should override this in built-in classes
    // to save object construction and memory overhead
    return new AtomicType(type());
  }
}
