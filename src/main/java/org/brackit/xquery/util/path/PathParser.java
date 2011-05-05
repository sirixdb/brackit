// $ANTLR 3.2 Sep 23, 2009 12:02:23 org/brackit/xquery/util/path/Path.g 2011-05-05 13:18:35

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
package org.brackit.xquery.util.path;

import java.io.StringReader;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class PathParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "SELFSTART", "STARTPARENT", "TAG", "CHILD_ATT", "WILDCARD", "DESC_ATT", "PARENT", "SELF", "CHILD", "DESC", "NUMBER", "LETTER", "CHAR", "PROTOCOL"
    };
    public static final int CHILD=12;
    public static final int PARENT=10;
    public static final int LETTER=15;
    public static final int DESC_ATT=9;
    public static final int WILDCARD=8;
    public static final int NUMBER=14;
    public static final int CHAR=16;
    public static final int SELFSTART=4;
    public static final int DESC=13;
    public static final int EOF=-1;
    public static final int SELF=11;
    public static final int PROTOCOL=17;
    public static final int CHILD_ATT=7;
    public static final int STARTPARENT=5;
    public static final int TAG=6;

    // delegates
    // delegators


        public PathParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public PathParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return PathParser.tokenNames; }
    public String getGrammarFileName() { return "org/brackit/xquery/util/path/Path.g"; }


    public PathParser(String arg) throws Exception {
            this(new CommonTokenStream(new PathLexer(new ANTLRReaderStream(new StringReader(arg)))));
    }

    @Override
    protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow) throws RecognitionException {
    	 throw new MismatchedTokenException(ttype, input);
    }

    @Override
    public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
    	throw e;
    }



    // $ANTLR start "parse"
    // org/brackit/xquery/util/path/Path.g:124:1: parse returns [ Path<String> p ] : ( startstep[p] )? ( axisstep[p] )* ( attributestep[p] )? EOF ;
    public final Path<String> parse() throws RecognitionException {
        Path<String> p = null;

        try {
            // org/brackit/xquery/util/path/Path.g:124:33: ( ( startstep[p] )? ( axisstep[p] )* ( attributestep[p] )? EOF )
            // org/brackit/xquery/util/path/Path.g:125:2: ( startstep[p] )? ( axisstep[p] )* ( attributestep[p] )? EOF
            {
             p = new Path<String>(); 
            // org/brackit/xquery/util/path/Path.g:126:2: ( startstep[p] )?
            int alt1=2;
            alt1 = dfa1.predict(input);
            switch (alt1) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:126:2: startstep[p]
                    {
                    pushFollow(FOLLOW_startstep_in_parse61);
                    startstep(p);

                    state._fsp--;


                    }
                    break;

            }

            // org/brackit/xquery/util/path/Path.g:126:16: ( axisstep[p] )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( ((LA2_0>=PARENT && LA2_0<=DESC)) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // org/brackit/xquery/util/path/Path.g:126:16: axisstep[p]
            	    {
            	    pushFollow(FOLLOW_axisstep_in_parse65);
            	    axisstep(p);

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);

            // org/brackit/xquery/util/path/Path.g:126:29: ( attributestep[p] )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==CHILD_ATT||LA3_0==DESC_ATT) ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:126:29: attributestep[p]
                    {
                    pushFollow(FOLLOW_attributestep_in_parse69);
                    attributestep(p);

                    state._fsp--;


                    }
                    break;

            }

            match(input,EOF,FOLLOW_EOF_in_parse73); 
             /* System.out.println("path: " + p); */ 

            }

        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return p;
    }
    // $ANTLR end "parse"


    // $ANTLR start "startstep"
    // org/brackit/xquery/util/path/Path.g:129:1: startstep[ Path<String> p] : ( SELFSTART | STARTPARENT | s= TAG );
    public final void startstep(Path<String> p) throws RecognitionException {
        Token s=null;

        try {
            // org/brackit/xquery/util/path/Path.g:129:28: ( SELFSTART | STARTPARENT | s= TAG )
            int alt4=3;
            switch ( input.LA(1) ) {
            case SELFSTART:
                {
                alt4=1;
                }
                break;
            case STARTPARENT:
                {
                alt4=2;
                }
                break;
            case TAG:
                {
                alt4=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }

            switch (alt4) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:130:2: SELFSTART
                    {
                    match(input,SELFSTART,FOLLOW_SELFSTART_in_startstep87); 
                    p.self();

                    }
                    break;
                case 2 :
                    // org/brackit/xquery/util/path/Path.g:131:4: STARTPARENT
                    {
                    match(input,STARTPARENT,FOLLOW_STARTPARENT_in_startstep94); 
                    p.parent();

                    }
                    break;
                case 3 :
                    // org/brackit/xquery/util/path/Path.g:132:4: s= TAG
                    {
                    s=(Token)match(input,TAG,FOLLOW_TAG_in_startstep103); 
                    p.self().child((s!=null?s.getText():null));

                    }
                    break;

            }
        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "startstep"


    // $ANTLR start "axisstep"
    // org/brackit/xquery/util/path/Path.g:135:1: axisstep[ Path<String> p] : ( parentstep[p] | selfstep[p] | namedstep[p] );
    public final void axisstep(Path<String> p) throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:135:27: ( parentstep[p] | selfstep[p] | namedstep[p] )
            int alt5=3;
            switch ( input.LA(1) ) {
            case PARENT:
                {
                alt5=1;
                }
                break;
            case SELF:
                {
                alt5=2;
                }
                break;
            case CHILD:
            case DESC:
                {
                alt5=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:136:2: parentstep[p]
                    {
                    pushFollow(FOLLOW_parentstep_in_axisstep116);
                    parentstep(p);

                    state._fsp--;


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/util/path/Path.g:136:18: selfstep[p]
                    {
                    pushFollow(FOLLOW_selfstep_in_axisstep121);
                    selfstep(p);

                    state._fsp--;


                    }
                    break;
                case 3 :
                    // org/brackit/xquery/util/path/Path.g:136:32: namedstep[p]
                    {
                    pushFollow(FOLLOW_namedstep_in_axisstep126);
                    namedstep(p);

                    state._fsp--;


                    }
                    break;

            }
        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "axisstep"


    // $ANTLR start "attributestep"
    // org/brackit/xquery/util/path/Path.g:139:1: attributestep[ Path<String> p] : ( ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) ) ) ;
    public final void attributestep(Path<String> p) throws RecognitionException {
        Token s=null;

        try {
            // org/brackit/xquery/util/path/Path.g:139:32: ( ( ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) ) ) )
            // org/brackit/xquery/util/path/Path.g:140:2: ( ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) ) )
            {

            	String tag = null;
            	
            // org/brackit/xquery/util/path/Path.g:143:2: ( ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) ) )
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==CHILD_ATT) ) {
                alt8=1;
            }
            else if ( (LA8_0==DESC_ATT) ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:143:3: ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) )
                    {
                    // org/brackit/xquery/util/path/Path.g:143:3: ( CHILD_ATT ( ( WILDCARD ) | (s= TAG ) ) )
                    // org/brackit/xquery/util/path/Path.g:143:4: CHILD_ATT ( ( WILDCARD ) | (s= TAG ) )
                    {
                    match(input,CHILD_ATT,FOLLOW_CHILD_ATT_in_attributestep143); 
                    // org/brackit/xquery/util/path/Path.g:143:14: ( ( WILDCARD ) | (s= TAG ) )
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==WILDCARD) ) {
                        alt6=1;
                    }
                    else if ( (LA6_0==TAG) ) {
                        alt6=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 6, 0, input);

                        throw nvae;
                    }
                    switch (alt6) {
                        case 1 :
                            // org/brackit/xquery/util/path/Path.g:143:15: ( WILDCARD )
                            {
                            // org/brackit/xquery/util/path/Path.g:143:15: ( WILDCARD )
                            // org/brackit/xquery/util/path/Path.g:143:16: WILDCARD
                            {
                            match(input,WILDCARD,FOLLOW_WILDCARD_in_attributestep147); 

                            }


                            }
                            break;
                        case 2 :
                            // org/brackit/xquery/util/path/Path.g:143:28: (s= TAG )
                            {
                            // org/brackit/xquery/util/path/Path.g:143:28: (s= TAG )
                            // org/brackit/xquery/util/path/Path.g:143:29: s= TAG
                            {
                            s=(Token)match(input,TAG,FOLLOW_TAG_in_attributestep155); 
                            tag = (s!=null?s.getText():null);

                            }


                            }
                            break;

                    }

                     p.attribute(tag);

                    }


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/util/path/Path.g:144:4: ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) )
                    {
                    // org/brackit/xquery/util/path/Path.g:144:4: ( DESC_ATT ( ( WILDCARD ) | (s= TAG ) ) )
                    // org/brackit/xquery/util/path/Path.g:144:5: DESC_ATT ( ( WILDCARD ) | (s= TAG ) )
                    {
                    match(input,DESC_ATT,FOLLOW_DESC_ATT_in_attributestep168); 
                    // org/brackit/xquery/util/path/Path.g:144:14: ( ( WILDCARD ) | (s= TAG ) )
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0==WILDCARD) ) {
                        alt7=1;
                    }
                    else if ( (LA7_0==TAG) ) {
                        alt7=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 7, 0, input);

                        throw nvae;
                    }
                    switch (alt7) {
                        case 1 :
                            // org/brackit/xquery/util/path/Path.g:144:15: ( WILDCARD )
                            {
                            // org/brackit/xquery/util/path/Path.g:144:15: ( WILDCARD )
                            // org/brackit/xquery/util/path/Path.g:144:16: WILDCARD
                            {
                            match(input,WILDCARD,FOLLOW_WILDCARD_in_attributestep172); 

                            }


                            }
                            break;
                        case 2 :
                            // org/brackit/xquery/util/path/Path.g:144:28: (s= TAG )
                            {
                            // org/brackit/xquery/util/path/Path.g:144:28: (s= TAG )
                            // org/brackit/xquery/util/path/Path.g:144:29: s= TAG
                            {
                            s=(Token)match(input,TAG,FOLLOW_TAG_in_attributestep180); 
                            tag = (s!=null?s.getText():null);

                            }


                            }
                            break;

                    }

                     p.descendantAttribute(tag);

                    }


                    }
                    break;

            }


            }

        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "attributestep"


    // $ANTLR start "parentstep"
    // org/brackit/xquery/util/path/Path.g:147:1: parentstep[ Path<String> p] : PARENT ;
    public final void parentstep(Path<String> p) throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:147:29: ( PARENT )
            // org/brackit/xquery/util/path/Path.g:148:2: PARENT
            {
            match(input,PARENT,FOLLOW_PARENT_in_parentstep199); 

            	p.parent();
            	

            }

        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "parentstep"


    // $ANTLR start "selfstep"
    // org/brackit/xquery/util/path/Path.g:154:1: selfstep[ Path<String> p] : SELF ;
    public final void selfstep(Path<String> p) throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:154:27: ( SELF )
            // org/brackit/xquery/util/path/Path.g:155:2: SELF
            {
            match(input,SELF,FOLLOW_SELF_in_selfstep213); 

            	p.self();
            	

            }

        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "selfstep"


    // $ANTLR start "namedstep"
    // org/brackit/xquery/util/path/Path.g:161:1: namedstep[ Path<String> p] : ( ( CHILD ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC ( ( WILDCARD ) | (s= TAG ) ) ) ) ;
    public final void namedstep(Path<String> p) throws RecognitionException {
        Token s=null;

        try {
            // org/brackit/xquery/util/path/Path.g:161:28: ( ( ( CHILD ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC ( ( WILDCARD ) | (s= TAG ) ) ) ) )
            // org/brackit/xquery/util/path/Path.g:162:2: ( ( CHILD ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC ( ( WILDCARD ) | (s= TAG ) ) ) )
            {

            	String tag = null;
            	
            // org/brackit/xquery/util/path/Path.g:165:2: ( ( CHILD ( ( WILDCARD ) | (s= TAG ) ) ) | ( DESC ( ( WILDCARD ) | (s= TAG ) ) ) )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==CHILD) ) {
                alt11=1;
            }
            else if ( (LA11_0==DESC) ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:165:3: ( CHILD ( ( WILDCARD ) | (s= TAG ) ) )
                    {
                    // org/brackit/xquery/util/path/Path.g:165:3: ( CHILD ( ( WILDCARD ) | (s= TAG ) ) )
                    // org/brackit/xquery/util/path/Path.g:165:4: CHILD ( ( WILDCARD ) | (s= TAG ) )
                    {
                    match(input,CHILD,FOLLOW_CHILD_in_namedstep232); 
                    // org/brackit/xquery/util/path/Path.g:165:10: ( ( WILDCARD ) | (s= TAG ) )
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0==WILDCARD) ) {
                        alt9=1;
                    }
                    else if ( (LA9_0==TAG) ) {
                        alt9=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 9, 0, input);

                        throw nvae;
                    }
                    switch (alt9) {
                        case 1 :
                            // org/brackit/xquery/util/path/Path.g:165:11: ( WILDCARD )
                            {
                            // org/brackit/xquery/util/path/Path.g:165:11: ( WILDCARD )
                            // org/brackit/xquery/util/path/Path.g:165:12: WILDCARD
                            {
                            match(input,WILDCARD,FOLLOW_WILDCARD_in_namedstep236); 

                            }


                            }
                            break;
                        case 2 :
                            // org/brackit/xquery/util/path/Path.g:165:24: (s= TAG )
                            {
                            // org/brackit/xquery/util/path/Path.g:165:24: (s= TAG )
                            // org/brackit/xquery/util/path/Path.g:165:25: s= TAG
                            {
                            s=(Token)match(input,TAG,FOLLOW_TAG_in_namedstep244); 
                            tag = (s!=null?s.getText():null);

                            }


                            }
                            break;

                    }

                     p.child(tag);

                    }


                    }
                    break;
                case 2 :
                    // org/brackit/xquery/util/path/Path.g:166:4: ( DESC ( ( WILDCARD ) | (s= TAG ) ) )
                    {
                    // org/brackit/xquery/util/path/Path.g:166:4: ( DESC ( ( WILDCARD ) | (s= TAG ) ) )
                    // org/brackit/xquery/util/path/Path.g:166:5: DESC ( ( WILDCARD ) | (s= TAG ) )
                    {
                    match(input,DESC,FOLLOW_DESC_in_namedstep257); 
                    // org/brackit/xquery/util/path/Path.g:166:10: ( ( WILDCARD ) | (s= TAG ) )
                    int alt10=2;
                    int LA10_0 = input.LA(1);

                    if ( (LA10_0==WILDCARD) ) {
                        alt10=1;
                    }
                    else if ( (LA10_0==TAG) ) {
                        alt10=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 10, 0, input);

                        throw nvae;
                    }
                    switch (alt10) {
                        case 1 :
                            // org/brackit/xquery/util/path/Path.g:166:11: ( WILDCARD )
                            {
                            // org/brackit/xquery/util/path/Path.g:166:11: ( WILDCARD )
                            // org/brackit/xquery/util/path/Path.g:166:12: WILDCARD
                            {
                            match(input,WILDCARD,FOLLOW_WILDCARD_in_namedstep261); 

                            }


                            }
                            break;
                        case 2 :
                            // org/brackit/xquery/util/path/Path.g:166:24: (s= TAG )
                            {
                            // org/brackit/xquery/util/path/Path.g:166:24: (s= TAG )
                            // org/brackit/xquery/util/path/Path.g:166:25: s= TAG
                            {
                            s=(Token)match(input,TAG,FOLLOW_TAG_in_namedstep269); 
                            tag = (s!=null?s.getText():null);

                            }


                            }
                            break;

                    }

                     p.descendant(tag);

                    }


                    }
                    break;

            }


            }

        }

        catch (RecognitionException e) {
        	throw e;
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "namedstep"

    // Delegated rules


    protected DFA1 dfa1 = new DFA1(this);
    static final String DFA1_eotS =
        "\13\uffff";
    static final String DFA1_eofS =
        "\1\4\12\uffff";
    static final String DFA1_minS =
        "\1\4\12\uffff";
    static final String DFA1_maxS =
        "\1\15\12\uffff";
    static final String DFA1_acceptS =
        "\1\uffff\1\1\2\uffff\1\2\6\uffff";
    static final String DFA1_specialS =
        "\13\uffff}>";
    static final String[] DFA1_transitionS = {
            "\3\1\1\4\1\uffff\5\4",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA1_eot = DFA.unpackEncodedString(DFA1_eotS);
    static final short[] DFA1_eof = DFA.unpackEncodedString(DFA1_eofS);
    static final char[] DFA1_min = DFA.unpackEncodedStringToUnsignedChars(DFA1_minS);
    static final char[] DFA1_max = DFA.unpackEncodedStringToUnsignedChars(DFA1_maxS);
    static final short[] DFA1_accept = DFA.unpackEncodedString(DFA1_acceptS);
    static final short[] DFA1_special = DFA.unpackEncodedString(DFA1_specialS);
    static final short[][] DFA1_transition;

    static {
        int numStates = DFA1_transitionS.length;
        DFA1_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA1_transition[i] = DFA.unpackEncodedString(DFA1_transitionS[i]);
        }
    }

    class DFA1 extends DFA {

        public DFA1(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 1;
            this.eot = DFA1_eot;
            this.eof = DFA1_eof;
            this.min = DFA1_min;
            this.max = DFA1_max;
            this.accept = DFA1_accept;
            this.special = DFA1_special;
            this.transition = DFA1_transition;
        }
        public String getDescription() {
            return "126:2: ( startstep[p] )?";
        }
    }
 

    public static final BitSet FOLLOW_startstep_in_parse61 = new BitSet(new long[]{0x0000000000003E80L});
    public static final BitSet FOLLOW_axisstep_in_parse65 = new BitSet(new long[]{0x0000000000003E80L});
    public static final BitSet FOLLOW_attributestep_in_parse69 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_parse73 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELFSTART_in_startstep87 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STARTPARENT_in_startstep94 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_startstep103 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_parentstep_in_axisstep116 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_selfstep_in_axisstep121 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_namedstep_in_axisstep126 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CHILD_ATT_in_attributestep143 = new BitSet(new long[]{0x0000000000000140L});
    public static final BitSet FOLLOW_WILDCARD_in_attributestep147 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_attributestep155 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DESC_ATT_in_attributestep168 = new BitSet(new long[]{0x0000000000000140L});
    public static final BitSet FOLLOW_WILDCARD_in_attributestep172 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_attributestep180 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PARENT_in_parentstep199 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SELF_in_selfstep213 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CHILD_in_namedstep232 = new BitSet(new long[]{0x0000000000000140L});
    public static final BitSet FOLLOW_WILDCARD_in_namedstep236 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_namedstep244 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DESC_in_namedstep257 = new BitSet(new long[]{0x0000000000000140L});
    public static final BitSet FOLLOW_WILDCARD_in_namedstep261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_namedstep269 = new BitSet(new long[]{0x0000000000000002L});

}