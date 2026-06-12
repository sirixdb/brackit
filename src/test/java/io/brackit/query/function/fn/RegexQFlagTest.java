package io.brackit.query.function.fn;

import io.brackit.query.ErrorCode;
import io.brackit.query.Query;
import io.brackit.query.QueryException;
import io.brackit.query.ResultChecker;
import io.brackit.query.XQueryBaseTest;
import io.brackit.query.atomic.Bool;
import io.brackit.query.atomic.Int32;
import io.brackit.query.atomic.Str;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the regular-expression flag {@code q} (XQuery F&amp;O 5.6.1.1): all characters of the
 * pattern represent themselves. The flag may be combined with {@code i}; {@code m}, {@code s},
 * and {@code x} have no effect in its presence. With fn:replace, the replacement string is also
 * interpreted literally ({@code $} and {@code \} have no special significance). The flag was
 * previously rejected with FORX0001.
 */
public class RegexQFlagTest extends XQueryBaseTest {

  @Test
  public void matchesTreatsPatternAsLiteral() {
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('a.b', '.', 'q')").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("fn:matches('ab', '.', 'q')").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('a[0-9]b', '[0-9]', 'q')").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("fn:matches('a5b', '[0-9]', 'q')").execute(ctx));
    // The empty literal pattern matches every string (substring semantics).
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('abc', '', 'q')").execute(ctx));
  }

  @Test
  public void replaceTreatsPatternAndReplacementAsLiteral() {
    ResultChecker.dCheck(new Str("a-b-c"), new Query("fn:replace('a.b.c', '.', '-', 'q')").execute(ctx));
    // "$" and "\" lose their special significance in the replacement string.
    ResultChecker.dCheck(new Str("a$0"), new Query("fn:replace('ab', 'b', '$0', 'q')").execute(ctx));
    ResultChecker.dCheck(new Str("a\\"), new Query("fn:replace('ab', 'b', '\\', 'q')").execute(ctx));
  }

  @Test
  public void tokenizeSplitsOnLiteralSeparator() {
    ResultChecker.dCheck(new Int32(3), new Query("count(fn:tokenize('a.b.c', '.', 'q'))").execute(ctx));
    ResultChecker.dCheck(new Str("a|b|c"), new Query("string-join(fn:tokenize('a.b.c', '.', 'q'), '|')").execute(ctx));
  }

  @Test
  public void qCombinesWithIgnoreCaseAndNeutralizesOtherFlags() {
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('AB', 'a', 'iq')").execute(ctx));
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('A.B', 'a.b', 'qi')").execute(ctx));
    // With q, the x flag has no effect: the pattern whitespace is significant.
    ResultChecker.dCheck(Bool.TRUE, new Query("fn:matches('a b', 'a b', 'qx')").execute(ctx));
    ResultChecker.dCheck(Bool.FALSE, new Query("fn:matches('ab', 'a b', 'qx')").execute(ctx));
  }

  @Test
  public void emptyLiteralPatternIsRejectedForReplaceAndTokenize() {
    QueryException ex = assertThrows(QueryException.class,
                                     () -> new Query("fn:replace('abc', '', '-', 'q')").execute(ctx));
    assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING, ex.getCode());
    ex = assertThrows(QueryException.class, () -> new Query("fn:tokenize('abc', '', 'q')").execute(ctx));
    assertEquals(ErrorCode.ERR_REGULAR_EXPRESSION_EMPTY_STRING, ex.getCode());
  }

  @Test
  public void unknownFlagsAreStillRejected() {
    QueryException ex = assertThrows(QueryException.class, () -> new Query("fn:matches('a', 'a', 'qz')").execute(ctx));
    assertEquals(ErrorCode.ERR_INVALID_REGULAR_EXPRESSION_FLAGS, ex.getCode());
  }
}
