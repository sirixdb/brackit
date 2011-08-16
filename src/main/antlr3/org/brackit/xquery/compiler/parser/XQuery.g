/*=============================================================================

    Copyright 2009 Nikolay Ognyanov
    Copyright 2010-2011 Caetano Sauer, Sebastian Baechle 

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

=============================================================================*/
/*
This file is a derivate of the original XQuery 1.0 grammar provided by Nikolay Ognyanov.
It has been fixed and modified in serveral ways for proper parsing and AST construction.
Further, it has been extended to support the XQuery 3.0 grammar.
*/
grammar XQuery;

options {
	output = AST;
	superClass = ParserBase;
}


// Tokens shared with the direct XML lexer
tokens {
// Lexical tokens
    LAngle;
    RAngle;
    LCurly;
    RCurly;
    LClose;
    RClose;
    SymEq;
    Colon;
    Quot;
    Apos;
    EscapeQuot;
    EscapeApos;
    EscapeLCurly;
    EscapeRClurly;
    ElementContentChar;
    PredefinedEntityRef;
    QuotAttrContentChar;
    AposAttrContentChar;
// Modules 
    XQuery;
    MainModule;
    LibraryModule;
    QueryBody;
    Prolog;
    NamespaceDeclaration;
    OptionDeclaration;
    VersionDeclaration;
    BaseURIDeclaration;
    BoundarySpaceDeclaration;
    OrderingModeDeclaration;
    EmptyOrderDeclaration;
    CopyNamespacesDeclaration;
    CollationDeclaration;
    ConstructionDeclaration;
    ModuleImport;
    Pragma;
// AST FLOWR
    FlowrExpr;
    ForClause;
    ForLetBindingExpr;
    ForLetExpr;
    LetClause;
    CountClause;
    GroupByClause;
    GroupBySpec;
    AllowingEmpty;
    OrderByClause;
    OrderBySpec;
    OrderByExprBinding;
    OrderByKind;
    OrderByEmptyMode;
    Collation;
    OrderModifier;
    WhereClause;
    ReturnClause;
    TypedVariableBinding;
    PosVariableBinding;
// Path expressions
    PathExpr;
    StepExpr;
    AxisSpec;
    NameTest;
    Wildcard;
    WildcardBeforeColon;
    WildcardAfterColon;
    KindTestDocument;
    KindTestElement;
    KindTestAttribute;
    KindTestSchemaElement;
    KindTestSchemaAttribute;
    KindTestPi;
    KindTestComment;
    KindTestText;
    KindTestAnyKind;
// TyCatch
	TryCatchExpr;
	TryClause;
	CatchClause;
	CatchErrorList;
	CatchVar;
// Switch
	SwitchExpr;
	SwitchCase;
// Operators
    GeneralCompEQ;
    GeneralCompNE;
    GeneralCompLT;
    GeneralCompLE;
    GeneralCompGT;
    GeneralCompGE;
    ValueCompEQ;
    ValueCompNE;
    ValueCompLE;
    ValueCompLT;
    ValueCompGE;
    ValueCompGT;
    NodeCompIs;
    NodeCompPrecedes;
    NodeCompFollows;
    AddOp;
    SubtractOp;
    MultiplyOp;
    DivideOp;
    IDivideOp;
    ModulusOp;
    UnionExpr;
    IntersectExpr;
    ExceptExpr;
// Standard expressions 
    OrExpr;
    AndExpr;
    RangeExpr;
    InstanceofExpr;
    TreatExpr;
    CastableExpr;
    CastExpr;
    NegateExpr;
    Predicate;
    FilterExpr;
    SequenceExpr;
    EmptySequence;
    ParenthesizedExpr;
    ContextItemExpr;
    OrderedExpr;
    UnorderedExpr;
    ComparisonExpr;
    ArithmeticExpr;
    IfExpr;
    SwitchExpr;
    ValidateExpr;
    ValidateStrict;
    ValidateLax;
// XML
    CompElementConstructor;
    CompDocumentConstructor;
    CompAttributeConstructor;
    CompTextConstructor;
    CompCommentConstructor;
    CompPIConstructor;
    ContentSequence;
// Functions and Variable access
    FunctionCall;
    FunctionDeclaration;
    Parameter;
    Variable;
    ExternalVariable;
    TypedVariableDeclaration;
    VariableRef;    
// Schema support
    SchemaImport;
    Namespace;
    DefaultElementNamespace;
    DefaultFunctionNamespace;
// Sequence types
    SequenceType;
    EmptySequenceType;
    ItemType;
    AtomicType;
    CardinalityOne;
    CardinalityOneOrMany;
    CardinalityZeroOrOne;
    CardinalityZeroOrMany;
    TypeSwitch;
    TypeSwitchCase;
    TypeSwitchDefault;
// Quantified expressions
    QuantifiedExpr;
    SomeQuantifier;
    EveryQuantifier;    
// Update expressions
    Insert;
    InsertFirst;
    InsertLast;
    InsertInto;
    InsertBefore;
    InsertAfter;
    Delete;
    Replace;
    ReplaceNode;
    ReplaceValue;
    Rename;
    Transform;
    CopyBinding;
    Modify;
    TransformReturn;
// Literals
    Literal;
    Int;
    Dec;
    Dbl;
    Str;
    Qname;
// JoinExpr
	JoinExpr;
	LeftJoinExpr;
	JoinClause;
// Set-oriented Parts
   ReturnExpr;
// Operators
	Selection;
	GroupBy;
	OrderBy;
    Join;
    Start;
    ForBind;
    LetBind;
    Count;
}

@header {
/*=============================================================================

    Copyright 2009 Nikolay Ognyanov
    Copyright 2010-2011 Caetano Sauer, Sebastian Baechle 

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

=============================================================================*/
package org.brackit.xquery.compiler.parser;
}

@lexer::header {
/*=============================================================================

    Copyright 2009 Nikolay Ognyanov
    Copyright 2010-2011 Caetano Sauer, Sebastian Baechle 

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

=============================================================================*/
package org.brackit.xquery.compiler.parser;
}

@members {
    // XQuery version constants
    public static final int XQUERY_1_0 = 0;
    public static final int XQUERY_3_0 = 1;

    // Pass some token codes to super class at creation time.
    boolean dummy = setTokenCodes(NCName, Colon);

    // Flags enabling XQuery extensions
    private boolean update = true;
    private boolean scripting = true;
    private boolean fullText = true;

    private VarScopes variables = new VarScopes();

    // XQuery version - must be one of XQUERY_1_0 or XQUERY_3_0 
    private int xqVersion = XQUERY_3_0;

    public boolean getUpdate   () {return update;}
    public boolean getScripting() {return scripting;}
    public boolean getFullText () {return fullText ;}
    public int getXQVersion() {return xqVersion;}

    public void setUpdate(boolean value) {update = value;}
    public void setScripting(boolean value) {scripting = value;}
    public void setFullText (boolean value) {fullText  = value;}

    public void setXQVersion(int value) {
        if (value != XQUERY_1_0 && value != XQUERY_3_0)
            throw new IllegalArgumentException("Unknown XQuery version.");
        xqVersion = value;
    }
    
    private void setXQVersion(String version) {
    	if ("3.0".equals(version)) {
		xqVersion = 1;
	} else if ("1.0".equals(version)) {
		xqVersion = 0;
	} else {
	            throw new IllegalArgumentException("Unknown XQuery version: " + version);
	}
    }
    
    protected Object recoverFromMismatchedToken(IntStream input,
                                            int ttype,
                                            BitSet follow)
    throws RecognitionException
{   
    throw new MismatchedTokenException(ttype, input);
}
}


@rulecatch {
    catch (RecognitionException re) {
        throw re;
    }
}


module
    : versionDecl? (libraryModule -> ^(XQuery libraryModule)| mainModule -> ^(XQuery mainModule)) EOF
    ;    
versionDecl
    :
	XQUERY 
	((ENCODING stringLiteral {checkEncoding();}) | 
	(VERSION version=stringLiteral { setXQVersion($version.text.substring(1, $version.text.length() - 1));} (ENCODING stringLiteral)?)) ';'
    ;
mainModule
    : prolog queryBody -> ^(MainModule prolog* queryBody)
    ;
libraryModule
    : moduleDecl prolog -> ^(LibraryModule moduleDecl prolog)
    ;
moduleDecl
    : MODULE NAMESPACE name=ncName SymEq uri=uriLiteral ';' -> ^(NamespaceDeclaration ^(Literal $name) ^(Literal $uri))
    ;
prolog
    : prologPartOne* 
      prologPartTwo*
      -> ^(Prolog prologPartOne* prologPartTwo*)
   ;
   
prologPartOne
    :	
    ((defaultNamespaceDecl | setter | namespaceDecl | importDecl | ftOptionDecl                                       // ext:fulltext
    ) ';' -> defaultNamespaceDecl* setter* namespaceDecl* importDecl* ftOptionDecl*)
    ;
prologPartTwo
    :	   
      ((annotatedDecl | optionDecl | contextItemDecl                                       // XQuery 3.0
      ) ';' -> annotatedDecl* optionDecl* contextItemDecl*)
    ;
annotatedDecl
    :	
   DECLARE annotation* (varDecl | functionDecl)
   -> annotation* varDecl? functionDecl?
   ;   
annotation
   	:
   	'%' eqName ('(' literal (',' literal)* ')')?	
   	;
        
setter
    : boundarySpaceDecl 
    | defaultCollationDecl 
    | baseURIDecl
    | constructionDecl  
    | orderingModeDecl     
    | emptyOrderDecl 
    | copyNamespacesDecl
    | {update}?                => revalidationDecl                // ext:update
    | {xqVersion==XQUERY_3_0}? => decimalFormatDecl               // XQuery 3.0
    ;
importDecl
    : schemaImport
    | moduleImport
    ;
namespaceDecl
    : DECLARE NAMESPACE name=ncName SymEq uri=uriLiteral -> ^(NamespaceDeclaration ^(Literal $name) ^(Literal $uri))
    ;
boundarySpaceDecl
    : 
    DECLARE BOUNDARY_SPACE
    (
    PRESERVE -> ^(BoundarySpaceDeclaration ^(Literal Str["preserve"]))
    | STRIP -> ^(BoundarySpaceDeclaration ^(Literal Str["strip"]))
    )
    ;
defaultNamespaceDecl
@init {
boolean elementNamespaceDecl = true;
}
    : DECLARE DEFAULT (ELEMENT | FUNCTION { elementNamespaceDecl = false; }) NAMESPACE uri=uriLiteral 
    -> {elementNamespaceDecl}? ^(DefaultElementNamespace ^(Literal $uri))
    -> ^(DefaultFunctionNamespace ^(Literal $uri))
    ;    
optionDecl
    : DECLARE OPTION qName stringLiteral -> ^(OptionDeclaration qName ^(Literal stringLiteral))
    ;
ftOptionDecl                                                    // ext:fulltext
    : {fullText}? => DECLARE FT_OPTION (USING ftMatchOption)+
    ;
orderingModeDecl
    : DECLARE ORDERING
    (
      ORDERED -> ^(OrderingModeDeclaration ^(Literal Str["ordered"]))
    | UNORDERED -> ^(OrderingModeDeclaration ^(Literal Str["unordered"]))
    )
    ;
emptyOrderDecl
    : DECLARE DEFAULT ORDER EMPTY
    (
    GREATEST -> ^(EmptyOrderDeclaration ^(Literal Str["greatest"]))
    | LEAST -> ^(EmptyOrderDeclaration ^(Literal Str["least"]))
    )
    ;
copyNamespacesDecl
    : DECLARE COPY_NAMESPACES preserveMode ',' inheritMode
    -> ^(CopyNamespacesDeclaration preserveMode inheritMode)
    ;
decimalFormatDecl                                                 // XQuery 3.0
    : DECLARE ((DECIMAL_FORMAT qName) | (DEFAULT DECIMAL_FORMAT))
      (dfPropertyName SymEq stringLiteral)*
    ;
dfPropertyName                                                    // XQuery 3.0
    : DECIMAL_SEPARATOR
    | GROUPING_SEPARATOR
    | INFINITY
    | MINUS_SIGN
    | NAN
    | PERCENT
    | PER_MILLE
    | ZERO_DIGIT
    | DIGIT
    | PATTERN_SEPARATOR
    ;
preserveMode
    : PRESERVE -> ^(Literal Str["preserve"]) | NO_PRESERVE -> ^(Literal Str["nopreserve"])
    ;
inheritMode
    : INHERIT -> ^(Literal Str["inherit"]) | NO_INHERIT -> ^(Literal Str["noinherit"])
    ;
defaultCollationDecl
    : DECLARE DEFAULT COLLATION uriLiteral -> ^(CollationDeclaration ^(Literal uriLiteral))
    ;
baseURIDecl
    : DECLARE BASE_URI uriLiteral -> ^(BaseURIDeclaration ^(Literal uriLiteral))
    ;
schemaImport
    : IMPORT SCHEMA schemaPrefix? uri=uriLiteral 
       (AT at+=uriLiteral (',' at+=uriLiteral)*)? -> ^(SchemaImport schemaPrefix? $uri $at*)
    ;
schemaPrefix
    : (NAMESPACE ncName SymEq) -> ^(Namespace ncName)
    | DEFAULT ELEMENT NAMESPACE -> DefaultElementNamespace
    ;
moduleImport
@init {
boolean hasPrefix = false;
}
    : IMPORT MODULE (NAMESPACE ncName SymEq { hasPrefix = true; })? uri=uriLiteral
      (AT at+=uriLiteral (',' at+=uriLiteral)*)?
      -> {!hasPrefix}? ^(ModuleImport $uri $at*)
      -> ^(ModuleImport ^(Namespace ncName) $uri $at*)
    ;
varDecl
    :
	varOrConst '$' name=qName typeDeclaration? declVarValue
	-> ^(TypedVariableDeclaration Variable[variables.declare($name.text)] typeDeclaration? declVarValue)
       ;

declVarValue
    : ':=' exprSingle -> exprSingle 
    | EXTERNAL externalDefaultValue -> ^(ExternalVariable externalDefaultValue?)
    ;

varOrConst
    : VARIABLE
    | {scripting}? => CONSTANT                                 // ext:scripting
    ;
externalDefaultValue
    : {xqVersion==XQUERY_3_0}? => ':=' varDefaultValue -> varDefaultValue          // XQuery 3.0
    |
    ;
varValue                                                          // XQuery 3.0
    : exprSingle
    ;
varDefaultValue                                                   // XQuery 3.0
    : exprSingle
    ;
constructionDecl
    : DECLARE CONSTRUCTION 
    (
    STRIP -> ^(ConstructionDeclaration ^(Literal Str["strip"]))
    | PRESERVE -> ^(ConstructionDeclaration ^(Literal Str["preserve"]))
    )
    ;
functionDecl
    :
	{ variables.openScope(); } 
	xq3FunModifier? (updateFunModifier | scriptingFunModifier)?
	FUNCTION fqName '(' paramList? ')'
	typeDeclaration? (enclosedExpr | EXTERNAL)
       { variables.closeScope(); } 
	-> ^(FunctionDeclaration fqName typeDeclaration? xq3FunModifier? updateFunModifier? scriptingFunModifier? ^(Parameter paramList?) enclosedExpr?)
    |    {scripting}? =>                                       // ext:sctipting 
	xq3FunModifier?
	SEQUENTIAL
	FUNCTION fqName '(' paramList? ')'
	typeDeclaration? (block        | EXTERNAL)
    ;
updateFunModifier
    : {update}? => UPDATING
    ;
scriptingFunModifier
    : {scripting}? => SIMPLE
    ;
xq3FunModifier
    : {xqVersion==XQUERY_3_0}? => DETERMINISTIC
    | {xqVersion==XQUERY_3_0}? => NONDETERMINISTIC
    ;
paramList
    : param (',' param)* -> param*
    ;
param
    : '$' name=qName typeDeclaration? -> ^(TypedVariableDeclaration Variable[variables.declare($name.text)] typeDeclaration?)
    ;
enclosedExpr
    : LCurly expr RCurly -> expr
    ;
    
    
/**
    QUERY BODY
*/
    
    
queryBody
    :	e=expr
    ->	^(QueryBody[$e.start, "QueryBody"] expr)
    ;
    
expr
      // : exprSingle (',' exprSingle)* ;                          //XQuery 1.0
      // Some hand crafted  parsing since                      // ext:scripting
      // original W3C grammar is not LL(*)
      @init
      {
        boolean isSeq  = false;
        boolean seqEnd = false;
      }
      @after {
        if(isSeq && !seqEnd) {
            raiseError("Sequential expression not terminated by ';'.");
        }
      }
    :	first=exprSingle
		(
      		(','
	      	|';'
				{
					if(!scripting) raiseError("Unexpectd token ';'.");
					isSeq = true;
				}
       		)
			further+=exprSingle
		)*
		(';'
			{
				if(!scripting) raiseError("Unexpectd token ';'.");
				isSeq = true; seqEnd = true;
			}
		)?
		->	{ further != null }? ^(SequenceExpr $first $further+)
		->	$first
    ;
/*
// W3C grammar:
expr 
    : applyExpr                                                // ext:scripting
    | concatExpr
    ;
applyExpr                                                      // ext:scripting
    : (concatExpr ';')+
    ;
concatExpr                                                     // ext:scripting
    : exprSingle (',' exprSingle)*
    ;
*/
exprSingle
    : flworExpr
    | quantifiedExpr
    | typeswitchExpr
    | ifExpr
    | orExpr
    | {update}?                => insertExpr                      // ext:update 
    | {update}?                => deleteExpr                      // ext:update
    | {update}?                => renameExpr                      // ext:update
    | {update}?                => replaceExpr                     // ext:update
    | {update}?                => transformExpr                   // ext:update
    | {scripting}?             => blockExpr                    // ext:scripting
    | {scripting}?             => assignmentExpr               // ext:scripting
    | {scripting}?             => exitExpr                     // ext:scripting
    | {scripting}?             => whileExpr                    // ext:scripting
    | {xqVersion==XQUERY_3_0}? => tryCatchExpr                    // XQuery 3.0
    ;

/*
	FLOWR expressions
*/

flworExpr
     	:
     	{xqVersion==XQUERY_3_0}? =>
	     	{ variables.openScope(); }
	     	initialClause intermediateClause* returnClause
		{ variables.closeScope(); }
		-> ^(FlowrExpr initialClause intermediateClause* returnClause)
	|		
     		{ variables.openScope(); }
     		initialClause+ whereClause? orderByClause? returnClause
    		{ variables.closeScope(); }
    		-> ^(FlowrExpr initialClause+ whereClause? orderByClause? returnClause)
    ;
    
initialClause                                                      // XQuery 3.0
    : forClause
    | letClause
    | {xqVersion==XQUERY_3_0}? => windowClause
    ;
intermediateClause                                                // XQuery 3.0
    : initialClause
    | whereClause
    | groupByClause
    | orderByClause
    | countClause
    ;
    
forClause
    : 
    FOR forClauseItem (',' forClauseItem)*
    -> ^(ForClause forClauseItem)+ // TODO this is part of normalization, also for letClauseItem
    ;
    
forClauseItem
	:	'$' inBinding
		IN exprSingle
    ->	inBinding exprSingle
	;
	
inBinding
	:	typedVarBinding allowingEmpty? positionalVar? ftScoreVar?
	;
	
typedVarBinding
	:	name=varName typeDeclaration?
	->	^(TypedVariableBinding Variable[variables.declare($name.text)] typeDeclaration?)
	;
	
allowingEmpty                                                     // XQuery 3.0
    :	{xqVersion==XQUERY_3_0}? => ALLOWING EMPTY
    ->	^(AllowingEmpty)
    ;
    
positionalVar
    :	AT '$' name=varName
    ->	^(TypedVariableBinding Variable[variables.declare($name.text)] varName)
    ;
    
ftScoreVar                                                      // ext:fulltext
    : {fullText}? => SCORE '$' varName
    ;
    
letClause
    :	
    	LET letClauseItem (',' letClauseItem)*
    	->	^(LetClause letClauseItem)+
    
    // only for full text:
    | {fullText}? =>                                            // ext:fulltext
      LET SCORE '$' varName ':=' exprSingle
      (','(('$' varName typeDeclaration?) | ftScoreVar) ':=' exprSingle)*
    ;
       
letClauseItem
	:	'$' typedVarBinding ':=' exprSingle
	->	typedVarBinding exprSingle
	;
	
letClauseItemFT
	:	(
			('$' typedVarBinding)	-> typedVarBinding
			| ftScoreVar					-> ftScoreVar
		)
		':=' exprSingle
	->	^(ForLetBindingExpr $letClauseItemFT ^(ForLetExpr exprSingle))
	;
    
windowClause                                                      // XQuery 3.0
    : FOR (tumblingWindowClause | slidingWindowClause)
    ;
    
tumblingWindowClause                                              // XQuery 3.0
    : TUMBLING WINDOW '$' varName typeDeclaration? IN exprSingle 
      windowStartCondition windowEndCondition?
    ;
    
slidingWindowClause                                               // XQuery 3.0
    : SLIDING WINDOW  '$' varName typeDeclaration? IN exprSingle 
      windowStartCondition windowEndCondition
    ;
windowStartCondition                                              // XQuery 3.0
    : START windowVars WHEN exprSingle
    ;
windowEndCondition                                                // XQuery 3.0
    : ONLY? END windowVars WHEN exprSingle
    ;
windowVars                                                        // XQuery 3.0
    : ('$' currentItem)? positionalVar? 
      (PREVIOUS '$' previousItem)? (NEXT '$' nextItem)?
    ;
currentItem                                                       // XQuery 3.0
    : qName
    ;
previousItem                                                      // XQuery 3.0
    : qName
    ;
nextItem                                                          // XQuery 3.0
    : qName
    ;
countClause                                                       // XQuery 3.0
    : COUNT '$' name=varName -> ^(CountClause Variable[variables.declare($name.text)])
    ;
whereClause
    :	WHERE exprSingle
    -> ^(WhereClause exprSingle)
    ;
groupByClause                                                     // XQuery 3.0
    : GROUP BY groupingSpecList -> ^(GroupByClause groupingSpecList)
    ;
groupingSpecList                                                  // XQuery 3.0
    : groupingSpec (',' groupingSpec)* -> ^(GroupBySpec groupingSpec)+
    ;
groupingSpec                                                      // XQuery 3.0
    : '$' name=varName (COLLATION uriLiteral)? -> VariableRef[variables.resolve($name.text)] ^(Collation uriLiteral)?
    ;
    
// order by stable necessary?
orderByClause
    :	(
    		(ORDER BY ->  OrderByClause) |
    		(STABLE ORDER BY -> OrderByClause)
    	)
    	orderSpecList -> ^($orderByClause orderSpecList)
    ;
    
orderSpecList
    :	orderSpec (',' orderSpec)*
    ->	^(OrderBySpec orderSpec)+
    ;
    
// TODO: Add orderModifier to AST
orderSpec
    :	exprSingle orderModifier
    ->	exprSingle orderModifier?
    ;
    
orderModifier
    : 	(ASCENDING | DESCENDING)? 
    	(EMPTY (GREATEST | LEAST))? 
    	(COLLATION uriLiteral)?
    ->	^(OrderByKind ASCENDING? DESCENDING?)?
    	^(OrderByEmptyMode GREATEST? LEAST?)?
    	^(Collation uriLiteral?)?
    ;
    
returnClause                                                      // XQuery 3.0
    :	RETURN exprSingle
    ->	^(ReturnClause exprSingle)
    ;
    
    
/*
	Other expressions
*/       
    
quantifiedExpr
    : SOME
      (quantifiedVarBinding (',' quantifiedVarBinding)* -> quantifiedVarBinding+)
      SATISFIES exprSingle
      -> ^(QuantifiedExpr SomeQuantifier quantifiedVarBinding exprSingle)
    | EVERY
      (quantifiedVarBinding (',' quantifiedVarBinding)* -> quantifiedVarBinding+)
      SATISFIES exprSingle
      -> ^(QuantifiedExpr EveryQuantifier quantifiedVarBinding+ exprSingle)
    ;
    
quantifiedVarBinding
    :
    '$' typedVarBinding IN exprSingle
    -> typedVarBinding exprSingle
    ;
    
typeswitchExpr
    : TYPESWITCH '(' expr ')' 
      caseClause+ 
      defaultCase
      -> ^(TypeSwitch expr caseClause+ defaultCase)
    ;
    
caseClause
@init {
  variables.openScope();
}
@after {
  variables.closeScope();
}
    : CASE caseVarBinding? sequenceType RETURN exprSingle 
    -> ^(TypeSwitchCase caseVarBinding? ^(SequenceType sequenceType) exprSingle)
    ;

caseVarBinding
  :
  '$' name=varName AS
  -> ^(Variable[variables.declare($name.text)])
  ;

defaultCase
@init {
  variables.openScope();
}
@after {
  variables.closeScope();
}
    : DEFAULT defaultVarBinding? RETURN exprSingle
-> ^(TypeSwitchDefault defaultVarBinding? exprSingle)
    ;
    
defaultVarBinding
  :
  '$' name=varName
-> ^(Variable[variables.declare($name.text)])
  ;
   
ifExpr
    : IF '(' expr ')' THEN a=exprSingle ELSE b=exprSingle
    -> ^(IfExpr expr $a $b)
    ;
    
    
/*
	OR expressions
*/   
    
orExpr
    :	(andExpr -> andExpr)
    	(OR operand=andExpr -> ^(OrExpr $orExpr $operand))*
    ;
    
andExpr
    :	(comparisonExpr -> comparisonExpr)
    	(AND operand=comparisonExpr -> ^(AndExpr $andExpr $operand))*
    ;
    
comparisonExpr
    : //XQuery 1.0 :
      //rangeExpr ( (valueComp | generalComp | nodeComp) rangeExpr )?
                                                                // ext:fulltext
     	(ftContainsExpr -> ftContainsExpr)
     	(
     		comparisonOperator operand=ftContainsExpr
    		-> ^(ComparisonExpr comparisonOperator $comparisonExpr $operand)
 		)?
    ;
    
comparisonOperator
	: (valueComp | generalComp | nodeComp)
	;    
    
// TODO, no fulltext, so AST generation goes staright to rangeExpr
ftContainsExpr                                                  // ext:fulltext
    : rangeExpr ftContainsClause?
    ;
    
ftContainsClause
    :  {fullText}? => CONTAINS TEXT ftSelection ftIgnoreOption?
    ; 
    
rangeExpr
    :	(additiveExpr -> additiveExpr)
    	(TO operand=additiveExpr -> ^(RangeExpr $rangeExpr $operand))?
    ;
    
additiveExpr
    :	(multiplicativeExpr -> multiplicativeExpr)
    	(
    		operator=additiveExprOperator
    		operand=multiplicativeExpr
    		-> ^(ArithmeticExpr $operator $additiveExpr $operand)
    	)*
    ;
    
additiveExprOperator
	:	'+' -> AddOp | '-' -> SubtractOp
	;
    
multiplicativeExpr
    :	(unionExpr -> unionExpr)
    	(
    		operator=multiplicativeExprOperator
    		operand=unionExpr 
    		-> ^(ArithmeticExpr $operator $multiplicativeExpr $operand)	
    	)*
    ;
    
multiplicativeExprOperator
	:	 '*' -> MultiplyOp
        | DIV  {needSpaceBetween(IntegerLiteral);}
               {needSpaceBetween(DecimalLiteral);}
               {needSpaceBetween(DoubleLiteral );}
		-> DivideOp
        | IDIV {needSpaceBetween(IntegerLiteral);}
               {needSpaceBetween(DecimalLiteral);}
               {needSpaceBetween(DoubleLiteral );}
        -> IDivideOp
        | MOD  {needSpaceBetween(IntegerLiteral);}
               {needSpaceBetween(DecimalLiteral);}
               {needSpaceBetween(DoubleLiteral );}
        -> ModulusOp
;
    
unionExpr
    :	(intersectExceptExpr -> intersectExceptExpr)
    	(
    		(UNION | '|') operand=intersectExceptExpr
    		-> ^(UnionExpr $unionExpr $operand)
    	)*
    ;
    
intersectExceptExpr
    :	(instanceofExpr -> instanceofExpr)
    	(
    		operator=intersectExceptExprOperator
    		operand=instanceofExpr
    		-> ^($operator $intersectExceptExpr $operand)
    	)*
    ;
    
intersectExceptExprOperator
	:	INTERSECT	-> IntersectExpr
		| EXCEPT	-> ExceptExpr
	;
    
instanceofExpr
    :	(treatExpr -> treatExpr)
    	(INSTANCE OF operand=sequenceType -> ^(InstanceofExpr $instanceofExpr ^(SequenceType $operand)))?
    ;
    
treatExpr
    :	(castableExpr -> castableExpr)
    	(TREAT AS operand=sequenceType -> ^(TreatExpr $treatExpr ^(SequenceType $operand)))?
    ;
    
castableExpr
    :	(castExpr -> castExpr)
    	(CASTABLE AS operand=singleType -> ^(CastableExpr $castableExpr $operand))?
    ;
    
castExpr
    :	(unaryExpr -> unaryExpr)
    	(CAST AS operand=singleType -> ^(CastExpr $castExpr $operand))?
    ;
    
// TODO: should we be compatible with XPath 1.0?
unaryExpr
@init {
int minusCount = 0;
}
    :	(('+'|'-')+ valueExpr) => 
        	(('+'|'-' {minusCount++;})+ valueExpr) -> { (minusCount & 1) == 1}? ^(ArithmeticExpr MultiplyOp ^(Literal ^(Int["-1"])) valueExpr)
                                                                            -> valueExpr
	| valueExpr -> valueExpr		
    ;
    
// TODO: validate and extension expr
valueExpr
    : validateExpr
    | pathExpr
    | extensionExpr
    ;
    
generalComp
    :	SymEq		-> GeneralCompEQ
    	| '!='		-> GeneralCompNE
    	| LAngle	-> GeneralCompLT
    	| '<='		-> GeneralCompLE
    	| RAngle	-> GeneralCompGT
    	| '>='		-> GeneralCompGE
    ;
    
valueComp
    :
    	EQ		-> ValueCompEQ
    	| NE	-> ValueCompNE
    	| LT	-> ValueCompLT
    	| LE	-> ValueCompLE
    	| GT	-> ValueCompGT
    	| GE	-> ValueCompGE
    ;
    
nodeComp
    :	IS		-> NodeCompIs
    	| '<<'	-> NodeCompPrecedes
    	| '>>'	-> NodeCompFollows
    ;
    
validateExpr
    : VALIDATE validationMode? LCurly expr RCurly -> ^(ValidateExpr validationMode? expr)
    ;
validationMode
 	: LAX -> ValidateLax| STRICT -> ValidateStrict
 	;
extensionExpr
    : ({ parsePragma(); } Pragma)+ LCurly expr? RCurly -> ^(Pragma expr?)
    ;
//W3C grammar :
//pragma                                                         // ws:explicit
//  : '(#' S? qName (S PragmaContents)? '#)'
//  ;


pathExpr                                              // xgs:leading-lone-slash
scope {
	// 1 for abbreviated child axis, 2 for descendant and 0 for plain relativePathExpr
	int stepAlternative;
}

    : ('/' relativePathExpr) => '/'! {  $pathExpr::stepAlternative = 1; } relativePathExpr
    | ('//' relativePathExpr) => '//'! {  $pathExpr::stepAlternative = 2; } relativePathExpr
    | ('/'        '*'       ) => '/'! '*'
    | '/' -> ^(TreatExpr ^(FunctionCall["fn:root"] ^(StepExpr ^(AxisSpec SELF["self"]) KindTestAnyKind)) ^(SequenceType KindTestDocument))
    | {  $pathExpr::stepAlternative = 0; } relativePathExpr
    ;
    
relativePathExpr
    :	firstStep=stepExpr
    	(
    		(
    			'/' { $pathExpr::stepAlternative = 1; }
    			| '//' {  $pathExpr::stepAlternative = 2; }
    		)
	    	furtherSteps+=stepExpr
	    )*
	   	->	{ furtherSteps != null }? ^(PathExpr $firstStep $furtherSteps+)
	   	->	^($firstStep)
    ;
    
// AST generation is postponed to filterExpr or axisExpr
stepExpr
    : axisStep | filterExpr
    ;
   
axisStep
    : 	(forwardStep | reverseStep) predicateList
	-> { $pathExpr::stepAlternative == 2 }?
		^(StepExpr ^(AxisSpec DESCENDANT_OR_SELF) KindTestAnyKind) ^(StepExpr forwardStep? reverseStep? predicateList?)
	->  	^(StepExpr forwardStep? reverseStep? predicateList?)
    ;
    
forwardStep
    :	(forwardAxis nodeTest)	-> ^(AxisSpec forwardAxis) nodeTest
    	| abbrevForwardStep		-> abbrevForwardStep
    ;
    
forwardAxis
    : CHILD '::'!
    | DESCENDANT '::'!
    | ATTRIBUTE '::'!
    | SELF '::'!
    | DESCENDANT_OR_SELF '::'!
    | FOLLOWING_SIBLING '::'!
    | FOLLOWING '::'!
    ;
    
abbrevForwardStep
// TODO When stepAlternative is 0 (unspecified axis) and nodetest is attribute or schema attribute test the axis is attribute
    :	('@') => '@' nodeTest	-> ^(AxisSpec ATTRIBUTE) nodeTest
    	| ((ATTRIBUTE | SCHEMA_ATTRIBUTE) '(') => kindTest -> ^(AxisSpec ATTRIBUTE) kindTest
    	| nodeTest -> ^(AxisSpec CHILD) nodeTest
    ;
    
reverseStep
    :	(reverseAxis nodeTest)	-> ^(AxisSpec reverseAxis) nodeTest
    	| abbrevReverseStep		-> abbrevReverseStep
    ;
    
reverseAxis
    : PARENT '::'!
    | ANCESTOR '::'!
    | PRECEDING_SIBLING '::'!
    | PRECEDING '::'!
    | ANCESTOR_OR_SELF '::'!
    ;
    
abbrevReverseStep
    : '..' -> ^(AxisSpec PARENT) ^(NameTest Wildcard)
    ;
    
nodeTest
    : kindTest	-> kindTest
    | nameTest	-> ^(NameTest nameTest)
    ;
    
nameTest
    : qName
    | wildcard
    ;
    
wildcard                                                         // ws:explicit
    : '*' -> Wildcard
    | ncName Colon {noSpaceBefore();} '*'    {noSpaceBefore();} -> ^(WildcardAfterColon ncName)
    | '*'    Colon {noSpaceBefore();} ncName {noSpaceBefore();} -> ^(WildcardBeforeColon ncName)
    ;
    
filterExpr
    :	primExpr=primaryExpr pred=predicate* // replaced original predicate list for predicate*
	-> 	{ $pathExpr::stepAlternative == 2 && pred != null }?
    		^(StepExpr ^(AxisSpec DESCENDANT_OR_SELF) KindTestAnyKind) ^(FilterExpr primaryExpr $pred*)
	->	{ $pathExpr::stepAlternative == 2 }?
    		^(StepExpr ^(AxisSpec DESCENDANT_OR_SELF) KindTestAnyKind) primaryExpr
	->	{ pred != null }? 
   		^(FilterExpr[$primExpr.start, "FilterExpr"] $primExpr $pred?)
   	->	$primExpr
    ;
    
predicateList
    :	predicate*
    ->	predicate*
    ;
    
predicate
    :	'[' expr ']'
    ->	^(Predicate expr)
    ;
    
/*
	Primary Expressions
	sub-primary expressions should generate an AST node, without the skipping technique used in expression chains
*/
primaryExpr
    : literal
    | varRef
    | parenthesizedExpr
    | contextItemExpr 
    | functionCall
    | orderedExpr
    | unorderedExpr
    | constructor
    ;
    
literal
    : numericLiteral -> ^(Literal numericLiteral)
    | slit=stringLiteral -> ^(Literal stringLiteral)
    ;
    
numericLiteral
    : inte=IntegerLiteral -> ^(Int[$inte.text])
    | dec=DecimalLiteral -> ^(Dec[$dec.text])
    | dbl=DoubleLiteral -> ^(Dbl[$dbl.text])
    ;
    
stringLiteral
    : qs=QuotedStringLiteral -> ^(Str[$qs.getText().substring(1, $qs.getText().length() - 1)])
    | as=AposedStringLiteral -> ^(Str[$as.getText().substring(1, $as.getText().length() - 1)])
    ;   
    
varRef
    :	'$' v=varName
//    ->	^(VariableVariableRef varName)
		-> VariableRef[variables.resolve($v.text)]
    ;
    
varName
    : qName
    ;
    
parenthesizedExpr
    :	'(' ')'			-> EmptySequence
    	| '(' expr ')'	-> expr
    ;
    
contextItemExpr
    : '.' -> ContextItemExpr
    ;
    
orderedExpr
    :	ORDERED LCurly expr RCurly
    ->	^(OrderedExpr expr)
    ;
    
unorderedExpr
    :	UNORDERED LCurly expr RCurly
    ->	^(UnorderedExpr expr)
    ;
    
functionCall                        // xgs:reserved-function-names // gn:parens
    : fqn=fqName '(' (exprSingle (',' exprSingle)* -> exprSingle+)? ')'
    ->	^(FunctionCall[$fqn.text] exprSingle*)
    ;
/*
	CONSTRUCTORS
*/    
    
constructor
    : directConstructor
    | computedConstructor
    ;
    

dirElemConstructor                                               //ws:explicit
: LAngle  { enterDirXml (); }  // '<'
       tagName=qName   { pushElemName($tagName.text); }
       attrList=dirAttributeList? S*
       (
       RClose { popElemName (); } // '/>'
         | (
         RAngle
         elemContent+=dirElemContent*
         LClose
         closeTag=qName { matchElemName($closeTag.text); }
         S*
         RAngle
         )
       )
       { leaveDirXml (); }
       // AST
       ->  ^(CompElementConstructor ^(Literal $tagName) ^(ContentSequence $attrList? $elemContent*))
    ;
directConstructor
@init{
String comment = null;
Pair<String, String> pi = null;
}
    :	
    	dirElemConstructor
    	| { comment=parseDirComment(); } DirCommentConstructor -> ^(CompCommentConstructor ^(Literal Str[comment]))
    	| { pi=parseDirPI(); }DirPIConstructor -> ^(CompPIConstructor ^(Literal Str[pi.getFirst()]) ^(Literal Str[pi.getSecond()]))
    ;

    

dirAttributeList
: (S+ dirAttribute)+
-> dirAttribute+
;
dirAttribute
: name=qName S* SymEq S* value=dirAttributeValue
-> ^(CompAttributeConstructor ^(Literal qName) ^(ContentSequence $value?))
;
    
dirAttributeValue                                                // ws:explicit
    : Quot quotAttrValue* Quot -> quotAttrValue*
    | Apos aposAttrValue* Apos -> aposAttrValue*
    ;
    
quotAttrValue
    :    
    c=EscapeQuot -> ^(Literal Str[$c, $c.getText()])
    | quotAttrValueContent -> quotAttrValueContent
    ;
    
aposAttrValue
    :
    c=EscapeApos -> ^(Literal Str[$c, $c.getText()])
    | aposAttrValueContent -> aposAttrValueContent
    ;
    
quotAttrValueContent
    : c=QuotAttrContentChar-> ^(Literal Str[$c, $c.getText()])
    | commonContent
    ;
    
aposAttrValueContent
    : c=AposAttrContentChar-> ^(Literal Str[$c, $c.getText()])
    | commonContent
    ;  
    
dirElemContent
	:	dirElemContentSingle -> dirElemContentSingle
	;    
    
dirElemContentSingle
@init{
String cdata = null;
}
    :	directConstructor
   	| { cdata = parseCData(); } CDataSection -> ^(Literal Str[cdata])
	| commonContent
	| c=ElementContentChar	-> ^(Literal Str[$c, $c.getText()])
   ;
    
commonContent
    : c=PredefinedEntityRef -> ^(Literal Str[$c, $c.getText()])
    | c=CharRef				-> ^(Literal Str[$c, $c.getText()])
    | c=EscapeLCurly		-> ^(Literal Str[$c, $c.getText()])
    | c=EscapeRCurly		-> ^(Literal Str[$c, $c.getText()])
    | dirEnclosedExpr
    ;
    
dirEnclosedExpr
    :	LCurly { enterXQuery(); } expr { leaveXQuery(); } RCurly
    ->	expr
    ;
    
//W3C grammar :
//dirCommentConstructor                                          // ws:explicit
//    : '<!--' DirCommentContents '-->'
//    ;
//dirPIConstructor                                               // ws:explicit
//    : '<?' PiTarget (S DirPIContents)? '?>'
//    ;
//cDataSection                                                   // ws:explicit
//    : '<![CDATA[' CDataSectionContents ']]>'
//    ;

computedConstructor
    : compDocConstructor
    | compElemConstructor
    | compAttrConstructor
    | compTextConstructor
    | compCommentConstructor
    | compPIConstructor
    | {xqVersion==XQUERY_3_0}? => compNamespaceConstructor        // XQuery 3.0
    ;
    
compDocConstructor
    :	DOCUMENT LCurly expr RCurly
    ->	^(CompDocumentConstructor ^(ContentSequence expr))
    ;
    
compElemConstructor
    :	ELEMENT
    	(
    		tag=qName -> ^(Literal $tag)
    		| (LCurly expr RCurly)  -> expr
    	)
    	LCurly contentExpr? RCurly
    ->  ^(CompElementConstructor $compElemConstructor ^(ContentSequence contentExpr?))
    ;
    
contentExpr
    :   expr -> expr
    ;
    
compAttrConstructor
    :	ATTRIBUTE
    	(
    		tag=qName -> ^(Literal $tag)
    		| (LCurly expr RCurly) -> expr
    	)
    	LCurly contentExpr? RCurly
    ->	^(CompAttributeConstructor $compAttrConstructor ^(ContentSequence contentExpr?))
    ;
    
compTextConstructor
    :	TEXT LCurly expr RCurly
    ->	^(CompTextConstructor expr)
    ;
    
compCommentConstructor
    :	COMMENT LCurly expr RCurly
    ->	^(CompCommentConstructor expr)
    ;
    
compPIConstructor
    :	PROCESSING_INSTRUCTION
    	(
    		name=ncName -> ^(Literal Str[$name.text])
    		| (LCurly expr RCurly) -> expr
    	)
    	LCurly vExpr=expr? RCurly
    ->	^(CompPIConstructor $compPIConstructor $vExpr?)
    ;
    
/*
	TYPES
*/
    
singleType
    : atomicType '?'?
    ;
    
typeDeclaration
    :	AS sequenceType
    ->	^(SequenceType sequenceType)
    ;
    
sequenceType
    :	(EMPTY_SEQUENCE '(' ')')
    	-> EmptySequenceType
    	| (itemType ((occurrenceIndicator) => occurrenceIndicator)?) 
    	-> itemType occurrenceIndicator?
    ;
    
occurrenceIndicator                                 // xgs:occurance-indicators
    : '?' -> CardinalityZeroOrOne
    | '*' -> CardinalityZeroOrMany
    | '+' -> CardinalityOneOrMany
    ;
    
itemType
    : kindTest
    | (ITEM '(' ')') -> ItemType
    | atomicType -> ^(AtomicType atomicType)
    ;
    
atomicType
    : qName
    ;
    
kindTest
    : documentTest
    | elementTest
    | attributeTest
    | schemaElementTest
    | schemaAttributeTest
    | piTest
    | commentTest
    | textTest
    | anyKindTest
    | {xqVersion==XQUERY_3_0}? => namespaceNodeTest               // XQuery 3.0
    ;
anyKindTest
    :ANYKIND
    ->	KindTestAnyKind
    ;
documentTest
    : DOCUMENT_NODE '(' (elementTest | schemaElementTest)? ')'
    ->	^(KindTestDocument elementTest? schemaElementTest?)
    ;
textTest
    :	TEXT '(' ')'
    ->	KindTestText
    ;
commentTest
    : COMMENT '(' ')'
    ->	KindTestComment
    ;
    
// for XPath 1.0 
piTest
    : PROCESSING_INSTRUCTION '(' (ncName | stringLiteral)? ')'
    ->	^(KindTestPi ncName? stringLiteral?)
    ;
attributeTest
    : ATTRIBUTE '(' (attribNameOrWildcard (',' typeName)?)? ')'
    ->	^(KindTestAttribute attribNameOrWildcard? typeName?)
    ;
attribNameOrWildcard
    : attributeName
    | '*'
    ;
schemaAttributeTest
    : SCHEMA_ATTRIBUTE '(' attributeDeclaration ')'
    ;
attributeDeclaration
    : attributeName
    ;
elementTest
    : ELEMENT '(' (elementNameOrWildcard (',' typeName '?'?)?)? ')'
    ->	^(KindTestElement elementNameOrWildcard? typeName? '?'?)
    ;
    
elementNameOrWildcard
    : elementName
    | '*'
    ;
    
schemaElementTest
    : SCHEMA_ELEMENT '(' elementDeclaration ')'
    ->	^(KindTestSchemaElement elementDeclaration)
    ;
    
/*
	SIMPLE QNAMES
*/    
    
elementDeclaration
    : elementName
    ;
attributeName
    : qName
    ;
elementName
    : qName
    ;
typeName
    : qName
    ;
uriLiteral
    : stringLiteral
    ;
    
/*
	XQUERY UPDATE
*/

revalidationDecl
    : DECLARE REVALIDATION (STRICT | LAX | SKIP)
    ;
insertExprTargetChoice
    : AS (FIRST -> InsertFirst| LAST -> InsertLast) INTO
    | INTO -> InsertInto
    | AFTER -> InsertAfter
    | BEFORE -> InsertBefore
    ;
insertExpr
    : INSERT (NODE | NODES) sourceExpr insertExprTargetChoice targetExpr -> ^(Insert insertExprTargetChoice sourceExpr targetExpr)
    ;
deleteExpr
    : DELETE (NODE | NODES) targetExpr -> ^(Delete targetExpr)
    ;
replaceExpr
    : REPLACE replaceTargetChoice targetExpr WITH exprSingle -> ^(Replace replaceTargetChoice targetExpr exprSingle)
    ;
replaceTargetChoice
	:	VALUE OF NODE -> ReplaceValue
	|	NODE -> ReplaceNode
	; 
   
renameExpr
    : RENAME NODE targetExpr AS newNameExpr -> ^(Rename targetExpr newNameExpr)
    ;
sourceExpr
    : exprSingle
    ;
targetExpr
    : exprSingle
    ;
newNameExpr
    : exprSingle
    ;
transformExpr
@init {
  variables.openScope();
}
@after {
  variables.closeScope();
}
  : COPY copyBinding (',' copyBinding)*
    MODIFY exprSingle
    RETURN exprSingle
  -> ^(Transform copyBinding+ 
       ^(Modify exprSingle)
       ^(TransformReturn exprSingle))
  ;
    
copyBinding
  :
  untypedVarBinding exprSingle
  -> ^(CopyBinding untypedVarBinding exprSingle)
  ;      

untypedVarBinding
  : '$' name=varName ':=' 
  ->  ^(Variable[variables.declare($name.text)])
  ;

/*
	XQUERY SCRIPTING
*/

assignmentExpr
    : SET '$' varName ':=' exprSingle
    ;
blockExpr
    : BLOCK block
    ;
block
    : LCurly blockDecls blockBody RCurly
    ;
blockDecls
    : (blockVarDecl ';')*
    ;
blockVarDecl
    : DECLARE '$' varName typeDeclaration? (':=' exprSingle)? 
         (',' '$' varName typeDeclaration? (':=' exprSingle)? )*
    ;
blockBody
    : expr
    ;
exitExpr
    : EXIT RETURNING exprSingle
    ;
whileExpr
    : WHILE '(' exprSingle ')' whileBody
    ;
whileBody
    : block
    ;

/*
	XQUERY FULL TEXT
*/

ftSelection
    : ftOr ftPosFilter*
    ;
ftOr
    : ftAnd (FTOR ftAnd)*
    ;
ftAnd
    : ftMildNot (FTAND ftMildNot)*
    ;
ftMildNot
    : ftUnaryNot (NOT IN ftUnaryNot)*
    ;
ftUnaryNot
    : FTNOT? ftPrimaryWithOptions
    ;
ftPrimaryWithOptions
    : ftPrimary (USING ftMatchOption)* ftWeight?
    ;
ftWeight
    : WEIGHT rangeExpr
    ;
ftPrimary
    : ftWords ftTimes?
    | '(' ftSelection ')'
    | ftExtensionSelection
    ;
ftWords
    : ftWordsValue ftAnyAllOption?
    ;
ftWordsValue
    : literal
    | LCurly expr RCurly
    ;
ftExtensionSelection
    : ({ parsePragma(); } Pragma)+ LCurly ftSelection? RCurly -> ^(Pragma ftSelection?)
    ;
ftAnyAllOption
    : ANY WORD?
    | ALL WORDS?
    | PHRASE
    ;
ftTimes
    : OCCURS ftRange TIMES
    ;
ftRange
    : EXACTLY  additiveExpr
    | AT LEAST additiveExpr
    | AT MOST  additiveExpr
    | FROM     additiveExpr TO additiveExpr
    ;
ftPosFilter
    : ftOrder
    | ftWindow
    | ftDistance
    | ftScope
    | ftContent
    ;
ftOrder
    : ORDERED
    ;
ftWindow
    : WINDOW additiveExpr ftUnit
    ;
ftDistance
    : DISTANCE ftRange ftUnit
    ;
ftUnit
    : WORDS
    | SENTENCES
    | PARAGRAPHS
    ;
ftScope
    : (SAME | DIFFERENT) ftBigUnit
    ;
ftBigUnit
    : SENTENCE
    | PARAGRAPH
    ;
ftContent
    : AT START
    | AT END
    | ENTIRE CONTENT
    ;
//W3C grammar :
//ftMatchOptions
//  : (ftMatchOption)+
//  ;
ftMatchOption
    : ftLanguageOption
    | ftWildCardOption
    | ftThesaurusOption
    | ftStemOption
    | ftCaseOption
    | ftDiacriticsOption
    | ftStopWordOption
    | ftExtensionOption
    ;
ftCaseOption
    : CASE INSENSITIVE
    | CASE SENSITIVE
    | LOWERCASE
    | UPPERCASE
    ;
ftDiacriticsOption
    : DIACRITICS INSENSITIVE
    | DIACRITICS SENSITIVE
    ;
ftStemOption
    :    STEMMING
    | NO STEMMING
    ;
ftThesaurusOption
    :    THESAURUS     (ftThesaurusID | DEFAULT) 
    |    THESAURUS '(' (ftThesaurusID | DEFAULT) (',' ftThesaurusID)* ')'
    | NO THESAURUS
    ;
ftThesaurusID
    : AT uriLiteral (RELATIONSHIP stringLiteral)? (ftRange LEVELS)?
    ;
ftStopWordOption
    :      STOP WORDS ftStopWords ftStopWordsInclExcl*
    | NO   STOP WORDS
    | STOP WORDS DEFAULT ftStopWordsInclExcl*
    ;
ftStopWords
    : AT uriLiteral
    | '(' stringLiteral (',' stringLiteral)* ')'
    ;
ftStopWordsInclExcl
    : (UNION | EXCEPT) ftStopWords
    ;
ftLanguageOption
    : LANGUAGE stringLiteral
    ;
ftWildCardOption
    :    WILDCARDS
    | NO WILDCARDS
    ;
ftExtensionOption
    : OPTION qName stringLiteral
    ;
ftIgnoreOption
    : WITHOUT CONTENT unionExpr
    ;

/*
	XQUERY 3.0
*/

contextItemDecl
    : {xqVersion==XQUERY_3_0}? =>
      DECLARE CONTEXT ITEM (AS itemType)? 
      ((':=' varValue) | (EXTERNAL (':=' varDefaultValue)?))
    ;
tryCatchExpr
    : tryClause catchClause+ -> ^(TryCatchExpr tryClause catchClause+)
    ;
tryClause
    : TRY LCurly tryTargetExpr RCurly -> ^(TryClause tryTargetExpr)
    ;
tryTargetExpr
    : expr
    ;
catchClause
    : CATCH catchErrorList catchVars? LCurly expr RCurly
    -> ^(CatchClause catchErrorList catchVars?)
    ;
catchErrorList
    : nameTest ('|' nameTest)* -> ^(CatchErrorList nameTest+)
    ;
catchVars
    : '(' catchErrorCode (',' catchErrorDesc (',' catchErrorVal)?)? ')'
    -> ^(CatchVar catchErrorCode catchErrorDesc? catchErrorVal?)
    ;
catchErrorCode
    : '$' varName
    ;
catchErrorDesc
    : '$' varName
    ;
catchErrorVal
    : '$' varName
    ;
compNamespaceConstructor
    : NAMESPACE (prefix | (LCurly prefixExpr RCurly)) LCurly uriExpr? RCurly
    ;
prefix
    : ncName
    ;
prefixExpr
    : expr
    ;
uriExpr
    : expr
    ;
namespaceNodeTest
    : NAMESPACE_NODE '(' ')'
    ;

/*
	NAMES
*/

qName
    :	ncn=ncName	
    //(ncn=ncName -> ncName)
    	(
    		Colon {noSpaceBefore();} local=ncName {noSpaceBefore();}
    		// TODO: was this necessary? (also above commented)
    		//-> ^(FullQName[$ncn.start, "FullQName"] $qName $local)
    	)?
		-> Qname[$qName.text]
    ;
eqName
    :
    qName | (uriLiteral ':' ncName)
    ;    
fqName
    :	ncn=ncName  Colon {noSpaceBefore();} ncName {noSpaceBefore();} -> Qname[$fqName.text]// -> ^(FullQName[$ncn.start, "FullQName"] ncName ncName)
    |	fncName -> Qname[$fncName.text]
    ;
ncName
    : fncName
    // reserved function names - not allowed in unprefixed form
    | ATTRIBUTE
    | COMMENT
    | DOCUMENT_NODE
    | ELEMENT
    | EMPTY_SEQUENCE
    | IF
    | ITEM
    | NODE
    | PROCESSING_INSTRUCTION
    | SCHEMA_ATTRIBUTE
    | SCHEMA_ELEMENT
    | TEXT
    | TYPESWITCH
    // intentionally not gated by semantic predicates
    // to facilitate writing "future proof" queries
    | WHILE                                                       // ext:update
    | NAMESPACE_NODE                                              // XQuery 3.0
	-> Str[$ncName.text]
    ;
fncName
    : NCName
    | ANCESTOR
    | ANCESTOR_OR_SELF
    | AND
    | AS
    | ASCENDING
    | AT
    | BASE_URI
    | BOUNDARY_SPACE
    | BY
    | CASE
    | CASTABLE
    | CAST
    | CHILD
    | COLLATION
    | CONSTRUCTION
    | COPY
    | COPY_NAMESPACES
    | DECLARE
    | DEFAULT
    | DESCENDANT
    | DESCENDANT_OR_SELF
    | DESCENDING
    | DIV
    | DOCUMENT
    | ELSE
    | EMPTY
    | ENCODING
    | EQ
    | EVERY
    | EXCEPT
    | EXTERNAL
    | FOLLOWING
    | FOLLOWING_SIBLING
    | FOR
    | FUNCTION
    | GE
    | GREATEST
    | GT
    | IDIV
    | IMPORT                  
    | INHERIT
    | IN
    | INSTANCE
    | INTERSECT
    | IS
    | LAX
    | LEAST
    | LE
    | LET
    | LT
    | MOD
    | MODULE
    | NAMESPACE
    | NE
    | NO_INHERIT
    | NO_PRESERVE
    | OF
    | OPTION
    | ORDERED
    | ORDERING
    | ORDER
    | OR
    | PARENT
    | PRECEDING
    | PRECEDING_SIBLING
    | PRESERVE
    | RETURN
    | SATISFIES
    | SCHEMA
    | SELF
    | SIMPLE
    | SOME
    | STABLE
    | STRIP
    | THEN
    | TO
    | TREAT
    | UNION
    | UNORDERED
    | VALIDATE                
    | VARIABLE
    | VERSION
    | WHERE
    | XQUERY
    | STRICT
    // start of ext:update tokens
    | AFTER
    | BEFORE
    | DELETE
    | FIRST
    | INSERT
    | INTO
    | LAST
    | MODIFY
    | NODES
    | RENAME
    | REPLACE
    | REVALIDATION
    | SKIP
    | UPDATING
    | VALUE
    | WITH
    // end   of ext:update    tokens
    // start of ext:scripting tokens
    | BLOCK
    | CONSTANT
    | EXIT
    | RETURNING
    | SEQUENTIAL
    | SET
    // WHILE
    // end   of ext:scripting tokens
    // start of ext:fulltext  tokens
    | ALL
    | ANY
    | CONTENT
    | DIACRITICS
    | DIFFERENT
    | DISTANCE
    | END
    | ENTIRE
    | EXACTLY
    | FROM
  //| FTAND         // stepExpr ambiguous
    | CONTAINS
    | FTNOT
    | FT_OPTION
  //| FTOR          // stepExpr ambiguous
    | INSENSITIVE
    | LANGUAGE
    | LEVELS
    | LOWERCASE
    | MOST
    | NO
    | NOT
    | OCCURS
    | PARAGRAPH
    | PARAGRAPHS
    | PHRASE
    | RELATIONSHIP
    | SAME
    | SCORE
    | SENSITIVE
    | SENTENCE
    | SENTENCES
    | START
    | STEMMING
    | STOP
    | THESAURUS
    | TIMES
    | UPPERCASE
    | USING
    | WEIGHT
    | WILDCARDS
    | WINDOW
    | WITHOUT
    | WORD
    | WORDS
    // end   of ext:fulltext tokens
    // start of XQuery 3.0   tokens
    | CATCH
    | CONTEXT
    | DETERMINISTIC
  //| NAMESPACE_NODE
    | NONDETERMINISTIC
    | TRY
    // tokens related to decimal formats
    | DECIMAL_FORMAT
    | DECIMAL_SEPARATOR
    | DIGIT
    | GROUPING_SEPARATOR
    | INFINITY
    | MINUS_SIGN
    | NAN
    | PATTERN_SEPARATOR
    | PER_MILLE
    | PERCENT
    | ZERO_DIGIT
    // tokens related to flwor enchancelents
    | COUNT
    | GROUP
    | NEXT
    | ONLY
    | PREVIOUS
    | SLIDING
    | TUMBLING
    | WHEN
    | ALLOWING
  //| EMPTY
    // end of XQUery 3.0 tokens
    ;
    
/***************
 * LEXER RULES *
 ***************/    
    
LAngle                  : '<';
RAngle                  : '>';
LCurly                  : '{';
RCurly                  : '}';
SymEq                   : '=';
Colon                   : ':';
LClose                  : '</';
RClose                  : '/>';
Quot                    : '"';
Apos                    : '\'';
fragment
EscapeQuot              : '""';
fragment
EscapeApos              : '\'\'';
fragment
EscapeLCurly            : '{{';
fragment
EscapeRCurly            : '}}';

ANCESTOR                : 'ancestor';
ANCESTOR_OR_SELF        : 'ancestor-or-self';
AND                     : 'and';
AS                      : 'as';
ASCENDING               : 'ascending';
AT                      : 'at';
ATTRIBUTE               : 'attribute';
BASE_URI                : 'base-uri';
BOUNDARY_SPACE          : 'boundary-space';
BY                      : 'by';
CASE                    : 'case';
CASTABLE                : 'castable';
CAST                    : 'cast';
CHILD                   : 'child';
COLLATION               : 'collation';
COMMENT                 : 'comment';
CONSTRUCTION            : 'construction';
COPY                    : 'copy';
COPY_NAMESPACES         : 'copy-namespaces';
DECLARE                 : 'declare';
DEFAULT                 : 'default';
DESCENDANT              : 'descendant';
DESCENDANT_OR_SELF      : 'descendant-or-self';
DESCENDING              : 'descending';
DIV                     : 'div';
DOCUMENT                : 'document';
DOCUMENT_NODE           : 'document-node';
ELEMENT                 : 'element';
ELSE                    : 'else';
EMPTY                   : 'empty';
EMPTY_SEQUENCE          : 'empty-sequence';
ENCODING                : 'encoding';
EQ                      : 'eq';
EVERY                   : 'every';
EXCEPT                  : 'except';
EXTERNAL                : 'external';
FOLLOWING               : 'following';
FOLLOWING_SIBLING       : 'following-sibling';
FOR                     : 'for';
FUNCTION                : 'function';
GE                      : 'ge';
GREATEST                : 'greatest';
GT                      : 'gt';
IDIV                    : 'idiv';
IF                      : 'if';
IMPORT                  : 'import';
INHERIT                 : 'inherit';
IN                      : 'in';
INSTANCE                : 'instance';
INTERSECT               : 'intersect';
IS                      : 'is';
ITEM                    : 'item';
LAX                     : 'lax';
LEAST                   : 'least';
LE                      : 'le';
LET                     : 'let';
LT                      : 'lt';
MOD                     : 'mod';
MODULE                  : 'module';
NAMESPACE               : 'namespace';
NE                      : 'ne';
NODE                    : 'node';
ANYKIND		: 'node()';
NO_INHERIT              : 'no-inherit';
NO_PRESERVE             : 'no-preserve';
OF                      : 'of';
OPTION                  : 'option';
ORDERED                 : 'ordered';
ORDERING                : 'ordering';
ORDER                   : 'order';
OR                      : 'or';
PARENT                  : 'parent';
PRECEDING               : 'preceding';
PRECEDING_SIBLING       : 'preceding-sibling';
PRESERVE                : 'preserve';
PROCESSING_INSTRUCTION  : 'processing-instruction';
RETURN                  : 'return';
SATISFIES               : 'satisfies';
SCHEMA_ATTRIBUTE        : 'schema-attribute';
SCHEMA_ELEMENT          : 'schema-element';
SCHEMA                  : 'schema';
SELF                    : 'self';
SIMPLE                  : 'simple';
SOME                    : 'some';
STABLE                  : 'stable';
STRICT                  : 'strict';
STRIP                   : 'strip';
TEXT                    : 'text';
THEN                    : 'then';
TO                      : 'to';
TREAT                   : 'treat';
TYPESWITCH              : 'typeswitch';
UNION                   : 'union';
UNORDERED               : 'unordered';
VALIDATE                : 'validate';
VARIABLE                : 'variable';
VERSION                 : 'version';
WHERE                   : 'where';
XQUERY                  : 'xquery';
// start of ext:update tokens
AFTER                   : 'after';
BEFORE                  : 'before';
DELETE                  : 'delete';
FIRST                   : 'first';
INSERT                  : 'insert';
INTO                    : 'into';
LAST                    : 'last';
MODIFY                  : 'modify';
NODES                   : 'nodes';
RENAME                  : 'rename';
REPLACE                 : 'replace';
REVALIDATION            : 'revalidation';
SKIP                    : 'skip';
UPDATING                : 'updating';
VALUE                   : 'value';
WITH                    : 'with';
// end   of ext:update    tokens
// start of ext:scripting tokens
BLOCK                   : 'block';
CONSTANT                : 'constant';
EXIT                    : 'exit';
SEQUENTIAL              : 'sequential';
RETURNING               : 'returning';
SET                     : 'set';
WHILE                   : 'while';
// end   of ext:scripting tokens
// start of ext:fulltext  tokens
ALL                     : 'all';
ANY                     : 'any';
CONTENT                 : 'content';
DIACRITICS              : 'diacritics';
DIFFERENT               : 'different';
DISTANCE                : 'distance';
END                     : 'end';
ENTIRE                  : 'entire';
EXACTLY                 : 'exactly';
FROM                    : 'from';
FTAND                   : 'ftand';
CONTAINS                : 'contains';
FTNOT                   : 'ftnot';
FT_OPTION               : 'ft-option';
FTOR                    : 'ftor';
INSENSITIVE             : 'insensitive';
LANGUAGE                : 'language';
LEVELS                  : 'levels';
LOWERCASE               : 'lowercase';
MOST                    : 'most';
NO                      : 'no';
NOT                     : 'not';
OCCURS                  : 'occurs';
PARAGRAPH               : 'paragraph';
PARAGRAPHS              : 'paragraphs';
PHRASE                  : 'phrase';
RELATIONSHIP            : 'relationship';
SAME                    : 'same';
SCORE                   : 'score';
SENSITIVE               : 'sensitive';
SENTENCE                : 'sentence';
SENTENCES               : 'sentences';
START                   : 'start';
STEMMING                : 'stemming';
STOP                    : 'stop';
THESAURUS               : 'thesaurus';
TIMES                   : 'times';
UPPERCASE               : 'uppercase';
USING                   : 'using';
WEIGHT                  : 'weight';
WILDCARDS               : 'wildcards';
WINDOW                  : 'window';
WITHOUT                 : 'without';
WORD                    : 'word';
WORDS                   : 'words';
// end   of ext:fulltext tokens
// start of XQuery 3.0   tokens
CATCH                   : 'catch';
CONTEXT                 : 'context';
DETERMINISTIC           : 'deterministic';
NAMESPACE_NODE          : 'namespace-node';
NONDETERMINISTIC        : 'nondeterministic';
TRY                     : 'try';
// tokens related to decimal formats
DECIMAL_FORMAT          : 'decimal-format';
DECIMAL_SEPARATOR       : 'decimal-separator';
DIGIT                   : 'digit';
GROUPING_SEPARATOR      : 'grouping-separatpr';
INFINITY                : 'infinity';
MINUS_SIGN              : 'minus-sign';
NAN                     : 'NaN';
PER_MILLE               : 'per-mille';
PERCENT                 : 'percent';
PATTERN_SEPARATOR       : 'pattern-separator';
ZERO_DIGIT              : 'zero-digit';
// tokens related to flwor enhancements
COUNT                   : 'count';
GROUP                   : 'group';
NEXT                    : 'next';
ONLY                    : 'only';
PREVIOUS                : 'previous';
SLIDING                 : 'sliding';
TUMBLING                : 'tumbling';
WHEN                    : 'when';
ALLOWING                : 'allowing';
// end of XQuery 3.0 tokens

DirCommentConstructor                                            // ws:explicit
    : '<!--' (options {greedy=false;} : . )* '-->'   
    ;
DirPIConstructor    
    : '<?' VS? NCName (VS (options {greedy=false;} : . )*)? '?>' // ws:explicit
    ;
/*
// Only allowed within direct XML and hence - parsed by XMLexer
CDataSection
    : '<![CDATA[' (options {greedy=false;} : . )* ']]>'          // ws:explicit  
    ;
*/
Pragma
    : '(#' VS? NCName (Colon NCName)? (VS (options {greedy=false;} : .)*)? '#)'
    ;
/*
// W3C grammar :
DirCommentContents                                               // ws:explicit  
    : ((Char - '-') | ('-' (Char - '-')))*
    ;
PiTarget
    : Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
    ;
Name
    : NameStartChar (NameChar)*
    ;
DirPIContents                                                    // ws:explicit 
    : (Char* - (Char* '?>' Char*))
    ;
CDataSectionContents                                             // ws:explicit
    : (Char* - (Char* ']]>' Char*))
    ;
PragmaContents
    : (Char - (Char* '#)' Char*))
    ;
*/
IntegerLiteral
    : Digits
    ;
DecimalLiteral
    : ('.' Digits) | (Digits '.' '0'..'9'*)
    ;
DoubleLiteral
    : (('.' Digits) | (Digits ('.' '0'..'9'*)?)) ('e' | 'E') ('+'|'-')? Digits
    ;
    
QuotedStringLiteral
	: Quot (
          options {greedy=false;}:
          (PredefinedEntityRef | CharRef | EscapeQuot | ~('"'  | '&'))*
          )
      Quot
      ;
      
AposedStringLiteral
	: Apos (
          options {greedy=false;}:
          (PredefinedEntityRef | CharRef | EscapeApos | ~('\''  | '&'))*
          )
      Apos
      ;
    
PredefinedEntityRef
    : '&' ('lt' | 'gt' | 'apos' | 'quot' | 'amp' ) ';'
    ;
CharRef
    : '&#'  Digits    ';' {checkCharRef();}
    | '&#x' HexDigits ';' {checkCharRef();}
    ;
Comment
    : '(:' (options {greedy=false;}: Comment | . )* ':)' { $channel = HIDDEN; }
    ;
NCName
    : NCNameStartChar NCNameChar*
    ;
S
    : ('\u0009' | '\u000A' | '\u000D' | '\u0020')+ { $channel = HIDDEN; }
    ;
fragment
VS
    : ('\u0009' | '\u000A' | '\u000D' | '\u0020')+
    ;
fragment
Digits
    : '0'..'9'+
    ;
fragment
HexDigits
    : ('0'..'9' | 'a'..'f' | 'A'..'F')+
    ;
fragment
Char
    : '\u0009'           | '\u000A'           | '\u000D' 
    | '\u0020'..'\uD7FF' | '\uE000'..'\uFFFD' // | '\u10000'..'\u10FFFF'
    ; 
fragment
NCNameStartChar
    : Letter | '_'
    ;
fragment
NCNameChar
    // NameChar - ':'  http://www.w3.org/TR/REC-xml-names/#NT-NCName
    : 'A'..'Z'           | 'a'..'z'           | '_' 
    | '\u00C0'..'\u00D6' | '\u00D8'..'\u00F6' | '\u00F8'..'\u02FF' 
    | '\u0370'..'\u037D' | '\u037F'..'\u1FFF' | '\u200C'..'\u200D' 
    | '\u2070'..'\u218F' | '\u2C00'..'\u2FEF' | '\u3001'..'\uD7FF' 
    | '\uF900'..'\uFDCF' | '\uFDF0'..'\uFFFD' 
  //| ':'                | '\u10000..'\uEFFFF] // end of NameStartChar
    | '-'                | '.'                | '0'..'9' 
    | '\u00B7'           | '\u0300'..'\u036F' | '\u203F'..'\u2040'
    ;
fragment
Letter
    // http://www.w3.org/TR/REC-xml/#NT-Letter
    : '\u0041'..'\u005A' | '\u0061'..'\u007A' | '\u00C0'..'\u00D6' 
    | '\u00D8'..'\u00F6' | '\u00F8'..'\u00FF' | '\u0100'..'\u0131'
    | '\u0134'..'\u013E' | '\u0141'..'\u0148' | '\u014A'..'\u017E'
    | '\u0180'..'\u01C3' | '\u01CD'..'\u01F0' | '\u01F4'..'\u01F5' 
    | '\u01FA'..'\u0217' | '\u0250'..'\u02A8' | '\u02BB'..'\u02C1'
    | '\u0386'           | '\u0388'..'\u038A' | '\u038C'
    | '\u038E'..'\u03A1' | '\u03A3'..'\u03CE' | '\u03D0'..'\u03D6' 
    | '\u03DA'           | '\u03DC'           | '\u03DE'
    | '\u03E0'           | '\u03E2'..'\u03F3' | '\u0401'..'\u040C' 
    | '\u040E'..'\u044F' | '\u0451'..'\u045C' | '\u045E'..'\u0481' 
    | '\u0490'..'\u04C4' | '\u04C7'..'\u04C8' | '\u04CB'..'\u04CC' 
    | '\u04D0'..'\u04EB' | '\u04EE'..'\u04F5' | '\u04F8'..'\u04F9' 
    | '\u0531'..'\u0556' | '\u0559'           | '\u0561'..'\u0586' 
    | '\u05D0'..'\u05EA' | '\u05F0'..'\u05F2' | '\u0621'..'\u063A' 
    | '\u0641'..'\u064A' | '\u0671'..'\u06B7' | '\u06BA'..'\u06BE' 
    | '\u06C0'..'\u06CE' | '\u06D0'..'\u06D3' | '\u06D5'
    | '\u06E5'..'\u06E6' | '\u0905'..'\u0939' | '\u093D'
    | '\u0958'..'\u0961' | '\u0985'..'\u098C' | '\u098F'..'\u0990'
    | '\u0993'..'\u09A8' | '\u09AA'..'\u09B0' | '\u09B2'
    | '\u09B6'..'\u09B9' | '\u09DC'..'\u09DD' | '\u09DF'..'\u09E1' 
    | '\u09F0'..'\u09F1' | '\u0A05'..'\u0A0A' | '\u0A0F'..'\u0A10' 
    | '\u0A13'..'\u0A28' | '\u0A2A'..'\u0A30' | '\u0A32'..'\u0A33' 
    | '\u0A35'..'\u0A36' | '\u0A38'..'\u0A39' | '\u0A59'..'\u0A5C' 
    | '\u0A5E'           | '\u0A72'..'\u0A74' | '\u0A85'..'\u0A8B' 
    | '\u0A8D'           | '\u0A8F'..'\u0A91' | '\u0A93'..'\u0AA8' 
    | '\u0AAA'..'\u0AB0' | '\u0AB2'..'\u0AB3' | '\u0AB5'..'\u0AB9'
    | '\u0ABD'           | '\u0AE0'           | '\u0B05'..'\u0B0C'
    | '\u0B0F'..'\u0B10' | '\u0B13'..'\u0B28' | '\u0B2A'..'\u0B30'
    | '\u0B32'..'\u0B33' | '\u0B36'..'\u0B39' | '\u0B3D'
    | '\u0B5C'..'\u0B5D' | '\u0B5F'..'\u0B61' | '\u0B85'..'\u0B8A'
    | '\u0B8E'..'\u0B90' | '\u0B92'..'\u0B95' | '\u0B99'..'\u0B9A'
    | '\u0B9C'           | '\u0B9E'..'\u0B9F' | '\u0BA3'..'\u0BA4'
    | '\u0BA8'..'\u0BAA' | '\u0BAE'..'\u0BB5' | '\u0BB7'..'\u0BB9'
    | '\u0C05'..'\u0C0C' | '\u0C0E'..'\u0C10' | '\u0C12'..'\u0C28'
    | '\u0C2A'..'\u0C33' | '\u0C35'..'\u0C39' | '\u0C60'..'\u0C61'
    | '\u0C85'..'\u0C8C' | '\u0C8E'..'\u0C90' | '\u0C92'..'\u0CA8'
    | '\u0CAA'..'\u0CB3' | '\u0CB5'..'\u0CB9' | '\u0CDE'
    | '\u0CE0'..'\u0CE1' | '\u0D05'..'\u0D0C' | '\u0D0E'..'\u0D10'
    | '\u0D12'..'\u0D28' | '\u0D2A'..'\u0D39' | '\u0D60'..'\u0D61'
    | '\u0E01'..'\u0E2E' | '\u0E30'           | '\u0E32'..'\u0E33'
    | '\u0E40'..'\u0E45' | '\u0E81'..'\u0E82' | '\u0E84'
    | '\u0E87'..'\u0E88' | '\u0E8A'           | '\u0E8D'
    | '\u0E94'..'\u0E97' | '\u0E99'..'\u0E9F' | '\u0EA1'..'\u0EA3'
    | '\u0EA5'           | '\u0EA7'           | '\u0EAA'..'\u0EAB'
    | '\u0EAD'..'\u0EAE' | '\u0EB0'           | '\u0EB2'..'\u0EB3'
    | '\u0EBD'           | '\u0EC0'..'\u0EC4' | '\u0F40'..'\u0F47'
    | '\u0F49'..'\u0F69' | '\u10A0'..'\u10C5' | '\u10D0'..'\u10F6'
    | '\u1100'           | '\u1102'..'\u1103' | '\u1105'..'\u1107'
    | '\u1109'           | '\u110B'..'\u110C' | '\u110E'..'\u1112'
    | '\u113C'           | '\u113E'           | '\u1140'
    | '\u114C'           | '\u114E'           | '\u1150'
    | '\u1154'..'\u1155' | '\u1159'           | '\u115F'..'\u1161'
    | '\u1163'           | '\u1165'           | '\u1167'
    | '\u1169'           | '\u116D'..'\u116E' | '\u1172'..'\u1173'
    | '\u1175'           | '\u119E'           | '\u11A8'
    | '\u11AB'           | '\u11AE'..'\u11AF' | '\u11B7'..'\u11B8'
    | '\u11BA'           | '\u11BC'..'\u11C2' | '\u11EB'
    | '\u11F0'           | '\u11F9'           | '\u1E00'..'\u1E9B'
    | '\u1EA0'..'\u1EF9' | '\u1F00'..'\u1F15' | '\u1F18'..'\u1F1D'
    | '\u1F20'..'\u1F45' | '\u1F48'..'\u1F4D' | '\u1F50'..'\u1F57'
    | '\u1F59'           | '\u1F5B'           | '\u1F5D'
    | '\u1F5F'..'\u1F7D' | '\u1F80'..'\u1FB4' | '\u1FB6'..'\u1FBC'
    | '\u1FBE'           | '\u1FC2'..'\u1FC4' | '\u1FC6'..'\u1FCC'
    | '\u1FD0'..'\u1FD3' | '\u1FD6'..'\u1FDB' | '\u1FE0'..'\u1FEC'
    | '\u1FF2'..'\u1FF4' | '\u1FF6'..'\u1FFC' | '\u2126'
    | '\u212A'..'\u212B' | '\u212E'           | '\u2180'..'\u2182'
    | '\u3041'..'\u3094' | '\u30A1'..'\u30FA' | '\u3105'..'\u312C'
    | '\uAC00'..'\uD7A3' | '\u4E00'..'\u9FA5' | '\u3007'
    | '\u3021'..'\u3029'
;