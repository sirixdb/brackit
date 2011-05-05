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

import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Hex extends AbstractAtomic {
	private final byte[] bytes;

	public Hex(byte[] bytes) {
		this.bytes = bytes;
	}

	public Hex(String str) throws QueryException {
		str = Whitespace.collapseTrimOnly(str);
		if ((str.length() & 1) != 0)
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:hexBinary", str);

		byte[] bytes = new byte[str.length() >>> 1];
		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			char c = str.charAt(j++);
			int v = hex(str, c);
			c = str.charAt(j++);
			bytes[i] = (byte) ((v << 4) | hex(str, c));
			// System.out.println("Byte " + i + " : " + (bytes[i] & 255));
		}
		this.bytes = bytes;
	}

	private int hex(String str, char c) throws QueryException {
		int v;
		if ((c >= '0') && (c <= '9'))
			v = c - 48;
		else if ((c >= 'A') && (c <= 'F'))
			v = c - 55;
		else if ((c >= 'a') && (c <= 'f'))
			v = c - 87;
		else
			throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
					"Cannot cast %s to xs:hexBinary", str);
		return v;
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
	}

	@Override
	public int cmp(Atomic atomic) throws QueryException {
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s with '%s'", type(), atomic.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic atomic) {
		byte[] bytes2 = ((Hex) atomic).bytes;
		for (int i = 0; i < Math.min(bytes.length, bytes2.length); i++) {
			if (bytes[i] != bytes2[i]) {
				return (bytes[i] & 255) - (bytes2[i] & 255);
			}
		}
		return bytes.length - bytes2.length;
	}

	@Override
	public int atomicCode() {
		return Type.HEX_CODE;
	}

	@Override
	public int hashCode() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean eq(Atomic atomic) throws QueryException {
		if (!(atomic instanceof Hex)) {
			throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
					"Cannot compare '%s with '%s'", type(), atomic.type());
		}
		return Arrays.equals(bytes, ((Hex) atomic).bytes);
	}

	@Override
	public String stringValue() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 255;
			int v1 = v >>> 4;
			int v2 = v & 15;
			char c1 = (char) (v1 < 10 ? v1 + 48 : v1 + 55);
			char c2 = (char) (v2 < 10 ? v2 + 48 : v2 + 55);
			out.append(c1);
			out.append(c2);
		}
		return out.toString();
	}

	@Override
	public Type type() {
		return Type.HEX;
	}

	@Override
	public boolean booleanValue(QueryContext ctx) throws QueryException {
		throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
				"Effective boolean value of '%s' is undefined.", type());
	}
}