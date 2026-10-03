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
package io.brackit.query.jsonitem.array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.atomic.Int32;
import io.brackit.query.function.json.JSONFun;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.jdm.Iter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingArrayTest {

  private StreamingArray array(String json) {
    return array(json, 16);
  }

  private StreamingArray array(String json, int bufferSize) {
    var input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    return (StreamingArray) new StreamingJSONParser(input, bufferSize).parse();
  }

  private void assertRereadFails(Runnable read) {
    QueryException error = assertThrows(QueryException.class, read::run);
    assertRereadError(error);
  }

  private void assertRereadFails(Future<?> read) {
    ExecutionException error = assertThrows(ExecutionException.class, () -> read.get(10, TimeUnit.SECONDS));
    assertRereadError(assertInstanceOf(QueryException.class, error.getCause()));
  }

  private void assertRereadError(QueryException error) {
    assertEquals(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, error.getCode());
    assertTrue(error.getMessage().contains("StreamingArray cannot be reread"));
  }

  private void assertTerminalRead(Runnable read, RuntimeException firstFailure) {
    QueryException error = assertThrows(QueryException.class, read::run);
    assertEquals(ErrorCode.BIT_DYN_RT_ILLEGAL_STATE_ERROR, error.getCode());
    assertTrue(error.getMessage().contains("StreamingArray cannot be read after a parsing failure"));
    assertSame(firstFailure, error.getCause());
  }

  private void assertTerminalReads(StreamingArray array, RuntimeException firstFailure) {
    assertTerminalRead(array::iterate, firstFailure);
    assertTerminalRead(array::values, firstFailure);
    assertTerminalRead(array::length, firstFailure);
    assertTerminalRead(array::len, firstFailure);
    assertTerminalRead(() -> array.at(0), firstFailure);
    assertTerminalRead(() -> array.at(Int32.ZERO), firstFailure);
    assertTerminalRead(() -> array.at(2), firstFailure);
    assertTerminalRead(() -> array.range(Int32.ZERO, Int32.ONE), firstFailure);
  }

  private StreamingArray failingInputArray(boolean wrappedInObject) {
    var input = new InputStream() {
      private final byte[] bytes = (wrappedInObject ? "[]" : "[1,").getBytes(StandardCharsets.UTF_8);
      private int index;

      @Override
      public int read() throws IOException {
        if (index == bytes.length) {
          throw new IOException("input failed");
        }
        return bytes[index++];
      }
    };
    var parser = new StreamingJSONParser(input, 1);
    var array = (StreamingArray) parser.parse();
    return wrappedInObject ? new StreamingArray(parser, true) : array;
  }

  private static Stream<Arguments> iterationFailures() {
    return Stream.of(1, 16)
                 .flatMap(bufferSize -> Stream.of(false, true).map(ownsStream -> Arguments.of(bufferSize, ownsStream)));
  }

  private static Stream<Arguments> materializationFailures() {
    return Stream.of(1, 16)
                 .flatMap(bufferSize -> Stream.<Consumer<StreamingArray>>of(StreamingArray::values,
                                                                            StreamingArray::length,
                                                                            StreamingArray::len,
                                                                            array -> array.at(1),
                                                                            array -> array.at(Int32.ONE),
                                                                            array -> array.range(Int32.ZERO,
                                                                                                 new Int32(2)))
                                              .map(read -> Arguments.of(bufferSize, read)));
  }

  private static Stream<Consumer<StreamingArray>> completionReads() {
    return Stream.of(array -> {
      try (Iter iter = array.iterate()) {
        assertNull(iter.next());
      }
    }, StreamingArray::values, array -> array.at(0));
  }

  @ParameterizedTest
  @MethodSource("iterationFailures")
  void failedIterationMakesEveryReadTerminal(int bufferSize, boolean ownsStream) {
    StreamingArray array = array(ownsStream ? "[1,truX,2]" : "[truX,2]", bufferSize);
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      if (ownsStream) {
        assertEquals(Int32.ONE, first.next());
      }
      QueryException error = assertThrows(QueryException.class, first::next);
      assertEquals(JSONFun.ERR_PARSING_ERROR, error.getCode());
      assertTrue(error.getMessage().contains("Expected 'true'"));
      assertTerminalRead(first::next, error);
      assertTerminalRead(second::next, error);
      assertTerminalReads(array, error);
      assertTerminalRead(first::next, error);
    }
  }

  @ParameterizedTest
  @MethodSource("materializationFailures")
  void failedMaterializationMakesCachedPrefixAndSuffixUnreadable(int bufferSize, Consumer<StreamingArray> read) {
    StreamingArray array = array("[1,truX,2]", bufferSize);
    assertEquals(Int32.ONE, array.at(0));
    try (Iter iter = array.iterate()) {
      QueryException error = assertThrows(QueryException.class, () -> read.accept(array));
      assertEquals(JSONFun.ERR_PARSING_ERROR, error.getCode());
      assertTrue(error.getMessage().contains("Expected 'true'"));
      assertTerminalRead(iter::next, error);
      assertTerminalReads(array, error);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 16 })
  void numericParserFailureAlsoMakesEveryReadTerminal(int bufferSize) {
    StreamingArray array = array("[1e+,2]", bufferSize);
    try (Iter iter = array.iterate()) {
      NumberFormatException error = assertThrows(NumberFormatException.class, iter::next);
      assertTerminalRead(iter::next, error);
      assertTerminalReads(array, error);
    }
  }

  @Test
  void inputFailureMakesOwningIteratorTerminal() {
    StreamingArray array = failingInputArray(false);
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      QueryException error = assertThrows(QueryException.class, iter::next);
      assertEquals(JSONFun.ERR_PARSING_ERROR, error.getCode());
      assertTrue(error.getMessage().contains("I/O error: input failed"));
      assertTerminalRead(iter::next, error);
      assertTerminalReads(array, error);
    }
  }

  @ParameterizedTest
  @MethodSource("completionReads")
  void trailingObjectFailureCannotPublishSuccessfulCompletion(Consumer<StreamingArray> read) {
    StreamingArray array = failingInputArray(true);
    try (Iter iter = array.iterate()) {
      QueryException error = assertThrows(QueryException.class, () -> read.accept(array));
      assertEquals(JSONFun.ERR_PARSING_ERROR, error.getCode());
      assertTrue(error.getMessage().contains("I/O error: input failed"));
      assertTerminalRead(iter::next, error);
      assertTerminalReads(array, error);
    }
  }

  private static final class PausingInput extends ByteArrayInputStream {
    private final AtomicBoolean pauseNextRead = new AtomicBoolean();
    private final CountDownLatch paused = new CountDownLatch(1);
    private final CountDownLatch resume = new CountDownLatch(1);

    private PausingInput(String json) {
      super(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
      if (pauseNextRead.compareAndSet(true, false)) {
        paused.countDown();
        try {
          assertTrue(resume.await(10, TimeUnit.SECONDS), "input read was not resumed");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AssertionError(e);
        }
      }
      return super.read(bytes, offset, length);
    }
  }

  private <T> Future<T> overlapReads(PausingInput input, Runnable firstRead, Callable<T> secondRead) throws Exception {
    input.pauseNextRead.set(true);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(firstRead);
      Future<T> second;
      try {
        assertTrue(input.paused.await(10, TimeUnit.SECONDS), "first reader did not reach input");
        var started = new CountDownLatch(1);
        second = executor.submit(() -> {
          started.countDown();
          return secondRead.call();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS), "second reader did not start");
        assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
      } finally {
        input.resume.countDown();
      }
      first.get(10, TimeUnit.SECONDS);
      return second;
    }
  }

  private static Stream<Consumer<StreamingArray>> materializations() {
    return Stream.of(array -> assertEquals(List.of(Int32.ONE, new Int32(2), new Int32(3)), array.values()),
                     array -> assertEquals(new Int32(3), array.length()),
                     array -> assertEquals(3, array.len()),
                     array -> assertEquals(Int32.ONE, array.at(0)),
                     array -> assertEquals(Int32.ONE, array.at(Int32.ZERO)),
                     array -> assertEquals(Int32.ONE, array.range(Int32.ZERO, Int32.ONE).at(0)));
  }

  @Test
  void exhaustedStreamCannotBeReread() {
    StreamingArray array = array("[1,2,3,4,5]");
    try (Iter iter = array.iterate()) {
      for (int i = 1; i <= 5; i++) {
        assertEquals(new Int32(i), iter.next());
      }
      assertNull(iter.next());
      assertNull(iter.next());
    }
    assertRereadFails(array::iterate);
    assertRereadFails(array::values);
    assertRereadFails(array::length);
    assertRereadFails(array::len);
    assertRereadFails(() -> array.at(0));
    assertRereadFails(() -> array.range(Int32.ZERO, Int32.ONE));
  }

  @Test
  void partiallyConsumedStreamCannotBeReread() {
    StreamingArray array = array("[1,2,3,4,5]");
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
    }
    assertRereadFails(array::iterate);
    assertRereadFails(() -> array.at(Int32.ZERO));
    assertRereadFails(array::values);
    assertRereadFails(array::length);
    assertRereadFails(array::len);
    assertRereadFails(() -> array.range(Int32.ZERO, Int32.ONE));
  }

  @Test
  void iteratorsCreatedBeforeStreamingCannotShareParserCursor() {
    StreamingArray array = array("[1,2,3]");
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      assertEquals(Int32.ONE, first.next());
      assertRereadFails(second::next);
      assertEquals(new Int32(2), first.next());
      assertEquals(new Int32(3), first.next());
      assertNull(first.next());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void overlappingIteratorsCannotShareParserCursor(boolean cachedPrefix) throws Exception {
    var input = new PausingInput("[1,2,3]");
    var array = (StreamingArray) new StreamingJSONParser(input, cachedPrefix ? 3 : 1).parse();
    if (cachedPrefix) {
      assertEquals(Int32.ONE, array.at(0));
    }
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      if (cachedPrefix) {
        assertEquals(Int32.ONE, first.next());
        assertEquals(Int32.ONE, second.next());
      }
      var reread = overlapReads(input, () -> assertEquals(new Int32(cachedPrefix ? 2 : 1), first.next()), second::next);
      assertRereadFails(reread);
      if (!cachedPrefix) {
        assertEquals(new Int32(2), first.next());
      }
      assertEquals(new Int32(3), first.next());
      assertNull(first.next());
    }
  }

  @Test
  void iteratorAdmissionCannotRaceWithStreaming() throws Exception {
    var input = new PausingInput("[1,2,3]");
    var array = (StreamingArray) new StreamingJSONParser(input, 1).parse();
    try (Iter iter = array.iterate()) {
      var reread = overlapReads(input, () -> assertEquals(Int32.ONE, iter.next()), array::iterate);
      assertRereadFails(reread);
      assertEquals(new Int32(2), iter.next());
      assertEquals(new Int32(3), iter.next());
      assertNull(iter.next());
    }
  }

  @ParameterizedTest
  @MethodSource("materializations")
  void materializationCannotRaceWithStreaming(Consumer<StreamingArray> materialize) throws Exception {
    var input = new PausingInput("[1,2,3]");
    var array = (StreamingArray) new StreamingJSONParser(input, 1).parse();
    try (Iter iter = array.iterate()) {
      var reread = overlapReads(input, () -> assertEquals(Int32.ONE, iter.next()), () -> {
        materialize.accept(array);
        return null;
      });
      assertRereadFails(reread);
      assertEquals(new Int32(2), iter.next());
      assertEquals(new Int32(3), iter.next());
      assertNull(iter.next());
    }
  }

  @ParameterizedTest
  @MethodSource("materializations")
  void materializationBeforeIterationPublishesAlignedCache(Consumer<StreamingArray> materialize) throws Exception {
    var input = new PausingInput("[1,2,3]");
    var array = (StreamingArray) new StreamingJSONParser(input, 1).parse();
    try (Iter iter = array.iterate()) {
      var read = overlapReads(input, () -> materialize.accept(array), iter::next);
      assertEquals(Int32.ONE, read.get(10, TimeUnit.SECONDS));
      assertEquals(List.of(Int32.ONE, new Int32(2), new Int32(3)), array.values());
      assertEquals(new Int32(2), iter.next());
      assertEquals(new Int32(3), iter.next());
      assertNull(iter.next());
    }
    try (Iter replay = array.iterate()) {
      assertEquals(Int32.ONE, replay.next());
      assertEquals(new Int32(2), replay.next());
      assertEquals(new Int32(3), replay.next());
      assertNull(replay.next());
    }
  }

  @Test
  void overlappingMaterializersPreserveArrayIndices() throws Exception {
    var input = new PausingInput("[1,2,3]");
    var array = (StreamingArray) new StreamingJSONParser(input, 1).parse();
    var read = overlapReads(input, () -> assertEquals(Int32.ONE, array.at(0)), array::values);
    assertEquals(List.of(Int32.ONE, new Int32(2), new Int32(3)), read.get(10, TimeUnit.SECONDS));
    assertEquals(Int32.ONE, array.at(0));
    assertEquals(new Int32(2), array.at(1));
    assertEquals(new Int32(3), array.at(2));
  }

  @Test
  void overlappingEmptyArrayIteratorsRemainReplayable() throws Exception {
    var input = new PausingInput("[]");
    var array = (StreamingArray) new StreamingJSONParser(input, 1).parse();
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      var read = overlapReads(input, () -> assertNull(first.next()), second::next);
      assertNull(read.get(10, TimeUnit.SECONDS));
    }
    assertEquals(0, array.len());
    try (Iter replay = array.iterate()) {
      assertNull(replay.next());
    }
  }

  @Test
  void cachedPrefixDoesNotPermitRereadingUncachedSuffix() {
    StreamingArray array = array("[1,2,3]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter first = array.iterate(); Iter second = array.iterate()) {
      assertEquals(Int32.ONE, first.next());
      assertEquals(Int32.ONE, second.next());
      assertEquals(new Int32(2), first.next());
      assertRereadFails(second::next);
      assertRereadFails(() -> array.at(0));
      assertEquals(new Int32(3), first.next());
      assertNull(first.next());
    }
  }

  @Test
  void fullyMaterializedArrayCanBeReadRepeatedly() {
    StreamingArray array = array("[1,2,3,4,5,6,7,8,9]");
    assertEquals(9, array.values().size());
    for (int pass = 0; pass < 2; pass++) {
      try (Iter iter = array.iterate()) {
        for (int i = 1; i <= 9; i++) {
          assertEquals(new Int32(i), iter.next());
        }
        assertNull(iter.next());
      }
    }
    assertEquals(new Int32(9), array.length());
    assertEquals(Int32.ONE, array.at(0));
  }

  @Test
  void readingOnlyCachedElementsDoesNotPreventMaterialization() {
    StreamingArray array = array("[1,2,3]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertEquals(3, array.values().size());
      assertEquals(new Int32(2), iter.next());
      assertEquals(new Int32(3), iter.next());
      assertNull(iter.next());
    }
    assertEquals(3, array.len());
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
    }
  }

  @Test
  void exhaustingFullyCachedPrefixRemainsReplayable() {
    StreamingArray array = array("[1]");
    assertEquals(Int32.ONE, array.at(0));
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertNull(iter.next());
    }
    try (Iter iter = array.iterate()) {
      assertEquals(Int32.ONE, iter.next());
      assertNull(iter.next());
    }
    assertEquals(1, array.len());
  }

  @Test
  void emptyArrayCanBeReadRepeatedly() {
    StreamingArray array = array("[]");
    for (int pass = 0; pass < 2; pass++) {
      try (Iter iter = array.iterate()) {
        assertNull(iter.next());
      }
    }
    assertEquals(0, array.len());
  }

  @Test
  void unusedIteratorDoesNotConsumeStream() {
    StreamingArray array = array("[1,2]");
    try (Iter unused = array.iterate()) {
    }
    assertEquals(2, array.values().size());
    assertEquals(Int32.ONE, array.at(0));
  }
}
