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

import java.io.FileNotFoundException;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 * 
 */
public class UpdateFacilityTest extends XQueryBaseTest {

	private static final String DOCUMENT = "<a><b>text1<b>text2</b></b><c>text2</c></a>";

	Node<?> doc;
	Node<?> orig;

	@Test
	public void insertInto() throws Exception {
		ctx.setContextItem(doc);
		orig.getFirstChild().append(Kind.ELEMENT, new QNm("test"));
		new XQuery("insert node <test/> into ./a").execute(ctx);
		ResultChecker.dCheck(orig, doc, false);
	}

	@Test
	public void simpleDelete() throws Exception {
		ctx.setContextItem(doc);
		new XQuery("delete node ./a/c").execute(ctx);
		orig.getFirstChild().getLastChild().delete();
		ResultChecker.dCheck(orig, doc, false);
	}

	@Test
	public void simpleReplaceNode() throws Exception {
		ctx.setContextItem(doc);
		new XQuery("replace node ./a/c with <d/>").execute(ctx);
		orig.getFirstChild().getLastChild().replaceWith(Kind.ELEMENT,
				new QNm("d"));
		ResultChecker.dCheck(orig, doc, false);
	}

	@Test
	public void simpleRename() throws Exception {
		ctx.setContextItem(doc);
		new XQuery("rename node ./a as 'b'").execute(ctx);
		orig.getFirstChild().setName(new QNm("b"));
		ResultChecker.dCheck(orig, doc, false);
	}

	@Test
	public void transformTestSimple() throws QueryException {
		Sequence res = new XQuery(
				"copy $n := <a att='1'><b/></a> modify delete node $n/@att return $n")
				.execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.append(Kind.ELEMENT, new QNm("b"));
		ResultChecker.dCheck(a, res, false);
	}

	@Test
	public void transformTestDeleteBoundCopy() throws QueryException {
		// Will not result in empty sequence as nodes without parents are
		// ignored by delete operation
		Sequence res = new XQuery(
				"copy $n := <a att='1'><b/></a> modify delete node $n return $n")
				.execute(ctx);
		Node<?> a = ctx.getNodeFactory().element(new QNm("a"));
		a.setAttribute(new QNm("att"), new Una("1"));
		a.append(Kind.ELEMENT, new QNm("b"));
		ResultChecker.dCheck(a, res, false);
	}

	@Test
	public void transformTestTwoCopyVars() throws QueryException {
		Sequence res = new XQuery(
				"let $f := <a att='1'><b/></a> return copy $m := $f, $n := $f, $o := $f modify delete node $n/b return ($m, $n)")
				.execute(ctx);
		Node<?> a1 = ctx.getNodeFactory().element(new QNm("a"));
		a1.setAttribute(new QNm("att"), new Una("1"));
		a1.append(Kind.ELEMENT, new QNm("b"));
		Node<?> a2 = ctx.getNodeFactory().element(new QNm("a"));
		a2.setAttribute(new QNm("att"), new Una("1"));
		ResultChecker.dCheck(new ItemSequence(a1, a2), res, false);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		doc = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
		orig = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
	}

}