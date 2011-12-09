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
package org.brackit.xquery.compiler;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.brackit.xquery.compiler.profiler.DotContext;
import org.brackit.xquery.compiler.profiler.DotNode;
import org.brackit.xquery.module.StaticContext;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class AST {
	protected AST parent;
	protected int type;
	protected Object value;
	protected Map<String, Object> properties;
	protected StaticContext sctx;
	protected AST[] children;

	public AST(int type) {
		this(type, XQ.NAMES[type]);
	}

	protected AST(int type, Object value, StaticContext sctx,
			Map<String, Object> properties) {
		this.type = type;
		this.value = value;
		this.sctx = sctx;
		this.properties = properties;
	}

	public AST(int type, Object value) {
		this.type = type;
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public String getStringValue() {
		return (value != null) ? value.toString() : "";
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setProperty(String name, Object value) {
		if (properties == null) {
			properties = new HashMap<String, Object>();
		}
		properties.put(name, value);
	}

	public Object getProperty(String name) {
		return (properties != null) ? properties.get(name) : null;
	}

	public boolean checkProperty(String name) {
		Object p = (properties != null) ? properties.get(name) : null;
		if (p == null) {
			return false;
		}
		return (Boolean) p;
	}

	public void delProperty(String name) {
		if (properties != null) {
			properties.remove(name);
		}
	}

	public AST getParent() {
		return parent;
	}

	public int getChildCount() {
		return (children == null) ? 0 : children.length;
	}

	public int getChildIndex() {
		if (parent == null) {
			return -1;
		}
		int i = 0;
		for (AST ast : parent.children) {
			if (ast == this) {
				return i;
			}
			i++;
		}
		throw new IllegalStateException();
	}

	public void addChildren(AST[] children) {
		for (AST child : children) {
			addChild(child);
		}
	}

	public void addChild(AST child) {
		if (child == null) {
			throw new NullPointerException();
		}
		if (child == this) {
			throw new IllegalArgumentException();
		}
		if (children == null) {
			children = new AST[] { child };
		} else {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = child;
		}
		child.parent = this;
	}

	public void insertChild(int position, AST child) {
		if ((position < 0) || (children == null)
				|| (position > children.length)) {
			throw new IllegalArgumentException(String.format(
					"Illegal child position: %s", position));
		}
		if (child == null) {
			throw new NullPointerException();
		}
		if (child == this) {
			throw new IllegalArgumentException();
		}
		if (children == null) {
			children = new AST[] { child };
		} else if (position == children.length) {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = child;
		} else {
			AST[] tmp = new AST[children.length + 1];
			if (position > 0) {
				System.arraycopy(children, 0, tmp, 0, position);
			}
			System.arraycopy(children, position, tmp, position + 1,
					children.length - position);
			tmp[position] = child;
			children = tmp;
		}
		child.parent = this;
	}

	public AST getChild(int position) {
		if ((position < 0) || (children == null)
				|| (position >= children.length)) {
			throw new IllegalArgumentException(String.format(
					"Illegal child position: %s", position));
		}
		return children[position];
	}

	public void replaceChild(int position, AST child) {
		if ((position < 0) || (children == null)
				|| (position >= children.length)) {
			throw new IllegalArgumentException(String.format(
					"Illegal child position: %s", position));
		}
		if (child == null) {
			throw new NullPointerException();
		}
		children[position] = child;
		child.parent = this;
	}

	public void deleteChild(int position) {
		if ((position < 0) || (children == null)
				|| (position >= children.length)) {
			throw new IllegalArgumentException(String.format(
					"Illegal child position: %s", position));
		}
		if (children.length == 1) {
			children = null;
		} else {
			AST[] tmp = new AST[children.length - 1];
			if (position > 0) {
				System.arraycopy(children, 0, tmp, 0, position);
			}
			if (position < children.length) {
				int length = children.length - (position + 1);
				System.arraycopy(children, position + 1, tmp, position, length);
			}
			children = tmp;
		}
	}

	public AST copy() {
		return new AST(type, value, sctx, (properties == null) ? null
				: new HashMap<String, Object>(properties));
	}

	public AST copyTree() {
		AST copy = copy();
		if (children != null) {
			copy.children = new AST[children.length];
			for (int i = 0; i < children.length; i++) {
				copy.children[i] = children[i].copyTree();
				copy.children[i].parent = copy;
			}
		}
		return copy;
	}

	public String dot() {
		DotContext dt = new DotContext();
		toDot(0, dt);
		return dt.toDotString();
	}

	public void dot(File file) {
		DotContext dt = new DotContext();
		toDot(0, dt);
		dt.write(file);
	}

	private int toDot(int no, DotContext dt) {
		final int myNo = no++;
		String label = getLabel(getStringValue());
		DotNode node = dt.addNode(String.valueOf(myNo));
		node.addRow(label, null);
		if (properties != null) {
			for (Entry<String, Object> prop : properties.entrySet()) {
				Object value = prop.getValue();
				node.addRow(prop.getKey(), value != null ? value.toString()
						: "");
			}
		}
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				dt.addEdge(String.valueOf(myNo), String.valueOf(no));
				no = children[i].toDot(no, dt);
			}
		}
		return no++;
	}

	protected String getLabel(String value) {
		return ((type > 0) && (type < XQ.NAMES.length)) ? (XQ.NAMES[type]
				.equals(value.toString())) ? value.toString() : XQ.NAMES[type]
				+ "[" + value.toString() + "]" : value.toString();
	}

	public AST getFirstChildWithType(int type) {
		if (children == null) {
			return null;
		}
		for (int i = 0; i < children.length; i++) {
			if (children[i].type == type) {
				return children[i];
			}
		}
		return null;
	}

	public void setStaticContext(StaticContext sctx) {
		this.sctx = sctx;
	}

	public StaticContext getStaticContext() {
		AST n = this;
		while (n != null) {
			if (n.sctx != null) {
				return n.sctx;
			}
			n = n.parent;
		}
		return null;
	}

	public void display() {
		try {
			File file = File.createTempFile("ast", ".dot");
			file.deleteOnExit();
			dot(file);
			Runtime.getRuntime()
					.exec(new String[] { "/usr/bin/dotty",
							file.getAbsolutePath() }).waitFor();
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		String value = getStringValue();
		return getLabel(value);
	}
}