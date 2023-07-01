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
package org.brackit.xquery.node.dom;

import java.util.ArrayDeque;
import java.util.Deque;

import org.brackit.xquery.node.parser.DefaultListener;
import org.brackit.xquery.node.parser.NodeSubtreeListener;
import org.brackit.xquery.jdm.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Sebastian Baechle
 */
public class DOMListener extends DefaultListener<org.brackit.xquery.jdm.node.Node<?>> implements
    NodeSubtreeListener<org.brackit.xquery.jdm.node.Node<?>> {
  protected DocumentImpl document;

  protected final Deque<NodeImpl> stack;

  public DOMListener() {
    this.stack = new ArrayDeque<>();
  }

  @Override
  public void startDocument() throws DocumentException {
    if (document != null) {
      throw new DocumentException("Multiple documents are not supported");
    }

    document = new DocumentImpl();
    stack.clear();
  }

  public Document getDocument() {
    return document;
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void attribute(T node) throws DocumentException {
    Node current = stack.peekLast();
    assert current != null;
    ((Element) current).setAttributeNode(new AttrImpl(document,
                                                      current,
                                                      node.getName().stringValue(),
                                                      node.getValue().stringValue()));
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void startElement(T node) throws DocumentException {
    NodeImpl current = stack.peekLast();

    ElementImpl newChild = new ElementImpl(document, current, node.getName().stringValue(), null);

    if (current != null) {
      ((Element) current).appendChild(newChild);
    } else {
      document.setDocumentElement(newChild);
    }

    stack.addLast(newChild);
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void endElement(T node) throws DocumentException {
    stack.pop();
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void text(T node) throws DocumentException {
    insertText(node.getValue().stringValue());
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void comment(T node) throws DocumentException {
    insertComment(node.getValue().stringValue());
  }

  @Override
  public <T extends org.brackit.xquery.jdm.node.Node<?>> void processingInstruction(T node) throws DocumentException {
    insertProcessingInstruction(node.getValue().stringValue());
  }

  private void insertText(String text) throws DocumentException {
    NodeImpl current = stack.peekLast();
    assert current != null;
    current.appendChild(new TextImpl(document, current, null, text));
  }

  private void insertProcessingInstruction(String value) {
    NodeImpl current = stack.peekLast();
    assert current != null;
    current.appendChild(new ProcInstrImpl(document, current, null, value));
  }

  private void insertComment(String value) {
    NodeImpl current = stack.peekLast();
    assert current != null;
    current.appendChild(new CommentImpl(document, current, null, value));
  }
}