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
package org.brackit.query.expr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.ResultChecker;
import org.brackit.query.Query;
import org.brackit.query.XQueryBaseTest;
import org.brackit.query.atomic.Dbl;
import org.brackit.query.atomic.Int;
import org.brackit.query.atomic.Int32;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Str;
import org.brackit.query.sequence.ItemSequence;
import org.brackit.query.jdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class PrologDeclarationTest extends XQueryBaseTest {

  @Test
  public void declareFunction() {
    Sequence res = new Query("declare function local:addOne($a as item()) { $a + 1 }; local:addOne(1)").execute(ctx);
    ResultChecker.check(new Int32(2), res);
  }

  @Test
  public void declareRecursiveFunction() {
    Sequence res = new Query(
                              "declare function local:countdown($a as xs:integer) { if ($a > 0) then ($a, local:countdown($a - 1)) else $a }; local:countdown(3)").execute(ctx);
    ResultChecker.check(new ItemSequence(new Int32(3), new Int32(2), new Int32(1), new Int32(0)), res);
  }

  @Test
  public void declareIndirectRecursiveFunctions() {
    Sequence res = new Query(
                              "declare function local:a($a as xs:integer) { if ($a > 0) then ('a', $a, local:b($a - 1)) else ('a', $a) }; declare function local:b($b as xs:integer) { if ($b > 0) then ('b', $b, local:a($b - 1)) else ('b', $b) }; local:a(3)").execute(ctx);
    ResultChecker.check(new ItemSequence(new Str("a"),
                                         new Int32(3),
                                         new Str("b"),
                                         new Int32(2),
                                         new Str("a"),
                                         new Int32(1),
                                         new Str("b"),
                                         new Int32(0)), res);
  }

  @Test
  public void declareFunctionInIllegalNS() {
    try {
      new Query("declare function xs:addOne($a as item()) { $a + 1 }; 1");
      fail("Illegal declaration not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE, e.getCode());
    }
  }

  @Test
  public void variableDeclarationWithAccess() {
    Sequence result = new Query("declare variable $x := 1; for $a in (1,2,3) return $a + $x").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(2), new Int32(3), new Int32(4)), result);
  }

  @Test
  public void twoVariableDeclarationsWithAccess() {
    Sequence result = new Query(
                                 "declare variable $x := 1; declare variable $y := $x + 2; for $a in (1,2,3) return $a + $x + $y").execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(5), new Int32(6), new Int32(7)), result);
  }

  @Test
  public void externalVariableDeclarationWithAccess() {
    ctx.bind(new QNm("x"), Int32.ZERO_TO_TWENTY[2]);
    Query query = new Query("declare variable $x external := 1; for $a in (1,2,3) return $a + $x");
    Sequence result = query.execute(ctx);
    ResultChecker.dCheck(new ItemSequence(new Int32(3), new Int32(4), new Int32(5)), result);
  }

  @Test
  public void declareVariableWithCylicInitializer() throws Exception {
    QueryContext ctx = createContext();
    ctx.setContextItem(new Int(1));
    try {
      new Query("declare variable $a := $a + 1; $a");
      fail("Illegal cycle in initializer not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
    }
  }

  @Test
  public void declare2VariablesWithCylicInitializer() throws Exception {
    QueryContext ctx = createContext();
    ctx.setContextItem(new Int(1));
    try {
      new Query("declare variable $a := $b + 1; declare variable $b := $a + 1; $a + $b");
      fail("Illegal cycle in initializer not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
    }
  }

  @Test
  public void declare2VariablesAndFunctionWithCylicInitializer() throws Exception {
    QueryContext ctx = createContext();
    ctx.setContextItem(new Int(1));
    try {
      new Query("declare variable $a := local:foo(); declare variable $b := $a + 1; declare function local:foo() { $b + 1 }; $a + $b");
      fail("Illegal cycle in initializer not detected");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_CIRCULAR_VARIABLE_DEPENDENCY, e.getCode());
    }
  }

  @Test
  public void declareNS() {
    Sequence result = new Query("declare namespace foo=\"http://brackit.org/foo\"; (<a/>)/foo:a").execute(ctx);
    ResultChecker.dCheck(null, result);
  }

  @Test
  public void declareDefaultElementNS() {
    Sequence result = new Query("declare default element namespace \"http://brackit.org/foo\"; <a/>").execute(ctx);
    ResultChecker.dCheck(ctx.getNodeFactory().element(new QNm("http://brackit.org/foo", "a")), result, false);
  }

  @Test
  public void declareDefaultFunctionNS() {
    Sequence result = new Query(
                                 "declare default function namespace \"http://brackit.org/foo\"; declare function inc($i as xs:integer) { $i + 1 }; inc(1)").execute(ctx);
    ResultChecker.dCheck(new Int(2), result);
  }

  @Test
  public void declareOption() {
    Sequence result = new Query("declare option foo \"bar\"; 1").execute(ctx);
    ResultChecker.dCheck(new Int(1), result);
  }

  @Test
  public void declareContextItemExternal() throws Exception {
    QueryContext ctx = createContext();
    ctx.setContextItem(new Int(1));
    Sequence result = new Query("declare context item as item() external; $$").execute(ctx);
    ResultChecker.dCheck(new Int(1), result);
  }

  @Test
  public void declareContextItemExternalWrongType() throws Exception {
    try {
      QueryContext ctx = createContext();
      ctx.setContextItem(new Int(1));
      new Query("declare context item as node() external; $$").execute(ctx);
      fail("Illegal context item access accepted");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE, e.getCode());
    }
  }

  @Test
  public void declareContextItemIgnoreExternal() throws Exception {
    QueryContext ctx = createContext();
    ctx.setContextItem(new Int(1));
    Sequence result = new Query("declare context item as xs:double := xs:double(1); $$").execute(ctx);
    ResultChecker.dCheck(new Dbl(1), result);
  }

  @Test
  public void declareContextItemContextDependentDefaultValue() {
    try {
      new Query("declare context item as item() := 1 + $$; $$");
      fail("Illegal context item declaration accepted");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER, e.getCode());
    }
  }

  @Test
  public void declareContextItemContextDependentDefaultValue2() {
    try {
      new Query("declare context item as item() := 1 + a; $$");
      fail("Illegal context item declaration accepted");
    } catch (QueryException e) {
      assertEquals("Correct error code", ErrorCode.ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER, e.getCode());
    }
  }

  @Test
  public void declareContextItemContextDefaultValue() {
    new Query("declare context item as item() := 1 + (<a/>)//a/($$); $$");
  }
}