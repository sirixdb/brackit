package io.brackit.query.expr;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Int;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.IntNumeric;
import io.brackit.query.function.json.StreamingJSONParser;
import io.brackit.query.jdm.Sequence;
import io.brackit.query.jsonitem.array.StreamingArray;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ArrayAccessStreamingLookupTest extends XQueryBaseTest {
  private static final class CountingInput extends ByteArrayInputStream {
    int bytesRead;

    CountingInput(String json) {
      super(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized int read(byte[] bytes, int offset, int length) {
      int count = super.read(bytes, offset, length);
      if (count > 0) {
        bytesRead += count;
      }
      return count;
    }
  }

  private Sequence lookup(StreamingArray array, boolean mapped, IntNumeric index) {
    Sequence operand = mapped ? new ItemSequence(array) : array;
    return new ArrayAccessExpr(operand, index).evaluate(ctx, null);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void positiveLookupReadsOnlyTheRequestedPrefix(boolean mapped) {
    CountingInput input = new CountingInput("[10,20," + "30,".repeat(1000) + "40]");
    StreamingArray array = (StreamingArray) new StreamingJSONParser(input, 32).parse();
    ResultChecker.dCheck(new Int32(10), lookup(array, mapped, Int32.ZERO));
    ResultChecker.dCheck(new Int32(20), lookup(array, mapped, Int32.ONE));
    ResultChecker.dCheck(new Int32(10), lookup(array, mapped, Int32.ZERO));
    assertEquals(32, input.bytesRead);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void unrepresentablePositiveIndicesDoNotReadTheStream(boolean mapped) {
    CountingInput input = new CountingInput("[10,20," + "30,".repeat(1000) + "40]");
    StreamingArray array = (StreamingArray) new StreamingJSONParser(input, 32).parse();
    ResultChecker.dCheck(null, lookup(array, mapped, new Int("18446744073709551616")));
    ResultChecker.dCheck(null, lookup(array, mapped, new Int32(Integer.MAX_VALUE)));
    assertEquals(32, input.bytesRead);
    ResultChecker.dCheck(new Int32(10), lookup(array, mapped, Int32.ZERO));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void positiveOutOfBoundsIsEmptyAndRandomAccessRemainsStrict(boolean mapped) {
    StreamingArray array = (StreamingArray) new StreamingJSONParser(new CountingInput("[10]"), 32).parse();
    ResultChecker.dCheck(null, lookup(array, mapped, Int32.ONE));
    ResultChecker.dCheck(new Int32(10), lookup(array, mapped, Int32.ZERO));
    QueryException error = assertThrows(QueryException.class, () -> array.at(1));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, error.getCode());
    assertNull(array.atOrEmpty(1));
    assertEquals(new Int32(10), array.at(0));

    StreamingArray empty = (StreamingArray) new StreamingJSONParser(new CountingInput("[]"), 32).parse();
    ResultChecker.dCheck(null, lookup(empty, mapped, Int32.ZERO));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void negativeLookupResolvesFromTheEndWithoutNarrowing(boolean mapped) {
    StreamingArray array = (StreamingArray) new StreamingJSONParser(new CountingInput("[10,20]"), 32).parse();
    ResultChecker.dCheck(new Int32(20), lookup(array, mapped, new Int32(-1)));
    ResultChecker.dCheck(new Int32(10), lookup(array, mapped, new Int32(-2)));
    QueryException error = assertThrows(QueryException.class,
                                       () -> ResultChecker.dCheck(null, lookup(array, mapped, new Int("-18446744073709551616"))));
    assertEquals(ErrorCode.ERR_INVALID_ARGUMENT_TYPE, error.getCode());
  }
}
