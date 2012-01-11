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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.node.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class NodeImpl implements org.w3c.dom.Node {
	protected short type;

	protected String name;

	protected String value;

	protected org.w3c.dom.Node parent;

	protected org.w3c.dom.Node nextSibling;

	protected org.w3c.dom.Node previousSibling;

	protected org.w3c.dom.Document document;

	protected List<org.w3c.dom.Node> children;

	protected Map<String, Attr> attributes;

	public NodeImpl(Document document, Node parent, short type, String name,
			String value) {
		super();
		this.document = document;
		this.parent = parent;
		this.type = type;
		this.name = name;
		this.value = value;
	}

	@Override
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild)
			throws DOMException {
		if (children == null) {
			children = new ArrayList<org.w3c.dom.Node>(3);
		}
		children.add(newChild);
		return newChild;
	}

	@Override
	public org.w3c.dom.Node cloneNode(boolean deep) {
		NodeImpl clone = null;

		try {
			clone = (NodeImpl) clone();

			if (deep) {
				if (children != null) {
					List<org.w3c.dom.Node> clonedChildren = new ArrayList<org.w3c.dom.Node>(
							children.size());

					for (org.w3c.dom.Node child : children) {
						clonedChildren.add(child.cloneNode(deep));
					}

					clone.children = clonedChildren;
				}

				if (attributes != null) {
					Map<String, Attr> clonedAttributes = new TreeMap<String, Attr>();

					for (Entry<String, Attr> attribute : attributes.entrySet()) {
						clonedAttributes.put(attribute.getKey(),
								(Attr) attribute.getValue().cloneNode(deep));
					}

					clone.attributes = clonedAttributes;
				}
			}
		} catch (CloneNotSupportedException e) {
		}

		return clone;
	}

	@Override
	public short compareDocumentPosition(org.w3c.dom.Node other)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public NamedNodeMap getAttributes() {
		return new org.brackit.xquery.node.dom.NamedNodeMapImpl(attributes);
	}

	@Override
	public String getBaseURI() {
		throw new RuntimeException();
	}

	@Override
	public NodeList getChildNodes() {
		return new org.brackit.xquery.node.dom.NodeListImpl(children);
	}

	@Override
	public Object getFeature(String feature, String version) {
		throw new RuntimeException();
	}

	@Override
	public org.w3c.dom.Node getFirstChild() {
		return (children != null) ? children.get(0) : null;
	}

	@Override
	public org.w3c.dom.Node getLastChild() {
		return (children != null) ? children.get(children.size() - 1) : null;
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public String getNamespaceURI() {
		throw new RuntimeException();
	}

	@Override
	public org.w3c.dom.Node getNextSibling() {
		return nextSibling;
	}

	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public short getNodeType() {
		return type;
	}

	@Override
	public String getNodeValue() throws DOMException {
		return value;
	}

	@Override
	public Document getOwnerDocument() {
		return document;
	}

	@Override
	public org.w3c.dom.Node getParentNode() {
		return parent;
	}

	@Override
	public String getPrefix() {
		throw new RuntimeException();
	}

	@Override
	public org.w3c.dom.Node getPreviousSibling() {
		return previousSibling;
	}

	@Override
	public String getTextContent() throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public Object getUserData(String key) {
		throw new RuntimeException();
	}

	@Override
	public boolean hasAttributes() {
		return (attributes != null);
	}

	@Override
	public boolean hasChildNodes() {
		return (children != null);
	}

	@Override
	public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild,
			org.w3c.dom.Node refChild) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		throw new RuntimeException();
	}

	@Override
	public boolean isEqualNode(org.w3c.dom.Node arg) {
		throw new RuntimeException();
	}

	@Override
	public boolean isSameNode(org.w3c.dom.Node other) {
		throw new RuntimeException();
	}

	@Override
	public boolean isSupported(String feature, String version) {
		throw new RuntimeException();
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		throw new RuntimeException();
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		throw new RuntimeException();
	}

	@Override
	public void normalize() {
		throw new RuntimeException();
	}

	@Override
	public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild,
			org.w3c.dom.Node oldChild) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		this.value = nodeValue;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		throw new RuntimeException();
	}

	@Override
	public String toString() {
		return String.format("%s(type=%s name=%s value=%s)", type, name, value);
	}
}