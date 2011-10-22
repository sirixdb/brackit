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
package org.brackit.xquery.function.fn;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Stream;

/**
 * Implementation of predefined functions fn:doc($arg1) and
 * fn:doc-available($arg1) as per http://www.w3.org/TR/xpath-functions/#func-doc
 * and http://www.w3.org/TR/xpath-functions/#func-doc-available. Also note
 * correction in
 * http://www.w3.org/XML/2007/qt-errata/xpath-functions-errata.html#E26
 * 
 * @author Sebastian Baechle
 * @author Max Bechtold
 * 
 */
public class Doc extends AbstractFunction {
	private boolean retrieve;

	public Doc(QNm name, boolean retrieve, Signature signature) {
		super(name, signature, true);
		this.retrieve = retrieve;
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args)
			throws QueryException {
		if (args[0] == null) {
			return (retrieve ? null : Bool.FALSE);
		}

		Str name = (Str) args[0];

		try {
			Node<?> document;
			if (name.stringValue().isEmpty()) {
				// Implementation defined: Handle empty name in the sense of
				// fn:doc() which is in fact not defined by XQuery
				document = ctx.getDefaultDocument();

				if (document == null) {
					if (retrieve) {
						throw new QueryException(
								ErrorCode.ERR_DOCUMENT_NOT_FOUND,
								"No default document defined.");
					} else {
						return Bool.FALSE;
					}
				}

				return document;
			} else {
				Collection<?> collection = ctx.getStore().lookup(
						name.stringValue());
				Stream<? extends Node<?>> docs = collection.getDocuments();
				try {
					document = (Node<?>) docs.next();

					if (document == null) {
						if (retrieve) {
							throw new QueryException(
									ErrorCode.ERR_DOCUMENT_NOT_FOUND,
									"Empty collection");
						} else {
							return Bool.FALSE;
						}
					}

					if (docs.next() != null) {
						throw new QueryException(
								ErrorCode.ERR_DOCUMENT_NOT_FOUND,
								"Collection %s contains more than one document",
								name);
					}

					if (retrieve) {
						return document;
					} else {
						return Bool.FALSE;
					}
				} finally {
					docs.close();
				}
			}
		} catch (DocumentException e) {
			if (retrieve) {
				throw new QueryException(e, ErrorCode.ERR_DOCUMENT_NOT_FOUND,
						"Document '%s' not found.", name);
			} else {
				return Bool.FALSE;
			}
		}
	}
}
