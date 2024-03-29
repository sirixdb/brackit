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
package io.brackit.query.function.json;

import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.type.AnyJsonItemType;
import io.brackit.query.jdm.type.AtomicType;
import io.brackit.query.jdm.type.Cardinality;
import io.brackit.query.jdm.type.SequenceType;
import io.brackit.query.module.Functions;

/**
 * @author Sebastian Baechle
 */
public final class JSONFun {
  public static final String JSON_NSURI = "http://jsoniq.org/functions";

  public static final String JSON_PREFIX = "jn";

  public static final QNm ERR_PARSING_ERROR = new QNm(JSON_NSURI, JSON_PREFIX, "BIJS0001");

  public static final QNm JSON_NULL = new QNm(JSON_NSURI, JSON_PREFIX, "null");

  public static final Keys KEYS_FUNC = new Keys();

  public static final Doc DOC_FUNC = new Doc(new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "json-doc"),
                                             true,
                                             new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM,
                                                                            Cardinality.ZeroOrOne),
                                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne)));
  public static final Doc DOC_FUNC_2 = new Doc(new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_PREFIX, "json-doc"),
                                               true,
                                               new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM,
                                                                              Cardinality.ZeroOrOne),
                                                             new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                             new SequenceType(AtomicType.INT, Cardinality.ZeroOrOne)));
  public static final Doc DOC_AVAILABLE_FUNC = new Doc(new QNm(JSONFun.JSON_NSURI,
                                                               JSONFun.JSON_PREFIX,
                                                               "json-doc-available"),
                                                       false,
                                                       new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                                                     new SequenceType(AtomicType.STR,
                                                                                      Cardinality.ZeroOrOne)));

  public static final Collection COLLECTION_FUNC = new Collection(new QNm(JSONFun.JSON_NSURI,
                                                                          JSONFun.JSON_PREFIX,
                                                                          "collection"),
                                                                  new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM,
                                                                                                 Cardinality.ZeroOrMany)));
  public static final Collection COLLECTION_FUNC_2 = new Collection(new QNm(JSONFun.JSON_NSURI,
                                                                            JSONFun.JSON_PREFIX,
                                                                            "collection"),
                                                                    new Signature(new SequenceType(AnyJsonItemType.ANY_JSON_ITEM,
                                                                                                   Cardinality.ZeroOrMany),
                                                                                  new SequenceType(AtomicType.STR,
                                                                                                   Cardinality.ZeroOrOne)));

  public static final Size SIZE_FUNC = new Size();

  public static final Load LOAD = new Load(true);

  public static final Load LOAD_2 = new Load(false);

  public static void register() {
    // dummy function to cause static block
    // to be executed exactly once
  }

  static {
    Functions.predefine(KEYS_FUNC);
    Functions.predefine(DOC_FUNC);
    Functions.predefine(DOC_FUNC_2);
    Functions.predefine(DOC_AVAILABLE_FUNC);
    Functions.predefine(COLLECTION_FUNC);
    Functions.predefine(COLLECTION_FUNC_2);
    Functions.predefine(SIZE_FUNC);
    Functions.predefine(LOAD);
    Functions.predefine(LOAD_2);
  }
}
