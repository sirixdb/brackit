/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
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
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.Type;

/**
 * Abstract base for expressions that have to construct computed nodes as
 * defined in XQuery 1.0: 3.7.3 Computed Constructors
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class ConstructedNodeBuilder {
	protected interface ContentSink {
		Node<?> addAttribute(QueryContext ctx, Node<?> attribute)
				throws QueryException;

		Node<?> addNode(QueryContext ctx, Node<?> node) throws QueryException;
	}

	public final class ContentList extends ArrayList<Node<?>> implements
			ContentSink {
		@Override
		public Node<?> addAttribute(QueryContext ctx, Node<?> attribute)
				throws QueryException {
			add(attribute);
			return attribute;
		}

		@Override
		public Node<?> addNode(QueryContext ctx, Node<?> node)
				throws QueryException {
			add(node);
			return node;
		}
	}

	protected Node<?> copy(QueryContext ctx, Node<?> source)
			throws QueryException {
		return ctx.getNodeFactory().copy(source);
	}

	protected void buildContentSequence(QueryContext ctx, ContentSink sink,
			Sequence source) throws QueryException {
		if (source != null) {
			if (source instanceof Item) {
				addContent(ctx, sink, (Item) source, null);
			} else {
				Node<?> prevSibling = null;
				Item next;
				Iter s = source.iterate();
				try {
					while ((next = s.next()) != null) {
						prevSibling = addContent(ctx, sink, next, prevSibling);
					}
				} finally {
					s.close();
				}
			}
		}
	}

	private Node<?> addContent(QueryContext ctx, ContentSink sink, Item item,
			Node<?> prevSibling) throws QueryException {
		if (item instanceof Node<?>) {
			Node<?> contentNode = (Node<?>) item;

			if (contentNode.getKind() == Kind.ATTRIBUTE) {
				if (prevSibling != null) {
					throw new QueryException(
							ErrorCode.ERR_TYPE_CONTENT_SEQUENCE_ATTRIBUTE_FOLLOWING_NON_ATTRIBUTE);
				}
				sink.addAttribute(ctx, contentNode);
				return null;
			} else if (contentNode.getKind() == Kind.DOCUMENT) {
				Stream<? extends Node<?>> children = contentNode.getChildren();
				try {
					Node<?> child;
					while ((child = children.next()) != null) {
						sink.addNode(ctx, prevSibling = child);
					}
				} finally {
					children.close();
				}
				return prevSibling;
			} else {
				sink.addNode(ctx, contentNode);
				return contentNode;
			}
		} else {
			if ((prevSibling != null) && (prevSibling.getKind() == Kind.TEXT)) {
				prevSibling.setValue(prevSibling.getValue() + " "
						+ ((Atomic) item).stringValue());
				return prevSibling;
			} else {
				Node<?> node = ctx.getNodeFactory().text(
						((Atomic) item).stringValue());
				return sink.addNode(ctx, node);
			}
		}
	}

	protected String buildTextContent(QueryContext ctx, Sequence source)
			throws QueryException {
		if (source == null) {
			return null;
		}
		if (source instanceof Item) {
			return ((Item) source).atomize().stringValue();
		}
		StringBuilder buf = new StringBuilder();
		Iter it = source.iterate();
		try {
			Item item = it.next();
			if (item != null) {
				buf.append(item.atomize().stringValue());
				while ((item = it.next()) != null) {
					buf.append(' ');
					buf.append(item.atomize().stringValue());
				}
			}
		} finally {
			it.close();
		}
		return buf.toString();
	}

	protected String buildAttributeContent(QueryContext ctx, Sequence content)
			throws QueryException {
		if (content == null) {
			return null;
		}
		// optimized value construction for single item case
		if (content instanceof Item) {
			return ((Item) content).atomize().stringValue();
		} else {
			Item next;
			Iter s = content.iterate();
			try {
				String stringValue = null;
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
			} finally {
				s.close();
			}
		}
	}

	/**
	 * TODO Switch to QNames with namespace handling
	 */
	protected QNm buildElementName(QueryContext ctx, Item name)
			throws QueryException {
		if (name == null) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}

		Atomic atomicName = name.atomize();
		Type nameType = atomicName.type();

		if ((nameType.instanceOf(Type.STR)) || (nameType.instanceOf(Type.UNA))) {
			String nameString = atomicName.stringValue();

			if (nameString.contains(":")) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
						"Namespaces are not supported yet.");
			}

			return new QNm(nameString);
		} else if (nameType.instanceOf(Type.QNM)) {
			return (QNm) name;
		} else {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}

	/**
	 * TODO Switch to QNames with namespace handling
	 */
	protected QNm buildAttributeName(QueryContext ctx, Item name)
			throws QueryException {
		if (name == null) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}

		Atomic atomicName = name.atomize();
		Type nameType = atomicName.type();

		if ((nameType.instanceOf(Type.STR)) || (nameType.instanceOf(Type.UNA))) {
			String nameString = atomicName.stringValue();

			if (nameString.contains(":")) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
						"Namespaces are not supported yet.");
			}

			return new QNm(nameString);
		} else if (nameType.instanceOf(Type.QNM)) {
			return (QNm) name;
		} else {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}

	/**
	 * TODO Switch to QNames with namespace handling
	 */
	protected QNm buildPIName(QueryContext ctx, Item name)
			throws QueryException {
		if (name == null) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}

		Atomic atomicName = name.atomize();
		Type nameType = atomicName.type();

		if ((nameType.instanceOf(Type.STR)) || (nameType.instanceOf(Type.UNA))) {
			String nameString = atomicName.stringValue();

			if (nameString.contains(":")) {
				throw new QueryException(
						ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
						"Namespaces are not supported yet.");
			}

			return new QNm(nameString);
		} else if (nameType.instanceOf(Type.QNM)) {
			return (QNm) name;
		} else {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE);
		}
	}
}
