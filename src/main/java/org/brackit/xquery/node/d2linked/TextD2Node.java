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

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.OperationNotSupportedException;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class TextD2Node extends D2Node {
	public Atomic value;

	public TextD2Node(Atomic value) {
		this(null, FIRST, value);
	}

	TextD2Node(ParentD2Node parent, int[] division, Atomic value) {
		super(parent, division);
		this.value = value;
	}

	public Kind getKind() {
		return Kind.TEXT;
	}

	@Override
	public QNm getName() throws DocumentException {
		return null;
	}

	@Override
	public Atomic getValue() {
		return value;
	}

	@Override
	public void setValue(Atomic value) throws OperationNotSupportedException,
			DocumentException {
		this.value = value;
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
	public D2Node insertAfter(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertAfter(kind, value);
	}

	@Override
	public D2Node insertAfter(Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertAfter(this, child);
	}

	@Override
	public D2Node insertAfter(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertAfter(this, parser);
	}

	@Override
	public D2Node insertBefore(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(kind, value);
	}

	@Override
	public D2Node insertBefore(Node<?> child)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(this, child);
	}

	@Override
	public D2Node insertBefore(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		if (parent == null) {
			throw new DocumentException("%s has no parent", this);
		}
		return parent.insertBefore(this, parser);
	}

	@Override
	public D2Node replaceWith(Kind kind, Atomic value)
			throws OperationNotSupportedException, DocumentException {
		if ((kind != Kind.ELEMENT) || (kind != Kind.TEXT)
				|| (kind != Kind.COMMENT)
				|| (kind != Kind.PROCESSING_INSTRUCTION)) {
			throw new DocumentException(
					"Cannot replace node with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replace(this, kind, value);
	}

	@Override
	public D2Node replaceWith(Node<?> node)
			throws OperationNotSupportedException, DocumentException {
		Kind kind = node.getKind();
		if ((kind != Kind.ELEMENT) && (kind != Kind.TEXT)
				&& (kind != Kind.COMMENT)
				&& (kind != Kind.PROCESSING_INSTRUCTION)) {
			throw new DocumentException(
					"Cannot replace node with node of type: %s.", kind);
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replace(this, node);
	}

	@Override
	public D2Node replaceWith(SubtreeParser parser)
			throws OperationNotSupportedException, DocumentException {
		D2Node node = builder.build(parser);
		Kind kind = node.getKind();

		if ((kind != Kind.ELEMENT) || (kind != Kind.TEXT)
				|| (kind != Kind.COMMENT)
				|| (kind != Kind.PROCESSING_INSTRUCTION)) {
		}

		if (parent == null) {
			throw new DocumentException("Cannot replace node without parent");
		}

		return parent.replace(this, node);
	}

	@Override
	public String toString() {
		return String.format("(type='%s', name='%s', value='%s')", Kind.TEXT,
				null, value);
	}
}
