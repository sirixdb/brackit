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
package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.function.AbstractFunction;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Scope;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.XMLChar;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.module.StaticContext;
import io.brackit.query.util.Whitespace;

/**
 * Implements fn:resolve-QName($qname as xs:string?, $element as element()) as xs:QName? as per
 * https://www.w3.org/TR/xpath-functions-31/#func-resolve-QName.
 *
 * <p>The lexical QName is resolved against the in-scope namespaces of the supplied element.
 * Unlike the xs:QName constructor, an unprefixed name IS resolved against the element's default
 * namespace (if any); without a default namespace binding the resulting QName is in no
 * namespace.</p>
 */
public class ResolveQName extends AbstractFunction {

  public ResolveQName(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args[0] == null) {
      return null;
    }
    final String lexical = Whitespace.collapseTrimOnly(((Atomic) args[0]).stringValue());
    if (!XMLChar.isQName(lexical)) {
      throw new QueryException(ErrorCode.ERR_INVALID_LEXICAL_VALUE,
                               "Invalid lexical QName in fn:resolve-QName: '%s'",
                               lexical);
    }
    final Node<?> element = (Node<?>) args[1];
    if (element.getKind() != Kind.ELEMENT) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Second argument of fn:resolve-QName must be an element");
    }
    final Scope scope = element.getScope();

    final int colon = lexical.indexOf(':');
    if (colon < 0) {
      // an unprefixed QName resolves against the default namespace of the element
      final String defaultNs = scope.defaultNS();
      return new QNm(defaultNs == null ? "" : defaultNs, null, lexical);
    }
    final String prefix = lexical.substring(0, colon);
    final String local = lexical.substring(colon + 1);
    final String uri = scope.resolvePrefix(prefix);
    if (uri == null || uri.isEmpty()) {
      throw new QueryException(ErrorCode.ERR_NO_NAMESPACE_FOR_PREFIX,
                               "No namespace binding for prefix '%s' in scope of element %s",
                               prefix,
                               element.getName());
    }
    return new QNm(uri, prefix, local);
  }
}
