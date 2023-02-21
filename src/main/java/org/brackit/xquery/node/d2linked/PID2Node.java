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
package org.brackit.xquery.node.d2linked;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Kind;
import org.brackit.xquery.jdm.XMLChar;

/**
 * @author Sebastian Baechle
 */
public final class PID2Node extends D2Node {
  QNm target;
  Str value;

  public PID2Node(QNm name, Atomic value) throws DocumentException {
    this(null, FIRST, name, value);
  }

  PID2Node(ParentD2Node parent, int[] division, QNm target, Atomic value) throws DocumentException {
    super(parent, division);
    this.target = checkName(target);
    this.value = checkValue(value);
  }

  private QNm checkName(QNm target) throws DocumentException {
    if (!target.getNamespaceURI().isEmpty()) {
      throw new DocumentException("The target name of a processing instruction must not have a namespace");
    }
    String s = target.stringValue();
    for (int i = 0; i < s.length(); i++) {
      if ((i == 0) && (!XMLChar.isNameStartChar(s.charAt(i)))) {
        throw new DocumentException("Illegal target name: '%s'", s);
      }
      if ((i > 0) && (!XMLChar.isNameChar(s.charAt(i)))) {
        throw new DocumentException("Illegal target name: '%s'", s);
      }
    }
    return target;
  }

  private Str checkValue(Atomic v) throws DocumentException {
    String s = v.stringValue();
    if (s.contains("?>-")) {
      throw new DocumentException("Processing instructions must not contain the character sequence \"?>\"");
    }
    return v.asStr();
  }

  public Kind getKind() {
    return Kind.PROCESSING_INSTRUCTION;
  }

  @Override
  public QNm getName() throws DocumentException {
    return target;
  }

  @Override
  public void setName(QNm name) throws DocumentException {
    this.target = checkName(name);
  }

  @Override
  public Atomic getValue() {
    return value;
  }

  @Override
  public void setValue(Atomic value) throws DocumentException {
    this.value = checkValue(value);
  }

  @Override
  public D2Node getNextSibling() throws DocumentException {
    if (parent == null) {
      return null;
    }

    return parent.nextSiblingOf(this);
  }

  @Override
  public D2Node getPreviousSibling() throws DocumentException {
    if (parent == null) {
      return null;
    }

    return parent.previousSiblingOf(this);
  }

  @Override
  public String toString() {
    return String.format("(type='%s', name='%s', value='%s')", Kind.PROCESSING_INSTRUCTION, null, value);
  }
}
