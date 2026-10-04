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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.d2linked.D2Node;
import io.brackit.query.node.parser.DocumentParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InsertSequenceOrderTest extends XQueryBaseTest {
  private static final String DOCUMENT = "<r><left/><anchor/><right/></r>";
  private static final String NESTED_ELEMENT = "<a x='1' y='2'>one<b z='3'>two<c/>three<!--deep--><?deep value?>four</b>five<!--tail--><?tail value?>six</a>";
  private static final String NESTED_SOURCE = "(" + NESTED_ELEMENT
      + ", <d/>, text {'end'}, comment {'outer'}, processing-instruction outer {'value'})";
  private static final String NESTED_XML = NESTED_ELEMENT + "<d/>end<!--outer--><?outer value?>";

  private static Stream<Arguments> xmlInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "elements", "(<a/>, <b/>, <c/>)", "<a/><b/><c/>", "" }, { "text",
        "(text {'a'}, text {'b'}, text {'c'})", "abc", "" }, { "mixed", "(<a/>, text {'b'}, <c/>, text {'d'})",
            "<a/>b<c/>d", "" }, { "attributes and children",
                "(attribute x {'1'}, attribute y {'2'}, <a/>, text {'b'}, <c/>)", "<a/>b<c/>", " x='1' y='2'" }, {
                    "attributes", "(attribute x {'1'}, attribute y {'2'}, attribute z {'3'})", "",
                    " x='1' y='2' z='3'" }, { "single element", "<a/>", "<a/>", "" }, { "single text", "text {'a'}",
                        "a", "" }, { "single attribute", "attribute x {'1'}", "", " x='1'" }, { "nested",
                            NESTED_SOURCE, NESTED_XML, "" }, { "nested with leading attributes",
                                "(attribute p {'1'}, attribute q {'2'}, " + NESTED_SOURCE + ")", NESTED_XML,
                                " p='1' q='2'" } };
    for (String position : List.of("into", "as first into", "as last into", "before", "after")) {
      for (String[] payload : content) {
        String children = switch (position) {
          case "as first into" -> payload[2] + "<left/><anchor/><right/>";
          case "before" -> "<left/>" + payload[2] + "<anchor/><right/>";
          case "after" -> "<left/><anchor/>" + payload[2] + "<right/>";
          default -> "<left/><anchor/><right/>" + payload[2];
        };
        String target = position.equals("before") || position.equals("after") ? "$$/r/anchor" : "$$/r";
        cases.add(Arguments.of(position, payload[0], payload[1], target, "<r" + payload[3] + ">" + children + "</r>"));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0}: {1}")
  @MethodSource("xmlInsertions")
  public void xmlSequenceKeepsDocumentOrder(String position, String label, String source, String target,
      String expectedXml) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
    ctx.setContextItem(document);

    new Query("insert nodes " + source + " " + position + " " + target).execute(ctx);

    Node<?> expected = ctx.getNodeFactory().build(new DocumentParser(expectedXml));
    ResultChecker.dCheck(expected, document, false);
    assertForwardTraversal(document);
    // Attribute application keeps its encounter order independently of child insertion.
    assertEquals(attributeNames(expected.getFirstChild()), attributeNames(document.getFirstChild()));
  }

  private static Stream<Arguments> textBoundaryInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "(<a/>, <b/>, <c/>)", "<a/><b/><c/>" }, { "(text {'a'}, text {'b'}, text {'c'})", "abc" },
        { "(text {'a'}, <b/>, text {'c'}, <d/>, text {'e'})", "a<b/>c<d/>e" }, { "<a/>", "<a/>" }, { "text {'a'}",
            "a" }, { NESTED_SOURCE, NESTED_XML } };
    for (String position : List.of("into", "as first into", "as last into", "before", "after")) {
      for (String[] payload : content) {
        String children = switch (position) {
          case "as first into" -> payload[1] + "left<anchor/>right";
          case "before" -> "left" + payload[1] + "<anchor/>right";
          case "after" -> "left<anchor/>" + payload[1] + "right";
          default -> "left<anchor/>right" + payload[1];
        };
        String target = position.equals("before") || position.equals("after") ? "$$/r/anchor" : "$$/r";
        cases.add(Arguments.of(position, payload[0], target, "<r>" + children + "</r>"));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0} with adjacent text: {1}")
  @MethodSource("textBoundaryInsertions")
  public void insertionMergesBoundaryTextInDocumentOrder(String position, String source, String target,
      String expectedXml) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser("<r>left<anchor/>right</r>"));
    ctx.setContextItem(document);

    new Query("insert nodes " + source + " " + position + " " + target).execute(ctx);

    Node<?> expected = ctx.getNodeFactory().build(new DocumentParser(expectedXml));
    ResultChecker.dCheck(expected, document, false);
    assertForwardTraversal(document);
  }

  private static Stream<Arguments> textAnchorInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "(<a/>, <b/>, <c/>)", "<a/><b/><c/>" }, { "(text {'a'}, text {'b'}, text {'c'})", "abc" },
        { "(text {'a'}, <b/>, text {'c'}, <d/>, text {'e'})", "a<b/>c<d/>e" }, { "<a/>", "<a/>" }, { "text {'a'}",
            "a" }, { NESTED_SOURCE, NESTED_XML } };
    for (String position : List.of("before", "after")) {
      for (String[] payload : content) {
        String children = position.equals("before") ? payload[1] + "old" : "old" + payload[1];
        cases.add(Arguments.of(position, payload[0], "<r><left/>" + children + "<right/></r>"));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0} text anchor: {1}")
  @MethodSource("textAnchorInsertions")
  public void insertionBesideTextKeepsDocumentOrder(String position, String source, String expectedXml) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser("<r><left/>old<right/></r>"));
    ctx.setContextItem(document);

    new Query("insert nodes " + source + " " + position + " $$/r/text()").execute(ctx);

    Node<?> expected = ctx.getNodeFactory().build(new DocumentParser(expectedXml));
    ResultChecker.dCheck(expected, document, false);
    assertForwardTraversal(document);
  }

  private static Stream<Arguments> aliasedInsertions() {
    List<Arguments> cases = new ArrayList<>();
    for (String position : List.of("into", "as first into", "as last into", "before", "after")) {
      for (boolean repeated : List.of(false, true)) {
        String source = "($$/r/text(), text {'x'}" + (repeated ? ", $$/r/text())" : ")");
        String payload = repeated ? "oldxold" : "oldx";
        String value = position.equals("as first into") || position.equals("before")
            ? payload + "old"
            : "old" + payload;
        String target = position.equals("before") || position.equals("after") ? "$$/r/text()" : "$$/r";
        cases.add(Arguments.of(position, source, target, "<r>" + value + "</r>"));
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0} with aliased source: {1}")
  @MethodSource("aliasedInsertions")
  public void insertSnapshotsOverlappingSources(String position, String source, String target, String expectedXml) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser("<r>old</r>"));
    ctx.setContextItem(document);

    new Query("insert nodes " + source + " " + position + " " + target).execute(ctx);

    assertDocument(expectedXml, document);
  }

  @ParameterizedTest(name = "{0} snapshots before other pending updates")
  @MethodSource("insertPositions")
  public void pendingInsertSnapshotsSubtreesAndAttributes(String position) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser("<r><src a='old'>old</src><dest><anchor/></dest></r>"));
    ctx.setContextItem(document);
    String target = position.equals("before") || position.equals("after") ? "$$/r/dest/anchor" : "$$/r/dest";

    new Query("(insert nodes (<n/>, text {'x'}) into $$/r/src, "
        + "replace value of node $$/r/src/@a with 'new', "
        + "insert nodes ($$/r/src/@a, $$/r/src) " + position + " " + target + ")").execute(ctx);

    String snapshot = "<src a='old'>old</src>";
    String children = position.equals("as first into") || position.equals("before")
        ? snapshot + "<anchor/>"
        : "<anchor/>" + snapshot;
    assertDocument("<r><src a='new'>old<n/>x</src><dest a='old'>" + children + "</dest></r>", document);
  }

  @ParameterizedTest(name = "{0} snapshots text before another pending insertion")
  @MethodSource("insertPositions")
  public void pendingInsertSnapshotsText(String position) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser("<r>old</r>"));
    ctx.setContextItem(document);
    String target = position.equals("before") || position.equals("after") ? "$$/r/text()" : "$$/r";

    new Query("(insert node text {'x'} into $$/r, insert nodes ($$/r/text(), text {'y'}) " + position + " "
        + target + ")").execute(ctx);

    String value = position.equals("as first into") || position.equals("before") ? "oldyoldx" : "oldxoldy";
    assertDocument("<r>" + value + "</r>", document);
  }

  private static Stream<String> insertPositions() {
    return Stream.of("into", "as first into", "as last into", "before", "after");
  }

  private static Stream<Arguments> deletionInsertions() {
    return insertPositions().flatMap(position -> Stream.of("<x/>", "<!--x-->", "<?x value?>")
                                                       .map(separator -> Arguments.of(position, separator)));
  }

  @ParameterizedTest(name = "{0} after deleting {1}")
  @MethodSource("deletionInsertions")
  public void deletionNormalizesTextBeforeNextQuery(String position, String separator) {
    var deletionContext = new BrackitQueryContext();
    Node<?> document = deletionContext.getNodeFactory().build(new DocumentParser("<r>L" + separator + "R</r>"));
    deletionContext.setContextItem(document);

    new Query("delete node $$/r/node()[?2]").execute(deletionContext);
    assertDocument("<r>LR</r>", document);

    var insertionContext = new BrackitQueryContext();
    insertionContext.setContextItem(document);
    String target = position.equals("before") || position.equals("after") ? "$$/r/text()" : "$$/r";
    new Query("insert nodes (<a/>, text {'b'}, <c/>) " + position + " " + target).execute(insertionContext);

    String children = position.equals("as first into") || position.equals("before")
        ? "<a/>b<c/>LR"
        : "LR<a/>b<c/>";
    assertDocument("<r>" + children + "</r>", document);
  }

  private static Stream<Arguments> completionUpdates() {
    return Stream.of(Arguments.of("<r>L<x/>M<y/>R</r>", "delete nodes ($$/r/x, $$/r/y)", "<r>LMR</r>"),
                     Arguments.of("<r>L<x/>R</r>", "delete nodes ($$/r/x, $$/r/text()[?last()])", "<r>L</r>"),
                     Arguments.of("<r>L<x/>R</r>", "delete nodes ($$/r/x, $$/r/text()[?1])", "<r>R</r>"),
                     Arguments.of("<r>L<x/>R</r>", "replace node $$/r/x with text {'M'}", "<r>LMR</r>"),
                     Arguments.of("<r>L<x/>R</r>", "replace value of node $$/r/text()[?1] with ''", "<r><x/>R</r>"),
                     Arguments.of("<r>L<x/>R</r>",
                                  "(replace value of node $$/r/text()[?1] with '', delete node $$/r/x)",
                                  "<r>R</r>"),
                     Arguments.of("<r><x>old</x></r>", "replace value of node $$/r/x with ''", "<r><x/></r>"),
                     Arguments.of("<r>L<x/>R</r>", "delete nodes $$/r/node()", "<r/>"));
  }

  @ParameterizedTest(name = "normalize on completion: {1}")
  @MethodSource("completionUpdates")
  public void completionNormalizesWithoutConsumingPendingTargets(String input, String update, String expectedXml) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser(input));
    ctx.setContextItem(document);

    new Query(update).execute(ctx);

    assertDocument(expectedXml, document);
  }

  private static Stream<Arguments> nodeInsertions() {
    return insertPositions().flatMap(position -> Stream.of("node", "parser", "kind")
                                                       .flatMap(representation -> Stream.of(Kind.ELEMENT,
                                                                                            Kind.TEXT,
                                                                                            Kind.COMMENT,
                                                                                            Kind.PROCESSING_INSTRUCTION)
                                                                                        .map(kind -> Arguments.of(position,
                                                                                                                   representation,
                                                                                                                   kind))));
  }

  @ParameterizedTest(name = "node API {0}: {1} {2}")
  @MethodSource("nodeInsertions")
  public void nodeInsertionRepresentationsUseTheSamePosition(String position, String representation, Kind kind) {
    var ctx = new BrackitQueryContext();
    Node<?> document = ctx.getNodeFactory().build(new DocumentParser(DOCUMENT));
    ctx.setContextItem(document);
    String xml = switch (kind) {
      case ELEMENT -> NESTED_ELEMENT;
      case TEXT -> "payload";
      case COMMENT -> "<!--payload-->";
      case PROCESSING_INSTRUCTION -> "<?p payload?>";
      default -> throw new IllegalArgumentException();
    };
    Node<?> source = ctx.getNodeFactory().build(new DocumentParser("<source>" + xml + "</source>"))
                        .getFirstChild().getFirstChild();
    Node<?> target = position.equals("before") || position.equals("after")
        ? document.getFirstChild().getFirstChild().getNextSibling()
        : document.getFirstChild();
    Node<?> inserted = switch (representation) {
      case "node" -> switch (position) {
        case "as first into" -> target.prepend(source);
        case "before" -> target.insertBefore(source);
        case "after" -> target.insertAfter(source);
        default -> target.append(source);
      };
      case "parser" -> switch (position) {
        case "as first into" -> target.prepend(source::parse);
        case "before" -> target.insertBefore(source::parse);
        case "after" -> target.insertAfter(source::parse);
        default -> target.append(source::parse);
      };
      case "kind" -> switch (position) {
        case "as first into" -> target.prepend(kind, source.getName(), source.getValue());
        case "before" -> target.insertBefore(kind, source.getName(), source.getValue());
        case "after" -> target.insertAfter(kind, source.getName(), source.getValue());
        default -> target.append(kind, source.getName(), source.getValue());
      };
      default -> throw new IllegalArgumentException();
    };
    if (representation.equals("kind") && kind == Kind.ELEMENT) {
      xml = "<a/>";
    }
    String children = switch (position) {
      case "as first into" -> xml + "<left/><anchor/><right/>";
      case "before" -> "<left/>" + xml + "<anchor/><right/>";
      case "after" -> "<left/><anchor/>" + xml + "<right/>";
      default -> "<left/><anchor/><right/>" + xml;
    };

    assertTrue(inserted.isChildOf(document.getFirstChild()));
    assertDocument("<r>" + children + "</r>", document);
  }

  private static void assertDocument(String expectedXml, Node<?> document) {
    var ctx = new BrackitQueryContext();
    Node<?> expected = ctx.getNodeFactory().build(new DocumentParser(expectedXml));
    ResultChecker.dCheck(expected, document, false);
    assertForwardTraversal(document);
  }

  private static void assertForwardTraversal(Node<?> parent) {
    assertInstanceOf(D2Node.class, parent);
    Node<?> previous = null;
    try (var children = parent.getChildren()) {
      Node<?> child;
      while ((child = children.next()) != null) {
        assertTrue(child.isChildOf(parent));
        assertEquals(previous, child.getPreviousSibling());
        if (previous == null) {
          assertEquals(child, parent.getFirstChild());
        } else {
          assertEquals(child, previous.getNextSibling());
          assertTrue(previous.cmp(child) < 0);
        }
        assertForwardTraversal(child);
        previous = child;
      }
    }
    assertEquals(previous, parent.getLastChild());
    if (previous != null) {
      assertNull(previous.getNextSibling());
    }
  }

  private static List<String> attributeNames(Node<?> node) {
    List<String> names = new ArrayList<>();
    try (var attributes = node.getAttributes()) {
      Node<?> attribute;
      while ((attribute = attributes.next()) != null) {
        names.add(attribute.getName().stringValue());
      }
    }
    return names;
  }

  private static Stream<Arguments> jsonInsertions() {
    return Stream.of(0, 1, 3, -1)
                 .flatMap(position -> Stream.of(Arguments.of(position, "(1, 2, 3)", List.of("1", "2", "3")),
                                                Arguments.of(position, "1", List.of("1"))));
  }

  @ParameterizedTest(name = "JSON position {0}: {1}")
  @MethodSource("jsonInsertions")
  public void jsonArraySourceKeepsItsOrder(int position, String source, List<String> expectedSource) {
    var ctx = new BrackitQueryContext();
    Array target = assertInstanceOf(Array.class, new Query("[10, 20, 30]").execute(ctx));
    ctx.setContextItem(target);
    String update = position == -1
        ? "append json " + source + " into $$"
        : "insert json " + source + " into $$ at position " + position;

    new Query(update).execute(ctx);

    int insertionIndex = position == -1 ? 3 : position;
    assertEquals(4, target.len());
    for (int index = 0; index < 3; index++) {
      int resultIndex = index < insertionIndex ? index : index + 1;
      ResultChecker.dCheck(new Query(Integer.toString((index + 1) * 10)).execute(ctx), target.at(resultIndex), false);
    }
    // Brackit inserts a multi-item source as one array member. Check its internal order as well.
    Sequence inserted = target.at(insertionIndex);
    if (expectedSource.size() > 1) {
      Array values = assertInstanceOf(Array.class, inserted);
      assertEquals(expectedSource.size(), values.len());
      for (int index = 0; index < expectedSource.size(); index++) {
        ResultChecker.dCheck(new Query(expectedSource.get(index)).execute(ctx), values.at(index), false);
      }
    } else {
      ResultChecker.dCheck(new Query(expectedSource.getFirst()).execute(ctx), inserted, false);
    }
  }
}
