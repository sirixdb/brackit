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
package io.brackit.query.node.d2linked;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class DocumentD2Node extends ParentD2Node {

  private final D2NodeCollection collection;

  public DocumentD2Node(String name) {
    super(null, FIRST);
    this.collection = new D2NodeCollection(name, this);
  }

  public DocumentD2Node(D2NodeCollection collection) {
    super(null, FIRST);
    this.collection = collection;
    collection.add(this);
  }

  public DocumentD2Node() {
    super(null, FIRST);
    this.collection = new D2NodeCollection(String.format("%s_%s_%s.xml",
                                                         Thread.currentThread().getName(),
                                                         "noname",
                                                         System.currentTimeMillis()), this);
  }

  @Override
  public D2NodeCollection getCollection() {
    return collection;
  }

  @Override
  public QNm getName() throws DocumentException {
    return null;
  }

  @Override
  public boolean isDocumentOf(Node<?> node) {
    return getKind() == Kind.DOCUMENT && node == this;
  }

  @Override
  public Kind getKind() {
    return Kind.DOCUMENT;
  }

  @Override
  public Stream<? extends D2Node> getDescendantOrSelf() throws DocumentException {
    return new DescendantScanner(this);
  }

  @Override
  public String toString() {
    return String.format("(type='%s', name='%s', value='%s')", Kind.DOCUMENT, collection.getName(), null);
  }
}