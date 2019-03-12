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
package org.brackit.xquery.expr;

import java.io.PrintStream;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 *
 */
public class DirConstructorTest extends XQueryBaseTest {
	@Test
	public void dirAttributeContent1() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"7\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"7\"/>",
				buf.toString());
	}

	@Test
	public void dirAttributeContent2() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"{7}\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"7\"/>",
				buf.toString());
	}

	@Test
	public void dirAttributeContent3() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"{()}\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"\"/>",
				buf.toString());
	}

	@Test
	public void dirAttributeContent4() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"[{1, 5 to 7, 9}]\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs",
				"<shoe size=\"[1 5 6 7 9]\"/>", buf.toString());
	}

	@Test
	public void dirElementContent1() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1</a>", buf
				.toString());
	}

	@Test
	public void dirElementContent2() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1, 2, 3}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1 2 3</a>", buf
				.toString());
	}

	@Test
	public void dirElementContent3() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1}{2}{3}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>123</a>", buf
				.toString());
	}

	@Test
	public void dirElementContent4() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1, \"2\", \"3\"}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1 2 3</a>", buf
				.toString());
	}

	@Test
	public void dirElementContent5() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<fact>I saw {5 + 3} cats.</fact>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs",
				"<fact>I saw 8 cats.</fact>", buf.toString());
	}

	@Test
	public void dirElementContent6a() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<fact>I saw <howmany>{5 + 3}</howmany> cats.</fact>")
				.serialize(ctx, buf);
		Assert
				.assertEquals("serialized result differs",
						"<fact>I saw <howmany>8</howmany> cats.</fact>", buf
								.toString());
	}

	@Test
	public void dirElementContent6b() throws Exception {
		Sequence res = new XQuery(
				"<fact>I saw <howmany>{5 + 3}</howmany> cats.</fact>")
				.execute(ctx);
		DocumentParser parser = new DocumentParser(
				"<fact>I saw <howmany>8</howmany> cats.</fact>");
		parser.setRetainWhitespace(true);
		Node<?> doc = ctx.getNodeFactory().build(parser);
		Node<?> fact = doc.getFirstChild();
		ResultChecker.dCheck(fact, res, false);
	}

	@Test
	public void boundarySpaceStrip() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery(
				"declare boundary-space strip; <cat>\n    <breed>2</breed>\n    <color>4</color>2 	\n</cat>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs",
				"<cat><breed>2</breed><color>4</color>2 	\n</cat>", buf
						.toString());
	}

	@Test
	public void directElementExpr() throws Exception {
		Sequence result = new XQuery("<a/>").execute(ctx);
		ResultChecker.dCheck(ctx.getNodeFactory().element(new QNm("a")),
				result, false);
	}

	@Test
	public void directElementExprWithEmptyAttribute() throws Exception {
		Sequence result = new XQuery("<a b=''/>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("b"), new Una(""));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithAttribute() throws Exception {
		Sequence result = new XQuery("<a b='c'/>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("b"), new Una("c"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithAttributeAndComputedValue()
			throws Exception {
		Sequence result = new XQuery("<a b=\"{1 + 2}\"/> ").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("b"), new Una("3"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWith2Attributes() throws Exception {
		Sequence result = new XQuery("<a b='c' d='e'/>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("b"), new Una("c"));
		a.setAttribute(new QNm("d"), new Una("e"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWith2AttributesAndComputedValue()
			throws Exception {
		Sequence result = new XQuery("<a b='{1 + 2}'   c='{2 + 2}'/>")
				.execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("b"), new Una("3"));
		a.setAttribute(new QNm("c"), new Una("4"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithText() throws Exception {
		Sequence result = new XQuery("<a>test</a>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.append(Kind.TEXT, null, new Una("test"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithChildren() throws Exception {
		Sequence result = new XQuery("<a><b/><c/></a>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.append(Kind.ELEMENT, new QNm("b"), null);
		a.append(Kind.ELEMENT, new QNm("c"), null);
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithComputedContent() throws Exception {
		Sequence result = new XQuery("<a>{(1 to 3)}</a>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.append(Kind.TEXT, null, new Una("1 2 3"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void directElementExprWithComputedSequenceContent() throws Exception {
		Sequence result = new XQuery("<a>{1, 2, 3}</a>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.append(Kind.TEXT, null, new Una("1 2 3"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void nesteddirectElementExprInSequence() throws Exception {
		Sequence result = new XQuery("<a>{<b>{2 + 4}</b>, 1}</a>").execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		Node<?> b = a.append(Kind.ELEMENT, new QNm("b"), null);
		b.append(Kind.TEXT, null, new Una("6"));
		a.append(Kind.TEXT, null, new Una("1"));
		ResultChecker.dCheck(a, result, false);
	}

	@Test
	public void boundarySpace1() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("declare boundary-space strip; <a> {\"abc\"} </a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>abc</a>", buf
				.toString());
	}

	@Test
	public void boundarySpace2() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("declare boundary-space preserve; <a> {\"abc\"} </a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a> abc </a>", buf
				.toString());
	}

	@Test
	public void boundarySpace3() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("declare boundary-space preserve; <a> z {\"abc\"}</a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a> z abc</a>", buf
				.toString());
	}

	@Test
	public void boundarySpace4() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("declare boundary-space preserve; <a>&#x20;{\"abc\"}</a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a> abc</a>", buf
				.toString());
	}

	@Test
	public void boundarySpace5() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery(
				"declare boundary-space preserve; <a>  &#x20;  {\"abc\"}</a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>     abc</a>", buf
				.toString());
	}

	@Test
	public void boundarySpace6() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("declare boundary-space preserve; <a> &#8364; </a>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a> € </a>", buf
				.toString());
	}

	@Test
	public void elementWithNamespace() throws Exception {
		PrintStream buf = createBuffer();
		Sequence res = new XQuery(
				"<e xmlns:f=\"foo\" xmlns=\"bla\" f:att=\"\"><f/></e>")
				.execute(ctx);
	}

	// namespace-uri(<e xmlns:f="foo&lt;" xmlns="bla" f:att=""></e>/@*) eq
	// "foo&#x003c;"
}
