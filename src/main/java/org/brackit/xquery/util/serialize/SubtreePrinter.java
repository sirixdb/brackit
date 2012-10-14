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
package org.brackit.xquery.util.serialize;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.parser.DefaultHandler;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Node;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class SubtreePrinter extends DefaultHandler {

	private final PrintWriter out;

	private boolean printXmlHead = false;
	private boolean prettyPrint = true;
	private boolean autoFlush = true;
	private boolean printEmptyElementTag = true;
	private String indent = "    ";

	private int level;
	private boolean emptyElement;
	private boolean openElement;
	private NS ns;
	private Atomic pendingText;

	/**
	 * Linked list of current namespace mappings
	 */
	private static class NS {
		private final String prefix;
		private final String uri;
		private NS next;

		NS(String prefix, String uri) {
			this.prefix = prefix;
			this.uri = uri;
		}
	}

	public SubtreePrinter(PrintWriter out) {
		this(out, false);
	}

	public SubtreePrinter(PrintWriter out, boolean printXmlHead) {
		this.out = out;
		this.printXmlHead = printXmlHead;
	}

	public SubtreePrinter(PrintWriter out, boolean printXmlHead,
			boolean prettyPrint) {
		this.out = out;
		this.printXmlHead = printXmlHead;
		this.prettyPrint = prettyPrint;
	}

	public SubtreePrinter(PrintStream out) {
		this(out, false, true);
	}

	public SubtreePrinter(PrintStream out, boolean printXmlHead) {
		this(new PrintWriter(out, false), printXmlHead, false);
	}

	public SubtreePrinter(PrintStream out, boolean printXmlHead,
			boolean prettyPrint) {
		this(new PrintWriter(out, false), printXmlHead, prettyPrint);
	}

	@Override
	public void attribute(QNm name, Atomic value) throws DocumentException {
		out.print(" ");
		out.print(name);
		out.print("=\"");
		out.print(value);
		out.print("\"");
	}

	@Override
	public void startDocument() throws DocumentException {
		this.level = 0;
		this.emptyElement = false;

		if (printXmlHead) {
			out.println("<?xml version=\"1.0\"?>");
		}
	}

	@Override
	public void endDocument() throws DocumentException {
	}

	@Override
	public void end() throws DocumentException {
		if (autoFlush) {
			out.flush();
		}
	}

	@Override
	public void endElement(QNm name) throws DocumentException {
		level--;
		if (emptyElement) {
			if (pendingText != null) {
				out.print(">");
				out.print(pendingText);
				out.print("</");
				out.print(name);
				out.print(">");
				pendingText = null;
			} else if (printEmptyElementTag) {
				out.print("/>");
			} else {
				out.print(">");
				out.print("</");
				out.print(name);
				out.print(">");
			}
		} else {
			if (prettyPrint) {
				out.println();
			}
			indent();
			out.print("</");
			out.print(name);
			out.print(">");
		}
		openElement = false;
		emptyElement = false;
	}

	@Override
	public void fail() throws DocumentException {
		out.append("Terminating due tu error!");

		if (autoFlush) {
			out.flush();
		}
	}

	@Override
	public void startElement(QNm name) throws DocumentException {
		newChild();
		out.print("<");
		out.print(name);
		for (NS n = ns; n != null; n = n.next) {
			if ((n.prefix != null) && (!n.prefix.isEmpty())) {
				out.print(" xmlns:");
				out.print(n.prefix);
			} else {
				out.print(" xmlns");
			}
			out.print("=\"");
			out.print(n.uri);
			out.print("\"");
		}
		ns = null;
		level++;
		openElement = true;
		emptyElement = true;
	}

	@Override
	public void text(Atomic value) throws DocumentException {
		if (emptyElement) {
			pendingText = value;
		} else {
			newChild();
			out.print(value);
			emptyElement = false;
		}
	}

	@Override
	public void comment(Atomic value) throws DocumentException {
		newChild();
		out.print("<!-- ");
		out.print(value);
		out.print(" -->");
		emptyElement = false;
	}

	@Override
	public void processingInstruction(QNm target, Atomic value)
			throws DocumentException {
		newChild();
		out.print("<?");
		out.print(target);
		out.print(" ");
		out.print(value);
		out.print("?>");
		emptyElement = false;
	}

	private void newChild() {
		if (openElement) {
			out.print(">");
			openElement = false;
		}
		if (pendingText != null) {
			if (prettyPrint) {
				out.println();
			}
			indent();
			out.print(pendingText);
			pendingText = null;
		}
		if ((level > 0) && (prettyPrint)) {
			out.println();
		}
		indent();
	}

	private void indent() {
		if (prettyPrint) {
			for (int i = 0; i < level; i++)
				out.print(indent);
		}
	}

	public boolean isPrintXmlHead() {
		return printXmlHead;
	}

	public void setPrintXmlHead(boolean printXmlHead) {
		this.printXmlHead = printXmlHead;
	}

	public boolean isPrettyPrint() {
		return prettyPrint;
	}

	public void setPrettyPrint(boolean indent) {
		this.prettyPrint = indent;
	}

	public boolean isAutoFlush() {
		return autoFlush;
	}

	public void setAutoFlush(boolean autoFlush) {
		this.autoFlush = autoFlush;
	}

	public void flush() {
		out.flush();
	}

	public boolean isPrintEmptyElementTag() {
		return printEmptyElementTag;
	}

	public void setPrintEmptyElementTag(boolean print) {
		this.printEmptyElementTag = print;
	}

	public String getIndent() {
		return indent;
	}

	public void setIndent(String indent) {
		this.indent = indent;
	}

	public void print(Node<?> node) throws DocumentException {
		node.parse(this);
	}

	public static void print(Node<?> node, PrintStream out)
			throws DocumentException {
		node.parse(new SubtreePrinter(out));
	}

	public static void print(Node<?> node, PrintWriter out)
			throws DocumentException {
		node.parse(new SubtreePrinter(out));
	}

	@Override
	public void endMapping(String prefix) throws DocumentException {
	}

	@Override
	public void startMapping(String prefix, String uri)
			throws DocumentException {
		NS tmp = ns;
		ns = new NS(prefix, uri);
		ns.next = tmp;
	}
}