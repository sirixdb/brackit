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
package org.brackit.xquery.node.linked;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.node.stream.EmptyStream;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class ElementLNode extends ParentLNode {
	protected QNm name;

	protected LNode firstAttribute;

	public ElementLNode(QNm name) {
		this.name = name;
	}

	ElementLNode(ParentLNode parent, QNm name) {
		super(parent);
		this.name = name;
	}

	@Override
	public QNm getName() throws DocumentException {
		return name;
	}

	public Kind getKind() {
		return Kind.ELEMENT;
	}

	@Override
	public LNode getAttribute(QNm name) throws DocumentException {
		for (LNode attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
			if (attribute.getName().equals(name)) {
				return attribute;
			}
		}

		return null;
	}

	@Override
	protected boolean hasAttribute(LNode attribute) {
		for (LNode myAttribute = firstAttribute; myAttribute != null; myAttribute = myAttribute.sibling) {
			if (attribute == myAttribute) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getAttributeValue(QNm name) throws DocumentException {
		LNode attribute = getAttribute(name);
		return (attribute != null) ? attribute.getValue().stringValue() : null;
	}

	@Override
	public Stream<LNode> getAttributes() throws OperationNotSupportedException,
			DocumentException {
		if (firstAttribute == null) {
			return new EmptyStream<LNode>();
		}
		return new SiblingStream(firstAttribute);
	}

	@Override
	public LNode getNextSibling() throws DocumentException {
		if (parent == null) {
			return null;
		}

		return parent.nextSiblingOf(this);
	}

	@Override
	public LNode getPreviousSibling() throws DocumentException {
		if (parent == null) {
			return null;
		}

		return parent.previousSiblingOf(this);
	}

	@Override
	public boolean hasAttributes() throws DocumentException {
		return (firstAttribute != null);
	}

	@Override
	public boolean deleteAttribute(QNm name)
			throws OperationNotSupportedException, DocumentException {
		LNode prev = null;
		for (LNode attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
			if (attribute.getName().equals(name)) {
				if (prev != null) {
					prev.sibling = attribute.sibling;
				} else {
					firstAttribute = attribute.sibling;
				}
				return true;
			}
			prev = attribute;
		}
		return false;
	}

	@Override
	public LNode setAttribute(Node<?> attribute)
			throws OperationNotSupportedException, DocumentException {
		if (attribute.getKind() != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot set nodes of type '%s' as attribute", attribute
							.getKind());
		}

		return setAttribute(attribute.getName(), attribute.getValue());
	}

	@Override
	public LNode setAttribute(QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if (firstAttribute == null) {
			return (firstAttribute = new AttributeLNode(this, name, value));
		} else {
			LNode prev = null;
			for (LNode attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
				if (attribute.getName().equals(name)) {
					attribute.setValue(value);
					return attribute;
				}
				prev = attribute;
			}
			return (prev.sibling = new AttributeLNode(this, name, value));
		}
	}

	@Override
	public void setName(QNm name) throws OperationNotSupportedException,
			DocumentException {
		this.name = name;
	}

	@Override
	public LNode insertAfter(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertAfter(this, parser);
	}

	@Override
	public LNode insertBefore(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(this, kind, value);
	}

	@Override
	public LNode insertBefore(Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(this, child);
	}

	@Override
	public LNode insertBefore(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(this, parser);
	}

	@Override
	public LNode replaceWith(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if ((isRoot()) && (kind != Kind.ELEMENT)) {
			throw new DocumentException(
					"Cannot replace root node with node of type: %s", kind);
		}

		if ((kind != Kind.ELEMENT) && (kind != Kind.TEXT)) {
			throw new DocumentException(
					"Cannot replace element with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replace(this, kind, value);
	}

	@Override
	public LNode replaceWith(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		Kind kind = node.getKind();
		if ((isRoot()) && (kind != Kind.ELEMENT)) {
			throw new DocumentException(
					"Cannot replace root node with node of type: %s", kind);
		}

		if ((kind != Kind.ELEMENT) && (kind != Kind.TEXT)) {
			throw new DocumentException(
					"Cannot replace element with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replace(this, node);
	}

	@Override
	public LNode replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode node = builder.build(parser);
		Kind kind = node.getKind();
		if ((isRoot()) && (kind != Kind.ELEMENT)) {
			throw new DocumentException(
					"Cannot replace root node with node of type: %s", kind);
		}

		if ((kind != Kind.ELEMENT) && (kind != Kind.TEXT)) {
			throw new DocumentException(
					"Cannot replace element with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replaceDirect(this, node);
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')",
				Kind.ELEMENT, name, null);
	}
}
