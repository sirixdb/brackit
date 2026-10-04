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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.atomic.Bool;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.parser.DocumentParser;
import io.brackit.query.node.parser.FragmentHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateBoundaryRegressionTest extends XQueryBaseTest {
  private static final String NESTED = "<a x='1' y='2'>one<b z='3'>two<c/>three<!--deep--><?deep value?>four</b>five</a>";

  @Test
  public void fragmentHelperMergesInsertedTextImmediately() {
    var factory = new BrackitQueryContext().getNodeFactory();
    Node<?> root = new FragmentHelper().openElement("r").content("a").insert(factory.text(new Str("b")))
                                      .closeElement().getRoot();
    var context = new BrackitQueryContext();
    context.setContextItem(root);
    assertEquals(Bool.TRUE, new Query("count($$/text()) = 1 and string($$/text()) = 'ab'").execute(context));
    assertEquals("ab", root.getFirstChild().getValue().stringValue());
    assertNull(root.getFirstChild().getNextSibling());
  }

  @ParameterizedTest
  @ValueSource(strings = { "success", "failure" })
  public void ordinaryInsertionMergesAfterPendingUpdatesReturn(String outcome) {
    Node<?> document = document("<r a='original'>old</r>");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    if (outcome.equals("failure")) {
      assertThrows(QueryException.class,
          () -> new Query("insert node attribute a {'duplicate'} into $$/r").execute(context));
    } else {
      new Query("insert node text {'x'} into $$/r").execute(context);
    }
    document.getFirstChild().append(context.getNodeFactory().text(new Str("Y")));
    assertDocument("<r a='original'>" + (outcome.equals("failure") ? "oldY" : "oldxY") + "</r>", document);
    assertNull(document.getFirstChild().getFirstChild().getNextSibling());
  }

  private static Stream<Arguments> ordinaryTextInsertions() {
    return Stream.of("append", "prepend", "before", "after").flatMap(position -> {
      Stream<String> boundaries = switch (position) {
        case "append" -> Stream.of("none", "left");
        case "prepend" -> Stream.of("none", "right");
        default -> Stream.of("none", "left", "right", "both");
      };
      return boundaries.flatMap(boundary -> Stream.of("kind", "node", "parser")
                                                 .map(input -> Arguments.of(position, boundary, input)));
    });
  }

  @ParameterizedTest(name = "ordinary {0}, {1} text boundary, {2} input")
  @MethodSource("ordinaryTextInsertions")
  public void ordinaryInsertionsMergeEveryAdjacentTextBoundary(String position, String boundary, String input) {
    boolean leftText = boundary.equals("left") || boundary.equals("both");
    boolean rightText = boundary.equals("right") || boundary.equals("both");
    String left = position.equals("prepend") ? "" : leftText ? "L" : "<left/>";
    String right = position.equals("append") ? "" : rightText ? "R" : "<right/>";
    Node<?> document = document("<r>" + left + (boundary.equals("both") ? "<cut/>" : "") + right + "</r>");
    Node<?> root = document.getFirstChild();
    if (boundary.equals("both")) child(root, "cut").delete();
    Node<?> leftNode = root.getFirstChild();
    Node<?> rightNode = root.getLastChild();
    Node<?> source = new BrackitQueryContext().getNodeFactory().text(new Str("X"));
    Node<?> inserted = switch (input) {
      case "kind" -> switch (position) {
        case "append" -> root.append(Kind.TEXT, null, source.getValue());
        case "prepend" -> root.prepend(Kind.TEXT, null, source.getValue());
        case "before" -> rightNode.insertBefore(Kind.TEXT, null, source.getValue());
        case "after" -> leftNode.insertAfter(Kind.TEXT, null, source.getValue());
        default -> throw new IllegalArgumentException();
      };
      case "node" -> switch (position) {
        case "append" -> root.append(source);
        case "prepend" -> root.prepend(source);
        case "before" -> rightNode.insertBefore(source);
        case "after" -> leftNode.insertAfter(source);
        default -> throw new IllegalArgumentException();
      };
      case "parser" -> switch (position) {
        case "append" -> root.append(source::parse);
        case "prepend" -> root.prepend(source::parse);
        case "before" -> rightNode.insertBefore(source::parse);
        case "after" -> leftNode.insertAfter(source::parse);
        default -> throw new IllegalArgumentException();
      };
      default -> throw new IllegalArgumentException();
    };
    assertDocument("<r>" + left + "X" + right + "</r>", document);
    assertTrue(inserted.isChildOf(root));
    assertEquals((leftText ? "L" : "") + "X" + (rightText ? "R" : ""), inserted.getValue().stringValue());
    assertEquals("X", source.getValue().stringValue());
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    assertEquals(Bool.TRUE, new Query("count($$/r/text()) = 1").execute(context));
    if (boundary.equals("both")) {
      assertDetached(rightNode);
      assertEquals("R", rightNode.getValue().stringValue());
    }
  }

  private static Stream<Arguments> namespaceUpdates() {
    return Stream.of("into", "as first into", "as last into", "before", "after", "replace")
                 .flatMap(position -> Stream.of(false, true).map(prefixed -> Arguments.of(position, prefixed)));
  }

  @ParameterizedTest(name = "namespace scopes {0}, prefixed root {1}")
  @MethodSource("namespaceUpdates")
  public void snapshotsPreserveRootAndDescendantScopes(String position, boolean prefixed) {
    String name = prefixed ? "p:src" : "src";
    var context = new BrackitQueryContext();
    Node<?> document = document("<r xmlns:inherited='urn:inherited'><" + name
        + " xmlns:p='urn:source' xmlns:q='urn:unused' a='1' b='2'>p:value"
        + "<child xmlns:p='urn:child'><leaf>p:value</leaf></child></" + name
        + "><dest xmlns:p='urn:dest' xmlns:inherited='urn:dest-inherited'><anchor/></dest></r>");
    Node<?> source = document.getFirstChild().getFirstChild();
    Node<?> destination = source.getNextSibling();
    Node<?> target = position.equals("before") || position.equals("after") || position.equals("replace")
        ? destination.getFirstChild() : destination;
    context.bind(new QNm("source"), source);
    context.bind(new QNm("target"), target);
    String update = position.equals("replace") ? "replace node $target with $source"
        : "insert node $source " + position + " $target";
    new Query("declare variable $source external; declare variable $target external; " + update).execute(context);

    Node<?> copy = child(destination, "src");
    assertEquals("urn:source", resolve(copy, "p:value").getNamespaceURI());
    assertEquals("urn:unused", resolve(copy, "q:value").getNamespaceURI());
    assertEquals("urn:inherited", resolve(copy, "inherited:value").getNamespaceURI());
    assertEquals("urn:child", resolve(child(child(copy, "child"), "leaf"), "p:value").getNamespaceURI());
    assertEquals("urn:dest", resolve(destination, "p:value").getNamespaceURI());
    assertEquals(source.getName(), copy.getName());
    assertEquals(source.getName().getPrefix(), copy.getName().getPrefix());
    assertEquals(List.of("a", "b"), attributes(copy));
    assertNotSame(source, copy);
  }

  @Test
  public void copyPreservesInheritedDefaultNamespaceAndUndeclarations() {
    Node<?> sourceDocument = document("<outer xmlns='urn:outer' xmlns:p='urn:p'><src><child xmlns=''/></src></outer>");
    Node<?> source = sourceDocument.getFirstChild().getFirstChild();
    Node<?> targetDocument = document("<r xmlns='urn:destination'/>");
    var context = new BrackitQueryContext();
    context.bind(new QNm("source"), source);
    context.bind(new QNm("target"), targetDocument.getFirstChild());
    new Query("declare variable $source external; declare variable $target external; insert node $source into $target").execute(context);

    Node<?> copy = targetDocument.getFirstChild().getFirstChild();
    assertEquals("urn:outer", resolve(copy, "value").getNamespaceURI());
    assertEquals("urn:p", resolve(copy, "p:value").getNamespaceURI());
    assertEquals("", resolve(copy.getFirstChild(), "value").getNamespaceURI());
  }

  private static Stream<Arguments> replacements() {
    List<Arguments> cases = new ArrayList<>();
    String[][] payloads = { { "(<a/>, <b/>, <c/>)", "<a/><b/><c/>" },
        { "(text {'a'}, text {'b'}, text {'c'})", "abc" },
        { "(text {'a'}, <b/>, text {'c'}, <d/>, text {'e'})", "a<b/>c<d/>e" },
        { "(" + NESTED + ", <d/>, comment {'end'}, processing-instruction p {'value'})",
            NESTED + "<d/><!--end--><?p value?>" }, { "()", "" } };
    for (boolean text : List.of(false, true)) {
      for (String position : List.of("first", "middle", "last")) {
        String prefix = position.equals("first") ? "" : text ? "<head/>" : "<head/>left";
        String suffix = position.equals("last") ? "" : text ? "<tail/>" : "right<tail/>";
        String input = "<r>" + prefix + (text ? "old" : "<x/>") + suffix + "</r>";
        for (String[] payload : payloads) {
          cases.add(Arguments.of(input, text ? "$$/r/text()" : "$$/r/x", payload[0],
                                 "<r>" + prefix + payload[1] + suffix + "</r>"));
        }
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "replace {1} in {0} with {2}")
  @MethodSource("replacements")
  public void replacementKeepsSequenceOrderAndRemovesEmptyContent(String input, String target, String source,
                                                                  String expected) {
    Node<?> document = document(input);
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    Node<?> saved;
    try (var selected = new Query(target).execute(context).iterate()) {
      saved = assertInstanceOf(Node.class, selected.next());
      assertNull(selected.next());
    }
    new Query("replace node " + target + " with " + source).execute(context);

    assertDocument(expected, document);
    assertDetached(saved);
    deleteSaved(saved);
    assertDocument(expected, document);
  }

  @ParameterizedTest
  @ValueSource(strings = { "<r>old<tail/></r>", "<r><head/>old<tail/></r>", "<r><head/>old</r>" })
  public void repeatedTextReplacementSourcesAreSnapshots(String input) {
    Node<?> document = document(input);
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    new Query("replace node $$/r/text() with ($$/r/text(), text {'x'}, $$/r/text())").execute(context);
    assertDocument(input.replace("old", "oldxold"), document);
  }

  @Test
  public void replacementSnapshotsBeforeOtherPendingUpdates() {
    Node<?> document = document("<r><src xmlns:p='urn:source' a='old'>old</src><x/><tail/></r>");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    new Query("(insert node text {'mut'} into $$/r/src, replace value of node $$/r/src/@a with 'new', "
        + "replace node $$/r/x with ($$/r/src, $$/r/src))").execute(context);
    assertDocument("<r><src a='new'>oldmut</src><src a='old'>old</src><src a='old'>old</src><tail/></r>", document);
    Node<?> copy = document.getFirstChild().getFirstChild().getNextSibling();
    assertEquals("urn:source", resolve(copy, "p:value").getNamespaceURI());
    assertEquals("urn:source", resolve(copy.getNextSibling(), "p:value").getNamespaceURI());
  }

  @Test
  public void adjacentPendingReplacementsKeepTheirBoundaries() {
    Node<?> document = document("<r>L<x/>R<y/>S<tail/></r>");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    new Query("(replace node $$/r/x with text {'M'}, "
        + "replace node $$/r/text()[?2] with (text {'a'}, <b/>, text {'c'}), "
        + "replace node $$/r/y with (text {'d'}, <e/>, text {'f'}))").execute(context);
    assertDocument("<r>LMa<b/>cd<e/>fS<tail/></r>", document);
  }

  private static Stream<Arguments> pendingTextTargets() {
    return Stream.of("into", "as first into", "as last into", "before", "after")
                 .flatMap(position -> Stream.of("delete", "replace", "value")
                                            .map(update -> Arguments.of(position, update)));
  }

  @ParameterizedTest(name = "{0} before pending {1} of original text")
  @MethodSource("pendingTextTargets")
  public void textInsertionStaysSeparateFromPendingTarget(String position, String update) {
    Node<?> document = document("<r>old</r>");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    String target = position.equals("before") || position.equals("after") ? "$$/r/text()" : "$$/r";
    String pending = switch (update) {
      case "delete" -> "delete node $$/r/text()";
      case "replace" -> "replace node $$/r/text() with <new/>";
      case "value" -> "replace value of node $$/r/text() with 'new'";
      default -> throw new IllegalArgumentException();
    };
    new Query("(insert node text {'x'} " + position + " " + target + ", " + pending + ")").execute(context);
    boolean first = position.equals("before") || position.equals("as first into");
    String replacement = update.equals("delete") ? "" : update.equals("replace") ? "<new/>" : "new";
    assertDocument("<r>" + (first ? "x" + replacement : replacement + "x") + "</r>", document);
  }

  @ParameterizedTest
  @ValueSource(strings = { "element r { ($source, 'b', 'c', text {'d'}) }",
      "<r>{($source, 'b', 'c', text {'d'})}</r>", "document { ($source, 'b', 'c', text {'d'}) }" })
  public void constructorsStillNormalizeCopiedText(String expression) {
    Node<?> source = document("<src>orig</src>").getFirstChild().getFirstChild();
    var context = new BrackitQueryContext();
    context.bind(new QNm("source"), source);
    Node<?> constructed = assertInstanceOf(Node.class,
        new Query("declare variable $source external; " + expression).execute(context));
    Node<?> text = constructed.getFirstChild();
    assertEquals(Kind.TEXT, text.getKind());
    assertEquals("origb cd", text.getValue().stringValue());
    assertNull(text.getNextSibling());
    assertEquals("orig", source.getValue().stringValue());
  }

  @ParameterizedTest
  @ValueSource(strings = { "()", "(attribute a {'A'}, attribute b {'B'})" })
  public void attributeReplacementPreservesOrderAndDetachesTheTarget(String source) {
    Node<?> document = document("<r first='1' x='old' last='3'/>");
    Node<?> saved = document.getFirstChild().getAttribute(new QNm("x"));
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    new Query("replace node $$/r/@x with " + source).execute(context);
    String newAttributes = source.equals("()") ? "" : " a='A' b='B'";
    assertDocument("<r first='1' last='3'" + newAttributes + "/>", document);
    assertEquals(source.equals("()") ? List.of("first", "last") : List.of("first", "last", "a", "b"),
                 attributes(document.getFirstChild()));
    assertDetached(saved);
    deleteSaved(saved);
    assertDocument("<r first='1' last='3'" + newAttributes + "/>", document);
  }

  @ParameterizedTest
  @ValueSource(strings = { "replace node $$/r/x with attribute bad {'value'}", "replace node $$/r/@a with <bad/>" })
  public void replacementStillValidatesSourceKinds(String update) {
    Node<?> document = document("<r a='1'><x/></r>");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    assertThrows(QueryException.class, () -> new Query(update).execute(context));
    assertDocument("<r a='1'><x/></r>", document);
  }

  @ParameterizedTest
  @ValueSource(strings = { "merged", "empty" })
  public void retainedNormalizedTextCannotDeleteSurvivingSiblings(String variant) {
    Node<?> document = document("<r>L<x/>R<tail/></r>");
    Node<?> saved = document.getFirstChild().getFirstChild().getNextSibling().getNextSibling();
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    String update = variant.equals("merged") ? "delete node $$/r/x"
        : "replace value of node $$/r/text()[?last()] with ''";
    new Query(update).execute(context);
    String expected = variant.equals("merged") ? "<r>LR<tail/></r>" : "<r>L<x/><tail/></r>";
    assertDocument(expected, document);
    assertDetached(saved);
    deleteSaved(saved);
    assertDocument(expected, document);
  }

  private static Stream<Arguments> subtreeRemovals() {
    return Stream.of("delete", "query replace", "query content", "set value", "kind", "node", "parser")
                 .flatMap(removal -> Stream.of("first", "middle", "last").map(position -> Arguments.of(removal, position)));
  }

  @ParameterizedTest(name = "detach via {0} at {1}")
  @MethodSource("subtreeRemovals")
  public void removedSubtreesKeepTheirContentAndNamespaceScope(String removal, String position) {
    String prefix = position.equals("first") ? "" : "<head/>";
    String suffix = position.equals("last") ? "" : "<tail/>";
    Node<?> document = document("<r xmlns:p='urn:outer'>" + prefix + "<x a='A'><inner>p:value</inner></x>" + suffix + "</r>");
    Node<?> root = document.getFirstChild();
    List<Node<?>> originals = new ArrayList<>();
    try (var children = root.getChildren()) {
      Node<?> next;
      while ((next = children.next()) != null) originals.add(next);
    }
    Node<?> saved = child(root, "x");
    var context = new BrackitQueryContext();
    context.setContextItem(document);
    String expected = "<r>" + prefix + "<new/>" + suffix + "</r>";
    switch (removal) {
      case "delete" -> { new Query("delete node $$/r/x").execute(context); expected = "<r>" + prefix + suffix + "</r>"; }
      case "query replace" -> new Query("replace node $$/r/x with <new/>").execute(context);
      case "query content" -> { new Query("replace value of node $$/r with 'new'").execute(context); expected = "<r>new</r>"; }
      case "set value" -> { root.setValue(new Str("new")); expected = "<r>new</r>"; }
      case "kind" -> saved.replaceWith(Kind.ELEMENT, new QNm("new"), null);
      case "node" -> saved.replaceWith(context.getNodeFactory().element(new QNm("new")));
      case "parser" -> saved.replaceWith(context.getNodeFactory().element(new QNm("new"))::parse);
      default -> throw new IllegalArgumentException();
    }
    assertDocument(expected, document);
    assertDetached(saved);
    assertEquals("A", saved.getAttribute(new QNm("a")).getValue().stringValue());
    assertTrue(saved.getFirstChild().isChildOf(saved));
    assertEquals("urn:outer", resolve(saved.getFirstChild(), "p:value").getNamespaceURI());
    assertEquals(-Integer.signum(root.cmp(saved)), Integer.signum(saved.cmp(root)));
    if (removal.equals("query content") || removal.equals("set value")) {
      for (Node<?> old : originals) assertDetached(old);
    }
    deleteSaved(saved);
    assertDocument(expected, document);
    Node<?> reuse = document("<reuse/>");
    var reuseContext = new BrackitQueryContext();
    reuseContext.setContextItem(reuse);
    reuseContext.bind(new QNm("saved"), saved);
    new Query("declare variable $saved external; insert node $saved into $$/reuse").execute(reuseContext);
    assertDocument("<reuse><x a='A'><inner>p:value</inner></x></reuse>", reuse);
    assertEquals("urn:outer", resolve(reuse.getFirstChild().getFirstChild(), "p:value").getNamespaceURI());
  }

  private static Stream<Arguments> attributeRemovals() {
    return Stream.of("delete", "kind", "node", "parser")
                 .flatMap(removal -> Stream.of("first", "x", "last").map(name -> Arguments.of(removal, name)));
  }

  @ParameterizedTest
  @MethodSource("attributeRemovals")
  public void directAttributeRemovalDetachesSavedReferences(String removal, String name) {
    Node<?> document = document("<r first='1' x='old' last='3'/>");
    Node<?> root = document.getFirstChild();
    Node<?> saved = root.getAttribute(new QNm(name));
    var factory = new BrackitQueryContext().getNodeFactory();
    switch (removal) {
      case "delete" -> root.deleteAttribute(new QNm(name));
      case "kind" -> saved.replaceWith(Kind.ATTRIBUTE, new QNm("new"), new Str("value"));
      case "node" -> saved.replaceWith(factory.attribute(new QNm("new"), new Str("value")));
      case "parser" -> saved.replaceWith(factory.attribute(new QNm("new"), new Str("value"))::parse);
      default -> throw new IllegalArgumentException();
    }
    String remaining = switch (name) {
      case "first" -> " x='old' last='3'";
      case "x" -> " first='1' last='3'";
      case "last" -> " first='1' x='old'";
      default -> throw new IllegalArgumentException();
    };
    String expected = "<r" + remaining + (removal.equals("delete") ? "" : " new='value'") + "/>";
    assertDocument(expected, document);
    assertDetached(saved);
    deleteSaved(saved);
    assertDocument(expected, document);
  }

  @Test
  public void insertingAfterBoundAnchorDoesNotScanUnrelatedChildren() {
    int small = insertionWork(16);
    int large = insertionWork(4096);
    assertTrue(large <= 10, "tree navigation count: " + large);
    assertEquals(small, large);
  }

  private int insertionWork(int size) {
    Node<?> document = document("<r><anchor/>" + "<n/>".repeat(size) + "</r>");
    var counter = new NavigationCounter();
    var context = new BrackitQueryContext();
    context.bind(new QNm("anchor"), counter.wrap(document.getFirstChild().getFirstChild()));
    new Query("declare variable $anchor external; insert node <inserted/> after $anchor").execute(context);
    int work = counter.visits;
    Node<?> inserted = document.getFirstChild().getFirstChild().getNextSibling();
    assertEquals("inserted", inserted.getName().getLocalName());
    int count = 0;
    try (var children = document.getFirstChild().getChildren()) {
      while (children.next() != null) count++;
    }
    assertEquals(size + 2, count);
    return work;
  }

  private static class NavigationCounter {
    int visits;
    final Map<Node<?>, Node<?>> wrappers = new IdentityHashMap<>();
    final Map<Object, Node<?>> delegates = new IdentityHashMap<>();

    Node<?> wrap(Node<?> node) {
      if (node == null) return null;
      Node<?> existing = wrappers.get(node);
      if (existing != null) return existing;
      Node<?> proxy = (Node<?>) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
          (receiver, method, arguments) -> {
            if (method.getName().equals("getNextSibling") || method.getName().equals("getFirstChild")
                || method.getName().equals("getPreviousSibling") || method.getName().equals("getLastChild")) visits++;
            if (arguments != null) {
              arguments = arguments.clone();
              for (int index = 0; index < arguments.length; index++) {
                if (delegates.containsKey(arguments[index])) arguments[index] = delegates.get(arguments[index]);
              }
            }
            try {
              Object result = method.invoke(node, arguments);
              if (Node.class.isAssignableFrom(method.getReturnType())) return wrap((Node<?>) result);
              return result;
            } catch (InvocationTargetException exception) {
              throw exception.getCause();
            }
          });
      wrappers.put(node, proxy);
      delegates.put(proxy, node);
      return proxy;
    }
  }

  private static Node<?> document(String xml) {
    return new BrackitQueryContext().getNodeFactory().build(new DocumentParser(xml));
  }

  private static void assertDocument(String xml, Node<?> actual) {
    ResultChecker.dCheck(document(xml), actual, false);
  }

  private static Node<?> child(Node<?> parent, String name) {
    try (var children = parent.getChildren()) {
      Node<?> node;
      while ((node = children.next()) != null) {
        if (node.getKind() == Kind.ELEMENT && node.getName().getLocalName().equals(name)) return node;
      }
    }
    throw new AssertionError("Missing child " + name);
  }

  private static List<String> attributes(Node<?> node) {
    List<String> result = new ArrayList<>();
    try (var attributes = node.getAttributes()) {
      Node<?> attribute;
      while ((attribute = attributes.next()) != null) result.add(attribute.getName().getLocalName());
    }
    return result;
  }

  private static QNm resolve(Node<?> node, String name) {
    var context = new BrackitQueryContext();
    context.bind(new QNm("node"), node);
    return assertInstanceOf(QNm.class, new Query("declare variable $node external; resolve-QName('" + name + "', $node)").execute(context));
  }

  private static void assertDetached(Node<?> node) {
    assertNull(node.getParent());
    assertNull(node.getPreviousSibling());
    assertNull(node.getNextSibling());
  }

  private static void deleteSaved(Node<?> node) {
    var context = new BrackitQueryContext();
    context.bind(new QNm("saved"), node);
    new Query("declare variable $saved external; delete node $saved").execute(context);
  }
}
