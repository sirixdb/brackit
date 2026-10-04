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
package io.brackit.query.node;

import java.util.HashSet;
import java.util.Set;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.node.Node;

public final class AttributeReplacement {
  private AttributeReplacement() {
  }

  public static <E extends Node<E>> E replace(Node<E> target, Node<?>[] content, int size) throws DocumentException {
    if (target.getKind() != Kind.ATTRIBUTE) {
      throw new DocumentException("Cannot replace node of type '%s' as attribute", target.getKind());
    }
    E parent = target.getParent();
    if (parent == null) {
      throw new DocumentException("Cannot replace node without parent");
    }

    Set<QNm> names = new HashSet<>();
    try (var attributes = parent.getAttributes()) {
      Node<?> attribute;
      while ((attribute = attributes.next()) != null) {
        if (!attribute.isSelfOf(target)) names.add(attribute.getName());
      }
    }
    for (int i = 0; i < size; i++) {
      if (content[i].getKind() != Kind.ATTRIBUTE) {
        throw new DocumentException("Cannot replace attribute with node of type: %s.", content[i].getKind());
      }
      QNm name = content[i].getName();
      if (!names.add(name)) {
        throw new DocumentException("Attribute '%s' already exists.", name);
      }
    }

    target.delete();
    E first = null;
    for (int i = 0; i < size; i++) {
      E inserted = parent.setAttribute(content[i]);
      if (first == null) first = inserted;
    }
    return first;
  }
}
