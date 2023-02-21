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

import java.util.List;

import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Kind;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.node.Node;

/**
 * Navigating {@link NodeSubtreeProcessor} for fragments.
 *
 * @author Sebastian Baechle
 */
public class NavigationalSubtreeProcessor<E extends Node<E>> extends NodeSubtreeProcessor<E> {
  private final E root;

  public NavigationalSubtreeProcessor(E root, List<NodeSubtreeListener<? super E>> listeners) {
    super(listeners);
    this.root = root;
  }

  public void process() throws DocumentException {
    try {
      notifyBegin();
      notifyBeginFragment();
      traverse(root);
      notifyEndFragment();
      notifyEnd();
    } catch (DocumentException e) {
      notifyFail();
      throw e;
    }
  }

  private void traverse(E node) throws DocumentException {
    Kind kind = node.getKind();

    if (kind == Kind.ELEMENT) {
      notifyStartElement(node);

      Stream<? extends E> attributeStream = node.getAttributes();

      try {
        E attribute;
        while ((attribute = attributeStream.next()) != null) {
          notifyAttribute(attribute);
        }
      } finally {
        attributeStream.close();
      }

      Stream<? extends E> childStream = node.getChildren();
      try {
        E child;
        while ((child = childStream.next()) != null) {
          traverse(child);
        }

      } finally {
        childStream.close();
      }

      notifyEndElement(node);
    } else if (kind == Kind.TEXT) {
      notifyText(node);
    } else if (kind == Kind.COMMENT) {
      notifyComment(node);
    } else if (kind == Kind.PROCESSING_INSTRUCTION) {
      notifyProcessingInstruction(node);
    } else if (kind == Kind.ATTRIBUTE) {
      notifyAttribute(node);
    } else if (kind == Kind.DOCUMENT) {
      notifyBeginDocument();
      Stream<? extends E> childStream = node.getChildren();
      try {
        E child;
        while ((child = childStream.next()) != null) {
          traverse(child);
        }
      } finally {
        childStream.close();
      }
      notifyEndDocument();
    } else {
      throw new DocumentException("Illegal node type: %s", kind);
    }
  }
}