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
package io.brackit.query;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int64;
import io.brackit.query.atomic.Null;
import io.brackit.query.atomic.Str;
import io.brackit.query.compiler.BlockCompileChain;
import io.brackit.query.compiler.CompileChain;
import io.brackit.query.util.Cfg;
import io.brackit.query.function.json.FastJSONParser;
import io.brackit.query.function.json.JSONParser;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.operator.vectorized.ParallelGroupByExec;
import io.brackit.query.operator.vectorized.VectorizedGroupByExec;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jsonitem.array.DArray;
import io.brackit.query.util.serialize.StringSerializer;

/**
 * A jq-like command-line interface for the Brackit JSONiq engine.
 *
 * <p>Usage: bjq [OPTIONS] QUERY [FILE...]</p>
 *
 * <p>Examples:</p>
 * <pre>
 * echo '{"foo": "bar"}' | bjq '$$.foo' # Query JSON from stdin
 * bjq '$$.users[]' data.json # Query JSON file
 * bjq -c '$$' data.json # Compact output
 * bjq -r '$$.name' data.json # Raw string output
 * bjq -n '{"a": 1}' # Generate JSON without input
 * </pre>
 *
 * @author Brackit Project Team
 */
public class BrackitJq {

  private static final String VERSION = "0.6-SNAPSHOT";

  // Exit codes
  private static final int EXIT_SUCCESS = 0;
  private static final int EXIT_ERROR_RESULT = 1;
  private static final int EXIT_USAGE_ERROR = 2;
  private static final int EXIT_JSON_PARSE_ERROR = 3;
  private static final int EXIT_QUERY_ERROR = 4;
  private static final int EXIT_IO_ERROR = 5;

  /**
   * Configuration parsed from command-line arguments.
   */
  private record Config(String query, List<String> inputFiles, boolean compact, boolean rawOutput, boolean nullInput,
                        boolean slurp, boolean exitStatus, boolean help, boolean version) {
  }

  public static void main(String[] args) {
    int exitCode = run(args, System.in, System.out, System.err);
    if (exitCode != EXIT_SUCCESS) {
      System.exit(exitCode);
    }
  }

  /**
   * Run the bjq command with the given arguments and streams.
   * This method is designed to be testable without calling System.exit().
   *
   * @param args command-line arguments
   * @param in   input stream for JSON data
   * @param out  output stream for results
   * @param err  error stream for error messages
   * @return exit code (0 for success)
   */
  public static int run(String[] args, InputStream in, PrintStream out, PrintStream err) {
    try {
      Config config = parseArgs(args);

      if (config.help()) {
        printUsage(out);
        return EXIT_SUCCESS;
      }

      if (config.version()) {
        out.println("bjq (Brackit JSONiq) version " + VERSION);
        return EXIT_SUCCESS;
      }

      if (config.query() == null) {
        err.println("Error: No query provided");
        printUsage(err);
        return EXIT_USAGE_ERROR;
      }

      // Try vectorized execution for streaming queries on large files
      Sequence vectorizedResult = tryVectorizedExecution(config, in);
      if (vectorizedResult != null) {
        boolean hasOutput = serializeOutput(vectorizedResult, config, out);
        if (config.exitStatus() && !hasOutput) {
          return EXIT_ERROR_RESULT;
        }
        return EXIT_SUCCESS;
      }

      // Read JSON input
      Item contextItem = null;
      if (!config.nullInput()) {
        contextItem = readJsonInput(config, in);
      }

      // Execute query
      Sequence result = executeQuery(config.query(), contextItem);

      // Serialize output
      boolean hasOutput = serializeOutput(result, config, out);

      // Exit status handling
      if (config.exitStatus() && !hasOutput) {
        return EXIT_ERROR_RESULT;
      }

      return EXIT_SUCCESS;

    } catch (IllegalArgumentException e) {
      err.println("Error: " + e.getMessage());
      printUsage(err);
      return EXIT_USAGE_ERROR;
    } catch (JsonParseException e) {
      err.println("JSON parse error: " + e.getMessage());
      return EXIT_JSON_PARSE_ERROR;
    } catch (QueryException e) {
      err.println("Query error: " + e.getMessage());
      return EXIT_QUERY_ERROR;
    } catch (IOException e) {
      err.println("I/O error: " + e.getMessage());
      return EXIT_IO_ERROR;
    }
  }

  /**
   * Parse command-line arguments.
   */
  private static Config parseArgs(String[] args) {
    String query = null;
    List<String> inputFiles = new ArrayList<>();
    boolean compact = false;
    boolean rawOutput = false;
    boolean nullInput = false;
    boolean slurp = false;
    boolean exitStatus = false;
    boolean help = false;
    boolean version = false;

    int i = 0;
    while (i < args.length) {
      String arg = args[i];

      if (arg.startsWith("-") && !arg.equals("-")) {
        switch (arg) {
          case "-c", "--compact-output" -> compact = true;
          case "-r", "--raw-output" -> rawOutput = true;
          case "-n", "--null-input" -> nullInput = true;
          case "-s", "--slurp" -> slurp = true;
          case "-e", "--exit-status" -> exitStatus = true;
          case "-h", "--help" -> help = true;
          case "-v", "--version" -> version = true;
          default -> throw new IllegalArgumentException("Unknown option: " + arg);
        }
      } else {
        // First positional argument is the query
        if (query == null) {
          query = arg;
        } else {
          // Subsequent positional arguments are input files
          inputFiles.add(arg);
        }
      }
      i++;
    }

    return new Config(query, inputFiles, compact, rawOutput, nullInput, slurp, exitStatus, help, version);
  }

  /**
   * Read JSON input from files or stdin.
   */
  private static Item readJsonInput(Config config, InputStream in) throws IOException, JsonParseException {
    List<Item> items = new ArrayList<>();

    // Threshold for switching from in-memory FastJSONParser to StreamingJSONParser.
    // Files under this size are read entirely into memory for maximum parse speed.
    // Files above this size (or stdin) use streaming to avoid OOM.
    long streamingThreshold = 1_500_000_000L; // ~1.5 GB (below Java's 2GB array limit)

    if (config.inputFiles().isEmpty()) {
      // Read from stdin — use streaming parser (can't know size ahead of time)
      if (config.slurp()) {
        String json = readString(in);
        items.addAll(parseMultipleJson(json));
      } else {
        return parseStreaming(java.io.BufferedInputStream.class.isInstance(in)
            ? in
            : new java.io.BufferedInputStream(in));
      }
    } else {
      for (String file : config.inputFiles()) {
        if (config.slurp()) {
          String json = readFile(file);
          items.addAll(parseMultipleJson(json));
        } else {
          long fileSize = new java.io.File(file).length();
          if (fileSize > streamingThreshold) {
            // Large file — use streaming parser to avoid OOM
            items.add(parseStreaming(new java.io.BufferedInputStream(new FileInputStream(file), 8 * 1024 * 1024)));
          } else {
            // Small/medium file — use fast in-memory parser
            String json = readFile(file);
            items.add(parseJson(json));
          }
        }
      }
    }

    if (config.slurp()) {
      // Return all items as an array
      return new DArray(items);
    }

    // For multiple files without slurp, return just the first
    return items.isEmpty() ? null : items.getFirst();
  }

  /**
   * Parse JSON from an InputStream using the streaming parser.
   * Returns a StreamingArray for top-level arrays, enabling lazy element-by-element parsing
   * without loading the entire file into memory.
   */
  private static Item parseStreaming(InputStream in) throws JsonParseException {
    try {
      return new StreamingJSONParser(in).parse();
    } catch (QueryException e) {
      throw new JsonParseException(e.getMessage(), e);
    }
  }

  /**
   * Parse a JSON string into a Brackit Item.
   */
  private static Item parseJson(String json) throws JsonParseException {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return new FastJSONParser(json).parse();
    } catch (QueryException e) {
      throw new JsonParseException(e.getMessage(), e);
    }
  }

  /**
   * Parse multiple JSON values from a string (newline-delimited or consecutive).
   */
  private static List<Item> parseMultipleJson(String json) throws JsonParseException {
    List<Item> items = new ArrayList<>();
    if (json == null || json.isBlank()) {
      return items;
    }

    // Split by lines and parse each non-empty line as JSON
    // This handles newline-delimited JSON (NDJSON) format
    String[] lines = json.split("\n");
    StringBuilder buffer = new StringBuilder();
    int braceDepth = 0;
    int bracketDepth = 0;
    boolean inString = false;

    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      // Track nesting to handle multi-line JSON
      for (int i = 0; i < trimmed.length(); i++) {
        char c = trimmed.charAt(i);
        if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
          inString = !inString;
        }
        if (!inString) {
          if (c == '{')
            braceDepth++;
          else if (c == '}')
            braceDepth--;
          else if (c == '[')
            bracketDepth++;
          else if (c == ']')
            bracketDepth--;
        }
      }

      buffer.append(trimmed);

      // If we have balanced braces/brackets, we have a complete JSON value
      if (braceDepth == 0 && bracketDepth == 0 && !buffer.isEmpty()) {
        items.add(parseJson(buffer.toString()));
        buffer.setLength(0);
      }
    }

    // Parse any remaining content
    if (!buffer.isEmpty()) {
      items.add(parseJson(buffer.toString()));
    }

    return items;
  }

  /**
   * Try to execute the query using the vectorized DataChunk path.
   * Returns null if the query pattern isn't supported (falls back to Volcano).
   * <p>
   * Supported patterns:
   * <ul>
   * <li>{@code for $u in $$[] let $c := $u.FIELD group by $c return {"FIELD": $c, "count": count($u)}}</li>
   * <li>{@code count(for $u in $$[] where $u.FIELD > N return ...)}</li>
   * <li>{@code for $u in $$.KEY[] ...} (object-wrapped arrays)</li>
   * </ul>
   */
  private static Sequence tryVectorizedExecution(Config config, InputStream in) {
    if (config.nullInput() || config.slurp()) {
      return null;
    }

    String query = config.query().trim();

    // Only for file inputs large enough to benefit from vectorized execution.
    // Stdin is excluded — we can't determine size ahead of time and small inputs
    // should use the proven Volcano path.
    boolean isLargeFile = false;
    for (String file : config.inputFiles()) {
      if (new java.io.File(file).length() > 100_000_000) { // >100MB
        isLargeFile = true;
        break;
      }
    }
    if (!isLargeFile) {
      return null;
    }

    try {
      // Pattern: for $VAR in $$[] let $GK := $VAR.FIELD group by $GK return {"FIELD": $GK, "count": count($VAR)}
      // Simplified regex matching for the common group-by-count pattern
      var groupByMatch = java.util.regex.Pattern.compile("for\\s+\\$\\w+\\s+in\\s+\\$\\$(?:\\.\\w+)?\\[]\\s+"
          + "let\\s+\\$\\w+\\s*:=\\s*\\$\\w+\\.(\\w+)\\s+" + "group\\s+by\\s+\\$\\w+\\s+"
          + "return\\s+.*count\\(\\$\\w+\\)").matcher(query);

      if (groupByMatch.find()) {
        String groupField = groupByMatch.group(1);

        // Use parallel memory-mapped I/O (1BRC-inspired, N cores)
        if (!config.inputFiles().isEmpty()) {
          java.nio.file.Path path = java.nio.file.Path.of(config.inputFiles().getFirst());
          List<Item> results = ParallelGroupByExec.executeGroupByCount(path, groupField);
          return new DArray(results);
        }

        // Stdin fallback: streaming parser
        StreamingJSONParser parser = new StreamingJSONParser(new java.io.BufferedInputStream(in, 8 * 1024 * 1024));
        Item root = parser.parse();
        if (root instanceof io.brackit.query.jsonitem.array.StreamingArray) {
          List<Item> results = VectorizedGroupByExec.executeGroupByCount(parser, groupField);
          return new DArray(results);
        }
        return null;
      }

      // Pattern: count(for $VAR in $$[] where $VAR.FIELD > N return ...)
      var countFilterMatch = java.util.regex.Pattern.compile(
                                                             "count\\(\\s*for\\s+\\$\\w+\\s+in\\s+\\$\\$(?:\\.\\w+)?\\[]\\s+"
                                                                 + "where\\s+\\$\\w+\\.(\\w+)\\s*(>|<|>=|<=|eq)\\s*(\\d+)\\s+"
                                                                 + "return").matcher(query);

      if (countFilterMatch.find()) {
        String filterField = countFilterMatch.group(1);
        String op = switch (countFilterMatch.group(2)) {
          case ">" -> "gt";
          case "<" -> "lt";
          case ">=" -> "ge";
          case "<=" -> "le";
          case "eq" -> "eq";
          default -> "gt";
        };
        long filterValue = Long.parseLong(countFilterMatch.group(3));

        // Parallel memory-mapped I/O for file inputs
        if (!config.inputFiles().isEmpty()) {
          java.nio.file.Path path = java.nio.file.Path.of(config.inputFiles().getFirst());
          long count = ParallelGroupByExec.executeFilterCount(path, filterField, op, filterValue);
          return new Int64(count);
        }

        // Stdin fallback
        StreamingJSONParser parser = new StreamingJSONParser(new java.io.BufferedInputStream(in, 8 * 1024 * 1024));
        Item root = parser.parse();
        if (root instanceof io.brackit.query.jsonitem.array.StreamingArray) {
          long count = VectorizedGroupByExec.executeFilterCount(parser, filterField, op, filterValue);
          return new Int64(count);
        }
      }

    } catch (Exception e) {
      // Vectorized path failed — fall back to Volcano
    }

    return null;
  }

  private static String extractArraySource(String query) {
    var m = java.util.regex.Pattern.compile("\\$\\$\\.(\\w+)\\[]").matcher(query);
    return m.find() ? m.group(1) : null;
  }

  private static InputStream getInputStream(Config config, InputStream stdin) throws IOException {
    if (config.inputFiles().isEmpty()) {
      return stdin;
    }
    return new FileInputStream(config.inputFiles().getFirst());
  }

  /**
   * Execute a JSONiq query with the given context item.
   */
  private static final boolean PARALLEL = Cfg.asBool("io.brackit.query.parallel", false);

  private static Sequence executeQuery(String queryString, Item contextItem) throws QueryException {
    QueryContext ctx = new BrackitQueryContext();
    CompileChain compileChain = PARALLEL ? new BlockCompileChain() : new CompileChain();

    if (contextItem != null) {
      ctx.setContextItem(contextItem);
    }

    Query query = new Query(compileChain, queryString);
    return query.execute(ctx);
  }

  /**
   * Serialize the query result to the output stream.
   *
   * @return true if any output was produced, false otherwise
   */
  private static boolean serializeOutput(Sequence result, Config config, PrintStream out) throws QueryException {
    if (result == null) {
      return false;
    }

    boolean prettyPrint = !config.compact();

    // If result is a single JSON item (Array or Object), serialize it directly
    // This preserves the array/object structure in the output
    if (result instanceof Array || result instanceof Object) {
      StringSerializer serializer = new StringSerializer(new PrintWriter(out, true));
      serializer.setFormat(prettyPrint);
      serializer.serialize(result);
      out.println();
      return true;
    }

    // For sequences, iterate and output each item
    boolean hasOutput = false;
    try (Iter it = result.iterate()) {
      Item item;
      while ((item = it.next()) != null) {
        hasOutput = true;

        // Check for false/null with exit status
        if (config.exitStatus()) {
          if (item instanceof Null) {
            continue;
          }
          if (item instanceof Bool bool && !bool.booleanValue()) {
            continue;
          }
        }

        if (config.rawOutput() && item instanceof Str str) {
          // Raw output: print strings without quotes
          out.println(str.stringValue());
        } else {
          // Use StringSerializer for proper JSON formatting
          StringSerializer serializer = new StringSerializer(new PrintWriter(out, true));
          serializer.setFormat(prettyPrint);
          serializer.serialize(item);
          out.println();
        }
      }
    }

    return hasOutput;
  }

  /**
   * Read entire contents of a file as a string.
   */
  private static String readFile(String file) throws IOException {
    try (FileInputStream fin = new FileInputStream(file)) {
      return readString(fin);
    }
  }

  /**
   * Read entire contents of an input stream as a string.
   */
  private static String readString(InputStream in) throws IOException {
    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
  }

  /**
   * Print usage information.
   */
  private static void printUsage(PrintStream out) {
    out.println("Usage: bjq [OPTIONS] QUERY [FILE...]");
    out.println();
    out.println("A jq-like command-line JSON processor powered by Brackit JSONiq.");
    out.println("Unlike jq, bjq supports FLWOR expressions, user-defined functions,");
    out.println("joins with automatic optimization, and the full power of JSONiq.");
    out.println();
    out.println("Arguments:");
    out.println("  QUERY          JSONiq query expression (use $$ for input)");
    out.println("  FILE           Input JSON file(s); if omitted, reads from stdin");
    out.println();
    out.println("Options:");
    out.println("  -c, --compact-output   Produce compact output (no pretty-printing)");
    out.println("  -r, --raw-output       Output raw strings without JSON quotes");
    out.println("  -n, --null-input       Don't read any input; query runs without context");
    out.println("  -s, --slurp            Read all inputs into an array");
    out.println("  -e, --exit-status      Exit with 1 if last output is false/null");
    out.println("  -h, --help             Show this help message");
    out.println("  -v, --version          Show version information");
    out.println();
    out.println("Basic Examples:");
    out.println("  bjq '$$.name' data.json                    # Field access");
    out.println("  bjq '$$.users[].email' data.json           # Array iteration");
    out.println("  bjq '$$[0:5]' data.json                    # Array slice (Python-style)");
    out.println("  bjq '$$[-1]' data.json                     # Negative index (last element)");
    out.println("  bjq '$${name,age}' data.json               # Object projection");
    out.println("  bjq '$$[][?$$.active]' data.json           # Predicate filter");
    out.println();
    out.println("FLWOR Expressions (the killer feature!):");
    out.println("  # Iterate, filter, and transform");
    out.println("  bjq 'for $u in $$.users[] where $u.age > 21 return $u.name' data.json");
    out.println();
    out.println("  # Group by with aggregation");
    out.println("  bjq 'for $item in $$[]");
    out.println("       let $cat := $item.category");
    out.println("       group by $cat");
    out.println("       return {$cat: count($item)}' sales.json");
    out.println();
    out.println("  # Order by");
    out.println("  bjq 'for $p in $$.products[] order by $p.price descending return $p' data.json");
    out.println();
    out.println("  # Joins (automatically optimized with hash-joins!)");
    out.println("  bjq 'let $orders := $$.orders[]");
    out.println("       let $customers := $$.customers[]");
    out.println("       for $o in $orders, $c in $customers");
    out.println("       where $o.customer_id eq $c.id");
    out.println("       return {\"order\": $o.id, \"customer\": $c.name}' data.json");
    out.println();
    out.println("User-Defined Functions:");
    out.println("  bjq 'declare function local:double($x) { $x * 2 };");
    out.println("       for $n in $$[] return local:double($n)' numbers.json");
    out.println();
    out.println("  # Recursive functions");
    out.println("  bjq -n 'declare function local:factorial($n) {");
    out.println("            if ($n le 1) then 1 else $n * local:factorial($n - 1)");
    out.println("          };");
    out.println("          local:factorial(10)'");
    out.println();
    out.println("Anonymous Functions & Closures:");
    out.println("  bjq -n 'let $mult := function($x, $y) { $x * $y }");
    out.println("          return $mult(6, 7)'");
    out.println();
    out.println("Built-in Functions:");
    out.println("  bjq 'count($$.items[])'                    # Count items");
    out.println("  bjq 'sum($$.prices[])'                     # Sum values");
    out.println("  bjq 'avg($$.scores[])'                     # Average");
    out.println("  bjq 'string-join($$.tags[], \", \")'        # Join strings");
    out.println("  bjq 'distinct-values($$.categories[])'     # Unique values");
    out.println("  bjq 'keys($$)'                             # Object keys");
    out.println("  bjq 'contains($$.text, \"error\")'          # String search");
    out.println("  bjq 'fn:current-dateTime()'                # Current timestamp");
    out.println();
    out.println("Object Construction:");
    out.println("  # Dynamic field names");
    out.println("  bjq '{| for $k in keys($$) return {upper-case($k): $$.$k} |}' data.json");
    out.println();
    out.println("  # Combine objects (note: duplicate keys cause an error)");
    out.println("  bjq '{| $$.base, $$.extra |}' config.json");
    out.println();
    out.println("Conditional Logic:");
    out.println("  bjq 'for $p in $$[] return");
    out.println("       if ($p.stock > 0) then {\"available\": $p.name}");
    out.println("       else {\"out_of_stock\": $p.name}' products.json");
    out.println();
    out.println("Quantified Expressions:");
    out.println("  bjq 'some $x in $$[] satisfies $x.price > 100'   # Existential");
    out.println("  bjq 'every $x in $$[] satisfies $x.valid'        # Universal");
    out.println();
    out.println("Type Checking:");
    out.println("  bjq '$$[][?$$.value instance of xs:integer]' data.json");
    out.println();
    out.println("JSONiq Syntax Reference:");
    out.println("  $$              Context item (input document)");
    out.println("  $$.field        Object field access (also: $$.\"field name\")");
    out.println("  $$[]            Array unboxing (iterate elements)");
    out.println("  $$[0]           Array index (0-based)");
    out.println("  $$[0:3]         Array slice [start:end]");
    out.println("  $$[::2]         Array slice with step [start:end:step]");
    out.println("  $$[-1]          Negative index (from end)");
    out.println("  $${f1,f2}       Object projection");
    out.println("  [=seq]          Flatten sequence into array");
    out.println();
    out.println("Performance Notes:");
    out.println("  Brackit includes sophisticated AST rewrite rules that automatically");
    out.println("  optimize your queries. Joins are converted to hash-joins, predicates");
    out.println("  are pushed down, and common subexpressions are eliminated.");
  }

  /**
   * Exception for JSON parsing errors.
   */
  private static class JsonParseException extends Exception {
    JsonParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
