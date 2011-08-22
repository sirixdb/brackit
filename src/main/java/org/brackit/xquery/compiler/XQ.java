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
 * Update number assignment with awk '/= [0-9]+/{sub(/= [0-9]+/, sprintf("= %i",
 * cnt++));} 1' XQ.java > XQ.tmp && mv XQ.tmp XQ.java
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
	public static final int CardinalityZeroOrOne = 62;
	public static final int CardinalityZeroOrMany = 63;
	public static final int CardinalityOneOrMany = 64;
	public static final int TreatExpr = 65;
	public static final int CastableExpr = 66;
	public static final int CastExpr = 67;
	public static final int FunctionCall = 68;
	public static final int PathExpr = 69;
	public static final int StepExpr = 70;
	public static final int AxisSpec = 71;
	public static final int SELF = 72;
	public static final int CHILD = 73;
	public static final int DESCENDANT = 74;
	public static final int DESCENDANT_OR_SELF = 75;
	public static final int ATTRIBUTE = 76;
	public static final int FOLLOWING_SIBLING = 77;
	public static final int FOLLOWING = 78;
	public static final int PARENT = 79;
	public static final int ANCESTOR = 80;
	public static final int PRECEDING_SIBLING = 81;
	public static final int PRECEDING = 82;
	public static final int ANCESTOR_OR_SELF = 83;
	public static final int KindTestAnyKind = 84;
	public static final int KindTestDocument = 85;
	public static final int NameTest = 86;
	public static final int Wildcard = 87;
	public static final int Nilled = 88;
	public static final int KindTestElement = 89;
	public static final int KindTestAttribute = 90;
	public static final int KindTestSchemaElement = 91;
	public static final int KindTestSchemaAttribute = 92;
	public static final int KindTestPi = 93;
	public static final int KindTestComment = 94;
	public static final int KindTestText = 95;
	public static final int KindTestNamespaceNode = 96;
	public static final int WildcardBeforeColon = 97;
	public static final int WildcardAfterColon = 98;
	public static final int EmptySequence = 99;
	public static final int ContextItemExpr = 100;
	public static final int ArgumentPlaceHolder = 101;
	public static final int OrderedExpr = 102;
	public static final int UnorderedExpr = 103;
	public static final int Int = 104;
	public static final int Str = 105;
	public static final int QNm = 106;
	public static final int Dbl = 107;
	public static final int Dec = 108;
	public static final int AnyURI = 109;
	public static final int Annotation = 110;
	public static final int FunctionTest = 111;
	public static final int AnyFunctionType = 112;
	public static final int TypedFunctionType = 113;
	public static final int SomeQuantifier = 114;
	public static final int EveryQuantifier = 115;
	public static final int QuantifiedExpr = 116;
	public static final int SwitchExpr = 117;
	public static final int SwitchClause = 118;
	public static final int TypeSwitch = 119;
	public static final int TypeSwitchCase = 120;
	public static final int IfExpr = 121;
	public static final int TryCatchExpr = 122;
	public static final int CatchClause = 123;
	public static final int CatchErrorList = 124;
	public static final int CatchVar = 125;
	public static final int ExtensionExpr = 126;
	public static final int Pragma = 127;
	public static final int PragmaContent = 128;
	public static final int ValidateExpr = 129;
	public static final int ValidateLax = 130;
	public static final int ValidateStrict = 131;
	public static final int LiteralFuncItem = 132;
	public static final int InlineFuncItem = 133;
	public static final int TypedVariableDeclaration = 134;
	public static final int CompElementConstructor = 135;
	public static final int CompAttributeConstructor = 136;
	public static final int CompCommentConstructor = 137;
	public static final int ContentSequence = 138;
	public static final int DirPIConstructor = 139;
	public static final int PITarget = 140;
	public static final int CompDocumentConstructor = 141;
	public static final int CompTextConstructor = 142;
	public static final int DefaultElementNamespace = 143;
	public static final int DefaultFunctionNamespace = 144;
	public static final int SchemaImport = 145;
	public static final int ModuleImport = 146;
	public static final int Namespace = 147;
	public static final int ContextItemDeclaration = 148;
	public static final int ExternalVariable = 149;
	public static final int AnnotatedDecl = 150;
	public static final int FunctionDecl = 151;
	public static final int ExternalFunction = 152;
	public static final int BoundarySpaceDeclaration = 153;
	public static final int BoundarySpaceModePreserve = 154;
	public static final int BoundarySpaceModeStrip = 155;
	public static final int CollationDeclaration = 156;
	public static final int BaseURIDeclaration = 157;
	public static final int ConstructionDeclaration = 158;
	public static final int ConstructionModePreserve = 159;
	public static final int ConstructionModeStrip = 160;
	public static final int OrderingModeDeclaration = 161;
	public static final int OrderingModeOrdered = 162;
	public static final int OrderingModeUnordered = 163;
	public static final int EmptyOrderDeclaration = 164;
	public static final int EmptyOrderModeGreatest = 165;
	public static final int EmptyOrderModeLeast = 166;
	public static final int CopyNamespacesDeclaration = 167;
	public static final int CopyNamespacesPreserveModePreserve = 168;
	public static final int CopyNamespacesPreserveModeNoPreserve = 169;
	public static final int CopyNamespacesInheritModeInherit = 170;
	public static final int CopyNamespacesInheritModeNoInherit = 171;
	public static final int DecimalFormatDeclaration = 172;
	public static final int DecimalFormatProperty = 173;
	public static final int DecimalFormatPropertyDecimalSeparator = 174;
	public static final int DecimalFormatPropertyGroupingSeparator = 175;
	public static final int DecimalFormatPropertyInfinity = 176;
	public static final int DecimalFormatPropertyMinusSign = 177;
	public static final int DecimalFormatPropertyNaN = 178;
	public static final int DecimalFormatPropertyPercent = 179;
	public static final int DecimalFormatPropertyPerMille = 180;
	public static final int DecimalFormatPropertyZeroDigit = 181;
	public static final int DecimalFormatPropertyDigit = 182;
	public static final int DecimalFormatPropertyPatternSeparator = 183;
	public static final int OptionDeclaration = 184;
	public static final int CompNamespaceConstructor = 185;
	public static final int CompProcessingInstructionConstructor = 186;
	public static final int Predicate = 187;
	public static final int FilterExpr = 188;
	public static final int DynamicFunctionCallExpr = 189;
	public static final int SlidingWindowClause = 190;
	public static final int TumblingWindowClause = 191;
	public static final int WindowStartCondition = 192;
	public static final int WindowEndCondition = 193;
	public static final int PreviousItemBinding = 194;
	public static final int NextItemBinding = 195;
	// Begin XQuery Update Facility 1.0
	public static final int RevalidationDeclaration = 196;
	public static final int RevalidationModeStrict = 197;
	public static final int RevalidationModeLax = 198;
	public static final int RevalidationModeSkip = 199;
	public static final int InsertExpr = 200;
	public static final int InsertFirst = 201;
	public static final int InsertLast = 202;
	public static final int InsertAfter = 203;
	public static final int InsertBefore = 204;
	public static final int InsertInto = 205;
	public static final int DeleteExpr = 206;
	public static final int ReplaceValueExpr = 207;
	public static final int ReplaceNodeExpr = 208;
	public static final int RenameExpr = 209;
	public static final int TransformExpr = 210;
	public static final int CopyVariableBinding = 211;
	// Begin XQuery Update Facility 1.0
	// Begin brackit's set-oriented extensions
	public static final int ReturnExpr = 212;
	public static final int Selection = 213;
	public static final int GroupBy = 214;
	public static final int OrderBy = 215;
	public static final int Join = 216;
	public static final int JoinClause = 217;
	public static final int Start = 218;
	public static final int ForBind = 219;
	public static final int LetBind = 220;
	public static final int Count = 221;
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
			"AtomicOrUnionType", "CardinalityZeroOrOne",
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
			"ContextItemExpr", "ArgumentPlaceHolder", "OrderedExpr",
			"UnorderedExpr", "Int", "Str", "QNm", "Dbl", "Dec", "AnyURI",
			"Annotation", "FunctionTest", "AnyFunctionType",
			"TypedFunctionType", "SomeQuantifier", "EveryQuantifier",
			"QuantifiedExpr", "SwitchExpr", "SwitchClause", "TypeSwitch",
			"TypeSwitchCase", "IfExpr", "TryCatchExpr", "CatchClause",
			"CatchErrorList", "CatchVar", "ExtensionExpr", "Pragma",
			"PragmaContent", "ValidateExpr", "ValidateLax", "ValidateStrict",
			"LiteralFuncItem", "InlineFuncItem", "TypedVariableDeclaration",
			"CompElementConstructor", "CompAttributeConstructor",
			"CompCommentConstructor", "ContentSequence", "DirPIConstructor",
			"PITarget", "CompDocumentConstructor", "CompTextConstructor",
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
			"DecimalFormatProperty", "DecimalFormatPropertyDecimalSeparator",
			"DecimalFormatPropertyGroupingSeparator",
			"DecimalFormatPropertyInfinity", "DecimalFormatPropertyMinusSign",
			"DecimalFormatPropertyNaN", "DecimalFormatPropertyPercent",
			"DecimalFormatPropertyPerMille", "DecimalFormatPropertyZeroDigit",
			"DecimalFormatPropertyDigit",
			"DecimalFormatPropertyPatternSeparator", "OptionDeclaration",
			"CompNamespaceConstructor", "CompProcessingInstructionConstructor",
			"Predicate", "FilterExpr",
			"DynamicFunctionCallExpr",
			"SlidingWindowClause",
			"TumblingWindowClause",
			"WindowStartCondition",
			"WindowEndCondition",
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
			"ReturnExpr", "Selection", "GroupBy", "OrderBy", "Join",
			"JoinClause", "Start", "ForBind", "LetBind", "Count"
	// End brackit's set-oriented extensions
	};

	private XQ() {
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
