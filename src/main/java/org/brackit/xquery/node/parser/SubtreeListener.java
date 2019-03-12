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
package org.brackit.xquery.node.parser;

import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.node.Node;

/**
 * SAX-like handler-interface for documents.
 * 
 * @author Sebastian Baechle
 * 
 */
public interface SubtreeListener<E extends Node<?>> {
	public void startDocument() throws DocumentException;

	public void endDocument() throws DocumentException;

	public <T extends E> void text(T node) throws DocumentException;

	public <T extends E> void comment(T node) throws DocumentException;

	public <T extends E> void processingInstruction(T node)
			throws DocumentException;

	public <T extends E> void startElement(T node) throws DocumentException;

	public <T extends E> void endElement(T node) throws DocumentException;

	public <T extends E> void attribute(T node) throws DocumentException;

	public void begin() throws DocumentException;

	public void beginFragment() throws DocumentException;

	public void endFragment() throws DocumentException;

	public void end() throws DocumentException;

	public void fail() throws DocumentException;
}
