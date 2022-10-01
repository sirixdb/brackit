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
package org.brackit.xquery.expr;

import java.util.ArrayList;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.XMLChar;
import org.brackit.xquery.xdm.node.Node;

/**
 * Abstract base for expressions that have to construct computed nodes as
 * defined in XQuery 1.0: 3.7.3 Computed Constructors
 *
 * @author Sebastian Baechle
 */
public abstract class ConstructedNodeBuilder {
  protected interface ContentSink {
    Node<?> addAttribute(QueryContext ctx, Node<?> attribute) throws QueryException;

    Node<?> addNode(QueryContext ctx, Node<?> node) throws QueryException;
  }

  public static final class ContentList extends ArrayList<Node<?>> implements ContentSink {
    @Override
    public Node<?> addAttribute(QueryContext ctx, Node<?> attribute) {
      add(attribute);
      return attribute;
    }

    @Override
    public Node<?> addNode(QueryContext ctx, Node<?> node) {
      add(node);
      return node;
    }
  }

  protected Node<?> copy(QueryContext ctx, Node<?> source) {
    return ctx.getNodeFactory().copy(source);
  }

  protected void buildContentSequence(QueryContext ctx, ContentSink sink, Sequence source) {
    if (source != null) {
      if (source instanceof Item) {
        addContent(ctx, sink, (Item) source, null);
      } else {
        Node<?> prevSibling = null;
        Item next;
        try (Iter s = source.iterate()) {
          while ((next = s.next()) != null) {
            prevSibling = addContent(ctx, sink, next, prevSibling);
          }
        }
      }
    }
  }

  private Node<?> addContent(QueryContext ctx, ContentSink sink, Item item, Node<?> prevSibling) {
    if (item instanceof Node<?>) {
      Node<?> contentNode = (Node<?>) item;

      if (contentNode.getKind() == Kind.ATTRIBUTE) {
        if (prevSibling != null) {
          throw new QueryException(ErrorCode.ERR_TYPE_CONTENT_SEQUENCE_ATTRIBUTE_FOLLOWING_NON_ATTRIBUTE);
        }
        sink.addAttribute(ctx, contentNode);
        return null;
      } else if (contentNode.getKind() == Kind.DOCUMENT) {
        try (Stream<? extends Node<?>> children = contentNode.getChildren()) {
          Node<?> child;
          while ((child = children.next()) != null) {
            sink.addNode(ctx, prevSibling = child);
          }
        }
        return prevSibling;
      } else {
        sink.addNode(ctx, contentNode);
        return contentNode;
      }
    } else {
      if (prevSibling != null && prevSibling.getKind() == Kind.TEXT) {
        prevSibling.setValue(new Str(prevSibling.getValue().stringValue() + " " + ((Atomic) item).stringValue()));
        return prevSibling;
      } else {
        Node<?> node = ctx.getNodeFactory().text(new Str(((Atomic) item).stringValue()));
        return sink.addNode(ctx, node);
      }
    }
  }

  protected String buildTextContent(Sequence source) {
    if (source == null) {
      return null;
    }
    if (source instanceof Item) {
      return ((Item) source).atomize().stringValue();
    }
    StringBuilder buf = new StringBuilder();
    try (Iter it = source.iterate()) {
      Item item = it.next();
      if (item != null) {
        buf.append(item.atomize().stringValue());
        while ((item = it.next()) != null) {
          buf.append(' ');
          buf.append(item.atomize().stringValue());
        }
      }
    }
    return buf.toString();
  }

  protected String buildAttributeContent(Sequence content) {
    if (content == null) {
      return "";
    }
    // optimized value construction for single item case
    if (content instanceof Item) {
      return ((Item) content).atomize().stringValue();
    } else {
      Item next;
      try (Iter s = content.iterate()) {
        String stringValue = "";
        if ((next = s.next()) != null) {
          stringValue = next.atomize().stringValue();
          if ((next = s.next()) != null) {
            StringBuilder builder = new StringBuilder();
            while (next != null) {
              builder.append(" ");
              builder.append(next.atomize().stringValue());
              next = s.next();
            }
            stringValue += builder.toString();
          }
        }
        return stringValue;
      }
    }
  }

  protected QNm buildElementName(StaticContext ctx, Item name) {
    if (name == null) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    }

    Atomic atomicName = name.atomize();
    Type nameType = atomicName.type();

    if (nameType.instanceOf(Type.QNM)) {
      return (QNm) name;
    } else if (nameType.instanceOf(Type.STR) || nameType.instanceOf(Type.UNA)) {
      QNm qnm = new QNm(atomicName.stringValue());
      if (qnm.getPrefix() != null && !qnm.getPrefix().isEmpty()) {
        String uri = ctx.getNamespaces().resolve(qnm.getPrefix());
        if (uri == null) {
          throw new QueryException(ErrorCode.ERR_UNKNOWN_NS_PREFIX_IN_COMP_CONSTR,
                                   "Statically unkown namespace prefix in computed element constructor: '%s'",
                                   qnm.getPrefix());
        }
        return new QNm(uri, null, qnm.getLocalName());
      } else {
        String uri = ctx.getNamespaces().getDefaultElementNamespace();
        if (uri == null || uri.isEmpty()) {
          return qnm;
        } else {
          return new QNm(uri, null, qnm.getLocalName());
        }
      }
    } else {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    }
  }

  protected QNm buildAttributeName(StaticContext ctx, Item name) {
    if (name == null) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    }

    Atomic atomicName = name.atomize();
    Type nameType = atomicName.type();

    if (nameType.instanceOf(Type.QNM)) {
      return (QNm) name;
    } else if (nameType.instanceOf(Type.STR) || nameType.instanceOf(Type.UNA)) {
      QNm qnm = new QNm(atomicName.stringValue());
      if (!qnm.getPrefix().isEmpty()) {
        String uri = ctx.getNamespaces().resolve(qnm.getPrefix());
        if (uri == null) {
          throw new QueryException(ErrorCode.ERR_UNKNOWN_NS_PREFIX_IN_COMP_CONSTR,
                                   "Statically unkown namespace prefix in computed element constructor: '%s'",
                                   qnm.getPrefix());
        }
        return new QNm(uri, null, qnm.getLocalName());
      } else {
        String uri = ctx.getNamespaces().getDefaultElementNamespace();
        if (uri == null || uri.isEmpty()) {
          return qnm;
        } else {
          return new QNm(uri, null, qnm.getLocalName());
        }
      }
    } else {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
    }
  }

  protected QNm buildPITarget(Item item) {

    if (item == null) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Empty target in processing instruction");
    }

    Atomic atomic = item.atomize();
    Type type = atomic.type();
    QNm target;

    if (type == Type.NCN) {
      target = new QNm(atomic.stringValue());
    } else if (type == Type.STR || type == Type.UNA) {
      String ncname = atomic.stringValue();
      ncname = Whitespace.normalizeXML11(ncname);
      ncname = Whitespace.collapse(ncname);
      if (!XMLChar.isNCName(ncname)) {
        throw new QueryException(ErrorCode.ERR_PI_TARGET_CAST_TO_NCNAME,
                                 "Cast target of processing instruction to xs:NCName failed: %s",
                                 ncname);
      }
      target = new QNm(ncname);
    } else {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, "Invalid target in processing instruction");
    }

    if (target.getLocalName().toLowerCase().equals("xml")) {
      throw new QueryException(ErrorCode.ERR_PI_TARGET_IS_XML,
                               "Illegal NCName in processing instruction: '%s'",
                               target);
    }
    return target;
  }

  protected String buildPIContent(Sequence s) {
    StringBuilder buf = new StringBuilder();

    Atomic atomic;
    if (s != null) {
      if (s instanceof Item) {
        atomic = ((Item) s).atomize();

        if (!atomic.type().instanceOf(Type.STR)) {
          atomic = Cast.cast(null, atomic, Type.STR, true);
        }

        buf.append(atomic.stringValue());
      } else {
        boolean first = true;
        try (Iter it = s.iterate()) {
          Item item;
          while ((item = it.next()) != null) {
            atomic = item.atomize();

            if (!atomic.type().instanceOf(Type.STR)) {
              atomic = Cast.cast(null, atomic, Type.STR, true);
            }

            String str = atomic.stringValue();
            if (!str.isEmpty()) {
              if (!first) {
                buf.append(' ');
              }
              first = false;
              buf.append(str);
            }
          }
        }
      }
    }

    String content = buf.toString();

    if (content.contains("?>")) {
      throw new QueryException(ErrorCode.ERR_PI_WOULD_CONTAIN_ILLEGAL_STRING,
                               "Content expression of processing instruction illegal string '?>'",
                               content);
    }
    return content;
  }
}