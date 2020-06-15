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
package org.brackit.xquery.node.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Sebastian Baechle
 */
public class CharacterDataImpl extends NodeImpl implements org.w3c.dom.CharacterData {
  public CharacterDataImpl(Document document, Node parent, short type, String name, String value) {
    super(document, parent, type, name, value);
  }

  @Override
  public void appendData(String arg) throws DOMException {
    if (value == null)
      value = arg;
    else if (arg != null)
      value += arg;
  }

  @Override
  public void deleteData(int offset, int count) throws DOMException {
    if ((value != null) && (count < value.length()))
      value = value.substring(offset, offset + count);
    else
      throw new DOMException(DOMException.INDEX_SIZE_ERR, null);
  }

  @Override
  public String getData() throws DOMException {
    return value;
  }

  @Override
  public int getLength() {
    return (value != null) ? value.length() : 0;
  }

  @Override
  public void insertData(int offset, String arg) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public void replaceData(int offset, int count, String arg) throws DOMException {
    throw new RuntimeException();
  }

  @Override
  public void setData(String data) throws DOMException {
    this.value = data;
  }

  @Override
  public String substringData(int offset, int count) throws DOMException {
    throw new RuntimeException();
  }
}