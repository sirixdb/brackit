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
package org.brackit.xquery.sequence.type;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class ElementType extends KindTest {
	private final QNm name;

	private final String localName;

	private final Type type;

	public ElementType() {
		this.name = null;
		this.localName = null;
		this.type = null;
	}

	public ElementType(QNm name) {
		this.name = name;
		this.localName = (name != null) ? name.getLocalName() : null;
		this.type = null;
	}

	public ElementType(QNm name, Type type) {
		this.name = name;
		this.localName = name.getLocalName();
		this.type = type;
	}

	public QNm getQName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public boolean isWildcard() {
		return name == null;
	}

	@Override
	public Kind getNodeKind() {
		return Kind.ELEMENT;
	}

	@Override
	public boolean matches(Node<?> node)
			throws QueryException {
		if (type != null) {
			throw new QueryException(
					ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
					"Type annotation support not implemented yet");
		}
		// TODO get correct QName of the node
		return (node.getKind() == Kind.ELEMENT)
				&& ((localName == null) || (localName.equals(node.getName())));
	}

	@Override
	public boolean matches(Item item) throws QueryException {
		return ((item instanceof Node<?>) && (matches((Node<?>) item)));
	}

	public String toString() {
		return (name != null) ? (type == null) ? String.format(
				"element(\"%s\")", name) : String.format(
				"element(\"%s\", \"%s\")", name, type) : "element()";
	}
}
