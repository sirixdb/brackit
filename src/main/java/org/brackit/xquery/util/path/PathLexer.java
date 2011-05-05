// $ANTLR 3.2 Sep 23, 2009 12:02:23 org/brackit/xquery/util/path/Path.g 2011-05-05 13:18:36

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


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class PathLexer extends Lexer {
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
    public static final int CHILD_ATT=7;
    public static final int PROTOCOL=17;
    public static final int STARTPARENT=5;
    public static final int TAG=6;

    // delegates
    // delegators

    public PathLexer() {;} 
    public PathLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public PathLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "org/brackit/xquery/util/path/Path.g"; }

    // $ANTLR start "STARTPARENT"
    public final void mSTARTPARENT() throws RecognitionException {
        try {
            int _type = STARTPARENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:172:12: ( '..' )
            // org/brackit/xquery/util/path/Path.g:172:14: '..'
            {
            match(".."); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STARTPARENT"

    // $ANTLR start "SELFSTART"
    public final void mSELFSTART() throws RecognitionException {
        try {
            int _type = SELFSTART;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:173:10: ( '.' )
            // org/brackit/xquery/util/path/Path.g:173:12: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SELFSTART"

    // $ANTLR start "SELF"
    public final void mSELF() throws RecognitionException {
        try {
            int _type = SELF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:174:6: ( '/.' )
            // org/brackit/xquery/util/path/Path.g:174:8: '/.'
            {
            match("/."); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SELF"

    // $ANTLR start "PARENT"
    public final void mPARENT() throws RecognitionException {
        try {
            int _type = PARENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:175:8: ( '/..' )
            // org/brackit/xquery/util/path/Path.g:175:10: '/..'
            {
            match("/.."); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARENT"

    // $ANTLR start "CHILD"
    public final void mCHILD() throws RecognitionException {
        try {
            int _type = CHILD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:176:7: ( '/' )
            // org/brackit/xquery/util/path/Path.g:176:9: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CHILD"

    // $ANTLR start "DESC"
    public final void mDESC() throws RecognitionException {
        try {
            int _type = DESC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:177:6: ( '//' )
            // org/brackit/xquery/util/path/Path.g:177:8: '//'
            {
            match("//"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DESC"

    // $ANTLR start "DESC_ATT"
    public final void mDESC_ATT() throws RecognitionException {
        try {
            int _type = DESC_ATT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:178:9: ( '//@' )
            // org/brackit/xquery/util/path/Path.g:178:11: '//@'
            {
            match("//@"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DESC_ATT"

    // $ANTLR start "CHILD_ATT"
    public final void mCHILD_ATT() throws RecognitionException {
        try {
            int _type = CHILD_ATT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:179:10: ( '/@' )
            // org/brackit/xquery/util/path/Path.g:179:12: '/@'
            {
            match("/@"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CHILD_ATT"

    // $ANTLR start "WILDCARD"
    public final void mWILDCARD() throws RecognitionException {
        try {
            int _type = WILDCARD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:180:9: ( '*' )
            // org/brackit/xquery/util/path/Path.g:180:11: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WILDCARD"

    // $ANTLR start "NUMBER"
    public final void mNUMBER() throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:181:17: ( ( '0' .. '9' ) )
            // org/brackit/xquery/util/path/Path.g:181:19: ( '0' .. '9' )
            {
            // org/brackit/xquery/util/path/Path.g:181:19: ( '0' .. '9' )
            // org/brackit/xquery/util/path/Path.g:181:20: '0' .. '9'
            {
            matchRange('0','9'); 

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "NUMBER"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:182:17: ( ( 'a' .. 'z' | 'A' .. 'Z' ) )
            // org/brackit/xquery/util/path/Path.g:182:19: ( 'a' .. 'z' | 'A' .. 'Z' )
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z') ) {
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
    // $ANTLR end "LETTER"

    // $ANTLR start "CHAR"
    public final void mCHAR() throws RecognitionException {
        try {
            // org/brackit/xquery/util/path/Path.g:183:16: ( ( LETTER | NUMBER | '_' | '-' | ':' ) )
            // org/brackit/xquery/util/path/Path.g:183:18: ( LETTER | NUMBER | '_' | '-' | ':' )
            {
            // org/brackit/xquery/util/path/Path.g:183:18: ( LETTER | NUMBER | '_' | '-' | ':' )
            int alt1=5;
            switch ( input.LA(1) ) {
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                {
                alt1=1;
                }
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                {
                alt1=2;
                }
                break;
            case '_':
                {
                alt1=3;
                }
                break;
            case '-':
                {
                alt1=4;
                }
                break;
            case ':':
                {
                alt1=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }

            switch (alt1) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:183:19: LETTER
                    {
                    mLETTER(); 

                    }
                    break;
                case 2 :
                    // org/brackit/xquery/util/path/Path.g:183:26: NUMBER
                    {
                    mNUMBER(); 

                    }
                    break;
                case 3 :
                    // org/brackit/xquery/util/path/Path.g:183:33: '_'
                    {
                    match('_'); 

                    }
                    break;
                case 4 :
                    // org/brackit/xquery/util/path/Path.g:183:37: '-'
                    {
                    match('-'); 

                    }
                    break;
                case 5 :
                    // org/brackit/xquery/util/path/Path.g:183:41: ':'
                    {
                    match(':'); 

                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "CHAR"

    // $ANTLR start "TAG"
    public final void mTAG() throws RecognitionException {
        try {
            int _type = TAG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:184:5: ( ( CHAR )+ ( '.' CHAR ( ( '.' )? CHAR )* )? )
            // org/brackit/xquery/util/path/Path.g:184:7: ( CHAR )+ ( '.' CHAR ( ( '.' )? CHAR )* )?
            {
            // org/brackit/xquery/util/path/Path.g:184:7: ( CHAR )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0=='-'||(LA2_0>='0' && LA2_0<=':')||(LA2_0>='A' && LA2_0<='Z')||LA2_0=='_'||(LA2_0>='a' && LA2_0<='z')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // org/brackit/xquery/util/path/Path.g:184:7: CHAR
            	    {
            	    mCHAR(); 

            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);

            // org/brackit/xquery/util/path/Path.g:184:12: ( '.' CHAR ( ( '.' )? CHAR )* )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='.') ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // org/brackit/xquery/util/path/Path.g:184:13: '.' CHAR ( ( '.' )? CHAR )*
                    {
                    match('.'); 
                    mCHAR(); 
                    // org/brackit/xquery/util/path/Path.g:184:20: ( ( '.' )? CHAR )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( ((LA4_0>='-' && LA4_0<='.')||(LA4_0>='0' && LA4_0<=':')||(LA4_0>='A' && LA4_0<='Z')||LA4_0=='_'||(LA4_0>='a' && LA4_0<='z')) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // org/brackit/xquery/util/path/Path.g:184:21: ( '.' )? CHAR
                    	    {
                    	    // org/brackit/xquery/util/path/Path.g:184:21: ( '.' )?
                    	    int alt3=2;
                    	    int LA3_0 = input.LA(1);

                    	    if ( (LA3_0=='.') ) {
                    	        alt3=1;
                    	    }
                    	    switch (alt3) {
                    	        case 1 :
                    	            // org/brackit/xquery/util/path/Path.g:184:22: '.'
                    	            {
                    	            match('.'); 

                    	            }
                    	            break;

                    	    }

                    	    mCHAR(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "TAG"

    // $ANTLR start "PROTOCOL"
    public final void mPROTOCOL() throws RecognitionException {
        try {
            int _type = PROTOCOL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // org/brackit/xquery/util/path/Path.g:185:9: ( LETTER LETTER ( LETTER )+ '://' )
            // org/brackit/xquery/util/path/Path.g:185:11: LETTER LETTER ( LETTER )+ '://'
            {
            mLETTER(); 
            mLETTER(); 
            // org/brackit/xquery/util/path/Path.g:185:25: ( LETTER )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='A' && LA6_0<='Z')||(LA6_0>='a' && LA6_0<='z')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // org/brackit/xquery/util/path/Path.g:185:25: LETTER
            	    {
            	    mLETTER(); 

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

            match("://"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PROTOCOL"

    public void mTokens() throws RecognitionException {
        // org/brackit/xquery/util/path/Path.g:1:8: ( STARTPARENT | SELFSTART | SELF | PARENT | CHILD | DESC | DESC_ATT | CHILD_ATT | WILDCARD | TAG | PROTOCOL )
        int alt7=11;
        alt7 = dfa7.predict(input);
        switch (alt7) {
            case 1 :
                // org/brackit/xquery/util/path/Path.g:1:10: STARTPARENT
                {
                mSTARTPARENT(); 

                }
                break;
            case 2 :
                // org/brackit/xquery/util/path/Path.g:1:22: SELFSTART
                {
                mSELFSTART(); 

                }
                break;
            case 3 :
                // org/brackit/xquery/util/path/Path.g:1:32: SELF
                {
                mSELF(); 

                }
                break;
            case 4 :
                // org/brackit/xquery/util/path/Path.g:1:37: PARENT
                {
                mPARENT(); 

                }
                break;
            case 5 :
                // org/brackit/xquery/util/path/Path.g:1:44: CHILD
                {
                mCHILD(); 

                }
                break;
            case 6 :
                // org/brackit/xquery/util/path/Path.g:1:50: DESC
                {
                mDESC(); 

                }
                break;
            case 7 :
                // org/brackit/xquery/util/path/Path.g:1:55: DESC_ATT
                {
                mDESC_ATT(); 

                }
                break;
            case 8 :
                // org/brackit/xquery/util/path/Path.g:1:64: CHILD_ATT
                {
                mCHILD_ATT(); 

                }
                break;
            case 9 :
                // org/brackit/xquery/util/path/Path.g:1:74: WILDCARD
                {
                mWILDCARD(); 

                }
                break;
            case 10 :
                // org/brackit/xquery/util/path/Path.g:1:83: TAG
                {
                mTAG(); 

                }
                break;
            case 11 :
                // org/brackit/xquery/util/path/Path.g:1:87: PROTOCOL
                {
                mPROTOCOL(); 

                }
                break;

        }

    }


    protected DFA7 dfa7 = new DFA7(this);
    static final String DFA7_eotS =
        "\1\uffff\1\7\1\13\1\uffff\1\5\3\uffff\1\16\1\20\2\uffff\1\5\4\uffff"+
        "\2\5\1\uffff";
    static final String DFA7_eofS =
        "\24\uffff";
    static final String DFA7_minS =
        "\1\52\2\56\1\uffff\1\101\3\uffff\1\56\1\100\2\uffff\1\101\4\uffff"+
        "\1\72\1\57\1\uffff";
    static final String DFA7_maxS =
        "\1\172\1\56\1\100\1\uffff\1\172\3\uffff\1\56\1\100\2\uffff\1\172"+
        "\4\uffff\1\172\1\57\1\uffff";
    static final String DFA7_acceptS =
        "\3\uffff\1\11\1\uffff\1\12\1\1\1\2\2\uffff\1\10\1\5\1\uffff\1\4"+
        "\1\3\1\7\1\6\2\uffff\1\13";
    static final String DFA7_specialS =
        "\24\uffff}>";
    static final String[] DFA7_transitionS = {
            "\1\3\2\uffff\1\5\1\1\1\2\13\5\6\uffff\32\4\4\uffff\1\5\1\uffff"+
            "\32\4",
            "\1\6",
            "\1\10\1\11\20\uffff\1\12",
            "",
            "\32\14\6\uffff\32\14",
            "",
            "",
            "",
            "\1\15",
            "\1\17",
            "",
            "",
            "\32\21\6\uffff\32\21",
            "",
            "",
            "",
            "",
            "\1\22\6\uffff\32\21\6\uffff\32\21",
            "\1\23",
            ""
    };

    static final short[] DFA7_eot = DFA.unpackEncodedString(DFA7_eotS);
    static final short[] DFA7_eof = DFA.unpackEncodedString(DFA7_eofS);
    static final char[] DFA7_min = DFA.unpackEncodedStringToUnsignedChars(DFA7_minS);
    static final char[] DFA7_max = DFA.unpackEncodedStringToUnsignedChars(DFA7_maxS);
    static final short[] DFA7_accept = DFA.unpackEncodedString(DFA7_acceptS);
    static final short[] DFA7_special = DFA.unpackEncodedString(DFA7_specialS);
    static final short[][] DFA7_transition;

    static {
        int numStates = DFA7_transitionS.length;
        DFA7_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA7_transition[i] = DFA.unpackEncodedString(DFA7_transitionS[i]);
        }
    }

    class DFA7 extends DFA {

        public DFA7(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 7;
            this.eot = DFA7_eot;
            this.eof = DFA7_eof;
            this.min = DFA7_min;
            this.max = DFA7_max;
            this.accept = DFA7_accept;
            this.special = DFA7_special;
            this.transition = DFA7_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( STARTPARENT | SELFSTART | SELF | PARENT | CHILD | DESC | DESC_ATT | CHILD_ATT | WILDCARD | TAG | PROTOCOL );";
        }
    }
 

}