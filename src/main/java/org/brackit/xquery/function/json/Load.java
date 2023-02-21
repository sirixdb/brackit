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
package org.brackit.xquery.function.json;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.function.bit.BitFun;
import org.brackit.xquery.jsonitem.ParserStream;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.annotation.FunctionAnnotation;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Sequence;
import org.brackit.xquery.jdm.Signature;
import org.brackit.xquery.jdm.json.JsonCollection;
import org.brackit.xquery.jdm.json.JsonStore;
import org.brackit.xquery.jdm.type.AnyJsonItemType;
import org.brackit.xquery.jdm.type.AtomicType;
import org.brackit.xquery.jdm.type.Cardinality;
import org.brackit.xquery.jdm.type.SequenceType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Johannes Lichtenberger
 */
@FunctionAnnotation(description = "Load (external) documents into a collection. "
    + "If explicitly required or if the collection does not exist, "
    + "a new collection will be created. ", parameters = { "$name", "$resources", "$create-new" })
public final class Load extends AbstractFunction {

  public static final QNm DEFAULT_NAME = new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "load");

  public Load(boolean createNew) {
    this(DEFAULT_NAME, createNew);
  }

  public Load(QNm name, boolean createNew) {
    super(name,
          createNew
              ? new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM, Cardinality.ZeroOrOne),
                              new SequenceType(AtomicType.STR, Cardinality.One),
                              new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany))
              : new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM, Cardinality.ZeroOrOne),
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

      final var s = ctx.getJsonItemStore();
      if (createNew) {
        create(s, name, resources);
      } else {
        try {
          JsonCollection<?> coll = s.lookup(name);
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

  private void add(JsonStore store, JsonCollection<?> coll, Sequence resources)
      throws DocumentException, IOException {
    if (resources instanceof Atomic) {
      String r = ((Atomic) resources).stringValue();
      coll.add(new String(URIHandler.getInputStream(r).readAllBytes(), StandardCharsets.UTF_8));
    } else {
      try (ParserStream parsers = new ParserStream(resources)) {
        String json;
        while ((json = parsers.next()) != null) {
          coll.add(json);
        }
      }
    }
  }

  private void create(JsonStore store, String name, Sequence resources)
      throws DocumentException, IOException {
    if (resources instanceof Atomic) {
      String r = ((Atomic) resources).stringValue();
      store.create(name, new String(URIHandler.getInputStream(r).readAllBytes(), StandardCharsets.UTF_8));
    } else {
      try (final var parsers = new ParserStream(resources)) {
        String json;
        while ((json = parsers.next()) != null) {
          store.create(name, json);
        }
      }
    }
  }
}