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
package org.brackit.xquery.util.serialize;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Null;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Type;
import org.brackit.xquery.xdm.json.Array;
import org.brackit.xquery.xdm.json.JsonItem;
import org.brackit.xquery.xdm.json.Object;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
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
        if (item instanceof Node<?>) {
          Node<?> node = (Node<?>) item;
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

  private void json(Sequence s, SubtreePrinter p, boolean isArrayContent, int depth) throws QueryException {
    if (s == null || s instanceof Null) {
      out.print("null");
    } else if (s instanceof Item) {
      if (s instanceof Atomic) {
        if (s instanceof Numeric) {
          out.write(s.toString());
        } else if (((Atomic) s).type() == Type.BOOL) {
          out.write(s.booleanValue() ? "true" : "false");
        } else if (((Atomic) s).type() == Type.NULL) {
          out.write("null");
        } else {
          out.write("\"");
          out.write(s.toString());
          out.write("\"");
        }
      } else if (s instanceof Array) {
	int arrayDepth = depth + 1;
        Array a = (Array) s;

        out.write("[");

        if (format && a.len() > 0) {
            out.write(EOL);
            out.write(indent(arrayDepth));
        }

        for (int i = 0; i < a.len(); i++) {
          if (i > 0) {
            out.append(",");

            if (format) {
              out.append(EOL);
              out.write(indent(arrayDepth));
            }
          }

          json(a.at(i), p, true, arrayDepth);
        }

        if (format && a.len() > 0) {
            out.write(EOL);
            out.write(indent(depth));
        }

        out.write("]");
      } else if (s instanceof Object) {
	int objDepth = depth + 1;
        Object r = (Object) s;

        out.write("{");

        if (format && r.len() > 0) {
          out.write(EOL);
          out.write(indent(objDepth));
        }

        for (int i = 0; i < r.len(); i++) {
          if (i > 0) {
            out.write(",");

            if (format) {
              out.write(EOL);
              out.write(indent(objDepth));
            }
          }

          out.write("\"");
          out.write(r.name(i).stringValue());
          out.write("\":");

          if (format) {
            out.write(" ");
          }

          final var value = r.value(i);
          json(value, p, value instanceof Array ? true : false, objDepth);
        }

        if (format && r.len() > 0) {
          out.write(EOL);
          out.write(indent(objDepth - 1));
        }

        out.write("}");
      } else if (s instanceof Node<?>) {
        // TODO: We should serialize XML trees as JSON record...
        Node<?> node = (Node<?>) s;
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
      } else {
        throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                                 "Serialization of item type '%s' not implemented yet.",
                                 ((Item) s).itemType());
      }
    } else {
      // Serialize sequence as JSON array
      if (!isArrayContent) {
        out.write("[");

        if (format && s.getSize() > 0) {
            out.write(EOL);
            out.write(indent(depth));
        }
      }

      Iter it = s.iterate();

      try {
        boolean first = true;
        Item i;

        while ((i = it.next()) != null) {
          if (!first) {
            out.write(",");
          }

          json(i, p, i instanceof Array ? true : false, depth);
          first = false;
        }
      } finally {
        it.close();
      }

      if (!isArrayContent) {
        if (format && s.getSize() > 0) {
          out.write(EOL);
        }

        out.write("]");
      }
    }
  }

  /**
   * Gets the correct indentation for the supplied document depth.
   *
   * @param depth A document depth
   * @return A corresponding indentation
   */
  private String indent(final int depth) {
      final StringBuilder indentation = new StringBuilder();

      for (int i = 0; i < depth; i++) {
	  indentation.append(indent);
      }

      return indentation.toString();
  }
}
