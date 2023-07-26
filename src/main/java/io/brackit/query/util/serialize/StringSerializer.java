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
package io.brackit.query.util.serialize;

import java.io.PrintStream;
import java.io.PrintWriter;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Null;
import io.brackit.query.atomic.Numeric;
import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Type;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.JsonItem;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jdm.node.Node;

/**
 * @author Sebastian Baechle
 */
@SuppressWarnings("unused")
public class StringSerializer implements Serializer {

  private static final String EOL = System.lineSeparator();

  private final PrintWriter out;
  private boolean format;
  private String indent = "  ";

  public StringSerializer(final PrintWriter out) {
    this.out = out;
  }

  public StringSerializer(final PrintStream out) {
    this.out = new PrintWriter(out);
  }

  public boolean isFormat() {
    return format;
  }

  public Serializer setFormat(final boolean format) {
    this.format = format;
    return this;
  }

  public String getIndent() {
    return indent;
  }

  public Serializer setIndent(final String indent) {
    this.indent = indent;
    return this;
  }

  @Override
  public void serialize(final Sequence s) throws QueryException {
    if (s == null) {
      return;
    }

    final SubtreePrinter printer = new SubtreePrinter(out);

    boolean first = true;
    int depth = 0;

    printer.setPrettyPrint(format);
    printer.setIndent(indent);
    printer.setAutoFlush(false);

    if (printJson(s, printer, depth)) {
      return;
    }

    Item item;

    try (final Iter it = s.iterate()) {
      while ((item = it.next()) != null) {
        if (item instanceof Node<?> node) {
          Kind kind = node.getKind();

          if (kind == Kind.ATTRIBUTE) {
            throw new QueryException(ErrorCode.ERR_SERIALIZE_ATTRIBUTE_OR_NAMESPACE_NODE);
          }

          if (kind == Kind.DOCUMENT) {
            node = node.getFirstChild();

            if (node != null) {
              while (node.getKind() != Kind.ELEMENT) {
                node = node.getNextSibling();
              }
            }
          }

          if (node != null) {
            printer.print(node);
          }

          first = true;
        } else if (item instanceof Atomic) {
          if (!first) {
            out.write(" ");
          }

          if (item instanceof JsonItem) {
            json(item, printer, false, depth);
          } else {
            out.write(item.toString());
          }

          first = false;
        } else if (item instanceof Array) {
          if (!first) {
            out.write(" ");
          }

          json(item, printer, true, depth);
          first = false;
        } else if (item instanceof Object) {
          if (!first) {
            out.write(" ");
          }

          json(item, printer, false, depth);
          first = false;
        } else {
          throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                                   "Serialization of item type '%s' not implemented yet.",
                                   item.itemType());
        }
      }
    } finally {
      printer.flush();
      out.flush();
    }
  }

  private boolean printJson(Sequence s, SubtreePrinter printer, int depth) {
    if (s instanceof Array) {
      try {
        json(s, printer, true, depth);
        return true;
      } finally {
        printer.flush();
        out.flush();
      }
    } else if (s instanceof Object) {
      try {
        json(s, printer, false, depth);
        return true;
      } finally {
        printer.flush();
        out.flush();
      }
    }

    return false;
  }

  private void json(Sequence sequence, SubtreePrinter p, boolean isArrayContent, int depth) throws QueryException {
    if (sequence == null || sequence instanceof Null) {
      out.print("null");
    } else if (sequence instanceof Item) {
      if (sequence instanceof Atomic) {
        if (sequence instanceof Numeric) {
          out.write(sequence.toString());
        } else if (((Atomic) sequence).type() == Type.BOOL) {
          out.write(sequence.booleanValue() ? "true" : "false");
        } else if (((Atomic) sequence).type() == Type.NULL) {
          out.write("null");
        } else {
          out.write("\"");
          out.write(sequence.toString());
          out.write("\"");
        }
      } else if (sequence instanceof Array array) {
        int arrayDepth = depth + 1;

        out.write("[");

        if (format && array.len() > 0) {
          out.write(EOL);
          out.write(indent(arrayDepth));
        }

        for (int i = 0; i < array.len(); i++) {
          if (i > 0) {
            out.append(",");

            if (format) {
              out.append(EOL);
              out.write(indent(arrayDepth));
            }
          }

          json(array.at(i), p, true, arrayDepth);
        }

        if (format && array.len() > 0) {
          out.write(EOL);
          out.write(indent(depth));
        }

        out.write("]");
      } else if (sequence instanceof Object object) {
        int objDepth = depth + 1;

        out.write("{");

        if (format && object.len() > 0) {
          out.write(EOL);
          out.write(indent(objDepth));
        }

        for (int i = 0; i < object.len(); i++) {
          if (i > 0) {
            out.write(",");

            if (format) {
              out.write(EOL);
              out.write(indent(objDepth));
            }
          }

          out.write("\"");
          out.write(object.name(i).stringValue());
          out.write("\":");

          if (format) {
            out.write(" ");
          }

          final var value = object.value(i);
          json(value, p, value instanceof Array, objDepth);
        }

        if (format && object.len() > 0) {
          out.write(EOL);
          out.write(indent(objDepth - 1));
        }

        out.write("}");
      } else if (sequence instanceof Node<?> node) {
        // TODO: We should serialize XML trees as JSON record...
        serializeNode(node, p, out);
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                                 "Serialization of item type '%sequence' not implemented yet.",
                                 ((Item) sequence).itemType());
      }
    } else {
      // Serialize sequence as JSON array
      if (!isArrayContent) {
        out.write("[");

        if (format && sequence.getSize() > 0) {
          out.write(EOL);
          out.write(indent(depth));
        }
      }

      try (Iter it = sequence.iterate()) {
        boolean first = true;
        Item i;

        while ((i = it.next()) != null) {
          if (!first) {
            out.write(",");

            if (format) {
              out.write(EOL);
              out.write(indent(depth));
            }
          }

          json(i, p, i instanceof Array, depth);
          first = false;
        }
      }

      if (!isArrayContent) {
        if (format && sequence.getSize() > 0) {
          out.write(EOL);
        }

        out.write("]");
      }
    }
  }

  static void serializeNode(Node<?> sequence, SubtreePrinter p, PrintWriter out) {
    Node<?> node = sequence;
    Kind kind = node.getKind();

    if (kind == Kind.ATTRIBUTE) {
      throw new QueryException(ErrorCode.ERR_SERIALIZE_ATTRIBUTE_OR_NAMESPACE_NODE);
    }

    if (kind == Kind.DOCUMENT) {
      node = node.getFirstChild();

      while (node.getKind() != Kind.ELEMENT) {
        node = node.getNextSibling();
      }
    }

    out.write("\"");
    p.print(node);
    out.write("\"");
  }

  /**
   * Gets the correct indentation for the supplied document depth.
   *
   * @param depth A document depth
   * @return A corresponding indentation
   */
  private String indent(final int depth) {
    if (depth < 0) {
      throw new IllegalArgumentException("Depth must be >= 0");
    }
    return String.valueOf(indent).repeat(depth);
  }
}
