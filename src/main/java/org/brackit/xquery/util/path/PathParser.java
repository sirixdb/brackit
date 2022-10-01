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
package org.brackit.xquery.util.path;

import java.util.Map;
import java.util.TreeMap;

import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.compiler.parser.Tokenizer;

/**
 * @author Sebastian Baechle, Max Bechtold
 */
public final class PathParser extends Tokenizer {

  public enum Type {
    JSON,

    XML
  }

  private final Type type;

  private final Path<QNm> p;

  private Map<String, String> namespaces;

  public PathParser(String s) {
    super(s);
    p = new Path<>();
    type = Type.XML;
  }

  public PathParser(String s, Type type) {
    super(s);
    p = new Path<>();
    this.type = type;
  }

  /**
   * Parse a path
   *
   * @return the path instance
   * @throws PathException if anything went wrong
   */
  public Path<QNm> parse() {
    try {
      nsPreamble();
      attemptS();
      startStep();
      while (axisStep())
        ;
      if (type == Type.XML) {
        attributeStep();
      }
      consumeEOF();
      return p;
    } catch (TokenizerException e) {
      throw new PathException(e, e.getMessage());
    }
  }

  /**
   * Allows for inlined namespace declarations of the form ( "namespace"
   * NCNAME "=" stringLiteral ";")&#042; preceding a path expression.
   */
  private void nsPreamble() throws TokenizerException, PathException {
    Token la = laSymSkipS("namespace");
    while (la != null) {
      consume(la);
      String prefix;
      String uri;
      Token pfx = laNCNameSkipS();
      if (pfx == null) {
        throw new MismatchException("NCName");
      }
      prefix = pfx.toString();
      consume(pfx);

      if (!attemptSkipS("=")) {
        throw new MismatchException("=");
      }
      attemptS();
      Token uriToken = laString(false);
      if (uriToken == null) {
        throw new MismatchException("URI");
      }
      uri = uriToken.toString();
      consume(uriToken);

      if (!attemptSkipS(";")) {
        throw new MismatchException(";");
      }

      if (namespaces == null) {
        namespaces = new TreeMap<>();
      }
      if (namespaces.put(prefix, uri) != null) {
        throw new PathException("Multiple declaration of namespace prefix: '%s'", prefix);
      }
      la = laSymSkipS("namespace");
    }
  }

  private void startStep() throws PathException {
    if (attempt("..")) {
      p.parent();
    } else if (attempt(".")) {
      p.self();
    } else {
      Token token;
      try {
        token = laStringSkipWS(true);
      } catch (TokenizerException e) {
        throw new PathException(e, e.getMessage());
      }
      if (token != null) {
        consume(token);
        p.self().child(new QNm(token.string()));
      } else {
        EQNameToken la = laQName();
        if (la != null) {
          consume(la);
          p.self().child(expand(la.qname()));
        }
      }
    }
  }

  private boolean axisStep() throws TokenizerException, PathException {
    return parentStep() || selfStep() || namedStep();
  }

  private boolean parentStep() {
    if (attempt("/..")) {
      p.parent();
      return true;
    }
    return false;
  }

  private boolean selfStep() {
    if (attempt("/.")) {
      p.self();
      return true;
    }
    return false;
  }

  private boolean namedStep() throws TokenizerException, PathException {
    QNm q = null;
    Token la = la("//");
    if (la != null) {
      if (type == Type.XML && la(la, "@") != null) {
        return false;
      }

      consume(la);

      if (type == Type.JSON) {
        if ((la = la("[]")) != null) {
          consume(la);
          p.descendantArray();
          return true;
        }
      }

      if (!attempt("*")) {
        if (type == Type.XML) {
          Token token;
          try {
            token = laStringSkipWS(true);
          } catch (TokenizerException e) {
            throw new PathException(e, e.getMessage());
          }
          if (token != null) {
            consume(token);
            q = new QNm(token.string());
          } else {
            EQNameToken ela = laQName();
            if (ela == null) {
              throw new MismatchException("Wildcard", "QName");
            }
            consume(ela);
            q = ela.qname();
          }
        } else {
          q = name();
        }
      }

      if (type == Type.XML) {
        p.descendant(expand(q));
      } else {
        p.descendantObjectField(q);
      }
      return true;
    } else if ((la = la("/")) != null) {
      if (type == Type.XML && la(la, "@") != null) {
        return false;
      }

      consume(la);

      if (type == Type.JSON) {
        if ((la = la("[]")) != null) {
          consume(la);
          p.childArray();
          return true;
        }
      }

      if (!attempt("*")) {
        if (type == Type.XML) {
          Token token;
          try {
            token = laStringSkipWS(true);
          } catch (TokenizerException e) {
            throw new PathException(e, e.getMessage());
          }
          if (token != null) {
            consume(token);
            q = new QNm(token.string());
          } else {
            EQNameToken ela = laQName();
            if (ela == null) {
              throw new MismatchException("Wildcard", "QName");
            }
            consume(ela);
            q = ela.qname();
          }
        } else {
          q = name();
        }
      }
      if (type == Type.XML) {
        p.child(expand(q));
      } else {
        p.childObjectField(expand(q));
      }
      return true;
    }
    return false;
  }

  private QNm name() throws TokenizerException {
    var pos = position();
    final StringBuilder pathSegmentName = new StringBuilder(scanString(position(), '/'));

    int i = 0;
    boolean isEnd = false;
    while (pathSegmentName.toString().endsWith("\\")) {
      i++;
      if (pos + pathSegmentName.length() + i > getEnd()) {
        isEnd = true;
        break;
      }
      resetTo(pos + pathSegmentName.length() + i);
      pathSegmentName.delete(pathSegmentName.length() - 1, pathSegmentName.length());
      final var segment = scanString(position(), '/');

      if (segment != null) {
        pathSegmentName.append("/");
        pathSegmentName.append(segment);
      } else if (attempt("/") || (position() == getEnd() && getInput()[position() - 1] == '/')) {
        pathSegmentName.append("/");
      }
    }

    resetTo(pos);

    final EQNameToken token = new EQNameToken(pos,
                                              isEnd ? getEnd() : pos + pathSegmentName.length() + i,
                                              null,
                                              null,
                                              pathSegmentName.toString().replaceAll("\\\\\\[", "[").replaceAll("\\\\\\]", "]"));

    consume(token);

    return token.qname();
  }

  private void attributeStep() throws TokenizerException, PathException {
    QNm q = null;
    if (attempt("//@")) {
      if (!attempt("*")) {
        EQNameToken la = laQName();
        if (la == null) {
          throw new MismatchException("Wildcard", "QName");
        }
        consume(la);
        q = la.qname();
      }
      p.descendantAttribute(expand(q));
    } else if (attempt("/@")) {
      if (!attempt("*")) {
        EQNameToken la = laQName();
        if (la == null) {
          throw new MismatchException("Wildcard", "QName");
        }
        consume(la);
        q = la.qname();
      }
      p.attribute(expand(q));
    }
  }

  private String scanString(int pos, char escapeChar) {
    int e = pos;
    int s = e;
    int len = 0;
    char c;
    while (e < getEnd()) {
      c = getInput()[e++];
      if (c == escapeChar) {
        break;
      } else {
        len++;
      }
    }
    if (len == 0) {
      return null;
    }
    return new String(getInput(), s, len);
  }

  /**
   * Resolves prefixed named steps like 'bit:email' against the namespace
   * declarations in the preamble (if any).
   */
  private QNm expand(QNm qname) throws PathException {
    if (namespaces == null || qname.getPrefix() == null) {
      return qname;
    }
    String uri = namespaces.get(qname.getPrefix());
    if (uri == null) {
      throw new PathException("Undefined namespace prefix: %s", qname.getPrefix());
    }
    return new QNm(uri, qname.getPrefix(), qname.getLocalName());
  }
}