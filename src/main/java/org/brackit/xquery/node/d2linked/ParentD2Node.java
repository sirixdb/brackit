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
import org.brackit.xquery.node.stream.EmptyStream;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Stream;

/**
 * Abstract base for all constructed nodes that may have children
 * 
 * @author Sebastian Baechle
 * 
 */
abstract class ParentD2Node extends D2Node {
	protected D2Node firstChild;

	protected static class SiblingStream implements Stream<D2Node> {
		D2Node node;

		SiblingStream(D2Node first) {
			node = first;
		}

		@Override
		public void close() {
			node = null;
		}

		@Override
		public D2Node next() throws DocumentException {
			if (node == null) {
				return null;
			}
			D2Node deliver = node;
			node = node.sibling;
			return deliver;
		}
	}

	private final class FragmentScanner implements Stream<D2Node> {
		D2Node current;

		D2Node first;

		D2Node root;

		boolean inAttribute;

		public FragmentScanner(D2Node root) {
			this.root = root;
			current = root;
			first = root;
		}

		@Override
		public void close() {
			current = null;
		}

		@Override
		public D2Node next() throws DocumentException {
			if (current == null) {
				return null;
			}
			if (first != null) {
				D2Node deliver = first;
				first = null;
				return deliver;
			}
			D2Node next;
			if ((current instanceof ElementD2Node)
					&& ((next = ((ElementD2Node) current).firstAttribute) != null)) {
				// try to descend to attribute
				inAttribute = true;
				current = next;
				return next;
			}

			if ((current instanceof ParentD2Node)
					&& ((next = ((ParentD2Node) current).firstChild) != null)) {
				// try to descend to subtree
				current = next;
				return next;
			}

			while (current != root) {
				if ((next = current.sibling) != null) {
					// try to switch to sibling of descendant
					current = next;
					return next;
				}
				current = current.parent;
				if (inAttribute) {
					inAttribute = false;
					if ((next = ((ParentD2Node) current).firstChild) != null) {
						current = next;
						return next;
					}
				}
			}
			return null;
		}
	}

	protected final class DescendantScanner implements Stream<D2Node> {
		D2Node current;

		D2Node root;

		boolean first;

		public DescendantScanner(D2Node root) {
			current = root;
			first = true;
			this.root = root;
		}

		@Override
		public void close() {
			current = null;
		}

		@Override
		public D2Node next() throws DocumentException {
			if (first) {
				first = false;
				return root;
			}
			D2Node next;
			if ((current instanceof ParentD2Node)
					&& ((next = ((ParentD2Node) current).firstChild) != null)) {
				// try to descend to subtree
				current = next;
				return next;
			}

			while (current != root) {
				if ((next = current.sibling) != null) {
					// try to switch to sibling of descendant
					current = next;
					return next;
				}
				current = current.parent;
			}
			return null;
		}
	}

	public Stream<D2Node> getSubtree() throws DocumentException {
		return new FragmentScanner(this);
	}

	protected ParentD2Node(ParentD2Node parent, int[] division) {
		super(parent, division);
	}

	protected boolean hasAttribute(D2Node attribute) {
		return false;
	}

	@Override
	public Atomic getValue() throws DocumentException {
		StringBuilder buffer = new StringBuilder();
		Stream<D2Node> scanner = new DescendantScanner(this);
		try {
			D2Node descendant;
			while ((descendant = scanner.next()) != null) {
				if (descendant.getKind() == Kind.TEXT) {
					buffer.append(descendant.getValue());
				}
			}
		} finally {
			scanner.close();
		}

		return new Una(buffer.toString());
	}

	D2Node nextSiblingOf(D2Node node) {
		return node.sibling;
	}

	D2Node previousSiblingOf(D2Node node) {
		if (node == firstChild) {
			return null;
		}
		for (D2Node child = firstChild; child != null; child = child.sibling) {
			if (child.sibling == node) {
				return child;
			}
		}
		return null;
	}

	void deleteChild(D2Node node) throws DocumentException {
		if ((getKind() == Kind.DOCUMENT) && (node.getKind() == Kind.ELEMENT)) {
			throw new DocumentException("The root element must not be deleted");
		}

		D2Node prev = previousSiblingOf(node);
		if (prev == null)
			firstChild = node.sibling;
		else
			prev.sibling = node.sibling;
	}

	D2Node insertChild(D2Node sibling, Kind kind, QNm name, Atomic value,
			boolean right) throws DocumentException {

		if ((getKind() == Kind.DOCUMENT) && (kind == Kind.ELEMENT)) {
			for (D2Node c = firstChild; c != null; c = c.sibling) {
				if (c.getKind() == Kind.ELEMENT) {
					throw new DocumentException(
							"Document nodes must have only one root element");
				}
			}
		}

		if (firstChild == null) {
			return (firstChild = buildChild(null, null, kind, name, value));
		}
		D2Node ps = null;
		D2Node ns = null;
		if (sibling == null) {
			if (right) {
				// insert as last child
				// -> scan to last child
				ps = firstChild;
				while (ps.sibling != null) {
					ps = ps.sibling;
				}
				ns = sibling;
			} else {
				// insert as first child
				ns = firstChild;
			}
		} else {
			if (right) {
				// insert after sibling
				ps = sibling;
				ns = sibling.sibling;
			} else {
				// insert before sibling
				ns = sibling;
				if (firstChild != sibling) {
					ps = firstChild;
					while (ps.sibling != sibling) {
						ps = ps.sibling;
					}
				}
			}
		}

		if (kind == Kind.TEXT) {
			// merge adjacent text nodes
			if ((ps != null) && (ps.getKind() == Kind.TEXT)) {
				ps.setValue(new Una(ps.getValue().stringValue()
						+ value.stringValue()));
				return ps;
			}
			if ((ns != null) && (ns.getKind() == Kind.TEXT)) {
				ns.setValue(new Una(ns.getValue().stringValue()
						+ value.stringValue()));
				return ns;
			}
		}

		int[] psd = (ps != null) ? ps.division : null;
		int[] nsd = (ns != null) ? ns.division : null;
		D2Node c = buildChild(psd, nsd, kind, name, value);
		c.sibling = ns;
		if (ps != null) {
			ps.sibling = c;
		}
		return c;
	}

	private D2Node replaceChild(D2Node sibling, Kind kind, QNm name,
			Atomic value) throws DocumentException {

		if ((getKind() == Kind.DOCUMENT) && (sibling != null)
				&& (sibling.getKind() == Kind.ELEMENT)
				&& (kind != Kind.ELEMENT)) {
			throw new DocumentException(
					"Cannot replace root element with of kind: %s", kind);
		}

		D2Node previous = firstChild;
		while ((previous.sibling != null) && (previous.sibling != sibling))
			previous = previous.sibling;

		D2Node child = buildChild(sibling.division, kind, name, value);

		child.sibling = sibling.sibling;

		if (previous != null)
			previous.sibling = child;
		else
			firstChild = child;
		return child;
	}

	private D2Node buildChild(int[] prevSibling, int[] nextSibling, Kind kind,
			QNm name, Atomic value) throws DocumentException {
		int[] division = (prevSibling != null) ? ((nextSibling != null) ? siblingBetween(
				prevSibling, nextSibling)
				: siblingAfter(prevSibling))
				: ((nextSibling != null) ? siblingBefore(nextSibling) : FIRST);

		return buildChild(division, kind, name, value);
	}

	private D2Node buildChild(int[] division, Kind kind, QNm name, Atomic value)
			throws DocumentException {
		D2Node child;
		if (kind == Kind.ELEMENT) {
			child = new ElementD2Node(this, division, name);
		} else if (kind == Kind.TEXT) {
			child = new TextD2Node(this, division, value);
		} else if (kind == Kind.COMMENT) {
			child = new CommentD2Node(this, division, value);
		} else if (kind == Kind.PROCESSING_INSTRUCTION) {
			child = new PID2Node(this, division, name, value);
		} else {
			throw new DocumentException("Illegal child node kind: %s", kind);
		}

		return child;
	}

	@Override
	public D2Node append(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(null, kind, name, value, true);
	}

	@Override
	public D2Node append(Node<?> child) throws DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, null, true);
		child.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	@Override
	public D2Node append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, null, true);
		parser.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	@Override
	public Stream<D2Node> getChildren() throws DocumentException {
		if (firstChild == null) {
			return new EmptyStream<D2Node>();
		}
		return new SiblingStream(firstChild);
	}

	@Override
	public D2Node getFirstChild() throws DocumentException {
		return firstChild;
	}

	@Override
	public D2Node getLastChild() throws DocumentException {
		if (firstChild == null) {
			return null;
		}
		D2Node child = firstChild;
		while (child.sibling != null)
			child = child.sibling;

		return child;
	}

	@Override
	public boolean hasChildren() throws DocumentException {
		return (firstChild != null);
	}

	@Override
	public boolean isAncestorOf(Node<?> node) {
		return (node != null)
				&& ((node instanceof D2Node) && (((D2Node) node)
						.isInSubtreeOf(this)));
	}

	@Override
	public boolean isAncestorOrSelfOf(Node<?> node) {
		return (node != null)
				&& ((Object) node == this)
				|| ((node instanceof D2Node) && (((D2Node) node)
						.isInSubtreeOf(this)));
	}

	@Override
	public boolean isParentOf(Node<?> node) {
		return (node != null)
				&& (((node.isChildOf(this)) || (node.isAttributeOf(this))));
	}

	@Override
	public D2Node prepend(Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(null, kind, name, value, true);
	}

	@Override
	public D2Node prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, null, false);
		child.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	@Override
	public D2Node prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, null, false);
		parser.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	D2Node insertBefore(D2Node node, Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(node, kind, name, value, true);
	}

	D2Node insertBefore(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, node, false);
		child.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	D2Node insertBefore(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(this, node, false);
		parser.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	D2Node insertAfter(D2Node node, Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(node, kind, name, value, false);
	}

	D2Node insertAfter(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(parent, this, true);
		child.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	D2Node insertAfter(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2NodeBuilder builder = new D2NodeBuilder(parent, this, true);
		parser.parse(builder);
		D2Node n = builder.root();
		return n;

	}

	D2Node replace(D2Node node, Kind kind, QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return replaceChild(node, kind, name, value);
	}

	D2Node replace(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}
		final D2Node me = this;
		D2NodeBuilder builder = new D2NodeBuilder() {
			@Override
			D2Node first(Kind kind, QNm name, Atomic value)
					throws DocumentException {
				return replace(me, kind, name, value);
			}
		};
		child.parse(builder);
		D2Node n = builder.root();
		return n;
	}

	D2Node replace(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}
		final D2Node me = this;
		D2NodeBuilder builder = new D2NodeBuilder() {
			@Override
			D2Node first(Kind kind, QNm name, Atomic value)
					throws DocumentException {
				return replace(me, kind, name, value);
			}
		};
		parser.parse(builder);
		D2Node n = builder.root();
		return n;
	}
}