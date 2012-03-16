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
package org.brackit.xquery.operator;

import java.util.Arrays;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.compiler.translator.Reference;

/**
 * Encapsulation of iteration nesting checks.
 * 
 * @author Sebastian Baechle
 * 
 */
public class Check {
	protected boolean check;
	private int len;
	private int[] iterations;

	public Check() {
		this.iterations = new int[2];
		this.len = 0;
	}

	public final boolean alive(Tuple t) throws QueryException {
		return t.get(iterations[len - 1]) != null;
	}

	public final boolean dead(Tuple t) throws QueryException {
		return t.get(iterations[len - 1]) == null;
	}

	public final boolean separate(Tuple t1, Tuple t2) throws QueryException {
		for (int i = len - 1; i >= 0; i--) {
			Atomic gk1 = (Atomic) t1.get(iterations[i]);			
			if (gk1 == null) {
				return true;
			}
			Atomic gk2 = (Atomic) t2.get(iterations[i]);
			if (gk2 == null) {
				return true;
			}
			if (gk1.atomicCmp(gk2) != 0) {
				return true;
			}
		}
		return false;
	}

	public final int local() {
		return iterations[len - 1];
	}

	public final Reference check() {
		check = true;
		final int i = len++;
		if (len == iterations.length) {
			iterations = Arrays.copyOf(iterations, len + 2);
		}
		return new Reference() {
			@Override
			public void setPos(int pos) {
				iterations[i] = pos;
			}
		};
	}
}
