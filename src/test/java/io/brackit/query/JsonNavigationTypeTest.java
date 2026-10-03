package io.brackit.query;

import io.brackit.query.atomic.Int32;
import io.brackit.query.sequence.ItemSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonNavigationTypeTest extends XQueryBaseTest {
  @ParameterizedTest
  @ValueSource(strings = { "1[0]", "1[]", "'text'[0]", "'text'[]", "<n/>[0]", "<n/>[]", "{}[0]", "{}[]", "(1)[0]",
      "('text')[0]", "(<n/>)[0]", "({})[0]", "(1)[]", "('text')[]", "(<n/>)[]", "({})[]", "(1, 'text', <n/>, {})[0]",
      "(1, 'text', <n/>, {})[]", "(1).a", "('text').a", "(<n/>).a", "([1]).a", "(1, 'text', <n/>, [1]).a" })
  void navigationSkipsNonMatchingItems(String query) {
    ResultChecker.dCheck(Int32.ZERO, new Query("count(" + query + ")").execute(ctx));
  }

  @Test
  void mixedSequencesPreserveMatchingArrayAndObjectResults() {
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, [10], {}, [20], 'text')[0]").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, [10], {}, [20], 'text')[]").execute(ctx));
    ResultChecker.dCheck(new ItemSequence(new Int32(10), new Int32(20)),
                         new Query("(1, <n/>, {\"a\":10}, [], {\"a\":20}, 'text').a").execute(ctx));
  }
}
