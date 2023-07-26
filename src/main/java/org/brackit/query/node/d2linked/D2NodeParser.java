/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
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
package org.brackit.query.node.d2linked;

import java.util.Map.Entry;

import org.brackit.query.node.parser.NodeSubtreeHandler;
import org.brackit.query.node.parser.NodeSubtreeParser;
import org.brackit.query.jdm.DocumentException;
import org.brackit.query.jdm.Kind;

/**
 * @author Sebastian Baechle
 */
public class D2NodeParser implements NodeSubtreeParser {

  private final D2Node root;

  public D2NodeParser(D2Node root) {
    this.root = root;
  }

  @Override
  public void parse(NodeSubtreeHandler handler) throws DocumentException {
    try {
      handler.begin();
      handler.beginFragment();
      traverse(handler, root);
      handler.endFragment();
      handler.end();
    } catch (DocumentException e) {
      handler.fail();
      throw e;
    }
  }

  private void traverse(NodeSubtreeHandler handler, D2Node node) throws DocumentException {
    Kind kind = node.getKind();
    if (kind == Kind.ELEMENT) {
      ElementD2Node elem = (ElementD2Node) node;
      if (elem.nsMappings != null) {
        for (Entry<String, String> ns : elem.nsMappings.entrySet()) {
          handler.startMapping(ns.getKey(), ns.getValue());
        }
      }
      handler.startElement(elem.name);
      for (D2Node n = elem.firstAttribute; n != null; n = n.sibling) {
        AttributeD2Node att = (AttributeD2Node) n;
        handler.attribute(att.name, att.value);
      }
      for (D2Node n = elem.firstChild; n != null; n = n.sibling) {
        traverse(handler, n);
      }
      handler.endElement(elem.name);
      if (elem.nsMappings != null) {
        for (Entry<String, String> ns : elem.nsMappings.entrySet()) {
          handler.endMapping(ns.getKey());
        }
      }
    } else if (kind == Kind.TEXT) {
      handler.text(node.getValue());
    } else if (kind == Kind.COMMENT) {
      handler.comment(node.getStrValue());
    } else if (kind == Kind.PROCESSING_INSTRUCTION) {
      handler.processingInstruction(node.getName(), node.getStrValue());
    } else if (kind == Kind.ATTRIBUTE) {
      handler.attribute(node.getName(), node.getValue());
    } else if (kind == Kind.DOCUMENT) {
      handler.startDocument();
      DocumentD2Node doc = (DocumentD2Node) node;
      for (D2Node n = doc.firstChild; n != null; n = n.sibling) {
        traverse(handler, n);
      }
      handler.endDocument();
    } else {
      throw new DocumentException("Illegal node type: %s", kind);
    }
  }

}
