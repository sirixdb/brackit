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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.atomic.Una;
import org.brackit.xquery.xdm.DocumentException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * @author Sebastian Baechle
 */
public class SAX2SubtreeHandlerAdapter extends DefaultHandler2 {
  private final SubtreeHandler handler;

  private boolean inComment;

  private final TrimmingStringBuilder content;

  private boolean retainWhitespace;

  private boolean parseAsFragment;

  private static class TrimmingStringBuilder {
    private char[] val;

    private int start;

    private int end;

    private TrimmingStringBuilder() {
      val = new char[20];
    }

    public int length() {
      return end - start;
    }

    public TrimmingStringBuilder trim() {
      while ((start < end) && (val[start] <= ' ')) {
        start++;
      }
      while ((start < end) && (val[end - 1] <= ' ')) {
        end--;
      }
      return this;
    }

    public void clear() {
      start = 0;
      end = 0;
    }

    public void append(char[] ch, int offset, int length) {
      int newEnd = end + length;

      // if (end == start) // empty buffer -> trim begin
      // {
      // int lastOffset = offset + length;
      // while ((offset < lastOffset) && (ch[offset] <= ' ')) {
      // offset++;
      // length--;
      // }
      //
      // if (offset == lastOffset)
      // {
      // return;
      // }
      // }

      if (newEnd >= val.length) {
        int newLength = (val.length * 3) / 2 + 1;
        if (newEnd > newLength) {
          newLength = newEnd;
        }
        char[] newVal = new char[newLength];
        System.arraycopy(val, 0, newVal, 0, end);
        val = newVal;
      }
      System.arraycopy(ch, offset, val, end, length);
      end += length;
    }

    public String toString() {
      return new String(val, start, end - start);
    }
  }

  public SAX2SubtreeHandlerAdapter(SubtreeHandler handler) {
    super();
    this.handler = handler;
    content = new TrimmingStringBuilder();
  }

  public boolean isRetainWhitespace() {
    return retainWhitespace;
  }

  public void setRetainWhitespace(boolean retainWhitespace) {
    this.retainWhitespace = retainWhitespace;
  }

  public boolean isParseAsFragment() {
    return parseAsFragment;
  }

  public void setParseAsFragment(boolean parseAsFragment) {
    this.parseAsFragment = parseAsFragment;
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
    // If file exists locally, return it; else try to get it from client
    File temp = new File(systemId);
    if (temp.exists()) {
      return new InputSource(new BufferedReader(new FileReader(systemId)));
    }
    return null;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (inComment) {
      try {
        handleText();
      } catch (DocumentException e) {
        throw new SAXException(e);
      }
      inComment = false;
    }
    content.append(ch, start, length);
  }

  private void handleText() throws DocumentException {
    if (content.length() > 0) {
      if (retainWhitespace || content.trim().length() > 0) {
        String text = content.toString();

        if (!inComment) {
          handler.text(new Una(text));
        } else {
          handler.comment(new Str(text));
        }
      }

      content.clear();
    }
  }

  @Override
  public void endDocument() throws SAXException {
    try {
      handleText();
      if (!parseAsFragment) {
        handler.endDocument();
      }
      handler.endFragment();
      handler.end();
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    try {
      if (content.length() > 0)
        handleText();
      handler.endElement(new QNm(uri, localName, name));
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    try {
      handler.fail();
    } catch (DocumentException e1) {
      throw new SAXException(e);
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    try {
      handler.fail();
    } catch (DocumentException e1) {
      throw new SAXException(e);
    }
  }

  @Override
  public void startDocument() throws SAXException {
    try {
      handler.begin();
      handler.beginFragment();
      if (!parseAsFragment) {
        handler.startDocument();
      }
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    try {
      if (content.length() > 0)
        handleText();
      int pos = name.indexOf(":");
      String prefix = (pos == -1) ? null : name.substring(0, pos);
      handler.startElement(new QNm(uri, prefix, localName));

      for (int i = 0; i < attributes.getLength(); i++) {
        name = attributes.getQName(i);
        pos = name.indexOf(":");
        prefix = (pos == -1) ? null : name.substring(0, pos);
        handler.attribute(new QNm(attributes.getURI(i), prefix, attributes.getLocalName(i)),
                          new Una(attributes.getValue(i)));
      }
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void comment(char[] ch, int start, int length) throws SAXException {
    if (!inComment) {
      try {
        handleText();
      } catch (DocumentException e) {
        throw new SAXException(e);
      }
      inComment = true;
    }
    content.append(ch, start, length);
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException {
    try {
      handler.endMapping(prefix);
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    try {
      handler.startMapping(prefix, uri);
    } catch (QueryException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException {
    try {
      if (content.length() > 0)
        handleText();
      handler.processingInstruction(new QNm(target), new Una(data));
    } catch (DocumentException e) {
      throw new SAXException(e);
    }
  }
}
