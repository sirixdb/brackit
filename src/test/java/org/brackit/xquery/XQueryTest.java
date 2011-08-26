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
package org.brackit.xquery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class XQueryTest extends XQueryBaseTest {

	@Test
	public void addOperation() throws Exception {
		Sequence result = new XQuery("1 + 2").execute(ctx);
		ResultChecker.dCheck(new Int32(3), result);
	}
	
	@Test
	public void multOperation() throws Exception {
		Sequence result = new XQuery("1 * 2").execute(ctx);
		ResultChecker.dCheck(new Int32(2), result);
	}
	
	@Test
	public void doubleArithmeticWithNegativeZero() throws Exception {
		Sequence result = new XQuery("1 + xs:double('.21E0')").execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(-0), result);
	}

	@Test
	public void composedArithmeticOperationWithMultPrecedence1()
			throws Exception {
		Sequence result = new XQuery("1 + 2 * 3").execute(ctx);
		ResultChecker.dCheck(new Int32(7), result);
	}

	@Test
	public void composedArithmeticOperationWithMultPrecedence2()
			throws Exception {
		Sequence result = new XQuery("1 * 2 + 3").execute(ctx);
		ResultChecker.dCheck(new Int32(5), result);
	}

	@Test
	public void composedArithmeticOperationWithParenthesizedPrecedence()
			throws Exception {
		Sequence result = new XQuery("(1 + 2) * 3").execute(ctx);
		ResultChecker.dCheck(new Int32(9), result);
	}

	
	@Test
	public void generalComparison() throws Exception {
		Sequence result = new XQuery("1 > 2").execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void generalComparisonNodeAndAtomics() throws Exception {
		// the content must be converted to numeric double
		Sequence result = new XQuery("(<a>12</a> < 24) and (<b>122</b> > 24)")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}
	
	@Test
	public void valueComparison() throws Exception {
		Sequence result = new XQuery("1 lt 2").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void valueComparisonNodeAndAtomics() throws Exception {
		// the content must be converted to numeric double
		Sequence result = new XQuery("(<a>12</a> lt 24) and (<b>122</b> gt 24)")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void rangeExpr() throws Exception {
		Sequence result = new XQuery("(1 to 5)").execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5)), result);
	}

	@Test
	public void ifExpr() throws Exception {
		Sequence result = new XQuery("if (0) then 1 else 0").execute(ctx);
		ResultChecker.dCheck(new Int32(0), result);
	}

	@Test
	public void stringLiteralInQuotes() throws Exception {
		Sequence result = new XQuery("\"test\"").execute(ctx);
		ResultChecker.dCheck(new Str("test"), result);
	}

	@Test
	public void stringLiteralInApostrophes() throws Exception {
		Sequence result = new XQuery("'test'").execute(ctx);
		ResultChecker.dCheck(new Str("test"), result);
	}

	@Test
	public void intLiteral() throws Exception {
		Sequence result = new XQuery("23").execute(ctx);
		ResultChecker.dCheck(new Int32(23), result);
	}

	@Test
	public void stringLiteral() throws Exception {
		Sequence result = new XQuery("' a h a '").execute(ctx);
		ResultChecker.dCheck(new Str(" a h a "), result);
	}


	@Test
	public void castStringAsDouble() throws Exception {
		Sequence result = new XQuery("'-1.000E4' cast as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(-10000), result);
	}

	@Test
	public void doubleConstructorFunction() throws Exception {
		Sequence result = new XQuery("xs:double('-1.000E4')").execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(-10000), result);
	}

	@Test
	public void testBug() throws Exception {
		Sequence result = new XQuery("xs:float(xs:integer(3))").execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(-10000), result);
	}

	@Test
	public void castStringAsUnsignedByte() throws Exception {
		Sequence result = new XQuery("'     255    ' cast as xs:unsignedByte")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(255).asType(Type.UBYT), result);
	}

	@Test
	public void illegalCastStringAsUnsignedByte() throws Exception {
		try {
			Sequence result = new XQuery("'256' cast as xs:unsignedByte")
					.execute(ctx);
			fail("Illegal cast not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_INVALID_VALUE_FOR_CAST, e.getCode());
		}
	}

	@Test
	public void stringCastableAsDouble() throws Exception {
		Sequence result = new XQuery("'-1.000E4' castable as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void integerTreatAsDecimal() throws Exception {
		Sequence result = new XQuery("3 treat as xs:decimal").execute(ctx);
		ResultChecker.dCheck(new Int32(3), result);
	}

	@Test
	public void integerInstanceOfDecimal() throws Exception {
		Sequence result = new XQuery("3 instance of xs:decimal").execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void stringInstanceOfDecimal() throws Exception {
		Sequence result = new XQuery("'Foo' instance of xs:decimal")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void illegalStringTreatAsDouble() throws Exception {
		try {
			new XQuery("'foo' treat as xs:double").execute(ctx);
			fail("Illegal treat not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE, e
							.getCode());
		}
	}

	@Test
	public void stringNotCastableAsDouble() throws Exception {
		Sequence result = new XQuery("'Foo' castable as xs:double")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void castLegalEmptySequenceAsDouble() throws Exception {
		Sequence result = new XQuery("() cast as xs:double?").execute(ctx);
		print(result);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void castIllegalEmptySequenceAsDouble() throws Exception {
		try {
			new XQuery("() cast as xs:double").execute(ctx);
			fail("Illegal case not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
		}
	}



	@Test
	public void docExp() throws Exception {
		Sequence result = new XQuery("document {<a/>}").execute(ctx);
		print(result);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void unionA() throws Exception {
		Sequence result = new XQuery(
				"let $at := (document {<a><b>text1<b>text2</b></b><c>text2<b>text3</b></c><b>text4</b></a>})/a "
						+ "return $at//b | $at//c").execute(ctx);
		print(result);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void exceptA() throws Exception {
		Sequence result = new XQuery(
				"let $at := (document {<a><b>text1<b>text2</b></b><c>text2<b>text3</b></c><b>text4</b></a>})/a "
						+ "return $at//b except $at/b").execute(ctx);
		print(result);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void arithmeticTest1() throws Exception {
		Sequence result = new XQuery("xs:double(1) + 1").execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(2), result);
	}

	@Test
	public void arithmeticTest2() throws Exception {
		ctx.bind(new QNm("ext"), new Una("1"));
		Sequence result = new XQuery(
				"declare variable $ext external; let $ctx := (<a att=\"1\"/>) return xs:integer(fn:data($ctx/@att)) + $ext")
				.execute(ctx);
		print(result);
		ResultChecker.dCheck(new Dbl(2), result);
	}


	@Test
	public void typeswitchTest() throws QueryException {
		Sequence result = new XQuery(
				"let $d := xs:dayTimeDuration('PT2H') return typeswitch($d)"
						+ "case $a as xs:yearMonthDuration return fn:concat('YMD:', $a)"
						+ "case $v as xs:dayTimeDuration return fn:concat('DTD:', $v)"
						+ "default $d return fn:concat('DUR:', $d)")
				.execute(ctx);
		ResultChecker.dCheck(new Str("DTD:PT2H"), result);
	}

	@Test
	public void typeswitchTestDefault() throws QueryException {
		Sequence result = new XQuery(
				"let $d := xs:duration('PT2H') return typeswitch($d)"
						+ "case $a as xs:yearMonthDuration return fn:concat('YMD:', $a)"
						+ "case $v as xs:dayTimeDuration return fn:concat('DTD:', $v)"
						+ "default $d return fn:concat('DUR:', $d)")
				.execute(ctx);
		ResultChecker.dCheck(new Str("DUR:PT2H"), result);
	}

	@Test
	public void nullTest() throws Exception {
		ctx.bind(new QNm("intVar"), new Una("4.0"));
		new XQuery(
				"for $i in 1 to 3 return <res>{ attribute {'att'} {} }</res>")
				.serialize(ctx, System.out);
	}

	@Test
	public void uriTest() {
		try {
			URI uri = new java.net.URI("test.xml");
			System.out.println(uri.toASCIIString());
		} catch (URISyntaxException e) {
			//
		}
	}

	@Test
	public void regexTest1() {
		try {
			Pattern p = Pattern.compile(".*ab.*");
			Matcher m = p.matcher("xabc");
			System.out.println(m.matches());
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	@Test
	public void regexTest2() {
		try {
			Pattern p = Pattern.compile("ab(x*)");
			Matcher m = p.matcher("ababx");
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(sb, "$ncd");
			}
			m.appendTail(sb);

			System.out.println(sb.toString());
		} catch (Exception e) {
			System.err.println(e);
		}
	}



	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		storeDocument("test.xml", "<a><b>text1<b>text2</b></b><c>text2</c></a>");
	}
}
