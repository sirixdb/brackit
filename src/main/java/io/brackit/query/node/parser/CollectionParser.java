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
package io.brackit.query.node.parser;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.QNm;
import io.brackit.query.node.stream.ArrayStream;
import io.brackit.query.node.stream.AtomStream;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Stream;

/**
 * A parser that announces each top level element from the input parsers as a
 * new document.
 *
 * @author Martin Hiller
 */
public class CollectionParser implements NodeSubtreeParser {

  private final Stream<NodeSubtreeParser> parsers;

  public CollectionParser(Stream<NodeSubtreeParser> parsers) {
    this.parsers = parsers;
  }

  public CollectionParser(NodeSubtreeParser parser) {
    this.parsers = new AtomStream<>(parser);
  }

  public CollectionParser(NodeSubtreeParser[] parsers) {
    this.parsers = new ArrayStream<>(parsers);
  }

  @Override
  public void parse(NodeSubtreeHandler handler) throws DocumentException {

    CollectionHandler collHandler = new CollectionHandler(handler);

    // announce begin / begin fragment
    handler.begin();
    NodeSubtreeParser current = null;
    while ((current = parsers.next()) != null) {
      current.parse(collHandler);
    }
    parsers.close();
    handler.end();
  }

  /**
   * Handler class used by the CollectionParser.
   */
  private class CollectionHandler implements NodeSubtreeHandler {

    private final NodeSubtreeHandler handler;

    private int level;

    public CollectionHandler(NodeSubtreeHandler handler) {
      this.handler = handler;
      this.level = 0;
    }

    @Override
    public void attribute(QNm name, Atomic value) throws DocumentException {

      if (level == 0) {
        // attribute on top level => invalid state
        throw new DocumentException("Attribute on top level!");
      }

      handler.attribute(name, value);
    }

    @Override
    public void begin() throws DocumentException {
      // do not propagate local begins
    }

    @Override
    public void beginFragment() throws DocumentException {
      handler.beginFragment();
    }

    @Override
    public void comment(Atomic content) throws DocumentException {

      if (level == 0) {
        // comment on top level => invalid state
        throw new DocumentException("Comment on top level!");
      }

      handler.comment(content);
    }

    @Override
    public void end() throws DocumentException {
      // do not propagate local ends
    }

    @Override
    public void endDocument() throws DocumentException {
      handler.endDocument();
    }

    @Override
    public void endElement(QNm name) throws DocumentException {

      handler.endElement(name);
      level--;

      if (level == 0) {
        // top level element => document ends
        handler.endDocument();
      }
    }

    @Override
    public void endFragment() throws DocumentException {
      handler.endFragment();
    }

    @Override
    public void endMapping(String prefix) throws DocumentException {
      handler.endMapping(prefix);
    }

    @Override
    public void fail() throws DocumentException {
      handler.fail();
    }

    @Override
    public void processingInstruction(QNm target, Atomic content) throws DocumentException {

      if (level == 0) {
        // processing instruction on top level => invalid state
        throw new DocumentException("Processing instruction on top level!");
      }

      handler.processingInstruction(target, content);
    }

    @Override
    public void startDocument() throws DocumentException {
      // do not propagate local document boundaries
      handler.startDocument();
    }

    @Override
    public void startElement(QNm name) throws DocumentException {

      if (level == 0) {
        // top level element => announce as new document
        handler.startDocument();
      }

      level++;
      handler.startElement(name);
    }

    @Override
    public void startMapping(String prefix, String uri) throws DocumentException {
      handler.startMapping(prefix, uri);
    }

    @Override
    public void text(Atomic content) throws DocumentException {

      if (level == 0) {
        // text on top level => invalid state
        throw new DocumentException("Text on top level!");
      }

      handler.text(content);
    }
  }
}
