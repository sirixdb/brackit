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

import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.ResultChecker;
import io.brackit.query.Query;
import io.brackit.query.sequence.ItemSequence;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 */
public class LetBindLiftTest extends XQueryBaseTest {

  @Test
  public void forLetWhereConditional() {
    Sequence res = new Query("for $a in (1,2,3) " + "let $b := 5 " + "let $c := " + "	for $d in (2 to 1)"
        + "	let $e := " + "		for $f in (4,5,6) " + "		return $f " + "	where exactly-one($d) " + "	return $d "
        + "return 'no'").execute(ctx);
    Str no = new Str("no");
    ResultChecker.dCheck(new ItemSequence(no, no, no), res);
  }

  @Test
  public void orderByBatched() {
    Sequence res = new Query("for $a in (1,2,3) " + "let $c := " + "	for $d in (6,5,4) " + "	for $e in (7,8,9) "
        + "	order by $d ascending, $e descending " + "	return ($d,$e) " + "return ($a, $c)").execute(ctx);
    ResultChecker.dCheck(intSequence(1,
                                     4,
                                     9,
                                     4,
                                     8,
                                     4,
                                     7,
                                     5,
                                     9,
                                     5,
                                     8,
                                     5,
                                     7,
                                     6,
                                     9,
                                     6,
                                     8,
                                     6,
                                     7,
                                     2,
                                     4,
                                     9,
                                     4,
                                     8,
                                     4,
                                     7,
                                     5,
                                     9,
                                     5,
                                     8,
                                     5,
                                     7,
                                     6,
                                     9,
                                     6,
                                     8,
                                     6,
                                     7,
                                     3,
                                     4,
                                     9,
                                     4,
                                     8,
                                     4,
                                     7,
                                     5,
                                     9,
                                     5,
                                     8,
                                     5,
                                     7,
                                     6,
                                     9,
                                     6,
                                     8,
                                     6,
                                     7), res);
  }

  @Test
  public void orderBy() {
    Sequence res = new Query("for $a in (7,8,9) " + "let $c := " + "	for $d in (3,2,1) " + "	for $e in (4,5,6) "
        + "	order by $d ascending, $e descending " + "	return ($d,$e) " + "return ($a, $c)").execute(ctx);
    ResultChecker.dCheck(intSequence(7,
                                     1,
                                     6,
                                     1,
                                     5,
                                     1,
                                     4,
                                     2,
                                     6,
                                     2,
                                     5,
                                     2,
                                     4,
                                     3,
                                     6,
                                     3,
                                     5,
                                     3,
                                     4,
                                     8,
                                     1,
                                     6,
                                     1,
                                     5,
                                     1,
                                     4,
                                     2,
                                     6,
                                     2,
                                     5,
                                     2,
                                     4,
                                     3,
                                     6,
                                     3,
                                     5,
                                     3,
                                     4,
                                     9,
                                     1,
                                     6,
                                     1,
                                     5,
                                     1,
                                     4,
                                     2,
                                     6,
                                     2,
                                     5,
                                     2,
                                     4,
                                     3,
                                     6,
                                     3,
                                     5,
                                     3,
                                     4), res);
  }

  @Test
  public void orderBy2() {
    Sequence res = new Query("	for $d in (3,2,1) " + "	for $e in (4,5,6) " + "	order by $d ascending, $e descending "
        + "	return ($d,$e)").execute(ctx);
    ResultChecker.dCheck(intSequence(1, 6, 1, 5, 1, 4, 2, 6, 2, 5, 2, 4, 3, 6, 3, 5, 3, 4), res);
  }

  @Test
  public void doubledNestedWithOrderBy() {
    Sequence res = new Query("for $z in 1 " + "let $x := " + "for $a in (1 to 5) " + "let $b := "
        + "	for $c in (2,4) " + "	let $f := if ($c) then (2,4) else () " + "	order by $c " + "	let $d := "
        + "		for $e in $f " + "		let $g := 'ignore' " + "		where $c = $a " + "		return $e " + "	return $d "
        + "return ($a,$b) " + "return $x").execute(ctx);
    ResultChecker.dCheck(intSequence(1, 2, 2, 4, 3, 4, 2, 4, 5), res);
  }

  private Sequence intSequence(int... v) {
    Int32[] s = new Int32[v.length];
    for (int i = 0; i < v.length; i++) {
      s[i] = new Int32(v[i]);
    }
    return new ItemSequence(s);
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    DefaultOptimizer.UNNEST = true;
  }
}
