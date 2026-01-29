/*
 * [New BSD License]
 * Copyright (c) 2011-2022, Brackit Project Team <info@brackit.org>
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
package io.brackit.query.function.fn;

import io.brackit.query.Query;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.jdm.Sequence;
import org.junit.jupiter.api.Test;

/**
 * Tests for fn:deep-equal with JSON items (arrays and objects).
 */
public class DeepEqualTest extends XQueryBaseTest {

  // ==================== Array Tests ====================

  @Test
  public void deepEqualEmptyArrays() {
    Sequence result = new Query("deep-equal([], [])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualIdenticalSimpleArrays() {
    Sequence result = new Query("deep-equal([1, 2, 3], [1, 2, 3])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualDifferentArrayValues() {
    Sequence result = new Query("deep-equal([1, 2, 3], [1, 2, 4])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualDifferentArrayLengths() {
    Sequence result = new Query("deep-equal([1, 2, 3], [1, 2])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualDifferentArrayLengthsReversed() {
    Sequence result = new Query("deep-equal([1, 2], [1, 2, 3])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualArraysWithStrings() {
    Sequence result = new Query("deep-equal(['a', 'b', 'c'], ['a', 'b', 'c'])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualArraysWithMixedTypes() {
    Sequence result = new Query("deep-equal([1, 'two', 3.0], [1, 'two', 3.0])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualArraysWithNull() {
    Sequence result = new Query("deep-equal([1, null, 3], [1, null, 3])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualArraysWithBooleans() {
    Sequence result = new Query("deep-equal([true, false], [true, false])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualNestedArrays() {
    Sequence result = new Query("deep-equal([[1, 2], [3, 4]], [[1, 2], [3, 4]])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualNestedArraysDifferent() {
    Sequence result = new Query("deep-equal([[1, 2], [3, 4]], [[1, 2], [3, 5]])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualDeeplyNestedArrays() {
    Sequence result = new Query("deep-equal([[[1]]], [[[1]]])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  // ==================== Object Tests ====================

  @Test
  public void deepEqualEmptyObjects() {
    Sequence result = new Query("deep-equal({}, {})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualIdenticalSimpleObjects() {
    Sequence result = new Query("deep-equal({\"a\": 1, \"b\": 2}, {\"a\": 1, \"b\": 2})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualObjectsDifferentKeyOrder() {
    Sequence result = new Query("deep-equal({\"a\": 1, \"b\": 2}, {\"b\": 2, \"a\": 1})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualObjectsDifferentValues() {
    Sequence result = new Query("deep-equal({\"a\": 1, \"b\": 2}, {\"a\": 1, \"b\": 3})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualObjectsDifferentKeys() {
    Sequence result = new Query("deep-equal({\"a\": 1, \"b\": 2}, {\"a\": 1, \"c\": 2})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualObjectsDifferentNumberOfKeys() {
    Sequence result = new Query("deep-equal({\"a\": 1, \"b\": 2}, {\"a\": 1})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualObjectsWithStrings() {
    Sequence result = new Query("deep-equal({\"key\": \"value\"}, {\"key\": \"value\"})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualObjectsWithNull() {
    Sequence result = new Query("deep-equal({\"a\": null}, {\"a\": null})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualNestedObjects() {
    Sequence result = new Query("deep-equal({\"outer\": {\"inner\": 1}}, {\"outer\": {\"inner\": 1}})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualNestedObjectsDifferent() {
    Sequence result = new Query("deep-equal({\"outer\": {\"inner\": 1}}, {\"outer\": {\"inner\": 2}})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  // ==================== Mixed Array/Object Tests ====================

  @Test
  public void deepEqualArrayWithObjects() {
    Sequence result = new Query("deep-equal([{\"a\": 1}, {\"b\": 2}], [{\"a\": 1}, {\"b\": 2}])").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualObjectWithArrays() {
    Sequence result = new Query("deep-equal({\"arr\": [1, 2, 3]}, {\"arr\": [1, 2, 3]})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualObjectWithArraysDifferent() {
    Sequence result = new Query("deep-equal({\"arr\": [1, 2, 3]}, {\"arr\": [1, 2, 4]})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualComplexStructure() {
    Sequence result = new Query(
                                "deep-equal({\"users\": [{\"name\": \"Alice\", \"age\": 30}, {\"name\": \"Bob\", \"age\": 25}]}, "
                                    + "{\"users\": [{\"name\": \"Alice\", \"age\": 30}, {\"name\": \"Bob\", \"age\": 25}]})").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualComplexStructureDifferent() {
    Sequence result = new Query("deep-equal({\"users\": [{\"name\": \"Alice\", \"age\": 30}]}, "
        + "{\"users\": [{\"name\": \"Alice\", \"age\": 31}]})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  // ==================== Type Mismatch Tests ====================

  @Test
  public void deepEqualArrayVsObject() {
    Sequence result = new Query("deep-equal([1, 2], {\"a\": 1})").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualObjectVsArray() {
    Sequence result = new Query("deep-equal({\"a\": 1}, [1, 2])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualArrayVsAtomic() {
    // An array is a distinct item type and should not equal an atomic
    Sequence result = new Query("deep-equal([1], 1)").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualObjectVsAtomic() {
    Sequence result = new Query("deep-equal({\"a\": 1}, 1)").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  // ==================== Empty Sequence Tests ====================

  @Test
  public void deepEqualEmptySequences() {
    Sequence result = new Query("deep-equal((), ())").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualEmptyVsNonEmpty() {
    Sequence result = new Query("deep-equal((), [1])").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  // ==================== Atomic Value Tests (for completeness) ====================

  @Test
  public void deepEqualAtomicIntegers() {
    Sequence result = new Query("deep-equal(42, 42)").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualAtomicStrings() {
    Sequence result = new Query("deep-equal('hello', 'hello')").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualAtomicDifferent() {
    Sequence result = new Query("deep-equal(42, 43)").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  // ==================== Sequence Tests ====================

  @Test
  public void deepEqualSequences() {
    Sequence result = new Query("deep-equal((1, 2, 3), (1, 2, 3))").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualSequencesDifferent() {
    Sequence result = new Query("deep-equal((1, 2, 3), (1, 2, 4))").execute(ctx);
    ResultChecker.dCheck(Bool.FALSE, result);
  }

  @Test
  public void deepEqualSequencesOfArrays() {
    Sequence result = new Query("deep-equal(([1], [2]), ([1], [2]))").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }

  @Test
  public void deepEqualSequencesOfObjects() {
    Sequence result = new Query("deep-equal(({\"a\": 1}, {\"b\": 2}), ({\"a\": 1}, {\"b\": 2}))").execute(ctx);
    ResultChecker.dCheck(Bool.TRUE, result);
  }
}
