/*
 * [New BSD License]
<<<<<<< HEAD
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
=======
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
>>>>>>> upstream/master
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
<<<<<<< HEAD
 *
=======
 *
>>>>>>> upstream/master
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
package org.brackit.xquery.update.op;

import java.util.Arrays;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.node.Node;

/**
 * Base class for all insert operations.
 *
 * @author Sebastian Baechle
 *
 */
public class ReplaceNodeOp implements UpdateOp {
  private final Node<?> target;

  private Node<?>[] content;

  private int size;

  public ReplaceNodeOp(Node<?> target) {
    this.target = target;
    this.content = new Node[1];
  }

  @Override
  public void apply() throws QueryException {
    if (target.getKind() == Kind.ATTRIBUTE) {
      Node<?> parentElement = target.getParent();
      parentElement.deleteAttribute(target.getName());

      for (int i = 0; i < size; i++) {
        parentElement.setAttribute(content[i]);
      }
    } else {
      Node<?> ancorNode;
      boolean insertAfter;
      if (target.getPreviousSibling() != null) {
        insertAfter = true;
        ancorNode = target.getPreviousSibling();
      } else {
        insertAfter = false;
        ancorNode = target.getParent();
      }

      if (target.getKind() == Kind.TEXT || target.isRoot()) {
        deleteAndThenInsert(ancorNode, insertAfter);
      } else {
        insertAndThenDelete(ancorNode, insertAfter);
      }
    }
  }

  private void insertAndThenDelete(Node<?> ancorNode, boolean insertAfter)
      throws OperationNotSupportedException, DocumentException {
    insert(ancorNode, insertAfter);

    target.delete();
  }

  private void deleteAndThenInsert(Node<?> ancorNode, boolean insertAfter)
      throws DocumentException, OperationNotSupportedException {
    target.delete();

    insert(ancorNode, insertAfter);
  }

  private void insert(Node<?> ancorNode, boolean insertAfter) throws OperationNotSupportedException, DocumentException {
    for (int i = 0; i < size; i++) {
      if (insertAfter)
        ancorNode.insertAfter(content[i]);
      else
        ancorNode.prepend(content[i]);
    }
  }

  @Override
  public Node<?> getTarget() {
    return target;
  }

  @Override
  public OpType getType() {
    return OpType.REPLACE_NODE;
  }

  public void addContent(Node<?> node) {
    if (size == content.length) {
      content = Arrays.copyOf(content, (content.length * 3) / 2 + 1);
    }

    content[size++] = node;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append(getType());
    out.append(" ");
    out.append(target);
    out.append(" with {");
    for (int i = 0; i < size; i++) {
      if (i > 0)
        out.append(", ");
      out.append(content[i]);
    }
    out.append("}");
    return out.toString();
  }
}
