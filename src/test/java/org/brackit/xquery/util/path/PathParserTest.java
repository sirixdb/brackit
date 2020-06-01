package org.brackit.xquery.util.path;

import org.junit.Test;

import static org.junit.Assert.*;

public final class PathParserTest {

  @Test
  public void testParseDescendantArray() {
    final var pathParser = new PathParser("//[]");
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantArray(), path);
  }

  @Test
  public void testParseChildArray() {
    final var pathParser = new PathParser("/[]");
    final var path = pathParser.parse();
    assertEquals(new Path<>().childArray(), path);
  }
}