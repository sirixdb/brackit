package io.brackit.query.util.serialize;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.brackit.query.util.io.IOUtils;
import io.brackit.query.BrackitQueryContext;
import io.brackit.query.Query;
import io.brackit.query.function.json.JSONParser;
import org.junit.Test;

/**
 * Tests of {@code org.brackit.xquery.util.StringSerializer}.
 */
public class StringSerializerTest {

  /**
   * Tests the {@code org.brackit.xquery.util.StringSerializer} without formatting.
   *
   * @throws IOException If there is trouble reading the test resource file
   */
  @Test
  public final void testStringSerializer() throws IOException {
    final PrintStream output = IOUtils.createBuffer();
    final String expected = getExpected("obj");

    try (StringSerializer serializer = new StringSerializer(output)) {
      serializer.setFormat(false);
      serializer.serialize(new JSONParser(expected).parse());
    }

    // Expected has formatting, strip that to check setFormat(false)
    assertEquals(expected.replaceAll("[\\s\\r\\n]", ""), output.toString());
  }

  /**
   * Tests the {@code org.brackit.xquery.util.StringSerializer} serializing a JSON object with double space
   * formatting turned on.
   *
   * @throws IOException If there is trouble reading from the test resource file
   */
  @Test
  public final void testSetFormatDoubleIndent() throws IOException {
    final PrintStream output = IOUtils.createBuffer();
    final String expected = getExpected("obj");

    try (StringSerializer serializer = new StringSerializer(output)) {
      serializer.setFormat(true);
      serializer.setIndent("  ");
      serializer.serialize(new JSONParser(expected).parse());
    }

    assertEquals(expected, output.toString());
  }

  /**
   * Tests the {@code org.brackit.xquery.util.StringSerializer} serializing a JSON array with single space
   * indentation formatting turned on.
   *
   * @throws IOException If there is trouble reading from the test resource file
   */
  @Test
  public final void testSetFormatSingleIndent() throws IOException {
    final PrintStream output = IOUtils.createBuffer();
    final String expected = getExpected("array");

    try (StringSerializer serializer = new StringSerializer(output)) {
      serializer.setFormat(true);
      serializer.setIndent(" ");
      serializer.serialize(new JSONParser(expected).parse());
    }

    assertEquals(expected, output.toString());
  }

  /**
   * Tests the {@code org.brackit.xquery.util.StringSerializer} serializing a typed sequence with double space
   * indentation formatting turned on. Sequences that don't implement {@code org.brackit.xquery.xdm.Item} are
   * handled slightly differently in the serializer.
   *
   * @throws IOException If there is trouble reading from the test resource file
   */
  @Test
  public final void testNonItemSeqSerialization() throws IOException {
    final PrintStream output = IOUtils.createBuffer();
    final String expected = getExpected("typed-seq");
    final String query = """
                   let $people :=
                       [
                           { "first_name" : "Caitlin", "last_name" : "Harris" },
                           { "first_name" : "Kylie", "last_name" : "Smith" }
                       ]
                   let $result :=
                       for $names in $people
                       return {
                           "first" : $names.first_name,
                           "last" : $names.last_name
                       }
                   return [$result]
        """;

    try (StringSerializer serializer = new StringSerializer(output)) {
      serializer.setFormat(true);
      serializer.setIndent("  ");
      serializer.serialize(new Query(query).execute(new BrackitQueryContext()));
    }

    assertEquals(expected, output.toString());
  }

  /**
   * Gets the expected JSON output from test resource file.
   *
   * @param format A test file's format
   * @return The expected JSON value
   * @throws IOException If there is trouble reading the JSON test resource
   */
  private final String getExpected(final String format) throws IOException {
    return Files.readString(Path.of("src/test/resources/json/formatted-" + format + ".json"), UTF_8);
  }

}
