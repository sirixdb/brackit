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
package org.brackit.xquery.function.bit;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.Bits;

/**
 * @author Sebastian Baechle
 * 
 */
public class BitFun {
	public static final Every EVERY_FUNC = new Every();

	public static final Some SOME_FUNC = new Some();

	/**
	 * Errors for the predefined bit functions
	 */
	public static final QNm BIT_ADDTOCOLLECTION_INT_ERROR = new QNm(
			Bits.BIT_NSURI, Bits.BIT_PREFIX, "BIBI0001");
	public static final QNm BIT_CREATECOLLECTION_INT_ERROR = new QNm(
			Bits.BIT_NSURI, Bits.BIT_PREFIX, "BIBI0002");
	public static final QNm BIT_DROPCOLLECTION_INT_ERROR = new QNm(
			Bits.BIT_NSURI, Bits.BIT_PREFIX, "BIBI0003");
	public static final QNm BIT_EVAL_INT_ERROR = new QNm(Bits.BIT_NSURI,
			Bits.BIT_PREFIX, "BIT0004");
	public static final QNm BIT_EXISTCOLLECTION_INT_ERROR = new QNm(
			Bits.BIT_NSURI, Bits.BIT_PREFIX, "BIBI0005");
	public static final QNm BIT_MAKEDIRECTORY_INT_ERROR = new QNm(
			Bits.BIT_NSURI, Bits.BIT_PREFIX, "BIBI0007");
	public static final QNm BIT_STOREDOC_INT_ERROR = new QNm(Bits.BIT_NSURI,
			Bits.BIT_PREFIX, "BIBI0008");
}