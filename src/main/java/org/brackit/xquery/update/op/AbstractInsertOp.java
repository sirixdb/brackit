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
package org.brackit.xquery.update.op;

import java.util.Arrays;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.jdm.node.Node;

/**
 * Base class for all insert operations.
 *
 * @author Sebastian Baechle
 */
public abstract class AbstractInsertOp implements UpdateOp {
  private final Node<?> target;

  private Node<?>[] content;

  private int size;

  public AbstractInsertOp(Node<?> target) {
    this.target = target;
    this.content = new Node[1];
  }

  @Override
  public void apply() {
    for (int i = 0; i < size; i++) {
      doInsert(target, content[i]);
    }
  }

  @Override
  public Node<?> getTarget() {
    return target;
  }

  public void addContent(Node<?> node) {
    if (size == content.length) {
      content = Arrays.copyOf(content, content.length * 3 / 2 + 1);
    }

    content[size++] = node;
  }

  protected abstract void doInsert(Node<?> target, Node<?> content) throws QueryException;

  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append(getType());
    out.append(" {");
    for (int i = 0; i < size; i++) {
      if (i > 0)
        out.append(", ");
      out.append(content[i]);
    }
    out.append("} on ");
    out.append(target);
    return out.toString();
  }
}
