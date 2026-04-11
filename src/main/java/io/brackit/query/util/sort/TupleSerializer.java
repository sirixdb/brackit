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
package io.brackit.query.util.sort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import io.brackit.query.QueryException;
import io.brackit.query.Tuple;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Dbl;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Null;
import io.brackit.query.atomic.Str;
import io.brackit.query.function.json.FastJSONParser;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.util.serialize.StringSerializer;

/**
 * Binary serialization for {@link Tuple} instances, used by external merge sort
 * and spillable aggregation operators.
 * <p>
 * Format per tuple:
 * <pre>
 * [int: width]
 * For each column:
 * [byte: type tag]
 * [variable: payload]
 * </pre>
 * <p>
 * Type tags:
 * <ul>
 * <li>0 = column is null (no Sequence)</li>
 * <li>1 = {@link Null} (JSONiq null)</li>
 * <li>2 = {@link Bool}</li>
 * <li>3 = {@link Int32}</li>
 * <li>4 = {@link Int64}</li>
 * <li>5 = {@link Dbl}</li>
 * <li>6 = {@link Str}</li>
 * <li>7 = JSON fallback (serialized as UTF-8 string, parsed back with FastJSONParser)</li>
 * </ul>
 */
public final class TupleSerializer {

  private static final byte TAG_ABSENT = 0;
  private static final byte TAG_NULL = 1;
  private static final byte TAG_BOOL = 2;
  private static final byte TAG_INT32 = 3;
  private static final byte TAG_INT64 = 4;
  private static final byte TAG_DBL = 5;
  private static final byte TAG_STR = 6;
  private static final byte TAG_JSON = 7;

  private TupleSerializer() {
  }

  /**
   * Write a tuple to the output stream.
   */
  public static void write(OutputStream out, Tuple tuple) throws IOException {
    DataOutputStream dos = (out instanceof DataOutputStream d) ? d : new DataOutputStream(out);
    int width = tuple.getSize();
    dos.writeInt(width);

    for (int i = 0; i < width; i++) {
      Sequence seq = tuple.get(i);
      writeSequence(dos, seq);
    }
  }

  /**
   * Read a tuple from the input stream. Returns null at end of stream.
   */
  public static Tuple read(InputStream in) throws IOException {
    DataInputStream dis = (in instanceof DataInputStream d) ? d : new DataInputStream(in);

    int width;
    try {
      width = dis.readInt();
    } catch (java.io.EOFException e) {
      return null; // end of stream
    }

    Sequence[] columns = new Sequence[width];
    for (int i = 0; i < width; i++) {
      columns[i] = readSequence(dis);
    }
    return new TupleImpl(columns);
  }

  /**
   * Estimate the serialized byte size of a tuple (for memory budget tracking).
   */
  public static long estimateSize(Tuple tuple) {
    long size = 4; // width int
    int width = tuple.getSize();
    for (int i = 0; i < width; i++) {
      Sequence seq = tuple.get(i);
      size += 1; // type tag
      if (seq == null) {
        continue;
      }
      if (seq instanceof Int32) {
        size += 4;
      } else if (seq instanceof Int64) {
        size += 8;
      } else if (seq instanceof Dbl) {
        size += 8;
      } else if (seq instanceof Str s) {
        size += 4 + s.stringValue().length() * 2L; // length prefix + chars
      } else if (seq instanceof Bool) {
        size += 1;
      } else if (seq instanceof Null) {
        // tag only
      } else {
        size += 64; // rough estimate for complex types
      }
    }
    return size;
  }

  // ==================== Internal ====================

  private static void writeSequence(DataOutputStream dos, Sequence seq) throws IOException {
    if (seq == null) {
      dos.writeByte(TAG_ABSENT);
      return;
    }

    if (seq instanceof Null) {
      dos.writeByte(TAG_NULL);
    } else if (seq instanceof Bool b) {
      dos.writeByte(TAG_BOOL);
      dos.writeBoolean(b.booleanValue());
    } else if (seq instanceof Int32 n) {
      dos.writeByte(TAG_INT32);
      dos.writeInt(n.intValue());
    } else if (seq instanceof Int64 n) {
      dos.writeByte(TAG_INT64);
      dos.writeLong(n.longValue());
    } else if (seq instanceof Dbl d) {
      dos.writeByte(TAG_DBL);
      dos.writeDouble(d.doubleValue());
    } else if (seq instanceof Str s) {
      dos.writeByte(TAG_STR);
      byte[] bytes = s.stringValue().getBytes(StandardCharsets.UTF_8);
      dos.writeInt(bytes.length);
      dos.write(bytes);
    } else {
      // Fallback: serialize as JSON string
      dos.writeByte(TAG_JSON);
      String json = serializeToJson(seq);
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      dos.writeInt(bytes.length);
      dos.write(bytes);
    }
  }

  private static Sequence readSequence(DataInputStream dis) throws IOException {
    byte tag = dis.readByte();
    return switch (tag) {
      case TAG_ABSENT -> null;
      case TAG_NULL -> Null.INSTANCE;
      case TAG_BOOL -> dis.readBoolean() ? Bool.TRUE : Bool.FALSE;
      case TAG_INT32 -> {
        int v = dis.readInt();
        yield (v >= 0 && v <= 20) ? Int32.ZERO_TO_TWENTY[v] : new Int32(v);
      }
      case TAG_INT64 -> new Int64(dis.readLong());
      case TAG_DBL -> new Dbl(dis.readDouble());
      case TAG_STR -> {
        int len = dis.readInt();
        byte[] bytes = dis.readNBytes(len);
        yield new Str(new String(bytes, StandardCharsets.UTF_8));
      }
      case TAG_JSON -> {
        int len = dis.readInt();
        byte[] bytes = dis.readNBytes(len);
        try {
          yield new FastJSONParser(bytes).parse();
        } catch (QueryException e) {
          throw new IOException("Failed to deserialize JSON tuple column", e);
        }
      }
      default -> throw new IOException("Unknown tuple column type tag: " + tag);
    };
  }

  private static String serializeToJson(Sequence seq) {
    StringWriter sw = new StringWriter();
    try (StringSerializer ser = new StringSerializer(new PrintWriter(sw))) {
      if (seq instanceof Item item) {
        ser.serialize(item);
      } else {
        // Multi-item sequence: serialize as JSON array
        sw.write("[");
        boolean first = true;
        try (var iter = seq.iterate()) {
          Item item;
          while ((item = iter.next()) != null) {
            if (!first)
              sw.write(",");
            ser.serialize(item);
            first = false;
          }
        }
        sw.write("]");
      }
    }
    return sw.toString();
  }
}
