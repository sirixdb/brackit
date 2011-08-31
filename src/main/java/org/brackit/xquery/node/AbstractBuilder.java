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
package org.brackit.xquery.node;

import java.util.Arrays;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.parser.SubtreeHandler;
import org.brackit.xquery.node.parser.SubtreeListener;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Node;

/**
 * @author Sebastian Baechle
 * 
 */
public abstract class AbstractBuilder<E extends Node<E>> implements
		SubtreeListener<E>, SubtreeHandler {
	private Node[] stack;

	private int stackSize;

	private E parent;

	private E root;

	public AbstractBuilder(E root) {
		this.stack = new Node[5];
		this.root = root;
		stack[stackSize++] = root;
	}

	public AbstractBuilder() {
		this.stack = new Node[5];
	}

	protected abstract E buildDocument() throws DocumentException;

	protected abstract E buildElement(E parent, QNm name)
			throws DocumentException;

	protected abstract E buildAttribute(E parent, QNm name, Atomic value)
			throws DocumentException;

	protected abstract E buildText(E parent, Atomic text) throws DocumentException;

	protected abstract E buildComment(E parent, Str text)
			throws DocumentException;

	// TODO check params for PI's
	protected abstract E buildProcessingInstruction(E parent, Str text)
			throws DocumentException;

	private void prepare() throws DocumentException {
		if ((stackSize == 0) && (root != null)) {
			throw new DocumentException("A root already exists");
		}
		if (stackSize == stack.length) {
			stack = Arrays.copyOf(stack, ((stackSize * 3) / 2 + 1));
		}
	}

	public E root() throws DocumentException {
		if (root == null) {
			root = (E) stack[0];
			if (root == null) {
				throw new DocumentException("No root node has been build");
			}
		}
		return root;
	}

	@Override
	public void begin() throws DocumentException {
	}

	@Override
	public void end() throws DocumentException {
	}

	@Override
	public void fail() throws DocumentException {
	}

	@Override
	public void beginFragment() throws DocumentException {
	}

	@Override
	public void endFragment() throws DocumentException {
	}

	@Override
	public void startDocument() throws DocumentException {
		prepare();
		parent = buildDocument();
		stack[stackSize++] = parent;
	}

	@Override
	public void endDocument() throws DocumentException {
		parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
	}

	@Override
	public <T extends E> void startElement(T node) throws DocumentException {
		prepare();
		parent = buildElement(parent, node.getName());
		stack[stackSize++] = parent;
	}

	@Override
	public <T extends E> void endElement(T node) throws DocumentException {
		parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
	}

	@Override
	public <T extends E> void attribute(T node) throws DocumentException {
		prepare();
		stack[stackSize] = buildAttribute(parent, node.getName(), node
				.getValue());
	}

	@Override
	public <T extends E> void text(T node) throws DocumentException {
		prepare();
		stack[stackSize] = buildText(parent, new Una(node.getValue()
				.stringValue()));
	}

	@Override
	public <T extends E> void comment(T node) throws DocumentException {
		prepare();
		stack[stackSize] = buildComment(parent, new Str(node.getValue()
				.stringValue()));
	}

	@Override
	public <T extends E> void processingInstruction(T node)
			throws DocumentException {
		prepare();
		stack[stackSize] = buildProcessingInstruction(parent, new Str(node
				.getValue().stringValue()));
	}

	@Override
	public void startElement(QNm name) throws DocumentException {
		prepare();
		parent = buildElement(parent, name);
		stack[stackSize++] = parent;
	}

	@Override
	public void endElement(QNm name) throws DocumentException {
		parent = (E) (--stackSize > 0 ? stack[stackSize - 1] : null);
	}

	@Override
	public void attribute(QNm name, Atomic value) throws DocumentException {
		prepare();
		stack[stackSize] = buildAttribute(parent, name, value);
	}

	@Override
	public void text(Atomic content) throws DocumentException {
		prepare();
		stack[stackSize] = buildText(parent, content);
	}

	@Override
	public void comment(Str content) throws DocumentException {
		prepare();
		stack[stackSize] = buildComment(parent, content);
	}

	@Override
	public void processingInstruction(Str content) throws DocumentException {
		prepare();
		stack[stackSize] = buildProcessingInstruction(parent, content);
	}
}
