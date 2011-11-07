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
package org.brackit.xquery.function.fn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.atomic.Date;
import org.brackit.xquery.atomic.DateTime;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Time;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Before;
import org.junit.Test;

public class FnTest extends XQueryBaseTest {

	@Test
	public void fnConcatNoVarArg() throws Exception {
		Sequence result = new XQuery("concat('A', 'B')").execute(ctx);
		ResultChecker.dCheck(new Str("AB"), result);
	}

	@Test
	public void fnConcatOneVarArg() throws Exception {
		Sequence result = new XQuery("concat('A', 'B', 'C')").execute(ctx);
		ResultChecker.dCheck(new Str("ABC"), result);
	}

	@Test
	public void fnConcatTwoVarArgs() throws Exception {
		Sequence result = new XQuery("concat('A', 'B', 'C', 'D')").execute(ctx);
		ResultChecker.dCheck(new Str("ABCD"), result);
	}

	@Test
	public void fnAvg() throws Exception {
		Sequence result = new XQuery("avg((1,2,3,4,5,6,7,8,9))").execute(ctx);
		ResultChecker.dCheck(new Int32("5"), result);
	}

	@Test
	public void fnAvgEmptySequence() throws Exception {
		Sequence result = new XQuery("fn:avg(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnAvgLargeSequence() throws Exception {
		Sequence result = new XQuery("avg((1 to 19999999))").execute(ctx);
		ResultChecker.dCheck(new Int32("10000000"), result);
	}

	@Test
	public void fnDoc() throws Exception {
		Sequence result = new XQuery("doc('test.xml')").execute(ctx);
		ResultChecker.dCheck(store.lookup("test.xml").getDocument(), result);
	}

	@Test
	public void fnDocDefaultDocument() throws Exception {
		org.brackit.xquery.xdm.Collection<?> collection = store
				.lookup("test.xml");
		Node<?> documentNode = collection.getDocument();
		ctx.setDefaultDocument(collection.getDocument());
		Sequence result = new XQuery("doc('')").execute(ctx);
		ResultChecker.dCheck(documentNode, result);
	}

	@Test
	public void fnTrue() throws Exception {
		Sequence result = new XQuery("true()").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnFalse() throws Exception {
		Sequence result = new XQuery("fn:false()").execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void fnRootDefaultContextItem() throws Exception {
		org.brackit.xquery.xdm.Collection<?> collection = store
				.lookup("test.xml");
		Node<?> documentNode = collection.getDocument();
		ctx.setContextItem(documentNode);
		Sequence result = new XQuery("fn:root()").execute(ctx);
		ResultChecker.dCheck(documentNode, result);
	}

	@Test
	public void fnRootDefaultContextItemExplicit() throws Exception {
		org.brackit.xquery.xdm.Collection<?> collection = store
				.lookup("test.xml");
		Node<?> documentNode = collection.getDocument();
		ctx.setContextItem(documentNode);
		Sequence result = new XQuery("fn:root(.)").execute(ctx);
		ResultChecker.dCheck(documentNode, result);
	}

	@Test
	public void fnRootInPathExpr() throws Exception {
		Collection<?> coll = ctx.getStore().create("test.xml",
				new DocumentParser("<a><b><c/><d/></b></a>"));
		Node<?> doc = coll.getDocument();
		ctx.setContextItem(doc);
		Sequence result = new XQuery(".//d/fn:root()").execute(ctx);
		ResultChecker.dCheck(doc, result);
	}

	@Test
	public void fnStringValueString() throws QueryException {
		Sequence result = new XQuery("string('string-value')").execute(ctx);
		ResultChecker.dCheck(new Str("string-value"), result);
	}

	@Test
	public void fnStringValueNode() throws QueryException {
		Sequence result = new XQuery("string(doc('test.xml')/a/c)")
				.execute(ctx);
		ResultChecker.dCheck(new Str("text2"), result);
	}

	@Test
	public void fnStringValueEmptySeq() throws QueryException {
		Sequence result = new XQuery("string(())").execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnStringValueNoArg() throws QueryException {
		ctx.setContextItem(new Str("string-of-length-19"));
		Sequence result = new XQuery("string()").execute(ctx);
		ResultChecker.dCheck(new Str("string-of-length-19"), result);
	}

	@Test
	public void fnStringValueNoArgCtxItemNotSet() throws QueryException {
		try {
			new XQuery("string()").execute(ctx);
			fail("No error thrown despite context item not set.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
					e.getCode());
		}
	}

	@Test
	public void fnStringLength() throws QueryException {
		Sequence result = new XQuery("string-length('xtc')").execute(ctx);
		ResultChecker.dCheck(new Int32(3), result);
	}

	@Test
	public void fnStringLengthAccessContextItem() throws QueryException {
		Sequence result = new XQuery("(<a>AHA</a>)/string-length()")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(3), result);
	}

	@Test
	public void fnStringLengthEmptySeq() throws QueryException {
		Sequence result = new XQuery("string-length(())").execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnStringLengthNoArg() throws QueryException {
		ctx.setContextItem(new Str("string-of-length-19"));
		Sequence result = new XQuery("string-length()").execute(ctx);
		ResultChecker.dCheck(new Int32(19), result);
	}

	@Test
	public void fnStringLengthNoArgCtxItemNotSet() throws QueryException {
		try {
			new XQuery("string-length()").execute(ctx);
			fail("No error thrown despite context item not set.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
					e.getCode());
		}
	}

	@Test
	public void fnUpperCase() throws QueryException {
		Sequence result = new XQuery("upper-case('xtc0!')").execute(ctx);
		ResultChecker.dCheck(new Str("XTC0!"), result);
	}

	@Test
	public void fnUpperCaseEmptySeq() throws QueryException {
		Sequence result = new XQuery("upper-case(())").execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnLowerCase() throws QueryException {
		Sequence result = new XQuery("lower-case('XTC0!')").execute(ctx);
		ResultChecker.dCheck(new Str("xtc0!"), result);
	}

	@Test
	public void fnLowerCaseEmptySeq() throws QueryException {
		Sequence result = new XQuery("lower-case(())").execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnStringNormalize() throws QueryException {
		Sequence result = new XQuery(
				"normalize-space(' \tin\r\nvade d by_whitespace \n ')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("in vade d by_whitespace"), result);
	}

	@Test
	public void fnStringNormalizeEmptySeq() throws QueryException {
		Sequence result = new XQuery("normalize-space(())").execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnStringNormalizeNoArg() throws QueryException {
		ctx.setContextItem(new Str(" \tin\r\nvade d by_whitespace \n "));
		Sequence result = new XQuery("normalize-space()").execute(ctx);
		ResultChecker.dCheck(new Str("in vade d by_whitespace"), result);
	}

	@Test
	public void fnStringNormalizeNoArgCtxItemNotSet() throws QueryException {
		try {
			new XQuery("normalize-space()").execute(ctx);
			fail("No error thrown despite context item not set.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED,
					e.getCode());
		}
	}

	@Test
	public void fnStringTranslate() throws QueryException {
		Sequence result = new XQuery("fn:translate('Gasthaus', 'tGh', 'sHri')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("Hassraus"), result);
	}

	@Test
	public void fnStringTranslateEmptySeq() throws QueryException {
		Sequence result = new XQuery("fn:translate((), 'map', 'trans')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnSubsequence2ArgFromStart() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(1))").execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnSubsequence2ArgFromMiddle() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(3))").execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(3), new Int32(4),
				new Int32(5)), result);
	}

	@Test
	public void fnSubsequence2ArgFromEnd() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(5))").execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(5)), result);
	}

	@Test
	public void fnSubsequence2ArgFromBeforeStart() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(0))").execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnSubsequence2ArgFromAfterEnd() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(6))").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnSubsequence3ArgFromStart() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(1), xs:double(5))")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnSubsequence3ArgFromMiddle() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(4), xs:double(1))")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(4)), result);
	}

	@Test
	public void fnSubsequence3ArgFromMiddleToAfterEnd() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((1, 2, 3, 4, 5), xs:double(4), xs:double(3))")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(4), new Int32(5)),
				result);
	}

	@Test
	public void fnSubsequenceEmpSeq() throws Exception {
		Sequence result = new XQuery(
				"fn:subsequence((), xs:double(4), xs:double(3))").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnReverse() throws Exception {
		Sequence result = new XQuery("fn:reverse((1, 2, 3, 4, 5))")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(5), new Int32(4),
				new Int32(3), new Int32(2), new Int32(1)), result);
	}

	@Test
	public void fnReverseEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:reverse(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnRemoveStart() throws Exception {
		Sequence result = new XQuery("fn:remove((1, 2, 3, 4, 5), 1)")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnRemoveMiddle() throws Exception {
		Sequence result = new XQuery("fn:remove((1, 2, 3, 4, 5), 2)")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(3),
				new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnRemoveEnd() throws Exception {
		Sequence result = new XQuery("fn:remove((1, 2, 3, 4, 5), 5)")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4)), result);
	}

	@Test
	public void fnRemoveIllegalIndex() throws Exception {
		Sequence result = new XQuery("fn:remove((1, 2, 3, 4, 5), 7)")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnRemoveEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:remove((), 1)").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnInsertBeforeStart() throws Exception {
		Sequence result = new XQuery(
				"fn:insert-before((1, 2, 3, 4, 5), 1, (7))").execute(ctx);
		ResultChecker
				.dCheck(new ItemSequence(new Int32(7), new Int32(1), new Int32(
						2), new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnInsertBeforeMiddle() throws Exception {
		Sequence result = new XQuery(
				"fn:insert-before((1, 2, 3, 4, 5), 3, (7))").execute(ctx);
		ResultChecker
				.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(
						7), new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnInsertBeforeEnd() throws Exception {
		Sequence result = new XQuery(
				"fn:insert-before((1, 2, 3, 4, 5), 6, (7))").execute(ctx);
		ResultChecker
				.dCheck(new ItemSequence(new Int32(1), new Int32(2), new Int32(
						3), new Int32(4), new Int32(5), new Int32(7)), result);
	}

	@Test
	public void fnInsertBeforeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:insert-before((), 6, (7))")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(7)), result);
	}

	@Test
	public void fnInsertBeforeEmpIns() throws Exception {
		Sequence result = new XQuery("fn:insert-before((1, 2, 3, 4, 5), 6, ())")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void fnInsertBeforeEmpBoth() throws Exception {
		Sequence result = new XQuery("fn:insert-before((), 6, ())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnIndexOfExistant() throws Exception {
		Sequence result = new XQuery("fn:index-of((1, 2, 3, 4, 5), 5)")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(5)), result);
	}

	@Test
	public void fnIndexOfUnExistant() throws Exception {
		Sequence result = new XQuery("fn:index-of((1, 2, 3, 4, 5), 6)")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnIndexOfEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:index-of((), 5)").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnStringToCodepoints() throws Exception {
		Sequence result = new XQuery("fn:string-to-codepoints('Thérèse')")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(84), new Int32(104),
				new Int32(233), new Int32(114), new Int32(232), new Int32(115),
				new Int32(101)), result);
	}

	@Test
	public void fnStringToCodepointsEmpStr() throws Exception {
		Sequence result = new XQuery("fn:string-to-codepoints('')")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnStringToCodepointsEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:string-to-codepoints(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnCodepointsToString() throws Exception {
		Sequence result = new XQuery(
				"fn:codepoints-to-string((84, 104, 233, 114, 232, 115, 101))")
				.execute(ctx);
		ResultChecker.dCheck(new Str("Thérèse"), result);
	}

	@Test
	public void fnCodepointsToStringEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:codepoints-to-string(())")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnCodepointsToStrIllegalCodepoint() throws Exception {
		try {
			new XQuery("codepoints-to-string((1))").execute(ctx);
			fail("Accepted invalid codepoint");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_CODE_POINT_NOT_VALID, e.getCode());
		}
	}

	@Test
	public void fnSubstringBefore() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-before('Thérèse est jolie.', ' est')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("Thérèse"), result);
	}

	@Test
	public void fnSubstringBeforeEmpPattern() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-before('Thérèse est jolie.', '')").execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnSubstringBeforeEmpTarget() throws Exception {
		Sequence result = new XQuery("fn:substring-before('', ' est')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnSubstringBeforeNotContained() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-before('Thérèse est jolie.', 'baguette')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnSubstringAfter() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-after('Thérèse est jolie.', 'est ')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("jolie."), result);
	}

	@Test
	public void fnSubstringAfterEmpPattern() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-after('Thérèse est jolie.', '')").execute(ctx);
		ResultChecker.dCheck(new Str("Thérèse est jolie."), result);
	}

	@Test
	public void fnSubstringAfterEmpTarget() throws Exception {
		Sequence result = new XQuery("fn:substring-after('', ' est')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnSubstringAfterNotContained() throws Exception {
		Sequence result = new XQuery(
				"fn:substring-after('Thérèse est jolie.', 'baguette')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnEncodeForUri() throws Exception {
		Sequence result = new XQuery("fn:encode-for-uri('test')").execute(ctx);
		ResultChecker.dCheck(new Str("test"), result);
	}

	@Test
	public void fnEncodeForUriIllegalUri() throws Exception {
		Sequence result = new XQuery("fn:encode-for-uri('http://test@hallo')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("http%3A%2F%2Ftest%40hallo"), result);
	}

	@Test
	public void fnEncodeForUriIllegalUriIsIri() throws Exception {
		Sequence result = new XQuery("fn:encode-for-uri('bébé')").execute(ctx);
		ResultChecker.dCheck(new Str("b%C3%A9b%C3%A9"), result);
	}

	@Test
	public void fnEncodeForUriIllegalUriIllegalASCII() throws Exception {
		Sequence result = new XQuery("fn:encode-for-uri('test\u0001\u0000')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("test%01%00"), result);
	}

	@Test
	public void fnEncodeForUriIllegalUriEndsOnSurrogatePair() throws Exception {
		Sequence result = new XQuery("fn:encode-for-uri('test\uD800\uDC00')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("test%F0%90%80%80"), result);
	}

	@Test
	public void fnIriToUri() throws Exception {
		Sequence result = new XQuery("fn:iri-to-uri('http://bébé.fr')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("http://b%C3%A9b%C3%A9.fr"), result);
	}

	@Test
	public void fnIriToUriIllegalIri() throws Exception {
		Sequence result = new XQuery(
				"fn:iri-to-uri('http://bébé.fr/\uD800\uDC00')").execute(ctx);
		ResultChecker.dCheck(new Str("http://b%C3%A9b%C3%A9.fr/%F0%90%80%80"),
				result);
	}

	@Test
	public void fnMatchesSimple1() throws Exception {
		Sequence result = new XQuery("fn:matches('abracadabra', 'bra')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesSimple2() throws Exception {
		Sequence result = new XQuery("fn:matches('abracadabra', '^a.*a$')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesSimple3() throws Exception {
		Sequence result = new XQuery("fn:matches('abracadabra', '^bra')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void fnMatchesSimple4() throws Exception {
		Sequence result = new XQuery("fn:matches('abra\ncadabra', 'bra')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesSimpleEmptySeqInput() throws Exception {
		Sequence result = new XQuery("fn:matches((), '.?')").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesSimpleEmptyInput() throws Exception {
		Sequence result = new XQuery("fn:matches('', '.?')").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesEmptyFlags() throws Exception {
		Sequence result = new XQuery("fn:matches('abra\ncadabra', 'bra', '')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesIrrelaventFlags() throws Exception {
		Sequence result = new XQuery("fn:matches('abra\ncadabra', 'bra', 'm')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesNoMatchSingleLine() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'Kaum.*krähen')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void fnMatchesMatchLineSpanning() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'Kaum.*krähen', 's')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesNoMatchWholeInput() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', '^Kaum.*gesehen,$')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void fnMatchesMatchWholeLine() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', '^Kaum.*gesehen,$', 'm')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesIgnoreCase() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'kaum', 'i')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesIgnoreWhitespace() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('Kaum hat dies der Hahn gesehen,\nFängt er auch schon an zu krähen:', 'K aum', 'x')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesKeepWhitespaceInCharClass() throws Exception {
		Sequence result = new XQuery(
				"fn:matches('bra bra', '(br a)[ ]\\1', 'x')").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesIllegalFlag() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:matches('abracadabra', 'bra', 'ü')").execute(ctx);
			fail("Function accepted illegal flag.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION_FLAGS,
					e.getCode());
		}
	}

	@Test
	public void fnMatchesPureGroup() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:matches('abracadabra', '(?bra).*')").execute(ctx);
			fail("Pure groups disallowed.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesTooManyOpenBrackets() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('abracadabra', '[bra')")
					.execute(ctx);
			fail("Too many opening brackets.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesTooManyCloseBrackets() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('abracadabra', 'bra]')")
					.execute(ctx);
			fail("Too many closing brackets.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesTooManyOpenParens() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('abracadabra', '(bra')")
					.execute(ctx);
			fail("Too many opening parentheses.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesTooManyCloseParens() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('abracadabra', 'bra)')")
					.execute(ctx);
			fail("Too many closing parentheses.");
		} catch (QueryException e) {
		}
	}

	@Test
	public void fnMatchesLegalBackRef() throws Exception {
		Sequence result = new XQuery("fn:matches('brabra', '(bra)\\1')")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void fnMatchesIllegalBackRef() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('brabra', '(bra)\\2')")
					.execute(ctx);
			fail("Illegal back reference.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesTrailingBackslash() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('brabra', '(bra)\\')")
					.execute(ctx);
			fail("Trailing back slash.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesBackRefToGroup0() throws Exception {
		try {
			Sequence result = new XQuery("fn:matches('brabra', '(bra)\\0')")
					.execute(ctx);
			fail("Back ref to group 0.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnMatchesIllegalBackRefInCharClass() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:matches('brabra', '(bra)[a-r\\1]*')").execute(ctx);
			fail("Illegal back reference in character class.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION, e.getCode());
		}
	}

	@Test
	public void fnReplaceSimple1() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'bra', '*')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("a*cada*"), result);
	}

	@Test
	public void fnReplaceSimple2() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'a.*a', '*')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("*"), result);
	}

	@Test
	public void fnReplaceSimple3() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'a.*?a', '*')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("*c*bra"), result);
	}

	@Test
	public void fnReplaceSimple4() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'a', '')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("brcdbr"), result);
	}

	@Test
	public void fnReplaceSimple5() throws Exception {
		Sequence result = new XQuery(
				"fn:replace('abracadabra', 'a(.)', 'a$1$1')").execute(ctx);
		ResultChecker.dCheck(new Str("abbraccaddabbra"), result);
	}

	@Test
	public void fnReplaceEmptySeqInput() throws Exception {
		Sequence result = new XQuery("fn:replace((), 'a(.)', 'a$1$1')")
				.execute(ctx);
		ResultChecker.dCheck(Str.EMPTY, result);
	}

	@Test
	public void fnReplaceEmptyPattern() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:replace('abracadabra', '.*?', '$1')").execute(ctx);
			fail("Pattern matches empty string.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING,
					e.getCode());
		}
	}

	@Test
	public void fnReplaceSingleBackslash() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:replace('abracadabra', 'bra', '\\')").execute(ctx);
			fail("Replacement string consists of single backslash.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
		}
	}

	@Test
	public void fnReplaceSingleDollarSign() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:replace('abracadabra', 'bra', '$')").execute(ctx);
			fail("Replacement string consists of single dollar sign.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
		}
	}

	@Test
	public void fnReplaceIllegallyUsedBackslash() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:replace('abracadabra', 'bra', 'x\\x')").execute(ctx);
			fail("Replacement string makes illegal use of backslash.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
		}
	}

	@Test
	public void fnReplaceIllegallyUsedDollarSign() throws Exception {
		try {
			Sequence result = new XQuery(
					"fn:replace('abracadabra', 'bra', 'x$x')").execute(ctx);
			fail("Replacement string makes illegal use of dollar sign.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_INVALID_REPLACEMENT_STRING, e.getCode());
		}
	}

	@Test
	public void fnReplaceEscapedDollarSign() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'bra', '\\$')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("a$cada$"), result);
	}

	@Test
	public void fnReplaceEscapedBackSlash() throws Exception {
		Sequence result = new XQuery("fn:replace('abracadabra', 'bra', '\\\\')")
				.execute(ctx);
		ResultChecker.dCheck(new Str("a\\cada\\"), result);
	}

	@Test
	public void fnReplaceAlternative() throws Exception {
		Sequence result = new XQuery(
				"fn:replace('abcd', '(ab)|(a)', '[1=$1][2=$2]')").execute(ctx);
		ResultChecker.dCheck(new Str("[1=ab][2=]cd"), result);
	}

	@Test
	public void fnTokenizeSimple1() throws Exception {
		Sequence result = new XQuery("fn:tokenize('The cat sat', '\\s+')")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Str("The"), new Str("cat"),
				new Str("sat")), result);
	}

	@Test
	public void fnTokenizeSimple2() throws Exception {
		Sequence result = new XQuery("fn:tokenize('1,15,,24,50,', ',')")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Str("1"), new Str("15"),
				Str.EMPTY, new Str("24"), new Str("50"), Str.EMPTY), result);
	}

	@Test
	public void fnTokenizeEmptySeqInput() throws Exception {
		Sequence result = new XQuery("fn:tokenize((), ',')").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTokenizeEmptyInput() throws Exception {
		Sequence result = new XQuery("fn:tokenize('', ',')").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTokenizeEmptyPattern() throws Exception {
		try {
			Sequence result = new XQuery("fn:tokenize('abracadabra', '.?')")
					.execute(ctx);
			fail("Pattern matches empty string.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING,
					e.getCode());
		}
	}

	@Test
	public void fnYearsFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:years-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(21), result);
	}

	@Test
	public void fnYearsFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:years-from-duration(xs:yearMonthDuration('-P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(-21), result);
	}

	@Test
	public void fnYearsFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:years-from-duration(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnYearsFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:years-from-duration(xs:dayTimeDuration('P7DT25H'))")
				.execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnMonthsFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:months-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(3), result);
	}

	@Test
	public void fnMonthsFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:months-from-duration(xs:yearMonthDuration('-P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(-3), result);
	}

	@Test
	public void fnMonthsFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:months-from-duration(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMonthsFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:months-from-duration(xs:dayTimeDuration('P7DT25H'))")
				.execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnDaysFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:days-from-duration(xs:dayTimeDuration('P7DT25H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(8), result);
	}

	@Test
	public void fnDaysFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:days-from-duration(xs:dayTimeDuration('-P7DT25H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(-8), result);
	}

	@Test
	public void fnDaysFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:days-from-duration(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnDaysFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:days-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnHoursFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:hours-from-duration(xs:dayTimeDuration('P7DT25H61M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(2), result);
	}

	@Test
	public void fnHoursFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:hours-from-duration(xs:dayTimeDuration('-P7DT25H61M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(-2), result);
	}

	@Test
	public void fnHoursFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:hours-from-duration(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnHoursFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:hours-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnMinutesFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:minutes-from-duration(xs:dayTimeDuration('PT1M61S'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(2), result);
	}

	@Test
	public void fnMinutesFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:minutes-from-duration(xs:dayTimeDuration('-PT1M61S'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(-2), result);
	}

	@Test
	public void fnMinutesFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:minutes-from-duration(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMinutesFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:minutes-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(Int32.ZERO, result);
	}

	@Test
	public void fnSecondsFromDuration() throws Exception {
		Sequence result = new XQuery(
				"fn:seconds-from-duration(xs:dayTimeDuration('PT1M61S'))")
				.execute(ctx);
		ResultChecker.dCheck(new Dbl(1.0), result);
	}

	@Test
	public void fnSecondsFromDurationNegative() throws Exception {
		Sequence result = new XQuery(
				"fn:seconds-from-duration(xs:dayTimeDuration('-PT1M61S'))")
				.execute(ctx);
		ResultChecker.dCheck(new Dbl(-1.0), result);
	}

	@Test
	public void fnSecondsFromDurationEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:seconds-from-duration(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnSecondsFromDurationOtherDur() throws Exception {
		Sequence result = new XQuery(
				"fn:seconds-from-duration(xs:yearMonthDuration('P20Y15M'))")
				.execute(ctx);
		ResultChecker.dCheck(new Dbl(0.0), result);
	}

	@Test
	public void fnYearFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:year-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(1999), result);
	}

	@Test
	public void fnYearFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:year-from-dateTime(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMonthFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:month-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(5), result);
	}

	@Test
	public void fnMonthFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:month-from-dateTime(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnDayFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:day-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(31), result);
	}

	@Test
	public void fnDayFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:day-from-dateTime(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnHoursFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:hours-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(13), result);
	}

	@Test
	public void fnHoursFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:hours-from-dateTime(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMinutesFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:minutes-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(20), result);
	}

	@Test
	public void fnMinutesFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:minutes-from-dateTime(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnSecondsFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:seconds-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Dbl(0.0), result);
	}

	@Test
	public void fnSecondsFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:seconds-from-dateTime(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromDateTime() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-dateTime(xs:dateTime('1999-05-31T13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new DTD("-PT5H"), result);
	}

	@Test
	public void fnTimezoneFromDateTimeWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-dateTime(xs:dateTime('1999-05-31T13:20:00'))")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromDateTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:timezone-from-dateTime(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnYearFromDate() throws Exception {
		Sequence result = new XQuery(
				"fn:year-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Int32(1999), result);
	}

	@Test
	public void fnYearFromDateEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:year-from-date(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMonthFromDate() throws Exception {
		Sequence result = new XQuery(
				"fn:month-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Int32(5), result);
	}

	@Test
	public void fnMonthFromDateEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:month-from-date(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnDayFromDate() throws Exception {
		Sequence result = new XQuery(
				"fn:day-from-date(xs:date('1999-05-31-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Int32(31), result);
	}

	@Test
	public void fnDayFromDateEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:day-from-date(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromDate() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-date(xs:date('1999-05-31-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new DTD("-PT5H"), result);
	}

	@Test
	public void fnTimezoneFromDateWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-date(xs:date('1999-05-31'))").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromDateEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:timezone-from-date(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnHoursFromTime() throws Exception {
		Sequence result = new XQuery(
				"fn:hours-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Int32(13), result);
	}

	@Test
	public void fnHoursFromTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:hours-from-time(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnMinutesFromTime() throws Exception {
		Sequence result = new XQuery(
				"fn:minutes-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Int32(20), result);
	}

	@Test
	public void fnMinutesFromTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:minutes-from-time(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnSecondsFromTime() throws Exception {
		Sequence result = new XQuery(
				"fn:seconds-from-time(xs:time('13:20:00-05:00'))").execute(ctx);
		ResultChecker.dCheck(new Dbl(0.0), result);
	}

	@Test
	public void fnSecondsFromTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:seconds-from-time(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromTime() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-time(xs:time('13:20:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new DTD("-PT5H"), result);
	}

	@Test
	public void fnTimezoneFromTimeWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:timezone-from-time(xs:time('13:20:00'))").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnTimezoneFromTimeEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:timezone-from-time(())").execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnAdjustDateTimeToTimezoneEmptyTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), ())")
				.execute(ctx);
		ResultChecker.dCheck(new DateTime("2002-03-07T10:00:00"), result);
	}

	@Test
	public void fnAdjustDateTimeToTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), xs:dayTimeDuration('PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new DateTime("2002-03-08T01:00:00+10:00"), result);
	}

	@Test
	public void fnAdjustDateTimeToTimezoneWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00'), xs:dayTimeDuration('PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new DateTime("2002-03-07T10:00:00+10:00"), result);
	}

	@Test
	public void fnAdjustDateTimeToTimezoneImplicitTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new DateTime("2002-03-07T16:00:00+01:00"), result);
	}

	@Test
	public void fnAdjustDateTimeToTimezoneEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:adjust-dateTime-to-timezone(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnAdjustDateTimeToTimezoneIllegalTimezone() throws Exception {
		try {
			new XQuery(
					"fn:adjust-dateTime-to-timezone(xs:dateTime('2002-03-07T10:00:00-05:00'), xs:dayTimeDuration('PT15H'))")
					.execute(ctx);
			fail("Accepted illegal timezone.");
		} catch (QueryException e) {
			assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE,
					e.getCode());
		}
	}

	@Test
	public void fnAdjustDateToTimezoneEmptyTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), ())")
				.execute(ctx);
		ResultChecker.dCheck(new Date("2002-03-07"), result);
	}

	@Test
	public void fnAdjustDateToTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), xs:dayTimeDuration('-PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Date("2002-03-06-10:00"), result);
	}

	@Test
	public void fnAdjustDateToTimezoneWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-date-to-timezone(xs:date('2002-03-07'), xs:dayTimeDuration('-PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Date("2002-03-07-10:00"), result);
	}

	@Test
	public void fnAdjustDateToTimezoneImplicitTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Date("2002-03-07+01:00"), result);
	}

	@Test
	public void fnAdjustDateToTimezoneEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:adjust-date-to-timezone(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnAdjustDateToTimezoneIllegalTimezone() throws Exception {
		try {
			new XQuery(
					"fn:adjust-date-to-timezone(xs:date('2002-03-07-05:00'), xs:dayTimeDuration('PT15H'))")
					.execute(ctx);
			fail("Accepted illegal timezone.");
		} catch (QueryException e) {
			assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE,
					e.getCode());
		}
	}

	@Test
	public void fnAdjustTimeToTimezoneEmptyTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), ())")
				.execute(ctx);
		ResultChecker.dCheck(new Time("10:00:00"), result);
	}

	@Test
	public void fnAdjustTimeToTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), xs:dayTimeDuration('PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Time("01:00:00+10:00"), result);
	}

	@Test
	public void fnAdjustTimeToTimezoneWithoutTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-time-to-timezone(xs:time('10:00:00'), xs:dayTimeDuration('PT10H'))")
				.execute(ctx);
		ResultChecker.dCheck(new Time("10:00:00+10:00"), result);
	}

	@Test
	public void fnAdjustTimeToTimezoneImplicitTimezone() throws Exception {
		Sequence result = new XQuery(
				"fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'))")
				.execute(ctx);
		ResultChecker.dCheck(new Time("16:00:00+01:00"), result);
	}

	@Test
	public void fnAdjustTimeToTimezoneEmpSeq() throws Exception {
		Sequence result = new XQuery("fn:adjust-time-to-timezone(())")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void fnAdjustTimeToTimezoneIllegalTimezone() throws Exception {
		try {
			new XQuery(
					"fn:adjust-time-to-timezone(xs:time('10:00:00-05:00'), xs:dayTimeDuration('PT15H'))")
					.execute(ctx);
			fail("Accepted illegal timezone.");
		} catch (QueryException e) {
			assertEquals("Wrong error code", ErrorCode.ERR_INVALID_TIMEZONE,
					e.getCode());
		}
	}

	@Test
	public void fnMinDouble() throws Exception {
		Sequence result = new XQuery(
				"fn:min((198.95E0,282.69E0,188.72E0 ,268.38E0))").execute(ctx);
		ResultChecker.check(new Dbl(188.72), result);
	}

	@Test
	public void fnMaxDouble() throws Exception {
		Sequence result = new XQuery(
				"fn:max((198.95E0,282.69E0,188.72E0 ,268.38E0))").execute(ctx);
		ResultChecker.check(new Dbl(282.69), result);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		storeDocument("test.xml", "<a><b>text1<b>text2</b></b><c>text2</c></a>");
	}
}
