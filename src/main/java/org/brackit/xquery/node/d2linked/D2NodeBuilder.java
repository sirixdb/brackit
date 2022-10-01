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

import java.util.Map;

import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.AbstractBuilder;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;

/**
 * @author Sebastian Baechle
 */
public class D2NodeBuilder extends AbstractBuilder<D2Node> {

  private final D2Node sibling;
  private final boolean right;
  private final D2NodeCollection coll;

  public D2NodeBuilder(String name) throws DocumentException {
    this(new D2NodeCollection(name));
  }

  public D2NodeBuilder(D2NodeCollection coll) throws DocumentException {
    this.coll = coll;
    sibling = null;
    right = true;
  }

  public D2NodeBuilder() throws DocumentException {
    coll = null;
    sibling = null;
    right = true;
  }

  public D2NodeBuilder(D2Node parent, D2Node sibling, boolean right) throws DocumentException {
    super(parent);
    this.coll = parent != null ? parent.getCollection() : null;
    this.sibling = sibling;
    this.right = right;
  }

  @Override
  protected D2Node buildDocument() throws DocumentException {
    return coll == null ? new DocumentD2Node() : new DocumentD2Node(coll);
  }

  @Override
  protected D2Node buildAttribute(D2Node parent, QNm name, Atomic value) throws DocumentException {
    return parent != null ? parent.setAttribute(name, value) : first(Kind.ATTRIBUTE, name, value);
  }

  D2Node first(Kind kind, QNm name, Atomic value) throws DocumentException {
    D2Node child;
    if (kind == Kind.ELEMENT) {
      child = new ElementD2Node(name);
    } else if (kind == Kind.TEXT) {
      child = new TextD2Node(value);
    } else if (kind == Kind.COMMENT) {
      child = new CommentD2Node(value);
    } else if (kind == Kind.PROCESSING_INSTRUCTION) {
      child = new PID2Node(name, value);
    } else if (kind == Kind.ATTRIBUTE) {
      child = new AttributeD2Node(name, value);
    } else {
      throw new DocumentException("Illegal node kind: %s", kind);
    }
    return child;
  }

  @Override
  protected D2Node buildElement(D2Node parent, QNm name, Map<String, String> nsMappings) throws DocumentException {
    ElementD2Node e;
    if (parent != null) {
      e = (ElementD2Node) ((ParentD2Node) parent).insertChild(sibling, Kind.ELEMENT, name, null, right);
    } else {
      e = (ElementD2Node) first(Kind.ELEMENT, name, null);
    }
    e.nsMappings = nsMappings;
    return e;
  }

  @Override
  protected D2Node buildText(D2Node parent, Atomic text) throws DocumentException {
    if (parent != null) {
      return ((ParentD2Node) parent).insertChild(sibling, Kind.TEXT, null, text.asUna(), right);
    } else {
      return first(Kind.TEXT, null, text);
    }
  }

  @Override
  protected D2Node buildComment(D2Node parent, Atomic text) throws DocumentException {
    if (parent != null) {
      return ((ParentD2Node) parent).insertChild(sibling, Kind.COMMENT, null, text, right);
    } else {
      return first(Kind.COMMENT, null, text);
    }
  }

  @Override
  protected D2Node buildProcessingInstruction(D2Node parent, QNm target, Atomic text) throws DocumentException {
    if (parent != null) {
      return ((ParentD2Node) parent).insertChild(sibling, Kind.PROCESSING_INSTRUCTION, target, text, right);
    } else {
      return first(Kind.PROCESSING_INSTRUCTION, target, text);
    }
  }
}
