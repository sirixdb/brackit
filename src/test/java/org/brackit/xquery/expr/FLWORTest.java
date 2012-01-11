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
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 * 
 */
public class FLWORTest extends XQueryBaseTest {

	@Test
	public void forExpr() throws Exception {
		Sequence result = new XQuery("for $a in (1,2,3) return $a + 1")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(2, 3, 4), result);
	}

	@Test
	public void forWithTwoBindingExprs() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3), $b in (4,5,6) return $a + $b").execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7, 6, 7, 8, 7, 8, 9), result);
	}

	@Test
	public void twoForExprs() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) for $b in (4,5,6) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7, 6, 7, 8, 7, 8, 9), result);
	}

	@Test
	public void forExprWithRunVariable() throws Exception {
		Sequence result = new XQuery("for $a  at $b in (4,5,6) return $b + 1")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(2, 3, 4), result);
	}

	@Test
	public void forExprWithWhereClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1 to 5) where $a > 3 return $a").execute(ctx);
		ResultChecker.dCheck(intSequence(4, 5), result);
	}

	@Test
	public void forExprWithTwoWhereClauses() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1 to 5) where $a > 3 where $a < 5 return $a")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(4), result);
	}

	@Test
	public void forExprWithOrderByClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) order by $a ascending return $a")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(1, 2, 3), result);
	}

	@Test
	public void forExprOneLetBinding() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 return $a + $b").execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7), result);
	}

	@Test
	public void forExprTwoLetBindings() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 1 return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(6, 7, 8), result);
	}

	@Test
	public void forExprThreeLetBindingsOneLetUnused() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 9 let $d:=1 return $a + $b + $d")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(6, 7, 8), result);
	}

	@Test
	public void forExprTwoLetBindingsWithWhereClause() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) let $b:=4 let $c:= 1 where $a + $b + $c > 7 return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(new Int32(8), result);
	}

	@Test
	public void forExprTwoLetBindingsWithOrderByRunVar() throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) let $b:=4 let $c:= 1 order by $a ascending return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(6, 7, 8), result);
	}

	@Test
	public void forExprTwoLetBindingsWithOrderByLetVarAndRunVar()
			throws Exception {
		Sequence result = new XQuery(
				"for $a in (3,2,1) let $b:=4 let $c:= 1 order by $c, $a ascending return $a + $b + $c")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(6, 7, 8), result);
	}

	@Test
	public void letExpr() throws Exception {
		Sequence result = new XQuery("let $a:= (1,2,3) return $a").execute(ctx);
		ResultChecker.dCheck(intSequence(1, 2, 3), result);
	}

	@Test
	public void letExprVarUnused() throws Exception {
		Sequence result = new XQuery("let $a:= (1,2,3) return (1,2,3)")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(1, 2, 3), result);
	}

	@Test
	public void letExprWithWhereClause() throws Exception {
		Sequence result = new XQuery("let $a:= 2 where $a > 3 return $a")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void nestedFor() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) return for $b in (4,5,6) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7, 6, 7, 8, 7, 8, 9), result);
	}

	@Test
	public void variableShadowingWithNormalize() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) for $b in (for $a in (4,5,6) return $a) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7, 6, 7, 8, 7, 8, 9), result);
	}

	@Test
	public void nestedForWithAnotherForInNestedIn() throws Exception {
		Sequence result = new XQuery(
				"for $a in (1,2,3) return for $b in (for $c in (4,5,6) return $c) return $a + $b")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(5, 6, 7, 6, 7, 8, 7, 8, 9), result);
	}

	@Test
	public void simpleLetExpr() throws Exception {
		Sequence result = new XQuery("let $a := (1,2,3) return 1").execute(ctx);
		ResultChecker.dCheck(new Int32(1), result);
	}

	@Test
	public void sequenceOfLetExpressions() throws Exception {
		Sequence result = new XQuery(
				"let $a := (1,2,3) let $b := (4,5,6) let $c := (7,8,9) return ($a, $b, $c) ")
				.execute(ctx);
		ResultChecker.dCheck(intSequence(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
	}

	@Test
	public void sequenceOfLetAndForExpressions() throws Exception {
		Sequence result = new XQuery(
				"let $a := (1,2,3) let $b := (4,5,6) let $c := (7,8,9) for $d in $a for $e in $b for $f in $c let $g := ($d + $f) return $d + $d + $f ")
				.execute(ctx);		
		Sequence ints = intSequence(9, 10, 11, 9, 10, 11, 9, 10, 11, 11, 12,
				13, 11, 12, 13, 11, 12, 13, 13, 14, 15, 13, 14, 15, 13, 14, 15);
		ResultChecker.dCheck(ints, result);
	}

	@Test
	public void forWithIllegalOrderByOnMixedData() throws Exception {
		try {
			Sequence result = new XQuery(
					"for $a in ('1aha', <a>12</a>, 19, 4) order by $a return $a")
					.execute(ctx);
			Iter it = result.iterate();
			try {
				while (it.next() != null)
					;
			} finally {
				it.close();
			}
			fail("No error thrown despite illegal conversion.");
		} catch (QueryException e) {
			assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
		}
	}

	private Sequence intSequence(int... v) {
		Int32[] s = new Int32[v.length];
		for (int i = 0; i < v.length; i++) {
			s[i] = new Int32(v[i]);
		}
		return new ItemSequence(s);
	}
}