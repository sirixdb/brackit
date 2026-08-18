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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import io.brackit.query.ErrorCode;
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
import io.brackit.query.util.Cfg;
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
 * <li>8 = reference into the {@link SpillContext} of the spill session (see below)</li>
 * </ul>
 *
 * <h2>Items that must not be copied</h2>
 * The JSON fallback renders an item as text, which is fine for a record and ruinous for a binding
 * that holds a whole document: a pipeline tuple carries every variable in scope, including the
 * {@code let $doc := jn:doc(...)} the query started from, so copying such a column per spilled row
 * would serialize the database once per row. A caller that spills within one JVM — every caller
 * here does, the files are its own temporaries — passes a {@link SpillContext}; a column whose
 * rendering would run past the {@link #MAX_INLINE_CHARS_CFG} character limit is then written as a
 * reference to the live item instead, which is both cheaper and exact (a referenced item keeps its
 * identity, a re-parsed one does not).
 */
public final class TupleSerializer {

  /** Config key: rendering length above which a column is spilled by reference. */
  public static final String MAX_INLINE_CHARS_CFG = "io.brackit.query.spill.max_inline_chars";

  /** Config key: how many distinct oversized items one spill session may hold on to. */
  public static final String MAX_REFS_CFG = "io.brackit.query.spill.max_refs";

  private static final int MAX_INLINE_CHARS = Cfg.asInt(MAX_INLINE_CHARS_CFG, 64 * 1024);

  private static final int MAX_REFS = Cfg.asInt(MAX_REFS_CFG, 4096);

  private static final byte TAG_ABSENT = 0;
  private static final byte TAG_NULL = 1;
  private static final byte TAG_BOOL = 2;
  private static final byte TAG_INT32 = 3;
  private static final byte TAG_INT64 = 4;
  private static final byte TAG_DBL = 5;
  private static final byte TAG_STR = 6;
  private static final byte TAG_JSON = 7;
  private static final byte TAG_REF = 8;

  private TupleSerializer() {
  }

  /**
   * The live items of one spill session, referenced by the tuples it wrote. Valid only for files
   * written and read by the same session, which is what an operator's own spill files are.
   * <p>
   * Not thread-safe: a spill session belongs to one cursor.
   */
  public static final class SpillContext {
    private final Map<Sequence, Integer> handles = new IdentityHashMap<>();
    private final List<Sequence> items = new ArrayList<>();

    /** The handle of an item already referenced by this session, or -1. */
    int handleOf(Sequence seq) {
      final Integer handle = handles.get(seq);
      return handle == null ? -1 : handle;
    }

    /** Hands out a handle for an item this session has to keep alive. */
    int reference(Sequence seq) throws IOException {
      if (items.size() >= MAX_REFS) {
        throw new IOException("cannot spill: more than " + MAX_REFS + " oversized items would have to be kept in "
            + "memory (raise " + MAX_REFS_CFG + " or lower " + MAX_INLINE_CHARS_CFG + ")");
      }
      final int handle = items.size();
      items.add(seq);
      handles.put(seq, handle);
      return handle;
    }

    Sequence resolve(int handle) throws IOException {
      if (handle < 0 || handle >= items.size()) {
        throw new IOException("spilled tuple references item " + handle + ", which this session never wrote");
      }
      return items.get(handle);
    }

    /** Number of live items this session holds on to. */
    public int referencedItems() {
      return items.size();
    }

    /** Releases the referenced items. The session's spill files are unreadable afterwards. */
    public void clear() {
      handles.clear();
      items.clear();
    }
  }

  /**
   * Write a tuple to the output stream, copying every column.
   */
  public static void write(OutputStream out, Tuple tuple) throws IOException {
    write(out, tuple, null);
  }

  /**
   * Write a tuple to the output stream, referencing columns too large to copy through
   * {@code ctx}.
   */
  public static void write(OutputStream out, Tuple tuple, SpillContext ctx) throws IOException {
    DataOutputStream dos = (out instanceof DataOutputStream d) ? d : new DataOutputStream(out);
    int width = tuple.getSize();
    dos.writeInt(width);

    for (int i = 0; i < width; i++) {
      Sequence seq = tuple.get(i);
      writeSequence(dos, seq, ctx);
    }
  }

  /**
   * Read a tuple from the input stream. Returns null at end of stream.
   */
  public static Tuple read(InputStream in) throws IOException {
    return read(in, null);
  }

  /**
   * Read a tuple written by the same spill session. Returns null at end of stream.
   */
  public static Tuple read(InputStream in, SpillContext ctx) throws IOException {
    DataInputStream dis = (in instanceof DataInputStream d) ? d : new DataInputStream(in);

    int width;
    try {
      width = dis.readInt();
    } catch (java.io.EOFException e) {
      return null; // end of stream
    }

    Sequence[] columns = new Sequence[width];
    for (int i = 0; i < width; i++) {
      columns[i] = readSequence(dis, ctx);
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

  private static void writeSequence(DataOutputStream dos, Sequence seq, SpillContext ctx) throws IOException {
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
    } else if (ctx == null) {
      // Fallback: serialize as JSON string
      dos.writeByte(TAG_JSON);
      String json = serializeToJson(seq);
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      dos.writeInt(bytes.length);
      dos.write(bytes);
    } else {
      final int known = ctx.handleOf(seq);
      if (known >= 0) {
        dos.writeByte(TAG_REF);
        dos.writeInt(known);
        return;
      }
      final String json = serializeToJson(seq, MAX_INLINE_CHARS);
      if (json == null) {
        // Too large to copy — hold on to the item itself and write a reference to it.
        dos.writeByte(TAG_REF);
        dos.writeInt(ctx.reference(seq));
        return;
      }
      dos.writeByte(TAG_JSON);
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      dos.writeInt(bytes.length);
      dos.write(bytes);
    }
  }

  private static Sequence readSequence(DataInputStream dis, SpillContext ctx) throws IOException {
    byte tag = dis.readByte();
    return switch (tag) {
      case TAG_REF -> {
        final int handle = dis.readInt();
        if (ctx == null) {
          throw new IOException("spilled tuple references a live item, but no spill context was given");
        }
        yield ctx.resolve(handle);
      }
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

  /**
   * Renders an item as JSON, or returns {@code null} once the rendering passes {@code maxChars} —
   * the signal to spill it by reference. Stopping early is what keeps the decision cheap: a
   * document-sized column costs {@code maxChars}, not its full text, and only on first sight.
   */
  private static String serializeToJson(Sequence seq, int maxChars) {
    final CappedWriter capped = new CappedWriter(maxChars);
    try (StringSerializer ser = new StringSerializer(new PrintWriter(capped))) {
      serializeTo(ser, capped, seq);
    } catch (CappedWriter.LimitReached e) {
      return null;
    }
    return capped.isOverLimit() ? null : capped.toString();
  }

  /**
   * A {@link StringWriter} that gives up instead of growing without bound. The serializer writes
   * through a {@link PrintWriter}, which swallows {@link IOException}, so the limit is signalled
   * with an unchecked throw.
   */
  private static final class CappedWriter extends Writer {
    /** Thrown once the cap is passed; carries no stack trace, it is control flow, not a failure. */
    static final class LimitReached extends RuntimeException {
      private static final long serialVersionUID = 1L;

      LimitReached() {
        super(null, null, false, false);
      }
    }

    private final StringBuilder builder = new StringBuilder();
    private final int maxChars;
    private boolean overLimit;

    CappedWriter(int maxChars) {
      this.maxChars = maxChars;
    }

    @Override
    public void write(char[] buf, int off, int len) {
      if (builder.length() + len > maxChars) {
        overLimit = true;
        throw new LimitReached();
      }
      builder.append(buf, off, len);
    }

    @Override
    public void write(String str, int off, int len) {
      if (builder.length() + len > maxChars) {
        overLimit = true;
        throw new LimitReached();
      }
      builder.append(str, off, off + len);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    boolean isOverLimit() {
      return overLimit;
    }

    @Override
    public String toString() {
      return builder.toString();
    }
  }

  private static String serializeToJson(Sequence seq) {
    StringWriter sw = new StringWriter();
    try (StringSerializer ser = new StringSerializer(new PrintWriter(sw))) {
      serializeTo(ser, sw, seq);
    }
    return sw.toString();
  }

  private static void serializeTo(StringSerializer ser, Writer out, Sequence seq) {
    if (seq instanceof Item item) {
      ser.serialize(item);
      return;
    }
    // Multi-item sequence: serialize as JSON array
    writeRaw(out, "[");
    boolean first = true;
    try (var iter = seq.iterate()) {
      Item item;
      while ((item = iter.next()) != null) {
        if (!first) {
          writeRaw(out, ",");
        }
        ser.serialize(item);
        first = false;
      }
    }
    writeRaw(out, "]");
  }

  private static void writeRaw(Writer out, String s) {
    try {
      out.write(s);
    } catch (IOException e) {
      // Both writers used here buffer in memory; neither can actually fail.
      throw new QueryException(e, ErrorCode.BIT_DYN_INT_ERROR);
    }
  }
}
