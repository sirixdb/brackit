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
import org.brackit.xquery.node.parser.NavigationalSubtreeParser;
import org.brackit.xquery.node.parser.NodeSubtreeParser;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.node.Node;
import org.brackit.xquery.jdm.node.NodeCollection;
import org.brackit.xquery.jdm.node.NodeFactory;

/**
 * @author Sebastian Baechle
 */
public class D2NodeFactory implements NodeFactory<D2Node> {
  @Override
  public D2Node attribute(QNm name, Atomic value) throws DocumentException {
    return new AttributeD2Node(name, value);
  }

  @Override
  public D2Node comment(Str value) throws DocumentException {
    return new CommentD2Node(value);
  }

  @Override
  public D2Node document(Str name) throws DocumentException {
    String s = name != null ? name.stringValue() : null;
    return new DocumentD2Node(s);
  }

  @Override
  public D2Node element(QNm name) throws DocumentException {
    return new ElementD2Node(name);
  }

  @Override
  public D2Node pi(QNm target, Str value) throws DocumentException {
    return new PID2Node(target, value);
  }

  @Override
  public D2Node text(Atomic value) throws DocumentException {
    return new TextD2Node(value);
  }

  @Override
  public D2Node copy(Node<?> source) throws DocumentException {
    return build(new NavigationalSubtreeParser(source));
  }

  public D2Node build(NodeSubtreeParser parser) throws DocumentException {
    D2NodeBuilder handler = new D2NodeBuilder();
    parser.parse(handler);
    return handler.root();
  }

  @Override
  public NodeCollection<D2Node> collection(String name, NodeSubtreeParser parser) throws DocumentException {
    D2NodeCollection coll = new D2NodeCollection(name);
    D2NodeBuilder builder = new D2NodeBuilder(coll);
    parser.parse(builder);
    return coll;
  }

  @Override
  public NodeCollection<D2Node> collection(String name, Stream<NodeSubtreeParser> parsers) throws DocumentException {
    D2NodeCollection coll = new D2NodeCollection(name);
    D2NodeBuilder builder = new D2NodeBuilder(coll);
    try (parsers) {
      NodeSubtreeParser parser;
      while ((parser = parsers.next()) != null) {
        parser.parse(builder);
      }
    }
    return coll;
  }

  @Override
  public NodeCollection<D2Node> collection(String name) throws DocumentException {
    return new D2NodeCollection(name);
  }
}
