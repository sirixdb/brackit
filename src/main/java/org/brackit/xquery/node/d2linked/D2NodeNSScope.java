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

import java.util.TreeMap;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.NamespaceScope;

/**
 * @author Sebastian Baechle
 * 
 */
public class D2NodeNSScope implements NamespaceScope {

	// may be null for unrooted non-element nodes
	private final ElementD2Node node;

	public D2NodeNSScope(ElementD2Node node) {
		this.node = node;
	}

	@Override
	public void addPrefix(Str prefix, AnyURI uri) throws DocumentException {
		if (node == null) {
			throw new DocumentException();
		}
		// TODO checks
		if (node.nsMappings == null) {
			// use tree map because we expect only a few
			// entries and a tree map is much more space efficient
			node.nsMappings = new TreeMap<Str, AnyURI>();
		}
		node.nsMappings.put(prefix, uri);
	}

	@Override
	public AnyURI defaultNS() throws DocumentException {
		return resolvePrefix(Str.EMPTY);
	}

	@Override
	public void setDefaultNS(AnyURI uri) throws DocumentException {
		addPrefix(Str.EMPTY, uri);
	}

	@Override
	public AnyURI resolvePrefix(Str prefix) throws DocumentException {
		if (node == null) {
			throw new DocumentException();
		}
		ElementD2Node n = node;
		while (true) {
			if (n.nsMappings != null) {
				AnyURI uri = n.nsMappings.get(prefix);
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
		return null;
	}
}
