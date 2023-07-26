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

import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.jdm.node.NodeCollection;
import io.brackit.query.jdm.node.TemporalNodeCollection;
import io.brackit.query.module.StaticContext;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.AnyURI;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.function.AbstractFunction;

/**
 * Implementation of predefined functions fn:doc($arg1) and
 * fn:doc-available($arg1) as per http://www.w3.org/TR/xpath-functions/#func-doc
 * and http://www.w3.org/TR/xpath-functions/#func-doc-available. Also note
 * correction in
 * http://www.w3.org/XML/2007/qt-errata/xpath-functions-errata.html#E26
 *
 * @author Sebastian Baechle
 * @author Max Bechtold
 * @author Johannes Lichtenberger
 */
public class Doc extends AbstractFunction {
  private final boolean retrieve;

  public Doc(final QNm name, final boolean retrieve, final Signature signature) {
    super(name, signature, true);
    this.retrieve = retrieve;
  }

  @Override
  public Sequence execute(final StaticContext sctx, final QueryContext ctx, final Sequence[] args)
      throws QueryException {
    if (args[0] == null) {
      return retrieve ? null : Bool.FALSE;
    }

    final String name = ((Str) args[0]).stringValue();
    final int revision = args.length == 2 ? ((Int32) args[1]).intValue() : -1;

    try {
      Node<?> document;
      if (name.isEmpty()) {
        // Implementation defined: Handle empty name in the sense of
        // fn:doc() which is in fact not defined by XQuery
        document = ctx.getDefaultDocument();

        if (document == null) {
          if (retrieve) {
            throw new QueryException(ErrorCode.ERR_DOCUMENT_NOT_FOUND, "No default document defined.");
          } else {
            return Bool.FALSE;
          }
        }

        return document;
      } else {
        final AnyURI uri = resolve(sctx, name);
        final NodeCollection<?> collection = ctx.getNodeStore().lookup(uri.stringValue());
        final long documents = collection.getDocumentCount();

        if (documents == 0) {
          if (retrieve) {
            throw new QueryException(ErrorCode.ERR_DOCUMENT_NOT_FOUND, "Empty collection");
          } else {
            return Bool.FALSE;
          }
        }

        if (documents > 1) {
          throw new QueryException(ErrorCode.ERR_DOCUMENT_NOT_FOUND,
                                   "Collection %s contains more than one document",
                                   name);
        }

        if (collection instanceof TemporalNodeCollection<?> temporalNodeCollection) {
          document = temporalNodeCollection.getDocument(revision);
        } else {
          document = collection.getDocument();
        }

        if (document == null) {
          if (retrieve) {
            throw new QueryException(ErrorCode.ERR_DOCUMENT_NOT_FOUND, "Empty collection");
          } else {
            return Bool.FALSE;
          }
        }

        if (retrieve) {
          return document;
        } else {
          return Bool.FALSE;
        }
      }
    } catch (DocumentException e) {
      if (retrieve) {
        throw new QueryException(e, ErrorCode.ERR_DOCUMENT_NOT_FOUND, "Document '%s' not found.", name);
      } else {
        return Bool.FALSE;
      }
    }
  }

  static AnyURI resolve(StaticContext sctx, AnyURI base, AnyURI relative) throws QueryException {
    if (relative.isAbsolute()) {
      return relative;
    }
    if (base == null) {
      base = sctx.getBaseURI();
      if (base == null || !base.isAbsolute()) {
        return relative;
      }
    }
    try {
      return relative.absolutize(base);
    } catch (Exception e) {
      throw new QueryException(e, ErrorCode.ERR_FN_RESOLVE_URI, "Error resolving URI %s against base URI %s");
    }
  }

  static AnyURI resolve(StaticContext sctx, String relStr) throws QueryException {
    try {
      AnyURI relative = new AnyURI(relStr);
      return resolve(sctx, null, relative);
    } catch (DocumentException e) {
      throw new QueryException(ErrorCode.ERR_INVALID_URI, "Invalid relative URI: %s", relStr);
    }
  }
}