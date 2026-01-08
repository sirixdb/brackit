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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
    assertEquals("Only DELETE should be applied", List.of("DELETE"), appliedOps);
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
    assertEquals("Only DELETE should be applied", List.of("DELETE"), appliedOps);
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
    assertEquals("Both operations should be applied", 2, appliedOps.size());
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
}
