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

import java.util.TreeMap;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ElementImpl extends NodeImpl implements org.w3c.dom.Element {
	public ElementImpl(Document document, Node parent, String name, String value) {
		super(document, parent, Node.ELEMENT_NODE, name, value);
	}

	@Override
	public String getAttribute(String name) {
		String value = null;

		if (attributes != null) {
			Attr attribute = attributes.get(name);
			if (attribute != null) {
				value = attribute.getValue();
			}
		}

		return value;
	}

	@Override
	public String getAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public Attr getAttributeNode(String name) {
		return (attributes != null) ? attributes.get(name) : null;
	}

	@Override
	public Attr getAttributeNodeNS(String namespaceURI, String localName)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public NodeList getElementsByTagName(String name) {
		throw new RuntimeException();
	}

	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new RuntimeException();
	}

	@Override
	public String getTagName() {
		return name;
	}

	@Override
	public boolean hasAttribute(String name) {
		return (getAttributeNode(name) != null);
	}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void removeAttribute(String name) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void removeAttributeNS(String namespaceURI, String localName)
			throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setAttribute(String name, String value) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setAttributeNS(String namespaceURI, String qualifiedName,
			String value) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public Attr setAttributeNode(Attr newAttr) throws DOMException {
		if (attributes == null) {
			attributes = new TreeMap<String, Attr>();
		}

		return attributes.put(newAttr.getName(), newAttr);
	}

	@Override
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setIdAttributeNS(String namespaceURI, String localName,
			boolean isId) throws DOMException {
		throw new RuntimeException();
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId)
			throws DOMException {
		throw new RuntimeException();
	}

}