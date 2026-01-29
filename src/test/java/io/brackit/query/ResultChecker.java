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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.brackit.query.atomic.Atomic;
import io.brackit.query.atomic.Int32;
import io.brackit.query.jdm.json.Array;
import io.brackit.query.jdm.json.Object;
import io.brackit.query.jdm.node.Node;
import io.brackit.query.operator.Cursor;
import io.brackit.query.operator.TupleImpl;
import io.brackit.query.jdm.DocumentException;
import io.brackit.query.jdm.Item;
import io.brackit.query.jdm.Iter;
import io.brackit.query.jdm.Kind;
import io.brackit.query.jdm.OperationNotSupportedException;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jdm.Stream;
import org.junit.jupiter.api.Assertions;

/**
 * @author Sebastian Baechle
 */
public class ResultChecker {
  public static void dCheckS(QueryContext ctx, Cursor op, Sequence... expected) throws QueryException {
    // double check result to verify that plan can be evaluated repeatedly
    checkS(ctx, op, expected);
    checkS(ctx, op, expected);
  }

  public static void checkS(QueryContext ctx, Cursor op, Sequence... expected) throws QueryException {
    Tuple[] e = new Tuple[expected.length];
    for (int i = 0; i < expected.length; i++) {
      e[i] = new TupleImpl(expected[i]);
    }
    checkT(ctx, op, e);
  }

  public static void dCheckT(QueryContext ctx, Cursor op, Tuple... expected) throws QueryException {
    // double check result to verify that plan can be evaluated repeatedly
    checkT(ctx, op, expected);
    checkT(ctx, op, expected);
  }

  public static void checkT(QueryContext ctx, Cursor op, Tuple... expected) throws QueryException {
    Tuple next;
    op.open(ctx);
    for (int i = 0; i < expected.length; i++) {
      assertNotNull(next = op.next(ctx), "Result is empty");
      checkTuple(ctx, expected[i], next);
    }
    assertNull(op.next(ctx), "No more results delivered");
    op.close(ctx);
  }

  public static void checkTuple(QueryContext ctx, Tuple expected, Tuple result) throws QueryException {
    int eSize = expected.getSize();
    int rSize = result.getSize();
    assertEquals(eSize, rSize, "Result tuple has same size");
    for (int i = 0; i < eSize; i++) {
      Sequence eSequence = expected.get(i);
      Sequence rSequence = result.get(i);
      check(eSequence, rSequence);
    }
  }

  public static void dCheck(Sequence expected, Sequence result) throws QueryException {
    // double check result to verify that result can be evaluated repeatedly
    check(expected, result, true);
    check(expected, result, true);
  }

  public static void dCheck(Sequence expected, Sequence result, boolean nodeIdentity) throws QueryException {
    // double check result to verify that result can be evaluated repeatedly
    check(expected, result, nodeIdentity);
    check(expected, result, nodeIdentity);
  }

  public static void check(Sequence expected, Sequence result) throws QueryException {
    compare(expected, result, true);
  }

  public static void check(Sequence expected, Sequence result, boolean nodeIdentity) throws QueryException {
    compare(expected, result, nodeIdentity);
  }

  private static void compare(Sequence expected, Sequence result, boolean nodeIdentity) throws QueryException,
      AssertionError {
    if (expected == null) {
      if (result != null) // verify that result sequence has no results
      {
        Iter s = result.iterate();
        try {
          assertNull(s.next(), "Result sequence is empty");
        } finally {
          s.close();
        }
        assertFalse(result.booleanValue(), "Result has boolean value of empty sequence");
        assertTrue(Int32.ZERO.cmp(result.size()) == 0, "Result has size of empty sequence");
      }
    } else {
      assertNotNull(result, "Result sequence is not empty");
      Iter es = expected.iterate();
      try {
        Iter rs = result.iterate();
        try {
          Item eItem;
          Item rItem;
          while ((eItem = es.next()) != null) {
            assertNotNull(rItem = rs.next(), "Result sequence has more results");
            try {
              assertEquals(eItem.itemType(), rItem.itemType(), "Result item has same type");

              if (eItem instanceof Node<?>) {
                compareNode(eItem, rItem, nodeIdentity);
              } else if (eItem instanceof Object) {
                compareRecord(eItem, rItem);
              } else if (eItem instanceof Array) {
                compareArray(eItem, rItem);
              } else {
                compareAtomic(eItem, rItem);
              }
            } catch (AssertionError e) {
              System.err.println(String.format("Expected: '%s'\t Result: '%s'", eItem, rItem));
              throw e;
            } catch (QueryException e) {
              if (e.getCode() == ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE) {
                fail(String.format("Wrong item (type) in result. Expected: '%s'\t Result: '%s'", eItem, rItem));
              }
              throw e;
            }
          }
          assertNull(rs.next(), "Result sequence has not more results than expected");
        } finally {
          rs.close();
        }
      } finally {
        es.close();
      }

      try {
        boolean expectedBooleanValue = expected.booleanValue();
        boolean resultBooleanValue = false;

        try {
          resultBooleanValue = result.booleanValue();
        } catch (QueryException e) {
          fail("Result does not have defined boolean value");
        }

        assertEquals(expectedBooleanValue, resultBooleanValue, "Result has expected boolean value");
      } catch (QueryException e) {
        assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e.getCode(), "Correct error code");

        try {
          result.booleanValue();
          fail("Result has defined boolean value");
        } catch (QueryException e1) {
          assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, e1.getCode(), "Correct error code");
        }
      }
    }
  }

  private static void compareArray(Item eItem, Item rItem) {
  }

  private static void compareAtomic(Item eItem, Item rItem) throws QueryException {
    assertTrue(rItem instanceof Atomic, "Result item is atomic");
    assertTrue(((Atomic) eItem).eq((Atomic) rItem), "Result atomic is equal to expected");
  }

  private static void compareRecord(Item eItem, Item rItem) throws DocumentException {
    assertTrue(rItem instanceof Object, "Result item is record");
    Object eNode = (Object) eItem;
    Object rNode = (Object) rItem;

    Assertions.assertEquals(eNode.size(), rNode.size());

    // TODO
  }

  private static void compareNode(Item eItem, Item rItem, boolean nodeIdentity) throws DocumentException {
    assertTrue(rItem instanceof Node<?>, "Result item is node");
    Node<?> eNode = (Node<?>) eItem;
    Node<?> rNode = (Node<?>) rItem;
    if (nodeIdentity) {
      assertTrue(eNode.isSelfOf(rNode), "Result node is equal to expected");
    } else {
      compareNode(eNode, rNode);
    }
  }

  private static void compareNode(Node<?> eNode, Node<?> rNode) throws DocumentException,
      OperationNotSupportedException {
    Assertions.assertEquals(eNode.getKind(), rNode.getKind(), "Node kind is correct");
    if (eNode.getKind() == Kind.DOCUMENT) {
      compareChildren(eNode, rNode);
    } else {
      Assertions.assertEquals(eNode.getName(), rNode.getName(), "Node name is correct");
      if (eNode.getKind() == Kind.ELEMENT) {
        compareAttributes(eNode, rNode);
        compareChildren(eNode, rNode);
      } else {
        Assertions.assertEquals(eNode.getValue(), rNode.getValue(), "Node value correct");
      }
    }
  }

  private static void compareChildren(Node<?> eNode, Node<?> rNode) throws DocumentException {
    Stream<? extends Node<?>> eChildren = eNode.getChildren();
    try {
      Stream<? extends Node<?>> rChildren = rNode.getChildren();
      try {
        Node<?> eChild;
        Node<?> rChild;
        while ((eChild = eChildren.next()) != null) {
          assertNotNull(rChild = rChildren.next(), "Child is in result");
          compareNode(eChild, rChild);
        }
        assertNull(rChildren.next(), "Result has no further children");
      } finally {
        rChildren.close();
      }
    } finally {
      eChildren.close();
    }
  }

  private static void compareAttributes(Node<?> eNode, Node<?> rNode) throws DocumentException {
    Stream<? extends Node<?>> eAtts = eNode.getAttributes();
    try {
      Stream<? extends Node<?>> rAtts = rNode.getAttributes();
      try {
        Node<?> eAtt;
        Node<?> rAtt;
        while ((eAtt = eAtts.next()) != null) {
          assertNotNull(rAtt = rAtts.next(), "Attribute is in result");
          Assertions.assertEquals(Kind.ATTRIBUTE, rAtt.getKind(), "Node kind is correct");
          Assertions.assertEquals(eAtt.getName(), rAtt.getName(), "Node name is correct");
          Assertions.assertEquals(eAtt.getValue(), rAtt.getValue(), "Node name value correct");
        }
        assertNull(rAtts.next(), "Result has no further attributes");
      } finally {
        rAtts.close();
      }
    } finally {
      eAtts.close();
    }
  }
}
