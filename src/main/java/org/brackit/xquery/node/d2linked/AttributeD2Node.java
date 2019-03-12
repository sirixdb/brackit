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
package org.brackit.xquery.node.d2linked;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class AttributeD2Node extends D2Node {
	QNm name;
	Una value;

	AttributeD2Node(ElementD2Node parent, int[] division, QNm name, Atomic value)
			throws DocumentException {
		super(parent, division);
		this.name = checkName(name);
		this.value = value.asUna();
	}

	AttributeD2Node(ElementD2Node parent, QNm name, Atomic value)
			throws DocumentException {
		this(parent, FIRST, name, value);
	}

	public AttributeD2Node(QNm name, Atomic value) throws DocumentException {
		this(null, FIRST, name, value);
	}

	private QNm checkName(QNm name) throws DocumentException {
		if ((name.getPrefix() == null) || (parent == null)) {
			return name;
		}
		return ((ElementD2Node) parent).checkName(name);
	}

	@Override
	public QNm getName() throws DocumentException {
		return name;
	}

	@Override
	public Atomic getValue() {
		return value;
	}

	@Override
	public void setName(QNm name) throws OperationNotSupportedException,
			DocumentException {
		this.name = checkName(name);
	}

	@Override
	public void setValue(Atomic value) throws OperationNotSupportedException,
			DocumentException {
		this.value = value.asUna();
	}

	public Kind getKind() {
		return Kind.ATTRIBUTE;
	}

	public boolean isChildOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isAttributeOf(Node<?> node) {
		return ((parent != null) && (parent == (Object) node));
	}

	@Override
	public boolean isDescendantOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isDescendantOrSelfOf(Node<?> node) {
		// check only for self
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && ((this == (Object) node)));
	}

	@Override
	public boolean isFollowingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isFollowingSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isPrecedingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isPrecedingSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isSiblingOf(Node<?> node) {
		return false;
	}

	@Override
	public D2Node replaceWith(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if (kind != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot replace attribute with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.setAttribute(name, value);
	}

	@Override
	public D2Node replaceWith(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		Kind kind = node.getKind();
		if (kind != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot replace attribute with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.setAttribute(name, value);
	}

	@Override
	public D2Node replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder() {
			@Override
			D2Node first(Kind kind, QNm name, Atomic value)
					throws DocumentException {
				if (kind != Kind.ATTRIBUTE) {
					throw new DocumentException(
							"Cannot replace attribute with node of type: %s.",
							kind);
				}
				if (parent == null) {
					throw new DocumentException(
							"Cannot replace node without parent");
				}

				return parent.setAttribute(name, value);
			}
		};
		parser.parse(builder);
		return builder.root();
	}

	@Override
	public D2Node getPreviousSibling() throws DocumentException {
		return null;
	}

	@Override
	public D2Node getNextSibling() throws DocumentException {
		return null;
	}

	@Override
	public D2Node append(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node append(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertAfter(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertAfter(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertAfter(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertBefore(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertBefore(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node insertBefore(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node prepend(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public D2Node prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')",
				Kind.ATTRIBUTE, name, value);
	}
}
