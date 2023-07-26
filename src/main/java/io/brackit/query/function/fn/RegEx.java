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

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.module.StaticContext;
import io.brackit.query.util.Regex;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.function.AbstractFunction;

/**
 * Implementation of predefined functions fn:matches($arg1, $arg2),
 * fn:matches($arg1, $arg2, $arg3), fn:replace($arg1, $arg2, $arg3),
 * fn:replace($arg1, $arg2, $arg3, $arg4), fn:tokenize($arg1, $arg2), and
 * fn:tokenize($arg1, $arg2, $arg3) as per
 * http://www.w3.org/TR/xpath-functions/#func-matches,
 * http://www.w3.org/TR/xpath-functions/#func-replace, and
 * http://www.w3.org/TR/xpath-functions/#func-tokenize. Also note corrections in
 * http://www.w3.org/XML/2007/qt-errata/xpath-functions-errata.html.
 *
 * @author Max Bechtold
 */
public class RegEx extends AbstractFunction {

  private Regex.Mode mode;

  public RegEx(QNm name, Regex.Mode mode, Signature signature) {
    super(name, signature, true);
    this.mode = mode;
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    String input = (args[0] != null ? ((Atomic) args[0]).stringValue() : "");
    String pattern = ((Atomic) args[1]).stringValue();
    String replacement = (mode == Regex.Mode.REPLACE) ? ((Atomic) args[2]).stringValue() : null;
    String flags = (mode == Regex.Mode.REPLACE)
        ? (args.length > 3 ? ((Atomic) args[3]).stringValue() : null)
        : (args.length > 2 ? ((Atomic) args[2]).stringValue() : null);
    return Regex.match(mode, input, pattern, replacement, flags);
  }

}
