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
  public static final int AggregateSpec = 16;
  public static final int DftAggregateSpec = 17;
  public static final int AggregateBinding = 18;
  public static final int SequenceAgg = 19;
  public static final int CountAgg = 20;
  public static final int SumAgg = 21;
  public static final int AvgAgg = 22;
  public static final int MinAgg = 23;
  public static final int MaxAgg = 24;
  public static final int SingleAgg = 25;
  public static final int VariableRef = 26;
  public static final int Collation = 27;
  public static final int OrderByClause = 28;
  public static final int OrderBySpec = 29;
  public static final int OrderByKind = 30;
  public static final int ASCENDING = 31;
  public static final int DESCENDING = 32;
  public static final int OrderByEmptyMode = 33;
  public static final int GREATEST = 34;
  public static final int LEAST = 35;
  public static final int CountClause = 36;
  public static final int ReturnClause = 37;
  public static final int OrExpr = 38;
  public static final int AndExpr = 39;
  public static final int GeneralCompEQ = 40;
  public static final int GeneralCompNE = 41;
  public static final int GeneralCompLT = 42;
  public static final int GeneralCompLE = 43;
  public static final int GeneralCompGT = 44;
  public static final int GeneralCompGE = 45;
  public static final int ValueCompEQ = 46;
  public static final int ValueCompNE = 47;
  public static final int ValueCompLT = 48;
  public static final int ValueCompLE = 49;
  public static final int ValueCompGT = 50;
  public static final int ValueCompGE = 51;
  public static final int NodeCompIs = 52;
  public static final int NodeCompPrecedes = 53;
  public static final int NodeCompFollows = 54;
  public static final int ComparisonExpr = 55;
  public static final int StringConcatExpr = 56;
  public static final int RangeExpr = 57;
  public static final int AddOp = 58;
  public static final int SubtractOp = 59;
  public static final int ArithmeticExpr = 60;
  public static final int MultiplyOp = 61;
  public static final int DivideOp = 62;
  public static final int IDivideOp = 63;
  public static final int ModulusOp = 64;
  public static final int UnionExpr = 65;
  public static final int IntersectExpr = 66;
  public static final int ExceptExpr = 67;
  public static final int InstanceofExpr = 68;
  public static final int SequenceType = 69;
  public static final int EmptySequenceType = 70;
  public static final int ItemType = 71;
  public static final int AtomicOrUnionType = 72;
  public static final int CardinalityOne = 73;
  public static final int CardinalityZeroOrOne = 74;
  public static final int CardinalityZeroOrMany = 75;
  public static final int CardinalityOneOrMany = 76;
  public static final int TreatExpr = 77;
  public static final int CastableExpr = 78;
  public static final int CastExpr = 79;
  public static final int FunctionCall = 80;
  public static final int PathExpr = 81;
  public static final int MapExpr = 82;
  public static final int StepExpr = 83;
  public static final int AxisSpec = 84;
  public static final int SELF = 85;
  public static final int CHILD = 86;
  public static final int DESCENDANT = 87;
  public static final int DESCENDANT_OR_SELF = 88;
  public static final int ATTRIBUTE = 89;
  public static final int FOLLOWING_SIBLING = 90;
  public static final int FOLLOWING = 91;
  public static final int PARENT = 92;
  public static final int ANCESTOR = 93;
  public static final int PRECEDING_SIBLING = 94;
  public static final int PRECEDING = 95;
  public static final int ANCESTOR_OR_SELF = 96;
  public static final int KindTestAnyKind = 97;
  public static final int KindTestDocument = 98;
  public static final int NameTest = 99;
  public static final int Wildcard = 100;
  public static final int Nilled = 101;
  public static final int KindTestElement = 102;
  public static final int KindTestAttribute = 103;
  public static final int KindTestSchemaElement = 104;
  public static final int KindTestSchemaAttribute = 105;
  public static final int KindTestPi = 106;
  public static final int KindTestComment = 107;
  public static final int KindTestText = 108;
  public static final int KindTestNamespaceNode = 109;
  public static final int NSWildcardNameTest = 110;
  public static final int NSNameWildcardTest = 111;
  public static final int ParenthesizedExpr = 112;
  public static final int ContextItemExpr = 113;
  public static final int ArgumentPlaceHolder = 114;
  public static final int OrderedExpr = 115;
  public static final int UnorderedExpr = 116;
  public static final int Int = 117;
  public static final int Str = 118;
  public static final int QNm = 119;
  public static final int Dbl = 120;
  public static final int Dec = 121;
  public static final int AnyURI = 122;
  public static final int Bool = 123;
  public static final int Annotation = 124;
  public static final int FunctionTest = 125;
  public static final int AnyFunctionType = 126;
  public static final int TypedFunctionType = 127;
  public static final int SomeQuantifier = 128;
  public static final int EveryQuantifier = 129;
  public static final int QuantifiedExpr = 130;
  public static final int QuantifiedBinding = 131;
  public static final int SwitchExpr = 132;
  public static final int SwitchClause = 133;
  public static final int TypeSwitch = 134;
  public static final int TypeSwitchCase = 135;
  public static final int IfExpr = 136;
  public static final int TryCatchExpr = 137;
  public static final int CatchClause = 138;
  public static final int CatchErrorList = 139;
  public static final int ExtensionExpr = 140;
  public static final int Pragma = 141;
  public static final int PragmaContent = 142;
  public static final int ValidateExpr = 143;
  public static final int ValidateLax = 144;
  public static final int ValidateStrict = 145;
  public static final int LiteralFuncItem = 146;
  public static final int InlineFuncItem = 147;
  public static final int TypedVariableDeclaration = 148;
  public static final int CompElementConstructor = 149;
  public static final int CompAttributeConstructor = 150;
  public static final int CompCommentConstructor = 151;
  public static final int ContentSequence = 152;
  public static final int EnclosedExpr = 153;
  public static final int DirElementConstructor = 154;
  public static final int DirAttributeConstructor = 155;
  public static final int DirCommentConstructor = 156;
  public static final int DirPIConstructor = 157;
  public static final int CompDocumentConstructor = 158;
  public static final int CompTextConstructor = 159;
  public static final int DefaultElementNamespace = 160;
  public static final int DefaultFunctionNamespace = 161;
  public static final int SchemaImport = 162;
  public static final int ModuleImport = 163;
  public static final int Namespace = 164;
  public static final int ContextItemDeclaration = 165;
  public static final int ExternalVariable = 166;
  public static final int AnnotatedDecl = 167;
  public static final int FunctionDecl = 168;
  public static final int ExternalFunction = 169;
  public static final int BoundarySpaceDeclaration = 170;
  public static final int BoundarySpaceModePreserve = 171;
  public static final int BoundarySpaceModeStrip = 172;
  public static final int CollationDeclaration = 173;
  public static final int BaseURIDeclaration = 174;
  public static final int ConstructionDeclaration = 175;
  public static final int ConstructionModePreserve = 176;
  public static final int ConstructionModeStrip = 177;
  public static final int OrderingModeDeclaration = 178;
  public static final int OrderingModeOrdered = 179;
  public static final int OrderingModeUnordered = 180;
  public static final int EmptyOrderDeclaration = 181;
  public static final int EmptyOrderModeGreatest = 182;
  public static final int EmptyOrderModeLeast = 183;
  public static final int CopyNamespacesDeclaration = 184;
  public static final int CopyNamespacesPreserveModePreserve = 185;
  public static final int CopyNamespacesPreserveModeNoPreserve = 186;
  public static final int CopyNamespacesInheritModeInherit = 187;
  public static final int CopyNamespacesInheritModeNoInherit = 188;
  public static final int DecimalFormatDeclaration = 189;
  public static final int DecimalFormatDefault = 190;
  public static final int DecimalFormatProperty = 191;
  public static final int DecimalFormatPropertyDecimalSeparator = 192;
  public static final int DecimalFormatPropertyGroupingSeparator = 193;
  public static final int DecimalFormatPropertyInfinity = 194;
  public static final int DecimalFormatPropertyMinusSign = 195;
  public static final int DecimalFormatPropertyNaN = 196;
  public static final int DecimalFormatPropertyPercent = 197;
  public static final int DecimalFormatPropertyPerMille = 198;
  public static final int DecimalFormatPropertyZeroDigit = 199;
  public static final int DecimalFormatPropertyDigit = 200;
  public static final int DecimalFormatPropertyPatternSeparator = 201;
  public static final int OptionDeclaration = 202;
  public static final int CompNamespaceConstructor = 203;
  public static final int CompPIConstructor = 204;
  public static final int Predicate = 205;
  public static final int FilterExpr = 206;
  public static final int DynamicFunctionCallExpr = 207;
  public static final int SlidingWindowClause = 208;
  public static final int TumblingWindowClause = 209;
  public static final int WindowStartCondition = 210;
  public static final int WindowEndCondition = 211;
  public static final int WindowVars = 212;
  public static final int PreviousItemBinding = 213;
  public static final int NextItemBinding = 214;
  // Begin XQuery Update Facility 1.0
  public static final int RevalidationDeclaration = 215;
  public static final int RevalidationModeStrict = 216;
  public static final int RevalidationModeLax = 217;
  public static final int RevalidationModeSkip = 218;
  public static final int InsertExpr = 219;
  public static final int InsertFirst = 220;
  public static final int InsertLast = 221;
  public static final int InsertAfter = 222;
  public static final int InsertBefore = 223;
  public static final int InsertInto = 224;
  public static final int DeleteExpr = 225;
  public static final int ReplaceValueExpr = 226;
  public static final int ReplaceNodeExpr = 227;
  public static final int RenameExpr = 228;
  public static final int TransformExpr = 229;
  public static final int CopyVariableBinding = 230;
  // End XQuery Update Facility 1.0
  // Begin brackit's set-oriented extensions
  public static final int PipeExpr = 231;
  public static final int Selection = 232;
  public static final int GroupBy = 233;
  public static final int OrderBy = 234;
  public static final int Join = 235;
  public static final int JoinClause = 236;
  public static final int Start = 237;
  public static final int ForBind = 238;
  public static final int LetBind = 239;
  public static final int Count = 240;
  public static final int End = 241;
  // End brackit's set-oriented extensions
  // Begin brackit's custom array syntax
  public static final int KindTestArray = 242;
  public static final int ArrayAccess = 243;
  public static final int ArrayConstructor = 244;
  public static final int SequenceField = 245;
  public static final int FlattenedField = 246;
  public static final int ArrayIndexSlice = 247;
  // End brackit's custom array syntax
  // Begin brackit's custom object syntax
  public static final int KindTestObject = 248;
  public static final int ObjectProjection = 249;
  public static final int ObjectConstructor = 250;
  public static final int ObjectField = 251;
  public static final int KeyValueField = 252;
  public static final int DerefExpr = 253;
  // End brackit's custom object syntax
  // Begin temporal
  public static final int NEXT = 254;
  public static final int PREVIOUS = 255;
  public static final int FUTURE = 256;
  public static final int FUTURE_OR_SELF = 257;
  public static final int PAST = 258;
  public static final int PAST_OR_SELF = 259;
  public static final int FIRST = 260;
  public static final int LAST = 261;
  public static final int ALL_TIMES = 262;
  // End temporal
  public static final int JsonItemTest = 263;
  public static final int StructuredItemTest = 264;
  public static final int KindTestNull = 265;
  public static final int Null = 266;
  // Begin JSONiq update expressions
  public static final int InsertJsonExpr = 267;
  public static final int DeleteJsonExpr = 268;
  public static final int RenameJsonExpr = 269;
  public static final int ReplaceJsonExpr = 270;
  public static final int AppendJsonExpr = 271;
  // End JSONiq update expressions

  public static final String[] NAMES =
      new String[] { "XQuery", "LibraryModule", "MainModule", "Prolog", "NamespaceDeclaration", "QueryBody",
          "SequenceExpr", "FlowrExpr", "ForClause", "AllowingEmpty", "TypedVariableBinding", "Variable", "LetClause",
          "WhereClause", "GroupByClause", "GroupBySpec", "AggregateSpec", "DftAggregateSpec", "AggregateBinding",
          "SequenceAgg", "CountAgg", "SumAgg", "AvgAgg", "MinAgg", "MaxAgg", "SingleAgg", "VariableRef", "Collation",
          "OrderByClause", "OrderBySpec", "OrderByKind", "ASCENDING", "DESCENDING", "OrderByEmptyMode", "GREATEST",
          "LEAST", "CountClause", "ReturnClause", "OrExpr", "AndExpr", "GeneralCompEQ", "GeneralCompNE",
          "GeneralCompLT", "GeneralCompLE", "GeneralCompGT", "GeneralCompGE", "ValueCompEQ", "ValueCompNE",
          "ValueCompLT", "ValueCompLE", "ValueCompGT", "ValueCompGE", "NodeCompIs", "NodeCompPrecedes",
          "NodeCompFollows", "ComparisonExpr", "StringConcatExpr", "RangeExpr", "AddOp", "SubtractOp", "ArithmeticExpr",
          "MultiplyOp", "DivideOp", "IDivideOp", "ModulusOp", "UnionExpr", "IntersectExpr", "ExceptExpr",
          "InstanceofExpr", "SequenceType", "EmptySequenceType", "ItemType", "AtomicOrUnionType", "CardinalityOne",
          "CardinalityZeroOrOne", "CardinalityZeroOrMany", "CardinalityOneOrMany", "TreatExpr", "CastableExpr",
          "CastExpr", "FunctionCall", "PathExpr", "MapExpr", "StepExpr", "AxisSpec", "SELF", "CHILD", "DESCENDANT",
          "DESCENDANT_OR_SELF", "ATTRIBUTE", "FOLLOWING_SIBLING", "FOLLOWING", "PARENT", "ANCESTOR",
          "PRECEDING_SIBLING", "PRECEDING", "ANCESTOR_OR_SELF", "KindTestAnyKind", "KindTestDocument", "NameTest",
          "Wildcard", "Nilled", "KindTestElement", "KindTestAttribute", "KindTestSchemaElement",
          "KindTestSchemaAttribute", "KindTestPi", "KindTestComment", "KindTestText", "KindTestNamespaceNode",
          "NSWildcardNameTest", "NSNameWildcardTest", "ParenthesizedExpr", "ContextItemExpr", "ArgumentPlaceHolder",
          "OrderedExpr", "UnorderedExpr", "Int", "Str", "QNm", "Dbl", "Dec", "AnyURI", "Bool", "Annotation",
          "FunctionTest", "AnyFunctionType", "TypedFunctionType", "SomeQuantifier", "EveryQuantifier", "QuantifiedExpr",
          "QuantifiedBinding", "SwitchExpr", "SwitchClause", "TypeSwitch", "TypeSwitchCase", "IfExpr", "TryCatchExpr",
          "CatchClause", "CatchErrorList", "ExtensionExpr", "Pragma", "PragmaContent", "ValidateExpr", "ValidateLax",
          "ValidateStrict", "LiteralFuncItem", "InlineFuncItem", "TypedVariableDeclaration", "CompElementConstructor",
          "CompAttributeConstructor", "CompCommentConstructor", "ContentSequence", "EnclosedExpr",
          "DirElementConstructor", "DirAttributeConstructor", "DirCommentConstructor", "DirPIConstructor",
          "CompDocumentConstructor", "CompTextConstructor", "DefaultElementNamespace", "DefaultFunctionNamespace",
          "SchemaImport", "ModuleImport", "Namespace", "ContextItemDeclaration", "ExternalVariable", "AnnotatedDecl",
          "FunctionDecl", "ExternalFunction", "BoundarySpaceDeclaration", "BoundarySpaceModePreserve",
          "BoundarySpaceModeStrip", "CollationDeclaration", "BaseURIDeclaration", "ConstructionDeclaration",
          "ConstructionModePreserve", "ConstructionModeStrip", "OrderingModeDeclaration", "OrderingModeOrdered",
          "OrderingModeUnordered", "EmptyOrderDeclaration", "EmptyOrderModeGreatest", "EmptyOrderModeLeast",
          "CopyNamespacesDeclaration", "CopyNamespacesPreserveModePreserve", "CopyNamespacesPreserveModeNoPreserve",
          "CopyNamespacesInheritModeInherit", "CopyNamespacesInheritModeNoInherit", "DecimalFormatDeclaration",
          "DecimalFormatDefault", "DecimalFormatProperty", "DecimalFormatPropertyDecimalSeparator",
          "DecimalFormatPropertyGroupingSeparator", "DecimalFormatPropertyInfinity", "DecimalFormatPropertyMinusSign",
          "DecimalFormatPropertyNaN", "DecimalFormatPropertyPercent", "DecimalFormatPropertyPerMille",
          "DecimalFormatPropertyZeroDigit", "DecimalFormatPropertyDigit", "DecimalFormatPropertyPatternSeparator",
          "OptionDeclaration", "CompNamespaceConstructor", "CompPIConstructor", "Predicate", "FilterExpr",
          "DynamicFunctionCallExpr", "SlidingWindowClause", "TumblingWindowClause", "WindowStartCondition",
          "WindowEndCondition", "WindowVars", "PreviousItemBinding", "NextItemBinding",
          // Begin XQuery Update Facility 1.0
          "RevalidationDeclaration", "RevalidationModeStrict", "RevalidationModeLax", "RevalidationModeSkip",
          "InsertExpr", "InsertFirst", "InsertLast", "InsertAfter", "InsertBefore", "InsertInto", "DeleteExpr",
          "ReplaceValueExpr", "ReplaceNodeExpr", "RenameExpr", "TransformExpr", "CopyVariableBinding",
          // End XQuery Update Facility 1.0
          // Begin brackit's set-oriented extensions
          "PipeExpr", "Selection", "GroupBy", "OrderBy", "Join", "JoinClause", "Start", "ForBind", "LetBind", "Count",
          "End",
          // End brackit's set-oriented extensions
          // Begin brackit's custom array syntax
          "KindTestArray", "ArrayAccess", "ArrayConstructor", "SequenceField", "FlattenedField", "ArrayIndexSlice",
          // End brackit's custom array syntax
          // Begin brackit's custom recird syntax
          "KindTestRecord", "RecordProjection", "RecordConstructor", "RecordField", "KeyValueField", "DerefExpr",
          // End brackit's custom record syntax
          // Begin temporal
          "NEXT", "PREVIOUS", "EARLIER", "EARLIER_OR_SELF", "FUTURE", "FUTURE_OR_SELF", "FIRST", "LAST", "ALL_TIME",
          // End temporal
          "JsonItemTest", "StructuredItemTest", "KindTestNull", "Null",
          // Begin JSONiq update expressions
          "InsertJsonExpr", "DeleteJsonExpr", "RenameJsonExpr", "ReplaceJsonExpr", "AppendJsonExpr",
          // End JSONiq update expressions
      };

  private XQ() {
  }

  private static int base = 272;

  public static synchronized int allocate(int noOfTokens) {
    int r = base;
    base += noOfTokens;
    return r;
  }

  public static void main(String[] args) throws Exception {
    XQ xq = new XQ();
    for (Field f : XQ.class.getFields()) {
      if ((f.getType().isPrimitive()) && (f.getType().equals(Integer.TYPE))) {
        int pos = f.getInt(xq);
        String val = NAMES[pos];
        if (!val.equals(f.getName())) {
          System.out.println(String.format("Mismatch at %s: %s != %s", pos, f.getName(), val));
        }
      }
    }
  }
}
