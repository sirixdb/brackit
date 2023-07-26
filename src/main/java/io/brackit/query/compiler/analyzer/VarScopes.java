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
package io.brackit.query.compiler.analyzer;

import java.util.HashMap;

import io.brackit.query.atomic.QNm;

/**
 * @author Sebastian Baechle
 */
public class VarScopes {
  private int idSequence;
  private final Scope root = new Scope(null);
  private Scope current = root;
  private Scope resolveIn = root;
  private int level;

  private static class Scope {
    Scope parent;
    HashMap<QNm, Variable> mapping = new HashMap<>();

    Scope(Scope parent) {
      this.parent = parent;
    }

    public String toString() {
      return mapping.toString();
    }
  }

  public static class Variable {
    QNm name;

    public Variable(QNm name) {
      this.name = name;
    }

    public String toString() {
      return name.toString();
    }
  }

  public int scopeCount() {
    return level;
  }

  public void openScope() {
    level++;
    current = new Scope(current);
  }

  public void offerScope() {
    resolveIn = current;
  }

  public void closeScope() {
    if (level == 0) {
      throw new RuntimeException();
    }
    level--;
    current = current.parent;
    resolveIn = current;
  }

  public boolean check(QNm name) {
    return current.mapping.containsKey(name);
  }

  public QNm declare(QNm name) {
    Variable var = (level > 0)
        ? new Variable(new QNm(name.getNamespaceURI(), name.getPrefix(), name.getLocalName() + ";" + idSequence++))
        : new Variable(name);
    current.mapping.put(name, var);
    return var.name;
  }

  public QNm resolve(QNm name) {
    Scope scope = resolveIn;
    Variable var;

    while (((var = scope.mapping.get(name)) == null) && ((scope = scope.parent) != null))
      ;

    if (var == null) {
      return null;
    }

    return var.name;
  }
}