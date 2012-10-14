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
package org.brackit.xquery.compiler;

import org.brackit.xquery.atomic.QNm;

/**
 * Collection of brackit-related constants (names, error codes).
 * 
 * @author Sebastian Baechle
 * 
 */
public class Bits {
	public static final String BIT_NSURI = "http://brackit.org/ns/bit";

	public static final String BIT_PREFIX = "bit";

	/*
	 * Variables names for internal compilation processes
	 */
	public static final String FS_NSURI = "http://brackit.org/ns/InternalUseOnly";

	public static final String FS_PREFIX = "fs";

	public static final QNm FS_DOT = new QNm(FS_NSURI, FS_PREFIX, "dot");

	public static final QNm FS_LAST = new QNm(FS_NSURI, FS_PREFIX, "last");

	public static final QNm FS_POSITION = new QNm(FS_NSURI, FS_PREFIX,
			"position");

	public static final QNm FS_PARENT = new QNm(FS_NSURI, FS_PREFIX, "parent");

	public static final QNm FS_FOO = new QNm(FS_NSURI, FS_PREFIX, "foo");

	/*
	 * Error codes for runtime errors and built-in expressions.
	 */
	public static final QNm BIT_ILLEGAL_RECORD_FIELD = new QNm(BIT_NSURI,
			"bit", "BIEX0001");

	public static final QNm BIT_DUPLICATE_RECORD_FIELD = new QNm(BIT_NSURI,
			"bit", "BIEX0002");
}
