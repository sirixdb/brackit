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

import java.util.Arrays;

import org.brackit.query.ErrorCode;
import org.brackit.query.QueryException;
import org.brackit.query.util.Whitespace;
import org.brackit.query.jdm.Type;

/**
 * @author Sebastian Baechle
 */
public class B64 extends AbstractAtomic {
  /*
   * Base64 Alphabet Value Encoding Value Encoding Value Encoding Value Encoding 0 A 17 R 34 i 51 z 1
   * B 18 S 35 j 52 0 2 C 19 T 36 k 53 1 3 D 20 U 37 l 54 2 4 E 21 V 38 m 55 3 5 F 22 W 39 n 56 4 6 G
   * 23 X 40 o 57 5 7 H 24 Y 41 p 58 6 8 I 25 Z 42 q 59 7 9 J 26 a 43 r 60 8 10 K 27 b 44 s 61 9 11 L
   * 28 c 45 t 62 + 12 M 29 d 46 u 63 / 13 N 30 e 47 v 14 O 31 f 48 w (pad) = 15 P 32 g 49 x 16 Q 33 h
   * 50 y
   */

  private final byte[] bytes;

  public B64(byte[] bytes) {
    this.bytes = bytes;
  }

  public B64(String str) throws QueryException {
    str = Whitespace.fullcollapse(str);

    if ((str.length() & 3) != 0) {
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:base64Binary", str);
    }

    int size = 0;
    int length = str.length();
    byte[] bytes = new byte[noOfOctets(str)];

    for (int charPos = 0; charPos < length;) {
      char c1 = str.charAt(charPos++);
      char c2 = str.charAt(charPos++);
      char c3 = str.charAt(charPos++);
      char c4 = str.charAt(charPos++);

      bytes[size++] = (byte) b64(str, c1);
      bytes[size++] = (byte) (c3 != '=' ? b64(str, c2) : b04(str, c2));

      if (c4 != '=') {
        bytes[size++] = (byte) b64(str, c3);
        bytes[size++] = (byte) b64(str, c4);
      } else {
        if (c3 != '=') {
          bytes[size] = (byte) b16(str, c3);
        }

        if (charPos != length) {
          throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:base64Binary", str);
        }
        break;
      }
    }
    this.bytes = bytes;
  }

  private int b04(String str, char c) throws QueryException {
    int v;
    if (c == 'A')
      v = 0;
    else if (c == 'Q')
      v = 16;
    else if (c == 'g')
      v = 32;
    else if (c == 'w')
      v = 48;
    else
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:base64Binary", str);
    return v;
  }

  private int b16(String str, char c) throws QueryException {
    int v;
    if (c == 'A')
      v = 0;
    else if (c == 'E')
      v = 4;
    else if (c == 'I')
      v = 8;
    else if (c == 'M')
      v = 12;
    else if (c == 'Q')
      v = 16;
    else if (c == 'U')
      v = 20;
    else if (c == 'Y')
      v = 24;
    else if (c == 'c')
      v = 28;
    else if (c == 'g')
      v = 32;
    else if (c == 'k')
      v = 36;
    else if (c == 'o')
      v = 40;
    else if (c == 's')
      v = 44;
    else if (c == 'w')
      v = 48;
    else if (c == '0')
      v = 52;
    else if (c == '4')
      v = 56;
    else if (c == '8')
      v = 60;
    else
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:base64Binary", str);
    return v;
  }

  private int noOfOctets(String str) {
    int dataLength = str.length();
    if (dataLength > 0 && str.charAt(dataLength - 1) == '=')
      dataLength--;
    if (dataLength > 0 && str.charAt(dataLength - 1) == '=')
      dataLength--;
    return dataLength;
  }

  private int b64(String str, char c) throws QueryException {
    int v;
    if (c >= '0' && c <= '9')
      v = 52 + c - 48;
    else if (c >= 'A' && c <= 'Z')
      v = c - 65;
    else if (c >= 'a' && c <= 'z')
      v = 26 + c - 87;
    else if (c == '+')
      v = 62;
    else if (c == '/')
      v = 63;
    else
      throw new QueryException(ErrorCode.ERR_INVALID_VALUE_FOR_CAST, "Cannot cast %s to xs:base64Binary", str);
    return v;
  }

  @Override
  public Atomic asType(Type type) throws QueryException {
    throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR);
  }

  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public int cmp(Atomic atomic) throws QueryException {
    throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             "Cannot compare '%s with '%s'",
                             type(),
                             atomic.type());
  }

  @Override
  public boolean eq(Atomic atomic) throws QueryException {
    if (!(atomic instanceof B64)) {
      throw new QueryException(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                               "Cannot compare '%s with '%s'",
                               type(),
                               atomic.type());
    }
    return Arrays.equals(bytes, ((B64) atomic).bytes);
  }

  @Override
  public int atomicCmpInternal(Atomic atomic) {
    byte[] bytes2 = ((B64) atomic).bytes;
    for (int i = 0; i < Math.min(bytes.length, bytes2.length); i++) {
      if (bytes[i] != bytes2[i]) {
        return (bytes[i] & 255) - (bytes2[i] & 255);
      }
    }
    return bytes.length - bytes2.length;
  }

  @Override
  public int atomicCode() {
    return Type.B64_CODE;
  }

  @Override
  public String stringValue() {
    StringBuilder out = new StringBuilder();
    for (byte aByte : bytes) {
      int v = aByte & 255;
      char c = (char) (v < 26 ? v + 65 : v < 52 ? v - 26 + 87 : v < 62 ? v - 52 + 48 : v == 62 ? '+' : '/');
      out.append(c);
    }
    return out.toString();
  }

  @Override
  public Type type() {
    return Type.B64;
  }

  @Override
  public boolean booleanValue() throws QueryException {
    throw new QueryException(ErrorCode.ERR_INVALID_ARGUMENT_TYPE,
                             "Effective boolean value of '%s' is undefined.",
                             type());
  }

  // public static void main(String[] args) throws QueryException
  // {
  // char[] SYMBOLS = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
  // 'J', 'K', 'L', 'M', 'N', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
  // 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
  // 'A', 'B', 'C', 'D', 'E', 'F'};
  // B64 h = new B64("+/ \t\n1 Q \t M M 8 ="); //new Hex(new byte[]{15,
  // (byte) 183});
  // for (int i = 0; i < SYMBOLS.length; i++)
  // {
  // System.out.println(SYMBOLS[i] + " " + ((int) SYMBOLS[i]));
  // }
  // for (int i = 0; i < SYMBOLS2.length; i++)
  // {
  // System.out.println(SYMBOLS2[i] + " " + ((int) SYMBOLS2[i]));
  // }
  // System.out.println(h.toString());
  // }
}
