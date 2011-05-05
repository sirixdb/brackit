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
package org.brackit.xquery.xdm;

import java.util.regex.Pattern;

import org.brackit.xquery.atomic.Atomic;

/**
 * Constraining facets of a type.
 * 
 * @author Sebastian Baechle
 * 
 */
public class Facets {
	public enum WS {
		PRESERVE, REPLACE, COLLAPSE
	};

	/**
	 * Number of characters (xs:string, xs:anyURI), octets (xs:hexBinary,
	 * xs:base64Binary), or list items
	 */
	public final int length;

	/**
	 * Maximal number of characters (xs:string, xs:anyURI), octets
	 * (xs:hexBinary, xs:base64Binary), or list items
	 */
	public final int maxLength;

	/**
	 * Minimal number of characters (xs:string, xs:anyURI), octets
	 * (xs:hexBinary, xs:base64Binary), or list items
	 */
	public final int minLength;

	/**
	 * Regular expression pattern constraining the lexical space.
	 */
	public final Pattern pattern;

	/**
	 * Fixed number of values constraining the value space.
	 */
	public final Atomic[] enumeration;

	/**
	 * Whitespace normalization constraining the lexical space of types derived
	 * from xs:string.
	 */
	public final WS whiltespace;

	/**
	 * Upper bound included value of the value space.
	 */
	public final Atomic maxInclusive;

	/**
	 * Lower bound included value of the value space.
	 */
	public final Atomic minInclusive;

	/**
	 * Upper bound excluded value of the value space.
	 */
	public final Atomic maxExclusive;

	/**
	 * Lower bound excluded value of the value space.
	 */
	public final Atomic minExclusive;

	/**
	 * Maximal number of digits for the lexical representation of types derived
	 * from xs:decimal excluding leading and trailing zeros.
	 */
	public final int totalDigits;

	/**
	 * Maximal number of digits right of the decimal point for the lexical
	 * representation of types derived from xs:decimal excluding trailing zeros.
	 */
	public final int fractionDigits;

	public Facets(int length, int maxLength, int minLength, String pattern,
			Atomic[] enumeration, WS whiltespace, Atomic maxInclusive,
			Atomic minInclusive, Atomic maxExclusive, Atomic minExclusive,
			int totalDigits, int fractionDigits) {
		this.length = length;
		this.maxLength = maxLength;
		this.minLength = minLength;
		this.pattern = (pattern != null) ? Pattern.compile(pattern) : null;
		this.enumeration = enumeration;
		this.whiltespace = whiltespace;
		this.maxInclusive = maxInclusive;
		this.minInclusive = minInclusive;
		this.maxExclusive = maxExclusive;
		this.minExclusive = minExclusive;
		this.totalDigits = totalDigits;
		this.fractionDigits = fractionDigits;
	}
}
