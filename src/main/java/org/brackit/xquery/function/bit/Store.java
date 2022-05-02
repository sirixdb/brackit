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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.Bits;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.node.parser.StreamSubtreeParser;
import org.brackit.xquery.node.parser.SubtreeHandler;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.util.annotation.FunctionAnnotation;
import org.brackit.xquery.xdm.*;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.node.NodeCollection;
import org.brackit.xquery.xdm.type.*;

/**
 * @author Henrique Valer
 * @author Martin Hiller
 * @author Sebastian Baechle
 */
@FunctionAnnotation(description = "Store the given fragments in a collection. "
    + "If explicitly required or if the collection does not exist, "
    + "a new collection will be created. ", parameters = { "$name", "$fragments", "$create-new" })
public class Store extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, "store");

  public Store(boolean createNew) {
    this(DEFAULT_NAME, createNew);
  }

  public Store(QNm name, boolean createNew) {
    super(name,
          createNew
              ? new Signature(new SequenceType(ElementType.ELEMENT, Cardinality.ZeroOrOne),
                              new SequenceType(AtomicType.STR, Cardinality.One),
                              new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrMany))
              : new Signature(new SequenceType(ElementType.ELEMENT, Cardinality.ZeroOrOne),
                              new SequenceType(AtomicType.STR, Cardinality.One),
                              new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrMany),
                              new SequenceType(AtomicType.BOOL, Cardinality.One)),
          true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    try {
      final boolean createNew = args.length != 3 || args[2].booleanValue();
      final String name = ((Atomic) args[0]).stringValue();
      final Sequence nodes = args[1];

      org.brackit.xquery.xdm.node.NodeStore s = ctx.getNodeStore();
      if (createNew) {
        create(s, name, nodes);
      } else {
        try {
          final NodeCollection<?> coll = s.lookup(name);
          add(coll, nodes);
        } catch (DocumentException e) {
          // collection does not exist
          create(s, name, nodes);
        }
      }
      // TODO return statistics?
      return null;
    } catch (Exception e) {
      throw new QueryException(e, BitFun.BIT_ADDTOCOLLECTION_INT_ERROR, e.getMessage());
    }
  }

  private void add(NodeCollection<?> coll, Sequence nodes)
      throws DocumentException {

    if (nodes instanceof Node<?> n) {
      coll.add(new StoreParser(n));
    } else {
      try (ParserStream parsers = new ParserStream(nodes)) {
        SubtreeParser parser;
        while ((parser = parsers.next()) != null) {
          coll.add(parser);
        }
      }
    }
  }

  private void create(org.brackit.xquery.xdm.node.NodeStore store, String name, Sequence nodes)
      throws DocumentException {
    if (nodes instanceof Node<?> n) {
      store.create(name, new StoreParser(n));
    } else {
      store.create(name, new ParserStream(nodes));
    }
  }

  private static class StoreParser extends StreamSubtreeParser {
    private final boolean intercept;

    public StoreParser(Node<?> node) throws DocumentException {
      super(node.getSubtree());
      intercept = (node.getKind() != Kind.DOCUMENT);
    }

    @Override
    public void parse(SubtreeHandler handler) throws DocumentException {
      if (intercept) {
        handler = new InterceptorHandler(handler);
      }
      super.parse(handler);
    }
  }

  private static class InterceptorHandler implements SubtreeHandler {
    private final SubtreeHandler handler;

    public InterceptorHandler(SubtreeHandler handler) {
      this.handler = handler;
    }

    public void beginFragment() throws DocumentException {
      handler.beginFragment();
      handler.startDocument();
    }

    public void endFragment() throws DocumentException {
      handler.endDocument();
      handler.endFragment();
    }

    public void startDocument() throws DocumentException {
      handler.startDocument();
    }

    public void endDocument() throws DocumentException {
      handler.endDocument();
    }

    public void text(Atomic content) throws DocumentException {
      handler.text(content);
    }

    public void comment(Atomic content) throws DocumentException {
      handler.comment(content);
    }

    public void processingInstruction(QNm target, Atomic content) throws DocumentException {
      handler.processingInstruction(target, content);
    }

    public void startMapping(String prefix, String uri) throws DocumentException {
      handler.startMapping(prefix, uri);
    }

    public void endMapping(String prefix) throws DocumentException {
      handler.endMapping(prefix);
    }

    public void startElement(QNm name) throws DocumentException {
      handler.startElement(name);
    }

    public void endElement(QNm name) throws DocumentException {
      handler.endElement(name);
    }

    public void attribute(QNm name, Atomic value) throws DocumentException {
      handler.attribute(name, value);
    }

    public void begin() throws DocumentException {
      handler.begin();
    }

    public void end() throws DocumentException {
      handler.end();
    }

    public void fail() throws DocumentException {
      handler.fail();
    }
  }

  private static class ParserStream implements Stream<SubtreeParser> {
    Iter it;

    public ParserStream(Sequence locs) {
      it = locs.iterate();
    }

    @Override
    public SubtreeParser next() throws DocumentException {
      try {
        Item i = it.next();
        if (i == null) {
          return null;
        }
        if (i instanceof Node<?> n) {
          return new StoreParser(n);
        } else {
          throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                                   "Cannot create subtree parser for item of type: %s",
                                   i.itemType());
        }
      } catch (QueryException e) {
        throw new DocumentException(e);
      }
    }

    @Override
    public void close() {
      it.close();
    }
  }
}