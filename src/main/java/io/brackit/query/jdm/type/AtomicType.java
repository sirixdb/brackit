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
package io.brackit.query.jdm.type;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class AtomicType implements ItemType {
  /**
   * xs:boolean
   */
  public static final AtomicType BOOL = new AtomicType(Type.BOOL);

  /**
   * xs:string
   */
  public static final AtomicType STR = new AtomicType(Type.STR);

  /**
   * xs:double
   */
  public static final AtomicType DBL = new AtomicType(Type.DBL);

  /**
   * xs:int
   */
  public static final AtomicType INT = new AtomicType(Type.INT);

  /**
   * xs:integer
   */
  public static final AtomicType INR = new AtomicType(Type.INR);

  /**
   * xs:decimal
   */
  public static final AtomicType DEC = new AtomicType(Type.DEC);

  /**
   * xs:dateTime
   */
  public static final AtomicType DATI = new AtomicType(Type.DATI);

  /**
   * xs:date
   */
  public static final AtomicType DATE = new AtomicType(Type.DATE);

  /**
   * xs:time
   */
  public static final AtomicType TIME = new AtomicType(Type.TIME);

  /**
   * xs:duration
   */
  public static final AtomicType DUR = new AtomicType(Type.DUR);

  /**
   * xs:dayTimeDuration
   */
  public static final AtomicType DTD = new AtomicType(Type.DTD);

  /**
   * xs:yearMonthDuration
   */
  public static final AtomicType YMD = new AtomicType(Type.YMD);

  /**
   * xs:QName
   */
  public static final AtomicType QNM = new AtomicType(Type.QNM);

  /**
   * xs:anyURI
   */
  public static final AtomicType AURI = new AtomicType(Type.AURI);

  /**
   * xs:anyAtomicType
   */
  public static final AtomicType ANA = new AtomicType(Type.ANA);

  private final Type type;

  public AtomicType(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  @Override
  public boolean isAnyItem() {
    return false;
  }

  @Override
  public boolean isAtomic() {
    return true;
  }

  @Override
  public boolean isNode() {
    return false;
  }

  @Override
  public boolean isFunction() {
    return false;
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isObject() {
    return false;
  }

  @Override
  public boolean isJsonItem() {
    return false;
  }

  @Override
  public boolean isStructuredItem() {
    return false;
  }

  @Override
  public boolean matches(Item item) throws QueryException {
    return item instanceof Atomic && ((Atomic) item).type().instanceOf(type);
  }

  public boolean instanceOf(AtomicType other) {
    return type.instanceOf(other.type);
  }

  @Override
  public String toString() {
    return type.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || obj instanceof AtomicType && ((AtomicType) obj).type.equals(type);
  }
}
