// $ANTLR 3.2 Sep 23, 2009 12:02:23 org/brackit/xquery/compiler/parser/XQuery.g 2011-05-05 13:18:31

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

public class XQueryLexer extends Lexer {
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

    // delegates
    // delegators

    public XQueryLexer() {;} 
    public XQueryLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public XQueryLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "org/brackit/xquery/compiler/parser/XQuery.g"; }

    // $ANTLR start "T__406"
    public final void mT__406() throws RecognitionException {
        try {
            int _type = T__406;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:25:8: ( ';' )
            // org/brackit/xquery/compiler/parser/XQuery.g:25:10: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__406"

    // $ANTLR start "T__407"
    public final void mT__407() throws RecognitionException {
        try {
            int _type = T__407;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:26:8: ( '%' )
            // org/brackit/xquery/compiler/parser/XQuery.g:26:10: '%'
            {
            match('%'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__407"

    // $ANTLR start "T__408"
    public final void mT__408() throws RecognitionException {
        try {
            int _type = T__408;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:27:8: ( '(' )
            // org/brackit/xquery/compiler/parser/XQuery.g:27:10: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__408"

    // $ANTLR start "T__409"
    public final void mT__409() throws RecognitionException {
        try {
            int _type = T__409;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:28:8: ( ',' )
            // org/brackit/xquery/compiler/parser/XQuery.g:28:10: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__409"

    // $ANTLR start "T__410"
    public final void mT__410() throws RecognitionException {
        try {
            int _type = T__410;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:29:8: ( ')' )
            // org/brackit/xquery/compiler/parser/XQuery.g:29:10: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__410"

    // $ANTLR start "T__411"
    public final void mT__411() throws RecognitionException {
        try {
            int _type = T__411;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:30:8: ( '$' )
            // org/brackit/xquery/compiler/parser/XQuery.g:30:10: '$'
            {
            match('$'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__411"

    // $ANTLR start "T__412"
    public final void mT__412() throws RecognitionException {
        try {
            int _type = T__412;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:31:8: ( ':=' )
            // org/brackit/xquery/compiler/parser/XQuery.g:31:10: ':='
            {
            match(":="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__412"

    // $ANTLR start "T__413"
    public final void mT__413() throws RecognitionException {
        try {
            int _type = T__413;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:32:8: ( '+' )
            // org/brackit/xquery/compiler/parser/XQuery.g:32:10: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__413"

    // $ANTLR start "T__414"
    public final void mT__414() throws RecognitionException {
        try {
            int _type = T__414;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:33:8: ( '-' )
            // org/brackit/xquery/compiler/parser/XQuery.g:33:10: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__414"

    // $ANTLR start "T__415"
    public final void mT__415() throws RecognitionException {
        try {
            int _type = T__415;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:34:8: ( '*' )
            // org/brackit/xquery/compiler/parser/XQuery.g:34:10: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__415"

    // $ANTLR start "T__416"
    public final void mT__416() throws RecognitionException {
        try {
            int _type = T__416;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:35:8: ( '|' )
            // org/brackit/xquery/compiler/parser/XQuery.g:35:10: '|'
            {
            match('|'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__416"

    // $ANTLR start "T__417"
    public final void mT__417() throws RecognitionException {
        try {
            int _type = T__417;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:36:8: ( '!=' )
            // org/brackit/xquery/compiler/parser/XQuery.g:36:10: '!='
            {
            match("!="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__417"

    // $ANTLR start "T__418"
    public final void mT__418() throws RecognitionException {
        try {
            int _type = T__418;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:37:8: ( '<=' )
            // org/brackit/xquery/compiler/parser/XQuery.g:37:10: '<='
            {
            match("<="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__418"

    // $ANTLR start "T__419"
    public final void mT__419() throws RecognitionException {
        try {
            int _type = T__419;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:38:8: ( '>=' )
            // org/brackit/xquery/compiler/parser/XQuery.g:38:10: '>='
            {
            match(">="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__419"

    // $ANTLR start "T__420"
    public final void mT__420() throws RecognitionException {
        try {
            int _type = T__420;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:39:8: ( '<<' )
            // org/brackit/xquery/compiler/parser/XQuery.g:39:10: '<<'
            {
            match("<<"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__420"

    // $ANTLR start "T__421"
    public final void mT__421() throws RecognitionException {
        try {
            int _type = T__421;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:40:8: ( '>>' )
            // org/brackit/xquery/compiler/parser/XQuery.g:40:10: '>>'
            {
            match(">>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__421"

    // $ANTLR start "T__422"
    public final void mT__422() throws RecognitionException {
        try {
            int _type = T__422;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:41:8: ( '/' )
            // org/brackit/xquery/compiler/parser/XQuery.g:41:10: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__422"

    // $ANTLR start "T__423"
    public final void mT__423() throws RecognitionException {
        try {
            int _type = T__423;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:42:8: ( '//' )
            // org/brackit/xquery/compiler/parser/XQuery.g:42:10: '//'
            {
            match("//"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__423"

    // $ANTLR start "T__424"
    public final void mT__424() throws RecognitionException {
        try {
            int _type = T__424;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:43:8: ( '::' )
            // org/brackit/xquery/compiler/parser/XQuery.g:43:10: '::'
            {
            match("::"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__424"

    // $ANTLR start "T__425"
    public final void mT__425() throws RecognitionException {
        try {
            int _type = T__425;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:44:8: ( '@' )
            // org/brackit/xquery/compiler/parser/XQuery.g:44:10: '@'
            {
            match('@'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__425"

    // $ANTLR start "T__426"
    public final void mT__426() throws RecognitionException {
        try {
            int _type = T__426;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:45:8: ( '..' )
            // org/brackit/xquery/compiler/parser/XQuery.g:45:10: '..'
            {
            match(".."); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__426"

    // $ANTLR start "T__427"
    public final void mT__427() throws RecognitionException {
        try {
            int _type = T__427;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:46:8: ( '[' )
            // org/brackit/xquery/compiler/parser/XQuery.g:46:10: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__427"

    // $ANTLR start "T__428"
    public final void mT__428() throws RecognitionException {
        try {
            int _type = T__428;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:47:8: ( ']' )
            // org/brackit/xquery/compiler/parser/XQuery.g:47:10: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__428"

    // $ANTLR start "T__429"
    public final void mT__429() throws RecognitionException {
        try {
            int _type = T__429;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:48:8: ( '.' )
            // org/brackit/xquery/compiler/parser/XQuery.g:48:10: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__429"

    // $ANTLR start "T__430"
    public final void mT__430() throws RecognitionException {
        try {
            int _type = T__430;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:49:8: ( '?' )
            // org/brackit/xquery/compiler/parser/XQuery.g:49:10: '?'
            {
            match('?'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__430"

    // $ANTLR start "LAngle"
    public final void mLAngle() throws RecognitionException {
        try {
            int _type = LAngle;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2076:25: ( '<' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2076:27: '<'
            {
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2077:25: ( '>' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2077:27: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RAngle"

    // $ANTLR start "LCurly"
    public final void mLCurly() throws RecognitionException {
        try {
            int _type = LCurly;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2078:25: ( '{' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2078:27: '{'
            {
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2079:25: ( '}' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2079:27: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RCurly"

    // $ANTLR start "SymEq"
    public final void mSymEq() throws RecognitionException {
        try {
            int _type = SymEq;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2080:25: ( '=' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2080:27: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SymEq"

    // $ANTLR start "Colon"
    public final void mColon() throws RecognitionException {
        try {
            int _type = Colon;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2081:25: ( ':' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2081:27: ':'
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

    // $ANTLR start "LClose"
    public final void mLClose() throws RecognitionException {
        try {
            int _type = LClose;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2082:25: ( '</' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2082:27: '</'
            {
            match("</"); 


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
            // org/brackit/xquery/compiler/parser/XQuery.g:2083:25: ( '/>' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2083:27: '/>'
            {
            match("/>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RClose"

    // $ANTLR start "Quot"
    public final void mQuot() throws RecognitionException {
        try {
            int _type = Quot;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2084:25: ( '\"' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2084:27: '\"'
            {
            match('\"'); 

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
            // org/brackit/xquery/compiler/parser/XQuery.g:2085:25: ( '\\'' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2085:27: '\\''
            {
            match('\''); 

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
            // org/brackit/xquery/compiler/parser/XQuery.g:2087:25: ( '\"\"' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2087:27: '\"\"'
            {
            match("\"\""); 


            }

        }
        finally {
        }
    }
    // $ANTLR end "EscapeQuot"

    // $ANTLR start "EscapeApos"
    public final void mEscapeApos() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2089:25: ( '\\'\\'' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2089:27: '\\'\\''
            {
            match("''"); 


            }

        }
        finally {
        }
    }
    // $ANTLR end "EscapeApos"

    // $ANTLR start "EscapeLCurly"
    public final void mEscapeLCurly() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2091:25: ( '{{' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2091:27: '{{'
            {
            match("{{"); 


            }

        }
        finally {
        }
    }
    // $ANTLR end "EscapeLCurly"

    // $ANTLR start "EscapeRCurly"
    public final void mEscapeRCurly() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2093:25: ( '}}' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2093:27: '}}'
            {
            match("}}"); 


            }

        }
        finally {
        }
    }
    // $ANTLR end "EscapeRCurly"

    // $ANTLR start "ANCESTOR"
    public final void mANCESTOR() throws RecognitionException {
        try {
            int _type = ANCESTOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2095:25: ( 'ancestor' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2095:27: 'ancestor'
            {
            match("ancestor"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ANCESTOR"

    // $ANTLR start "ANCESTOR_OR_SELF"
    public final void mANCESTOR_OR_SELF() throws RecognitionException {
        try {
            int _type = ANCESTOR_OR_SELF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2096:25: ( 'ancestor-or-self' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2096:27: 'ancestor-or-self'
            {
            match("ancestor-or-self"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ANCESTOR_OR_SELF"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2097:25: ( 'and' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2097:27: 'and'
            {
            match("and"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "AS"
    public final void mAS() throws RecognitionException {
        try {
            int _type = AS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2098:25: ( 'as' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2098:27: 'as'
            {
            match("as"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AS"

    // $ANTLR start "ASCENDING"
    public final void mASCENDING() throws RecognitionException {
        try {
            int _type = ASCENDING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2099:25: ( 'ascending' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2099:27: 'ascending'
            {
            match("ascending"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ASCENDING"

    // $ANTLR start "AT"
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2100:25: ( 'at' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2100:27: 'at'
            {
            match("at"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AT"

    // $ANTLR start "ATTRIBUTE"
    public final void mATTRIBUTE() throws RecognitionException {
        try {
            int _type = ATTRIBUTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2101:25: ( 'attribute' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2101:27: 'attribute'
            {
            match("attribute"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ATTRIBUTE"

    // $ANTLR start "BASE_URI"
    public final void mBASE_URI() throws RecognitionException {
        try {
            int _type = BASE_URI;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2102:25: ( 'base-uri' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2102:27: 'base-uri'
            {
            match("base-uri"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BASE_URI"

    // $ANTLR start "BOUNDARY_SPACE"
    public final void mBOUNDARY_SPACE() throws RecognitionException {
        try {
            int _type = BOUNDARY_SPACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2103:25: ( 'boundary-space' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2103:27: 'boundary-space'
            {
            match("boundary-space"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BOUNDARY_SPACE"

    // $ANTLR start "BY"
    public final void mBY() throws RecognitionException {
        try {
            int _type = BY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2104:25: ( 'by' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2104:27: 'by'
            {
            match("by"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BY"

    // $ANTLR start "CASE"
    public final void mCASE() throws RecognitionException {
        try {
            int _type = CASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2105:25: ( 'case' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2105:27: 'case'
            {
            match("case"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CASE"

    // $ANTLR start "CASTABLE"
    public final void mCASTABLE() throws RecognitionException {
        try {
            int _type = CASTABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2106:25: ( 'castable' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2106:27: 'castable'
            {
            match("castable"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CASTABLE"

    // $ANTLR start "CAST"
    public final void mCAST() throws RecognitionException {
        try {
            int _type = CAST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2107:25: ( 'cast' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2107:27: 'cast'
            {
            match("cast"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CAST"

    // $ANTLR start "CHILD"
    public final void mCHILD() throws RecognitionException {
        try {
            int _type = CHILD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2108:25: ( 'child' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2108:27: 'child'
            {
            match("child"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CHILD"

    // $ANTLR start "COLLATION"
    public final void mCOLLATION() throws RecognitionException {
        try {
            int _type = COLLATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2109:25: ( 'collation' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2109:27: 'collation'
            {
            match("collation"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COLLATION"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2110:25: ( 'comment' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2110:27: 'comment'
            {
            match("comment"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "CONSTRUCTION"
    public final void mCONSTRUCTION() throws RecognitionException {
        try {
            int _type = CONSTRUCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2111:25: ( 'construction' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2111:27: 'construction'
            {
            match("construction"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CONSTRUCTION"

    // $ANTLR start "COPY"
    public final void mCOPY() throws RecognitionException {
        try {
            int _type = COPY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2112:25: ( 'copy' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2112:27: 'copy'
            {
            match("copy"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COPY"

    // $ANTLR start "COPY_NAMESPACES"
    public final void mCOPY_NAMESPACES() throws RecognitionException {
        try {
            int _type = COPY_NAMESPACES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2113:25: ( 'copy-namespaces' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2113:27: 'copy-namespaces'
            {
            match("copy-namespaces"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COPY_NAMESPACES"

    // $ANTLR start "DECLARE"
    public final void mDECLARE() throws RecognitionException {
        try {
            int _type = DECLARE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2114:25: ( 'declare' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2114:27: 'declare'
            {
            match("declare"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DECLARE"

    // $ANTLR start "DEFAULT"
    public final void mDEFAULT() throws RecognitionException {
        try {
            int _type = DEFAULT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2115:25: ( 'default' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2115:27: 'default'
            {
            match("default"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DEFAULT"

    // $ANTLR start "DESCENDANT"
    public final void mDESCENDANT() throws RecognitionException {
        try {
            int _type = DESCENDANT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2116:25: ( 'descendant' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2116:27: 'descendant'
            {
            match("descendant"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DESCENDANT"

    // $ANTLR start "DESCENDANT_OR_SELF"
    public final void mDESCENDANT_OR_SELF() throws RecognitionException {
        try {
            int _type = DESCENDANT_OR_SELF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2117:25: ( 'descendant-or-self' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2117:27: 'descendant-or-self'
            {
            match("descendant-or-self"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DESCENDANT_OR_SELF"

    // $ANTLR start "DESCENDING"
    public final void mDESCENDING() throws RecognitionException {
        try {
            int _type = DESCENDING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2118:25: ( 'descending' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2118:27: 'descending'
            {
            match("descending"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DESCENDING"

    // $ANTLR start "DIV"
    public final void mDIV() throws RecognitionException {
        try {
            int _type = DIV;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2119:25: ( 'div' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2119:27: 'div'
            {
            match("div"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIV"

    // $ANTLR start "DOCUMENT"
    public final void mDOCUMENT() throws RecognitionException {
        try {
            int _type = DOCUMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2120:25: ( 'document' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2120:27: 'document'
            {
            match("document"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DOCUMENT"

    // $ANTLR start "DOCUMENT_NODE"
    public final void mDOCUMENT_NODE() throws RecognitionException {
        try {
            int _type = DOCUMENT_NODE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2121:25: ( 'document-node' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2121:27: 'document-node'
            {
            match("document-node"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DOCUMENT_NODE"

    // $ANTLR start "ELEMENT"
    public final void mELEMENT() throws RecognitionException {
        try {
            int _type = ELEMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2122:25: ( 'element' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2122:27: 'element'
            {
            match("element"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ELEMENT"

    // $ANTLR start "ELSE"
    public final void mELSE() throws RecognitionException {
        try {
            int _type = ELSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2123:25: ( 'else' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2123:27: 'else'
            {
            match("else"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ELSE"

    // $ANTLR start "EMPTY"
    public final void mEMPTY() throws RecognitionException {
        try {
            int _type = EMPTY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2124:25: ( 'empty' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2124:27: 'empty'
            {
            match("empty"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EMPTY"

    // $ANTLR start "EMPTY_SEQUENCE"
    public final void mEMPTY_SEQUENCE() throws RecognitionException {
        try {
            int _type = EMPTY_SEQUENCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2125:25: ( 'empty-sequence' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2125:27: 'empty-sequence'
            {
            match("empty-sequence"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EMPTY_SEQUENCE"

    // $ANTLR start "ENCODING"
    public final void mENCODING() throws RecognitionException {
        try {
            int _type = ENCODING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2126:25: ( 'encoding' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2126:27: 'encoding'
            {
            match("encoding"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ENCODING"

    // $ANTLR start "EQ"
    public final void mEQ() throws RecognitionException {
        try {
            int _type = EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2127:25: ( 'eq' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2127:27: 'eq'
            {
            match("eq"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EQ"

    // $ANTLR start "EVERY"
    public final void mEVERY() throws RecognitionException {
        try {
            int _type = EVERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2128:25: ( 'every' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2128:27: 'every'
            {
            match("every"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EVERY"

    // $ANTLR start "EXCEPT"
    public final void mEXCEPT() throws RecognitionException {
        try {
            int _type = EXCEPT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2129:25: ( 'except' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2129:27: 'except'
            {
            match("except"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXCEPT"

    // $ANTLR start "EXTERNAL"
    public final void mEXTERNAL() throws RecognitionException {
        try {
            int _type = EXTERNAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2130:25: ( 'external' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2130:27: 'external'
            {
            match("external"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXTERNAL"

    // $ANTLR start "FOLLOWING"
    public final void mFOLLOWING() throws RecognitionException {
        try {
            int _type = FOLLOWING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2131:25: ( 'following' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2131:27: 'following'
            {
            match("following"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FOLLOWING"

    // $ANTLR start "FOLLOWING_SIBLING"
    public final void mFOLLOWING_SIBLING() throws RecognitionException {
        try {
            int _type = FOLLOWING_SIBLING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2132:25: ( 'following-sibling' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2132:27: 'following-sibling'
            {
            match("following-sibling"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FOLLOWING_SIBLING"

    // $ANTLR start "FOR"
    public final void mFOR() throws RecognitionException {
        try {
            int _type = FOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2133:25: ( 'for' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2133:27: 'for'
            {
            match("for"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FOR"

    // $ANTLR start "FUNCTION"
    public final void mFUNCTION() throws RecognitionException {
        try {
            int _type = FUNCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2134:25: ( 'function' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2134:27: 'function'
            {
            match("function"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FUNCTION"

    // $ANTLR start "GE"
    public final void mGE() throws RecognitionException {
        try {
            int _type = GE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2135:25: ( 'ge' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2135:27: 'ge'
            {
            match("ge"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GE"

    // $ANTLR start "GREATEST"
    public final void mGREATEST() throws RecognitionException {
        try {
            int _type = GREATEST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2136:25: ( 'greatest' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2136:27: 'greatest'
            {
            match("greatest"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GREATEST"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2137:25: ( 'gt' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2137:27: 'gt'
            {
            match("gt"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "IDIV"
    public final void mIDIV() throws RecognitionException {
        try {
            int _type = IDIV;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2138:25: ( 'idiv' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2138:27: 'idiv'
            {
            match("idiv"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IDIV"

    // $ANTLR start "IF"
    public final void mIF() throws RecognitionException {
        try {
            int _type = IF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2139:25: ( 'if' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2139:27: 'if'
            {
            match("if"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IF"

    // $ANTLR start "IMPORT"
    public final void mIMPORT() throws RecognitionException {
        try {
            int _type = IMPORT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2140:25: ( 'import' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2140:27: 'import'
            {
            match("import"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IMPORT"

    // $ANTLR start "INHERIT"
    public final void mINHERIT() throws RecognitionException {
        try {
            int _type = INHERIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2141:25: ( 'inherit' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2141:27: 'inherit'
            {
            match("inherit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INHERIT"

    // $ANTLR start "IN"
    public final void mIN() throws RecognitionException {
        try {
            int _type = IN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2142:25: ( 'in' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2142:27: 'in'
            {
            match("in"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IN"

    // $ANTLR start "INSTANCE"
    public final void mINSTANCE() throws RecognitionException {
        try {
            int _type = INSTANCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2143:25: ( 'instance' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2143:27: 'instance'
            {
            match("instance"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INSTANCE"

    // $ANTLR start "INTERSECT"
    public final void mINTERSECT() throws RecognitionException {
        try {
            int _type = INTERSECT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2144:25: ( 'intersect' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2144:27: 'intersect'
            {
            match("intersect"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INTERSECT"

    // $ANTLR start "IS"
    public final void mIS() throws RecognitionException {
        try {
            int _type = IS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2145:25: ( 'is' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2145:27: 'is'
            {
            match("is"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IS"

    // $ANTLR start "ITEM"
    public final void mITEM() throws RecognitionException {
        try {
            int _type = ITEM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2146:25: ( 'item' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2146:27: 'item'
            {
            match("item"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ITEM"

    // $ANTLR start "LAX"
    public final void mLAX() throws RecognitionException {
        try {
            int _type = LAX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2147:25: ( 'lax' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2147:27: 'lax'
            {
            match("lax"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LAX"

    // $ANTLR start "LEAST"
    public final void mLEAST() throws RecognitionException {
        try {
            int _type = LEAST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2148:25: ( 'least' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2148:27: 'least'
            {
            match("least"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LEAST"

    // $ANTLR start "LE"
    public final void mLE() throws RecognitionException {
        try {
            int _type = LE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2149:25: ( 'le' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2149:27: 'le'
            {
            match("le"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LE"

    // $ANTLR start "LET"
    public final void mLET() throws RecognitionException {
        try {
            int _type = LET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2150:25: ( 'let' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2150:27: 'let'
            {
            match("let"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LET"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2151:25: ( 'lt' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2151:27: 'lt'
            {
            match("lt"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "MOD"
    public final void mMOD() throws RecognitionException {
        try {
            int _type = MOD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2152:25: ( 'mod' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2152:27: 'mod'
            {
            match("mod"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MOD"

    // $ANTLR start "MODULE"
    public final void mMODULE() throws RecognitionException {
        try {
            int _type = MODULE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2153:25: ( 'module' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2153:27: 'module'
            {
            match("module"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MODULE"

    // $ANTLR start "NAMESPACE"
    public final void mNAMESPACE() throws RecognitionException {
        try {
            int _type = NAMESPACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2154:25: ( 'namespace' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2154:27: 'namespace'
            {
            match("namespace"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NAMESPACE"

    // $ANTLR start "NE"
    public final void mNE() throws RecognitionException {
        try {
            int _type = NE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2155:25: ( 'ne' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2155:27: 'ne'
            {
            match("ne"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NE"

    // $ANTLR start "NODE"
    public final void mNODE() throws RecognitionException {
        try {
            int _type = NODE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2156:25: ( 'node' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2156:27: 'node'
            {
            match("node"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NODE"

    // $ANTLR start "ANYKIND"
    public final void mANYKIND() throws RecognitionException {
        try {
            int _type = ANYKIND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2157:10: ( 'node()' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2157:12: 'node()'
            {
            match("node()"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ANYKIND"

    // $ANTLR start "NO_INHERIT"
    public final void mNO_INHERIT() throws RecognitionException {
        try {
            int _type = NO_INHERIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2158:25: ( 'no-inherit' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2158:27: 'no-inherit'
            {
            match("no-inherit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NO_INHERIT"

    // $ANTLR start "NO_PRESERVE"
    public final void mNO_PRESERVE() throws RecognitionException {
        try {
            int _type = NO_PRESERVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2159:25: ( 'no-preserve' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2159:27: 'no-preserve'
            {
            match("no-preserve"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NO_PRESERVE"

    // $ANTLR start "OF"
    public final void mOF() throws RecognitionException {
        try {
            int _type = OF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2160:25: ( 'of' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2160:27: 'of'
            {
            match("of"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OF"

    // $ANTLR start "OPTION"
    public final void mOPTION() throws RecognitionException {
        try {
            int _type = OPTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2161:25: ( 'option' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2161:27: 'option'
            {
            match("option"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OPTION"

    // $ANTLR start "ORDERED"
    public final void mORDERED() throws RecognitionException {
        try {
            int _type = ORDERED;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2162:25: ( 'ordered' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2162:27: 'ordered'
            {
            match("ordered"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ORDERED"

    // $ANTLR start "ORDERING"
    public final void mORDERING() throws RecognitionException {
        try {
            int _type = ORDERING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2163:25: ( 'ordering' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2163:27: 'ordering'
            {
            match("ordering"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ORDERING"

    // $ANTLR start "ORDER"
    public final void mORDER() throws RecognitionException {
        try {
            int _type = ORDER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2164:25: ( 'order' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2164:27: 'order'
            {
            match("order"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ORDER"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2165:25: ( 'or' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2165:27: 'or'
            {
            match("or"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "PARENT"
    public final void mPARENT() throws RecognitionException {
        try {
            int _type = PARENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2166:25: ( 'parent' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2166:27: 'parent'
            {
            match("parent"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARENT"

    // $ANTLR start "PRECEDING"
    public final void mPRECEDING() throws RecognitionException {
        try {
            int _type = PRECEDING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2167:25: ( 'preceding' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2167:27: 'preceding'
            {
            match("preceding"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PRECEDING"

    // $ANTLR start "PRECEDING_SIBLING"
    public final void mPRECEDING_SIBLING() throws RecognitionException {
        try {
            int _type = PRECEDING_SIBLING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2168:25: ( 'preceding-sibling' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2168:27: 'preceding-sibling'
            {
            match("preceding-sibling"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PRECEDING_SIBLING"

    // $ANTLR start "PRESERVE"
    public final void mPRESERVE() throws RecognitionException {
        try {
            int _type = PRESERVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2169:25: ( 'preserve' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2169:27: 'preserve'
            {
            match("preserve"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PRESERVE"

    // $ANTLR start "PROCESSING_INSTRUCTION"
    public final void mPROCESSING_INSTRUCTION() throws RecognitionException {
        try {
            int _type = PROCESSING_INSTRUCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2170:25: ( 'processing-instruction' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2170:27: 'processing-instruction'
            {
            match("processing-instruction"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PROCESSING_INSTRUCTION"

    // $ANTLR start "RETURN"
    public final void mRETURN() throws RecognitionException {
        try {
            int _type = RETURN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2171:25: ( 'return' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2171:27: 'return'
            {
            match("return"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RETURN"

    // $ANTLR start "SATISFIES"
    public final void mSATISFIES() throws RecognitionException {
        try {
            int _type = SATISFIES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2172:25: ( 'satisfies' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2172:27: 'satisfies'
            {
            match("satisfies"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SATISFIES"

    // $ANTLR start "SCHEMA_ATTRIBUTE"
    public final void mSCHEMA_ATTRIBUTE() throws RecognitionException {
        try {
            int _type = SCHEMA_ATTRIBUTE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2173:25: ( 'schema-attribute' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2173:27: 'schema-attribute'
            {
            match("schema-attribute"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SCHEMA_ATTRIBUTE"

    // $ANTLR start "SCHEMA_ELEMENT"
    public final void mSCHEMA_ELEMENT() throws RecognitionException {
        try {
            int _type = SCHEMA_ELEMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2174:25: ( 'schema-element' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2174:27: 'schema-element'
            {
            match("schema-element"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SCHEMA_ELEMENT"

    // $ANTLR start "SCHEMA"
    public final void mSCHEMA() throws RecognitionException {
        try {
            int _type = SCHEMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2175:25: ( 'schema' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2175:27: 'schema'
            {
            match("schema"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SCHEMA"

    // $ANTLR start "SELF"
    public final void mSELF() throws RecognitionException {
        try {
            int _type = SELF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2176:25: ( 'self' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2176:27: 'self'
            {
            match("self"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SELF"

    // $ANTLR start "SIMPLE"
    public final void mSIMPLE() throws RecognitionException {
        try {
            int _type = SIMPLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2177:25: ( 'simple' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2177:27: 'simple'
            {
            match("simple"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SIMPLE"

    // $ANTLR start "SOME"
    public final void mSOME() throws RecognitionException {
        try {
            int _type = SOME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2178:25: ( 'some' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2178:27: 'some'
            {
            match("some"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SOME"

    // $ANTLR start "STABLE"
    public final void mSTABLE() throws RecognitionException {
        try {
            int _type = STABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2179:25: ( 'stable' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2179:27: 'stable'
            {
            match("stable"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STABLE"

    // $ANTLR start "STRICT"
    public final void mSTRICT() throws RecognitionException {
        try {
            int _type = STRICT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2180:25: ( 'strict' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2180:27: 'strict'
            {
            match("strict"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRICT"

    // $ANTLR start "STRIP"
    public final void mSTRIP() throws RecognitionException {
        try {
            int _type = STRIP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2181:25: ( 'strip' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2181:27: 'strip'
            {
            match("strip"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRIP"

    // $ANTLR start "TEXT"
    public final void mTEXT() throws RecognitionException {
        try {
            int _type = TEXT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2182:25: ( 'text' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2182:27: 'text'
            {
            match("text"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TEXT"

    // $ANTLR start "THEN"
    public final void mTHEN() throws RecognitionException {
        try {
            int _type = THEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2183:25: ( 'then' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2183:27: 'then'
            {
            match("then"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "THEN"

    // $ANTLR start "TO"
    public final void mTO() throws RecognitionException {
        try {
            int _type = TO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2184:25: ( 'to' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2184:27: 'to'
            {
            match("to"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TO"

    // $ANTLR start "TREAT"
    public final void mTREAT() throws RecognitionException {
        try {
            int _type = TREAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2185:25: ( 'treat' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2185:27: 'treat'
            {
            match("treat"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TREAT"

    // $ANTLR start "TYPESWITCH"
    public final void mTYPESWITCH() throws RecognitionException {
        try {
            int _type = TYPESWITCH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2186:25: ( 'typeswitch' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2186:27: 'typeswitch'
            {
            match("typeswitch"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TYPESWITCH"

    // $ANTLR start "UNION"
    public final void mUNION() throws RecognitionException {
        try {
            int _type = UNION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2187:25: ( 'union' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2187:27: 'union'
            {
            match("union"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UNION"

    // $ANTLR start "UNORDERED"
    public final void mUNORDERED() throws RecognitionException {
        try {
            int _type = UNORDERED;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2188:25: ( 'unordered' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2188:27: 'unordered'
            {
            match("unordered"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UNORDERED"

    // $ANTLR start "VALIDATE"
    public final void mVALIDATE() throws RecognitionException {
        try {
            int _type = VALIDATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2189:25: ( 'validate' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2189:27: 'validate'
            {
            match("validate"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "VALIDATE"

    // $ANTLR start "VARIABLE"
    public final void mVARIABLE() throws RecognitionException {
        try {
            int _type = VARIABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2190:25: ( 'variable' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2190:27: 'variable'
            {
            match("variable"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "VARIABLE"

    // $ANTLR start "VERSION"
    public final void mVERSION() throws RecognitionException {
        try {
            int _type = VERSION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2191:25: ( 'version' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2191:27: 'version'
            {
            match("version"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "VERSION"

    // $ANTLR start "WHERE"
    public final void mWHERE() throws RecognitionException {
        try {
            int _type = WHERE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2192:25: ( 'where' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2192:27: 'where'
            {
            match("where"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WHERE"

    // $ANTLR start "XQUERY"
    public final void mXQUERY() throws RecognitionException {
        try {
            int _type = XQUERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2193:25: ( 'xquery' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2193:27: 'xquery'
            {
            match("xquery"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "XQUERY"

    // $ANTLR start "AFTER"
    public final void mAFTER() throws RecognitionException {
        try {
            int _type = AFTER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2195:25: ( 'after' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2195:27: 'after'
            {
            match("after"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AFTER"

    // $ANTLR start "BEFORE"
    public final void mBEFORE() throws RecognitionException {
        try {
            int _type = BEFORE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2196:25: ( 'before' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2196:27: 'before'
            {
            match("before"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BEFORE"

    // $ANTLR start "DELETE"
    public final void mDELETE() throws RecognitionException {
        try {
            int _type = DELETE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2197:25: ( 'delete' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2197:27: 'delete'
            {
            match("delete"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DELETE"

    // $ANTLR start "FIRST"
    public final void mFIRST() throws RecognitionException {
        try {
            int _type = FIRST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2198:25: ( 'first' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2198:27: 'first'
            {
            match("first"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FIRST"

    // $ANTLR start "INSERT"
    public final void mINSERT() throws RecognitionException {
        try {
            int _type = INSERT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2199:25: ( 'insert' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2199:27: 'insert'
            {
            match("insert"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INSERT"

    // $ANTLR start "INTO"
    public final void mINTO() throws RecognitionException {
        try {
            int _type = INTO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2200:25: ( 'into' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2200:27: 'into'
            {
            match("into"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INTO"

    // $ANTLR start "LAST"
    public final void mLAST() throws RecognitionException {
        try {
            int _type = LAST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2201:25: ( 'last' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2201:27: 'last'
            {
            match("last"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LAST"

    // $ANTLR start "MODIFY"
    public final void mMODIFY() throws RecognitionException {
        try {
            int _type = MODIFY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2202:25: ( 'modify' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2202:27: 'modify'
            {
            match("modify"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MODIFY"

    // $ANTLR start "NODES"
    public final void mNODES() throws RecognitionException {
        try {
            int _type = NODES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2203:25: ( 'nodes' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2203:27: 'nodes'
            {
            match("nodes"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NODES"

    // $ANTLR start "RENAME"
    public final void mRENAME() throws RecognitionException {
        try {
            int _type = RENAME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2204:25: ( 'rename' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2204:27: 'rename'
            {
            match("rename"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RENAME"

    // $ANTLR start "REPLACE"
    public final void mREPLACE() throws RecognitionException {
        try {
            int _type = REPLACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2205:25: ( 'replace' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2205:27: 'replace'
            {
            match("replace"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "REPLACE"

    // $ANTLR start "REVALIDATION"
    public final void mREVALIDATION() throws RecognitionException {
        try {
            int _type = REVALIDATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2206:25: ( 'revalidation' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2206:27: 'revalidation'
            {
            match("revalidation"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "REVALIDATION"

    // $ANTLR start "SKIP"
    public final void mSKIP() throws RecognitionException {
        try {
            int _type = SKIP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2207:25: ( 'skip' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2207:27: 'skip'
            {
            match("skip"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SKIP"

    // $ANTLR start "UPDATING"
    public final void mUPDATING() throws RecognitionException {
        try {
            int _type = UPDATING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2208:25: ( 'updating' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2208:27: 'updating'
            {
            match("updating"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UPDATING"

    // $ANTLR start "VALUE"
    public final void mVALUE() throws RecognitionException {
        try {
            int _type = VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2209:25: ( 'value' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2209:27: 'value'
            {
            match("value"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "VALUE"

    // $ANTLR start "WITH"
    public final void mWITH() throws RecognitionException {
        try {
            int _type = WITH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2210:25: ( 'with' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2210:27: 'with'
            {
            match("with"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WITH"

    // $ANTLR start "BLOCK"
    public final void mBLOCK() throws RecognitionException {
        try {
            int _type = BLOCK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2213:25: ( 'block' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2213:27: 'block'
            {
            match("block"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BLOCK"

    // $ANTLR start "CONSTANT"
    public final void mCONSTANT() throws RecognitionException {
        try {
            int _type = CONSTANT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2214:25: ( 'constant' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2214:27: 'constant'
            {
            match("constant"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CONSTANT"

    // $ANTLR start "EXIT"
    public final void mEXIT() throws RecognitionException {
        try {
            int _type = EXIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2215:25: ( 'exit' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2215:27: 'exit'
            {
            match("exit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXIT"

    // $ANTLR start "SEQUENTIAL"
    public final void mSEQUENTIAL() throws RecognitionException {
        try {
            int _type = SEQUENTIAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2216:25: ( 'sequential' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2216:27: 'sequential'
            {
            match("sequential"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SEQUENTIAL"

    // $ANTLR start "RETURNING"
    public final void mRETURNING() throws RecognitionException {
        try {
            int _type = RETURNING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2217:25: ( 'returning' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2217:27: 'returning'
            {
            match("returning"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RETURNING"

    // $ANTLR start "SET"
    public final void mSET() throws RecognitionException {
        try {
            int _type = SET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2218:25: ( 'set' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2218:27: 'set'
            {
            match("set"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SET"

    // $ANTLR start "WHILE"
    public final void mWHILE() throws RecognitionException {
        try {
            int _type = WHILE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2219:25: ( 'while' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2219:27: 'while'
            {
            match("while"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WHILE"

    // $ANTLR start "ALL"
    public final void mALL() throws RecognitionException {
        try {
            int _type = ALL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2222:25: ( 'all' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2222:27: 'all'
            {
            match("all"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ALL"

    // $ANTLR start "ANY"
    public final void mANY() throws RecognitionException {
        try {
            int _type = ANY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2223:25: ( 'any' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2223:27: 'any'
            {
            match("any"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ANY"

    // $ANTLR start "CONTENT"
    public final void mCONTENT() throws RecognitionException {
        try {
            int _type = CONTENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2224:25: ( 'content' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2224:27: 'content'
            {
            match("content"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CONTENT"

    // $ANTLR start "DIACRITICS"
    public final void mDIACRITICS() throws RecognitionException {
        try {
            int _type = DIACRITICS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2225:25: ( 'diacritics' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2225:27: 'diacritics'
            {
            match("diacritics"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIACRITICS"

    // $ANTLR start "DIFFERENT"
    public final void mDIFFERENT() throws RecognitionException {
        try {
            int _type = DIFFERENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2226:25: ( 'different' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2226:27: 'different'
            {
            match("different"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIFFERENT"

    // $ANTLR start "DISTANCE"
    public final void mDISTANCE() throws RecognitionException {
        try {
            int _type = DISTANCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2227:25: ( 'distance' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2227:27: 'distance'
            {
            match("distance"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DISTANCE"

    // $ANTLR start "END"
    public final void mEND() throws RecognitionException {
        try {
            int _type = END;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2228:25: ( 'end' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2228:27: 'end'
            {
            match("end"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "END"

    // $ANTLR start "ENTIRE"
    public final void mENTIRE() throws RecognitionException {
        try {
            int _type = ENTIRE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2229:25: ( 'entire' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2229:27: 'entire'
            {
            match("entire"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ENTIRE"

    // $ANTLR start "EXACTLY"
    public final void mEXACTLY() throws RecognitionException {
        try {
            int _type = EXACTLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2230:25: ( 'exactly' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2230:27: 'exactly'
            {
            match("exactly"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXACTLY"

    // $ANTLR start "FROM"
    public final void mFROM() throws RecognitionException {
        try {
            int _type = FROM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2231:25: ( 'from' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2231:27: 'from'
            {
            match("from"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FROM"

    // $ANTLR start "FTAND"
    public final void mFTAND() throws RecognitionException {
        try {
            int _type = FTAND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2232:25: ( 'ftand' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2232:27: 'ftand'
            {
            match("ftand"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FTAND"

    // $ANTLR start "CONTAINS"
    public final void mCONTAINS() throws RecognitionException {
        try {
            int _type = CONTAINS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2233:25: ( 'contains' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2233:27: 'contains'
            {
            match("contains"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CONTAINS"

    // $ANTLR start "FTNOT"
    public final void mFTNOT() throws RecognitionException {
        try {
            int _type = FTNOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2234:25: ( 'ftnot' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2234:27: 'ftnot'
            {
            match("ftnot"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FTNOT"

    // $ANTLR start "FT_OPTION"
    public final void mFT_OPTION() throws RecognitionException {
        try {
            int _type = FT_OPTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2235:25: ( 'ft-option' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2235:27: 'ft-option'
            {
            match("ft-option"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FT_OPTION"

    // $ANTLR start "FTOR"
    public final void mFTOR() throws RecognitionException {
        try {
            int _type = FTOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2236:25: ( 'ftor' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2236:27: 'ftor'
            {
            match("ftor"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FTOR"

    // $ANTLR start "INSENSITIVE"
    public final void mINSENSITIVE() throws RecognitionException {
        try {
            int _type = INSENSITIVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2237:25: ( 'insensitive' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2237:27: 'insensitive'
            {
            match("insensitive"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INSENSITIVE"

    // $ANTLR start "LANGUAGE"
    public final void mLANGUAGE() throws RecognitionException {
        try {
            int _type = LANGUAGE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2238:25: ( 'language' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2238:27: 'language'
            {
            match("language"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LANGUAGE"

    // $ANTLR start "LEVELS"
    public final void mLEVELS() throws RecognitionException {
        try {
            int _type = LEVELS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2239:25: ( 'levels' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2239:27: 'levels'
            {
            match("levels"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LEVELS"

    // $ANTLR start "LOWERCASE"
    public final void mLOWERCASE() throws RecognitionException {
        try {
            int _type = LOWERCASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2240:25: ( 'lowercase' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2240:27: 'lowercase'
            {
            match("lowercase"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "LOWERCASE"

    // $ANTLR start "MOST"
    public final void mMOST() throws RecognitionException {
        try {
            int _type = MOST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2241:25: ( 'most' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2241:27: 'most'
            {
            match("most"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MOST"

    // $ANTLR start "NO"
    public final void mNO() throws RecognitionException {
        try {
            int _type = NO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2242:25: ( 'no' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2242:27: 'no'
            {
            match("no"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NO"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2243:25: ( 'not' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2243:27: 'not'
            {
            match("not"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "OCCURS"
    public final void mOCCURS() throws RecognitionException {
        try {
            int _type = OCCURS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2244:25: ( 'occurs' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2244:27: 'occurs'
            {
            match("occurs"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OCCURS"

    // $ANTLR start "PARAGRAPH"
    public final void mPARAGRAPH() throws RecognitionException {
        try {
            int _type = PARAGRAPH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2245:25: ( 'paragraph' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2245:27: 'paragraph'
            {
            match("paragraph"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARAGRAPH"

    // $ANTLR start "PARAGRAPHS"
    public final void mPARAGRAPHS() throws RecognitionException {
        try {
            int _type = PARAGRAPHS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2246:25: ( 'paragraphs' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2246:27: 'paragraphs'
            {
            match("paragraphs"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARAGRAPHS"

    // $ANTLR start "PHRASE"
    public final void mPHRASE() throws RecognitionException {
        try {
            int _type = PHRASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2247:25: ( 'phrase' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2247:27: 'phrase'
            {
            match("phrase"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PHRASE"

    // $ANTLR start "RELATIONSHIP"
    public final void mRELATIONSHIP() throws RecognitionException {
        try {
            int _type = RELATIONSHIP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2248:25: ( 'relationship' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2248:27: 'relationship'
            {
            match("relationship"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "RELATIONSHIP"

    // $ANTLR start "SAME"
    public final void mSAME() throws RecognitionException {
        try {
            int _type = SAME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2249:25: ( 'same' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2249:27: 'same'
            {
            match("same"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SAME"

    // $ANTLR start "SCORE"
    public final void mSCORE() throws RecognitionException {
        try {
            int _type = SCORE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2250:25: ( 'score' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2250:27: 'score'
            {
            match("score"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SCORE"

    // $ANTLR start "SENSITIVE"
    public final void mSENSITIVE() throws RecognitionException {
        try {
            int _type = SENSITIVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2251:25: ( 'sensitive' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2251:27: 'sensitive'
            {
            match("sensitive"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SENSITIVE"

    // $ANTLR start "SENTENCE"
    public final void mSENTENCE() throws RecognitionException {
        try {
            int _type = SENTENCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2252:25: ( 'sentence' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2252:27: 'sentence'
            {
            match("sentence"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SENTENCE"

    // $ANTLR start "SENTENCES"
    public final void mSENTENCES() throws RecognitionException {
        try {
            int _type = SENTENCES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2253:25: ( 'sentences' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2253:27: 'sentences'
            {
            match("sentences"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SENTENCES"

    // $ANTLR start "START"
    public final void mSTART() throws RecognitionException {
        try {
            int _type = START;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2254:25: ( 'start' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2254:27: 'start'
            {
            match("start"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "START"

    // $ANTLR start "STEMMING"
    public final void mSTEMMING() throws RecognitionException {
        try {
            int _type = STEMMING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2255:25: ( 'stemming' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2255:27: 'stemming'
            {
            match("stemming"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STEMMING"

    // $ANTLR start "STOP"
    public final void mSTOP() throws RecognitionException {
        try {
            int _type = STOP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2256:25: ( 'stop' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2256:27: 'stop'
            {
            match("stop"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STOP"

    // $ANTLR start "THESAURUS"
    public final void mTHESAURUS() throws RecognitionException {
        try {
            int _type = THESAURUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2257:25: ( 'thesaurus' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2257:27: 'thesaurus'
            {
            match("thesaurus"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "THESAURUS"

    // $ANTLR start "TIMES"
    public final void mTIMES() throws RecognitionException {
        try {
            int _type = TIMES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2258:25: ( 'times' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2258:27: 'times'
            {
            match("times"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TIMES"

    // $ANTLR start "UPPERCASE"
    public final void mUPPERCASE() throws RecognitionException {
        try {
            int _type = UPPERCASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2259:25: ( 'uppercase' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2259:27: 'uppercase'
            {
            match("uppercase"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "UPPERCASE"

    // $ANTLR start "USING"
    public final void mUSING() throws RecognitionException {
        try {
            int _type = USING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2260:25: ( 'using' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2260:27: 'using'
            {
            match("using"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "USING"

    // $ANTLR start "WEIGHT"
    public final void mWEIGHT() throws RecognitionException {
        try {
            int _type = WEIGHT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2261:25: ( 'weight' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2261:27: 'weight'
            {
            match("weight"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WEIGHT"

    // $ANTLR start "WILDCARDS"
    public final void mWILDCARDS() throws RecognitionException {
        try {
            int _type = WILDCARDS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2262:25: ( 'wildcards' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2262:27: 'wildcards'
            {
            match("wildcards"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WILDCARDS"

    // $ANTLR start "WINDOW"
    public final void mWINDOW() throws RecognitionException {
        try {
            int _type = WINDOW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2263:25: ( 'window' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2263:27: 'window'
            {
            match("window"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WINDOW"

    // $ANTLR start "WITHOUT"
    public final void mWITHOUT() throws RecognitionException {
        try {
            int _type = WITHOUT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2264:25: ( 'without' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2264:27: 'without'
            {
            match("without"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WITHOUT"

    // $ANTLR start "WORD"
    public final void mWORD() throws RecognitionException {
        try {
            int _type = WORD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2265:25: ( 'word' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2265:27: 'word'
            {
            match("word"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WORD"

    // $ANTLR start "WORDS"
    public final void mWORDS() throws RecognitionException {
        try {
            int _type = WORDS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2266:25: ( 'words' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2266:27: 'words'
            {
            match("words"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WORDS"

    // $ANTLR start "CATCH"
    public final void mCATCH() throws RecognitionException {
        try {
            int _type = CATCH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2269:25: ( 'catch' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2269:27: 'catch'
            {
            match("catch"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CATCH"

    // $ANTLR start "CONTEXT"
    public final void mCONTEXT() throws RecognitionException {
        try {
            int _type = CONTEXT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2270:25: ( 'context' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2270:27: 'context'
            {
            match("context"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CONTEXT"

    // $ANTLR start "DETERMINISTIC"
    public final void mDETERMINISTIC() throws RecognitionException {
        try {
            int _type = DETERMINISTIC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2271:25: ( 'deterministic' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2271:27: 'deterministic'
            {
            match("deterministic"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DETERMINISTIC"

    // $ANTLR start "NAMESPACE_NODE"
    public final void mNAMESPACE_NODE() throws RecognitionException {
        try {
            int _type = NAMESPACE_NODE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2272:25: ( 'namespace-node' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2272:27: 'namespace-node'
            {
            match("namespace-node"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NAMESPACE_NODE"

    // $ANTLR start "NONDETERMINISTIC"
    public final void mNONDETERMINISTIC() throws RecognitionException {
        try {
            int _type = NONDETERMINISTIC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2273:25: ( 'nondeterministic' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2273:27: 'nondeterministic'
            {
            match("nondeterministic"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NONDETERMINISTIC"

    // $ANTLR start "TRY"
    public final void mTRY() throws RecognitionException {
        try {
            int _type = TRY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2274:25: ( 'try' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2274:27: 'try'
            {
            match("try"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TRY"

    // $ANTLR start "DECIMAL_FORMAT"
    public final void mDECIMAL_FORMAT() throws RecognitionException {
        try {
            int _type = DECIMAL_FORMAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2276:25: ( 'decimal-format' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2276:27: 'decimal-format'
            {
            match("decimal-format"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DECIMAL_FORMAT"

    // $ANTLR start "DECIMAL_SEPARATOR"
    public final void mDECIMAL_SEPARATOR() throws RecognitionException {
        try {
            int _type = DECIMAL_SEPARATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2277:25: ( 'decimal-separator' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2277:27: 'decimal-separator'
            {
            match("decimal-separator"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DECIMAL_SEPARATOR"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            int _type = DIGIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2278:25: ( 'digit' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2278:27: 'digit'
            {
            match("digit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "GROUPING_SEPARATOR"
    public final void mGROUPING_SEPARATOR() throws RecognitionException {
        try {
            int _type = GROUPING_SEPARATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2279:25: ( 'grouping-separatpr' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2279:27: 'grouping-separatpr'
            {
            match("grouping-separatpr"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GROUPING_SEPARATOR"

    // $ANTLR start "INFINITY"
    public final void mINFINITY() throws RecognitionException {
        try {
            int _type = INFINITY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2280:25: ( 'infinity' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2280:27: 'infinity'
            {
            match("infinity"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INFINITY"

    // $ANTLR start "MINUS_SIGN"
    public final void mMINUS_SIGN() throws RecognitionException {
        try {
            int _type = MINUS_SIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2281:25: ( 'minus-sign' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2281:27: 'minus-sign'
            {
            match("minus-sign"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MINUS_SIGN"

    // $ANTLR start "NAN"
    public final void mNAN() throws RecognitionException {
        try {
            int _type = NAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2282:25: ( 'NaN' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2282:27: 'NaN'
            {
            match("NaN"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NAN"

    // $ANTLR start "PER_MILLE"
    public final void mPER_MILLE() throws RecognitionException {
        try {
            int _type = PER_MILLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2283:25: ( 'per-mille' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2283:27: 'per-mille'
            {
            match("per-mille"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PER_MILLE"

    // $ANTLR start "PERCENT"
    public final void mPERCENT() throws RecognitionException {
        try {
            int _type = PERCENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2284:25: ( 'percent' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2284:27: 'percent'
            {
            match("percent"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PERCENT"

    // $ANTLR start "PATTERN_SEPARATOR"
    public final void mPATTERN_SEPARATOR() throws RecognitionException {
        try {
            int _type = PATTERN_SEPARATOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2285:25: ( 'pattern-separator' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2285:27: 'pattern-separator'
            {
            match("pattern-separator"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PATTERN_SEPARATOR"

    // $ANTLR start "ZERO_DIGIT"
    public final void mZERO_DIGIT() throws RecognitionException {
        try {
            int _type = ZERO_DIGIT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2286:25: ( 'zero-digit' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2286:27: 'zero-digit'
            {
            match("zero-digit"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ZERO_DIGIT"

    // $ANTLR start "COUNT"
    public final void mCOUNT() throws RecognitionException {
        try {
            int _type = COUNT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2288:25: ( 'count' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2288:27: 'count'
            {
            match("count"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COUNT"

    // $ANTLR start "GROUP"
    public final void mGROUP() throws RecognitionException {
        try {
            int _type = GROUP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2289:25: ( 'group' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2289:27: 'group'
            {
            match("group"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "GROUP"

    // $ANTLR start "NEXT"
    public final void mNEXT() throws RecognitionException {
        try {
            int _type = NEXT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2290:25: ( 'next' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2290:27: 'next'
            {
            match("next"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NEXT"

    // $ANTLR start "ONLY"
    public final void mONLY() throws RecognitionException {
        try {
            int _type = ONLY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2291:25: ( 'only' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2291:27: 'only'
            {
            match("only"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ONLY"

    // $ANTLR start "PREVIOUS"
    public final void mPREVIOUS() throws RecognitionException {
        try {
            int _type = PREVIOUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2292:25: ( 'previous' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2292:27: 'previous'
            {
            match("previous"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PREVIOUS"

    // $ANTLR start "SLIDING"
    public final void mSLIDING() throws RecognitionException {
        try {
            int _type = SLIDING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2293:25: ( 'sliding' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2293:27: 'sliding'
            {
            match("sliding"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SLIDING"

    // $ANTLR start "TUMBLING"
    public final void mTUMBLING() throws RecognitionException {
        try {
            int _type = TUMBLING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2294:25: ( 'tumbling' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2294:27: 'tumbling'
            {
            match("tumbling"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TUMBLING"

    // $ANTLR start "WHEN"
    public final void mWHEN() throws RecognitionException {
        try {
            int _type = WHEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2295:25: ( 'when' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2295:27: 'when'
            {
            match("when"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WHEN"

    // $ANTLR start "ALLOWING"
    public final void mALLOWING() throws RecognitionException {
        try {
            int _type = ALLOWING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2296:25: ( 'allowing' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2296:27: 'allowing'
            {
            match("allowing"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ALLOWING"

    // $ANTLR start "DirCommentConstructor"
    public final void mDirCommentConstructor() throws RecognitionException {
        try {
            int _type = DirCommentConstructor;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2300:5: ( '<!--' ( options {greedy=false; } : . )* '-->' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2300:7: '<!--' ( options {greedy=false; } : . )* '-->'
            {
            match("<!--"); 

            // org/brackit/xquery/compiler/parser/XQuery.g:2300:14: ( options {greedy=false; } : . )*
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
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2300:41: .
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2303:5: ( '<?' ( VS )? NCName ( VS ( options {greedy=false; } : . )* )? '?>' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2303:7: '<?' ( VS )? NCName ( VS ( options {greedy=false; } : . )* )? '?>'
            {
            match("<?"); 

            // org/brackit/xquery/compiler/parser/XQuery.g:2303:12: ( VS )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( ((LA2_0>='\t' && LA2_0<='\n')||LA2_0=='\r'||LA2_0==' ') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2303:12: VS
                    {
                    mVS(); 

                    }
                    break;

            }

            mNCName(); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2303:23: ( VS ( options {greedy=false; } : . )* )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( ((LA4_0>='\t' && LA4_0<='\n')||LA4_0=='\r'||LA4_0==' ') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2303:24: VS ( options {greedy=false; } : . )*
                    {
                    mVS(); 
                    // org/brackit/xquery/compiler/parser/XQuery.g:2303:27: ( options {greedy=false; } : . )*
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
                    	    // org/brackit/xquery/compiler/parser/XQuery.g:2303:54: .
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

    // $ANTLR start "Pragma"
    public final void mPragma() throws RecognitionException {
        try {
            int _type = Pragma;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2312:5: ( '(#' ( VS )? NCName ( Colon NCName )? ( VS ( options {greedy=false; } : . )* )? '#)' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2312:7: '(#' ( VS )? NCName ( Colon NCName )? ( VS ( options {greedy=false; } : . )* )? '#)'
            {
            match("(#"); 

            // org/brackit/xquery/compiler/parser/XQuery.g:2312:12: ( VS )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( ((LA5_0>='\t' && LA5_0<='\n')||LA5_0=='\r'||LA5_0==' ') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2312:12: VS
                    {
                    mVS(); 

                    }
                    break;

            }

            mNCName(); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2312:23: ( Colon NCName )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0==':') ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2312:24: Colon NCName
                    {
                    mColon(); 
                    mNCName(); 

                    }
                    break;

            }

            // org/brackit/xquery/compiler/parser/XQuery.g:2312:39: ( VS ( options {greedy=false; } : . )* )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( ((LA8_0>='\t' && LA8_0<='\n')||LA8_0=='\r'||LA8_0==' ') ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2312:40: VS ( options {greedy=false; } : . )*
                    {
                    mVS(); 
                    // org/brackit/xquery/compiler/parser/XQuery.g:2312:43: ( options {greedy=false; } : . )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( (LA7_0=='#') ) {
                            int LA7_1 = input.LA(2);

                            if ( (LA7_1==')') ) {
                                alt7=2;
                            }
                            else if ( ((LA7_1>='\u0000' && LA7_1<='(')||(LA7_1>='*' && LA7_1<='\uFFFF')) ) {
                                alt7=1;
                            }


                        }
                        else if ( ((LA7_0>='\u0000' && LA7_0<='\"')||(LA7_0>='$' && LA7_0<='\uFFFF')) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // org/brackit/xquery/compiler/parser/XQuery.g:2312:70: .
                    	    {
                    	    matchAny(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);


                    }
                    break;

            }

            match("#)"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Pragma"

    // $ANTLR start "IntegerLiteral"
    public final void mIntegerLiteral() throws RecognitionException {
        try {
            int _type = IntegerLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2336:5: ( Digits )
            // org/brackit/xquery/compiler/parser/XQuery.g:2336:7: Digits
            {
            mDigits(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IntegerLiteral"

    // $ANTLR start "DecimalLiteral"
    public final void mDecimalLiteral() throws RecognitionException {
        try {
            int _type = DecimalLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2339:5: ( ( '.' Digits ) | ( Digits '.' ( '0' .. '9' )* ) )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0=='.') ) {
                alt10=1;
            }
            else if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:7: ( '.' Digits )
                    {
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:7: ( '.' Digits )
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:8: '.' Digits
                    {
                    match('.'); 
                    mDigits(); 

                    }


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:22: ( Digits '.' ( '0' .. '9' )* )
                    {
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:22: ( Digits '.' ( '0' .. '9' )* )
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:23: Digits '.' ( '0' .. '9' )*
                    {
                    mDigits(); 
                    match('.'); 
                    // org/brackit/xquery/compiler/parser/XQuery.g:2339:34: ( '0' .. '9' )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( ((LA9_0>='0' && LA9_0<='9')) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // org/brackit/xquery/compiler/parser/XQuery.g:2339:34: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);


                    }


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DecimalLiteral"

    // $ANTLR start "DoubleLiteral"
    public final void mDoubleLiteral() throws RecognitionException {
        try {
            int _type = DoubleLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2342:5: ( ( ( '.' Digits ) | ( Digits ( '.' ( '0' .. '9' )* )? ) ) ( 'e' | 'E' ) ( '+' | '-' )? Digits )
            // org/brackit/xquery/compiler/parser/XQuery.g:2342:7: ( ( '.' Digits ) | ( Digits ( '.' ( '0' .. '9' )* )? ) ) ( 'e' | 'E' ) ( '+' | '-' )? Digits
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2342:7: ( ( '.' Digits ) | ( Digits ( '.' ( '0' .. '9' )* )? ) )
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0=='.') ) {
                alt13=1;
            }
            else if ( ((LA13_0>='0' && LA13_0<='9')) ) {
                alt13=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }
            switch (alt13) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:8: ( '.' Digits )
                    {
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:8: ( '.' Digits )
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:9: '.' Digits
                    {
                    match('.'); 
                    mDigits(); 

                    }


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:23: ( Digits ( '.' ( '0' .. '9' )* )? )
                    {
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:23: ( Digits ( '.' ( '0' .. '9' )* )? )
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:24: Digits ( '.' ( '0' .. '9' )* )?
                    {
                    mDigits(); 
                    // org/brackit/xquery/compiler/parser/XQuery.g:2342:31: ( '.' ( '0' .. '9' )* )?
                    int alt12=2;
                    int LA12_0 = input.LA(1);

                    if ( (LA12_0=='.') ) {
                        alt12=1;
                    }
                    switch (alt12) {
                        case 1 :
                            // org/brackit/xquery/compiler/parser/XQuery.g:2342:32: '.' ( '0' .. '9' )*
                            {
                            match('.'); 
                            // org/brackit/xquery/compiler/parser/XQuery.g:2342:36: ( '0' .. '9' )*
                            loop11:
                            do {
                                int alt11=2;
                                int LA11_0 = input.LA(1);

                                if ( ((LA11_0>='0' && LA11_0<='9')) ) {
                                    alt11=1;
                                }


                                switch (alt11) {
                            	case 1 :
                            	    // org/brackit/xquery/compiler/parser/XQuery.g:2342:36: '0' .. '9'
                            	    {
                            	    matchRange('0','9'); 

                            	    }
                            	    break;

                            	default :
                            	    break loop11;
                                }
                            } while (true);


                            }
                            break;

                    }


                    }


                    }
                    break;

            }

            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // org/brackit/xquery/compiler/parser/XQuery.g:2342:62: ( '+' | '-' )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0=='+'||LA14_0=='-') ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            mDigits(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DoubleLiteral"

    // $ANTLR start "QuotedStringLiteral"
    public final void mQuotedStringLiteral() throws RecognitionException {
        try {
            int _type = QuotedStringLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2346:2: ( Quot ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeQuot | ~ ( '\"' | '&' ) )* ) Quot )
            // org/brackit/xquery/compiler/parser/XQuery.g:2346:4: Quot ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeQuot | ~ ( '\"' | '&' ) )* ) Quot
            {
            mQuot(); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2346:9: ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeQuot | ~ ( '\"' | '&' ) )* )
            // org/brackit/xquery/compiler/parser/XQuery.g:2348:11: ( PredefinedEntityRef | CharRef | EscapeQuot | ~ ( '\"' | '&' ) )*
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2348:11: ( PredefinedEntityRef | CharRef | EscapeQuot | ~ ( '\"' | '&' ) )*
            loop15:
            do {
                int alt15=5;
                int LA15_0 = input.LA(1);

                if ( (LA15_0=='\"') ) {
                    int LA15_1 = input.LA(2);

                    if ( (LA15_1=='\"') ) {
                        alt15=3;
                    }


                }
                else if ( (LA15_0=='&') ) {
                    int LA15_2 = input.LA(2);

                    if ( (LA15_2=='#') ) {
                        alt15=2;
                    }
                    else if ( (LA15_2=='a'||LA15_2=='g'||LA15_2=='l'||LA15_2=='q') ) {
                        alt15=1;
                    }


                }
                else if ( ((LA15_0>='\u0000' && LA15_0<='!')||(LA15_0>='#' && LA15_0<='%')||(LA15_0>='\'' && LA15_0<='\uFFFF')) ) {
                    alt15=4;
                }


                switch (alt15) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2348:12: PredefinedEntityRef
            	    {
            	    mPredefinedEntityRef(); 

            	    }
            	    break;
            	case 2 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2348:34: CharRef
            	    {
            	    mCharRef(); 

            	    }
            	    break;
            	case 3 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2348:44: EscapeQuot
            	    {
            	    mEscapeQuot(); 

            	    }
            	    break;
            	case 4 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2348:57: ~ ( '\"' | '&' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='%')||(input.LA(1)>='\'' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);


            }

            mQuot(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "QuotedStringLiteral"

    // $ANTLR start "AposedStringLiteral"
    public final void mAposedStringLiteral() throws RecognitionException {
        try {
            int _type = AposedStringLiteral;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2354:2: ( Apos ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeApos | ~ ( '\\'' | '&' ) )* ) Apos )
            // org/brackit/xquery/compiler/parser/XQuery.g:2354:4: Apos ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeApos | ~ ( '\\'' | '&' ) )* ) Apos
            {
            mApos(); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2354:9: ( options {greedy=false; } : ( PredefinedEntityRef | CharRef | EscapeApos | ~ ( '\\'' | '&' ) )* )
            // org/brackit/xquery/compiler/parser/XQuery.g:2356:11: ( PredefinedEntityRef | CharRef | EscapeApos | ~ ( '\\'' | '&' ) )*
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2356:11: ( PredefinedEntityRef | CharRef | EscapeApos | ~ ( '\\'' | '&' ) )*
            loop16:
            do {
                int alt16=5;
                int LA16_0 = input.LA(1);

                if ( (LA16_0=='\'') ) {
                    int LA16_1 = input.LA(2);

                    if ( (LA16_1=='\'') ) {
                        alt16=3;
                    }


                }
                else if ( (LA16_0=='&') ) {
                    int LA16_2 = input.LA(2);

                    if ( (LA16_2=='#') ) {
                        alt16=2;
                    }
                    else if ( (LA16_2=='a'||LA16_2=='g'||LA16_2=='l'||LA16_2=='q') ) {
                        alt16=1;
                    }


                }
                else if ( ((LA16_0>='\u0000' && LA16_0<='%')||(LA16_0>='(' && LA16_0<='\uFFFF')) ) {
                    alt16=4;
                }


                switch (alt16) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2356:12: PredefinedEntityRef
            	    {
            	    mPredefinedEntityRef(); 

            	    }
            	    break;
            	case 2 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2356:34: CharRef
            	    {
            	    mCharRef(); 

            	    }
            	    break;
            	case 3 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2356:44: EscapeApos
            	    {
            	    mEscapeApos(); 

            	    }
            	    break;
            	case 4 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2356:57: ~ ( '\\'' | '&' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='%')||(input.LA(1)>='(' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop16;
                }
            } while (true);


            }

            mApos(); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AposedStringLiteral"

    // $ANTLR start "PredefinedEntityRef"
    public final void mPredefinedEntityRef() throws RecognitionException {
        try {
            int _type = PredefinedEntityRef;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2362:5: ( '&' ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' ) ';' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2362:7: '&' ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' ) ';'
            {
            match('&'); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2362:11: ( 'lt' | 'gt' | 'apos' | 'quot' | 'amp' )
            int alt17=5;
            switch ( input.LA(1) ) {
            case 'l':
                {
                alt17=1;
                }
                break;
            case 'g':
                {
                alt17=2;
                }
                break;
            case 'a':
                {
                int LA17_3 = input.LA(2);

                if ( (LA17_3=='p') ) {
                    alt17=3;
                }
                else if ( (LA17_3=='m') ) {
                    alt17=5;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 17, 3, input);

                    throw nvae;
                }
                }
                break;
            case 'q':
                {
                alt17=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;
            }

            switch (alt17) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2362:12: 'lt'
                    {
                    match("lt"); 


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2362:19: 'gt'
                    {
                    match("gt"); 


                    }
                    break;
                case 3 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2362:26: 'apos'
                    {
                    match("apos"); 


                    }
                    break;
                case 4 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2362:35: 'quot'
                    {
                    match("quot"); 


                    }
                    break;
                case 5 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2362:44: 'amp'
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2365:5: ( '&#' Digits ';' | '&#x' HexDigits ';' )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0=='&') ) {
                int LA18_1 = input.LA(2);

                if ( (LA18_1=='#') ) {
                    int LA18_2 = input.LA(3);

                    if ( (LA18_2=='x') ) {
                        alt18=2;
                    }
                    else if ( ((LA18_2>='0' && LA18_2<='9')) ) {
                        alt18=1;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 18, 2, input);

                        throw nvae;
                    }
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 18, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 18, 0, input);

                throw nvae;
            }
            switch (alt18) {
                case 1 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2365:7: '&#' Digits ';'
                    {
                    match("&#"); 

                    mDigits(); 
                    match(';'); 
                    checkCharRef();

                    }
                    break;
                case 2 :
                    // org/brackit/xquery/compiler/parser/XQuery.g:2366:7: '&#x' HexDigits ';'
                    {
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

    // $ANTLR start "Comment"
    public final void mComment() throws RecognitionException {
        try {
            int _type = Comment;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2369:5: ( '(:' ( options {greedy=false; } : Comment | . )* ':)' )
            // org/brackit/xquery/compiler/parser/XQuery.g:2369:7: '(:' ( options {greedy=false; } : Comment | . )* ':)'
            {
            match("(:"); 

            // org/brackit/xquery/compiler/parser/XQuery.g:2369:12: ( options {greedy=false; } : Comment | . )*
            loop19:
            do {
                int alt19=3;
                int LA19_0 = input.LA(1);

                if ( (LA19_0==':') ) {
                    int LA19_1 = input.LA(2);

                    if ( (LA19_1==')') ) {
                        alt19=3;
                    }
                    else if ( ((LA19_1>='\u0000' && LA19_1<='(')||(LA19_1>='*' && LA19_1<='\uFFFF')) ) {
                        alt19=2;
                    }


                }
                else if ( (LA19_0=='(') ) {
                    int LA19_2 = input.LA(2);

                    if ( (LA19_2==':') ) {
                        alt19=1;
                    }
                    else if ( ((LA19_2>='\u0000' && LA19_2<='9')||(LA19_2>=';' && LA19_2<='\uFFFF')) ) {
                        alt19=2;
                    }


                }
                else if ( ((LA19_0>='\u0000' && LA19_0<='\'')||(LA19_0>=')' && LA19_0<='9')||(LA19_0>=';' && LA19_0<='\uFFFF')) ) {
                    alt19=2;
                }


                switch (alt19) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2369:38: Comment
            	    {
            	    mComment(); 

            	    }
            	    break;
            	case 2 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2369:48: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop19;
                }
            } while (true);

            match(":)"); 

             _channel = HIDDEN; 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "Comment"

    // $ANTLR start "NCName"
    public final void mNCName() throws RecognitionException {
        try {
            int _type = NCName;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2372:5: ( NCNameStartChar ( NCNameChar )* )
            // org/brackit/xquery/compiler/parser/XQuery.g:2372:7: NCNameStartChar ( NCNameChar )*
            {
            mNCNameStartChar(); 
            // org/brackit/xquery/compiler/parser/XQuery.g:2372:23: ( NCNameChar )*
            loop20:
            do {
                int alt20=2;
                int LA20_0 = input.LA(1);

                if ( ((LA20_0>='-' && LA20_0<='.')||(LA20_0>='0' && LA20_0<='9')||(LA20_0>='A' && LA20_0<='Z')||LA20_0=='_'||(LA20_0>='a' && LA20_0<='z')||LA20_0=='\u00B7'||(LA20_0>='\u00C0' && LA20_0<='\u00D6')||(LA20_0>='\u00D8' && LA20_0<='\u00F6')||(LA20_0>='\u00F8' && LA20_0<='\u037D')||(LA20_0>='\u037F' && LA20_0<='\u1FFF')||(LA20_0>='\u200C' && LA20_0<='\u200D')||(LA20_0>='\u203F' && LA20_0<='\u2040')||(LA20_0>='\u2070' && LA20_0<='\u218F')||(LA20_0>='\u2C00' && LA20_0<='\u2FEF')||(LA20_0>='\u3001' && LA20_0<='\uD7FF')||(LA20_0>='\uF900' && LA20_0<='\uFDCF')||(LA20_0>='\uFDF0' && LA20_0<='\uFFFD')) ) {
                    alt20=1;
                }


                switch (alt20) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2372:23: NCNameChar
            	    {
            	    mNCNameChar(); 

            	    }
            	    break;

            	default :
            	    break loop20;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NCName"

    // $ANTLR start "S"
    public final void mS() throws RecognitionException {
        try {
            int _type = S;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/compiler/parser/XQuery.g:2375:5: ( ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+ )
            // org/brackit/xquery/compiler/parser/XQuery.g:2375:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2375:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            int cnt21=0;
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( ((LA21_0>='\t' && LA21_0<='\n')||LA21_0=='\r'||LA21_0==' ') ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:
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
            	    if ( cnt21 >= 1 ) break loop21;
                        EarlyExitException eee =
                            new EarlyExitException(21, input);
                        throw eee;
                }
                cnt21++;
            } while (true);

             _channel = HIDDEN; 

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
            // org/brackit/xquery/compiler/parser/XQuery.g:2379:5: ( ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+ )
            // org/brackit/xquery/compiler/parser/XQuery.g:2379:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2379:7: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' )+
            int cnt22=0;
            loop22:
            do {
                int alt22=2;
                int LA22_0 = input.LA(1);

                if ( ((LA22_0>='\t' && LA22_0<='\n')||LA22_0=='\r'||LA22_0==' ') ) {
                    alt22=1;
                }


                switch (alt22) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:
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
            	    if ( cnt22 >= 1 ) break loop22;
                        EarlyExitException eee =
                            new EarlyExitException(22, input);
                        throw eee;
                }
                cnt22++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "VS"

    // $ANTLR start "Digits"
    public final void mDigits() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2383:5: ( ( '0' .. '9' )+ )
            // org/brackit/xquery/compiler/parser/XQuery.g:2383:7: ( '0' .. '9' )+
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2383:7: ( '0' .. '9' )+
            int cnt23=0;
            loop23:
            do {
                int alt23=2;
                int LA23_0 = input.LA(1);

                if ( ((LA23_0>='0' && LA23_0<='9')) ) {
                    alt23=1;
                }


                switch (alt23) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:2383:7: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt23 >= 1 ) break loop23;
                        EarlyExitException eee =
                            new EarlyExitException(23, input);
                        throw eee;
                }
                cnt23++;
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2387:5: ( ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+ )
            // org/brackit/xquery/compiler/parser/XQuery.g:2387:7: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
            {
            // org/brackit/xquery/compiler/parser/XQuery.g:2387:7: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
            int cnt24=0;
            loop24:
            do {
                int alt24=2;
                int LA24_0 = input.LA(1);

                if ( ((LA24_0>='0' && LA24_0<='9')||(LA24_0>='A' && LA24_0<='F')||(LA24_0>='a' && LA24_0<='f')) ) {
                    alt24=1;
                }


                switch (alt24) {
            	case 1 :
            	    // org/brackit/xquery/compiler/parser/XQuery.g:
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
            	    if ( cnt24 >= 1 ) break loop24;
                        EarlyExitException eee =
                            new EarlyExitException(24, input);
                        throw eee;
                }
                cnt24++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "HexDigits"

    // $ANTLR start "Char"
    public final void mChar() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2391:5: ( '\\u0009' | '\\u000A' | '\\u000D' | '\\u0020' .. '\\uD7FF' | '\\uE000' .. '\\uFFFD' )
            // org/brackit/xquery/compiler/parser/XQuery.g:
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||(input.LA(1)>=' ' && input.LA(1)<='\uD7FF')||(input.LA(1)>='\uE000' && input.LA(1)<='\uFFFD') ) {
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
    // $ANTLR end "Char"

    // $ANTLR start "NCNameStartChar"
    public final void mNCNameStartChar() throws RecognitionException {
        try {
            // org/brackit/xquery/compiler/parser/XQuery.g:2396:5: ( Letter | '_' )
            // org/brackit/xquery/compiler/parser/XQuery.g:
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2401:5: ( 'A' .. 'Z' | 'a' .. 'z' | '_' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u02FF' | '\\u0370' .. '\\u037D' | '\\u037F' .. '\\u1FFF' | '\\u200C' .. '\\u200D' | '\\u2070' .. '\\u218F' | '\\u2C00' .. '\\u2FEF' | '\\u3001' .. '\\uD7FF' | '\\uF900' .. '\\uFDCF' | '\\uFDF0' .. '\\uFFFD' | '-' | '.' | '0' .. '9' | '\\u00B7' | '\\u0300' .. '\\u036F' | '\\u203F' .. '\\u2040' )
            // org/brackit/xquery/compiler/parser/XQuery.g:
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
            // org/brackit/xquery/compiler/parser/XQuery.g:2413:5: ( '\\u0041' .. '\\u005A' | '\\u0061' .. '\\u007A' | '\\u00C0' .. '\\u00D6' | '\\u00D8' .. '\\u00F6' | '\\u00F8' .. '\\u00FF' | '\\u0100' .. '\\u0131' | '\\u0134' .. '\\u013E' | '\\u0141' .. '\\u0148' | '\\u014A' .. '\\u017E' | '\\u0180' .. '\\u01C3' | '\\u01CD' .. '\\u01F0' | '\\u01F4' .. '\\u01F5' | '\\u01FA' .. '\\u0217' | '\\u0250' .. '\\u02A8' | '\\u02BB' .. '\\u02C1' | '\\u0386' | '\\u0388' .. '\\u038A' | '\\u038C' | '\\u038E' .. '\\u03A1' | '\\u03A3' .. '\\u03CE' | '\\u03D0' .. '\\u03D6' | '\\u03DA' | '\\u03DC' | '\\u03DE' | '\\u03E0' | '\\u03E2' .. '\\u03F3' | '\\u0401' .. '\\u040C' | '\\u040E' .. '\\u044F' | '\\u0451' .. '\\u045C' | '\\u045E' .. '\\u0481' | '\\u0490' .. '\\u04C4' | '\\u04C7' .. '\\u04C8' | '\\u04CB' .. '\\u04CC' | '\\u04D0' .. '\\u04EB' | '\\u04EE' .. '\\u04F5' | '\\u04F8' .. '\\u04F9' | '\\u0531' .. '\\u0556' | '\\u0559' | '\\u0561' .. '\\u0586' | '\\u05D0' .. '\\u05EA' | '\\u05F0' .. '\\u05F2' | '\\u0621' .. '\\u063A' | '\\u0641' .. '\\u064A' | '\\u0671' .. '\\u06B7' | '\\u06BA' .. '\\u06BE' | '\\u06C0' .. '\\u06CE' | '\\u06D0' .. '\\u06D3' | '\\u06D5' | '\\u06E5' .. '\\u06E6' | '\\u0905' .. '\\u0939' | '\\u093D' | '\\u0958' .. '\\u0961' | '\\u0985' .. '\\u098C' | '\\u098F' .. '\\u0990' | '\\u0993' .. '\\u09A8' | '\\u09AA' .. '\\u09B0' | '\\u09B2' | '\\u09B6' .. '\\u09B9' | '\\u09DC' .. '\\u09DD' | '\\u09DF' .. '\\u09E1' | '\\u09F0' .. '\\u09F1' | '\\u0A05' .. '\\u0A0A' | '\\u0A0F' .. '\\u0A10' | '\\u0A13' .. '\\u0A28' | '\\u0A2A' .. '\\u0A30' | '\\u0A32' .. '\\u0A33' | '\\u0A35' .. '\\u0A36' | '\\u0A38' .. '\\u0A39' | '\\u0A59' .. '\\u0A5C' | '\\u0A5E' | '\\u0A72' .. '\\u0A74' | '\\u0A85' .. '\\u0A8B' | '\\u0A8D' | '\\u0A8F' .. '\\u0A91' | '\\u0A93' .. '\\u0AA8' | '\\u0AAA' .. '\\u0AB0' | '\\u0AB2' .. '\\u0AB3' | '\\u0AB5' .. '\\u0AB9' | '\\u0ABD' | '\\u0AE0' | '\\u0B05' .. '\\u0B0C' | '\\u0B0F' .. '\\u0B10' | '\\u0B13' .. '\\u0B28' | '\\u0B2A' .. '\\u0B30' | '\\u0B32' .. '\\u0B33' | '\\u0B36' .. '\\u0B39' | '\\u0B3D' | '\\u0B5C' .. '\\u0B5D' | '\\u0B5F' .. '\\u0B61' | '\\u0B85' .. '\\u0B8A' | '\\u0B8E' .. '\\u0B90' | '\\u0B92' .. '\\u0B95' | '\\u0B99' .. '\\u0B9A' | '\\u0B9C' | '\\u0B9E' .. '\\u0B9F' | '\\u0BA3' .. '\\u0BA4' | '\\u0BA8' .. '\\u0BAA' | '\\u0BAE' .. '\\u0BB5' | '\\u0BB7' .. '\\u0BB9' | '\\u0C05' .. '\\u0C0C' | '\\u0C0E' .. '\\u0C10' | '\\u0C12' .. '\\u0C28' | '\\u0C2A' .. '\\u0C33' | '\\u0C35' .. '\\u0C39' | '\\u0C60' .. '\\u0C61' | '\\u0C85' .. '\\u0C8C' | '\\u0C8E' .. '\\u0C90' | '\\u0C92' .. '\\u0CA8' | '\\u0CAA' .. '\\u0CB3' | '\\u0CB5' .. '\\u0CB9' | '\\u0CDE' | '\\u0CE0' .. '\\u0CE1' | '\\u0D05' .. '\\u0D0C' | '\\u0D0E' .. '\\u0D10' | '\\u0D12' .. '\\u0D28' | '\\u0D2A' .. '\\u0D39' | '\\u0D60' .. '\\u0D61' | '\\u0E01' .. '\\u0E2E' | '\\u0E30' | '\\u0E32' .. '\\u0E33' | '\\u0E40' .. '\\u0E45' | '\\u0E81' .. '\\u0E82' | '\\u0E84' | '\\u0E87' .. '\\u0E88' | '\\u0E8A' | '\\u0E8D' | '\\u0E94' .. '\\u0E97' | '\\u0E99' .. '\\u0E9F' | '\\u0EA1' .. '\\u0EA3' | '\\u0EA5' | '\\u0EA7' | '\\u0EAA' .. '\\u0EAB' | '\\u0EAD' .. '\\u0EAE' | '\\u0EB0' | '\\u0EB2' .. '\\u0EB3' | '\\u0EBD' | '\\u0EC0' .. '\\u0EC4' | '\\u0F40' .. '\\u0F47' | '\\u0F49' .. '\\u0F69' | '\\u10A0' .. '\\u10C5' | '\\u10D0' .. '\\u10F6' | '\\u1100' | '\\u1102' .. '\\u1103' | '\\u1105' .. '\\u1107' | '\\u1109' | '\\u110B' .. '\\u110C' | '\\u110E' .. '\\u1112' | '\\u113C' | '\\u113E' | '\\u1140' | '\\u114C' | '\\u114E' | '\\u1150' | '\\u1154' .. '\\u1155' | '\\u1159' | '\\u115F' .. '\\u1161' | '\\u1163' | '\\u1165' | '\\u1167' | '\\u1169' | '\\u116D' .. '\\u116E' | '\\u1172' .. '\\u1173' | '\\u1175' | '\\u119E' | '\\u11A8' | '\\u11AB' | '\\u11AE' .. '\\u11AF' | '\\u11B7' .. '\\u11B8' | '\\u11BA' | '\\u11BC' .. '\\u11C2' | '\\u11EB' | '\\u11F0' | '\\u11F9' | '\\u1E00' .. '\\u1E9B' | '\\u1EA0' .. '\\u1EF9' | '\\u1F00' .. '\\u1F15' | '\\u1F18' .. '\\u1F1D' | '\\u1F20' .. '\\u1F45' | '\\u1F48' .. '\\u1F4D' | '\\u1F50' .. '\\u1F57' | '\\u1F59' | '\\u1F5B' | '\\u1F5D' | '\\u1F5F' .. '\\u1F7D' | '\\u1F80' .. '\\u1FB4' | '\\u1FB6' .. '\\u1FBC' | '\\u1FBE' | '\\u1FC2' .. '\\u1FC4' | '\\u1FC6' .. '\\u1FCC' | '\\u1FD0' .. '\\u1FD3' | '\\u1FD6' .. '\\u1FDB' | '\\u1FE0' .. '\\u1FEC' | '\\u1FF2' .. '\\u1FF4' | '\\u1FF6' .. '\\u1FFC' | '\\u2126' | '\\u212A' .. '\\u212B' | '\\u212E' | '\\u2180' .. '\\u2182' | '\\u3041' .. '\\u3094' | '\\u30A1' .. '\\u30FA' | '\\u3105' .. '\\u312C' | '\\uAC00' .. '\\uD7A3' | '\\u4E00' .. '\\u9FA5' | '\\u3007' | '\\u3021' .. '\\u3029' )
            // org/brackit/xquery/compiler/parser/XQuery.g:
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
        // org/brackit/xquery/compiler/parser/XQuery.g:1:8: ( T__406 | T__407 | T__408 | T__409 | T__410 | T__411 | T__412 | T__413 | T__414 | T__415 | T__416 | T__417 | T__418 | T__419 | T__420 | T__421 | T__422 | T__423 | T__424 | T__425 | T__426 | T__427 | T__428 | T__429 | T__430 | LAngle | RAngle | LCurly | RCurly | SymEq | Colon | LClose | RClose | Quot | Apos | ANCESTOR | ANCESTOR_OR_SELF | AND | AS | ASCENDING | AT | ATTRIBUTE | BASE_URI | BOUNDARY_SPACE | BY | CASE | CASTABLE | CAST | CHILD | COLLATION | COMMENT | CONSTRUCTION | COPY | COPY_NAMESPACES | DECLARE | DEFAULT | DESCENDANT | DESCENDANT_OR_SELF | DESCENDING | DIV | DOCUMENT | DOCUMENT_NODE | ELEMENT | ELSE | EMPTY | EMPTY_SEQUENCE | ENCODING | EQ | EVERY | EXCEPT | EXTERNAL | FOLLOWING | FOLLOWING_SIBLING | FOR | FUNCTION | GE | GREATEST | GT | IDIV | IF | IMPORT | INHERIT | IN | INSTANCE | INTERSECT | IS | ITEM | LAX | LEAST | LE | LET | LT | MOD | MODULE | NAMESPACE | NE | NODE | ANYKIND | NO_INHERIT | NO_PRESERVE | OF | OPTION | ORDERED | ORDERING | ORDER | OR | PARENT | PRECEDING | PRECEDING_SIBLING | PRESERVE | PROCESSING_INSTRUCTION | RETURN | SATISFIES | SCHEMA_ATTRIBUTE | SCHEMA_ELEMENT | SCHEMA | SELF | SIMPLE | SOME | STABLE | STRICT | STRIP | TEXT | THEN | TO | TREAT | TYPESWITCH | UNION | UNORDERED | VALIDATE | VARIABLE | VERSION | WHERE | XQUERY | AFTER | BEFORE | DELETE | FIRST | INSERT | INTO | LAST | MODIFY | NODES | RENAME | REPLACE | REVALIDATION | SKIP | UPDATING | VALUE | WITH | BLOCK | CONSTANT | EXIT | SEQUENTIAL | RETURNING | SET | WHILE | ALL | ANY | CONTENT | DIACRITICS | DIFFERENT | DISTANCE | END | ENTIRE | EXACTLY | FROM | FTAND | CONTAINS | FTNOT | FT_OPTION | FTOR | INSENSITIVE | LANGUAGE | LEVELS | LOWERCASE | MOST | NO | NOT | OCCURS | PARAGRAPH | PARAGRAPHS | PHRASE | RELATIONSHIP | SAME | SCORE | SENSITIVE | SENTENCE | SENTENCES | START | STEMMING | STOP | THESAURUS | TIMES | UPPERCASE | USING | WEIGHT | WILDCARDS | WINDOW | WITHOUT | WORD | WORDS | CATCH | CONTEXT | DETERMINISTIC | NAMESPACE_NODE | NONDETERMINISTIC | TRY | DECIMAL_FORMAT | DECIMAL_SEPARATOR | DIGIT | GROUPING_SEPARATOR | INFINITY | MINUS_SIGN | NAN | PER_MILLE | PERCENT | PATTERN_SEPARATOR | ZERO_DIGIT | COUNT | GROUP | NEXT | ONLY | PREVIOUS | SLIDING | TUMBLING | WHEN | ALLOWING | DirCommentConstructor | DirPIConstructor | Pragma | IntegerLiteral | DecimalLiteral | DoubleLiteral | QuotedStringLiteral | AposedStringLiteral | PredefinedEntityRef | CharRef | Comment | NCName | S )
        int alt25=241;
        alt25 = dfa25.predict(input);
        switch (alt25) {
            case 1 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:10: T__406
                {
                mT__406(); 

                }
                break;
            case 2 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:17: T__407
                {
                mT__407(); 

                }
                break;
            case 3 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:24: T__408
                {
                mT__408(); 

                }
                break;
            case 4 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:31: T__409
                {
                mT__409(); 

                }
                break;
            case 5 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:38: T__410
                {
                mT__410(); 

                }
                break;
            case 6 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:45: T__411
                {
                mT__411(); 

                }
                break;
            case 7 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:52: T__412
                {
                mT__412(); 

                }
                break;
            case 8 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:59: T__413
                {
                mT__413(); 

                }
                break;
            case 9 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:66: T__414
                {
                mT__414(); 

                }
                break;
            case 10 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:73: T__415
                {
                mT__415(); 

                }
                break;
            case 11 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:80: T__416
                {
                mT__416(); 

                }
                break;
            case 12 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:87: T__417
                {
                mT__417(); 

                }
                break;
            case 13 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:94: T__418
                {
                mT__418(); 

                }
                break;
            case 14 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:101: T__419
                {
                mT__419(); 

                }
                break;
            case 15 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:108: T__420
                {
                mT__420(); 

                }
                break;
            case 16 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:115: T__421
                {
                mT__421(); 

                }
                break;
            case 17 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:122: T__422
                {
                mT__422(); 

                }
                break;
            case 18 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:129: T__423
                {
                mT__423(); 

                }
                break;
            case 19 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:136: T__424
                {
                mT__424(); 

                }
                break;
            case 20 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:143: T__425
                {
                mT__425(); 

                }
                break;
            case 21 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:150: T__426
                {
                mT__426(); 

                }
                break;
            case 22 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:157: T__427
                {
                mT__427(); 

                }
                break;
            case 23 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:164: T__428
                {
                mT__428(); 

                }
                break;
            case 24 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:171: T__429
                {
                mT__429(); 

                }
                break;
            case 25 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:178: T__430
                {
                mT__430(); 

                }
                break;
            case 26 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:185: LAngle
                {
                mLAngle(); 

                }
                break;
            case 27 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:192: RAngle
                {
                mRAngle(); 

                }
                break;
            case 28 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:199: LCurly
                {
                mLCurly(); 

                }
                break;
            case 29 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:206: RCurly
                {
                mRCurly(); 

                }
                break;
            case 30 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:213: SymEq
                {
                mSymEq(); 

                }
                break;
            case 31 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:219: Colon
                {
                mColon(); 

                }
                break;
            case 32 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:225: LClose
                {
                mLClose(); 

                }
                break;
            case 33 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:232: RClose
                {
                mRClose(); 

                }
                break;
            case 34 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:239: Quot
                {
                mQuot(); 

                }
                break;
            case 35 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:244: Apos
                {
                mApos(); 

                }
                break;
            case 36 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:249: ANCESTOR
                {
                mANCESTOR(); 

                }
                break;
            case 37 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:258: ANCESTOR_OR_SELF
                {
                mANCESTOR_OR_SELF(); 

                }
                break;
            case 38 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:275: AND
                {
                mAND(); 

                }
                break;
            case 39 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:279: AS
                {
                mAS(); 

                }
                break;
            case 40 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:282: ASCENDING
                {
                mASCENDING(); 

                }
                break;
            case 41 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:292: AT
                {
                mAT(); 

                }
                break;
            case 42 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:295: ATTRIBUTE
                {
                mATTRIBUTE(); 

                }
                break;
            case 43 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:305: BASE_URI
                {
                mBASE_URI(); 

                }
                break;
            case 44 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:314: BOUNDARY_SPACE
                {
                mBOUNDARY_SPACE(); 

                }
                break;
            case 45 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:329: BY
                {
                mBY(); 

                }
                break;
            case 46 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:332: CASE
                {
                mCASE(); 

                }
                break;
            case 47 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:337: CASTABLE
                {
                mCASTABLE(); 

                }
                break;
            case 48 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:346: CAST
                {
                mCAST(); 

                }
                break;
            case 49 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:351: CHILD
                {
                mCHILD(); 

                }
                break;
            case 50 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:357: COLLATION
                {
                mCOLLATION(); 

                }
                break;
            case 51 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:367: COMMENT
                {
                mCOMMENT(); 

                }
                break;
            case 52 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:375: CONSTRUCTION
                {
                mCONSTRUCTION(); 

                }
                break;
            case 53 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:388: COPY
                {
                mCOPY(); 

                }
                break;
            case 54 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:393: COPY_NAMESPACES
                {
                mCOPY_NAMESPACES(); 

                }
                break;
            case 55 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:409: DECLARE
                {
                mDECLARE(); 

                }
                break;
            case 56 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:417: DEFAULT
                {
                mDEFAULT(); 

                }
                break;
            case 57 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:425: DESCENDANT
                {
                mDESCENDANT(); 

                }
                break;
            case 58 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:436: DESCENDANT_OR_SELF
                {
                mDESCENDANT_OR_SELF(); 

                }
                break;
            case 59 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:455: DESCENDING
                {
                mDESCENDING(); 

                }
                break;
            case 60 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:466: DIV
                {
                mDIV(); 

                }
                break;
            case 61 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:470: DOCUMENT
                {
                mDOCUMENT(); 

                }
                break;
            case 62 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:479: DOCUMENT_NODE
                {
                mDOCUMENT_NODE(); 

                }
                break;
            case 63 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:493: ELEMENT
                {
                mELEMENT(); 

                }
                break;
            case 64 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:501: ELSE
                {
                mELSE(); 

                }
                break;
            case 65 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:506: EMPTY
                {
                mEMPTY(); 

                }
                break;
            case 66 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:512: EMPTY_SEQUENCE
                {
                mEMPTY_SEQUENCE(); 

                }
                break;
            case 67 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:527: ENCODING
                {
                mENCODING(); 

                }
                break;
            case 68 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:536: EQ
                {
                mEQ(); 

                }
                break;
            case 69 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:539: EVERY
                {
                mEVERY(); 

                }
                break;
            case 70 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:545: EXCEPT
                {
                mEXCEPT(); 

                }
                break;
            case 71 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:552: EXTERNAL
                {
                mEXTERNAL(); 

                }
                break;
            case 72 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:561: FOLLOWING
                {
                mFOLLOWING(); 

                }
                break;
            case 73 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:571: FOLLOWING_SIBLING
                {
                mFOLLOWING_SIBLING(); 

                }
                break;
            case 74 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:589: FOR
                {
                mFOR(); 

                }
                break;
            case 75 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:593: FUNCTION
                {
                mFUNCTION(); 

                }
                break;
            case 76 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:602: GE
                {
                mGE(); 

                }
                break;
            case 77 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:605: GREATEST
                {
                mGREATEST(); 

                }
                break;
            case 78 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:614: GT
                {
                mGT(); 

                }
                break;
            case 79 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:617: IDIV
                {
                mIDIV(); 

                }
                break;
            case 80 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:622: IF
                {
                mIF(); 

                }
                break;
            case 81 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:625: IMPORT
                {
                mIMPORT(); 

                }
                break;
            case 82 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:632: INHERIT
                {
                mINHERIT(); 

                }
                break;
            case 83 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:640: IN
                {
                mIN(); 

                }
                break;
            case 84 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:643: INSTANCE
                {
                mINSTANCE(); 

                }
                break;
            case 85 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:652: INTERSECT
                {
                mINTERSECT(); 

                }
                break;
            case 86 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:662: IS
                {
                mIS(); 

                }
                break;
            case 87 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:665: ITEM
                {
                mITEM(); 

                }
                break;
            case 88 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:670: LAX
                {
                mLAX(); 

                }
                break;
            case 89 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:674: LEAST
                {
                mLEAST(); 

                }
                break;
            case 90 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:680: LE
                {
                mLE(); 

                }
                break;
            case 91 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:683: LET
                {
                mLET(); 

                }
                break;
            case 92 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:687: LT
                {
                mLT(); 

                }
                break;
            case 93 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:690: MOD
                {
                mMOD(); 

                }
                break;
            case 94 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:694: MODULE
                {
                mMODULE(); 

                }
                break;
            case 95 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:701: NAMESPACE
                {
                mNAMESPACE(); 

                }
                break;
            case 96 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:711: NE
                {
                mNE(); 

                }
                break;
            case 97 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:714: NODE
                {
                mNODE(); 

                }
                break;
            case 98 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:719: ANYKIND
                {
                mANYKIND(); 

                }
                break;
            case 99 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:727: NO_INHERIT
                {
                mNO_INHERIT(); 

                }
                break;
            case 100 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:738: NO_PRESERVE
                {
                mNO_PRESERVE(); 

                }
                break;
            case 101 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:750: OF
                {
                mOF(); 

                }
                break;
            case 102 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:753: OPTION
                {
                mOPTION(); 

                }
                break;
            case 103 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:760: ORDERED
                {
                mORDERED(); 

                }
                break;
            case 104 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:768: ORDERING
                {
                mORDERING(); 

                }
                break;
            case 105 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:777: ORDER
                {
                mORDER(); 

                }
                break;
            case 106 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:783: OR
                {
                mOR(); 

                }
                break;
            case 107 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:786: PARENT
                {
                mPARENT(); 

                }
                break;
            case 108 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:793: PRECEDING
                {
                mPRECEDING(); 

                }
                break;
            case 109 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:803: PRECEDING_SIBLING
                {
                mPRECEDING_SIBLING(); 

                }
                break;
            case 110 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:821: PRESERVE
                {
                mPRESERVE(); 

                }
                break;
            case 111 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:830: PROCESSING_INSTRUCTION
                {
                mPROCESSING_INSTRUCTION(); 

                }
                break;
            case 112 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:853: RETURN
                {
                mRETURN(); 

                }
                break;
            case 113 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:860: SATISFIES
                {
                mSATISFIES(); 

                }
                break;
            case 114 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:870: SCHEMA_ATTRIBUTE
                {
                mSCHEMA_ATTRIBUTE(); 

                }
                break;
            case 115 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:887: SCHEMA_ELEMENT
                {
                mSCHEMA_ELEMENT(); 

                }
                break;
            case 116 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:902: SCHEMA
                {
                mSCHEMA(); 

                }
                break;
            case 117 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:909: SELF
                {
                mSELF(); 

                }
                break;
            case 118 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:914: SIMPLE
                {
                mSIMPLE(); 

                }
                break;
            case 119 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:921: SOME
                {
                mSOME(); 

                }
                break;
            case 120 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:926: STABLE
                {
                mSTABLE(); 

                }
                break;
            case 121 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:933: STRICT
                {
                mSTRICT(); 

                }
                break;
            case 122 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:940: STRIP
                {
                mSTRIP(); 

                }
                break;
            case 123 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:946: TEXT
                {
                mTEXT(); 

                }
                break;
            case 124 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:951: THEN
                {
                mTHEN(); 

                }
                break;
            case 125 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:956: TO
                {
                mTO(); 

                }
                break;
            case 126 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:959: TREAT
                {
                mTREAT(); 

                }
                break;
            case 127 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:965: TYPESWITCH
                {
                mTYPESWITCH(); 

                }
                break;
            case 128 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:976: UNION
                {
                mUNION(); 

                }
                break;
            case 129 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:982: UNORDERED
                {
                mUNORDERED(); 

                }
                break;
            case 130 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:992: VALIDATE
                {
                mVALIDATE(); 

                }
                break;
            case 131 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1001: VARIABLE
                {
                mVARIABLE(); 

                }
                break;
            case 132 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1010: VERSION
                {
                mVERSION(); 

                }
                break;
            case 133 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1018: WHERE
                {
                mWHERE(); 

                }
                break;
            case 134 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1024: XQUERY
                {
                mXQUERY(); 

                }
                break;
            case 135 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1031: AFTER
                {
                mAFTER(); 

                }
                break;
            case 136 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1037: BEFORE
                {
                mBEFORE(); 

                }
                break;
            case 137 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1044: DELETE
                {
                mDELETE(); 

                }
                break;
            case 138 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1051: FIRST
                {
                mFIRST(); 

                }
                break;
            case 139 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1057: INSERT
                {
                mINSERT(); 

                }
                break;
            case 140 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1064: INTO
                {
                mINTO(); 

                }
                break;
            case 141 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1069: LAST
                {
                mLAST(); 

                }
                break;
            case 142 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1074: MODIFY
                {
                mMODIFY(); 

                }
                break;
            case 143 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1081: NODES
                {
                mNODES(); 

                }
                break;
            case 144 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1087: RENAME
                {
                mRENAME(); 

                }
                break;
            case 145 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1094: REPLACE
                {
                mREPLACE(); 

                }
                break;
            case 146 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1102: REVALIDATION
                {
                mREVALIDATION(); 

                }
                break;
            case 147 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1115: SKIP
                {
                mSKIP(); 

                }
                break;
            case 148 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1120: UPDATING
                {
                mUPDATING(); 

                }
                break;
            case 149 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1129: VALUE
                {
                mVALUE(); 

                }
                break;
            case 150 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1135: WITH
                {
                mWITH(); 

                }
                break;
            case 151 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1140: BLOCK
                {
                mBLOCK(); 

                }
                break;
            case 152 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1146: CONSTANT
                {
                mCONSTANT(); 

                }
                break;
            case 153 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1155: EXIT
                {
                mEXIT(); 

                }
                break;
            case 154 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1160: SEQUENTIAL
                {
                mSEQUENTIAL(); 

                }
                break;
            case 155 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1171: RETURNING
                {
                mRETURNING(); 

                }
                break;
            case 156 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1181: SET
                {
                mSET(); 

                }
                break;
            case 157 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1185: WHILE
                {
                mWHILE(); 

                }
                break;
            case 158 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1191: ALL
                {
                mALL(); 

                }
                break;
            case 159 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1195: ANY
                {
                mANY(); 

                }
                break;
            case 160 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1199: CONTENT
                {
                mCONTENT(); 

                }
                break;
            case 161 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1207: DIACRITICS
                {
                mDIACRITICS(); 

                }
                break;
            case 162 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1218: DIFFERENT
                {
                mDIFFERENT(); 

                }
                break;
            case 163 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1228: DISTANCE
                {
                mDISTANCE(); 

                }
                break;
            case 164 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1237: END
                {
                mEND(); 

                }
                break;
            case 165 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1241: ENTIRE
                {
                mENTIRE(); 

                }
                break;
            case 166 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1248: EXACTLY
                {
                mEXACTLY(); 

                }
                break;
            case 167 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1256: FROM
                {
                mFROM(); 

                }
                break;
            case 168 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1261: FTAND
                {
                mFTAND(); 

                }
                break;
            case 169 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1267: CONTAINS
                {
                mCONTAINS(); 

                }
                break;
            case 170 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1276: FTNOT
                {
                mFTNOT(); 

                }
                break;
            case 171 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1282: FT_OPTION
                {
                mFT_OPTION(); 

                }
                break;
            case 172 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1292: FTOR
                {
                mFTOR(); 

                }
                break;
            case 173 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1297: INSENSITIVE
                {
                mINSENSITIVE(); 

                }
                break;
            case 174 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1309: LANGUAGE
                {
                mLANGUAGE(); 

                }
                break;
            case 175 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1318: LEVELS
                {
                mLEVELS(); 

                }
                break;
            case 176 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1325: LOWERCASE
                {
                mLOWERCASE(); 

                }
                break;
            case 177 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1335: MOST
                {
                mMOST(); 

                }
                break;
            case 178 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1340: NO
                {
                mNO(); 

                }
                break;
            case 179 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1343: NOT
                {
                mNOT(); 

                }
                break;
            case 180 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1347: OCCURS
                {
                mOCCURS(); 

                }
                break;
            case 181 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1354: PARAGRAPH
                {
                mPARAGRAPH(); 

                }
                break;
            case 182 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1364: PARAGRAPHS
                {
                mPARAGRAPHS(); 

                }
                break;
            case 183 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1375: PHRASE
                {
                mPHRASE(); 

                }
                break;
            case 184 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1382: RELATIONSHIP
                {
                mRELATIONSHIP(); 

                }
                break;
            case 185 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1395: SAME
                {
                mSAME(); 

                }
                break;
            case 186 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1400: SCORE
                {
                mSCORE(); 

                }
                break;
            case 187 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1406: SENSITIVE
                {
                mSENSITIVE(); 

                }
                break;
            case 188 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1416: SENTENCE
                {
                mSENTENCE(); 

                }
                break;
            case 189 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1425: SENTENCES
                {
                mSENTENCES(); 

                }
                break;
            case 190 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1435: START
                {
                mSTART(); 

                }
                break;
            case 191 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1441: STEMMING
                {
                mSTEMMING(); 

                }
                break;
            case 192 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1450: STOP
                {
                mSTOP(); 

                }
                break;
            case 193 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1455: THESAURUS
                {
                mTHESAURUS(); 

                }
                break;
            case 194 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1465: TIMES
                {
                mTIMES(); 

                }
                break;
            case 195 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1471: UPPERCASE
                {
                mUPPERCASE(); 

                }
                break;
            case 196 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1481: USING
                {
                mUSING(); 

                }
                break;
            case 197 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1487: WEIGHT
                {
                mWEIGHT(); 

                }
                break;
            case 198 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1494: WILDCARDS
                {
                mWILDCARDS(); 

                }
                break;
            case 199 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1504: WINDOW
                {
                mWINDOW(); 

                }
                break;
            case 200 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1511: WITHOUT
                {
                mWITHOUT(); 

                }
                break;
            case 201 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1519: WORD
                {
                mWORD(); 

                }
                break;
            case 202 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1524: WORDS
                {
                mWORDS(); 

                }
                break;
            case 203 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1530: CATCH
                {
                mCATCH(); 

                }
                break;
            case 204 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1536: CONTEXT
                {
                mCONTEXT(); 

                }
                break;
            case 205 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1544: DETERMINISTIC
                {
                mDETERMINISTIC(); 

                }
                break;
            case 206 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1558: NAMESPACE_NODE
                {
                mNAMESPACE_NODE(); 

                }
                break;
            case 207 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1573: NONDETERMINISTIC
                {
                mNONDETERMINISTIC(); 

                }
                break;
            case 208 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1590: TRY
                {
                mTRY(); 

                }
                break;
            case 209 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1594: DECIMAL_FORMAT
                {
                mDECIMAL_FORMAT(); 

                }
                break;
            case 210 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1609: DECIMAL_SEPARATOR
                {
                mDECIMAL_SEPARATOR(); 

                }
                break;
            case 211 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1627: DIGIT
                {
                mDIGIT(); 

                }
                break;
            case 212 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1633: GROUPING_SEPARATOR
                {
                mGROUPING_SEPARATOR(); 

                }
                break;
            case 213 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1652: INFINITY
                {
                mINFINITY(); 

                }
                break;
            case 214 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1661: MINUS_SIGN
                {
                mMINUS_SIGN(); 

                }
                break;
            case 215 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1672: NAN
                {
                mNAN(); 

                }
                break;
            case 216 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1676: PER_MILLE
                {
                mPER_MILLE(); 

                }
                break;
            case 217 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1686: PERCENT
                {
                mPERCENT(); 

                }
                break;
            case 218 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1694: PATTERN_SEPARATOR
                {
                mPATTERN_SEPARATOR(); 

                }
                break;
            case 219 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1712: ZERO_DIGIT
                {
                mZERO_DIGIT(); 

                }
                break;
            case 220 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1723: COUNT
                {
                mCOUNT(); 

                }
                break;
            case 221 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1729: GROUP
                {
                mGROUP(); 

                }
                break;
            case 222 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1735: NEXT
                {
                mNEXT(); 

                }
                break;
            case 223 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1740: ONLY
                {
                mONLY(); 

                }
                break;
            case 224 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1745: PREVIOUS
                {
                mPREVIOUS(); 

                }
                break;
            case 225 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1754: SLIDING
                {
                mSLIDING(); 

                }
                break;
            case 226 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1762: TUMBLING
                {
                mTUMBLING(); 

                }
                break;
            case 227 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1771: WHEN
                {
                mWHEN(); 

                }
                break;
            case 228 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1776: ALLOWING
                {
                mALLOWING(); 

                }
                break;
            case 229 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1785: DirCommentConstructor
                {
                mDirCommentConstructor(); 

                }
                break;
            case 230 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1807: DirPIConstructor
                {
                mDirPIConstructor(); 

                }
                break;
            case 231 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1824: Pragma
                {
                mPragma(); 

                }
                break;
            case 232 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1831: IntegerLiteral
                {
                mIntegerLiteral(); 

                }
                break;
            case 233 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1846: DecimalLiteral
                {
                mDecimalLiteral(); 

                }
                break;
            case 234 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1861: DoubleLiteral
                {
                mDoubleLiteral(); 

                }
                break;
            case 235 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1875: QuotedStringLiteral
                {
                mQuotedStringLiteral(); 

                }
                break;
            case 236 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1895: AposedStringLiteral
                {
                mAposedStringLiteral(); 

                }
                break;
            case 237 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1915: PredefinedEntityRef
                {
                mPredefinedEntityRef(); 

                }
                break;
            case 238 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1935: CharRef
                {
                mCharRef(); 

                }
                break;
            case 239 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1943: Comment
                {
                mComment(); 

                }
                break;
            case 240 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1951: NCName
                {
                mNCName(); 

                }
                break;
            case 241 :
                // org/brackit/xquery/compiler/parser/XQuery.g:1:1958: S
                {
                mS(); 

                }
                break;

        }

    }


    protected DFA25 dfa25 = new DFA25(this);
    static final String DFA25_eotS =
        "\3\uffff\1\66\3\uffff\1\71\5\uffff\1\77\1\102\1\105\1\uffff\1\107"+
        "\6\uffff\1\111\1\113\26\62\1\u009f\27\uffff\1\u00a4\4\uffff\1\62"+
        "\1\u00a9\1\u00ab\4\62\1\u00b0\13\62\1\u00cc\7\62\1\u00db\1\62\1"+
        "\u00de\1\62\1\u00e0\1\62\1\u00e6\1\u00e7\2\62\1\u00ef\1\u00f0\4"+
        "\62\1\u00f7\1\u00fc\1\u00fd\1\62\1\u0100\21\62\1\u0120\20\62\1\uffff"+
        "\1\u00a4\4\uffff\1\62\1\u013a\1\u013b\1\62\1\uffff\1\62\1\uffff"+
        "\1\62\1\u0140\2\62\1\uffff\17\62\1\u0155\11\62\1\u015f\1\62\1\uffff"+
        "\6\62\1\u0167\7\62\1\uffff\2\62\1\uffff\1\62\1\uffff\5\62\2\uffff"+
        "\1\62\1\u017a\3\62\1\u017e\1\62\2\uffff\1\62\1\u0183\4\62\1\uffff"+
        "\2\62\1\u018b\1\62\2\uffff\2\62\1\uffff\23\62\1\u01a6\13\62\1\uffff"+
        "\1\62\1\u01b6\23\62\1\u01cc\1\62\1\u00a4\1\62\2\uffff\4\62\1\uffff"+
        "\4\62\1\u01d7\1\u01d9\6\62\1\u01e2\7\62\1\uffff\6\62\1\u01f0\2\62"+
        "\1\uffff\4\62\1\u01f7\2\62\1\uffff\2\62\1\u01fc\3\62\1\u0200\2\62"+
        "\1\u0203\5\62\1\u020a\1\62\1\u020c\1\uffff\1\u020d\2\62\1\uffff"+
        "\4\62\1\uffff\1\u0214\2\62\1\u0217\1\u021a\2\62\1\uffff\4\62\1\u0221"+
        "\20\62\1\u0232\2\62\1\u0235\1\62\1\uffff\3\62\1\u023a\4\62\1\u0240"+
        "\1\u0241\1\62\1\u0243\1\u0244\2\62\1\uffff\15\62\1\u0254\1\62\1"+
        "\u0257\3\62\1\u025c\1\62\1\uffff\4\62\1\u0262\4\62\1\u0267\1\uffff"+
        "\1\62\1\uffff\1\u0269\1\u026a\6\62\1\uffff\1\u0273\11\62\1\u027d"+
        "\2\62\1\uffff\1\u0281\2\62\1\u0284\2\62\1\uffff\3\62\1\u028a\1\uffff"+
        "\1\u028b\1\u028c\1\62\1\uffff\1\62\1\u0290\1\uffff\6\62\1\uffff"+
        "\1\62\2\uffff\1\62\1\u0299\4\62\1\uffff\2\62\2\uffff\1\u02a0\1\uffff"+
        "\4\62\1\u02a7\1\62\1\uffff\20\62\1\uffff\1\62\1\u02ba\1\uffff\4"+
        "\62\1\uffff\1\62\1\u02c0\1\62\1\u02c2\1\62\2\uffff\1\62\2\uffff"+
        "\1\62\1\u02c6\1\62\1\u02c8\1\62\1\u02ca\3\62\1\u02ce\1\62\1\u02d0"+
        "\2\62\1\u02d3\1\uffff\1\u02d4\1\62\1\uffff\3\62\1\u02d9\1\uffff"+
        "\5\62\1\uffff\3\62\1\u02e2\1\uffff\1\62\2\uffff\10\62\1\uffff\4"+
        "\62\1\u02f0\4\62\1\uffff\3\62\1\uffff\1\62\1\u02f9\1\uffff\1\u02fa"+
        "\4\62\3\uffff\3\62\1\uffff\1\u0302\2\62\1\u0305\4\62\1\uffff\1\u030a"+
        "\1\62\1\u030c\1\u030d\2\62\1\uffff\3\62\1\u0313\2\62\1\uffff\1\u0316"+
        "\1\u0317\6\62\1\u031e\2\62\1\u0322\1\u0323\4\62\1\u0329\1\uffff"+
        "\3\62\1\u032d\1\u032e\1\uffff\1\u032f\1\uffff\3\62\1\uffff\1\62"+
        "\1\uffff\1\62\1\uffff\3\62\1\uffff\1\62\1\uffff\2\62\2\uffff\2\62"+
        "\1\u033d\1\u033e\1\uffff\1\u033f\7\62\1\uffff\2\62\1\u0349\2\62"+
        "\1\u034c\1\u034d\2\62\1\u0350\1\62\1\u0352\1\62\1\uffff\5\62\1\u035a"+
        "\2\62\2\uffff\1\62\1\u035e\5\62\1\uffff\1\u0364\1\62\1\uffff\4\62"+
        "\1\uffff\1\62\2\uffff\5\62\1\uffff\1\u0370\1\62\2\uffff\6\62\1\uffff"+
        "\1\62\1\u0379\1\62\2\uffff\1\u037b\4\62\1\uffff\3\62\3\uffff\1\62"+
        "\1\u0385\10\62\1\u038e\1\u038f\1\62\3\uffff\1\62\1\u0393\2\62\1"+
        "\u0396\1\u0397\1\62\1\u0399\1\62\1\uffff\1\62\1\u039c\2\uffff\1"+
        "\u039d\1\62\1\uffff\1\62\1\uffff\5\62\1\u03a6\1\u03a8\1\uffff\1"+
        "\62\1\u03aa\1\u03ab\1\uffff\1\62\1\u03ad\1\62\1\u03af\1\62\1\uffff"+
        "\1\u03b1\2\62\1\u03b4\1\u03b5\6\62\1\uffff\1\u03bc\3\62\1\u03c0"+
        "\1\u03c1\2\62\1\uffff\1\62\1\uffff\7\62\1\u03cd\1\u03ce\1\uffff"+
        "\2\62\1\u03d1\1\62\1\u03d3\1\62\1\u03d5\1\u03d6\2\uffff\3\62\1\uffff"+
        "\1\u03da\1\u03db\2\uffff\1\62\1\uffff\1\u03dd\1\62\2\uffff\7\62"+
        "\1\u03e6\1\uffff\1\62\1\uffff\1\62\2\uffff\1\u03ea\1\uffff\1\u03eb"+
        "\1\uffff\1\62\1\uffff\1\62\1\u03ee\2\uffff\1\u03ef\1\62\1\u03f2"+
        "\3\62\1\uffff\1\u03f7\1\62\1\u03fa\2\uffff\1\62\1\u03fc\1\u03fd"+
        "\2\62\1\u0400\3\62\1\u0404\1\u0405\2\uffff\1\u0406\1\62\1\uffff"+
        "\1\u0408\1\uffff\1\u0409\2\uffff\1\u040a\2\62\2\uffff\1\62\1\uffff"+
        "\4\62\1\u0413\1\u0414\1\62\1\u0416\1\uffff\3\62\2\uffff\2\62\2\uffff"+
        "\1\u041c\1\62\1\uffff\1\u041e\2\62\1\u0421\1\uffff\2\62\1\uffff"+
        "\1\62\2\uffff\2\62\1\uffff\2\62\1\u0429\3\uffff\1\u042a\3\uffff"+
        "\1\u042b\7\62\2\uffff\1\62\1\uffff\4\62\1\u0438\1\uffff\1\62\1\uffff"+
        "\1\u043a\1\62\1\uffff\7\62\3\uffff\2\62\1\u0445\11\62\1\uffff\1"+
        "\62\1\uffff\4\62\1\u0454\1\u0455\4\62\1\uffff\4\62\1\u045e\1\u045f"+
        "\10\62\2\uffff\3\62\1\u046b\1\62\1\u046d\2\62\2\uffff\1\u0470\2"+
        "\62\1\u0473\5\62\1\u0479\1\62\1\uffff\1\u047b\1\uffff\2\62\1\uffff"+
        "\2\62\1\uffff\5\62\1\uffff\1\u0485\1\uffff\4\62\1\u048a\3\62\1\u048e"+
        "\1\uffff\1\u048f\1\62\1\u0491\1\62\1\uffff\1\u0493\1\u0494\1\62"+
        "\2\uffff\1\u0496\1\uffff\1\u0497\2\uffff\1\62\2\uffff\3\62\1\u049c"+
        "\1\uffff";
    static final String DFA25_eofS =
        "\u049d\uffff";
    static final String DFA25_minS =
        "\1\11\2\uffff\1\43\3\uffff\1\72\5\uffff\1\41\1\75\1\57\1\uffff\1"+
        "\56\6\uffff\2\0\1\146\2\141\1\145\1\154\1\151\1\145\1\144\1\141"+
        "\1\151\1\141\1\143\1\141\1\145\1\141\1\145\1\156\1\141\1\145\1\161"+
        "\1\141\1\145\1\56\1\43\26\uffff\1\60\4\uffff\1\143\2\55\1\164\1"+
        "\154\1\163\1\165\1\55\1\146\1\157\1\163\1\151\1\154\1\143\1\141"+
        "\1\143\1\145\1\160\1\143\1\55\1\145\1\141\1\154\1\156\1\162\1\157"+
        "\2\55\1\145\1\55\1\151\1\55\1\160\2\55\1\145\1\156\2\55\1\167\1"+
        "\144\1\156\1\155\3\55\1\164\1\55\1\143\1\154\1\162\1\145\2\162\1"+
        "\154\1\155\1\150\1\154\2\155\1\141\2\151\1\170\1\145\1\55\1\145"+
        "\1\160\2\155\1\151\1\144\1\151\1\154\1\162\1\145\1\154\1\151\1\162"+
        "\1\165\1\116\1\162\1\uffff\1\60\4\uffff\1\145\2\55\1\145\1\uffff"+
        "\1\162\1\uffff\1\145\1\55\1\145\1\156\1\uffff\1\157\1\143\1\145"+
        "\1\143\2\154\1\155\1\163\1\171\1\156\1\151\1\141\1\143\2\145\1\55"+
        "\1\143\1\146\1\164\1\151\1\165\1\155\1\145\1\164\1\157\1\55\1\151"+
        "\1\uffff\1\162\2\145\1\164\1\143\1\154\1\55\1\143\1\163\1\155\1"+
        "\156\2\157\1\162\1\uffff\1\141\1\165\1\uffff\1\166\1\uffff\1\157"+
        "\3\145\1\151\2\uffff\1\155\1\55\1\164\1\147\1\163\1\55\1\145\2\uffff"+
        "\1\145\1\55\1\164\1\165\1\145\1\164\1\uffff\1\145\1\151\1\55\1\144"+
        "\2\uffff\1\151\1\145\1\uffff\1\165\1\171\1\141\1\164\2\143\1\141"+
        "\1\55\1\165\1\141\1\154\2\141\1\151\2\145\1\162\1\146\1\165\1\55"+
        "\1\163\1\160\1\145\1\142\1\151\1\155\2\160\1\144\1\164\1\156\1\uffff"+
        "\1\141\1\55\2\145\1\142\1\157\1\162\1\141\1\145\1\156\2\151\1\163"+
        "\1\156\1\154\1\150\2\144\1\147\1\144\1\145\1\55\1\157\1\60\1\163"+
        "\2\uffff\1\156\1\151\1\162\1\167\1\uffff\1\55\1\144\1\162\1\153"+
        "\2\55\1\150\1\144\1\141\1\145\1\164\1\141\1\55\1\164\1\141\1\155"+
        "\1\165\1\145\1\164\1\162\1\uffff\1\162\1\145\1\141\1\164\1\155\1"+
        "\145\1\55\1\171\1\144\1\uffff\1\162\1\171\1\160\1\162\1\55\1\164"+
        "\1\157\1\uffff\2\164\1\55\1\144\1\164\1\160\1\55\1\164\1\160\1\55"+
        "\2\162\1\141\1\156\1\162\1\55\1\156\1\55\1\uffff\1\55\1\165\1\164"+
        "\1\uffff\1\154\1\162\1\154\1\146\1\uffff\1\55\2\163\1\55\1\50\1"+
        "\156\1\162\1\uffff\1\145\1\157\2\162\1\55\1\156\1\147\3\145\1\151"+
        "\1\145\1\163\1\155\1\145\1\162\1\155\1\141\1\154\1\164\1\163\1\55"+
        "\1\155\1\145\1\55\1\145\1\uffff\1\151\1\145\1\154\1\55\1\154\1\164"+
        "\1\143\1\155\2\55\1\151\2\55\1\141\1\164\1\uffff\2\163\1\154\1\156"+
        "\1\144\1\164\1\162\1\147\1\144\1\145\1\141\1\151\1\145\1\55\1\145"+
        "\1\55\1\143\1\157\1\150\1\55\1\162\1\uffff\1\55\1\164\1\144\1\142"+
        "\1\55\1\151\1\165\1\141\1\145\1\55\1\uffff\1\142\1\uffff\2\55\1"+
        "\164\1\156\1\141\1\156\1\151\1\156\1\uffff\1\55\1\162\1\141\1\154"+
        "\1\156\1\145\1\155\1\151\1\162\1\156\1\55\1\145\1\156\1\uffff\1"+
        "\55\1\151\1\145\1\55\1\164\1\156\1\uffff\1\154\1\167\1\151\1\55"+
        "\1\uffff\2\55\1\164\1\uffff\1\145\1\55\1\uffff\1\164\1\151\1\156"+
        "\1\164\2\163\1\uffff\1\151\2\uffff\1\141\1\55\1\163\1\143\1\145"+
        "\1\171\1\uffff\1\55\1\160\2\uffff\1\55\1\uffff\1\150\1\145\1\164"+
        "\1\156\1\55\1\163\1\uffff\1\164\2\162\1\144\1\162\1\157\1\163\1"+
        "\145\1\151\2\156\1\145\1\143\2\151\1\146\1\uffff\1\141\1\55\1\uffff"+
        "\1\156\1\164\1\156\1\145\1\uffff\1\145\1\55\1\164\1\55\1\151\2\uffff"+
        "\1\156\2\uffff\1\165\1\55\1\167\1\55\1\151\1\55\1\145\1\151\1\143"+
        "\1\55\1\141\1\55\1\142\1\157\1\55\1\uffff\1\55\1\165\1\uffff\1\141"+
        "\1\167\1\164\1\55\1\uffff\1\171\1\144\1\157\1\151\1\165\1\uffff"+
        "\1\156\2\162\1\55\1\uffff\1\154\2\uffff\1\151\1\164\1\165\1\156"+
        "\2\164\1\156\1\141\1\uffff\1\145\1\154\1\164\1\144\1\55\1\151\1"+
        "\164\1\145\1\143\1\uffff\1\156\1\164\1\163\1\uffff\1\156\1\55\1"+
        "\uffff\1\55\1\141\1\171\1\151\1\157\3\uffff\1\151\1\163\1\156\1"+
        "\uffff\1\55\1\164\1\143\1\55\1\151\1\145\1\164\1\147\1\uffff\1\55"+
        "\1\141\2\55\1\163\1\141\1\uffff\1\145\1\163\1\145\1\55\1\144\1\156"+
        "\1\uffff\2\55\1\141\1\156\1\151\1\166\1\165\1\163\1\55\1\154\1\164"+
        "\2\55\1\145\1\144\1\157\1\151\1\55\1\uffff\1\164\1\151\1\143\2\55"+
        "\1\uffff\1\55\1\uffff\1\156\1\147\1\162\1\uffff\1\151\1\uffff\1"+
        "\156\1\uffff\1\162\1\156\1\141\1\uffff\1\164\1\uffff\1\154\1\156"+
        "\2\uffff\1\164\1\162\2\55\1\uffff\1\55\1\151\1\162\1\156\1\164\1"+
        "\147\1\151\1\171\1\uffff\1\145\1\157\1\55\1\143\1\164\2\55\1\163"+
        "\1\155\3\55\1\141\1\uffff\1\156\1\151\1\156\1\145\1\164\1\55\1\145"+
        "\1\147\2\uffff\1\154\1\55\2\156\1\157\1\164\1\147\1\uffff\1\55\1"+
        "\145\1\uffff\1\164\1\143\1\171\1\145\1\uffff\1\163\2\uffff\1\151"+
        "\1\143\1\162\1\145\1\162\1\uffff\1\55\1\147\2\uffff\1\160\1\55\1"+
        "\156\1\145\1\163\1\151\1\uffff\1\154\1\55\1\156\2\uffff\1\55\1\141"+
        "\1\156\1\145\1\141\1\uffff\1\151\1\166\1\145\3\uffff\1\147\1\55"+
        "\1\165\1\164\1\147\1\145\1\147\1\163\2\145\2\55\1\144\3\uffff\1"+
        "\147\1\55\1\147\1\145\4\55\1\156\1\uffff\1\164\1\55\2\uffff\1\55"+
        "\1\145\1\uffff\1\146\1\uffff\2\156\1\151\1\143\1\164\2\55\1\uffff"+
        "\1\161\2\55\1\uffff\1\147\1\55\1\156\2\55\1\uffff\1\55\1\151\1\164"+
        "\2\55\1\145\1\147\1\145\1\151\1\162\1\155\1\uffff\1\55\1\150\1\163"+
        "\1\147\2\55\1\156\1\145\1\uffff\1\147\1\uffff\1\164\2\163\1\164"+
        "\1\154\1\141\1\145\2\55\1\uffff\1\163\1\143\1\55\1\144\1\55\1\145"+
        "\2\55\2\uffff\1\163\1\151\1\157\1\uffff\2\55\2\uffff\1\163\1\uffff"+
        "\1\55\1\151\2\uffff\1\163\1\157\1\145\1\164\1\147\2\163\1\55\1\uffff"+
        "\1\156\1\uffff\1\165\2\uffff\1\55\1\uffff\1\55\1\uffff\1\163\1\uffff"+
        "\1\166\1\55\2\uffff\1\55\1\156\1\55\1\164\1\166\1\151\1\uffff\1"+
        "\55\1\145\1\55\2\uffff\1\147\2\55\1\151\1\150\1\55\1\164\1\145\1"+
        "\154\2\55\2\uffff\1\55\1\150\1\uffff\1\55\1\uffff\1\55\2\uffff\1"+
        "\55\1\164\1\162\2\uffff\1\160\1\uffff\1\157\1\160\1\162\1\160\2"+
        "\55\1\164\1\55\1\uffff\1\157\1\145\1\163\2\uffff\2\145\2\uffff\1"+
        "\55\1\156\1\uffff\1\55\1\145\1\156\1\55\1\uffff\1\160\1\163\1\uffff"+
        "\1\55\2\uffff\1\157\1\151\1\uffff\1\162\1\155\1\55\3\uffff\1\55"+
        "\3\uffff\2\55\1\141\1\156\1\141\1\155\1\141\1\157\2\uffff\1\151"+
        "\1\uffff\1\144\1\156\1\151\1\160\1\55\1\uffff\1\157\1\uffff\1\55"+
        "\1\151\1\uffff\1\141\2\151\1\156\1\160\1\151\1\145\3\uffff\1\163"+
        "\1\143\1\55\1\143\1\141\2\162\1\143\1\145\1\143\1\142\1\141\1\uffff"+
        "\1\144\1\uffff\1\163\1\162\1\142\1\156\2\55\1\142\1\156\2\145\1"+
        "\uffff\1\145\1\164\1\141\3\55\1\145\1\154\1\162\1\145\1\164\1\141"+
        "\1\154\1\163\2\uffff\1\165\1\164\1\154\1\55\1\163\1\55\1\164\1\163"+
        "\2\uffff\1\55\1\151\1\141\1\55\1\151\1\164\1\151\2\164\1\55\1\146"+
        "\1\uffff\1\55\1\uffff\1\157\1\145\1\uffff\1\156\1\164\1\uffff\1"+
        "\143\1\157\1\156\1\162\1\145\1\uffff\1\55\1\uffff\1\162\1\154\1"+
        "\147\1\160\1\55\1\162\1\147\1\165\1\55\1\uffff\1\55\1\146\1\55\1"+
        "\162\1\uffff\2\55\1\143\2\uffff\1\55\1\uffff\1\55\2\uffff\1\164"+
        "\2\uffff\1\151\1\157\1\156\1\55\1\uffff";
    static final String DFA25_maxS =
        "\1\ud7a3\2\uffff\1\72\3\uffff\1\75\5\uffff\1\77\2\76\1\uffff\1\71"+
        "\6\uffff\2\uffff\1\164\1\171\2\157\1\170\1\165\3\164\2\157\2\162"+
        "\1\145\1\164\1\171\1\163\1\145\1\157\1\161\1\141\2\145\1\161\26"+
        "\uffff\1\145\4\uffff\1\171\2\ufffd\1\164\1\154\1\163\1\165\1\ufffd"+
        "\1\146\1\157\1\164\1\151\1\165\1\164\1\166\1\143\1\163\1\160\1\164"+
        "\1\ufffd\1\145\1\164\1\162\1\156\1\162\2\157\1\ufffd\1\157\1\ufffd"+
        "\1\151\1\ufffd\1\160\2\ufffd\1\145\1\170\2\ufffd\1\167\1\163\1\156"+
        "\1\155\3\ufffd\1\164\1\ufffd\1\143\1\154\1\164\1\157\2\162\1\166"+
        "\1\164\1\157\1\164\2\155\1\162\2\151\1\170\1\145\1\ufffd\1\171\1"+
        "\160\2\155\1\157\1\160\1\151\2\162\1\151\1\164\1\151\1\162\1\165"+
        "\1\116\1\162\1\uffff\1\145\4\uffff\1\145\2\ufffd\1\145\1\uffff\1"+
        "\162\1\uffff\1\145\1\ufffd\1\145\1\156\1\uffff\1\157\1\143\1\164"+
        "\1\143\2\154\1\155\1\164\1\171\1\156\1\154\1\141\1\143\2\145\1\ufffd"+
        "\1\143\1\146\1\164\1\151\1\165\1\155\1\145\1\164\1\157\1\ufffd\1"+
        "\151\1\uffff\1\162\2\145\1\164\1\143\1\154\1\ufffd\1\143\1\163\1"+
        "\155\1\156\2\157\1\162\1\uffff\1\141\1\165\1\uffff\1\166\1\uffff"+
        "\1\157\1\145\1\164\1\157\1\151\2\uffff\1\155\1\ufffd\1\164\1\147"+
        "\1\163\1\ufffd\1\145\2\uffff\1\145\1\ufffd\1\164\1\165\1\145\1\164"+
        "\1\uffff\1\145\1\160\1\ufffd\1\144\2\uffff\1\151\1\145\1\uffff\1"+
        "\165\1\171\1\145\1\164\1\166\1\143\1\141\1\143\1\165\1\141\1\154"+
        "\2\141\1\151\2\145\1\162\1\146\1\165\1\ufffd\1\164\1\160\1\145\1"+
        "\162\1\151\1\155\2\160\1\144\1\164\1\163\1\uffff\1\141\1\ufffd\2"+
        "\145\1\142\1\157\1\162\1\141\1\145\1\156\1\165\1\151\1\163\1\162"+
        "\1\154\1\150\2\144\1\147\1\144\1\145\1\ufffd\1\157\1\145\1\163\2"+
        "\uffff\1\156\1\151\1\162\1\167\1\uffff\1\55\1\144\1\162\1\153\2"+
        "\ufffd\1\150\1\144\1\141\1\145\1\164\1\145\1\ufffd\1\164\1\141\1"+
        "\155\1\165\1\145\1\164\1\162\1\uffff\1\162\1\145\1\141\1\164\1\155"+
        "\1\145\1\ufffd\1\171\1\144\1\uffff\1\162\1\171\1\160\1\162\1\ufffd"+
        "\1\164\1\157\1\uffff\2\164\1\ufffd\1\144\1\164\1\160\1\ufffd\1\164"+
        "\1\160\1\ufffd\2\162\1\141\2\162\1\ufffd\1\156\1\ufffd\1\uffff\1"+
        "\ufffd\1\165\1\164\1\uffff\1\154\1\162\1\154\1\146\1\uffff\1\ufffd"+
        "\2\163\2\ufffd\1\156\1\162\1\uffff\1\145\1\157\2\162\1\ufffd\1\156"+
        "\1\147\3\145\1\151\1\145\1\163\1\155\1\145\1\162\1\155\1\141\1\154"+
        "\1\164\1\163\1\ufffd\1\155\1\145\1\ufffd\1\145\1\uffff\1\151\1\145"+
        "\1\154\1\ufffd\1\154\1\164\1\160\1\155\2\ufffd\1\151\2\ufffd\1\141"+
        "\1\164\1\uffff\2\163\1\154\1\156\1\144\1\164\1\162\1\147\1\144\1"+
        "\145\1\141\1\151\1\145\1\ufffd\1\145\1\ufffd\1\143\1\157\1\150\1"+
        "\ufffd\1\162\1\uffff\1\55\1\164\1\144\1\142\1\ufffd\1\151\1\165"+
        "\1\141\1\145\1\ufffd\1\uffff\1\142\1\uffff\2\ufffd\1\164\1\156\1"+
        "\162\1\170\1\151\1\156\1\uffff\1\ufffd\1\162\1\141\1\154\1\156\1"+
        "\145\1\155\1\151\1\162\1\156\1\ufffd\1\145\1\156\1\uffff\1\ufffd"+
        "\1\151\1\145\1\ufffd\1\164\1\156\1\uffff\1\154\1\167\1\151\1\ufffd"+
        "\1\uffff\2\ufffd\1\164\1\uffff\1\145\1\ufffd\1\uffff\1\164\1\151"+
        "\1\156\1\164\2\163\1\uffff\1\151\2\uffff\1\141\1\ufffd\1\163\1\143"+
        "\1\145\1\171\1\uffff\1\55\1\160\2\uffff\1\ufffd\1\uffff\1\150\1"+
        "\145\1\164\1\156\1\ufffd\1\163\1\uffff\1\164\2\162\1\144\1\162\1"+
        "\157\1\163\1\145\1\151\2\156\1\145\1\143\2\151\1\146\1\uffff\1\141"+
        "\1\ufffd\1\uffff\1\156\1\164\1\156\1\145\1\uffff\1\145\1\ufffd\1"+
        "\164\1\ufffd\1\151\2\uffff\1\156\2\uffff\1\165\1\ufffd\1\167\1\ufffd"+
        "\1\151\1\ufffd\1\145\1\151\1\143\1\ufffd\1\141\1\ufffd\1\142\1\157"+
        "\1\ufffd\1\uffff\1\ufffd\1\165\1\uffff\1\141\1\167\1\164\1\ufffd"+
        "\1\uffff\1\171\1\144\1\157\1\151\1\165\1\uffff\1\156\2\162\1\ufffd"+
        "\1\uffff\1\154\2\uffff\1\151\1\164\1\165\1\156\2\164\1\156\1\141"+
        "\1\uffff\1\145\1\154\1\164\1\144\1\ufffd\1\151\1\164\1\145\1\143"+
        "\1\uffff\1\156\1\164\1\163\1\uffff\1\156\1\ufffd\1\uffff\1\ufffd"+
        "\1\141\1\171\1\151\1\157\3\uffff\1\151\1\163\1\156\1\uffff\1\ufffd"+
        "\1\164\1\143\1\ufffd\1\151\1\145\1\164\1\147\1\uffff\1\ufffd\1\141"+
        "\2\ufffd\1\163\1\141\1\uffff\1\145\1\163\1\145\1\ufffd\1\144\1\156"+
        "\1\uffff\2\ufffd\1\141\1\156\1\151\1\166\1\165\1\163\1\ufffd\1\154"+
        "\1\164\2\ufffd\1\145\1\144\1\157\1\151\1\ufffd\1\uffff\1\164\1\151"+
        "\1\143\2\ufffd\1\uffff\1\ufffd\1\uffff\1\156\1\147\1\162\1\uffff"+
        "\1\151\1\uffff\1\156\1\uffff\1\162\1\156\1\141\1\uffff\1\164\1\uffff"+
        "\1\154\1\156\2\uffff\1\164\1\162\2\ufffd\1\uffff\1\ufffd\1\151\1"+
        "\162\1\156\1\164\1\147\1\151\1\171\1\uffff\1\145\1\157\1\ufffd\1"+
        "\143\1\164\2\ufffd\1\163\1\155\1\ufffd\1\55\1\ufffd\1\151\1\uffff"+
        "\1\156\1\151\1\156\1\145\1\164\1\ufffd\1\145\1\147\2\uffff\1\154"+
        "\1\ufffd\2\156\1\157\1\164\1\147\1\uffff\1\ufffd\1\145\1\uffff\1"+
        "\164\1\143\1\171\1\145\1\uffff\1\163\2\uffff\1\151\1\143\1\162\1"+
        "\145\1\162\1\uffff\1\ufffd\1\147\2\uffff\1\160\1\55\1\156\1\145"+
        "\1\163\1\151\1\uffff\1\154\1\ufffd\1\156\2\uffff\1\ufffd\1\141\1"+
        "\156\2\145\1\uffff\1\151\1\166\1\145\3\uffff\1\147\1\ufffd\1\165"+
        "\1\164\1\147\1\145\1\147\1\163\2\145\2\ufffd\1\144\3\uffff\1\147"+
        "\1\ufffd\1\147\1\145\2\ufffd\1\55\1\ufffd\1\156\1\uffff\1\164\1"+
        "\ufffd\2\uffff\1\ufffd\1\145\1\uffff\1\163\1\uffff\2\156\1\151\1"+
        "\143\1\164\2\ufffd\1\uffff\1\161\2\ufffd\1\uffff\1\147\1\ufffd\1"+
        "\156\1\ufffd\1\55\1\uffff\1\ufffd\1\151\1\164\2\ufffd\1\145\1\147"+
        "\1\145\1\151\1\162\1\155\1\uffff\1\ufffd\1\150\1\163\1\147\2\ufffd"+
        "\1\156\1\145\1\uffff\1\147\1\uffff\1\164\2\163\1\164\1\154\1\141"+
        "\1\145\2\ufffd\1\uffff\1\163\1\143\1\ufffd\1\144\1\ufffd\1\145\2"+
        "\ufffd\2\uffff\1\163\1\151\1\157\1\uffff\2\ufffd\2\uffff\1\163\1"+
        "\uffff\1\ufffd\1\151\2\uffff\1\163\1\157\1\145\1\164\1\147\2\163"+
        "\1\ufffd\1\uffff\1\156\1\uffff\1\165\2\uffff\1\ufffd\1\uffff\1\ufffd"+
        "\1\uffff\1\163\1\uffff\1\166\1\ufffd\2\uffff\1\ufffd\1\156\1\ufffd"+
        "\1\164\1\166\1\151\1\uffff\1\ufffd\1\145\1\ufffd\2\uffff\1\147\2"+
        "\ufffd\1\151\1\150\1\ufffd\1\164\1\145\1\154\2\ufffd\2\uffff\1\ufffd"+
        "\1\150\1\uffff\1\ufffd\1\uffff\1\ufffd\2\uffff\1\ufffd\1\164\1\162"+
        "\2\uffff\1\160\1\uffff\1\157\1\160\1\162\1\160\2\ufffd\1\164\1\ufffd"+
        "\1\uffff\1\157\1\145\1\163\2\uffff\2\145\2\uffff\1\ufffd\1\156\1"+
        "\uffff\1\ufffd\1\145\1\156\1\ufffd\1\uffff\1\160\1\163\1\uffff\1"+
        "\55\2\uffff\1\157\1\151\1\uffff\1\162\1\155\1\ufffd\3\uffff\1\ufffd"+
        "\3\uffff\1\ufffd\1\55\1\141\1\156\1\141\1\155\1\141\1\157\2\uffff"+
        "\1\151\1\uffff\1\144\1\156\1\151\1\160\1\ufffd\1\uffff\1\157\1\uffff"+
        "\1\ufffd\1\151\1\uffff\1\141\2\151\1\156\1\160\1\151\1\145\3\uffff"+
        "\1\163\1\143\1\ufffd\1\143\1\141\2\162\1\143\1\145\1\143\1\142\1"+
        "\141\1\uffff\1\144\1\uffff\1\163\1\162\1\142\1\156\2\ufffd\1\142"+
        "\1\156\2\145\1\uffff\1\145\1\164\1\141\1\55\2\ufffd\1\145\1\154"+
        "\1\162\1\145\1\164\1\141\1\154\1\163\2\uffff\1\165\1\164\1\154\1"+
        "\ufffd\1\163\1\ufffd\1\164\1\163\2\uffff\1\ufffd\1\151\1\141\1\ufffd"+
        "\1\151\1\164\1\151\2\164\1\ufffd\1\146\1\uffff\1\ufffd\1\uffff\1"+
        "\157\1\145\1\uffff\1\156\1\164\1\uffff\1\143\1\157\1\156\1\162\1"+
        "\145\1\uffff\1\ufffd\1\uffff\1\162\1\154\1\147\1\160\1\ufffd\1\162"+
        "\1\147\1\165\1\ufffd\1\uffff\1\ufffd\1\146\1\ufffd\1\162\1\uffff"+
        "\2\ufffd\1\143\2\uffff\1\ufffd\1\uffff\1\ufffd\2\uffff\1\164\2\uffff"+
        "\1\151\1\157\1\156\1\ufffd\1\uffff";
    static final String DFA25_acceptS =
        "\1\uffff\1\1\1\2\1\uffff\1\4\1\5\1\6\1\uffff\1\10\1\11\1\12\1\13"+
        "\1\14\3\uffff\1\24\1\uffff\1\26\1\27\1\31\1\34\1\35\1\36\32\uffff"+
        "\1\u00f0\1\u00f1\1\u00e7\1\u00ef\1\3\1\7\1\23\1\37\1\15\1\17\1\40"+
        "\1\u00e5\1\u00e6\1\32\1\16\1\20\1\33\1\22\1\41\1\21\1\25\1\30\1"+
        "\uffff\1\42\1\u00eb\1\43\1\u00ec\122\uffff\1\u00e8\1\uffff\1\u00ea"+
        "\1\u00ee\1\u00ed\1\u00e9\4\uffff\1\47\1\uffff\1\51\4\uffff\1\55"+
        "\33\uffff\1\104\16\uffff\1\114\2\uffff\1\116\1\uffff\1\120\5\uffff"+
        "\1\123\1\126\7\uffff\1\132\1\134\6\uffff\1\140\4\uffff\1\u00b2\1"+
        "\145\2\uffff\1\152\37\uffff\1\175\31\uffff\1\46\1\u009f\4\uffff"+
        "\1\u009e\24\uffff\1\74\11\uffff\1\u00a4\7\uffff\1\112\22\uffff\1"+
        "\130\3\uffff\1\133\4\uffff\1\135\7\uffff\1\u00b3\32\uffff\1\u009c"+
        "\17\uffff\1\u00d0\25\uffff\1\u00d7\12\uffff\1\56\1\uffff\1\60\10"+
        "\uffff\1\65\15\uffff\1\100\6\uffff\1\u0099\4\uffff\1\u00a7\3\uffff"+
        "\1\u00ac\2\uffff\1\117\6\uffff\1\u008c\1\uffff\1\127\1\u008d\6\uffff"+
        "\1\u00b1\2\uffff\1\u00de\1\142\1\uffff\1\141\6\uffff\1\u00df\20"+
        "\uffff\1\u00b9\2\uffff\1\165\4\uffff\1\167\5\uffff\1\u00c0\1\u0093"+
        "\1\uffff\1\173\1\174\17\uffff\1\u00e3\2\uffff\1\u0096\4\uffff\1"+
        "\u00c9\5\uffff\1\u0087\4\uffff\1\u0097\1\uffff\1\u00cb\1\61\10\uffff"+
        "\1\u00dc\11\uffff\1\u00d3\3\uffff\1\101\2\uffff\1\105\5\uffff\1"+
        "\u008a\1\u00a8\1\u00aa\3\uffff\1\u00dd\10\uffff\1\131\6\uffff\1"+
        "\u008f\6\uffff\1\151\22\uffff\1\u00ba\5\uffff\1\u00be\1\uffff\1"+
        "\172\3\uffff\1\176\1\uffff\1\u00c2\1\uffff\1\u0080\3\uffff\1\u00c4"+
        "\1\uffff\1\u0095\2\uffff\1\u0085\1\u009d\4\uffff\1\u00ca\10\uffff"+
        "\1\u0088\15\uffff\1\u0089\10\uffff\1\u00a5\1\106\7\uffff\1\121\2"+
        "\uffff\1\u008b\4\uffff\1\u00af\1\uffff\1\136\1\u008e\5\uffff\1\146"+
        "\2\uffff\1\u00b4\1\153\6\uffff\1\u00b7\3\uffff\1\160\1\u0090\5\uffff"+
        "\1\164\3\uffff\1\166\1\170\1\171\15\uffff\1\u00c7\1\u00c5\1\u0086"+
        "\11\uffff\1\63\2\uffff\1\u00a0\1\u00cc\2\uffff\1\67\1\uffff\1\70"+
        "\7\uffff\1\77\3\uffff\1\u00a6\5\uffff\1\122\13\uffff\1\147\10\uffff"+
        "\1\u00d9\1\uffff\1\u0091\11\uffff\1\u00e1\10\uffff\1\u0084\1\u00c8"+
        "\3\uffff\1\44\2\uffff\1\u00e4\1\53\1\uffff\1\57\2\uffff\1\u0098"+
        "\1\u00a9\10\uffff\1\u00a3\1\uffff\1\75\1\uffff\1\103\1\107\1\uffff"+
        "\1\113\1\uffff\1\115\1\uffff\1\124\2\uffff\1\u00d5\1\u00ae\6\uffff"+
        "\1\150\3\uffff\1\156\1\u00e0\13\uffff\1\u00bc\1\u00bf\2\uffff\1"+
        "\u00e2\1\uffff\1\u0094\1\uffff\1\u0082\1\u0083\3\uffff\1\50\1\52"+
        "\1\uffff\1\62\10\uffff\1\u00a2\3\uffff\1\110\1\u00ab\2\uffff\1\125"+
        "\1\u00b0\2\uffff\1\137\4\uffff\1\u00b5\2\uffff\1\154\1\uffff\1\u00d8"+
        "\1\u009b\2\uffff\1\161\3\uffff\1\u00bb\1\u00bd\1\u00c1\1\uffff\1"+
        "\u0081\1\u00c3\1\u00c6\10\uffff\1\71\1\73\1\uffff\1\u00a1\5\uffff"+
        "\1\u00d6\1\uffff\1\143\2\uffff\1\u00b6\7\uffff\1\u009a\1\177\1\u00db"+
        "\14\uffff\1\u00ad\1\uffff\1\144\12\uffff\1\64\16\uffff\1\u0092\1"+
        "\u00b8\10\uffff\1\u00cd\1\76\13\uffff\1\54\1\uffff\1\u00d1\2\uffff"+
        "\1\102\2\uffff\1\u00ce\5\uffff\1\163\1\uffff\1\66\11\uffff\1\45"+
        "\4\uffff\1\u00cf\3\uffff\1\162\1\u00d2\1\uffff\1\111\1\uffff\1\u00da"+
        "\1\155\1\uffff\1\72\1\u00d4\4\uffff\1\157";
    static final String DFA25_specialS =
        "\30\uffff\1\0\1\1\u0483\uffff}>";
    static final String[] DFA25_transitionS = {
            "\2\63\2\uffff\1\63\22\uffff\1\63\1\14\1\30\1\uffff\1\6\1\2\1"+
            "\61\1\31\1\3\1\5\1\12\1\10\1\4\1\11\1\21\1\17\12\60\1\7\1\1"+
            "\1\15\1\27\1\16\1\24\1\20\15\62\1\56\14\62\1\22\1\uffff\1\23"+
            "\1\uffff\1\62\1\uffff\1\32\1\33\1\34\1\35\1\36\1\37\1\40\1\62"+
            "\1\41\2\62\1\42\1\43\1\44\1\45\1\46\1\62\1\47\1\50\1\51\1\52"+
            "\1\53\1\54\1\55\1\62\1\57\1\25\1\13\1\26\102\uffff\27\62\1\uffff"+
            "\37\62\1\uffff\72\62\2\uffff\13\62\2\uffff\10\62\1\uffff\65"+
            "\62\1\uffff\104\62\11\uffff\44\62\3\uffff\2\62\4\uffff\36\62"+
            "\70\uffff\131\62\22\uffff\7\62\u00c4\uffff\1\62\1\uffff\3\62"+
            "\1\uffff\1\62\1\uffff\24\62\1\uffff\54\62\1\uffff\7\62\3\uffff"+
            "\1\62\1\uffff\1\62\1\uffff\1\62\1\uffff\1\62\1\uffff\22\62\15"+
            "\uffff\14\62\1\uffff\102\62\1\uffff\14\62\1\uffff\44\62\16\uffff"+
            "\65\62\2\uffff\2\62\2\uffff\2\62\3\uffff\34\62\2\uffff\10\62"+
            "\2\uffff\2\62\67\uffff\46\62\2\uffff\1\62\7\uffff\46\62\111"+
            "\uffff\33\62\5\uffff\3\62\56\uffff\32\62\6\uffff\12\62\46\uffff"+
            "\107\62\2\uffff\5\62\1\uffff\17\62\1\uffff\4\62\1\uffff\1\62"+
            "\17\uffff\2\62\u021e\uffff\65\62\3\uffff\1\62\32\uffff\12\62"+
            "\43\uffff\10\62\2\uffff\2\62\2\uffff\26\62\1\uffff\7\62\1\uffff"+
            "\1\62\3\uffff\4\62\42\uffff\2\62\1\uffff\3\62\16\uffff\2\62"+
            "\23\uffff\6\62\4\uffff\2\62\2\uffff\26\62\1\uffff\7\62\1\uffff"+
            "\2\62\1\uffff\2\62\1\uffff\2\62\37\uffff\4\62\1\uffff\1\62\23"+
            "\uffff\3\62\20\uffff\7\62\1\uffff\1\62\1\uffff\3\62\1\uffff"+
            "\26\62\1\uffff\7\62\1\uffff\2\62\1\uffff\5\62\3\uffff\1\62\42"+
            "\uffff\1\62\44\uffff\10\62\2\uffff\2\62\2\uffff\26\62\1\uffff"+
            "\7\62\1\uffff\2\62\2\uffff\4\62\3\uffff\1\62\36\uffff\2\62\1"+
            "\uffff\3\62\43\uffff\6\62\3\uffff\3\62\1\uffff\4\62\3\uffff"+
            "\2\62\1\uffff\1\62\1\uffff\2\62\3\uffff\2\62\3\uffff\3\62\3"+
            "\uffff\10\62\1\uffff\3\62\113\uffff\10\62\1\uffff\3\62\1\uffff"+
            "\27\62\1\uffff\12\62\1\uffff\5\62\46\uffff\2\62\43\uffff\10"+
            "\62\1\uffff\3\62\1\uffff\27\62\1\uffff\12\62\1\uffff\5\62\44"+
            "\uffff\1\62\1\uffff\2\62\43\uffff\10\62\1\uffff\3\62\1\uffff"+
            "\27\62\1\uffff\20\62\46\uffff\2\62\u009f\uffff\56\62\1\uffff"+
            "\1\62\1\uffff\2\62\14\uffff\6\62\73\uffff\2\62\1\uffff\1\62"+
            "\2\uffff\2\62\1\uffff\1\62\2\uffff\1\62\6\uffff\4\62\1\uffff"+
            "\7\62\1\uffff\3\62\1\uffff\1\62\1\uffff\1\62\2\uffff\2\62\1"+
            "\uffff\2\62\1\uffff\1\62\1\uffff\2\62\11\uffff\1\62\2\uffff"+
            "\5\62\173\uffff\10\62\1\uffff\41\62\u0136\uffff\46\62\12\uffff"+
            "\47\62\11\uffff\1\62\1\uffff\2\62\1\uffff\3\62\1\uffff\1\62"+
            "\1\uffff\2\62\1\uffff\5\62\51\uffff\1\62\1\uffff\1\62\1\uffff"+
            "\1\62\13\uffff\1\62\1\uffff\1\62\1\uffff\1\62\3\uffff\2\62\3"+
            "\uffff\1\62\5\uffff\3\62\1\uffff\1\62\1\uffff\1\62\1\uffff\1"+
            "\62\1\uffff\1\62\3\uffff\2\62\3\uffff\2\62\1\uffff\1\62\50\uffff"+
            "\1\62\11\uffff\1\62\2\uffff\1\62\2\uffff\2\62\7\uffff\2\62\1"+
            "\uffff\1\62\1\uffff\7\62\50\uffff\1\62\4\uffff\1\62\10\uffff"+
            "\1\62\u0c06\uffff\u009c\62\4\uffff\132\62\6\uffff\26\62\2\uffff"+
            "\6\62\2\uffff\46\62\2\uffff\6\62\2\uffff\10\62\1\uffff\1\62"+
            "\1\uffff\1\62\1\uffff\1\62\1\uffff\37\62\2\uffff\65\62\1\uffff"+
            "\7\62\1\uffff\1\62\3\uffff\3\62\1\uffff\7\62\3\uffff\4\62\2"+
            "\uffff\6\62\4\uffff\15\62\5\uffff\3\62\1\uffff\7\62\u0129\uffff"+
            "\1\62\3\uffff\2\62\2\uffff\1\62\121\uffff\3\62\u0e84\uffff\1"+
            "\62\31\uffff\11\62\27\uffff\124\62\14\uffff\132\62\12\uffff"+
            "\50\62\u1cd3\uffff\u51a6\62\u0c5a\uffff\u2ba4\62",
            "",
            "",
            "\1\64\26\uffff\1\65",
            "",
            "",
            "",
            "\1\70\2\uffff\1\67",
            "",
            "",
            "",
            "",
            "",
            "\1\75\15\uffff\1\74\14\uffff\1\73\1\72\1\uffff\1\76",
            "\1\100\1\101",
            "\1\103\16\uffff\1\104",
            "",
            "\1\106\1\uffff\12\110",
            "",
            "",
            "",
            "",
            "",
            "",
            "\0\112",
            "\0\114",
            "\1\120\5\uffff\1\121\1\uffff\1\115\4\uffff\1\116\1\117",
            "\1\122\3\uffff\1\125\6\uffff\1\126\2\uffff\1\123\11\uffff\1"+
            "\124",
            "\1\127\6\uffff\1\130\6\uffff\1\131",
            "\1\132\3\uffff\1\133\5\uffff\1\134",
            "\1\135\1\136\1\137\2\uffff\1\140\4\uffff\1\141\1\uffff\1\142",
            "\1\145\5\uffff\1\143\2\uffff\1\146\1\uffff\1\147\1\144",
            "\1\150\14\uffff\1\151\1\uffff\1\152",
            "\1\153\1\uffff\1\154\6\uffff\1\155\1\156\4\uffff\1\157\1\160",
            "\1\161\3\uffff\1\162\11\uffff\1\164\4\uffff\1\163",
            "\1\166\5\uffff\1\165",
            "\1\167\3\uffff\1\170\11\uffff\1\171",
            "\1\175\2\uffff\1\172\7\uffff\1\176\1\uffff\1\173\1\uffff\1"+
            "\174",
            "\1\177\3\uffff\1\u0082\2\uffff\1\u0081\11\uffff\1\u0080",
            "\1\u0083",
            "\1\u0084\1\uffff\1\u0085\1\uffff\1\u0086\3\uffff\1\u0087\1"+
            "\uffff\1\u008a\1\u008b\2\uffff\1\u0088\4\uffff\1\u0089",
            "\1\u008c\2\uffff\1\u008d\1\u0091\5\uffff\1\u008e\2\uffff\1"+
            "\u008f\2\uffff\1\u0092\3\uffff\1\u0090",
            "\1\u0093\1\uffff\1\u0094\2\uffff\1\u0095",
            "\1\u0096\3\uffff\1\u0097",
            "\1\u009a\2\uffff\1\u0098\1\u0099\5\uffff\1\u009b",
            "\1\u009c",
            "\1\u009d",
            "\1\u009e",
            "\1\u00a0\1\uffff\12\60\13\uffff\1\u00a1\37\uffff\1\u00a1",
            "\1\u00a2\75\uffff\1\u00a3\5\uffff\1\u00a3\4\uffff\1\u00a3\4"+
            "\uffff\1\u00a3",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\12\110\13\uffff\1\u00a1\37\uffff\1\u00a1",
            "",
            "",
            "",
            "",
            "\1\u00a5\1\u00a6\24\uffff\1\u00a7",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\2\62"+
            "\1\u00a8\27\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\23\62"+
            "\1\u00aa\6\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff"+
            "\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff"+
            "\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff"+
            "\u04d0\62\40\uffff\u020e\62",
            "\1\u00ac",
            "\1\u00ad",
            "\1\u00ae",
            "\1\u00af",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00b1",
            "\1\u00b2",
            "\1\u00b3\1\u00b4",
            "\1\u00b5",
            "\1\u00b6\1\u00b7\1\u00b8\1\uffff\1\u00b9\4\uffff\1\u00ba",
            "\1\u00bb\2\uffff\1\u00bc\5\uffff\1\u00be\6\uffff\1\u00bd\1"+
            "\u00bf",
            "\1\u00c1\4\uffff\1\u00c2\1\u00c4\13\uffff\1\u00c3\2\uffff\1"+
            "\u00c0",
            "\1\u00c5",
            "\1\u00c6\15\uffff\1\u00c7",
            "\1\u00c8",
            "\1\u00c9\1\u00ca\17\uffff\1\u00cb",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00cd",
            "\1\u00d1\1\uffff\1\u00ce\5\uffff\1\u00d0\12\uffff\1\u00cf",
            "\1\u00d2\5\uffff\1\u00d3",
            "\1\u00d4",
            "\1\u00d5",
            "\1\u00d6",
            "\1\u00d9\63\uffff\1\u00d7\14\uffff\1\u00d8\1\u00da",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00dc\11\uffff\1\u00dd",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00df",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00e1",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\5\62"+
            "\1\u00e5\1\62\1\u00e2\12\62\1\u00e3\1\u00e4\6\62\74\uffff\1"+
            "\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62\1\uffff\u1c81"+
            "\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120\62\u0a70\uffff"+
            "\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0\62\40\uffff\u020e"+
            "\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00e8",
            "\1\u00eb\4\uffff\1\u00ea\4\uffff\1\u00e9",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\1\u00ec"+
            "\22\62\1\u00ed\1\62\1\u00ee\4\62\74\uffff\1\62\10\uffff\27\62"+
            "\1\uffff\37\62\1\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2"+
            "\62\61\uffff\2\62\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21"+
            "\uffff\ua7ff\62\u2100\uffff\u04d0\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00f1",
            "\1\u00f2\16\uffff\1\u00f3",
            "\1\u00f4",
            "\1\u00f5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\27\62"+
            "\1\u00f6\2\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff"+
            "\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff"+
            "\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff"+
            "\u04d0\62\40\uffff\u020e\62",
            "\1\u00f9\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\3\62\1\u00f8\11\62\1\u00fb\5\62\1\u00fa\6\62\74\uffff\1\62"+
            "\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62\1\uffff\u1c81"+
            "\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120\62\u0a70\uffff"+
            "\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0\62\40\uffff\u020e"+
            "\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u00fe",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\3\62"+
            "\1\u00ff\26\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u0101",
            "\1\u0102",
            "\1\u0103\1\uffff\1\u0104",
            "\1\u0105\11\uffff\1\u0106",
            "\1\u0107",
            "\1\u0108",
            "\1\u010d\1\uffff\1\u010a\1\uffff\1\u010b\3\uffff\1\u0109\1"+
            "\uffff\1\u010c",
            "\1\u010f\6\uffff\1\u010e",
            "\1\u0110\6\uffff\1\u0111",
            "\1\u0112\1\uffff\1\u0115\2\uffff\1\u0113\2\uffff\1\u0114",
            "\1\u0116",
            "\1\u0117",
            "\1\u0118\3\uffff\1\u011a\11\uffff\1\u011b\2\uffff\1\u0119",
            "\1\u011c",
            "\1\u011d",
            "\1\u011e",
            "\1\u011f",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0121\23\uffff\1\u0122",
            "\1\u0123",
            "\1\u0124",
            "\1\u0125",
            "\1\u0126\5\uffff\1\u0127",
            "\1\u0128\13\uffff\1\u0129",
            "\1\u012a",
            "\1\u012b\5\uffff\1\u012c",
            "\1\u012d",
            "\1\u012e\3\uffff\1\u012f",
            "\1\u0131\1\uffff\1\u0132\5\uffff\1\u0130",
            "\1\u0133",
            "\1\u0134",
            "\1\u0135",
            "\1\u0136",
            "\1\u0137",
            "",
            "\12\u0138\13\uffff\1\u00a1\37\uffff\1\u00a1",
            "",
            "",
            "",
            "",
            "\1\u0139",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u013c",
            "",
            "\1\u013d",
            "",
            "\1\u013e",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\16\62"+
            "\1\u013f\13\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u0141",
            "\1\u0142",
            "",
            "\1\u0143",
            "\1\u0144",
            "\1\u0145\16\uffff\1\u0146",
            "\1\u0147",
            "\1\u0148",
            "\1\u0149",
            "\1\u014a",
            "\1\u014b\1\u014c",
            "\1\u014d",
            "\1\u014e",
            "\1\u0150\2\uffff\1\u014f",
            "\1\u0151",
            "\1\u0152",
            "\1\u0153",
            "\1\u0154",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0156",
            "\1\u0157",
            "\1\u0158",
            "\1\u0159",
            "\1\u015a",
            "\1\u015b",
            "\1\u015c",
            "\1\u015d",
            "\1\u015e",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0160",
            "",
            "\1\u0161",
            "\1\u0162",
            "\1\u0163",
            "\1\u0164",
            "\1\u0165",
            "\1\u0166",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0168",
            "\1\u0169",
            "\1\u016a",
            "\1\u016b",
            "\1\u016c",
            "\1\u016d",
            "\1\u016e",
            "",
            "\1\u016f",
            "\1\u0170",
            "",
            "\1\u0171",
            "",
            "\1\u0172",
            "\1\u0173",
            "\1\u0175\16\uffff\1\u0174",
            "\1\u0176\11\uffff\1\u0177",
            "\1\u0178",
            "",
            "",
            "\1\u0179",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u017b",
            "\1\u017c",
            "\1\u017d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u017f",
            "",
            "",
            "\1\u0180",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\10\62"+
            "\1\u0182\13\62\1\u0181\5\62\74\uffff\1\62\10\uffff\27\62\1\uffff"+
            "\37\62\1\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff"+
            "\2\62\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff"+
            "\62\u2100\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u0184",
            "\1\u0185",
            "\1\u0186",
            "\1\u0187",
            "",
            "\1\u0188",
            "\1\u0189\6\uffff\1\u018a",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u018c",
            "",
            "",
            "\1\u018d",
            "\1\u018e",
            "",
            "\1\u018f",
            "\1\u0190",
            "\1\u0192\3\uffff\1\u0191",
            "\1\u0193",
            "\1\u0194\17\uffff\1\u0195\2\uffff\1\u0196",
            "\1\u0197",
            "\1\u0198",
            "\1\u0199\65\uffff\1\u019a",
            "\1\u019b",
            "\1\u019c",
            "\1\u019d",
            "\1\u019e",
            "\1\u019f",
            "\1\u01a0",
            "\1\u01a1",
            "\1\u01a2",
            "\1\u01a3",
            "\1\u01a4",
            "\1\u01a5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01a7\1\u01a8",
            "\1\u01a9",
            "\1\u01aa",
            "\1\u01ab\17\uffff\1\u01ac",
            "\1\u01ad",
            "\1\u01ae",
            "\1\u01af",
            "\1\u01b0",
            "\1\u01b1",
            "\1\u01b2",
            "\1\u01b3\4\uffff\1\u01b4",
            "",
            "\1\u01b5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01b7",
            "\1\u01b8",
            "\1\u01b9",
            "\1\u01ba",
            "\1\u01bb",
            "\1\u01bc",
            "\1\u01bd",
            "\1\u01be",
            "\1\u01bf\13\uffff\1\u01c0",
            "\1\u01c1",
            "\1\u01c2",
            "\1\u01c4\3\uffff\1\u01c3",
            "\1\u01c5",
            "\1\u01c6",
            "\1\u01c7",
            "\1\u01c8",
            "\1\u01c9",
            "\1\u01ca",
            "\1\u01cb",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01cd",
            "\12\u0138\13\uffff\1\u00a1\37\uffff\1\u00a1",
            "\1\u01ce",
            "",
            "",
            "\1\u01cf",
            "\1\u01d0",
            "\1\u01d1",
            "\1\u01d2",
            "",
            "\1\u01d3",
            "\1\u01d4",
            "\1\u01d5",
            "\1\u01d6",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\1\u01d8"+
            "\31\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01da",
            "\1\u01db",
            "\1\u01dc",
            "\1\u01dd",
            "\1\u01de",
            "\1\u01e0\3\uffff\1\u01df",
            "\1\u01e1\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01e3",
            "\1\u01e4",
            "\1\u01e5",
            "\1\u01e6",
            "\1\u01e7",
            "\1\u01e8",
            "\1\u01e9",
            "",
            "\1\u01ea",
            "\1\u01eb",
            "\1\u01ec",
            "\1\u01ed",
            "\1\u01ee",
            "\1\u01ef",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01f1",
            "\1\u01f2",
            "",
            "\1\u01f3",
            "\1\u01f4",
            "\1\u01f5",
            "\1\u01f6",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01f8",
            "\1\u01f9",
            "",
            "\1\u01fa",
            "\1\u01fb",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u01fd",
            "\1\u01fe",
            "\1\u01ff",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0201",
            "\1\u0202",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0204",
            "\1\u0205",
            "\1\u0206",
            "\1\u0208\3\uffff\1\u0207",
            "\1\u0209",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u020b",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u020e",
            "\1\u020f",
            "",
            "\1\u0210",
            "\1\u0211",
            "\1\u0212",
            "\1\u0213",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0215",
            "\1\u0216",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0218\4\uffff\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1"+
            "\62\1\uffff\22\62\1\u0219\7\62\74\uffff\1\62\10\uffff\27\62"+
            "\1\uffff\37\62\1\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2"+
            "\62\61\uffff\2\62\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21"+
            "\uffff\ua7ff\62\u2100\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u021b",
            "\1\u021c",
            "",
            "\1\u021d",
            "\1\u021e",
            "\1\u021f",
            "\1\u0220",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0222",
            "\1\u0223",
            "\1\u0224",
            "\1\u0225",
            "\1\u0226",
            "\1\u0227",
            "\1\u0228",
            "\1\u0229",
            "\1\u022a",
            "\1\u022b",
            "\1\u022c",
            "\1\u022d",
            "\1\u022e",
            "\1\u022f",
            "\1\u0230",
            "\1\u0231",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0233",
            "\1\u0234",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0236",
            "",
            "\1\u0237",
            "\1\u0238",
            "\1\u0239",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u023b",
            "\1\u023c",
            "\1\u023d\14\uffff\1\u023e",
            "\1\u023f",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0242",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0245",
            "\1\u0246",
            "",
            "\1\u0247",
            "\1\u0248",
            "\1\u0249",
            "\1\u024a",
            "\1\u024b",
            "\1\u024c",
            "\1\u024d",
            "\1\u024e",
            "\1\u024f",
            "\1\u0250",
            "\1\u0251",
            "\1\u0252",
            "\1\u0253",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0255",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\16\62"+
            "\1\u0256\13\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u0258",
            "\1\u0259",
            "\1\u025a",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\22\62"+
            "\1\u025b\7\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff"+
            "\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff"+
            "\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff"+
            "\u04d0\62\40\uffff\u020e\62",
            "\1\u025d",
            "",
            "\1\u025e",
            "\1\u025f",
            "\1\u0260",
            "\1\u0261",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0263",
            "\1\u0264",
            "\1\u0265",
            "\1\u0266",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0268",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u026b",
            "\1\u026c",
            "\1\u026e\20\uffff\1\u026d",
            "\1\u026f\11\uffff\1\u0270",
            "\1\u0271",
            "\1\u0272",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0274",
            "\1\u0275",
            "\1\u0276",
            "\1\u0277",
            "\1\u0278",
            "\1\u0279",
            "\1\u027a",
            "\1\u027b",
            "\1\u027c",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u027e",
            "\1\u027f",
            "",
            "\1\u0280\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0282",
            "\1\u0283",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0285",
            "\1\u0286",
            "",
            "\1\u0287",
            "\1\u0288",
            "\1\u0289",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u028d",
            "",
            "\1\u028e",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\10\62"+
            "\1\u028f\21\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "",
            "\1\u0291",
            "\1\u0292",
            "\1\u0293",
            "\1\u0294",
            "\1\u0295",
            "\1\u0296",
            "",
            "\1\u0297",
            "",
            "",
            "\1\u0298",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u029a",
            "\1\u029b",
            "\1\u029c",
            "\1\u029d",
            "",
            "\1\u029e",
            "\1\u029f",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u02a1",
            "\1\u02a2",
            "\1\u02a3",
            "\1\u02a4",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\4\62"+
            "\1\u02a5\3\62\1\u02a6\21\62\74\uffff\1\62\10\uffff\27\62\1\uffff"+
            "\37\62\1\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff"+
            "\2\62\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff"+
            "\62\u2100\uffff\u04d0\62\40\uffff\u020e\62",
            "\1\u02a8",
            "",
            "\1\u02a9",
            "\1\u02aa",
            "\1\u02ab",
            "\1\u02ac",
            "\1\u02ad",
            "\1\u02ae",
            "\1\u02af",
            "\1\u02b0",
            "\1\u02b1",
            "\1\u02b2",
            "\1\u02b3",
            "\1\u02b4",
            "\1\u02b5",
            "\1\u02b6",
            "\1\u02b7",
            "\1\u02b8",
            "",
            "\1\u02b9",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u02bb",
            "\1\u02bc",
            "\1\u02bd",
            "\1\u02be",
            "",
            "\1\u02bf",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02c1",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02c3",
            "",
            "",
            "\1\u02c4",
            "",
            "",
            "\1\u02c5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02c7",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02c9",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02cb",
            "\1\u02cc",
            "\1\u02cd",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02cf",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02d1",
            "\1\u02d2",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02d5",
            "",
            "\1\u02d6",
            "\1\u02d7",
            "\1\u02d8",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u02da",
            "\1\u02db",
            "\1\u02dc",
            "\1\u02dd",
            "\1\u02de",
            "",
            "\1\u02df",
            "\1\u02e0",
            "\1\u02e1",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u02e3",
            "",
            "",
            "\1\u02e4",
            "\1\u02e5",
            "\1\u02e6",
            "\1\u02e7",
            "\1\u02e8",
            "\1\u02e9",
            "\1\u02ea",
            "\1\u02eb",
            "",
            "\1\u02ec",
            "\1\u02ed",
            "\1\u02ee",
            "\1\u02ef",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02f1",
            "\1\u02f2",
            "\1\u02f3",
            "\1\u02f4",
            "",
            "\1\u02f5",
            "\1\u02f6",
            "\1\u02f7",
            "",
            "\1\u02f8",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u02fb",
            "\1\u02fc",
            "\1\u02fd",
            "\1\u02fe",
            "",
            "",
            "",
            "\1\u02ff",
            "\1\u0300",
            "\1\u0301",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0303",
            "\1\u0304",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0306",
            "\1\u0307",
            "\1\u0308",
            "\1\u0309",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u030b",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u030e",
            "\1\u030f",
            "",
            "\1\u0310",
            "\1\u0311",
            "\1\u0312",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0314",
            "\1\u0315",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0318",
            "\1\u0319",
            "\1\u031a",
            "\1\u031b",
            "\1\u031c",
            "\1\u031d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u031f",
            "\1\u0320",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\10\62"+
            "\1\u0321\21\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1"+
            "\uffff\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62"+
            "\57\uffff\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100"+
            "\uffff\u04d0\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0324",
            "\1\u0325",
            "\1\u0326",
            "\1\u0327",
            "\1\u0328\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u032a",
            "\1\u032b",
            "\1\u032c",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0330",
            "\1\u0331",
            "\1\u0332",
            "",
            "\1\u0333",
            "",
            "\1\u0334",
            "",
            "\1\u0335",
            "\1\u0336",
            "\1\u0337",
            "",
            "\1\u0338",
            "",
            "\1\u0339",
            "\1\u033a",
            "",
            "",
            "\1\u033b",
            "\1\u033c",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0340",
            "\1\u0341",
            "\1\u0342",
            "\1\u0343",
            "\1\u0344",
            "\1\u0345",
            "\1\u0346",
            "",
            "\1\u0347",
            "\1\u0348",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u034a",
            "\1\u034b",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u034e",
            "\1\u034f",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0351",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0353\7\uffff\1\u0354",
            "",
            "\1\u0355",
            "\1\u0356",
            "\1\u0357",
            "\1\u0358",
            "\1\u0359",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u035b",
            "\1\u035c",
            "",
            "",
            "\1\u035d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u035f",
            "\1\u0360",
            "\1\u0361",
            "\1\u0362",
            "\1\u0363",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0365",
            "",
            "\1\u0366",
            "\1\u0367",
            "\1\u0368",
            "\1\u0369",
            "",
            "\1\u036a",
            "",
            "",
            "\1\u036b",
            "\1\u036c",
            "\1\u036d",
            "\1\u036e",
            "\1\u036f",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0371",
            "",
            "",
            "\1\u0372",
            "\1\u0373",
            "\1\u0374",
            "\1\u0375",
            "\1\u0376",
            "\1\u0377",
            "",
            "\1\u0378",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u037a",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u037c",
            "\1\u037d",
            "\1\u037e",
            "\1\u037f\3\uffff\1\u0380",
            "",
            "\1\u0381",
            "\1\u0382",
            "\1\u0383",
            "",
            "",
            "",
            "\1\u0384",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0386",
            "\1\u0387",
            "\1\u0388",
            "\1\u0389",
            "\1\u038a",
            "\1\u038b",
            "\1\u038c",
            "\1\u038d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0390",
            "",
            "",
            "",
            "\1\u0391",
            "\1\u0392\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0394",
            "\1\u0395",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0398",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u039a",
            "",
            "\1\u039b",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u039e",
            "",
            "\1\u039f\14\uffff\1\u03a0",
            "",
            "\1\u03a1",
            "\1\u03a2",
            "\1\u03a3",
            "\1\u03a4",
            "\1\u03a5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03a7\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u03a9",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u03ac",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03ae",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03b0",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03b2",
            "\1\u03b3",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03b6",
            "\1\u03b7",
            "\1\u03b8",
            "\1\u03b9",
            "\1\u03ba",
            "\1\u03bb",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03bd",
            "\1\u03be",
            "\1\u03bf",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03c2",
            "\1\u03c3",
            "",
            "\1\u03c4",
            "",
            "\1\u03c5",
            "\1\u03c6",
            "\1\u03c7",
            "\1\u03c8",
            "\1\u03c9",
            "\1\u03ca",
            "\1\u03cb",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\22\62"+
            "\1\u03cc\7\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff"+
            "\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff"+
            "\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff"+
            "\u04d0\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u03cf",
            "\1\u03d0",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03d2",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03d4",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\1\u03d7",
            "\1\u03d8",
            "\1\u03d9",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\1\u03dc",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03de",
            "",
            "",
            "\1\u03df",
            "\1\u03e0",
            "\1\u03e1",
            "\1\u03e2",
            "\1\u03e3",
            "\1\u03e4",
            "\1\u03e5",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u03e7",
            "",
            "\1\u03e8",
            "",
            "",
            "\1\u03e9\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u03ec",
            "",
            "\1\u03ed",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03f0",
            "\1\u03f1\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03f3",
            "\1\u03f4",
            "\1\u03f5",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\22\62"+
            "\1\u03f6\7\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff"+
            "\u0286\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff"+
            "\u0120\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff"+
            "\u04d0\62\40\uffff\u020e\62",
            "\1\u03f8",
            "\1\u03f9\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\1\u03fb",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u03fe",
            "\1\u03ff",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0401",
            "\1\u0402",
            "\1\u0403",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0407",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u040b",
            "\1\u040c",
            "",
            "",
            "\1\u040d",
            "",
            "\1\u040e",
            "\1\u040f",
            "\1\u0410",
            "\1\u0411",
            "\1\u0412\1\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff"+
            "\32\62\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286"+
            "\62\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0415",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0417",
            "\1\u0418",
            "\1\u0419",
            "",
            "",
            "\1\u041a",
            "\1\u041b",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u041d",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u041f",
            "\1\u0420",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0422",
            "\1\u0423",
            "",
            "\1\u0424",
            "",
            "",
            "\1\u0425",
            "\1\u0426",
            "",
            "\1\u0427",
            "\1\u0428",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u042c",
            "\1\u042d",
            "\1\u042e",
            "\1\u042f",
            "\1\u0430",
            "\1\u0431",
            "\1\u0432",
            "",
            "",
            "\1\u0433",
            "",
            "\1\u0434",
            "\1\u0435",
            "\1\u0436",
            "\1\u0437",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0439",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u043b",
            "",
            "\1\u043c",
            "\1\u043d",
            "\1\u043e",
            "\1\u043f",
            "\1\u0440",
            "\1\u0441",
            "\1\u0442",
            "",
            "",
            "",
            "\1\u0443",
            "\1\u0444",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0446",
            "\1\u0447",
            "\1\u0448",
            "\1\u0449",
            "\1\u044a",
            "\1\u044b",
            "\1\u044c",
            "\1\u044d",
            "\1\u044e",
            "",
            "\1\u044f",
            "",
            "\1\u0450",
            "\1\u0451",
            "\1\u0452",
            "\1\u0453",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0456",
            "\1\u0457",
            "\1\u0458",
            "\1\u0459",
            "",
            "\1\u045a",
            "\1\u045b",
            "\1\u045c",
            "\1\u045d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0460",
            "\1\u0461",
            "\1\u0462",
            "\1\u0463",
            "\1\u0464",
            "\1\u0465",
            "\1\u0466",
            "\1\u0467",
            "",
            "",
            "\1\u0468",
            "\1\u0469",
            "\1\u046a",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u046c",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u046e",
            "\1\u046f",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0471",
            "\1\u0472",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0474",
            "\1\u0475",
            "\1\u0476",
            "\1\u0477",
            "\1\u0478",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u047a",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u047c",
            "\1\u047d",
            "",
            "\1\u047e",
            "\1\u047f",
            "",
            "\1\u0480",
            "\1\u0481",
            "\1\u0482",
            "\1\u0483",
            "\1\u0484",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\1\u0486",
            "\1\u0487",
            "\1\u0488",
            "\1\u0489",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u048b",
            "\1\u048c",
            "\1\u048d",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0490",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0492",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "\1\u0495",
            "",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            "",
            "",
            "\1\u0498",
            "",
            "",
            "\1\u0499",
            "\1\u049a",
            "\1\u049b",
            "\2\62\1\uffff\12\62\7\uffff\32\62\4\uffff\1\62\1\uffff\32\62"+
            "\74\uffff\1\62\10\uffff\27\62\1\uffff\37\62\1\uffff\u0286\62"+
            "\1\uffff\u1c81\62\14\uffff\2\62\61\uffff\2\62\57\uffff\u0120"+
            "\62\u0a70\uffff\u03f0\62\21\uffff\ua7ff\62\u2100\uffff\u04d0"+
            "\62\40\uffff\u020e\62",
            ""
    };

    static final short[] DFA25_eot = DFA.unpackEncodedString(DFA25_eotS);
    static final short[] DFA25_eof = DFA.unpackEncodedString(DFA25_eofS);
    static final char[] DFA25_min = DFA.unpackEncodedStringToUnsignedChars(DFA25_minS);
    static final char[] DFA25_max = DFA.unpackEncodedStringToUnsignedChars(DFA25_maxS);
    static final short[] DFA25_accept = DFA.unpackEncodedString(DFA25_acceptS);
    static final short[] DFA25_special = DFA.unpackEncodedString(DFA25_specialS);
    static final short[][] DFA25_transition;

    static {
        int numStates = DFA25_transitionS.length;
        DFA25_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA25_transition[i] = DFA.unpackEncodedString(DFA25_transitionS[i]);
        }
    }

    class DFA25 extends DFA {

        public DFA25(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 25;
            this.eot = DFA25_eot;
            this.eof = DFA25_eof;
            this.min = DFA25_min;
            this.max = DFA25_max;
            this.accept = DFA25_accept;
            this.special = DFA25_special;
            this.transition = DFA25_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__406 | T__407 | T__408 | T__409 | T__410 | T__411 | T__412 | T__413 | T__414 | T__415 | T__416 | T__417 | T__418 | T__419 | T__420 | T__421 | T__422 | T__423 | T__424 | T__425 | T__426 | T__427 | T__428 | T__429 | T__430 | LAngle | RAngle | LCurly | RCurly | SymEq | Colon | LClose | RClose | Quot | Apos | ANCESTOR | ANCESTOR_OR_SELF | AND | AS | ASCENDING | AT | ATTRIBUTE | BASE_URI | BOUNDARY_SPACE | BY | CASE | CASTABLE | CAST | CHILD | COLLATION | COMMENT | CONSTRUCTION | COPY | COPY_NAMESPACES | DECLARE | DEFAULT | DESCENDANT | DESCENDANT_OR_SELF | DESCENDING | DIV | DOCUMENT | DOCUMENT_NODE | ELEMENT | ELSE | EMPTY | EMPTY_SEQUENCE | ENCODING | EQ | EVERY | EXCEPT | EXTERNAL | FOLLOWING | FOLLOWING_SIBLING | FOR | FUNCTION | GE | GREATEST | GT | IDIV | IF | IMPORT | INHERIT | IN | INSTANCE | INTERSECT | IS | ITEM | LAX | LEAST | LE | LET | LT | MOD | MODULE | NAMESPACE | NE | NODE | ANYKIND | NO_INHERIT | NO_PRESERVE | OF | OPTION | ORDERED | ORDERING | ORDER | OR | PARENT | PRECEDING | PRECEDING_SIBLING | PRESERVE | PROCESSING_INSTRUCTION | RETURN | SATISFIES | SCHEMA_ATTRIBUTE | SCHEMA_ELEMENT | SCHEMA | SELF | SIMPLE | SOME | STABLE | STRICT | STRIP | TEXT | THEN | TO | TREAT | TYPESWITCH | UNION | UNORDERED | VALIDATE | VARIABLE | VERSION | WHERE | XQUERY | AFTER | BEFORE | DELETE | FIRST | INSERT | INTO | LAST | MODIFY | NODES | RENAME | REPLACE | REVALIDATION | SKIP | UPDATING | VALUE | WITH | BLOCK | CONSTANT | EXIT | SEQUENTIAL | RETURNING | SET | WHILE | ALL | ANY | CONTENT | DIACRITICS | DIFFERENT | DISTANCE | END | ENTIRE | EXACTLY | FROM | FTAND | CONTAINS | FTNOT | FT_OPTION | FTOR | INSENSITIVE | LANGUAGE | LEVELS | LOWERCASE | MOST | NO | NOT | OCCURS | PARAGRAPH | PARAGRAPHS | PHRASE | RELATIONSHIP | SAME | SCORE | SENSITIVE | SENTENCE | SENTENCES | START | STEMMING | STOP | THESAURUS | TIMES | UPPERCASE | USING | WEIGHT | WILDCARDS | WINDOW | WITHOUT | WORD | WORDS | CATCH | CONTEXT | DETERMINISTIC | NAMESPACE_NODE | NONDETERMINISTIC | TRY | DECIMAL_FORMAT | DECIMAL_SEPARATOR | DIGIT | GROUPING_SEPARATOR | INFINITY | MINUS_SIGN | NAN | PER_MILLE | PERCENT | PATTERN_SEPARATOR | ZERO_DIGIT | COUNT | GROUP | NEXT | ONLY | PREVIOUS | SLIDING | TUMBLING | WHEN | ALLOWING | DirCommentConstructor | DirPIConstructor | Pragma | IntegerLiteral | DecimalLiteral | DoubleLiteral | QuotedStringLiteral | AposedStringLiteral | PredefinedEntityRef | CharRef | Comment | NCName | S );";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            IntStream input = _input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA25_24 = input.LA(1);

                        s = -1;
                        if ( ((LA25_24>='\u0000' && LA25_24<='\uFFFF')) ) {s = 74;}

                        else s = 73;

                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA25_25 = input.LA(1);

                        s = -1;
                        if ( ((LA25_25>='\u0000' && LA25_25<='\uFFFF')) ) {s = 76;}

                        else s = 75;

                        if ( s>=0 ) return s;
                        break;
            }
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 25, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

}