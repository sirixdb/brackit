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

import java.net.URI;
import java.net.URISyntaxException;

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
public class AnyURI extends AbstractAtomic {
	public static final AnyURI EMPTY = new AnyURI("", false);

	public final String str;

	public final boolean absolute;

	private class DAnyURI extends AnyURI {
		private final Type type;

		public DAnyURI(String str, Type type, boolean absolute) {
			super(str, absolute);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public AnyURI(String str) throws QueryException {
		if ((str == null) || ((str = Whitespace.collapse(str)).isEmpty())) {
			this.str = "";
			this.absolute = false;
		} else {
			try {
				this.absolute = new URI(str).isAbsolute();
				this.str = str;
			} catch (URISyntaxException e) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Cannot cast '%s' to xs:anyURI", str);
			}
		}
	}

	public AnyURI(String str, boolean absolute) {
		if (str == null)
			str = "";
		this.str = str;
		this.absolute = absolute;
	}

	@Override
	public Type type() {
		return Type.AURI;
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		return new DAnyURI(str, type, absolute);
	}

	@Override
	public boolean booleanValue(QueryContext ctx) throws QueryException {
		return (!str.isEmpty());
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof AnyURI) {
			return (str.compareTo(((AnyURI) other).str));
		}
		if (other instanceof Str) {
			return (str.compareTo(other.stringValue()));
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic atomic) {
		return (str.compareTo(atomic.stringValue()));
	}

	@Override
	public int atomicCode() {
		return Type.STRING_CODE;
	}

	@Override
	public String stringValue() {
		return str;
	}

	public boolean isAbsolute() {
		return absolute;
	}

	@Override
	public int hashCode() {
		return str.hashCode();
	}
}
