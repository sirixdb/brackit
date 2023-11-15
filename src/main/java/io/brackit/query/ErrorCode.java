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
package io.brackit.query;

import static io.brackit.query.compiler.Bits.BIT_NSURI;
import static io.brackit.query.compiler.Bits.BIT_PREFIX;

import io.brackit.query.atomic.QNm;
import io.brackit.query.module.Namespaces;

/**
 * @author Sebastian Baechle
 */
public class ErrorCode {
  /**
   * err:FOER0000
   */
  public static final QNm ERR_UNIDENTIFIED_ERROR = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOER0000");

  /**
   * err:FONS0004
   */
  public static final QNm ERR_NO_NAMESPACE_FOR_PREFIX = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "FONS0004");

  /**
   * err:XPDY0002
   */
  public static final QNm ERR_DYNAMIC_CONTEXT_VARIABLE_NOT_DEFINED = new QNm(Namespaces.ERR_NSURI,
                                                                             Namespaces.ERR_PREFIX,
                                                                             "XPDY0002");

  /**
   * err:XPDY0050
   */
  public static final QNm ERR_DYNAMIC_TYPE_DOES_NOT_MATCH_TREAT_TYPE = new QNm(Namespaces.ERR_NSURI,
                                                                               Namespaces.ERR_PREFIX,
                                                                               "XPDY0050");

  /**
   * err:XPDY0003
   */
  public static final QNm ERR_PARSING_ERROR = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XPST0003");

  /**
   * err:XPTY0004
   */
  public static final QNm ERR_TYPE_INAPPROPRIATE_TYPE = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "XPTY0004");

  /**
   * err:XPTY0018
   */
  public static final QNm ERR_PATH_STEP_RETURNED_NODE_AND_NON_NODE_VALUES = new QNm(Namespaces.ERR_NSURI,
                                                                                    Namespaces.ERR_PREFIX,
                                                                                    "XPTY0018");

  /**
   * err:XPTY0019
   */
  public static final QNm ERR_PATH_STEP_RETURNED_NON_NODE_VALUE = new QNm(Namespaces.ERR_NSURI,
                                                                          Namespaces.ERR_PREFIX,
                                                                          "XPTY0019");

  /**
   * err:XPTY0020
   */
  public static final QNm ERR_PATH_STEP_CONTEXT_ITEM_IS_NOT_A_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                             Namespaces.ERR_PREFIX,
                                                                             "XPTY0020");

  /**
   * err:XPTY0117
   */
  public static final QNm ERR_TYPE_CAST_TO_NAMESPACE_SENSITIVE_TYPE = new QNm(Namespaces.ERR_NSURI,
                                                                              Namespaces.ERR_PREFIX,
                                                                              "XPTY0117");

  /**
   * err:XQTY0024
   */
  public static final QNm ERR_TYPE_CONTENT_SEQUENCE_ATTRIBUTE_FOLLOWING_NON_ATTRIBUTE = new QNm(Namespaces.ERR_NSURI,
                                                                                                Namespaces.ERR_PREFIX,
                                                                                                "XQTY0024");

  /**
   * err:XPST0008
   */
  public static final QNm ERR_UNDEFINED_REFERENCE = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XPST0008");

  /**
   * err:XPST0051
   */
  public static final QNm ERR_UNKNOWN_ATOMIC_SCHEMA_TYPE = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "XPST0051");

  /**
   * err:PST0080
   */
  public static final QNm ERR_ILLEGAL_CAST_TARGET_TYPE = new QNm(Namespaces.ERR_NSURI,
                                                                 Namespaces.ERR_PREFIX,
                                                                 "XPST0080");

  /**
   * err:PST0081
   */
  public static final QNm ERR_UNDEFINED_NAMESPACE_PREFIX = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "XPST0081");

  /**
   * err:XPST0017
   */
  public static final QNm ERR_UNDEFINED_FUNCTION = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XPST0017");

  /**
   * err:XQST0009
   */
  public static final QNm ERR_SCHEMA_IMPORT_FEATURE_NOT_SUPPORTED = new QNm(Namespaces.ERR_NSURI,
                                                                            Namespaces.ERR_PREFIX,
                                                                            "XQST0009");

  /**
   * err:XQST0016
   */
  public static final QNm ERR_MODULE_IMPORT_OR_DECLARATION_FEATURE_NOT_SUPPORTED = new QNm(Namespaces.ERR_NSURI,
                                                                                           Namespaces.ERR_PREFIX,
                                                                                           "XQST0016");

  /**
   * err:XQST0040
   */
  public static final QNm ERR_ENCLOSED_EXPR_IN_NS_ATTRIBUTE = new QNm(Namespaces.ERR_NSURI,
                                                                      Namespaces.ERR_PREFIX,
                                                                      "XQST0040");

  /**
   * err:XQST0032
   */
  public static final QNm ERR_BASE_URI_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                  Namespaces.ERR_PREFIX,
                                                                  "XQST0032");

  /**
   * err:XQST0033
   */
  public static final QNm ERR_MULTIPLE_NS_BINDINGS_FOR_PREFIX = new QNm(Namespaces.ERR_NSURI,
                                                                        Namespaces.ERR_PREFIX,
                                                                        "XQST0033");

  /**
   * err:XQST0034
   */
  public static final QNm ERR_MULTIPLE_FUNCTION_DECLARATIONS = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "XQST0034");

  /**
   * err:XQST0039
   */
  public static final QNm ERR_DUPLICATE_FUN_PARAMETER = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "XQST0039");

  /**
   * err:XQST0045
   */
  public static final QNm ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE = new QNm(Namespaces.ERR_NSURI,
                                                                           Namespaces.ERR_PREFIX,
                                                                           "XQST0045");

  /**
   * err:XQST0046
   */
  public static final QNm ERR_INVALID_URI_LITERAL = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XQST0046");

  /**
   * err:XQST0047
   */
  public static final QNm ERR_MULTIPLE_IMPORTS_IN_SAME_NS = new QNm(Namespaces.ERR_NSURI,
                                                                    Namespaces.ERR_PREFIX,
                                                                    "XQST0047");

  /**
   * err:XQST0048
   */
  public static final QNm ERR_FUN_OR_VAR_NOT_IN_TARGET_NS = new QNm(Namespaces.ERR_NSURI,
                                                                    Namespaces.ERR_PREFIX,
                                                                    "XQST0048");

  /**
   * err:XQST0049
   */
  public static final QNm ERR_DUPLICATE_VARIABLE_DECL = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "XQST0049");

  /**
   * err:XQST0054
   */
  public static final QNm ERR_CIRCULAR_VARIABLE_DEPENDENCY = new QNm(Namespaces.ERR_NSURI,
                                                                     Namespaces.ERR_PREFIX,
                                                                     "XQST0054");

  /**
   * err:XQST0055
   */
  public static final QNm ERR_COPY_NAMESPACES_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "XQST0055");

  /**
   * err:XQST0059
   */
  public static final QNm ERR_SCHEMA_OR_MODULE_NOT_FOUND = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "XQST0059");

  /**
   * err:XQST0060
   */
  public static final QNm ERR_FUNCTION_DECL_NOT_IN_NS = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "XQST0060");

  /**
   * err:XQST0065
   */
  public static final QNm ERR_ORDERING_MODE_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "XQST0065");

  /**
   * err:XQST0066
   */
  public static final QNm ERR_DEFAULT_NS_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                    Namespaces.ERR_PREFIX,
                                                                    "XQST0066");

  /**
   * err:XQST0067
   */
  public static final QNm ERR_CONSTRUCTION_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                      Namespaces.ERR_PREFIX,
                                                                      "XQST0067");

  /**
   * err:XQST0068
   */
  public static final QNm ERR_BOUNDARY_SPACE_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                        Namespaces.ERR_PREFIX,
                                                                        "XQST0068");

  /**
   * err:XQST0069
   */
  public static final QNm ERR_EMPTY_ORDER_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                     Namespaces.ERR_PREFIX,
                                                                     "XQST0069");

  /**
   * err:XQST0070
   */
  public static final QNm ERR_ILLEGAL_NAMESPACE_DECL = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XQST0070");

  /**
   * err:XQDY0074
   */
  public static final QNm ERR_UNKNOWN_NS_PREFIX_IN_COMP_CONSTR = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "XQDY0074");

  /**
   * err:XQST0075
   */
  public static final QNm ERR_SCHEMA_VALIDATION_FEATURE_NOT_SUPPORTED = new QNm(Namespaces.ERR_NSURI,
                                                                                Namespaces.ERR_PREFIX,
                                                                                "XQST0075");

  /**
   * err:XQST0076
   */
  public static final QNm ERR_UNKNOWN_COLLATION_IN_FLWOR_CLAUSE = new QNm(Namespaces.ERR_NSURI,
                                                                          Namespaces.ERR_PREFIX,
                                                                          "XQST0076");

  /**
   * err:XQST0088
   */
  public static final QNm ERR_TARGET_NS_EMPTY = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XQST0088");

  /**
   * err:XQST0089
   */
  public static final QNm ERR_FOR_VAR_AND_POS_VAR_EQUAL = new QNm(Namespaces.ERR_NSURI,
                                                                  Namespaces.ERR_PREFIX,
                                                                  "XQST0089");

  /**
   * err:XQST0090
   */
  public static final QNm ERR_UNDEFINED_CHARACTER_REFERENCE = new QNm(Namespaces.ERR_NSURI,
                                                                      Namespaces.ERR_PREFIX,
                                                                      "XQST0090");

  /**
   * err:XQST0099
   */
  public static final QNm ERR_CONTEXT_ITEM_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                      Namespaces.ERR_PREFIX,
                                                                      "XQST0099");

  /**
   * err:XQST0106
   */
  public static QNm ERR_FUN_PRIVATE_OR_PUBLIC_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "XQST0106");

  /**
   * err:XQST0107
   */
  public static final QNm ERR_CIRCULAR_CONTEXT_ITEM_INITIALIZER = new QNm(Namespaces.ERR_NSURI,
                                                                          Namespaces.ERR_PREFIX,
                                                                          "XQST0107");

  /**
   * err:XQST0113
   */
  public static QNm ERR_CONTEXT_ITEM_VALUE_SPEC_IN_LIBRARY = new QNm(Namespaces.ERR_NSURI,
                                                                     Namespaces.ERR_PREFIX,
                                                                     "XQST0113");

  /**
   * err:XQST0114
   */
  public static QNm ERR_DECIMAL_FORMAT_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                  Namespaces.ERR_PREFIX,
                                                                  "XQST0114");

  /**
   * err:XQST0116
   */
  public static QNm ERR_VAR_PRIVATE_OR_PUBLIC_ALREADY_DECLARED = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "XQST0116");

  /**
   * err:FOAR0001
   */
  public static final QNm ERR_DIVISION_BY_ZERO = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOAR0001");

  /**
   * err:FOCA0002
   */
  public static final QNm ERR_INVALID_LEXICAL_VALUE = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOCA0002");

  /**
   * err:FOCA0004
   */
  public static final QNm ERR_PARAMETER_NAN = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOCA0004");

  /**
   * err:FOAR0002
   */
  public static final QNm ERR_NUMERIC_OVERFLOW_UNDERFLOW = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "FOAR0002");

  /**
   * err:FOCH0001
   */
  public static final QNm ERR_CODE_POINT_NOT_VALID = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOCH0001");

  /**
   * err:FOCH0002
   */
  public static final QNm ERR_UNSUPPORTED_COLLATION = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FOCH0002");

  /**
   * err:FODC0002
   */
  public static final QNm ERR_RETRIEVING_RESOURCE = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FODC0002");

  /**
   * err:FODC0004
   */
  public static final QNm ERR_COLLECTION_NOT_FOUND = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FODC0004");

  /**
   * err:FODT0001
   */
  public static final QNm ERR_OVERFLOW_UNDERFLOW_IN_DATETIME = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "FODT0001");

  /**
   * err:FODT0002
   */
  public static final QNm ERR_OVERFLOW_UNDERFLOW_IN_DURATION = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "FODT0002");

  /**
   * err:FODT0003
   */
  public static final QNm ERR_INVALID_TIMEZONE = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FODT0003");

  /**
   * err:FODC0005
   */
  public static final QNm ERR_DOCUMENT_NOT_FOUND = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FODC0005");

  /**
   * err:FONS0005
   */
  public static final QNm ERR_UNDEFINED_STATIC_BASE_URI = new QNm(Namespaces.ERR_NSURI,
                                                                  Namespaces.ERR_PREFIX,
                                                                  "FONS0005");

  /**
   * err:FORG0001
   */
  public static final QNm ERR_INVALID_VALUE_FOR_CAST = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0001");

  /**
   * err:FORG0002
   */
  public static final QNm ERR_INVALID_URI = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0002");

  /**
   * err:FORG0003
   */
  public static final QNm ERR_ZERO_OR_ONE_FAILED = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0003");

  /**
   * err:FORG0004
   */
  public static final QNm ERR_ONE_OR_MORE_FAILED = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0004");

  /**
   * err:FORG0005
   */
  public static final QNm ERR_EXACTLY_ONCE_FAILED = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0005");

  /**
   * err:FORG0006
   */
  public static final QNm ERR_INVALID_ARGUMENT_TYPE = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0006");

  /**
   * err:FORG0008
   */
  public static final QNm ERR_DATETIME_FUNCTION_DIFFERENT_TZ = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "FORG0008");

  /**
   * err:FORG0009
   */
  public static final QNm ERR_FN_RESOLVE_URI = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "FORG0009");

  /**
   * err:FORX0001
   */
  public static final QNm ERR_INVALID_REGULAR_EXPRESSION_FLAGS = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "FORX0001");

  /**
   * err:FORX0002
   */
  public static final QNm ERR_INVALID_REGULAR_EXPRESSION = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "FORX0002");

  /**
   * err:FORX0003
   */
  public static final QNm ERR_REGULAR_EXPRESSION_EMPTY_STRING = new QNm(Namespaces.ERR_NSURI,
                                                                        Namespaces.ERR_PREFIX,
                                                                        "FORX0003");

  /**
   * err:FORX0004
   */
  public static final QNm ERR_INVALID_REPLACEMENT_STRING = new QNm(Namespaces.ERR_NSURI,
                                                                   Namespaces.ERR_PREFIX,
                                                                   "FORX0004");

  /**
   * err:FOTY0012
   */
  public static final QNm ERR_ITEM_HAS_NO_TYPED_VALUE = new QNm(Namespaces.ERR_NSURI,
                                                                Namespaces.ERR_PREFIX,
                                                                "FOTY0012");

  /**
   * err:SENR0001
   */
  public static final QNm ERR_SERIALIZE_ATTRIBUTE_OR_NAMESPACE_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                              Namespaces.ERR_PREFIX,
                                                                              "SENR0001");

  /**
   * err:XUST0001
   */
  public static final QNm ERR_UPDATE_ILLEGAL_NESTED_UPDATE = new QNm(Namespaces.ERR_NSURI,
                                                                     Namespaces.ERR_PREFIX,
                                                                     "XUST0001");

  /**
   * err:XUST0002
   */
  public static final QNm ERR_UPDATING_OR_VACUOUS_EXPR_REQUIRED = new QNm(Namespaces.ERR_NSURI,
                                                                          Namespaces.ERR_PREFIX,
                                                                          "XUST0002");

  /**
   * err:XUDY0009
   */
  public static final QNm ERR_UPDATE_REPLACE_TARGET_NODE_HAS_NO_PARENT = new QNm(Namespaces.ERR_NSURI,
                                                                                 Namespaces.ERR_PREFIX,
                                                                                 "XUDY0009");

  /**
   * err:XUDY0014
   */
  public static final QNm ERR_TRANSFORM_MODIFIES_EXISTING_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                         Namespaces.ERR_PREFIX,
                                                                         "XUDY0014");
  /**
   * err:XUDY0015
   */
  public static final QNm ERR_UPDATE_DUPLICATE_RENAME_TARGET = new QNm(Namespaces.ERR_NSURI,
                                                                       Namespaces.ERR_PREFIX,
                                                                       "XUDY0015");

  /**
   * err:XUDY0016
   */
  public static final QNm ERR_UPDATE_DUPLICATE_REPLACE_NODE_TARGET = new QNm(Namespaces.ERR_NSURI,
                                                                             Namespaces.ERR_PREFIX,
                                                                             "XUDY0016");

  /**
   * err:XUDY0017
   */
  public static final QNm ERR_UPDATE_DUPLICATE_REPLACE_VALUE_TARGET = new QNm(Namespaces.ERR_NSURI,
                                                                              Namespaces.ERR_PREFIX,
                                                                              "XUDY0017");

  /**
   * err:XUDY0027
   */
  public static final QNm ERR_UPDATE_INSERT_TARGET_IS_EMPTY_SEQUENCE = new QNm(Namespaces.ERR_NSURI,
                                                                               Namespaces.ERR_PREFIX,
                                                                               "XUDY0027");

  /**
   * err:XUTY0005
   */
  public static final QNm ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ED_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                  Namespaces.ERR_PREFIX,
                                                                                  "XUTY0005");

  /**
   * err:XUTY0006
   */
  public static final QNm ERR_UPDATE_INSERT_TARGET_NOT_A_SINGLE_ETCP_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                    Namespaces.ERR_PREFIX,
                                                                                    "XUTY0006");

  /**
   * err:XUTY0007
   */
  public static final QNm ERR_UPDATE_DELETE_TARGET_NOT_A_NODE_SEQUENCE = new QNm(Namespaces.ERR_NSURI,
                                                                                 Namespaces.ERR_PREFIX,
                                                                                 "XUTY0007");

  /**
   * err:XUTY0008
   */
  public static final QNm ERR_UPDATE_REPLACE_TARGET_NOT_A_EATCP_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                               Namespaces.ERR_PREFIX,
                                                                               "XUTY0008");

  /**
   * err:XUTY0010
   */
  public static final QNm ERR_UPDATE_REPLACE_NODE_REPLACEMENT_NOT_A_ETCP_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                        Namespaces.ERR_PREFIX,
                                                                                        "XUTY0010");

  /**
   * err:XUTY0011
   */
  public static final QNm ERR_UPDATE_REPLACE_NODE_REPLACEMENT_NOT_AN_A_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                      Namespaces.ERR_PREFIX,
                                                                                      "XUTY0011");

  /**
   * err:XUTY0012
   */
  public static final QNm ERR_UPDATE_RENAME_TARGET_NOT_A_EAP_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                            Namespaces.ERR_PREFIX,
                                                                            "XUTY0012");

  /**
   * err:XUTY0013
   */
  public static final QNm ERR_TRANSFORM_SOURCE_EXPRESSION_NOT_SINGLE_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                    Namespaces.ERR_PREFIX,
                                                                                    "XUTY0013");

  /**
   * err:XUTY0022
   */
  public static final QNm ERR_UPDATE_INSERT_INTO_TARGET_IS_DOCUMENT_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                   Namespaces.ERR_PREFIX,
                                                                                   "XUTY0022");

  /**
   * err:XUDY0029
   */
  public static final QNm ERR_UPDATE_INSERT_TARGET_NODE_HAS_NO_PARENT = new QNm(Namespaces.ERR_NSURI,
                                                                                Namespaces.ERR_PREFIX,
                                                                                "XUDY0029");

  /**
   * err:XUDY0030
   */
  public static final QNm ERR_UPDATE_INSERT_BEFORE_AFTER_TARGET_PARENT_IS_DOCUMENT_NODE = new QNm(Namespaces.ERR_NSURI,
                                                                                                  Namespaces.ERR_PREFIX,
                                                                                                  "XUDY0030");

  /**
   * err:XQDY0026
   */
  public static final QNm ERR_PI_WOULD_CONTAIN_ILLEGAL_STRING = new QNm(Namespaces.ERR_NSURI,
                                                                        Namespaces.ERR_PREFIX,
                                                                        "XQDY0026");

  /**
   * err:XQDY0041
   */
  public static final QNm ERR_PI_TARGET_CAST_TO_NCNAME = new QNm(Namespaces.ERR_NSURI,
                                                                 Namespaces.ERR_PREFIX,
                                                                 "XQDY0041");

  /**
   * err:XQDY0044
   */
  public static final QNm ERR_ILLEGAL_NAME_OF_CONSTRUCTED_ATTRIBUTE = new QNm(Namespaces.ERR_NSURI,
                                                                              Namespaces.ERR_PREFIX,
                                                                              "err:XQDY0044");

  /**
   * err:XQDY0064
   */
  public static final QNm ERR_PI_TARGET_IS_XML = new QNm(Namespaces.ERR_NSURI, Namespaces.ERR_PREFIX, "XQDY0064");

  /**
   * err:XQDY0072
   */
  public static final QNm ERR_COMMENT_WOULD_CONTAIN_ILLEGAL_HYPHENS = new QNm(Namespaces.ERR_NSURI,
                                                                              Namespaces.ERR_PREFIX,
                                                                              "XQDY0072");

  /*
   * Error codes of the runtime. This should be placed in {@link Bits}, but
   * for compatibility reasons we leave it here.
   */
  public static final QNm BIT_DYN_RT_ILLEGAL_STATE_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0001");

  public static final QNm BIT_DYN_RT_OUT_OF_BOUNDS_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0002");

  public static final QNm BIT_DYN_RT_ILLEGAL_COMPARISON_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0003");

  public static final QNm BIT_DYN_RT_ILLEGAL_ARGUMENTS_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0004");

  public static final QNm BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0005");

  public static final QNm BIT_DYN_RT_STACK_OVERFLOW = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0006");

  public static final QNm BIT_DYN_ABORTED_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0007");

  public static final QNm BIT_DYN_DOCUMENT_ACCESS_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0200");

  public static final QNm BIT_DYN_INT_ERROR = new QNm(BIT_NSURI, BIT_PREFIX, "BIDY0300");
}