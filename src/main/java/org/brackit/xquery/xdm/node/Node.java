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
package org.brackit.xquery.xdm.node;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.node.parser.SubtreeHandler;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.Axis;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Scope;
import org.brackit.xquery.xdm.Stream;
import org.brackit.xquery.xdm.StructuredItem;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.type.NodeType;

/**
 * A {@link Node} defines the common interface of an XML node in the system.
 *
 * <p>
 * The API is designed to follow the general rules of the following standards:
 * <ul>
 * <li><b>XQuery and XPath Data Model 3.0</b></li>
 * <li><b>Namespaces in XML 1.1</b></li>
 * <li><b>XQuery Update Facility 1.0</b></li>
 * </ul>
 * </p>
 * <p>
 * Each document node has a unique immutable identity, but several instances of {@link Node Nodes}
 * in the system may represent the same (logical) XML node. Thus, the identity of a node must not be
 * checked with the <code>==</code> operator but only with {@link Node#isSelfOf(Node)} or
 * {@link Node#equals(Object)}. Implementors have also to ensure to ensure to override
 * {@link Object#equals(Object)} and {@link Object#hashCode()} appropriately.
 * </p>
 * <p>
 * Methods reading unsupported properties of nodes like {@link #getChildren()} for an
 * <em>attribute</em> do not throw an exception but return <code>null</code> or an empty
 * {@link Stream}.
 * </p>
 * <p>
 * Updates must not violate any of the consistency constraints defined in in the data model. This
 * includes checks for invalid tree structures (e.g. more than one root element), invalid values
 * (e.g. the character sequence "--" in comment values), and namespace violations (e.g. QName
 * prefixes not bound to a valid namespace URI).
 * </p>
 * <p>
 * Consistency checks are generally not expected to be performed by the caller. Implementors must
 * ensure to perform appropriate checks themselves. Note these checks do not include any validation
 * according to a specific schema. Schema validation is not considered in this API.
 * </p>
 * <p>
 * Attempts to modify unsupported properties of nodes like {@link #append(Kind, QNm, Atomic)} for an
 * <em>attribute</em> throw an {@link UnsupportedOperationException}.
 * </p>
 * <p>
 * <b>Insert operations</b> always insert deep copies of the provided nodes. This is a necessary
 * constraint because the parent relationship of existing nodes must only be set during node
 * creation and can be only unset with delete.
 * </p>
 * <p>
 * <b>Delete operations</b> break up the relationship between a a node and its parent. Logically,
 * this node and all its descendant nodes then become deleted, i.e., depending on the underlying
 * implementation, a delete operation might propagate a physical deletion of all ancestors. The
 * runtime behavior is unspecified when attempting to access logically deleted nodes.
 * </p>
 * <p>
 * <b>Update operations</b> modify an existing node but do not change its identity. Implementations
 * must ensure that updates are visible and consistent in all objects representing the same logical
 * XML node.
 * </p>
 * <p>
 * At runtime the system must be able to distinguish different implementations of this interface,
 * e.g., to establish a global ordering among nodes. For that, each implementation must be
 * identified by a system-wide unique node class ID. ID collisions will result in unexpected
 * behavior in many places. The node class ID of a node instance can be obtained by the method
 * {@link #getNodeClassID()}.
 * </p>
 * <p>
 * Users are responsible to ensure uniqueness of all node class IDs of the used node
 * implementations, but implementors are advised to cross-check existing implementations when
 * implementing a new node type to avoid collisions. Note that uniqueness must also be ensured
 * across JVM boundaries for distributed computations.
 * </p>
 *
 * @author Sebastian Baechle
 * @see http://www.w3.org/TR/xpath-datamodel-30/
 * @see http://www.w3.org/TR/2009/CR-xquery-update-10-20090609/
 * @see http://www.w3.org/TR/xml-names11/
 */
public interface Node<E extends Node<E>> extends StructuredItem {
  /**
   * Checks if this node is the document root node.
   *
   * @return {@code true}, iff the current node is the document root node, {@code false} otherwise
   */
  public boolean isDocumentRoot();

  /**
   * Checks if this node is the same as <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is the same as <code>node</code>
   */
  public boolean isSelfOf(Node<?> node);

  /**
   * Checks if this node is the parent node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is the parent of <code>node</code>
   */
  public boolean isParentOf(Node<?> node);

  /**
   * Checks if this node is a child node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a child of <code>node</code>
   */
  public boolean isChildOf(Node<?> node);

  /**
   * Checks if this node is a descendant node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a descendant of <code>node</code>
   */
  public boolean isDescendantOf(Node<?> node);

  /**
   * Checks if this node is a descendant or self node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a descendant or self of <code>node</code>
   */
  public boolean isDescendantOrSelfOf(Node<?> node);

  /**
   * Checks if this node is an ancestor node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is an ancestor of <code>node</code>
   */
  public boolean isAncestorOf(Node<?> node);

  /**
   * Checks if this node is an ancestor or self node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is an ancestor or self of <code>node</code>
   */
  public boolean isAncestorOrSelfOf(Node<?> node);

  /**
   * Checks if this node is a sibling node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a child of <code>node</code>
   */
  public boolean isSiblingOf(Node<?> node);

  /**
   * Checks if this node is a preceding sibling node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a preceding sibling of <code>node</code>
   */
  public boolean isPrecedingSiblingOf(Node<?> node);

  /**
   * Checks if this node is a following sibling node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a following sibling of <code>node</code>
   */
  public boolean isFollowingSiblingOf(Node<?> node);

  /**
   * Checks if this node is a preceding node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a preceding of <code>node</code>
   */
  public boolean isPrecedingOf(Node<?> node);

  /**
   * Checks if this node is a following node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is a following of <code>node</code>
   */
  public boolean isFollowingOf(Node<?> node);

  /**
   * Checks if this node is an attribute node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is an attribute of <code>node</code>
   */
  public boolean isAttributeOf(Node<?> node);

  /**
   * Checks if this node is the document node of <code>node</code>.
   *
   * @param node the node to be checked
   * @return <code>true</code> iff the current node is the document node of <code>node</code>
   */
  public boolean isDocumentOf(Node<?> node);

  /**
   * Checks if this node is the root element node of a document.
   *
   * @return <code>true</code> iff this node is the root element node of the document
   */
  public boolean isRoot();

  /**
   * Identifier for a particular implementation of the {@link Node} interface. Identifiers must be
   * ensure to be system-wide unique, e.g., for global ordering. Note that uniqueness must also be
   * ensured across JVM boundaries for distributed computations. ID collisions will result in
   * unexpected behavior in many places.
   */
  public int getNodeClassID();

  /**
   * Compares this node to another w.r.t. to a global document order as indicated by the fragment IDs
   * and their structural relationship
   */
  public int cmp(Node<?> other);

  /**
   * Returns the {@link NodeCollection} of this document, and <code>null</code> if this node not
   * associated with a document.
   *
   * @return the collection associated with this node
   */
  public NodeCollection<E> getCollection();

  /**
   * Returns the {@link Scope} for this node.
   *
   * @return the {@link Scope} for this node
   */
  public Scope getScope();

  /**
   * Returns the {@link AnyURI base URI} type of this node.
   *
   * <p>
   * Realizes the dm:base-uri accessor.
   * </p>
   *
   * @return the base URI of this node
   */
  public AnyURI getBaseURI();

  /**
   * Returns the {@link Type} type of this node.
   *
   * <p>
   * Realizes the dm:type-name accessor.
   * </p>
   *
   * @return the type of this node
   */
  public Type type();

  /**
   * Returns the {@link Kind} type of this node.
   *
   * <p>
   * Realizes the dm:node-kind accessor.
   * </p>
   *
   * @return the kind of this node
   */
  public Kind getKind();

  /**
   * Returns the name of this node, and <code>null</code> if this type of node has no name.
   *
   * <p>
   * Realizes the dm:node-name accessor.
   * </p>
   *
   * @return name of this node, and <code>null</code> if this type of node has no name
   * @throws DocumentException if the operation failed
   */
  public QNm getName() throws DocumentException;

  /**
   * Sets the name of this node to <code>name</code>.
   *
   * @param name the new name
   * @throws OperationNotSupportedException if the operation is not supported by this type of node
   * @throws DocumentException              if the operation failed
   */
  public void setName(QNm name) throws OperationNotSupportedException, DocumentException;

  /**
   * Returns the typed value of this node, and <code>null</code> if this type of node has no value.
   *
   * <p>
   * For nodes created from an Infoset, the type of the returned value is as follows:
   * <table border="1">
   * <th>
   * <tr>
   * <td><b>node-kind</b></td>
   * <td><b>type of typed-value</b></td>
   * </tr>
   * </th>
   * <tr>
   * <td>{@link Kind#DOCUMENT DOCUMENT}</td>
   * <td>{@link Type#UNA xs:untypedAtomic}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#ELEMENT ELEMENT}</td>
   * <td>{@link Type#UNA xs:untypedAtomic}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#ATTRIBUTE ATTRIBUTE}</td>
   * <td>{@link Type#UNA xs:untypedAtomic}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#NAMESPACE NAMESPACE}</td>
   * <td>{@link Type#UNA xs:untypedAtomic}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#PROCESSING_INSTRUCTION PROCESSING_INSTRUCTION}</td>
   * <td>{@link Type#STR xs:string}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#COMMENT COMMENT}</td>
   * <td>{@link Type#STR xs:string}</td>
   * </tr>
   * <tr>
   * <td>{@link Kind#TEXT TEXT}</td>
   * <td>{@link Type#UNA xs:untypedAtomic}</td>
   * </tr>
   * </table>
   * </p>
   *
   * <p>
   * Realizes the dm:typed-value accessor.
   * </p>
   *
   * <p>
   * Note, this method is not intended to work with nodes created from a PSVI (schema validated
   * document) where the typed-value property of a node may be a sequence of zero or more items. For
   * documents with a schema use {@link Node#getValues()}.
   * </p>
   *
   * @return value of this node, and <code>null</code> if this type of node has no value
   * @throws DocumentException if the operation failed
   * @see http://www.w3.org/TR/xml-infoset/
   */
  public Atomic getValue() throws DocumentException;

  /**
   * Returns the typed values of this node, and an empty stream if this type of node has no typed
   * value.
   *
   * <p>
   * Realizes the dm:typed-value accessor.
   * </p>
   *
   * @return typed values of this node, and an empty stream if this type of node has no typed value
   * @throws DocumentException if the operation failed
   */
  public Stream<Atomic> getValues() throws DocumentException;

  /**
   * Returns the string value of this node, and <code>null</code> if this type of node has no value.
   *
   * <p>
   * Realizes the dm:string-value accessor.
   * </p>
   *
   * @return value of this node, and <code>null</code> if this type of node has no value
   * @throws DocumentException if the operation failed
   */
  public Str getStrValue() throws DocumentException;

  /**
   * Sets the value of this node to <code>value</code>.
   *
   * @param value the new value
   * @throws OperationNotSupportedException if the operation is not supported by this type of node
   * @throws DocumentException              if the operation failed
   */
  public void setValue(Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Returns the parent node, and <code>null</code> if this node has no parent.
   *
   * <p>
   * Realizes the dm:parent accessor.
   * </p>
   *
   * @return the parent node, and <code>null</code> if this node has no parent
   * @throws DocumentException if the operation failed
   */
  public E getParent() throws DocumentException;

  /**
   * Returns the first child node, and <code>null</code> if this node has no children.
   *
   * @return the first child node, and <code>null</code> if this node has no children
   * @throws DocumentException if the operation failed
   */
  public E getFirstChild() throws DocumentException;

  /**
   * Returns the last child node, and <code>null</code> if this node has no children.
   *
   * @return the last child node, and <code>null</code> if this node has no children
   * @throws DocumentException if the operation failed
   */
  public E getLastChild() throws DocumentException;

  /**
   * Returns all children, and an empty {@link Stream} if this node has no children.
   *
   * <p>
   * Realizes the dm:children accessor.
   * </p>
   *
   * @return all children, and an empty {link Stream} if this node has no children
   * @throws DocumentException if the operation failed
   */
  public Stream<? extends E> getChildren() throws DocumentException;

  /**
   * Returns a {@link Stream} over all nodes in the subtree rooted a this current node in document
   * order
   *
   * @return all nodes in the subtree rooted a this current node in document order
   * @throws DocumentException if the operation failed
   */
  public Stream<? extends E> getSubtree() throws DocumentException;

  /**
   * Returns a {@link Stream} over all descendant nodes in the subtree rooted a this current node in
   * document order
   *
   * @return all descendant nodes in the subtree rooted a this current node in document order
   * @throws DocumentException if the operation failed
   */
  public Stream<? extends E> getDescendantOrSelf() throws DocumentException;

  /**
   * Returns a {@link Stream} over all nodes on the path starting at this node up to the root
   *
   * @return all nodes in the subtree rooted a this current node in document order
   * @throws DocumentException if the operation failed
   */
  public Stream<? extends E> getPath() throws DocumentException;

  /**
   * Checks if this node has children.
   *
   * @return <code>true</code> iff this node has children
   * @throws DocumentException if the operation failed
   */
  public boolean hasChildren() throws DocumentException;

  /**
   * Returns the next sibling node, and <code>null</code> if this node has no next sibling.
   *
   * @return the next sibling node, and <code>null</code> if this node has no next sibling
   * @throws DocumentException if the operation failed
   */
  public E getNextSibling() throws DocumentException;

  /**
   * Returns the previous sibling node, and <code>null</code> if this node has no previous sibling.
   *
   * @return the previous sibling node, and <code>null</code> if this node has no previous sibling
   * @throws DocumentException if the operation failed
   */
  public E getPreviousSibling() throws DocumentException;

  /**
   * Appends a new node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E append(Kind kind, QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Appends a deep copy of the given node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E append(Node<?> child) throws OperationNotSupportedException, DocumentException;

  /**
   * Appends a copy of the parsed fragment.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E append(SubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  /**
   * Prepends a new node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E prepend(Kind kind, QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Prepends a deep copy of the given node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E prepend(Node<?> child) throws OperationNotSupportedException, DocumentException;

  /**
   * Prepends a copy of the parsed fragment.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E prepend(SubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a new node before this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertBefore(Kind kind, QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a deep copy of the given node before this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertBefore(Node<?> node) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a copy of the parsed fragment before this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertBefore(SubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a new node after this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertAfter(Kind kind, QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a deep copy of the given node after this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertAfter(Node<?> node) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a copy of the parsed fragment after this node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E insertAfter(SubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  /**
   * Inserts a copy of the given node as attribute.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E setAttribute(Node<?> attribute) throws OperationNotSupportedException, DocumentException;

  /**
   * Creates or updates an attribute with given name and the given value.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E setAttribute(QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Deletes the attribute.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public boolean deleteAttribute(QNm name) throws OperationNotSupportedException, DocumentException;

  /**
   * Returns all attributes, and an empty {@link Stream} if this node has no attributes.
   *
   * <p>
   * Realizes the dm:attributes accessor.
   * </p>
   *
   * @return all attributes, and an empty {link Stream} if this node has no attributes
   * @throws DocumentException if the operation failed
   */
  public Stream<? extends E> getAttributes() throws OperationNotSupportedException, DocumentException;

  /**
   * Returns the attribute with the given name.
   *
   * @return the attribute with the given name.
   * @throws DocumentException if the operation failed
   */
  public E getAttribute(QNm name) throws DocumentException;

  /**
   * Replaces this node with a deep copy of the given node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E replaceWith(Node<?> node) throws OperationNotSupportedException, DocumentException;

  /**
   * Replaces this node with a copy of the parsed fragment.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E replaceWith(SubtreeParser parser) throws OperationNotSupportedException, DocumentException;

  /**
   * Replaces this node with a new node.
   *
   * @return the newly created node
   * @throws OperationNotSupportedException if this operation is not supported
   * @throws DocumentException              if the operation failed
   */
  public E replaceWith(Kind kind, QNm name, Atomic value) throws OperationNotSupportedException, DocumentException;

  /**
   * Checks if this node has attributes.
   *
   * @return <code>true</code> iff this node has attributes
   * @throws DocumentException if the operation failed
   */
  public boolean hasAttributes() throws DocumentException;

  /**
   * Delete this node.
   *
   * @throws DocumentException if the operation failed
   */
  public void delete() throws DocumentException;

  /**
   * Parse the subtree rooted at this node.
   *
   * @throws DocumentException if the operation or the handler failed
   */
  public void parse(SubtreeHandler handler) throws DocumentException;

  /**
   * Try to pushdown navigation to node
   */
  public Stream<? extends Node<?>> performStep(Axis axis, NodeType test) throws DocumentException;
}
