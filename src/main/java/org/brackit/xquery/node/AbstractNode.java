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

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.node.stream.AtomStream;
import org.brackit.xquery.node.stream.filter.Filter;
import org.brackit.xquery.node.stream.filter.FilteredStream;
import org.brackit.xquery.xdm.AbstractItem;
import org.brackit.xquery.xdm.Axis;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.type.*;

/**
 *
 * @author Sebastian Baechle
 *
 */
public abstract class AbstractNode<E extends Node<E>> extends AbstractItem implements Node<E> {

  protected static final Kind[] mapping =
      new Kind[] {Kind.ELEMENT, Kind.ATTRIBUTE, Kind.TEXT, Kind.DOCUMENT, Kind.COMMENT, Kind.PROCESSING_INSTRUCTION};

  @SuppressWarnings("unchecked")
  @Override
  public Stream<? extends E> getPath() throws DocumentException {
    final Node<? extends E> node = this;
    return new Stream() {
      Node<? extends E> next = node;

      @Override
      public void close() {}

      @Override
      public Object next() throws DocumentException {
        if (next == null) {
          return null;
        }
        Node<? extends E> deliver = next;
        next = deliver.getParent();
        return deliver;
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public final int cmp(Node<?> other) {
    if ((other) == this) {
      return 0;
    }
    int fragmentIDA = getNodeClassID();
    int fragmentIDB = other.getNodeClassID();
    if (fragmentIDA == fragmentIDB) {
      return cmpInternal((E) other);
    } else {
      return fragmentIDA < fragmentIDB
          ? -1
          : 1;
    }
  }

  protected abstract int cmpInternal(E other);

  private static class AttributeFilter implements Filter<Node<?>> {
    @Override
    public boolean filter(Node<?> node) throws DocumentException {
      boolean check = node.getKind() == Kind.ATTRIBUTE;
      return check;
    }
  }

  @Override
  public Stream<? extends E> getDescendantOrSelf() throws DocumentException {
    return new FilteredStream<E>(getSubtree(), new AttributeFilter());
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return true;
  }

  @Override
  public AnyURI getBaseURI() {
    return null;
  }

  @Override
  public Stream<Atomic> getValues() throws DocumentException {
    return new AtomStream<Atomic>(getValue());
  }

  @Override
  public Str getStrValue() throws DocumentException {
    if ((getKind() == Kind.ELEMENT) || (getKind() == Kind.DOCUMENT)) {
      final StringBuilder buffer = new StringBuilder();
      final Stream<? extends E> scanner = getDescendantOrSelf();
      try {
        E descendant;
        while ((descendant = scanner.next()) != null) {
          if (descendant.getKind() == Kind.TEXT) {
            buffer.append(descendant.getValue());
          }
        }
      } finally {
        scanner.close();
      }
      return new Str(buffer.toString());
    } else {
      return getValue().asStr();
    }
  }

  @Override
  public Type type() {
    switch (getKind()) {
      case ELEMENT:
        return Type.UN;
      case ATTRIBUTE:
      case TEXT:
        return Type.UNA;
      default:
        return null;
    }
  }

  @Override
  public ItemType itemType() throws DocumentException {
    switch (getKind()) {
      case ELEMENT:
        return new ElementType(getName(), Type.UN);
      case ATTRIBUTE:
        return new AttributeType(getName(), Type.UNA);
      case TEXT:
        return new TextType();
      default:
        return null;
    }
  }

  @Override
  public Stream<? extends Node<?>> performStep(Axis axis, NodeType test) throws DocumentException {
    return null;
  }

  @Override
  public Atomic atomize() throws QueryException {
    return getValue();
  }

  @Override
  public final boolean equals(Object obj) {
    return ((obj instanceof Node<?>) && (isSelfOf(((Node<?>) obj))));
  }
}