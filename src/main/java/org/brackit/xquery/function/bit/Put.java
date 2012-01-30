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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Put extends AbstractFunction {
	public static final QNm PUT = new QNm(Namespaces.BIT_NSURI,
			Namespaces.BIT_PREFIX, "put");

	public Put(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args)
			throws QueryException {
		URLConnection conn = null;

		try {
			URI uri = new URI(((Str) args[0]).stringValue());
			String targetName = (args.length > 1) ? ((Str) args[1])
					.stringValue() : null;
			String scheme = uri.getScheme();

			DocumentParser parser;

			if ((scheme == null) || (scheme.equals("file"))) {
				String fullPath = uri.getSchemeSpecificPart();
				if (fullPath == null) {
					throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
							"Illegal file name: %s", args[0]);
				}
				if (fullPath.startsWith("//")) {
					fullPath = fullPath.substring(1);
				}
				File xmlFile = new File(fullPath);
				parser = new DocumentParser(xmlFile);
			} else if (scheme.equals("http") || scheme.equals("https")
					|| scheme.equals("ftp") || scheme.equals("jar")) {
				URL url = new URL(((AnyURI) args[0]).stringValue());
				conn = url.openConnection();
				parser = new DocumentParser(new BufferedInputStream(conn
						.getInputStream()));
			} else {
				throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
						"Unsupported protocol: %s", scheme);
			}

			if (targetName == null) {
				targetName = uri.getPath();
				int p = targetName.lastIndexOf('/');
				if (p != -1) {
					targetName = targetName.substring(p);
				}
			}
			if (targetName.startsWith("/")) {
				targetName = targetName.substring(1);
			}

			Collection<?> coll = ctx.getStore().create(targetName, parser);
			return new Str(coll.getName());
		} catch (IOException e) {
			throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
		} catch (URISyntaxException e) {
			throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
		} finally {
			if (conn != null) {
				if (conn != null) {
					if (conn instanceof HttpURLConnection) {
						((HttpURLConnection) conn).disconnect();
					}
					conn = null;
				}
			}
		}
	}
}
