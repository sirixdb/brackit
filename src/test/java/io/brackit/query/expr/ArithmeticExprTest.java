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
package io.brackit.query.expr;

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int32;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.ResultChecker;
import io.brackit.query.Query;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class ArithmeticExprTest extends XQueryBaseTest {

  @Test
  public void add() {
    Sequence result = new Query("1 + 2").execute(ctx);
    ResultChecker.dCheck(new Int32(3), result);
  }

  @Test
  public void mult() {
    Sequence result = new Query("1 * 2").execute(ctx);
    ResultChecker.dCheck(new Int32(2), result);
  }

  @Test
  public void multPrecedence1() {
    Sequence result = new Query("1 + 2 * 3").execute(ctx);
    ResultChecker.dCheck(new Int32(7), result);
  }

  @Test
  public void multPrecedence2() {
    Sequence result = new Query("1 * 2 + 3").execute(ctx);
    ResultChecker.dCheck(new Int32(5), result);
  }

  @Test
  public void parenthesizedPrecedence() {
    Sequence result = new Query("(1 + 2) * 3").execute(ctx);
    ResultChecker.dCheck(new Int32(9), result);
  }

}