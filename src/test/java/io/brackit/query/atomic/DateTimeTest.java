package io.brackit.query.atomic;

import org.junit.jupiter.api.Test;

public final class DateTimeTest {
  @Test
  public void testParseString() {
    new DateTime("2020-05-06T11:07:21");
  }
}
