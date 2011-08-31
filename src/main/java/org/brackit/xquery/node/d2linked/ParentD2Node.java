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
package org.brackit.xquery.node.d2linked;

import java.util.Arrays;

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
import org.brackit.xquery.xdm.Type;

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
					&& ((next = ((ElementD2Node) current).firstAttribute) != null)) // try
			// to
			// descend
			// to
			// attribute
			{
				inAttribute = true;
				current = next;
				return next;
			}

			if ((current instanceof ParentD2Node)
					&& ((next = ((ParentD2Node) current).firstChild) != null)) // try
			// to
			// descend
			// to
			// subtree
			{
				current = next;
				return next;
			}

			while (current != root) {
				if ((next = current.sibling) != null) // try to switch to
				// sibling of descendant
				{
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

	private final class DescendantScanner implements Stream<D2Node> {
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
					&& ((next = ((ParentD2Node) current).firstChild) != null)) // try
			// to
			// descend
			// to
			// subtree
			{
				current = next;
				return next;
			}

			while (current != root) {
				if ((next = current.sibling) != null) // try to switch to
				// sibling of descendant
				{
					current = next;
					return next;
				}
				current = current.parent;
			}
			return null;
		}
	}

	@Override
	public Stream<? extends D2Node> getDescendantOrSelf()
			throws DocumentException {
		return new DescendantScanner(this);
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
		StringBuffer buffer = new StringBuffer();
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

	void deleteChild(D2Node node) {
		D2Node prev = previousSiblingOf(node);
		if (prev == null)
			firstChild = node.sibling;
		else
			prev.sibling = node.sibling;
	}

	private D2Node addChild(Kind kind, Atomic value, boolean append)
			throws DocumentException {
		if (((kind == Kind.ATTRIBUTE) || (kind == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					kind);
		}

		D2Node child;

		if (firstChild == null) {
			return (firstChild = buildChild(null, null, kind, value));
		} else if (append) {
			D2Node previous = firstChild;
			while (previous.sibling != null)
				previous = previous.sibling;

			if ((kind == Kind.TEXT) && (previous != null)
					&& (previous.getKind() == Kind.TEXT)) {
				((TextD2Node) previous).value = new Una(
						((TextD2Node) previous).value.stringValue()
								+ value.stringValue());
				return previous;
			}
			child = buildChild(previous.division,
					(previous.sibling != null) ? previous.sibling.division
							: null, kind, value);
			previous.sibling = child;
		} else {
			D2Node first = firstChild;
			if ((kind == Kind.TEXT) && (first != null)
					&& (first.getKind() == Kind.TEXT)) {
				((TextD2Node) first).value = new Una(value.stringValue()
						+ ((TextD2Node) first).value.stringValue());
				return first;
			}
			child = buildChild(null, first.division, kind, value);
			child.sibling = first;
			firstChild = sibling;
		}
		return child;
	}

	private D2Node insertChild(D2Node sibling, Kind kind, Atomic value,
			boolean after) throws DocumentException {
		if (((kind == Kind.ATTRIBUTE) || (kind == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					kind);
		}

		D2Node child;
		if (firstChild == null) {
			return (firstChild = buildChild(null, null, kind, value));
		} else if (!after) {
			D2Node previous = null;
			if (firstChild != sibling) {
				previous = firstChild;
				while ((previous.sibling != null)
						&& (previous.sibling != sibling))
					previous = previous.sibling;
			}

			if (kind == Kind.TEXT) {
				if ((previous != null) && (previous.getKind() == Kind.TEXT)) {
					((TextD2Node) previous).value = new Una(
							((TextD2Node) previous).value.stringValue()
									+ value.stringValue());
					return previous;
				}
				if ((sibling != null) && (sibling.getKind() == Kind.TEXT)) {
					((TextD2Node) sibling).value = new Una(
							((TextD2Node) sibling).value.stringValue()
									+ value.stringValue());
					return sibling;
				}
			}
			if (previous != null) {
				child = buildChild(previous.division, sibling.division, kind,
						value);
				previous.sibling = child;
			} else {
				child = buildChild(null, sibling.division, kind, value);
			}
			child.sibling = sibling;

		} else {
			if (kind == Kind.TEXT) {
				if ((sibling != null) && (sibling.getKind() == Kind.TEXT)) {
					((TextD2Node) sibling).value = new Una(
							((TextD2Node) sibling).value.stringValue()
									+ value.stringValue());
					return sibling;
				}
				if ((sibling.sibling != null)
						&& (sibling.sibling.getKind() == Kind.TEXT)) {
					((TextD2Node) sibling.sibling).value = new Una(
							((TextD2Node) sibling.sibling).value.stringValue()
									+ value.stringValue());
					return sibling.sibling;
				}
			}
			child = buildChild(
					sibling.division,
					(sibling.sibling != null) ? sibling.sibling.division : null,
					kind, value);
			child.sibling = sibling.sibling;
			child.sibling = sibling;
		}
		return child;
	}

	private D2Node replaceChild(D2Node sibling, Kind kind, Atomic value)
			throws DocumentException {
		if (((kind == Kind.ATTRIBUTE) || (kind == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					kind);
		}

		D2Node previous = firstChild;
		while ((previous.sibling != null) && (previous.sibling != sibling))
			previous = previous.sibling;

		D2Node child = buildChild(sibling.division, kind, value);

		child.sibling = sibling.sibling;

		if (previous != null)
			previous.sibling = child;
		else
			firstChild = child;
		return child;
	}

	private D2Node buildChild(int[] prevSibling, int[] nextSibling, Kind kind,
			Atomic value) throws DocumentException {
		int[] division = (prevSibling != null) ? ((nextSibling != null) ? siblingBetween(
				prevSibling, nextSibling)
				: siblingAfter(prevSibling))
				: ((nextSibling != null) ? siblingBefore(nextSibling) : FIRST);

		return buildChild(division, kind, value);
	}

	private D2Node buildChild(int[] division, Kind kind, Atomic value)
			throws DocumentException {
		D2Node child;
		if (kind == Kind.ELEMENT) {
			if (!value.type().instanceOf(Type.QNM)) {
				throw new DocumentException("Element name is not an xs:QName: %s (%s)", value, value.type());
			}
			child = new ElementD2Node(this, division, (QNm) value);
		} else if (kind == Kind.TEXT) {
			child = new TextD2Node(this, division, value);
		} else if (kind == Kind.COMMENT) {
			if (!value.type().instanceOf(Type.STR)) {
				throw new DocumentException("Comment value is not an xs:string: %s (%s)", value, value.type());
			}
			child = new CommentD2Node(this, division, value.asStr());
		} else if (kind == Kind.PROCESSING_INSTRUCTION) {
			throw new DocumentException("Illegal node kind: %s", kind);
		} else {
			throw new DocumentException("Illegal node kind: %s", kind);
		}

		return child;
	}

	private D2Node insert(Stream<? extends Node<?>> scanner, D2Node sibling,
			boolean replace, boolean after) throws DocumentException {
		Node[] stack = new Node[5];
		int stackSize = 0;
		D2Node currentCopy = null;
		D2Node rootCopy = null;

		try {
			Node<?> next;
			while ((next = scanner.next()) != null) {
				if (currentCopy == null) {
					Kind kind = next.getKind();
					Atomic value = (kind == Kind.ELEMENT) ? next.getName()
							: next.getValue();

					if (sibling == null) {
						currentCopy = addChild(kind, value, after);
					} else if (replace) {
						currentCopy = replaceChild(sibling, kind, value);
					} else {
						currentCopy = insertChild(sibling, kind, value, after);
					}
					rootCopy = currentCopy;
				} else {
					while (!stack[stackSize - 1].isAncestorOf(next)) {
						if (stackSize == 1) {
							throw new DocumentException(
									"Found node %s is not a descendant of subtree root %s",
									next, stack[0]);
						}

						currentCopy = currentCopy.getParent();
						stackSize--;
					}

					if (!(currentCopy instanceof ParentD2Node)) {
						throw new DocumentException(
								"Cannot append node of type %s to node of type",
								next.getKind(), currentCopy.getKind());
					}

					currentCopy = append((ParentD2Node) currentCopy, next);
				}
				if (stackSize == stack.length) {
					stack = Arrays.copyOf(stack, ((stackSize * 3) / 2 + 1));
				}
				stack[stackSize++] = next;
			}
		} finally {
			scanner.close();
		}
		return rootCopy;
	}

	private D2Node append(ParentD2Node parent, Node<?> node)
			throws DocumentException {
		Kind kind = node.getKind();

		switch (kind) {
		case ELEMENT:
			return parent.addChild(kind, node.getName(), true);
		case TEXT:
			return parent.addChild(kind, node.getValue(), true);
		case ATTRIBUTE:
			return parent.setAttribute(node.getName(), node.getValue());
		case COMMENT:
			return parent.addChild(kind, node.getValue(), true);
		case PROCESSING_INSTRUCTION:
			return parent.addChild(kind, node.getValue(), true);
		default:
			throw new DocumentException("Unknown node type: %s", kind);
		}
	}

	@Override
	public D2Node append(Kind kind, Atomic value) throws DocumentException {
		return addChild(kind, value, true);
	}

	@Override
	public D2Node append(Node<?> child) throws DocumentException {
		return insert(child.getSubtree(), null, false, true);
	}

	@Override
	public D2Node append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node child = builder.build(parser);
		return append(child);
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
	public D2Node prepend(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return addChild(kind, value, false);
	}

	@Override
	public D2Node prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		return insert(child.getSubtree(), null, false, false);
	}

	@Override
	public D2Node prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node child = builder.build(parser);
		return prepend(child);
	}

	D2Node insertBefore(D2Node node, Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(node, kind, value, true);
	}

	D2Node insertBefore(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		return insert(child.getSubtree(), node, false, false);
	}

	D2Node insertBefore(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node child = builder.build(parser);
		return insertBefore(child);
	}

	D2Node insertAfter(D2Node node, Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return insertChild(node, kind, value, false);
	}

	D2Node insertAfter(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		return insert(child.getSubtree(), node, false, true);
	}

	D2Node insertAfter(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node child = builder.build(parser);
		return insertAfter(child);
	}

	D2Node replace(D2Node node, Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		return replaceChild(node, kind, value);
	}

	D2Node replace(D2Node node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		return insert(child.getSubtree(), node, true, false);
	}

	D2Node replace(D2Node node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node child = builder.build(parser);
		return replace(node, child);
	}
}