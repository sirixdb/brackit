package org.brackit.xquery.util.path;

import org.brackit.xquery.atomic.QNm;
import org.junit.Test;

import static org.junit.Assert.*;

public final class PathParserTest {

  @Test
  public void testParseDescendantArray() {
    final var pathParser = new PathParser("//[]", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantArray(), path);
  }

  @Test
  public void testParseChildArray() {
    final var pathParser = new PathParser("/[]", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().childArray(), path);
  }

  @Test
  public void testParseDescendantWithAtSign() {
    final var pathParser = new PathParser("//@a", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantObjectField(new QNm("@a")), path);
  }

  @Test
  public void testJsonFieldWithSlash1() {
    final var pathParser = new PathParser("//@a/\\/", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantObjectField(new QNm("@a")).childObjectField(new QNm("/")), path);
  }

  @Test
  public void testJsonFieldWithSlash2() {
    final var pathParser = new PathParser("//@a/\\//*", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantObjectField(new QNm("@a")).childObjectField(new QNm("/")).childObjectField(),
                 path);
  }

  @Test
  public void testJsonFieldWithSlash3() {
    final var pathParser = new PathParser("//\\/\\/\\/", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantObjectField(new QNm("///")), path);
  }

  @Test
  public void testJsonFieldWithBrackets() {
    final var pathParser = new PathParser("//\\[\\]", PathParser.Type.JSON);
    final var path = pathParser.parse();
    assertEquals(new Path<>().descendantObjectField(new QNm("[]")), path);
  }
}