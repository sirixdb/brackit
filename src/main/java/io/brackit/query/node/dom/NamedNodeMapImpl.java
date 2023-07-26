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
package io.brackit.query.node.dom;

import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * @author Sebastian Baechle
 */
public class NamedNodeMapImpl implements org.w3c.dom.NamedNodeMap {
  private final Map<String, ? extends org.w3c.dom.Node> map;

  public NamedNodeMapImpl(Map<String, ? extends org.w3c.dom.Node> map) {
    this.map = map;
  }

  @Override
  public int getLength() {
    return (map != null) ? map.size() : 0;
  }

  @Override
  public Node getNamedItem(String name) {
    return (map != null) ? map.get(name) : null;
  }

  @Override
  public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public Node item(int index) {
    if ((index >= 0) && (index < map.size())) {
      String key = (String) (map.keySet().toArray())[index];
      return map.get(key);
    }

    return null;
  }

  @Override
  public Node removeNamedItem(String name) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public Node setNamedItem(Node arg) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public Node setNamedItemNS(Node arg) throws DOMException {
    throw new RuntimeException();
  }
}