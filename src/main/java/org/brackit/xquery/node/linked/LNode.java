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

import java.util.concurrent.atomic.AtomicInteger;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.AbstractNode;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.node.stream.AtomStream;
import org.brackit.xquery.node.stream.EmptyStream;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.NamespaceScope;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Stream;

/**
 * Abstract base class for memory nodes.
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class LNode extends AbstractNode<LNode> {
	protected static final LNodeFactory builder = new LNodeFactory();

	protected static final AtomicInteger idSource = new AtomicInteger();

	protected final ParentLNode parent;

	protected LNode sibling;

	protected int localFragmentID = -1;

	protected LNode(ParentLNode parent) {
		if ((this.parent != null) && (this.parent != parent)) {
			throw new RuntimeException(String.format(
					"Node is already connected to parent node %s.", parent));
		}
		this.parent = parent;
		this.localFragmentID = (parent == null) ? localFragmentID
				: parent.localFragmentID;
		;
	}
	

	@Override
	public NamespaceScope getScope() {
		return ((parent != null) && (parent.getKind() == Kind.ELEMENT)) ? parent
				.getScope()
				: new LNSScope(null);
	}

	private LNode getRoot() {
		LNode parent = this;
		while (parent.parent != null) {
			parent = parent.parent;
		}
		return parent;
	}

	@Override
	public Collection<LNode> getCollection() {
		return (parent == null) ? null : getRoot().getCollection();
	}

	@Override
	public long getFragmentID() {
		return localFragmentID;
	}

	private int localFragmentID() {
		int localFragmentID = idSource.incrementAndGet();
		while (localFragmentID < 0) {
			if (idSource.compareAndSet(localFragmentID, 1)) {
				localFragmentID = 1;
				return localFragmentID;
			}
			localFragmentID = idSource.incrementAndGet();
		}
		return localFragmentID;
	}

	protected final int cmpInternal(final LNode node) {
		if (node == this)
			return 0;
		LNode c = null;
		LNode cp = this;
		while (cp != null) {
			LNode lcap = null;
			LNode lca = node;
			while (lca != null) {
				if (lca == cp) {
					// found least common ancestor
					// case 0: node == this is not allowed
					// case 1: lca is this
					if (lca == this)
						return -1;
					// case 2: lca is node
					if (lca == node)
						return 1;
					// case 3: c and lcap have the same parent
					Kind kind = c.getKind();
					Kind nkind = lcap.getKind();
					if ((kind == Kind.ATTRIBUTE) ^ (nkind == Kind.ATTRIBUTE)) {
						return (kind == Kind.ATTRIBUTE) ? -1 : 1;
					}
					// nodes must be in the same chain -> search
					// scan from context node to n
					LNode s = c;
					while ((s = s.sibling) != null) {
						if (s == lcap) {
							return -1;
						}
					}
					return 1;
				}
				lcap = lca;
				lca = lca.parent;
			}
			c = cp;
			cp = cp.parent;
		}
		return -1;
	}

	protected final boolean isInSubtreeOf(LNode n) {
		LNode a = parent;
		while (a != null) {
			if (a == n) {
				return true;
			}
			a = a.parent;
		}
		return false;
	}

	@Override
	public LNode setAttribute(Node<?> attribute)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode append(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode append(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode prepend(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode getAttribute(QNm name) throws DocumentException {
		return null;
	}

	@Override
	public String getAttributeValue(QNm name) throws DocumentException {
		return null;
	}

	@Override
	public Stream<LNode> getAttributes() throws OperationNotSupportedException,
			DocumentException {
		return new EmptyStream<LNode>();
	}

	@Override
	public Stream<LNode> getChildren() throws DocumentException {
		return new EmptyStream<LNode>();
	}

	@Override
	public LNode getFirstChild() throws DocumentException {
		return null;
	}

	@Override
	public LNode getLastChild() throws DocumentException {
		return null;
	}

	@Override
	public LNode getNextSibling() throws DocumentException {
		return null;
	}

	@Override
	public LNode getParent() throws DocumentException {
		return parent;
	}

	@Override
	public LNode getPreviousSibling() throws DocumentException {
		return null;
	}

	@Override
	public Stream<LNode> getSubtree() throws DocumentException {
		return new AtomStream<LNode>(this);
	}

	@Override
	public Stream<? extends LNode> getDescendantOrSelf()
			throws DocumentException {
		return new AtomStream<LNode>(this);
	}

	@Override
	public boolean hasAttributes() throws DocumentException {
		return false;
	}

	@Override
	public boolean hasChildren() throws DocumentException {
		return false;
	}

	@Override
	public final boolean isSelfOf(Node<?> node) {
		return (((Object) node) == this);
	}

	public boolean isAncestorOf(Node<?> node) {
		return false;
	}

	public boolean isAncestorOrSelfOf(Node<?> node) {
		// check only for self; overridden in parent node
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && ((this == (Object) node)));
	}

	@Override
	public boolean isAttributeOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isChildOf(Node<?> node) {
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && (parent == (Object) node));
	}

	@Override
	public boolean isDescendantOf(Node<?> node) {
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && ((parent == (Object) node) || ((parent != null) && (parent
				.isDescendantOf(node)))));
	}

	@Override
	public boolean isDescendantOrSelfOf(Node<?> node) {
		// TODO: fix for sun's compiler bug using generics parent == node
		return ((node != null) && ((this == (Object) node)
				|| (parent == (Object) node) || ((parent != null) && (parent
				.isDescendantOrSelfOf(node)))));
	}

	@Override
	public boolean isParentOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isFollowingOf(Node<?> node) {
		if ((node == null) || ((Object) node == this)
				|| (node.getFragmentID() != localFragmentID)
				|| (getKind() == Kind.ATTRIBUTE)) {
			return false;
		}
		LNode n = (LNode) node;
		if (cmpInternal(n) <= 0) {
			return false;
		}
		LNode c = this.parent;
		while (c != null) {
			if (c == n) {
				// n is ancestor
				return false;
			}
			c = c.parent;
		}
		return true;
	}

	@Override
	public boolean isFollowingSiblingOf(Node<?> node) {
		if ((node == null) || (parent == null) || ((Object) node == this)
				|| (node.getFragmentID() != localFragmentID)
				|| (node.getKind() == Kind.ATTRIBUTE)) {
			return false;
		}
		LNode n = (LNode) node;
		if (parent != n.parent) {
			return false;
		}
		LNode s = n.sibling;
		while (s != null) {
			if (s == this) {
				return true;
			}
			s = s.sibling;
		}
		return false;
	}

	@Override
	public boolean isPrecedingOf(Node<?> node) {
		if ((node == null) || ((Object) node == this)
				|| (node.getFragmentID() != localFragmentID)
				|| (getKind() == Kind.ATTRIBUTE)) {
			return false;
		}
		LNode n = (LNode) node;
		if (cmpInternal(n) >= 0) {
			return false;
		}
		n = n.parent;
		while (n != null) {
			if (n == this) {
				// n is ancestor
				return false;
			}
			n = n.parent;
		}
		return true;
	}

	@Override
	public boolean isPrecedingSiblingOf(Node<?> node) {
		if ((node == null) || (parent == null) || ((Object) node == this)
				|| (node.getFragmentID() != localFragmentID)
				|| (node.getKind() == Kind.ATTRIBUTE)) {
			return false;
		}
		LNode n = (LNode) node;
		if (parent != n.parent) {
			return false;
		}
		LNode s = sibling;
		while (s != null) {
			if (s == n) {
				return true;
			}
			s = s.sibling;
		}
		return false;
	}

	@Override
	public boolean isRoot() {
		return (parent == null);
	}

	public boolean isDocumentOf(Node<?> node) {
		return false;
	}

	@Override
	public boolean isSiblingOf(Node<?> node) {
		return (node != null)
				// TODO: fix for sun's compiler bug using generics parent ==
				// node
				&& ((Object) node != this) && (parent != null)
				&& (node.isChildOf(parent));
	}

	@Override
	public LNode prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertAfter(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertAfter(Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertAfter(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertBefore(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertBefore(Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode insertBefore(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode replaceWith(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode replaceWith(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public boolean deleteAttribute(QNm name)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public LNode setAttribute(QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void setName(QNm name) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void setValue(Atomic value) throws OperationNotSupportedException,
			DocumentException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void delete() throws DocumentException {
		if (parent != null) {
			if (this.getKind() == Kind.ATTRIBUTE) {
				parent.deleteAttribute(this.getName());
			} else {
				parent.deleteChild(this);
			}
		}
	}
}
