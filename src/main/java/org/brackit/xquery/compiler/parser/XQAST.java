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
package org.brackit.xquery.compiler.parser;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.brackit.xquery.compiler.profiler.DotContext;
import org.brackit.xquery.compiler.profiler.DotNode;

public class XQAST {

	private static int seq = 0;
	public static final int XQuery = seq++;
	public static final int LibraryModule = seq++;
	public static final int MainModule = seq++;
	public static final int NamespaceDeclaration = seq++;
	public static final int Literal = seq++;
	public static final int QueryBody = seq++;
	public static final int SequenceExpr = seq++;
	public static final int FlowrExpr = seq++;
	public static final int ForClause = seq++;
	public static final int AllowingEmpty = seq++;
	public static final int TypedVariableBinding = seq++;
	public static final int Variable = seq++;
	public static final int LetClause = seq++;
	public static final int WhereClause = seq++;
	public static final int GroupByClause = seq++;
	public static final int GroupBySpec = seq++;
	public static final int VariableRef = seq++;
	public static final int Collation = seq++;
	public static final int OrderByClause = seq++;
	public static final int OrderBySpec = seq++;
	public static final int OrderByKind = seq++;
	public static final int ASCENDING = seq++;
	public static final int DESCENDING = seq++;
	public static final int OrderByEmptyMode = seq++;
	public static final int GREATEST = seq++;
	public static final int LEAST = seq++;
	public static final int CountClause = seq++;
	public static final int ReturnExpr = seq++;
	public static final int OrExpr = seq++;
	public static final int AndExpr = seq++;
	public static final int GeneralCompEQ = seq++;
	public static final int GeneralCompNE = seq++;
	public static final int GeneralCompLT = seq++;
	public static final int GeneralCompLE = seq++;
	public static final int GeneralCompGT = seq++;
	public static final int GeneralCompGE = seq++;
	public static final int ValueCompEQ = seq++;
	public static final int ValueCompNE = seq++;
	public static final int ValueCompLT = seq++;
	public static final int ValueCompLE = seq++;
	public static final int ValueCompGT = seq++;
	public static final int ValueCompGE = seq++;
	public static final int NodeCompIs = seq++;
	public static final int NodeCompPrecedes = seq++;
	public static final int NodeCompFollows = seq++;
	public static final int ComparisonExpr = seq++;
	public static final int RangeExpr = seq++;
	public static final int AddOp = seq++;
	public static final int SubtractOp = seq++;
	public static final int ArithmeticExpr = seq++;
	public static final int MultiplyOp = seq++;
	public static final int DivideOp = seq++;
	public static final int IDivideOp = seq++;
	public static final int UnionExpr = seq++;
	public static final int IntersectExpr = seq++;
	public static final int ExceptExpr = seq++;
	public static final int InstanceofExpr = seq++;
	public static final int SequenceType = seq++;
	public static final int EmptySequenceType = seq++;
	public static final int ItemType = seq++;
	public static final int CardinalityZeroOrOne = seq++;
	public static final int CardinalityZeroOrMany = seq++;
	public static final int CardinalityOneOrMany = seq++;
	public static final int TreatExpr = seq++;
	public static final int CastableExpr = seq++;
	public static final int CastExpr = seq++;
	public static final int FunctionCall = seq++;
	public static final int PathExpr = seq++;
	public static final int StepExpr = seq++;
	public static final int AxisSpec = seq++;
	public static final int SELF = seq++;
	public static final int CHILD = seq++;
	public static final int DESCENDANT = seq++;
	public static final int DESCENDANT_OR_SELF = seq++;
	public static final int ATTRIBUTE = seq++;
	public static final int FOLLOWING_SIBLING = seq++;
	public static final int FOLLOWING = seq++;
	public static final int PARENT = seq++;
	public static final int ANCESTOR = seq++;
	public static final int PRECEDING_SIBLING = seq++;
	public static final int PRECEDING = seq++;
	public static final int ANCESTOR_OR_SELF = seq++;
	public static final int KindTestAnyKind = seq++;
	public static final int KindTestDocument = seq++;
	public static final int NameTest = seq++;
	public static final int Wildcard = seq++;
	public static final int Nilled = seq++;
	public static final int KindTestElement = seq++;
	public static final int KindTestAttribute = seq++;
	public static final int KindTestSchemaElement = seq++;
	public static final int KindTestSchemaAttribute = seq++;
	public static final int KindTestPi = seq++;
	public static final int KindTestComment = seq++;
	public static final int KindTestText = seq++;
	public static final int KindTestNamespaceNode = seq++;
	public static final int WildcardBeforeColon = seq++;
	public static final int WildcardAfterColon = seq++;
	public static final int EmptySequence = seq++;
	public static final int ContextItemExpr = seq++;
	public static final int PlaceHolder = seq++;
	public static final int OrderedExpr = seq++;
	public static final int UnorderedExpr = seq++;
	public static final int Int = seq++;
	public static final int Str = seq++;
	public static final int Qname = seq++;
	public static final int Dbl = seq++;
	public static final int Dec = seq++;
	public static final int Annotation = seq++;
	public static final int FunctionTest = seq++;
	public static final int AnyFunctionType = seq++;
	public static final int TypedFunctionType = seq++;
	public static final int SingleType = seq++;
	public static final int Optional = seq++;
	public static final int SomeQuantifier = seq++;
	public static final int EveryQuantifier = seq++;
	public static final int QuantifiedExpr = seq++;
	public static final int SwitchExpr = seq++;
	public static final int SwitchClause = seq++;
	public static final int TypeSwitch = seq++;
	public static final int TypeSwitchCase = seq++;
	public static final int IfExpr = seq++;
	public static final int TryCatchExpr = seq++;
	public static final int CatchClause = seq++;
	public static final int CatchErrorList = seq++;
	public static final int CatchVar = seq++;
	public static final int ExtensionExpr = seq++;
	public static final int Pragma = seq++;
	public static final int PragmaContent = seq++;
	public static final int ValidateExpr = seq++;
	public static final int ValidateLax = seq++;
	public static final int ValidateStrict = seq++;
	public static final int LiteralFuncItem = seq++;
	public static final int InlineFuncItem = seq++;
	public static final int TypedVariableDeclaration = seq++;
	public static final int CompElementConstructor = seq++;
	public static final int CompAttributeConstructor = seq++;
	public static final int CompCommentConstructor = seq++;
	public static final int ContentSequence = seq++;
	public static final int DirPIConstructor = seq++;
	public static final int PITarget = seq++;

	public static final String NAMES[] = new String[] { "XQuery",
			"LibraryModule", "MainModule", "NamespaceDeclaration", "Literal",
			"QueryBody", "SequenceExpr", "FlowrExpr", "ForClause",
			"AllowingEmpty", "TypedVariableBinding", "Variable", "LetClause",
			"WhereClause", "GroupByClause", "GroupBySpec", "VariableRef",
			"Collation", "OrderByClause", "OrderBySpec", "OrderByKind",
			"ASCENDING", "DESCENDING", "OrderByEmptyMode", "GREATEST", "LEAST",
			"CountClause", "ReturnExpr", "OrExpr", "AndExpr", "GeneralCompEQ",
			"GeneralCompNE", "GeneralCompLT", "GeneralCompLE", "GeneralCompGT",
			"GeneralCompGE", "ValueCompEQ", "ValueCompNE", "ValueCompLT",
			"ValueCompLE", "ValueCompGT", "ValueCompGE", "NodeCompIs",
			"NodeCompPrecedes", "NodeCompFollows", "ComparisonExpr",
			"RangeExpr", "AddOp", "SubtractOp", "ArithmeticExpr", "MultiplyOp",
			"DivideOp", "IDivideOp", "UnionExpr", "IntersectExpr",
			"ExceptExpr", "InstanceofExpr", "SequenceType",
			"EmptySequenceType", "ItemType", "CardinalityZeroOrOne",
			"CardinalityZeroOrMany", "CardinalityOneOrMany", "TreatExpr",
			"CastableExpr", "CastExpr", "FunctionCall", "PathExpr", "StepExpr",
			"AxisSpec", "SELF", "CHILD", "DESCENDANT", "DESCENDANT_OR_SELF",
			"ATTRIBUTE", "FOLLOWING_SIBLING", "FOLLOWING", "PARENT",
			"ANCESTOR", "PRECEDING_SIBLING", "PRECEDING", "ANCESTOR_OR_SELF",
			"KindTestAnyKind", "KindTestDocument", "NameTest", "Wildcard",
			"Nilled", "KindTestElement", "KindTestAttribute",
			"KindTestSchemaElement", "KindTestSchemaAttribute", "KindTestPi",
			"KindTestComment", "KindTestText", "KindTestNamespaceNode",
			"WildcardBeforeColon", "WildcardAfterColon", "EmptySequence",
			"ContextItemExpr", "PlaceHolder", "OrderedExpr", "UnorderedExpr",
			"Int", "Str", "Qname", "Dbl", "Dec", "Annotation", "FunctionTest",
			"AnyFunctionType", "TypedFunctionType", "SingleType", "Optional",
			"SomeQuantifier", "EveryQuantifier", "QuantifiedExpr",
			"SwitchExpr", "SwitchClause", "TypeSwitch", "TypeSwitchCase",
			"IfExpr", "TryCatchExpr", "CatchClause", "CatchErrorList",
			"CatchVar", "ExtensionExpr", "Pragma", "PragmaContent",
			"ValidateExpr", "ValidateLax", "ValidateStrict", "LiteralFuncItem",
			"InlineFuncItem", "TypedVariableDeclaration",
			"CompElementConstructor", "CompAttributeConstructor", "CompCommentConstructor",
			"ContentSequence", "DirPIConstructor", "PITarget"};
	public static final int CompDocumentConstructor = 0;
	public static final int CompTextConstructor = 0;

	private XQAST parent;

	private int type;

	private String value;

	private Map<String, String> properties;

	private XQAST[] children;

	public XQAST(int type, String value, Map<String, String> properties) {
		this.type = type;
		this.value = value;
		this.properties = properties;
	}

	public XQAST(int type) {
		this.type = type;
		this.value = XQAST.NAMES[type];
	}

	public XQAST(int type, String value) {
		this.type = type;
		this.value = value;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setProperty(String name, String value) {
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put(name, value);
	}

	public String getProperty(String name) {
		return (properties != null) ? properties.get(name) : null;
	}

	public XQAST getParent() {
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
		for (XQAST XQAST : parent.children) {
			if (XQAST == this) {
				return i;
			}
			i++;
		}
		throw new IllegalStateException();
	}

	public void addChildren(XQAST[] children) {
		for (XQAST child : children) {
			addChild(child);
		}
	}

	public void addChild(XQAST child) {
		if (child == null) {
			throw new NullPointerException();
		}
		if (child == this) {
			throw new IllegalArgumentException();
		}
		if (children == null) {
			children = new XQAST[] { child };
		} else {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = child;
		}
		child.parent = this;
	}

	public void insertChild(int position, XQAST child) {
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
			children = new XQAST[] { child };
		} else if (position == children.length) {
			children = Arrays.copyOf(children, children.length + 1);
			children[children.length - 1] = child;
		} else {
			XQAST[] tmp = new XQAST[children.length + 1];
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

	public XQAST getChild(int position) {
		if ((position < 0) || (children == null)
				|| (position >= children.length)) {
			throw new IllegalArgumentException(String.format(
					"Illegal child position: %s", position));
		}
		return children[position];
	}

	public void replaceChild(int position, XQAST child) {
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
			XQAST[] tmp = new XQAST[children.length - 1];
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

	public XQAST copy() {
		return new XQAST(type, value, (properties == null) ? null
				: new HashMap<String, String>(properties));
	}

	public XQAST copyTree() {
		XQAST copy = copy();
		if (children != null) {
			copy.children = new XQAST[children.length];
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
		String label = toString();
		DotNode node = dt.addNode(String.valueOf(myNo));
		node.addRow(label, null);
		if (properties != null) {
			for (Entry<String, String> prop : properties.entrySet()) {
				node.addRow(prop.getKey(), prop.getValue());
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

	public XQAST getFirstChildWithType(int type) {
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

	public void display() {
		try {
			File file = File.createTempFile("XQAST", ".dot");
			file.deleteOnExit();
			dot(file);
			Runtime.getRuntime().exec(
					new String[] { "/usr/bin/dotty", file.getAbsolutePath() })
					.waitFor();
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		return ((type > 0) && (type < NAMES.length)) ? (NAMES[type]
				.equals(value)) ? value : NAMES[type] + "[" + value + "]"
				: value;
	}
}
