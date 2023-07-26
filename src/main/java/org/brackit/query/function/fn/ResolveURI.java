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
package org.brackit.query.function.fn;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryContext;
import org.brackit.query.QueryException;
import org.brackit.query.atomic.AnyURI;
import org.brackit.query.atomic.QNm;
import org.brackit.query.atomic.Str;
import org.brackit.query.function.AbstractFunction;
import org.brackit.query.module.StaticContext;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Sequence;
import org.brackit.query.jdm.Signature;

/**
 * @author Sebastian Baechle
 */
public class ResolveURI extends AbstractFunction {
  public ResolveURI(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) throws QueryException {
    if (args[0] == null) {
      return null;
    }
    String relStr = ((Str) args[0]).stringValue();
    String baseStr = (args.length == 2) ? ((Str) args[1]).stringValue() : null;

    AnyURI relative;
    AnyURI base;
    try {
      relative = new AnyURI(relStr);
    } catch (DocumentException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_URI, "Invalid relative URI: %s", relStr);
    }
    try {
      base = (baseStr != null) ? new AnyURI(baseStr) : null;
    } catch (DocumentException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_URI, "Invalid base URI: %s", baseStr);
    }

    return resolve(sctx, base, relative);
  }

  public static AnyURI resolve(StaticContext sctx, AnyURI base, AnyURI relative) throws QueryException {
    if (relative.isAbsolute()) {
      return relative;
    }
    if (base == null) {
      base = sctx.getBaseURI();
      if (base == null) {
        throw new QueryException(ErrorCode.ERR_UNDEFINED_STATIC_BASE_URI, "Base-URI not defined in static context");
      }
    }
    if (!base.isAbsolute()) {
      throw new QueryException(ErrorCode.ERR_INVALID_URI, "Base URI is not an absolute URI: %s", base);
    }
    try {
      return relative.absolutize(base);
    } catch (Exception e) {
      throw new QueryException(e, ErrorCode.ERR_FN_RESOLVE_URI, "Error resolving URI %s against base URI %s");
    }
  }

  public static AnyURI resolve(StaticContext sctx, String relStr) throws QueryException {
    try {
      AnyURI relative = new AnyURI(relStr);
      return resolve(sctx, null, relative);
    } catch (DocumentException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_URI, "Invalid relative URI: %s", relStr);
    }
  }
}