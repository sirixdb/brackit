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
package org.brackit.xquery.xdm;

import org.brackit.xquery.node.parser.SubtreeParser;

/**
 * A {@link Node} defines the common interface of an XML node in the system.
 * 
 * <p>
 * The API is designed to follow the general rules of the {@linkplain http 
 * ://www.w3.org/TR/2007/REC-xpath-datamodel-20070123/ XQuery 1.0 and XPath 2.0
 * Data Model (XDM)} and the {@link http
 * ://www.w3.org/TR/2009/CR-xquery-update-10-20090609/ XQuery Update Facility
 * 1.0}: <br/>
 * <ul>
 * <li>
 * Each document node has a unique immutable identity, but several instances of
 * {@link Node Nodes} in the system may represent the same (logical) document
 * node. Thus, the identity of a node must not be checked with the
 * <code>==</code> operator but only with {@link Node#equals(Object)}.</li>
 * <li>
 * Methods reading unsupported properties of nodes like, e.g. <i>children</i>
 * for an <em>attribute</em> do not throw an exception but return
 * <code>null</code> or empty {@link Stream Streams}.</li>
 * <li>
 * Attempts to modify unsupported properties of nodes like, e.g. <i>children</i>
 * for an <em>attribute</em> throw an {@link UnsupportedOperationException}.</li>
 * <li>
 * Insert operations always insert deep copies of the provided nodes. This is a
 * necessary constraint because the parent relationship of existing nodes must
 * only be set during node creation and can only unset with delete.
 * </li>
 * <li>
 * Delete operations break up the relationship between a a node and its parent.
 * Logically, this node and all its descendant nodes then become deleted, i.e.,
 * depending on the underlying implementation, a delete operation might
 * propagate a physical deletion of all ancestors. The runtime behavior is
 * unspecified when attempting to access logically deleted nodes.</li>
 * </ul>
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
public interface Node<E extends Node<E>> extends Item {
	/**
	 * Checks if this node is the same as <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is the same as
	 *         <code>node</code>
	 */
	public boolean isSelfOf(Node<?> node);

	/**
	 * Checks if this node is the parent node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is the parent of
	 *         <code>node</code>
	 */
	public boolean isParentOf(Node<?> node);

	/**
	 * Checks if this node is a child node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a child of
	 *         <code>node</code>
	 */
	public boolean isChildOf(Node<?> node);

	/**
	 * Checks if this node is a descendant node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a descendant of
	 *         <code>node</code>
	 */
	public boolean isDescendantOf(Node<?> node);

	/**
	 * Checks if this node is a descendant or self node of <code>node</code>.
	 * 
	 * @param rootNode
	 * @return <code>true</code> iff the current node is a descendant or self of
	 *         <code>node</code>
	 */
	public boolean isDescendantOrSelfOf(Node<?> node);

	/**
	 * Checks if this node is an ancestor node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is an ancestor of
	 *         <code>node</code>
	 */
	public boolean isAncestorOf(Node<?> node);

	/**
	 * Checks if this node is an ancestor or self node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is an ancestor or self of
	 *         <code>node</code>
	 */
	public boolean isAncestorOrSelfOf(Node<?> peek);

	/**
	 * Checks if this node is a sibling node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a child of
	 *         <code>node</code>
	 */
	public boolean isSiblingOf(Node<?> node);

	/**
	 * Checks if this node is a preceding sibling node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a preceding sibling of
	 *         <code>node</code>
	 */
	public boolean isPrecedingSiblingOf(Node<?> node);

	/**
	 * Checks if this node is a following sibling node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a following sibling of
	 *         <code>node</code>
	 */
	public boolean isFollowingSiblingOf(Node<?> node);

	/**
	 * Checks if this node is a preceding node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a preceding of
	 *         <code>node</code>
	 */
	public boolean isPrecedingOf(Node<?> node);

	/**
	 * Checks if this node is a following node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is a following of
	 *         <code>node</code>
	 */
	public boolean isFollowingOf(Node<?> node);

	/**
	 * Checks if this node is an attribute node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is an attribute of
	 *         <code>node</code>
	 */
	public boolean isAttributeOf(Node<?> node);

	/**
	 * Checks if this node is the document node of <code>node</code>.
	 * 
	 * @param node
	 *            the node to be checked
	 * @return <code>true</code> iff the current node is the document node of
	 *         <code>node</code>
	 */
	public boolean isDocumentOf(Node<?> node);

	/**
	 * Checks if this node is the root element node of a document.
	 * 
	 * @return <code>true</code> iff this node is the root element node of the
	 *         document
	 */
	public boolean isRoot();

	/**
	 * Returns a system-wide unique identifying the fragment of a node to use
	 * for global ordering.
	 */
	public long getFragmentID();

	/**
	 * Compares this node to another w.r.t. to a global document order as
	 * indicated by the fragment IDs and their structural relationship
	 */
	public int cmp(Node<?> other);

	/**
	 * Returns the {@link Collection} of this document, and <code>null</code> if
	 * this node not associated with a document.
	 * 
	 * @return the collection associated with this node
	 */
	public Collection<E> getCollection();

	/**
	 * Returns the {@link Kind} type of this node.
	 * 
	 * @return the kind of this node
	 */
	public Kind getKind();

	/**
	 * Returns the name of this node, and <code>null</code> if this type of node
	 * has no name.
	 * 
	 * @return name of this node, and <code>null</code> if this type of node has
	 *         no name
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public String getName() throws DocumentException;

	/**
	 * Sets the name of this node to <code>name</code>.
	 * 
	 * @param name
	 *            the new name
	 * @throws OperationNotSupportedException
	 *             if the operation is not supported by this type of node
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public void setName(String name) throws OperationNotSupportedException,
			DocumentException;

	/**
	 * Returns the value of this node, and <code>null</code> if this type of
	 * node has no value.
	 * 
	 * @return value of this node, and <code>null</code> if this type of node
	 *         has no value
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public String getValue() throws DocumentException;

	/**
	 * Sets the value of this node to <code>value</code>.
	 * 
	 * @param value
	 *            the new value
	 * @throws OperationNotSupportedException
	 *             if the operation is not supported by this type of node
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public void setValue(String value) throws OperationNotSupportedException,
			DocumentException;

	/**
	 * Returns the parent node, and <code>null</code> if this node has no
	 * parent.
	 * 
	 * @return the parent node, and <code>null</code> if this node has no parent
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public E getParent() throws DocumentException;

	/**
	 * Returns the first child node, and <code>null</code> if this node has no
	 * children.
	 * 
	 * @return the first child node, and <code>null</code> if this node has no
	 *         children
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public E getFirstChild() throws DocumentException;

	/**
	 * Returns the last child node, and <code>null</code> if this node has no
	 * children.
	 * 
	 * @return the last child node, and <code>null</code> if this node has no
	 *         children
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public E getLastChild() throws DocumentException;

	/**
	 * Returns all children, and an empty {@link Stream} if this node has no
	 * children.
	 * 
	 * @return all children, and an empty {link Stream} if this node has no
	 *         children
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public Stream<? extends E> getChildren() throws DocumentException;

	/**
	 * Returns a {@link Stream} over all nodes in the subtree rooted a this
	 * current node in document order
	 * 
	 * @return all nodes in the subtree rooted a this current node in document
	 *         order
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public Stream<? extends E> getSubtree() throws DocumentException;

	/**
	 * Returns a {@link Stream} over all descendant nodes in the subtree rooted
	 * a this current node in document order
	 * 
	 * @return all descendant nodes in the subtree rooted a this current node in
	 *         document order
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public Stream<? extends E> getDescendantOrSelf() throws DocumentException;

	/**
	 * Returns a {@link Stream} over all nodes on the path starting at this node
	 * up to the root
	 * 
	 * @return all nodes in the subtree rooted a this current node in document
	 *         order
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public Stream<? extends E> getPath() throws DocumentException;

	/**
	 * Checks if this node has children.
	 * 
	 * @return <code>true</code> iff this node has children
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public boolean hasChildren() throws DocumentException;

	/**
	 * Returns the next sibling node, and <code>null</code> if this node has no
	 * next sibling.
	 * 
	 * @return the next sibling node, and <code>null</code> if this node has no
	 *         next sibling
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public E getNextSibling() throws DocumentException;

	/**
	 * Returns the previous sibling node, and <code>null</code> if this node has
	 * no previous sibling.
	 * 
	 * @return the previous sibling node, and <code>null</code> if this node has
	 *         no previous sibling
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public E getPreviousSibling() throws DocumentException;

	/**
	 * Appends a new node
	 * 
	 * @param kind
	 * @param value
	 * @return
	 * @throws OperationNotSupportedException
	 *             if this
	 * @throws DocumentException
	 */
	public E append(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException;

	public E append(Node<?> child) throws OperationNotSupportedException,
			DocumentException;

	public E append(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException;

	public E prepend(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException;

	public E prepend(Node<?> child) throws OperationNotSupportedException,
			DocumentException;

	public E prepend(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException;

	public E insertBefore(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException;

	public E insertBefore(Node<?> node) throws OperationNotSupportedException,
			DocumentException;

	public E insertBefore(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException;

	public E insertAfter(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException;

	public E insertAfter(Node<?> node) throws OperationNotSupportedException,
			DocumentException;

	public E insertAfter(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException;

	public E setAttribute(Node<?> attribute)
			throws OperationNotSupportedException, DocumentException;

	public E setAttribute(String name, String value)
			throws OperationNotSupportedException, DocumentException;

	public boolean deleteAttribute(String name)
			throws OperationNotSupportedException, DocumentException;

	public Stream<? extends E> getAttributes()
			throws OperationNotSupportedException, DocumentException;

	public String getAttributeValue(String name) throws DocumentException;

	public E getAttribute(String name) throws DocumentException;

	public E replaceWith(Node<?> node) throws OperationNotSupportedException,
			DocumentException;

	public E replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException;

	public E replaceWith(Kind kind, String value)
			throws OperationNotSupportedException, DocumentException;

	/**
	 * Checks if this node has attributes.
	 * 
	 * @return <code>true</code> iff this node has attributes
	 * @throws DocumentException
	 *             if the operation failed
	 */
	public boolean hasAttributes() throws DocumentException;

	public void delete() throws DocumentException;
}
