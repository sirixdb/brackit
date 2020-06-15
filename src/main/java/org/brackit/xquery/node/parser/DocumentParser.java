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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.brackit.xquery.util.Cfg;
import org.brackit.xquery.xdm.DocumentException;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Sebastian Baechle
 */
public class DocumentParser implements SubtreeParser {
  public final static String IGNORE_COMMENTS = "org.brackit.xquery.node.parser.DocumentParser.ignoreComments";

  private final XMLReader xmlReader;

  private final InputSource source;

  private DTDHandler dtdHandler;

  private boolean retainWhitespace;

  private boolean parseAsFragment;

  private String baseDir;

  public DocumentParser(File xmlFile) throws DocumentException, FileNotFoundException {
    this(new InputSource(new BufferedReader(new FileReader(xmlFile))));
    File dir = xmlFile.getParentFile();
    if (dir != null) {
      baseDir = dir.getAbsolutePath();
    }
  }

  public DocumentParser(String xmlFragment) throws DocumentException {
    this(new InputSource(new StringReader(xmlFragment)));
  }

  public DocumentParser(InputStream in) throws DocumentException {
    this(new InputSource(in));
  }

  public DocumentParser(InputSource source) throws DocumentException {
    this.source = source;
    try {
      xmlReader = XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      throw new DocumentException(e, "Error creating document parser.");
    }
  }

  public InputSource getSource() {
    return source;
  }

  public void setParseAsFragment(boolean parseAsFragment) {
    this.parseAsFragment = parseAsFragment;
  }

  public void setRetainWhitespace(boolean retainWhitespace) {
    this.retainWhitespace = retainWhitespace;
  }

  @Override
  public void parse(SubtreeHandler handler) throws DocumentException {
    try {
      SAX2SubtreeHandlerAdapter handlerAdapter = new SAX2SubtreeHandlerAdapter(handler);
      if (retainWhitespace) {
        handlerAdapter.setRetainWhitespace(true);
      }
      if (parseAsFragment) {
        handlerAdapter.setParseAsFragment(true);
      }

      xmlReader.setContentHandler(handlerAdapter);

      if (baseDir != null) {
        xmlReader.setEntityResolver(new RelativeEntityResolver(baseDir));
      }
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);
      //			xmlReader.setFeature(
      //					"http://xml.org/sax/features/namespace-prefixes", true);
      if (!Cfg.asBool(IGNORE_COMMENTS, false))
        xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handlerAdapter);
      xmlReader.parse(source);
    } catch (SAXException e) {
      throw new DocumentException(e, "Error parsing document.");
    } catch (IOException e) {
      throw new DocumentException(e, "Error parsing document.");
    }
  }
}