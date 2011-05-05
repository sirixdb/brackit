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
package org.brackit.xquery.node.d2linked;

import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class D2NodeDivisionTest extends D2Node {
	public D2NodeDivisionTest() {
		super(null, FIRST);
	}

	@Test
	public void testSiblingBetween() {
		verify(new int[] { 4, 3 }, siblingBetween(new int[] { 3 },
				new int[] { 5 }));
		verify(new int[] { 5 },
				siblingBetween(new int[] { 3 }, new int[] { 7 }));

		verify(new int[] { 4, 3 }, siblingBetween(new int[] { 4 },
				new int[] { 5 }));
		verify(new int[] { 5 },
				siblingBetween(new int[] { 4 }, new int[] { 7 }));

		verify(new int[] { 5 },
				siblingBetween(new int[] { 4 }, new int[] { 6 }));
		verify(new int[] { 5 },
				siblingBetween(new int[] { 4 }, new int[] { 8 }));

		verify(new int[] { 3 }, siblingBetween(new int[] { 2, 2, 5 },
				new int[] { 5 }));
		verify(new int[] { 2, 2, 6, 3 }, siblingBetween(new int[] { 2, 2, 5 },
				new int[] { 2, 2, 7 }));
		verify(new int[] { 2, 3 }, siblingBetween(new int[] { 2, 2, 5 },
				new int[] { 2, 4, 3 }));

		verify(new int[] { 3, 2, 3 }, siblingBetween(
				new int[] { 3, 2, 2, 4, 5 }, new int[] { 3, 2, 4, 3 }));
		verify(new int[] { 3, 2, 2, 4, 6, 3 }, siblingBetween(new int[] { 3, 2,
				2, 4, 5 }, new int[] { 3, 2, 2, 4, 7 }));
	}

	@Test
	public void testBefore() {
		verify(new int[] { 5 }, siblingBefore(new int[] { 7 }));
		verify(new int[] { 5 }, siblingBefore(new int[] { 6, 3 }));
		verify(new int[] { 5 }, siblingBefore(new int[] { 6, 2, 4, 2 }));
		verify(new int[] { 5 }, siblingBefore(new int[] { 6, 4, 5, 3 }));
		verify(new int[] { 2, 3 }, siblingBefore(new int[] { 3 }));
		verify(new int[] { 2, 2, 3 }, siblingBefore(new int[] { 2, 3 }));
		verify(new int[] { 2, 2, 2, 3 }, siblingBefore(new int[] { 2, 2, 3 }));
		verify(new int[] { 2, 3 }, siblingBefore(new int[] { 2, 5, 2, 3 }));
		verify(new int[] { 2, 2, 3 }, siblingBefore(new int[] { 2, 2, 5, 3 }));
	}

	private void verify(int[] expected, int[] actual) {
		Assert.assertEquals("correct size", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals("division " + i, expected[i], actual[i]);
		}
	}

	@Override
	public Kind getKind() {
		return null;
	}

	@Override
	public String getName() throws DocumentException {
		return null;
	}

	@Override
	public String getValue() throws DocumentException {
		return null;
	}

}
