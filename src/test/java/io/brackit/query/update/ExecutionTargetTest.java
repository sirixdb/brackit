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
package io.brackit.query.update;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import io.brackit.query.ResultChecker;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.node.d2linked.D2NodeFactory;
import io.brackit.query.node.parser.DocumentParser;
import io.brackit.query.update.op.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionTargetTest {
  private static final D2NodeFactory FACTORY = new D2NodeFactory();

  private static Stream<Arguments> operations() {
    return Stream.of(Arguments.of("into",
                                  "<r><target><old/></target></r>",
                                  "<r><target><old/><one/><two/></target></r>"),
                     Arguments.of("first",
                                  "<r><target><old/></target></r>",
                                  "<r><target><one/><two/><old/></target></r>"),
                     Arguments.of("last",
                                  "<r><target><old/></target></r>",
                                  "<r><target><old/><one/><two/></target></r>"),
                     Arguments.of("before", "<r><target/></r>", "<r><one/><two/><target/></r>"),
                     Arguments.of("after", "<r><target/></r>", "<r><target/><one/><two/></r>"),
                     Arguments.of("before-text", "<r>old</r>", "<r><one/><two/>old</r>"),
                     Arguments.of("after-text", "<r>old</r>", "<r>old<one/><two/></r>"),
                     Arguments.of("attributes", "<r><target/></r>", "<r><target one='1' two='2'/></r>"),
                     Arguments.of("delete", "<r><target/></r>", "<r/>"),
                     Arguments.of("rename", "<r><target/></r>", "<r><renamed/></r>"),
                     Arguments.of("value", "<r>old</r>", "<r>new</r>"),
                     Arguments.of("replace", "<r><target/></r>", "<r><one/><two/></r>"),
                     Arguments.of("replace-text", "<r>old</r>", "<r><one/><two/></r>"),
                     Arguments.of("replace-text-next", "<r>old<next/></r>", "<r><one/><two/><next/></r>"),
                     Arguments.of("replace-text-previous", "<r><previous/>old</r>", "<r><previous/><one/><two/></r>"),
                     Arguments.of("replace-attribute", "<r target='old'/>", "<r one='1' two='2'/>"),
                     Arguments.of("content",
                                  "<r><target>old<a/><!--comment--><?pi value?><b/>tail</target></r>",
                                  "<r><target>new</target></r>"),
                     Arguments.of("empty-content", "<r><target>old<a/><b/>tail</target></r>", "<r><target/></r>"),
                     Arguments.of("null-content", "<r><target>old<a/><b/>tail</target></r>", "<r><target/></r>"));
  }

  @ParameterizedTest(name = "{0} uses the execution target")
  @MethodSource("operations")
  public void applyUsesExecutionTarget(String kind, String input, String expected) {
    // Independent D2 trees model read and writer views of the same logical path.
    Node<?> originalDocument = document(input);
    Node<?> executionDocument = document(input);
    Node<?> originalNode = target(originalDocument, kind);
    Node<?> executionTarget = target(executionDocument, kind);
    boolean[] forbidOriginalAccess = { false };
    Node<?> originalTarget = (Node<?>) Proxy.newProxyInstance(Node.class.getClassLoader(),
                                                              new Class<?>[] { Node.class },
                                                              (receiver, method, arguments) -> {
                                                                if (forbidOriginalAccess[0])
                                                                  throw new AssertionError("Original target accessed: "
                                                                      + method.getName());
                                                                return method.invoke(originalNode, arguments);
                                                              });
    UpdateOp operation = operation(kind, originalTarget);
    assertNotSame(originalTarget, executionTarget);
    assertSame(originalTarget, operation.getTarget());
    assertSame(originalTarget, operation.getTargetIdentity());
    forbidOriginalAccess[0] = true;
    operation.apply(executionTarget);
    assertSame(originalTarget, operation.getTarget());
    assertSame(originalTarget, operation.getTargetIdentity());
    ResultChecker.dCheck(document(expected), executionDocument, false);
    // Reuse the pending operation for another writer without redirecting its target.
    Node<?> anotherExecutionDocument = document(input);
    operation.apply(target(anotherExecutionDocument, kind));
    assertSame(originalTarget, operation.getTarget());
    assertSame(originalTarget, operation.getTargetIdentity());
    ResultChecker.dCheck(document(expected), anotherExecutionDocument, false);
    ResultChecker.dCheck(document(input), originalDocument, false);
  }

  @ParameterizedTest(name = "{0} retains no-argument apply behavior")
  @MethodSource("operations")
  public void applyUsesCapturedTarget(String kind, String input, String expected) {
    Node<?> document = document(input);
    Node<?> target = target(document, kind);
    UpdateOp operation = operation(kind, target);
    operation.apply();
    assertSame(target, operation.getTarget());
    ResultChecker.dCheck(document(expected), document, false);
  }

  private static Node<?> target(Node<?> document, String kind) {
    Node<?> root = document.getFirstChild();
    return switch (kind) {
      case "replace-attribute" -> root.getAttribute(new QNm("target"));
      case "replace-text-previous" -> root.getLastChild();
      default -> root.getFirstChild();
    };
  }

  private static UpdateOp operation(String kind, Node<?> target) {
    UpdateOp operation = switch (kind) {
      case "into" -> new InsertIntoOp(target);
      case "first" -> new InsertIntoAsFirstOp(target);
      case "last" -> new InsertIntoAsLastOp(target);
      case "before", "before-text" -> new InsertBeforeOp(target);
      case "after", "after-text" -> new InsertAfterOp(target);
      case "attributes" -> new InsertAttributesOp(target);
      case "delete" -> new DeleteOp(target);
      case "rename" -> new RenameOp(target, new QNm("renamed"));
      case "value" -> new ReplaceValueOp(target, new Str("new"));
      case "content" -> new ReplaceElementContentOp(target, new Str("new"));
      case "empty-content" -> new ReplaceElementContentOp(target, new Str(""));
      case "null-content" -> new ReplaceElementContentOp(target, null);
      default -> new ReplaceNodeOp(target);
    };
    if (operation instanceof AbstractInsertOp insert) {
      boolean attributes = kind.equals("attributes") || kind.equals("replace-attribute");
      Node<?> first = attributes ? FACTORY.attribute(new QNm("one"), new Str("1")) : FACTORY.element(new QNm("one"));
      insert.addContent(first, FACTORY);
      insert.addContent(attributes ? FACTORY.attribute(new QNm("two"), new Str("2")) : FACTORY.element(new QNm("two")),
                        FACTORY);
      // The operation must retain its captured payload when the source changes.
      first.setName(new QNm("changed"));
    }
    return operation;
  }

  private static Node<?> document(String xml) {
    return FACTORY.build(new DocumentParser(xml));
  }
}
