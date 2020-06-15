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
package org.brackit.xquery.xdm.type;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public final class DocumentType extends NodeType {
  public static final DocumentType DOC = new DocumentType();

  private final ElementType elementType;

  public DocumentType() {
    elementType = null;
  }

  public DocumentType(ElementType elementType) {
    this.elementType = elementType;
  }

  public ElementType getElementType() {
    return elementType;
  }

  @Override
  public Kind getNodeKind() {
    return Kind.DOCUMENT;
  }

  @Override
  public boolean matches(Node<?> node) throws QueryException {
    if (elementType != null) {
      if (node.getKind() != Kind.DOCUMENT) {
        return false;
      }
      Stream<? extends Node<?>> children = node.getChildren();
      try {
        Node<?> rootElem;
        while ((rootElem = children.next()) != null) {
          if (rootElem.getKind() == Kind.ELEMENT) {
            return elementType.matches(rootElem);
          }
        }
      } finally {
        children.close();
      }
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Document node does not have a root element");
    }
    return (node.getKind() == Kind.DOCUMENT);
  }

  @Override
  public boolean matches(Item item) throws QueryException {
    if (elementType != null) {
      if (((Node<?>) item).getKind() != Kind.DOCUMENT) {
        return false;
      }
      Stream<? extends Node<?>> children = ((Node<?>) item).getChildren();
      try {
        Node<?> rootElem;
        while ((rootElem = children.next()) != null) {
          if (rootElem.getKind() == Kind.ELEMENT) {
            return elementType.matches(rootElem);
          }
        }
      } finally {
        children.close();
      }
      throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Document node does not have a root element");
    }
    return ((item instanceof Node<?>) && (((Node<?>) item).getKind() == Kind.DOCUMENT));
  }

  public String toString() {
    return (elementType != null) ? String.format("document-node(\"%s\")", elementType) : "document-node()";
  }

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DocumentType)) {
      return false;
    }
    DocumentType t = (DocumentType) obj;
    if (elementType == null) {
      if (t.elementType != null) {
        return false;
      }
    } else {
      if ((t.elementType == null) || (!elementType.equals(t.elementType))) {
        return false;
      }
    }
    return true;
  }
}
