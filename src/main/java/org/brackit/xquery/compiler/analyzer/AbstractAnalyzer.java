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
package org.brackit.xquery.compiler.analyzer;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.AST;
import org.brackit.xquery.compiler.XQ;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.Whitespace;
import org.brackit.xquery.util.log.Logger;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.XMLChar;
import org.brackit.xquery.xdm.type.*;

/**
 * @author Sebastian Baechle
 *
 */
public abstract class AbstractAnalyzer {

    protected static final Logger log = Logger
            .getLogger(AbstractAnalyzer.class);

    protected enum DefaultNS {
        EMPTY, FUNCTION, ELEMENT_OR_TYPE, PRAGMA
    }

    protected StaticContext sctx;

    SequenceType sequenceType(AST stype) throws QueryException {
        AST type = stype.getChild(0);
        if (type.getType() == XQ.EmptySequenceType) {
            return SequenceType.EMPTY_SEQUENCE;
        }
        ItemType itype = itemType(type);
        Cardinality card = Cardinality.ZeroOrMany;
        if (stype.getChildCount() == 2) {
            card = occurrenceIndicator(stype.getChild(1));
        }
        return new SequenceType(itype, card);
    }

    protected ItemType anyKind(AST kind) throws QueryException {
        if (kind.getType() != XQ.ItemType) {
            return null;
        }
        return AnyItemType.ANY;
    }

    protected Cardinality occurrenceIndicator(AST card) {
        if (card.getType() == XQ.CardinalityZeroOrOne) {
            return Cardinality.ZeroOrOne;
        } else if (card.getType() == XQ.CardinalityZeroOrMany) {
            return Cardinality.ZeroOrMany;
        } else {
            return Cardinality.OneOrMany;
        }
    }

    ItemType itemType(AST itype) throws QueryException {
        ItemType type = kindTest(itype);
        type = (type != null) ? type : anyKind(itype);
        type = (type != null) ? type : functionTest(itype);
        type = (type != null) ? type : atomicOrUnionType(itype);
        type = (type != null) ? type : parenthesizedItemType(itype);
        return type;
    }

    protected ItemType functionTest(AST type) throws QueryException {
        if (type.getType() != XQ.FunctionTest) {
            return null;
        }
        int pos = 0;
        AST annotationOrTest = type.getChild(pos);
        while (annotationOrTest.getType() == XQ.Annotation) {
            QNm name = (QNm) annotationOrTest.getChild(0).getValue();
            // expands to default function namespace
            // if no prefix is present
            // expand and update AST
            name = expand(name, DefaultNS.FUNCTION);
            annotationOrTest.getChild(0).setValue(name);
            String uri = name.getNamespaceURI();
            if ((uri.equals(Namespaces.XML_NSURI))
                    || (uri.equals(Namespaces.XS_NSURI))
                    || (uri.equals(Namespaces.XSI_NSURI))
                    || (uri.equals(Namespaces.FN_NSURI))
                    || (uri.equals(Namespaces.FNMATH_NSURI))) {
                throw new QueryException(
                        ErrorCode.ERR_FUNCTION_DECL_IN_ILLEGAL_NAMESPACE,
                        "Function declaration %s is in illegal namespace: %s",
                        name, uri);
            }
        }
        if (annotationOrTest.getType() == XQ.AnyFunctionType) {
            SequenceType any = new SequenceType(AnyItemType.ANY,
                    Cardinality.ZeroOrMany);
            return new FunctionType(new Signature(any, true, null, any));
        } else if (annotationOrTest.getType() == XQ.TypedFunctionType) {
            return typedFunctionTest(annotationOrTest);
        } else {
            return null;
        }
    }

    protected ItemType typedFunctionTest(AST test) throws QueryException {
        SequenceType[] params = new SequenceType[test.getChildCount() - 1];
        for (int i = 0; i < test.getChildCount() - 1; i++) {
            params[i] = sequenceType(test.getChild(i));
        }
        SequenceType resType = sequenceType(test
                .getChild(test.getChildCount() - 1));
        return new FunctionType(new Signature(resType, params));
    }

    protected ItemType atomicOrUnionType(AST type) throws QueryException {
        QNm name = (QNm) type.getChild(0).getValue();
        // expand and update AST
        name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
        type.getChild(0).setValue(name);
        Type t = sctx.getTypes().resolveType(name);
        return new AtomicType(t);
    }

    protected ItemType parenthesizedItemType(AST type) throws QueryException {
        return itemType(type);
    }

    protected SequenceType singleType(AST type) throws QueryException {
        ItemType aouType = atomicOrUnionType(type.getChild(0));
        Cardinality card = Cardinality.One;
        if ((type.getChildCount() >= 2)
                && (type.getChild(1).getType() == XQ.CardinalityZeroOrOne)) {
            card = Cardinality.ZeroOrOne;
        }
        return new SequenceType(aouType, card);
    }

    protected ItemType kindTest(AST kindTest) throws QueryException {
        ItemType test = documentTest(kindTest);
        test = (test != null) ? test : elementTest(kindTest);
        test = (test != null) ? test : attributeTest(kindTest);
        test = (test != null) ? test : schemaElementTest(kindTest);
        test = (test != null) ? test : schemaAttributeTest(kindTest);
        test = (test != null) ? test : piTest(kindTest);
        test = (test != null) ? test : commentTest(kindTest);
        test = (test != null) ? test : textTest(kindTest);
        test = (test != null) ? test : namespaceNodeTest(kindTest);
        test = (test != null) ? test : structuredItemTest(kindTest);
        test = (test != null) ? test : jsonItemTest(kindTest);
        test = (test != null) ? test : recordTest(kindTest);
        test = (test != null) ? test : arrayTest(kindTest);
        test = (test != null) ? test : anyKindTest(kindTest);
        return test;
    }

    protected DocumentType documentTest(AST kindTest) throws QueryException {
        if (kindTest.getType() != XQ.KindTestDocument) {
            return null;
        }
        if (kindTest.getChildCount() == 0) {
            return new DocumentType();
        }
        AST child = kindTest.getChild(0);
        ElementType test = elementTest(child);
        test = (test != null) ? test : schemaElementTest(child);
        return new DocumentType(test);
    }

    protected ElementType elementTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestElement) {
            return null;
        }
        Type type = null;
        QNm name = null;
        if (test.getChildCount() >= 1) {
            AST child = test.getChild(0);
            if (child.getType() == XQ.Wildcard) {
                // name is already null
            } else if (child.getType() == XQ.QNm) {
                name = (QNm) child.getValue();
                // expand and update AST
                name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
                child.setValue(name);
            }
            if (test.getChildCount() >= 2) {
                child = test.getChild(1);
                QNm typeName = (QNm) child.getValue();
                // expand and update AST
                typeName = expand(typeName, DefaultNS.ELEMENT_OR_TYPE);
                child.setValue(typeName);
                type = sctx.getTypes().resolveType(typeName);
                if (test.getChildCount() >= 3) {
                    child = test.getChild(2);
                }
            }
        }
        return new ElementType(name, type);
    }

    protected AttributeType attributeTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestAttribute) {
            return null;
        }
        Type type = null;
        QNm name = null;
        if (test.getChildCount() >= 1) {
            AST child = test.getChild(0);
            if (child.getType() == XQ.Wildcard) {
                // name already is null
            } else if (child.getType() == XQ.QNm) {
                name = (QNm) child.getValue();
                // expand and update AST
                name = expand(name, DefaultNS.EMPTY);
                child.setValue(name);
            }
            if (test.getChildCount() >= 2) {
                child = test.getChild(1);
                QNm typeName = (QNm) child.getValue();
                // expand and update AST
                typeName = expand(typeName, DefaultNS.ELEMENT_OR_TYPE);
                child.setValue(typeName);
                type = sctx.getTypes().resolveType(typeName);
            }
        }
        return new AttributeType(name, type);
    }

    protected ElementType schemaElementTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestSchemaElement) {
            return null;
        }
        AST child = test.getChild(0);
        QNm name = (QNm) child.getValue();
        // expand and update AST
        name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
        child.setValue(name);
        Type type = sctx.getTypes().resolveType(name);
        return new ElementType(name, type);
    }

    protected AttributeType schemaAttributeTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestSchemaAttribute) {
            return null;
        }
        AST child = test.getChild(0);
        QNm name = (QNm) child.getValue();
        // expand and update AST
        name = expand(name, DefaultNS.ELEMENT_OR_TYPE);
        child.setValue(name);
        Type type = sctx.getTypes().resolveType(name);
        return new AttributeType(name, type);
    }

    protected ItemType piTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestPi) {
            return null;
        }
        String target = null;

        if (test.getChildCount() == 1) {
            target = test.getChild(0).getStringValue();
            target = Whitespace.normalizeXML11(target);
            target = Whitespace.collapse(target);
            if (!XMLChar.isNCName(target)) {
                throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                        "PI target name is not a valid NCName: %s", target);
            }
            test.getChild(0).setValue(target);
        }
        return new PIType(target);
    }

    protected ItemType commentTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestComment) {
            return null;
        }
        return new CommentType();
    }

    protected ItemType textTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestText) {
            return null;
        }
        return new TextType();
    }

    protected ItemType namespaceNodeTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestNamespaceNode) {
            return null;
        }
        throw new QueryException(
                ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                "Namespace test not implemented yet");
    }

    protected ItemType structuredItemTest(AST test) throws QueryException {
        if (test.getType() != XQ.StructuredItemTest) {
            return null;
        }
        return new AnyStructuredItemType();
    }

    protected ItemType jsonItemTest(AST test) throws QueryException {
        if (test.getType() != XQ.JsonItemTest) {
            return null;
        }
        return new AnyJsonItemType();
    }

    protected ItemType recordTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestRecord) {
            return null;
        }
        return new RecordType();
    }

    protected ItemType arrayTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestArray) {
            return null;
        }
        return new ArrayType();
    }

    protected ItemType anyKindTest(AST test) throws QueryException {
        if (test.getType() != XQ.KindTestAnyKind) {
            return null;
        }
        return AnyNodeType.ANY_NODE;
    }

    protected QNm expand(QNm name, DefaultNS mode) throws QueryException {
        // Don't try to expand when name Namespace URI is already
        // defined, e.g., an EQName or an internal special QName
        // like 'fs:dot'
        if (!name.getNamespaceURI().isEmpty()) {
            return name;
        }
        String prefix = name.getPrefix();
        String uri;
        Namespaces ns = sctx.getNamespaces();
        if (mode == DefaultNS.ELEMENT_OR_TYPE) {
            if (prefix.isEmpty()) {
                return new QNm(ns.getDefaultElementNamespace(), "",
                        name.getLocalName());
            } else if ((uri = ns.resolve(prefix)) != null) {
                return new QNm(uri, prefix, name.getLocalName());
            }
        } else if (mode == DefaultNS.FUNCTION) {
            if (prefix.isEmpty()) {
                return new QNm(ns.getDefaultFunctionNamespace(), "",
                        name.getLocalName());
            } else if ((uri = ns.resolve(prefix)) != null) {
                return new QNm(uri, prefix, name.getLocalName());
            }
        } else if (mode == DefaultNS.PRAGMA) {
            // pragmas aren't resolved to the empty default namespace
            if ((prefix.isEmpty()) && (!name.getNamespaceURI().isEmpty())) {
                return name;
            } else if ((uri = ns.resolve(prefix)) != null) {
                return new QNm(uri, prefix, name.getLocalName());
            }
        } else {
            if (prefix.isEmpty()) {
                return name;
            } else if ((uri = ns.resolve(prefix)) != null) {
                return new QNm(uri, prefix, name.getLocalName());
            }
        }
        throw new QueryException(ErrorCode.ERR_UNDEFINED_NAMESPACE_PREFIX,
                "Undefined namespace prefix: '%s'", prefix);
    }

    protected String resolvePrefix(String prefix) throws QueryException {
        String nsURI = sctx.getNamespaces().resolve(prefix);
        if (nsURI == null) {
            throw new QueryException(ErrorCode.ERR_UNDEFINED_NAMESPACE_PREFIX,
                    "Undefined namespace prefix: '%s'", prefix);
        }
        return nsURI;
    }
}