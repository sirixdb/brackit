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
package org.brackit.xquery.compiler.optimizer;

import java.io.FileNotFoundException;

import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.util.serialize.StringSerializer;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 *
 */
public class JoinTest extends XQueryBaseTest {
	
	@Test
	public void simpleNestedFor() throws Exception {
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"for $b in (2,3,4) " +
				"where $a = $b " +
				"return $a").execute(ctx);
		ResultChecker.dCheck(intSequence(2,3), res);
	}
	
	@Test
	public void nestedForWithLetsInOuterFor() throws Exception {
		Sequence res = new XQuery(
				"for $w in (2,4) " +
				"let $x := (2 to $w) " +
				"for $a in (1,2,3) " +
				"for $b in $x " +
				"let $z := $a + $x[0] " +
				"where $a = $b " +
				"return $a").execute(ctx);
		ResultChecker.dCheck(intSequence(2,2,3), res);
	}
	
	@Test
	public void nestedForWithLets() throws Exception {
		Sequence res = new XQuery(
				"let $x := (2,3,4) " +
				"for $a in (1,2,3) " +
				"let $y := 'foo'" +
				"for $b in $x " +
				"let $z := $a + $x[0] " + 
				"where $a = $b " +
				"return $a").execute(ctx);
		ResultChecker.dCheck(intSequence(2,3), res);
	}
	
	@Test
	public void letNestedFor() throws Exception {
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"let $c := for $b in (2,3,4) " +
				"          where $a = $b " +
				"          return $a " +
				"return $c").execute(ctx);
		ResultChecker.dCheck(intSequence(2,3), res);
	}
	
	@Test
	public void letNestedForWithLets() throws Exception {
		Sequence res = new XQuery(
				"for $w in (2,4) " +
				"let $x := (2 to $w) " +
				"for $a in (1,2,3) " +
				"let $y := 'foo'" +
				"let $c := for $b in $x " +
				"          let $z := $a + $x[0] " +
				"          where $a = $b " +
				"          return $a " +
				"return $c").execute(ctx);
		ResultChecker.dCheck(intSequence(2,2,3), res);
	}
	
	@Test
	public void fakeJoin() throws Exception {
		Sequence res = new XQuery(
				"for $a in (1,2,3) " +
				"for $b in (2,3,4) " +
				"let $x := for $c in 1 " +
				"			where $a = $b " +
				"			return $c " +
				"return $x").execute(ctx);
		ResultChecker.dCheck(intSequence(1,1), res);
	}
	
	@Test
	public void simpleForFor() throws Exception {
		String query = readQuery("/join/", "simpleForFor.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(2, 3, 5), res);
	}

	@Test
	public void forNestedFor() throws Exception {
		String query = readQuery("/join/", "forNestedFor.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(2, 3, 5), res);
	}

	@Test
	public void forNestedFor2JoinPredicates() throws Exception {
		String query = readQuery("/join/", "forNestedFor2JoinPredicates.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(3, 4, 6), res);
	}

	@Test
	public void forNestedForWithOutsideRef() throws Exception {
		String query = readQuery("/join/", "forNestedForWithOutsideRef.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(3, 3, 4, 4, 6, 6), res);
	}
	
	private Sequence intSequence(int... v) {
		Int32[] s = new Int32[v.length];
		for (int i = 0; i < v.length; i++) {
			s[i] = new Int32(v[i]);
		}
		return new ItemSequence(s);
	}

	@Before
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		DefaultOptimizer.UNNEST = true;
		DefaultOptimizer.JOIN_DETECTION = true;
	}
}
