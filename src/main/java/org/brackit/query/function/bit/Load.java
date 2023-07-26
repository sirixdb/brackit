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
package org.brackit.query.function.bit;

import java.io.IOException;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.atomic.Atomic;
import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.Bits;
import org.brackit.query.function.AbstractFunction;
import org.brackit.query.module.StaticContext;
import org.brackit.query.node.parser.DocumentParser;
import org.brackit.query.node.parser.NodeSubtreeParser;
import org.brackit.query.util.annotation.FunctionAnnotation;
import org.brackit.query.util.io.URIHandler;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Item;
import org.brackit.query.jdm.Iter;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Signature;
import org.brackit.query.jdm.Stream;
import org.brackit.query.jdm.node.NodeCollection;
import org.brackit.query.jdm.type.AtomicType;
import org.brackit.query.jdm.type.Cardinality;
import org.brackit.query.jdm.type.ElementType;
import org.brackit.query.jdm.type.SequenceType;

/**
 * @author Henrique Valer
 * @author Martin Hiller
 * @author Sebastian Baechle
 */
@FunctionAnnotation(description = "Load (external) documents into a collection. "
    + "If explicitly required or if the collection does not exist, " + "a new collection will be created. ",
    parameters = { "$name", "$resources", "$create-new" })
public class Load extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, "load");

  public Load(boolean createNew) {
    this(DEFAULT_NAME, createNew);
  }

  public Load(QNm name, boolean createNew) {
    super(name,
          createNew
              ? new Signature(new SequenceType(ElementType.ELEMENT, Cardinality.ZeroOrOne),
                              new SequenceType(AtomicType.STR, Cardinality.One),
                              new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany))
              : new Signature(new SequenceType(ElementType.ELEMENT, Cardinality.ZeroOrOne),
                              new SequenceType(AtomicType.STR, Cardinality.One),
                              new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
                              new SequenceType(AtomicType.BOOL, Cardinality.One)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    try {
      boolean createNew = args.length != 3 || args[2].booleanValue();
      String name = ((Atomic) args[0]).stringValue();
      Sequence resources = args[1];

      org.brackit.query.jdm.node.NodeStore s = ctx.getNodeStore();
      if (createNew) {
        create(s, name, resources);
      } else {
        try {
          NodeCollection<?> coll = s.lookup(name);
          add(s, coll, resources);
        } catch (DocumentException e) {
          // collection does not exist
          create(s, name, resources);
        }
      }
      // TODO return statistics?
      return null;
    } catch (Exception e) {
      throw new QueryException(e, BitFun.BIT_ADDTOCOLLECTION_INT_ERROR, e.getMessage());
    }
  }

  private void add(org.brackit.query.jdm.node.NodeStore store, NodeCollection<?> coll, Sequence resources)
      throws DocumentException, IOException {
    if (resources instanceof Atomic) {
      String r = ((Atomic) resources).stringValue();
      coll.add(new DocumentParser(URIHandler.getInputStream(r)));
    } else {
      try (ParserStream parsers = new ParserStream(resources)) {
        NodeSubtreeParser parser;
        while ((parser = parsers.next()) != null) {
          coll.add(parser);
        }
      }
    }
  }

  private void create(org.brackit.query.jdm.node.NodeStore store, String name, Sequence resources)
      throws DocumentException, IOException {
    if (resources instanceof Atomic) {
      String r = ((Atomic) resources).stringValue();
      store.create(name, new DocumentParser(URIHandler.getInputStream(r)));
    } else {
      store.create(name, new ParserStream(resources));
    }
  }

  private static class ParserStream implements Stream<NodeSubtreeParser> {
    Iter it;

    public ParserStream(Sequence locs) {
      it = locs.iterate();
    }

    @Override
    public NodeSubtreeParser next() throws DocumentException {
      try {
        Item i = it.next();
        if (i == null) {
          return null;
        }
        if (i instanceof Atomic) {
          String s = ((Atomic) i).stringValue();
          return new DocumentParser(URIHandler.getInputStream(s));
        } else {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Cannot create subtree parser for item of type: %s",
                                   i.itemType());
        }
      } catch (IOException | QueryException e) {
        throw new DocumentException(e);
      }
    }

    @Override
    public void close() {
      it.close();
    }
  }
}