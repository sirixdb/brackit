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
package org.brackit.xquery.atomic;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class Una extends AbstractAtomic {
  public static final Una EMPTY = new Una("");

  public final String str;

  private static class DUna extends Una {
    private final Type type;

    public DUna(String str, Type type) {
      super(str);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public Una(String str) {
    if (str == null)
      str = "";
    this.str = str;
  }

  @Override
  public Type type() {
    return Type.UNA;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return new DUna(str, type);
  }

  @Override
  public Una asUna() {
    return this;
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return (!str.isEmpty());
  }

  @Override
  public int cmp(Atomic other) throws QueryException {
    if ((other instanceof Str) || (other instanceof Una)) // Optimization
    // treat
    // xs:untypedAtomic
    // as string to
    // avoid cast
    {
      return (str.compareTo(other.stringValue()));
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s' with '%s'",
                             type(),
                             other.type());
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    return (str.compareTo(atomic.stringValue()));
  }

  @Override
  public int atomicCode() {
    return Type.STRING_CODE;
  }

  @Override
  public String stringValue() {
    return str;
  }

  @Override
  public int hashCode() {
    return str.hashCode();
  }
}
