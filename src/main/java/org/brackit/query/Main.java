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
package org.brackit.query;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.brackit.query.compiler.CompileChain;
import org.brackit.query.node.parser.DocumentParser;
import org.brackit.query.node.parser.NodeSubtreeParser;
import org.brackit.query.util.io.URIHandler;
import org.brackit.query.jdm.node.Node;
import org.brackit.query.jdm.node.NodeCollection;
import org.brackit.query.jdm.node.NodeFactory;

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
    options.add(new Option("-qf", "query file [use '-' for stdin (default)]", true));
    options.add(new Option("-q", "query string", true));
    options.add(new Option("-iqf", "query files [use '-' for stdin (default)]", false));
    options.add(new Option("-iq", "query strings", false));
    options.add(new Option("-f", "default document", true));
    options.add(new Option("-p", "pretty print", false));
  }

  public static void main(String[] args) {
    try {
      Config config = parseParams(args);
      QueryContext ctx = new BrackitQueryContext();
      CompileChain compileChain = new CompileChain();

      String file = config.getValue("-f");
      if (file != null) {
        URI uri = new URI(file);
        try (InputStream in = URIHandler.getInputStream(uri)) {
          NodeSubtreeParser parser = new DocumentParser(in);
          String name = uri.toURL().getFile();
          NodeFactory<?> factory = ctx.getNodeFactory();
          NodeCollection<?> coll = factory.collection(name, parser);
          Node<?> doc = coll.getDocument();
          ctx.setContextItem(doc);
        }
      }

      String query;
      if (((config.isSet("-qf")) && (!"-".equals(config.getValue("-qf"))))) {
        query = readFile(config.getValue("-qf"));
      } else if (config.isSet("-q")) {
        query = config.getValue("-q");
      } else if (config.isSet("-iq")) {
        while (true) {
          System.out.println("Enter query string (terminate with END on the last line):");
          query = readStringFromScannerWithEndMark();
          executeQuery(config, compileChain, ctx, query);
        }
      } else if (config.isSet("-iqf")) {
        while (true) {
          System.out.println("Enter query string (terminate with END on the last line):");
          query = readFile(config.getValue("-iqf"));
          executeQuery(config, compileChain, ctx, query);
        }
      } else {
        query = readString(System.in);
      }

      executeQuery(config, compileChain, ctx, query);
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

  private static void executeQuery(Config config, CompileChain compileChain, QueryContext ctx, String query) {
    Query xq = new Query(compileChain, query);
    if (config.isSet("-p")) {
      xq.prettyPrint();
    }
    System.out.println();
    System.out.println("Query result:");
    xq.serialize(ctx, System.out);
    System.out.println();
    System.out.println();
  }

  private static String readStringFromScannerWithEndMark() {
    final Scanner scanner = new Scanner(System.in);
    final StringBuilder strbuf = new StringBuilder();

    while (scanner.hasNextLine()) {
      final String line = scanner.nextLine();

      if (line.trim().equals("END"))
        break;

      strbuf.append(line);
    }

    return strbuf.toString();
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
    System.out.printf("Usage: java %s [options]%n", Main.class.getName());
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