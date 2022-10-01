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
package org.brackit.xquery.node;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.parser.SubtreeHandler;
import org.brackit.xquery.node.parser.SubtreeListener;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractBuilder<E extends Node<E>> implements SubtreeListener<E>, SubtreeHandler {
  private Node[] stack;

  private int stackSize;

  private E parent;

  private final E rootParent;

  private Map<String, String> nsMappings;

  @Override
  public void endMapping(String prefix) throws DocumentException {
    // ignore???
  }

  @Override
  public void startMapping(String prefix, String uri) throws DocumentException {
    if (nsMappings == null) {
      // use tree map for space-efficiency
      nsMappings = new TreeMap<>();
    }
    nsMappings.put(prefix, uri);
  }

  public AbstractBuilder(E parent) {
    this.stack = new Node[5];
    this.parent = parent;
    this.rootParent = parent;
    stack[stackSize++] = parent;
  }

  public AbstractBuilder() {
    this.stack = new Node[5];
    this.rootParent = null;
  }

  protected abstract E buildDocument() throws DocumentException;

  protected abstract E buildElement(E parent, QNm name, Map<String, String> nsMappings) throws DocumentException;

  protected abstract E buildAttribute(E parent, QNm name, Atomic value) throws DocumentException;

  protected abstract E buildText(E parent, Atomic text) throws DocumentException;

  protected abstract E buildComment(E parent, Atomic text) throws DocumentException;

  protected abstract E buildProcessingInstruction(E parent, QNm target, Atomic text) throws DocumentException;

  private void prepare() throws DocumentException {
    if (stackSize == 0 && rootParent != null) {
      throw new DocumentException("A root already exists");
    }
    if (stackSize == stack.length) {
      stack = Arrays.copyOf(stack, stackSize * 3 / 2 + 1);
    }
  }

  @SuppressWarnings("unchecked")
  public E root() throws DocumentException {
    E root = (E) (rootParent == null ? stack[0] : stack[1]);
    if (root == null) {
      throw new DocumentException("No root node has been build");
    }
    return root;
  }

  @Override
  public void begin() throws DocumentException {
  }

  @Override
  public void end() throws DocumentException {
  }

  @Override
  public void fail() throws DocumentException {
  }

  @Override
  public void beginFragment() throws DocumentException {
  }

  @Override
  public void endFragment() throws DocumentException {
  }

  @Override
  public void startDocument() throws DocumentException {
    prepare();
    parent = buildDocument();
    stack[stackSize++] = parent;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void endDocument() throws DocumentException {
    parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
  }

  @Override
  public <T extends E> void startElement(T node) throws DocumentException {
    prepare();
    parent = buildElement(parent, node.getName(), null);
    stack[stackSize++] = parent;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends E> void endElement(T node) throws DocumentException {
    parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
  }

  @Override
  public <T extends E> void attribute(T node) throws DocumentException {
    prepare();
    stack[stackSize] = buildAttribute(parent, node.getName(), node.getValue());
  }

  @Override
  public <T extends E> void text(T node) throws DocumentException {
    prepare();
    stack[stackSize] = buildText(parent, node.getValue().asUna());
  }

  @Override
  public <T extends E> void comment(T node) throws DocumentException {
    prepare();
    stack[stackSize] = buildComment(parent, node.getValue().asStr());
  }

  @Override
  public <T extends E> void processingInstruction(T node) throws DocumentException {
    prepare();
    stack[stackSize] = buildProcessingInstruction(parent, node.getName(), node.getValue().asStr());
  }

  @Override
  public void startElement(QNm name) throws DocumentException {
    prepare();
    parent = buildElement(parent, name, nsMappings);
    stack[stackSize++] = parent;
    nsMappings = null; // clear mappings
  }

  @SuppressWarnings("unchecked")
  @Override
  public void endElement(QNm name) throws DocumentException {
    parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
  }

  @Override
  public void attribute(QNm name, Atomic value) throws DocumentException {
    prepare();
    stack[stackSize] = buildAttribute(parent, name, value);
  }

  @Override
  public void text(Atomic content) throws DocumentException {
    prepare();
    stack[stackSize] = buildText(parent, content);
  }

  @Override
  public void comment(Atomic content) throws DocumentException {
    prepare();
    stack[stackSize] = buildComment(parent, content);
  }

  @Override
  public void processingInstruction(QNm target, Atomic content) throws DocumentException {
    prepare();
    stack[stackSize] = buildProcessingInstruction(parent, target, content);
  }
}
