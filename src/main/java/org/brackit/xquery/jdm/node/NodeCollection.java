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
package org.brackit.xquery.jdm.node;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.node.parser.NodeSubtreeParser;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.OperationNotSupportedException;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.StructuredItemCollection;

/**
 * @param <E>
 * @author Sebastian Baechle
 */
public interface NodeCollection<E extends Node<E>> extends StructuredItemCollection<Node<E>> {

  @Override
  public AnyURI getDocumentURI();

  @Override
  public String getName();

  @Override
  public void delete();

  @Override
  public void remove(long documentID) throws OperationNotSupportedException, DocumentException;

  @Override
  public E getDocument() throws DocumentException;

  @Override
  public Stream<? extends E> getDocuments() throws DocumentException;

  /**
   * Add to the collection.
   *
   * @param parser the subtree parser
   * @return the new root node added to the collection
   * @throws OperationNotSupportedException if the operation is not supported
   * @throws DocumentException              if anything else went wrong
   */
  public E add(NodeSubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  @Override
  public long getDocumentCount();
}
