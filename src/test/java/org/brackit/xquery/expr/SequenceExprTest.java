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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.operator.TupleImpl;
import org.brackit.xquery.sequence.ItemSequence;
import org.junit.Test;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class SequenceExprTest {
	QueryContext ctx = new QueryContext(null);

	@Test
	public void simpleSequence() throws Exception {
		SequenceExpr expr = new SequenceExpr(new Int32(1), new Int32(2),
				new Int32(3));
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3)), expr.evaluate(ctx, new TupleImpl()));
	}

	@Test
	public void emptySequence() throws Exception {
		SequenceExpr expr = new SequenceExpr();
		ResultChecker.dCheck(ctx, null, expr.evaluate(ctx, new TupleImpl()));
	}

	@Test
	public void simpleAndEmptySequence() throws Exception {
		SequenceExpr expr = new SequenceExpr(new Int32(1), new SequenceExpr(),
				new Int32(2), new Int32(3));
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3)), expr.evaluate(ctx, new TupleImpl()));
	}

	@Test
	public void nestedSequences() throws Exception {
		SequenceExpr expr = new SequenceExpr(new Int32(1), new SequenceExpr(
				new Int32(2), new Int32(3)), new Int32(4), new SequenceExpr(
				new Int32(5), new Int32(6), new Int32(7)));
		ResultChecker.dCheck(ctx, new ItemSequence(new Int32(1), new Int32(2),
				new Int32(3), new Int32(4), new Int32(5), new Int32(6),
				new Int32(7)), expr.evaluate(ctx, new TupleImpl()));
	}
}