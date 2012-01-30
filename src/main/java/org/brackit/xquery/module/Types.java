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
package org.brackit.xquery.module;

import java.util.HashMap;
import java.util.Map;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.xdm.Type;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Types {
	protected final Map<QNm, Type> atomicTypes = new HashMap<QNm, Type>();

	public Type resolveSchemaType(QNm name) throws QueryException {
		// of course there's no import - we don't support imports yet ;-)
		throw new QueryException(ErrorCode.ERR_UNDEFINED_REFERENCE,
				"No schema import found for namespace '%s'", name
						.getNamespaceURI());
	}

	public Type resolveType(QNm name) throws QueryException {
		Type type = resolveInternal(name);
		if (type != null) {
			return type;
		}
		throw new QueryException(ErrorCode.ERR_UNDEFINED_REFERENCE,
				"Unknown type: '%s'", name);
	}

	public Type resolveAtomicType(QNm name) throws QueryException {
		Type type = resolveInternal(name);
		if (type != null) {
			return type;
		}
		throw new QueryException(ErrorCode.ERR_UNKNOWN_ATOMIC_SCHEMA_TYPE,
				"Unknown atomic schema type: '%s'", name);
	}

	private Type resolveInternal(QNm name) {
		if (Namespaces.XS_NSURI.equals(name.getNamespaceURI())) {
			for (Type type : Type.builtInTypes) {
				if (type.getName().getLocalName().equals(name.getLocalName())) {
					return type;
				}
			}
			return null;
		}

		return atomicTypes.get(name);
	}
}
