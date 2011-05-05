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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.linked.TextLNode;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Before;
import org.junit.Test;

public class XQueryKnownBugsTest extends XQueryBaseTest {
	/*
	 * only the first item in a build element content sequence was appended to
	 * the element
	 */
	@Test
	public void incompleteElementContentSequence() throws Exception {
		Sequence res = new XQuery("data((<x>{2, 1+2 , (())}</x>)//text())")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("2 3")), res);
	}

	@Test
	public void delcareNamespace() throws Exception {
		Sequence res = new XQuery(
				"declare namespace ns1 = 'http://www.example.org/ns1';"
						+ "declare namespace ns2 = 'http://www.example.org/ns2';"
						+ "let $element as element(ns1:foo) := <ns1:foo/>"
						+ "return count($element/self::ns2:*)").execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("2 3")), res);
	}

	@Test
	public void unaryFunction() throws Exception {
		Sequence res = new XQuery("for $ a in (1,2,3) return -$a").execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(-1),
				new Int32(-2), new Int32(-3)), res);
	}

	@Test
	public void parseError() throws Exception {
		Sequence res = new XQuery("<a>1&amp;<b/>12 {'aha'}soso<c/></a>")
				.execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("2 3")), res);
	}

	@Test
	public void parseError2() throws Exception {
		Sequence res = new XQuery("for $a in (1,2,3) return text(*)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("2 3")), res);
	}

	@Test
	public void parseError3() throws Exception {
		Sequence res = new XQuery("\"a string &;\"").execute(ctx);
		print(res);
	}

	@Test
	public void parseError5() throws Exception {
		Sequence res = new XQuery(
				"string(<elem>{'a'} a {1,2,3} b <![CDATA[ b ]]> c {'a', 'b'}</elem>)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Str(" a  b  c ")), res);
	}

	@Test
	public void parseError6() throws Exception {
		Sequence res = new XQuery(
				"string(<elem a='5' b='{1+1}'> { 'a ' }{' b', 'd' }{' c' } </elem>)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Str(" a   b d  c ")),
				res);
	}

	@Test
	public void parseError4() throws Exception {
		Sequence res = new XQuery(
				"declare variable $input-context external;\n\n-.0.1")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("2 3")), res);
	}

	@Test
	public void parseError13() throws Exception {
		Sequence res = new XQuery("processing-instruction XmL {'pi'}")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Str("12"), res);
	}

	@Test
	public void parseError8() throws Exception, IOException {
		Sequence res = new XQuery(readQuery("./", "query.xq")).execute(ctx);
		print(res);
	}

	@Test
	public void parseError9() throws Exception {
		Sequence res = new XQuery(
				"<part partid='{'a'}{1+1}' name='{'soso' }' > { 1 + 1}</part>")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new Str("12"), res);
	}

	@Test
	public void parseError10() throws Exception {
		Sequence res = new XQuery("<elem attr=\"&amp;&lt;&gt;\"   />")
				.execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, new Str("12"), res);
	}

	@Test
	public void sortOrder() throws Exception {
		Sequence res = new XQuery(
				"for $a in (<b>10</b>, <a>8b</a>, 9E0, 0) order by $a return $a")
				.execute(ctx);
		print(res);
		ResultChecker.dCheck(ctx, new Str("12"), res);
	}

	@Test
	public void steps() throws Exception {
		Sequence res = new XQuery(
				"for $i in (<a><b/></a>, <a><b/></a>, <a><b/></a>, <a><b/></a>) return $i/b[position() < 10]/fn:root()[.]/b/fn:true()")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(Bool.TRUE, Bool.TRUE,
				Bool.TRUE, Bool.TRUE), res);
	}

	@Test
	public void pathExpressionWithFunctionStep() throws Exception {
		Sequence res = new XQuery(
				"for $i in <res att=\"hello\" att2=\"world\"/> return $i/@*/data(.)")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new Una("hello"), new Una(
				"world")), res);
	}

	@Test
	public void testSerialization() throws Exception {
		Sequence res = new XQuery(
				"(<p><a><b>a1b1</b><b>a1b2</b></a><a><b>a2b1</b><b>a2b2</b></a></p>)//a//b/text()")
				.execute(ctx);
		ResultChecker.dCheck(ctx, new ItemSequence(new TextLNode("a1b1"),
				new TextLNode("a1b2"), new TextLNode("a2b1"), new TextLNode(
						"a2b2")), res);
	}

	// (<p><a><b>a1b1</b><b>a1b2</b></a><a><b>a2b1</b><b>a2b2</b></a></p>)//a//b/text()
	// -> a1b1a1b2a2b1a2b2
	// (<p><a><b>a1b1</b><b>a1b2</b></a><a><b>a2b1</b><b>a2b2</b></a></p>)//a//b/data(.)
	// -> a1b1 a1b2 a2b1 a2b2

	@Test
	public void current() throws Exception {
		Sequence res = new XQuery("nametest : nametest").execute(ctx);
		print(res);
	}

	@Test
	public void intDowncast() throws Exception {
		Sequence res = new XQuery("declare boundary-space strip;" + "<cat>\n"
				+ "    <breed>2</breed>\n" + "    <color>4</color>2 	\n"
				+ "</cat>").execute(ctx);
		print(res);
	}

	// \"http://anyURI\"
	@Test
	public void declarations() throws Exception {
		Sequence res = new XQuery(
				"xs:gYear(\"1970+01:00\") eq xs:gYear(\"1970Z\")").execute(ctx);
		print(res);
	}

	@Test
	public void boundaryWS1() throws Exception {
		Sequence res = new XQuery("<a>    <b/> </a>").execute(ctx);
		print(res);
	}

	@Test
	public void boundaryWS2() throws Exception {
		Sequence res = new XQuery("<a>  2  <b/> </a>").execute(ctx);
		print(res);
	}

	@Test
	public void leadingLoneSlash() throws Exception {
		Sequence res = new XQuery(
				"let $x:='bound' return <a>  1 = 2{$x} end </a>").execute(ctx);
		print(res);
	}

	@Test
	public void distinctTest() throws Exception {
		new XQuery(
				"fn:distinct-values((1, 2.0, 3, 'a', 'b', 'a', fn:true(), fn:false(), 1 le 2))")
				.serialize(ctx, System.out);
		// new
		// XQuery("fn:doc('tpcc.xml')/tpcc/Warehouses/Warehouse/Districts/District/Customers/Customer[@c_w_id = 1 and @c_d_id = 1 and @c_id = 416]/@c_balance/fn:data(.)").serialize(ctx,
		// System.out);
	}

	@Test
	public void distinctTest3() throws Exception {
		new XQuery(
				"for $v in fn:distinct-values((<doc><a att1='16'/><a att1='12'/></doc>)//a/@att1) return if ($v instance of xs:untypedAtomic) then 'y' else 'n'")
				.serialize(ctx, System.out);
		// new
		// XQuery("fn:doc('tpcc.xml')/tpcc/Warehouses/Warehouse/Districts/District/Customers/Customer[@c_w_id = 1 and @c_d_id = 1 and @c_id = 416]/@c_balance/fn:data(.)").serialize(ctx,
		// System.out);
	}

	@Test
	public void distinctTest2() throws Exception {
		storeDocument("test2.xml", "<doc><a att1='16'/><a att1='12'/></doc>");
		new XQuery("fn:distinct-values(fn:doc('test2.xml')/doc/a/@att1)")
				.serialize(ctx, System.out);
		// new
		// XQuery("fn:doc('tpcc.xml')/tpcc/Warehouses/Warehouse/Districts/District/Customers/Customer[@c_w_id = 1 and @c_d_id = 1 and @c_id = 416]/@c_balance/fn:data(.)").serialize(ctx,
		// System.out);
	}

	@Test
	public void fromFile() throws Exception, IOException {
		// storeDocument("tpcc.xml", new File("/docs/tpcc.xml"));
		new XQuery(readQuery("./", "debug.xq")).serialize(ctx, System.out);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
	}
}
