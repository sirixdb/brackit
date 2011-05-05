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
package org.brackit.xquery.xqts;

import org.brackit.xquery.node.dom.CommentImpl;
import org.brackit.xquery.node.dom.DOMListener;
import org.brackit.xquery.node.dom.DocumentImpl;
import org.brackit.xquery.node.dom.ElementImpl;
import org.brackit.xquery.node.dom.NodeImpl;
import org.brackit.xquery.node.dom.ProcInstrImpl;
import org.brackit.xquery.node.dom.TextImpl;
import org.brackit.xquery.xdm.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Allows for collecting several top level nodes that can be contained in XQuery
 * result sequences and puts them under a single root element of name
 * <i>rootName</i>.
 * 
 * @author Max Bechtold
 * 
 */
public class SequenceListener extends DOMListener {
	private String rootName;

	private StringBuilder collectedText;

	public SequenceListener(String rootName) {
		super();
		this.rootName = rootName;
		stack.clear();
		document = new DocumentImpl();
		collectedText = new StringBuilder();
	}

	@Override
	public <T extends org.brackit.xquery.xdm.Node<?>> void endElement(T node)
			throws DocumentException {
		stack.pollLast();
	}

	@Override
	public <T extends org.brackit.xquery.xdm.Node<?>> void text(T node)
			throws DocumentException {
		if (stack.size() <= 1)
			collectedText.append(node.getValue());
		else
			stack.peekLast().appendChild(
					new TextImpl(document, stack.peekLast(), null, node
							.getValue()));
	}

	@Override
	public <T extends org.brackit.xquery.xdm.Node<?>> void startElement(T node)
			throws DocumentException {
		handlePendingNodes();
		NodeImpl current = stack.peekLast();
		ElementImpl newChild = new ElementImpl(document, current, node
				.getName(), null);
		((Element) current).appendChild(newChild);
		stack.addLast(newChild);
	}

	private <T extends org.brackit.xquery.xdm.Node<?>> void handlePendingNodes()
			throws DocumentException {
		if (document.getDocumentElement() == null) {
			ElementImpl root = new ElementImpl(document, document, rootName,
					null);
			document.setDocumentElement(root);
			stack.addLast(root);
		}

		if (collectedText.length() > 0) {
			stack.peekLast().appendChild(
					new TextImpl(document, stack.peekLast(), null,
							collectedText.toString()));
			collectedText = new StringBuilder();
		}
	}

	@Override
	public Document getDocument() {
		try {
			handlePendingNodes();
		} catch (DocumentException e) {

		}
		return document;
	}

	@Override
	public <T extends org.brackit.xquery.xdm.Node<?>> void comment(T node)
			throws DocumentException {
		handlePendingNodes();
		stack.peekLast().appendChild(
				new CommentImpl(document, stack.peekLast(), null, node
						.getValue()));
	}

	@Override
	public <T extends org.brackit.xquery.xdm.Node<?>> void processingInstruction(
			T node) throws DocumentException {
		handlePendingNodes();
		stack.peekLast().appendChild(
				new ProcInstrImpl(document, stack.peekLast(), null, node
						.getValue()));
	}

	public void appendText(String value) {
		collectedText.append(value);
	}
}
