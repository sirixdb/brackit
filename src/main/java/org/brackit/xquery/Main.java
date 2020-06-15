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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.SubtreeParser;
import org.brackit.xquery.util.io.URIHandler;
import org.brackit.xquery.xdm.node.Node;
import org.brackit.xquery.xdm.node.NodeCollection;
import org.brackit.xquery.xdm.node.NodeFactory;

/**
 * @author Sebastian Baechle
 */
public class Main {

  private static class Config {
    Map<String, String> options = new HashMap<>();

    boolean isSet(String option) {
      return options.containsKey(option);
    }

    String getValue(String option) {
      return options.get(option);
    }

    void setOption(String option, String value) {
      options.put(option, value);
    }
  }

  private static class Option {
    final String key;
    final String desc;
    final boolean hasValue;

    Option(String key, String desc, boolean hasValue) {
      this.key = key;
      this.desc = desc;
      this.hasValue = hasValue;
    }
  }

  private static final List<Option> options = new ArrayList<>();

  static {
    options.add(new Option("-q", "query file [use '-' for stdin (default)]", true));
    options.add(new Option("-f", "default document", true));
    options.add(new Option("-p", "pretty print", false));
  }

  public static void main(String[] args) {
    try {
      Config config = parseParams(args);
      QueryContext ctx = new BrackitQueryContext();

      String file = config.getValue("-f");
      if (file != null) {
        URI uri = new URI(file);
        try (InputStream in = URIHandler.getInputStream(uri)) {
          SubtreeParser parser = new DocumentParser(in);
          String name = uri.toURL().getFile();
          NodeFactory<?> factory = ctx.getNodeFactory();
          NodeCollection<?> coll = factory.collection(name, parser);
          Node<?> doc = coll.getDocument();
          ctx.setContextItem(doc);
        }
      }

      String query;
      if (((config.isSet("-q")) && (!"-".equals(config.getValue("-q"))))) {
        query = readFile(config.getValue("-q"));
      } else {
        query = readString(System.in);
      }

      XQuery xq = new XQuery(query);
      if (config.isSet("-p")) {
        xq.prettyPrint();
      }
      xq.serialize(ctx, System.out);
    } catch (QueryException e) {
      System.out.println("Error: " + e.getMessage());
      System.exit(-2);
    } catch (IOException e) {
      System.out.println("I/O Error: " + e.getMessage());
      System.exit(-3);
    } catch (Throwable e) {
      System.out.println("Error: " + e.getMessage());
      System.exit(-4);
    }
  }

  private static Config parseParams(String[] args) throws Exception {
    Config config = new Config();
    for (int i = 0; i < args.length; i++) {
      boolean valid = false;
      String s = args[i];
      for (Option o : options) {
        if (o.key.equals(s)) {
          String val = (o.hasValue) ? args[++i] : null;
          config.setOption(o.key, val);
          valid = true;
          break;
        }
      }
      if (!valid) {
        printUsage();
        throw new Exception("Invalid parameter: " + s);
      }
    }
    return config;
  }

  private static String readFile(String file) throws IOException {
    try (FileInputStream fin = new FileInputStream(file)) {
      return readString(fin);
    }
  }

  private static String readString(InputStream in) throws IOException {
    int r;
    ByteArrayOutputStream payload = new ByteArrayOutputStream();
    while ((r = in.read()) != -1) {
      payload.write(r);
    }
    return payload.toString(StandardCharsets.UTF_8);
  }

  private static void printUsage() {
    System.out.println("No query provided");
    System.out.println(String.format("Usage: java %s [options]", Main.class.getName()));
    System.out.println("Options:");
    for (Option o : options) {
      System.out.print(" ");
      System.out.print(o.key);
      if (o.hasValue) {
        System.out.print(" <param>\t");
      } else {
        System.out.print("\t\t");
      }
      System.out.print("- ");
      System.out.println(o.desc);
    }
    System.exit(-1);
  }
}