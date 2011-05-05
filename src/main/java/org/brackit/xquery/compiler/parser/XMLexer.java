// $ANTLR 3.2 Sep 23, 2009 12:02:23 org/brackit/xquery/compiler/parser/XMLexer.g 2011-05-05 13:18:34

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


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class XMLexer extends Lexer {
    public static final int FUNCTION=204;
    public static final int OrderBySpec=49;
    public static final int Insert=160;
    public static final int PATTERN_SEPARATOR=226;
    public static final int TypeSwitchCase=155;
    public static final int LetBind=191;
    public static final int DivideOp=100;
    public static final int ReplaceValue=169;
    public static final int EXCEPT=285;
    public static final int Int=176;
    public static final int DISTANCE=370;
    public static final int EveryQuantifier=159;
    public static final int SchemaImport=142;
    public static final int KindTestPi=71;
    public static final int PRECEDING=311;
    public static final int WORDS=363;
    public static final int RETURN=265;
    public static final int ValidateLax=127;
    public static final int ELEMENT=203;
    public static final int Rename=170;
    public static final int EmptySequence=117;
    public static final int TypeSwitch=154;
    public static final int EQ=291;
    public static final int ValueCompLE=90;
    public static final int GE=296;
    public static final int STRICT=300;
    public static final int PRESERVE=200;
    public static final int MultiplyOp=99;
    public static final int TYPESWITCH=269;
    public static final int STOP=389;
    public static final int ItemType=148;
    public static final int CONTENT=378;
    public static final int S=318;
    public static final int VALUE=346;
    public static final int SubtractOp=98;
    public static final int TryCatchExpr=75;
    public static final int EMPTY=212;
    public static final int INTERSECT=284;
    public static final int GROUP=260;
    public static final int FTAND=356;
    public static final int ANYKIND=329;
    public static final int TypedVariableDeclaration=140;
    public static final int SCHEMA_ELEMENT=332;
    public static final int NO_PRESERVE=227;
    public static final int CONSTANT=237;
    public static final int GT=295;
    public static final int UnorderedExpr=121;
    public static final int LClose=8;
    public static final int IDivideOp=101;
    public static final int CollationDeclaration=35;
    public static final int NAN=221;
    public static final int MODIFY=349;
    public static final int Wildcard=63;
    public static final int THESAURUS=386;
    public static final int VERSION=195;
    public static final int BoundarySpaceDeclaration=31;
    public static final int ASCENDING=263;
    public static final int InstanceofExpr=109;
    public static final int UPDATING=240;
    public static final int ForLetExpr=42;
    public static final int AT=234;
    public static final int AS=271;
    public static final int CONSTRUCTION=238;
    public static final int DOCUMENT=324;
    public static final int PREVIOUS=256;
    public static final int NODES=342;
    public static final int SequenceType=146;
    public static final int DefaultElementNamespace=144;
    public static final int Predicate=114;
    public static final int CASTABLE=289;
    public static final int BY=261;
    public static final int ValueCompLT=91;
    public static final int SwitchCase=81;
    public static final int INHERIT=228;
    public static final int ANCESTOR_OR_SELF=312;
    public static final int UnionExpr=103;
    public static final int VNCName=431;
    public static final int EscapeApos=15;
    public static final int FTNOT=358;
    public static final int MODULE=196;
    public static final int ORDERED=209;
    public static final int NAMESPACE_NODE=396;
    public static final int SAME=373;
    public static final int Parameter=137;
    public static final int VariableRef=141;
    public static final int SCHEMA_ATTRIBUTE=331;
    public static final int CompDocumentConstructor=129;
    public static final int WildcardAfterColon=65;
    public static final int GroupByClause=45;
    public static final int Str=179;
    public static final int INSTANCE=286;
    public static final int INFINITY=219;
    public static final int Join=188;
    public static final int ValidateExpr=125;
    public static final int BLOCK=351;
    public static final int WILDCARDS=391;
    public static final int MainModule=23;
    public static final int DESCENDING=264;
    public static final int SEQUENTIAL=239;
    public static final int SELF=304;
    public static final int WhereClause=55;
    public static final int ValueCompNE=89;
    public static final int TryClause=76;
    public static final int LT=293;
    public static final int Collation=53;
    public static final int DIFFERENT=374;
    public static final int COUNT=258;
    public static final int OrderByExprBinding=50;
    public static final int OrderByEmptyMode=52;
    public static final int ReplaceNode=168;
    public static final int FunctionDeclaration=136;
    public static final int Digits=399;
    public static final int Char=404;
    public static final int RETURNING=353;
    public static final int QuotAttrContentChar=20;
    public static final int LCurly=6;
    public static final int Dbl=178;
    public static final int ForClause=40;
    public static final int NegateExpr=113;
    public static final int RangeExpr=108;
    public static final int COMMENT=325;
    public static final int StepExpr=60;
    public static final int INTO=337;
    public static final int REVALIDATION=333;
    public static final int Quot=12;
    public static final int CommonContentChar=432;
    public static final int NE=292;
    public static final int EXIT=352;
    public static final int NO_INHERIT=229;
    public static final int ModuleImport=37;
    public static final int AposAttrContentChar=21;
    public static final int AposedStringLiteral=317;
    public static final int DefaultFunctionNamespace=145;
    public static final int WITHOUT=392;
    public static final int NO=385;
    public static final int EVERY=268;
    public static final int SENSITIVE=380;
    public static final int Colon=11;
    public static final int DELETE=343;
    public static final int WEIGHT=359;
    public static final int ParenthesizedExpr=118;
    public static final int OF=287;
    public static final int Dec=177;
    public static final int EmptySequenceType=147;
    public static final int DESCENDANT_OR_SELF=305;
    public static final int CastableExpr=111;
    public static final int OR=275;
    public static final int ConstructionDeclaration=36;
    public static final int EscapeQuot=14;
    public static final int AllowingEmpty=47;
    public static final int Letter=405;
    public static final int NCName=397;
    public static final int WHERE=259;
    public static final int LAX=299;
    public static final int OrderModifier=54;
    public static final int ValueCompGE=92;
    public static final int NamespaceDeclaration=27;
    public static final int KindTestSchemaElement=69;
    public static final int GroupBy=186;
    public static final int SymEq=10;
    public static final int NodeCompIs=94;
    public static final int DECIMAL_SEPARATOR=217;
    public static final int Delete=166;
    public static final int CONTEXT=393;
    public static final int IF=272;
    public static final int ForBind=190;
    public static final int IN=245;
    public static final int RAngle=5;
    public static final int ExceptExpr=105;
    public static final int IS=297;
    public static final int SOME=266;
    public static final int NEXT=257;
    public static final int WildcardBeforeColon=64;
    public static final int DIGIT=225;
    public static final int ValueCompGT=93;
    public static final int Prolog=26;
    public static final int SLIDING=251;
    public static final int WITH=345;
    public static final int HexDigits=400;
    public static final int XQUERY=193;
    public static final int EXACTLY=367;
    public static final int QueryBody=25;
    public static final int DEFAULT=202;
    public static final int DESCENDANT=302;
    public static final int FilterExpr=115;
    public static final int PHRASE=364;
    public static final int Modify=173;
    public static final int InsertLast=162;
    public static final int ElementContentChar=18;
    public static final int VersionDeclaration=29;
    public static final int EscapeLCurly=16;
    public static final int CopyBinding=172;
    public static final int Replace=167;
    public static final int ValueCompEQ=88;
    public static final int LEAST=214;
    public static final int WINDOW=250;
    public static final int OrderedExpr=120;
    public static final int PARAGRAPHS=372;
    public static final int LET=248;
    public static final int LE=294;
    public static final int Selection=185;
    public static final int CardinalityZeroOrMany=153;
    public static final int CompCommentConstructor=132;
    public static final int ValidateStrict=126;
    public static final int GeneralCompLT=84;
    public static final int VS=398;
    public static final int Apos=13;
    public static final int PER_MILLE=223;
    public static final int MOD=282;
    public static final int GeneralCompLE=85;
    public static final int CONTAINS=277;
    public static final int ContentSequence=134;
    public static final int NOT=357;
    public static final int ExternalVariable=139;
    public static final int EOF=-1;
    public static final int IMPORT=232;
    public static final int OrderBy=187;
    public static final int RClose=9;
    public static final int USING=207;
    public static final int T__426=426;
    public static final int SENTENCE=375;
    public static final int T__427=427;
    public static final int T__428=428;
    public static final int T__429=429;
    public static final int T__422=422;
    public static final int T__423=423;
    public static final int T__424=424;
    public static final int T__425=425;
    public static final int T__430=430;
    public static final int OrderByClause=48;
    public static final int QuotedStringLiteral=316;
    public static final int OrExpr=106;
    public static final int ReturnExpr=184;
    public static final int DIACRITICS=383;
    public static final int CHILD=301;
    public static final int T__419=419;
    public static final int NONDETERMINISTIC=243;
    public static final int TreatExpr=110;
    public static final int T__413=413;
    public static final int T__414=414;
    public static final int T__411=411;
    public static final int T__412=412;
    public static final int T__417=417;
    public static final int NodeCompPrecedes=95;
    public static final int T__418=418;
    public static final int T__415=415;
    public static final int LibraryModule=24;
    public static final int ELSE=274;
    public static final int KindTestText=73;
    public static final int T__416=416;
    public static final int FlowrExpr=39;
    public static final int ModulusOp=102;
    public static final int T__421=421;
    public static final int T__420=420;
    public static final int ContextItemExpr=119;
    public static final int GeneralCompNE=83;
    public static final int FTOR=355;
    public static final int XQuery=22;
    public static final int RCurly=7;
    public static final int ReturnClause=56;
    public static final int T__408=408;
    public static final int T__409=409;
    public static final int LANGUAGE=390;
    public static final int CompTextConstructor=131;
    public static final int LetClause=43;
    public static final int LOWERCASE=381;
    public static final int INSENSITIVE=379;
    public static final int BaseURIDeclaration=30;
    public static final int FT_OPTION=206;
    public static final int T__406=406;
    public static final int T__407=407;
    public static final int T__410=410;
    public static final int CompAttributeConstructor=130;
    public static final int END=255;
    public static final int DoubleLiteral=315;
    public static final int RENAME=347;
    public static final int OPTION=205;
    public static final int CharRef=322;
    public static final int SwitchExpr=80;
    public static final int InsertInto=163;
    public static final int BOUNDARY_SPACE=199;
    public static final int DirCommentConstructor=319;
    public static final int CatchVar=79;
    public static final int ONLY=254;
    public static final int OCCURS=365;
    public static final int KindTestComment=72;
    public static final int GeneralCompGT=86;
    public static final int SATISFIES=267;
    public static final int IDIV=281;
    public static final int EMPTY_SEQUENCE=327;
    public static final int PARENT=308;
    public static final int GeneralCompGE=87;
    public static final int Literal=175;
    public static final int THEN=273;
    public static final int CatchErrorList=78;
    public static final int COLLATION=230;
    public static final int MINUS_SIGN=220;
    public static final int REPLACE=344;
    public static final int PosVariableBinding=58;
    public static final int ANCESTOR=309;
    public static final int KindTestAnyKind=74;
    public static final int ComparisonExpr=122;
    public static final int MOST=368;
    public static final int AxisSpec=61;
    public static final int Start=189;
    public static final int NCNameChar=403;
    public static final int ITEM=328;
    public static final int TO=279;
    public static final int GroupBySpec=46;
    public static final int CompElementConstructor=128;
    public static final int AddOp=97;
    public static final int STEMMING=384;
    public static final int Transform=171;
    public static final int SET=350;
    public static final int NodeCompFollows=96;
    public static final int IfExpr=124;
    public static final int TEXT=278;
    public static final int DirPIConstructor=320;
    public static final int UNION=283;
    public static final int CompPIConstructor=133;
    public static final int CardinalityOne=150;
    public static final int NameTest=62;
    public static final int FOLLOWING_SIBLING=306;
    public static final int SCHEMA=233;
    public static final int ENTIRE=377;
    public static final int WHEN=253;
    public static final int VALIDATE=298;
    public static final int JoinExpr=181;
    public static final int DECLARE=198;
    public static final int START=252;
    public static final int IntegerLiteral=313;
    public static final int DIV=280;
    public static final int ForLetBindingExpr=41;
    public static final int PathExpr=59;
    public static final int CardinalityOneOrMany=151;
    public static final int FIRST=335;
    public static final int SENTENCES=371;
    public static final int CAST=290;
    public static final int EXTERNAL=235;
    public static final int InsertAfter=165;
    public static final int WHILE=354;
    public static final int DETERMINISTIC=242;
    public static final int CASE=270;
    public static final int ENCODING=194;
    public static final int OptionDeclaration=28;
    public static final int JoinClause=183;
    public static final int AtomicType=149;
    public static final int DOCUMENT_NODE=330;
    public static final int KindTestSchemaAttribute=70;
    public static final int CDataSection=321;
    public static final int TypedVariableBinding=57;
    public static final int WORD=361;
    public static final int UPPERCASE=382;
    public static final int Variable=138;
    public static final int INSERT=340;
    public static final int Comment=401;
    public static final int CountClause=44;
    public static final int ZERO_DIGIT=224;
    public static final int LAST=336;
    public static final int EscapeRCurly=323;
    public static final int CardinalityZeroOrOne=152;
    public static final int LeftJoinExpr=182;
    public static final int OrderByKind=51;
    public static final int NODE=341;
    public static final int KindTestAttribute=68;
    public static final int GeneralCompEQ=82;
    public static final int DECIMAL_FORMAT=216;
    public static final int GREATEST=213;
    public static final int LAngle=4;
    public static final int AndExpr=107;
    public static final int PROCESSING_INSTRUCTION=326;
    public static final int TRY=394;
    public static final int RELATIONSHIP=387;
    public static final int NAMESPACE=197;
    public static final int SKIP=334;
    public static final int VARIABLE=236;
    public static final int TypeSwitchDefault=156;
    public static final int BASE_URI=231;
    public static final int KindTestElement=67;
    public static final int OrderingModeDeclaration=32;
    public static final int FROM=369;
    public static final int CATCH=395;
    public static final int PRECEDING_SIBLING=310;
    public static final int Namespace=143;
    public static final int SIMPLE=241;
    public static final int ORDER=211;
    public static final int ATTRIBUTE=303;
    public static final int FOR=244;
    public static final int FOLLOWING=307;
    public static final int AND=276;
    public static final int IntersectExpr=104;
    public static final int PARAGRAPH=376;
    public static final int COPY_NAMESPACES=215;
    public static final int TransformReturn=174;
    public static final int SomeQuantifier=158;
    public static final int COPY=348;
    public static final int FunctionCall=135;
    public static final int ALL=362;
    public static final int CopyNamespacesDeclaration=34;
    public static final int CatchClause=77;
    public static final int STRIP=201;
    public static final int STABLE=262;
    public static final int KindTestDocument=66;
    public static final int NCNameStartChar=402;
    public static final int Count=192;
    public static final int PERCENT=222;
    public static final int Pragma=38;
    public static final int CastExpr=112;
    public static final int Qname=180;
    public static final int TUMBLING=249;
    public static final int ArithmeticExpr=123;
    public static final int BEFORE=339;
    public static final int AFTER=338;
    public static final int DecimalLiteral=314;
    public static final int GROUPING_SEPARATOR=218;
    public static final int UNORDERED=210;
    public static final int EscapeRClurly=17;
    public static final int SequenceExpr=116;
    public static final int ANY=360;
    public static final int SCORE=247;
    public static final int InsertFirst=161;
    public static final int InsertBefore=164;
    public static final int ORDERING=208;
    public static final int ALLOWING=246;
    public static final int PredefinedEntityRef=19;
    public static final int EmptyOrderDeclaration=33;
    public static final int TIMES=366;
    public static final int LEVELS=388;
    public static final int QuantifiedExpr=157;
    public static final int TREAT=288;

        protected class LexerState
        {
            public final boolean fromXML;
            public boolean inTag = true;
            public boolean inQuotAttr = false;
            public boolean inAposAttr = false;

            public LexerState(boolean previousInXMl)
            {
                this.fromXML = previousInXMl;
            }
        }

        protected Stack<LexerState> stateStack = new Stack<LexerState>();;
            
        public void pushState(boolean fromXML)
        {
            stateStack.push(new LexerState(fromXML));
        }

        public boolean popState()
        {
            LexerState state = stateStack.pop();
            return ((state != null) && (state.fromXML));
        }

    	protected boolean isInTag()
    	{
    		return stateStack.peek().inTag;
    	}
    	
    	protected void setInTag()
    	{
    		stateStack.peek().inTag = true;
    	}
    	
    	protected void unsetInTag()
    	{
    		stateStack.peek().inTag = false;
    	}
    	
    	protected void negateInTag()
    	{
    		stateStack.peek().inTag = !stateStack.peek().inTag;
    	}
    	
    	protected boolean isInQuotAttr()
    	{
    		return stateStack.peek().inQuotAttr;
    	}
    	
    	protected void setInQuotAttr()
    	{
    		stateStack.peek().inQuotAttr = true;
    	}
    	
    	protected void unsetInQuotAttr()
    	{
    		stateStack.peek().inQuotAttr = false;
    	}
    	
    	protected void negateInQuotAttr()
    	{
    		stateStack.peek().inQuotAttr = !stateStack.peek().inQuotAttr;
    	}
    	
    	protected boolean isInAposAttr()
    	{
    		return stateStack.peek().inAposAttr;
    	}
    	
    	protected void setInAposAttr()
    	{
    		stateStack.peek().inAposAttr = true;
    	}
    	
    	protected void unsetInAposAttr()
    	{
    		stateStack.peek().inAposAttr = false;
    	}
    	
    	protected void negateInAposAttr()
    	{
    		stateStack.peek().inAposAttr = !stateStack.peek().inAposAttr;
    	}


    // delegates
    // delegators

    public XMLexer() {;} 
    public XMLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public XMLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "org/brackit/xquery/compiler/parser/XMLexer.g"; }

    // $ANTLR start "LAngle"
    public final void mLAngle() throws RecognitionException {
        try {
            int _type = LAngle;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:140:14: ({...}? => '<' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:140:16: {...}? => '<'
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "LAngle", " !isInTag() ");
            }
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LAngle"

    // $ANTLR start "RAngle"
    public final void mRAngle() throws RecognitionException {
        try {
            int _type = RAngle;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:141:14: ({...}? => '>' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:141:16: {...}? => '>'
            {
            if ( !((  isInTag() )) ) {
                throw new FailedPredicateException(input, "RAngle", "  isInTag() ");
            }
            match('>'); 
             unsetInTag(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RAngle"

    // $ANTLR start "LClose"
    public final void mLClose() throws RecognitionException {
        try {
            int _type = LClose;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:142:14: ({...}? => '</' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:142:16: {...}? => '</'
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "LClose", " !isInTag() ");
            }
            match("</"); 

             setInTag();  

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LClose"

    // $ANTLR start "RClose"
    public final void mRClose() throws RecognitionException {
        try {
            int _type = RClose;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:143:14: ({...}? => '/>' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:143:16: {...}? => '/>'
            {
            if ( !((  isInTag() )) ) {
                throw new FailedPredicateException(input, "RClose", "  isInTag() ");
            }
            match("/>"); 

             unsetInTag(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RClose"

    // $ANTLR start "SymEq"
    public final void mSymEq() throws RecognitionException {
        try {
            int _type = SymEq;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:144:14: ({...}? => '=' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:144:16: {...}? => '='
            {
            if ( !((  isInTag() )) ) {
                throw new FailedPredicateException(input, "SymEq", "  isInTag() ");
            }
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SymEq"

    // $ANTLR start "LCurly"
    public final void mLCurly() throws RecognitionException {
        try {
            int _type = LCurly;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:145:14: ({...}? => '{' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:145:16: {...}? => '{'
            {
            if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "LCurly", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
            }
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LCurly"

    // $ANTLR start "RCurly"
    public final void mRCurly() throws RecognitionException {
        try {
            int _type = RCurly;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:146:14: ({...}? => '}' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:146:16: {...}? => '}'
            {
            if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "RCurly", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
            }
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RCurly"

    // $ANTLR start "Quot"
    public final void mQuot() throws RecognitionException {
        try {
            int _type = Quot;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:147:14: ({...}? => '\"' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:147:16: {...}? => '\"'
            {
            if ( !((  isInTag() || isInAposAttr() )) ) {
                throw new FailedPredicateException(input, "Quot", "  isInTag() || isInAposAttr() ");
            }
            match('\"'); 
             negateInQuotAttr(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Quot"

    // $ANTLR start "Apos"
    public final void mApos() throws RecognitionException {
        try {
            int _type = Apos;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:148:14: ({...}? => '\\'' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:148:16: {...}? => '\\''
            {
            if ( !((  isInTag() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "Apos", "  isInTag() || isInQuotAttr() ");
            }
            match('\''); 
             negateInAposAttr(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Apos"

    // $ANTLR start "EscapeQuot"
    public final void mEscapeQuot() throws RecognitionException {
        try {
            int _type = EscapeQuot;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:149:14: ({...}? => '\"\"' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:149:16: {...}? => '\"\"'
            {
            if ( !(( isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "EscapeQuot", " isInQuotAttr() ");
            }
            match("\"\""); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EscapeQuot"

    // $ANTLR start "EscapeApos"
    public final void mEscapeApos() throws RecognitionException {
        try {
            int _type = EscapeApos;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:150:14: ({...}? => '\\'\\'' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:150:16: {...}? => '\\'\\''
            {
            if ( !(( isInAposAttr() )) ) {
                throw new FailedPredicateException(input, "EscapeApos", " isInAposAttr() ");
            }
            match("''"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EscapeApos"

    // $ANTLR start "EscapeLCurly"
    public final void mEscapeLCurly() throws RecognitionException {
        try {
            int _type = EscapeLCurly;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:151:14: ({...}? => '{{' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:151:16: {...}? => '{{'
            {
            if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "EscapeLCurly", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
            }
            match("{{"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EscapeLCurly"

    // $ANTLR start "EscapeRCurly"
    public final void mEscapeRCurly() throws RecognitionException {
        try {
            int _type = EscapeRCurly;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:152:14: ({...}? => '}}' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:152:16: {...}? => '}}'
            {
            if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "EscapeRCurly", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
            }
            match("}}"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EscapeRCurly"

    // $ANTLR start "Colon"
    public final void mColon() throws RecognitionException {
        try {
            int _type = Colon;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:153:14: ( ':' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:153:16: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Colon"

    // $ANTLR start "DirCommentConstructor"
    public final void mDirCommentConstructor() throws RecognitionException {
        try {
            int _type = DirCommentConstructor;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:156:5: ({...}? => '<!--' ( options {greedy=false; } : . )* '-->' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:156:7: {...}? => '<!--' ( options {greedy=false; } : . )* '-->'
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "DirCommentConstructor", " !isInTag() ");
            }
            match("<!--"); 

            // org/brackit/xquery/compiler/parser/XMLexer.g:157:14: ( options {greedy=false; } : . )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='-') ) {
                    int LA1_1 = input.LA(2);

                    if ( (LA1_1=='-') ) {
                        int LA1_3 = input.LA(3);

                        if ( (LA1_3=='>') ) {
                            alt1=2;
                        }
                        else if ( ((LA1_3>='\u0000' && LA1_3<='=')||(LA1_3>='?' && LA1_3<='\uFFFF')) ) {
                            alt1=1;
                        }


                    }
                    else if ( ((LA1_1>='\u0000' && LA1_1<=',')||(LA1_1>='.' && LA1_1<='\uFFFF')) ) {
                        alt1=1;
                    }


                }
                else if ( ((LA1_0>='\u0000' && LA1_0<=',')||(LA1_0>='.' && LA1_0<='\uFFFF')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:157:41: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);

            match("-->"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DirCommentConstructor"

    // $ANTLR start "DirPIConstructor"
    public final void mDirPIConstructor() throws RecognitionException {
        try {
            int _type = DirPIConstructor;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:160:5: ({...}? => '<?' ( VS )? VNCName ( VS ( options {greedy=false; } : . )* )? '?>' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:160:7: {...}? => '<?' ( VS )? VNCName ( VS ( options {greedy=false; } : . )* )? '?>'
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "DirPIConstructor", " !isInTag() ");
            }
            match("<?"); 

            // org/brackit/xquery/compiler/parser/XMLexer.g:161:12: ( VS )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( ((LA2_0>='\t' && LA2_0<='\n')||LA2_0=='\r'||LA2_0==' ') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:161:12: VS
                    {
                    mVS(); 

                    }
                    break;

            }

            mVNCName(); 
            // org/brackit/xquery/compiler/parser/XMLexer.g:161:24: ( VS ( options {greedy=false; } : . )* )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( ((LA4_0>='\t' && LA4_0<='\n')||LA4_0=='\r'||LA4_0==' ') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:161:25: VS ( options {greedy=false; } : . )*
                    {
                    mVS(); 
                    // org/brackit/xquery/compiler/parser/XMLexer.g:161:28: ( options {greedy=false; } : . )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( (LA3_0=='?') ) {
                            int LA3_1 = input.LA(2);

                            if ( (LA3_1=='>') ) {
                                alt3=2;
                            }
                            else if ( ((LA3_1>='\u0000' && LA3_1<='=')||(LA3_1>='?' && LA3_1<='\uFFFF')) ) {
                                alt3=1;
                            }


                        }
                        else if ( ((LA3_0>='\u0000' && LA3_0<='>')||(LA3_0>='@' && LA3_0<='\uFFFF')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // org/brackit/xquery/compiler/parser/XMLexer.g:161:55: .
                    	    {
                    	    matchAny(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    }
                    break;

            }

            match("?>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DirPIConstructor"

    // $ANTLR start "CDataSection"
    public final void mCDataSection() throws RecognitionException {
        try {
            int _type = CDataSection;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:164:5: ({...}? => '<![CDATA[' ( options {greedy=false; } : . )* ']]>' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:164:7: {...}? => '<![CDATA[' ( options {greedy=false; } : . )* ']]>'
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "CDataSection", " !isInTag() ");
            }
            match("<![CDATA["); 

            // org/brackit/xquery/compiler/parser/XMLexer.g:165:19: ( options {greedy=false; } : . )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==']') ) {
                    int LA5_1 = input.LA(2);

                    if ( (LA5_1==']') ) {
                        int LA5_3 = input.LA(3);

                        if ( (LA5_3=='>') ) {
                            alt5=2;
                        }
                        else if ( ((LA5_3>='\u0000' && LA5_3<='=')||(LA5_3>='?' && LA5_3<='\uFFFF')) ) {
                            alt5=1;
                        }


                    }
                    else if ( ((LA5_1>='\u0000' && LA5_1<='\\')||(LA5_1>='^' && LA5_1<='\uFFFF')) ) {
                        alt5=1;
                    }


                }
                else if ( ((LA5_0>='\u0000' && LA5_0<='\\')||(LA5_0>='^' && LA5_0<='\uFFFF')) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:165:46: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);

            match("]]>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CDataSection"

    // $ANTLR start "ElementContentChar"
    public final void mElementContentChar() throws RecognitionException {
        try {
            int _type = ElementContentChar;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:168:5: ({...}? => ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0027' .. '\\u003B' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:168:7: {...}? => ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0027' .. '\\u003B' )+
            {
            if ( !(( !isInTag() )) ) {
                throw new FailedPredicateException(input, "ElementContentChar", " !isInTag() ");
            }
            // org/brackit/xquery/compiler/parser/XMLexer.g:169:7: ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0027' .. '\\u003B' )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='\t' && LA6_0<='\n')||LA6_0=='\r'||(LA6_0>=' ' && LA6_0<='%')||(LA6_0>='\'' && LA6_0<=';')||(LA6_0>='=' && LA6_0<='z')||LA6_0=='|'||(LA6_0>='~' && LA6_0<='\uD7FF')||(LA6_0>='\uE000' && LA6_0<='\uFFFD')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||(input.LA(1)>=' ' && input.LA(1)<='%')||(input.LA(1)>='\'' && input.LA(1)<=';')||(input.LA(1)>='=' && input.LA(1)<='z')||input.LA(1)=='|'||(input.LA(1)>='~' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFD') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ElementContentChar"

    // $ANTLR start "QuotAttrContentChar"
    public final void mQuotAttrContentChar() throws RecognitionException {
        try {
            int _type = QuotAttrContentChar;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:175:5: ({...}? => ( CommonContentChar | '\\u0020' .. '\\u0021' | '\\u0023' .. '\\u0025' | '\\u0027' .. '\\u003B' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:175:7: {...}? => ( CommonContentChar | '\\u0020' .. '\\u0021' | '\\u0023' .. '\\u0025' | '\\u0027' .. '\\u003B' )+
            {
            if ( !(( isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "QuotAttrContentChar", " isInQuotAttr() ");
            }
            // org/brackit/xquery/compiler/parser/XMLexer.g:176:7: ( CommonContentChar | '\\u0020' .. '\\u0021' | '\\u0023' .. '\\u0025' | '\\u0027' .. '\\u003B' )+
            int cnt7=0;
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>='\t' && LA7_0<='\n')||LA7_0=='\r'||(LA7_0>=' ' && LA7_0<='!')||(LA7_0>='#' && LA7_0<='%')||(LA7_0>='\'' && LA7_0<=';')||(LA7_0>='=' && LA7_0<='z')||LA7_0=='|'||(LA7_0>='~' && LA7_0<='\uD7FF')||(LA7_0>='\uE000' && LA7_0<='\uFFFD')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||(input.LA(1)>=' ' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='%')||(input.LA(1)>='\'' && input.LA(1)<=';')||(input.LA(1)>='=' && input.LA(1)<='z')||input.LA(1)=='|'||(input.LA(1)>='~' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFD') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt7 >= 1 ) break loop7;
                        EarlyExitException eee =
                            new EarlyExitException(7, input);
                        throw eee;
                }
                cnt7++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "QuotAttrContentChar"

    // $ANTLR start "AposAttrContentChar"
    public final void mAposAttrContentChar() throws RecognitionException {
        try {
            int _type = AposAttrContentChar;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:182:5: ({...}? => ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0028' .. '\\u003B' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:182:7: {...}? => ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0028' .. '\\u003B' )+
            {
            if ( !(( isInAposAttr() )) ) {
                throw new FailedPredicateException(input, "AposAttrContentChar", " isInAposAttr() ");
            }
            // org/brackit/xquery/compiler/parser/XMLexer.g:183:7: ( CommonContentChar | '\\u0020' .. '\\u0025' | '\\u0028' .. '\\u003B' )+
            int cnt8=0;
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( ((LA8_0>='\t' && LA8_0<='\n')||LA8_0=='\r'||(LA8_0>=' ' && LA8_0<='%')||(LA8_0>='(' && LA8_0<=';')||(LA8_0>='=' && LA8_0<='z')||LA8_0=='|'||(LA8_0>='~' && LA8_0<='\uD7FF')||(LA8_0>='\uE000' && LA8_0<='\uFFFD')) ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||(input.LA(1)>=' ' && input.LA(1)<='%')||(input.LA(1)>='(' && input.LA(1)<=';')||(input.LA(1)>='=' && input.LA(1)<='z')||input.LA(1)=='|'||(input.LA(1)>='~' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFD') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt8 >= 1 ) break loop8;
                        EarlyExitException eee =
                            new EarlyExitException(8, input);
                        throw eee;
                }
                cnt8++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AposAttrContentChar"

    // $ANTLR start "PredefinedEntityRef"
    public final void mPredefinedEntityRef() throws RecognitionException {
        try {
            int _type = PredefinedEntityRef;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:189:5: ({...}? => '&' ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' ) ';' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:189:7: {...}? => '&' ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' ) ';'
            {
            if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                throw new FailedPredicateException(input, "PredefinedEntityRef", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
            }
            match('&'); 
            // org/brackit/xquery/compiler/parser/XMLexer.g:190:11: ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' )
            int alt9=5;
            switch ( input.LA(1) ) {
            case 'l':
                {
                alt9=1;
                }
                break;
            case 'g':
                {
                alt9=2;
                }
                break;
            case 'a':
                {
                int LA9_3 = input.LA(2);

                if ( (LA9_3=='p') ) {
                    alt9=3;
                }
                else if ( (LA9_3=='m') ) {
                    alt9=5;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 3, input);

                    throw nvae;
                }
                }
                break;
            case 'q':
                {
                alt9=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }

            switch (alt9) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:190:12: 'lt'
                    {
                    match("lt"); 


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:190:19: 'gt'
                    {
                    match("gt"); 


                    }
                    break;
                case 3 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:190:26: 'apos'
                    {
                    match("apos"); 


                    }
                    break;
                case 4 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:190:35: 'quot'
                    {
                    match("quot"); 


                    }
                    break;
                case 5 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:190:44: 'amp'
                    {
                    match("amp"); 


                    }
                    break;

            }

            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PredefinedEntityRef"

    // $ANTLR start "CharRef"
    public final void mCharRef() throws RecognitionException {
        try {
            int _type = CharRef;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:193:5: ({...}? => '&#' Digits ';' | {...}? => '&#x' HexDigits ';' )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0=='&') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {
                int LA10_1 = input.LA(2);

                if ( (LA10_1=='#') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {
                    int LA10_2 = input.LA(3);

                    if ( (LA10_2=='x') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {
                        alt10=2;
                    }
                    else if ( ((LA10_2>='0' && LA10_2<='9')) && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {
                        alt10=1;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 10, 2, input);

                        throw nvae;
                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 10, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:193:7: {...}? => '&#' Digits ';'
                    {
                    if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                        throw new FailedPredicateException(input, "CharRef", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
                    }
                    match("&#"); 

                    mDigits(); 
                    match(';'); 
                    checkCharRef();

                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XMLexer.g:195:7: {...}? => '&#x' HexDigits ';'
                    {
                    if ( !(( !isInTag() || isInAposAttr() || isInQuotAttr() )) ) {
                        throw new FailedPredicateException(input, "CharRef", " !isInTag() || isInAposAttr() || isInQuotAttr() ");
                    }
                    match("&#x"); 

                    mHexDigits(); 
                    match(';'); 
                    checkCharRef();

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CharRef"

    // $ANTLR start "CommonContentChar"
    public final void mCommonContentChar() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:201:5: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u003D' .. '\\u007A' | '\\u007C' .. '\\u007C' | '\\u007E' .. '\\uD7FF' | '\\uE000' .. '\\uFFFD' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||(input.LA(1)>='=' && input.LA(1)<='z')||input.LA(1)=='|'||(input.LA(1)>='~' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFD') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "CommonContentChar"

    // $ANTLR start "Digits"
    public final void mDigits() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:207:5: ( ( '0' .. '9' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:207:7: ( '0' .. '9' )+
            {
            // org/brackit/xquery/compiler/parser/XMLexer.g:207:7: ( '0' .. '9' )+
            int cnt11=0;
            loop11:
            do {
                int alt11=2;
                int LA11_0 = input.LA(1);

                if ( ((LA11_0>='0' && LA11_0<='9')) ) {
                    alt11=1;
                }


                switch (alt11) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:207:7: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt11 >= 1 ) break loop11;
                        EarlyExitException eee =
                            new EarlyExitException(11, input);
                        throw eee;
                }
                cnt11++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "Digits"

    // $ANTLR start "HexDigits"
    public final void mHexDigits() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:211:5: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:211:7: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
            {
            // org/brackit/xquery/compiler/parser/XMLexer.g:211:7: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
            int cnt12=0;
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( ((LA12_0>='0' && LA12_0<='9')||(LA12_0>='A' && LA12_0<='F')||(LA12_0>='a' && LA12_0<='f')) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt12 >= 1 ) break loop12;
                        EarlyExitException eee =
                            new EarlyExitException(12, input);
                        throw eee;
                }
                cnt12++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "HexDigits"

    // $ANTLR start "S"
    public final void mS() throws RecognitionException {
        try {
            int _type = S;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:214:5: ({...}? => ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:214:7: {...}? => ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            {
            if ( !(( isInTag() )) ) {
                throw new FailedPredicateException(input, "S", " isInTag() ");
            }
            // org/brackit/xquery/compiler/parser/XMLexer.g:214:25: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            int cnt13=0;
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( ((LA13_0>='\t' && LA13_0<='\n')||LA13_0=='\r'||LA13_0==' ') ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt13 >= 1 ) break loop13;
                        EarlyExitException eee =
                            new EarlyExitException(13, input);
                        throw eee;
                }
                cnt13++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "S"

    // $ANTLR start "VS"
    public final void mVS() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:218:5: ( ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+ )
            // org/brackit/xquery/compiler/parser/XMLexer.g:218:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            {
            // org/brackit/xquery/compiler/parser/XMLexer.g:218:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            int cnt14=0;
            loop14:
            do {
                int alt14=2;
                int LA14_0 = input.LA(1);

                if ( ((LA14_0>='\t' && LA14_0<='\n')||LA14_0=='\r'||LA14_0==' ') ) {
                    alt14=1;
                }


                switch (alt14) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt14 >= 1 ) break loop14;
                        EarlyExitException eee =
                            new EarlyExitException(14, input);
                        throw eee;
                }
                cnt14++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "VS"

    // $ANTLR start "NCName"
    public final void mNCName() throws RecognitionException {
        try {
            int _type = NCName;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XMLexer.g:221:5: ({...}? => VNCName )
            // org/brackit/xquery/compiler/parser/XMLexer.g:221:7: {...}? => VNCName
            {
            if ( !(( isInTag() )) ) {
                throw new FailedPredicateException(input, "NCName", " isInTag() ");
            }
            mVNCName(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NCName"

    // $ANTLR start "VNCName"
    public final void mVNCName() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:225:5: ( NCNameStartChar ( NCNameChar )* )
            // org/brackit/xquery/compiler/parser/XMLexer.g:225:7: NCNameStartChar ( NCNameChar )*
            {
            mNCNameStartChar(); 
            // org/brackit/xquery/compiler/parser/XMLexer.g:225:23: ( NCNameChar )*
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( ((LA15_0>='-' && LA15_0<='.')||(LA15_0>='0' && LA15_0<='9')||(LA15_0>='A' && LA15_0<='Z')||LA15_0=='_'||(LA15_0>='a' && LA15_0<='z')||LA15_0=='\u00B7'||(LA15_0>='\u00C0' && LA15_0<='\u00D6')||(LA15_0>='\u00D8' && LA15_0<='\u00F6')||(LA15_0>='\u00F8' && LA15_0<='\u037D')||(LA15_0>='\u037F' && LA15_0<='\u1FFF')||(LA15_0>='\u200C' && LA15_0<='\u200D')||(LA15_0>='\u203F' && LA15_0<='\u2040')||(LA15_0>='\u2070' && LA15_0<='\u218F')||(LA15_0>='\u2C00' && LA15_0<='\u2FEF')||(LA15_0>='\u3001' && LA15_0<='\uD7FF')||(LA15_0>='\uF900' && LA15_0<='\uFDCF')||(LA15_0>='\uFDF0' && LA15_0<='\uFFFD')) ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XMLexer.g:225:23: NCNameChar
            	    {
            	    mNCNameChar(); 

            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "VNCName"

    // $ANTLR start "NCNameStartChar"
    public final void mNCNameStartChar() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:229:5: ( Letter | '_' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u00C0' && input.LA(1)<='\u00D6')||(input.LA(1)>='\u00D8' && input.LA(1)<='\u00F6')||(input.LA(1)>='\u00F8' && input.LA(1)<='\u0131')||(input.LA(1)>='\u0134' && input.LA(1)<='\u013E')||(input.LA(1)>='\u0141' && input.LA(1)<='\u0148')||(input.LA(1)>='\u014A' && input.LA(1)<='\u017E')||(input.LA(1)>='\u0180' && input.LA(1)<='\u01C3')||(input.LA(1)>='\u01CD' && input.LA(1)<='\u01F0')||(input.LA(1)>='\u01F4' && input.LA(1)<='\u01F5')||(input.LA(1)>='\u01FA' && input.LA(1)<='\u0217')||(input.LA(1)>='\u0250' && input.LA(1)<='\u02A8')||(input.LA(1)>='\u02BB' && input.LA(1)<='\u02C1')||input.LA(1)=='\u0386'||(input.LA(1)>='\u0388' && input.LA(1)<='\u038A')||input.LA(1)=='\u038C'||(input.LA(1)>='\u038E' && input.LA(1)<='\u03A1')||(input.LA(1)>='\u03A3' && input.LA(1)<='\u03CE')||(input.LA(1)>='\u03D0' && input.LA(1)<='\u03D6')||input.LA(1)=='\u03DA'||input.LA(1)=='\u03DC'||input.LA(1)=='\u03DE'||input.LA(1)=='\u03E0'||(input.LA(1)>='\u03E2' && input.LA(1)<='\u03F3')||(input.LA(1)>='\u0401' && input.LA(1)<='\u040C')||(input.LA(1)>='\u040E' && input.LA(1)<='\u044F')||(input.LA(1)>='\u0451' && input.LA(1)<='\u045C')||(input.LA(1)>='\u045E' && input.LA(1)<='\u0481')||(input.LA(1)>='\u0490' && input.LA(1)<='\u04C4')||(input.LA(1)>='\u04C7' && input.LA(1)<='\u04C8')||(input.LA(1)>='\u04CB' && input.LA(1)<='\u04CC')||(input.LA(1)>='\u04D0' && input.LA(1)<='\u04EB')||(input.LA(1)>='\u04EE' && input.LA(1)<='\u04F5')||(input.LA(1)>='\u04F8' && input.LA(1)<='\u04F9')||(input.LA(1)>='\u0531' && input.LA(1)<='\u0556')||input.LA(1)=='\u0559'||(input.LA(1)>='\u0561' && input.LA(1)<='\u0586')||(input.LA(1)>='\u05D0' && input.LA(1)<='\u05EA')||(input.LA(1)>='\u05F0' && input.LA(1)<='\u05F2')||(input.LA(1)>='\u0621' && input.LA(1)<='\u063A')||(input.LA(1)>='\u0641' && input.LA(1)<='\u064A')||(input.LA(1)>='\u0671' && input.LA(1)<='\u06B7')||(input.LA(1)>='\u06BA' && input.LA(1)<='\u06BE')||(input.LA(1)>='\u06C0' && input.LA(1)<='\u06CE')||(input.LA(1)>='\u06D0' && input.LA(1)<='\u06D3')||input.LA(1)=='\u06D5'||(input.LA(1)>='\u06E5' && input.LA(1)<='\u06E6')||(input.LA(1)>='\u0905' && input.LA(1)<='\u0939')||input.LA(1)=='\u093D'||(input.LA(1)>='\u0958' && input.LA(1)<='\u0961')||(input.LA(1)>='\u0985' && input.LA(1)<='\u098C')||(input.LA(1)>='\u098F' && input.LA(1)<='\u0990')||(input.LA(1)>='\u0993' && input.LA(1)<='\u09A8')||(input.LA(1)>='\u09AA' && input.LA(1)<='\u09B0')||input.LA(1)=='\u09B2'||(input.LA(1)>='\u09B6' && input.LA(1)<='\u09B9')||(input.LA(1)>='\u09DC' && input.LA(1)<='\u09DD')||(input.LA(1)>='\u09DF' && input.LA(1)<='\u09E1')||(input.LA(1)>='\u09F0' && input.LA(1)<='\u09F1')||(input.LA(1)>='\u0A05' && input.LA(1)<='\u0A0A')||(input.LA(1)>='\u0A0F' && input.LA(1)<='\u0A10')||(input.LA(1)>='\u0A13' && input.LA(1)<='\u0A28')||(input.LA(1)>='\u0A2A' && input.LA(1)<='\u0A30')||(input.LA(1)>='\u0A32' && input.LA(1)<='\u0A33')||(input.LA(1)>='\u0A35' && input.LA(1)<='\u0A36')||(input.LA(1)>='\u0A38' && input.LA(1)<='\u0A39')||(input.LA(1)>='\u0A59' && input.LA(1)<='\u0A5C')||input.LA(1)=='\u0A5E'||(input.LA(1)>='\u0A72' && input.LA(1)<='\u0A74')||(input.LA(1)>='\u0A85' && input.LA(1)<='\u0A8B')||input.LA(1)=='\u0A8D'||(input.LA(1)>='\u0A8F' && input.LA(1)<='\u0A91')||(input.LA(1)>='\u0A93' && input.LA(1)<='\u0AA8')||(input.LA(1)>='\u0AAA' && input.LA(1)<='\u0AB0')||(input.LA(1)>='\u0AB2' && input.LA(1)<='\u0AB3')||(input.LA(1)>='\u0AB5' && input.LA(1)<='\u0AB9')||input.LA(1)=='\u0ABD'||input.LA(1)=='\u0AE0'||(input.LA(1)>='\u0B05' && input.LA(1)<='\u0B0C')||(input.LA(1)>='\u0B0F' && input.LA(1)<='\u0B10')||(input.LA(1)>='\u0B13' && input.LA(1)<='\u0B28')||(input.LA(1)>='\u0B2A' && input.LA(1)<='\u0B30')||(input.LA(1)>='\u0B32' && input.LA(1)<='\u0B33')||(input.LA(1)>='\u0B36' && input.LA(1)<='\u0B39')||input.LA(1)=='\u0B3D'||(input.LA(1)>='\u0B5C' && input.LA(1)<='\u0B5D')||(input.LA(1)>='\u0B5F' && input.LA(1)<='\u0B61')||(input.LA(1)>='\u0B85' && input.LA(1)<='\u0B8A')||(input.LA(1)>='\u0B8E' && input.LA(1)<='\u0B90')||(input.LA(1)>='\u0B92' && input.LA(1)<='\u0B95')||(input.LA(1)>='\u0B99' && input.LA(1)<='\u0B9A')||input.LA(1)=='\u0B9C'||(input.LA(1)>='\u0B9E' && input.LA(1)<='\u0B9F')||(input.LA(1)>='\u0BA3' && input.LA(1)<='\u0BA4')||(input.LA(1)>='\u0BA8' && input.LA(1)<='\u0BAA')||(input.LA(1)>='\u0BAE' && input.LA(1)<='\u0BB5')||(input.LA(1)>='\u0BB7' && input.LA(1)<='\u0BB9')||(input.LA(1)>='\u0C05' && input.LA(1)<='\u0C0C')||(input.LA(1)>='\u0C0E' && input.LA(1)<='\u0C10')||(input.LA(1)>='\u0C12' && input.LA(1)<='\u0C28')||(input.LA(1)>='\u0C2A' && input.LA(1)<='\u0C33')||(input.LA(1)>='\u0C35' && input.LA(1)<='\u0C39')||(input.LA(1)>='\u0C60' && input.LA(1)<='\u0C61')||(input.LA(1)>='\u0C85' && input.LA(1)<='\u0C8C')||(input.LA(1)>='\u0C8E' && input.LA(1)<='\u0C90')||(input.LA(1)>='\u0C92' && input.LA(1)<='\u0CA8')||(input.LA(1)>='\u0CAA' && input.LA(1)<='\u0CB3')||(input.LA(1)>='\u0CB5' && input.LA(1)<='\u0CB9')||input.LA(1)=='\u0CDE'||(input.LA(1)>='\u0CE0' && input.LA(1)<='\u0CE1')||(input.LA(1)>='\u0D05' && input.LA(1)<='\u0D0C')||(input.LA(1)>='\u0D0E' && input.LA(1)<='\u0D10')||(input.LA(1)>='\u0D12' && input.LA(1)<='\u0D28')||(input.LA(1)>='\u0D2A' && input.LA(1)<='\u0D39')||(input.LA(1)>='\u0D60' && input.LA(1)<='\u0D61')||(input.LA(1)>='\u0E01' && input.LA(1)<='\u0E2E')||input.LA(1)=='\u0E30'||(input.LA(1)>='\u0E32' && input.LA(1)<='\u0E33')||(input.LA(1)>='\u0E40' && input.LA(1)<='\u0E45')||(input.LA(1)>='\u0E81' && input.LA(1)<='\u0E82')||input.LA(1)=='\u0E84'||(input.LA(1)>='\u0E87' && input.LA(1)<='\u0E88')||input.LA(1)=='\u0E8A'||input.LA(1)=='\u0E8D'||(input.LA(1)>='\u0E94' && input.LA(1)<='\u0E97')||(input.LA(1)>='\u0E99' && input.LA(1)<='\u0E9F')||(input.LA(1)>='\u0EA1' && input.LA(1)<='\u0EA3')||input.LA(1)=='\u0EA5'||input.LA(1)=='\u0EA7'||(input.LA(1)>='\u0EAA' && input.LA(1)<='\u0EAB')||(input.LA(1)>='\u0EAD' && input.LA(1)<='\u0EAE')||input.LA(1)=='\u0EB0'||(input.LA(1)>='\u0EB2' && input.LA(1)<='\u0EB3')||input.LA(1)=='\u0EBD'||(input.LA(1)>='\u0EC0' && input.LA(1)<='\u0EC4')||(input.LA(1)>='\u0F40' && input.LA(1)<='\u0F47')||(input.LA(1)>='\u0F49' && input.LA(1)<='\u0F69')||(input.LA(1)>='\u10A0' && input.LA(1)<='\u10C5')||(input.LA(1)>='\u10D0' && input.LA(1)<='\u10F6')||input.LA(1)=='\u1100'||(input.LA(1)>='\u1102' && input.LA(1)<='\u1103')||(input.LA(1)>='\u1105' && input.LA(1)<='\u1107')||input.LA(1)=='\u1109'||(input.LA(1)>='\u110B' && input.LA(1)<='\u110C')||(input.LA(1)>='\u110E' && input.LA(1)<='\u1112')||input.LA(1)=='\u113C'||input.LA(1)=='\u113E'||input.LA(1)=='\u1140'||input.LA(1)=='\u114C'||input.LA(1)=='\u114E'||input.LA(1)=='\u1150'||(input.LA(1)>='\u1154' && input.LA(1)<='\u1155')||input.LA(1)=='\u1159'||(input.LA(1)>='\u115F' && input.LA(1)<='\u1161')||input.LA(1)=='\u1163'||input.LA(1)=='\u1165'||input.LA(1)=='\u1167'||input.LA(1)=='\u1169'||(input.LA(1)>='\u116D' && input.LA(1)<='\u116E')||(input.LA(1)>='\u1172' && input.LA(1)<='\u1173')||input.LA(1)=='\u1175'||input.LA(1)=='\u119E'||input.LA(1)=='\u11A8'||input.LA(1)=='\u11AB'||(input.LA(1)>='\u11AE' && input.LA(1)<='\u11AF')||(input.LA(1)>='\u11B7' && input.LA(1)<='\u11B8')||input.LA(1)=='\u11BA'||(input.LA(1)>='\u11BC' && input.LA(1)<='\u11C2')||input.LA(1)=='\u11EB'||input.LA(1)=='\u11F0'||input.LA(1)=='\u11F9'||(input.LA(1)>='\u1E00' && input.LA(1)<='\u1E9B')||(input.LA(1)>='\u1EA0' && input.LA(1)<='\u1EF9')||(input.LA(1)>='\u1F00' && input.LA(1)<='\u1F15')||(input.LA(1)>='\u1F18' && input.LA(1)<='\u1F1D')||(input.LA(1)>='\u1F20' && input.LA(1)<='\u1F45')||(input.LA(1)>='\u1F48' && input.LA(1)<='\u1F4D')||(input.LA(1)>='\u1F50' && input.LA(1)<='\u1F57')||input.LA(1)=='\u1F59'||input.LA(1)=='\u1F5B'||input.LA(1)=='\u1F5D'||(input.LA(1)>='\u1F5F' && input.LA(1)<='\u1F7D')||(input.LA(1)>='\u1F80' && input.LA(1)<='\u1FB4')||(input.LA(1)>='\u1FB6' && input.LA(1)<='\u1FBC')||input.LA(1)=='\u1FBE'||(input.LA(1)>='\u1FC2' && input.LA(1)<='\u1FC4')||(input.LA(1)>='\u1FC6' && input.LA(1)<='\u1FCC')||(input.LA(1)>='\u1FD0' && input.LA(1)<='\u1FD3')||(input.LA(1)>='\u1FD6' && input.LA(1)<='\u1FDB')||(input.LA(1)>='\u1FE0' && input.LA(1)<='\u1FEC')||(input.LA(1)>='\u1FF2' && input.LA(1)<='\u1FF4')||(input.LA(1)>='\u1FF6' && input.LA(1)<='\u1FFC')||input.LA(1)=='\u2126'||(input.LA(1)>='\u212A' && input.LA(1)<='\u212B')||input.LA(1)=='\u212E'||(input.LA(1)>='\u2180' && input.LA(1)<='\u2182')||input.LA(1)=='\u3007'||(input.LA(1)>='\u3021' && input.LA(1)<='\u3029')||(input.LA(1)>='\u3041' && input.LA(1)<='\u3094')||(input.LA(1)>='\u30A1' && input.LA(1)<='\u30FA')||(input.LA(1)>='\u3105' && input.LA(1)<='\u312C')||(input.LA(1)>='\u4E00' && input.LA(1)<='\u9FA5')||(input.LA(1)>='\uAC00' && input.LA(1)<='\uD7A3') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "NCNameStartChar"

    // $ANTLR start "NCNameChar"
    public final void mNCNameChar() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:234:5: ( 'A' .. 'Z' | 'a' .. 'z' | '_' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u02FF' | '\\u0370' .. '\\u037D' | '\\u037F' .. '\\u1FFF' | '\\u200C' .. '\\u200D' | '\\u2070' .. '\\u218F' | '\\u2C00' .. '\\u2FEF' | '\\u3001' .. '\\uD7FF' | '\\uF900' .. '\\uFDCF' | '\\uFDF0' .. '\\uFFFD' | '-' | '.' | '0' .. '9' | '\\u00B7' | '\\u0300' .. '\\u036F' | '\\u203F' .. '\\u2040' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:
            {
            if ( (input.LA(1)>='-' && input.LA(1)<='.')||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z')||input.LA(1)=='\u00B7'||(input.LA(1)>='\u00C0' && input.LA(1)<='\u00D6')||(input.LA(1)>='\u00D8' && input.LA(1)<='\u00F6')||(input.LA(1)>='\u00F8' && input.LA(1)<='\u037D')||(input.LA(1)>='\u037F' && input.LA(1)<='\u1FFF')||(input.LA(1)>='\u200C' && input.LA(1)<='\u200D')||(input.LA(1)>='\u203F' && input.LA(1)<='\u2040')||(input.LA(1)>='\u2070' && input.LA(1)<='\u218F')||(input.LA(1)>='\u2C00' && input.LA(1)<='\u2FEF')||(input.LA(1)>='\u3001' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uF900' && input.LA(1)<='\uFDCF')||(input.LA(1)>='\uFDF0' && input.LA(1)<='\uFFFD') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "NCNameChar"

    // $ANTLR start "Letter"
    public final void mLetter() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XMLexer.g:246:5: ( '\\u0041' .. '\\u005A' | '\\u0061' .. '\\u007A' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u00FF' | '\\u0100' .. '\\u0131' | '\\u0134' .. '\\u013E' | '\\u0141' .. '\\u0148' | '\\u014A' .. '\\u017E' | '\\u0180' .. '\\u01C3' | '\\u01CD' .. '\\u01F0' | '\\u01F4' .. '\\u01F5' | '\\u01FA' .. '\\u0217' | '\\u0250' .. '\\u02A8' | '\\u02BB' .. '\\u02C1' | '\\u0386' | '\\u0388' .. '\\u038A' | '\\u038C' | '\\u038E' .. '\\u03A1' | '\\u03A3' .. '\\u03CE' | '\\u03D0' .. '\\u03D6' | '\\u03DA' | '\\u03DC' | '\\u03DE' | '\\u03E0' | '\\u03E2' .. '\\u03F3' | '\\u0401' .. '\\u040C' | '\\u040E' .. '\\u044F' | '\\u0451' .. '\\u045C' | '\\u045E' .. '\\u0481' | '\\u0490' .. '\\u04C4' | '\\u04C7' .. '\\u04C8' | '\\u04CB' .. '\\u04CC' | '\\u04D0' .. '\\u04EB' | '\\u04EE' .. '\\u04F5' | '\\u04F8' .. '\\u04F9' | '\\u0531' .. '\\u0556' | '\\u0559' | '\\u0561' .. '\\u0586' | '\\u05D0' .. '\\u05EA' | '\\u05F0' .. '\\u05F2' | '\\u0621' .. '\\u063A' | '\\u0641' .. '\\u064A' | '\\u0671' .. '\\u06B7' | '\\u06BA' .. '\\u06BE' | '\\u06C0' .. '\\u06CE' | '\\u06D0' .. '\\u06D3' | '\\u06D5' | '\\u06E5' .. '\\u06E6' | '\\u0905' .. '\\u0939' | '\\u093D' | '\\u0958' .. '\\u0961' | '\\u0985' .. '\\u098C' | '\\u098F' .. '\\u0990' | '\\u0993' .. '\\u09A8' | '\\u09AA' .. '\\u09B0' | '\\u09B2' | '\\u09B6' .. '\\u09B9' | '\\u09DC' .. '\\u09DD' | '\\u09DF' .. '\\u09E1' | '\\u09F0' .. '\\u09F1' | '\\u0A05' .. '\\u0A0A' | '\\u0A0F' .. '\\u0A10' | '\\u0A13' .. '\\u0A28' | '\\u0A2A' .. '\\u0A30' | '\\u0A32' .. '\\u0A33' | '\\u0A35' .. '\\u0A36' | '\\u0A38' .. '\\u0A39' | '\\u0A59' .. '\\u0A5C' | '\\u0A5E' | '\\u0A72' .. '\\u0A74' | '\\u0A85' .. '\\u0A8B' | '\\u0A8D' | '\\u0A8F' .. '\\u0A91' | '\\u0A93' .. '\\u0AA8' | '\\u0AAA' .. '\\u0AB0' | '\\u0AB2' .. '\\u0AB3' | '\\u0AB5' .. '\\u0AB9' | '\\u0ABD' | '\\u0AE0' | '\\u0B05' .. '\\u0B0C' | '\\u0B0F' .. '\\u0B10' | '\\u0B13' .. '\\u0B28' | '\\u0B2A' .. '\\u0B30' | '\\u0B32' .. '\\u0B33' | '\\u0B36' .. '\\u0B39' | '\\u0B3D' | '\\u0B5C' .. '\\u0B5D' | '\\u0B5F' .. '\\u0B61' | '\\u0B85' .. '\\u0B8A' | '\\u0B8E' .. '\\u0B90' | '\\u0B92' .. '\\u0B95' | '\\u0B99' .. '\\u0B9A' | '\\u0B9C' | '\\u0B9E' .. '\\u0B9F' | '\\u0BA3' .. '\\u0BA4' | '\\u0BA8' .. '\\u0BAA' | '\\u0BAE' .. '\\u0BB5' | '\\u0BB7' .. '\\u0BB9' | '\\u0C05' .. '\\u0C0C' | '\\u0C0E' .. '\\u0C10' | '\\u0C12' .. '\\u0C28' | '\\u0C2A' .. '\\u0C33' | '\\u0C35' .. '\\u0C39' | '\\u0C60' .. '\\u0C61' | '\\u0C85' .. '\\u0C8C' | '\\u0C8E' .. '\\u0C90' | '\\u0C92' .. '\\u0CA8' | '\\u0CAA' .. '\\u0CB3' | '\\u0CB5' .. '\\u0CB9' | '\\u0CDE' | '\\u0CE0' .. '\\u0CE1' | '\\u0D05' .. '\\u0D0C' | '\\u0D0E' .. '\\u0D10' | '\\u0D12' .. '\\u0D28' | '\\u0D2A' .. '\\u0D39' | '\\u0D60' .. '\\u0D61' | '\\u0E01' .. '\\u0E2E' | '\\u0E30' | '\\u0E32' .. '\\u0E33' | '\\u0E40' .. '\\u0E45' | '\\u0E81' .. '\\u0E82' | '\\u0E84' | '\\u0E87' .. '\\u0E88' | '\\u0E8A' | '\\u0E8D' | '\\u0E94' .. '\\u0E97' | '\\u0E99' .. '\\u0E9F' | '\\u0EA1' .. '\\u0EA3' | '\\u0EA5' | '\\u0EA7' | '\\u0EAA' .. '\\u0EAB' | '\\u0EAD' .. '\\u0EAE' | '\\u0EB0' | '\\u0EB2' .. '\\u0EB3' | '\\u0EBD' | '\\u0EC0' .. '\\u0EC4' | '\\u0F40' .. '\\u0F47' | '\\u0F49' .. '\\u0F69' | '\\u10A0' .. '\\u10C5' | '\\u10D0' .. '\\u10F6' | '\\u1100' | '\\u1102' .. '\\u1103' | '\\u1105' .. '\\u1107' | '\\u1109' | '\\u110B' .. '\\u110C' | '\\u110E' .. '\\u1112' | '\\u113C' | '\\u113E' | '\\u1140' | '\\u114C' | '\\u114E' | '\\u1150' | '\\u1154' .. '\\u1155' | '\\u1159' | '\\u115F' .. '\\u1161' | '\\u1163' | '\\u1165' | '\\u1167' | '\\u1169' | '\\u116D' .. '\\u116E' | '\\u1172' .. '\\u1173' | '\\u1175' | '\\u119E' | '\\u11A8' | '\\u11AB' | '\\u11AE' .. '\\u11AF' | '\\u11B7' .. '\\u11B8' | '\\u11BA' | '\\u11BC' .. '\\u11C2' | '\\u11EB' | '\\u11F0' | '\\u11F9' | '\\u1E00' .. '\\u1E9B' | '\\u1EA0' .. '\\u1EF9' | '\\u1F00' .. '\\u1F15' | '\\u1F18' .. '\\u1F1D' | '\\u1F20' .. '\\u1F45' | '\\u1F48' .. '\\u1F4D' | '\\u1F50' .. '\\u1F57' | '\\u1F59' | '\\u1F5B' | '\\u1F5D' | '\\u1F5F' .. '\\u1F7D' | '\\u1F80' .. '\\u1FB4' | '\\u1FB6' .. '\\u1FBC' | '\\u1FBE' | '\\u1FC2' .. '\\u1FC4' | '\\u1FC6' .. '\\u1FCC' | '\\u1FD0' .. '\\u1FD3' | '\\u1FD6' .. '\\u1FDB' | '\\u1FE0' .. '\\u1FEC' | '\\u1FF2' .. '\\u1FF4' | '\\u1FF6' .. '\\u1FFC' | '\\u2126' | '\\u212A' .. '\\u212B' | '\\u212E' | '\\u2180' .. '\\u2182' | '\\u3041' .. '\\u3094' | '\\u30A1' .. '\\u30FA' | '\\u3105' .. '\\u312C' | '\\uAC00' .. '\\uD7A3' | '\\u4E00' .. '\\u9FA5' | '\\u3007' | '\\u3021' .. '\\u3029' )
            // org/brackit/xquery/compiler/parser/XMLexer.g:
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z')||(input.LA(1)>='\u00C0' && input.LA(1)<='\u00D6')||(input.LA(1)>='\u00D8' && input.LA(1)<='\u00F6')||(input.LA(1)>='\u00F8' && input.LA(1)<='\u0131')||(input.LA(1)>='\u0134' && input.LA(1)<='\u013E')||(input.LA(1)>='\u0141' && input.LA(1)<='\u0148')||(input.LA(1)>='\u014A' && input.LA(1)<='\u017E')||(input.LA(1)>='\u0180' && input.LA(1)<='\u01C3')||(input.LA(1)>='\u01CD' && input.LA(1)<='\u01F0')||(input.LA(1)>='\u01F4' && input.LA(1)<='\u01F5')||(input.LA(1)>='\u01FA' && input.LA(1)<='\u0217')||(input.LA(1)>='\u0250' && input.LA(1)<='\u02A8')||(input.LA(1)>='\u02BB' && input.LA(1)<='\u02C1')||input.LA(1)=='\u0386'||(input.LA(1)>='\u0388' && input.LA(1)<='\u038A')||input.LA(1)=='\u038C'||(input.LA(1)>='\u038E' && input.LA(1)<='\u03A1')||(input.LA(1)>='\u03A3' && input.LA(1)<='\u03CE')||(input.LA(1)>='\u03D0' && input.LA(1)<='\u03D6')||input.LA(1)=='\u03DA'||input.LA(1)=='\u03DC'||input.LA(1)=='\u03DE'||input.LA(1)=='\u03E0'||(input.LA(1)>='\u03E2' && input.LA(1)<='\u03F3')||(input.LA(1)>='\u0401' && input.LA(1)<='\u040C')||(input.LA(1)>='\u040E' && input.LA(1)<='\u044F')||(input.LA(1)>='\u0451' && input.LA(1)<='\u045C')||(input.LA(1)>='\u045E' && input.LA(1)<='\u0481')||(input.LA(1)>='\u0490' && input.LA(1)<='\u04C4')||(input.LA(1)>='\u04C7' && input.LA(1)<='\u04C8')||(input.LA(1)>='\u04CB' && input.LA(1)<='\u04CC')||(input.LA(1)>='\u04D0' && input.LA(1)<='\u04EB')||(input.LA(1)>='\u04EE' && input.LA(1)<='\u04F5')||(input.LA(1)>='\u04F8' && input.LA(1)<='\u04F9')||(input.LA(1)>='\u0531' && input.LA(1)<='\u0556')||input.LA(1)=='\u0559'||(input.LA(1)>='\u0561' && input.LA(1)<='\u0586')||(input.LA(1)>='\u05D0' && input.LA(1)<='\u05EA')||(input.LA(1)>='\u05F0' && input.LA(1)<='\u05F2')||(input.LA(1)>='\u0621' && input.LA(1)<='\u063A')||(input.LA(1)>='\u0641' && input.LA(1)<='\u064A')||(input.LA(1)>='\u0671' && input.LA(1)<='\u06B7')||(input.LA(1)>='\u06BA' && input.LA(1)<='\u06BE')||(input.LA(1)>='\u06C0' && input.LA(1)<='\u06CE')||(input.LA(1)>='\u06D0' && input.LA(1)<='\u06D3')||input.LA(1)=='\u06D5'||(input.LA(1)>='\u06E5' && input.LA(1)<='\u06E6')||(input.LA(1)>='\u0905' && input.LA(1)<='\u0939')||input.LA(1)=='\u093D'||(input.LA(1)>='\u0958' && input.LA(1)<='\u0961')||(input.LA(1)>='\u0985' && input.LA(1)<='\u098C')||(input.LA(1)>='\u098F' && input.LA(1)<='\u0990')||(input.LA(1)>='\u0993' && input.LA(1)<='\u09A8')||(input.LA(1)>='\u09AA' && input.LA(1)<='\u09B0')||input.LA(1)=='\u09B2'||(input.LA(1)>='\u09B6' && input.LA(1)<='\u09B9')||(input.LA(1)>='\u09DC' && input.LA(1)<='\u09DD')||(input.LA(1)>='\u09DF' && input.LA(1)<='\u09E1')||(input.LA(1)>='\u09F0' && input.LA(1)<='\u09F1')||(input.LA(1)>='\u0A05' && input.LA(1)<='\u0A0A')||(input.LA(1)>='\u0A0F' && input.LA(1)<='\u0A10')||(input.LA(1)>='\u0A13' && input.LA(1)<='\u0A28')||(input.LA(1)>='\u0A2A' && input.LA(1)<='\u0A30')||(input.LA(1)>='\u0A32' && input.LA(1)<='\u0A33')||(input.LA(1)>='\u0A35' && input.LA(1)<='\u0A36')||(input.LA(1)>='\u0A38' && input.LA(1)<='\u0A39')||(input.LA(1)>='\u0A59' && input.LA(1)<='\u0A5C')||input.LA(1)=='\u0A5E'||(input.LA(1)>='\u0A72' && input.LA(1)<='\u0A74')||(input.LA(1)>='\u0A85' && input.LA(1)<='\u0A8B')||input.LA(1)=='\u0A8D'||(input.LA(1)>='\u0A8F' && input.LA(1)<='\u0A91')||(input.LA(1)>='\u0A93' && input.LA(1)<='\u0AA8')||(input.LA(1)>='\u0AAA' && input.LA(1)<='\u0AB0')||(input.LA(1)>='\u0AB2' && input.LA(1)<='\u0AB3')||(input.LA(1)>='\u0AB5' && input.LA(1)<='\u0AB9')||input.LA(1)=='\u0ABD'||input.LA(1)=='\u0AE0'||(input.LA(1)>='\u0B05' && input.LA(1)<='\u0B0C')||(input.LA(1)>='\u0B0F' && input.LA(1)<='\u0B10')||(input.LA(1)>='\u0B13' && input.LA(1)<='\u0B28')||(input.LA(1)>='\u0B2A' && input.LA(1)<='\u0B30')||(input.LA(1)>='\u0B32' && input.LA(1)<='\u0B33')||(input.LA(1)>='\u0B36' && input.LA(1)<='\u0B39')||input.LA(1)=='\u0B3D'||(input.LA(1)>='\u0B5C' && input.LA(1)<='\u0B5D')||(input.LA(1)>='\u0B5F' && input.LA(1)<='\u0B61')||(input.LA(1)>='\u0B85' && input.LA(1)<='\u0B8A')||(input.LA(1)>='\u0B8E' && input.LA(1)<='\u0B90')||(input.LA(1)>='\u0B92' && input.LA(1)<='\u0B95')||(input.LA(1)>='\u0B99' && input.LA(1)<='\u0B9A')||input.LA(1)=='\u0B9C'||(input.LA(1)>='\u0B9E' && input.LA(1)<='\u0B9F')||(input.LA(1)>='\u0BA3' && input.LA(1)<='\u0BA4')||(input.LA(1)>='\u0BA8' && input.LA(1)<='\u0BAA')||(input.LA(1)>='\u0BAE' && input.LA(1)<='\u0BB5')||(input.LA(1)>='\u0BB7' && input.LA(1)<='\u0BB9')||(input.LA(1)>='\u0C05' && input.LA(1)<='\u0C0C')||(input.LA(1)>='\u0C0E' && input.LA(1)<='\u0C10')||(input.LA(1)>='\u0C12' && input.LA(1)<='\u0C28')||(input.LA(1)>='\u0C2A' && input.LA(1)<='\u0C33')||(input.LA(1)>='\u0C35' && input.LA(1)<='\u0C39')||(input.LA(1)>='\u0C60' && input.LA(1)<='\u0C61')||(input.LA(1)>='\u0C85' && input.LA(1)<='\u0C8C')||(input.LA(1)>='\u0C8E' && input.LA(1)<='\u0C90')||(input.LA(1)>='\u0C92' && input.LA(1)<='\u0CA8')||(input.LA(1)>='\u0CAA' && input.LA(1)<='\u0CB3')||(input.LA(1)>='\u0CB5' && input.LA(1)<='\u0CB9')||input.LA(1)=='\u0CDE'||(input.LA(1)>='\u0CE0' && input.LA(1)<='\u0CE1')||(input.LA(1)>='\u0D05' && input.LA(1)<='\u0D0C')||(input.LA(1)>='\u0D0E' && input.LA(1)<='\u0D10')||(input.LA(1)>='\u0D12' && input.LA(1)<='\u0D28')||(input.LA(1)>='\u0D2A' && input.LA(1)<='\u0D39')||(input.LA(1)>='\u0D60' && input.LA(1)<='\u0D61')||(input.LA(1)>='\u0E01' && input.LA(1)<='\u0E2E')||input.LA(1)=='\u0E30'||(input.LA(1)>='\u0E32' && input.LA(1)<='\u0E33')||(input.LA(1)>='\u0E40' && input.LA(1)<='\u0E45')||(input.LA(1)>='\u0E81' && input.LA(1)<='\u0E82')||input.LA(1)=='\u0E84'||(input.LA(1)>='\u0E87' && input.LA(1)<='\u0E88')||input.LA(1)=='\u0E8A'||input.LA(1)=='\u0E8D'||(input.LA(1)>='\u0E94' && input.LA(1)<='\u0E97')||(input.LA(1)>='\u0E99' && input.LA(1)<='\u0E9F')||(input.LA(1)>='\u0EA1' && input.LA(1)<='\u0EA3')||input.LA(1)=='\u0EA5'||input.LA(1)=='\u0EA7'||(input.LA(1)>='\u0EAA' && input.LA(1)<='\u0EAB')||(input.LA(1)>='\u0EAD' && input.LA(1)<='\u0EAE')||input.LA(1)=='\u0EB0'||(input.LA(1)>='\u0EB2' && input.LA(1)<='\u0EB3')||input.LA(1)=='\u0EBD'||(input.LA(1)>='\u0EC0' && input.LA(1)<='\u0EC4')||(input.LA(1)>='\u0F40' && input.LA(1)<='\u0F47')||(input.LA(1)>='\u0F49' && input.LA(1)<='\u0F69')||(input.LA(1)>='\u10A0' && input.LA(1)<='\u10C5')||(input.LA(1)>='\u10D0' && input.LA(1)<='\u10F6')||input.LA(1)=='\u1100'||(input.LA(1)>='\u1102' && input.LA(1)<='\u1103')||(input.LA(1)>='\u1105' && input.LA(1)<='\u1107')||input.LA(1)=='\u1109'||(input.LA(1)>='\u110B' && input.LA(1)<='\u110C')||(input.LA(1)>='\u110E' && input.LA(1)<='\u1112')||input.LA(1)=='\u113C'||input.LA(1)=='\u113E'||input.LA(1)=='\u1140'||input.LA(1)=='\u114C'||input.LA(1)=='\u114E'||input.LA(1)=='\u1150'||(input.LA(1)>='\u1154' && input.LA(1)<='\u1155')||input.LA(1)=='\u1159'||(input.LA(1)>='\u115F' && input.LA(1)<='\u1161')||input.LA(1)=='\u1163'||input.LA(1)=='\u1165'||input.LA(1)=='\u1167'||input.LA(1)=='\u1169'||(input.LA(1)>='\u116D' && input.LA(1)<='\u116E')||(input.LA(1)>='\u1172' && input.LA(1)<='\u1173')||input.LA(1)=='\u1175'||input.LA(1)=='\u119E'||input.LA(1)=='\u11A8'||input.LA(1)=='\u11AB'||(input.LA(1)>='\u11AE' && input.LA(1)<='\u11AF')||(input.LA(1)>='\u11B7' && input.LA(1)<='\u11B8')||input.LA(1)=='\u11BA'||(input.LA(1)>='\u11BC' && input.LA(1)<='\u11C2')||input.LA(1)=='\u11EB'||input.LA(1)=='\u11F0'||input.LA(1)=='\u11F9'||(input.LA(1)>='\u1E00' && input.LA(1)<='\u1E9B')||(input.LA(1)>='\u1EA0' && input.LA(1)<='\u1EF9')||(input.LA(1)>='\u1F00' && input.LA(1)<='\u1F15')||(input.LA(1)>='\u1F18' && input.LA(1)<='\u1F1D')||(input.LA(1)>='\u1F20' && input.LA(1)<='\u1F45')||(input.LA(1)>='\u1F48' && input.LA(1)<='\u1F4D')||(input.LA(1)>='\u1F50' && input.LA(1)<='\u1F57')||input.LA(1)=='\u1F59'||input.LA(1)=='\u1F5B'||input.LA(1)=='\u1F5D'||(input.LA(1)>='\u1F5F' && input.LA(1)<='\u1F7D')||(input.LA(1)>='\u1F80' && input.LA(1)<='\u1FB4')||(input.LA(1)>='\u1FB6' && input.LA(1)<='\u1FBC')||input.LA(1)=='\u1FBE'||(input.LA(1)>='\u1FC2' && input.LA(1)<='\u1FC4')||(input.LA(1)>='\u1FC6' && input.LA(1)<='\u1FCC')||(input.LA(1)>='\u1FD0' && input.LA(1)<='\u1FD3')||(input.LA(1)>='\u1FD6' && input.LA(1)<='\u1FDB')||(input.LA(1)>='\u1FE0' && input.LA(1)<='\u1FEC')||(input.LA(1)>='\u1FF2' && input.LA(1)<='\u1FF4')||(input.LA(1)>='\u1FF6' && input.LA(1)<='\u1FFC')||input.LA(1)=='\u2126'||(input.LA(1)>='\u212A' && input.LA(1)<='\u212B')||input.LA(1)=='\u212E'||(input.LA(1)>='\u2180' && input.LA(1)<='\u2182')||input.LA(1)=='\u3007'||(input.LA(1)>='\u3021' && input.LA(1)<='\u3029')||(input.LA(1)>='\u3041' && input.LA(1)<='\u3094')||(input.LA(1)>='\u30A1' && input.LA(1)<='\u30FA')||(input.LA(1)>='\u3105' && input.LA(1)<='\u312C')||(input.LA(1)>='\u4E00' && input.LA(1)<='\u9FA5')||(input.LA(1)>='\uAC00' && input.LA(1)<='\uD7A3') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "Letter"

    public void mTokens() throws RecognitionException {
        // org/brackit/xquery/compiler/parser/XMLexer.g:1:8: ( LAngle | RAngle | LClose | RClose | SymEq | LCurly | RCurly | Quot | Apos | EscapeQuot | EscapeApos | EscapeLCurly | EscapeRCurly | Colon | DirCommentConstructor | DirPIConstructor | CDataSection | ElementContentChar | QuotAttrContentChar | AposAttrContentChar | PredefinedEntityRef | CharRef | S | NCName )
        int alt16=24;
        alt16 = dfa16.predict(input);
        switch (alt16) {
            case 1 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:10: LAngle
                {
                mLAngle(); 

                }
                break;
            case 2 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:17: RAngle
                {
                mRAngle(); 

                }
                break;
            case 3 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:24: LClose
                {
                mLClose(); 

                }
                break;
            case 4 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:31: RClose
                {
                mRClose(); 

                }
                break;
            case 5 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:38: SymEq
                {
                mSymEq(); 

                }
                break;
            case 6 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:44: LCurly
                {
                mLCurly(); 

                }
                break;
            case 7 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:51: RCurly
                {
                mRCurly(); 

                }
                break;
            case 8 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:58: Quot
                {
                mQuot(); 

                }
                break;
            case 9 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:63: Apos
                {
                mApos(); 

                }
                break;
            case 10 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:68: EscapeQuot
                {
                mEscapeQuot(); 

                }
                break;
            case 11 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:79: EscapeApos
                {
                mEscapeApos(); 

                }
                break;
            case 12 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:90: EscapeLCurly
                {
                mEscapeLCurly(); 

                }
                break;
            case 13 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:103: EscapeRCurly
                {
                mEscapeRCurly(); 

                }
                break;
            case 14 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:116: Colon
                {
                mColon(); 

                }
                break;
            case 15 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:122: DirCommentConstructor
                {
                mDirCommentConstructor(); 

                }
                break;
            case 16 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:144: DirPIConstructor
                {
                mDirPIConstructor(); 

                }
                break;
            case 17 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:161: CDataSection
                {
                mCDataSection(); 

                }
                break;
            case 18 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:174: ElementContentChar
                {
                mElementContentChar(); 

                }
                break;
            case 19 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:193: QuotAttrContentChar
                {
                mQuotAttrContentChar(); 

                }
                break;
            case 20 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:213: AposAttrContentChar
                {
                mAposAttrContentChar(); 

                }
                break;
            case 21 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:233: PredefinedEntityRef
                {
                mPredefinedEntityRef(); 

                }
                break;
            case 22 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:253: CharRef
                {
                mCharRef(); 

                }
                break;
            case 23 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:261: S
                {
                mS(); 

                }
                break;
            case 24 :
                // org/brackit/xquery/compiler/parser/XMLexer.g:1:263: NCName
                {
                mNCName(); 

                }
                break;

        }

    }


    protected DFA16 dfa16 = new DFA16(this);
    static final String DFA16_eotS =
        "\1\uffff\1\21\1\22\1\26\1\27\1\31\1\33\1\35\1\40\1\41\1\42\1\uffff"+
        "\1\45\1\26\5\uffff\1\54\1\55\1\56\6\uffff\1\60\2\uffff\1\62\6\uffff"+
        "\1\45\23\uffff";
    static final String DFA16_eofS =
        "\72\uffff";
    static final String DFA16_minS =
        "\1\11\1\41\3\11\1\173\1\175\4\11\1\43\2\11\1\uffff\1\55\2\uffff"+
        "\1\0\3\11\2\0\4\uffff\1\11\1\0\1\uffff\1\11\3\0\2\uffff\1\0\1\11"+
        "\5\uffff\3\0\1\uffff\1\0\1\uffff\1\0\7\uffff";
    static final String DFA16_maxS =
        "\1\ufffd\1\77\3\ufffd\1\173\1\175\4\ufffd\1\161\2\ufffd\1\uffff"+
        "\1\133\2\uffff\1\0\3\ufffd\2\0\4\uffff\1\ufffd\1\0\1\uffff\1\ufffd"+
        "\3\0\2\uffff\1\0\1\ufffd\5\uffff\3\0\1\uffff\1\0\1\uffff\1\0\7\uffff";
    static final String DFA16_acceptS =
        "\16\uffff\1\3\1\uffff\1\20\1\1\6\uffff\1\14\1\6\1\15\1\7\2\uffff"+
        "\1\22\4\uffff\1\26\1\25\2\uffff\1\17\1\21\1\2\1\23\1\24\3\uffff"+
        "\1\5\1\uffff\1\10\1\uffff\1\11\1\16\1\27\1\30\1\4\1\12\1\13";
    static final String DFA16_specialS =
        "\1\14\1\40\1\26\1\21\1\31\1\15\1\20\1\16\1\32\1\24\1\4\1\5\1\23"+
        "\1\30\1\uffff\1\10\2\uffff\1\11\1\22\1\17\1\6\1\25\1\13\4\uffff"+
        "\1\41\1\36\1\uffff\1\27\1\37\1\7\1\1\2\uffff\1\0\1\35\5\uffff\1"+
        "\2\1\3\1\12\1\uffff\1\34\1\uffff\1\33\7\uffff}>";
    static final String[] DFA16_transitionS = {
            "\2\12\2\uffff\1\12\22\uffff\1\12\1\15\1\7\3\15\1\13\1\10\7\15"+
            "\1\3\12\15\1\11\1\15\1\1\1\4\1\2\2\15\32\14\4\15\1\14\1\15\32"+
            "\14\1\5\1\15\1\6\102\15\27\14\1\15\37\14\1\15\72\14\2\15\13"+
            "\14\2\15\10\14\1\15\65\14\1\15\104\14\11\15\44\14\3\15\2\14"+
            "\4\15\36\14\70\15\131\14\22\15\7\14\u00c4\15\1\14\1\15\3\14"+
            "\1\15\1\14\1\15\24\14\1\15\54\14\1\15\7\14\3\15\1\14\1\15\1"+
            "\14\1\15\1\14\1\15\1\14\1\15\22\14\15\15\14\14\1\15\102\14\1"+
            "\15\14\14\1\15\44\14\16\15\65\14\2\15\2\14\2\15\2\14\3\15\34"+
            "\14\2\15\10\14\2\15\2\14\67\15\46\14\2\15\1\14\7\15\46\14\111"+
            "\15\33\14\5\15\3\14\56\15\32\14\6\15\12\14\46\15\107\14\2\15"+
            "\5\14\1\15\17\14\1\15\4\14\1\15\1\14\17\15\2\14\u021e\15\65"+
            "\14\3\15\1\14\32\15\12\14\43\15\10\14\2\15\2\14\2\15\26\14\1"+
            "\15\7\14\1\15\1\14\3\15\4\14\42\15\2\14\1\15\3\14\16\15\2\14"+
            "\23\15\6\14\4\15\2\14\2\15\26\14\1\15\7\14\1\15\2\14\1\15\2"+
            "\14\1\15\2\14\37\15\4\14\1\15\1\14\23\15\3\14\20\15\7\14\1\15"+
            "\1\14\1\15\3\14\1\15\26\14\1\15\7\14\1\15\2\14\1\15\5\14\3\15"+
            "\1\14\42\15\1\14\44\15\10\14\2\15\2\14\2\15\26\14\1\15\7\14"+
            "\1\15\2\14\2\15\4\14\3\15\1\14\36\15\2\14\1\15\3\14\43\15\6"+
            "\14\3\15\3\14\1\15\4\14\3\15\2\14\1\15\1\14\1\15\2\14\3\15\2"+
            "\14\3\15\3\14\3\15\10\14\1\15\3\14\113\15\10\14\1\15\3\14\1"+
            "\15\27\14\1\15\12\14\1\15\5\14\46\15\2\14\43\15\10\14\1\15\3"+
            "\14\1\15\27\14\1\15\12\14\1\15\5\14\44\15\1\14\1\15\2\14\43"+
            "\15\10\14\1\15\3\14\1\15\27\14\1\15\20\14\46\15\2\14\u009f\15"+
            "\56\14\1\15\1\14\1\15\2\14\14\15\6\14\73\15\2\14\1\15\1\14\2"+
            "\15\2\14\1\15\1\14\2\15\1\14\6\15\4\14\1\15\7\14\1\15\3\14\1"+
            "\15\1\14\1\15\1\14\2\15\2\14\1\15\2\14\1\15\1\14\1\15\2\14\11"+
            "\15\1\14\2\15\5\14\173\15\10\14\1\15\41\14\u0136\15\46\14\12"+
            "\15\47\14\11\15\1\14\1\15\2\14\1\15\3\14\1\15\1\14\1\15\2\14"+
            "\1\15\5\14\51\15\1\14\1\15\1\14\1\15\1\14\13\15\1\14\1\15\1"+
            "\14\1\15\1\14\3\15\2\14\3\15\1\14\5\15\3\14\1\15\1\14\1\15\1"+
            "\14\1\15\1\14\1\15\1\14\3\15\2\14\3\15\2\14\1\15\1\14\50\15"+
            "\1\14\11\15\1\14\2\15\1\14\2\15\2\14\7\15\2\14\1\15\1\14\1\15"+
            "\7\14\50\15\1\14\4\15\1\14\10\15\1\14\u0c06\15\u009c\14\4\15"+
            "\132\14\6\15\26\14\2\15\6\14\2\15\46\14\2\15\6\14\2\15\10\14"+
            "\1\15\1\14\1\15\1\14\1\15\1\14\1\15\37\14\2\15\65\14\1\15\7"+
            "\14\1\15\1\14\3\15\3\14\1\15\7\14\3\15\4\14\2\15\6\14\4\15\15"+
            "\14\5\15\3\14\1\15\7\14\u0129\15\1\14\3\15\2\14\2\15\1\14\121"+
            "\15\3\14\u0e84\15\1\14\31\15\11\14\27\15\124\14\14\15\132\14"+
            "\12\15\50\14\u1cd3\15\u51a6\14\u0c5a\15\u2ba4\14\134\15\u0800"+
            "\uffff\u1ffe\15",
            "\1\17\15\uffff\1\16\17\uffff\1\20",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\1\15\1\25\74\15\1\uffff\1\15\1\uffff\ud782\15\u0800"+
            "\uffff\u1ffe\15",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "\1\30",
            "\1\32",
            "\2\24\2\uffff\1\24\22\uffff\2\24\1\34\3\24\1\uffff\1\36\24"+
            "\24\1\uffff\76\24\1\uffff\1\24\1\uffff\ud782\24\u0800\uffff"+
            "\u1ffe\24",
            "\2\23\2\uffff\1\23\22\uffff\2\23\1\36\3\23\1\uffff\1\37\24"+
            "\23\1\uffff\76\23\1\uffff\1\23\1\uffff\ud782\23\u0800\uffff"+
            "\u1ffe\23",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "\2\12\2\uffff\1\12\22\uffff\1\12\1\15\1\24\3\15\1\uffff\1\23"+
            "\24\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "\1\43\75\uffff\1\44\5\uffff\1\44\4\uffff\1\44\4\uffff\1\44",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\5\15"+
            "\2\46\1\15\12\46\2\15\1\uffff\4\15\32\46\4\15\1\46\1\15\32\46"+
            "\1\uffff\1\15\1\uffff\71\15\1\46\10\15\27\46\1\15\37\46\1\15"+
            "\u0286\46\1\15\u1c81\46\14\15\2\46\61\15\2\46\57\15\u0120\46"+
            "\u0a70\15\u03f0\46\21\15\ua7ff\46\u0800\uffff\u1900\15\u04d0"+
            "\46\40\15\u020e\46",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "",
            "\1\47\55\uffff\1\50",
            "",
            "",
            "\1\uffff",
            "\2\23\2\uffff\1\23\22\uffff\2\23\1\36\3\23\1\uffff\25\23\1"+
            "\uffff\76\23\1\uffff\1\23\1\uffff\ud782\23\u0800\uffff\u1ffe"+
            "\23",
            "\2\24\2\uffff\1\24\22\uffff\6\24\1\uffff\1\36\24\24\1\uffff"+
            "\76\24\1\uffff\1\24\1\uffff\ud782\24\u0800\uffff\u1ffe\24",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\24"+
            "\15\1\uffff\76\15\1\uffff\1\15\1\uffff\ud782\15\u0800\uffff"+
            "\u1ffe\15",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "\2\24\2\uffff\1\24\22\uffff\6\24\1\uffff\1\36\24\24\1\uffff"+
            "\76\24\1\uffff\1\24\1\uffff\ud782\24\u0800\uffff\u1ffe\24",
            "\1\uffff",
            "",
            "\2\23\2\uffff\1\23\22\uffff\2\23\1\36\3\23\1\uffff\25\23\1"+
            "\uffff\76\23\1\uffff\1\23\1\uffff\ud782\23\u0800\uffff\u1ffe"+
            "\23",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "\1\uffff",
            "\2\15\2\uffff\1\15\22\uffff\2\15\1\24\3\15\1\uffff\1\23\5\15"+
            "\2\46\1\15\12\46\2\15\1\uffff\4\15\32\46\4\15\1\46\1\15\32\46"+
            "\1\uffff\1\15\1\uffff\71\15\1\46\10\15\27\46\1\15\37\46\1\15"+
            "\u0286\46\1\15\u1c81\46\14\15\2\46\61\15\2\46\57\15\u0120\46"+
            "\u0a70\15\u03f0\46\21\15\ua7ff\46\u0800\uffff\u1900\15\u04d0"+
            "\46\40\15\u020e\46",
            "",
            "",
            "",
            "",
            "",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "\1\uffff",
            "",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA16_eot = DFA.unpackEncodedString(DFA16_eotS);
    static final short[] DFA16_eof = DFA.unpackEncodedString(DFA16_eofS);
    static final char[] DFA16_min = DFA.unpackEncodedStringToUnsignedChars(DFA16_minS);
    static final char[] DFA16_max = DFA.unpackEncodedStringToUnsignedChars(DFA16_maxS);
    static final short[] DFA16_accept = DFA.unpackEncodedString(DFA16_acceptS);
    static final short[] DFA16_special = DFA.unpackEncodedString(DFA16_specialS);
    static final short[][] DFA16_transition;

    static {
        int numStates = DFA16_transitionS.length;
        DFA16_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA16_transition[i] = DFA.unpackEncodedString(DFA16_transitionS[i]);
        }
    }

    class DFA16 extends DFA {

        public DFA16(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 16;
            this.eot = DFA16_eot;
            this.eof = DFA16_eof;
            this.min = DFA16_min;
            this.max = DFA16_max;
            this.accept = DFA16_accept;
            this.special = DFA16_special;
            this.transition = DFA16_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( LAngle | RAngle | LClose | RClose | SymEq | LCurly | RCurly | Quot | Apos | EscapeQuot | EscapeApos | EscapeLCurly | EscapeRCurly | Colon | DirCommentConstructor | DirPIConstructor | CDataSection | ElementContentChar | QuotAttrContentChar | AposAttrContentChar | PredefinedEntityRef | CharRef | S | NCName );";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            IntStream input = _input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA16_37 = input.LA(1);

                         
                        int index16_37 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                        else if ( (( isInTag() )) ) {s = 54;}

                         
                        input.seek(index16_37);
                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA16_34 = input.LA(1);

                         
                        int index16_34 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                        else if ( (( isInTag() )) ) {s = 53;}

                         
                        input.seek(index16_34);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA16_44 = input.LA(1);

                         
                        int index16_44 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                         
                        input.seek(index16_44);
                        if ( s>=0 ) return s;
                        break;
                    case 3 : 
                        int LA16_45 = input.LA(1);

                         
                        int index16_45 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_45);
                        if ( s>=0 ) return s;
                        break;
                    case 4 : 
                        int LA16_10 = input.LA(1);

                         
                        int index16_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_10>='\t' && LA16_10<='\n')||LA16_10=='\r'||LA16_10==' ') && ((( !isInTag() )||( isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 10;}

                        else if ( (LA16_10=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_10=='!'||(LA16_10>='#' && LA16_10<='%')||(LA16_10>='(' && LA16_10<=';')||(LA16_10>='=' && LA16_10<='z')||LA16_10=='|'||(LA16_10>='~' && LA16_10<='\uD7FF')||(LA16_10>='\uE000' && LA16_10<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_10=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 34;

                         
                        input.seek(index16_10);
                        if ( s>=0 ) return s;
                        break;
                    case 5 : 
                        int LA16_11 = input.LA(1);

                         
                        int index16_11 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_11=='#') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 35;}

                        else if ( (LA16_11=='a'||LA16_11=='g'||LA16_11=='l'||LA16_11=='q') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 36;}

                         
                        input.seek(index16_11);
                        if ( s>=0 ) return s;
                        break;
                    case 6 : 
                        int LA16_21 = input.LA(1);

                         
                        int index16_21 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_21>='\t' && LA16_21<='\n')||LA16_21=='\r'||(LA16_21>=' ' && LA16_21<='!')||(LA16_21>='#' && LA16_21<='%')||(LA16_21>='(' && LA16_21<=';')||(LA16_21>='=' && LA16_21<='z')||LA16_21=='|'||(LA16_21>='~' && LA16_21<='\uD7FF')||(LA16_21>='\uE000' && LA16_21<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_21=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_21=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 46;

                         
                        input.seek(index16_21);
                        if ( s>=0 ) return s;
                        break;
                    case 7 : 
                        int LA16_33 = input.LA(1);

                         
                        int index16_33 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (!(((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() ))))) ) {s = 52;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_33);
                        if ( s>=0 ) return s;
                        break;
                    case 8 : 
                        int LA16_15 = input.LA(1);

                         
                        int index16_15 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_15=='-') && (( !isInTag() ))) {s = 39;}

                        else if ( (LA16_15=='[') && (( !isInTag() ))) {s = 40;}

                         
                        input.seek(index16_15);
                        if ( s>=0 ) return s;
                        break;
                    case 9 : 
                        int LA16_18 = input.LA(1);

                         
                        int index16_18 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((  isInTag() )) ) {s = 41;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_18);
                        if ( s>=0 ) return s;
                        break;
                    case 10 : 
                        int LA16_46 = input.LA(1);

                         
                        int index16_46 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((  isInTag() )) ) {s = 55;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_46);
                        if ( s>=0 ) return s;
                        break;
                    case 11 : 
                        int LA16_23 = input.LA(1);

                         
                        int index16_23 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((  isInTag() )) ) {s = 47;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_23);
                        if ( s>=0 ) return s;
                        break;
                    case 12 : 
                        int LA16_0 = input.LA(1);

                         
                        int index16_0 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_0=='<') && (( !isInTag() ))) {s = 1;}

                        else if ( (LA16_0=='>') && ((( !isInTag() )||( isInAposAttr() )||(  isInTag() )||( isInQuotAttr() )))) {s = 2;}

                        else if ( (LA16_0=='/') && ((( !isInTag() )||( isInAposAttr() )||(  isInTag() )||( isInQuotAttr() )))) {s = 3;}

                        else if ( (LA16_0=='=') && ((( !isInTag() )||( isInAposAttr() )||(  isInTag() )||( isInQuotAttr() )))) {s = 4;}

                        else if ( (LA16_0=='{') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 5;}

                        else if ( (LA16_0=='}') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 6;}

                        else if ( (LA16_0=='\"') && ((( !isInTag() )||(  isInTag() || isInAposAttr() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 7;}

                        else if ( (LA16_0=='\'') && ((( !isInTag() )||(  isInTag() || isInQuotAttr() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 8;}

                        else if ( (LA16_0==':') ) {s = 9;}

                        else if ( ((LA16_0>='\t' && LA16_0<='\n')||LA16_0=='\r'||LA16_0==' ') && ((( !isInTag() )||( isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 10;}

                        else if ( (LA16_0=='&') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 11;}

                        else if ( ((LA16_0>='A' && LA16_0<='Z')||LA16_0=='_'||(LA16_0>='a' && LA16_0<='z')||(LA16_0>='\u00C0' && LA16_0<='\u00D6')||(LA16_0>='\u00D8' && LA16_0<='\u00F6')||(LA16_0>='\u00F8' && LA16_0<='\u0131')||(LA16_0>='\u0134' && LA16_0<='\u013E')||(LA16_0>='\u0141' && LA16_0<='\u0148')||(LA16_0>='\u014A' && LA16_0<='\u017E')||(LA16_0>='\u0180' && LA16_0<='\u01C3')||(LA16_0>='\u01CD' && LA16_0<='\u01F0')||(LA16_0>='\u01F4' && LA16_0<='\u01F5')||(LA16_0>='\u01FA' && LA16_0<='\u0217')||(LA16_0>='\u0250' && LA16_0<='\u02A8')||(LA16_0>='\u02BB' && LA16_0<='\u02C1')||LA16_0=='\u0386'||(LA16_0>='\u0388' && LA16_0<='\u038A')||LA16_0=='\u038C'||(LA16_0>='\u038E' && LA16_0<='\u03A1')||(LA16_0>='\u03A3' && LA16_0<='\u03CE')||(LA16_0>='\u03D0' && LA16_0<='\u03D6')||LA16_0=='\u03DA'||LA16_0=='\u03DC'||LA16_0=='\u03DE'||LA16_0=='\u03E0'||(LA16_0>='\u03E2' && LA16_0<='\u03F3')||(LA16_0>='\u0401' && LA16_0<='\u040C')||(LA16_0>='\u040E' && LA16_0<='\u044F')||(LA16_0>='\u0451' && LA16_0<='\u045C')||(LA16_0>='\u045E' && LA16_0<='\u0481')||(LA16_0>='\u0490' && LA16_0<='\u04C4')||(LA16_0>='\u04C7' && LA16_0<='\u04C8')||(LA16_0>='\u04CB' && LA16_0<='\u04CC')||(LA16_0>='\u04D0' && LA16_0<='\u04EB')||(LA16_0>='\u04EE' && LA16_0<='\u04F5')||(LA16_0>='\u04F8' && LA16_0<='\u04F9')||(LA16_0>='\u0531' && LA16_0<='\u0556')||LA16_0=='\u0559'||(LA16_0>='\u0561' && LA16_0<='\u0586')||(LA16_0>='\u05D0' && LA16_0<='\u05EA')||(LA16_0>='\u05F0' && LA16_0<='\u05F2')||(LA16_0>='\u0621' && LA16_0<='\u063A')||(LA16_0>='\u0641' && LA16_0<='\u064A')||(LA16_0>='\u0671' && LA16_0<='\u06B7')||(LA16_0>='\u06BA' && LA16_0<='\u06BE')||(LA16_0>='\u06C0' && LA16_0<='\u06CE')||(LA16_0>='\u06D0' && LA16_0<='\u06D3')||LA16_0=='\u06D5'||(LA16_0>='\u06E5' && LA16_0<='\u06E6')||(LA16_0>='\u0905' && LA16_0<='\u0939')||LA16_0=='\u093D'||(LA16_0>='\u0958' && LA16_0<='\u0961')||(LA16_0>='\u0985' && LA16_0<='\u098C')||(LA16_0>='\u098F' && LA16_0<='\u0990')||(LA16_0>='\u0993' && LA16_0<='\u09A8')||(LA16_0>='\u09AA' && LA16_0<='\u09B0')||LA16_0=='\u09B2'||(LA16_0>='\u09B6' && LA16_0<='\u09B9')||(LA16_0>='\u09DC' && LA16_0<='\u09DD')||(LA16_0>='\u09DF' && LA16_0<='\u09E1')||(LA16_0>='\u09F0' && LA16_0<='\u09F1')||(LA16_0>='\u0A05' && LA16_0<='\u0A0A')||(LA16_0>='\u0A0F' && LA16_0<='\u0A10')||(LA16_0>='\u0A13' && LA16_0<='\u0A28')||(LA16_0>='\u0A2A' && LA16_0<='\u0A30')||(LA16_0>='\u0A32' && LA16_0<='\u0A33')||(LA16_0>='\u0A35' && LA16_0<='\u0A36')||(LA16_0>='\u0A38' && LA16_0<='\u0A39')||(LA16_0>='\u0A59' && LA16_0<='\u0A5C')||LA16_0=='\u0A5E'||(LA16_0>='\u0A72' && LA16_0<='\u0A74')||(LA16_0>='\u0A85' && LA16_0<='\u0A8B')||LA16_0=='\u0A8D'||(LA16_0>='\u0A8F' && LA16_0<='\u0A91')||(LA16_0>='\u0A93' && LA16_0<='\u0AA8')||(LA16_0>='\u0AAA' && LA16_0<='\u0AB0')||(LA16_0>='\u0AB2' && LA16_0<='\u0AB3')||(LA16_0>='\u0AB5' && LA16_0<='\u0AB9')||LA16_0=='\u0ABD'||LA16_0=='\u0AE0'||(LA16_0>='\u0B05' && LA16_0<='\u0B0C')||(LA16_0>='\u0B0F' && LA16_0<='\u0B10')||(LA16_0>='\u0B13' && LA16_0<='\u0B28')||(LA16_0>='\u0B2A' && LA16_0<='\u0B30')||(LA16_0>='\u0B32' && LA16_0<='\u0B33')||(LA16_0>='\u0B36' && LA16_0<='\u0B39')||LA16_0=='\u0B3D'||(LA16_0>='\u0B5C' && LA16_0<='\u0B5D')||(LA16_0>='\u0B5F' && LA16_0<='\u0B61')||(LA16_0>='\u0B85' && LA16_0<='\u0B8A')||(LA16_0>='\u0B8E' && LA16_0<='\u0B90')||(LA16_0>='\u0B92' && LA16_0<='\u0B95')||(LA16_0>='\u0B99' && LA16_0<='\u0B9A')||LA16_0=='\u0B9C'||(LA16_0>='\u0B9E' && LA16_0<='\u0B9F')||(LA16_0>='\u0BA3' && LA16_0<='\u0BA4')||(LA16_0>='\u0BA8' && LA16_0<='\u0BAA')||(LA16_0>='\u0BAE' && LA16_0<='\u0BB5')||(LA16_0>='\u0BB7' && LA16_0<='\u0BB9')||(LA16_0>='\u0C05' && LA16_0<='\u0C0C')||(LA16_0>='\u0C0E' && LA16_0<='\u0C10')||(LA16_0>='\u0C12' && LA16_0<='\u0C28')||(LA16_0>='\u0C2A' && LA16_0<='\u0C33')||(LA16_0>='\u0C35' && LA16_0<='\u0C39')||(LA16_0>='\u0C60' && LA16_0<='\u0C61')||(LA16_0>='\u0C85' && LA16_0<='\u0C8C')||(LA16_0>='\u0C8E' && LA16_0<='\u0C90')||(LA16_0>='\u0C92' && LA16_0<='\u0CA8')||(LA16_0>='\u0CAA' && LA16_0<='\u0CB3')||(LA16_0>='\u0CB5' && LA16_0<='\u0CB9')||LA16_0=='\u0CDE'||(LA16_0>='\u0CE0' && LA16_0<='\u0CE1')||(LA16_0>='\u0D05' && LA16_0<='\u0D0C')||(LA16_0>='\u0D0E' && LA16_0<='\u0D10')||(LA16_0>='\u0D12' && LA16_0<='\u0D28')||(LA16_0>='\u0D2A' && LA16_0<='\u0D39')||(LA16_0>='\u0D60' && LA16_0<='\u0D61')||(LA16_0>='\u0E01' && LA16_0<='\u0E2E')||LA16_0=='\u0E30'||(LA16_0>='\u0E32' && LA16_0<='\u0E33')||(LA16_0>='\u0E40' && LA16_0<='\u0E45')||(LA16_0>='\u0E81' && LA16_0<='\u0E82')||LA16_0=='\u0E84'||(LA16_0>='\u0E87' && LA16_0<='\u0E88')||LA16_0=='\u0E8A'||LA16_0=='\u0E8D'||(LA16_0>='\u0E94' && LA16_0<='\u0E97')||(LA16_0>='\u0E99' && LA16_0<='\u0E9F')||(LA16_0>='\u0EA1' && LA16_0<='\u0EA3')||LA16_0=='\u0EA5'||LA16_0=='\u0EA7'||(LA16_0>='\u0EAA' && LA16_0<='\u0EAB')||(LA16_0>='\u0EAD' && LA16_0<='\u0EAE')||LA16_0=='\u0EB0'||(LA16_0>='\u0EB2' && LA16_0<='\u0EB3')||LA16_0=='\u0EBD'||(LA16_0>='\u0EC0' && LA16_0<='\u0EC4')||(LA16_0>='\u0F40' && LA16_0<='\u0F47')||(LA16_0>='\u0F49' && LA16_0<='\u0F69')||(LA16_0>='\u10A0' && LA16_0<='\u10C5')||(LA16_0>='\u10D0' && LA16_0<='\u10F6')||LA16_0=='\u1100'||(LA16_0>='\u1102' && LA16_0<='\u1103')||(LA16_0>='\u1105' && LA16_0<='\u1107')||LA16_0=='\u1109'||(LA16_0>='\u110B' && LA16_0<='\u110C')||(LA16_0>='\u110E' && LA16_0<='\u1112')||LA16_0=='\u113C'||LA16_0=='\u113E'||LA16_0=='\u1140'||LA16_0=='\u114C'||LA16_0=='\u114E'||LA16_0=='\u1150'||(LA16_0>='\u1154' && LA16_0<='\u1155')||LA16_0=='\u1159'||(LA16_0>='\u115F' && LA16_0<='\u1161')||LA16_0=='\u1163'||LA16_0=='\u1165'||LA16_0=='\u1167'||LA16_0=='\u1169'||(LA16_0>='\u116D' && LA16_0<='\u116E')||(LA16_0>='\u1172' && LA16_0<='\u1173')||LA16_0=='\u1175'||LA16_0=='\u119E'||LA16_0=='\u11A8'||LA16_0=='\u11AB'||(LA16_0>='\u11AE' && LA16_0<='\u11AF')||(LA16_0>='\u11B7' && LA16_0<='\u11B8')||LA16_0=='\u11BA'||(LA16_0>='\u11BC' && LA16_0<='\u11C2')||LA16_0=='\u11EB'||LA16_0=='\u11F0'||LA16_0=='\u11F9'||(LA16_0>='\u1E00' && LA16_0<='\u1E9B')||(LA16_0>='\u1EA0' && LA16_0<='\u1EF9')||(LA16_0>='\u1F00' && LA16_0<='\u1F15')||(LA16_0>='\u1F18' && LA16_0<='\u1F1D')||(LA16_0>='\u1F20' && LA16_0<='\u1F45')||(LA16_0>='\u1F48' && LA16_0<='\u1F4D')||(LA16_0>='\u1F50' && LA16_0<='\u1F57')||LA16_0=='\u1F59'||LA16_0=='\u1F5B'||LA16_0=='\u1F5D'||(LA16_0>='\u1F5F' && LA16_0<='\u1F7D')||(LA16_0>='\u1F80' && LA16_0<='\u1FB4')||(LA16_0>='\u1FB6' && LA16_0<='\u1FBC')||LA16_0=='\u1FBE'||(LA16_0>='\u1FC2' && LA16_0<='\u1FC4')||(LA16_0>='\u1FC6' && LA16_0<='\u1FCC')||(LA16_0>='\u1FD0' && LA16_0<='\u1FD3')||(LA16_0>='\u1FD6' && LA16_0<='\u1FDB')||(LA16_0>='\u1FE0' && LA16_0<='\u1FEC')||(LA16_0>='\u1FF2' && LA16_0<='\u1FF4')||(LA16_0>='\u1FF6' && LA16_0<='\u1FFC')||LA16_0=='\u2126'||(LA16_0>='\u212A' && LA16_0<='\u212B')||LA16_0=='\u212E'||(LA16_0>='\u2180' && LA16_0<='\u2182')||LA16_0=='\u3007'||(LA16_0>='\u3021' && LA16_0<='\u3029')||(LA16_0>='\u3041' && LA16_0<='\u3094')||(LA16_0>='\u30A1' && LA16_0<='\u30FA')||(LA16_0>='\u3105' && LA16_0<='\u312C')||(LA16_0>='\u4E00' && LA16_0<='\u9FA5')||(LA16_0>='\uAC00' && LA16_0<='\uD7A3')) && ((( !isInTag() )||( isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 12;}

                        else if ( (LA16_0=='!'||(LA16_0>='#' && LA16_0<='%')||(LA16_0>='(' && LA16_0<='.')||(LA16_0>='0' && LA16_0<='9')||LA16_0==';'||(LA16_0>='?' && LA16_0<='@')||(LA16_0>='[' && LA16_0<='^')||LA16_0=='`'||LA16_0=='|'||(LA16_0>='~' && LA16_0<='\u00BF')||LA16_0=='\u00D7'||LA16_0=='\u00F7'||(LA16_0>='\u0132' && LA16_0<='\u0133')||(LA16_0>='\u013F' && LA16_0<='\u0140')||LA16_0=='\u0149'||LA16_0=='\u017F'||(LA16_0>='\u01C4' && LA16_0<='\u01CC')||(LA16_0>='\u01F1' && LA16_0<='\u01F3')||(LA16_0>='\u01F6' && LA16_0<='\u01F9')||(LA16_0>='\u0218' && LA16_0<='\u024F')||(LA16_0>='\u02A9' && LA16_0<='\u02BA')||(LA16_0>='\u02C2' && LA16_0<='\u0385')||LA16_0=='\u0387'||LA16_0=='\u038B'||LA16_0=='\u038D'||LA16_0=='\u03A2'||LA16_0=='\u03CF'||(LA16_0>='\u03D7' && LA16_0<='\u03D9')||LA16_0=='\u03DB'||LA16_0=='\u03DD'||LA16_0=='\u03DF'||LA16_0=='\u03E1'||(LA16_0>='\u03F4' && LA16_0<='\u0400')||LA16_0=='\u040D'||LA16_0=='\u0450'||LA16_0=='\u045D'||(LA16_0>='\u0482' && LA16_0<='\u048F')||(LA16_0>='\u04C5' && LA16_0<='\u04C6')||(LA16_0>='\u04C9' && LA16_0<='\u04CA')||(LA16_0>='\u04CD' && LA16_0<='\u04CF')||(LA16_0>='\u04EC' && LA16_0<='\u04ED')||(LA16_0>='\u04F6' && LA16_0<='\u04F7')||(LA16_0>='\u04FA' && LA16_0<='\u0530')||(LA16_0>='\u0557' && LA16_0<='\u0558')||(LA16_0>='\u055A' && LA16_0<='\u0560')||(LA16_0>='\u0587' && LA16_0<='\u05CF')||(LA16_0>='\u05EB' && LA16_0<='\u05EF')||(LA16_0>='\u05F3' && LA16_0<='\u0620')||(LA16_0>='\u063B' && LA16_0<='\u0640')||(LA16_0>='\u064B' && LA16_0<='\u0670')||(LA16_0>='\u06B8' && LA16_0<='\u06B9')||LA16_0=='\u06BF'||LA16_0=='\u06CF'||LA16_0=='\u06D4'||(LA16_0>='\u06D6' && LA16_0<='\u06E4')||(LA16_0>='\u06E7' && LA16_0<='\u0904')||(LA16_0>='\u093A' && LA16_0<='\u093C')||(LA16_0>='\u093E' && LA16_0<='\u0957')||(LA16_0>='\u0962' && LA16_0<='\u0984')||(LA16_0>='\u098D' && LA16_0<='\u098E')||(LA16_0>='\u0991' && LA16_0<='\u0992')||LA16_0=='\u09A9'||LA16_0=='\u09B1'||(LA16_0>='\u09B3' && LA16_0<='\u09B5')||(LA16_0>='\u09BA' && LA16_0<='\u09DB')||LA16_0=='\u09DE'||(LA16_0>='\u09E2' && LA16_0<='\u09EF')||(LA16_0>='\u09F2' && LA16_0<='\u0A04')||(LA16_0>='\u0A0B' && LA16_0<='\u0A0E')||(LA16_0>='\u0A11' && LA16_0<='\u0A12')||LA16_0=='\u0A29'||LA16_0=='\u0A31'||LA16_0=='\u0A34'||LA16_0=='\u0A37'||(LA16_0>='\u0A3A' && LA16_0<='\u0A58')||LA16_0=='\u0A5D'||(LA16_0>='\u0A5F' && LA16_0<='\u0A71')||(LA16_0>='\u0A75' && LA16_0<='\u0A84')||LA16_0=='\u0A8C'||LA16_0=='\u0A8E'||LA16_0=='\u0A92'||LA16_0=='\u0AA9'||LA16_0=='\u0AB1'||LA16_0=='\u0AB4'||(LA16_0>='\u0ABA' && LA16_0<='\u0ABC')||(LA16_0>='\u0ABE' && LA16_0<='\u0ADF')||(LA16_0>='\u0AE1' && LA16_0<='\u0B04')||(LA16_0>='\u0B0D' && LA16_0<='\u0B0E')||(LA16_0>='\u0B11' && LA16_0<='\u0B12')||LA16_0=='\u0B29'||LA16_0=='\u0B31'||(LA16_0>='\u0B34' && LA16_0<='\u0B35')||(LA16_0>='\u0B3A' && LA16_0<='\u0B3C')||(LA16_0>='\u0B3E' && LA16_0<='\u0B5B')||LA16_0=='\u0B5E'||(LA16_0>='\u0B62' && LA16_0<='\u0B84')||(LA16_0>='\u0B8B' && LA16_0<='\u0B8D')||LA16_0=='\u0B91'||(LA16_0>='\u0B96' && LA16_0<='\u0B98')||LA16_0=='\u0B9B'||LA16_0=='\u0B9D'||(LA16_0>='\u0BA0' && LA16_0<='\u0BA2')||(LA16_0>='\u0BA5' && LA16_0<='\u0BA7')||(LA16_0>='\u0BAB' && LA16_0<='\u0BAD')||LA16_0=='\u0BB6'||(LA16_0>='\u0BBA' && LA16_0<='\u0C04')||LA16_0=='\u0C0D'||LA16_0=='\u0C11'||LA16_0=='\u0C29'||LA16_0=='\u0C34'||(LA16_0>='\u0C3A' && LA16_0<='\u0C5F')||(LA16_0>='\u0C62' && LA16_0<='\u0C84')||LA16_0=='\u0C8D'||LA16_0=='\u0C91'||LA16_0=='\u0CA9'||LA16_0=='\u0CB4'||(LA16_0>='\u0CBA' && LA16_0<='\u0CDD')||LA16_0=='\u0CDF'||(LA16_0>='\u0CE2' && LA16_0<='\u0D04')||LA16_0=='\u0D0D'||LA16_0=='\u0D11'||LA16_0=='\u0D29'||(LA16_0>='\u0D3A' && LA16_0<='\u0D5F')||(LA16_0>='\u0D62' && LA16_0<='\u0E00')||LA16_0=='\u0E2F'||LA16_0=='\u0E31'||(LA16_0>='\u0E34' && LA16_0<='\u0E3F')||(LA16_0>='\u0E46' && LA16_0<='\u0E80')||LA16_0=='\u0E83'||(LA16_0>='\u0E85' && LA16_0<='\u0E86')||LA16_0=='\u0E89'||(LA16_0>='\u0E8B' && LA16_0<='\u0E8C')||(LA16_0>='\u0E8E' && LA16_0<='\u0E93')||LA16_0=='\u0E98'||LA16_0=='\u0EA0'||LA16_0=='\u0EA4'||LA16_0=='\u0EA6'||(LA16_0>='\u0EA8' && LA16_0<='\u0EA9')||LA16_0=='\u0EAC'||LA16_0=='\u0EAF'||LA16_0=='\u0EB1'||(LA16_0>='\u0EB4' && LA16_0<='\u0EBC')||(LA16_0>='\u0EBE' && LA16_0<='\u0EBF')||(LA16_0>='\u0EC5' && LA16_0<='\u0F3F')||LA16_0=='\u0F48'||(LA16_0>='\u0F6A' && LA16_0<='\u109F')||(LA16_0>='\u10C6' && LA16_0<='\u10CF')||(LA16_0>='\u10F7' && LA16_0<='\u10FF')||LA16_0=='\u1101'||LA16_0=='\u1104'||LA16_0=='\u1108'||LA16_0=='\u110A'||LA16_0=='\u110D'||(LA16_0>='\u1113' && LA16_0<='\u113B')||LA16_0=='\u113D'||LA16_0=='\u113F'||(LA16_0>='\u1141' && LA16_0<='\u114B')||LA16_0=='\u114D'||LA16_0=='\u114F'||(LA16_0>='\u1151' && LA16_0<='\u1153')||(LA16_0>='\u1156' && LA16_0<='\u1158')||(LA16_0>='\u115A' && LA16_0<='\u115E')||LA16_0=='\u1162'||LA16_0=='\u1164'||LA16_0=='\u1166'||LA16_0=='\u1168'||(LA16_0>='\u116A' && LA16_0<='\u116C')||(LA16_0>='\u116F' && LA16_0<='\u1171')||LA16_0=='\u1174'||(LA16_0>='\u1176' && LA16_0<='\u119D')||(LA16_0>='\u119F' && LA16_0<='\u11A7')||(LA16_0>='\u11A9' && LA16_0<='\u11AA')||(LA16_0>='\u11AC' && LA16_0<='\u11AD')||(LA16_0>='\u11B0' && LA16_0<='\u11B6')||LA16_0=='\u11B9'||LA16_0=='\u11BB'||(LA16_0>='\u11C3' && LA16_0<='\u11EA')||(LA16_0>='\u11EC' && LA16_0<='\u11EF')||(LA16_0>='\u11F1' && LA16_0<='\u11F8')||(LA16_0>='\u11FA' && LA16_0<='\u1DFF')||(LA16_0>='\u1E9C' && LA16_0<='\u1E9F')||(LA16_0>='\u1EFA' && LA16_0<='\u1EFF')||(LA16_0>='\u1F16' && LA16_0<='\u1F17')||(LA16_0>='\u1F1E' && LA16_0<='\u1F1F')||(LA16_0>='\u1F46' && LA16_0<='\u1F47')||(LA16_0>='\u1F4E' && LA16_0<='\u1F4F')||LA16_0=='\u1F58'||LA16_0=='\u1F5A'||LA16_0=='\u1F5C'||LA16_0=='\u1F5E'||(LA16_0>='\u1F7E' && LA16_0<='\u1F7F')||LA16_0=='\u1FB5'||LA16_0=='\u1FBD'||(LA16_0>='\u1FBF' && LA16_0<='\u1FC1')||LA16_0=='\u1FC5'||(LA16_0>='\u1FCD' && LA16_0<='\u1FCF')||(LA16_0>='\u1FD4' && LA16_0<='\u1FD5')||(LA16_0>='\u1FDC' && LA16_0<='\u1FDF')||(LA16_0>='\u1FED' && LA16_0<='\u1FF1')||LA16_0=='\u1FF5'||(LA16_0>='\u1FFD' && LA16_0<='\u2125')||(LA16_0>='\u2127' && LA16_0<='\u2129')||(LA16_0>='\u212C' && LA16_0<='\u212D')||(LA16_0>='\u212F' && LA16_0<='\u217F')||(LA16_0>='\u2183' && LA16_0<='\u3006')||(LA16_0>='\u3008' && LA16_0<='\u3020')||(LA16_0>='\u302A' && LA16_0<='\u3040')||(LA16_0>='\u3095' && LA16_0<='\u30A0')||(LA16_0>='\u30FB' && LA16_0<='\u3104')||(LA16_0>='\u312D' && LA16_0<='\u4DFF')||(LA16_0>='\u9FA6' && LA16_0<='\uABFF')||(LA16_0>='\uD7A4' && LA16_0<='\uD7FF')||(LA16_0>='\uE000' && LA16_0<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                         
                        input.seek(index16_0);
                        if ( s>=0 ) return s;
                        break;
                    case 13 : 
                        int LA16_5 = input.LA(1);

                         
                        int index16_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_5=='{') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 24;}

                        else s = 25;

                         
                        input.seek(index16_5);
                        if ( s>=0 ) return s;
                        break;
                    case 14 : 
                        int LA16_7 = input.LA(1);

                         
                        int index16_7 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_7=='\"') && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 28;}

                        else if ( ((LA16_7>='\t' && LA16_7<='\n')||LA16_7=='\r'||(LA16_7>=' ' && LA16_7<='!')||(LA16_7>='#' && LA16_7<='%')||(LA16_7>='(' && LA16_7<=';')||(LA16_7>='=' && LA16_7<='z')||LA16_7=='|'||(LA16_7>='~' && LA16_7<='\uD7FF')||(LA16_7>='\uE000' && LA16_7<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else if ( (LA16_7=='\'') && (( !isInTag() ))) {s = 30;}

                        else s = 29;

                         
                        input.seek(index16_7);
                        if ( s>=0 ) return s;
                        break;
                    case 15 : 
                        int LA16_20 = input.LA(1);

                         
                        int index16_20 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_20>='\t' && LA16_20<='\n')||LA16_20=='\r'||(LA16_20>=' ' && LA16_20<='%')||(LA16_20>='(' && LA16_20<=';')||(LA16_20>='=' && LA16_20<='z')||LA16_20=='|'||(LA16_20>='~' && LA16_20<='\uD7FF')||(LA16_20>='\uE000' && LA16_20<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else if ( (LA16_20=='\'') && (( !isInTag() ))) {s = 30;}

                        else s = 45;

                         
                        input.seek(index16_20);
                        if ( s>=0 ) return s;
                        break;
                    case 16 : 
                        int LA16_6 = input.LA(1);

                         
                        int index16_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_6=='}') && (( !isInTag() || isInAposAttr() || isInQuotAttr() ))) {s = 26;}

                        else s = 27;

                         
                        input.seek(index16_6);
                        if ( s>=0 ) return s;
                        break;
                    case 17 : 
                        int LA16_3 = input.LA(1);

                         
                        int index16_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_3=='>') && ((( !isInTag() )||( isInAposAttr() )||(  isInTag() )||( isInQuotAttr() )))) {s = 21;}

                        else if ( ((LA16_3>='\t' && LA16_3<='\n')||LA16_3=='\r'||(LA16_3>=' ' && LA16_3<='!')||(LA16_3>='#' && LA16_3<='%')||(LA16_3>='(' && LA16_3<=';')||LA16_3=='='||(LA16_3>='?' && LA16_3<='z')||LA16_3=='|'||(LA16_3>='~' && LA16_3<='\uD7FF')||(LA16_3>='\uE000' && LA16_3<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_3=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_3=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 22;

                         
                        input.seek(index16_3);
                        if ( s>=0 ) return s;
                        break;
                    case 18 : 
                        int LA16_19 = input.LA(1);

                         
                        int index16_19 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_19>='\t' && LA16_19<='\n')||LA16_19=='\r'||(LA16_19>=' ' && LA16_19<='!')||(LA16_19>='#' && LA16_19<='%')||(LA16_19>='\'' && LA16_19<=';')||(LA16_19>='=' && LA16_19<='z')||LA16_19=='|'||(LA16_19>='~' && LA16_19<='\uD7FF')||(LA16_19>='\uE000' && LA16_19<='\uFFFD')) && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_19=='\"') && (( !isInTag() ))) {s = 30;}

                        else s = 44;

                         
                        input.seek(index16_19);
                        if ( s>=0 ) return s;
                        break;
                    case 19 : 
                        int LA16_12 = input.LA(1);

                         
                        int index16_12 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_12>='-' && LA16_12<='.')||(LA16_12>='0' && LA16_12<='9')||(LA16_12>='A' && LA16_12<='Z')||LA16_12=='_'||(LA16_12>='a' && LA16_12<='z')||LA16_12=='\u00B7'||(LA16_12>='\u00C0' && LA16_12<='\u00D6')||(LA16_12>='\u00D8' && LA16_12<='\u00F6')||(LA16_12>='\u00F8' && LA16_12<='\u037D')||(LA16_12>='\u037F' && LA16_12<='\u1FFF')||(LA16_12>='\u200C' && LA16_12<='\u200D')||(LA16_12>='\u203F' && LA16_12<='\u2040')||(LA16_12>='\u2070' && LA16_12<='\u218F')||(LA16_12>='\u2C00' && LA16_12<='\u2FEF')||(LA16_12>='\u3001' && LA16_12<='\uD7FF')||(LA16_12>='\uF900' && LA16_12<='\uFDCF')||(LA16_12>='\uFDF0' && LA16_12<='\uFFFD')) && ((( !isInTag() )||( isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 38;}

                        else if ( (LA16_12=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( ((LA16_12>='\t' && LA16_12<='\n')||LA16_12=='\r'||(LA16_12>=' ' && LA16_12<='!')||(LA16_12>='#' && LA16_12<='%')||(LA16_12>='(' && LA16_12<=',')||LA16_12=='/'||(LA16_12>=':' && LA16_12<=';')||(LA16_12>='=' && LA16_12<='@')||(LA16_12>='[' && LA16_12<='^')||LA16_12=='`'||LA16_12=='|'||(LA16_12>='~' && LA16_12<='\u00B6')||(LA16_12>='\u00B8' && LA16_12<='\u00BF')||LA16_12=='\u00D7'||LA16_12=='\u00F7'||LA16_12=='\u037E'||(LA16_12>='\u2000' && LA16_12<='\u200B')||(LA16_12>='\u200E' && LA16_12<='\u203E')||(LA16_12>='\u2041' && LA16_12<='\u206F')||(LA16_12>='\u2190' && LA16_12<='\u2BFF')||(LA16_12>='\u2FF0' && LA16_12<='\u3000')||(LA16_12>='\uE000' && LA16_12<='\uF8FF')||(LA16_12>='\uFDD0' && LA16_12<='\uFDEF')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_12=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 37;

                         
                        input.seek(index16_12);
                        if ( s>=0 ) return s;
                        break;
                    case 20 : 
                        int LA16_9 = input.LA(1);

                         
                        int index16_9 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_9>='\t' && LA16_9<='\n')||LA16_9=='\r'||(LA16_9>=' ' && LA16_9<='!')||(LA16_9>='#' && LA16_9<='%')||(LA16_9>='(' && LA16_9<=';')||(LA16_9>='=' && LA16_9<='z')||LA16_9=='|'||(LA16_9>='~' && LA16_9<='\uD7FF')||(LA16_9>='\uE000' && LA16_9<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_9=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_9=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 33;

                         
                        input.seek(index16_9);
                        if ( s>=0 ) return s;
                        break;
                    case 21 : 
                        int LA16_22 = input.LA(1);

                         
                        int index16_22 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_22);
                        if ( s>=0 ) return s;
                        break;
                    case 22 : 
                        int LA16_2 = input.LA(1);

                         
                        int index16_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_2>='\t' && LA16_2<='\n')||LA16_2=='\r'||(LA16_2>=' ' && LA16_2<='!')||(LA16_2>='#' && LA16_2<='%')||(LA16_2>='(' && LA16_2<=';')||(LA16_2>='=' && LA16_2<='z')||LA16_2=='|'||(LA16_2>='~' && LA16_2<='\uD7FF')||(LA16_2>='\uE000' && LA16_2<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_2=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_2=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 18;

                         
                        input.seek(index16_2);
                        if ( s>=0 ) return s;
                        break;
                    case 23 : 
                        int LA16_31 = input.LA(1);

                         
                        int index16_31 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_31>='\t' && LA16_31<='\n')||LA16_31=='\r'||(LA16_31>=' ' && LA16_31<='!')||(LA16_31>='#' && LA16_31<='%')||(LA16_31>='\'' && LA16_31<=';')||(LA16_31>='=' && LA16_31<='z')||LA16_31=='|'||(LA16_31>='~' && LA16_31<='\uD7FF')||(LA16_31>='\uE000' && LA16_31<='\uFFFD')) && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_31=='\"') && (( !isInTag() ))) {s = 30;}

                        else s = 50;

                         
                        input.seek(index16_31);
                        if ( s>=0 ) return s;
                        break;
                    case 24 : 
                        int LA16_13 = input.LA(1);

                         
                        int index16_13 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_13>='\t' && LA16_13<='\n')||LA16_13=='\r'||(LA16_13>=' ' && LA16_13<='!')||(LA16_13>='#' && LA16_13<='%')||(LA16_13>='(' && LA16_13<=';')||(LA16_13>='=' && LA16_13<='z')||LA16_13=='|'||(LA16_13>='~' && LA16_13<='\uD7FF')||(LA16_13>='\uE000' && LA16_13<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_13=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_13=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 22;

                         
                        input.seek(index16_13);
                        if ( s>=0 ) return s;
                        break;
                    case 25 : 
                        int LA16_4 = input.LA(1);

                         
                        int index16_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_4>='\t' && LA16_4<='\n')||LA16_4=='\r'||(LA16_4>=' ' && LA16_4<='!')||(LA16_4>='#' && LA16_4<='%')||(LA16_4>='(' && LA16_4<=';')||(LA16_4>='=' && LA16_4<='z')||LA16_4=='|'||(LA16_4>='~' && LA16_4<='\uD7FF')||(LA16_4>='\uE000' && LA16_4<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_4=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_4=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 23;

                         
                        input.seek(index16_4);
                        if ( s>=0 ) return s;
                        break;
                    case 26 : 
                        int LA16_8 = input.LA(1);

                         
                        int index16_8 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_8=='\'') && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 31;}

                        else if ( ((LA16_8>='\t' && LA16_8<='\n')||LA16_8=='\r'||(LA16_8>=' ' && LA16_8<='!')||(LA16_8>='#' && LA16_8<='%')||(LA16_8>='(' && LA16_8<=';')||(LA16_8>='=' && LA16_8<='z')||LA16_8=='|'||(LA16_8>='~' && LA16_8<='\uD7FF')||(LA16_8>='\uE000' && LA16_8<='\uFFFD')) && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( (LA16_8=='\"') && (( !isInTag() ))) {s = 30;}

                        else s = 32;

                         
                        input.seek(index16_8);
                        if ( s>=0 ) return s;
                        break;
                    case 27 : 
                        int LA16_50 = input.LA(1);

                         
                        int index16_50 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( isInAposAttr() )) ) {s = 57;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                         
                        input.seek(index16_50);
                        if ( s>=0 ) return s;
                        break;
                    case 28 : 
                        int LA16_48 = input.LA(1);

                         
                        int index16_48 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (( isInQuotAttr() )) ) {s = 56;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_48);
                        if ( s>=0 ) return s;
                        break;
                    case 29 : 
                        int LA16_38 = input.LA(1);

                         
                        int index16_38 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_38>='-' && LA16_38<='.')||(LA16_38>='0' && LA16_38<='9')||(LA16_38>='A' && LA16_38<='Z')||LA16_38=='_'||(LA16_38>='a' && LA16_38<='z')||LA16_38=='\u00B7'||(LA16_38>='\u00C0' && LA16_38<='\u00D6')||(LA16_38>='\u00D8' && LA16_38<='\u00F6')||(LA16_38>='\u00F8' && LA16_38<='\u037D')||(LA16_38>='\u037F' && LA16_38<='\u1FFF')||(LA16_38>='\u200C' && LA16_38<='\u200D')||(LA16_38>='\u203F' && LA16_38<='\u2040')||(LA16_38>='\u2070' && LA16_38<='\u218F')||(LA16_38>='\u2C00' && LA16_38<='\u2FEF')||(LA16_38>='\u3001' && LA16_38<='\uD7FF')||(LA16_38>='\uF900' && LA16_38<='\uFDCF')||(LA16_38>='\uFDF0' && LA16_38<='\uFFFD')) && ((( !isInTag() )||( isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 38;}

                        else if ( (LA16_38=='\'') && ((( !isInTag() )||( isInQuotAttr() )))) {s = 19;}

                        else if ( ((LA16_38>='\t' && LA16_38<='\n')||LA16_38=='\r'||(LA16_38>=' ' && LA16_38<='!')||(LA16_38>='#' && LA16_38<='%')||(LA16_38>='(' && LA16_38<=',')||LA16_38=='/'||(LA16_38>=':' && LA16_38<=';')||(LA16_38>='=' && LA16_38<='@')||(LA16_38>='[' && LA16_38<='^')||LA16_38=='`'||LA16_38=='|'||(LA16_38>='~' && LA16_38<='\u00B6')||(LA16_38>='\u00B8' && LA16_38<='\u00BF')||LA16_38=='\u00D7'||LA16_38=='\u00F7'||LA16_38=='\u037E'||(LA16_38>='\u2000' && LA16_38<='\u200B')||(LA16_38>='\u200E' && LA16_38<='\u203E')||(LA16_38>='\u2041' && LA16_38<='\u206F')||(LA16_38>='\u2190' && LA16_38<='\u2BFF')||(LA16_38>='\u2FF0' && LA16_38<='\u3000')||(LA16_38>='\uE000' && LA16_38<='\uF8FF')||(LA16_38>='\uFDD0' && LA16_38<='\uFDEF')) && ((( !isInTag() )||( isInAposAttr() )||( isInQuotAttr() )))) {s = 13;}

                        else if ( (LA16_38=='\"') && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else s = 37;

                         
                        input.seek(index16_38);
                        if ( s>=0 ) return s;
                        break;
                    case 30 : 
                        int LA16_29 = input.LA(1);

                         
                        int index16_29 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((  isInTag() || isInAposAttr() )) ) {s = 49;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInAposAttr() )) ) {s = 43;}

                         
                        input.seek(index16_29);
                        if ( s>=0 ) return s;
                        break;
                    case 31 : 
                        int LA16_32 = input.LA(1);

                         
                        int index16_32 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((  isInTag() || isInQuotAttr() )) ) {s = 51;}

                        else if ( (( !isInTag() )) ) {s = 30;}

                        else if ( (( isInQuotAttr() )) ) {s = 42;}

                         
                        input.seek(index16_32);
                        if ( s>=0 ) return s;
                        break;
                    case 32 : 
                        int LA16_1 = input.LA(1);

                         
                        int index16_1 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA16_1=='/') && (( !isInTag() ))) {s = 14;}

                        else if ( (LA16_1=='!') && (( !isInTag() ))) {s = 15;}

                        else if ( (LA16_1=='?') && (( !isInTag() ))) {s = 16;}

                        else s = 17;

                         
                        input.seek(index16_1);
                        if ( s>=0 ) return s;
                        break;
                    case 33 : 
                        int LA16_28 = input.LA(1);

                         
                        int index16_28 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA16_28>='\t' && LA16_28<='\n')||LA16_28=='\r'||(LA16_28>=' ' && LA16_28<='%')||(LA16_28>='(' && LA16_28<=';')||(LA16_28>='=' && LA16_28<='z')||LA16_28=='|'||(LA16_28>='~' && LA16_28<='\uD7FF')||(LA16_28>='\uE000' && LA16_28<='\uFFFD')) && ((( !isInTag() )||( isInAposAttr() )))) {s = 20;}

                        else if ( (LA16_28=='\'') && (( !isInTag() ))) {s = 30;}

                        else s = 48;

                         
                        input.seek(index16_28);
                        if ( s>=0 ) return s;
                        break;
            }
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 16, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

}