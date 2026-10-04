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
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.parser.DocumentParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class InsertSequenceOrderTest {
  private static final String DOCUMENT = "<r><left/><anchor/><right/></r>";

  private static Stream<Arguments> xmlInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "elements", "(<a/>, <b/>, <c/>)", "<a/><b/><c/>", "" }, { "text",
        "(text {'a'}, text {'b'}, text {'c'})", "abc", "" }, { "mixed", "(<a/>, text {'b'}, <c/>, text {'d'})",
            "<a/>b<c/>d", "" }, { "attributes and children",
                "(attribute x {'1'}, attribute y {'2'}, <a/>, text {'b'}, <c/>)", "<a/>b<c/>", " x='1' y='2'" }, {
                    "attributes", "(attribute x {'1'}, attribute y {'2'}, attribute z {'3'})", "",
                    " x='1' y='2' z='3'" }, { "single element", "<a/>", "<a/>", "" }, { "single text", "text {'a'}",
                        "a", "" }, { "single attribute", "attribute x {'1'}", "", " x='1'" } };
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
    // Attribute application keeps its encounter order independently of child insertion.
    assertEquals(attributeNames(expected.getFirstChild()), attributeNames(document.getFirstChild()));
  }

  private static Stream<Arguments> textBoundaryInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "(<a/>, <b/>, <c/>)", "<a/><b/><c/>" }, { "(text {'a'}, text {'b'}, text {'c'})", "abc" },
        { "(text {'a'}, <b/>, text {'c'}, <d/>, text {'e'})", "a<b/>c<d/>e" }, { "<a/>", "<a/>" }, { "text {'a'}",
            "a" } };
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
  }

  private static Stream<Arguments> textAnchorInsertions() {
    List<Arguments> cases = new ArrayList<>();
    String[][] content = { { "(<a/>, <b/>, <c/>)", "<a/><b/><c/>" }, { "(text {'a'}, text {'b'}, text {'c'})", "abc" },
        { "(text {'a'}, <b/>, text {'c'}, <d/>, text {'e'})", "a<b/>c<d/>e" }, { "<a/>", "<a/>" }, { "text {'a'}",
            "a" } };
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
