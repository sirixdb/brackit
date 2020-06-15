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
package org.brackit.xquery.node.parser;

import java.util.ArrayDeque;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.d2linked.D2NodeFactory;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.node.Node;

/**
 * Utility that provides a convenient way to build in-memory document fragments.
 *
 * @author Sebastian Baechle
 */
public class FragmentHelper {
  private final ArrayDeque<Node<?>> stack;

  private Node<?> root;

  public FragmentHelper() {
    this.stack = new ArrayDeque<>();
  }

  public Node<?> getRoot() {
    return root;
  }

  public FragmentHelper openElement(String name) throws DocumentException {
    return openElement(new QNm(name));
  }

  public FragmentHelper openElement(QNm name) throws DocumentException {
    if (!stack.isEmpty()) {
      stack.push(stack.peek().append(Kind.ELEMENT, name, null));
    } else {
      root = (new D2NodeFactory()).element(name);
      stack.push(root);
    }

    return this;
  }

  public FragmentHelper closeElement() throws DocumentException {
    if (stack.isEmpty()) {
      throw new DocumentException("No element opened");
    }

    stack.pop();
    return this;
  }

  public FragmentHelper element(String name) throws DocumentException {
    return element(new QNm(name));
  }

  public FragmentHelper element(QNm name) throws DocumentException {
    openElement(name);
    closeElement();
    return this;
  }

  public FragmentHelper attribute(String name, String value) throws DocumentException {
    return attribute(new QNm(name), new Una(value));
  }

  public FragmentHelper attribute(QNm name, Atomic value) throws DocumentException {
    if (stack.isEmpty() || stack.peek().getKind() != Kind.ELEMENT) {
      throw new DocumentException("No element on stack.");
    }

    Node<?> element = stack.peek();

    element.setAttribute(name, value);
    return this;
  }

  public FragmentHelper content(String content) throws DocumentException {
    return content(new Una(content));
  }

  public FragmentHelper content(Atomic content) throws DocumentException {
    if ((stack.isEmpty()) || (stack.peek().getKind() != Kind.ELEMENT)) {
      throw new DocumentException("No element on stack.");
    }

    Node<?> element = stack.peek();

    element.append(Kind.TEXT, null, content);
    return this;
  }

  public FragmentHelper insert(Node<?> node) throws DocumentException {
    if (!stack.isEmpty()) {
      if (stack.peek().getKind() != Kind.ELEMENT) {
        throw new DocumentException("No element on stack.");
      }

      Node<?> element = stack.peek();
      element.append(node);
    } else if (node.getKind() == Kind.ELEMENT) {
      root = (new D2NodeFactory()).element(node.getName());
      stack.push(root);
    } else {
      throw new DocumentException("Unexpected node type '%s'", node.getKind());
    }

    return this;
  }
}