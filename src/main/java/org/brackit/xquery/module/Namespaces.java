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
package org.brackit.xquery.module;

import java.util.HashMap;
import java.util.Map;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Namespaces {
	public static final String LOCAL_NSURI = "http://www.w3.org/2005/xquery-local-functions";

	public static final String FN_NSURI = "http://www.w3.org/2005/xpath-functions";

	public static final String XSI_NSURI = "http://www.w3.org/2001/XMLSchema-instance";

	public static final String XS_NSURI = "http://www.w3.org/2001/XMLSchema";

	public static final String XML_NSURI = "http://www.w3.org/XML/1998/namespace";

	public static final String XMLNS_NSURI = "http://www.w3.org/2000/xmlns";

	public static final String ERR_NSURI = "http://www.w3.org/2005/xqt-errors";

	public static final String BIT_NSURI = "http://brackit.org/ns/bit";

	public static final String IO_NSURI = "http://brackit.org/ns/io";

	public static final String LOCAL_PREFIX = "local";

	public static final String FN_PREFIX = "fn";

	public static final String XSI_PREFIX = "xsi";

	public static final String XS_PREFIX = "xs";

	public static final String XML_PREFIX = "xml";

	public static final String BIT_PREFIX = "bit";

	public static final String IO_PREFIX = "io";

	public static final String XMLNS_PREFIX = "xmlns";

	public static final String FS_PREFIX = "fs";

	public static final String ERR_PREFIX = "err";

	public static final String FS_NSURI = "FormalSemanticsOnly";

	public static final QNm FS_DOT = new QNm(FS_NSURI, FS_PREFIX, "dot");

	public static final QNm FS_LAST = new QNm(FS_NSURI, FS_PREFIX, "last");

	public static final QNm FS_POSITION = new QNm(FS_NSURI, FS_PREFIX,
			"position");

	public static final QNm FS_PARENT = new QNm(FS_NSURI, FS_PREFIX, "parent");

	protected static final Map<String, NamespaceDecl> predefined = new HashMap<String, NamespaceDecl>();

	protected final Map<String, NamespaceDecl> namespaces = new HashMap<String, NamespaceDecl>();

	protected String defaultFunctionNamespace = FN_NSURI;

	protected String defaultElementNamespace = null;

	static {
		predefine(XML_PREFIX, XML_NSURI);
		predefine(XS_PREFIX, XS_NSURI);
		predefine(XSI_PREFIX, XSI_NSURI);
		predefine(FN_PREFIX, FN_NSURI);
		predefine(LOCAL_PREFIX, LOCAL_NSURI);
		predefine(ERR_PREFIX, ERR_NSURI);
		predefine(BIT_PREFIX, BIT_NSURI);
		predefine(IO_PREFIX, IO_NSURI);
	}

	public static void predefine(String prefix, String nsUri) {
		predefined.put(prefix, new NamespaceDecl(prefix, nsUri));
	}

	public String getDefaultFunctionNamespace() {
		return defaultFunctionNamespace;
	}

	public void setDefaultFunctionNamespace(String defaultFunctionNamespace) {
		this.defaultFunctionNamespace = defaultFunctionNamespace;
	}

	public String getDefaultElementNamespace() {
		return defaultElementNamespace;
	}

	public void setDefaultElementNamespace(String defaultElementNamespace) {
		this.defaultElementNamespace = defaultElementNamespace;
	}

	public void declare(String prefix, String nsURI) throws QueryException {
		if ((XML_PREFIX.equals(prefix)) || (XMLNS_PREFIX.equals(prefix))) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"The prefix '%s' must not be used in a namespace declaration");
		} else if (nsURI.isEmpty()) {
			namespaces.remove(prefix);
		} else if ((XML_NSURI.equals(nsURI)) || (XMLNS_NSURI.equals(nsURI))) {
			throw new QueryException(ErrorCode.ERR_ILLEGAL_NAMESPACE_DECL,
					"The namespace URI '%s' must not be used in a namespace declaration");
		} else {
			NamespaceDecl pNsDecl = namespaces.put(prefix, new NamespaceDecl(
					prefix, nsURI));
			if ((pNsDecl != null) && (!nsURI.equals(pNsDecl.getUri()))) {
				throw new QueryException(
						ErrorCode.ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX,
						"Namespace prefix '%s' is already bound to '%s",
						prefix, pNsDecl.getUri());
			}
		}
	}

	public String resolve(String prefix) throws QueryException {
		NamespaceDecl nsDecl = namespaces.get(prefix);

		if (nsDecl != null) {
			if (nsDecl.getUri().isEmpty()) {
				throw new QueryException(
						ErrorCode.ERR_UNDEFINED_NAMESPACE_PREFIX,
						"Undefined namespace prefix: '%s'", prefix);
			}
			return nsDecl.getUri();
		}

		nsDecl = predefined.get(prefix);
		if (nsDecl != null) {
			return nsDecl.getUri();
		}

		throw new QueryException(ErrorCode.ERR_UNDEFINED_NAMESPACE_PREFIX,
				"Undefined namespace prefix: '%s'", prefix);
	}

	public QNm expand(String prefix, String localname) throws QueryException {
		return expand(prefix, localname, "");
	}

	public QNm expandElement(String prefix, String localname) throws QueryException {
		return expand(prefix, localname, defaultElementNamespace);
	}

	public QNm expandFunction(String prefix, String localname) throws QueryException {
		return expand(prefix, localname, defaultFunctionNamespace);
	}

	public QNm expand(String prefix, String localname, String defaultNSURI) throws QueryException {
		String namespaceURI = (prefix != null) ? resolve(prefix) : defaultNSURI;
		return new QNm(namespaceURI, prefix, localname);
	}
}
