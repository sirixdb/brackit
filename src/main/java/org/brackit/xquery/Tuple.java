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
package org.brackit.xquery;

import org.brackit.xquery.xdm.Sequence;

/**
 * A tuple represents the data that "flows" through the query pipeline. In
 * XQuery, tuples are used to unroll nested evaluation. Its columns represent
 * dynamically bound variables like in-scope variables of let, for, some, or
 * every expressions.
 * 
 * @author Sebastian Baechle
 * 
 */
public interface Tuple {
	/**
	 * Returns the size of the tuple.
	 */
	public int getSize();

	/**
	 * Returns the sequence at the specified position.
	 */
	public Sequence get(int position) throws QueryException;

	/**
	 * Returns an array representation of this tuple.
	 */
	public Sequence[] array() throws QueryException;

	/**
	 * Projects the given positions into a new tuple.
	 */
	public Tuple project(int... positions) throws QueryException;

	/**
	 * Projects the given range into a new tuple.
	 */
	public Tuple project(int start, int end) throws QueryException;

	/**
	 * Create a copy where the specified position is updated with the given
	 * sequence.
	 */
	public Tuple replace(int position, Sequence s) throws QueryException;

	/**
	 * Append the given sequence to a copy.
	 */
	public Tuple concat(Sequence s) throws QueryException;

	/**
	 * Append the given sequences to a copy.
	 */
	public Tuple concat(Sequence[] s) throws QueryException;
	
	/**
	 * Append the given sequence to a copy and update the
	 * specified position with the given sequence.
	 */
	public Tuple conreplace(Sequence con, int position, Sequence s) throws QueryException;
	
	/**
	 * Append the given sequences to a copy and update the
	 * specified position with the given sequence.
	 */
	public Tuple conreplace(Sequence[] con, int position, Sequence s) throws QueryException;
}
