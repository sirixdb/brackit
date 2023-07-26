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

import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.type.*;
import io.brackit.query.node.stream.AtomStream;
import io.brackit.query.node.stream.filter.Filter;
import io.brackit.query.node.stream.filter.FilteredStream;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.AbstractItem;
import io.brackit.query.jdm.Axis;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Stream;
import io.brackit.query.jdm.Type;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public abstract class AbstractNode<E extends Node<E>> extends AbstractItem implements Node<E> {

  @SuppressWarnings("unchecked")
  @Override
  public Stream<? extends E> getPath() {
    final Node<? extends E> node = this;
    return new Stream() {
      Node<? extends E> next = node;

      @Override
      public void close() {
      }

      @Override
      public Object next() {
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
    if (other == this) {
      return 0;
    }
    int fragmentIDA = getNodeClassID();
    int fragmentIDB = other.getNodeClassID();
    if (fragmentIDA == fragmentIDB) {
      return cmpInternal((E) other);
    } else {
      return fragmentIDA < fragmentIDB ? -1 : 1;
    }
  }

  protected abstract int cmpInternal(E other);

  private static class AttributeFilter implements Filter<Node<?>> {
    @Override
    public boolean filter(Node<?> node) {
      return node.getKind() == Kind.ATTRIBUTE;
    }
  }

  @Override
  public Stream<? extends E> getDescendantOrSelf() {
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
  public Stream<Atomic> getValues() {
    return new AtomStream<>(getValue());
  }

  @Override
  public Str getStrValue() {
    if (getKind() == Kind.ELEMENT || getKind() == Kind.DOCUMENT) {
      final StringBuilder buffer = new StringBuilder();
      try (Stream<? extends E> scanner = getDescendantOrSelf()) {
        E descendant;
        while ((descendant = scanner.next()) != null) {
          if (descendant.getKind() == Kind.TEXT) {
            buffer.append(descendant.getValue());
          }
        }
      }
      return new Str(buffer.toString());
    } else {
      return getValue().asStr();
    }
  }

  @Override
  public Type type() {
    return switch (getKind()) {
      case ELEMENT -> Type.UN;
      case ATTRIBUTE, TEXT -> Type.UNA;
      default -> null;
    };
  }

  @Override
  public ItemType itemType() {
    return switch (getKind()) {
      case ELEMENT -> new ElementType(getName(), Type.UN);
      case ATTRIBUTE -> new AttributeType(getName(), Type.UNA);
      case TEXT -> new TextType();
      case COMMENT -> new CommentType();
      case PROCESSING_INSTRUCTION -> new PIType();
      case DOCUMENT -> new DocumentType();
      default -> null;
    };
  }

  @Override
  public Stream<? extends Node<?>> performStep(Axis axis, NodeType test) {
    return null;
  }

  @Override
  public Atomic atomize() throws QueryException {
    return getValue();
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof Node<?> && isSelfOf((Node<?>) obj);
  }
}