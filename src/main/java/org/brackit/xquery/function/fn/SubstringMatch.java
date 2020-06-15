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

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;

/**
 * @author Sebastian Baechle
 */
public class SubstringMatch extends AbstractFunction {
  public enum Mode {
    CONTAINS, STARTS_WITH, ENDS_WITH
  }

  ;

  private final Mode mode;

  public SubstringMatch(QNm name, Mode mode, Signature signature) {
    super(name, signature, true);
    this.mode = mode;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args.length == 3) {
      Str collation = (Str) args[2];

      if (!collation.stringValue().equals("http://www.w3.org/2005/xpath-functions/collation/codepoint")) {
        throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION, "Unsupported collation: %s", collation);
      }
    }

    String str = (args[0] != null) ? ((Str) args[0]).stringValue() : "";
    String pattern = (args[1] != null) ? ((Str) args[1]).stringValue() : "";

    if (pattern.isEmpty()) {
      return Bool.TRUE;
    }
    if (str.isEmpty()) {
      return Bool.FALSE;
    }

    switch (mode) {
      case CONTAINS:
        return (str.contains(pattern)) ? Bool.TRUE : Bool.FALSE;
      case STARTS_WITH:
        return (str.startsWith(pattern)) ? Bool.TRUE : Bool.FALSE;
      case ENDS_WITH:
        return (str.endsWith(pattern)) ? Bool.TRUE : Bool.FALSE;
      default:
        throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR);
    }
  }
}