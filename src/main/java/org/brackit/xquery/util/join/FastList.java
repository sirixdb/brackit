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
package org.brackit.xquery.util.join;

import java.util.Arrays;

/**
 * Array-based variant of a list. Faster than java.util.ArrayList for out
 * purposes, e.g., makes less error-checks.
 * 
 * @author Sebastian Baechle
 * 
 */
public class FastList<E> {

	@SuppressWarnings("unchecked")
	public static final FastList EMPTY_LIST = new FastList(0);

	private Object[] values;

	private int size;

	public FastList(int size) {
		values = new Object[size];
	}

	public FastList() {
		values = new Object[10];
	}

	public int getSize() {
		return size;
	}

	@SuppressWarnings("unchecked")
	public E get(int p) {
		return (E) values[p];
	}

	public void addAll(E[] v, int off, int len) {
		capacity(size + len);
		System.arraycopy(v, off, values, size, len);
	}

	private void capacity(int capacity) {
		if (values.length < capacity) {
			values = Arrays.copyOf(values, capacity);
		}
	}

	public void sort() {
		Arrays.sort(values, 0, size);
	}

	public void add(E v) {
		if (size == values.length) {
			values = Arrays.copyOf(values, ((values.length * 3) / 2 + 1));
		}
		values[size++] = v;
	}

	public void addUnchecked(E v) {
		values[size++] = v;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void ensureAdditional(int len) {
		capacity(size + len);
	}
}