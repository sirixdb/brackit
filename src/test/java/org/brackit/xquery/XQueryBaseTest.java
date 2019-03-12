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
package org.brackit.xquery;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import org.brackit.xquery.atomic.DTD;
import org.brackit.xquery.node.SimpleStore;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.util.serialize.SubtreePrinter;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.node.NodeCollection;
import org.brackit.xquery.xdm.node.NodeStore;
import org.junit.Before;
import org.junit.Ignore;

/**
 *
 * @author Sebastian Baechle
 *
 */
@Ignore
public class XQueryBaseTest {

  /** Path to resources folder. */
  public static final String RESOURCES = new StringBuilder("src").append(File.separator)
                                                                 .append("test")
                                                                 .append(File.separator)
                                                                 .append("resources")
                                                                 .toString();

  protected QueryContext ctx;

  protected Random rand;

  protected NodeStore store;

  protected void print(Sequence s) throws QueryException {
    if (s == null) {
      return;
    }
    Iter it = s.iterate();
    Item item;
    try {
      while ((item = it.next()) != null) {
        System.out.print(item);
        System.out.print(" ");
        if ((item instanceof Node<?>) && (((Node<?>) item).getKind() != Kind.ATTRIBUTE)) {
          new SubtreePrinter(System.out, false, false).print((Node<?>) item);
        }
      }
    } finally {
      it.close();
    }
    System.out.println();
  }

  protected XQuery xquery(String query) throws QueryException {
    return new XQuery(query);
  }

  protected PrintStream createBuffer() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    return new PrintStream(out) {
      final OutputStream baos = out;

      @Override
      public String toString() {
        return baos.toString();
      }
    };
  }

  protected String readQuery(String dirname, String filename) throws IOException {
    StringBuilder query = new StringBuilder();
    // URL url = getClass().getResource(dirname + filename);
    // if (url == null) {
    // throw new RuntimeException("Resource not found: " + dirname + filename);
    // }
    BufferedReader file = new BufferedReader(new FileReader(new File(dirname + filename)));
    boolean first = true;

    String line;
    while ((line = file.readLine()) != null) {
      if (!first)
        query.append(' ');
      query.append(line);
      first = false;
    }
    file.close();
    return query.toString();
  }

  protected String readFile(String dirname, String filename) throws IOException {
    StringBuilder read = new StringBuilder();
    // URL url = getClass().getResource(dirname + filename);
    BufferedReader file = new BufferedReader(new FileReader(new File(dirname + filename)));
    boolean first = true;

    String line;
    while ((line = file.readLine()) != null) {
      if (!first)
        read.append('\n');
      read.append(line);
      first = false;
    }
    file.close();
    return read.toString();
  }

  protected NodeCollection<?> storeFile(String name, String document)
      throws Exception, FileNotFoundException {
    // URL url = getClass().getResource(document);
    DocumentParser parser = new DocumentParser(new File(document));// new
                                                                   // File(url.getFile()));
    parser.setRetainWhitespace(true);
    return storeDocument(name, parser);
  }

  protected NodeCollection<?> storeDocument(String name, String document) throws Exception {
    return storeDocument(name, new DocumentParser(document));
  }

  protected NodeCollection<?> storeDocument(String name, SubtreeParser parser) throws Exception {
    NodeCollection<?> collection = store.create(name, parser);
    return collection;
  }

  protected NodeStore createStore() throws Exception {
    return new SimpleStore();
  }

  protected QueryContext createContext() throws Exception {
    return new QueryContext(store) {
      @Override
      public DTD getImplicitTimezone() {
        return new DTD(false, (short) 0, (byte) 2, (byte) 0, 0);
      }
    };
  }

  @Before
  public void setUp() throws Exception, FileNotFoundException {
    store = createStore();
    ctx = createContext();

    // use same random source to get reproducible results in case of an
    // error
    rand = new Random(12345678);
  }
}