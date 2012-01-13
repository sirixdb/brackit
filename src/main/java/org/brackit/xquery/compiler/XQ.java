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
package org.brackit.xquery.compiler;

import java.lang.reflect.Field;

/**
 * <p>
 * {@link AST} node type constants for XQuery ASTs.
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
/*
 * Update number assignment with awk '/= [0-9]+/{sub(/= [0-9]+/, sprintf("= %i", cnt++));} 1' XQ.java > XQ.tmp && mv XQ.tmp XQ.java
 */
public final class XQ {

	public static final int XQuery = 0;
	public static final int LibraryModule = 1;
	public static final int MainModule = 2;
	public static final int Prolog = 3;
	public static final int NamespaceDeclaration = 4;
	public static final int QueryBody = 5;
	public static final int SequenceExpr = 6;
	public static final int FlowrExpr = 7;
	public static final int ForClause = 8;
	public static final int AllowingEmpty = 9;
	public static final int TypedVariableBinding = 10;
	public static final int Variable = 11;
	public static final int LetClause = 12;
	public static final int WhereClause = 13;
	public static final int GroupByClause = 14;
	public static final int GroupBySpec = 15;
	public static final int VariableRef = 16;
	public static final int Collation = 17;
	public static final int OrderByClause = 18;
	public static final int OrderBySpec = 19;
	public static final int OrderByKind = 20;
	public static final int ASCENDING = 21;
	public static final int DESCENDING = 22;
	public static final int OrderByEmptyMode = 23;
	public static final int GREATEST = 24;
	public static final int LEAST = 25;
	public static final int CountClause = 26;
	public static final int ReturnClause = 27;
	public static final int OrExpr = 28;
	public static final int AndExpr = 29;
	public static final int GeneralCompEQ = 30;
	public static final int GeneralCompNE = 31;
	public static final int GeneralCompLT = 32;
	public static final int GeneralCompLE = 33;
	public static final int GeneralCompGT = 34;
	public static final int GeneralCompGE = 35;
	public static final int ValueCompEQ = 36;
	public static final int ValueCompNE = 37;
	public static final int ValueCompLT = 38;
	public static final int ValueCompLE = 39;
	public static final int ValueCompGT = 40;
	public static final int ValueCompGE = 41;
	public static final int NodeCompIs = 42;
	public static final int NodeCompPrecedes = 43;
	public static final int NodeCompFollows = 44;
	public static final int ComparisonExpr = 45;
	public static final int RangeExpr = 46;
	public static final int AddOp = 47;
	public static final int SubtractOp = 48;
	public static final int ArithmeticExpr = 49;
	public static final int MultiplyOp = 50;
	public static final int DivideOp = 51;
	public static final int IDivideOp = 52;
	public static final int ModulusOp = 53;
	public static final int UnionExpr = 54;
	public static final int IntersectExpr = 55;
	public static final int ExceptExpr = 56;
	public static final int InstanceofExpr = 57;
	public static final int SequenceType = 58;
	public static final int EmptySequenceType = 59;
	public static final int ItemType = 60;
	public static final int AtomicOrUnionType = 61;
	public static final int CardinalityOne = 62;
	public static final int CardinalityZeroOrOne = 63;
	public static final int CardinalityZeroOrMany = 64;
	public static final int CardinalityOneOrMany = 65;
	public static final int TreatExpr = 66;
	public static final int CastableExpr = 67;
	public static final int CastExpr = 68;
	public static final int FunctionCall = 69;
	public static final int PathExpr = 70;
	public static final int StepExpr = 71;
	public static final int AxisSpec = 72;
	public static final int SELF = 73;
	public static final int CHILD = 74;
	public static final int DESCENDANT = 75;
	public static final int DESCENDANT_OR_SELF = 76;
	public static final int ATTRIBUTE = 77;
	public static final int FOLLOWING_SIBLING = 78;
	public static final int FOLLOWING = 79;
	public static final int PARENT = 80;
	public static final int ANCESTOR = 81;
	public static final int PRECEDING_SIBLING = 82;
	public static final int PRECEDING = 83;
	public static final int ANCESTOR_OR_SELF = 84;
	public static final int KindTestAnyKind = 85;
	public static final int KindTestDocument = 86;
	public static final int NameTest = 87;
	public static final int Wildcard = 88;
	public static final int Nilled = 89;
	public static final int KindTestElement = 90;
	public static final int KindTestAttribute = 91;
	public static final int KindTestSchemaElement = 92;
	public static final int KindTestSchemaAttribute = 93;
	public static final int KindTestPi = 94;
	public static final int KindTestComment = 95;
	public static final int KindTestText = 96;
	public static final int KindTestNamespaceNode = 97;
	public static final int NSWildcardNameTest = 98;
	public static final int NSNameWildcardTest = 99;
	public static final int ParenthesizedExpr = 100;
	public static final int ContextItemExpr = 101;
	public static final int ArgumentPlaceHolder = 102;
	public static final int OrderedExpr = 103;
	public static final int UnorderedExpr = 104;
	public static final int Int = 105;
	public static final int Str = 106;
	public static final int QNm = 107;
	public static final int Dbl = 108;
	public static final int Dec = 109;
	public static final int AnyURI = 110;
	public static final int Bool = 111;
	public static final int Annotation = 112;
	public static final int FunctionTest = 113;
	public static final int AnyFunctionType = 114;
	public static final int TypedFunctionType = 115;
	public static final int SomeQuantifier = 116;
	public static final int EveryQuantifier = 117;
	public static final int QuantifiedExpr = 118;
	public static final int SwitchExpr = 119;
	public static final int SwitchClause = 120;
	public static final int TypeSwitch = 121;
	public static final int TypeSwitchCase = 122;
	public static final int IfExpr = 123;
	public static final int TryCatchExpr = 124;
	public static final int CatchClause = 125;
	public static final int CatchErrorList = 126;
	public static final int ExtensionExpr = 127;
	public static final int Pragma = 128;
	public static final int PragmaContent = 129;
	public static final int ValidateExpr = 130;
	public static final int ValidateLax = 131;
	public static final int ValidateStrict = 132;
	public static final int LiteralFuncItem = 133;
	public static final int InlineFuncItem = 134;
	public static final int TypedVariableDeclaration = 135;
	public static final int CompElementConstructor = 136;
	public static final int CompAttributeConstructor = 137;
	public static final int CompCommentConstructor = 138;
	public static final int ContentSequence = 139;
	public static final int EnclosedExpr = 140;
	public static final int DirElementConstructor = 141;
	public static final int DirAttributeConstructor = 142;
	public static final int DirCommentConstructor = 143;
	public static final int DirPIConstructor = 144;
	public static final int CompDocumentConstructor = 145;
	public static final int CompTextConstructor = 146;
	public static final int DefaultElementNamespace = 147;
	public static final int DefaultFunctionNamespace = 148;
	public static final int SchemaImport = 149;
	public static final int ModuleImport = 150;
	public static final int Namespace = 151;
	public static final int ContextItemDeclaration = 152;
	public static final int ExternalVariable = 153;
	public static final int AnnotatedDecl = 154;
	public static final int FunctionDecl = 155;
	public static final int ExternalFunction = 156;
	public static final int BoundarySpaceDeclaration = 157;
	public static final int BoundarySpaceModePreserve = 158;
	public static final int BoundarySpaceModeStrip = 159;
	public static final int CollationDeclaration = 160;
	public static final int BaseURIDeclaration = 161;
	public static final int ConstructionDeclaration = 162;
	public static final int ConstructionModePreserve = 163;
	public static final int ConstructionModeStrip = 164;
	public static final int OrderingModeDeclaration = 165;
	public static final int OrderingModeOrdered = 166;
	public static final int OrderingModeUnordered = 167;
	public static final int EmptyOrderDeclaration = 168;
	public static final int EmptyOrderModeGreatest = 169;
	public static final int EmptyOrderModeLeast = 170;
	public static final int CopyNamespacesDeclaration = 171;
	public static final int CopyNamespacesPreserveModePreserve = 172;
	public static final int CopyNamespacesPreserveModeNoPreserve = 173;
	public static final int CopyNamespacesInheritModeInherit = 174;
	public static final int CopyNamespacesInheritModeNoInherit = 175;
	public static final int DecimalFormatDeclaration = 176;
	public static final int DecimalFormatDefault = 177;
	public static final int DecimalFormatProperty = 178;
	public static final int DecimalFormatPropertyDecimalSeparator = 179;
	public static final int DecimalFormatPropertyGroupingSeparator = 180;
	public static final int DecimalFormatPropertyInfinity = 181;
	public static final int DecimalFormatPropertyMinusSign = 182;
	public static final int DecimalFormatPropertyNaN = 183;
	public static final int DecimalFormatPropertyPercent = 184;
	public static final int DecimalFormatPropertyPerMille = 185;
	public static final int DecimalFormatPropertyZeroDigit = 186;
	public static final int DecimalFormatPropertyDigit = 187;
	public static final int DecimalFormatPropertyPatternSeparator = 188;
	public static final int OptionDeclaration = 189;
	public static final int CompNamespaceConstructor = 190;
	public static final int CompPIConstructor = 191;
	public static final int Predicate = 192;
	public static final int FilterExpr = 193;
	public static final int DynamicFunctionCallExpr = 194;
	public static final int SlidingWindowClause = 195;
	public static final int TumblingWindowClause = 196;
	public static final int WindowStartCondition = 197;
	public static final int WindowEndCondition = 198;
	public static final int WindowVars = 199;
	public static final int PreviousItemBinding = 200;
	public static final int NextItemBinding = 201;
	// Begin XQuery Update Facility 1.0
	public static final int RevalidationDeclaration = 202;
	public static final int RevalidationModeStrict = 203;
	public static final int RevalidationModeLax = 204;
	public static final int RevalidationModeSkip = 205;
	public static final int InsertExpr = 206;
	public static final int InsertFirst = 207;
	public static final int InsertLast = 208;
	public static final int InsertAfter = 209;
	public static final int InsertBefore = 210;
	public static final int InsertInto = 211;
	public static final int DeleteExpr = 212;
	public static final int ReplaceValueExpr = 213;
	public static final int ReplaceNodeExpr = 214;
	public static final int RenameExpr = 215;
	public static final int TransformExpr = 216;
	public static final int CopyVariableBinding = 217;
	// Begin XQuery Update Facility 1.0
	// Begin brackit's set-oriented extensions
	public static final int PipeExpr = 218;
	public static final int Selection = 219;
	public static final int GroupBy = 220;
	public static final int OrderBy = 221;
	public static final int Join = 222;
	public static final int JoinClause = 223;
	public static final int Start = 224;
	public static final int ForBind = 225;
	public static final int LetBind = 226;
	public static final int Count = 227;
	// End brackit's set-oriented extensions

	public static final String NAMES[] = new String[] { "XQuery",
			"LibraryModule", "MainModule", "Prolog", "NamespaceDeclaration",
			"QueryBody", "SequenceExpr", "FlowrExpr", "ForClause",
			"AllowingEmpty", "TypedVariableBinding", "Variable", "LetClause",
			"WhereClause", "GroupByClause", "GroupBySpec", "VariableRef",
			"Collation", "OrderByClause", "OrderBySpec", "OrderByKind",
			"ASCENDING", "DESCENDING", "OrderByEmptyMode", "GREATEST", "LEAST",
			"CountClause", "ReturnClause", "OrExpr", "AndExpr",
			"GeneralCompEQ", "GeneralCompNE", "GeneralCompLT", "GeneralCompLE",
			"GeneralCompGT", "GeneralCompGE", "ValueCompEQ", "ValueCompNE",
			"ValueCompLT", "ValueCompLE", "ValueCompGT", "ValueCompGE",
			"NodeCompIs", "NodeCompPrecedes", "NodeCompFollows",
			"ComparisonExpr", "RangeExpr", "AddOp", "SubtractOp",
			"ArithmeticExpr", "MultiplyOp", "DivideOp", "IDivideOp",
			"ModulusOp", "UnionExpr", "IntersectExpr", "ExceptExpr",
			"InstanceofExpr", "SequenceType", "EmptySequenceType", "ItemType",
			"AtomicOrUnionType", "CardinalityOne", "CardinalityZeroOrOne",
			"CardinalityZeroOrMany", "CardinalityOneOrMany", "TreatExpr",
			"CastableExpr", "CastExpr", "FunctionCall", "PathExpr", "StepExpr",
			"AxisSpec", "SELF", "CHILD", "DESCENDANT", "DESCENDANT_OR_SELF",
			"ATTRIBUTE", "FOLLOWING_SIBLING", "FOLLOWING", "PARENT",
			"ANCESTOR", "PRECEDING_SIBLING", "PRECEDING", "ANCESTOR_OR_SELF",
			"KindTestAnyKind", "KindTestDocument", "NameTest", "Wildcard",
			"Nilled", "KindTestElement", "KindTestAttribute",
			"KindTestSchemaElement", "KindTestSchemaAttribute", "KindTestPi",
			"KindTestComment", "KindTestText", "KindTestNamespaceNode",
			"NSWildcardNameTest", "NSNameWildcardTest", "ParenthesizedExpr",
			"ContextItemExpr", "ArgumentPlaceHolder", "OrderedExpr",
			"UnorderedExpr", "Int", "Str", "QNm", "Dbl", "Dec", "AnyURI",
			"Bool", "Annotation", "FunctionTest", "AnyFunctionType",
			"TypedFunctionType", "SomeQuantifier", "EveryQuantifier",
			"QuantifiedExpr", "SwitchExpr", "SwitchClause", "TypeSwitch",
			"TypeSwitchCase", "IfExpr", "TryCatchExpr", "CatchClause",
			"CatchErrorList", "ExtensionExpr", "Pragma",
			"PragmaContent", "ValidateExpr", "ValidateLax", "ValidateStrict",
			"LiteralFuncItem", "InlineFuncItem", "TypedVariableDeclaration",
			"CompElementConstructor", "CompAttributeConstructor",
			"CompCommentConstructor", "ContentSequence", "EnclosedExpr",
			"DirElementConstructor", "DirAttributeConstructor",
			"DirCommentConstructor", "DirPIConstructor",
			"CompDocumentConstructor", "CompTextConstructor",
			"DefaultElementNamespace", "DefaultFunctionNamespace",
			"SchemaImport", "ModuleImport", "Namespace",
			"ContextItemDeclaration", "ExternalVariable", "AnnotatedDecl",
			"FunctionDecl", "ExternalFunction", "BoundarySpaceDeclaration",
			"BoundarySpaceModePreserve", "BoundarySpaceModeStrip",
			"CollationDeclaration", "BaseURIDeclaration",
			"ConstructionDeclaration", "ConstructionModePreserve",
			"ConstructionModeStrip", "OrderingModeDeclaration",
			"OrderingModeOrdered", "OrderingModeUnordered",
			"EmptyOrderDeclaration", "EmptyOrderModeGreatest",
			"EmptyOrderModeLeast", "CopyNamespacesDeclaration",
			"CopyNamespacesPreserveModePreserve",
			"CopyNamespacesPreserveModeNoPreserve",
			"CopyNamespacesInheritModeInherit",
			"CopyNamespacesInheritModeNoInherit", "DecimalFormatDeclaration",
			"DecimalFormatDefault", "DecimalFormatProperty",
			"DecimalFormatPropertyDecimalSeparator",
			"DecimalFormatPropertyGroupingSeparator",
			"DecimalFormatPropertyInfinity", "DecimalFormatPropertyMinusSign",
			"DecimalFormatPropertyNaN", "DecimalFormatPropertyPercent",
			"DecimalFormatPropertyPerMille", "DecimalFormatPropertyZeroDigit",
			"DecimalFormatPropertyDigit",
			"DecimalFormatPropertyPatternSeparator", "OptionDeclaration",
			"CompNamespaceConstructor", "CompPIConstructor", "Predicate",
			"FilterExpr", "DynamicFunctionCallExpr",
			"SlidingWindowClause",
			"TumblingWindowClause",
			"WindowStartCondition",
			"WindowEndCondition",
			"WindowVars",
			"PreviousItemBinding",
			"NextItemBinding",
			// Begin XQuery Update Facility 1.0
			"RevalidationDeclaration", "RevalidationModeStrict",
			"RevalidationModeLax", "RevalidationModeSkip", "InsertExpr",
			"InsertFirst", "InsertLast", "InsertAfter", "InsertBefore",
			"InsertInto", "DeleteExpr", "ReplaceValueExpr", "ReplaceNodeExpr",
			"RenameExpr", "TransformExpr",
			"CopyVariableBinding",
			// End XQuery Update Facility 1.0
			// Begin brackit's set-oriented extensions
			"PipeExpr", "Selection", "GroupBy", "OrderBy", "Join",
			"JoinClause", "Start", "ForBind", "LetBind", "Count"
	// End brackit's set-oriented extensions
	};

	private XQ() {
	}

	private static int base = 228;
	
	public static synchronized int allocate(int noOfTokens) {
		int r = base;
		base += noOfTokens;
		return r;
	}

	public static void main(String[] args) throws Exception {
		XQ xq = new XQ();
		for (Field f : XQ.class.getFields()) {
			if ((f.getType().isPrimitive())
					&& (f.getType().equals(Integer.TYPE))) {
				int pos = f.getInt(xq);
				String val = NAMES[pos];
				if (!val.equals(f.getName())) {
					System.out.println(String.format(
							"Mismatch at %s: %s != %s", pos, f.getName(), val));
				}
			}
		}
	}
}
