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
package io.brackit.query.compiler.optimizer;

import io.brackit.query.Query;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Dec;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import org.junit.Test;

/**
 * Tests for the constant folding optimization pass.
 *
 * @author Brackit Project Team
 */
public class ConstantFoldingTest extends XQueryBaseTest {

  // ========== Arithmetic Tests ==========

  @Test
  public void addIntegers() throws Exception {
    Sequence res = new Query("1 + 2").execute(ctx);
    ResultChecker.dCheck(new Int32(3), res);
  }

  @Test
  public void subtractIntegers() throws Exception {
    Sequence res = new Query("10 - 3").execute(ctx);
    ResultChecker.dCheck(new Int32(7), res);
  }

  @Test
  public void multiplyIntegers() throws Exception {
    Sequence res = new Query("4 * 5").execute(ctx);
    ResultChecker.dCheck(new Int32(20), res);
  }

  @Test
  public void divideIntegers() throws Exception {
    Sequence res = new Query("10 div 2").execute(ctx);
    ResultChecker.dCheck(new Int32(5), res);
  }

  @Test
  public void idivideIntegers() throws Exception {
    Sequence res = new Query("10 idiv 3").execute(ctx);
    ResultChecker.dCheck(new Int32(3), res);
  }

  @Test
  public void modulusIntegers() throws Exception {
    Sequence res = new Query("10 mod 3").execute(ctx);
    ResultChecker.dCheck(new Int32(1), res);
  }

  @Test
  public void addDecimals() throws Exception {
    Sequence res = new Query("1.5 + 2.5").execute(ctx);
    ResultChecker.dCheck(new Dec("4.0"), res);
  }

  @Test
  public void multiplyDecimals() throws Exception {
    Sequence res = new Query("10.5 * 2").execute(ctx);
    ResultChecker.dCheck(new Dec("21.0"), res);
  }

  @Test
  public void nestedArithmetic() throws Exception {
    Sequence res = new Query("(1 + 2) * 3").execute(ctx);
    ResultChecker.dCheck(new Int32(9), res);
  }

  @Test
  public void complexNestedArithmetic() throws Exception {
    Sequence res = new Query("((2 + 3) * 4) - 10").execute(ctx);
    ResultChecker.dCheck(new Int32(10), res);
  }

  // ========== Comparison Tests ==========

  @Test
  public void valueCompEQ() throws Exception {
    Sequence res = new Query("5 eq 5").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompNE() throws Exception {
    Sequence res = new Query("5 ne 3").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompLT() throws Exception {
    Sequence res = new Query("3 lt 5").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompLE() throws Exception {
    Sequence res = new Query("5 le 5").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompGT() throws Exception {
    Sequence res = new Query("5 gt 3").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompGE() throws Exception {
    Sequence res = new Query("5 ge 5").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void valueCompFalse() throws Exception {
    Sequence res = new Query("3 gt 5").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, res);
  }

  @Test
  public void stringComparison() throws Exception {
    Sequence res = new Query("\"abc\" eq \"abc\"").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void stringComparisonLT() throws Exception {
    Sequence res = new Query("\"abc\" lt \"def\"").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  // ========== Boolean Tests ==========

  @Test
  public void andTrueTrue() throws Exception {
    Sequence res = new Query("(true()) and (true())").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void andTrueFalse() throws Exception {
    Sequence res = new Query("(true()) and (false())").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, res);
  }

  @Test
  public void andFalseTrue() throws Exception {
    Sequence res = new Query("(false()) and (true())").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, res);
  }

  @Test
  public void andFalseFalse() throws Exception {
    Sequence res = new Query("(false()) and (false())").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, res);
  }

  @Test
  public void orTrueTrue() throws Exception {
    Sequence res = new Query("(true()) or (true())").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void orTrueFalse() throws Exception {
    Sequence res = new Query("(true()) or (false())").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void orFalseTrue() throws Exception {
    Sequence res = new Query("(false()) or (true())").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void orFalseFalse() throws Exception {
    Sequence res = new Query("(false()) or (false())").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, res);
  }

  // ========== If Expression Tests ==========

  @Test
  public void ifTrueCondition() throws Exception {
    Sequence res = new Query("if (true()) then 1 else 2").execute(ctx);
    ResultChecker.dCheck(new Int32(1), res);
  }

  @Test
  public void ifFalseCondition() throws Exception {
    Sequence res = new Query("if (false()) then 1 else 2").execute(ctx);
    ResultChecker.dCheck(new Int32(2), res);
  }

  @Test
  public void ifWithConstantComparison() throws Exception {
    Sequence res = new Query("if (5 gt 3) then \"yes\" else \"no\"").execute(ctx);
    ResultChecker.dCheck(new Str("yes"), res);
  }

  // ========== String Concatenation Tests ==========

  @Test
  public void stringConcat() throws Exception {
    Sequence res = new Query("\"hello\" || \" \" || \"world\"").execute(ctx);
    ResultChecker.dCheck(new Str("hello world"), res);
  }

  @Test
  public void stringConcatSimple() throws Exception {
    Sequence res = new Query("\"a\" || \"b\"").execute(ctx);
    ResultChecker.dCheck(new Str("ab"), res);
  }

  // ========== Mixed/Complex Tests ==========

  @Test
  public void comparisonOfArithmetic() throws Exception {
    Sequence res = new Query("(1 + 2) eq 3").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  @Test
  public void arithmeticInIf() throws Exception {
    Sequence res = new Query("if ((2 + 2) eq 4) then 100 else 0").execute(ctx);
    ResultChecker.dCheck(new Int32(100), res);
  }

  @Test
  public void booleanWithComparison() throws Exception {
    Sequence res = new Query("(5 gt 3) and (2 lt 4)").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, res);
  }

  // ========== Non-Constant Expression Tests (should not fold) ==========

  @Test
  public void nonConstantArithmetic() throws Exception {
    Sequence res = new Query("let $x := 5 return $x + 3").execute(ctx);
    ResultChecker.dCheck(new Int32(8), res);
  }

  @Test
  public void nonConstantIf() throws Exception {
    Sequence res = new Query("let $x := true() return if ($x) then 1 else 2").execute(ctx);
    ResultChecker.dCheck(new Int32(1), res);
  }

  // ========== Edge Cases ==========

  @Test
  public void zeroMultiplication() throws Exception {
    Sequence res = new Query("0 * 1000000").execute(ctx);
    ResultChecker.dCheck(new Int32(0), res);
  }

  @Test
  public void negativeNumbers() throws Exception {
    Sequence res = new Query("-5 + 10").execute(ctx);
    ResultChecker.dCheck(new Int32(5), res);
  }

  @Test
  public void doubleNegative() throws Exception {
    Sequence res = new Query("--5").execute(ctx);
    ResultChecker.dCheck(new Int32(5), res);
  }
}
