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

import io.brackit.query.jdm.StructuredItem;
import io.brackit.query.jsonitem.object.ArrayObject;
import io.brackit.query.atomic.QNm;
import io.brackit.query.atomic.Str;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.update.op.OpType;
import io.brackit.query.update.op.UpdateOp;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link UpdateList}.
 *
 * @author Johannes Lichtenberger
 */
public class UpdateListTest {

  /**
   * Per XQuery Update Facility 1.0 section 3.2.1:
   * "If a node is marked for deletion, updates to its properties have no effect."
   *
   * When both REPLACE_VALUE and DELETE operations target the same item,
   * the REPLACE_VALUE should be skipped.
   */
  @Test
  public void updateThenDeleteSameNodeSkipsUpdate() {
    final List<String> appliedOps = new ArrayList<>();
    // Create a simple JSON object as target
    final StructuredItem targetItem = new ArrayObject(new QNm[] { new QNm("name") },
                                                      new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add a REPLACE_VALUE operation
    updateList.append(new TestUpdateOp(OpType.REPLACE_VALUE, targetItem, () -> appliedOps.add("REPLACE")));

    // Add a DELETE operation for the same target
    updateList.append(new TestUpdateOp(OpType.DELETE, targetItem, () -> appliedOps.add("DELETE")));

    // Apply updates
    updateList.apply();

    // Only DELETE should be applied, REPLACE should be skipped
    assertEquals(List.of("DELETE"), appliedOps, "Only DELETE should be applied");
  }

  /**
   * Test that the order of operations doesn't matter - delete always takes precedence.
   */
  @Test
  public void deleteThenUpdateSameNodeSkipsUpdate() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem targetItem = new ArrayObject(new QNm[] { new QNm("name") },
                                                      new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add DELETE first
    updateList.append(new TestUpdateOp(OpType.DELETE, targetItem, () -> appliedOps.add("DELETE")));

    // Add REPLACE_VALUE for the same target
    updateList.append(new TestUpdateOp(OpType.REPLACE_VALUE, targetItem, () -> appliedOps.add("REPLACE")));

    // Apply updates
    updateList.apply();

    // Only DELETE should be applied, REPLACE should be skipped
    assertEquals(List.of("DELETE"), appliedOps, "Only DELETE should be applied");
  }

  /**
   * Test that updates to different targets are not affected.
   */
  @Test
  public void updateAndDeleteDifferentNodesAppliesBoth() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem target1 = new ArrayObject(new QNm[] { new QNm("id") }, new Sequence[] { new Str("1") });
    final StructuredItem target2 = new ArrayObject(new QNm[] { new QNm("id") }, new Sequence[] { new Str("2") });

    final UpdateList updateList = new UpdateList();

    // Add REPLACE_VALUE for target1
    updateList.append(new TestUpdateOp(OpType.REPLACE_VALUE, target1, () -> appliedOps.add("REPLACE-1")));

    // Add DELETE for target2 (different target)
    updateList.append(new TestUpdateOp(OpType.DELETE, target2, () -> appliedOps.add("DELETE-2")));

    // Apply updates
    updateList.apply();

    // Both should be applied since they target different items
    assertEquals(2, appliedOps.size(), "Both operations should be applied");
  }

  /**
   * Per XQuery Update Facility 1.0 section 3.2.2:
   * Insert operations are applied before delete operations.
   * An INSERT_BEFORE targeting a node that will be deleted should NOT be skipped.
   * The inserted content becomes a sibling and persists after the delete.
   */
  @Test
  public void insertBeforeDeletedNodeIsNotSkipped() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem targetItem = new ArrayObject(new QNm[] { new QNm("name") },
                                                      new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add an INSERT_BEFORE operation targeting a node
    updateList.append(new TestUpdateOp(OpType.INSERT_BEFORE, targetItem, () -> appliedOps.add("INSERT_BEFORE")));

    // Add a DELETE operation for the same target
    updateList.append(new TestUpdateOp(OpType.DELETE, targetItem, () -> appliedOps.add("DELETE")));

    // Apply updates
    updateList.apply();

    // Both should be applied - INSERT_BEFORE creates a sibling, then DELETE removes the target
    assertEquals(List.of("INSERT_BEFORE", "DELETE"), appliedOps, "Both INSERT_BEFORE and DELETE should be applied");
  }

  /**
   * INSERT_INTO targeting a deleted node should also be applied.
   */
  @Test
  public void insertIntoDeletedNodeIsNotSkipped() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem targetItem = new ArrayObject(new QNm[] { new QNm("name") },
                                                      new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add an INSERT_INTO operation
    updateList.append(new TestUpdateOp(OpType.INSERT_INTO, targetItem, () -> appliedOps.add("INSERT_INTO")));

    // Add a DELETE operation for the same target
    updateList.append(new TestUpdateOp(OpType.DELETE, targetItem, () -> appliedOps.add("DELETE")));

    // Apply updates
    updateList.apply();

    // Both should be applied
    assertEquals(List.of("INSERT_INTO", "DELETE"), appliedOps, "Both INSERT_INTO and DELETE should be applied");
  }

  /**
   * INSERT_AFTER targeting a deleted node should also be applied.
   */
  @Test
  public void insertAfterDeletedNodeIsNotSkipped() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem targetItem = new ArrayObject(new QNm[] { new QNm("name") },
                                                      new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add an INSERT_AFTER operation
    updateList.append(new TestUpdateOp(OpType.INSERT_AFTER, targetItem, () -> appliedOps.add("INSERT_AFTER")));

    // Add a DELETE operation for the same target
    updateList.append(new TestUpdateOp(OpType.DELETE, targetItem, () -> appliedOps.add("DELETE")));

    // Apply updates
    updateList.apply();

    // Both should be applied
    assertEquals(List.of("INSERT_AFTER", "DELETE"), appliedOps, "Both INSERT_AFTER and DELETE should be applied");
  }

  /**
   * Test that operations on different fields of the same parent use getTargetIdentity()
   * to correctly differentiate targets. This is crucial for path-based operations like
   * $doc.field1 and $doc.field2 which share the same parent target but differ by field.
   */
  @Test
  public void replaceAndDeleteDifferentFieldsSameParentAppliesBoth() {
    final List<String> appliedOps = new ArrayList<>();
    // Same parent target
    final StructuredItem parentTarget = new ArrayObject(new QNm[] { new QNm("name") },
                                                        new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add REPLACE_VALUE for field "first" (uses custom targetIdentity)
    updateList.append(new FieldBasedTestUpdateOp(OpType.REPLACE_VALUE,
                                                 parentTarget,
                                                 "first",
                                                 () -> appliedOps.add("REPLACE-first")));

    // Add DELETE for field "second" (uses custom targetIdentity, same parent but different field)
    updateList.append(new FieldBasedTestUpdateOp(OpType.DELETE,
                                                 parentTarget,
                                                 "second",
                                                 () -> appliedOps.add("DELETE-second")));

    // Apply updates
    updateList.apply();

    // Both should be applied since they have different targetIdentity (different fields)
    assertEquals(List.of("REPLACE-first", "DELETE-second"),
                 appliedOps,
                 "Both operations should be applied for different fields");
  }

  /**
   * Test that operations on the SAME field of the same parent are correctly identified
   * as targeting the same node via getTargetIdentity().
   */
  @Test
  public void replaceAndDeleteSameFieldSameParentSkipsReplace() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem parentTarget = new ArrayObject(new QNm[] { new QNm("name") },
                                                        new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add REPLACE_VALUE for field "first"
    updateList.append(new FieldBasedTestUpdateOp(OpType.REPLACE_VALUE,
                                                 parentTarget,
                                                 "first",
                                                 () -> appliedOps.add("REPLACE-first")));

    // Add DELETE for the same field "first"
    updateList.append(new FieldBasedTestUpdateOp(OpType.DELETE,
                                                 parentTarget,
                                                 "first",
                                                 () -> appliedOps.add("DELETE-first")));

    // Apply updates
    updateList.apply();

    // Only DELETE should be applied, REPLACE should be skipped (same targetIdentity)
    assertEquals(List.of("DELETE-first"), appliedOps, "Only DELETE should be applied for same field");
  }

  /**
   * Test array index-based operations with different indices.
   */
  @Test
  public void replaceAndDeleteDifferentArrayIndicesAppliesBoth() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem parentArray = new ArrayObject(new QNm[] { new QNm("arr") },
                                                       new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add REPLACE_VALUE for index 0
    updateList.append(new IndexBasedTestUpdateOp(OpType.REPLACE_VALUE,
                                                 parentArray,
                                                 0,
                                                 () -> appliedOps.add("REPLACE-0")));

    // Add DELETE for index 1 (different index)
    updateList.append(new IndexBasedTestUpdateOp(OpType.DELETE, parentArray, 1, () -> appliedOps.add("DELETE-1")));

    // Apply updates
    updateList.apply();

    // Both should be applied since they have different indices
    assertEquals(List.of("REPLACE-0", "DELETE-1"),
                 appliedOps,
                 "Both operations should be applied for different indices");
  }

  /**
   * Test array index-based operations with same index.
   */
  @Test
  public void replaceAndDeleteSameArrayIndexSkipsReplace() {
    final List<String> appliedOps = new ArrayList<>();
    final StructuredItem parentArray = new ArrayObject(new QNm[] { new QNm("arr") },
                                                       new Sequence[] { new Str("test") });

    final UpdateList updateList = new UpdateList();

    // Add REPLACE_VALUE for index 0
    updateList.append(new IndexBasedTestUpdateOp(OpType.REPLACE_VALUE,
                                                 parentArray,
                                                 0,
                                                 () -> appliedOps.add("REPLACE-0")));

    // Add DELETE for same index 0
    updateList.append(new IndexBasedTestUpdateOp(OpType.DELETE, parentArray, 0, () -> appliedOps.add("DELETE-0")));

    // Apply updates
    updateList.apply();

    // Only DELETE should be applied, REPLACE should be skipped (same index)
    assertEquals(List.of("DELETE-0"), appliedOps, "Only DELETE should be applied for same index");
  }

  /**
   * Simple test implementation of UpdateOp for testing purposes.
   */
  private static class TestUpdateOp implements UpdateOp {
    private final OpType type;
    private final StructuredItem target;
    private final Runnable applyAction;

    TestUpdateOp(OpType type, StructuredItem target, Runnable applyAction) {
      this.type = type;
      this.target = target;
      this.applyAction = applyAction;
    }

    @Override
    public void apply() {
      applyAction.run();
    }

    @Override
    public StructuredItem getTarget() {
      return target;
    }

    @Override
    public OpType getType() {
      return type;
    }
  }

  /**
   * Test UpdateOp that simulates field-based operations (like $obj.field).
   * Uses a custom targetIdentity that combines target + field name.
   */
  private static class FieldBasedTestUpdateOp implements UpdateOp {
    private final OpType type;
    private final StructuredItem target;
    private final String field;
    private final Runnable applyAction;

    FieldBasedTestUpdateOp(OpType type, StructuredItem target, String field, Runnable applyAction) {
      this.type = type;
      this.target = target;
      this.field = field;
      this.applyAction = applyAction;
    }

    @Override
    public void apply() {
      applyAction.run();
    }

    @Override
    public StructuredItem getTarget() {
      return target;
    }

    @Override
    public Object getTargetIdentity() {
      // Combine target identity + field name
      return new TargetFieldIdentity(target, field);
    }

    @Override
    public OpType getType() {
      return type;
    }

    private record TargetFieldIdentity(StructuredItem target, String field) {
      @Override
      public boolean equals(Object o) {
        if (this == o)
          return true;
        if (!(o instanceof TargetFieldIdentity that))
          return false;
        return target == that.target && java.util.Objects.equals(field, that.field);
      }

      @Override
      public int hashCode() {
        return System.identityHashCode(target) * 31 + java.util.Objects.hashCode(field);
      }
    }
  }

  /**
   * Test UpdateOp that simulates index-based operations (like $arr[idx]).
   * Uses a custom targetIdentity that combines target + index.
   */
  private static class IndexBasedTestUpdateOp implements UpdateOp {
    private final OpType type;
    private final StructuredItem target;
    private final int index;
    private final Runnable applyAction;

    IndexBasedTestUpdateOp(OpType type, StructuredItem target, int index, Runnable applyAction) {
      this.type = type;
      this.target = target;
      this.index = index;
      this.applyAction = applyAction;
    }

    @Override
    public void apply() {
      applyAction.run();
    }

    @Override
    public StructuredItem getTarget() {
      return target;
    }

    @Override
    public Object getTargetIdentity() {
      // Combine target identity + index
      return new TargetIndexIdentity(target, index);
    }

    @Override
    public OpType getType() {
      return type;
    }

    private record TargetIndexIdentity(StructuredItem target, int index) {
      @Override
      public boolean equals(Object o) {
        if (this == o)
          return true;
        if (!(o instanceof TargetIndexIdentity that))
          return false;
        return target == that.target && index == that.index;
      }

      @Override
      public int hashCode() {
        return System.identityHashCode(target) * 31 + index;
      }
    }
  }
}
