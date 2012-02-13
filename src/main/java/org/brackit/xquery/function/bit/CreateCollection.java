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

import java.util.ArrayList;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.StreamSubtreeParser;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Sequence;

/**
 * 
 * @author Henrique Valer
 * 
 */
public class CreateCollection extends AbstractFunction {

	public static final QNm NAME = new QNm(Namespaces.BIT_NSURI,
			Namespaces.BIT_PREFIX, "create-collection");

	public CreateCollection(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		try {
			String collection = ((Atomic) args[0]).stringValue();
			
			if (args.length == 1) {
				ctx.getStore().create(collection);
			} else {
				
				// initialize collection with documents
				
				ArrayList<SubtreeParser> parserList = new ArrayList<SubtreeParser>();
				Item item = null;
				Iter it = args[1].iterate();
				try {
					while ((item = it.next()) != null) {
						
						SubtreeParser parser = null;
						if (item instanceof Atomic) {
							// take string value as document location
							parser = new DocumentParser(((Atomic) item).stringValue());
						} else {
							// take subtree as new document
							Node<?> root = (Node<?>) item;
							parser = new StreamSubtreeParser(root.getSubtree());
						}
						parserList.add(parser);
					}
				} finally {
					it.close();
				}
				
				// convert to array
				SubtreeParser[] parsers = parserList.toArray(new SubtreeParser[parserList.size()]);
				ctx.getStore().create(collection, parsers);				
			}
			
			return Bool.TRUE;
		} catch (Exception e) {
			throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR,
					e.getMessage());
		}
	}
}