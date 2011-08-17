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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class QNm extends AbstractAtomic {
	public final String nsURI;

	public final String prefix;

	public final String localName;

	private class DQnm extends QNm {
		private final Type type;

		public DQnm(String nsURI, String prefix, String localName, Type type) {
			super(nsURI, prefix, localName);
			this.type = type;
		}

		@Override
		public Type type() {
			return this.type;
		}
	}

	public QNm(String namespaceURI, String prefix, String localName) {
		this.nsURI = namespaceURI;
		this.prefix = prefix;
		this.localName = localName;
	}

	public QNm(String namespaceURI, String string) throws QueryException {
		int prefixLength = string.indexOf(":");
		if (prefixLength > -1) {
			if ((prefixLength == 0) || (prefixLength == string.length() - 1)
					|| (string.indexOf(":", prefixLength + 1) != -1)) {
				throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST,
						"Illegal QName: '%s'", string);
			}

			this.prefix = string.substring(0, prefixLength);
			string = string.substring(prefixLength + 1);
		} else {
			prefix = "";
		}
		this.nsURI = namespaceURI;
		this.localName = string;
	}

	public QNm(String string) {
		this.prefix = null;
		this.nsURI = null;
		this.localName = string;
	}

	@Override
	public Type type() {
		return Type.QNM;
	}

	@Override
	public Atomic asType(Type type) throws QueryException {
		return new DQnm(nsURI, prefix, localName, type);
	}

	public String getPrefix() {
		return prefix;
	}

	public String getLocalName() {
		return localName;
	}

	public String getNamespaceURI() {
		return nsURI;
	}

	@Override
	public boolean booleanValue() throws QueryException {
		return (!localName.isEmpty());
	}

	@Override
	public int cmp(Atomic other) throws QueryException {
		if (other instanceof QNm) {
			QNm qName = (QNm) other;

			if (nsURI == null) {
				if (qName.nsURI != null) {
					return -1;
				}
			} else if (qName.nsURI != null) {
				int res = nsURI.compareTo(qName.nsURI);
				if (res != 0) {
					return res;
				}
			} else {
				return 1;
			}

			return localName.compareTo(qName.localName);
		}
		throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
				"Cannot compare '%s' with '%s'", type(), other.type());
	}

	@Override
	protected int atomicCmpInternal(Atomic other) {
		QNm qName = (QNm) other;

		if (nsURI == null) {
			if (qName.nsURI != null) {
				return -1;
			}
		} else if (qName.nsURI != null) {
			int res = nsURI.compareTo(qName.nsURI);
			if (res != 0) {
				return res;
			}
		} else {
			return 1;
		}

		return localName.compareTo(qName.localName);
	}

	@Override
	public int atomicCode() {
		return Type.QNM_CODE;
	}

	@Override
	public String stringValue() {
		return localName;
	}

	@Override
	public int hashCode() {
		return localName.hashCode();
	}

	@Override
	public String toString() {
		return (prefix == null) ? localName : prefix + ":" + localName;
	}
}