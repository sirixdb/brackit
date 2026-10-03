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
package io.brackit.query.atomic;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Type;
import io.brackit.query.util.simd.VectorOps;

import java.nio.charset.StandardCharsets;

/**
 * String atomic type with Unicode codepoint comparisons and SIMD acceleration.
 * UTF-8 comparisons require lossless encoding; strings with unpaired surrogates
 * use scalar comparison even if replacement-encoded bytes were cached.
 *
 * @author Sebastian Baechle
 */
public class Str extends AbstractAtomic {
  public static final Str EMPTY = new Str("");

  /**
   * Length threshold in UTF-16 code units for uncached SIMD comparisons.
   * Below this threshold, scalar operations are typically faster due to SIMD overhead.
   */
  private static final int SIMD_THRESHOLD = 32;

  private final String str;

  private final boolean utf8Lossless;

  /**
   * Lazily cached UTF-8 bytes for SIMD operations.
   * Thread-safe via immutable String + volatile write pattern.
   */
  private volatile byte[] utf8Cache;

  private class DStr extends Str {
    private final Type type;

    public DStr(String str, Type type) {
      super(str);
      this.type = type;
    }

    @Override
    public Type type() {
      return this.type;
    }
  }

  public Str(String str) {
    if (str == null)
      str = "";
    this.str = str;
    this.utf8Lossless = isUtf8Lossless(str);
  }

  private static boolean isUtf8Lossless(String value) {
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (Character.isHighSurrogate(ch)) {
        if (i + 1 == value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
          return false;
        }
        i++;
      } else if (Character.isLowSurrogate(ch)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get UTF-8 bytes with lazy caching.
   * Thread-safe via immutable String + volatile write.
   *
   * @return UTF-8 encoded bytes of this string
   */
  public byte[] getUtf8Bytes() {
    byte[] cached = utf8Cache;
    if (cached == null) {
      cached = str.getBytes(StandardCharsets.UTF_8);
      utf8Cache = cached;
    }
    return cached;
  }

  @Override
  public Type type() {
    return Type.STR;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    return new DStr(str, type);
  }

  @Override
  public Str asStr() {
    return this;
  }

  @Override
  public boolean booleanValue() throws QueryException {
    return (!str.isEmpty());
  }

  @Override
  public int cmp(Atomic other) throws QueryException {
    if (other instanceof Str s) {
      return cmpStr(s);
    }
    if (other instanceof Una || other instanceof AnyURI) {
      return compareCodepoints(str, other.stringValue());
    }
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s' with '%s'",
                             type(),
                             other.type());
  }

  /**
   * SIMD-accelerated string comparison with a scalar fallback.
   * Uses cached UTF-8 when both are available; uncached comparisons use
   * {@link #SIMD_THRESHOLD} to choose the path.
   *
   * @param other the string to compare with
   * @return negative if this < other, 0 if equal, positive if this > other
   */
  private int cmpStr(Str other) {
    if (this == other)
      return 0;

    if (!utf8Lossless || !other.utf8Lossless) {
      return compareCodepoints(str, other.str);
    }

    // Fast path: use cached UTF-8 if both are available
    byte[] a = utf8Cache;
    byte[] b = other.utf8Cache;

    if (a != null && b != null) {
      return VectorOps.stringCompare(a, b);
    }

    // Short string optimization - use scalar comparison
    String s1 = this.str;
    String s2 = other.str;
    if (s1.length() < SIMD_THRESHOLD && s2.length() < SIMD_THRESHOLD) {
      return compareCodepoints(s1, s2);
    }

    // SIMD path for longer strings
    return VectorOps.stringCompare(getUtf8Bytes(), other.getUtf8Bytes());
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    return atomic instanceof Str s ? cmpStr(s) : compareCodepoints(str, atomic.stringValue());
  }

  static int compareCodepoints(String left, String right) {
    int limit = Math.min(left.length(), right.length());
    for (int i = 0; i < limit;) {
      int leftCodepoint = left.codePointAt(i);
      int rightCodepoint = right.codePointAt(i);
      if (leftCodepoint != rightCodepoint) {
        return leftCodepoint - rightCodepoint;
      }
      i += Character.charCount(leftCodepoint);
    }
    return left.length() - right.length();
  }

  /**
   * SIMD-accelerated equality check with a scalar fallback.
   * More efficient than cmp() == 0 due to early exit on length mismatch.
   */
  @Override
  public boolean eq(Atomic other) throws QueryException {
    if (this == other)
      return true;
    if (!(other instanceof Str s)) {
      // Untyped atomics and xs:anyURI compare as strings, mirroring cmp(): general comparisons
      // deliberately skip the Una->Str cast upstream as an optimization, and xs:anyURI promotes
      // to xs:string. Returning false here made '"literal" = $untypedNode' ALWAYS false (while
      // the reverse direction worked via Una's cmp fallback) and broke anyURI '=' equality.
      if (other instanceof Una || other instanceof AnyURI) {
        return str.equals(other.stringValue());
      }
      return false;
    }

    // Quick length check on underlying strings
    if (str.length() != s.str.length())
      return false;

    // Short string optimization
    if (str.length() < SIMD_THRESHOLD || !utf8Lossless || !s.utf8Lossless) {
      return str.equals(s.str);
    }

    // Fast path: use cached UTF-8 if available
    byte[] a = utf8Cache;
    byte[] b = s.utf8Cache;

    if (a != null && b != null) {
      return VectorOps.stringEquals(a, b);
    }

    // SIMD path for longer strings
    return VectorOps.stringEquals(getUtf8Bytes(), s.getUtf8Bytes());
  }

  @Override
  public int atomicCode() {
    return Type.STRING_CODE;
  }

  @Override
  public String stringValue() {
    return str;
  }

  public Str concat(Str s) {
    return new Str(str + s.str);
  }

  @Override
  public int hashCode() {
    return str.hashCode();
  }
}
