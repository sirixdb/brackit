package io.brackit.query.function.json;

import io.brackit.query.atomic.*;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Signature;
import io.brackit.query.jdm.json.JsonCollection;
import io.brackit.query.jdm.json.JsonItem;
import io.brackit.query.jdm.json.TemporalJsonCollection;
import io.brackit.query.module.StaticContext;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryContext;
import io.brackit.query.QueryException;
import io.brackit.query.function.AbstractFunction;

public final class Doc extends AbstractFunction {
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
      JsonItem document;
      if (name.isEmpty()) {
        if (retrieve) {
          throw new QueryException(ErrorCode.ERR_DOCUMENT_NOT_FOUND, "No default document defined.");
        } else {
          return Bool.FALSE;
        }
      } else {
        final AnyURI uri = resolve(sctx, name);
        final JsonCollection<?> collection = ctx.getJsonItemStore().lookup(uri.stringValue());
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

        if (collection instanceof TemporalJsonCollection<?> temporalJsonCollection) {
          document = temporalJsonCollection.getDocument(revision);
        } else {
          document = (JsonItem) collection.getDocument();
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