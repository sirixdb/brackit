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

import java.util.Arrays;

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
abstract class ParentLNode extends LNode {
	protected LNode firstChild;

	protected class SiblingStream implements Stream<LNode> {
		LNode node;

		SiblingStream(LNode first) {
			node = first;
		}

		@Override
		public void close() {
			node = null;
		}

		@Override
		public LNode next() throws DocumentException {
			if (node == null) {
				return null;
			}
			LNode deliver = node;
			node = node.sibling;
			return deliver;
		}
	}

	private final class FragmentScanner implements Stream<LNode> {
		LNode current;

		LNode first;

		LNode root;

		boolean inAttribute;

		public FragmentScanner(LNode root) {
			this.root = root;
			current = root;
			first = root;
		}

		@Override
		public void close() {
			current = null;
		}

		@Override
		public LNode next() throws DocumentException {
			if (current == null) {
				return null;
			}
			if (first != null) {
				LNode deliver = first;
				first = null;
				return deliver;
			}
			LNode next;
			if ((current instanceof ElementLNode)
					&& ((next = ((ElementLNode) current).firstAttribute) != null)) // try
			// to
			// descend
			// to
			// attribute
			{
				inAttribute = true;
				current = next;
				return next;
			}

			if ((current instanceof ParentLNode)
					&& ((next = ((ParentLNode) current).firstChild) != null)) // try
			// to
			// descend
			// to
			// subtree
			{
				current = next;
				return next;
			}

			while ((current != root) && (current != null)) {
				if ((next = current.sibling) != null) // try to switch to
				// sibling of descendant
				{
					current = next;
					return next;
				}
				current = current.parent;
				if (inAttribute) {
					inAttribute = false;
					if ((next = ((ParentLNode) current).firstChild) != null) {
						current = next;
						return next;
					}
				}
			}
			return null;
		}
	}

	private final class DescendantScanner implements Stream<LNode> {
		LNode current;

		LNode root;

		boolean first;

		public DescendantScanner(LNode root) {
			current = root;
			first = true;
			this.root = root;
		}

		@Override
		public void close() {
			current = null;
		}

		@Override
		public LNode next() throws DocumentException {
			if (first) {
				first = false;
				return root;
			}
			LNode next;
			if ((current instanceof ParentLNode)
					&& ((next = ((ParentLNode) current).firstChild) != null)) // try
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
	public Stream<? extends LNode> getDescendantOrSelf()
			throws DocumentException {
		return new DescendantScanner(this);
	}

	public Stream<LNode> getSubtree() throws DocumentException {
		return new FragmentScanner(this);
	}

	protected ParentLNode() {
		super(null);
	}

	protected ParentLNode(ParentLNode parent) {
		super(parent);
	}

	protected boolean hasAttribute(LNode attribute) {
		return false;
	}

	@Override
	public String getValue() throws DocumentException {
		StringBuffer buffer = new StringBuffer();
		Stream<LNode> scanner = new DescendantScanner(this);

		LNode descendant;
		while ((descendant = scanner.next()) != null) {
			if (descendant.getKind() == Kind.TEXT) {
				buffer.append(descendant.getValue());
			}
		}
		scanner.close();

		return buffer.toString();
	}

	LNode nextSiblingOf(LNode node) {
		return node.sibling;
	}

	LNode previousSiblingOf(LNode node) {
		if (node == firstChild) {
			return null;
		}
		for (LNode child = firstChild; child != null; child = child.sibling) {
			if (child.sibling == node) {
				return child;
			}
		}
		return null;
	}

	void deleteChild(LNode node) {
		LNode prev = previousSiblingOf(node);
		if (prev == null)
			firstChild = node.sibling;
		else
			prev.sibling = node.sibling;
	}

	private LNode addChild(LNode child, boolean append)
			throws DocumentException {
		Kind kind = child.getKind();
		if (((kind == Kind.ATTRIBUTE) || (kind == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					kind);
		}

		if (append) {
			if (firstChild == null) {
				return (firstChild = child);
			}

			LNode previous = firstChild;
			while (previous.sibling != null)
				previous = previous.sibling;

			if ((kind == Kind.TEXT) && (previous != null)
					&& (previous.getKind() == Kind.TEXT)) {
				((TextLNode) previous).value = ((TextLNode) previous).value
						+ ((TextLNode) child).value;
				return previous;
			}
			previous.sibling = child;
		} else {
			LNode first = firstChild;
			if ((kind == Kind.TEXT) && (first != null)
					&& (first.getKind() == Kind.TEXT)) {
				((TextLNode) first).value = ((TextLNode) child).value
						+ ((TextLNode) first).value;
				return first;
			}
			child.sibling = first;
			firstChild = sibling;
		}
		return child;
	}

	private LNode insertChild(LNode sibling, LNode child, boolean before)
			throws DocumentException {
		Kind kind = child.getKind();
		if (((kind == Kind.ATTRIBUTE) || (kind == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					kind);
		}

		if (firstChild == null) {
			firstChild = child;
		} else if (before) {
			LNode previous = null;			
			if (firstChild != sibling) {
				previous = firstChild;
				while ((previous.sibling != null) && (previous.sibling != sibling))
					previous = previous.sibling;
			};

			if (kind == Kind.TEXT) {
				if ((previous != null) && (previous.getKind() == Kind.TEXT)) {
					((TextLNode) previous).value = ((TextLNode) previous).value
							+ ((TextLNode) child).value;
					return previous;
				}
				if ((sibling != null) && (sibling.getKind() == Kind.TEXT)) {
					((TextLNode) sibling).value = ((TextLNode) sibling).value
							+ ((TextLNode) child).value;
					return sibling;
				}
			}
			child.sibling = sibling;
			if (previous != null) {
				previous.sibling = child;
			}
		} else {
			if (kind == Kind.TEXT) {
				if ((sibling != null) && (sibling.getKind() == Kind.TEXT)) {
					((TextLNode) sibling).value = ((TextLNode) sibling).value
							+ ((TextLNode) child).value;
					return sibling;
				}
				if ((sibling.sibling != null)
						&& (sibling.sibling.getKind() == Kind.TEXT)) {
					((TextLNode) sibling.sibling).value = ((TextLNode) sibling.sibling).value
							+ ((TextLNode) child).value;
					return sibling.sibling;
				}
			}
			child.sibling = sibling.sibling;
			child.sibling = sibling;
		}
		return child;
	}

	private LNode replaceChild(LNode sibling, LNode child)
			throws DocumentException {
		if (((child.getKind() == Kind.ATTRIBUTE) || (child.getKind() == Kind.DOCUMENT))) {
			throw new DocumentException("Cannot add children of type '%s'",
					child.getKind());
		}

		LNode previous = firstChild;
		while ((previous.sibling != null) && (previous.sibling != sibling))
			previous = previous.sibling;

		child.sibling = sibling.sibling;

		if (previous != null)
			previous.sibling = child;
		else
			firstChild = child;
		return child;
	}

	private LNode buildChild(Kind kind, String value) throws DocumentException {
		LNode child;

		if (kind == Kind.ELEMENT) {
			child = new ElementLNode(this, value);
		} else if (kind == Kind.TEXT) {
			child = new TextLNode(this, value);
		} else if (kind == Kind.COMMENT) {
			child = new CommentLNode(this, value);
		} else if (kind == Kind.PROCESSING_INSTRUCTION) {
			throw new DocumentException("Illegal node type: %s", kind);
		} else {
			throw new DocumentException("Illegal node type: %s", kind);
		}

		return child;
	}

	private LNode buildChild(Node<?> node) throws DocumentException {
		Kind kind = node.getKind();

		switch (kind) {
		case ELEMENT:
			return new ElementLNode(this, node.getName());
		case TEXT:
			return new TextLNode(this, node.getValue());
		case COMMENT:
			return new CommentLNode(this, node.getValue());
		case PROCESSING_INSTRUCTION:
			return new PILNode(this, node.getName(), node.getValue());
		default:
			throw new DocumentException("Illegal child node type: %s", kind);
		}
	}

	private LNode buildChild(Stream<? extends Node<?>> scanner)
			throws DocumentException {
		Node[] stack = new Node[5];
		int stackSize = 0;
		LNode currentCopy = null;
		LNode rootCopy = null;

		try {
			Node<?> next;
			while ((next = scanner.next()) != null) {
				if (currentCopy == null) {
					currentCopy = buildChild(next);
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

					if (!(currentCopy instanceof ParentLNode)) {
						throw new DocumentException(
								"Cannot append node of type %s to node of type",
								next.getKind(), currentCopy.getKind());
					}

					currentCopy = append((ParentLNode) currentCopy, next);
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

	private LNode append(ParentLNode parent, Node<?> node)
			throws DocumentException {
		Kind kind = node.getKind();

		switch (kind) {
		case ELEMENT:
			return parent.addChild(new ElementLNode(parent, node.getName()),
					true);
		case TEXT:
			return parent
					.addChild(new TextLNode(parent, node.getValue()), true);
		case ATTRIBUTE:
			return parent.setAttribute(node.getName(), node.getValue());
		case COMMENT:
			return parent.addChild(new CommentLNode(parent, node.getValue()),
					true);
		case PROCESSING_INSTRUCTION:
			return parent.addChild(new PILNode(parent, node.getName(), node
					.getValue()), true);
		default:
			throw new DocumentException("Unknown node type: %s", kind);
		}
	}

	@Override
	public LNode append(Kind kind, String value) throws DocumentException {
		LNode child = buildChild(kind, value);
		return addChild(child, true);
	}

	@Override
	public LNode append(Node<?> child) throws DocumentException {
		LNode newChild = (child.getKind() == Kind.ELEMENT) ? buildChild(child
				.getSubtree()) : buildChild(child);
		return addChild(newChild, true);
	}

	@Override
	public LNode append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode child = builder.build(parser);
		return append(child);
	}

	@Override
	public Stream<LNode> getChildren() throws DocumentException {
		if (firstChild == null) {
			return new EmptyStream<LNode>();
		}
		return new SiblingStream(firstChild);
	}

	@Override
	public LNode getFirstChild() throws DocumentException {
		return firstChild;
	}

	@Override
	public LNode getLastChild() throws DocumentException {
		if (firstChild == null) {
			return null;
		}
		LNode child = firstChild;
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
				&& ((node instanceof LNode) && (((LNode) node)
						.isInSubtreeOf(this)));
	}

	@Override
	public boolean isAncestorOrSelfOf(Node<?> node) {
		return (node != null)
				&& ((Object) node == this)
				|| ((node instanceof LNode) && (((LNode) node)
						.isInSubtreeOf(this)));
	}

	@Override
	public boolean isParentOf(Node<?> node) {
		return (node != null)
				&& (((node.isChildOf(this)) || (node.isAttributeOf(this))));
	}

	@Override
	public LNode prepend(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException {
		LNode child = buildChild(kind, value);
		return addChild(child, false);
	}

	@Override
	public LNode prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		LNode newChild = (child.getKind() == Kind.ELEMENT) ? buildChild(child
				.getSubtree()) : buildChild(child);
		return addChild(newChild, false);
	}

	@Override
	public LNode prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode child = builder.build(parser);
		return prepend(child);
	}

	LNode insertBefore(LNode node, Kind kind, String value)
			throws OperationNotSupportedException, DocumentException {
		LNode child = buildChild(kind, value);
		return insertChild(node, child, true);
	}

	LNode insertBefore(LNode node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		LNode newChild = (child.getKind() == Kind.ELEMENT) ? buildChild(child
				.getSubtree()) : buildChild(child);
		return insertChild(node, newChild, true);
	}

	LNode insertBefore(LNode node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode child = builder.build(parser);
		return insertBefore(node, child);
	}

	LNode insertAfter(LNode node, Kind kind, String value)
			throws OperationNotSupportedException, DocumentException {
		LNode child = buildChild(kind, value);
		return insertAfter(node, child);
	}

	LNode insertAfter(LNode node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		LNode newChild = (child.getKind() == Kind.ELEMENT) ? buildChild(child
				.getSubtree()) : buildChild(child);
		return insertChild(node, newChild, false);
	}

	LNode insertAfter(LNode node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode child = builder.build(parser);
		return insertAfter(node, child);
	}

	LNode replace(LNode node, Kind kind, String value)
			throws OperationNotSupportedException, DocumentException {
		LNode child = buildChild(kind, value);
		return replaceChild(node, child);
	}

	LNode replace(LNode node, Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		LNode newChild = buildChild(child);
		return replaceChild(node, newChild);
	}
	
	LNode replaceDirect(LNode node, LNode child)
	throws OperationNotSupportedException, DocumentException {
		return replaceChild(node, child);
	}

	LNode replace(LNode node, SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		LNode child = builder.build(parser);
		return replaceChild(node, child);
	}
}