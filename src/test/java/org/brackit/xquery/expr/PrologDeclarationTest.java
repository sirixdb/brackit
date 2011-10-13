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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Dbl;
import org.brackit.xquery.atomic.Int;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 * 
 */
public class PrologDeclarationTest extends XQueryBaseTest {

	@Test
	public void declareFunction() throws Exception {
		Sequence res = new XQuery(
				"declare function local:addOne($a as item()) { $a + 1 }; local:addOne(1)")
				.execute(ctx);
		ResultChecker.check(new Int32(2), res);
	}

	@Test
	public void declareRecursiveFunction() throws Exception {
		Sequence res = new XQuery(
				"declare function local:countdown($a as xs:integer) { if ($a > 0) then ($a, local:countdown($a - 1)) else $a }; local:countdown(3)")
				.execute(ctx);
		ResultChecker.check(new ItemSequence(new Int32(3), new Int32(2),
				new Int32(1), new Int32(0)), res);
	}

	@Test
	public void declareIndirectRecursiveFunctions() throws Exception {
		Sequence res = new XQuery(
				"declare function local:a($a as xs:integer) { if ($a > 0) then ('a', $a, local:b($a - 1)) else ('a', $a) }; declare function local:b($b as xs:integer) { if ($b > 0) then ('b', $b, local:a($b - 1)) else ('b', $b) }; local:a(3)")
				.execute(ctx);
		ResultChecker.check(new ItemSequence(new Str("a"), new Int32(3),
				new Str("b"), new Int32(2), new Str("a"), new Int32(1),
				new Str("b"), new Int32(0)), res);
	}

	@Test
	public void declareFunctionInIllegalNS() throws Exception {
		try {
			new XQuery("declare function xs:addOne($a as item()) { $a + 1 }; 1");
			fail("Illegal declaration not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
					e.getCode());
		}
	}

	@Test
	public void variableDeclarationWithAccess() throws Exception {
		Sequence result = new XQuery(
				"declare variable $x := 1; for $a in (1,2,3) return $a + $x")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(2), new Int32(3),
				new Int32(4)), result);
	}

	@Test
	public void twoVariableDeclarationsWithAccess() throws Exception {
		Sequence result = new XQuery(
				"declare variable $x := 1; declare variable $y := $x + 2; for $a in (1,2,3) return $a + $x + $y")
				.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(5), new Int32(6),
				new Int32(7)), result);
	}

	@Test
	public void externalVariableDeclarationWithAccess() throws Exception {
		ctx.bind(new QNm("x"), Int32.ZERO_TWO_TWENTY[2]);
		XQuery query = new XQuery(
				"declare variable $x external := 1; for $a in (1,2,3) return $a + $x");
		Sequence result = query.execute(ctx);
		ResultChecker.dCheck(new ItemSequence(new Int32(3), new Int32(4),
				new Int32(5)), result);
	}

	@Test
	public void declareVariableWithCylicInitializer() throws Exception {
		QueryContext ctx = createContext();
		ctx.setContextItem(new Int(1));
		try {
			new XQuery("declare variable $a := $a + 1; $a");
			fail("Illegal cycle in initializer not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
		}
	}
	
	@Test
	public void declare2VariablesWithCylicInitializer() throws Exception {
		QueryContext ctx = createContext();
		ctx.setContextItem(new Int(1));
		try {
			new XQuery("declare variable $a := $b + 1; declare variable $b := $a + 1; $a + $b");
			fail("Illegal cycle in initializer not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
		}
	}
	
	@Test
	public void declare2VariablesAndFunctionWithCylicInitializer() throws Exception {
		QueryContext ctx = createContext();
		ctx.setContextItem(new Int(1));
		try {
			new XQuery("declare variable $a := local:foo(); declare variable $b := $a + 1; declare function local:foo() { $b + 1 }; $a + $b");
			fail("Illegal cycle in initializer not detected");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
		}
	}

	@Test
	public void declareNS() throws Exception {
		Sequence result = new XQuery(
				"declare namespace foo=\"http://brackit.org/foo\"; (<a/>)/foo:a")
				.execute(ctx);
		ResultChecker.dCheck(null, result);
	}

	@Test
	public void declareDefaultElementNS() throws Exception {
		Sequence result = new XQuery(
				"declare default element namespace \"http://brackit.org/foo\"; <a/>")
				.execute(ctx);
		ResultChecker.dCheck(
				ctx.getNodeFactory().element(
						new QNm("http://brackit.org/foo", "a")), result, false);
	}

	@Test
	public void declareDefaultFunctionNS() throws Exception {
		Sequence result = new XQuery(
				"declare default function namespace \"http://brackit.org/foo\"; declare function inc($i as xs:integer) { $i + 1 }; inc(1)")
				.execute(ctx);
		ResultChecker.dCheck(new Int(2), result);
	}

	@Test
	public void declareOption() throws Exception {
		Sequence result = new XQuery("declare option foo \"bar\"; 1")
				.execute(ctx);
		ResultChecker.dCheck(new Int(1), result);
	}

	@Test
	public void declareContextItemExternal() throws Exception {
		QueryContext ctx = createContext();
		ctx.setContextItem(new Int(1));
		Sequence result = new XQuery(
				"declare context item as item() external; .").execute(ctx);
		ResultChecker.dCheck(new Int(1), result);
	}

	@Test
	public void declareContextItemExternalWrongType() throws Exception {
		try {
			QueryContext ctx = createContext();
			ctx.setContextItem(new Int(1));
			new XQuery("declare context item as node() external; .")
					.execute(ctx);
			fail("Illegal context item access accepted");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
		}
	}

	@Test
	public void declareContextItemIgnoreExternal() throws Exception {
		QueryContext ctx = createContext();
		ctx.setContextItem(new Int(1));
		Sequence result = new XQuery(
				"declare context item as xs:double := xs:double(1); .")
				.execute(ctx);
		ResultChecker.dCheck(new Dbl(1), result);
	}

	@Test
	public void declareContextItemContextDependentDefaultValue()
			throws Exception {
		try {
			new XQuery("declare context item as item() := 1 + .; .");
			fail("Illegal context item declaration accepted");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER,
					e.getCode());
		}
	}
	
	@Test
	public void declareContextItemContextDependentDefaultValue2()
			throws Exception {
		try {
			new XQuery("declare context item as item() := 1 + a; .");
			fail("Illegal context item declaration accepted");
		} catch (QueryException e) {
			assertEquals("Correct error code",
					ErrorCode.ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER,
					e.getCode());
		}
	}
	
	@Test
	public void declareContextItemContextDefaultValue()
			throws Exception {
		new XQuery("declare context item as item() := 1 + (<a/>)//a/(.); .");
	}
}