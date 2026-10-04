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
package io.brackit.query.update.op;

import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.node.Node;

/**
 * Base class for all insert operations.
 *
 * @author Sebastian Baechle
 */
public class ReplaceNodeOp extends InsertBeforeOp {
  public ReplaceNodeOp(Node<?> target) {
    super(target);
  }

  @Override
  protected void insertContent(Node<?> target, Node<?>[] content, int size) {
    if (target.getKind() == Kind.ATTRIBUTE) {
      Node<?> parentElement = target.getParent();
      target.delete();

      for (int i = 0; i < size; i++) {
        parentElement.setAttribute(content[i]);
      }
    } else {
      if (target.getKind() == Kind.TEXT || target.isRoot()) {
        Node<?> parent = target.getParent();
        Node<?> previous = target.getPreviousSibling();
        Node<?> next = target.getNextSibling();
        target.delete();
        if (next != null) {
          super.insertContent(next, content, size);
        } else if (previous != null) {
          new InsertAfterOp(previous).insertContent(previous, content, size);
        } else {
          new InsertIntoAsFirstOp(parent).insertContent(parent, content, size);
        }
      } else {
        super.insertContent(target, content, size);
        target.delete();
      }
    }
  }

  @Override
  public OpType getType() {
    return OpType.REPLACE_NODE;
  }
}
