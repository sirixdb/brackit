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

/**
 * @author Sebastian Baechle
 *
 */
public class XQ {

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
	public static final int ArgumentPlaceHolder = seq++;
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
	public static final int CompDocumentConstructor = seq++;
	public static final int CompTextConstructor = seq++;
	public static final int DefaultElementNamespace = seq++;
	public static final int DefaultFunctionNamespace = seq++;
	public static final int SchemaImport = seq++;
	public static final int ModuleImport = seq++;
	public static final int Namespace = seq++;
	public static final int ContextItemDeclaration = seq++;
	public static final int ExternalVariable = seq++;
	public static final int AnnotatedDecl = seq++;
	public static final int ExternalFunction = seq++;
	public static final int BoundarySpaceDeclaration = seq++;
	public static final int BoundarySpaceModePreserve = seq++;
	public static final int BoundarySpaceModeStrip = seq++;
	public static final int CollationDeclaration = seq++;
	public static final int BaseURIDeclaration = seq++;
	public static final int ConstructionDeclaration = seq++;
	public static final int ConstructionModePreserve = seq++;
	public static final int ConstructionModeStrip = seq++;
	public static final int OrderingModeDeclaration = seq++;
	public static final int OrderingModeOrdered = seq++;
	public static final int OrderingModeUnordered = seq++;
	public static final int EmptyOrderDeclaration = seq++;
	public static final int EmptyOrderModeGreatest = seq++;
	public static final int EmptyOrderModeLeast = seq++;
	public static final int CopyNamespacesDeclaration = seq++;
	public static final int CopyNamespacesPreserveModePreserve = seq++;
	public static final int CopyNamespacesPreserveModeNoPreserve = seq++;
	public static final int CopyNamespacesInheritModeInherit = seq++;
	public static final int CopyNamespacesInheritModeNoInherit = seq++;
	public static final int DecimalFormatDeclaration = seq++;
	public static final int DecimalFormatProperty = seq++;
	public static final int DecimalFormatPropertyDecimalSeparator = seq++;
	public static final int DecimalFormatPropertyGroupingSeparator = seq++;
	public static final int DecimalFormatPropertyInfinity = seq++;
	public static final int DecimalFormatPropertyMinusSign = seq++;
	public static final int DecimalFormatPropertyNaN = seq++;
	public static final int DecimalFormatPropertyPercent = seq++;
	public static final int DecimalFormatPropertyPerMille = seq++;
	public static final int DecimalFormatPropertyZeroDigit = seq++;
	public static final int DecimalFormatPropertyDigit = seq++;
	public static final int DecimalFormatPropertyPatternSeparator = seq++;
	public static final int OptionDeclaration = seq++;
	public static final int CompNamespaceConstructor = seq++;
	public static final int CompProcessingInstructionConstructor = seq++;
	public static final int Predicate = seq++;
	public static final int FilterExpr = seq++;
	public static final int DynamicFunctionCallExpr = seq++;
	public static final int SlidingWindowClause = seq++;
	public static final int TumblingWindowClause = seq++;
	public static final int WindowStartCondition = seq++;
	public static final int WindowEndCondition = seq++;
	public static final int PreviousItemBinding = seq++;
	public static final int NextItemBinding = seq++;
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
				"ContextItemExpr", "ArgumentPlaceHolder", "OrderedExpr",
				"UnorderedExpr", "Int", "Str", "Qname", "Dbl", "Dec", "Annotation",
				"FunctionTest", "AnyFunctionType", "TypedFunctionType",
				"SingleType", "Optional", "SomeQuantifier", "EveryQuantifier",
				"QuantifiedExpr", "SwitchExpr", "SwitchClause", "TypeSwitch",
				"TypeSwitchCase", "IfExpr", "TryCatchExpr", "CatchClause",
				"CatchErrorList", "CatchVar", "ExtensionExpr", "Pragma",
				"PragmaContent", "ValidateExpr", "ValidateLax", "ValidateStrict",
				"LiteralFuncItem", "InlineFuncItem", "TypedVariableDeclaration",
				"CompElementConstructor", "CompAttributeConstructor",
				"CompCommentConstructor", "ContentSequence", "DirPIConstructor",
				"PITarget", "DefaultElementNamespace", "DefaultFunctionNamespace",
				"SchemaImport", "ModuleImport", "Namespace",
				"ContextItemDeclaration", "ExternalVariable", "AnnotatedDecl",
				"ExternalFunction", "BoundarySpaceDeclaration",
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
				"Predicate", "FilterExpr", "DynamicFunctionCallExpr",
				"SlidingWindowClause", "TumblingWindowClause",
				"WindowStartCondition", "WindowEndCondition",
				"PreviousItemBinding", "NextItemBinding" };

	/**
	 * 
	 */
	public XQ() {
		super();
	}

}