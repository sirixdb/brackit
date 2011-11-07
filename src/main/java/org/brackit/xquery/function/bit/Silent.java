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
package org.brackit.xquery.function.bit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.Int64;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Silent extends AbstractFunction {

	public static final QNm SILENT = new QNm(Namespaces.BIT_NSURI,
			Namespaces.BIT_PREFIX, "silent");

	private static class CountStream extends OutputStream {
		long count = 0;

		@Override
		public void write(int b) throws IOException {
			count++;
		}
	}

	public Silent() {
		super(SILENT, new Signature(new SequenceType(AtomicType.INR,
				Cardinality.One), SequenceType.ITEM_SEQUENCE), true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args)
			throws QueryException {
		Sequence sequence = args[0];
		if (sequence == null) {
			return Int32.ZERO;
		}
		CountStream out = new CountStream();
		PrintWriter pw = new PrintWriter(out);
		serialize(sequence, false, pw);
		return new Int64(out.count);
	}

	private void serialize(Sequence result, boolean prettyPrint, PrintWriter out)
			throws QueryException {
		if (result == null) {
			return;
		}

		boolean first = true;
		SubtreePrinter printer = new SubtreePrinter(out);
		printer.setPrettyPrint(prettyPrint);
		printer.setAutoFlush(false);
		Item item;
		Iter it = result.iterate();
		try {
			while ((item = it.next()) != null) {
				if (item instanceof Node<?>) {
					Node<?> node = (Node<?>) item;
					Kind kind = node.getKind();

					if (kind == Kind.ATTRIBUTE) {
						throw new QueryException(
								ErrorCode.ERR_SERIALIZE_ATTRIBUTE_OR_NAMESPACE_NODE);
					}
					if (kind == Kind.DOCUMENT) {
						node = node.getFirstChild();
						while (node.getKind() != Kind.ELEMENT) {
							node = node.getNextSibling();
						}
					}

					printer.print(node);
					first = true;
				} else {
					if (!first) {
						out.write(" ");
					}
					out.write(item.toString());
					first = false;
				}
			}
		} finally {
			printer.flush();
			out.flush();
			it.close();
		}
	}
}