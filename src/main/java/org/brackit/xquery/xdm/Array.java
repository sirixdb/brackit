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
package org.brackit.xquery.xdm;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.IntNumeric;

/**
 * <p>
 * Representation of an ordered sequence of values.
 * </p>
 * <p>
 * CAVEAT: This representation allows to build arrays
 * of arbitrary sequences including nodes, functions,
 * the empty sequence and even nested arrays as values.   
 * </p>
 * <p>
 * The relaxed data representation does not conform to
 * XQuery's data model but facilitates a more flexible
 * data representation within the query engine. 
 * </p>
 * <p>
 * Since it is not possible to create or validate arrays,
 * i.e., list types in vanilla XQuery/XPath/XML (optionally 
 * with XMLSchema support), we must not take special care.
 * </p>
 * 
 * @author Sebastian Baechle
 * 
 */
public interface Array extends ListOrUnion {
	/**
	 * Returns the value at the given position. 
	 */
	public Sequence at(IntNumeric i) throws QueryException;
	
	/**
	 * Returns the value at the given position.
	 */
	public Sequence at(int i) throws QueryException;

	/**
	 * Returns the length of this array.
	 */
	public IntNumeric length() throws QueryException;
	
	/**
	 * Returns the length of this array.
	 */
	public int len() throws QueryException;

	/**
	 * Creates a slice of this array in the given boundaries. 
	 */
	public Array range(IntNumeric from, IntNumeric to) throws QueryException;
}
