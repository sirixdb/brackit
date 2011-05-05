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
package org.brackit.xquery.compiler.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class DotContext {
	private static final Logger log = Logger.getLogger(DotContext.class);

	List<DotNode> nodes = new ArrayList<DotNode>();
	List<DotEdge> edges = new ArrayList<DotEdge>();
	Stack<DotNode> stack = new Stack<DotNode>();

	public DotNode addNode(String name) {
		DotNode node = new DotNode(name);
		nodes.add(node);
		return node;
	}

	public DotNode pushChildNode(String name) {
		DotNode node = new DotNode(name);
		if (!stack.isEmpty()) {
			addEdge(stack.peek().name, name);
		}
		stack.push(node);
		nodes.add(node);
		return node;
	}

	public void popChildNode() {
		stack.pop();
	}

	public void addEdge(String src, String dest) {
		DotEdge edge = new DotEdge(src, dest);
		edges.add(edge);
	}

	public String toDotString() {
		StringBuilder out = new StringBuilder();
		out.append("digraph g {\n");
		out.append("node [shape = plaintext]\n");
		for (DotNode n : nodes) {
			out.append(n.toDotString());
			out.append("\n");
		}
		for (DotEdge r : edges) {
			out.append(r.toDotString());
			out.append("\n");
		}
		out.append("}");
		return out.toString();
	}

	public void write(File file) {
		try {
			FileWriter fout = new FileWriter(file);
			fout.append(toDotString());
			fout.close();
		} catch (IOException e) {
			log.error(e);
		}
	}

	static String maskHTML(String valueString) {
		return valueString.replace(">", "&gt;").replace("<", "&lt;").replace(
				"&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;");
	}
}