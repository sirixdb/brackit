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
package org.brackit.xquery.node.linked;

import java.util.Map;

import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.node.AbstractBuilder;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class LNodeBuilder extends AbstractBuilder<LNode> {
	public LNodeBuilder(String name) throws DocumentException {
		super(new DocumentLNode(name));
	}

	public LNodeBuilder() throws DocumentException {
		super();
	}

	@Override
	protected LNode buildDocument() throws DocumentException {
		return new DocumentLNode();
	}

	@Override
	protected LNode buildAttribute(LNode parent, QNm name, Atomic value)
			throws DocumentException {
		return (parent != null) ? parent.setAttribute(name, value)
				: new AttributeLNode(name, value);
	}

	@Override
	protected LNode buildElement(LNode parent, QNm name, Map<Str, AnyURI> nsMappings)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.ELEMENT, name)
				: new ElementLNode(name);
	}

	@Override
	protected LNode buildText(LNode parent, Atomic text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.TEXT, text)
				: new TextLNode(text);
	}

	@Override
	protected LNode buildComment(LNode parent, Str text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.COMMENT, text)
				: new CommentLNode(text);
	}

	@Override
	protected LNode buildProcessingInstruction(LNode parent, Str text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.PROCESSING_INSTRUCTION,
				text) : new PILNode(null, text);
	}
}
