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
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.node.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public final class PIType extends NodeType {
	private final String piTarget;

	public PIType(String piTarget) {
		this.piTarget = piTarget;
	}

	public PIType() {
		this.piTarget = null;
	}

	@Override
	public Kind getNodeKind() {
		return Kind.PROCESSING_INSTRUCTION;
	}

	@Override
	public boolean matches(Node<?> node) throws QueryException {
		if (piTarget != null) {
			return ((node.getKind() == Kind.PROCESSING_INSTRUCTION) && (node
					.getName().stringValue().equals(piTarget)));
		}
		return (node.getKind() == Kind.PROCESSING_INSTRUCTION);
	}

	@Override
	public boolean matches(Item item) throws QueryException {
		if (piTarget != null) {
			return ((item instanceof Node<?>)
					&& (((Node<?>) item).getKind() == Kind.PROCESSING_INSTRUCTION) && (((Node<?>) item)
					.getName().stringValue().equals(piTarget)));

		}
		return ((item instanceof Node<?>) && (((Node<?>) item).getKind() == Kind.PROCESSING_INSTRUCTION));
	}

	@Override
	public String toString() {
		return (piTarget != null) ? String.format(
				"processing-instruction(\"%s\")", piTarget)
				: "processing-instruction()";
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PIType)) {
			return false;
		}
		PIType t = (PIType) obj;
		if (piTarget == null) {
			if (t.piTarget != null) {
				return false;
			}
		} else {
			if ((t.piTarget == null) || (!piTarget.equals(t.piTarget))) {
				return false;
			}
		}
		return true;
	}
}
