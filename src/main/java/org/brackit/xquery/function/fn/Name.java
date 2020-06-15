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
package org.brackit.xquery.function.fn;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.AnyURI;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.node.Node;

/**
 * Implementation of predefined functions fn:name(), fn:name($arg1),
 * fn:local-name(), fn:local-name($arg1), fn:namespace-uri(), and
 * fn:namespace-uri($arg1) as per
 * http://www.w3.org/TR/xpath-functions/#func-name,
 * http://www.w3.org/TR/xpath-functions/#func-local-name, and
 * http://www.w3.org/TR/xpath-functions/#func-namespace-uri
 *
 * @author Max Bechtold
 */
public class Name extends AbstractFunction {
  public static enum Mode {
    NAME, LOCAL_NAME, NAMESPACE_URI
  }

  ;

  private Mode mode;

  public Name(QNm name, Mode mode, Signature signature) {
    super(name, signature, true);
    this.mode = mode;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    QNm name;
    if ((args[0] == null) || (name = ((Node<?>) args[0]).getName()) == null) {
      return (mode == Mode.NAMESPACE_URI ? AnyURI.EMPTY : Str.EMPTY);
    }

    switch (mode) {
      case LOCAL_NAME:
        return new Str(name.getLocalName());
      case NAME:
        return new Str(name.toString());
      default:
        return new AnyURI(name.getNamespaceURI());
    }
  }
}