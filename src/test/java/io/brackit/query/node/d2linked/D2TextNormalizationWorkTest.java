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

import java.util.stream.Stream;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.Query;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Kind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class D2TextNormalizationWorkTest extends XQueryBaseTest {
  private static Stream<Arguments> textSequences() {
    return Stream.of(Arguments.of("(text {'a'}, text {'b'})", "ab"),
                     Arguments.of("(text {'a'}, text {'b'}, text {'c'}, text {'d'}, text {'e'})", "abcde"),
                     Arguments.of("(text {''}, text {'b'})", "b"),
                     Arguments.of("(text {'a'}, text {''})", "a"),
                     Arguments.of("(text {''}, text {''})", ""),
                     Arguments.of("text {''}", ""));
  }

  @ParameterizedTest(name = "late anchor normalization: {0}")
  @MethodSource("textSequences")
  public void lateAnchorNormalizationDoesNotSearchPrecedingSiblings(String source, String expected) {
    int small = pendingInsertionWork(16, source, expected);
    int large = pendingInsertionWork(4096, source, expected);
    assertEquals(0, small, "predecessor searches under the small parent");
    assertEquals(0, large, "predecessor searches under the large parent");
  }

  private int pendingInsertionWork(int size, String source, String expected) {
    CountingDocument document = parentWithPrecedingSiblings(size);
    D2Node anchor = document.append(Kind.ELEMENT, new QNm("anchor"), null);
    var context = new BrackitQueryContext();
    context.bind(new QNm("anchor"), anchor);
    document.predecessorSearches = 0;
    new Query("declare variable $anchor external; insert nodes " + source + " after $anchor").execute(context);
    int searches = document.predecessorSearches;
    D2Node node = assertPrecedingSiblings(document, size);
    assertSame(anchor, node);
    assertNull(anchor.getFirstChild());
    assertTextTail(anchor.getNextSibling(), expected);
    return searches;
  }

  @ParameterizedTest
  @ValueSource(strings = { "kind", "node", "parser" })
  public void eagerTwoSidedMergeDoesNotSearchForItsKnownPredecessor(String input) {
    int small = eagerMergeWork(16, input);
    int large = eagerMergeWork(4096, input);
    assertEquals(0, small, "predecessor searches under the small parent");
    assertEquals(0, large, "predecessor searches under the large parent");
  }

  private int eagerMergeWork(int size, String input) {
    CountingDocument document = parentWithPrecedingSiblings(size);
    D2Node left = document.append(Kind.TEXT, null, new Str("L"));
    D2Node separator = document.append(Kind.COMMENT, null, new Str("separator"));
    D2Node right = document.append(Kind.TEXT, null, new Str("R"));
    separator.delete();
    D2Node source = new D2NodeFactory().text(new Str("X"));
    document.predecessorSearches = 0;
    D2Node merged = switch (input) {
      case "kind" -> left.insertAfter(Kind.TEXT, null, source.getValue());
      case "node" -> left.insertAfter(source);
      case "parser" -> left.insertAfter(source::parse);
      default -> throw new IllegalArgumentException();
    };
    int searches = document.predecessorSearches;
    assertSame(left, merged);
    assertSame(left, assertPrecedingSiblings(document, size));
    assertTextTail(left, "LXR");
    assertEquals("X", source.getValue().stringValue());
    assertEquals("R", right.getValue().stringValue());
    assertNull(right.getParent());
    assertNull(right.getPreviousSibling());
    assertNull(right.getNextSibling());
    return searches;
  }

  private static CountingDocument parentWithPrecedingSiblings(int size) {
    CountingDocument document = new CountingDocument();
    for (int i = 0; i < size; i++) {
      document.append(Kind.COMMENT, null, new Str("sibling" + i));
    }
    return document;
  }

  private static D2Node assertPrecedingSiblings(CountingDocument document, int size) {
    D2Node node = document.getFirstChild();
    for (int i = 0; i < size; i++) {
      assertNotNull(node);
      assertEquals(Kind.COMMENT, node.getKind());
      assertEquals("sibling" + i, node.getValue().stringValue());
      assertSame(document, node.getParent());
      node = node.getNextSibling();
    }
    return node;
  }

  private static void assertTextTail(D2Node text, String expected) {
    if (expected.isEmpty()) {
      assertNull(text);
    } else {
      assertNotNull(text);
      assertEquals(Kind.TEXT, text.getKind());
      assertEquals(expected, text.getValue().stringValue());
      assertNull(text.getNextSibling());
    }
  }

  private static class CountingDocument extends DocumentD2Node {
    int predecessorSearches;

    @Override
    D2Node previousSiblingOf(D2Node node) {
      predecessorSearches++;
      return super.previousSiblingOf(node);
    }
  }
}
