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
package org.brackit.query.module;

import org.brackit.query.atomic.QNm;
import org.brackit.query.compiler.Bits;
import org.brackit.query.function.ConstructorFunction;
import org.brackit.query.function.fn.Collection;
import org.brackit.query.function.fn.Number;
import org.brackit.query.function.fn.*;
import org.brackit.query.function.json.JSONFun;
import org.brackit.query.function.json.Keys;
import org.brackit.query.function.json.Size;
import org.brackit.query.util.Regex;
import org.brackit.query.jdm.Function;
import org.brackit.query.jdm.Signature;
import org.brackit.query.jdm.Type;
import org.brackit.query.jdm.type.*;

import java.util.*;

/**
 * @author Sebastian Baechle
 */
public final class Functions {

  private static final Map<QNm, Function[]> predefined = new HashMap<>();

  public static final QNm FN_POSITION = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "position");

  public static final QNm FN_LAST = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "last");

  public static final QNm FN_TRUE = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "true");

  public static final QNm FN_FALSE = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "false");

  public static final QNm FN_DEFAULT_COLLATION = new QNm(Namespaces.FN_NSURI,
                                                         Namespaces.FN_PREFIX,
                                                         "default-collation");

  public static final QNm FN_STATIC_BASE_URI = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "static-base-uri");

  public static final QNm FN_COUNT = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "count");

  public static final QNm FN_DISTINCT = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "distinct-values");

  public static final QNm FN_ROOT = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "root");

  private final Map<QNm, Function[]> functions = new HashMap<>();

  private final List<Functions> imports = new ArrayList<>();

  static {
    // See XQuery Functions and Operators 2 Accessors
    predefine(new NodeName(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "node-name"),
                           new Signature(new SequenceType(AtomicType.QNM, Cardinality.ZeroOrOne),
                                         new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new Nilled(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "nilled"),
                         new Signature(new SequenceType(AtomicType.QNM, Cardinality.ZeroOrOne),
                                       new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new BaseURI(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "base-uri"),
                          new Signature(new SequenceType(AtomicType.AURI, Cardinality.ZeroOrOne),
                                        new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new BaseURI(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "base-uri"),
                          new Signature(new SequenceType(AtomicType.AURI, Cardinality.ZeroOrOne),
                                        false,
                                        AnyNodeType.ANY_NODE)));
    predefine(new Data(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "data"),
                       new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                     false,
                                     AnyNodeType.ANY_NODE)));
    predefine(new Data(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "data"),
                       new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                     new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new StringValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string"),
                              new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                            new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrOne))));
    predefine(new StringValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string"),
                              new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                            false,
                                            AnyItemType.ANY)));
    predefine(new DocumentURI(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "document-uri"),
                              new Signature(new SequenceType(AtomicType.AURI, Cardinality.ZeroOrOne),
                                            new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 3 The Error Function
    predefine(new org.brackit.query.function.fn.Error(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "error"),
                                                      new Signature(new SequenceType(AnyItemType.ANY,
                                                                                      Cardinality.Zero))));
    predefine(new org.brackit.query.function.fn.Error(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "error"),
                                                      new Signature(new SequenceType(AnyItemType.ANY,
                                                                                      Cardinality.Zero),
                                                                     new SequenceType(AtomicType.QNM,
                                                                                      Cardinality.One))));
    predefine(new org.brackit.query.function.fn.Error(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "error"),
                                                      new Signature(new SequenceType(AnyItemType.ANY,
                                                                                      Cardinality.Zero),
                                                                     new SequenceType(AtomicType.QNM,
                                                                                      Cardinality.ZeroOrOne),
                                                                     new SequenceType(AtomicType.STR,
                                                                                      Cardinality.One))));
    predefine(new org.brackit.query.function.fn.Error(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "error"),
                                                      new Signature(new SequenceType(AnyItemType.ANY,
                                                                                      Cardinality.Zero),
                                                                     new SequenceType(AtomicType.QNM,
                                                                                      Cardinality.ZeroOrOne),
                                                                     new SequenceType(AtomicType.STR, Cardinality.One),
                                                                     new SequenceType(AnyItemType.ANY,
                                                                                      Cardinality.ZeroOrMany))));

    // See XQuery Functions and Operators 4 Trace Function
    predefine(new Trace(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "trace"),
                        new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                      new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));

    // See XQuery Functions and Operators 5.2 A Special Constructor Function
    // for xs:dateTime
    predefine(new DateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "dateTime"),
                           new Signature(new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne),
                                         new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne),
                                         new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 6.4 Functions on Numeric Values
    predefine(new Abs(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "abs"),
                      new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                    new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne))));
    predefine(new Ceiling(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "ceiling"),
                          new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                        new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne))));
    predefine(new Floor(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "floor"),
                        new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                      new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne))));
    predefine(new Round(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "round"),
                        new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                      new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne))));
    predefine(new RoundHalfEven(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "round-half-to-even"),
                                new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                              new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne))));
    predefine(new RoundHalfEven(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "round-half-to-even"),
                                new Signature(new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                              new SequenceType(NumericType.INSTANCE, Cardinality.ZeroOrOne),
                                              new SequenceType(AtomicType.INR, Cardinality.One))));

    // See XQuery Functions and Operators 7.2 Functions to Assemble and
    // Disassemble Strings
    predefine(new StringToCodepoints(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-to-codepoints"),
                                     new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrMany),
                                                   new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new CodepointsToString(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "codepoints-to-string"),
                                     new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                   new SequenceType(AtomicType.INR, Cardinality.ZeroOrMany))));

    // See XQuery Functions and Operators 7.3 Equality and Comparison of
    // Strings
    predefine(new Compare(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "compare"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new Compare(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "compare"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new CodepointEqual(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "codepoint-equal"),
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 7.4 Functions on String Values
    predefine(new Concat(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "concat"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.One),
                                       true,
                                       null,
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne))));
    predefine(new StringCase(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "upper-case"),
                             true,
                             new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringCase(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "lower-case"),
                             false,
                             new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringLength(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-length"),
                               new Signature(new SequenceType(AtomicType.INT, Cardinality.One),
                                             new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringLength(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-length"),
                               new Signature(new SequenceType(AtomicType.INT, Cardinality.One),
                                             false,
                                             AtomicType.STR)));
    predefine(new StringJoin(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "string-join"),
                             new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
                                           new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new Substring(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring"),
                            new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                          new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                          new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new Substring(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring"),
                            new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                          new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                          new SequenceType(AtomicType.DBL, Cardinality.One),
                                          new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new StringTranslate(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "translate"),
                                  new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                new SequenceType(AtomicType.STR, Cardinality.One),
                                                new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new StringNormalize(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "normalize-space"),
                                  new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringNormalize(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "normalize-space"),
                                  new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                false,
                                                AtomicType.STR)));
    predefine(new StringCase(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "upper-case"),
                             true,
                             new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringCase(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "lower-case"),
                             false,
                             new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new StringTranslate(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "translate"),
                                  new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                new SequenceType(AtomicType.STR, Cardinality.One),
                                                new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new ResolveURI(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "resolve-uri"),
                             new Signature(new SequenceType(AtomicType.AURI, Cardinality.ZeroOrOne),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new ResolveURI(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "resolve-uri"),
                             new Signature(new SequenceType(AtomicType.AURI, Cardinality.ZeroOrOne),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                           new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new Encode(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "encode-for-uri"),
                         Encode.Mode.ENCODE_FOR_URI,
                         new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                       new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new Encode(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "iri-to-uri"),
                         Encode.Mode.IRI_TO_URI,
                         new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                       new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new Encode(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "escape-html-uri"),
                         Encode.Mode.HTML_URI,
                         new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                       new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operator 7.5 Functions Based on Substring
    // Matching
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "contains"),
                                 SubstringMatch.Mode.CONTAINS,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "contains"),
                                 SubstringMatch.Mode.CONTAINS,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "starts-with"),
                                 SubstringMatch.Mode.STARTS_WITH,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "starts-with"),
                                 SubstringMatch.Mode.STARTS_WITH,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "ends-with"),
                                 SubstringMatch.Mode.ENDS_WITH,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new SubstringMatch(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "ends-with"),
                                 SubstringMatch.Mode.ENDS_WITH,
                                 new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                               new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new SubstringRelative(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring-before"),
                                    true,
                                    new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new SubstringRelative(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring-before"),
                                    true,
                                    new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new SubstringRelative(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring-after"),
                                    false,
                                    new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new SubstringRelative(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "substring-after"),
                                    false,
                                    new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                                  new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 7.6 String Functions that Use
    // Pattern Matching
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "matches"),
                        Regex.Mode.MATCH,
                        new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "matches"),
                        Regex.Mode.MATCH,
                        new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "replace"),
                        Regex.Mode.REPLACE,
                        new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "replace"),
                        Regex.Mode.REPLACE,
                        new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "tokenize"),
                        Regex.Mode.TOKENIZE,
                        new Signature(new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new RegEx(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "tokenize"),
                        Regex.Mode.TOKENIZE,
                        new Signature(new SequenceType(AtomicType.STR, Cardinality.ZeroOrMany),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));

    // See XQuery Functions and Operators 9.3 Functions on Boolean Values
    predefine(new BooleanValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "true"),
                               false,
                               new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One))));
    predefine(new BooleanValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "false"),
                               false,
                               new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One))));
    predefine(new BooleanValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "not"),
                               true,
                               new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                             new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));

    // See XQuery Functions and Operators 10.5 Component Extraction
    // Functions on Durations, Dates and Times
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "years-from-duration"),
                                      ExtractFromDuration.Comp.YEARS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "months-from-duration"),
                                      ExtractFromDuration.Comp.MONTHS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "days-from-duration"),
                                      ExtractFromDuration.Comp.DAYS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "hours-from-duration"),
                                      ExtractFromDuration.Comp.HOURS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "minutes-from-duration"),
                                      ExtractFromDuration.Comp.MINUTES,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDuration(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "seconds-from-duration"),
                                      ExtractFromDuration.Comp.SECONDS,
                                      new Signature(new SequenceType(AtomicType.DEC, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DUR, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "year-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.YEAR,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "month-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.MONTH,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "day-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.DAY,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "hours-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.HOURS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "minutes-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.MINUTES,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "seconds-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.SECONDS,
                                      new Signature(new SequenceType(AtomicType.DEC, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "timezone-from-dateTime"),
                                      ExtractFromDateTime.Source.DATE_TIME,
                                      ExtractFromDateTime.Comp.TIMEZONE,
                                      new Signature(new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "year-from-date"),
                                      ExtractFromDateTime.Source.DATE,
                                      ExtractFromDateTime.Comp.YEAR,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "month-from-date"),
                                      ExtractFromDateTime.Source.DATE,
                                      ExtractFromDateTime.Comp.MONTH,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "day-from-date"),
                                      ExtractFromDateTime.Source.DATE,
                                      ExtractFromDateTime.Comp.DAY,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "timezone-from-date"),
                                      ExtractFromDateTime.Source.DATE,
                                      ExtractFromDateTime.Comp.TIMEZONE,
                                      new Signature(new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "hours-from-time"),
                                      ExtractFromDateTime.Source.TIME,
                                      ExtractFromDateTime.Comp.HOURS,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "minutes-from-time"),
                                      ExtractFromDateTime.Source.TIME,
                                      ExtractFromDateTime.Comp.MINUTES,
                                      new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "seconds-from-time"),
                                      ExtractFromDateTime.Source.TIME,
                                      ExtractFromDateTime.Comp.SECONDS,
                                      new Signature(new SequenceType(AtomicType.DEC, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));
    predefine(new ExtractFromDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "timezone-from-time"),
                                      ExtractFromDateTime.Source.TIME,
                                      ExtractFromDateTime.Comp.TIMEZONE,
                                      new Signature(new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne),
                                                    new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 10.7 Timezone Adjustment Functions
    // on Dates and Time Values
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-dateTime-to-timezone"),
                                   AdjustToTimezone.Source.DATE_TIME,
                                   new Signature(new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne))));
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-dateTime-to-timezone"),
                                   AdjustToTimezone.Source.DATE_TIME,
                                   new Signature(new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DATI, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne))));
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-date-to-timezone"),
                                   AdjustToTimezone.Source.DATE,
                                   new Signature(new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne))));
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-date-to-timezone"),
                                   AdjustToTimezone.Source.DATE,
                                   new Signature(new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DATE, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne))));
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-time-to-timezone"),
                                   AdjustToTimezone.Source.TIME,
                                   new Signature(new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne))));
    predefine(new AdjustToTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "adjust-time-to-timezone"),
                                   AdjustToTimezone.Source.TIME,
                                   new Signature(new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.TIME, Cardinality.ZeroOrOne),
                                                 new SequenceType(AtomicType.DTD, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 11 Functions Related to QNames
    predefine(new QName(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "QName"),
                        new Signature(new SequenceType(AtomicType.QNM, Cardinality.One),
                                      new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                      new SequenceType(AtomicType.STR, Cardinality.One))));

    // See XQuery Functions and Operators 14 Functions and Operators on
    // Nodes
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "name"),
                       Name.Mode.NAME,
                       new Signature(new SequenceType(AtomicType.STR, Cardinality.One), false, AnyNodeType.ANY_NODE)));
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "name"),
                       Name.Mode.NAME,
                       new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                     new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "local-name"),
                       Name.Mode.LOCAL_NAME,
                       new Signature(new SequenceType(AtomicType.STR, Cardinality.One), false, AnyNodeType.ANY_NODE)));
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "local-name"),
                       Name.Mode.LOCAL_NAME,
                       new Signature(new SequenceType(AtomicType.STR, Cardinality.One),
                                     new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "namespace-uri"),
                       Name.Mode.NAMESPACE_URI,
                       new Signature(new SequenceType(AtomicType.AURI, Cardinality.One), false, AnyNodeType.ANY_NODE)));
    predefine(new Name(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "namespace-uri"),
                       Name.Mode.NAMESPACE_URI,
                       new Signature(new SequenceType(AtomicType.AURI, Cardinality.One),
                                     new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));
    predefine(new Number(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "number"),
                         new Signature(new SequenceType(AtomicType.DBL, Cardinality.One), false, AnyItemType.ANY)));
    predefine(new Number(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "number"),
                         new Signature(new SequenceType(AtomicType.DBL, Cardinality.One),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne))));
    predefine(new Root(FN_ROOT,
                       new Signature(new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne),
                                     false,
                                     AnyNodeType.ANY_NODE)));
    predefine(new Root(FN_ROOT,
                       new Signature(new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne),
                                     new SequenceType(AnyNodeType.ANY_NODE, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 15.1 General Functions and
    // Operators on Sequences
    predefine(new BooleanValue(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "boolean"),
                               false,
                               new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                             new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new IndexOf(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "index-of"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.One))));
    predefine(new IndexOf(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "index-of"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrOne),
                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.One),
                                        new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new EmptySequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "empty"),
                                true,
                                new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                              new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new EmptySequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "exists"),
                                false,
                                new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                              new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new Distinct(FN_DISTINCT,
                           new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                         new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                         new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new Distinct(FN_DISTINCT,
                           new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                         new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany))));
    predefine(new InsertBefore(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "insert-before"),
                               new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                             new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                             new SequenceType(AtomicType.INR, Cardinality.One),
                                             new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany))));
    predefine(new Remove(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "remove"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.INR, Cardinality.One))));
    predefine(new Reverse(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "reverse"),
                          new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany))));
    predefine(new Subsequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "subsequence"),
                              new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new Subsequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "subsequence"),
                              new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.DBL, Cardinality.One),
                                            new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new Subsequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "subsequence"),
                              new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                            new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.DBL, Cardinality.One),
                                            new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new Subsequence(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "subsequence"),
                              new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                            new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                            new SequenceType(AtomicType.DBL, Cardinality.One))));
    predefine(new Reverse(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "reverse"),
                          new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                        new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new Remove(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "remove"),
                         new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                       new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.INR, Cardinality.One))));
    predefine(new InsertBefore(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "insert-before"),
                               new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                             new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                             new SequenceType(AtomicType.INR, Cardinality.One),
                                             new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new IndexOf(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "index-of"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.One))));
    predefine(new IndexOf(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "index-of"),
                          new Signature(new SequenceType(AtomicType.INR, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                        new SequenceType(AtomicType.ANA, Cardinality.One),
                                        new SequenceType(AtomicType.STR, Cardinality.One))));
    predefine(new Unordered(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "unordered"),
                            new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                          new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));

    // See XQuery Functions and Operators 15.2 Functions That Test the
    // Cardinality of Sequences
    predefine(new CardinalityTest(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "zero-or-one"),
                                  new Signature(new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrOne),
                                                new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)),
                                  Cardinality.ZeroOrOne));
    predefine(new CardinalityTest(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "one-or-more"),
                                  new Signature(new SequenceType(AnyItemType.ANY, Cardinality.OneOrMany),
                                                new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)),
                                  Cardinality.OneOrMany));
    predefine(new CardinalityTest(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "exactly-one"),
                                  new Signature(new SequenceType(AnyItemType.ANY, Cardinality.One),
                                                new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany)),
                                  Cardinality.One));
    // See XQuery Functions and Operators 15.3 Equals, Union, Intersection
    // and Except
    predefine(new DeepEqual(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "deep-equal"),
                            new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                          new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                          new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));
    predefine(new DeepEqual(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "deep-equal"),
                            new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                          new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                          new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany),
                                          new SequenceType(AtomicType.STR, Cardinality.One))));

    // See XQuery Functions and Operators 15.4 Aggregate Functions
    predefine(new Count(FN_COUNT,
                        new Signature(new SequenceType(AtomicType.INR, Cardinality.One),
                                      new SequenceType(AnyItemType.ANY, Cardinality.ZeroOrMany))));

    predefine(new MinMax(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "min"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany)),
                         true));
    predefine(new MinMax(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "min"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.STR, Cardinality.One)),
                         true));
    predefine(new MinMax(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "max"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany)),
                         false));
    predefine(new MinMax(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "max"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.STR, Cardinality.One)),
                         false));
    predefine(new SumAvg(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "avg"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany)),
                         true));
    predefine(new SumAvg(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "sum"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany)),
                         false));
    predefine(new SumAvg(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "sum"),
                         new Signature(new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrMany),
                                       new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne)),
                         false));
    // See XQuery Functions and Operators 15.5 Functions and Operators that
    // Generate Sequences
    predefine(new Doc(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "doc"),
                      true,
                      new Signature(new SequenceType(DocumentType.DOC, Cardinality.ZeroOrOne),
                                    new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new Doc(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "doc"),
                      true,
                      new Signature(new SequenceType(DocumentType.DOC, Cardinality.ZeroOrOne),
                                    new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne),
                                    new SequenceType(AtomicType.INT, Cardinality.ZeroOrOne))));
    predefine(new Doc(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "doc-available"),
                      false,
                      new Signature(new SequenceType(AtomicType.BOOL, Cardinality.One),
                                    new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));
    predefine(new Collection(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "collection"),
                             new Signature(new SequenceType(DocumentType.DOC, Cardinality.ZeroOrMany))));
    predefine(new Collection(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "collection"),
                             new Signature(new SequenceType(DocumentType.DOC, Cardinality.ZeroOrMany),
                                           new SequenceType(AtomicType.STR, Cardinality.ZeroOrOne))));

    // See XQuery Functions and Operators 16 Context Functions
    predefine(new CurrentDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "current-dateTime"),
                                  new Signature(new SequenceType(AtomicType.DATI, Cardinality.One))));
    predefine(new CurrentDate(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "current-date"),
                              new Signature(new SequenceType(AtomicType.DATE, Cardinality.One))));
    predefine(new CurrentTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "current-time"),
                              new Signature(new SequenceType(AtomicType.TIME, Cardinality.One))));
    predefine(new ImplicitTimezone(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "implicit-timezone"),
                                   new Signature(new SequenceType(AtomicType.DTD, Cardinality.One))));
    predefine(new CurrentDateTime(new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, "default-collation"),
                                  new Signature(new SequenceType(AtomicType.STR, Cardinality.One))));

    predefine(new Keys());
    predefine(new Size());

    for (Type type : Type.builtInTypes) {
      if ((type != Type.ANA) && (type != Type.NOT)) {
        predefine(new ConstructorFunction(type.getName(),
                                          new Signature(new SequenceType(new AtomicType(type), Cardinality.ZeroOrOne),
                                                        new SequenceType(AtomicType.ANA, Cardinality.ZeroOrOne)),
                                          type));
      }
    }
  }

  public Function resolve(QNm name, int argCount) {
    if (name.getNamespaceURI().equals(Namespaces.DEFAULT_FN_NSURI)) {
      name = new QNm(Namespaces.FN_NSURI, Namespaces.FN_PREFIX, name.getLocalName());
      var function = getFunction(name, argCount);

      if (function != null) {
        return function;
      }

      name = new QNm(JSONFun.JSON_NSURI, JSONFun.JSON_NSURI, name.getLocalName());
      function = getFunction(name, argCount);

      if (function != null) {
        return function;
      }

      name = new QNm(Bits.BIT_NSURI, Bits.BIT_PREFIX, name.getLocalName());
      return getFunction(name, argCount);
    }

    return getFunction(name, argCount);
  }

  private Function getFunction(QNm name, int argCount) {
    // collect all available functions for the given name
    final var availableFunctions = new ArrayList<Function>();
    final Function[] declaredFuns = functions.get(name);
    if (declaredFuns != null) {
      availableFunctions.addAll(Arrays.asList(declaredFuns));
    }
    final Function[] predefFuns = predefined.get(name);
    if (predefFuns != null) {
      availableFunctions.addAll(Arrays.asList(predefFuns));
    }
    for (final Functions imported : imports) {
      // TODO check only public funs!
      Function[] importedFuns = imported.functions.get(name);
      if (importedFuns != null) {
        availableFunctions.addAll(Arrays.asList(importedFuns));
      }
    }
    if (availableFunctions.isEmpty() && name.getNamespaceURI().isEmpty()) {
      final Function[] predefFunsInDefaultNamespace = predefined.get(new QNm(Namespaces.FN_NSURI,
                                                                             Namespaces.FN_PREFIX,
                                                                             name.getLocalName()));
      if (predefFunsInDefaultNamespace != null) {
        availableFunctions.addAll(Arrays.asList(predefFunsInDefaultNamespace));
      }
    }

    if (availableFunctions.isEmpty()) {
      return null;
    }

    // find function with matching arity
    for (final Function f : availableFunctions) {
      final Signature sig = f.getSignature();
      final SequenceType[] params = sig.getParams();
      final int parCnt = params.length;

      if ((argCount == parCnt) || (((sig.lastIsVarArg()) && ((argCount == parCnt - 1) || (argCount > parCnt))))) {
        return f;
      }

      //          int zeroOrMore = 0;
      //
      //          for (final SequenceType param : params) {
      //              if (param.getCardinality() == Cardinality.Zero
      //                      || param.getCardinality() == Cardinality.ZeroOrMany
      //                      || param.getCardinality() == Cardinality.ZeroOrOne) {
      //                  zeroOrMore++;
      //              }
      //          }
      //
      //          argCount += zeroOrMore;
      //          if (argCount == parCnt) {
      ////                    || (((sig.lastIsVarArg()) && ((argCount == parCnt - 1) || (argCount > parCnt))))) {
      //              return f;
      //          }
    }
    return null;
  }

  public void declare(Function function) {
    Function[] funs = functions.get(function.getName());

    if (funs == null) {
      funs = new Function[] { function };
    } else {
      funs = Arrays.copyOf(funs, funs.length + 1);
      funs[funs.length - 1] = function;
    }

    functions.put(function.getName(), funs);
  }

  public void importFunctions(Functions functions) {
    imports.add(functions);
  }

  public Map<QNm, Function[]> getDeclaredFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  public Map<QNm, Function[]> getPredefinedFunctions() {
    return Collections.unmodifiableMap(predefined);
  }

  public static void predefine(Function function) {
    Function[] funs = predefined.computeIfAbsent(function.getName(), k -> new Function[1]);

    for (int i = 0; i < funs.length; i++) {
      if (funs[i] == null) {
        funs[i] = function;
        return;
      }
    }

    funs = Arrays.copyOf(funs, funs.length + 1);
    funs[funs.length - 1] = function;
    predefined.put(function.getName(), funs);
  }
}