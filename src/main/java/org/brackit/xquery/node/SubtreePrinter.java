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

	private int level;

	private int levelWithoutContent;

	private boolean openElement;

	private boolean printXmlHead = false;

	private boolean prettyPrint = true;

	private boolean autoFlush = true;
	
	private boolean printEmptyElementTag = false;
	
	private NS ns;
	
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
		this.levelWithoutContent = -1;

		if (printXmlHead)
			out.println("<?xml version=\"1.0\"?>");
	}

	@Override
	public void endDocument() throws DocumentException {
		checkOpenElement();
	}

	@Override
	public void end() throws DocumentException {
		if (autoFlush) {
			out.flush();
		}
	}

	@Override
	public void endElement(QNm name) throws DocumentException {
		if ((level == levelWithoutContent) && (!printEmptyElementTag)) {
			out.print("/>");
			openElement = false;
			level--;
			levelWithoutContent = -1;
		} else {
			checkOpenElement();
			level--;
			indent();
			out.print("</");
			out.print(name);
			out.print(">");
		}
		if (prettyPrint) {
			out.println();
		}
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
		checkOpenElement();
		indent();
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
		openElement = true;
		level++;
		levelWithoutContent = level;
	}

	@Override
	public void text(Atomic value) throws DocumentException {
		checkOpenElement();
		indent();
		out.print(value);
		levelWithoutContent = -1;
		if (prettyPrint) {
			out.println();
		}
	}

	@Override
	public void comment(Atomic value) throws DocumentException {
		checkOpenElement();
		indent();
		out.print("<!-- ");
		if (prettyPrint) {
			out.println();
		}
		indent();
		out.print(value);
		if (prettyPrint) {
			out.println();
		}
		indent();
		out.print(" -->");
		if (prettyPrint) {
			out.println();
		}
		levelWithoutContent = -1;
	}

	@Override
	public void processingInstruction(QNm target, Atomic value) throws DocumentException {
		checkOpenElement();
		indent();
		out.println("<? ");
		out.println(value);
		out.println(" ?>");
		levelWithoutContent = -1;
	}

	private void indent() {
		if (prettyPrint) {
			for (int i = 0; i < level; i++)
				out.print("\t");
		}
	}

	private void checkOpenElement() {
		if (openElement) {
			out.print(">");
			openElement = false;
			if (prettyPrint) {
				out.println();
			}
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
//		NS p = null;
//		for (NS ns = this.ns; ns != null; ns = ns.next) {
//			if (ns.prefix.atomicCmp(prefix) == 0) {
//				if (p == null) {
//					this.ns = ns.next;
//				} else {
//					p.next = ns.next;
//				}
//				return;
//			}
//			p = ns;
//		}
//		throw new DocumentException("Unknown namespace prefix: '%s'", prefix);
	}

	@Override
	public void startMapping(String prefix, String uri) throws DocumentException {
		NS tmp = ns;
		ns = new NS(prefix, uri);
		ns.next = tmp;
	}
}