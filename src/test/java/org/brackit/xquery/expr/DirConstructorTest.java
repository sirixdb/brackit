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
package org.brackit.xquery.expr;

import java.io.PrintStream;

import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
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
		Assert.assertEquals("serialized result differs", "<shoe size=\"7\"/>", buf.toString());
	}
	
	@Test
	public void dirAttributeContent2() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"{7}\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"7\"/>", buf.toString());
	}
	
	@Test
	public void dirAttributeContent3() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"{()}\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"\"/>", buf.toString());
	}
	
	@Test
	public void dirAttributeContent4() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<shoe size=\"[{1, 5 to 7, 9}]\"/>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<shoe size=\"[1 5 6 7 9]\"/>", buf.toString());
	}
	
	@Test
	public void dirElementContent1() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1</a>", buf.toString());
	}
	
	@Test
	public void dirElementContent2() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1, 2, 3}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1 2 3</a>", buf.toString());
	}
	
	@Test
	public void dirElementContent3() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1}{2}{3}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>123</a>", buf.toString());
	}
	
	@Test
	public void dirElementContent4() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<a>{1, \"2\", \"3\"}</a>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<a>1 2 3</a>", buf.toString());
	}
	
	@Test
	public void dirElementContent5() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<fact>I saw {5 + 3} cats.</fact>").serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<fact>I saw 8 cats.</fact>", buf.toString());
	}
	
	@Test
	public void dirElementContent6a() throws Exception {
		PrintStream buf = createBuffer();
		new XQuery("<fact>I saw <howmany>{5 + 3}</howmany> cats.</fact>")
				.serialize(ctx, buf);
		Assert.assertEquals("serialized result differs", "<fact>I saw <howmany>8</howmany> cats.</fact>", buf.toString());
	}

	@Test
	public void dirElementContent6b() throws Exception {
		Sequence res = new XQuery("<fact>I saw <howmany>{5 + 3}</howmany> cats.</fact>").execute(ctx);
		Node<?> doc = ctx.getNodeFactory().build(new DocumentParser("<fact>I saw <howmany>8</howmany> cats.</fact>"));
		Node<?> fact = doc.getFirstChild();
		ResultChecker.dCheck(ctx, fact, res, false);
	}
}
