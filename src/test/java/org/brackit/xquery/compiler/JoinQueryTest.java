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
package org.brackit.xquery.compiler;

import org.brackit.xquery.ResultChecker;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.sequence.ItemSequence;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Sequence;
import org.junit.Test;

/**
 * @author Sebastian Baechle
 *
 */
public class JoinQueryTest extends XQueryBaseTest {
	@Test
	public void simpleForFor() throws Exception {
		String query = readQuery("/join/", "simpleForFor.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(2,3,5), res);
	}

	@Test
	public void forNestedFor() throws Exception {
		String query = readQuery("/join/", "forNestedFor.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(2,3,5), res);
	}
	
	@Test
	public void forNestedFor2JoinPredicates() throws Exception {
		String query = readQuery("/join/", "forNestedFor2JoinPredicates.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		print(res);
		ResultChecker.dCheck(intSequence(2,3,5), res);
	}
	
	@Test
	public void forNestedForWithOutsideRef() throws Exception {
		String query = readQuery("/join/", "forNestedForWithOutsideRef.xq");
		XQuery xq = new XQuery(query);
		Sequence res = xq.execute(createContext());
		ResultChecker.dCheck(intSequence(3,3,4,4,6,6), res);
	}	
	
	private Sequence intSequence(int... values) {
		Item[] items = new Item[values.length];
		for (int i = 0; i < values.length; i++) {
			items[i] = new Int32(values[i]);
		}
		return new ItemSequence(items);
	}
}
