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
package org.brackit.xquery.util.serialize;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Bool;
import org.brackit.xquery.atomic.Numeric;
import org.brackit.xquery.jdm.json.Array;
import org.brackit.xquery.jdm.Item;
import org.brackit.xquery.jdm.Iter;
import org.brackit.xquery.jdm.Kind;
import org.brackit.xquery.jdm.node.Node;
import org.brackit.xquery.jdm.Record;
import org.brackit.xquery.jdm.Sequence;

/**
 * @author Sebastian Baechle
 *
 */
public class SerializationHandler {

  private final PrintWriter out;
  private final SubtreePrinter printer;
  private boolean format;
  private String indent = "    ";
  boolean first = true;

  public SerializationHandler(PrintWriter out) {
    this.out = out;
    printer = new SubtreePrinter(out);
    printer.setPrettyPrint(format);
    printer.setIndent(indent);
    printer.setAutoFlush(false);
  }

  public SerializationHandler(PrintStream out) {
    this(new PrintWriter(out));
  }

  public boolean isFormat() {
    return format;
  }

  public void setFormat(boolean format) {
    this.format = format;
  }

  public String getIndent() {
    return indent;
  }

  public void setIndent(String indent) {
    this.indent = indent;
  }

  public void begin() throws QueryException {
    first = true;
  }

  public void end() throws QueryException {
    printer.flush();
    out.flush();
  }

  public void node(Node<?> node) throws QueryException {
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
    printer.print(node);
    first = true;
  }

  public void atomic(Atomic atomic) throws QueryException {
    if (!first) {
      out.write(" ");
    }
    out.write(atomic.toString());
    first = false;
  }

  public void item(Item item) throws QueryException {
    if (item instanceof Node<?>) {
      Node<?> node = (Node<?>) item;
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

      printer.print(node);
      first = true;
    } else if (item instanceof Atomic) {
      if (!first) {
        out.write(" ");
      }
      out.write(item.toString());
      first = false;
    } else if ((item instanceof Array) || (item instanceof Record)) {
      json(item, printer);
    } else {
      throw new QueryException(ErrorCode.BIT_DYN_RT_NOT_IMPLEMENTED_YET_ERROR,
                               "Serialization of item type '%s' not implemented yet.",
                               item.itemType());
    }
  }

  private void json(Sequence s, SubtreePrinter p) throws QueryException {
    if (s == null) {
      out.print("null");
    } else if (s instanceof Item) {
      if (s instanceof Atomic) {
        if (s instanceof Numeric) {
          out.write(s.toString());
        } else if (s instanceof Bool) {
          out.write(((Bool) s).booleanValue() ? "true" : "false");
        } else {
          out.write("\"");
          out.write(s.toString());
          out.write("\"");
        }
      } else if (s instanceof Array) {
        Array a = (Array) s;
        out.write("[");
        for (int i = 0; i < a.len(); i++) {
          if (i > 0) {
            out.append(",");
          }
          json(a.at(i), p);
        }
        out.write("]");
      } else if (s instanceof Record) {
        Record r = (Record) s;
        out.write("{");
        for (int i = 0; i < r.len(); i++) {
          if (i > 0) {
            out.write(", ");
          }
          out.write(r.name(i).stringValue());
          out.write(" : ");
          json(r.value(i), p);
        }
        out.write("}");
      } else if (s instanceof Node<?>) {
        // TODO we should serialize XML trees as JSON record....
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
      // serialize sequence as JSON array
      out.write("[");
      Iter it = s.iterate();
      try {
        boolean first = true;
        Item i;
        while ((i = it.next()) != null) {
          if (!first) {
            out.write(",");
          }
          json(i, p);
          first = false;
        }
      } finally {
        it.close();
      }
      out.write("]");
    }
  }

}