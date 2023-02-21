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
package org.brackit.xquery.compiler.translator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.expr.BoundVariable;
import org.brackit.xquery.expr.Variable;
import org.brackit.xquery.module.Module;
import org.brackit.xquery.util.log.Logger;
import org.brackit.xquery.jdm.type.SequenceType;

/**
 * @author Sebastian Baechle
 */
public class VariableTable {
  private static final Logger log = Logger.getLogger(VariableTable.class);

  Binding[][] bTable;
  int bLength;
  int bTableCounts;
  Module module;

  public VariableTable(Module module) {
    bTable = new Binding[1][3];
    this.module = module;
  }

  /**
   * Resolve bound variable and connect it to provided reference
   *
   * @param name
   * @param ref
   * @throws QueryException
   */
  public void resolve(QNm name, Reference ref) throws QueryException {
    if (log.isTraceEnabled()) {
      log.trace(String.format("Resolving %s", name));
    }

    for (int i = bLength - 1; i > -1; i--) {
      if (bTable[bTableCounts][i].name.equals(name)) {
        bTable[bTableCounts][i].connect(ref);
        return;
      }
    }
    log.error(dumpTable());
    throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Cannot resolve var %s", name);
  }

  /**
   * Resolve variable and create variable access expression
   *
   * @param name
   * @return
   * @throws QueryException
   */
  public Variable resolve(QNm name) throws QueryException {
    if (log.isTraceEnabled()) {
      log.trace(String.format("Resolving %s", name));
    }

    for (int i = bLength - 1; i > -1; i--) {
      if (bTable[bTableCounts][i].name.equals(name)) {
        BoundVariable variable = new BoundVariable(name, bTable[bTableCounts][i].type);
        bTable[bTableCounts][i].connect(variable);
        return variable;
      }
    }

    Variable varRef = module.getVariables().resolve(name);
    if (varRef != null) {
      return varRef;
    }
    throw new QueryException(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, "Could not resolve variable %s", name);
  }

  public Binding bind(QNm name, SequenceType type) {
    if (log.isTraceEnabled()) {
      log.trace(String.format("Binding %s", name));
    }

    if (bLength == bTable[bTableCounts].length) {
      bTable[bTableCounts] = Arrays.copyOf(bTable[bTableCounts], ((bTable[bTableCounts].length * 3) / 2 + 1));
    }
    Binding binding = new Binding(name, type, (bLength > 0) ? bTable[bTableCounts][bLength - 1] : null);

    // chain new binding with previous in binding or set as new root binding
    if (bLength > 0) {
      bTable[bTableCounts][bLength - 1].append(binding);
    } else if (bTable[bTableCounts][0] != null) {
      // new binding root
      if (++bTableCounts == bTable.length) {
        bTable = Arrays.copyOf(bTable, ((bTable.length * 3) / 2 + 1));
      }
      bTable[bTableCounts] = new Binding[3];
      // TODO check if new is null
      // bTable[bTableCounts++][0] = binding;
    }
    bTable[bTableCounts][bLength++] = binding;
    return binding;
  }

  public void unbind() {
    if (log.isTraceEnabled()) {
      log.trace(String.format("Unbinding %s", bTable[bTableCounts][bLength - 1].name));
    }
    bLength--;
    if ((bLength > 0) && (!bTable[bTableCounts][bLength].isReferenced())) {
      if (log.isTraceEnabled()) {
        log.trace(String.format("Unchain unused binding %s", bTable[bTableCounts][bLength].name));
      }
      bTable[bTableCounts][bLength].unchain();
    }
  }

  public void resolvePositions() {
    for (int i = 0; (i <= bTableCounts) && (bTable[i][0] != null); i++) {
      if (log.isTraceEnabled()) {
        log.trace(dumpTable());
      }
      bTable[i][0].resolvePositions(-1);
    }
  }

  public String dumpTable() {
    StringWriter out = new StringWriter();
    PrintWriter printer = new PrintWriter(out);
    for (int i = 0; (i <= bTableCounts) && (bTable[i][0] != null); i++) {
      printer.write('\n');
      printer.write("Root " + i);
      printer.write('\n');
      bTable[i][0].dump(printer);
    }
    return out.toString();
  }

  public Binding[] bound() {
    return Arrays.copyOf(bTable[bTableCounts], bLength);
  }
}