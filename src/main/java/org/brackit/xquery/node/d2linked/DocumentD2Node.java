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

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.ArrayCollection;
import org.brackit.xquery.node.d2linked.ParentD2Node.DescendantScanner;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class DocumentD2Node extends ParentD2Node {

	private static class D2NodeCollection extends ArrayCollection<D2Node> {
		public D2NodeCollection(String name, DocumentD2Node document) {
			super(name, document);
		}
	}

	private final Collection<D2Node> collection;

	public DocumentD2Node(String name) {
		super(null, FIRST);
		this.collection = new D2NodeCollection(name, this);
	}

	public DocumentD2Node() {
		super(null, FIRST);
		this.collection = new D2NodeCollection(String.format("%s_%s_%s.xml",
				Thread.currentThread().getName(), "noname", Long
						.toString(System.currentTimeMillis())), this);
	}

	@Override
	public Collection<D2Node> getCollection() {
		return collection;
	}

	@Override
	public QNm getName() throws DocumentException {
		return null;
	}

	@Override
	public boolean isDocumentOf(Node<?> node) {
		return (getKind() == Kind.DOCUMENT) && (node == this);
	}

	@Override
	public Kind getKind() {
		return Kind.DOCUMENT;
	}
	
	@Override
	public Stream<? extends D2Node> getDescendantOrSelf()
			throws DocumentException {
		DescendantScanner desc = new DescendantScanner(this);
		desc.next(); // pop document
		return desc;
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')",
				Kind.DOCUMENT, collection.getName(), null);
	}
}