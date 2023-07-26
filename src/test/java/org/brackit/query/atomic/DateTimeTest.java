package org.brackit.query.atomic;

import junit.framework.TestCase;
import org.junit.Test;

public final class DateTimeTest extends TestCase {
  @Test
  public void testParseString() {
    new DateTime("2020-05-06T11:07:21");
  }
}
