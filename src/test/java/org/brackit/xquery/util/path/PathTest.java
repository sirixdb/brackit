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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.RecognitionException;
import org.brackit.xquery.util.path.Path;
import org.brackit.xquery.util.path.PathParser;
import org.junit.Test;

public class PathTest {
	@Test
	public void testSimplePath() throws Exception {
		Path<String> expected = (new Path<String>()).child("tag");
		Path<String> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath2() throws Exception {
		Path<String> expected = (new Path<String>()).child("tag")
				.child("hallo").descendant("aha");
		Path<String> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath3() throws Exception {
		Path<String> expected = (new Path<String>()).self().child("tag").child(
				"hallo").descendant("aha");
		Path<String> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSimplePath4() throws Exception {
		Path<String> expected = (new Path<String>()).self().child("tag").child(
				"hallo").descendant("aha");
		String implicitSelfPath = expected.toString().substring(2);
		Path<String> parsed = (new PathParser(implicitSelfPath)).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testSelfPath4() throws Exception {
		Path<String> expected = (new Path<String>()).self().self().descendant(
				"aha");
		Path<String> parsed = (new PathParser(expected.toString())).parse();
		assertEquals("Path parsed correctly", expected, parsed);
	}

	@Test
	public void testFilePath() throws Exception {
		Path<String> parsed = (new PathParser("/test.xml")).parse();
		System.out.println(parsed);
	}

	@Test
	public void testFile2Path() throws Exception {
		Path<String> parsed = (new PathParser("_test.xml")).parse();
		System.out.println(parsed.trailing());
		System.out.println(parsed);
	}

	@Test
	public void testFilePath2() throws Exception {
		Path<String> parsed = (new PathParser("../conf.d//test.xml")).parse();
		System.out.println(parsed);
	}

	@Test
	public void testInvalidPath1() throws Exception {
		try {
			Path<String> parsed = (new PathParser("/a/..b/c")).parse();
			fail("Parser accepted invalid input");
		} catch (RecognitionException e) {
			// expected
		}
	}

	@Test
	public void testMatchWithBacktracking() throws Exception {
		Path<String> pattern = (new PathParser("//a/b//c")).parse();
		Path<String> path = (new PathParser("/e/a/b/b/f/b/e/c")).parse();
		assertTrue("Pattern matches path", pattern.matches(path));
	}

	@Test
	public void testMatchWithDoubleTwoStagedBacktracking() throws Exception {
		Path<String> pattern = (new PathParser("//a/b/c//d")).parse();
		Path<String> path = (new PathParser("/a/b/c/a/b/b/c/f/b/c/e/d"))
				.parse();
		assertTrue("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithBacktrackingButNoDescendantStartAxis()
			throws Exception {
		Path<String> pattern = (new PathParser("/a/b//c")).parse();
		Path<String> path = (new PathParser("/e/a/b/b/f/b/e/c")).parse();
		assertFalse("Pattern matches path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithDoubleTwoStagedBacktracking() throws Exception {
		Path<String> pattern = (new PathParser("//a/b/c//d")).parse();
		Path<String> path = (new PathParser("/a/b/b/c/f/b/c/e/d")).parse();
		assertFalse("Pattern does not match path", pattern.matches(path));
	}

	@Test
	public void testNoMatchWithDoubleTwoStagedBacktrackingButNoDescendantStartAxis()
			throws Exception {
		Path<String> pattern = (new PathParser("/a/b/c//d")).parse();
		Path<String> path = (new PathParser("/e/a/b/c/a/b/b/c/f/b/c/e/d"))
				.parse();
		assertFalse("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testMatch() throws Exception {
		Path<String> pattern = (new PathParser("//c")).parse();
		Path<String> path = (new PathParser("//a/b/c")).parse();
		assertTrue("Pattern does match path", pattern.matches(path));
	}

	@Test
	public void testVerySimplePath() throws Exception {
		Path<String> parsed = (new PathParser("l")).parse();
		assertFalse(parsed.isEmpty());
	}
}
