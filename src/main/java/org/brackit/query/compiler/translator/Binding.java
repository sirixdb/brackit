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
package org.brackit.query.compiler.translator;

import java.io.PrintWriter;
import java.util.Arrays;

import org.brackit.query.atomic.QNm;
import org.brackit.query.util.log.Logger;
import org.brackit.query.jdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class Binding {
  private static final Logger log = Logger.getLogger(Binding.class);

  final QNm name;
  final SequenceType type;
  final Binding prev;
  Binding[] next;
  Reference[] refs;
  int refCount;
  int nextCount;

  Binding(QNm name, SequenceType type, Binding prev) {
    this.name = name;
    this.type = type;
    this.prev = prev;
  }

  public QNm getName() {
    return name;
  }

  public SequenceType getType() {
    return type;
  }

  void unchain() {
    prev.next[--prev.nextCount] = null;
    for (int i = 0; i < nextCount; i++) {
      prev.append(next[i]);
    }
  }

  void append(Binding binding) {
    if (next == null) {
      next = new Binding[2];
    }
    if (nextCount == next.length) {
      next = Arrays.copyOf(next, next.length * 3 / 2 + 1);
    }
    next[nextCount++] = binding;
  }

  void connect(Reference variable) {
    if (refs == null) {
      refs = new Reference[2];
    }
    if (refCount == refs.length) {
      refs = Arrays.copyOf(refs, refs.length * 3 / 2 + 1);
    }
    refs[refCount++] = variable;
  }

  public boolean isReferenced() {
    return refCount > 0;
  }

  void resolvePositions(int currentPos) {
    if (isReferenced()) {
      currentPos++;

      if (log.isTraceEnabled()) {
        log.trace(String.format("Setting reference pos of %s to %s", name, currentPos));
      }

      for (int i = 0; i < refCount; i++) {
        refs[i].setPos(currentPos);
      }
    }
    for (int i = 0; i < nextCount; i++) {
      next[i].resolvePositions(currentPos);
    }
  }

  public void dump(PrintWriter out) {
    dumpIndented(out, 0);
  }

  private void dumpIndented(PrintWriter out, int indent) {
    out.print(" -> ");
    out.print(name);
    out.print("(");
    out.print(refCount);
    out.print(" refs)");
    indent += 11 + name.toString().length() + Integer.toString(refCount).length();
    if (nextCount > 0) {
      next[0].dumpIndented(out, indent);
      for (int i = 1; i < nextCount; i++) {
        out.println();
        for (int j = 0; j < indent; j++) {
          out.print(" ");
        }
        next[i].dumpIndented(out, indent);
      }
    }
  }

  public String toString() {
    return name.toString();
  }
}