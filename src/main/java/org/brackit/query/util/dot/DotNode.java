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
package org.brackit.query.util.dot;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastian Baechle
 */
public class DotNode {
  private static class Row {
    final String port;
    final String name;
    final String value;

    Row(String port, String name, String value) {
      this.port = port;
      this.name = name;
      this.value = value;
    }
  }

  final String name;
  final List<DotNode.Row> rows = new ArrayList<DotNode.Row>();

  DotNode(String name) {
    this.name = name;
  }

  public void addRow(String port, String name, String value) {
    rows.add(new Row(port, name, value));
  }

  public void addRow(String name, String value) {
    rows.add(new Row(null, name, value));
  }

  public void addRow(String name, int value) {
    rows.add(new Row(null, name, String.valueOf(value)));
  }

  public void addRow(String name, long value) {
    rows.add(new Row(null, name, String.valueOf(value)));
  }

  public void addRow(String name, double value) {
    rows.add(new Row(null, name, String.format("%.3f", value)));
  }

  public String toDotString() {
    String head = String.format(
                                "%s [ label=<<TABLE PORT=\"port\" BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" ALIGN=\"CENTER\">",
                                DotContext.maskHTML(name));
    for (DotNode.Row r : rows) {
      if (r.value != null) {
        head += String.format("<TR><TD>%s</TD><TD>%s</TD></TR>",
                              DotContext.maskHTML(r.name),
                              DotContext.maskHTML(r.value));
      } else {
        head += String.format("<TR><TD COLSPAN=\"2\">%s</TD></TR>", DotContext.maskHTML(r.name));
      }
    }
    head += "</TABLE>>];";
    return head;
  }
}