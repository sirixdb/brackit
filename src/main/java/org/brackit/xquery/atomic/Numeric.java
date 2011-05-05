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
package org.brackit.xquery.atomic;

import java.math.BigDecimal;

import org.brackit.xquery.QueryException;

/**
 * Root of an interface-based type hierarchy that mimics the relationships
 * between numeric types (xs:double, xs:float, xs:decimal and xs:integer) in
 * XQuery and defines operations on numeric values as described in "XQuery 1.0
 * and XPath 2.0 Functions and Operators: 6.2 Operators on Numeric Values".
 * 
 * @author Sebastian Baechle
 * 
 */
public interface Numeric extends Atomic {
	public double doubleValue();

	public float floatValue();

	public BigDecimal integerValue();

	public BigDecimal decimalValue();

	public long longValue();

	public int intValue();

	public Numeric add(Numeric other) throws QueryException;

	public Numeric subtract(Numeric other) throws QueryException;

	public Numeric multiply(Numeric other) throws QueryException;

	public Numeric div(Numeric other) throws QueryException;

	public Numeric idiv(Numeric other) throws QueryException;

	public Numeric mod(Numeric other) throws QueryException;

	public Numeric negate() throws QueryException;

	public Numeric round() throws QueryException;

	public Numeric abs() throws QueryException;

	public Numeric floor() throws QueryException;

	public Numeric ceiling() throws QueryException;

	public Numeric roundHalfToEven(int precision) throws QueryException;
}
