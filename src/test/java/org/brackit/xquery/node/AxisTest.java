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
package org.brackit.xquery.node;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQueryBaseTest;
import org.brackit.xquery.expr.Accessor;
import org.brackit.xquery.node.stream.StreamUtil;
import org.brackit.xquery.node.stream.filter.Filter;
import org.brackit.xquery.node.stream.filter.FilteredStream;
import org.brackit.xquery.jdm.Axis;
import org.brackit.xquery.jdm.DocumentException;
import org.brackit.xquery.jdm.Stream;
import org.brackit.xquery.jdm.node.Node;
import org.brackit.xquery.jdm.node.NodeCollection;
import org.brackit.xquery.jdm.node.NodeStore;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Sebastian Baechle
 */
public abstract class AxisTest extends XQueryBaseTest {

  private static final Comparator<Node<?>> COMPARATOR = Node::cmp;

  private NodeCollection<?> collection;

  private static class AxisFilter implements Filter<Node<?>> {
    private final Node<?> node;
    private final Axis axis;

    public AxisFilter(Node<?> node, Axis axis) {
      this.node = node;
      this.axis = axis;
    }

    @Override
    public boolean filter(Node<?> element) throws DocumentException {
      try {
        boolean check = !axis.check(element, node);
        /*
         * if (check) { System.err.println("Filter out " + element + " -> !" +
         * axis + " of " + node); } else { System.out.println("Accept " +
         * element + " -> " + axis + " of " + node); }
         */
        return check;
      } catch (QueryException e) {
        throw new DocumentException(e);
      }
    }
  }

  @Test
  public void testCmp() {
    final Stream<? extends Node<?>> subtree = collection.getDocument().getSubtree();
    final List<? extends Node<?>> nodes = StreamUtil.asList(subtree);
    for (int i = 0; i < nodes.size(); i++) {
      final Node<?> a = nodes.get(i);
      for (int j = 0; j < nodes.size(); j++) {
        final Node<?> b = nodes.get(j);
        try {
          if (i < j)
            Assert.assertTrue("a < b", a.cmp(b) < 0);
          else if (i == j)
            Assert.assertEquals("a == b", 0, a.cmp(b));
          else
            Assert.assertTrue("a > b", a.cmp(b) > 0);
        } catch (AssertionError e) {
          // SubtreePrinter.print(collection.getDocument(), System.out);
          // System.err.println(nodes);
          System.err.println(a);
          System.err.println(b);
          System.err.println(a.cmp(b));
          throw e;
        }
      }
    }
  }

  @Test
  public void testRootElementChildren() {
    Node<?> node = collection.getDocument().getFirstChild();
    Set<Node<?>> expected = buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.CHILD));
    checkOutput(Accessor.CHILD.performStep(node), expected);
  }

  @Test
  public void testNonRootElementChildren() {
    Node<?> node = collection.getDocument().getFirstChild().getFirstChild();
    Set<Node<?>> expected = buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.CHILD));
    checkOutput(Accessor.CHILD.performStep(node), expected);
  }

  @Test
  public void testRootFollowing() {
    Node<?> node = collection.getDocument().getFirstChild();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.FOLLOWING));
    checkOutput(Accessor.FOLLOWING.performStep(node), expected);
  }

  @Test
  public void testNonRootFollowing() {
    Node<?> node = collection.getDocument().getFirstChild().getFirstChild().getFirstChild().getNextSibling();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.FOLLOWING));
    checkOutput(Accessor.FOLLOWING.performStep(node), expected);
  }

  @Test
  public void testRootPreceding() {
    Node<?> node = collection.getDocument().getFirstChild();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.PRECEDING));
    checkOutput(Accessor.PRECEDING.performStep(node), expected);
  }

  @Test
  public void testNonRootPreceding() {
    Node<?> node = collection.getDocument().getFirstChild().getFirstChild().getFirstChild().getNextSibling();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.PRECEDING));
    checkOutput(Accessor.PRECEDING.performStep(node), expected);
  }

  @Test
  public void testRootPrecedingSibling() {
    Node<?> node = collection.getDocument().getFirstChild();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.PRECEDING_SIBLING));
    checkOutput(Accessor.PRECEDING_SIBLING.performStep(node), expected);
  }

  @Test
  public void testNonRootPrecedingSibling() {
    Node<?> node = collection.getDocument().getFirstChild().getFirstChild().getFirstChild().getNextSibling();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.PRECEDING_SIBLING));
    checkOutput(Accessor.PRECEDING_SIBLING.performStep(node), expected);
  }

  @Test
  public void testRootFollowingSibling() {
    Node<?> node = collection.getDocument().getFirstChild();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.FOLLOWING_SIBLING));
    checkOutput(Accessor.FOLLOWING_SIBLING.performStep(node), expected);
  }

  @Test
  public void testNonRootFollowingSibling() {
    Node<?> node = collection.getDocument().getFirstChild().getFirstChild().getFirstChild().getNextSibling();
    Set<Node<?>> expected =
        buildExpectedSet(collection.getDocument().getSubtree(), new AxisFilter(node, Axis.FOLLOWING_SIBLING));
    checkOutput(Accessor.FOLLOWING_SIBLING.performStep(node), expected);
  }

  protected Set<Node<?>> buildExpectedSet(final Stream<? extends Node<?>> original, Filter<Node<?>> filter)
      throws DocumentException {
    Set<Node<?>> expected = new TreeSet<>(COMPARATOR);
    Stream<? extends Node<?>> stream = original;

    if (filter != null) {
      stream = new FilteredStream<Node<?>>(original, filter);
    }

    Node<?> next;
    while ((next = stream.next()) != null) {
      expected.add(next);
    }
    stream.close();
    return expected;
  }

  protected void checkOutput(Stream<? extends Node<?>> nodes, Set<Node<?>> expected) {
    Set<Node<?>> delivered = new TreeSet<>(COMPARATOR);
    Node<?> node;
    while ((node = nodes.next()) != null) {
      Assert.assertTrue("Node not delivered yet.", delivered.add(node));
      // System.out.println(node);
    }
    nodes.close();
    try {
      Assert.assertEquals("Expected number of nodes delivered", expected.size(), delivered.size());

      for (Node<?> n : delivered) {
        // System.err.println("CHECKING " + n);
        if (!expected.contains(n)) {
          // System.err.println(n + " is not contained in " +
          // expected);
          // System.err.println("Expected:\t" + expected);
          // System.err.println("Delivered:\t" + delivered);
          return;
        }
      }

      Assert.assertTrue("Expected nodes delivered", expected.containsAll(delivered));
    } catch (Error e) {
      // System.out.println("Expected:\t" + expected);
      // System.out.println("Delivered:\t" + delivered);
      throw e;
    }
  }

  @Override
  protected abstract NodeStore createStore() throws Exception;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    collection = storeFile("text.xml", RESOURCES.resolve("docs").resolve("orga.xml"));
  }

}