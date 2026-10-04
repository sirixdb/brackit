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
package io.brackit.query.node.d2linked;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Kind;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeReplacementRegressionTest extends XQueryBaseTest {
  private record Original(ElementD2Node root, List<D2Node> attributes, List<QNm> names, List<String> values) {
  }

  private static Stream<Arguments> singleReplacements() {
    return Stream.of("kind", "node", "parser", "query")
                 .flatMap(input -> Stream.of(0, 1, 2)
                                         .flatMap(position -> Stream.of(false, true)
                                                                    .map(prefixed -> Arguments.of(input,
                                                                                                  position,
                                                                                                  prefixed))));
  }

  private static Stream<Arguments> multipleReplacements() {
    return Stream.of("parser", "query")
                 .flatMap(input -> Stream.of(0, 1, 2)
                                         .flatMap(position -> Stream.of(false, true)
                                                                    .map(prefixed -> Arguments.of(input,
                                                                                                  position,
                                                                                                  prefixed))));
  }

  @ParameterizedTest(name = "surviving collision: {0}, target {1}, prefixed {2}")
  @MethodSource("singleReplacements")
  public void survivingNameCollisionPreservesEveryOriginalAttribute(String input, int position, boolean prefixed) {
    Original original = original(prefixed);
    D2Node target = original.attributes().get(position);
    QNm survivorName = original.names().get((position + 1) % 3);
    QNm collision = prefixed ? new QNm(survivorName.getNamespaceURI(), "q", survivorName.getLocalName()) : survivorName;
    D2Node source = attribute(collision, "replacement");
    D2Node[] payload = input.equals("query") || input.equals("parser")
        ? new D2Node[] { attribute(new QNm("new"), "first replacement"), source }
        : new D2Node[] { source };
    assertThrows(QueryException.class, () -> replace(input, target, payload));
    assertPreserved(original);
    assertNull(original.root().getScope().resolvePrefix("q"));
    assertEquals("replacement", source.getValue().stringValue());
    assertDetached(source);
  }

  @ParameterizedTest(name = "duplicate payload: {0}, target {1}, different prefixes {2}")
  @MethodSource("multipleReplacements")
  public void duplicatePayloadNamesPreserveEveryOriginalAttribute(String input, int position, boolean prefixed) {
    Original original = original(prefixed);
    D2Node target = original.attributes().get(position);
    QNm firstName = original.names().get(position);
    QNm duplicateName = prefixed ? new QNm(firstName.getNamespaceURI(), "q", firstName.getLocalName()) : firstName;
    D2Node first = attribute(firstName, "first duplicate");
    D2Node second = attribute(duplicateName, "second duplicate");
    assertThrows(QueryException.class,
                 () -> replace(input, target, attribute(new QNm("new"), "valid first member"), first, second));
    assertPreserved(original);
    assertNull(original.root().getScope().resolvePrefix("q"));
    assertEquals("first duplicate", first.getValue().stringValue());
    assertEquals("second duplicate", second.getValue().stringValue());
    assertDetached(first);
    assertDetached(second);
  }

  @ParameterizedTest(name = "same name: {0}, target {1}, prefixed {2}")
  @MethodSource("singleReplacements")
  public void sameExpandedNameReplacementSucceeds(String input, int position, boolean prefixed) {
    Original original = original(prefixed);
    D2Node target = original.attributes().get(position);
    QNm targetName = original.names().get(position);
    QNm replacementName = prefixed ? new QNm(targetName.getNamespaceURI(), "q", targetName.getLocalName()) : targetName;
    D2Node source = attribute(replacementName, "replacement");
    D2Node result = replace(input, target, source);
    assertReplaced(original, target, result, source);
    if (prefixed)
      assertEquals("urn:same", original.root().getScope().resolvePrefix("q"));
  }

  @ParameterizedTest(name = "valid sequence: {0}, target {1}, prefixed {2}")
  @MethodSource("multipleReplacements")
  public void validMultiAttributeReplacementKeepsForwardOrder(String input, int position, boolean prefixed) {
    Original original = original(prefixed);
    D2Node target = original.attributes().get(position);
    QNm secondName = prefixed ? new QNm("urn:other", "q", target.getName().getLocalName()) : new QNm("second");
    D2Node first = attribute(target.getName(), "replacement");
    D2Node second = attribute(secondName, "second replacement");
    D2Node result = replace(input, target, first, second);
    assertReplaced(original, target, result, first, second);
  }

  @ParameterizedTest(name = "empty sequence: {0}, target {1}, prefixed {2}")
  @MethodSource("multipleReplacements")
  public void emptyReplacementRemovesOnlyTheTarget(String input, int position, boolean prefixed) {
    Original original = original(prefixed);
    D2Node target = original.attributes().get(position);
    assertNull(replace(input, target));
    assertReplaced(original, target, null);
  }

  @ParameterizedTest
  @ValueSource(strings = { "kind", "node", "parser", "query" })
  public void wrongSourceKindPreservesOriginalAttributes(String input) {
    Original original = original(false);
    D2Node source = new D2NodeFactory().element(new QNm("invalid"));
    assertThrows(QueryException.class, () -> replace(input, original.attributes().get(1), source));
    assertPreserved(original);
  }

  @Test
  public void parserFailureAfterAnAttributeLeavesTheTargetUntouched() {
    Original original = original(false);
    D2Node target = original.attributes().get(1);
    assertThrows(DocumentException.class, () -> target.replaceWith(handler -> {
      handler.attribute(new QNm("new"), new Str("replacement"));
      throw new DocumentException("Invalid replacement input");
    }));
    assertPreserved(original);
  }

  @Test
  public void documentParserCannotTurnAttributeReplacementIntoDeletion() {
    Original original = original(false);
    D2Node source = new D2NodeFactory().document(null);
    assertThrows(DocumentException.class, () -> original.attributes().get(1).replaceWith(source::parse));
    assertPreserved(original);
  }

  private static Original original(boolean prefixed) {
    ElementD2Node root = (ElementD2Node) new D2NodeFactory().element(new QNm("r"));
    List<D2Node> attributes = new ArrayList<>();
    List<QNm> names = new ArrayList<>();
    List<String> values = List.of("A", "B", "C");
    for (int i = 0; i < 3; i++) {
      QNm name = prefixed ? new QNm("urn:same", "p", "attribute" + i) : new QNm("attribute" + i);
      D2Node attribute = root.setAttribute(name, new Str(values.get(i)));
      attributes.add(attribute);
      names.add(attribute.getName());
    }
    return new Original(root, attributes, names, values);
  }

  private static D2Node attribute(QNm name, String value) {
    return new D2NodeFactory().attribute(name, new Str(value));
  }

  private static D2Node replace(String input, D2Node target, D2Node... content) {
    return switch (input) {
      case "kind" -> target.replaceWith(content[0].getKind(), content[0].getName(), content[0].getValue());
      case "node" -> target.replaceWith(content[0]);
      case "parser" -> target.replaceWith(handler -> {
        for (D2Node node : content)
          node.parse(handler);
      });
      case "query" -> {
        var context = new BrackitQueryContext();
        context.bind(new QNm("target"), target);
        context.bind(new QNm("content"), new ItemSequence(content));
        new Query("declare variable $target external; declare variable $content external; "
            + "replace node $target with $content").execute(context);
        yield null;
      }
      default -> throw new IllegalArgumentException();
    };
  }

  private static List<D2Node> attributes(ElementD2Node root) {
    List<D2Node> attributes = new ArrayList<>();
    try (var stream = root.getAttributes()) {
      D2Node node;
      while ((node = stream.next()) != null)
        attributes.add(node);
    }
    return attributes;
  }

  private static void assertPreserved(Original original) {
    List<D2Node> actual = attributes(original.root());
    assertEquals(original.attributes().size(), actual.size());
    for (int i = 0; i < actual.size(); i++) {
      D2Node attribute = original.attributes().get(i);
      assertSame(attribute, actual.get(i));
      assertSame(original.root(), attribute.getParent());
      assertSame(i == 2 ? null : original.attributes().get(i + 1), attribute.sibling);
      assertSame(original.names().get(i), attribute.getName());
      assertEquals(original.values().get(i), attribute.getValue().stringValue());
      assertTrue(attribute.isAttributeOf(original.root()));
    }
    assertSame(original.attributes().getFirst(), original.root().firstAttribute);
    assertNull(original.root().getFirstChild());
  }

  private static void assertReplaced(Original original, D2Node target, D2Node result, D2Node... sources) {
    List<D2Node> actual = attributes(original.root());
    List<D2Node> survivors = original.attributes().stream().filter(node -> node != target).toList();
    assertEquals(survivors.size() + sources.length, actual.size());
    for (int i = 0; i < survivors.size(); i++) {
      assertSame(survivors.get(i), actual.get(i));
      assertEquals(original.values().get(original.attributes().indexOf(survivors.get(i))),
                   actual.get(i).getValue().stringValue());
    }
    for (int i = 0; i < sources.length; i++) {
      D2Node inserted = actual.get(survivors.size() + i);
      assertNotSame(sources[i], inserted);
      assertEquals(sources[i].getName(), inserted.getName());
      assertEquals(sources[i].getValue().stringValue(), inserted.getValue().stringValue());
      assertDetached(sources[i]);
    }
    for (int i = 0; i < actual.size(); i++) {
      assertSame(original.root(), actual.get(i).getParent());
      assertSame(i + 1 < actual.size() ? actual.get(i + 1) : null, actual.get(i).sibling);
    }
    if (result != null)
      assertSame(actual.get(survivors.size()), result);
    assertDetached(target);
    assertEquals(original.values().get(original.attributes().indexOf(target)), target.getValue().stringValue());
  }

  private static void assertDetached(D2Node node) {
    assertNull(node.getParent());
    assertNull(node.sibling);
    assertNull(node.getPreviousSibling());
    assertNull(node.getNextSibling());
  }
}
