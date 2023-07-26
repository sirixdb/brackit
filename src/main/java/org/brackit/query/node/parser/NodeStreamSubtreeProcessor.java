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
package org.brackit.query.node.parser;

import java.util.List;

import org.brackit.query.util.log.Logger;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Kind;
import org.brackit.query.jdm.Stream;
import org.brackit.query.jdm.node.Node;

/**
 * Streaming {@link NodeSubtreeProcessor} for fragments.
 *
 * @author Sebastian Baechle
 */
public class NodeStreamSubtreeProcessor<E extends Node<E>> extends NodeSubtreeProcessor<E> {
  private static final Logger log = Logger.getLogger(NodeStreamSubtreeProcessor.class);

  private boolean displayNodeIDs;

  private final Stream<? extends E> scanner;

  public NodeStreamSubtreeProcessor(Stream<? extends E> scanner, List<NodeSubtreeListener<? super E>> listeners) {
    super(listeners);
    this.scanner = scanner;
  }

  @SuppressWarnings("unchecked")
  public void process() throws DocumentException {
    try {
      E[] stack = (E[]) new Node[10];
      int stackSize = 0;
      E node;
      notifyBegin();

      while ((node = scanner.next()) != null) {
        if (stackSize == 0) {
          notifyBeginFragment();
        }

        // get kind of current node
        Kind kind = node.getKind();
        // handle closing tags and start new element if necessary
        if (kind != Kind.ATTRIBUTE) {
          while ((stackSize > 0) && (!stack[stackSize - 1].isParentOf(node))) {
            E ancestor = stack[--stackSize];

            if (ancestor.getKind() == Kind.ELEMENT)
              notifyEndElement(ancestor);
            else
              notifyEndDocument();
          }
        }

        // call handler methods depending on node type
        if (kind == Kind.ELEMENT) {
          notifyStartElement(node);
          if (stackSize == stack.length) {
            E[] newElementStack = (E[]) new Node[((stackSize * 3) / 2) + 1];
            System.arraycopy(stack, 0, newElementStack, 0, stackSize);
            stack = newElementStack;
          }
          stack[stackSize++] = node;
        } else if (kind == Kind.ATTRIBUTE) {
          notifyAttribute(node);
        } else if (kind == Kind.TEXT) {
          notifyText(node);
        } else if (kind == Kind.COMMENT) {
          notifyComment(node);
        } else if (kind == Kind.PROCESSING_INSTRUCTION) {
          notifyProcessingInstruction(node);
        } else if (kind == Kind.DOCUMENT) {
          notifyBeginDocument();
          if (stackSize == stack.length) {
            E[] newElementStack = (E[]) new Node[((stackSize * 3) / 2) + 1];
            System.arraycopy(stack, 0, newElementStack, 0, stackSize);
            stack = newElementStack;
          }
          stack[stackSize++] = node;
        }

        if (stackSize == 0) {
          notifyEndFragment();
        }
      }

      while (stackSize > 0) {
        E ancestor = stack[--stackSize];

        if (ancestor.getKind() == Kind.ELEMENT) {
          notifyEndElement(ancestor);
        } else {
          notifyEndDocument();
        }

        if (stackSize == 0) {
          notifyEndFragment();
        }
      }

      notifyEnd();
    } catch (Exception e) {
      notifyFail();

      log.error(e);
      throw new DocumentException(e);
    } finally {
      scanner.close();
    }
  }
}