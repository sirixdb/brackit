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
package org.brackit.query.atomic;

import java.net.URI;
import java.net.URL;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.util.Whitespace;
import org.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class AnyURI extends AbstractAtomic {
  public static final AnyURI EMPTY = new AnyURI((URI) null);
  public final URI uri;

  private class DAnyURI extends AnyURI {
    private final Type type;

    public DAnyURI(URI uri, Type type) {
      super(uri);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public AnyURI(String str) throws QueryException {
    if (str == null || (str = Whitespace.collapse(str)).isEmpty()) {
      this.uri = null;
    } else {
      try {
        // str = URLEncoder.encode(str, "UTF-8");
        this.uri = URI.create(str);
      } catch (Exception e) {
        throw new QueryException(e, ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast '%s' to xs:anyURI", str);
      }
    }
  }

  public AnyURI(URI uri) {
    this.uri = uri;
  }

  public static AnyURI fromString(String str) {
    return new AnyURI(URI.create(str));
  }

  public static boolean isValid(String str) {
    if (str == null || (str = Whitespace.collapse(str)).isEmpty()) {
      return false;
    }
    try {
      // str = URLEncoder.encode(str, "UTF-8");
      new URI(str);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public Type type() {
    return Type.AURI;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return new DAnyURI(uri, type);
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return uri != null;
  }

  @Override
  public int cmp(Atomic other) throws QueryException {
    if (other instanceof AnyURI || other instanceof Str) {
      return stringValue().compareTo(other.stringValue());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s' with '%s'",
                             type(),
                             other.type());
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    return stringValue().compareTo(atomic.stringValue());
  }

  @Override
  public int atomicCode() {
    return Type.STRING_CODE;
  }

  @Override
  public String stringValue() {
    return uri != null ? uri.toString() : "";
  }

  public boolean isAbsolute() {
    return uri != null && uri.isAbsolute();
  }

  @Override
  public int hashCode() {
    return uri != null ? uri.hashCode() : 0;
  }

  public AnyURI absolutize(AnyURI baseURI) throws QueryException {
    try {
      return new AnyURI(baseURI.uri.resolve(uri));
    } catch (IllegalArgumentException e) {
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }

  public URL toURL() throws QueryException {
    try {
      return uri.toURL();
    } catch (Exception e) {
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }
}
