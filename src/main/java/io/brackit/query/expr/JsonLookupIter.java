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
package io.brackit.query.expr;

import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.sequence.BaseIter;

/** Shared reader ownership for lazy array and object lookups. */
abstract class JsonLookupIter extends BaseIter {
  protected final Iter iter;
  protected Iter nestedIter;
  private boolean closed;

  JsonLookupIter(Iter iter) {
    this.iter = iter;
  }

  @Override
  public final Item next() {
    if (closed) {
      return null;
    }
    boolean returned = false;
    try {
      Item item = nextItem();
      returned = item != null;
      return item;
    } finally {
      if (!returned) {
        close();
      }
    }
  }

  protected abstract Item nextItem();

  protected final Item nextNested() {
    if (nestedIter == null) {
      return null;
    }
    Item item = nestedIter.next();
    if (item == null) {
      try {
        nestedIter.close();
      } finally {
        nestedIter = null;
      }
    }
    return item;
  }

  @Override
  public final void close() {
    if (!closed) {
      closed = true;
      try {
        if (nestedIter != null) {
          nestedIter.close();
        }
      } finally {
        iter.close();
      }
    }
  }
}
