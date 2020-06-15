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
package org.brackit.xquery.node.parser;

import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.node.Node;

/**
 * @author Sebastian Baechle
 */
public final class SubtreeListener2HandlerAdapter implements SubtreeListener<Node<?>> {
  private final SubtreeHandler handler;

  public SubtreeListener2HandlerAdapter(SubtreeHandler handler) {
    this.handler = handler;
  }

  @Override
  public void startDocument() throws DocumentException {
    handler.startDocument();
  }

  @Override
  public void endDocument() throws DocumentException {
    handler.endDocument();
  }

  @Override
  public void beginFragment() throws DocumentException {
    handler.beginFragment();
  }

  @Override
  public void endFragment() throws DocumentException {
    handler.endFragment();
  }

  @Override
  public <T extends Node<?>> void attribute(T node) throws DocumentException {
    handler.attribute(node.getName(), node.getValue());
  }

  @Override
  public void begin() throws DocumentException {
    handler.begin();
  }

  @Override
  public void end() throws DocumentException {
    handler.end();
  }

  @Override
  public <T extends Node<?>> void endElement(T node) throws DocumentException {
    handler.endElement(node.getName());
  }

  @Override
  public void fail() throws DocumentException {
    handler.fail();
  }

  @Override
  public <T extends Node<?>> void startElement(T node) throws DocumentException {
    handler.startElement(node.getName());
  }

  @Override
  public <T extends Node<?>> void text(T node) throws DocumentException {
    handler.text(node.getValue());
  }

  @Override
  public <T extends Node<?>> void comment(T node) throws DocumentException {
    handler.comment(node.getValue().asStr());
  }

  @Override
  public <T extends Node<?>> void processingInstruction(T node) throws DocumentException {
    handler.processingInstruction(node.getName(), node.getValue().asStr());
  }
}
