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

import org.brackit.xquery.QueryContext;
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
public class D2NodeBuilder extends AbstractBuilder<D2Node> {

	public D2NodeBuilder(QueryContext ctx, String name)
			throws DocumentException {
		super(new DocumentD2Node(name));
	}

	public D2NodeBuilder() throws DocumentException {
		super();
	}

	@Override
	protected D2Node buildDocument() throws DocumentException {
		return new DocumentD2Node();
	}

	@Override
	protected D2Node buildAttribute(D2Node parent, QNm name, Atomic value)
			throws DocumentException {
		return (parent != null) ? parent.setAttribute(name, value)
				: new AttributeD2Node(name, value);
	}

	@Override
	protected D2Node buildElement(D2Node parent, QNm name, Map<Str, AnyURI> nsMappings)
			throws DocumentException {
		ElementD2Node e = (parent != null) ? (ElementD2Node) parent.append(Kind.ELEMENT, name)
				: new ElementD2Node(name);
		e.nsMappings = nsMappings;
		return e;
	}

	@Override
	protected D2Node buildText(D2Node parent, Atomic text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.TEXT, text)
				: new TextD2Node(text);
	}

	@Override
	protected D2Node buildComment(D2Node parent, Str text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.COMMENT, text)
				: new CommentD2Node(text);
	}

	@Override
	protected D2Node buildProcessingInstruction(D2Node parent, Str text)
			throws DocumentException {
		return (parent != null) ? parent.append(Kind.PROCESSING_INSTRUCTION,
				text) : new PID2Node(null, text);
	}
}
