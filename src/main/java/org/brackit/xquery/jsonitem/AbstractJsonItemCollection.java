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
package org.brackit.xquery.jsonitem;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.sequence.BaseIter;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.json.JsonCollection;
import org.brackit.xquery.jdm.json.JsonItem;

/**
 * @param <E>
 * @author Sebastian Baechle
 */
public abstract class AbstractJsonItemCollection<E extends JsonItem> extends LazySequence implements JsonCollection<E> {
  protected String name;

  public AbstractJsonItemCollection(String name) {
    this.name = name;
  }

  protected AbstractJsonItemCollection(AbstractJsonItemCollection<E> collection) {
    this.name = collection.name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public AnyURI getDocumentURI() {
    return AnyURI.fromString(name);
  }

  @Override
  public Iter iterate() {
    return new BaseIter() {
      Stream<? extends JsonItem> docs;

      @Override
      public void close() {
        if (docs != null) {
          docs.close();
        }
      }

      @Override
      public Item next() throws QueryException {
        if (docs == null) {
          docs = getDocuments();
        }
        return docs.next();
      }

      @Override
      public Split split(int min, int max) throws QueryException {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }
}
