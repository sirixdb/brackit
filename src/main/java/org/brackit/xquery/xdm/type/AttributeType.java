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
package org.brackit.xquery.xdm.type;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class AttributeType extends NodeType {
	private final QNm name;

	private final Type type;

	public AttributeType() {
		this.name = null;
		this.type = null;
	}

	public AttributeType(QNm name) {
		this.name = name;
		this.type = null;
	}

	public AttributeType(QNm name, Type type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public QNm getQName() {
		return name;
	}

	@Override
	public Type getType() {
		return type;
	}

	public boolean isWildcard() {
		return name == null;
	}

	@Override
	public Kind getNodeKind() {
		return Kind.ATTRIBUTE;
	}

	@Override
	public boolean matches(Node<?> node) throws QueryException {
		return ((node.getKind() == Kind.ATTRIBUTE)
				&& ((name == null) || (name.eq(node.getName()))) && ((type == null) || (node
				.type().instanceOf(type))));
	}

	public String toString() {
		return (type == null) ? String.format("attribute(\"%s\")", name)
				: String.format("attribute(\"%s\", \"%s\")", name, type);
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AttributeType)) {
			return false;
		}
		AttributeType t = (AttributeType) obj;
		if (name == null) {
			if (t.name != null) {
				return false;
			}
		} else {
			if ((t.name == null) || (!name.equals(t.name))) {
				return false;
			}
		}
		if (type == null) {
			if (t.type != null) {
				return false;
			}
		} else {
			if ((t.type == null) || (!type.equals(t.type))) {
				return false;
			}
		}
		return true;
	}
}
