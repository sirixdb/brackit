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
package org.brackit.xquery.compiler.parser;

import java.util.Map;
import java.util.TreeMap;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.module.Namespaces;

/**
 * @author Sebastian Baechle
 * 
 */
public class StaticNamespaces {

	private static final class Scope {
		private final Scope parent;
		private Map<String, String> localNS;

		Scope(Scope parent) {
			this.parent = parent;
		}

		String resolve(String prefix) throws QueryException {
			Scope s = this;
			while (s != null) {
				if (localNS != null) {
					String uri = localNS.get(prefix);
					if (uri != null) {
						return uri;
					}
				}
				s = s.parent;
			}
			return null;
		}

		QNm expand(String prefix, String localname, String defaultNSURI)
				throws QueryException {
			String namespaceURI = (prefix != null) ? resolve(prefix)
					: defaultNSURI;
			return new QNm(namespaceURI, prefix, localname);
		}

		void declare(String prefix, String uri) throws QueryException {
			if (localNS == null) {
				localNS = new TreeMap<String, String>();
			}
			if (localNS.put(prefix, uri) != null) {
				throw new QueryException(
						ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
						"Namespace prefix '%s' is already bound to '%s",
						prefix, uri);
			}
		}
	}

	private final Namespaces ns;

	private Scope current;

	public StaticNamespaces() {
		this.ns = new Namespaces();
	}

	void declare(String prefix, String uri) throws QueryException {
		if (current != null) {
			current.declare(prefix, uri);
		} else {
			ns.declare(prefix, uri);
		}
	}

	void openScope() {
		current = new Scope(current);
	}

	void closeScope() {
		current = current.parent;
	}

	void setDefaultElementNamespace(String uri) {
		ns.setDefaultElementNamespace(uri);
	}

	void setDefaultFunctionNamespace(String uri) {
		ns.setDefaultFunctionNamespace(uri);
	}

	QNm expand(String prefix, String localname) throws QueryException {
		if (current != null) {
			String p = (prefix == null) ? "" : prefix;
			QNm resolved = current.expand(p, localname, "");
			if (resolved != null) {
				return resolved;
			}
		}
		return ns.expand(prefix, localname);
	}

	QNm expandElement(String prefix, String localname) throws QueryException {
		if (current != null) {
			String p = (prefix == null) ? "" : prefix;
			QNm resolved = current.expand(p, localname, ns
					.getDefaultElementNamespace());
			if (resolved != null) {
				return resolved;
			}
		}
		return ns.expandElement(prefix, localname);
	}

	QNm expandFunction(String prefix, String localname) throws QueryException {
		if (current != null) {
			String p = (prefix == null) ? "" : prefix;
			QNm resolved = current.expand(p, localname, ns
					.getDefaultFunctionNamespace());
			if (resolved != null) {
				return resolved;
			}
		}
		return ns.expandFunction(prefix, localname);
	}
}
