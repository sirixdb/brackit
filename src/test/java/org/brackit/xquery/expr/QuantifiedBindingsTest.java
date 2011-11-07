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

import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 *
 */
public class QuantifiedBindingsTest extends XQueryBaseTest {

	/**
	 * 
	 */
	public QuantifiedBindingsTest() {
		super();
	}

	@Test
	public void someQuantifiedExprOneBindingTrue() throws Exception {
		Sequence result = new XQuery("some $a in (1,2,3) satisfies $a > 2")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void someQuantifiedExprOneBindingFalse() throws Exception {
		Sequence result = new XQuery("some $a in (1,2,3) satisfies $a > 3")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void someQuantifiedExprTwoBindingsTrue() throws Exception {
		Sequence result = new XQuery(
				"some $a in (1,2,3), $b in (3,4,5) satisfies $a = $b")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void someQuantifiedExprTwoBindingsFalse() throws Exception {
		Sequence result = new XQuery(
				"some $a in (1,2,3), $b in (4,5,6) satisfies $a = $b")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void everyQuantifiedExprOneBindingTrue() throws Exception {
		Sequence result = new XQuery("every $a in (1,2,3) satisfies $a < 4")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void everyQuantifiedExprOneBindingFalse() throws Exception {
		Sequence result = new XQuery("every $a in (1,2,3) satisfies $a < 3")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

	@Test
	public void everyQuantifiedExprTwoBindingsTrue() throws Exception {
		Sequence result = new XQuery(
				"every $a in (1,2,3), $b in (4,5,6) satisfies $a < $b")
				.execute(ctx);
		ResultChecker.dCheck(Bool.TRUE, result);
	}

	@Test
	public void everyQuantifiedExprTwoBindingsFalse() throws Exception {
		Sequence result = new XQuery(
				"every $a in (1,2,3), $b in (3,4,5) satisfies $a < $b")
				.execute(ctx);
		ResultChecker.dCheck(Bool.FALSE, result);
	}

}