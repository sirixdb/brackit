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

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.parser.CollectionParser;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.ParserStream;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.util.annotation.FunctionAnnotation;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.type.AtomicType;
import org.brackit.xquery.xdm.type.Cardinality;
import org.brackit.xquery.xdm.type.ElementType;
import org.brackit.xquery.xdm.type.SequenceType;

/**
 * 
 * @author Henrique Valer
 * 
 */
@FunctionAnnotation(description = "Load (external) documents into a collection. "
		+ "If explicitly required or if the collection does not exist, "
		+ "a new collection will be created. ", parameters = { "$name",
		"$resources", "$create-new" })
public class Store extends AbstractFunction {

	public static final QNm DEFAULT_NAME = new QNm(Namespaces.BIT_NSURI,
			Namespaces.BIT_PREFIX, "load");

	public Store(boolean createNew) {
		this(DEFAULT_NAME, createNew);
	}

	public Store(QNm name, boolean createNew) {
		super(name, createNew ? new Signature(new SequenceType(
				ElementType.ELEMENT, Cardinality.ZeroOrOne), new SequenceType(
				AtomicType.STR, Cardinality.One), new SequenceType(
				AtomicType.STR, Cardinality.ZeroOrMany)) : new Signature(
				new SequenceType(ElementType.ELEMENT, Cardinality.ZeroOrOne),
				new SequenceType(AtomicType.STR, Cardinality.One),
				new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
				new SequenceType(AtomicType.BOOL, Cardinality.One)), true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		try {
			boolean createNew = (args.length != 3) ? true
					: ((Atomic) (args[2])).booleanValue();
			String name = ((Atomic) args[0]).stringValue();
			Sequence resources = args[1];
			SubtreeParser parser;

			if (resources instanceof Atomic) {
				String s = ((Atomic) resources).stringValue();
				parser = new DocumentParser(URIHandler.getInputStream(s));
			} else {
				parser = new CollectionParser(new ParserStream(resources));
			}

			org.brackit.xquery.xdm.Store s = ctx.getStore();
			if (createNew) {
				s.create(name, parser);
			} else {
				try {
					Collection<?> coll = s.lookup(name);
					coll.add(parser);
				} catch (DocumentException e) {
					// collection does not exist
					s.create(name, parser);
				}
			}
			// TODO return statistics?
			return null;
		} catch (Exception e) {
			throw new QueryException(e, BitError.BIT_ADDTOCOLLECTION_INT_ERROR,
					e.getMessage());
		}
	}
}