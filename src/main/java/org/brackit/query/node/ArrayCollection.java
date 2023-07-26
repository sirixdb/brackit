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
package org.brackit.query.node;

import java.util.Arrays;

import org.brackit.query.node.parser.NodeSubtreeParser;
import org.brackit.query.node.stream.ArrayStream;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.OperationNotSupportedException;
import org.brackit.query.jdm.Stream;
import org.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public class ArrayCollection<E extends Node<E>> extends AbstractNodeCollection<E> {
  protected Node[] docs;

  public ArrayCollection(String name, E doc) {
    super(name);
    this.docs = new Node[] { doc };
  }

  @SafeVarargs
  public ArrayCollection(String name, E... docs) {
    super(name);
    this.docs = docs;
  }

  @SuppressWarnings("unchecked")
  @Override
  public E getDocument() {
    if (docs.length == 1) {
      return (E) docs[0];
    }
    throw new DocumentException("Illegal access to non-singular collection");
  }

  @SuppressWarnings("unchecked")
  @Override
  public Stream<? extends E> getDocuments() {
    return new ArrayStream(docs);
  }

  @Override
  public E add(NodeSubtreeParser parser) {
    throw new OperationNotSupportedException();
  }

  public void add(Node<? super E> doc) {
    this.docs = Arrays.copyOf(docs, docs.length + 1);
    this.docs[docs.length - 1] = doc;
  }

  @Override
  public void delete() {
    throw new OperationNotSupportedException();
  }

  @Override
  public void remove(long documentID) {
    throw new OperationNotSupportedException();
  }

  @Override
  public long getDocumentCount() {
    return docs.length;
  }

}
