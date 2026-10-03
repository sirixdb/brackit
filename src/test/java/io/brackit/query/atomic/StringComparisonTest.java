package io.brackit.query.atomic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;

public class StringComparisonTest {
  @ParameterizedTest
  @ValueSource(strings = { "", "http://example.org/abcdefghijklmnop/" })
  public void stringFamilyUsesCodepointOrderWithAndWithoutCachedBytes(String prefix) {
    String[] suffixes = { "", "a", "\uFF21", "\uD83D\uDE00", "\uD83D\uDE00a" };
    for (String left : suffixes) {
      for (String right : suffixes) {
        String leftValue = prefix + left;
        String rightValue = prefix + right;
        int expected = Integer.signum(Arrays.compare(leftValue.codePoints().toArray(),
                                                     rightValue.codePoints().toArray()));
        Atomic[] leftKeys = { new Str(leftValue), new AnyURI(leftValue), new Una(leftValue) };
        Atomic[] rightKeys = { new Str(rightValue), new AnyURI(rightValue), new Una(rightValue) };
        for (boolean cached : new boolean[] { false, true }) {
          if (cached) {
            ((Str) leftKeys[0]).getUtf8Bytes();
            ((Str) rightKeys[0]).getUtf8Bytes();
          }
          for (Atomic leftKey : leftKeys) {
            for (Atomic rightKey : rightKeys) {
              assertEquals(expected, Integer.signum(leftKey.atomicCmp(rightKey)));
              assertEquals(expected, Integer.signum(leftKey.compareTo(rightKey)));
              if (leftKey instanceof AnyURI && rightKey instanceof Una || leftKey instanceof Una
                  && rightKey instanceof AnyURI) {
                assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                             assertThrows(QueryException.class, () -> leftKey.cmp(rightKey)).getCode());
              } else {
                assertEquals(expected, Integer.signum(leftKey.cmp(rightKey)));
              }
            }
          }
        }
      }
    }
  }

  @Test
  public void stringComparisonStillRejectsOtherAtomicTypes() {
    for (Atomic key : new Atomic[] { new Str("1"), new AnyURI("1"), new Una("1") }) {
      assertEquals(ErrorCode.ERR_TYPE_INAPPROPRIATE_TYPE,
                   assertThrows(QueryException.class, () -> key.cmp(Int32.ONE)).getCode());
    }
  }
}
