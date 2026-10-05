package io.brackit.query.util.serialize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import io.brackit.query.Query;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.QNm;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.d2linked.D2Node;
import io.brackit.query.node.d2linked.D2NodeFactory;
import io.brackit.query.node.parser.DocumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SubtreePrinterTest extends XQueryBaseTest {
  private static Stream<String> values() {
    return Stream.of("plain", "a&b", "a<b", "a>b", "a\"b", "a'b", "&<>\"'&amp;]]>");
  }

  @ParameterizedTest
  @MethodSource("values")
  public void defaultNamespaceRoundTrip(String value) {
    String uri = "https://example.test/" + value;
    Node<?> root = query("<root xmlns=\"" + encoded(uri) + "\"><child/></root>");
    assertEquals(uri, root.getName().getNamespaceURI());
    assertEquals(uri, root.getScope().defaultNS());
    assertRoundTrip(root);
    assertEquals(uri, root.getScope().defaultNS());
  }

  @ParameterizedTest
  @MethodSource("values")
  public void prefixedNamespaceRoundTrip(String value) {
    String uri = "https://example.test/" + value;
    Node<?> root = query("<p:root xmlns:p=\"" + encoded(uri) + "\" p:attr=\"plain\"><p:child/></p:root>");
    assertEquals(uri, root.getName().getNamespaceURI());
    assertEquals(uri, root.getScope().resolvePrefix("p"));
    assertRoundTrip(root);
    assertEquals(uri, root.getScope().resolvePrefix("p"));
  }

  @ParameterizedTest
  @MethodSource("values")
  public void attributeRoundTrip(String value) {
    Node<?> root = query("<root attr=\"" + encoded(value) + "\"/>");
    assertEquals(value, root.getAttribute(new QNm("attr")).getValue().stringValue());
    assertRoundTrip(root);
    assertEquals(value, root.getAttribute(new QNm("attr")).getValue().stringValue());
  }

  @ParameterizedTest
  @MethodSource("values")
  public void textRoundTrip(String value) {
    // Exercise pending text at endElement, pending text flushed by newChild,
    // and text emitted immediately after a child.
    for (String content : new String[] { encoded(value), encoded(value) + "<child/>", "<child/>" + encoded(value) }) {
      Node<?> root = query("<root>" + content + "</root>");
      assertEquals(value, root.getValue().stringValue());
      assertRoundTrip(root);
      assertEquals(value, root.getValue().stringValue());
    }
  }

  @Test
  public void querySerializationRoundTrip() {
    String constructor = "<root xmlns='https://example.test/ns?a=1&amp;b=2' xmlns:p='urn:&lt;&gt;&quot;&apos;'>"
        + "<p:child p:attr='&amp;&lt;&gt;&quot;&apos;'>&amp;&lt;&gt;&quot;&apos;</p:child></root>";
    Node<?> root = query(constructor);
    assertEquals("https://example.test/ns?a=1&b=2", root.getScope().defaultNS());
    assertEquals("urn:<>\"'", root.getScope().resolvePrefix("p"));
    StringWriter output = new StringWriter();
    Query query = new Query(constructor);
    query.serialize(ctx, new PrintWriter(output));
    assertSameTree(root, reparse(output.toString()));
  }

  @Test
  public void preservesCharacterReferencesThroughXmlNormalization() {
    Node<?> root = query("<root attr=\"a&#x9;b&#xA;c&#xD;d\">a&#xD;b</root>");
    assertEquals("a\tb\nc\rd", root.getAttribute(new QNm("attr")).getValue().stringValue());
    assertEquals("a\rb", root.getValue().stringValue());
    assertRoundTrip(root);
  }

  @Test
  public void plainFormattingRemainsUnchanged() {
    StringWriter output = new StringWriter();
    SubtreePrinter printer = new SubtreePrinter(new PrintWriter(output));
    printer.print(query("<root attr='plain'><child>plain</child></root>"));
    printer.flush();
    assertEquals("<root attr=\"plain\">" + System.lineSeparator() + "  <child>plain</child>" + System.lineSeparator()
        + "</root>" + System.lineSeparator(), output.toString());
  }

  private Node<?> query(String query) {
    Node<?> node = (Node<?>) new Query(query).execute(ctx);
    assertTrue(node instanceof D2Node);
    return node;
  }

  private void assertRoundTrip(Node<?> root) {
    StringWriter output = new StringWriter();
    SubtreePrinter printer = new SubtreePrinter(new PrintWriter(output));
    printer.setPrettyPrint(false);
    printer.print(root);
    assertSameTree(root, reparse(output.toString()));

    // The query-facing serializer uses the same printer.
    output = new StringWriter();
    try (StringSerializer serializer = new StringSerializer(new PrintWriter(output))) {
      serializer.serialize(root);
    }
    assertSameTree(root, reparse(output.toString()));
  }

  private Node<?> reparse(String xml) {
    DocumentParser parser = new DocumentParser(xml);
    parser.setRetainWhitespace(true);
    return new D2NodeFactory().build(parser).getFirstChild();
  }

  private void assertSameTree(Node<?> expected, Node<?> actual) {
    assertNotNull(actual);
    assertEquals(expected.getKind(), actual.getKind());
    assertEquals(expected.getName(), actual.getName());
    if (expected.getName() != null) {
      assertEquals(expected.getName().getPrefix(), actual.getName().getPrefix());
      assertEquals(expected.getName().getNamespaceURI(), actual.getName().getNamespaceURI());
    }
    assertEquals(expected.getValue().stringValue(), actual.getValue().stringValue());
    if (expected.getKind() == Kind.ELEMENT) {
      assertEquals(expected.getScope().defaultNS(), actual.getScope().defaultNS());
      try (var prefixes = expected.getScope().localPrefixes()) {
        String prefix;
        while ((prefix = prefixes.next()) != null) {
          assertEquals(expected.getScope().resolvePrefix(prefix), actual.getScope().resolvePrefix(prefix));
        }
      }
      try (var expectedAttributes = expected.getAttributes(); var actualAttributes = actual.getAttributes()) {
        Node<?> attribute;
        int count = 0;
        while ((attribute = expectedAttributes.next()) != null) {
          assertSameTree(attribute, actual.getAttribute(attribute.getName()));
          count++;
        }
        int actualCount = 0;
        while (actualAttributes.next() != null) {
          actualCount++;
        }
        assertEquals(count, actualCount);
      }
    }
    Node<?> expectedChild = expected.getFirstChild();
    Node<?> actualChild = actual.getFirstChild();
    while (expectedChild != null) {
      assertSameTree(expectedChild, actualChild);
      expectedChild = expectedChild.getNextSibling();
      actualChild = actualChild.getNextSibling();
    }
    assertNull(actualChild);
  }

  private static String encoded(String value) {
    return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
  }
}
