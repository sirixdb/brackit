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
package org.brackit.xquery.function.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AnyItemType;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Writeline extends AbstractFunction {
	public static final QNm WRITELINE = new QNm(Namespaces.IO_NSURI,
			Namespaces.IO_PREFIX, "writeline");

	public Writeline() {
		super(WRITELINE, new Signature(new SequenceType(AtomicType.INR,
				Cardinality.One), new SequenceType(AtomicType.AURI,
				Cardinality.One), new SequenceType(AnyItemType.ANY,
				Cardinality.ZeroOrMany)), true);
	}

	@Override
	public Sequence execute(QueryContext ctx, final Sequence[] args)
			throws QueryException {
		if (args[1] == null) {
			return Int32.ZERO;
		}

		try {
			IntNumeric count = Int32.ZERO;
			BufferedWriter out = createInput(args);
			Iter it = args[1].iterate();
			try {
				Item item;
				while ((item = it.next()) != null) {
					out.write(item.toString());
					out.write('\n');
					count = count.inc();
				}
			} finally {
				it.close();
			}
			out.close();

			return count;
		} catch (IOException e) {
			throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
		} catch (URISyntaxException e) {
			throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
		}
	}

	private BufferedWriter createInput(final Sequence[] args)
			throws QueryException, IOException, URISyntaxException {
		URI uri = new URI(((AnyURI) args[0]).stringValue());
		String scheme = uri.getScheme();

		DocumentParser parser;

		if ((scheme == null) || (scheme.equals("file"))) {
			// handle files locally
			String fullPath = uri.getSchemeSpecificPart();
			if (fullPath == null) {
				throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
						"Illegal file name: %s", args[0]);
			}
			if (fullPath.startsWith("//")) {
				fullPath = fullPath.substring(1);
			}
			File f = new File(fullPath);
			return new BufferedWriter(new FileWriter(f));
		} else if (scheme.equals("http") || scheme.equals("https")
				|| scheme.equals("ftp") || scheme.equals("jar")) {
			URL url = new URL(((AnyURI) args[0]).stringValue());
			URLConnection conn = url.openConnection();
			return new BufferedWriter(new OutputStreamWriter(conn
					.getOutputStream()));
		} else {
			throw new QueryException(ErrorCode.BIT_DYN_INT_ERROR,
					"Unsupported protocol: %s", scheme);
		}
	}

	public static void main(String[] args) throws Exception {
		Writeline rl = new Writeline();
		AnyURI uri = new AnyURI("/data/10k.dat");// new
		// AnyURI("http://lgis.informatik.uni-kl.de");
		Sequence s = rl.execute(null, new Sequence[] { uri });
		Iter it = s.iterate();
		Item line;
		while ((line = it.next()) != null) {
			System.out.println(line);
		}
		it.close();
	}
}