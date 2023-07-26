/*
 * [New BSD License]
 * Copyright (c) 2011-2022, Brackit Project Team <info@brackit.org>
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
package org.brackit.query.function.fn;

import org.brackit.query.*;
import org.brackit.query.atomic.DateTime;
import org.brackit.query.atomic.*;
import org.brackit.query.node.parser.DocumentParser;
import org.brackit.query.sequence.ItemSequence;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.node.Node;
import org.brackit.query.jdm.node.NodeCollection;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FnTest extends XQueryBaseTest {

  @Test
  public void fnConcatNoVarArg() {
    Sequence result = new Query("concat('A', 'B')").execute(ctx);
    ResultChecker.dCheck(new Str("AB"), result);
  }

  @Test
  public void fnConcatOneVarArg() {
    Sequence result = new Query("concat('A', 'B', 'C')").execute(ctx);
    ResultChecker.dCheck(new Str("ABC"), result);
  }

  @Test
  public void fnConcatTwoVarArgs() {
    Sequence result = new Query("concat('A', 'B', 'C', 'D')").execute(ctx);
    ResultChecker.dCheck(new Str("ABCD"), result);
  }

  @Test
  public void fnAvg() {
    Sequence result = new Query("avg((1,2,3,4,5,6,7,8,9))").execute(ctx);
    ResultChecker.dCheck(new Int32("5"), result);
  }

  @Test
  public void fnAvgEmptySequence() {
    Sequence result = new Query("fn:avg(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnAvgLargeSequence() {
    Sequence result = new Query("avg((1 to 19999999))").execute(ctx);
    ResultChecker.dCheck(new Int32("10000000"), result);
  }

  @Test
  public void fnDoc() {
    Sequence result = new Query("doc('test.xml')").execute(ctx);
    ResultChecker.dCheck(store.lookup("test.xml").getDocument(), result);
  }

  @Test
  public void fnDocDefaultDocument() {
    NodeCollection<?> collection = store.lookup("test.xml");
    Node<?> documentNode = collection.getDocument();
    ctx.setDefaultDocument(collection.getDocument());
    Sequence result = new Query("doc('')").execute(ctx);
    ResultChecker.dCheck(documentNode, result);
  }

  @Test
  public void fnTrue() {
    Sequence result = new Query("true()").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnFalse() {
    Sequence result = new Query("fn:false()").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void fnRootDefaultContextItem() {
    NodeCollection<?> collection = store.lookup("test.xml");
    Node<?> documentNode = collection.getDocument();
    ctx.setContextItem(documentNode);
    Sequence result = new Query("fn:root()").execute(ctx);
    ResultChecker.dCheck(documentNode, result);
  }

  @Test
  public void fnRootDefaultContextItemExplicit() {
    NodeCollection<?> collection = store.lookup("test.xml");
    Node<?> documentNode = collection.getDocument();
    ctx.setContextItem(documentNode);
    Sequence result = new Query("fn:root($$)").execute(ctx);
    ResultChecker.dCheck(documentNode, result);
  }

  @Test
  public void fnRootInPathExpr() {
    NodeCollection<?> coll = ctx.getNodeStore().create("test.xml", new DocumentParser("<a><b><c/><d/></b></a>"));
    Node<?> doc = coll.getDocument();
    ctx.setContextItem(doc);
    Sequence result = new Query("$$//d/fn:root()").execute(ctx);
    ResultChecker.dCheck(doc, result);
  }

  @Test
  public void fnStringValueString() throws QueryException {
    Sequence result = new Query("string('string-value')").execute(ctx);
    ResultChecker.dCheck(new Str("string-value"), result);
  }

  @Test
  public void fnStringValueNode() throws QueryException {
    Sequence result = new Query("string(doc('test.xml')/a/c)").execute(ctx);
    ResultChecker.dCheck(new Str("text2"), result);
  }

  @Test
  public void fnStringValueEmptySeq() throws QueryException {
    Sequence result = new Query("string(())").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnStringValueNoArg() throws QueryException {
    ctx.setContextItem(new Str("string-of-length-19"));
    Sequence result = new Query("string()").execute(ctx);
    ResultChecker.dCheck(new Str("string-of-length-19"), result);
  }

  @Test
  public void fnStringValueNoArgCtxItemNotSet() throws QueryException {
    try {
      new Query("string()").execute(ctx);
      fail("No error thrown despite context item not set.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, e.getCode());
    }
  }

  @Test
  public void fnStringLength() throws QueryException {
    Sequence result = new Query("string-length('xtc')").execute(ctx);
    ResultChecker.dCheck(new Int32(3), result);
  }

  @Test
  public void fnStringLengthAccessContextItem() throws QueryException {
    Sequence result = new Query("(<a>AHA</a>)/string-length()").execute(ctx);
    ResultChecker.dCheck(new Int32(3), result);
  }

  @Test
  public void fnStringLengthEmptySeq() throws QueryException {
    Sequence result = new Query("string-length(())").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnStringLengthNoArg() throws QueryException {
    ctx.setContextItem(new Str("string-of-length-19"));
    Sequence result = new Query("string-length()").execute(ctx);
    ResultChecker.dCheck(new Int32(19), result);
  }

  @Test
  public void fnStringLengthNoArgCtxItemNotSet() throws QueryException {
    try {
      new Query("string-length()").execute(ctx);
      fail("No error thrown despite context item not set.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, e.getCode());
    }
  }

  @Test
  public void fnUpperCase() throws QueryException {
    Sequence result = new Query("upper-case('xtc0!')").execute(ctx);
    ResultChecker.dCheck(new Str("XTC0!"), result);
  }

  @Test
  public void fnUpperCaseEmptySeq() throws QueryException {
    Sequence result = new Query("upper-case(())").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnLowerCase() throws QueryException {
    Sequence result = new Query("lower-case('XTC0!')").execute(ctx);
    ResultChecker.dCheck(new Str("xtc0!"), result);
  }

  @Test
  public void fnLowerCaseEmptySeq() throws QueryException {
    Sequence result = new Query("lower-case(())").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnStringNormalize() throws QueryException {
    Sequence result = new Query("normalize-space(' \tin\r\nvade d by_whitespace \n ')").execute(ctx);
    ResultChecker.dCheck(new Str("in vade d by_whitespace"), result);
  }

  @Test
  public void fnStringNormalizeEmptySeq() throws QueryException {
    Sequence result = new Query("normalize-space(())").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnStringNormalizeNoArg() throws QueryException {
    ctx.setContextItem(new Str(" \tin\r\nvade d by_whitespace \n "));
    Sequence result = new Query("normalize-space()").execute(ctx);
    ResultChecker.dCheck(new Str("in vade d by_whitespace"), result);
  }

  @Test
  public void fnStringNormalizeNoArgCtxItemNotSet() throws QueryException {
    try {
      new Query("normalize-space()").execute(ctx);
      fail("No error thrown despite context item not set.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED, e.getCode());
    }
  }

  @Test
  public void fnStringTranslate() throws QueryException {
    Sequence result = new Query("fn:translate('Gasthaus', 'tGh', 'sHri')").execute(ctx);
    ResultChecker.dCheck(new Str("Hassraus"), result);
  }

  @Test
  public void fnStringTranslateEmptySeq() throws QueryException {
    Sequence result = new Query("fn:translate((), 'map', 'trans')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnSubsequence2ArgFromStart() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(1))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4), new Int32(5)),
                         result);
  }

  @Test
  public void fnSubsequence2ArgFromMiddle() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(3))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(3), new Int32(4), new Int32(5)), result);
  }

  @Test
  public void fnSubsequence2ArgFromEnd() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(5))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(5)), result);
  }

  @Test
  public void fnSubsequence2ArgFromBeforeStart() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(0))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4), new Int32(5)),
                         result);
  }

  @Test
  public void fnSubsequence2ArgFromAfterEnd() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(6))").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnSubsequence3ArgFromStart() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(1), xs:double(5))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4), new Int32(5)),
                         result);
  }

  @Test
  public void fnSubsequence3ArgFromMiddle() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(4), xs:double(1))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(4)), result);
  }

  @Test
  public void fnSubsequence3ArgFromMiddleToAfterEnd() {
    Sequence result = new Query("fn:subsequence((1, 2, 3, 4, 5), xs:double(4), xs:double(3))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(4), new Int32(5)), result);
  }

  @Test
  public void fnSubsequenceEmpSeq() {
    Sequence result = new Query("fn:subsequence((), xs:double(4), xs:double(3))").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnReverse() {
    Sequence result = new Query("fn:reverse((1, 2, 3, 4, 5))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(5), new Int32(4), new Int32(3), new Int32(2), new Int32(1)),
                         result);
  }

  @Test
  public void fnReverseEmpSeq() {
    Sequence result = new Query("fn:reverse(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnRemoveStart() {
    Sequence result = new Query("fn:remove((1, 2, 3, 4, 5), 1)").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(2), new Int32(3), new Int32(4), new Int32(5)), result);
  }

  @Test
  public void fnRemoveMiddle() {
    Sequence result = new Query("fn:remove((1, 2, 3, 4, 5), 2)").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(3), new Int32(4), new Int32(5)), result);
  }

  @Test
  public void fnRemoveEnd() {
    Sequence result = new Query("fn:remove((1, 2, 3, 4, 5), 5)").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4)), result);
  }

  @Test
  public void fnRemoveIllegalIndex() {
    Sequence result = new Query("fn:remove((1, 2, 3, 4, 5), 7)").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4), new Int32(5)),
                         result);
  }

  @Test
  public void fnRemoveEmpSeq() {
    Sequence result = new Query("fn:remove((), 1)").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnInsertBeforeStart() {
    Sequence result = new Query("fn:insert-before((1, 2, 3, 4, 5), 1, (7))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(7),
                                          new Int32(1),
                                          new Int32(2),
                                          new Int32(3),
                                          new Int32(4),
                                          new Int32(5)), result);
  }

  @Test
  public void fnInsertBeforeMiddle() {
    Sequence result = new Query("fn:insert-before((1, 2, 3, 4, 5), 3, (7))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1),
                                          new Int32(2),
                                          new Int32(7),
                                          new Int32(3),
                                          new Int32(4),
                                          new Int32(5)), result);
  }

  @Test
  public void fnInsertBeforeEnd() {
    Sequence result = new Query("fn:insert-before((1, 2, 3, 4, 5), 6, (7))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1),
                                          new Int32(2),
                                          new Int32(3),
                                          new Int32(4),
                                          new Int32(5),
                                          new Int32(7)), result);
  }

  @Test
  public void fnInsertBeforeEmpSeq() {
    Sequence result = new Query("fn:insert-before((), 6, (7))").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(7)), result);
  }

  @Test
  public void fnInsertBeforeEmpIns() {
    Sequence result = new Query("fn:insert-before((1, 2, 3, 4, 5), 6, ())").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(3), new Int32(4), new Int32(5)),
                         result);
  }

  @Test
  public void fnInsertBeforeEmpBoth() {
    Sequence result = new Query("fn:insert-before((), 6, ())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnIndexOfExistant() {
    Sequence result = new Query("fn:index-of((1, 2, 3, 4, 5), 5)").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(5)), result);
  }

  @Test
  public void fnIndexOfUnExistant() {
    Sequence result = new Query("fn:index-of((1, 2, 3, 4, 5), 6)").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnIndexOfEmpSeq() {
    Sequence result = new Query("fn:index-of((), 5)").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnStringToCodepoints() {
    Sequence result = new Query("fn:string-to-codepoints('Thérèse')").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(84),
                                          new Int32(104),
                                          new Int32(233),
                                          new Int32(114),
                                          new Int32(232),
                                          new Int32(115),
                                          new Int32(101)), result);
  }

  @Test
  public void fnStringToCodepointsEmpStr() {
    Sequence result = new Query("fn:string-to-codepoints('')").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnStringToCodepointsEmpSeq() {
    Sequence result = new Query("fn:string-to-codepoints(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnCodepointsToString() {
    Sequence result = new Query("fn:codepoints-to-string((84, 104, 233, 114, 232, 115, 101))").execute(ctx);
    ResultChecker.dCheck(new Str("Thérèse"), result);
  }

  @Test
  public void fnCodepointsToStringEmpSeq() {
    Sequence result = new Query("fn:codepoints-to-string(())").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnCodepointsToStrIllegalCodepoint() {
    try {
      new Query("codepoints-to-string((1))").execute(ctx);
      fail("Accepted invalid codepoint");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_CODE_POINT_NOT_VALID, e.getCode());
    }
  }

  @Test
  public void fnSubstringBefore() {
    Sequence result = new Query("fn:substring-before('Thérèse est jolie.', ' est')").execute(ctx);
    ResultChecker.dCheck(new Str("Thérèse"), result);
  }

  @Test
  public void fnSubstringBeforeEmpPattern() {
    Sequence result = new Query("fn:substring-before('Thérèse est jolie.', '')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnSubstringBeforeEmpTarget() {
    Sequence result = new Query("fn:substring-before('', ' est')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnSubstringBeforeNotContained() {
    Sequence result = new Query("fn:substring-before('Thérèse est jolie.', 'baguette')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnSubstringAfter() {
    Sequence result = new Query("fn:substring-after('Thérèse est jolie.', 'est ')").execute(ctx);
    ResultChecker.dCheck(new Str("jolie."), result);
  }

  @Test
  public void fnSubstringAfterEmpPattern() {
    Sequence result = new Query("fn:substring-after('Thérèse est jolie.', '')").execute(ctx);
    ResultChecker.dCheck(new Str("Thérèse est jolie."), result);
  }

  @Test
  public void fnSubstringAfterEmpTarget() {
    Sequence result = new Query("fn:substring-after('', ' est')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnSubstringAfterNotContained() {
    Sequence result = new Query("fn:substring-after('Thérèse est jolie.', 'baguette')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnEncodeForUri() {
    Sequence result = new Query("fn:encode-for-uri('test')").execute(ctx);
    ResultChecker.dCheck(new Str("test"), result);
  }

  @Test
  public void fnEncodeForUriIllegalUri() {
    Sequence result = new Query("fn:encode-for-uri('http://test@hallo')").execute(ctx);
    ResultChecker.dCheck(new Str("http%3A%2F%2Ftest%40hallo"), result);
  }

  @Test
  public void fnEncodeForUriIllegalUriIsIri() {
    Sequence result = new Query("fn:encode-for-uri('bébé')").execute(ctx);
    ResultChecker.dCheck(new Str("b%C3%A9b%C3%A9"), result);
  }

  @Test
  public void fnEncodeForUriIllegalUriIllegalASCII() {
    Sequence result = new Query("fn:encode-for-uri('test\u0001\u0000')").execute(ctx);
    ResultChecker.dCheck(new Str("test%01%00"), result);
  }

  @Test
  public void fnEncodeForUriIllegalUriEndsOnSurrogatePair() {
    Sequence result = new Query("fn:encode-for-uri('test\uD800\uDC00')").execute(ctx);
    ResultChecker.dCheck(new Str("test%F0%90%80%80"), result);
  }

  @Test
  public void fnIriToUri() {
    Sequence result = new Query("fn:iri-to-uri('http://bébé.fr')").execute(ctx);
    ResultChecker.dCheck(new Str("http://b%C3%A9b%C3%A9.fr"), result);
  }

  @Test
  public void fnIriToUriIllegalIri() {
    Sequence result = new Query("fn:iri-to-uri('http://bébé.fr/\uD800\uDC00')").execute(ctx);
    ResultChecker.dCheck(new Str("http://b%C3%A9b%C3%A9.fr/%F0%90%80%80"), result);
  }

  @Test
  public void fnMatchesSimple1() {
    Sequence result = new Query("fn:matches('abracadabra', 'bra')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesSimple2() {
    Sequence result = new Query("fn:matches('abracadabra', '^a.*a$')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesSimple3() {
    Sequence result = new Query("fn:matches('abracadabra', '^bra')").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void fnMatchesSimple4() {
    Sequence result = new Query("fn:matches('abra\ncadabra', 'bra')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesSimpleEmptySeqInput() {
    Sequence result = new Query("fn:matches((), '.?')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesSimpleEmptyInput() {
    Sequence result = new Query("fn:matches('', '.?')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesEmptyFlags() {
    Sequence result = new Query("fn:matches('abra\ncadabra', 'bra', '')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesIrrelaventFlags() {
    Sequence result = new Query("fn:matches('abra\ncadabra', 'bra', 'm')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesNoMatchSingleLine() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'Kaum.*krähen')").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void fnMatchesMatchLineSpanning() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'Kaum.*krähen', 's')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesNoMatchWholeInput() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', '^Kaum.*gesehen,$')").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void fnMatchesMatchWholeLine() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', '^Kaum.*gesehen,$', 'm')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesIgnoreCase() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'kaum', 'i')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesIgnoreWhitespace() {
    Sequence result = new Query(
                                 "fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'K aum', 'x')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesKeepWhitespaceInCharClass() {
    Sequence result = new Query("fn:matches('bra bra', '(br a)[ ]\\1', 'x')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesIllegalFlag() {
    try {
      new Query("fn:matches('abracadabra', 'bra', 'ü')").execute(ctx);
      fail("Function accepted illegal flag.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION_FLAGS, e.getCode());
    }
  }

  @Test
  public void fnMatchesPureGroup() {
    try {
      new Query("fn:matches('abracadabra', '(?bra).*')").execute(ctx);
      fail("Pure groups disallowed.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesTooManyOpenBrackets() {
    try {
      new Query("fn:matches('abracadabra', '[bra')").execute(ctx);
      fail("Too many opening brackets.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesTooManyCloseBrackets() {
    try {
      new Query("fn:matches('abracadabra', 'bra]')").execute(ctx);
      fail("Too many closing brackets.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesTooManyOpenParens() {
    try {
      new Query("fn:matches('abracadabra', '(bra')").execute(ctx);
      fail("Too many opening parentheses.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesTooManyCloseParens() {
    try {
      new Query("fn:matches('abracadabra', 'bra)')").execute(ctx);
      fail("Too many closing parentheses.");
    } catch (QueryException ignored) {
    }
  }

  @Test
  public void fnMatchesLegalBackRef() {
    Sequence result = new Query("fn:matches('brabra', '(bra)\\1')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnMatchesIllegalBackRef() {
    try {
      new Query("fn:matches('brabra', '(bra)\\2')").execute(ctx);
      fail("Illegal back reference.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesTrailingBackslash() {
    try {
      new Query("fn:matches('brabra', '(bra)\\')").execute(ctx);
      fail("Trailing back slash.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesBackRefToGroup0() {
    try {
      new Query("fn:matches('brabra', '(bra)\\0')").execute(ctx);
      fail("Back ref to group 0.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnMatchesIllegalBackRefInCharClass() {
    try {
      new Query("fn:matches('brabra', '(bra)[a-r\\1]*')").execute(ctx);
      fail("Illegal back reference in character class.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
    }
  }

  @Test
  public void fnReplaceSimple1() {
    Sequence result = new Query("fn:replace('abracadabra', 'bra', '*')").execute(ctx);
    ResultChecker.dCheck(new Str("a*cada*"), result);
  }

  @Test
  public void fnReplaceSimple2() {
    Sequence result = new Query("fn:replace('abracadabra', 'a.*a', '*')").execute(ctx);
    ResultChecker.dCheck(new Str("*"), result);
  }

  @Test
  public void fnReplaceSimple3() {
    Sequence result = new Query("fn:replace('abracadabra', 'a.*?a', '*')").execute(ctx);
    ResultChecker.dCheck(new Str("*c*bra"), result);
  }

  @Test
  public void fnReplaceSimple4() {
    Sequence result = new Query("fn:replace('abracadabra', 'a', '')").execute(ctx);
    ResultChecker.dCheck(new Str("brcdbr"), result);
  }

  @Test
  public void fnReplaceSimple5() {
    Sequence result = new Query("fn:replace('abracadabra', 'a(.)', 'a$1$1')").execute(ctx);
    ResultChecker.dCheck(new Str("abbraccaddabbra"), result);
  }

  @Test
  public void fnReplaceEmptySeqInput() {
    Sequence result = new Query("fn:replace((), 'a(.)', 'a$1$1')").execute(ctx);
    ResultChecker.dCheck(Str.EMPTY, result);
  }

  @Test
  public void fnReplaceEmptyPattern() {
    try {
      new Query("fn:replace('abracadabra', '.*?', '$1')").execute(ctx);
      fail("Pattern matches empty string.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING, e.getCode());
    }
  }

  @Test
  public void fnReplaceSingleBackslash() {
    try {
      new Query("fn:replace('abracadabra', 'bra', '\\')").execute(ctx);
      fail("Replacement string consists of single backslash.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
    }
  }

  @Test
  public void fnReplaceSingleDollarSign() {
    try {
      new Query("fn:replace('abracadabra', 'bra', '$')").execute(ctx);
      fail("Replacement string consists of single dollar sign.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
    }
  }

  @Test
  public void fnReplaceIllegallyUsedBackslash() {
    try {
      new Query("fn:replace('abracadabra', 'bra', 'x\\x')").execute(ctx);
      fail("Replacement string makes illegal use of backslash.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
    }
  }

  @Test
  public void fnReplaceIllegallyUsedDollarSign() {
    try {
      new Query("fn:replace('abracadabra', 'bra', 'x$x')").execute(ctx);
      fail("Replacement string makes illegal use of dollar sign.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
    }
  }

  @Test
  public void fnReplaceEscapedDollarSign() {
    Sequence result = new Query("fn:replace('abracadabra', 'bra', '\\$')").execute(ctx);
    ResultChecker.dCheck(new Str("a$cada$"), result);
  }

  @Test
  public void fnReplaceEscapedBackSlash() {
    Sequence result = new Query("fn:replace('abracadabra', 'bra', '\\\\')").execute(ctx);
    ResultChecker.dCheck(new Str("a\\cada\\"), result);
  }

  @Test
  public void fnReplaceAlternative() {
    Sequence result = new Query("fn:replace('abcd', '(ab)|(a)', '[1=$1][2=$2]')").execute(ctx);
    ResultChecker.dCheck(new Str("[1=ab][2=]cd"), result);
  }

  @Test
  public void fnTokenizeSimple1() {
    Sequence result = new Query("fn:tokenize('The cat sat', '\\s+')").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Str("The"), new Str("cat"), new Str("sat")), result);
  }

  @Test
  public void fnTokenizeSimple2() {
    Sequence result = new Query("fn:tokenize('1,15,,24,50,', ',')").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Str("1"),
                                          new Str("15"),
                                          Str.EMPTY,
                                          new Str("24"),
                                          new Str("50"),
                                          Str.EMPTY), result);
  }

  @Test
  public void fnTokenizeEmptySeqInput() {
    Sequence result = new Query("fn:tokenize((), ',')").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTokenizeEmptyInput() {
    Sequence result = new Query("fn:tokenize('', ',')").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTokenizeEmptyPattern() {
    try {
      new Query("fn:tokenize('abracadabra', '.?')").execute(ctx);
      fail("Pattern matches empty string.");
    } catch (QueryException e) {
      assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING, e.getCode());
    }
  }

  @Test
  public void fnYearsFromDuration() {
    Sequence result = new Query("fn:years-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(21), result);
  }

  @Test
  public void fnYearsFromDurationNegative() {
    Sequence result = new Query("fn:years-from-duration(xs:yearMonthDuration('-P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(-21), result);
  }

  @Test
  public void fnYearsFromDurationEmpSeq() {
    Sequence result = new Query("fn:years-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnYearsFromDurationOtherDur() {
    Sequence result = new Query("fn:years-from-duration(xs:dayTimeDuration('P7DT25H'))").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnMonthsFromDuration() {
    Sequence result = new Query("fn:months-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(3), result);
  }

  @Test
  public void fnMonthsFromDurationNegative() {
    Sequence result = new Query("fn:months-from-duration(xs:yearMonthDuration('-P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(-3), result);
  }

  @Test
  public void fnMonthsFromDurationEmpSeq() {
    Sequence result = new Query("fn:months-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMonthsFromDurationOtherDur() {
    Sequence result = new Query("fn:months-from-duration(xs:dayTimeDuration('P7DT25H'))").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnDaysFromDuration() {
    Sequence result = new Query("fn:days-from-duration(xs:dayTimeDuration('P7DT25H'))").execute(ctx);
    ResultChecker.dCheck(new Int32(8), result);
  }

  @Test
  public void fnDaysFromDurationNegative() {
    Sequence result = new Query("fn:days-from-duration(xs:dayTimeDuration('-P7DT25H'))").execute(ctx);
    ResultChecker.dCheck(new Int32(-8), result);
  }

  @Test
  public void fnDaysFromDurationEmpSeq() {
    Sequence result = new Query("fn:days-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnDaysFromDurationOtherDur() {
    Sequence result = new Query("fn:days-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnHoursFromDuration() {
    Sequence result = new Query("fn:hours-from-duration(xs:dayTimeDuration('P7DT25H61M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(2), result);
  }

  @Test
  public void fnHoursFromDurationNegative() {
    Sequence result = new Query("fn:hours-from-duration(xs:dayTimeDuration('-P7DT25H61M'))").execute(ctx);
    ResultChecker.dCheck(new Int32(-2), result);
  }

  @Test
  public void fnHoursFromDurationEmpSeq() {
    Sequence result = new Query("fn:hours-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnHoursFromDurationOtherDur() {
    Sequence result = new Query("fn:hours-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnMinutesFromDuration() {
    Sequence result = new Query("fn:minutes-from-duration(xs:dayTimeDuration('PT1M61S'))").execute(ctx);
    ResultChecker.dCheck(new Int32(2), result);
  }

  @Test
  public void fnMinutesFromDurationNegative() {
    Sequence result = new Query("fn:minutes-from-duration(xs:dayTimeDuration('-PT1M61S'))").execute(ctx);
    ResultChecker.dCheck(new Int32(-2), result);
  }

  @Test
  public void fnMinutesFromDurationEmpSeq() {
    Sequence result = new Query("fn:minutes-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMinutesFromDurationOtherDur() {
    Sequence result = new Query("fn:minutes-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(Int32.ZERO, result);
  }

  @Test
  public void fnSecondsFromDuration() {
    Sequence result = new Query("fn:seconds-from-duration(xs:dayTimeDuration('PT1M61S'))").execute(ctx);
    ResultChecker.dCheck(new Dbl(1.0), result);
  }

  @Test
  public void fnSecondsFromDurationNegative() {
    Sequence result = new Query("fn:seconds-from-duration(xs:dayTimeDuration('-PT1M61S'))").execute(ctx);
    ResultChecker.dCheck(new Dbl(-1.0), result);
  }

  @Test
  public void fnSecondsFromDurationEmpSeq() {
    Sequence result = new Query("fn:seconds-from-duration(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnSecondsFromDurationOtherDur() {
    Sequence result = new Query("fn:seconds-from-duration(xs:yearMonthDuration('P20Y15M'))").execute(ctx);
    ResultChecker.dCheck(new Dbl(0.0), result);
  }

  @Test
  public void fnYearFromDateTime() {
    Sequence result = new Query("fn:year-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(1999), result);
  }

  @Test
  public void fnYearFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:year-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMonthFromDateTime() {
    Sequence result = new Query("fn:month-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(5), result);
  }

  @Test
  public void fnMonthFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:month-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnDayFromDateTime() {
    Sequence result = new Query("fn:day-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(31), result);
  }

  @Test
  public void fnDayFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:day-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnHoursFromDateTime() {
    Sequence result = new Query("fn:hours-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(13), result);
  }

  @Test
  public void fnHoursFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:hours-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMinutesFromDateTime() {
    Sequence result = new Query("fn:minutes-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(20), result);
  }

  @Test
  public void fnMinutesFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:minutes-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnSecondsFromDateTime() {
    Sequence result = new Query("fn:seconds-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Dbl(0.0), result);
  }

  @Test
  public void fnSecondsFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:seconds-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromDateTime() {
    Sequence result = new Query("fn:timezone-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new DTD("-PT5H"), result);
  }

  @Test
  public void fnTimezoneFromDateTimeWithoutTimezone() {
    Sequence result = new Query("fn:timezone-from-dateTime(xs:dateTime('1999-05-31T13:20:00'))").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromDateTimeEmpSeq() {
    Sequence result = new Query("fn:timezone-from-dateTime(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnYearFromDate() {
    Sequence result = new Query("fn:year-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(1999), result);
  }

  @Test
  public void fnYearFromDateEmpSeq() {
    Sequence result = new Query("fn:year-from-date(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMonthFromDate() {
    Sequence result = new Query("fn:month-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(5), result);
  }

  @Test
  public void fnMonthFromDateEmpSeq() {
    Sequence result = new Query("fn:month-from-date(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnDayFromDate() {
    Sequence result = new Query("fn:day-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(31), result);
  }

  @Test
  public void fnDayFromDateEmpSeq() {
    Sequence result = new Query("fn:day-from-date(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromDate() {
    Sequence result = new Query("fn:timezone-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
    ResultChecker.dCheck(new DTD("-PT5H"), result);
  }

  @Test
  public void fnTimezoneFromDateWithoutTimezone() {
    Sequence result = new Query("fn:timezone-from-date(xs:date('1999-05-31'))").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromDateEmpSeq() {
    Sequence result = new Query("fn:timezone-from-date(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnHoursFromTime() {
    Sequence result = new Query("fn:hours-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(13), result);
  }

  @Test
  public void fnHoursFromTimeEmpSeq() {
    Sequence result = new Query("fn:hours-from-time(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnMinutesFromTime() {
    Sequence result = new Query("fn:minutes-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Int32(20), result);
  }

  @Test
  public void fnMinutesFromTimeEmpSeq() {
    Sequence result = new Query("fn:minutes-from-time(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnSecondsFromTime() {
    Sequence result = new Query("fn:seconds-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Dbl(0.0), result);
  }

  @Test
  public void fnSecondsFromTimeEmpSeq() {
    Sequence result = new Query("fn:seconds-from-time(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromTime() {
    Sequence result = new Query("fn:timezone-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new DTD("-PT5H"), result);
  }

  @Test
  public void fnTimezoneFromTimeWithoutTimezone() {
    Sequence result = new Query("fn:timezone-from-time(xs:time('13:20:00'))").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnTimezoneFromTimeEmpSeq() {
    Sequence result = new Query("fn:timezone-from-time(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnAdjustDateTimeToTimezoneEmptyTimezone() {
    Sequence result = new Query("fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), ())")
                                                                                                                .execute(ctx);
    ResultChecker.dCheck(new DateTime("2002-03-07T10:00:00"), result);
  }

  @Test
  public void fnAdjustDateTimeToTimezone() {
    Sequence result = new Query(
                                 "fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), xs:dayTimeDuration('PT10H'))").execute(ctx);
    ResultChecker.dCheck(new DateTime("2002-03-08T01:00:00+10:00"), result);
  }

  @Test
  public void fnAdjustDateTimeToTimezoneWithoutTimezone() {
    Sequence result = new Query(
                                 "fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00'), xs:dayTimeDuration('PT10H'))").execute(ctx);
    ResultChecker.dCheck(new DateTime("2002-03-07T10:00:00+10:00"), result);
  }

  @Test
  public void fnAdjustDateTimeToTimezoneImplicitTimezone() {
    Sequence result = new Query("fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'))").execute(
                                                                                                                     ctx);
    ResultChecker.dCheck(new DateTime("2002-03-07T16:00:00+01:00"), result);
  }

  @Test
  public void fnAdjustDateTimeToTimezoneEmpSeq() {
    Sequence result = new Query("fn:adjust-dateTime-to-timezone(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnAdjustDateTimeToTimezoneIllegalTimezone() {
    try {
      new Query("fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), xs:dayTimeDuration('PT15H'))").execute(ctx);
      fail("Accepted illegal timezone.");
    } catch (QueryException e) {
      assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE, e.getCode());
    }
  }

  @Test
  public void fnAdjustDateToTimezoneEmptyTimezone() {
    Sequence result = new Query("fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), ())").execute(ctx);
    ResultChecker.dCheck(new Date("2002-03-07"), result);
  }

  @Test
  public void fnAdjustDateToTimezone() {
    Sequence result = new Query(
                                 "fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), xs:dayTimeDuration('-PT10H'))").execute(ctx);
    ResultChecker.dCheck(new Date("2002-03-06-10:00"), result);
  }

  @Test
  public void fnAdjustDateToTimezoneWithoutTimezone() {
    Sequence result = new Query("fn:adjust-date-to-timezone(xs:date('2002-03-07'), xs:dayTimeDuration('-PT10H'))")
                                                                                                                   .execute(ctx);
    ResultChecker.dCheck(new Date("2002-03-07-10:00"), result);
  }

  @Test
  public void fnAdjustDateToTimezoneImplicitTimezone() {
    Sequence result = new Query("fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Date("2002-03-07+02:00"), result);
  }

  @Test
  public void fnAdjustDateToTimezoneEmpSeq() {
    Sequence result = new Query("fn:adjust-date-to-timezone(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnAdjustDateToTimezoneIllegalTimezone() {
    try {
      new Query("fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), xs:dayTimeDuration('PT15H'))").execute(ctx);
      fail("Accepted illegal timezone.");
    } catch (QueryException e) {
      assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE, e.getCode());
    }
  }

  @Test
  public void fnAdjustTimeToTimezoneEmptyTimezone() {
    Sequence result = new Query("fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), ())").execute(ctx);
    ResultChecker.dCheck(new Time("10:00:00"), result);
  }

  @Test
  public void fnAdjustTimeToTimezone() {
    Sequence result = new Query("fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), xs:dayTimeDuration('PT10H'))")
                                                                                                                      .execute(ctx);
    ResultChecker.dCheck(new Time("01:00:00+10:00"), result);
  }

  @Test
  public void fnAdjustTimeToTimezoneWithoutTimezone() {
    Sequence result = new Query("fn:adjust-time-to-timezone(xs:time('10:00:00'), xs:dayTimeDuration('PT10H'))")
                                                                                                                .execute(ctx);
    ResultChecker.dCheck(new Time("10:00:00+10:00"), result);
  }

  @Test
  public void fnAdjustTimeToTimezoneImplicitTimezone() {
    Sequence result = new Query("fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'))").execute(ctx);
    ResultChecker.dCheck(new Time("16:00:00+01:00"), result);
  }

  @Test
  public void fnAdjustTimeToTimezoneEmpSeq() {
    Sequence result = new Query("fn:adjust-time-to-timezone(())").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void fnAdjustTimeToTimezoneIllegalTimezone() {
    try {
      new Query("fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), xs:dayTimeDuration('PT15H'))").execute(ctx);
      fail("Accepted illegal timezone.");
    } catch (QueryException e) {
      assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE, e.getCode());
    }
  }

  @Test
  public void fnMinDouble() {
    Sequence result = new Query("fn:min((198.95E0,282.69E0,188.72E0 ,268.38E0))").execute(ctx);
    ResultChecker.check(new Dbl(188.72), result);
  }

  @Test
  public void fnMaxDouble() {
    Sequence result = new Query("fn:max((198.95E0,282.69E0,188.72E0 ,268.38E0))").execute(ctx);
    ResultChecker.check(new Dbl(282.69), result);
  }

  @Test
  public void fnEndsWithTrue1() {
    Sequence result = new Query("fn:ends-with('abracadabra', 'bra')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnEndsWithTrue2() {
    Sequence result = new Query("fn:ends-with('tattoo', 'tattoo')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnEndsWithTrue3() {
    Sequence result = new Query("fn:ends-with('()', '()')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnEndsWithFalse() {
    Sequence result = new Query("fn:ends-with('tattoo', 'atto')").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void fnEndsWithEmpty() {
    Sequence result = new Query("fn:ends-with('', '')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnStartsWithTrue() {
    Sequence result = new Query("fn:starts-with('abracadabra', 'abr')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnStartsWithTrue2() {
    Sequence result = new Query("fn:starts-with('tattoo', 'tat')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnStartsWithTrue3() {
    Sequence result = new Query("fn:starts-with('()', '()')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void fnStartsWithFalse() {
    Sequence result = new Query("fn:starts-with('tattoo', 'att')").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    storeDocument("test.xml", "<a><b>text1<b>text2</b></b><c>text2</c></a>");
  }
}
