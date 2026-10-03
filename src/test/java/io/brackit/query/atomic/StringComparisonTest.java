package io.brackit.query.atomic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.brackit.query.ErrorCode;
import io.brackit.query.QueryException;
import io.brackit.query.function.json.FastJSONParser;
import io.brackit.query.util.Cmp;

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
  public void parsedLoneSurrogatesPreserveEqualityAndOrderingWithAndWithoutCachedBytes() {
    String prefix = "x".repeat(32);
    String[] escapes = { "\\uD800", "\\uD801", "\\uDC00", "\\uDC01", "\\uD800a", "a\\uDC00", "\\uD800\\uDC00", "?" };
    String[] suffixes = { "\uD800", "\uD801", "\uDC00", "\uDC01", "\uD800a", "a\uDC00", "\uD800\uDC00", "?" };
    for (int l = 0; l < escapes.length; l++) {
      for (int r = 0; r < escapes.length; r++) {
        String leftValue = prefix + suffixes[l];
        String rightValue = prefix + suffixes[r];
        int order = Integer.signum(Arrays.compare(leftValue.codePoints().toArray(), rightValue.codePoints().toArray()));
        boolean equal = leftValue.equals(rightValue);
        for (int cached = 0; cached < 4; cached++) {
          Str left = (Str) new FastJSONParser(("\"" + prefix + escapes[l] + "\"").getBytes(StandardCharsets.UTF_8))
                                                                                                                   .parse();
          Str right = (Str) new FastJSONParser(("\"" + prefix + escapes[r] + "\"").getBytes(StandardCharsets.UTF_8))
                                                                                                                    .parse();
          assertEquals(leftValue, left.stringValue());
          assertEquals(rightValue, right.stringValue());
          Atomic[] leftKeys = { left, left.asUna() };
          Atomic[] rightKeys = { right, right.asUna() };
          if ((cached & 1) != 0) {
            left.getUtf8Bytes();
          }
          if ((cached & 2) != 0) {
            right.getUtf8Bytes();
          }
          for (Atomic leftKey : leftKeys) {
            for (Atomic rightKey : rightKeys) {
              assertEquals(equal, leftKey.eq(rightKey));
              assertEquals(equal, rightKey.eq(leftKey));
              assertEquals(equal, Cmp.eq.gCmp(null, leftKey, rightKey));
              assertEquals(equal, Cmp.eq.gCmp(null, rightKey, leftKey));
              assertEquals(!equal, Cmp.ne.gCmp(null, leftKey, rightKey));
              assertEquals(!equal, Cmp.ne.gCmp(null, rightKey, leftKey));
              assertEquals(order, Integer.signum(leftKey.cmp(rightKey)));
              assertEquals(-order, Integer.signum(rightKey.cmp(leftKey)));
              assertEquals(order, Integer.signum(leftKey.atomicCmp(rightKey)));
              assertEquals(-order, Integer.signum(rightKey.atomicCmp(leftKey)));
              assertEquals(order, Integer.signum(leftKey.compareTo(rightKey)));
              assertEquals(-order, Integer.signum(rightKey.compareTo(leftKey)));
              assertEquals(equal, leftKey.equals(rightKey));
              assertEquals(equal, rightKey.equals(leftKey));
            }
          }
        }
      }
    }
  }

  @Test
  public void longURIAndUntypedKeysKeepComparisonDirection() {
    String suffix = "x".repeat(4096);
    Atomic[] lowKeys = { new Str("a" + suffix), new AnyURI("a" + suffix), new Una("a" + suffix) };
    Atomic[] highKeys = { new Str("b" + suffix), new AnyURI("b" + suffix), new Una("b" + suffix) };
    for (Atomic low : lowKeys) {
      for (Atomic high : highKeys) {
        assertEquals(-1, Integer.signum(low.atomicCmp(high)));
        assertEquals(1, Integer.signum(high.atomicCmp(low)));
        assertEquals(true, Cmp.lt.gCmp(null, low, high));
        assertEquals(false, Cmp.lt.gCmp(null, high, low));
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
