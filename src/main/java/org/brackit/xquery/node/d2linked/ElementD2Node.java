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

import java.util.Map;
import java.util.TreeMap;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.stream.EmptyStream;
import org.brackit.xquery.node.stream.IteratorStream;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;
import org.brackit.xquery.xdm.Scope;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class ElementD2Node extends ParentD2Node implements Scope {
	Map<String, String> nsMappings;
	QNm name;
	D2Node firstAttribute;

	public ElementD2Node(QNm name) throws DocumentException {
		super(null, FIRST);
		this.name = checkName(name);
	}

	ElementD2Node(ParentD2Node parent, int[] division, QNm name)
			throws DocumentException {
		super(parent, division);
		this.name = checkName(name);
	}

	QNm checkName(QNm name) throws DocumentException {
		if (name.getPrefix() == null) {
			return name;
		}
		String mappedUri = resolvePrefix(name.getPrefix());
		String uri = name.getNamespaceURI();
		if (mappedUri == null) {
			addPrefix(name.getPrefix(), uri);
			return name;
		}
		if (mappedUri.equals(uri)) {
			return name;
		}
		// create a subsitute prefix name
		if (nsMappings == null) {
			name = new QNm(uri, name.getPrefix() + "_1", name.getLocalName());
			addPrefix(name.getPrefix(), uri);
			return name;
		}
		int i = 1;
		while (true) {
			String newPrefix = name.getPrefix() + "_" + i++;
			mappedUri = nsMappings.get(newPrefix);
			if (mappedUri == null) {
				name = new QNm(uri, newPrefix, name.getLocalName());
				addPrefix(name.getPrefix(), uri);
				return name;
			} else if (mappedUri.equals(uri)) {
				// re-use prefix
				name = new QNm(uri, newPrefix, name.getLocalName());
				return name;
			}
		}
	}

	@Override
	public QNm getName() throws DocumentException {
		return name;
	}

	public Kind getKind() {
		return Kind.ELEMENT;
	}

	@Override
	public D2Node getAttribute(QNm name) throws DocumentException {
		for (D2Node attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
			if (attribute.getName().equals(name)) {
				return attribute;
			}
		}

		return null;
	}

	@Override
	protected boolean hasAttribute(D2Node attribute) {
		for (D2Node myAttribute = firstAttribute; myAttribute != null; myAttribute = myAttribute.sibling) {
			if (attribute == myAttribute) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Stream<D2Node> getAttributes()
			throws OperationNotSupportedException, DocumentException {
		if (firstAttribute == null) {
			return new EmptyStream<D2Node>();
		}
		return new SiblingStream(firstAttribute);
	}

	@Override
	public D2Node getNextSibling() throws DocumentException {
		if (parent == null) {
			return null;
		}

		return parent.nextSiblingOf(this);
	}

	@Override
	public D2Node getPreviousSibling() throws DocumentException {
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
		D2Node prev = null;
		for (D2Node attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
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
	public D2Node setAttribute(Node<?> attribute)
			throws OperationNotSupportedException, DocumentException {
		if (attribute.getKind() != Kind.ATTRIBUTE) {
			throw new DocumentException(
					"Cannot set nodes of type '%s' as attribute", attribute
							.getKind());
		}

		return setAttribute(attribute.getName(), attribute.getValue());
	}

	@Override
	public D2Node setAttribute(QNm name, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		checkName(name);
		if (firstAttribute == null) {
			return (firstAttribute = new AttributeD2Node(this, name, value));
		} else {
			D2Node prev = null;
			for (D2Node attribute = firstAttribute; attribute != null; attribute = attribute.sibling) {
				if (attribute.getName().equals(name)) {
					throw new DocumentException(
							"Attribute '%s' already exists", name);
				}
				prev = attribute;
			}
			return (prev.sibling = new AttributeD2Node(this,
					siblingAfter(prev.division), name, value));
		}
	}

	@Override
	public void setName(QNm name) throws OperationNotSupportedException,
			DocumentException {
		this.name = checkName(name);
	}

	@Override
	public void setValue(Atomic value) throws OperationNotSupportedException,
			DocumentException {
		firstChild = null;
		append(Kind.TEXT, null, value);
	}

	@Override
	public Scope getScope() {
		return this;
	}

	@Override
	public Stream<String> localPrefixes() throws DocumentException {
		if (nsMappings == null) {
			return new EmptyStream<String>();
		}
		return new IteratorStream<String>(nsMappings.keySet());
	}

	@Override
	public void addPrefix(String prefix, String uri) throws DocumentException {
		// TODO checks
		if (nsMappings == null) {
			// use tree map because we expect only a few
			// entries and a tree map is much more space efficient
			nsMappings = new TreeMap<String, String>();
		}
		nsMappings.put(prefix, uri);
	}

	@Override
	public String defaultNS() throws DocumentException {
		return resolvePrefix("");
	}

	@Override
	public void setDefaultNS(String uri) throws DocumentException {
		addPrefix("", uri);
	}

	@Override
	public String resolvePrefix(String prefix) throws DocumentException {
		if (prefix == null) {
			// search for the default namespace
			prefix = "";
		}
		ElementD2Node n = this;
		while (true) {
			if (n.nsMappings != null) {
				String uri = n.nsMappings.get(prefix);
				if (uri != null) {
					return uri;
				}
			}
			ParentD2Node p = n.parent;
			if ((p == null) || (!(p instanceof ElementD2Node))) {
				break;
			}
			n = (ElementD2Node) p;
		}
		if (prefix.equals("xml")) {
			return "http://www.w3.org/XML/1998/namespace";
		}
		return ((prefix == null) || (prefix.isEmpty())) ? "" : null;
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')",
				Kind.ELEMENT, name, null);
	}
}
