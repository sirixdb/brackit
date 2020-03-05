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
package org.brackit.xquery.util.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.brackit.xquery.atomic.QNm;
import org.junit.Test;

/**
 * 
 * @author Sebastian Baechle, Max Bechtold
 * 
 */
public class PathTest {
	@Test
	public void testSJsonPath() {
		// /paths/\/business_service_providers\/search/get.
		Path<QNm> expected = (new Path<QNm>()).child(new QNm("paths")).child(new QNm("\\/business_service_providers\\/search")).child(new QNm("get"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath() {
		Path<QNm> expected = (new Path<QNm>()).child(new QNm("tag"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath2() {
		Path<QNm> expected = (new Path<QNm>()).child(new QNm("tag"))
				.child(new QNm("hallo")).descendant(new QNm("aha"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath3() {
		Path<QNm> expected = (new Path<QNm>()).self().child(new QNm("tag"))
			.child(new QNm("hallo")).descendant(new QNm("aha"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath4() {
		Path<QNm> expected = (new Path<QNm>()).self().child(new QNm("tag"))
			.child(new QNm("hallo")).descendant(new QNm("aha"));
		String implicitSelfPath = expected.toString().substring(2);
		Path<QNm> parsed = (new PathParser(implicitSelfPath)).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSelfPath4() {
		Path<QNm> expected = (new Path<QNm>()).self().self().descendant(
				new QNm("aha"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testQualifiedPath() {
		Path<QNm> expected = (new Path<QNm>()).child(new QNm(null, "foo", "tag"));
		Path<QNm> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}
	
	@Test
	public void testQualifiedPathPreamble() {
		Path<QNm> expected = (new Path<QNm>()).child(
				new QNm("http://brackit.org/ns/bit", "bit", "tag"));
		String path = "namespace foo = 'localhost'; " +
				"namespace bit = 'http://brackit.org/ns/bit'; " + expected.toString();
		Path<QNm> parsed = (new PathParser(path)).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}
	
	@Test
	public void testQualifiedPathMalformedPreamble() {
		Path<QNm> expected = (new Path<QNm>()).child(
				new QNm("http://brackit.org/ns/bit", "bit", "tag"));
		String path = "namespace foo = 'localhost'" + expected.toString();
		try {
			Path<QNm> parsed = (new PathParser(path)).parse();
			assertTrue("Malformed preamble recognized", false);
		} catch (PathException e) {
		}
	}
	
	@Test
	public void testQualifiedPathUndefinedPrefix() {
		Path<QNm> expected = (new Path<QNm>()).child(
				new QNm("http://brackit.org/ns/bit", "xzibit", "tag"));
		String path = "namespace foo = 'localhost'; " + expected.toString();
		try {
			Path<QNm> parsed = (new PathParser(path)).parse();
			assertTrue("Missing namespace declaration recognized", false);
		} catch (PathException e) {
		}
	}
	
	@Test
	public void testFilePath() {
		Path<QNm> parsed = (new PathParser("/test.xml")).parse();
		assertEquals("Path parsed correctly", new Path<QNm>()
				.child(new QNm("test.xml")), parsed);
	}

	@Test
	public void testFile2Path() {
		Path<QNm> parsed = (new PathParser("_test.xml")).parse();
		assertEquals("Path parsed correctly", new Path<QNm>().self().child(
				new QNm("_test.xml")), parsed);
	}

	@Test
	public void testFilePath2() {
		Path<QNm> parsed = (new PathParser("../conf.d//test.xml")).parse();
		assertEquals("Path parsed correctly", new Path<QNm>().parent()
				.child(new QNm("conf.d")).descendant(new QNm("test.xml")), parsed);
	}

	@Test
	public void testInvalidPath1() {
		try {
			Path<QNm> parsed = (new PathParser("/a/..b/c")).parse();
			fail("Parser accepted invalid input");
		} catch (PathException e) {
			// expected
		}
	}

	@Test
	public void testMatchWithBacktracking() {
		Path<QNm> pattern = (new PathParser("//a/b//c")).parse();
		Path<QNm> path = (new PathParser("/e/a/b/b/f/b/e/c")).parse();
		assertTrue("Pattern matches path", pattern.matches(path));
	}

	@Test
	public void testMatchWithDoubleTwoStagedBacktracking() throws Exception {
		Path<QNm> pattern = (new PathParser("//a/b/c//d")).parse();
		Path<QNm> path = (new PathParser("/a/b/c/a/b/b/c/f/b/c/e/d"))
				.parse();
		assertTrue("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithBacktrackingButNoDescendantStartAxis()
			throws Exception {
		Path<QNm> pattern = (new PathParser("/a/b//c")).parse();
		Path<QNm> path = (new PathParser("/e/a/b/b/f/b/e/c")).parse();
		assertFalse("Pattern matches path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithDoubleTwoStagedBacktracking() {
		Path<QNm> pattern = (new PathParser("//a/b/c//d")).parse();
		Path<QNm> path = (new PathParser("/a/b/b/c/f/b/c/e/d")).parse();
		assertFalse("Pattern does not match path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithDoubleTwoStagedBacktrackingButNoDescendantStartAxis()
			throws Exception {
		Path<QNm> pattern = (new PathParser("/a/b/c//d")).parse();
		Path<QNm> path = (new PathParser("/e/a/b/c/a/b/b/c/f/b/c/e/d"))
				.parse();
		assertFalse("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testMatch() {
		Path<QNm> pattern = (new PathParser("//c")).parse();
		Path<QNm> path = (new PathParser("//a/b/c")).parse();
		assertTrue("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testVerySimplePath() {
		Path<QNm> parsed = (new PathParser("l")).parse();
		assertFalse(parsed.isEmpty());
	}
}
